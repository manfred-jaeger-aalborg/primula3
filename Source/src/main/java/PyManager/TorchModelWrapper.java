package PyManager;
import jep.*;
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
                def forward_single_primula_(x_dict, edge_dict, model):
                    xi = torch.as_tensor(list(x_dict.values())[0], dtype=torch.float32)
                    if edge_dict:
                        ei = torch.as_tensor(list(edge_dict.values())[0], dtype=torch.long)
                    else:
                        ei = torch.empty((2, 0), dtype=torch.long)
                    model.eval()
                    with torch.no_grad():
                        out = model(xi, ei)
                    return out.detach().numpy()
                
                def forward_hetero_primula_(x_dict, edge_dict, edge_rels, model):
                    data_h = HeteroData()
                    for key, value in x_dict.items():
                        data_h[key].x = torch.as_tensor(value, dtype=torch.float32)
                    for key, value in edge_dict.items():
                        edge_type = (edge_rels[key][0], key, edge_rels[key][1])
                        if value:
                            data_h[edge_type].edge_index = torch.as_tensor(value, dtype=torch.long)
                        else:
                            data_h[edge_type].edge_index = torch.empty((2, 0), dtype=torch.long)
                    model.eval()
                    with torch.no_grad():
                        out = model(data_h.x_dict, data_h.edge_index_dict)
                    return out.detach().numpy()
                """);
        } catch (JepException e) {
            throw new RuntimeException("Failed to initialize Python environment", e);
        }
    }

    public Object[] forward(Map<String, double[][]> xDict,
                              Map<String, ArrayList<ArrayList<Integer>>> edgeDict,
                              List<TorchInputSpecs> gnnInputs,
                              boolean withgradients) {
        // we use the same convention:
        // result[0] is probabilities and result[1] gradients
        Object[] result = new Object[2];
        try {
            // Set input data
            modelInterpreter.set("x_dict", xDict);
            modelInterpreter.set("edge_dict", edgeDict);

            if (withgradients)
                modelInterpreter.exec("x_dict = x_dict.clone().detach().requires_grad_(True)");

            if (xDict.size() == 1) {
                modelInterpreter.exec("out = forward_single_primula_(x_dict, edge_dict, " + modelName + ")");
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
                modelInterpreter.set("edge_rels", edgeRels);
                modelInterpreter.exec("out = forward_hetero_primula_(x_dict, edge_dict, edge_rels, " + modelName + ")");
            }

            NDArray outArray = (NDArray) modelInterpreter.getValue("out");
            float[] temp = (float[]) outArray.getData();
            // to keep some consistency, we convert the float data to double
            double[] flatData = new double[temp.length];
            for (int i = 0; i < temp.length; i++)
                flatData[i] = temp[i];

            int rows = outArray.getDimensions()[0];
            int cols = outArray.getDimensions()[1];
            result[0] = PyUtils.convertTo2D(flatData, rows, cols);

            NDArray grad_x = null;
            if (withgradients) {
                modelInterpreter.exec("out.backward()");
                modelInterpreter.exec("grad_x = x_dict.grad.cpu().numpy()");
                grad_x = (NDArray) modelInterpreter.getValue("grad_x");

                temp = (float[]) grad_x.getData();
                double[] flatGradX = new double[temp.length];
                for (int i = 0; i < temp.length; i++)
                    flatGradX[i] = temp[i];

                result[1] = PyUtils.convertTo2D(flatGradX, rows, cols);
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

    @Override
    public String toString() {
        return "TorchModelWrapper{" +
                "pyModel=" + modelName +
                ", class=" + modelClassName +
                ", interpreter=" + modelInterpreter +
                '}';
    }
}
