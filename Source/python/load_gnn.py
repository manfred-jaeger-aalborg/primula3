from gcn_model_def import *

# this function will be called by Primula with: model, x and edge_index, **kwargs
# the model needs to output a probability for each node! (if not -> out = torch.sigmoid(out))
# the idea should be that everyone define the function for the specific model: TODO maybe using python decorator?
# right now the naive solution is to just rewrite the function every time it is necessary
def infer_model_nodes(model, x, edge_index, **kwargs):        
    if model is not None:
        out = model(x, edge_index)
        return out
    else:
        print("Model is None!")
    return None

def infer_model_graph(model, x, edge_index, **kwargs):
    if model is not None:
        out = model(x, edge_index)
        return out
    return None

def set_model(model_class, weights_path, **kwargs):
    model = model_class(**kwargs).to("cpu")
    model.load_state_dict(torch.load(weights_path, map_location="cpu", weights_only=False))
    model.eval()
    return model

# Helper to generate model configurations
def create_models_info(base_path, models_definitions):
    models_info = {}
    for model_id, (model_class, model_name, parameters) in models_definitions.items():
        weights_path = f"{base_path}/{model_name}.pt"
        models_info[model_id] = (model_class, weights_path, parameters)
    return models_info

# Define base path and models dynamically
base_path = "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained"
models_definitions = {
    f"GCNtexas{i}": (GCN_graph, f"GCN_Texas_{i}", {
        "nfeat": 1703, "nlayers": 2, "nhid": 16, "nclass": 5, "dropout": 0.5, "primula": True
    }) for i in range(10)
}

# models_definitions.update({
#     f"GGCNsquirrel{i}": (GGCN_raf, f"GGCN_squirrel_{i}", {
#         "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0,
#         "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5,
#         "use_bn": False, "use_ln": False, "generated": False, "pre_feature": True, "primula": True
#     }) for i in range(10)
# })
#
# models_definitions["riverGNN"] = (
#     HeteroGraph, "model", {
#         "in_sub": 2, "in_hru_agr": 5, "in_hru_urb": 10,
#         "hidden_dims": 32, "out_dims": 3, "num_layers": 4
#     }
# )
models_info = create_models_info(base_path, models_definitions)

def use_model(model_id):
    model_class, weights_path, kwargs = models_info[model_id]
    return set_model(model_class, weights_path, **kwargs)