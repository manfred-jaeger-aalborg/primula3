/**
 * This class should be used as an interface between PyTorch models and Java
 */
package PyManager;
import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.*;
import RBNgui.Primula;
import RBNinference.PFNetworkNode;
import RBNpackage.*;
import RBNutilities.Pair;
import jep.*;
import jep.python.PyObject;

import java.io.*;
import java.util.*;

public class GnnPy {
    // We assume that the forward method for the gnn have always firm like this: forward(self, x, edge_index, ...)
    // x and edge_index are necessary, ot
    // hers arguments like batch can be set as None
    private final String INFER_GNN = "forward"; // default function name to call in python
    private String scriptPath;
    private String moduleName;
    private String pythonHome;
    private SharedInterpreter sharedInterpreter;
    private static ThreadLocal<SharedInterpreter> threadSharedInterp = new ThreadLocal<>();
    private double[][] currentResult;
    private Map<String, double[][]> currentXdict;
    private Map<String, ArrayList<ArrayList<Integer>>> currentEdgeDict;
    private String lastId;
    private long dimOut;
    private int numClass;
    private ArrayList<String> gnnModelsId;
    private OneStrucData GGonsd;
    private SparseRelStruc GGsampledRel;
    private Vector<BoolRel> GGboolRel;
    private Map<String, double[][]> xDict;
    private Map<String, ArrayList<ArrayList<Integer>>> edgeDict;
    private Map<String, double[][]> GGxDict;
    private Map<String, ArrayList<ArrayList<Integer>>> GGedgeDict;
    Map<Integer, Integer> nodeMap;
    private int GGnumNodes;
    private Map<Rel, int[][]> GGNodesDict;
    private SparseRelStruc sampledRelGobal;
    private boolean changedUpdate;
    private Primula myprimula;
    private GradientGraphO mygg;
    private TorchModelWrapper torchModel;
    Map<String, Object> torchModels;
    private CatGnn currentCatGnn;
    private boolean savedData;
    private OneStrucData oldInst;
    public GnnPy(String scriptPath, String scriptName, String pythonHome) throws IOException {
        this.scriptPath = scriptPath;
        this.moduleName = scriptName;
        this.pythonHome = pythonHome;
        this.gnnModelsId = new ArrayList<>();
        this.numClass = -1;
        this.dimOut = -1;
        currentXdict = new Hashtable<>();
        currentEdgeDict = new Hashtable<>();
        GGNodesDict = new Hashtable<>();
        GGedgeDict = new Hashtable<>();
        xDict = new Hashtable<>();
        edgeDict = new Hashtable<>();
        changedUpdate = false;
        sharedInterpreter = null;
        // pip install jep in a miniconda env (torch)
//        initializeJep();
//        sharedInterpreter = threadSharedInterp.get();
//        sharedInterpreter.exec("data = HeteroData()");
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            handleShutdown();
//        }));
    }

    public GnnPy(Primula primula) throws IOException {
        this.scriptPath = primula.getScriptPath();
        this.moduleName = primula.getScriptName();
        this.pythonHome = primula.getPythonHome();
        myprimula = primula;
        this.gnnModelsId = new ArrayList<>();
        this.numClass = -1;
        this.dimOut = -1;
        currentXdict = new Hashtable<>();
        currentEdgeDict = new Hashtable<>();
        GGNodesDict = new Hashtable<>();
        GGedgeDict = new Hashtable<>();
        xDict = new Hashtable<>();
        edgeDict = new Hashtable<>();
        changedUpdate = false;
        sharedInterpreter = null;
        // pip install jep in a miniconda env (torch)
//        initializeJep();
//        sharedInterpreter = threadSharedInterp.get();
//        sharedInterpreter.exec("data = HeteroData()");
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            handleShutdown();
//        }));
    }

    public GnnPy(Primula primula, GradientGraphO gg) throws IOException {
        this.scriptPath = primula.getScriptPath();
        this.moduleName = primula.getScriptName();
        this.pythonHome = primula.getPythonHome();
        myprimula = primula;
        mygg = gg;
        this.gnnModelsId = new ArrayList<>();
        this.numClass = -1;
        this.dimOut = -1;
        currentXdict = new Hashtable<>();
        currentEdgeDict = new Hashtable<>();
        GGNodesDict = new Hashtable<>();
        GGedgeDict = new Hashtable<>();
        xDict = new Hashtable<>();
        edgeDict = new Hashtable<>();
        changedUpdate = false;
        sharedInterpreter = null;
        // pip install jep in a miniconda env (torch)
//        initializeJep();
//        sharedInterpreter = threadSharedInterp.get();
//        sharedInterpreter.exec("data = HeteroData()");
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            handleShutdown();
//        }));
    }

