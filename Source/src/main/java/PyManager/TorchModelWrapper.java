package PyManager;
import RBNpackage.ProbForm;
import jep.*;

import java.io.StringWriter;
import java.util.*;

public class TorchModelWrapper {
    private final String modelName;
    private final SharedInterpreter modelInterpreter;
    private final String modelClassName;

    // Cache Python function references to avoid string execution overhead
    private Object forwardSingleFunc;
    private Object forwardHeteroFunc;
    private Object modelRef;

    public TorchModelWrapper(String modelName, String modelClassName, SharedInterpreter interpreter) {
        this.modelName = modelName;
        this.modelInterpreter = interpreter;
        this.modelClassName = modelClassName;

        initializePythonEnvironment();
        cachePythonReferences();
    }

    private void initializePythonEnvironment() {
        try {
            modelInterpreter.exec("import torch");
            modelInterpreter.exec("from torch_geometric.data import HeteroData");

            // Optimized Python functions with minimal overhead
            modelInterpreter.exec("""
                def _get_type_and_device(model):
                    param = next(model.parameters(), None)
                    if param is None:
                        buf = next(model.buffers(), None)
                        if buf is None:
                            return torch.get_default_dtype(), torch.device("cpu")
                        return buf.dtype, buf.device
                    return param.dtype, param.device
                
                def _to_tensor(x, dtype, device, requires_grad=False):
                    if isinstance(x, torch.Tensor):
                        t = x.to(device=device, dtype=dtype)
                    else:
                        t = torch.as_tensor(x, dtype=dtype, device=device)
                    t = t.clone().detach()
                    if requires_grad:
                        t.requires_grad_(True)
                    return t
                    
                def forward_single_primula_(model, x_dict, edge_dict, with_gradients, edge_attr):
                    model_dtype, device = _get_type_and_device(model)
                    
                    # Direct extraction without iterator overhead
                    x_items = list(x_dict.items())
                    edge_items = list(edge_dict.items())
                    
                    x_val = x_items[0][1]
                    e_val = edge_items[0][1] if edge_items and len(edge_items[0][1]) > 0 else None
                    ea_val = list(edge_attr.items())[0][1] if edge_attr else None
                
                    xi = _to_tensor(x_val, dtype=model_dtype, device=device, requires_grad=with_gradients)
                    
                    if e_val is not None:
                        ei = torch.as_tensor(e_val, dtype=torch.long, device=device)
                    else:
                        ei = torch.empty((2, 0), dtype=torch.long, device=device)
                
                    ea = _to_tensor(ea_val, dtype=model_dtype, device=device, requires_grad=with_gradients) if ea_val is not None else None
                
                    if with_gradients:
                        if ea is not None:
                            out = model(xi, ei, ea)
                        else:
                            out = model(xi, ei)
                        
                        out_scalar = out.sum()
                        out_scalar.backward()
                        
                        x_grad = xi.grad.detach().cpu().numpy() if xi.grad is not None else None
                        ea_grad = ea.grad.detach().cpu().numpy() if (ea is not None and ea.grad is not None) else None
                        
                        # Return tuple: (output_numpy, x_grad, ea_grad)
                        return out.detach().cpu().numpy(), x_grad, ea_grad
                    else:
                        model.eval()
                        with torch.no_grad():
                            out = model(xi, ei, ea) if ea is not None else model(xi, ei)
                        return out.cpu().numpy(), None, None
                
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
                    return out.cpu().numpy()
                """);
        } catch (JepException e) {
            throw new RuntimeException("Failed to initialize Python environment", e);
        }
    }

    private void cachePythonReferences() {
        try {
            // Cache function and model references to avoid repeated lookups
            forwardSingleFunc = modelInterpreter.getValue("forward_single_primula_");
            forwardHeteroFunc = modelInterpreter.getValue("forward_hetero_primula_");
            modelRef = modelInterpreter.getValue(modelName);
        } catch (JepException e) {
            throw new RuntimeException("Failed to cache Python references", e);
        }
    }

