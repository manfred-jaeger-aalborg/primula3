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
import java.sql.Types;
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
    private GGGnnNode currentGGGnn;

    private volatile RelStruc cachedA = null;
    private volatile OneStrucData cachedInst = null;
    private volatile String cachedGnnId = null;
    private volatile Object[] cachedResult = null;
    private volatile int cachedAHash = 0;
    private volatile int cachedInstHash = 0;


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

    // this function check if the current input (dictionary of (rel/types, matrix of values)
    // has already been computed by the gnn and return the result
    public Object[] inferModelHetero(Map<String, double[][]> x_dict,
                                     Map<String, ArrayList<ArrayList<Integer>>> edge_dict,
                                     Map<String, double[][]> edge_attr,
                                     List<TorchInputSpecs> gnnInputs,
                                     String idGnn,
                                     boolean valonly) {

        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath);

        try {
            // Quick hash-based check first
            if (checkValuesDictCache(x_dict, edge_dict, edge_attr, idGnn) && currentResult[0] != null){
                if (valonly)
                    return currentResult;

                // if gradients are not present, recompute
                if (!valonly && currentResult[1] != null)
                    return currentResult;
            }

            // Update cache with the latest input
            updateCache(x_dict, edge_dict, edge_attr, idGnn);
            // perform the forward to the model
            currentResult = torchModel.forward(currentNodeAttrDict, currentEdgeDict, currentEdgeAttrDict, gnnInputs, !valonly);
            return currentResult;
        } catch (JepException e) {
            System.err.println("Failed to execute inference: " + e);
            return null;
        }
    }

    private synchronized Object[] basicStructCacheGet(OneStrucData inst, String gnnId, boolean valonly) {
        if (cachedInst == null || cachedGnnId == null || cachedResult == null) return null;

        if (cachedInst.equals(inst) && (cachedGnnId != null && cachedGnnId.equals(gnnId)) && cachedResult != null) {
            if (valonly) return cachedResult.clone();
            if (!valonly && cachedResult[1] != null) return cachedResult.clone();
            // gradients missing, miss
            return null;
        }

        // hashCode based check as a secondary
        int iHash = (inst == null) ? 0 : inst.hashCode();

        if (cachedGnnId != null && cachedGnnId.equals(gnnId)
                && cachedInstHash == iHash
                && cachedResult != null) {
            // TODO maybe it is better to use equals and implement it in RelStruct and OneStructData
            if (valonly) return cachedResult.clone();
            if (!valonly && cachedResult[1] != null) return cachedResult.clone();
        }
        // Miss
        return null;
    }

    private synchronized void basicStructCachePut(OneStrucData inst, String gnnId, Object[] result) {
        if (inst == null || gnnId == null || result == null) return;

        cachedInst = inst;
        cachedGnnId = gnnId;
        cachedResult = result.clone();

        // store hash codes for next quick comparison
        cachedInstHash = (inst == null) ? 0 : inst.hashCode();
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
//        for (double[] ints : node_bool) Arrays.fill(ints, 0);
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

    // this function creates and prepares the data from the relstruct/inst
    // several dictionaries are created to maintain the order of the data with the python and the primula data
    public Object[] evaluate_gnnHetero(RelStruc A, OneStrucData inst, CatGnn cpmGnn, boolean valonly) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        // mode torch model to the new interpreter
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath);

        if (!valonly)
            throw new RuntimeException("Not implemented. Use evaluate_gnnGradients instead. Try with Gradient Graph");

        Object[] resultCopy = null;
