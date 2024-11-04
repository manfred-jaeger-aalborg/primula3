import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.nn.parameter import Parameter
from torch_geometric.nn import GCNConv,  SAGEConv, HeteroConv,  MessagePassing, APPNP, global_mean_pool
from torch_geometric.nn.conv.gcn_conv import gcn_norm
from torch_geometric.utils import to_networkx
from torch_geometric.data import Data, HeteroData
from torch_geometric.utils import normalize_edge_index
import math
import numpy as np
import scipy.sparse as sp
import networkx as nx

class Net2(torch.nn.Module):
    def __init__(self, num_features, dim=16, num_classes=1):
        super(Net, self).__init__()
        self.conv1 = GCNConv(num_features, dim)
        self.conv2 = GCNConv(dim, num_classes)

    def forward(self, x, edge_index, data=None):
        x = F.relu(self.conv1(x, edge_index))
        x = F.dropout(x, training=self.training)
        x = self.conv2(x, edge_index)
        return F.log_softmax(x, dim=1)

class Net(torch.nn.Module):
    def __init__(self, num_features, dim=16, num_classes=1):
        super(Net, self).__init__()
        self.conv1 = GCNConv(num_features, dim, improved=True)
        self.lin = nn.Linear(dim, num_classes)

    def forward(self, x, edge_index):
        h = F.relu(self.conv1(x, edge_index))
        h = self.lin(h)
        return torch.sigmoid(h)

class Net3(torch.nn.Module):
    def __init__(self, num_features, dim=32, num_classes=1):
        super(Net3, self).__init__()
        self.conv1 = GCNConv(num_features, dim, improved=True)
        self.conv2 = GCNConv(dim, dim, improved=True)
        self.conv3 = GCNConv(dim, dim, improved=True)
        self.lin = torch.nn.Linear(dim, num_classes)

    def forward(self, x, edge_index):
        h = F.dropout(x, p=0.5, training=self.training)
        h = F.relu(self.conv1(h, edge_index))
        h = F.dropout(h, p=0.5, training=self.training)
        h = F.relu(self.conv2(h, edge_index))

        h = F.dropout(h, p=0.5, training=self.training)
        h = F.relu(self.conv3(h, edge_index))

        h = self.lin(h)
        return F.softmax(h, dim=1)

class NetG(torch.nn.Module):
    def __init__(self, num_features, dim=16, num_classes=1):
        super(NetG, self).__init__()
        self.num_classes = num_classes
        self.conv1 = GCNConv(num_features, dim, improved=True)
        self.conv2 = GCNConv(dim, num_classes, improved=True)

    def forward(self, x, edge_index):
        h = F.relu(self.conv1(x, edge_index))
        h = F.dropout(h, p=0.5, training=self.training)
        h = self.conv2(h, edge_index)
        if self.num_classes == 1:
            return F.sigmoid(h)
        else:
            return F.softmax(h, dim=1)

class NodeNet(torch.nn.Module):
    def __init__(self, dim_input, num_layers, dim=16, num_classes=1, dropout=0.5):
        super(NodeNet, self).__init__()
        self.num_classes = num_classes
        self.dropout = dropout
        self.convs = torch.nn.ModuleList()
        self.convs.append(GCNConv(dim_input, dim, improved=True))
        for _ in range(num_layers-1):
            self.convs.append(GCNConv(dim, dim, improved=True))
        self.lin = torch.nn.Linear(dim, num_classes)

    def forward(self, x, edge_index, batch=None):
        h = x
        for conv in self.convs:
            h = F.relu(conv(h, edge_index))
            h = F.dropout(h, p=self.dropout, training=self.training)
        h = self.lin(h)
        if self.num_classes == 1:
            return F.sigmoid(h)
        else:
            if not self.training:
                return F.softmax(h, dim=1)
            else:
                return h

