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

public class GnnPy {
    private final String INFER_NODE = "infer_model_nodes"; // default function name to call in python
    private final String INFER_GRAPH = "infer_model_graph";
    private String scriptPath;
    private String scriptName;
    private String pythonHome;
    private Interpreter sharedInterpreter;

    // those 4 next vairables are used to save the current query
    private String currentX;

    private String currentEdgeIndex;

    private String currentMethod;

    private Double[][] currentResult;
    private String lastId;

    private long dimOut;
    private int numClass;
    private ArrayList<String> gnnModelsId;

    static private OneStrucData GGonsd;
    static private SparseRelStruc GGsampledRel;
    static private Vector<BoolRel> GGboolRel;
    static private String GGx;
    static private String GGedge_index;
    private boolean GGedge_pred;
    static private int GGnumNodes;
    private boolean jepReady;

    public GnnPy(String scriptPath, String scriptName, String pythonHome) throws IOException {
        // pip install jep in a miniconda env (torch)
        initializeJep(scriptPath, scriptName, pythonHome);
//        JepConfig jepConfig = new JepConfig();
//        jepConfig.redirectStdout(System.out);
//        jepConfig.redirectStdErr(System.err);
//        MainInterpreter.setJepLibraryPath("/Users/lz50rg/miniconda3/envs/torch/lib/python3.10/site-packages/jep/libjep.jnilib");
//        PyConfig pyConfig = new PyConfig();
//        pyConfig.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");

//        jep.JepConfig jepConf = new JepConfig();
//        jepConf.addSharedModules("numpy"); // this helps for the warning (https://github.com/ninia/jep/issues/418#issuecomment-1165062651)
    }

    public GnnPy() {
        jepReady = false;
    }

    public String loadjep(String pythonHome) throws IOException {
        // taken from https://gist.github.com/vwxyzjn/c054bae6dfa6f80e6c663df70347e238
        // automatically find the library path in the python home it is installed
        Process p = Runtime.getRuntime().exec(pythonHome + " python/get_jep_path.py");
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        return in.readLine();
    }

    public void initializeJep(String scriptPath, String scriptName, String pythonHome)  throws IOException {
        this.scriptPath = scriptPath;
        this.scriptName = scriptName;
        this.pythonHome = pythonHome;
        this.gnnModelsId = new ArrayList<>();
        this.numClass = -1;
        this.dimOut = -1;

        try {
            MainInterpreter.setJepLibraryPath(loadjep(this.pythonHome));
            jepReady = true;
        } catch (IllegalStateException e) {
            System.out.println(e);
            jepReady = false;
        }
        initializePythonScript();
    }

    private void initializePythonScript() {
        try {
            this.sharedInterpreter = new SharedInterpreter();
            this.sharedInterpreter.exec("import sys");
            this.sharedInterpreter.exec("import torch");
            this.sharedInterpreter.exec("import numpy as np");
            this.sharedInterpreter.eval("sys.path.append('" + this.scriptPath + "')");
            this.sharedInterpreter.eval("import " + this.scriptName + " as intt");
        } catch (JepException e) {
            System.err.println("Failed to initialize interpreter: " + e);
            jepReady = false;
        }
    }

