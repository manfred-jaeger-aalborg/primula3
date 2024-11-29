import sys
sys.path.append('/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python')
# from rbn import *
from gnn.ACR_graph import *
from gnn.ACR_node import *
# from ggcn_model_def import *
from gcn_model_def import *

# this function will be called by Primula with: model, x and edge_index, **kwargs
# the model needs to output a probability for each node! (if not -> out = torch.sigmoid(out))
# the idea should be that everyone define the function for the specific model: TODO maybe using python decorator?
# right now the naive solution is to just rewrite the function every time it is necessary
def infer_model_nodes(model, x, edge_index, **kwargs):        
    if model is not None:
        if type(model) is MYACRGnnNode:
            batch = torch.zeros(x.size()[0]).type(torch.LongTensor)
            out = model(x, edge_index, batch)
        else:
            out = model(x, edge_index)
        return out
    else:
        print("Model is None!")
    return None

def infer_model_graph(model, x, edge_index, **kwargs):
    if model is not None:
        if type(model) is MYACRGnnNode:
            batch = torch.zeros(x.size()[0]).type(torch.LongTensor)
            out = model(x, edge_index, batch)
        else:
            out = model(x, edge_index)
        return out
    return None

def set_model(model_class, weights_path, **kwargs):
    model = model_class(**kwargs).to("cpu")
    model.load_state_dict(torch.load(weights_path))
    model.eval()
    return model

