from gcn_model_def import *

# this function will be called by Primula with: model, x and edge_index, **kwargs
# the model needs to output a probability for each node! (if not -> out = torch.sigmoid(out))
# the idea should be that everyone define the function for the specific model: TODO maybe using python decorator?
# right now the naive solution is to just rewrite the function every time it is necessary
def infer_model_nodes(model, x, edge_index, **kwargs):
    model.eval()
    if model is not None:
        out = model(x, edge_index)
        return out
    else:
        print("Model is None!")
    return None

def infer_model_graph(model, x, edge_index, **kwargs):
    model.eval()
    if model is not None:
        out = model(x, edge_index)
        return out
    return None

def set_model(model_class, weights_path, **kwargs):
    model = model_class(**kwargs).to("cpu")
    model.load_state_dict(torch.load(weights_path, map_location="cpu", weights_only=True)) # find out why False with water
    model.eval()
    return model

# Helper to generate model configurations
def create_models_info(base_path, models_definitions):
    models_info = {}
    for model_id, (model_class, model_name, parameters) in models_definitions.items():
        # raise KeyError(f"PROVA {base_path}{model_name}")
        weights_path = f"{base_path}{model_name}.pt" ## .pt for the others!! only water pth
        models_info[model_id] = (model_class, weights_path, parameters)
    return models_info


base_path = "default_path"
sdataset = "default_dataset"
nfeat = 0
nlayers = 0
nclass = 0
nhid = 0

# Initialize model definitions and models_info
models_definitions = dict()
models_info = create_models_info(base_path, models_definitions)

def set_vars(setd):
    global base_path, sdataset, nfeat, nlayers, nclass, nhid, models_info, models_definitions
    base_path = setd['base_path']
#     models_definitions = {
#         f"GCN{setd["sdataset"]}{i}": (GCN_graph, f"GCN_{setd["sdataset"]}_{i}", {
#             "nfeat": setd["nfeat"], "nlayers": setd["nlayers"], "nhid": setd["nhid"], "nclass": setd["nclass"], "dropout": 0.5, "primula": True
#         }) for i in range(10)
#     }
    if setd['model'] == 'GCN':
        models_definitions = {
            f"GCN{setd['sdataset']}{i}": (GCN_graph, f"GCN_{setd['sdataset']}_{i}", {
                "nfeat": setd['nfeat'], "nlayers": setd['nlayers'], "nhid": setd['nhid'], "nclass": setd['nclass'], "dropout": 0.5, "primula": True
            }) for i in range(10)
        }

    if setd['model'] == 'GraphNet':
        models_definitions = {
                    f"GraphNet{setd['sdataset']}": (
                        GCN_graph, f"{setd['model']}_{setd['N']}_{setd['J']}_{setd['Jb']}_{setd['temp']}_{setd['iter']}", {
                        "nfeat": setd['nfeat'], "nlayers": setd['nlayers'], "nhid": setd['nhid'], "nclass": setd['nclass'], "dropout": 0.3, "primula": True}
                    )
        }
    if setd['model'] == 'GGCN_raf':
            models_definitions = {
                        f"GGCN_raf{setd['sdataset']}": (
                            GGCN_raf, f"{setd['model']}_{setd['N']}_{setd['J']}_{setd['Jb']}_{setd['temp']}_{setd['iter']}", {
                            "nfeat": setd['nfeat'], "nlayers": setd['nlayers'], "nhidden": setd['nhid'], "nclass": setd['nclass'], "dropout": 0.5,
                            "decay_rate": 0.9, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False,
                            "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated": False, "pre_feature": False, "primula": True }
                        )
            }
    if setd['model'] == 'riverGNN':
        models_definitions["riverGNN"] = (
            HeteroGraph, "model", {
                "in_sub": 2, "in_hru_agr": 5, "in_hru_urb": 10, "hidden_dims": 32, "out_dims": 3, "num_layers": 4
            }
        )

    models_info = create_models_info(base_path, models_definitions)
    return models_info
    
# models_definitions.update({
#     f"GGCNsquirrel{i}": (GGCN_raf, f"GGCN_squirrel_{i}", {
#         "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0,
#         "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5,
#         "use_bn": False, "use_ln": False, "generated": False, "pre_feature": True, "primula": True
#     }) for i in range(10)
# })
#



def use_model(model_id):
    if model_id not in models_info:
        raise KeyError(f"Model ID '{model_id}' not found in models_info. Available keys: {list(models_info.keys())}")
    model_class, weights_path, kwargs = models_info[model_id]
    return set_model(model_class, weights_path, **kwargs)