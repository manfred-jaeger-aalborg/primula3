/**
 * This class should be used as an interface between PyTorch models and Java
 */
package RBNpackage;
import jep.*;
import jep.python.PyObject;

import java.io.IOException;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GnnPy {
    private final String INFER_NODE = "infer_model_nodes"; // default function name to call in python
    private final String INFER_GRAPH = "infer_model_graph";
    private String modelPath;
    private String scriptPath;
    private String scriptName;
    private Jep sharedInterpreter;

    // those 4 next vairables are used to save the current query
    private String currentX;

    private String currentEdgeIndex;

    private String currentMethod;

    private float[] currentResult;

    public GnnPy(String modelPath, String scriptPath, String scriptName, String pythonHome) throws IOException {
        initJep(modelPath, scriptPath, scriptName, pythonHome);
    }

    public GnnPy() {}

    public String loadjep(String pythonHome) throws IOException {
        // taken from https://gist.github.com/vwxyzjn/c054bae6dfa6f80e6c663df70347e238
        // automatically find the library path in the python home it is installed
        Process p = Runtime.getRuntime().exec(pythonHome + " python/get_jep_path.py");
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        return in.readLine();

    }
    private void initJep(String modelPath, String scriptPath, String scriptName, String pythonHome) throws IOException {
        this.modelPath = modelPath;
        this.scriptPath = scriptPath;
        this.scriptName = scriptName;

        // pip install jep in a miniconda env (torch)
        MainInterpreter.setJepLibraryPath(loadjep(pythonHome));

//        MainInterpreter.setJepLibraryPath("/Users/lz50rg/miniconda3/envs/torch/lib/python3.10/site-packages/jep/libjep.jnilib");
//        PyConfig pyConfig = new PyConfig();
//        pyConfig.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");

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
            this.sharedInterpreter.exec("model = intt.set_model_graph()");
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
            // check if there is already computed the results for the specific node in the result matrix, otherwise compute for all nodes with one forward propagation
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
                this.sharedInterpreter.eval("out = intt." + this.INFER_NODE + "(model, x, edge_index, None)");
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

    public double inferModelGraphDouble(int classId, String x, String edge_index, String method) {
        assert this.sharedInterpreter != null;
        try {
            // check if there is already computed the results for the specific node in the result matrix, otherwise compute for all nodes with one forward propagation
//            if (this.currentX != null && this.currentEdgeIndex != null && this.currentMethod != null && this.currentResult != null) {
//                if (this.currentX.equals(x) && this.currentEdgeIndex.equals(edge_index) && this.currentMethod.equals(method)) {
//                    return (double) this.currentResult[classId];
//                } else {
//                    this.currentResult = null;
//                }
//            }
//            this.currentX = x;
//            this.currentEdgeIndex = edge_index;
//            this.currentMethod = method;
            // if there are no edges
            if (Objects.equals(edge_index, "")) {
                this.sharedInterpreter.eval("a = torch.tensor([[]], dtype=torch.long)");
                this.sharedInterpreter.eval("b = torch.tensor([[]], dtype=torch.long)");
                this.sharedInterpreter.eval("edge_index = torch.cat((a,b), 0)");
            } else
                this.sharedInterpreter.eval(edge_index);
            this.sharedInterpreter.eval(x);
            if (!Objects.equals(method, "")) {
                this.sharedInterpreter.eval("out = intt." + method + "(model, x, edge_index, None)");
            } else {
                this.sharedInterpreter.eval("out = intt." + this.INFER_GRAPH + "(model, x, edge_index, None)");
            }
            this.sharedInterpreter.eval("out = out.detach().numpy().flatten()");
            NDArray ndArray = (NDArray) this.sharedInterpreter.getValue("out");
            this.currentResult = (float[]) ndArray.getData();
            return (double) this.currentResult[classId];
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

    private int[][] createBoolEncodingMatrix(int num_nodes, int num_columns) {
        int[][] node_bool = new int[num_nodes][num_columns * 2];
        // initialize with all false (i.e. [0,1])
        for (int i = 0; i < node_bool.length; i++) {
            for (int j = 0; j < node_bool[i].length; j += 2) {
                node_bool[i][j] = 0;
                node_bool[i][j + 1] = 1;
            }
        }
        return node_bool;
    }

    private int[][] createOneHotEncodingMatrix(int num_nodes, int num_columns) {
        int[][] node_bool = new int[num_nodes][num_columns];
        // initialize with all false (i.e. [0])
        for (int[] ints : node_bool) Arrays.fill(ints, 0);
        return node_bool;
    }

    /**
     * Write the structure in a graph form for the GNN input using the attributes that will be
     * used in the features matrix for the input
     * @param num_nodes
     * @param finalre
     * @param attributes
     * @return
     */
    public int[][] nodeToEncoding(int num_nodes, SparseRelStruc finalre, Rel[] attributes, boolean oneHotEncoding) {
        // table for the encoding of the node features, each feature is represented in a boolean encoding
        // 1 0 true - 0 1 false
        // example: 2 possible features blue and red. the node is red: 0 1 1 0

        OneStrucData data = finalre.getmydata();
        // create the feature matrix with the attributes that have arity = 1
        int num_columns = 0;
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].arity == 1) {
                num_columns++;
            }
        }

        int[][] node_bool;
        if (oneHotEncoding)
            node_bool = createOneHotEncodingMatrix(num_nodes, num_columns);
        else
            node_bool = createBoolEncodingMatrix(num_nodes, num_columns); // we should put different types of encoding, for now leave like that

        int idxFeat = 0;
        for (Rel parent : attributes) {
            if (parent.arity == 1) { // only nodes
                Vector<int[]> featureTrueData = data.allTrue(parent);
                Vector<Vector<int[]>> allTrueData = new Vector<>();

                allTrueData.add(featureTrueData);

                // change the true value
                for (Vector<int[]> feature : allTrueData) {
                    for (int[] node : feature) {
                        node_bool[node[0]][idxFeat] = 1;
                        if (!oneHotEncoding) // same, as in the creation of node_bool
                            node_bool[node[0]][idxFeat + 1] = 0;
                    }
                    idxFeat++;
                }
            }
        }
        return node_bool;
    }

    public String stringifyGnnFeatures(int num_nodes, SparseRelStruc finalre, Rel[] attributes, boolean oneHotEncoding) {
        int[][] bool_nodes = this.nodeToEncoding(num_nodes, finalre, attributes, oneHotEncoding);
        StringBuilder node_features = new StringBuilder("x=torch.tensor([");
        for (int i = 0; i < bool_nodes.length; i++) {
            node_features.append("[");
            for (int j = 0; j < bool_nodes[i].length; j++) {
                node_features.append(bool_nodes[i][j]);
                node_features.append(".");
                if (j < bool_nodes[i].length - 1) {
                    node_features.append(",");
                }
            }
            node_features.append("]");
            if (i < bool_nodes.length - 1) {
                node_features.append(",");
            }
        }
        node_features.append("])");
        return node_features.toString();

    }

    public String arraysToEdgeTensor(int[][] edges) {
        StringBuilder result = new StringBuilder();
        result.append("edge_index=torch.tensor([[");
        for (int i = 0; i < edges.length; i++) {
            result.append(edges[i][0]);
            if (i < edges.length - 1) {
                result.append(",");
            }
        }
        result.append("],[");
        for (int i = 0; i < edges.length; i++) {
            result.append(edges[i][1]);
            if (i < edges.length - 1) {
                result.append(",");
            }
        }
        result.append("]])");
        return result.toString();
    }

    /**
     * This function will write the edges in both directions
     * A -> B and B -> A
     * @param sampledRel
     * @param edgerel
     * @return
     */
    public String stringifyGnnEdgesABBA(SparseRelStruc sampledRel, BoolRel edgerel) {
        // use the edge relation present in mydata
        OneBoolRelData edgeinst = null;
        String edge_index = "";

        edgeinst = (OneBoolRelData) sampledRel.getmydata().find(edgerel);
        TreeSet<int[]> edges_list = edgeinst.allTrue();
        // in order to transpose the edge matrix we use 2 arrays
        // we are dealing with directed edges while in our PyTorch data is undirected!
        int[][] arrays = new int[edges_list.size()*2][2];
        int idx = 0;
        for (int[] array : edges_list) {
            arrays[idx][0] = array[0];
            arrays[idx][1] = array[1];
            arrays[idx+1][0] = array[1];
            arrays[idx+1][1] = array[0];
            idx += 2;
        }
        // Sort the arrays based on the first column
        Arrays.sort(arrays, Comparator.comparingInt(a -> a[0]));
        return this.arraysToEdgeTensor(arrays);
    }

    /**
     * This function will write the edges in one direction
     * A -> B
     * @param sampledRel
     * @param edgerel
     * @return
     */
    public String stringifyGnnEdgesAB(SparseRelStruc sampledRel, BoolRel edgerel) {
        // use the edge relation present in mydata
        OneBoolRelData edgeinst = null;
        String edge_index = "";

        edgeinst = (OneBoolRelData) sampledRel.getmydata().find(edgerel);
        TreeSet<int[]> edges_list = edgeinst.allTrue();
        // in order to transpose the edge matrix we use 2 arrays
        // we are dealing with directed edges while in our PyTorch data is undirected!
        int[][] arrays = new int[edges_list.size()][2];
        int idx = 0;
        for (int[] array : edges_list) {
            arrays[idx][0] = array[0];
            arrays[idx][1] = array[1];
            idx++;
        }
        // Sort the arrays based on the first column
        Arrays.sort(arrays, Comparator.comparingInt(a -> a[0]));
        return this.arraysToEdgeTensor(arrays);
    }

    /**
     * This function will write the edges in one direction but opposite
     * B -> A
     * @param sampledRel
     * @param edgerel
     * @return
     */
    public String stringifyGnnEdgesBA(SparseRelStruc sampledRel, BoolRel edgerel) {
        // use the edge relation present in mydata
        OneBoolRelData edgeinst = null;
        String edge_index = "";

        edgeinst = (OneBoolRelData) sampledRel.getmydata().find(edgerel);
        TreeSet<int[]> edges_list = edgeinst.allTrue();
        // in order to transpose the edge matrix we use 2 arrays
        // we are dealing with directed edges while in our PyTorch data is undirected!
        int[][] arrays = new int[edges_list.size()][2];
        int idx = 0;
        for (int[] array : edges_list) {
            arrays[idx][0] = array[1];
            arrays[idx][1] = array[0];
            idx++;
        }
        // Sort the arrays based on the first column
        Arrays.sort(arrays, Comparator.comparingInt(a -> a[0]));
        return this.arraysToEdgeTensor(arrays);
    }

}


