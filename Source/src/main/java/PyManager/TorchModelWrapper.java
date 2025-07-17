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

    public double[][] forward(Map<String, double[][]> xDict,
                              Map<String, ArrayList<ArrayList<Integer>>> edgeDict,
                              List<TorchInputSpecs> gnnInputs) {
        try {
            // Set input data
            modelInterpreter.set("x_dict", xDict);
            modelInterpreter.set("edge_dict", edgeDict);

            if (xDict.size() == 1) {
                // Use single GNN logic
                modelInterpreter.exec("out = forward_single_primula_(x_dict, edge_dict, " + modelName + ")");
            } else {
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

            NDArray ndArray = (NDArray) modelInterpreter.getValue("out");
            float[] flatData = (float[]) ndArray.getData();
            int rows = ndArray.getDimensions()[0];
            int cols = ndArray.getDimensions()[1];
            return PyUtils.convertTo2D(flatData, rows, cols);
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
