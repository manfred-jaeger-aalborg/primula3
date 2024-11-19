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
    model.load_state_dict(torch.load(weights_path, map_location="cpu", weights_only=True))
    model.eval()
    return model

# Define a dictionary to store model information with IDs
models_info = {
    "GCNsquirrel0": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_0.pt",
        {
            "nfeat":2089, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNsquirrel1": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_1.pt",
        {
            "nfeat":2089, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNsquirrel2": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_2.pt",
        {
            "nfeat":2089, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNsquirrel3": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_3.pt",
        {
            "nfeat":2089, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNsquirrel4": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_4.pt",
        {
            "nfeat":2089, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNsquirrel5": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_5.pt",
        {
            "nfeat":2089, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNsquirrel6": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_6.pt",
        {
            "nfeat":2089, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNsquirrel7": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_7.pt",
        {
            "nfeat":2089, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNsquirrel8": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_8.pt",
        {
            "nfeat":2089, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GCNsquirrel9": (GCN_graph, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/GCN_squirrel_9.pt",
        {
            "nfeat":2089, "nlayers":2, "nhid":16, "nclass":5, "dropout":0.5, "primula":True
        }
    ),
    "GGCNcornell0": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Cornell_0.pt",
        {
            "nfeat": 1703, "nlayers": 6, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.7, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNcornell1": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Cornell_1.pt",
        {
            "nfeat": 1703, "nlayers": 6, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.7, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNcornell2": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Cornell_2.pt",
        {
            "nfeat": 1703, "nlayers": 6, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.7, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNcornell3": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Cornell_3.pt",
        {
            "nfeat": 1703, "nlayers": 6, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.7, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNcornell4": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Cornell_4.pt",
        {
            "nfeat": 1703, "nlayers": 6, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.7, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNcornell5": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Cornell_5.pt",
        {
            "nfeat": 1703, "nlayers": 6, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.7, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNcornell6": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Cornell_6.pt",
        {
            "nfeat": 1703, "nlayers": 6, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.7, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNcornell7": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Cornell_7.pt",
        {
            "nfeat": 1703, "nlayers": 6, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.7, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNcornell8": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Cornell_8.pt",
        {
            "nfeat": 1703, "nlayers": 6, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.7, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
        }
    ),
     "GGCNcornell9": (GGCN_raf, "/Users/lz50rg/Dev/homophily/experiments/Heterophily_and_oversmoothing/pretrained/ggcn_Cornell_9.pt",
        {
            "nfeat": 1703, "nlayers": 6, "nhidden": 16, "nclass": 5, "dropout": 0.4, "decay_rate": 0.7, "exponent": 3.0, "use_degree": True, "use_sign": True, "use_decay": True, "use_sparse": False, "scale_init": 0.5, "deg_intercept_init": 0.5, "use_bn": False, "use_ln": False, "generated":False, "pre_feature":True, "primula":True
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