class GGCNlayer(nn.Module):
    def __init__(self, in_features, out_features, use_degree=True, use_sign=True, use_decay=True, scale_init=0.5, deg_intercept_init=0.5):
        super(GGCNlayer, self).__init__()
        self.in_features = in_features
        self.out_features = out_features
        self.fcn = nn.Linear(in_features, out_features)
        self.use_degree = use_degree
        self.use_sign = use_sign
        if use_degree:
            if use_decay:
                self.deg_coeff = nn.Parameter(torch.tensor([0.5,0.0]))
            else:
                self.deg_coeff = nn.Parameter(torch.tensor([deg_intercept_init,0.0]))
        if use_sign:
            self.coeff = nn.Parameter(0*torch.ones([3]))
            if use_decay:
                self.scale = nn.Parameter(2*torch.ones([1]))
            else:
                self.scale = nn.Parameter(scale_init*torch.ones([1]))
        self.sftmax = nn.Softmax(dim=-1)
        self.sftpls = nn.Softplus(beta=1)

    def forward(self, h, adj, degree_precompute):
        if self.use_degree:
            sc = self.deg_coeff[0]*degree_precompute+self.deg_coeff[1]
            sc = self.sftpls(sc)

        Wh = self.fcn(h)
        if self.use_sign:
            prod = torch.matmul(Wh, torch.transpose(Wh, 0, 1))
            sq = torch.unsqueeze(torch.diag(prod),1)
            scaling = torch.matmul(sq, torch.transpose(sq, 0, 1))
            e = prod/torch.max(torch.sqrt(scaling),1e-9*torch.ones_like(scaling))
            e = e-torch.diag(torch.diag(e))
            if self.use_degree:
                attention = e*adj*sc
            else:
                attention = e*adj

            attention_pos = F.relu(attention)
            attention_neg = -F.relu(-attention)
            prop_pos = torch.matmul(attention_pos, Wh)
            prop_neg = torch.matmul(attention_neg, Wh)

            coeff = self.sftmax(self.coeff)
            scale = self.sftpls(self.scale)
            result = scale*(coeff[0]*prop_pos+coeff[1]*prop_neg+coeff[2]*Wh)

        else:
            if self.use_degree:
                prop = torch.matmul(adj*sc, Wh)
            else:
                prop = torch.matmul(adj, Wh)

            result = prop

        return result

