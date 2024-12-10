/**
 * This class should be used as an interface between PyTorch models and Java
 */
package RBNpackage;
import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.*;
import RBNinference.PFNetworkNode;
import jep.*;
import jep.python.PyObject;

import java.io.IOException;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class GnnPy {

    // We assume that the forward method for the gnn have always firm like this: forward(self, x, edge_index, ...)
    // x and edge_index are necessary, others arguments like batch can be set as None
    private final String INFER_NODE = "infer_model_nodes"; // default function name to call in python
    private final String INFER_GRAPH = "infer_model_graph";
    private String scriptPath;
    private String scriptName;
    private String pythonHome;
    private SharedInterpreter sharedInterpreter;
    private static ThreadLocal<SharedInterpreter> threadSharedInterp = new ThreadLocal<>();

    // those 4 next vairables are used to save the current query
    private String currentXString;
    private double[][] currentX;

    private String currentEdgeIndexString;
    private int[][] currentEdgeIndex;

    private String currentMethod;

    private Double[][] currentResult;

    // Also for heterogeneous
    private Map<String, String> currentXdictString;
    private Map<String, double[][]> currentXdict;
    private Map<String, String> currentEdgeDictString;
    private Map<String, int[][]> currentEdgeDict;

    private String lastId;

    private long dimOut;
    private int numClass;
    private ArrayList<String> gnnModelsId;

    static private OneStrucData GGonsd;
    static private SparseRelStruc GGsampledRel;
    static private Vector<BoolRel> GGboolRel;
    static private String GGxString;
    static private double[][] GGx;
    static private String GGedge_indexString;
    static private int[][] GGedge_index;

    static private Map<String, double[][]> GGxDict;
    static private Map<String, int[][]> GGedgeDict;
    private boolean GGedge_pred;
    static private int GGnumNodes;
    static private Map<Rel, int[][]> GGNodesDict;
    public GnnPy(String scriptPath, String scriptName, String pythonHome) throws IOException {
        this.scriptPath = scriptPath;
        this.scriptName = scriptName;
        this.pythonHome = pythonHome;
        this.gnnModelsId = new ArrayList<>();
        this.numClass = -1;
        this.dimOut = -1;
        currentXdictString = new Hashtable<>();
        currentEdgeDictString = new Hashtable<>();
        currentXdict = new Hashtable<>();
        currentEdgeDict = new Hashtable<>();
        GGNodesDict = new Hashtable<>();

        // pip install jep in a miniconda env (torch)
        initializeJep();
        sharedInterpreter = threadSharedInterp.get();
//        sharedInterpreter.exec("data = HeteroData()");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            handleShutdown();
        }));
    }

    public GnnPy() {
    }

    public String loadjep(String pythonHome) throws IOException {
        // taken from https://gist.github.com/vwxyzjn/c054bae6dfa6f80e6c663df70347e238
        // automatically find the library path in the python home it is installed
        Process p = Runtime.getRuntime().exec(pythonHome + " python/get_jep_path.py");
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        return in.readLine();
    }

    public void initializeJep()  throws IOException {
        if (threadSharedInterp.get() == null) {
            try {
                MainInterpreter.setJepLibraryPath(loadjep(this.pythonHome));
            } catch (IllegalStateException e) {
                System.out.println("Error in loading the JepLibraryPath: " + e);
            }
            initializePythonScript();
        }
    }

    private void initializePythonScript() {
        SharedInterpreter interpreter = new SharedInterpreter();
        interpreter.exec("import sys");
        interpreter.exec("import torch");
        interpreter.exec("import numpy as np");
        interpreter.exec("from torch_geometric.data import Data, HeteroData");
        interpreter.exec("sys.path.append('" + this.scriptPath + "')");
        interpreter.exec("import " + this.scriptName + " as intt");
        threadSharedInterp.set(interpreter);
    }

    public void closeInterpreter() {
        SharedInterpreter interpreter = threadSharedInterp.get();
        if (interpreter != null) {
            interpreter.exec("import gc; gc.collect()");
            interpreter.close();
            threadSharedInterp.remove();
            System.out.println("Interpreter closed for thread " + Thread.currentThread().getName());
        }
    }

    private void handleShutdown() {
        System.out.println("Shutdown hook triggered! Closing interpreter...");
        closeInterpreter();
    }

    public void load_gnn_set(Map<String, Object> sett) {
        SharedInterpreter interpreter = threadSharedInterp.get();
        if (interpreter != null) {
            interpreter.set("gnn_set_dict", sett);
            interpreter.exec("intt.set_vars(gnn_set_dict)");
        }
    }

    /**
     * Checks if the model is already instantiated
     * Each gnn is associated with an id
     * @param id of the model
     */
    private void createModelIfNull(String id) {
        if (this.gnnModelsId != null) {
            if (!this.gnnModelsId.contains(id)) {
                this.sharedInterpreter.exec(id + " = intt.use_model(\"" + id + "\")");
                this.gnnModelsId.add(id);
            }
        } else {
            this.gnnModelsId = new ArrayList<>();
            this.sharedInterpreter.exec(id + " = intt.use_model(\"" + id + "\")");
            this.gnnModelsId.add(id);
        }
    }

    public static Double[][] convertTo2D(float[] inputArray, int rows, int cols) {
        if (inputArray.length != rows * cols) {
            throw new IllegalArgumentException("The length of the input array does not match the provided dimensions.");
        }

        Double[][] outputArray = new Double[rows][cols];

        for (int i = 0; i < inputArray.length; i++) {
            int row = i / cols;
            int col = i % cols;
            outputArray[row][col] = (double) inputArray[i];
        }

        return outputArray;
    }

    public Double[] inferModelNodeDoubleString(int node, String x, String edge_index, String idGnn, String method, boolean boundValue) {
        assert this.sharedInterpreter != null;
        this.createModelIfNull(idGnn);

        try {
            // check if there is already computed the results for the specific node in the result matrix, otherwise compute for all nodes with one forward propagation
            // needs also to have the same id as before
            if (checkValuesCacheString(x, edge_index, idGnn, method)) return currentResult[node];
            this.currentXString = x;
            this.currentEdgeIndexString = edge_index;
            this.currentMethod = method;
            this.lastId = idGnn;
            this.sharedInterpreter.eval(edge_index);
            this.sharedInterpreter.eval(x);

            if (!Objects.equals(method, "")) {
                this.sharedInterpreter.eval("out = intt." + method + "(" + idGnn + ", x, edge_index)");
            } else {
                this.sharedInterpreter.eval("out = intt." + this.INFER_NODE + "(" + idGnn + ", x, edge_index)");
            }
            this.sharedInterpreter.eval("out_size = len(out.shape)");

            if (this.numClass == -1) {
                this.sharedInterpreter.eval("class_num = out.shape[1] if out_size == 2 else 1");
                long nc = (Long) this.sharedInterpreter.getValue("class_num");
                this.numClass = (int) nc;
            }

            this.dimOut = (Long) this.sharedInterpreter.getValue("out_size");
            if (dimOut == 1) {
                System.out.println("not implemented");
//                this.sharedInterpreter.eval("out = out.detach().numpy().flatten()");
//                NDArray ndArray = (NDArray) this.sharedInterpreter.getValue("out");
//                this.currentResult = (float[]) ndArray.getData();
//                return (double) this.currentResult[node];
            } else if (dimOut == 2) {
                this.sharedInterpreter.eval("out = out.detach().numpy()");
                NDArray ndArray = (NDArray) this.sharedInterpreter.getValue("out");
                float[] tarr = (float[]) ndArray.getData();
                currentResult = convertTo2D(tarr, ndArray.getDimensions()[0], ndArray.getDimensions()[1]);
                Double[] catres = currentResult[node];
                return catres;
            }
            // else nothing
            return null;
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    public Double[] inferModelNodeDouble(int node, double[][] x, int[][] edge_index, String idGnn, String method) {
        assert this.sharedInterpreter != null;
        this.createModelIfNull(idGnn);

        try {
            // check if there is already computed the results for the specific node in the result matrix, otherwise compute for all nodes with one forward propagation
            // needs also to have the same id as before
            if (checkValuesCache(x, edge_index, idGnn, method)) return currentResult[node];
            this.currentX = x;
            this.currentEdgeIndex = edge_index;
            this.currentMethod = method;
            this.lastId = idGnn;
            this.sharedInterpreter.set("java_x", x);
            this.sharedInterpreter.set("java_edge", edge_index);
            this.sharedInterpreter.exec("xi = torch.as_tensor(java_x, dtype=torch.float32)");
            if (edge_index.length > 0)
                this.sharedInterpreter.exec("ei = torch.as_tensor(java_edge, dtype=torch.long)");
            else
                this.sharedInterpreter.exec("ei = torch.empty((2, 0), dtype=torch.long)");

            if (!Objects.equals(method, "")) {
                this.sharedInterpreter.eval("out = intt." + method + "(" + idGnn + ", xi, ei)");
            } else {
                this.sharedInterpreter.eval("out = intt." + this.INFER_NODE + "(" + idGnn + ", xi, ei)");
            }
            this.sharedInterpreter.eval("out_size = len(out.shape)");

            if (this.numClass == -1) {
                this.sharedInterpreter.eval("class_num = out.shape[1] if out_size == 2 else 1");
                long nc = (Long) this.sharedInterpreter.getValue("class_num");
                this.numClass = (int) nc;
            }

            this.dimOut = (Long) this.sharedInterpreter.getValue("out_size");
            if (dimOut == 1) {
                System.out.println("not implemented");
            } else if (dimOut == 2) {
                this.sharedInterpreter.eval("out = out.detach().numpy()");
                NDArray ndArray = (NDArray) this.sharedInterpreter.getValue("out");
                float[] tarr = (float[]) ndArray.getData();
                currentResult = convertTo2D(tarr, ndArray.getDimensions()[0], ndArray.getDimensions()[1]);
                Double[] catres = currentResult[node];
                return catres;
            }
            // else nothing
            return null;
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    public Double[] inferModelNodeHeteroDouble(int node, Map<String, double[][]> x_dict, Map<String, int[][]> edge_dict, String idGnn) {
        assert this.sharedInterpreter != null;
        this.createModelIfNull(idGnn);

        try {
            // check if there is already computed the results for the specific node in the result matrix,
            // otherwise compute for all nodes with one forward propagation
            // needs also to have the same id as before

            if (checkValuesHeteroCache(x_dict, edge_dict, idGnn))
                return currentResult[node];
            currentXdict = x_dict;
            currentEdgeDict = edge_dict;
            lastId = idGnn;

            this.sharedInterpreter.set("java_map_x", currentXdict);
            this.sharedInterpreter.set("java_map_edge", currentEdgeDict);

            this.sharedInterpreter.exec(
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

            this.sharedInterpreter.exec("out = intt." + this.INFER_NODE + "(" + idGnn + ", data_h.x_dict, data_h.edge_index_dict)");

            this.sharedInterpreter.exec("out_size = len(out.shape)");

            if (this.numClass == -1) {
                this.sharedInterpreter.exec("class_num = out.shape[1] if out_size == 2 else 1");
                long nc = (Long) this.sharedInterpreter.getValue("class_num");
                this.numClass = (int) nc;
            }


            this.dimOut = (Long) this.sharedInterpreter.getValue("out_size");
            if (dimOut == 1) {
                System.out.println("not implemented");
            } else if (dimOut == 2) {
                this.sharedInterpreter.exec("out = out.detach().numpy()");
                NDArray ndArray = (NDArray) this.sharedInterpreter.getValue("out");
                float[] tarr = (float[]) ndArray.getData();
                currentResult = convertTo2D(tarr, ndArray.getDimensions()[0], ndArray.getDimensions()[1]);

                Double[] catres = currentResult[node];
                return catres;
            }
            return null;
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    public Double[] inferModelNodeHeteroDoubleString(int node, Map<String, String> x_dict_s, Map<String, String> edge_dict_s, String idGnn) {
        assert this.sharedInterpreter != null;
        this.createModelIfNull(idGnn);

        try {
            // check if there is already computed the results for the specific node in the result matrix,
            // otherwise compute for all nodes with one forward propagation
            // needs also to have the same id as before
            if (checkValuesHeteroCacheString(x_dict_s, edge_dict_s, idGnn)) return currentResult[node];
            currentXdictString = x_dict_s;
            currentEdgeDictString = edge_dict_s;
            lastId = idGnn;

            this.sharedInterpreter.eval("data_h = HeteroData()");
            for (String k: x_dict_s.keySet()) {
                this.sharedInterpreter.eval("data_h['"+k+"'].x="+x_dict_s.get(k));
            }
            // the edges dict has to be in the form type1_keyword_type2
            // type1 and type2 are the same as in the rdef file and the same for the x
            // keyword is for now discarded
            for (String k: edge_dict_s.keySet()) {
                String[] edgeDirs = k.split("_", 3);
                this.sharedInterpreter.eval("data_h['"+edgeDirs[0]+"','to','"+edgeDirs[2]+"'].edge_index="+edge_dict_s.get(k));
            }

            this.sharedInterpreter.eval("out = intt." + this.INFER_NODE + "(" + idGnn + ", data_h.x_dict, data_h.edge_index_dict)");

            this.sharedInterpreter.eval("out_size = len(out.shape)");

            if (this.numClass == -1) {
                this.sharedInterpreter.eval("class_num = out.shape[1] if out_size == 2 else 1");
                long nc = (Long) this.sharedInterpreter.getValue("class_num");
                this.numClass = (int) nc;
            }

            this.dimOut = (Long) this.sharedInterpreter.getValue("out_size");
            if (dimOut == 1) {
                System.out.println("not implemented");
            } else if (dimOut == 2) {
                this.sharedInterpreter.eval("out = out.detach().numpy()");
                NDArray ndArray = (NDArray) this.sharedInterpreter.getValue("out");
                float[] tarr = (float[]) ndArray.getData();
                currentResult = convertTo2D(tarr, ndArray.getDimensions()[0], ndArray.getDimensions()[1]);

                Double[] catres = currentResult[node];
                return catres;
            }
            return null;
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    public Double[] inferModelGraphDoubleString(String x, String edge_index, String idGnn, String method) {
        assert this.sharedInterpreter != null;
        this.createModelIfNull(idGnn);

        try {
            if (checkValuesCacheString(x, edge_index, idGnn, method)) return this.currentResult[0];
            this.currentXString = x;
            this.currentEdgeIndexString = edge_index;
            this.currentMethod = method;
            this.lastId = idGnn;
            this.sharedInterpreter.eval(edge_index);
            this.sharedInterpreter.eval(x);
            // if there are no edges we use this trick for pyg
            if (Objects.equals(edge_index, "")) {
                this.sharedInterpreter.eval("edge_index = torch.cat((torch.tensor([[]], dtype=torch.long), torch.tensor([[]], dtype=torch.long)), 0)");
            } else
                this.sharedInterpreter.eval(edge_index);
            this.sharedInterpreter.eval(x);
            if (!Objects.equals(method, "")) {
                this.sharedInterpreter.eval("out = intt." + method + "(" + idGnn + ", x, edge_index)");
            } else {
                this.sharedInterpreter.eval("out = intt." + this.INFER_GRAPH + "(" + idGnn + ", x, edge_index)");
            }

            this.sharedInterpreter.eval("out = out.detach().numpy()");
            NDArray ndArray = (NDArray) this.sharedInterpreter.getValue("out");
            float[] tarr = (float[]) ndArray.getData();
            currentResult = convertTo2D(tarr, ndArray.getDimensions()[0], ndArray.getDimensions()[1]);
//            Double[] catres = currentResult[classId];
//            return catres;
            return currentResult[0];
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    public Double[] inferModelGraphDouble(double[][] x, int[][] edge_index, String idGnn, String method) {
        assert this.sharedInterpreter != null;
        this.createModelIfNull(idGnn);

        try {
            if (checkValuesCache(x, edge_index, idGnn, method)) return this.currentResult[0];
            this.currentX= x;
            this.currentEdgeIndex = edge_index;
            this.currentMethod = method;
            this.lastId = idGnn;
            this.sharedInterpreter.set("java_x", x);
            this.sharedInterpreter.set("java_edge", edge_index);
            this.sharedInterpreter.exec("xi = torch.as_tensor(java_x, dtype=torch.float32)");
            if (edge_index.length > 0)
                this.sharedInterpreter.exec("ei = torch.as_tensor(java_edge, dtype=torch.long)");
            else
                this.sharedInterpreter.exec("ei = torch.empty((2, 0), dtype=torch.long)");

            if (!Objects.equals(method, "")) {
                this.sharedInterpreter.eval("out = intt." + method + "(" + idGnn + ", xi, ei)");
            } else {
                this.sharedInterpreter.eval("out = intt." + this.INFER_GRAPH + "(" + idGnn + ", xi, ei)");
            }

            this.sharedInterpreter.eval("out = out.detach().numpy()");
            NDArray ndArray = (NDArray) this.sharedInterpreter.getValue("out");
            float[] tarr = (float[]) ndArray.getData();
            currentResult = convertTo2D(tarr, ndArray.getDimensions()[0], ndArray.getDimensions()[1]);
            return currentResult[0];
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    private boolean checkValuesCacheString(String x, String edge_index, String idGnn, String method) {
        if (this.currentX != null && this.currentEdgeIndex != null && this.currentMethod != null && this.currentResult != null && Objects.equals(this.lastId, idGnn)) {
            if (this.currentX.equals(x) && this.currentEdgeIndex.equals(edge_index) && this.currentMethod.equals(method)) {
                if (this.dimOut == 1)
                    System.out.println("not implemented");
                else return this.dimOut == 2;
            } else
                this.currentResult = null;
        }
        return false;
    }

    private boolean checkValuesCache(double[][] x, int[][] edge_index, String idGnn, String method) {
        if (this.currentX != null && this.currentEdgeIndex != null && this.currentMethod != null && this.currentResult != null && Objects.equals(this.lastId, idGnn)) {
            if (Arrays.deepEquals(currentX, x) && Arrays.deepEquals(currentEdgeIndex, edge_index) && this.currentMethod.equals(method)) {
                if (this.dimOut == 1)
                    System.out.println("not implemented");
                else return this.dimOut == 2;
            } else
                this.currentResult = null;
        }
        return false;
    }

    private boolean checkValuesHeteroCacheString(Map<String, String> x_dict, Map<String, String> edge_dict, String idGnn) {
        if (currentXdict != null && currentEdgeDict != null && this.currentResult != null && Objects.equals(this.lastId, idGnn)) {
            if (currentXdictString.equals(x_dict) && currentEdgeDictString.equals(edge_dict)) {
                return true;
            } else
                currentResult = null;
        }
        return false;
    }

    private boolean checkValuesHeteroCache(Map<String, double[][]> x_dict, Map<String, int[][]> edge_dict, String idGnn) {
        if (currentXdict != null && currentEdgeDict != null && this.currentResult != null && Objects.equals(this.lastId, idGnn)) {
            if (!currentXdict.keySet().equals(x_dict.keySet()) || !currentEdgeDict.keySet().equals(edge_dict.keySet()))
                return false;
            for (String key : currentXdict.keySet()) {
                if (!Arrays.deepEquals(currentXdict.get(key), x_dict.get(key))) {
                    currentResult = null;
                    return false;
                }
            }
            for (String key : currentEdgeDict.keySet()) {
                if (!Arrays.deepEquals(currentEdgeDict.get(key), edge_dict.get(key))) {
                    currentResult = null;
                    return false;
                }
            }
            return true;
        }
        return false;
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

    private double[][] createBoolEncodingMatrix(int num_nodes, int num_columns) {
        double[][] node_bool = new double[num_nodes][num_columns * 2];
        // initialize with all false (i.e. [0,1])
        for (int i = 0; i < node_bool.length; i++) {
            for (int j = 0; j < node_bool[i].length; j += 2) {
                node_bool[i][j] = 0;
                node_bool[i][j + 1] = 1;
            }
        }
        return node_bool;
    }

    private double[][] createOneHotEncodingMatrix(int num_nodes, int num_columns) {
        double[][] node_bool = new double[num_nodes][num_columns];
        // initialize with all false (i.e. [0])
        for (double[] ints : node_bool) Arrays.fill(ints, 0);
        return node_bool;
    }

    /**
     * Write the structure in a graph form for the GNN input using the attributes that will be
     * used in the features matrix for the input
     * @param num_nodes
     * @param finalre
     * @param cpmGnn
     * @return
     */
    public double[][] nodeToEncoding(int num_nodes, SparseRelStruc finalre, CPMGnn cpmGnn) {
        // table for the encoding of the node features, each feature is represented in a boolean encoding
        // 1 0 true - 0 1 false
        // example: 2 possible features blue and red. the node is red: 0 1 1 0

        OneStrucData data = finalre.getmydata();
        int num_columns = 0;
        // create the feature matrix with the attributes that have arity = 1
        if (cpmGnn instanceof CatGnn) {
            for (int i = 0; i < cpmGnn.getGnnattr().length; i++) {
                if (cpmGnn.getGnnattr()[i].arity == 1) {
                    num_columns++;
                }
            }
//            num_columns = ((CatGnn) cpmGnn).numvals();
        } else if (cpmGnn instanceof  CatGnnHetero) {
            for (int i = 0; i < ((CatGnnHetero) cpmGnn).getInput_attr().size(); i++) {
                if (cpmGnn.getGnnattr()[i].arity == 1) {
                    num_columns++;
                }
            }
        }

        double[][] node_bool;
        if (cpmGnn.isOneHotEncoding())
            node_bool = createOneHotEncodingMatrix(num_nodes, num_columns);
        else
            node_bool = createBoolEncodingMatrix(num_nodes, num_columns*2); // we should put different types of encoding, for now leave like that

        int idxFeat = 0;
        for (Rel parent : cpmGnn.getGnnattr()) {
            if (parent.arity == 1) { // only nodes
                if (parent instanceof CatRel) {
                    OneCatRelData relData = (OneCatRelData) data.find(parent);
                    for (int[] key : relData.values.keySet()) {
                        node_bool[key[0]][relData.values.get(key)] = 1;
                    }
                } else {
                    Vector<int[]> featureTrueData = data.allTrue(parent);
                    Vector<Vector<int[]>> allTrueData = new Vector<>();
                    allTrueData.add(featureTrueData);
                    // TODO find a method to handle node encoding for numeric feature !!
                    if (parent.valtype() == 2) { // if numeric
                        OneNumRelData num_data = (OneNumRelData) data.find(parent);
                        for (Vector<int[]> feature : allTrueData) {
                            for (int[] node : feature) {
                                node_bool[node[0]][idxFeat] = num_data.valueOf(node);
                            }
                            idxFeat++;
                        }
                    } else {
                        // change the true value
                        for (Vector<int[]> feature : allTrueData) {
                            for (int[] node : feature) {
                                node_bool[node[0]][idxFeat] = 1;
                                if (!cpmGnn.isOneHotEncoding()) // same, as in the creation of node_bool
                                    node_bool[node[0]][idxFeat + 1] = 0;
                            }
                            idxFeat++;
                        }
                    }
                }
            }
        }
        return node_bool;
    }

    public String stringifyGnnFeatures(int num_nodes, SparseRelStruc finalre, CPMGnn cpmGnn) {
        double[][] bool_nodes = this.nodeToEncoding(num_nodes, finalre, cpmGnn);
        return getTensorString(bool_nodes, true);
    }

    public String getTensorString(double[][] bool_nodes, boolean includeDeclaration) {
        StringBuilder node_features = new StringBuilder();
        if (includeDeclaration)
            node_features.append("x=torch.tensor([");
        else
            node_features.append("torch.tensor([");

        for (int i = 0; i < bool_nodes.length; i++) {
            node_features.append("[");
            for (int j = 0; j < bool_nodes[i].length; j++) {
                node_features.append(bool_nodes[i][j]);
//                node_features.append(".");
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

    public String arraysToEdgeTensor(int[][] edges, boolean includeDeclaration) {
        StringBuilder result = new StringBuilder();
        if (includeDeclaration)
            result.append("edge_index=torch.tensor([[");
        else
            result.append("torch.tensor([[");
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
        return this.arraysToEdgeTensor(arrays, true);
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
        return this.arraysToEdgeTensor(arrays, true);
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
        return this.arraysToEdgeTensor(arrays, true);
    }

    // Return the probability of the given graph by calling the Gnn model
    private Double[] evaluateInputGraph(CPMGnn cpmGnn,
                                      SparseRelStruc sampledRel,
                                      int num_nodes) {

        // find the bool relations in the .rdef (edges)
        Vector<BoolRel> boolrel = sampledRel.getBoolBinaryRelations();
        int[][] edge_index = null;
        for (BoolRel element : boolrel) {
            OneBoolRelData edgeinst = (OneBoolRelData) sampledRel.getmydata().find(element);
            Type[] argType = element.getTypes();
            int[] minValue = findStartNode(argType, sampledRel);
            TreeSet<int[]> edges_list = edgeinst.allTrue();
            edge_index = createEdgeArray(edges_list, minValue);
        }

        double[][] x = this.nodeToEncoding(num_nodes, sampledRel, cpmGnn);

        if (Objects.equals(cpmGnn.getGnn_inference(), "node"))
            return this.inferModelNodeDouble(Integer.parseInt(cpmGnn.getArgument()), x, edge_index, cpmGnn.getIdGnn(), "");
        else if (Objects.equals(cpmGnn.getGnn_inference(), "graph")) {
            return this.inferModelGraphDouble(x, edge_index, cpmGnn.getIdGnn(), "");
        }
        else
            throw new IllegalArgumentException("not valid keyword used: " + cpmGnn.getGnn_inference());
    }

    // this function encapsulate in one, all the evaluate function for a GNN inside Primula. The idea is to have a single point where the call to the python interface will be done.
    public Object[] evaluate_gnn(RelStruc A,
                                 OneStrucData inst,
                                 CPMGnn cpmGnn,
                                 boolean valonly) {

        Object[] result = new Object[2];
        double value;
        if (cpmGnn instanceof CatGnn) {
            result[0] = new double[((CatGnn) cpmGnn).numvals()];
        } else {
            result[0] = new double[2]; // ??
        }
        // only val no gradient computed
        if (valonly) {
            OneStrucData onsd = new OneStrucData(A.getmydata().copy()); // maybe i can avoid using the copy...
            SparseRelStruc sampledRel = new SparseRelStruc(A.getNames(), onsd, A.getCoords(), A.signature());
            sampledRel.getmydata().add(inst.copy());
            TreeSet<Rel> attr_parents = cpmGnn.parentRels();
            // if it has no parents we use the current attributes (should work for numeric rel)
            if (attr_parents.isEmpty()) {
                int num_nodes = A.domSize(); // the number of nodes should correspond to the domain size
                attr_parents.addAll(Arrays.asList(cpmGnn.getGnnattr()));
                // for now just return like this, checking if the attributes are NaN (below) maybe here can be avoided
                result[0] = this.evaluateInputGraph(cpmGnn, sampledRel, num_nodes);
                return result;
            } // else
            try {
                int num_nodes = 0;

                for (Rel parent : attr_parents) {
                    int[][] mat = A.allTypedTuples(parent.getTypes());
                    num_nodes = mat.length;
                    // for now, we just check if the attributes values are NaN
                    for (int i = 0; i < mat.length; i++) {
                        if (parent.ispredefined()) {
                            value = A.valueOf(parent, mat[i]);
                            if (Double.isNaN(value)) {
                                result[0] = new Double[]{Double.NaN};
                                return result;
                            }
                        } else if (parent.isprobabilistic()) {
                            value = inst.valueOf(parent, mat[i]);
                            if (Double.isNaN(value)) {
//                                    result[0] = new Double[]{Double.NaN};
                                Arrays.fill((double[])result[0],Double.NaN);
                                return result;
                            }
                        }
                    }
                }
                result[0] = this.evaluateInputGraph(cpmGnn, sampledRel, num_nodes);
            } catch (RBNIllegalArgumentException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("GRADIENT IN EVALUATION NOT IMPLEMENTED FOR GNN-RBN!");
        }
        return result;

    }

    public Map<Rel, int[][]> constructNodesDict(CatGnnHetero cpmHetero, RelStruc A) {
        // Dictionary with the name of the rel as key, and the nodes. (KEY DO NOT DIFFER WITH TYPE)
        Map<Rel, int[][]> nodesDict = new Hashtable<>();
        for (int i = 0; i < cpmHetero.getInput_attr().size(); i++) {
            ArrayList<Rel> subList = cpmHetero.getInput_attr().get(i);
            for (Rel rel : subList) {
                try {
                    int[][] mat = A.allTypedTuples(rel.getTypes());
                    nodesDict.put(rel, mat);
                } catch (RBNIllegalArgumentException e) {
                    throw new RuntimeException("Error in saveGnnData for features creation: " + e);
                }
            }
        }
        return nodesDict;
    }

    public Object[] evaluate_gnnHetero(RelStruc A, OneStrucData inst, CPMGnn cpmGnn, boolean valonly) {
        if (this.sharedInterpreter == null)
            throw new NullPointerException("GnnPy object null!");
        if (!(cpmGnn instanceof CatGnnHetero))
            throw new RuntimeException("CPMGnn must be CatGnnHetero");

        Object[] result = new Object[2];
        CatGnnHetero cpmHetero = (CatGnnHetero) cpmGnn;

        double value;
        result[0] = new double[((CatGnnHetero) cpmGnn).numvals()];

        // only val no gradient computed
        if (valonly) {
            OneStrucData onsd = new OneStrucData(A.getmydata().copy());
            SparseRelStruc sampledRel = new SparseRelStruc(A.getNames(), onsd, A.getCoords(), A.signature());
            sampledRel.getmydata().add(inst.copy());
            TreeSet<Rel> attr_parents = cpmGnn.parentRels();
            if (GGboolRel == null)
                GGboolRel = sampledRel.getBoolBinaryRelations();
            if (GGNodesDict.isEmpty())
                GGNodesDict = constructNodesDict(cpmHetero, A);
            // if it has no parents we use the current attributes
            if (attr_parents.isEmpty()) {
                Map<String, double[][]> x_dict = inputAttrToDict(cpmHetero, GGNodesDict, sampledRel, cpmGnn);
                Map<String, int[][]> edge_dict = edgesToDict(GGboolRel, sampledRel);

                if (cpmGnn.getGnn_inference().equals("node"))
                    result[0] = inferModelNodeHeteroDouble(Integer.parseInt(cpmGnn.getArgument()), x_dict, edge_dict, cpmGnn.getIdGnn());
                return result;
            }
            // else
            try {
                for (Rel parent : attr_parents) {
                    int[][] mat = A.allTypedTuples(parent.getTypes());
                    // for now, we just check if the attributes values are NaN
                    for (int i = 0; i < mat.length; i++) {
                        if (parent.ispredefined()) {
                            value = A.valueOf(parent, mat[i]);
                            if (Double.isNaN(value)) {
                                result[0] = new Double[]{Double.NaN};
                                return result;
                            }
                        } else if (parent.isprobabilistic()) {
                            value = inst.valueOf(parent, mat[i]);
                            if (Double.isNaN(value)) {
//                                    result[0] = new Double[]{Double.NaN};
                                Arrays.fill((double[])result[0],Double.NaN);
                                return result;
                            }
                        }
                    }
                }
                Map<String, double[][]> x_dict = inputAttrToDict(cpmHetero, GGNodesDict, sampledRel, cpmGnn);
                Map<String, int[][]> edge_dict = edgesToDict(GGboolRel, sampledRel);
                if (cpmGnn.getGnn_inference().equals("node"))
                    result[0] = inferModelNodeHeteroDouble(Integer.parseInt(cpmGnn.getArgument()), x_dict, edge_dict, cpmGnn.getIdGnn());
            } catch (RBNIllegalArgumentException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("GRADIENT IN EVALUATION NOT IMPLEMENTED FOR GNN-RBN!");
        }
        return result;
    }

    // for Gradient Graph
//    public Double[] GGevaluate_gnn(RelStruc A, GradientGraphO gg, CPMGnn cpmGnn, GGCPMGnn ggcpmGnn) {
//        if (ggcpmGnn.value() != null) {
//            return ggcpmGnn.value();
//        }
//
//        if (this.sharedInterpreter == null) {
//            throw new NullPointerException("GnnPy object null!");
//        } else {
//            // this first part set the x and edge_index object once only if predefined
//            if (cpmGnn.parentRels().isEmpty() && Objects.equals(GGx, "")) {
//                GGx = this.stringifyGnnFeatures(GGnumNodes, GGsampledRel, cpmGnn);
//            }
//            if (GGedge_pred && Objects.equals(GGedge_index, "")) {
//                for (BoolRel element : GGboolRel) {
//                    if (GGsampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
//                        GGedge_index = "";
//                        break;
//                    } else {
//                        if (Objects.equals(element.name(), cpmGnn.getEdge_name())) {
//                            GGedge_pred = true;
//                            GGedge_index = edgeDirection(GGsampledRel, cpmGnn, element);
//                            break;
//                        }
//                    }
//                }
//            }
//            // else we need to use the sampled values in the gradient graph structure (maxindicator) and assign them to the rel
//            try {
//                for (Rel parent : cpmGnn.parentRels()) {
//                    int[][] mat = A.allTypedTuples(parent.getTypes());
//                    for (int i = 0; i < mat.length; i++) {
//                        // find the nodes that have no values (-1) and assign the values form the currentInst in the maxIndicator to the sampledRel
//                        if (Objects.equals(GGsampledRel.getData().find(parent).dv(), "?")) {
////                            if (sampledRel.truthValueOf(parent, mat[i]) == -1) {
//                            GroundAtom myatom = new GroundAtom(parent, mat[i]);
//                            GGAtomMaxNode currentMaxNode = gg.findInMaxindicators(myatom);
//
//                            if (parent instanceof CatRel)
//                                GGsampledRel.getmydata().findInCatRel(parent).add(mat[i], currentMaxNode.getCurrentInst());
//                            else {
//                                boolean sampledVal = currentMaxNode.getCurrentInst() == 1;
//                                GGsampledRel.getmydata().findInBoolRel(parent).add(mat[i], sampledVal);
//                            }
//                        }
//                    }
//                }
//                if (!cpmGnn.parentRels().isEmpty())
//                    GGx = this.stringifyGnnFeatures(GGnumNodes, GGsampledRel, cpmGnn);
//            } catch (RBNIllegalArgumentException e) {
//                throw new RuntimeException(e);
//            }
//
//            // take only the "edge" relation
//            // this can be later changed to a more general approach
//            // find the boolean relations that should represent edges
//            double result;
//            if (!GGedge_pred) {
//                GGboolRel = GGsampledRel.getBoolBinaryRelations();
//                for (BoolRel element : GGboolRel) {
//                    if (GGsampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
//                        GGedge_index = "";
//                        break;
//                    } else {
//                        if (Objects.equals(element.name(), cpmGnn.getEdge_name())) {
//                            GGedge_index = edgeDirection(GGsampledRel, cpmGnn, element);
//                        }
//                    }
//                }
//            }
//
//            Double[] value;
//            if (Objects.equals(cpmGnn.getGnn_inference(), "node"))
//                value = this.inferModelNodeDouble(Integer.parseInt(cpmGnn.getArgument()), GGx, GGedge_index, cpmGnn.getIdGnn(), "", true);
//            else if (Objects.equals(cpmGnn.getGnn_inference(), "graph")) {
//                value = this.inferModelGraphDouble(GGx, GGedge_index, cpmGnn.getIdGnn(),"");
//            } else
//                throw new IllegalArgumentException("not valid keyword used: " + cpmGnn.getGnn_inference());
//            return value;
//        }
//    }

    public Double[] GGevaluate_gnn(RelStruc A, GradientGraphO gg, CPMGnn cpmGnn, GGCPMGnn ggcpmGnn) {
        if (sharedInterpreter == null)
            throw new NullPointerException("GnnPy object null in GGevaluate_gnn ...");
        if (!(cpmGnn instanceof CatGnn))
            throw new RuntimeException("CPMGnn must be CatGnn in GGevaluate_gnn ...");
        CatGnn cpm = (CatGnn) cpmGnn;

        if (GGx == null && GGedge_index == null) {
           if (cpm.parentRels().isEmpty() && GGx == null)
                GGx = this.nodeToEncoding(GGnumNodes, GGsampledRel, cpm);
        }
        if (GGedge_pred && GGedge_index == null) {
            for (BoolRel element : GGboolRel) {
                if (GGsampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
                    GGedge_index = null;
                    break;
                } else {
                    if (Objects.equals(element.name(), cpm.getEdge_name())) {
                        GGedge_pred = true;
                        OneBoolRelData edgeinst = (OneBoolRelData) GGsampledRel.getmydata().find(element);
                        TreeSet<int[]> edges_list = edgeinst.allTrue();
                        int[][] edge_index = new int[2][edges_list.size()];
                        int idx = 0;
                        for (int[] array : edges_list) {
                            edge_index[0][idx] = array[0];
                            edge_index[1][idx] = array[1];
                            idx++;
                        }
                        // Sort the arrays based on the first column
                        Arrays.sort(edge_index, Comparator.comparingInt(a -> a[0]));
                        GGedge_index = edge_index;
                        break;
                    }
                }
            }
        }
        // else we need to use the sampled values in the gradient graph structure (maxindicator) and assign them to the rel
        try {
            for (Rel parent : cpm.parentRels()) {
                int[][] mat = A.allTypedTuples(parent.getTypes());
                for (int i = 0; i < mat.length; i++) {
                    // find the nodes that have no values (-1) and assign the values form the currentInst in the maxIndicator to the sampledRel
                    if (Objects.equals(GGsampledRel.getData().find(parent).dv(), "?")) {
                        GroundAtom myatom = new GroundAtom(parent, mat[i]);
                        GGAtomMaxNode currentMaxNode = gg.findInMaxindicators(myatom);
                        if (parent instanceof CatRel)
                            GGsampledRel.getmydata().findInCatRel(parent).add(mat[i], currentMaxNode.getCurrentInst());
                        else {
                            boolean sampledVal = currentMaxNode.getCurrentInst() == 1;
                            GGsampledRel.getmydata().findInBoolRel(parent).add(mat[i], sampledVal);
                        }
                    }
                }
            }
            if (!cpm.parentRels().isEmpty())
                GGx = this.nodeToEncoding(GGnumNodes, GGsampledRel, cpm);
        } catch (RBNIllegalArgumentException e) {
            throw new RuntimeException(e);
        }

        Double[] value;
        if (Objects.equals(cpm.getGnn_inference(), "node"))
            value = this.inferModelNodeDouble(Integer.parseInt(cpm.getArgument()), GGx, GGedge_index, cpm.getIdGnn(), "");
        else if (Objects.equals(cpm.getGnn_inference(), "graph")) {
            value = this.inferModelGraphDouble(GGx, GGedge_index, cpm.getIdGnn(),"");
        } else
            throw new IllegalArgumentException("not valid keyword used: " + cpm.getGnn_inference());
        return value;
    }

    public Map<String, String> inputAttrToDictString(CatGnnHetero cpmHetero, Map<Rel, int[][]> GGnumNodesDict, SparseRelStruc sampledRel, CPMGnn cpmGnn) {
        Map<String, String> x_dict = new Hashtable<>();
        for (int i = 0; i < cpmHetero.getInput_attr().size(); i++) {
            ArrayList<Rel> subList = cpmHetero.getInput_attr().get(i);
            for (int j = 0; j < subList.size() - 1; j++) {
                // check if all the types in the subList are the same
                if (!subList.get(j).getTypes()[0].getName().equals(subList.get(j + 1).getTypes()[0].getName())) {
                    throw new RuntimeException("Types of the relations do not match!");
                }
            }
            String key = subList.get(0).getTypes()[0].getName();
            double[][] inputMatrix = createTensorMatrix(subList, GGnumNodesDict, sampledRel);
            x_dict.put(key, getTensorString(inputMatrix, false));
        }
        return x_dict;
    }

    public Map<String, double[][]> inputAttrToDict(CatGnnHetero cpmHetero, Map<Rel, int[][]> GGNodesDict, SparseRelStruc sampledRel, CPMGnn cpmGnn) {
        Map<String, double[][]> x_dict = new Hashtable<>();
        for (int i = 0; i < cpmHetero.getInput_attr().size(); i++) {
            ArrayList<Rel> subList = cpmHetero.getInput_attr().get(i);
            for (int j = 0; j < subList.size() - 1; j++) {
                // check if all the types in the subList are the same
                if (!subList.get(j).getTypes()[0].getName().equals(subList.get(j + 1).getTypes()[0].getName())) {
                    throw new RuntimeException("Types of the relations do not match!");
                }
            }
            String key = subList.get(0).getTypes()[0].getName();
            x_dict.put(key, createTensorMatrix(subList, GGNodesDict, sampledRel));
        }
        return x_dict;
    }


    // look for the first node in the nodetype
    private int[] findStartNode(Type[] argType, SparseRelStruc sampledRel) {
        int[] minValue = new int[2];
        for (int i = 0; i < argType.length; i++) {
            for (OneBoolRelData ob : sampledRel.getmydata().allonebooldata) {
                if (ob.rel.name().equals(argType[i].getName())) {
                    minValue[i] = ob.allTrue().first()[0];
                }
            }
        }
        return minValue;
    }

    private int[][] createEdgeArray(TreeSet<int[]> edges_list, int[] minValue) {
        int[][] arrays = new int[2][edges_list.size()];
        int idx = 0;
        for (int[] edge : edges_list) {
            arrays[0][idx] = edge[0] - minValue[0];
            arrays[1][idx] = edge[1] - minValue[1];
            idx++;
        }
//        Arrays.sort(arrays, Comparator.comparingInt(a -> a[0]));
        return arrays; // do we need to sort it? (see previous implementation)
    }
    public Map<String, String> edgesToDictString(Vector<BoolRel> GGboolRel, SparseRelStruc sampledRel) {
        Map<String, String> edge_dict = new Hashtable<>();
        for (BoolRel element : GGboolRel) {
            if (sampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
                edge_dict.put(element.name(), "torch.empty((2, 0), dtype=torch.long)");
            } else {
                OneBoolRelData edgeinst = (OneBoolRelData) sampledRel.getmydata().find(element);
                Type[] argType = element.getTypes();
                int[] minValue = findStartNode(argType, sampledRel);

                TreeSet<int[]> edges_list = edgeinst.allTrue();
                int[][] arrays = createEdgeArray(edges_list, minValue);
                edge_dict.put(element.name(), arraysToEdgeTensor(arrays, false));
            }
        }
        return edge_dict;
    }

    public Map<String, int[][]> edgesToDict(Vector<BoolRel> GGboolRel, SparseRelStruc sampledRel) {
        Map<String, int[][]> edge_dict = new Hashtable<>();
        for (BoolRel element : GGboolRel) {
            if (sampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
                edge_dict.put(element.name(), new int[0][2]);
            } else {
                OneBoolRelData edgeinst = (OneBoolRelData) sampledRel.getmydata().find(element);
                Type[] argType = element.getTypes();
                int[] minValue = findStartNode(argType, sampledRel);

                TreeSet<int[]> edges_list = edgeinst.allTrue();
                edge_dict.put(element.name(), createEdgeArray(edges_list, minValue));
            }
        }
        return edge_dict;
    }

    // this function works only with predefined rels! all the values in the GG will be set as 0
    // also with instantiated probabilistic rels (are inside the finalre)
    public double[][] createTensorMatrix(ArrayList<Rel> attributes, Map<Rel, int[][]> nodes_dict, SparseRelStruc finalre) {
        int num_col = 0;
        int num_nodes = nodes_dict.get(attributes.get(0)).length; // take the first (they should have all the same dimension)
        // count how many columns the matrix will have
        for (Rel r : attributes) {
            if (r instanceof CatRel)
                num_col += r.numvals();
            else
                num_col += 1;
        }
        double[][] bool_nodes = createOneHotEncodingMatrix(num_nodes, num_col);
        OneStrucData data = finalre.getmydata();
        int current_col = 0;
        int idxFeat = 0;
        for (Rel r : attributes) {
            if (r instanceof CatRel) {
                OneCatRelData relData = (OneCatRelData) data.find(r);
                int minValue = nodes_dict.get(r)[0][0]; // Big assumption! The first element should be also the smallest!

                /** TreeMap should be sorted by keys, the min value is in the first position
                 * node id can start from numbers higher than 0 but all the matrix not,
                 * then subtract the minimum to start from zero
                 */
//                int minValue = -1;
//                Type typ = relData.rel.getTypes()[0];
//                for (OneBoolRelData ob: data.allonebooldata) {
//                    if (ob.rel.name().equals(typ.getName())) {
//                        minValue = ob.allTrue().first()[0];
//                        break;
//                    }
//                }

                for (int[] key : relData.values.keySet()) {
                    bool_nodes[key[0] - minValue][relData.values.get(key) + idxFeat] = 1;
                }
                idxFeat += r.numvals();
            } else {
                Vector<int[]> featureTrueData = data.allTrue(r);
                Vector<Vector<int[]>> allTrueData = new Vector<>();
                allTrueData.add(featureTrueData);
                if (r.valtype() == Rel.NUMERIC) {
                    OneNumRelData num_data = (OneNumRelData) data.find(r);
                    int minValue = -1;
                    Type typ = num_data.rel.getTypes()[0];
                    for (OneBoolRelData ob: data.allonebooldata) {
                        if (ob.rel.name().equals(typ.getName()))
                            minValue = ob.allTrue().first()[0];
                    }
                    for (Vector<int[]> feature : allTrueData) {
                        for (int[] node : feature) {
                            bool_nodes[node[0] - minValue][idxFeat] = num_data.valueOf(node);
                        }
                        idxFeat++;
                    }
                } else {
                    // change the true value
                    // TODO need to do the same as NUMERIC -> subtract the min value
                    for (Vector<int[]> feature : allTrueData) {
                        for (int[] node : feature) {
                            bool_nodes[node[0]][idxFeat] = 1;
                        }
                        idxFeat++;
                    }
                }
            }
        }

        return bool_nodes;
    }

    // initialize the input matrix with the values of the rels that are predefines otherwise set to 0
    // in sampledRel there are also the inst values!
    public Map<String, double[][]> initXdict(CatGnnHetero cpmHetero, Map<Rel, int[][]> GGnumNodesDict, SparseRelStruc sampledRel) {
        Map<String, double[][]> x_dict = new HashMap<>();
        for (int i = 0; i < cpmHetero.getInput_attr().size(); i++) {
            ArrayList<Rel> subList = cpmHetero.getInput_attr().get(i);
            for (int j = 0; j < subList.size() - 1; j++) {
                // check if all the types in the subList are the same
                if (!subList.get(j).getTypes()[0].getName().equals(subList.get(j + 1).getTypes()[0].getName())) {
                    throw new RuntimeException("Types of the relations do not match!");
                }
            }
            String key = subList.get(0).getTypes()[0].getName();
            double[][] inputXmatrix = createTensorMatrix(subList, GGnumNodesDict, sampledRel);
            x_dict.put(key, inputXmatrix);
        }
        return x_dict;
    }
    public void updateInputDict(Map<String, double[][]> input_dict, Rel rel, CatGnnHetero cpmHetero, Map<Rel, int[][]> GGnumNodesDict, GradientGraphO gg, GGCPMNode ggcpmNode) {
        Vector<GGCPMNode> childred = ggcpmNode.getChildren(); // is this always will work?
        for (ArrayList<Rel> subList: cpmHetero.getInput_attr()) {
            Rel firstRel = subList.get(0);
            String key = firstRel.getTypes()[0].getName(); // should have the same type, we take the first
            int idxFeat = 0;
            double[][] inputMatrix = input_dict.get(key);
            for (Rel subRel: subList) {
                if (rel.equals(subRel)) {
                    int[][] nodes = GGnumNodesDict.get(rel);
                    int min_node = nodes[0][0];
                    for (GGCPMNode node: childred) {
                        GGAtomMaxNode maxNode = (GGAtomMaxNode) node;
                        int arg = maxNode.myatom().args[0];
                        int value = maxNode.getCurrentInst();
                        if (subRel instanceof CatRel)
                            inputMatrix[arg-min_node][value+idxFeat] = 1;
                        else // numeric values should not be here
                            inputMatrix[arg-min_node][idxFeat] = 1;
                    }
                }
                if (subRel instanceof CatRel)
                    idxFeat += subRel.numvals();
                else
                    idxFeat++;
            }
        }
    }

    public Map<String, int[][]> initEdgesDict(Vector<BoolRel> GGboolRel, SparseRelStruc sampledRel) {
        Map<String, int[][]> edge_dict = new HashMap<>();
        for (BoolRel element : GGboolRel) { // maybe check if all the binary rels are the edges?
            OneBoolRelData edgeinst = (OneBoolRelData) sampledRel.getmydata().find(element);
            Type[] argType = element.getTypes();
            int[] minValue = findStartNode(argType, sampledRel);

            TreeSet<int[]> edges_list = edgeinst.allTrue();
            int[][] arrays = createEdgeArray(edges_list, minValue);
            edge_dict.put(element.name(), arrays);
        }
        return edge_dict;
    }

    public void resetDict() {
        GGxDict = new HashMap<>();
        GGedgeDict = new HashMap<>();
    }

    public Double[] GGevaluate_gnnHetero(RelStruc A, GradientGraphO gg, CPMGnn cpmGnn, GGCPMNode ggcpmGnn) {
        if (this.sharedInterpreter == null)
            throw new NullPointerException("GnnPy object null!");
        if (!(cpmGnn instanceof CatGnnHetero))
            throw new RuntimeException("CPMGnn must be CatGnnHetero");
        CatGnnHetero cpmHetero = (CatGnnHetero) cpmGnn;

        if (GGxDict.isEmpty() && GGedgeDict.isEmpty()) {
            GGxDict = initXdict(cpmHetero, GGNodesDict, GGsampledRel);
            GGedgeDict = initEdgesDict(GGboolRel, GGsampledRel);

            // we need to use the sampled values in the gradient graph structure (maxindicator) and assign them to the rel
            cpmHetero.parentRels().forEach(parent -> {
                updateInputDict(GGxDict, parent, cpmHetero, GGNodesDict, gg, ggcpmGnn);
            });
        }
        Double[] res = inferModelNodeHeteroDouble(Integer.parseInt(cpmGnn.getArgument()), GGxDict, GGedgeDict, cpmGnn.getIdGnn());
        return res;
    }

    public void savePickleHetero(Map<String, double[][]> xDict, Map<String, int[][]> edgeDict) {
        this.sharedInterpreter.set("java_map_x", xDict);
        this.sharedInterpreter.set("java_map_edge", edgeDict);

        this.sharedInterpreter.exec(
                 "import pickle\n" +
                    "data_h = HeteroData()\n" +

                    "for key, value in java_map_x.items():\n" +
                    "    data_h[key].x = torch.as_tensor(value, dtype=torch.float32)\n" +

                     "for key, value in java_map_edge.items():\n" +
                     "    n_key = key.split('_to_')\n" + // here the key must have the form type_to_type
                     "    if len(value) > 0:\n" +
                     "        data_h[n_key[0], 'to', n_key[1]].edge_index = torch.as_tensor(value, dtype=torch.long)\n" +
                     "    else:\n" +
                     "        data_h[n_key[0], 'to', n_key[1]].edge_index = torch.empty((2, 0), dtype=torch.long)\n" +

                    "with open('/Users/lz50rg/Dev/water-hawqs/map-inf-low.pkl', 'wb') as f:\n" +
                    "    pickle.dump(data_h, f)\n"
        );
        System.out.println("Pickle written in /Users/lz50rg/Dev/water-hawqs/map-inf-low.pkl!");
    }

    private String edgeDirection(SparseRelStruc sampledRel, CPMGnn cpmGnn, BoolRel element) {
        if (Objects.equals(cpmGnn.getEdge_direction(), "ABBA"))
            return this.stringifyGnnEdgesABBA(sampledRel, element);
        if (Objects.equals(cpmGnn.getEdge_direction(), "AB"))
            return this.stringifyGnnEdgesAB(sampledRel, element);
        if (Objects.equals(cpmGnn.getEdge_direction(), "BA"))
            return this.stringifyGnnEdgesBA(sampledRel, element);
        return null;
    }

    // Initial caching of the data in GGGnnNode
    public void saveGnnData(CPMGnn cpmGnn, RelStruc A, OneStrucData inst) {
        if (GGonsd == null && GGsampledRel == null) {
            GGonsd = new OneStrucData(A.getmydata().copy()); // only one copy per time
            GGsampledRel = new SparseRelStruc(A.getNames(), GGonsd, A.getCoords(), A.signature());
            GGsampledRel.getmydata().add(inst.copy());
        }
        GGxString = "";
        GGx = null;

        if (cpmGnn instanceof CatGnnHetero) {
            GGNodesDict = constructNodesDict((CatGnnHetero) cpmGnn, A);
            GGxDict = new HashMap<>();
            GGedgeDict = new HashMap<>();
            if (GGboolRel == null)
                GGboolRel = GGsampledRel.getBoolBinaryRelations();
            GGedge_indexString = "";
        } else {
            GGnumNodes = -1;
            for (Rel attr : cpmGnn.getGnnattr()) {
                try {
                    int[][] mat = A.allTypedTuples(attr.getTypes());
                    // maybe there could be attributes with different number, we keep the biggest
                    if (attr.arity == 1 && mat.length >= GGnumNodes)
                        GGnumNodes = mat.length;
                } catch (RBNIllegalArgumentException e) {
                    throw new RuntimeException("Error in saveGnnData for features creation: " + e);
                }
            }
            // if the edge relations are predefined compute only once
            if (GGboolRel == null)
                GGboolRel = GGsampledRel.getBoolBinaryRelations();
            GGedge_indexString = "";
            GGedge_index = null;
            GGedge_pred = false;
            for (BoolRel element : GGboolRel) {
                if (element.ispredefined()) {
                    if (Objects.equals(element.name(), cpmGnn.getEdge_name()))
                        GGedge_pred = true;
                }
            }
        }
    }

    public double[] evalSample_gnn(CPMGnn cpmGnn, RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst) {
        OneStrucData onsd = new OneStrucData(A.getmydata().copy());
        // we need to sample the entire structure before sending to python
        int num_features = 0;
        SparseRelStruc sampledRel = new SparseRelStruc(A.getNames(), onsd, A.getCoords(), A.signature());
        sampledRel.getmydata().add(inst.copy());
        TreeSet<Rel> attr_parents = cpmGnn.parentRels();
        // if it has no parents we use the current attributes (should work for numeric rel)
        if (attr_parents.isEmpty()) {
            attr_parents.addAll(Arrays.asList(cpmGnn.getGnnattr()));
        }
        for (Rel parent : attr_parents) {
            try {
                int[][] mat = A.allTypedTuples(parent.getTypes());
                // maybe there could be attributes with different number, we keep the biggest
                if (parent.arity == 1 && mat.length >= num_features)
                    num_features = mat.length;

                for (int i = 0; i < mat.length; i++) {
                    if (sampledRel.truthValueOf(parent, mat[i]) == -1) {
                        GroundAtom myatom = new GroundAtom(parent, mat[i]);
                        String myatomname = myatom.asString();
                        PFNetworkNode gan = (PFNetworkNode) atomhasht.get(myatomname);
                        if (gan != null) {
                            double result = (double) gan.sampleinstVal();
                            boolean sampledVal = false;
                            if (result == 1)
                                sampledVal = true;
                            sampledRel.getmydata().findInBoolRel(parent).add(mat[i], sampledVal);
                        }
                    }
                }

            } catch (RBNIllegalArgumentException e) {
                throw new RuntimeException(e);
            }
        }

        // take only the "edge" relation
        // this can be later changed to a more general approach
        // find the boolean relations that should represent edges
        Vector<BoolRel> boolrel = sampledRel.getBoolBinaryRelations();
        int[][] edge_index = null;
        for (BoolRel element : boolrel) {
            OneBoolRelData edgeinst = (OneBoolRelData) sampledRel.getmydata().find(element);
            Type[] argType = element.getTypes();
            int[] minValue = findStartNode(argType, sampledRel);
            TreeSet<int[]> edges_list = edgeinst.allTrue();
            edge_index = createEdgeArray(edges_list, minValue);
        }

//        String x = this.stringifyGnnFeatures(num_features, sampledRel, cpmGnn);
        double[][] x = this.nodeToEncoding(num_features, sampledRel, cpmGnn);
        Double[] res_py =  this.inferModelNodeDouble(Integer.parseInt(cpmGnn.getArgument()), x, edge_index, cpmGnn.getIdGnn(), "");
        double[] res = new double[res_py.length];
        for (int i = 0; i < res_py.length; i++) {
            res[i] = res_py[i];
        }
        return res;
    }

    public Map<String, double[][]> getCurrentXdict() {
        return currentXdict;
    }

    public Map<String, int[][]> getCurrentEdgeDict() {
        return currentEdgeDict;
    }
}


