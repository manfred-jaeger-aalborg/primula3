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
    private double[][] currentResult;
    private Map<String, double[][]> currentXdict;
    private Map<String, ArrayList<ArrayList<Integer>>> currentEdgeDict;
    private String lastId;
    private OneStrucData GGonsd;
    private SparseRelStruc GGsampledRel;
    private Vector<BoolRel> GGboolRel;
    private Map<String, double[][]> GGxDict;
    private Map<String, ArrayList<ArrayList<Integer>>> GGedgeDict;
    Map<Integer, Integer> nodeMap;
    private Map<Rel, int[][]> GGNodesDict;
    private SparseRelStruc sampledRelGobal;
    private boolean changedUpdate;
    private GradientGraphO mygg;
    private TorchModelWrapper torchModel;
    private CatGnn currentCatGnn;
    private boolean savedData;
    private OneStrucData oldInst;

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
        currentXdict = new Hashtable<>();
        currentEdgeDict = new Hashtable<>();
        GGNodesDict = new Hashtable<>();
        GGedgeDict = new Hashtable<>();
        nodeMap = new HashMap<>();
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

    public double[] inferModelHetero(int node, Map<String, double[][]> x_dict, Map<String, ArrayList<ArrayList<Integer>>> edge_dict, List<TorchInputSpecs> gnnInputs, String idGnn) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath); // update the model if they differ with interpreters
        int currentNode = (node != -1) ? node : 0;

        try {
            // check if there are already computed the results for the specific node in the result matrix,
            // otherwise compute for all nodes with one forward propagation
            // needs also to have the same id as before
            if (checkValuesDictCache(x_dict, edge_dict, idGnn))
                return currentResult[currentNode];
            currentXdict = x_dict;
            currentEdgeDict = edge_dict;
            lastId = idGnn;
            changedUpdate = false;

            currentResult = torchModel.forward(currentXdict, currentEdgeDict, gnnInputs);
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
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getFeatures();
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
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getFeatures();
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

        // only val no gradient computed
        if (valonly) {
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
            if (GGNodesDict.isEmpty())
                GGNodesDict = constructNodesDict(cpmGnn, A);
            if (nodeMap.isEmpty())
                nodeMap = constructNodesDictMap(cpmGnn, A);

            Map<String, double[][]> x_dict = inputAttrToDict(cpmGnn, nodeMap, GGNodesDict, sampledRelGobal);
            Map<String, ArrayList<ArrayList<Integer>>> edge_dict = edgesToDict(GGboolRel, sampledRelGobal, nodeMap);
            if (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))
                result[0] = inferModelHetero(-1, x_dict, edge_dict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId());
            else
                result[0] = inferModelHetero(Integer.parseInt(cpmGnn.getArgument()), x_dict, edge_dict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId());
        } else {
            throw new RuntimeException("GRADIENT IN EVALUATION NOT IMPLEMENTED FOR GNN-RBN!");
        }
        oldInst = inst;
        return result;
    }

    public static Map<String, double[][]> inputAttrToDict(CatGnn cpmGnn, Map<Integer, Integer> nodeMap, Map<Rel, int[][]> GGNodesDict, SparseRelStruc sampledRel) {
        Map<String, double[][]> x_dict = new Hashtable<>();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getFeatures();
            for (int j = 0; j < subList.size() - 1; j++) {
                // check if all the types in the subList are the same
                if (!subList.get(j).getTypes()[0].getName().equals(subList.get(j + 1).getTypes()[0].getName())) {
                    throw new RuntimeException("Types of the relations do not match!");
                }
            }
            String key = subList.get(0).getTypes()[0].getName();
            x_dict.put(key, createTensorMatrix(subList, nodeMap, GGNodesDict, sampledRel, cpmGnn.isOneHotEncoding()));
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
    public static double[][] createTensorMatrix(ArrayList<Rel> attributes, Map<Integer, Integer> nodeMap, Map<Rel, int[][]> nodes_dict, SparseRelStruc finalre, boolean oneHot) {
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
            if (oneHot)
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

    // initialize the input matrix with the values of the rels that are predefined otherwise set to 0
    // in sampledRel there are also the inst values!
    public static Map<String, double[][]> initXdict(CatGnn cpmGnn, Map<Rel, int[][]> GGnumNodesDict, Map<Integer, Integer> nodeMap, SparseRelStruc sampledRel) {
        Map<String, double[][]> x_dict = new HashMap<>();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getFeatures();
            for (int j = 0; j < subList.size() - 1; j++) {
                // check if all the types in the subList are the same
                if (!subList.get(j).getTypes()[0].getName().equals(subList.get(j + 1).getTypes()[0].getName())) {
                    throw new RuntimeException("Types of the relations do not match! " + subList.get(j).getTypes()[0].getName() + " / " + subList.get(j + 1).getTypes()[0].getName());
                }
            }
            String key = subList.get(0).getTypes()[0].getName();
            double[][] inputXmatrix = createTensorMatrix(subList, nodeMap, GGnumNodesDict, sampledRel, cpmGnn.isOneHotEncoding());
            x_dict.put(key, inputXmatrix);
        }
        return x_dict;
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
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> inputRels = (ArrayList<Rel>) pair.getFeatures();
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
                            if (subRel instanceof CatRel && cpmGnn.isOneHotEncoding())
                                inputMatrix[nodeMap.get(arg)][value + idxFeat] = 1;
                            else
                                inputMatrix[nodeMap.get(arg)][idxFeat] = value;
                        }
                    }
                }
                if (subRel instanceof CatRel && cpmGnn.isOneHotEncoding())
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
            for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
                idxFeat = 0;
                ArrayList<Rel> inputRels = (ArrayList<Rel>) pair.getFeatures();

                for (Rel r: inputRels) {
                    if (currentMaxNode.myatom().rel() instanceof CatRel && r.equals(currentMaxNode.myatom().rel())) {
                        String key = currentMaxNode.myatom().rel().getTypes()[0].getName();
                        if (cpmGnn.isOneHotEncoding()) {
                            for (int i = 0; i < currentMaxNode.myatom().rel().numvals(); i++) {
                                GGxDict.get(key)[nodeMap.get(nodeToUpdate)][i + idxFeat] = 0;
                            }
                            GGxDict.get(key)[nodeMap.get(nodeToUpdate)][currentInst + idxFeat] = 1;
                        } else {
                            GGxDict.get(key)[nodeMap.get(nodeToUpdate)][idxFeat] = currentInst;
                        }
                        changedUpdate = true;
                    } else if (!(currentMaxNode.myatom().rel() instanceof CatRel) && r.equals(currentMaxNode.myatom().rel())) {
                        throw new RuntimeException("Relation " + r.name() + " not supported in setCurrentInstPy!");
                    }
                    if (cpmGnn.isOneHotEncoding() && r instanceof CatRel)
                        idxFeat += r.numvals();
                    else
                        idxFeat++;
                }
            }
        }
        if (!GGedgeDict.isEmpty() && currentMaxNode.myatom().args.length==2) {
            int[] nodesToUpdate = currentMaxNode.myatom().args();
            // count the starting position for the feature
            Rel edgeRelToUpdate = null;
            CatGnn cpmGnn = (CatGnn) parent.getCpm();
            for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
                Rel edgeRel = pair.getEdgeRelation();
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

        if (GGxDict.isEmpty()) {
            GGxDict = initXdict(cpm, GGNodesDict, nodeMap, GGsampledRel);
            // we need to use the sampled values in the gradient graph structure (maxindicator) and assign them to the rel
            // for GNNs the order of the features needs to be respected: the order in input_attr in CatGnn will be used for constructing the vector
            updateInputDict2(GGxDict, GGNodesDict, cpm, ggcpmGnn);
        }
        if (GGedgeDict.isEmpty()) {
            GGedgeDict = initEdgesDict(GGboolRel, GGsampledRel);
            updateEdgeDict(GGedgeDict, cpm, ggcpmGnn);
        }

        if (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))
            return inferModelHetero(-1, GGxDict, GGedgeDict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId());
        else
            return inferModelHetero(Integer.parseInt(cpmGnn.getArgument()), GGxDict, GGedgeDict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId());
    }


    public void initGnnData(CatGnn cpmGnn, RelStruc A, OneStrucData inst) {
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
            ArrayList<Rel> inputRels = (ArrayList<Rel>) pair.getFeatures();
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
    }

    public double[] evalSample_gnn(CatGnn cpmGnn, RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath); // update the model if they differ with interpreters

        // expensive operation, everytime we create complexly the input matrix
        GGxDict = initXdict(cpmGnn, GGNodesDict, nodeMap, GGsampledRel);
        updateInputDictForSampling(GGxDict, GGNodesDict, cpmGnn, atomhasht);

        // TODO update edges in sampling
        GGedgeDict = initEdgesDict(GGboolRel, GGsampledRel);
        updateEdgeDictForSampling(GGedgeDict, cpmGnn, atomhasht);

        double[] res = null;
        if (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))
            res = inferModelHetero(-1, GGxDict, GGedgeDict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId());
        else
            res = inferModelHetero(Integer.parseInt(cpmGnn.getArgument()), GGxDict, GGedgeDict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId());
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

    public void setGradientGraph(GradientGraphO mygg) { this.mygg = mygg; }

    public TorchModelWrapper getTorchModel() { return torchModel; }
}