    public Object[] forward(Map<String, double[][]> xDict,
                            Map<String, ArrayList<ArrayList<Integer>>> edgeDict,
                            Map<String, double[][]> edge_attr,
                            List<TorchInputSpecs> gnnInputs,
                            boolean withgradients) {
        Object[] result = new Object[2];

        try {
            if (xDict.size() == 1) {
                Object pythonResult = modelInterpreter.invoke(
                        "forward_single_primula_",
                        modelRef,
                        xDict,
                        edgeDict,
                        withgradients,
                        edge_attr.isEmpty() ? null : edge_attr
                );

                @SuppressWarnings("unchecked")
                List<Object> pyTuple = (List<Object>) pythonResult;
                NDArray outArray = (NDArray) pyTuple.get(0);

                result[0] = convertNDArrayTo2D(outArray);

                // Handle binary output correction if needed
                correctBinaryOutput(result[0], outArray.getDimensions());

                // Handle gradients
                if (withgradients) {
                    Map<String, double[][]> gradsDict = new HashMap<>();

                    if (pyTuple.get(1) != null) {
                        NDArray gradX = (NDArray) pyTuple.get(1);
                        gradsDict.put("x", convertNDArrayTo2D(gradX));
                    }

                    if (pyTuple.get(2) != null) {
                        NDArray gradEA = (NDArray) pyTuple.get(2);
                        gradsDict.put("ea", convertNDArrayTo2D(gradEA));
                    }

                    result[1] = gradsDict.isEmpty() ? null : gradsDict;
                } else {
                    result[1] = null;
                }

            } else {
                // Heterogeneous GNN
                if (!edge_attr.isEmpty()) {
                    throw new RuntimeException("Edge attributes not yet implemented for heterogeneous GNNs");
                }

                if (withgradients) {
                    throw new RuntimeException("Gradients for heterogeneous GNNs not yet implemented");
                }

                // Build edge relation dictionary once
                Map<String, String[]> edgeRels = buildEdgeRelations(gnnInputs);

                Object pythonResult = modelInterpreter.invoke(
                        "forward_hetero_primula_",
                        modelRef,
                        xDict,
                        edgeDict,
                        edgeRels
                );

                NDArray outArray = (NDArray) pythonResult;
                result[0] = convertNDArrayTo2D(outArray);
                result[1] = null;
            }

            return result;

        } catch (JepException e) {
            System.err.println("Failed forward pass: " + e);
            return null;
        }
    }

    // Helper method to convert NDArray to 2D double array
    private double[][] convertNDArrayTo2D(NDArray array) {
        Object raw = array.getData();
        double[] flatData = PyUtils.toDoubleArray(raw);
        int[] dims = array.getDimensions();

        if (dims.length == 2) {
            return PyUtils.convertTo2D(flatData, dims[0], dims[1]);
        } else if (dims.length == 1) {
            return new double[][]{flatData};
        } else {
            throw new RuntimeException("Invalid output shape: " + Arrays.toString(dims));
        }
    }

    // Optimized binary output correction
    private void correctBinaryOutput(Object outputObj, int[] dims) {
        if (dims.length == 2 && dims[0] == 1 && dims[1] == 2) {
            double[][] output = (double[][]) outputObj;
            double val0 = output[0][0];
            double val1 = output[0][1];

            if (val0 == 1.0 || val0 == 0.0) {
                output[0][0] = 1.0 - val1;
            }
            if (val1 == 1.0 || val1 == 0.0) {
                output[0][1] = 1.0 - val0;
            }
        }
    }

    // Build edge relations map
    private Map<String, String[]> buildEdgeRelations(List<TorchInputSpecs> gnnInputs) {
        Map<String, String[]> edgeRels = new HashMap<>();
        for (TorchInputSpecs input : gnnInputs) {
            edgeRels.put(
                    input.getEdgeRelation().name(),
                    new String[]{
                            input.getEdgeRelation().getTypes()[0].getName(),
                            input.getEdgeRelation().getTypes()[1].getName()
                    }
            );
        }
        return edgeRels;
    }

    public SharedInterpreter getModelInterpreter() {
        return modelInterpreter;
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