//        Object[] cached = basicStructCacheGet(inst, cpmGnn.getGnnId(), valonly);
        // DO WE NEED CACHING HERE?
        // N.B.: in the construction of the bayesian network, [BayesConstruct: makeCPT()]
        // we change the inst without a copy of the object so we have the same reference (equal() fail to compare!)
        Object[] cached = null;

        if (cached != null && cached[0] != null)
            resultCopy = cached.clone();
        else {
            // if cache miss, build inputs
            // reconstruct sampledRelGobal from A+inst
            OneStrucData onsd = new OneStrucData(A.getmydata().copy());
            sampledRelGobal = new SparseRelStruc(A.getNames(), onsd, A.getCoords(), A.signature());
            sampledRelGobal.getmydata().add(inst.copy());

            if (GGboolRel == null) {
                GGboolRel = new Vector<>();
                for (TorchInputSpecs inps : cpmGnn.getGnnInputs())
                    GGboolRel.add(inps.getEdgeRelation());
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

            Object[] result = inferModelHetero(x_dict, edge_dict, edge_attr, cpmGnn.getGnnInputs(), cpmGnn.getGnnId(), valonly);
            resultCopy = result.clone();
            basicStructCachePut(inst, cpmGnn.getGnnId(), resultCopy);
        }
        double[][] outProbsFull = (double[][]) resultCopy[0];

        String outType = cpmGnn.getOutTypes().get(0);
        int nodeIndex = (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals("")) ? 0 : getNodeByType(relToNodeMap, outType, Integer.parseInt(cpmGnn.getArgument()));
        if (nodeIndex == -1) {
            throw new RuntimeException("Could not find node of type " + outType + " with index " + cpmGnn.getArgument());
        }
        resultCopy[0] = outProbsFull[nodeIndex];
        return resultCopy;
    }

    public int getNodeByType(Map<Rel, int[][]> relNodeMap, String type, int nodeIdx) {
        for (Map.Entry<Rel, int[][]> entry : relNodeMap.entrySet()) {
            String typeName = entry.getKey().getTypes()[0].getName();
            if (type.equals(typeName)) {
                return entry.getValue()[nodeIdx][0];
            }
        }
        return -1;
    }

    public Object[] GGevaluate_gnnGradients(Integer sno, RelStruc A, OneStrucData inst, CatGnn cpmGnn, GGCPMNode ggcpmNode) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath);

        Object[] resultCopy = null;
        Object[] cached = basicStructCacheGet(inst, cpmGnn.getGnnId(), false);
        if (cached != null && cached[0] != null) {
            resultCopy = cached.clone();
            return resultCopy;
        }

        OneStrucData onsd = new OneStrucData(A.getmydata().copy());
        sampledRelGobal = new SparseRelStruc(A.getNames(), onsd, A.getCoords(), A.signature());
        sampledRelGobal.getmydata().add(inst.copy());

        if (GGboolRel == null) {
            GGboolRel = new Vector<>();
            for (TorchInputSpecs inps : cpmGnn.getGnnInputs())
                GGboolRel.add(inps.getEdgeRelation());
        }

        GGGnnNode ggcnn = (GGGnnNode) ggcpmNode;

        Object[] inputGraph = GGconstructInputGraph(cpmGnn, ggcnn, sno);
        Map<String, double[][]> x_dict = (Map<String, double[][]>) inputGraph[0];
        Map<String, ArrayList<ArrayList<Integer>>> edge_dict = (Map<String, ArrayList<ArrayList<Integer>>>) inputGraph[1];
        Map<String, double[][]> edgeAttr_dict = (Map<String, double[][]>) inputGraph[2];

        Object[] result = inferModelHetero(x_dict, edge_dict, edgeAttr_dict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId(), false);
        resultCopy = result.clone();

        Map<String, double[][]> resGrads = (Map<String, double[][]>) resultCopy[1];
        Map<String, double[][]> outGrads = new HashMap<>();
        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getNodeAttributes();
            int relIdx = 0;
            double[][] xGrads = resGrads.get("x");
            for (Rel rel : subList) {
                if (rel instanceof NumRel) {
                    for (int i = 0; i < xGrads.length; i++) {
                        String atomString = rel.name() + "(" + i + ")";
                        double[][] relGrad = new double[1][1];
                        relGrad[0][0] = xGrads[i][relIdx];
                        outGrads.put(atomString, relGrad);
                    }
                }
                relIdx++;
            }

            // edge attributes grads13 = {double[57]@1625} [-0.22445032000541687, 0.01452187541872263, -0.09580807387828827, 0.04337388649582863, 0.033709827810525894, 0.1422426551580429, 0.17550571262836456, 0.061612337827682495, -0.01995239220559597, -0.0016841309843584895, -0.001965058036148548, -0.002265435876324773, -0.0025815789122134447, -0.002909214235842228, -0.003245773958042264, -0.0035912671592086554, -0.00394787872210145, -0.004318977706134319, -0.0047082314267754555, -0.005119148176163435, -0.00555501040071249, -0.006018909625709057, -0.006513677537441254, -0.007041583303362131, -0.007603800389915705, -0.00819958746433258, -0.00882485881447792, -0.009468862786889076, -0.010104347951710224, -0.010655861347913742, -0.01089834701269865, -0.010156515054404736, -0.006567280273884535, -0.001269652508199215, -0.0014863497344776988, -0.0017154887318611145, -0.0019549503922462463, -0.002201878698542714, -0.002454758621752262, -0.00271426048129797, -0.00298296264372766, -0.0032645014580339193, -0.0035627398174256086, -0.003881273325532675,… View
            subList = (ArrayList<Rel>) pair.getEdgeAttributes();
            relIdx = 0;
            double[][] eaGrads = resGrads.get("ea");
            if (eaGrads != null) {
                for (Rel rel : subList) {
                    if (rel instanceof NumRel) {
                        for (int i = 0; i < eaGrads.length; i++) {
                            String atomString = rel.name() + "(" + i + ")";
                            double[][] relGrad = new double[1][1];
                            relGrad[0][0] = eaGrads[i][relIdx];
                            outGrads.put(atomString, relGrad);
                        }
                    }
                    relIdx++;
                }
            }
        }



        double[][] outProbsFull = (double[][]) resultCopy[0];
        int nodeIndex = 0;
        if (!(cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))) {
            String outType = cpmGnn.getOutTypes().get(0);
            nodeIndex = ggcnn.getNodeIndexIfPresent(outType, Integer.parseInt(cpmGnn.getArgument()));
        }
        resultCopy[0] = outProbsFull[nodeIndex];
        resultCopy[1] = outGrads;
        basicStructCachePut(inst, cpmGnn.getGnnId(), resultCopy);
        return resultCopy;
    }


    public Object[] evaluate_gnnGradients(RelStruc A, OneStrucData inst, CatGnn cpmGnn, GGCPMNode ggcpmNode) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        // mode torch model to the new interpreter
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath);

        Object[] resultCopy = null;
        Object[] cached = basicStructCacheGet(inst, cpmGnn.getGnnId(), false);
        if (cached != null && cached[0] != null)
            resultCopy = cached.clone();
        else {
//        // avoid copying if the inst (the one that should change) are the same
//        if (!inst.equals(oldInst)) {
//            // reconstruct sampledRelGobal from A+inst
//            OneStrucData onsd = new OneStrucData(A.getmydata().copy());
//            sampledRelGobal = new SparseRelStruc(A.getNames(), onsd, A.getCoords(), A.signature());
//            oldInst = inst.copy();
//            sampledRelGobal.getmydata().add(oldInst);
//        }
            OneStrucData onsd = new OneStrucData(A.getmydata().copy());
            sampledRelGobal = new SparseRelStruc(A.getNames(), onsd, A.getCoords(), A.signature());
            sampledRelGobal.getmydata().add(inst.copy());

            if (GGboolRel == null) {
                GGboolRel = new Vector<>();
                for (TorchInputSpecs inps : cpmGnn.getGnnInputs())
                    GGboolRel.add(inps.getEdgeRelation());
            }
            if (relToNodeMap.isEmpty())
                relToNodeMap = constructNodesDict(cpmGnn, A);
            if (nodeMap.isEmpty())
                nodeMap = constructNodesDictMap(cpmGnn, A);
            if (relToEdgeAttrMap.isEmpty())
                relToEdgeAttrMap = constructEdgeAttrDict(cpmGnn, A);

            Map<String, double[][]> x_dict = inputAttrToDict(cpmGnn, nodeMap, relToNodeMap, sampledRelGobal);
            x_dict = updateAttrDict(x_dict, cpmGnn, ggcpmNode, false);

            Map<String, ArrayList<ArrayList<Integer>>> edge_dict = edgesToDict(GGboolRel, sampledRelGobal, nodeMap);
            edge_dict = updateEdgeDict(edge_dict, cpmGnn, ggcpmNode);

            Map<String, double[][]> edge_attr = new HashMap<>();
            if (relToEdgeAttrMap.size() > 0) {
                edge_attr = initEdgeAttrdict(cpmGnn, relToEdgeAttrMap, sampledRelGobal);
                edge_attr = updateAttrDict(edge_attr, cpmGnn, ggcpmNode, true);
            }

            Object[] result = inferModelHetero(x_dict, edge_dict, edge_attr, cpmGnn.getGnnInputs(), cpmGnn.getGnnId(), false);
            resultCopy = result.clone();

            Map<String, double[][]> resGrads = (Map<String, double[][]>) resultCopy[1];
            Map<String, double[][]> outGrads = new HashMap<>();
            for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
                ArrayList<Rel> subList = (ArrayList<Rel>) pair.getNodeAttributes();
                int relIdx = 0;
                double[][] xGrads = resGrads.get("x");
                for (Rel rel : subList) {
                    if (rel instanceof NumRel) {
                        for (int i = 0; i < xGrads.length; i++) {
                            String atomString = rel.name() + "(" + i + ")";
                            double[][] relGrad = new double[1][1];
                            relGrad[0][0] = xGrads[i][relIdx];
                            outGrads.put(atomString, relGrad);
                        }
                    }
                    relIdx++;
                }

                subList = (ArrayList<Rel>) pair.getEdgeAttributes();
                relIdx = 0;
                xGrads = resGrads.get("ea");
                for (Rel rel : subList) {
                    if (rel instanceof NumRel) {
                        for (int i = 0; i < xGrads.length; i++) {
                            String atomString = rel.name() + "(" + i + ")";
                            double[][] relGrad = new double[1][1];
                            relGrad[0][0] = xGrads[i][relIdx];
                            outGrads.put(atomString, relGrad);
                        }
                    }
                    relIdx++;
                }
            }
            resultCopy[1] = outGrads;

            double[][] outProbsFull = (double[][]) resultCopy[0];
            int nodeIndex = (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals("")) ? 0 : Integer.parseInt(cpmGnn.getArgument());
            resultCopy[0] = outProbsFull[nodeIndex];
            basicStructCachePut(inst, cpmGnn.getGnnId(), resultCopy);
        }
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
                startIndex += (int) r.numvals();
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
            for (int i = 0; i < nodeRels.size(); i++) {
                Rel r = nodeRels.get(i);
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
//                        CPModel inputProb = gnnInputProbs.getPfargsNodeAt(i);
//                        try {
//                            double res = inputProb.evaluate(finalre.getData().getParentRelStruc(), finalre.getData());
//                            System.out.println(res);
//                        } catch (RBNCompatibilityException e) {
//                            throw new RuntimeException(e);
//                        }
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
    public static Map<String, double[][]> initNodeAttrDict(GGCPMNode ggcpmGnn, CatGnn cpmGnn, Map<Rel, int[][]> GGnumNodesDict, Map<Integer, Integer> nodeMap, SparseRelStruc sampledRel) {
        Map<String, double[][]> x_dict = new HashMap<>();
        System.out.println(ggcpmGnn);
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
                // check if we are learning parameters
                if (mygg.getParamNodes().length > 0) {
                    for (GGConstantNode node : mygg.getParamNodes()) {
                        String name = node.paramname();
                        int idx = name.indexOf('(');
                        String relName = (idx != -1) ? name.substring(0, idx) : name;
                        if (uniqueChildren.contains(node) && relName.equals(subRel.name())) {
                            double value = node.getCurrentParamVal();
                            int[] args = extractParamArgs(name);
                            if (args != null && args.length > 0) {
                                int row = nodeMap.get(args[0]);
                                inputMatrix[row][idxFeat] = value;
                            }
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
            if (subList.size() > 0) {
                String key = subList.get(0).getTypesAsString();
                double[][] inputXmatrix = createEdgeAttrMatrix(subList, relToEdgeAttrMap, sampledRel, cpmGnn.isOneHotEncoding());
                edge_attr.put(key, inputXmatrix);
            }
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

    // use the pre-computed nodes in the gradient graph and construct the input for the gnns
    // the input is the subgraph depends on the specific node
    // if some nodes have still to be evaluated, it evaluates them
    public Object[] GGconstructInputGraph(CatGnn cpmGnn, GGGnnNode ggcnn, Integer sno) {
        Map<String, double[][]> x_dict = new HashMap<>();
        Map<String, ArrayList<ArrayList<Integer>>> edge_dict = new HashMap<>();
        Map<String, double[][]> edgeAttr_dict = new HashMap<>();

        for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
            int num_col = 0,  startIndex = 0;
            String pftype = pair.getType();
            ArrayList<Rel> subList = (ArrayList<Rel>) pair.getNodeAttributes();

            int[] startIndices = new int[subList.size()];
            Rel[] rels = new Rel[subList.size()];

            Type nodeType = null;
            int pfIndex = 0;
            for (Rel r : subList) {
                num_col += (r instanceof CatRel && cpmGnn.isOneHotEncoding()) ? r.numvals() : 1;

                if (nodeType == null && r.getTypes().length == 1)
                    nodeType = r.getTypes()[0];
                else if (nodeType != null && nodeType.equals(r.getTypes()[0]))
                    throw new RuntimeException("Not all the same type for node attribute " + r.name() + " typed: " + r.getTypesAsString());
                if (r.getTypes().length > 1)
                    throw new RuntimeException("More than one type for node attribute " + r.name());

                startIndices[pfIndex] = startIndex;
                rels[pfIndex] = r;

                // for each feature, see where in the vector it starts
                startIndex += (r instanceof CatRel) ? (int) r.numvals() : 1;
                pfIndex++;
            }
            HashMap<String, HashMap<String, Object[]>> evalNodesAll = ggcnn.getEvalOfNodes();
            HashMap<String, Object[]> nodesTable = (evalNodesAll != null) ? evalNodesAll.get(pftype) : null;

            int num_nodes = nodesTable.size();
            if (num_nodes == 0) // should we also check if nodesTable is 0? this has to be well-defined also in the gnn
                num_nodes = 1;

            if (nodesTable.size()%subList.size()!=0)
                throw new RuntimeException("The number of nodes in the eval table does not match the number of node attributes!");
            // divide the total number of nodes by the attributes per nodes
            double[][] bool_nodes = createOneHotEncodingMatrix(nodesTable.size()/subList.size(), num_col);
            if (nodesTable.size()/subList.size() == 0)
                bool_nodes = createOneHotEncodingMatrix(1, num_col);

            for (Object[] node : nodesTable.values()) {
                int argNode = ((int[]) node[1])[0];
                double value = (double) node[2];
                if (Double.isNaN(value)) {
                    GGCPMNode constructedchild = (GGCPMNode) node[0];
                    double[] valchild = constructedchild.evaluate(sno);
                    value = valchild[0];
                }
                int col = (int) node[4];
                int startingIndex = startIndices[col];
                Rel r = rels[col];

                double matrixValue = 1.;
                if (r instanceof CatRel) {
                    // if onehot encoding
                    col = (int) (value + startingIndex);
                } else {
                    if (r.valtype() == Rel.NUMERIC || r.valtype() == Rel.BOOLEAN) {
                        col = startingIndex;
                        matrixValue = value;
                    }
                }
                // if argNode is -1 then we apply for all the rows
                if (argNode == -1) {
                    for (int c = 0; c < num_nodes; c++)
                        bool_nodes[c][col] = matrixValue;
                } else
                    bool_nodes[argNode][col] = matrixValue;
            }
            x_dict.put(pftype, bool_nodes);

            HashMap<String, HashMap<String, Object[]>> evalEdgesAll = ggcnn.getEvalOfEdge();
            HashMap<String, Object[]> edgeTable = (evalNodesAll != null) ? evalEdgesAll.get(pftype) : null;

            Map<String, Integer> edgeKeyToIndex = new LinkedHashMap<>();
            ArrayList<ArrayList<Integer>> edges = new ArrayList<>();
            edges.add(new ArrayList<>());
            edges.add(new ArrayList<>());
            int idx = 0;
            for (Object[] edge : edgeTable.values()) {
                double value = (double) edge[2];
                int[] edge_index = (int[]) edge[1];
                if (Double.isNaN(value)) {
                    GGCPMNode constructedchild = (GGCPMNode) edge[0];
                    double[] valchild = constructedchild.evaluate(sno);
                    value = valchild[0];
                }

                if (value != 0) {
                    edges.get(0).add(edge_index[0]);
                    edges.get(1).add(edge_index[1]);
                    String key = Arrays.toString((int[]) edge[3]);
                    if (!edgeKeyToIndex.containsKey(key)) {
                        edgeKeyToIndex.put(key, idx);
                    }
                    idx++;
                }
            }
            edge_dict.put(pair.getEdgeRelation().name(), edges);

            ArrayList<Rel> subListEdgeAttr = (ArrayList<Rel>) pair.getEdgeAttributes();
            startIndex = 0; pfIndex = 0;
            if (subListEdgeAttr != null && !subListEdgeAttr.isEmpty()) {
                int edgeAttrSize = subListEdgeAttr.size();
                int[] edgeStartIndices = new int[edgeAttrSize];
                Rel[] edgeRels = new Rel[edgeAttrSize];
                boolean[] edgeIsCatRel = new boolean[edgeAttrSize];
                int[] edgeNumVals = new int[edgeAttrSize];

                num_col = 0;
                for (Rel r : subListEdgeAttr) {
                    boolean isCat = r instanceof CatRel;
                    edgeIsCatRel[pfIndex] = isCat;

                    int nVals = isCat ? (int) r.numvals() : 1;
                    edgeNumVals[pfIndex] = nVals;

                    if (isCat && cpmGnn.isOneHotEncoding())
                        num_col += r.numvals();
                    else
                        num_col += 1;

                    // for each feature, see where in the vector it starts
                    edgeStartIndices[pfIndex] = startIndex;
                    edgeRels[pfIndex] = r;
                    pfIndex++;
                    startIndex += isCat ? nVals : 1;
                }
                double[][] edge_attr = createOneHotEncodingMatrix(edges.get(0).size(), num_col);

                HashMap<String, HashMap<String, Object[]>> evalOfEdgesAttr = ggcnn.getEvalOfEdgeAttr();
                HashMap<String, Object[]> edgeAttrTable = (evalOfEdgesAttr != null) ? evalOfEdgesAttr.get(pftype) : null;
                if (edgeAttrTable != null) {
                    for (Object[] edgeattr : edgeAttrTable.values()) {
                        int argEdge = -2;
                        double value = Double.NaN;
                        int col = -1;

                        if (edgeattr.length > 1 && edgeattr[1] instanceof Number) {
                            argEdge = ((Number) edgeattr[1]).intValue();
                        }

                        if (edgeattr.length > 2 && edgeattr[2] instanceof Number) {
                            value = ((Number) edgeattr[2]).doubleValue();
                        }

                        if (edgeattr.length > 4 && edgeattr[4] instanceof Number) {
                            col = ((Number) edgeattr[4]).intValue();
                        } else {
                            throw new IllegalStateException("Cannot determine column index for edge attribute entry; check table layout.");
                        }

                        if (Double.isNaN(value)) {
                            if (edgeattr[0] instanceof GGCPMNode) {
                                GGCPMNode constructedchild = (GGCPMNode) edgeattr[0];
                                double[] valchild = constructedchild.evaluate(sno);
                                value = valchild[0];
                            } else {
                                continue;
                            }
                        }

                        int startingIndex = edgeStartIndices[col];
                        Rel r = edgeRels[col];
                        boolean isCat = edgeIsCatRel[col];

                        // determine which row(s) and columns to update
                        double matrixValue = 1.;
                        if (isCat) {
                            // if onehot encoding
                            col = (int) (value + startingIndex);
                        } else {
                            if (r.valtype() == Rel.NUMERIC || r.valtype() == Rel.BOOLEAN) {
                                col = startingIndex;
                                matrixValue = value;
                            }
                        }

                        if (argEdge == -1) {
                            // apply to all edges (all rows)
                            for (int ri = 0; ri < edge_attr.length; ri++) {
                                edge_attr[ri][col] = matrixValue;
                            }
                        } else {
                            String edgeKey = Arrays.toString((int[]) edgeattr[3]);
                            if (edgeKey != null) {
                                if (edgeKeyToIndex.containsKey(edgeKey)) {
                                    int index = edgeKeyToIndex.get(edgeKey);
                                    edge_attr[index][col] = matrixValue;
                                }
                            }
                        }
                    }
                }
                edgeAttr_dict.put(pair.getEdgeRelation().getTypesAsString(), edge_attr);
            }
        }
        return new Object[]{x_dict, edge_dict, edgeAttr_dict};
    }

    public double[] GGevaluate_gnnHetero(Integer sno, RelStruc A, OneStrucData inst, CatGnn cpmGnn, GGCPMNode ggcpmGnn) {
        SharedInterpreter interpreter = JepManager.getInterpreter(true);
        // mode torch model to the new interpreter
        if (torchModel.getModelInterpreter() != interpreter)
            torchModel = loadTorchModel(interpreter, currentCatGnn, scriptPath);

        CatGnn cpm = (CatGnn) cpmGnn;
        GGGnnNode ggcnn = (GGGnnNode) ggcpmGnn;

        Object[] inputGraph = GGconstructInputGraph(cpm, ggcnn, sno);
        Map<String, double[][]> x_dict = (Map<String, double[][]>) inputGraph[0];
        Map<String, ArrayList<ArrayList<Integer>>> edge_dict = (Map<String, ArrayList<ArrayList<Integer>>>) inputGraph[1];
        Map<String, double[][]> edgeAttr_dict = (Map<String, double[][]>) inputGraph[2];

        Object[] result = inferModelHetero(x_dict, edge_dict, edgeAttr_dict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId(), true);
        double[][] outProbs = (double[][]) result[0];

        if (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))
            return outProbs[0];
        else {
            // TODO riscky here! If we have more than one out types!
            String outType = cpm.getOutTypes().get(0);
            return outProbs[ggcnn.getNodeIndexIfPresent(outType, Integer.parseInt(cpmGnn.getArgument()))];
        }

//        if (!savedData || !ggcpmGnn.getIs_evaluated_val_for_samples()[0]) {
//            initGnnData(cpm, A, inst);
//            savedData = true;
//        }
//
//        if (GGnodeAttrDict.isEmpty()) {
//            GGnodeAttrDict = initNodeAttrDict(ggcpmGnn, cpm, relToNodeMap, nodeMap, GGsampledRel);
//            // we need to use the sampled values in the gradient graph structure (maxindicator) and assign them to the rel
//            // for GNNs the order of the features needs to be respected: the order in input_attr in CatGnn will be used for constructing the vector
//            GGnodeAttrDict = updateAttrDict(GGnodeAttrDict, cpm, ggcpmGnn, false);
//        }
//        if (GGedgeDict.isEmpty()) {
//            GGedgeDict = initEdgesDict(GGboolRel, GGsampledRel);
//            GGedgeDict = updateEdgeDict(GGedgeDict, cpm, ggcpmGnn);
//        }
//        if (GGedgeAttrDict.isEmpty() && relToEdgeAttrMap.size() > 0) {
//            GGedgeAttrDict = initEdgeAttrdict(cpm, relToEdgeAttrMap, GGsampledRel);
//            GGedgeAttrDict = updateAttrDict(GGedgeAttrDict, cpm, ggcpmGnn,true);
//        }
//
//        Object[] result = inferModelHetero(GGnodeAttrDict, GGedgeDict, GGedgeAttrDict, cpmGnn.getGnnInputs(), cpmGnn.getGnnId(), true);
//        double[][] outProbs = (double[][]) result[0];
//
//        if (cpmGnn.getArgument().equals("[]") || cpmGnn.getArgument().equals(""))
//            return outProbs[0];
//        else
//            return outProbs[Integer.parseInt(cpmGnn.getArgument())];
    }



    /**
     * Overload of inferModelHetero that supports edges as int[][] edge_index (shape [2][E]).
     * It converts to the existing internal format and delegates to the original method.
     */
    public Object[] inferModelHeteroInt(Map<String, double[][]> x_dict,
                                        Map<String, int[][]> edge_dict,
                                        Map<String, double[][]> edge_attr,
                                        List<TorchInputSpecs> gnnInputs,
                                        String idGnn,
                                        boolean valonly) {

        Map<String, ArrayList<ArrayList<Integer>>> convertedEdgeDict = toEdgeDictList(edge_dict);
        return inferModelHetero(x_dict, convertedEdgeDict, edge_attr, gnnInputs, idGnn, valonly);
    }

    /**
     * Utility: convert edge_index maps from int[][] (shape [E][2]) to ArrayList<ArrayList<Integer>>.
     */
    private static Map<String, ArrayList<ArrayList<Integer>>> toEdgeDictList(Map<String, int[][]> edgeDict) {
        if (edgeDict == null) return null;
        Map<String, ArrayList<ArrayList<Integer>>> out = new HashMap<>(edgeDict.size());
        for (Map.Entry<String, int[][]> e : edgeDict.entrySet()) {
            int[][] idx = e.getValue();
            if (idx == null) {
                out.put(e.getKey(), null);
                continue;
            }
            ArrayList<Integer> src = new ArrayList<>();
            ArrayList<Integer> dst = new ArrayList<>();
            for (int[] edge : idx) {
                if (edge == null || edge.length != 2) {
                    throw new IllegalArgumentException("edge_index for key '" + e.getKey() + "' must have shape [E][2]");
                }
                src.add(edge[0]);
                dst.add(edge[1]);
            }
            ArrayList<ArrayList<Integer>> pair = new ArrayList<>(2);
            pair.add(src);
            pair.add(dst);
            out.put(e.getKey(), pair);
        }
        return out;
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
            GGedgeAttrDict = new HashMap<>();
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
        GGnodeAttrDict = initNodeAttrDict(null, cpmGnn, relToNodeMap, nodeMap, GGsampledRel);
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

    private int[] extractParamArgs(String paramName) {
        int start = paramName.indexOf('(');
        int end = paramName.indexOf(')');
        if (start == -1 || end == -1 || end <= start) return null;

        String s = paramName;
        int i = start + 1;
        int limit = end;

        int count = 1;
        for (int k = i; k < limit; k++) {
            if (s.charAt(k) == ',') count++;
        }

        int[] out = new int[count];
        int idx = 0;

        while (i < limit) {
            // skip leading whitespace
            while (i < limit && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= limit) {
                // trailing whitespace only — that means there was an empty token -> error
                return null;
            }

            char c = s.charAt(i);

            // parse digits
            long val = 0;
            int digits = 0;
            while (i < limit) {
                c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    val = val * 10 + (c - '0');
                    digits++;
                    i++;
                } else break;
            }
            if (digits == 0) return null;
            if (val < Integer.MIN_VALUE || val > Integer.MAX_VALUE) return null;
            out[idx++] = (int) val;
            while (i < limit && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= limit) break;
            if (s.charAt(i) == ',') {
                i++; // consume comma and continue to next argument
                continue;
            } else {
                return null;
            }
        }

        if (idx != count) {
            return null;
        }
        return out;
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


