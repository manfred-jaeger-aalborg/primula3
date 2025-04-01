package PyManager;
import jep.*;

import java.util.ArrayList;
import java.util.Map;

public class TorchModelWrapper {
    private final String modelName;
    private final int numLayers;
    private final SharedInterpreter modelInterpreter;
    private final String modelClassName;

    public TorchModelWrapper(String modelName, int numLayers, String modelClassName, SharedInterpreter interpreter) {
        this.modelName = modelName;
        this.numLayers = numLayers;
        this.modelInterpreter = interpreter;
        this.modelClassName = modelClassName;
    }

    public int getNumLayers() {
        return numLayers;
    }

    public SharedInterpreter getModelInterpreter() {
        return modelInterpreter;
    }

    public double[][] forward(SharedInterpreter interpreter, Map<String, double[][]> xDict, Map<String, ArrayList<ArrayList<Integer>>> edgeDict) {
        try {
            if (interpreter == null) {
                throw new IllegalStateException("Interpreter null");
            }
            interpreter.set("java_map_x", xDict);
            interpreter.set("java_map_edge", edgeDict);

            if (xDict.size() == 1) {
                String keyX = xDict.entrySet().iterator().next().getKey(); // in this case the dictionary should have only one key
                interpreter.exec("xi = torch.as_tensor(java_map_x['" + keyX + "'], dtype=torch.float32)"); // TODO maybe this key can be more general (like take just the first element in the dict)
                if (!edgeDict.isEmpty())
                    interpreter.exec("ei = torch.as_tensor(java_map_edge['edge'], dtype=torch.long)");
                else
                    interpreter.exec("ei = torch.empty((2, 0), dtype=torch.long)");
                interpreter.exec(modelName + ".eval()");
                interpreter.exec("with torch.no_grad(): out = " + modelName + "(xi, ei)");
            } else {
                interpreter.exec(
                        "data_h = HeteroData()\n" +

                                "for key, value in java_map_x.items():\n" +
                                "    data_h[key].x = torch.as_tensor(value, dtype=torch.float32)\n" +

                                "for key, value in java_map_edge.items():\n" +
                                "    n_key = key.split('_to_')\n" + // here the key must have the form type_to_type
                                "    if len(value) > 0:\n" +
                                "        data_h[n_key[0], 'to', n_key[1]].edge_index = torch.as_tensor(value, dtype=torch.long)\n" +
                                "    else:\n" +
                                "        data_h[n_key[0], 'to', n_key[1]].edge_index = torch.empty((2, 0), dtype=torch.long)\n"
                );
                interpreter.exec(modelName + ".eval()");
                interpreter.exec("with torch.no_grad(): out = " + modelName + "(data_h.x_dict, data_h.edge_index_dict)");
            }

            // Convert PyTorch tensor to NumPy array
            interpreter.exec("out = out.detach().numpy()");
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
                ", numLayers=" + numLayers +
                ", interpreter=" + modelInterpreter +
                '}';
    }
}
