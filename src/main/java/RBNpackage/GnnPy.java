/**
 * This class should be used as an interface between PyTorch models and Java
 */
package RBNpackage;
import jep.*;

public class GnnPy {
    private String modelPath;
    private String scriptPath;
    private String scriptName;
    private static Jep sharedInterpreter;

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

        jep.JepConfig jepConf = new JepConfig();
        jepConf.addSharedModules("numpy"); // this helps for the warning (https://github.com/ninia/jep/issues/418#issuecomment-1165062651)

        initializeInterpreter();
    }

    private void initializeInterpreter() {
        try {
            sharedInterpreter = new SharedInterpreter();
            sharedInterpreter.exec("import sys");
            sharedInterpreter.exec("import torch");
            sharedInterpreter.exec("sys.path.append('" + this.modelPath + "')");
            sharedInterpreter.exec("sys.path.append('" + this.scriptPath + "')");
            sharedInterpreter.exec("import " + this.scriptName + " as intt");
        } catch (JepException e) {
            System.err.println("Failed to initialize interpreter: " + e);
        }
    }

    private void closeInterpreter() {
        if (sharedInterpreter != null) {
            sharedInterpreter.close();
            sharedInterpreter = null;
        }
    }

    public Object inferModelNode(String x, String edge_index, String method) {
        assert sharedInterpreter != null;
        try {
            sharedInterpreter.eval(edge_index);
            sharedInterpreter.eval(x);
            if (method != null) {
                sharedInterpreter.eval("out = intt."+ method +"(x, edge_index, None)");
            } else {
                sharedInterpreter.eval("out = intt.infer_model_nodes(x, edge_index, None)");
            }
            return sharedInterpreter.getValue("out");
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return "None";
        }
    }

}


