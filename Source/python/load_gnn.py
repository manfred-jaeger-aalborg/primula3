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

# Define a dictionary to store model information with IDs
models_info = {
    "GCNtexas0": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_Texas_0.pt",
        {
            "nfeat":1703, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNtexas1": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_1.pt",
        {
            "nfeat":1703, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNtexas2": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_2.pt",
        {
            "nfeat":1703, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNtexas3": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_3.pt",
        {
            "nfeat":1703, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNtexas4": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_4.pt",
        {
            "nfeat":1703, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNtexas5": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_5.pt",
        {
            "nfeat":1703, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNtexas6": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_6.pt",
        {
            "nfeat":1703, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNtexas7": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_7.pt",
        {
            "nfeat":1703, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNtexas8": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_8.pt",
        {
            "nfeat":1703, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNtexas9": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_9.pt",
        {
            "nfeat":1703, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GGCNsquirrel0": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_squirrel_0.pt",
        {
            "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNsquirrel1": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_squirrel_1.pt",
        {
            "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNsquirrel2": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_squirrel_2.pt",
        {
            "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNsquirrel3": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_squirrel_3.pt",
        {
            "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNsquirrel4": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_squirrel_4.pt",
        {
            "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNsquirrel5": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_squirrel_5.pt",
        {
            "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNsquirrel6": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_squirrel_6.pt",
        {
            "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNsquirrel7": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_squirrel_7.pt",
        {
            "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNsquirrel8": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_squirrel_8.pt",
        {
            "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNsquirrel9": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GGCN_squirrel_9.pt",
        {
            "nfeat": 2089, "nlayers": 2, "nhidden": 32, "nclass": 5, "dropout": 0.5, "decay_rate": 1.0, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
    "riverGNN": (HeteroGraph, "/Users/lz50rg/Dev/water-hawqs/models/model.pth",
        {
            "in_sub":2, "in_hru_agr":5, "in_hru_urb":10, "hidden_dims":32, "out_dims":3, "num_layers":4
        }
    )
}

def use_model(model_id):
    model_class, weights_path, kwargs = models_info[model_id]
    return set_model(model_class, weights_path, **kwargs)