class GGCN_raf(nn.Module):
    def __init__(self, nfeat, nlayers, nhidden, nclass, dropout, decay_rate, exponent, use_degree=True, use_sign=True, use_decay=True, use_sparse=False, scale_init=0.5, deg_intercept_init=0.5, use_bn=False, use_ln=False, generated=False, pre_feature=True, primula=False):
        super(GGCN_raf, self).__init__()
        self.dropout = dropout
        self.convs = nn.ModuleList()
        if use_sparse:
            model_sel = GGCNlayer_SP
        else:
            model_sel = GGCNlayer
        self.convs.append(model_sel(nfeat, nhidden, use_degree, use_sign, use_decay, scale_init, deg_intercept_init))
        for _ in range(nlayers-2):
            self.convs.append(model_sel(nhidden, nhidden, use_degree, use_sign, use_decay, scale_init, deg_intercept_init))
        self.convs.append(model_sel(nhidden, nclass, use_degree, use_sign, use_decay, scale_init, deg_intercept_init))
        self.fcn = nn.Linear(nfeat, nhidden)
        self.act_fn = F.elu
        self.dropout = dropout
        self.use_decay = use_decay
        if self.use_decay:
            self.decay = decay_rate
            self.exponent = exponent
        self.degree_precompute = None
        self.use_degree = use_degree
        self.use_sparse = use_sparse
        self.use_norm = use_bn or use_ln
        if self.use_norm:
            self.norms = nn.ModuleList()
        if use_bn:
            for _ in range(nlayers-1):
                self.norms.append(nn.BatchNorm1d(nhidden))
        if use_ln:
            for _ in range(nlayers-1):
                self.norms.append(nn.LayerNorm(nhidden))
        self.generated = generated
        self.pre_feature = pre_feature
        self.primula = primula

    def precompute_degree_d(self, adj):
        diag_adj = torch.diag(adj)
        diag_adj = torch.unsqueeze(diag_adj, dim=1)
        self.degree_precompute = diag_adj/torch.max(adj, 1e-9*torch.ones_like(adj))-1

    def precompute_degree_s(self, adj):
        adj_i = adj._indices()
        adj_v = adj._values()
        adj_diag_ind = (adj_i[0,:]==adj_i[1,:])
        adj_diag = adj_v[adj_diag_ind]
        v_new = torch.zeros_like(adj_v)
        for i in range(adj_i.shape[1]):
            v_new[i] = adj_diag[adj_i[0,i]]/adj_v[i]-1
        self.degree_precompute = torch.sparse.FloatTensor(adj_i, v_new, adj.size())

    def sys_normalized_adjacency(self, adj):
        adj = sp.coo_matrix(adj)
        adj = adj + sp.eye(adj.shape[0])
        row_sum = np.array(adj.sum(1))
        row_sum=(row_sum==0)*1+row_sum
        d_inv_sqrt = np.power(row_sum, -0.5).flatten()
        d_inv_sqrt[np.isinf(d_inv_sqrt)] = 0.
        d_mat_inv_sqrt = sp.diags(d_inv_sqrt)
        return d_mat_inv_sqrt.dot(adj).dot(d_mat_inv_sqrt).tocoo()

    def sparse_mx_to_torch_sparse_tensor(self, sparse_mx):
        """Convert a scipy sparse matrix to a torch sparse tensor."""
        sparse_mx = sparse_mx.tocoo().astype(np.float32)
        indices = torch.from_numpy(
            np.vstack((sparse_mx.row, sparse_mx.col)).astype(np.int64))
        values = torch.from_numpy(sparse_mx.data)
        shape = torch.Size(sparse_mx.shape)
        return torch.sparse.FloatTensor(indices, values, shape)

    def preprocess_features(self, features):
        """Row-normalize feature matrix and convert to tuple representation"""
        rowsum = np.array(features.sum(1))
        rowsum = (rowsum==0)*1+rowsum
        r_inv = np.power(rowsum, -1).flatten()
        r_inv[np.isinf(r_inv)] = 0.
        r_mat_inv = sp.diags(r_inv)
        features = r_mat_inv.dot(features)
        return features

    def forward(self, x, adj):
        if not self.generated: # prepare the data for GGCN as they to in the original code
            data = Data(x=x, edge_index=adj)
            G = to_networkx(data)
            adj = nx.adjacency_matrix(G, sorted(G.nodes()))

            adj = self.sys_normalized_adjacency(adj)
            adj = self.sparse_mx_to_torch_sparse_tensor(adj)
            if self.pre_feature: # false if we have 1 numeric feature only
                x = self.preprocess_features(x)
            x = torch.FloatTensor(x)
            adj = adj.to_dense() # remove if sparse

        if self.use_degree:
            if self.degree_precompute is None:
                if self.use_sparse:
                    self.precompute_degree_s(adj)
                else:
                    self.precompute_degree_d(adj)
        x = F.dropout(x, self.dropout, training=self.training)
        layer_previous = self.fcn(x)
        layer_previous = self.act_fn(layer_previous)
        layer_inner = self.convs[0](x, adj, self.degree_precompute)

        for i,con in enumerate(self.convs[1:]):
            if self.use_norm:
                layer_inner = self.norms[i](layer_inner)
            layer_inner = self.act_fn(layer_inner)
            layer_inner = F.dropout(layer_inner, self.dropout, training=self.training)
            if i==0:
                layer_previous = layer_inner + layer_previous
            else:
                if self.use_decay:
                    coeff = math.log(self.decay/(i+2)**self.exponent+1)
                else:
                    coeff = 1
                layer_previous = coeff*layer_inner + layer_previous
            layer_inner = con(layer_previous,adj,self.degree_precompute)

        if not self.primula:
            return F.log_softmax(layer_inner, dim=1)
        else:
            return F.softmax(layer_inner, dim=1)

