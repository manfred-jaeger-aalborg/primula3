/**
 * This class should be used as an interface between PyTorch models and Java
 */
package PyManager;
import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.*;
import RBNinference.PFNetworkNode;
import RBNpackage.*;
import jep.*;
import jep.python.PyObject;

import java.io.*;
import java.util.*;

public class GnnPy {
    // We assume that the forward method for the gnn have always firm like this: forward(self, x, edge_index, ...)
    // x and edge_index are necessary, ot
    // hers arguments like batch can be set as None
    private String scriptPath;
    private static ThreadLocal<SharedInterpreter> threadSharedInterp = new ThreadLocal<>();
    private Object[] currentResult;
    private Map<String, double[][]> currentNodeAttrDict;
    private Map<String, ArrayList<ArrayList<Integer>>> currentEdgeDict;
    private Map<String, double[][]> currentEdgeAttrDict;
    private String lastId;
    private OneStrucData GGonsd;
    private SparseRelStruc GGsampledRel;
    private Vector<BoolRel> GGboolRel;
    private Map<String, double[][]> GGnodeAttrDict;
    private Map<String, ArrayList<ArrayList<Integer>>> GGedgeDict;
    private Map<String, double[][]> GGedgeAttrDict;
    Map<Integer, Integer> nodeMap; // Node Rel to Node Index
    private Map<Rel, int[][]> relToNodeMap; // Rels to nodes
    private Map<Rel, Vector<int[]>> relToEdgeAttrMap;
    private SparseRelStruc sampledRelGobal;
    private boolean changedUpdate;
    private GradientGraphO mygg;
    private TorchModelWrapper torchModel;
    private CatGnn currentCatGnn;
    private boolean savedData;
    private OneStrucData oldInst;
    private int currentXdictHash;
    private int currentEdgeDictHash;
    private int currentEdgeAttrDictHash;

    public GnnPy(CatGnn catGnn, String configModelPath) {
        initData();
        scriptPath = configModelPath;
        currentCatGnn = catGnn;
        JepManager.addShutdownHook();
        System.out.println("Loading torch model: " + catGnn.getGnnId() + " from: " + configModelPath + "...");
        long startTime = System.currentTimeMillis();
        torchModel = loadTorchModel(JepManager.getInterpreter(true), catGnn, configModelPath);
        long endTime = System.currentTimeMillis();
        System.out.println("Torch model loaded in " + (endTime - startTime)/1000. + " sec.");
    }

    public void initData() {
        currentNodeAttrDict = new Hashtable<>();
        currentEdgeDict = new Hashtable<>();
        relToNodeMap = new Hashtable<>();
        relToEdgeAttrMap = new Hashtable<>();
        GGedgeDict = new Hashtable<>();
        GGedgeAttrDict = new Hashtable<>();
        nodeMap = new HashMap<>();
        currentXdictHash = 0;
        currentEdgeDictHash = 0;
        currentEdgeAttrDictHash = 0;
        currentResult = null;
        oldInst = null;
        GGonsd = null;
        lastId = null;
        GGsampledRel = null;
        GGboolRel = null;
        changedUpdate = false;
        savedData = false;
    }

    public void load_gnn_set(Map<String, Object> sett) {
        if (!sett.isEmpty()) {
            SharedInterpreter interpreter = threadSharedInterp.get();
            if (interpreter != null) {
                interpreter.set("gnn_set_dict", sett);
                interpreter.exec("dat = intt.set_vars(gnn_set_dict)");
            }
        }
    }

