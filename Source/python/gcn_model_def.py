import torch
import torch.nn as nn
from torch_geometric.nn import GCNConv
from torch_geometric.nn import global_mean_pool
import torch.nn.functional as F

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
        self.lin = torch.nn.Linear(dim, num_classes)

    def forward(self, x, edge_index):
        h = F.dropout(x, p=0.5, training=self.training)
        h = F.relu(self.conv1(h, edge_index))
        h = F.dropout(h, p=0.5, training=self.training)
        h = F.relu(self.conv2(h, edge_index))
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

class GraphNet(torch.nn.Module):
    def __init__(self, num_features, dim=16, num_classes=1):
        super(GraphNet, self).__init__()
        self.num_classes = num_classes
        self.conv1 = GCNConv(num_features, dim)
        self.conv2 = GCNConv(dim, dim)
        self.conv3 = GCNConv(dim, dim)
        self.lin = torch.nn.Linear(dim, num_classes)

    def forward(self, x, edge_index, batch=None):
        h = F.relu(self.conv1(x, edge_index))
        # h = F.dropout(h, p=0.5, training=self.training)
        h = self.conv2(h, edge_index)
        h = self.conv3(h, edge_index)

        h = global_mean_pool(h, batch)

        h = self.lin(h)

        if self.num_classes == 1:
            return F.sigmoid(h)
        else:
            if not self.training:
                return F.softmax(h, dim=1)
            else:
                return h