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
    }

    public SharedInterpreter getModelInterpreter() {
        return modelInterpreter;
    }

    public double[][] forward(SharedInterpreter interpreter, Map<String, double[][]> xDict, Map<String, ArrayList<ArrayList<Integer>>> edgeDict, List<TorchInputSpecs> gnnInputs) {
        try {
            if (interpreter == null) {
                throw new IllegalStateException("Interpreter null");
            }
            interpreter.set("java_map_x", xDict);
            interpreter.set("java_map_edge", edgeDict);

            Dictionary<String, String[]> edgeRels = new Hashtable<>();
            for (TorchInputSpecs input: gnnInputs) {
                edgeRels.put(
                        input.getEdgeRelation().name(),
                        new String[]{input.getEdgeRelation().getTypes()[0].getName(), input.getEdgeRelation().getTypes()[1].getName()}
                );
            }
            interpreter.set("java_edge_rels", edgeRels);

            StringBuilder sb = new StringBuilder();
            // if size is equal to 1, it is a "normal" gnn, else Heterogeneous
            if (xDict.size() == 1) {
                String keyX = xDict.entrySet().iterator().next().getKey(); // in this case the dictionary should have only one key
                String keyEdge = edgeDict.entrySet().iterator().next().getKey(); // in this case the dictionary should have only one key

                sb.append("xi = torch.as_tensor(java_map_x['" + keyX + "'], dtype=torch.float32)\n");  // TODO maybe this key can be more general (like take just the first element in the dict)
                if (!edgeDict.isEmpty())
                    sb.append("ei = torch.as_tensor(java_map_edge['" + keyEdge + "'], dtype=torch.long)\n");
                else
                    sb.append("ei = torch.empty((2, 0), dtype=torch.long)\n");
                sb.append(modelName + ".eval()\n");
                sb.append("with torch.no_grad(): out = " + modelName + "(xi, ei)\n");
            } else {
                sb.append(
                    "data_h = HeteroData()\n" +

                    "for key, value in java_map_x.items():\n" +
                    "    data_h[key].x = torch.as_tensor(value, dtype=torch.float32)\n" +

                    "for key, value in java_map_edge.items():\n" +
                    "    if len(value) > 0:\n" +
                    "        data_h[java_edge_rels[key][0], key, java_edge_rels[key][1]].edge_index = torch.as_tensor(value, dtype=torch.long)\n" +
                    "    else:\n" +
                    "        data_h[java_edge_rels[key][0], key, java_edge_rels[key][1]].edge_index = torch.empty((2, 0), dtype=torch.long)\n"
                );
                sb.append(modelName + ".eval()\n");
                sb.append("with torch.no_grad(): out = " + modelName + "(data_h.x_dict, data_h.edge_index_dict)\n");
            }

            // Convert PyTorch tensor to NumPy array
            sb.append("out = out.detach().numpy()\n");
            interpreter.exec(sb.toString());
            NDArray ndArray = (NDArray) interpreter.getValue("out");
            float[] tarr = (float[]) ndArray.getData();
            return PyUtils.convertTo2D(tarr, ndArray.getDimensions()[0], ndArray.getDimensions()[1]);
        } catch (JepException e) {
            System.err.println("Failed to forward pass: " + e);
            return null;
        }
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
