package PyManager;
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
                
                def forward_single_primula_(model, x_dict, edge_dict, edge_attr=None):
                    model_dtype, device = _get_type_and_device(model)
                
                    xi = torch.as_tensor(list(x_dict.values())[0], dtype=model_dtype, device=device)
                    if edge_dict:
                        ei = torch.as_tensor(list(edge_dict.values())[0], dtype=torch.long, device=device)
                    else:
                        ei = torch.empty((2, 0), dtype=torch.long, device=device)
                    if edge_attr is not None:
                        ea = torch.as_tensor(list(edge_attr.values())[0], dtype=model_dtype, device=device)
                    model.eval()
                    with torch.no_grad():
                        if edge_attr is not None:
                            out = model(xi, ei, ea)
                        else:
                            out = model(xi, ei)
                    return out.detach().numpy()
                
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
                    return out.detach().numpy()
                """);
            // TODO implement forward_hetero_primula_ for edge_attr support
        } catch (JepException e) {
            throw new RuntimeException("Failed to initialize Python environment", e);
        }
    }

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

            if (withgradients) {
                modelInterpreter.exec("x_dict_pyGNNRBN = x_pyGNNRBN.clone().detach().requires_grad_(True)");
                if (edge_attr.size() > 0)
                    modelInterpreter.exec("edge_attr_pyGNNRBN = edge_attr_pyGNNRBN.clone().detach().requires_grad_(True)");
            }

            if (xDict.size() == 1) {
                if (edge_attr.size() > 0)
                    modelInterpreter.exec("out_pyGNNRBN = forward_single_primula_(model=" + modelName + ", x_dict=x_pyGNNRBN, edge_dict=edge_pyGNNRBN, edge_attr=edge_attr_pyGNNRBN)");
                else
                    modelInterpreter.exec("out_pyGNNRBN = forward_single_primula_(model=" + modelName + ", x_dict=x_pyGNNRBN, edge_dict=edge_pyGNNRBN)");
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

            NDArray outArray = (NDArray) modelInterpreter.getValue("out_pyGNNRBN");
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
            if (withgradients) {
                modelInterpreter.exec("out_pyGNNRBN.backward()");
                modelInterpreter.exec("grad_x_pyGNNRBN = x_pyGNNRBN.grad.cpu().numpy()");
                grad_x = (NDArray) modelInterpreter.getValue("grad_x_pyGNNRBN");

                raw = grad_x.getData();
                double[] flatGradX = PyUtils.toDoubleArray(raw);
                outDim = grad_x.getDimensions();
                if (outDim.length == 2) {
                    int rows = grad_x.getDimensions()[0];
                    int cols = grad_x.getDimensions()[1];
                    result[1] = PyUtils.convertTo2D(flatGradX, rows, cols);
                } else if (outDim.length == 1) {
                    result[1] = new double[][]{flatGradX};
                } else {
                    throw new RuntimeException("Invalid gradient output shape: " + Arrays.toString(outDim));
                }
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