class HeteroGraph(torch.nn.Module):
    def __init__(self, in_sub, in_hru, hidden_dims, out_dims, num_layers=3):
        super().__init__()
        self.layers = torch.nn.ModuleList()
        self.num_layers = num_layers
        self.sub_size = in_sub
        self.hru_size = in_hru
        for i in range(self.num_layers):
            if i == 0:
                self.layers.append(HeteroConv({
                    ('sub', 'to', 'sub'): SAGEConv(self.sub_size, hidden_dims, aggr='sum'),
                    ('hru', 'to', 'sub'): SAGEConv((self.hru_size, self.sub_size), hidden_dims, aggr='sum'),
                    ('sub', 'to', 'hru'): SAGEConv((self.sub_size, self.hru_size), hidden_dims),
                }, aggr='cat'))
            elif i != self.num_layers - 1:
                self.layers.append(HeteroConv({
                    ('sub', 'to', 'sub'): SAGEConv(hidden_dims*2, hidden_dims),
                    ('hru', 'to', 'sub'): SAGEConv((self.hru_size, hidden_dims*2), hidden_dims),
                    ('sub', 'to', 'hru'): SAGEConv((hidden_dims*2, self.hru_size), hidden_dims),
                }, aggr='cat'))
            elif i == self.num_layers - 1:
                self.layers.append(HeteroConv({
                    ('sub', 'to', 'sub'): SAGEConv(hidden_dims*2, hidden_dims),
                    ('hru', 'to', 'sub'): SAGEConv((self.hru_size, hidden_dims*2), hidden_dims),
                    ('sub', 'to', 'hru'): SAGEConv((hidden_dims*2, self.hru_size), hidden_dims),
                }, aggr='cat'))

        self.conv = SAGEConv(-1, hidden_dims)
        self.final_lin = torch.nn.Linear(hidden_dims*2, out_dims)

    def forward(self, x_dict, edge_index_dict):
        skip_hru = x_dict['hru']
        for i, layer in enumerate(self.layers):
            x_dict = layer(x_dict, edge_index_dict)
            if i != self.num_layers - 1:
                x_dict['sub'] = F.relu(x_dict['sub'])
                x_dict['hru'] = skip_hru

        out = self.final_lin(x_dict['sub'])
        return F.softmax(out, dim=1)

class GCN_graph(nn.Module):
    def __init__(self, nfeat, nlayers, nhid, nclass, dropout, primula=False):
        super(GCN_graph, self).__init__()
        self.convs = nn.ModuleList()
        self.convs.append(GCNConv(nfeat, nhid, normalize=False))
        for _ in range(nlayers-2):
            self.convs.append(GCNConv(nhid, nhid, normalize=False))
        self.convs.append(GCNConv(nhid, nclass, normalize=False))
        self.dropout = dropout
        self.primula = primula
        self.num_classes = nclass

    def forward(self, x, adj):
        shape = torch.Size([x.shape[0], x.shape[0]])
        adj = normalize_edge_index(adj, x.size(0), add_self_loops=True)
        adj = torch.sparse.FloatTensor(adj[0], adj[1], shape)

        for gc in self.convs[:-1]:
            x = F.relu(gc(x, adj))
            x = F.dropout(x, self.dropout, training=self.training)

        x = self.convs[-1](x, adj)
        if self.num_classes == 1:
            return F.sigmoid(x)
        else:
            if not self.primula:
                return F.log_softmax(x, dim=1)
            else:
                return F.softmax(x, dim=1)