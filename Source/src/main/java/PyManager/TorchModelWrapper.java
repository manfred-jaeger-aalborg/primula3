package PyManager;
import RBNpackage.ProbForm;
import jep.*;

import java.io.StringWriter;
import java.util.*;

public class TorchModelWrapper {
    private final String modelName;
    private final SharedInterpreter modelInterpreter;
    private final String modelClassName;

    public TorchModelWrapper(String modelName, String modelClassName, SharedInterpreter interpreter) {
        this.modelName = modelName;
        this.modelInterpreter = interpreter;
        this.modelClassName = modelClassName;

        initializePythonEnvironment();
    }

    private void initializePythonEnvironment() {
        try {
            modelInterpreter.exec("import torch");
            modelInterpreter.exec("from torch_geometric.data import HeteroData");

            // Define reusable Python functions for GNN forward pass
            modelInterpreter.exec("""
                def _get_type_and_device(model):
                    # Determine model dtype & device from parameters or buffers (fallback to default float & cpu)
                    param = next(model.parameters(), None)
                    if param is None:
                        buf = next(model.buffers(), None)
                        if buf is None:
                            model_dtype = torch.get_default_dtype()
                            device = torch.device("cpu")
                        else:
                            model_dtype = buf.dtype
                            device = buf.device
                    else:
                        model_dtype = param.dtype
                        device = param.device
                    return model_dtype, device
                
                def _to_tensor(x, dtype, device, requires_grad=False):
                    if isinstance(x, torch.Tensor):
                        t = x.to(device=device, dtype=dtype)
                    else:
                        t = torch.as_tensor(x, dtype=dtype, device=device)
                    if requires_grad:
                        t = t.clone().detach().requires_grad_(True)
                    else:
                        # clone+detach so we don't accidentally keep a graph or modify input
                        t = t.clone().detach()
                    return t
                    
                    
                def forward_single_primula_(model, x_dict, edge_dict=None, with_gradients=False, edge_attr=None):
                
                    # get dtype & device from model
                    model_dtype, device = _get_type_and_device(model)
                
                    # extract the first values from the dict-like inputs
                    x_val = next(iter(x_dict.values()))
                    e_val = next(iter(edge_dict.values())) if edge_dict else None
                    ea_val = next(iter(edge_attr.values())) if edge_attr is not None else None
                
                    xi = _to_tensor(x_val, dtype=model_dtype, device=device, requires_grad=with_gradients)
                
                    if e_val is not None:
                        ei = torch.as_tensor(e_val, dtype=torch.long, device=device)
                    else:
                        ei = torch.empty((2, 0), dtype=torch.long, device=device)
                
                    ea = None
                    if ea_val is not None:
                        ea = _to_tensor(ea_val, dtype=model_dtype, device=device, requires_grad=with_gradients)
                
                    if with_gradients:
                        # clear gradients on inputs if present
                        if xi.grad is not None:
                            xi.grad.zero_()
                        if ea is not None and ea.grad is not None:
                            ea.grad.zero_()
                
                        out = model(xi, ei, ea) if ea is not None else model(xi, ei)
                
                        # reduce to scalar then backprop
                        out_scalar = out.sum()
                        out_scalar.backward()
                
                        # gather gradients and convert to numpy (or None)
                        x_grad = xi.grad.detach().cpu().numpy() if xi.grad is not None else None
                        ea_grad = ea.grad.detach().cpu().numpy() if (ea is not None and ea.grad is not None) else None
                
                        grads = (x_grad, ea_grad)
                    else:
                        with torch.no_grad():
                            out = model(xi, ei, ea) if ea is not None else model(xi, ei)
                        grads = None
                
                    return out, grads
                
                def forward_hetero_primula_(model, x_dict, edge_dict, edge_rels):
                    model_dtype, device = _get_type_and_device(model)
                    data_h = HeteroData()
                    for key, value in x_dict.items():
                        data_h[key].x = torch.as_tensor(value, dtype=model_dtype, device=device)
                    for key, value in edge_dict.items():
                        edge_type = (edge_rels[key][0], key, edge_rels[key][1])
                        if value:
                            data_h[edge_type].edge_index = torch.as_tensor(value, dtype=torch.long, device=device)
                        else:
                            data_h[edge_type].edge_index = torch.empty((2, 0), dtype=torch.long, device=device)
                    model.eval()
                    with torch.no_grad():
                        out = model(data_h.x_dict, data_h.edge_index_dict)
                    return out
                """);
            // TODO implement forward_hetero_primula_ for edge_attr support
        } catch (JepException e) {
            throw new RuntimeException("Failed to initialize Python environment", e);
        }
    }