    public GnnPy(CatGnn catGnn, String configModelPath) {
        scriptPath = configModelPath;
        currentCatGnn = catGnn;

        gnnModelsId = new ArrayList<>();
        numClass = -1;
        dimOut = -1;
        currentXdict = new Hashtable<>();
        currentEdgeDict = new Hashtable<>();
        GGNodesDict = new Hashtable<>();
        GGedgeDict = new Hashtable<>();
        xDict = new Hashtable<>();
        edgeDict = new Hashtable<>();
        changedUpdate = false;
        sharedInterpreter = null;
        savedData = false;
        nodeMap = new HashMap<>();
        //            JepManager.initializeJep();
        JepManager.addShutdownHook();
        torchModel = loadTorchModel(JepManager.getInterpreter(true), catGnn, configModelPath);
    }

//    public GnnPy() {
////        this.scriptName = "config_torch.py";
////        this.gnnModelsId = new ArrayList<>();
//        this.numClass = -1;
//        this.dimOut = -1;
//        currentXdict = new Hashtable<>();
//        currentEdgeDict = new Hashtable<>();
//        GGNodesDict = new Hashtable<>();
//        GGedgeDict = new Hashtable<>();
//        xDict = new Hashtable<>();
//        edgeDict = new Hashtable<>();
//        changedUpdate = false;
//        sharedInterpreter = null;
//        this.torchModels = new HashMap<>();
//        // pip install jep in a miniconda env (torch)
//        try {
//            initializeJep();
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                handleShutdown();
//            }));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public void load_gnn_set(Map<String, Object> sett) {
        if (!sett.isEmpty()) {
            SharedInterpreter interpreter = threadSharedInterp.get();
            if (interpreter != null) {
                interpreter.set("gnn_set_dict", sett);
                interpreter.exec("dat = intt.set_vars(gnn_set_dict)");
            }
        }
    }

    public TorchModelWrapper loadTorchModel(SharedInterpreter interp, CatGnn catGnn, String configFile) {
        try {
            String modelName = "py_model_" + catGnn.getGnnId();
            interp.exec("sys.path.append('" + configFile + "')");
            interp.exec("import " + catGnn.getGnnId() + " as " + catGnn.getGnnId() + "_module");
            interp.exec(modelName + ", n_layers = " + catGnn.getGnnId() + "_module.load_model()");
            Number num_layer = (Number) interp.getValue("n_layers");
            interp.exec("model_class_name = type(" + modelName + ").__name__");
            String modelClassName = interp.getValue("model_class_name").toString();

            int layers = (num_layer != null) ? num_layer.intValue() : -1;
            return new TorchModelWrapper(modelName, layers, modelClassName, interp);
        } catch (RuntimeException e) {
            System.err.println("Error loading torch model: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if the model is already instantiated
     * Each gnn is associated with an id
     * @param id of the model
     */
    private void createModelIfNull(String id) {
        threadSharedInterp.get().exec("from config_torch import load_model");
        threadSharedInterp.get().exec(id + ", mod_layers = load_model(" + id + ")");
        threadSharedInterp.get().exec("models_primua[(\"" + id + "\", mod_layers)]");

        if (this.gnnModelsId != null) {
            if (!this.gnnModelsId.contains(id)) {
                threadSharedInterp.get().exec(id + " = use_model(\"" + id + "\")");
                this.gnnModelsId.add(id);
            }
        } else {
            this.gnnModelsId = new ArrayList<>();
            threadSharedInterp.get().exec(id + " = use_model(\"" + id + "\")");
            this.gnnModelsId.add(id);
        }
    }

    // if node is set to -1, we perform graph classification (arity 0)
    public double[] inferModel(int node, Map<String, double[][]> x_dict, Map<String, ArrayList<ArrayList<Integer>>> edge_dict, String idGnn) {
        assert threadSharedInterp.get() != null;
        this.createModelIfNull(idGnn);
        int currentNode = 0;
        if (node != -1)
            currentNode = node;

        try {
            // check if there is already computed the results for the specific node in the result matrix,
            // otherwise compute for all nodes with one forward propagation
            // needs also to have the same id as before
            if (checkValuesDictCache(x_dict, edge_dict, idGnn))
                return currentResult[currentNode];
            currentXdict = x_dict;
            currentEdgeDict = edge_dict;
            lastId = idGnn;
            changedUpdate = false;

            threadSharedInterp.get().set("java_map_x", currentXdict);
            String keyX = currentXdict.entrySet().iterator().next().getKey(); // in this case the dictionary should have only one key
            threadSharedInterp.get().set("java_map_edge", currentEdgeDict);
            threadSharedInterp.get().exec("xi = torch.as_tensor(java_map_x['" + keyX + "'], dtype=torch.float32)"); // TODO maybe this key can be more general (like take just the first element in the dict)
            if (!edge_dict.isEmpty())
                threadSharedInterp.get().exec("ei = torch.as_tensor(java_map_edge['edge'], dtype=torch.long)");
            else
                threadSharedInterp.get().exec("ei = torch.empty((2, 0), dtype=torch.long)");

            threadSharedInterp.get().eval("out = intt." + this.INFER_GNN + "(" + idGnn + ", xi, ei)");

            threadSharedInterp.get().exec("out_size = len(out.shape)");

            if (this.numClass == -1) {
                threadSharedInterp.get().exec("class_num = out.shape[1] if out_size == 2 else 1");
                long nc = (Long) threadSharedInterp.get().getValue("class_num");
                this.numClass = (int) nc;
            }

            this.dimOut = (Long) threadSharedInterp.get().getValue("out_size");
            if (dimOut == 1) {
                threadSharedInterp.get().exec("out = out.detach().numpy()");
                NDArray ndArray = (NDArray) threadSharedInterp.get().getValue("out");
                float[] tarr = (float[]) ndArray.getData();
                double[] res = new double[2];
                res[0] = Double.valueOf(tarr[0]);
                res[1] = Double.valueOf(tarr[1]);
                return res;
            } else if (dimOut == 2) {
                threadSharedInterp.get().exec("out = out.detach().numpy()");
                NDArray ndArray = (NDArray) threadSharedInterp.get().getValue("out");
                float[] tarr = (float[]) ndArray.getData();
                currentResult = PyUtils.convertTo2D(tarr, ndArray.getDimensions()[0], ndArray.getDimensions()[1]);
                if (currentNode < 0 || currentNode >= currentResult.length)
                    throw new IndexOutOfBoundsException("Index out of bounds for node " + currentNode);
                return currentResult[currentNode];
            }
            return null;
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    public double[] inferModelHetero(int node, Map<String, double[][]> x_dict, Map<String, ArrayList<ArrayList<Integer>>> edge_dict, String idGnn) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath); // update the model if they differ with interpreters
        int currentNode = (node != -1) ? node : 0;

        try {
            // check if there is already computed the results for the specific node in the result matrix,
            // otherwise compute for all nodes with one forward propagation
            // needs also to have the same id as before
            if (checkValuesDictCache(x_dict, edge_dict, idGnn))
                return currentResult[currentNode];
            currentXdict = x_dict;
            currentEdgeDict = edge_dict;
            lastId = idGnn;
            changedUpdate = false;

            currentResult = torchModel.forward(interpreter, currentXdict, currentEdgeDict);
            return currentResult[currentNode];
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    private boolean checkValuesDictCache(Map<String, double[][]> x_dict, Map<String, ArrayList<ArrayList<Integer>>> edge_dict, String idGnn) {
        if (changedUpdate)
            return false;

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
                if (!currentEdgeDict.get(key).equals(edge_dict.get(key))) {
                    currentResult = null;
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public double[] getData(PyObject out){
        assert threadSharedInterp.get() != null;
        try {
            threadSharedInterp.get().set("out_np", out);
            threadSharedInterp.get().exec("torch.tensor(X_before, dtype=torch.float32");
            threadSharedInterp.get().exec("out_np_np = np.array(out_np)");
            // Retrieve the numerical values directly as a Java array
            return (double[]) threadSharedInterp.get().getValue("out_np_np");
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

    private static double[][] createOneHotEncodingMatrix(int num_nodes, int num_columns) {
        double[][] node_bool = new double[num_nodes][num_columns];
        // initialize with all false (i.e. [0])
        for (double[] ints : node_bool) Arrays.fill(ints, 0);
        return node_bool;
    }

    public static Map<Rel, int[][]> constructNodesDict(CatGnn cpmGnn, RelStruc A) {
        // Dictionary with the name of the rel as key, and the nodes. (KEY DO NOT DIFFER WITH TYPE)
        Map<Rel, int[][]> nodesDict = new Hashtable<>();
        for (Pair<BoolRel, ArrayList<Rel>> pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = pair.getSecond();
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

    // Return the nodes for each relation
    // starting from 0 for each Rel
    public static Map<Integer, Integer> constructNodesDictMap(CatGnn cpmGnn, RelStruc A) {
        Map<Integer, Integer> nodesMap = new Hashtable<>();
        for (Pair<BoolRel, ArrayList<Rel>> pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = pair.getSecond();
            for (Rel rel : subList) {
                int nodeIdx = 0;
                try {
                    int[][] mat = A.allTypedTuples(rel.getTypes());
                    for (int[] node: mat) {
                        if (node.length > 0) {
                            nodesMap.put(node[0], nodeIdx);
                            nodeIdx++;
                        }
                    }
                } catch (RBNIllegalArgumentException e) {
                    throw new RuntimeException("Error in saveGnnData for features creation: " + e);
                }
            }
        }
        return nodesMap;
    }

    public Object[] evaluate_gnnHetero(RelStruc A, OneStrucData inst, CatGnn cpmGnn, boolean valonly) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        // mode torch model to the new interpreter
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath);
        Object[] result = new Object[2];
        CatGnn cpmHetero = (CatGnn) cpmGnn;
        result[0] = new double[((CatGnn) cpmGnn).numvals()];

        // only val no gradient computed
        if (valonly) {
            if (oldInst == null || inst.containsAll(oldInst)) { // if the inst is different from the prior inst used, reconstruct
                OneStrucData onsd = new OneStrucData(A.getmydata().copy()); // maybe avoid using copy...
                sampledRelGobal = new SparseRelStruc(A.getNames(), onsd, A.getCoords(), A.signature());
                sampledRelGobal.getmydata().add(inst.copy());
                System.out.println(nodeMap);
                xDict = new Hashtable<>();
                edgeDict = new Hashtable<>();
            }

            if (GGboolRel == null)
                GGboolRel = sampledRelGobal.getBoolBinaryRelations();
            if (GGNodesDict.isEmpty())
                GGNodesDict = constructNodesDict(cpmHetero, A);
            if (nodeMap.isEmpty())
                nodeMap = constructNodesDictMap(cpmHetero, A);

            Map<String, double[][]> x_dict = inputAttrToDict(cpmHetero, nodeMap, GGNodesDict, sampledRelGobal);
            Map<String, ArrayList<ArrayList<Integer>>> edge_dict = edgesToDict(GGboolRel, sampledRelGobal, nodeMap);
            if (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))
                result[0] = inferModelHetero(-1, x_dict, edge_dict, cpmGnn.getGnnId());
            else
                result[0] = inferModelHetero(Integer.parseInt(cpmGnn.getArgument()), x_dict, edge_dict, cpmGnn.getGnnId());
        } else {
            throw new RuntimeException("GRADIENT IN EVALUATION NOT IMPLEMENTED FOR GNN-RBN!");
        }
        oldInst = inst;
        return result;
    }

    public static Map<String, double[][]> inputAttrToDict(CatGnn cpmGnn, Map<Integer, Integer> nodeMap, Map<Rel, int[][]> GGNodesDict, SparseRelStruc sampledRel) {
        Map<String, double[][]> x_dict = new Hashtable<>();
        for (Pair<BoolRel, ArrayList<Rel>> pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = pair.getSecond();
            for (int j = 0; j < subList.size() - 1; j++) {
                // check if all the types in the subList are the same
                if (!subList.get(j).getTypes()[0].getName().equals(subList.get(j + 1).getTypes()[0].getName())) {
                    throw new RuntimeException("Types of the relations do not match!");
                }
            }
            String key = subList.get(0).getTypes()[0].getName();
            x_dict.put(key, createTensorMatrix(subList, nodeMap, GGNodesDict, sampledRel));
        }
        return x_dict;
    }

    private static ArrayList<ArrayList<Integer>> createEdgeArray(TreeSet<int[]> edges_list, Map<Integer,Integer> nodeMap) {
        ArrayList<ArrayList<Integer>> arrays = new ArrayList<>();
        arrays.add(new ArrayList<>());
        arrays.add(new ArrayList<>());
        for (int[] edge : edges_list) {
            arrays.get(0).add(nodeMap.get(edge[0]));
            arrays.get(1).add(nodeMap.get(edge[1]));
        }
        return arrays;
    }

    public static Map<String, ArrayList<ArrayList<Integer>>> edgesToDict(Vector<BoolRel> GGboolRel, SparseRelStruc sampledRel, Map<Integer,Integer> nodeMap) {
        Map<String, ArrayList<ArrayList<Integer>>> edge_dict = new Hashtable<>();
        for (BoolRel element : GGboolRel) {
            if (sampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
                edge_dict.put(element.name(), new ArrayList<>());
            } else {
                OneBoolRelData edgeinst = (OneBoolRelData) sampledRel.getmydata().find(element);
                TreeSet<int[]> edges_list = edgeinst.allTrue();
                edge_dict.put(element.name(), createEdgeArray(edges_list, nodeMap));
            }
        }
        return edge_dict;
    }

    // this function works only with predefined rels! all the values in the GG will be set as 0
    // also with instantiated probabilistic rels (are inside the finalre)
    public static double[][] createTensorMatrix(ArrayList<Rel> attributes, Map<Integer, Integer> nodeMap, Map<Rel, int[][]> nodes_dict, SparseRelStruc finalre) {
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
        int idxFeat = 0;

        // find all the rels that each node have, using treemap the entries maintained sorted using the node (key)
        Map<Integer, ArrayList<Rel>> nodeMapRel = new TreeMap<>();
        Map<Rel, OneRelData> relMap = new HashMap<>();
        Map<Rel, Integer> relIndex = new HashMap<>();

        int startIndex = 0;
        for (Rel r: attributes) {
            Vector<int[]> nodeForRel = data.allTrue(r);
            relMap.put(r, data.find(r));

            // for each feature, see where in the vector it starts
            relIndex.put(r, startIndex);
            startIndex += r.numvals();

            for (int[] node: nodeForRel) {
                if (!nodeMapRel.containsKey(node[0])) {
                    nodeMapRel.put(node[0], new ArrayList<Rel>());
                }
                nodeMapRel.get(node[0]).add(r);
            }
        }

        for (Map.Entry<Integer, ArrayList<Rel>> entry : nodeMapRel.entrySet()) {
            Integer currentNode = entry.getKey();
            ArrayList<Rel> nodeRels = entry.getValue();
            idxFeat = 0;
            int rowIndex = nodeMap.get(currentNode);
            // for each node write its row in the feature array
            for (Rel r: nodeRels) {
                if (r instanceof CatRel) {
                    OneCatRelData relData = (OneCatRelData) relMap.get(r);
                    bool_nodes[rowIndex][relData.values.get(new int[]{currentNode}) + relIndex.get(r)] = 1;
                } else {
                    if (r.valtype() == Rel.NUMERIC) {
                        OneNumRelData num_data = (OneNumRelData) relMap.get(r);
                        bool_nodes[rowIndex][relIndex.get(r)] = num_data.valueOf(new int[]{currentNode});
                    } else {
                        Vector<int[]> featureTrueData = data.allTrue(r);
                        Vector<Vector<int[]>> allTrueData = new Vector<>();
                        allTrueData.add(featureTrueData);
                        for (Vector<int[]> feature : allTrueData) {
                            for (int[] node : feature) {
                                bool_nodes[rowIndex][relIndex.get(r)] = 1;
                            }
                        }
                    }
                }
            }
        }
        return bool_nodes;
    }

    // initialize the input matrix with the values of the rels that are predefines otherwise set to 0
    // in sampledRel there are also the inst values!
    public static Map<String, double[][]> initXdict(CatGnn cpmGnn, Map<Rel, int[][]> GGnumNodesDict, Map<Integer, Integer> nodeMap, SparseRelStruc sampledRel) {
        Map<String, double[][]> x_dict = new HashMap<>();
        for (Pair<BoolRel, ArrayList<Rel>> pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = pair.getSecond();
            for (int j = 0; j < subList.size() - 1; j++) {
                // check if all the types in the subList are the same
                if (!subList.get(j).getTypes()[0].getName().equals(subList.get(j + 1).getTypes()[0].getName())) {
                    throw new RuntimeException("Types of the relations do not match! " + subList.get(j).getTypes()[0].getName() + " / " + subList.get(j + 1).getTypes()[0].getName());
                }
            }
            String key = subList.get(0).getTypes()[0].getName();
            double[][] inputXmatrix = createTensorMatrix(subList, nodeMap, GGnumNodesDict, sampledRel);
            x_dict.put(key, inputXmatrix);
        }
        return x_dict;
    }
    public void updateInputDict(Map<String, double[][]> input_dict, Rel rel, CatGnn cpmGnn, Map<Rel, int[][]> GGnumNodesDict, GradientGraphO gg, GGCPMNode ggcpmNode) {
        Vector<GGCPMNode> childred = ggcpmNode.getChildren();
        for (Pair<BoolRel, ArrayList<Rel>> pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = pair.getSecond();
            Rel firstRel = subList.get(0);
            String key = firstRel.getTypes()[0].getName();
            int idxFeat = 0;
            // since they (should) have all the same type, take the first to generate the input matrix
            double[][] inputMatrix = input_dict.get(key);
            for (Rel subRel: subList) {
                if (rel.equals(subRel)) {
                    int[][] nodes = GGnumNodesDict.get(rel);
                    int min_node = nodes[0][0]; // the first node should be the first in the array
                    for (GGCPMNode node : childred) {
                        if (((GGAtomNode) node).myatom().rel().equals(rel)) {
                            GGAtomMaxNode maxNode = (GGAtomMaxNode) node;
                            if (maxNode.getmapInstVal() == -1) { // if the value is not in the evidence
                                int arg = maxNode.myatom().args[0];
                                int value = maxNode.getCurrentInst();
                                if (subRel instanceof CatRel)
                                    inputMatrix[arg - min_node][value + idxFeat] = 1;
                                else // numeric values should not be here
                                    inputMatrix[arg - min_node][idxFeat] = 1;
                            }
                        }
                    }
                    if (subRel instanceof CatRel)
                        idxFeat += subRel.numvals();
                    else
                        idxFeat++;
                }
            }
        }
    }
    public void updateInputDict2(Map<String, double[][]> input_dict,  Map<Rel, int[][]> GGnumNodesDict, CatGnn cpmGnn, GGCPMNode ggcpmNode) {
        Vector<GGCPMNode> children = ggcpmNode.getChildren();
        // collect all the nodes of the GNN
        Set<GGCPMNode> uniqueChildren = new HashSet<>(children);
        for (GGCPMNode llchild: mygg.getllchildred()) {
            if( llchild instanceof GGGnnNode)
                uniqueChildren.addAll(llchild.getChildren());
        }

        TreeSet<Rel> parentRels = cpmGnn.parentRels();
        for (Pair<BoolRel, ArrayList<Rel>> pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> inputRels = pair.getSecond();
            Rel firstRel = inputRels.get(0);
            String key = firstRel.getTypes()[0].getName(); // should have the same type, we take the first
            int idxFeat = 0;
            double[][] inputMatrix = input_dict.get(key);
            for (Rel subRel: inputRels) {
                if (parentRels.contains(subRel)) {
                    for (GGCPMNode node: uniqueChildren) {
                        GGAtomMaxNode maxNode = (GGAtomMaxNode) node;
                        // if the value is not in the evidence
                        if (maxNode.getmapInstVal() == -1 && maxNode.myatom().rel().equals(subRel)) {
                            int arg = maxNode.myatom().args[0];
                            int value = maxNode.getCurrentInst();
                            if (subRel instanceof CatRel)
                                inputMatrix[nodeMap.get(arg)][value + idxFeat] = 1;
                            else // numeric values should not be here
                                inputMatrix[nodeMap.get(arg)][idxFeat] = 1;
                        }
                    }
                }
                if (subRel instanceof CatRel)
                    idxFeat += subRel.numvals();
                else
                    idxFeat++;
            }
        }
    }

    /*
    Set the current inst by changing only the selected value and not overwrite all the feature matrix
     */
    public void setCurrentInstPy(int currentInst, GGAtomMaxNode currentMaxNode, GGGnnNode parent) {
        if (!GGNodesDict.isEmpty() && !GGxDict.isEmpty() && currentMaxNode.myatom().args.length==1) {
            int nodeToUpdate = currentMaxNode.myatom().args()[0];
            // count the starting position for the feature
            int idxFeat = 0;
            CatGnn cpmGnn = (CatGnn) parent.getCpm();
            for (Pair<BoolRel, ArrayList<Rel>> pair : cpmGnn.getGnnInputs()) {
                idxFeat = 0;
                ArrayList<Rel> inputRels = pair.getSecond();

                for (Rel r: inputRels) {
                    if (currentMaxNode.myatom().rel() instanceof CatRel && r.equals(currentMaxNode.myatom().rel())) {
                        String key = currentMaxNode.myatom().rel().getTypes()[0].getName();
                        for (int i = 0; i < currentMaxNode.myatom().rel().numvals(); i++) {
                            GGxDict.get(key)[nodeMap.get(nodeToUpdate)][i + idxFeat] = 0;
                        }
                        GGxDict.get(key)[nodeMap.get(nodeToUpdate)][currentInst + idxFeat] = 1;
                        changedUpdate = true;
                    } else if (!(currentMaxNode.myatom().rel() instanceof CatRel) && r.equals(currentMaxNode.myatom().rel())) {
                        throw new RuntimeException("Relation " + r.name() + " not supported in setCurrentInstPy!");
                    }
                    idxFeat += r.numvals();
                }
            }
        }
        if (!GGedgeDict.isEmpty() && currentMaxNode.myatom().args.length==2) {
            int[] nodesToUpdate = currentMaxNode.myatom().args();
            // count the starting position for the feature
            Rel edgeRelToUpdate = null;
            CatGnn cpmGnn = (CatGnn) parent.getCpm();
            for (Pair<BoolRel, ArrayList<Rel>> pair : cpmGnn.getGnnInputs()) {
                Rel edgeRel = pair.getFirst();
                if (edgeRel.equals(currentMaxNode.myatom().rel()))
                    edgeRelToUpdate = edgeRel;
            }

            // if found, set to zero the current feature vector corresponding to the rel
            // then, set the value of currentInst
            if (edgeRelToUpdate != null) {
                ArrayList<ArrayList<Integer>> edges = GGedgeDict.get(edgeRelToUpdate.name());
                if (currentInst == 0) {
                    for (int i = 0; i < edges.get(0).size(); i++) {
                        if (edges.get(0).get(i) == nodesToUpdate[0] && edges.get(1).get(i) == nodesToUpdate[1]) {
                            edges.get(0).remove(i);
                            edges.get(1).remove(i);
                            break;
                        }
                    }
                } else {
                    edges.get(0).add(nodesToUpdate[0]);
                    edges.get(1).add(nodesToUpdate[1]);
                }

                changedUpdate = true;
            } else
                throw new RuntimeException("Something went wrong in setCurrentInstPy!");
        }
    }

    public void updateEdgeDict(Map<String, ArrayList<ArrayList<Integer>>> edge_dict, CatGnn cpmGnn,  GGCPMNode ggcpmNode) {
        // at the moment, edge-features are not implemented/supported
        Vector<GGCPMNode> childred = ggcpmNode.getChildren();
        TreeSet<Rel> parentRels = cpmGnn.parentRels();
        for (Pair<BoolRel, ArrayList<Rel>> pair : cpmGnn.getGnnInputs()) {
            Rel edge = pair.getFirst();
            ArrayList<ArrayList<Integer>> edge_index = edge_dict.get(edge.name());
            if (parentRels.contains(edge)) {
                for (GGCPMNode node: childred) {
                    GGAtomMaxNode maxNode = (GGAtomMaxNode) node;
                    if (maxNode.getmapInstVal() == -1 && maxNode.myatom().rel().equals(edge) && maxNode.myatom().args.length == 2) {
//                        System.out.println(maxNode.myatom().rel() + "(" + maxNode.myatom().args[0] + "," +  maxNode.myatom().args[1] + ")");
                        if (maxNode.getCurrentInst() > 0) {
                            edge_index.get(0).add(maxNode.myatom().args[0]);
                            edge_index.get(1).add(maxNode.myatom().args[1]);
                        }
                    }
                }
            }
        }
    }

    public Map<String, ArrayList<ArrayList<Integer>>> initEdgesDict(Vector<BoolRel> GGboolRel, SparseRelStruc sampledRel) {
        Map<String, ArrayList<ArrayList<Integer>>> edge_dict = new HashMap<>();
        for (BoolRel element : GGboolRel) { // maybe check if all the binary rels are the edges?
            OneBoolRelData edgeinst = (OneBoolRelData) sampledRel.getmydata().find(element);
            TreeSet<int[]> edges_list = edgeinst.allTrue();
            ArrayList<ArrayList<Integer>> arrays = createEdgeArray(edges_list, nodeMap);
            edge_dict.put(element.name(), arrays);
        }
        return edge_dict;
    }

    public void resetDict(boolean x, boolean edge) {
        if (x)
            GGxDict = new HashMap<>();
        if (edge)
            GGedgeDict = new HashMap<>();
    }

    public double[] GGevaluate_gnnHetero(RelStruc A, OneStrucData inst, GradientGraphO gg, CatGnn cpmGnn, GGCPMNode ggcpmGnn) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        // mode torch model to the new interpreter
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath);
        CatGnn cpm = (CatGnn) cpmGnn;

        if (!savedData) {
            saveGnnData(cpm, A, inst);
            savedData = true;
        }

        if (GGxDict.isEmpty()) {
            GGxDict = initXdict(cpm, GGNodesDict, nodeMap, GGsampledRel);
            // we need to use the sampled values in the gradient graph structure (maxindicator) and assign them to the rel
            // for GNNs the order of the features need to be respected: the order in input_attr in CatGnn will be used for constructing the vector
            updateInputDict2(GGxDict, GGNodesDict, cpm, ggcpmGnn);
        }
        if (GGedgeDict.isEmpty()) {
            GGedgeDict = initEdgesDict(GGboolRel, GGsampledRel);
            updateEdgeDict(GGedgeDict, cpm, ggcpmGnn);
        }

        if (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))
            return inferModelHetero(-1, GGxDict, GGedgeDict, cpmGnn.getGnnId());
        else
            return inferModelHetero(Integer.parseInt(cpmGnn.getArgument()), GGxDict, GGedgeDict, cpmGnn.getGnnId());
    }

    // Initial caching of the data in GGGnnNode
    public void saveGnnData(CatGnn cpmGnn, RelStruc A, OneStrucData inst) {
        if (GGonsd == null && GGsampledRel == null) {
            GGonsd = new OneStrucData(A.getmydata().copy()); // only one copy per time
            GGsampledRel = new SparseRelStruc(A.getNames(), GGonsd, A.getCoords(), A.signature());
            GGsampledRel.getmydata().add(inst.copy());
        }

        if (cpmGnn instanceof CatGnn) {
            GGNodesDict = constructNodesDict(cpmGnn, A);
            nodeMap = constructNodesDictMap(cpmGnn, A);
            GGxDict = new HashMap<>();
            GGedgeDict = new HashMap<>();
            if (GGboolRel == null)
                GGboolRel = GGsampledRel.getBoolBinaryRelations();
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
        }
    }

    public void updateEdgeDictForSampling(Map<String, ArrayList<ArrayList<Integer>>> edge_dict, CatGnn cpmGnn, Hashtable<String, PFNetworkNode> atomhasht) {
        // at the moment, edge-features are not implemented/supported
//        Vector<GGCPMNode> childred = ggcpmNode.getChildren();
//        TreeSet<Rel> parentRels = cpmGnn.parentRels();
//        for (Rel edge: cpmGnn.getEdge_attr()) {
//            ArrayList<ArrayList<Integer>> edge_index = edge_dict.get(edge.name());
//            if (parentRels.contains(edge)) {
//                for (GGCPMNode node: childred) {
//                    GGAtomMaxNode maxNode = (GGAtomMaxNode) node;
//                    if (maxNode.getmapInstVal() == -1 && maxNode.myatom().rel().equals(edge) && maxNode.myatom().args.length == 2) {
////                        System.out.println(maxNode.myatom().rel() + "(" + maxNode.myatom().args[0] + "," +  maxNode.myatom().args[1] + ")");
//                        if (maxNode.getCurrentInst() > 0) {
//                            edge_index.get(0).add(maxNode.myatom().args[0]);
//                            edge_index.get(1).add(maxNode.myatom().args[1]);
//                        }
//                    }
//                }
//            }
//        }

//        TreeSet<Rel> parentRels = cpmGnn.parentRels();
//        for (Rel edge: cpmGnn.getEdge_attr()) {
//            ArrayList<ArrayList<Integer>> edge_index = edge_dict.get(edge.name());
//            if (parentRels.contains(edge)) {
//                for (int i = 0; i < edge_index.size(); i++) {
//                    if (GGsampledRel.truthValueOf(edge, Arrays.a edge_index[i]) == -1) {
//                        if (maxNode.getmapInstVal() == -1 && maxNode.myatom().rel().equals(edge) && maxNode.myatom().args.length == 2) {
//                            if (maxNode.getCurrentInst() > 0) {
//                                edge_index.get(0).add(maxNode.myatom().args[0]);
//                                edge_index.get(1).add(maxNode.myatom().args[1]);
//                            }
//                        }
//                    }
//                }
//            }
//        }

    }

    public void updateInputDictForSampling(Map<String, double[][]> input_dict, Map<Rel, int[][]> GGnumNodesDict, CatGnn cpmGnn, Hashtable<String, PFNetworkNode> atomhasht) {
        TreeSet<Rel> parentRels = cpmGnn.parentRels();
        for (Pair<BoolRel, ArrayList<Rel>> pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> inputRels = pair.getSecond();
            Rel firstRel = inputRels.get(0);
            String key = firstRel.getTypes()[0].getName(); // should have the same type, we take the first
            int idxFeat = 0;
            double[][] inputMatrix = input_dict.get(key);
            for (Rel subRel: inputRels) {
                if (parentRels.contains(subRel)) {
                    int[][] nodes = GGnumNodesDict.get(subRel);
                    for (int i = 0; i < nodes.length; i++) {
                        if (GGsampledRel.truthValueOf(subRel, nodes[i]) == -1) {
                            GroundAtom myatom = new GroundAtom(subRel, nodes[i]);
                            PFNetworkNode gan = (PFNetworkNode) atomhasht.get(myatom.asString());
                            if (gan != null) {
                                int value = gan.sampleinstVal();
                                int arg = gan.myatom().args[0];
                                if (subRel instanceof CatRel)
                                    inputMatrix[nodeMap.get(arg)][value + idxFeat] = 1;
                                else
                                    inputMatrix[nodeMap.get(arg)][idxFeat] = 1;
                            }
                        }

                    }
                }
            }
        }
//
//                    for (int i = 0; i < mat.length; i++) {
//                        if (GGsampledRel.truthValueOf(parent, mat[i]) == -1) {
//                            GroundAtom myatom = new GroundAtom(parent, mat[i]);
//                            String myatomname = myatom.asString();
//                            PFNetworkNode gan = (PFNetworkNode) atomhasht.get(myatomname);
//                            if (gan != null) {
//                                double result = (double) gan.sampleinstVal();
//                                boolean sampledVal = false;
////                            GGsampledRel.getmydata().find(parent).
//                                if (result == 1)
//                                    sampledVal = true;
//                                GGsampledRel.getmydata().findInBoolRel(parent).add(mat[i], sampledVal);
//                            }
//                        }
//                    }
//                }
    }

    public double[] evalSample_gnn(CatGnn cpmGnn, RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath); // update the model if they differ with interpreters

        CatGnn cpmHetero = (CatGnn) cpmGnn;

        if (GGxDict.isEmpty()) {
            GGxDict = initXdict(cpmHetero, GGNodesDict, nodeMap, GGsampledRel);
            updateInputDictForSampling(GGxDict, GGNodesDict, cpmHetero, atomhasht);
        }
        if (GGedgeDict.isEmpty()) {
            GGedgeDict = initEdgesDict(GGboolRel, GGsampledRel);
            updateEdgeDictForSampling(GGedgeDict, cpmHetero, atomhasht);
        }

        double[] res = null;
        if (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))
            res = inferModelHetero(-1, GGxDict, GGedgeDict, cpmGnn.getGnnId());
        else
            res = inferModelHetero(Integer.parseInt(cpmGnn.getArgument()), GGxDict, GGedgeDict, cpmGnn.getGnnId());
        return res;
    }

    private void printPython(Interpreter interpreter, String var) {
        StringWriter output = new StringWriter();
        interpreter.set("output", output);
        interpreter.eval("import sys");
        interpreter.eval("sys.stdout = output");
        interpreter.eval("print(" + var + ")");
        System.out.println("Captured output: " + output.toString());
    }

    public Map<String, double[][]> getCurrentXdict() {
        return currentXdict;
    }

    public Map<String, ArrayList<ArrayList<Integer>>> getCurrentEdgeDict() {
        return currentEdgeDict;
    }

    public void setGradientGraph(GradientGraphO mygg) { this.mygg = mygg; }

    public TorchModelWrapper getTorchModel() { return torchModel; }
}


