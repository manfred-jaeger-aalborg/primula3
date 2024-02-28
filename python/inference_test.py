
import sys
sys.path.append('/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python')
print(sys.path)
from rbn import *
from gnn.ACR_graph import *
from gnn.ACR_node import *

def infer_model_nodes(model, x, edge_index, batch=None):
    if batch is None:
        batch = torch.zeros(x.size()[0]).type(torch.LongTensor)
    out = model(x, edge_index, batch)
    out = torch.sigmoid(out)
    return out

def infer_model_graph(model, x, edge_index, batch=None):
    if batch is None:
        batch = torch.zeros(x.size()[0]).type(torch.LongTensor)
    out = model(x, edge_index, batch)
    m = nn.Softmax(dim=1)
    return m(out)[0]

def set_model(model_class, weights_path, **kwargs):
    model = model_class(**kwargs)
    model.load_state_dict(torch.load(weights_path))
    model.eval()
    return model

# Define a dictionary to store model information with IDs
models_info = {
    "gnnNode": (MYACRGnnNode, 
        "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/models/only_blue_alpha1_10_5_20240123-151213/RBN_acr_graph_alpha1_10_5.pt", 
            {
                "input_dim": 2,
                "hidden_dim": [10, 5],
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
        )
}

def use_model(model_id):
    model_class, weights_path, kwargs = models_info[model_id]
    return set_model(model_class, weights_path, **kwargs)

if __name__ == "__main__":
    model = use_model("gnnGraph")
    print(model)
    