    // return an object[]
    // obj[0] is double[][] of probabilities
    // obj[1] is a Map<String, double[][]> of gradient for "x" (node attributes) and "ea" (edge attributes)
    public Object[] forward(Map<String, double[][]> xDict,
                              Map<String, ArrayList<ArrayList<Integer>>> edgeDict,
                              Map<String, double[][]> edge_attr,
                              List<TorchInputSpecs> gnnInputs,
                              boolean withgradients) {
        // we use the same convention:
        // result[0] is probabilities and result[1] gradients
        Object[] result = new Object[2];
        try {
            // Set input data
            modelInterpreter.set("x_pyGNNRBN", xDict);
            modelInterpreter.set("edge_pyGNNRBN", edgeDict);
            if (edge_attr.size() > 0)
                modelInterpreter.set("edge_attr_pyGNNRBN", edge_attr);

            if (xDict.size() == 1) {
                if (edge_attr.size() > 0)
                    modelInterpreter.exec("out_pyGNNRBN, grad = forward_single_primula_(model=" + modelName + ", x_dict=x_pyGNNRBN, edge_dict=edge_pyGNNRBN, with_gradients=" + (withgradients ? "True":"False") + ", edge_attr=edge_attr_pyGNNRBN)");
                else
                    modelInterpreter.exec("out_pyGNNRBN, grad = forward_single_primula_(model=" + modelName + ", x_dict=x_pyGNNRBN, edge_dict=edge_pyGNNRBN, with_gradients=" + (withgradients ? "True":"False") + ")");
            } else {
                // Here the GNN is heterogeneous
                // Build edge relation dictionary
                Map<String, String[]> edgeRels = new HashMap<>();
                for (TorchInputSpecs input : gnnInputs) {
                    edgeRels.put(
                            input.getEdgeRelation().name(),
                            new String[]{
                                    input.getEdgeRelation().getTypes()[0].getName(),
                                    input.getEdgeRelation().getTypes()[1].getName()
                            });
                }
                if (edge_attr.size() > 0)
                    throw new RuntimeException("Edge attribute not jet implemented for heterogeneous GNNs");
                modelInterpreter.set("edge_rels_pyGNNRBN", edgeRels);
                modelInterpreter.exec("out_pyGNNRBN = forward_hetero_primula_(model=" + modelName + ", x_dict=x_pyGNNRBN, edge_dict=edge_pyGNNRBN, edge_rels=edge_rels_pyGNNRBN)");
            }

            NDArray outArray = (NDArray) modelInterpreter.getValue("out_pyGNNRBN.detach().numpy()");
            Object raw = outArray.getData();
            double[] flatData = PyUtils.toDoubleArray(raw);

            int[] outDim = outArray.getDimensions();
            if (outDim.length == 2) {
                int rows = outArray.getDimensions()[0];
                int cols = outArray.getDimensions()[1];
                result[0] = PyUtils.convertTo2D(flatData, rows, cols);
            } else if (outDim.length == 1) {
                result[0] = new double[][]{flatData};
            } else {
                throw new RuntimeException("Invalid output shape: " + Arrays.toString(outDim));
            }

            NDArray grad_x = null;
            if (withgradients && xDict.size() == 1) {
                // store the gradients for each input (x and if present also edge attributes) in a dictionary
                Map<String, double[][]> gradsDict = new HashMap<>();

                modelInterpreter.exec("grad_x_pyGNNRBN = grad[0]");
                grad_x = (NDArray) modelInterpreter.getValue("grad_x_pyGNNRBN");

                raw = grad_x.getData();
                double[] flatGradX = PyUtils.toDoubleArray(raw);
                outDim = grad_x.getDimensions();
                double[][] gradXArray;
                if (outDim.length == 2) {
                    int rows = grad_x.getDimensions()[0];
                    int cols = grad_x.getDimensions()[1];
                    gradXArray = PyUtils.convertTo2D(flatGradX, rows, cols);
                } else if (outDim.length == 1)
                    gradXArray = new double[][]{flatGradX};
                else
                    throw new RuntimeException("Invalid gradient output shape: " + Arrays.toString(outDim));

                gradsDict.put("x", gradXArray);

                NDArray grad_ea = null;
                modelInterpreter.exec("grad_ea_pyGNNRBN = grad[1] if grad[1] is not None else None");
                grad_ea = (NDArray) modelInterpreter.getValue("grad_ea_pyGNNRBN");
                if (grad_ea != null) {
                    raw = grad_ea.getData();
                    double[] flatGradEA = PyUtils.toDoubleArray(raw);
                    outDim = grad_ea.getDimensions();
                    double[][] gradEAArray;
                    if (outDim.length == 2) {
                        int rows = grad_ea.getDimensions()[0];
                        int cols = grad_ea.getDimensions()[1];
                        gradEAArray = PyUtils.convertTo2D(flatGradEA, rows, cols);
                    } else if (outDim.length == 1)
                        gradEAArray = new double[][]{flatGradEA};
                    else
                        throw new RuntimeException("Invalid edge attr gradient shape: " + Arrays.toString(outDim));
                    gradsDict.put("ea", gradEAArray);
                }
                result[1] = gradsDict;
            } else if (withgradients && xDict.size() > 1) {
                throw new RuntimeException("Gradients for heterogeneous GNNs not yet implemented");
            } else
                result[1] = null;

            return result;
        } catch (JepException e) {
            System.err.println("Failed forward pass: " + e);
            return null;
        }
    }

    public SharedInterpreter getModelInterpreter() {
        return modelInterpreter;
    }


    private void printPython(Interpreter interpreter, String var) {
        StringWriter output = new StringWriter();
        interpreter.set("output", output);
        interpreter.eval("import sys");
        interpreter.eval("sys.stdout = output");
        interpreter.eval("print(" + var + ")");
        System.out.println("Captured output: " + output.toString());
    }

    @Override
    public String toString() {
        return "TorchModelWrapper{" +
                "pyModel=" + modelName +
                ", class=" + modelClassName +
                ", interpreter=" + modelInterpreter +
                '}';
    }
}