    public TorchModelWrapper loadTorchModel(SharedInterpreter interp, CatGnn catGnn, String configPath) {
        try {
            String gnnId = catGnn.getGnnId();
            String moduleName = gnnId + "_module";
            String modelVar = "py_model_" + gnnId;

            String initScript = String.join("\n",
                    "import sys",
                    "if '" + configPath + "' not in sys.path: sys.path.append('" + configPath + "')",
                    "import " + gnnId + " as " + moduleName,
                    modelVar + " = " + moduleName + ".load_model()",
                    "model_class_name = type(" + modelVar + ").__name__"
            );
            interp.exec(initScript);
            String modelClassName = interp.getValue("model_class_name").toString();
            return new TorchModelWrapper(modelVar, modelClassName, interp);
        } catch (JepException e) {
            System.err.println("Error loading torch model: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Object[] inferModelHetero(Map<String, double[][]> x_dict, Map<String, ArrayList<ArrayList<Integer>>> edge_dict, Map<String, double[][]> edge_attr,
                                     List<TorchInputSpecs> gnnInputs, String idGnn, boolean valonly) {

        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath);

        try {
            // Quick hash-based check first
            if (checkValuesDictCache(x_dict, edge_dict, edge_attr, idGnn))
                return currentResult;

            // Update cache
            updateCache(x_dict, edge_dict, edge_attr, idGnn);

            currentResult = torchModel.forward(currentNodeAttrDict, currentEdgeDict, currentEdgeAttrDict, gnnInputs, !valonly);
            return currentResult;
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    private boolean checkValuesDictCache(Map<String, double[][]> x_dict,
                                         Map<String, ArrayList<ArrayList<Integer>>> edge_dict,
                                         Map<String, double[][]> edge_attr,
                                         String idGnn) {

        if (changedUpdate || currentNodeAttrDict == null || currentEdgeDict == null || currentEdgeAttrDict == null ||
                currentResult == null || !Objects.equals(lastId, idGnn)) {
            return false;
        }

        if (currentNodeAttrDict.size() != x_dict.size() || currentEdgeDict.size() != edge_dict.size() ||
                currentEdgeAttrDict.size() != edge_attr.size()) {
            return false;
        }

        int newXdictHash = computeMatrixHash(x_dict);
        int newEdgeDictHash = computeListListHash(edge_dict);
        int newEdgeAttrHash = computeMatrixHash(edge_attr);

        // If hashes differ, definitely not equal
        if (currentXdictHash != newXdictHash || currentEdgeDictHash != newEdgeDictHash ||
                currentEdgeAttrDictHash != newEdgeAttrHash) {
            currentResult = null;
            return false;
        }

        // Hashes match, but we need deep comparison to be certain (hash collisions)
        if (!currentNodeAttrDict.keySet().equals(x_dict.keySet())
                || !currentEdgeDict.keySet().equals(edge_dict.keySet())
                || !currentEdgeAttrDict.keySet().equals(edge_attr.keySet())) {
            currentResult = null;
            return false;
        }

        // Deep value comparison (expensive, but only if hashes match)
        for (Map.Entry<String, double[][]> entry : x_dict.entrySet()) {
            if (!Arrays.deepEquals(currentNodeAttrDict.get(entry.getKey()), entry.getValue())) {
                currentResult = null;
                return false;
            }
        }

        for (Map.Entry<String, ArrayList<ArrayList<Integer>>> entry : edge_dict.entrySet()) {
            if (!Objects.equals(currentEdgeDict.get(entry.getKey()), entry.getValue())) {
                currentResult = null;
                return false;
            }
        }


        for (Map.Entry<String, double[][]> entry : edge_attr.entrySet()) {
            if (!Arrays.deepEquals(currentEdgeAttrDict.get(entry.getKey()), entry.getValue())) {
                currentResult = null;
                return false;
            }
        }

        return true;
    }

    private void updateCache(Map<String, double[][]> x_dict, Map<String, ArrayList<ArrayList<Integer>>> edge_dict, Map<String, double[][]> edge_attr, String idGnn) {
        currentNodeAttrDict = x_dict;
        currentEdgeDict = edge_dict;
        currentEdgeAttrDict = edge_attr;
        lastId = idGnn;
        changedUpdate = false;

        // Update hashes
        currentXdictHash = computeMatrixHash(x_dict);
        currentEdgeDictHash = computeListListHash(edge_dict);
        if (edge_attr.size() > 0)
            currentEdgeAttrDictHash = computeMatrixHash(edge_attr);
    }

    private int computeMatrixHash(Map<String, double[][]> x_dict) {
        int hash = 17;
        for (Map.Entry<String, double[][]> entry : x_dict.entrySet()) {
            hash = hash * 31 + entry.getKey().hashCode();
            hash = hash * 31 + Arrays.deepHashCode(entry.getValue());
        }
        return hash;
    }

    private int computeListListHash(Map<String, ArrayList<ArrayList<Integer>>> edge_dict) {
        int hash = 17;
        for (Map.Entry<String, ArrayList<ArrayList<Integer>>> entry : edge_dict.entrySet()) {
            hash = hash * 31 + entry.getKey().hashCode();
            hash = hash * 31 + Objects.hashCode(entry.getValue());
        }
        return hash;
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

    private static double[][] createOneHotEncodingMatrix(int num_nodes, int num_columns) {
        double[][] node_bool = new double[num_nodes][num_columns];
        // initialize with all false (i.e. [0])
        for (double[] ints : node_bool) Arrays.fill(ints, 0);
        return node_bool;
    }

    public static Map<Rel, int[][]> constructNodesDict(CatGnn cpmGnn, RelStruc A) {
        // Dictionary with the name of the rel as key, and the nodes. (KEY DO NOT DIFFER WITH TYPE)
        Map<Rel, int[][]> nodesDict = new Hashtable<>();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getNodeAttributes();
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

    public static Map<Rel, Vector<int[]>> constructEdgeAttrDict(CatGnn cpmGnn, RelStruc A) {
        Map<Rel, Vector<int[]>> edgeDict = new Hashtable<>();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getEdgeAttributes();
            if (subList.size() == 0) continue;
            BoolRel edgeRel = pair.getEdgeRelation(); // take for which edges we can have the attributes
            for (Rel rel : subList) {
                Vector<int[]> allTrue = A.allTrue(edgeRel);
                edgeDict.put(rel, allTrue);
            }
        }
        return edgeDict;
    }

    // Return the nodes for each relation
    // starting from 0 for each Rel
    public static Map<Integer, Integer> constructNodesDictMap(CatGnn cpmGnn, RelStruc A) {
        Map<Integer, Integer> nodesMap = new Hashtable<>();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getNodeAttributes();
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
        result[0] = new double[((CatGnn) cpmGnn).numvals()];

        if (oldInst == null || inst.containsAll(oldInst)) { // if the inst is different from the prior inst used, reconstruct
            OneStrucData onsd = new OneStrucData(A.getmydata().copy()); // maybe avoid using copy...
            sampledRelGobal = new SparseRelStruc(A.getNames(), onsd, A.getCoords(), A.signature());
            sampledRelGobal.getmydata().add(inst.copy());
        }

        if (GGboolRel == null) {
            GGboolRel = new Vector<>();
            for (TorchInputSpecs inps: cpmGnn.getGnnInputs()) {
                GGboolRel.add(inps.getEdgeRelation());
            }
        }
        if (relToNodeMap.isEmpty())
            relToNodeMap = constructNodesDict(cpmGnn, A);
        if (nodeMap.isEmpty())
            nodeMap = constructNodesDictMap(cpmGnn, A);
        if (relToEdgeAttrMap.isEmpty())
            relToEdgeAttrMap = constructEdgeAttrDict(cpmGnn, A);

        Map<String, double[][]> x_dict = inputAttrToDict(cpmGnn, nodeMap, relToNodeMap, sampledRelGobal);
        Map<String, ArrayList<ArrayList<Integer>>> edge_dict = edgesToDict(GGboolRel, sampledRelGobal, nodeMap);
        Map<String, double[][]> edge_attr = initEdgeAttrdict(cpmGnn, relToEdgeAttrMap, sampledRelGobal);

        result = inferModelHetero(x_dict, edge_dict, edge_attr, cpmGnn.getGnnInputs(), cpmGnn.getGnnId(), valonly);
        double[][] outProbs = (double[][]) result[0];
        double[][] outGrads;

        int index = (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals("")) ? 0 : Integer.parseInt(cpmGnn.getArgument());

        Object[] resultCopy = result.clone();
        resultCopy[0] = outProbs[index];
        if (!valonly) {
            outGrads = (double[][]) resultCopy[1];
            resultCopy[1] = outGrads[index];
        }
        oldInst = inst;
        return resultCopy;
    }

    public static Map<String, double[][]> inputAttrToDict(CatGnn cpmGnn, Map<Integer, Integer> nodeMap, Map<Rel, int[][]> GGNodesDict, SparseRelStruc sampledRel) {
        Map<String, double[][]> x_dict = new Hashtable<>();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getNodeAttributes();
            for (int j = 0; j < subList.size() - 1; j++) {
                // check if all the types in the subList are the same
                if (!subList.get(j).getTypesAsString().equals(subList.get(j + 1).getTypesAsString())) {
                    throw new RuntimeException("Types of the relations do not match!");
                }
            }
            String key = subList.get(0).getTypesAsString();
            x_dict.put(key, createNodeTensorMatrix(subList, nodeMap, GGNodesDict, sampledRel, cpmGnn.isOneHotEncoding()));
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
    public static double[][] createNodeTensorMatrix(ArrayList<Rel> attributes, Map<Integer, Integer> nodeMap, Map<Rel, int[][]> nodes_dict, SparseRelStruc finalre, boolean oneHot) {
        int num_col = 0;
        int num_nodes = nodes_dict.get(attributes.get(0)).length; // take the first (they should have all the same dimension)
        // count how many columns the matrix will have
        for (Rel r : attributes) {
            if (r instanceof CatRel && oneHot)
                num_col += r.numvals();
            else
                num_col += 1;
        }
        double[][] bool_nodes = createOneHotEncodingMatrix(num_nodes, num_col);
        OneStrucData data = finalre.getmydata();

        // find all the rels that each node has, using treemap the entries maintained sorted using the node (key)
        Map<Integer, ArrayList<Rel>> nodeMapRel = new TreeMap<>();
        Map<Rel, OneRelData> relMap = new HashMap<>();
        Map<Rel, Integer> relIndex = new HashMap<>();

        int startIndex = 0;

        for (Rel r: attributes) {
            int[][] nodes = nodes_dict.get(r);
            for (int[] node: nodes) {
                if (node.length > 0) {
                    if (!nodeMapRel.containsKey(node[0]))
                        nodeMapRel.put(node[0], new ArrayList<Rel>());
                    nodeMapRel.get(node[0]).add(r);
                }
            }

            relMap.put(r, data.find(r));

            // for each feature, see where in the vector it starts
            relIndex.put(r, startIndex);
            if (oneHot && r instanceof CatRel)
                startIndex += r.numvals();
            else
                startIndex++;
        }

        // It can happen that, for some relations, not all the nodes for the gnn input will not be filled in the bool_nodes
        // this will leave part of the matrix with 0. If the GNN is defined properly in the rbn, this should not change the results
        // because the incomplete nodes should not be dependent for the atom we are querying
        for (Map.Entry<Integer, ArrayList<Rel>> entry : nodeMapRel.entrySet()) {
            Integer currentNode = entry.getKey();
            ArrayList<Rel> nodeRels = entry.getValue();
            int rowIndex = nodeMap.get(currentNode);
            // for each node write its row in the feature array
            for (Rel r: nodeRels) {
                if (r instanceof CatRel) {
                    OneCatRelData relData = (OneCatRelData) relMap.get(r);
                    int[] nodeKey = new int[]{currentNode};
                    if (relData.values.containsKey(nodeKey)) {
                        if (oneHot)
                            bool_nodes[rowIndex][relData.values.get(nodeKey) + relIndex.get(r)] = 1;
                        else
                            bool_nodes[rowIndex][relIndex.get(r)] = relData.values.get(nodeKey);
                    }
//                    else {
//                        System.err.println("Warning: " + r.name() + " and node " + currentNode + " not found in the data");
//                    }
                } else {
                    if (r.valtype() == Rel.NUMERIC) {
                        OneNumRelData num_data = (OneNumRelData) relMap.get(r);
                        bool_nodes[rowIndex][relIndex.get(r)] = num_data.valueOf(new int[]{currentNode});
                    } else if (r.valtype() == Rel.BOOLEAN) {
                        OneBoolRelData num_data = (OneBoolRelData) relMap.get(r);
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

    public static double[][] createEdgeAttrMatrix(ArrayList<Rel> attributes,
                                                  Map<Rel, Vector<int[]>> edge_dict,
                                                  SparseRelStruc finalre,
                                                  boolean oneHot) {
        if (attributes == null || attributes.size() == 0)
            return new double[0][0];

        // ordering of unique edges (tuple -> int[]), preserving first-seen order
        Map<String, int[]> edgeKeyToTuple = new LinkedHashMap<>();
        for (Rel r : attributes) {
            Vector<int[]> vec = edge_dict.get(r);
            if (vec == null) continue;
            for (int i = 0; i < vec.size(); i++) {
                int[] tup = vec.get(i);
                if (tup == null || tup.length == 0) continue;
                String key = Arrays.toString(tup);
                if (!edgeKeyToTuple.containsKey(key)) {
                    edgeKeyToTuple.put(key, tup);
                }
            }
        }

        int num_edges = edgeKeyToTuple.size();

        // compute number of columns
        int num_col = 0;
        for (Rel r : attributes) {
            if (r instanceof CatRel && oneHot)
                num_col += r.numvals();
            else
                num_col += 1;
        }

        double[][] edge_features = createOneHotEncodingMatrix(num_edges, num_col);
        OneStrucData data = finalre.getmydata();

        // prepare maps similar to node version
        Map<Rel, OneRelData> relMap = new HashMap<>();
        Map<Rel, Integer> relIndex = new HashMap<>();

        int startIndex = 0;
        for (Rel r : attributes) {
            relMap.put(r, data.find(r));
            relIndex.put(r, startIndex);
            if (oneHot && r instanceof CatRel)
                startIndex += r.numvals();
            else
                startIndex++;
        }
        System.out.println(relMap);
//
        // create edge -> row index mapping
        Map<String, Integer> edgeToRow = new HashMap<>();
        int rowIdx = 0;
        for (String key : edgeKeyToTuple.keySet()) {
            edgeToRow.put(key, rowIdx++);
        }

        // Fill the matrix: for each relation fill values for every edge row (if present)
        for (Rel r : attributes) {
            OneRelData ore = relMap.get(r);
            if (ore == null) continue; // no data for this rel

            if (r instanceof CatRel) {
                OneCatRelData crel = (OneCatRelData) ore;
                for (Map.Entry<String, int[]> entry : edgeKeyToTuple.entrySet()) {
                    String key = entry.getKey();
                    int[] tup = entry.getValue();
                    Integer row = edgeToRow.get(key);
                    if (row == null) continue;

                    // If the categorical value exists for this tuple, set either one-hot or raw index
                    if (crel.values.containsKey(tup)) {
                        int val = crel.values.get(tup);
                        if (oneHot) {
                            edge_features[row][relIndex.get(r) + val] = 1.0;
                        } else {
                            edge_features[row][relIndex.get(r)] = val;
                        }
                    }
                }
            } else {
                if (r.valtype() == Rel.NUMERIC) {
                    OneNumRelData nrel = (OneNumRelData) ore;
                    for (Map.Entry<String, int[]> entry : edgeKeyToTuple.entrySet()) {
                        String key = entry.getKey();
                        int[] tup = entry.getValue();
                        Integer row = edgeToRow.get(key);
                        if (row == null) continue;
                        edge_features[row][relIndex.get(r)] = nrel.valueOf(tup);
                    }
                } else if (r.valtype() == Rel.BOOLEAN) {
                    OneBoolRelData brel = (OneBoolRelData) ore;
                    for (Map.Entry<String, int[]> entry : edgeKeyToTuple.entrySet()) {
                        String key = entry.getKey();
                        int[] tup = entry.getValue();
                        Integer row = edgeToRow.get(key);
                        if (row == null) continue;
                        edge_features[row][relIndex.get(r)] = brel.valueOf(tup);
                    }
                } else {
                    Vector<int[]> featureTrueData = data.allTrue(r);
                    if (featureTrueData != null) {
                        for (int[] t : featureTrueData) {
                            if (t == null || t.length == 0) continue;
                            String key = Arrays.toString(t);
                            Integer row = edgeToRow.get(key);
                            if (row != null) {
                                edge_features[row][relIndex.get(r)] = 1.0;
                            }
                        }
                    }
                }
            }
        }

        return edge_features;
    }

    // initialize the input matrix with the values of the rels that are predefined otherwise set to 0
    // in sampledRel there are also the inst values!
    public static Map<String, double[][]> initNodeAttrDict(CatGnn cpmGnn, Map<Rel, int[][]> GGnumNodesDict, Map<Integer, Integer> nodeMap, SparseRelStruc sampledRel) {
        Map<String, double[][]> x_dict = new HashMap<>();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getNodeAttributes();
            for (int j = 0; j < subList.size() - 1; j++) {
                // check if all the types in the subList are the same
                if (!subList.get(j).getTypesAsString().equals(subList.get(j + 1).getTypesAsString())) {
                    throw new RuntimeException("Types of the relations do not match! " + subList.get(j).getTypesAsString() + " / " + subList.get(j + 1).getTypesAsString());
                }
            }
            String key = subList.get(0).getTypesAsString();
            double[][] inputXmatrix = createNodeTensorMatrix(subList, nodeMap, GGnumNodesDict, sampledRel, cpmGnn.isOneHotEncoding());
            x_dict.put(key, inputXmatrix);
        }
        return x_dict;
    }

    // if edgeAttrs is true, use to update the edgeAttrs matrix, else for node attributes
    public Map<String, double[][]> updateAttrDict(Map<String, double[][]> inputDict, CatGnn cpmGnn, GGCPMNode ggcpmNode, boolean edgeAttrs) {
        Vector<GGCPMNode> children = ggcpmNode.getChildren();
        // collect all the nodes of the GNN
        Set<GGCPMNode> uniqueChildren = new HashSet<>(children);
        for (GGCPMNode llchild : mygg.getllchildred()) {
            if (llchild instanceof GGGnnNode)
                uniqueChildren.addAll(llchild.getChildren());
        }

        TreeSet<Rel> parentRels = cpmGnn.parentRels();

        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> inputRels = edgeAttrs
                    ? (ArrayList<Rel>) pair.getEdgeAttributes()
                    : (ArrayList<Rel>) pair.getNodeAttributes();

            if (inputRels == null || inputRels.isEmpty())
                continue;

            Rel firstRel = inputRels.get(0);
            String key = firstRel.getTypesAsString();

            int idxFeat = 0;
            double[][] inputMatrix = inputDict.get(key);
            if (inputMatrix == null)
                continue; // nothing to update for this key

            for (Rel subRel : inputRels) {
                if (parentRels.contains(subRel)) {
                    for (GGCPMNode node : uniqueChildren) {
                        GGAtomMaxNode maxNode = (GGAtomMaxNode) node;
                        // if the value is not in the evidence
                        if (maxNode.getmapInstVal() == -1 && maxNode.myatom().rel().equals(subRel)) {
                            int arg = maxNode.myatom().args[0];
                            int value = maxNode.getCurrentInst();
                            int row = nodeMap.get(arg);
                            if (subRel instanceof CatRel && cpmGnn.isOneHotEncoding())
                                inputMatrix[row][value + idxFeat] = 1;
                            else
                                inputMatrix[row][idxFeat] = value;
                        }
                    }
                }
                if (subRel instanceof CatRel && cpmGnn.isOneHotEncoding())
                    idxFeat += subRel.numvals();
                else
                    idxFeat++;
            }
        }

        return inputDict;
    }

    // create the edge attributes matrix
    public static Map<String, double[][]> initEdgeAttrdict(CatGnn cpmGnn, Map<Rel, Vector<int[]>> relToEdgeAttrMap, SparseRelStruc sampledRel) {
        Map<String, double[][]> edge_attr = new HashMap<>();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getEdgeAttributes();
            for (int j = 0; j < subList.size() - 1; j++) {
                // check if all the types in the subList are the same
                if (!subList.get(j).getTypesAsString().equals(subList.get(j + 1).getTypesAsString())) {
                    throw new RuntimeException("Types of the relations do not match! " + subList.get(j).getTypesAsString() + " / " + subList.get(j + 1).getTypesAsString());
                }
            }
            String key = subList.get(0).getTypesAsString();

            double[][] inputXmatrix = createEdgeAttrMatrix(subList, relToEdgeAttrMap, sampledRel, cpmGnn.isOneHotEncoding());
            edge_attr.put(key, inputXmatrix);
        }
        return edge_attr;
    }

    /*
    Set the current inst by changing only the selected value and not overwrite all the feature matrix
     */
    public void setCurrentInstPy(int currentInst, GGAtomMaxNode currentMaxNode, GGGnnNode parent) {
        Rel currentRel = currentMaxNode.myatom().rel();
        CatGnn cpmGnn = (CatGnn) parent.getCpm();
        final boolean oneHot = cpmGnn.isOneHotEncoding();
        // update for node attributes
        if (setNodeAttributePy(currentRel, currentMaxNode, cpmGnn, currentInst, oneHot))
            return;
        // update edge index
        if (setEdgeIndexPy(currentRel, currentMaxNode, cpmGnn, currentInst))
            return;
        // update for edge attributes
        if (setEdgeAttributePy(currentRel, currentMaxNode, cpmGnn, currentInst, oneHot))
            return;
    }

    private boolean setNodeAttributePy(Rel currentRel,
                                       GGAtomMaxNode currentMaxNode,
                                       CatGnn cpmGnn,
                                       int currentInst,
                                       boolean oneHot) {

        if (relToNodeMap.isEmpty()
                || currentMaxNode.myatom().args.length != 1
                || GGnodeAttrDict.isEmpty()
                || !relToNodeMap.containsKey(currentRel)) {
            return false;
        }

        int nodeToUpdate = currentMaxNode.myatom().args()[0];
        Integer nodeIndexObj = nodeMap.get(nodeToUpdate);
        if (nodeIndexObj == null) return false;
        int nodeIndex = nodeIndexObj;

        Rel targetRel = currentMaxNode.myatom().rel();
        String key = targetRel.getTypesAsString();

        boolean curIsCat = (currentRel instanceof CatRel);
        boolean curIsBool = (currentRel instanceof BoolRel);
        if (!curIsCat && !curIsBool)
            throw new RuntimeException("Invalid relation type for edge attribute " + currentRel.name());

        // iterate input specs; each pair has its own feature offset starting at 0
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            int idxFeat = 0;
            List<Rel> inputRels = pair.getNodeAttributes();
            if (inputRels == null) continue;

            for (Rel r : inputRels) {
                // only operate when the input relation matches the target relation
                if (r.equals(targetRel)) {
                    // fetch the attribute matrix for this relation once
                    double[][] attrs = GGnodeAttrDict.get(key);
                    if (attrs == null || nodeIndex < 0 || nodeIndex >= attrs.length) return false;
                    double[] row = attrs[nodeIndex];

                    if (curIsCat) {
                        if (oneHot) {
                            final double numVals = currentRel.numvals();
                            // zero the one-hot slice
                            for (int i = 0; i < numVals; i++)
                                row[i + idxFeat] = 0;
                            // set the selected category
                            row[currentInst + idxFeat] = 1;
                        } else
                            row[idxFeat] = currentInst;
                    } else // boolean rel
                        row[idxFeat] = currentInst;

                    changedUpdate = true;
                    return true;
                }

                // move feature offset for the next relation
                if (oneHot && (r instanceof CatRel)) {
                    idxFeat += r.numvals();
                } else {
                    idxFeat++;
                }
            }
        }

        return false;
    }

    private boolean setEdgeAttributePy(Rel currentRel,
                                    GGAtomMaxNode currentMaxNode,
                                    CatGnn cpmGnn,
                                    int currentInst,
                                    boolean oneHot) {

        if (!GGedgeAttrDict.isEmpty()
                && currentMaxNode.myatom().args.length==2
                && !relToEdgeAttrMap.isEmpty()
                && relToEdgeAttrMap.containsKey(currentRel)) {

            int[] nodesToUpdate = currentMaxNode.myatom().args();
            final int nodeA = nodesToUpdate[0];
            final int nodeB = nodesToUpdate[1];

            int idxFeat = 0;
            for (TorchInputSpecs edgePair : cpmGnn.getGnnInputs()) {
                // get edge_index once
                ArrayList<ArrayList<Integer>> edge_index = GGedgeDict.get(edgePair.getEdgeRelation().name());
                if (edge_index == null || edge_index.isEmpty()) continue;

                // collect matching row index that matches the edge to change
                List<Integer> matchingRows = new ArrayList<>();
                for (int i = 0, n = edge_index.size(); i < n; i++) {
                    ArrayList<Integer> row = edge_index.get(i);
                    boolean matches;
                    if (row.size() == 2) {
                        int v0 = row.get(0), v1 = row.get(1);
                        matches = (v0 == nodeA && v1 == nodeB) || (v0 == nodeB && v1 == nodeA);
                    } else {
                        matches = row.contains(nodeA) && row.contains(nodeB);
                    }
                    if (matches) matchingRows.add(i);
                }

                List<Rel> edgeAttrs = edgePair.getEdgeAttributes();
                for (Rel r : edgeAttrs) {
                    if (!matchingRows.isEmpty() && r.equals(currentRel)) {
                        String key = currentRel.getTypesAsString();
                        double[][] attrMatrix = GGedgeAttrDict.get(key);
                        if (attrMatrix == null) {
                            changedUpdate = false;
                            throw new RuntimeException("Edge attribute " + r.name() + " not found in GGedgeAttrDict!");
                        }

                        if (currentRel instanceof CatRel) {
                            if (oneHot) {
                                for (int rowIdx : matchingRows) {
                                    for (int i = 0; i < currentRel.numvals(); i++)
                                        attrMatrix[rowIdx][i + idxFeat] = 0;
                                    attrMatrix[rowIdx][nodeA + idxFeat] = 1;
                                }
                            } else {
                                for (int rowIdx : matchingRows)
                                    attrMatrix[rowIdx][nodeA] = currentInst;
                            }
                            changedUpdate = true;
                            return true;
                        }

                        if (currentRel instanceof BoolRel) {
                            for (int rowIdx : matchingRows)
                                attrMatrix[rowIdx][idxFeat] = currentInst;
                            changedUpdate = true;
                            return true;
                        }

                        if (!(currentRel instanceof BoolRel) && !(currentRel instanceof CatRel))
                            throw new RuntimeException("Invalid relation type for edge attribute " + r.name());
                    }

                    if (oneHot && r instanceof CatRel) {
                        idxFeat += r.numvals();
                    } else {
                        idxFeat++;
                    }
                } // for edgeAttr
            } // for cpmGnn.getGnnInputs()
        }
        return false;
    }

    private boolean setEdgeIndexPy(Rel currentRel,
                                GGAtomMaxNode currentMaxNode,
                                CatGnn cpmGnn,
                                int currentInst) {

        if (!GGedgeDict.isEmpty()
                && currentMaxNode.myatom().args.length==2
                && !GGboolRel.isEmpty()
                && GGboolRel.contains(currentRel)) {

            int[] nodesToUpdate = currentMaxNode.myatom().args();
            // count the starting position for the feature
            Rel edgeRelToUpdate = null;
            for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
                Rel edgeRel = pair.getEdgeRelation();
                if (edgeRel.equals(currentRel))
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
                return true;
            } else
                throw new RuntimeException("Something went wrong in setCurrentInstPy!");
        }
        return false;
    }

    public Map<String, ArrayList<ArrayList<Integer>>> updateEdgeDict(Map<String, ArrayList<ArrayList<Integer>>> edge_dict, CatGnn cpmGnn,  GGCPMNode ggcpmNode) {
        // at the moment, edge-features are not implemented/supported
        Vector<GGCPMNode> childred = ggcpmNode.getChildren();
        TreeSet<Rel> parentRels = cpmGnn.parentRels();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            Rel edge = pair.getEdgeRelation();
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
        return edge_dict;
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

    public double[] GGevaluate_gnnHetero(RelStruc A, OneStrucData inst, GradientGraphO gg, CatGnn cpmGnn, GGCPMNode ggcpmGnn) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        // mode torch model to the new interpreter
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath);
        CatGnn cpm = (CatGnn) cpmGnn;

        if (!savedData) {
            initGnnData(cpm, A, inst);
            savedData = true;
        }

        if (GGnodeAttrDict.isEmpty()) {
            GGnodeAttrDict = initNodeAttrDict(cpm, relToNodeMap, nodeMap, GGsampledRel);
            // we need to use the sampled values in the gradient graph structure (maxindicator) and assign them to the rel
            // for GNNs the order of the features needs to be respected: the order in input_attr in CatGnn will be used for constructing the vector
            GGnodeAttrDict = updateAttrDict(GGnodeAttrDict, cpm, ggcpmGnn, false);
        }
        if (GGedgeDict.isEmpty()) {
            GGedgeDict = initEdgesDict(GGboolRel, GGsampledRel);
            GGedgeDict = updateEdgeDict(GGedgeDict, cpm, ggcpmGnn);
        }
        if (GGedgeAttrDict.isEmpty() && relToEdgeAttrMap.size() > 0) {
            GGedgeAttrDict = initEdgeAttrdict(cpm, relToEdgeAttrMap, GGsampledRel);
            GGedgeAttrDict = updateAttrDict(GGedgeAttrDict, cpm, ggcpmGnn,true);
        }

        Object[] result = inferModelHetero(GGnodeAttrDict, GGedgeDict, GGedgeAttrDict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId(), true);
        double[][] outProbs = (double[][]) result[0];
        if (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))
            return outProbs[0];
        else
            return outProbs[Integer.parseInt(cpmGnn.getArgument())];
    }


    public void initGnnData(CatGnn cpmGnn, RelStruc A, OneStrucData inst) {
        if (GGonsd == null && GGsampledRel == null) {
            GGonsd = new OneStrucData(A.getmydata().copy()); // only one copy per time
            GGsampledRel = new SparseRelStruc(A.getNames(), GGonsd, A.getCoords(), A.signature());
            GGsampledRel.getmydata().add(inst.copy());
        }

        if (cpmGnn instanceof CatGnn) {
            relToNodeMap = constructNodesDict(cpmGnn, A);
            nodeMap = constructNodesDictMap(cpmGnn, A);
            relToEdgeAttrMap = constructEdgeAttrDict(cpmGnn, A);
            GGnodeAttrDict = new HashMap<>();
            GGedgeDict = new HashMap<>();
            if (GGboolRel == null) {
                GGboolRel = new Vector<>();
                for (TorchInputSpecs inps: cpmGnn.getGnnInputs()) {
                    GGboolRel.add(inps.getEdgeRelation());
                }
            }
        }
    }

    public void updateEdgeDictForSampling(Map<String, ArrayList<ArrayList<Integer>>> edge_dict, CatGnn cpmGnn, Hashtable<String, PFNetworkNode> atomhasht) {
        TreeSet<Rel> parentRels = cpmGnn.parentRels();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            BoolRel edge = pair.getEdgeRelation();
            if (parentRels.contains(edge)) {
                throw new RuntimeException("Edge features are not yet implemented for sampling!");
//                for (int i = 0; i < edge_index.size(); i++) {
//                    if (GGsampledRel.truthValueOf(edge, Arrays.edge_index[i]) == -1) {
//                        if (maxNode.getmapInstVal() == -1 && maxNode.myatom().rel().equals(edge) && maxNode.myatom().args.length == 2) {
//                            if (maxNode.getCurrentInst() > 0) {
//                                edge_index.get(0).add(maxNode.myatom().args[0]);
//                                edge_index.get(1).add(maxNode.myatom().args[1]);
//                            }
//                        }
//                    }
//                }
            }
        }
    }

    // update the x matrix with the sampled value
    public void updateInputDictForSampling(Map<String, double[][]> input_dict, Map<Rel, int[][]> GGnumNodesDict, CatGnn cpmGnn, Hashtable<String, PFNetworkNode> atomhasht) {
        TreeSet<Rel> parentRels = cpmGnn.parentRels();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> inputRels = (ArrayList<Rel>) pair.getNodeAttributes();
            Rel firstRel = inputRels.get(0);
            String key = firstRel.getTypesAsString(); // should have the same type, we take the first
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
                                if (cpmGnn.isOneHotEncoding()) {
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
        }
    }

    public void updateEdgeInputDictForSampling(
            Map<String, double[][]> input_dict,
            Map<Rel, Vector<int[]>> GGedgeIndex,
            CatGnn cpmGnn,
            Hashtable<String, PFNetworkNode> atomhasht) {

        TreeSet<Rel> parentRels = cpmGnn.parentRels();
        // for all the inputs of the cpmGnn
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            List<Rel> edgeAttrs = pair.getEdgeAttributes();
            int idxFeat = 0;
            // loop over all the edge attributes
            for (Rel r : edgeAttrs) {
                if (parentRels.contains(r)) {
                    String key = r.getTypesAsString();
                    double[][] attrMatrix = input_dict.get(key);
                    Vector<int[]> edge_index_vec = GGedgeIndex.get(r);
                    int rowIdx = 0;
                    // loop over all the edges of that attribute
                    for (int i = 0; i < edge_index_vec.size(); i++) {
                        int[] edge_index_int = edge_index_vec.get(i);
                        // if the value has been sampled, update the attribute feature matrix
                        if (GGsampledRel.truthValueOf(r, edge_index_int) == -1) {
                            GroundAtom myatom = new GroundAtom(r, edge_index_int);
                            PFNetworkNode gan = (PFNetworkNode) atomhasht.get(myatom.asString());
                            if (gan != null) {
                                int value = gan.sampleinstVal();
                                int nodeA = edge_index_int[0];

                                if (r instanceof CatRel) {
                                    if (cpmGnn.isOneHotEncoding()) {
                                        for (int j = 0; j < r.numvals(); j++)
                                            attrMatrix[rowIdx][j + idxFeat] = 0;
                                        attrMatrix[rowIdx][nodeA + idxFeat] = 1;
                                    } else
                                        attrMatrix[rowIdx][nodeA] = value;
                                }

                                if (r instanceof BoolRel)
                                    attrMatrix[rowIdx][idxFeat] = value;

                                if (!(r instanceof BoolRel) && !(r instanceof CatRel))
                                    throw new RuntimeException("Invalid relation type for edge attribute " + r.name());
                            }
                        }
                        rowIdx++;
                    }
                }
                if (cpmGnn.isOneHotEncoding() && r instanceof CatRel) {
                    idxFeat += r.numvals();
                } else {
                    idxFeat++;
                }
            }
        }
    }


    public double[] evalSample_gnn(CatGnn cpmGnn, Hashtable<String, PFNetworkNode> atomhasht) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath); // update the model if they differ with interpreters

