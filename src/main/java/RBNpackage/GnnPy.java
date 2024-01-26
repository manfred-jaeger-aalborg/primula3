/**
 * This class should be used as an interface between PyTorch models and Java
 */
package RBNpackage;
import jep.*;
import jep.python.PyObject;

import java.util.Objects;

public class GnnPy {
    private String modelPath;
    private String scriptPath;
    private String scriptName;
    private Jep sharedInterpreter;

    // those 4 next vairables are used to save the current query
    private String currentX;

    private String currentEdgeIndex;

    private String currentMethod;

    private float[] currentResult;

    public GnnPy(String modelPath, String scriptPath, String scriptName, String jepPath, String pythonHome) {
        initJep(modelPath, scriptPath, scriptName, jepPath, pythonHome);
    }

    private void initJep(String modelPath, String scriptPath, String scriptName, String jepPath, String pythonHome) {
        this.modelPath = "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python/primula-gnn";
        this.scriptPath = "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python";
        this.scriptName = "inference_test";

        // pip install jep in a miniconda env (torch)
        // TODO: can I put this libjep.jnlib in Primula?
        MainInterpreter.setJepLibraryPath("/Users/lz50rg/miniconda3/envs/torch/lib/python3.10/site-packages/jep/libjep.jnilib");
        PyConfig pyConfig = new PyConfig();
        pyConfig.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");

//        jep.JepConfig jepConf = new JepConfig();
//        jepConf.addSharedModules("numpy"); // this helps for the warning (https://github.com/ninia/jep/issues/418#issuecomment-1165062651)

        initializeInterpreter();
    }

    private void initializeInterpreter() {
        try {
            this.sharedInterpreter = new SharedInterpreter();
            this.sharedInterpreter.exec("import sys");
            this.sharedInterpreter.exec("import torch");
            this.sharedInterpreter.exec("sys.path.append('" + this.modelPath + "')");
            this.sharedInterpreter.exec("sys.path.append('" + this.scriptPath + "')");
            this.sharedInterpreter.exec("import " + this.scriptName + " as intt");
            this.sharedInterpreter.exec("model = intt.set_model_node()");
        } catch (JepException e) {
            System.err.println("Failed to initialize interpreter: " + e);
        }
    }

    public void closeInterpreter() {
        if (sharedInterpreter != null) {
            sharedInterpreter.close();
            sharedInterpreter = null;
            System.out.println("Interpreter closed");
        }
    }
    public Object inferModelNodeObject(String x, String edge_index, String method) {
        assert this.sharedInterpreter != null;
        try {
            this.sharedInterpreter.eval(edge_index);
            this.sharedInterpreter.eval(x);
            if (method != null) {
                this.sharedInterpreter.eval("out = intt."+ method +"(model, x, edge_index, None)");
            } else {
                this.sharedInterpreter.eval("out = intt.infer_model_nodes(model, x, edge_index, None)");
            }
            return this.sharedInterpreter.getValue("out");
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return "None";
        }
    }

    public double inferModelNodeDouble(int node, String x, String edge_index, String method) {
        assert this.sharedInterpreter != null;
        try {
            // check if there is already computed the results for the specific node in the result matrix, otherwise compute for all
            if (this.currentX != null && this.currentEdgeIndex != null && this.currentMethod != null && this.currentResult != null) {
                if (this.currentX.equals(x) && this.currentEdgeIndex.equals(edge_index) && this.currentMethod.equals(method)) {
                    return (double) this.currentResult[node];
                } else {
                    this.currentResult = null;
                }
            }
            this.currentX = x;
            this.currentEdgeIndex = edge_index;
            this.currentMethod = method;
            this.sharedInterpreter.eval(edge_index);
            this.sharedInterpreter.eval(x);
            if (!Objects.equals(method, "")) {
                this.sharedInterpreter.eval("out = intt." + method + "(model, x, edge_index, None)");
            } else {
                this.sharedInterpreter.eval("out = intt.infer_model_nodes(model, x, edge_index, None)");
            }
            this.sharedInterpreter.eval("out = out.detach().numpy().flatten()");
            NDArray ndArray = (NDArray) this.sharedInterpreter.getValue("out");
            this.currentResult = (float[]) ndArray.getData();
            return (double) this.currentResult[node];
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return -1;
        }
    }

    public double[] getData(PyObject out){
        assert this.sharedInterpreter != null;
        try {
            this.sharedInterpreter.set("out_np", out);
            this.sharedInterpreter.exec("torch.tensor(X_before, dtype=torch.float32");
            this.sharedInterpreter.exec("out_np_np = np.array(out_np)");
            // Retrieve the numerical values directly as a Java array
            return (double[]) this.sharedInterpreter.getValue("out_np_np");
        } catch (JepException e) {
            System.err.println("Failed to getData: " + e);
            return null;
        }
    }

}