    public void closeInterpreter() {
        if (sharedInterpreter != null) {
            sharedInterpreter.close();
            sharedInterpreter = null;
            jepReady = false;
            System.out.println("Interpreter closed");
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

    public Double[] inferModelNodeDouble(int node, String x, String edge_index, String idGnn, String method, boolean boundValue) {
        assert this.sharedInterpreter != null;
        this.createModelIfNull(idGnn);

        try {
            // check if there is already computed the results for the specific node in the result matrix, otherwise compute for all nodes with one forward propagation
            // needs also to have the same id as before
            if (this.currentX != null && this.currentEdgeIndex != null && this.currentMethod != null && this.currentResult != null && Objects.equals(this.lastId, idGnn)) {
                if (this.currentX.equals(x) && this.currentEdgeIndex.equals(edge_index) && this.currentMethod.equals(method)) {
                    if (this.dimOut == 1)
                        System.out.println("not implemented");
//                        return (double) this.currentResult[node];
                    else if (this.dimOut == 2) {
                        Double[] catres = currentResult[node];
                        // this serves to help MAP inference (avoid infinite in the score)
                        for (int i = 0; boundValue && i < catres.length; i++) {
                            catres[i] = catres[i]*0.99999 + (1-catres[i])*0.00001;
                        }
                        return catres;
                    }
                } else {
                    this.currentResult = null;
                }
            }
            this.currentX = x;
            this.currentEdgeIndex = edge_index;
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
                // this serves to help MAP inference (avoid infinite in the score)
                for (int i = 0; boundValue && i < catres.length; i++) {
                        catres[i] = catres[i]*0.9999 + (1-catres[i])*0.0001;
                }

                return catres;
            }
            // else nothing
            return null;
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    public Double[] inferModelGraphDouble(int classId, String x, String edge_index, String idGnn, String method) {
        assert this.sharedInterpreter != null;
        this.createModelIfNull(idGnn);

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
            // if there are no edges we use this trick for pyg
            if (Objects.equals(edge_index, "")) {
                this.sharedInterpreter.eval("edge_index = torch.cat((torch.tensor([[]], dtype=torch.long), torch.tensor([[]], dtype=torch.long)), 0)");
            } else
                this.sharedInterpreter.eval(edge_index);
            this.sharedInterpreter.eval(x);
            if (!Objects.equals(method, "")) {
                this.sharedInterpreter.eval("out = intt." + method + "(" + idGnn + ", x, edge_index, None)");
            } else {
                this.sharedInterpreter.eval("out = intt." + this.INFER_GRAPH + "(" + idGnn + ", x, edge_index, None)");
            }

            this.sharedInterpreter.eval("out = out.detach().numpy()");
            NDArray ndArray = (NDArray) this.sharedInterpreter.getValue("out");
            float[] tarr = (float[]) ndArray.getData();
            currentResult = convertTo2D(tarr, ndArray.getDimensions()[0], ndArray.getDimensions()[1]);
            Double[] catres = currentResult[classId];
            return catres;
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
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
     * @param attributes
     * @return
     */
    public double[][] nodeToEncoding(int num_nodes, SparseRelStruc finalre, Rel[] attributes, boolean oneHotEncoding) {
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

        double[][] node_bool;
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
                            if (!oneHotEncoding) // same, as in the creation of node_bool
                                node_bool[node[0]][idxFeat + 1] = 0;
                        }
                        idxFeat++;
                    }
                }
            }
        }
        return node_bool;
    }

    public String stringifyGnnFeatures(int num_nodes, SparseRelStruc finalre, Rel[] attributes, boolean oneHotEncoding) {
        double[][] bool_nodes = this.nodeToEncoding(num_nodes, finalre, attributes, oneHotEncoding);
        StringBuilder node_features = new StringBuilder("x=torch.tensor([");
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

    // Return the probability of the given graph by calling the Gnn model
    private Double[] evaluateInputGraph(CPMGnn cpmGnn,
                                      SparseRelStruc sampledRel,
                                      int num_nodes) {

        Vector<BoolRel> boolrel = sampledRel.getBoolBinaryRelations();
        String edge_index = "";
        for (BoolRel element : boolrel) {
            if (Objects.equals(element.name(), cpmGnn.getEdge_name())) {
                if (Objects.equals(cpmGnn.getEdge_direction(), "ABBA"))
                    edge_index = this.stringifyGnnEdgesABBA(sampledRel, element);
                if (Objects.equals(cpmGnn.getEdge_direction(), "AB"))
                    edge_index = this.stringifyGnnEdgesAB(sampledRel, element);
                if (Objects.equals(cpmGnn.getEdge_direction(), "BA"))
                    edge_index = this.stringifyGnnEdgesBA(sampledRel, element);
                break;
            }
        }

        String x = this.stringifyGnnFeatures(num_nodes, sampledRel, cpmGnn.getGnnattr(), cpmGnn.isOneHotEncoding());

        if (Objects.equals(cpmGnn.getGnn_inference(), "node"))
            return this.inferModelNodeDouble(Integer.parseInt(cpmGnn.getArgument()), x, edge_index, cpmGnn.getIdGnn(), "", cpmGnn.isBoolean()); // if it is not categorical the softmax should prevent this already
//        else if (Objects.equals(cpmGnn.getGnn_inference(), "graph")) {
//            return this.inferModelGraphDouble(cpmGnn.getClassId(), x, edge_index, cpmGnn.getIdGnn(), "");
//        }
        else
            throw new IllegalArgumentException("not valid keyword used: " + cpmGnn.getGnn_inference());
    }

    // this function encapsulate in one, all the evaluate function for a GNN inside Primula. The idea is to have a single point where the call to the python interface will be done.
    public Object[] evaluate_gnn(RelStruc A,
                                 OneStrucData inst,
                                 CPMGnn cpmGnn,
                                 boolean valonly) {

        if (!cpmGnn.isBoolean()) {
            Object[] result = new Object[2];
//            result[0]=new double[gnnattr.length];

            return result;

        } else {
            Double[][] result = new Double[2][];
            double value;

            // only val no gradient computed
            if (valonly) {
                OneStrucData onsd = new OneStrucData(A.getmydata().copy());
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
                }
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
                                    result[0] = new Double[]{Double.NaN};
                                    return result;
                                }
                            }
                        }
                    }
                    result[0] = this.evaluateInputGraph(cpmGnn, sampledRel, num_nodes);
                } catch (RBNIllegalArgumentException e) {
                    throw new RuntimeException(e);
                }
            }
            return result;
        }
    }

    // for Gradient Graph
    public Double[] GGevaluate_gnn(RelStruc A, GradientGraphO gg, CPMGnn cpmGnn, GGCPMGnn ggcpmGnn) {
        if (ggcpmGnn.value() != null) {
            return ggcpmGnn.value();
        }

        if (this.sharedInterpreter == null) {
            throw new NullPointerException("GnnPy object null!");
        } else {
            // this first part set the x and edge_index object once only if predefined
            if (cpmGnn.parentRels().isEmpty() && Objects.equals(GGx, "")) {
                GGx = this.stringifyGnnFeatures(GGnumNodes, GGsampledRel, cpmGnn.getGnnattr(), cpmGnn.isOneHotEncoding());
            }
            if (GGedge_pred && Objects.equals(GGedge_index, "")) {
                for (BoolRel element : GGboolRel) {
                    if (GGsampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
                        GGedge_index = "";
                        break;
                    } else {
                        if (Objects.equals(element.name(), cpmGnn.getEdge_name())) {
                            GGedge_pred = true;
                            if (Objects.equals(cpmGnn.getEdge_direction(), "ABBA"))
                                GGedge_index = this.stringifyGnnEdgesABBA(GGsampledRel, element);
                            if (Objects.equals(cpmGnn.getEdge_direction(), "AB"))
                                GGedge_index = this.stringifyGnnEdgesAB(GGsampledRel, element);
                            if (Objects.equals(cpmGnn.getEdge_direction(), "BA"))
                                GGedge_index = this.stringifyGnnEdgesBA(GGsampledRel, element);
                            break;
                        }
                    }
                }
            }
            // else this
            try {
                for (Rel parent : cpmGnn.parentRels()) {
                    int[][] mat = A.allTypedTuples(parent.getTypes());
                    for (int i = 0; i < mat.length; i++) {
                        // find the nodes that have no values (-1) and assign the values form the currentInst in the maxIndicator to the sampledRel
                        if (Objects.equals(GGsampledRel.getData().find(parent).dv(), "?")) {
//                            if (sampledRel.truthValueOf(parent, mat[i]) == -1) {
                            GroundAtom myatom = new GroundAtom(parent, mat[i]);
                            GGAtomMaxNode currentMaxNode = gg.findInMaxindicators(myatom); // I don't like this so much, I keep now for greediness
                            boolean sampledVal = false;
                            if (currentMaxNode.getCurrentInst() == 1)
                                sampledVal = true;
                            GGsampledRel.getmydata().findInBoolRel(parent).add(mat[i], sampledVal);
                        }
                    }
                }
                if (!cpmGnn.parentRels().isEmpty())
                    GGx = this.stringifyGnnFeatures(GGnumNodes, GGsampledRel, cpmGnn.getGnnattr(), cpmGnn.isOneHotEncoding());
            } catch (RBNIllegalArgumentException e) {
                throw new RuntimeException(e);
            }

            // take only the "edge" relation
            // this can be later changed to a more general approach
            // find the boolean relations that should represent edges
            double result;
            if (!GGedge_pred) {
                GGboolRel = GGsampledRel.getBoolBinaryRelations();
                for (BoolRel element : GGboolRel) {
                    if (GGsampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
                        GGedge_index = "";
                        break;
                    } else {
                        if (Objects.equals(element.name(), cpmGnn.getEdge_name())) {
                            if (Objects.equals(cpmGnn.getEdge_direction(), "ABBA"))
                                GGedge_index = this.stringifyGnnEdgesABBA(GGsampledRel, element);
                            if (Objects.equals(cpmGnn.getEdge_direction(), "AB"))
                                GGedge_index = this.stringifyGnnEdgesAB(GGsampledRel, element);
                            if (Objects.equals(cpmGnn.getEdge_direction(), "BA"))
                                GGedge_index = this.stringifyGnnEdgesBA(GGsampledRel, element);
                            break;
                        }
                    }
                }
            }

            Double[] value = new Double[0];
            if (Objects.equals(cpmGnn.getGnn_inference(), "node"))
                value = this.inferModelNodeDouble(Integer.parseInt(cpmGnn.getArgument()), GGx, GGedge_index, cpmGnn.getIdGnn(), "", true);
            else if (Objects.equals(cpmGnn.getGnn_inference(), "graph")) {
//                value = this.inferModelGraphDouble(cpmGnn.getClassId(), GGx, GGedge_index, cpmGnn.getIdGnn(),"");
            } else
                throw new IllegalArgumentException("not valid keyword used: " + cpmGnn.getGnn_inference());

//            if (this.isuga()) {
//                int iv = this.instval(); // Can only be 0,1, or -1, because if a relation is defined by ProbFormCombFunc
//                // it can only be Boolean
//                if (iv == -1)
//                    System.out.println("Warning: undefined instantiation value in GGCombFuncNode.evaluate()");
//                if (iv == 0)
//                    this.value = 1 - this.value;
//            }

//            ggcpmGnn.setValue(value);
            return value;
        }
    }

    public void saveGnnData(CPMGnn cpmGnn, RelStruc A, OneStrucData inst) {
        if (GGonsd == null && GGsampledRel == null) {
            GGonsd = new OneStrucData(A.getmydata().copy()); // only one copy per time
            GGsampledRel = new SparseRelStruc(A.getNames(), GGonsd, A.getCoords(), A.signature());
            GGsampledRel.getmydata().add(inst.copy());
        }
        GGx = "";

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
        GGedge_index = "";
        GGedge_pred = false;
        for (BoolRel element : GGboolRel) {
            if (element.ispredefined()) {
                if (Objects.equals(element.name(), cpmGnn.getEdge_name()))
                    GGedge_pred = true;
            }
        }
    }

    public Double evalSample_gnn(CPMGnn cpmGnn, RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst) {
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
        String edge_index = "";
        for (BoolRel element : boolrel) {
            if (Objects.equals(element.name(), cpmGnn.getEdge_name())) {
                if (Objects.equals(cpmGnn.getEdge_direction(), "ABBA"))
                    edge_index =  this.stringifyGnnEdgesABBA(sampledRel, element);
                if (Objects.equals(cpmGnn.getEdge_direction(), "AB"))
                    edge_index =  this.stringifyGnnEdgesAB(sampledRel, element);
                if (Objects.equals(cpmGnn.getEdge_direction(), "BA"))
                    edge_index =  this.stringifyGnnEdgesBA(sampledRel, element);
                break;
            }
        }

        String x = this.stringifyGnnFeatures(num_features, sampledRel, cpmGnn.getGnnattr(), cpmGnn.isOneHotEncoding());
        return this.inferModelNodeDouble(Integer.parseInt(cpmGnn.getArgument()), x, edge_index, cpmGnn.getIdGnn(), "", false);
    }

}