# Define a dictionary to store model information with IDs
models_info = {
    "gnnNode": (MYACRGnnNode, 
        "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/models/alpha1_8_4_20240604-164429/RBN_acr_graph_alpha1_8_4.pt",
        {
            "input_dim": 2,
            "hidden_dim": [8,4],
            "num_layers": 2,
            "mlp_layers": 0,
            "final_read": "add",
            "num_classes": 1
        }
    ),
    "gnnGraph": (MYACRGnnGraph, "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/models/triangle_16_10_8_20240222-104544/RBN_graph_triangle_16_10_8.pt",
        {
            "input_dim": 7,
            "hidden_dim": [16, 10, 8],
            "num_layers": 3,
            "mlp_layers": 0,
            "final_read": "add",
            "num_classes": 2
        }
    ),
    "gnnGraph2": (MYACRGnnGraph, "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/models/triangle_16_10_8_20240222-104544/RBN_graph_triangle_16_10_8.pt", 
        {
            "input_dim": 7,
            "hidden_dim": [16, 10, 8],
            "num_layers": 3,
            "mlp_layers": 0,
            "final_read": "add",
            "num_classes": 2
        }
    ),
    "gnnGraphBig": (MYACRGnnNode, "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/models/alpha1_64_64_64_20240126-144611/RBN_acr_graph_alpha1_64_64_64.pt", 
        {
            "input_dim": 2,
            "hidden_dim": [64, 64, 64],
            "num_layers": 3,
            "mlp_layers": 0,
            "final_read": "add",
            "num_classes": 1
        }
    ),
    "gnnHomophily": (MYACRGnnNode, "/Users/lz50rg/Dev/homophily/gnn_trained_model_0.7.pt",
        {
            "input_dim": 2,
            "hidden_dim": [4],
            "num_layers": 1,
            "mlp_layers": 0,
            "final_read": "add",
            "num_classes": 1
        }
    ),
    "GCNHomophily": (Net, "/Users/lz50rg/Dev/homophily/experiments/models/GCN_1.0.pt",
        {
            "num_features":2,
            "dim":16,
            "num_classes":1
        }
    ),
    "GCNcora": (Net3, "/Users/lz50rg/Dev/homophily/experiments_cora/gcn.pt",
        {
            "num_features":1433,
            "dim":32,
            "num_classes":7
        }
    ),
    "GCNwis": (Net3, "/Users/lz50rg/Dev/homophily/experiments_wisconsin/gcn.pt",
        {
            "num_features":1703,
            "dim":32,
            "num_classes":2
        }
    ),
    "GCNcha": (Net3, "/Users/lz50rg/Dev/homophily/experiments_wiki/gcn.pt",
        {
            "num_features":2325,
            "dim":32,
            "num_classes":2
        }
    ),
    "GCNcat": (NetG, "/Users/lz50rg/Dev/homophily/categorical_gnn/homophily_2.pt",
        {
            "num_features":2,
            "dim":8,
            "num_classes":4
        }
    ),
    "GCNWisCat": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments_GGCN/Heterophily_and_oversmoothing/pretrained/gcn_Wisconsin_0.pt",
        {
            "dim_input":1703,
            "num_layers":2,
            "dim":32,
            "num_classes":5,
            "dropout":0.5
        }
    ),
    "GCNCham": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_chameleon_0.pt",
        {
            "dim_input":2325,
            "num_layers":2,
            "dim":16,
            "num_classes":5,
            "dropout":0.5,
            "primula": True
        }
    ),
    "GCNgraph": (GCN_graph, "/Users/lz50rg/Dev/logic-gnn/models/cat-graph.pt",
        {
            "num_features":7,
            "dim":8,
            "num_classes":2
        }
    ),
    "GCNwisconsin0": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/gcn_Wisconsin_0.pt",
        {
            "dim_input": 1703, "num_layers": 2, "dim": 16, "num_classes": 5, "dropout": 0.5, "primula": True
        }
    ),
    "GCNwisconsin1": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/gcn_Wisconsin_1.pt",
        {
            "dim_input": 1703, "num_layers": 2, "dim": 16, "num_classes": 5, "dropout": 0.5, "primula": True
        }
    ),
    "GCNwisconsin2": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/gcn_Wisconsin_2.pt",
        {
            "dim_input": 1703, "num_layers": 2, "dim": 16, "num_classes": 5, "dropout": 0.5, "primula": True
        }
    ),

    "GGCNwisconsin": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Wisconsin_0.pt",
        {
            "nfeat": 1703, "nlayers": 5, "nhidden": 16, "nclass": 5, "dropout": 0.5, "decay_rate": 0.9, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "primula":True
        }
    ),
    "GGCNtexas": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Texas_9.pt",
            {
                "nfeat": 1703, "nlayers": 2, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.9, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "primula":True
            }
    ),
    "GGCNchameleon": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_chameleon_1.pt",
        {
            "nfeat": 2325, "nlayers": 5, "nhidden": 32, "nclass": 5, "dropout": 0.3, "decay_rate": 0.8, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True,
        }
    ),
    "GGCNcora": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_cora_6.pt",
            {
                "nfeat": 1433, "nlayers": 32, "nhidden": 16, "nclass": 7, "dropout": 0.3, "decay_rate": 0.9, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True,
            }
    ),
    "GCNising": (GCN_graph,
        "/Users/lz50rg/Dev/homophily/experiments/ising/trained/GraphGCN_32_-0.5_-0.4_0.4_4.pt",
        {
            "dim_input":1,
            "num_layers":2,
            "dim":16,
            "num_classes":2,
            "dropout":0.3,
            "primula": True
        }
    ),
    "GGCNising": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/ising/trained/GGCN_raf_32_1.1_0.0_0.4_4.pt",
        {
            "nfeat": 1, "nlayers": 2, "nhidden": 16, "nclass": 2, "dropout": 0.5, "decay_rate": 0.9, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "primula":True
        }
    ),
    "riverGNN": (HeteroGraph, "/Users/lz50rg/Dev/water-hawqs/models/model.pth",
        {
            "in_sub":2, "in_hru":6, "hidden_dims":16, "out_dims":3, "num_layers":3
        }
    )
}

def use_model(model_id):
    model_class, weights_path, kwargs = models_info[model_id]
    return set_model(model_class, weights_path, **kwargs)

if __name__ == "__main__":
    model = use_model("GCN")
    print(model)
    