        // expensive operation, everytime we create complexly the input matrix
        GGnodeAttrDict = initNodeAttrDict(cpmGnn, relToNodeMap, nodeMap, GGsampledRel);
        updateInputDictForSampling(GGnodeAttrDict, relToNodeMap, cpmGnn, atomhasht);

        // TODO update edges in sampling
        GGedgeDict = initEdgesDict(GGboolRel, GGsampledRel);
        updateEdgeDictForSampling(GGedgeDict, cpmGnn, atomhasht);

        GGedgeAttrDict = initEdgeAttrdict(cpmGnn, relToEdgeAttrMap, GGsampledRel);
        updateEdgeInputDictForSampling(GGedgeAttrDict, relToEdgeAttrMap, cpmGnn, atomhasht);

        Object[] result = inferModelHetero(GGnodeAttrDict, GGedgeDict, GGedgeAttrDict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId(), true);
        if (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))
            return (double[]) result[0];

        double[][] outProbs = (double[][]) result[0];
        return outProbs[Integer.parseInt(cpmGnn.getArgument())];
    }

    private void printPython(Interpreter interpreter, String var) {
        StringWriter output = new StringWriter();
        interpreter.set("output", output);
        interpreter.eval("import sys");
        interpreter.eval("sys.stdout = output");
        interpreter.eval("print(" + var + ")");
        System.out.println("Captured output: " + output.toString());
    }

    public void setGradientGraph(GradientGraphO mygg) { this.mygg = mygg; }

    public TorchModelWrapper getTorchModel() { return torchModel; }
}


