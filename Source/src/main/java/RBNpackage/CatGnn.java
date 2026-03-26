package RBNpackage;

import PyManager.*;
import RBNExceptions.RBNCompatibilityException;
import RBNLearning.GGCPMNode;
import RBNLearning.Gradient_Array;
import RBNLearning.Gradient_TreeMap;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;
import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;
import RBNutilities.rbnutilities;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CatGnn extends CPModel {
    // the order of attributes need to be respected! this order will be used for the gnn encoding
    private ArrayList<ArrayList<Rel>> input_attr;
    private ArrayList edge_attr;
    ArgTerm[] arguments;

    // true if we use one-hot encoding for the features representation
    private boolean oneHotEncoding;
    private GnnPy gnnPy;
    // each gnn will have an id that identify the model
    private String gnnId;
    String configModelPath;
    List<TorchInputSpecs> gnnInputs;

    TypedTorchPf typedTorchPf;

    // if is set to true, means that the GNN is for categorical output, if false is boolean
    private boolean categorical;
    private int numvals;
    private static boolean isInitialized = false;

    Type[] outTypes;

    private RelStruc lastDictA = null;
    private OneStrucData lastDictInst = null;
    private int lastDictInstHash = 0;
    private Object[] lastDicts = null;

    private long dictVersion = 0;
    private long cachedDictVersion = -1;

    private Map<String, Map<Integer, Integer>> nodeMappingByType;

    private boolean optimizeForInput;

    private static final Map<Long, Object[]> sharedDictCache = new ConcurrentHashMap<>();
    private static final Map<Long, Boolean> sharedEvalInputs = new ConcurrentHashMap<>();

    public CatGnn(ArgTerm[] arguments, String gnnId, int numLayers, int numvals, ArrayList input_attr, ArrayList edge_attr, String gnn_inference, boolean oneHotEncoding) {
        this.arguments = arguments;
        this.gnnId = gnnId;
        this.categorical = true;
        this.numvals = numvals;
        this.input_attr = input_attr;
        this.edge_attr = edge_attr;
        this.oneHotEncoding = oneHotEncoding;
        isInitialized = false;
    }

    public CatGnn(String configModelPath, ArgTerm[] arguments, int numVals, List<TorchInputSpecs> inputs, TypedTorchPf typedTorchPf, Type[] outTypes, boolean optimOneInp, boolean withGnnPy) {
        File f = new File(configModelPath);
        // get a file name without extension
        int lastIndexOfDot = f.getName().lastIndexOf('.');
        if (lastIndexOfDot == -1)
            this.gnnId = f.getName(); // No extension found
        else
            this.gnnId = f.getName().substring(0, lastIndexOfDot);

        this.arguments = arguments.clone();

        this.categorical = true;
        this.configModelPath = f.getParent();
        this.numvals = numVals;
        this.gnnInputs = inputs;
        this.typedTorchPf = typedTorchPf;
        this.outTypes = outTypes;
        this.nodeMappingByType = new HashMap<>();

        this.oneHotEncoding = true; // this for now it is always true, later we need to add this to the RBN specification

        if (withGnnPy)
            this.gnnPy = new GnnPy(this, f.getParent());

        isInitialized = false;
        optimizeForInput = optimOneInp;
    }

    //eval-result holder
    private record InputEntry(List<int[]> argNodes, double evalValue, int pfIdx) {}

    private long sharedDictKey(RelStruc A, OneStrucData inst) {
        return  Objects.hash(gnnInputs.hashCode(), A.hashCode(), inst.hashCode());
    }

    // very similar logic to GGGnnNode
    public Object[] buildInputDicts(RelStruc A,
                                    OneStrucData inst,
                                    ArgTerm[] vars,
                                    int[] tuple,
                                    int gradindx,
                                    boolean useCurrentCvals,
                                    boolean useCurrentPvals,
                                    HashMap<Rel, GroundAtomList> mapatoms,
                                    boolean useCurrentMvals,
                                    HashMap<String, Object[]> evaluated,
                                    HashMap<String, Integer> params,
                                    int returntype,
                                    boolean valonly,
                                    Profiler profiler)
            throws RBNCompatibilityException {

        if (isOptimizeForOneInput()) {
            long key = sharedDictKey(A, inst);
            Object[] cached = sharedDictCache.get(key);
            if (cached != null) {
                // restore nodeMappingByType so getNodeIndexIfPresent works on this instance
                Map<String, Map<Integer, Integer>> sharedMapping = (Map<String, Map<Integer, Integer>>) cached[3];
                this.nodeMappingByType = sharedMapping;
                // x_dict, edge_dict, edgeAttr_dict
                Object[] result = new Object[]{cached[0], cached[1], cached[2]};
                return result;
            }
        }

        if (isCacheValid(A, inst)) {
            return lastDicts;
        }

        Map<String, Integer> nextIndexByType = new HashMap<>();

        Map<String, Map<String, InputEntry>> evalEdge = new HashMap<>();
        Map<String, Map<String, InputEntry>> evalNode = new HashMap<>();
        Map<String, Map<String, InputEntry>> evalEdgeAttr = new HashMap<>();

        // EDGES build subgraph topology, assign compact indices
        for (String pftype : typedTorchPf.getTypedNames()) {
            for (TorchInputPf tip : typedTorchPf.getCombines(pftype)) {
                int[][] subslist = tip.tuplesSatisfyingCConstr(A, new ArgTerm[0], new int[0]);

                for (int pi = 0; pi < tip.getPfargsEdge().length; pi++) {
                    CPModel edgePf = tip.getPfargsEdgeAt(pi);
                    for (int[] sub : subslist) {
                        CPModel ground = edgePf.substitute(tip.getQuantvars(), sub);
                        List<int[]> argNodes = resolveEdgeArgIds(ground);
                        if (argNodes == null) continue;

                        // assign new compact indices for both endpoints
                        if (!remapEdgeToCompact(argNodes, edgePf, nodeMappingByType, nextIndexByType, true))
                            continue;

                        double val = evalGroundPf(ground, A, inst, vars, tuple, gradindx, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, params, returntype, valonly, profiler);
                        evalEdge.computeIfAbsent(pftype.intern(), k -> new HashMap<>()).put(ground.makeKey(A), new InputEntry(argNodes, val, pi));
                    }
                }
            }
        }

        // Build edge_dict
        Map<String, ArrayList<ArrayList<Integer>>> edge_dict = new HashMap<>();
        for (TorchInputSpecs spec : gnnInputs) {
            ArrayList<Integer> srcs = new ArrayList<>(), dsts = new ArrayList<>();
            for (InputEntry e : evalEdge.getOrDefault(spec.getType(), Collections.emptyMap()).values()) {
                for (int[] ep : e.argNodes())
                    if (ep.length == 2 && ep[0] >= 0 && ep[1] >= 0) {
                        srcs.add(ep[0]);
                        dsts.add(ep[1]);
                    }
            }
            ArrayList<ArrayList<Integer>> el = new ArrayList<>();
            el.add(srcs);
            el.add(dsts);
            edge_dict.put(spec.getEdgeRelation().name(), el);
        }

        // NODES only for nodes already in the subgraph
        for (String pftype : typedTorchPf.getTypedNames()) {
            for (TorchInputPf tip : typedTorchPf.getCombines(pftype)) {
                int[][] subslist = tip.tuplesSatisfyingCConstr(A, new ArgTerm[0], new int[0]);

                for (int pi = 0; pi < tip.getPfargsNode().length; pi++) {
                    CPModel nodePf = tip.getPfargsNodeAt(pi);
                    for (int[] sub : subslist) {
                        CPModel ground = nodePf.substitute(tip.getQuantvars(), sub);
                        List<int[]> argNodes = resolveNodeArgIds(ground, nodePf, sub, tip.getQuantvars(), A);
                        if (argNodes == null) continue;

                        // look-up only, skip if node not in subgraph
                        if (!remapNodeToCompact(argNodes, pftype, nodeMappingByType, nextIndexByType, false))
                            continue;

                        double val = evalGroundPf(ground, A, inst, vars, tuple, gradindx, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, params, returntype, valonly, profiler);
                        evalNode.computeIfAbsent(pftype.intern(), k -> new HashMap<>()).put(ground.makeKey(A), new InputEntry(argNodes, val, pi));
                    }
                }
            }
        }

        // fallback: no edges, assign compact indices for every node
        if (nodeMappingByType.isEmpty()) {
            for (String pftype : typedTorchPf.getTypedNames()) {
                for (TorchInputPf tip : typedTorchPf.getCombines(pftype)) {
                    int[][] subslist =
                            tip.tuplesSatisfyingCConstr(A, new ArgTerm[0], new int[0]);
                    for (int pi = 0; pi < tip.getPfargsNode().length; pi++) {
                        CPModel nodePf = tip.getPfargsNodeAt(pi);
                        for (int[] sub : subslist) {
                            CPModel ground = nodePf.substitute(tip.getQuantvars(), sub);
                            List<int[]> argNodes = resolveNodeArgIds(ground, nodePf, sub, tip.getQuantvars(), A);
                            if (argNodes == null) continue;
                            remapNodeToCompact(argNodes, pftype, nodeMappingByType, nextIndexByType, true);
                            double val = evalGroundPf(ground, A, inst, vars, tuple, gradindx, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, params, returntype, valonly, profiler);
                            evalNode.computeIfAbsent(pftype.intern(), k -> new HashMap<>()).put(ground.makeKey(A), new InputEntry(argNodes, val, pi));
                        }
                    }
                }
            }
        }

        // EDGE ATTRIBUTES
        for (String pftype : typedTorchPf.getTypedNames()) {
            for (TorchInputPf tip : typedTorchPf.getCombines(pftype)) {
                int[][] subslist = tip.tuplesSatisfyingCConstr(A, new ArgTerm[0], new int[0]);

                for (int pi = 0; pi < tip.getPfargsEdgeAttr().length; pi++) {
                    CPModel eaPf = tip.getPfargsEdgeAttrAt(pi);
                    for (int[] sub : subslist) {
                        CPModel ground = eaPf.substitute(tip.getQuantvars(), sub);
                        List<int[]> argNodes = resolveEdgeAttrArgIds(ground, sub);
                        if (argNodes == null) continue;

                        if (!remapEdgeAttrToCompact(argNodes, pftype, nodeMappingByType))
                            continue;

                        double val = evalGroundPf(ground, A, inst, vars, tuple, gradindx, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, params, returntype, valonly, profiler);
                        evalEdgeAttr.computeIfAbsent(pftype.intern(), k -> new HashMap<>()).put(ground.makeKey(A), new InputEntry(argNodes, val, pi));
                    }
                }
            }
        }

        // Build x_dict
        Map<String, double[][]> x_dict = new HashMap<>();
        for (TorchInputSpecs spec : gnnInputs) {
            @SuppressWarnings("unchecked")
            List<Rel> attrs = (List<Rel>) spec.getNodeAttributes();
            if (attrs == null || attrs.isEmpty()) continue;

            String pftype = spec.getType();
            int uniqueNodes = Math.max(1, nextIndexByType.getOrDefault(pftype, 0));
            int[] starts = colStarts(attrs, oneHotEncoding);
            int numCols = totalCols(attrs, oneHotEncoding);
            double[][] mat = new double[uniqueNodes][numCols];

            for (InputEntry e : evalNode.getOrDefault(pftype, Collections.emptyMap()).values()) {
                double value = e.evalValue();
                if (Double.isNaN(value)) continue;

                int argNode = e.argNodes().get(0)[0];
                Rel r = attrs.get(e.pfIdx());
                int col = starts[e.pfIdx()];
                double cell = 1.0;

                if (r instanceof CatRel && value >= 0)
                    col = (int) (value + starts[e.pfIdx()]);
                else if (r.valtype() == Rel.NUMERIC || r.valtype() == Rel.BOOLEAN)
                    cell = value;

                if (argNode < 0) for (int row = 0; row < uniqueNodes; row++) mat[row][col] = cell;
                else mat[argNode][col] = cell;
            }
            x_dict.put(pftype, mat);
        }

        // Build edgeAttr_dict
        Map<String, double[][]> edgeAttr_dict = new HashMap<>();
        for (TorchInputSpecs spec : gnnInputs) {
            @SuppressWarnings("unchecked")
            List<Rel> attrs = (List<Rel>) spec.getEdgeAttributes();
            if (attrs == null || attrs.isEmpty()) continue;

            String pftype = spec.getType();
            String edgeKey = spec.getEdgeRelation().name();
            ArrayList<ArrayList<Integer>> edgeList = edge_dict.get(edgeKey);

            int numEdges = Math.max(1, edgeList != null && !edgeList.isEmpty() ? edgeList.get(0).size() : 0);
            int[] starts = colStarts(attrs, oneHotEncoding);
            int numCols = totalCols(attrs, oneHotEncoding);
            double[][] mat = new double[numEdges][numCols];

            for (InputEntry e : evalEdgeAttr.getOrDefault(pftype, Collections.emptyMap()).values()) {
                double value = e.evalValue();
                if (Double.isNaN(value)) continue;

                int[] endpoints = e.argNodes().get(0);
                Rel r = attrs.get(e.pfIdx());
                int col = starts[e.pfIdx()];
                double cell = 1.0;

                if (r instanceof CatRel)
                    col = (int) (value + starts[e.pfIdx()]);
                else if (r.valtype() == Rel.NUMERIC || r.valtype() == Rel.BOOLEAN)
                    cell = value;

                if (endpoints.length < 2 || endpoints[0] < 0)
                    for (int row = 0; row < numEdges; row++) mat[row][col] = cell;
                else {
                    int edgeRow = findEdgeRow(edge_dict, edgeKey, endpoints[0], endpoints[1]);
                    if (edgeRow >= 0) mat[edgeRow][col] = cell;
                }
            }
            edgeAttr_dict.put(pftype, mat);
        }

        Object[] result = new Object[]{x_dict, edge_dict, edgeAttr_dict};
        if (isOptimizeForOneInput()) {
            Object[] toCache = new Object[]{x_dict, edge_dict, edgeAttr_dict, nodeMappingByType};
            sharedDictCache.put(sharedDictKey(A, inst), toCache);
        }

        // store cache
        lastDicts = result;
        lastDictA = A;
        lastDictInst = inst;
        lastDictInstHash = inst != null ? inst.hashCode() : 0;
        cachedDictVersion = dictVersion;
        return result;
    }


    /**
     * Evaluate a fully-grounded CPModel eagerly
     */
    private static double evalGroundPf(CPModel ground, RelStruc A,
        OneStrucData inst,
        ArgTerm[] vars,
        int[] tuple,
        int gradindx,
        boolean useCurrentCvals,
        boolean useCurrentPvals,
        HashMap<Rel, GroundAtomList> mapatoms,
        boolean useCurrentMvals,
        HashMap<String, Object[]> evaluated,
        HashMap<String, Integer> params,
        int returntype,
        boolean valonly,
        Profiler profiler) {

        try {
            Object[] res = ground.evaluate(A, inst, vars, tuple, gradindx, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, params, returntype, valonly, profiler);
            return (double) res[0];
        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate grounded ProbForm: " + ground, e);
//            return Double.NaN;
        }
    }

    /**
     * Returns raw (src, dst) node-id pairs for a grounded edge pf.
     */
    private static List<int[]> resolveEdgeArgIds(CPModel ground)
            throws RBNCompatibilityException {

        if (ground instanceof ProbFormAtom pfa) {
            if (pfa.getArguments().length != 2)
                throw new RBNCompatibilityException("Edge ProbFormAtom must have exactly 2 arguments");

            ArgTerm[] args = pfa.getArguments();
            return List.of(new int[]{
                    Integer.parseInt(args[0].argEval()),
                    Integer.parseInt(args[1].argEval())});
        }
        if (ground instanceof ProbFormBoolComposite composite) {
            List<int[]> result = new ArrayList<>();
            for (int i = 0; i < composite.numComponents(); i++)
                if (composite.componentAt(i) instanceof ProbFormBoolAtom atom) {
                    ArgTerm[] args = atom.getArguments();
                    result.add(new int[]{
                            Integer.parseInt(args[0].argEval()),
                            Integer.parseInt(args[1].argEval())});
                }
            return result;
        }
        return null;
    }

    /**
     * Returns the single raw node id for a grounded node-feature pf.
     */
    private static List<int[]> resolveNodeArgIds(CPModel ground, CPModel pf, int[] sub, ArgTerm[] qv, RelStruc A) {
        int nodeId;
        if (ground instanceof ProbFormAtom pfa) {
            nodeId = pfa.getArguments().length == 1 ? Integer.parseInt(pfa.getArguments()[0].argEval()) : -1;

        } else if (ground instanceof ProbFormMacroCall pmc) {
            nodeId = pmc.args().length == 1 ? Integer.parseInt(pmc.args()[0].argEval()) : -1;

        } else if (ground instanceof ProbFormCombFunc) {
            if (sub.length == 1) nodeId = sub[0];
            else if (sub.length == 2) nodeId = resolveReferringArgId(ground, pf, qv, sub, A);
            else return null;
        } else {
            nodeId = -1; // ProbFormConstant or unknown, broadcast
        }
        return List.of(new int[]{nodeId});
    }

    /**
     * Mirror of GGGnnNode.resolveReferringArg — picks which index in a 2-tuple is "this node".
     */
    private static int resolveReferringArgId(CPModel ground, CPModel pf, ArgTerm[] qv, int[] sub, RelStruc A) {
        String groundStr = ground.asString(0, 0, A, false, false);
        ArgTerm[] qv00 = {qv[0], qv[0]};
        String g0Str = pf.substitute(qv00, sub).asString(0, 0, A, false, false);

        if (g0Str.equals(groundStr)) {
            ArgTerm[] qv11 = {new VarTerm(qv[0].argEval() + qv[0].argEval()), new VarTerm(qv[0].argEval() + qv[0].argEval())};

            String g1Str = pf.substitute(qv11, sub).asString(0, 0, A, false, false);
            return g0Str.equals(g1Str) ? -1 : sub[0];
        }
        return sub[1];
    }

    /**
     * Returns raw endpoint ids for a grounded edge-attribute pf.
     */
    private static List<int[]> resolveEdgeAttrArgIds(CPModel ground, int[] sub) {
        if (ground instanceof ProbFormConstant) return List.of(new int[]{-1});
        if (sub.length == 2) return List.of(new int[]{sub[0], sub[1]});
        return List.of(new int[]{-1});
    }

    /**
     * Assigns or looks up compact indices for both endpoints of every argNode pair.
     */
    private static boolean remapEdgeToCompact(
            List<int[]> argNodes, CPModel pf,
            Map<String, Map<Integer, Integer>> nodeMapping,
            Map<String, Integer> nextIndex,
            boolean assignNewIndices) {

        int compIdx = 0;
        for (int[] argNode : argNodes) {
            if (argNode[0] < 0) {
                compIdx++;
                continue;
            }

            if (pf instanceof ProbFormAtom pfa && pfa.getArguments().length == 2) {
                String t0 = pfa.getRelation().getTypes()[0].getName();
                String t1 = pfa.getRelation().getTypes()[1].getName();
                argNode[0] = assignNewIndices
                        ? getOrAssignIdx(nodeMapping, nextIndex, t0, argNode[0])
                        : lookupOrMinusOne(nodeMapping, t0, argNode[0]);
                argNode[1] = assignNewIndices
                        ? getOrAssignIdx(nodeMapping, nextIndex, t1, argNode[1])
                        : lookupOrMinusOne(nodeMapping, t1, argNode[1]);

            } else if (pf instanceof ProbFormBoolComposite composite
                    && composite.componentAt(compIdx) instanceof ProbFormBoolAtom atom) {
                String t0 = atom.getRelation().getTypes()[0].getName();
                String t1 = atom.getRelation().getTypes()[1].getName();
                argNode[0] = assignNewIndices
                        ? getOrAssignIdx(nodeMapping, nextIndex, t0, argNode[0])
                        : lookupOrMinusOne(nodeMapping, t0, argNode[0]);
                argNode[1] = assignNewIndices
                        ? getOrAssignIdx(nodeMapping, nextIndex, t1, argNode[1])
                        : lookupOrMinusOne(nodeMapping, t1, argNode[1]);
            }

            if (argNode[0] < 0 || argNode[1] < 0) return false;
            compIdx++;
        }
        return true;
    }

    /**
     * Assigns or looks up compact indices for node-feature argNodes.
     * Mirrors the {@code NODE} branch of {@code GGGnnNode.remapToCompactIndices}.
     */
    private static boolean remapNodeToCompact(
            List<int[]> argNodes, String pftype,
            Map<String, Map<Integer, Integer>> nodeMapping,
            Map<String, Integer> nextIndex,
            boolean assignNewIndices) {

        for (int[] argNode : argNodes) {
            if (argNode[0] < 0) continue;
            if (assignNewIndices) {
                argNode[0] = getOrAssignIdx(nodeMapping, nextIndex, pftype, argNode[0]);
            } else {
                Integer mapped = lookupOrNull(nodeMapping, pftype, argNode[0]);
                if (mapped == null) return false; // node not in subgraph → skip
                argNode[0] = mapped;
            }
        }
        return true;
    }

    /**
     * Looks up compact src/dst indices for an edge-attribute tuple.
     * Returns {@code false} if either endpoint is absent from the subgraph.
     * Mirrors the {@code EDGE_ATTR} branch of {@code GGGnnNode.remapToCompactIndices}.
     */
    private static boolean remapEdgeAttrToCompact(List<int[]> argNodes, String pftype, Map<String, Map<Integer, Integer>> nodeMapping) {

        for (int[] argNode : argNodes) {
            if (argNode.length == 2 && argNode[0] >= 0) {
                Integer src = lookupOrNull(nodeMapping, pftype, argNode[0]);
                Integer dst = lookupOrNull(nodeMapping, pftype, argNode[1]);
                if (src == null || dst == null) return false;
                argNode[0] = src;
                argNode[1] = dst;
            }
        }
        return true;
    }


    private static int getOrAssignIdx(Map<String, Map<Integer, Integer>> nodeMapping,
                                      Map<String, Integer> nextIndex,
                                      String type, int nodeId) {
        String t = type.intern();
        return nodeMapping.computeIfAbsent(t, k -> new HashMap<>())
                .computeIfAbsent(nodeId, id -> {
                    int n = nextIndex.getOrDefault(t, 0);
                    nextIndex.put(t, n + 1);
                    return n;
                });
    }

    private static Integer lookupOrNull(Map<String, Map<Integer, Integer>> nodeMapping, String type, int nodeId) {
        Map<Integer, Integer> m = nodeMapping.get(type);
        return m != null ? m.get(nodeId) : null;
    }

    private static int lookupOrMinusOne(Map<String, Map<Integer, Integer>> nodeMapping, String type, int nodeId) {
        Integer v = lookupOrNull(nodeMapping, type, nodeId);
        return v != null ? v : -1;
    }

    private static int[] colStarts(List<Rel> attrs, boolean oneHot) {
        int[] starts = new int[attrs.size()];
        int col = 0;
        for (int i = 0; i < attrs.size(); i++) {
            starts[i] = col;
            Rel r = attrs.get(i);
            col += (oneHot && r instanceof CatRel) ? (int) r.numvals() : 1;
        }
        return starts;
    }

    private static int totalCols(List<Rel> attrs, boolean oneHot) {
        int col = 0;
        for (Rel r : attrs)
            col += (oneHot && r instanceof CatRel) ? (int) r.numvals() : 1;
        return col;
    }

    private static int findEdgeRow(Map<String, ArrayList<ArrayList<Integer>>> edge_dict, String edgeKey, int src, int dst) {
        ArrayList<ArrayList<Integer>> edgeList = edge_dict.get(edgeKey);
        if (edgeList == null || edgeList.size() < 2) return -1;
        ArrayList<Integer> sources = edgeList.get(0), dests = edgeList.get(1);
        for (int i = 0; i < sources.size(); i++)
            if (sources.get(i) == src && dests.get(i) == dst) return i;
        return -1;
    }

    private boolean isCacheValid(RelStruc A, OneStrucData inst) {
        if (lastDicts == null) return false;
        if (cachedDictVersion != dictVersion) return false;
        if (lastDictA != A) return false;

        int instHash = (inst != null) ? inst.hashCode() : 0;
        if (lastDictInstHash != instHash) return false;

        if (lastDictInst != null && lastDictInst == inst) return true;

        return true;
    }

    public void invalidateDictCache() {
        dictVersion++;
        if (isOptimizeForOneInput()) {
            sharedDictCache.clear();
        }
    }

    public Integer getNodeIndexIfPresent(String type, int nodeId) {
        Map<Integer, Integer> mapping = nodeMappingByType.get(type);
        return mapping != null ? mapping.get(nodeId) : null;
    }

    @Override
    public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias) {
        StringBuilder sb = new StringBuilder();
        sb.append("GNN("+Arrays.toString(this.arguments)+")=");
        sb.append("[");
        sb.append(gnnPy.getTorchModel().toString()+", ");
        if (gnnInputs != null && !gnnInputs.isEmpty()) {
            for (TorchInputSpecs pair : gnnInputs)
                sb.append(pair.toString()).append(", ");
            sb.setLength(sb.length() - 2);
        } else
            sb.append("null");
        sb.append("]");
        return sb.toString();
    }

    @Override
    public CPModel conditionEvidence(RelStruc A, OneStrucData inst) throws RBNCompatibilityException {
        // RAF: This is not required for gnn since it does not have direct dependencies
        System.out.println("conditionEvidence code");
        return null;
    }

    @Override
    public boolean dependsOn(String variable, RelStruc A, OneStrucData data) throws RBNCompatibilityException {
        System.out.println("dependsOn code");
        return false;
    }

    @Override
    public Object[] evaluate(RelStruc A,
                             OneStrucData inst,
                             ArgTerm[] vars,
                             int[] tuple,
                             int gradindx,
                             boolean useCurrentCvals,
                             boolean useCurrentPvals,
                             HashMap<Rel, GroundAtomList> mapatoms,
                             boolean useCurrentMvals,
                             HashMap<String, Object[]> evaluated,
                             HashMap<String, Integer> params,
                             int returntype,
                             boolean valonly,
                             Profiler profiler)
            throws RBNCompatibilityException {

        // CHECK IF THIS DOES NOT BREAK INFERENCE WITH MAP or MCMC

        // remove this later!
        if (isOptimizeForOneInput()) {
            long key = sharedDictKey(A, inst);
            Boolean cached = sharedEvalInputs.get(key);
            if (cached != null && cached) {
                return new Object[]{Double.NaN};
            }

            if (cached == null) {
                for (TorchInputPf inps : getTypedTorchPf().getCombines()) {
                    Object[] res = inps.evaluate(A, inst, vars, tuple, gradindx, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, params, returntype, valonly, profiler);
                    // if res[0] contains NaN return res
                    if (res[0] instanceof Double) {
                        if (Double.isNaN((Double) res[0])) {
                            return res;
                        }
                    }
                }
            }
            sharedEvalInputs.put(key, false);
        } else {
            for (TorchInputPf inps : getTypedTorchPf().getCombines()) {
                Object[] res = inps.evaluate(A, inst, vars, tuple, gradindx, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, params, returntype, valonly, profiler);
                // if res[0] contains NaN return res
                if (res[0] instanceof Double) {
                    if (Double.isNaN((Double) res[0])) {
                        return res;
                    }
                }
            }
        }

        CatGnn subCatGnn = null;
        if (this instanceof CatGnnBool)
            subCatGnn = (CatGnnBool)this.substitute(vars, tuple);
        else
            subCatGnn = (CatGnn)this.substitute(vars, tuple);

        Object[] dicts = subCatGnn.buildInputDicts(A, inst, vars, tuple, gradindx, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, params, returntype, valonly, profiler);
        Object[] res = gnnPy.evaluate_gnnHetero((Map<String, double[][]>) dicts[0],
                (Map<String, ArrayList<ArrayList<Integer>>>) dicts[1],
                (Map<String, double[][]>) dicts[2], subCatGnn, valonly);

//        Object[] res = gnnPy.evaluate_gnnHetero(A, inst, subCatGnn, valonly);

        if (subCatGnn instanceof CatGnnBool) {
            double[] trueProb = (double[]) res[0];
            res[0] = trueProb[0];
        }
        if (!(subCatGnn instanceof CatGnnBool) && this.numvals() == 1) {
            double[] trueProb = (double[]) res[0];
            double[] resultArray =  new double[] {1-trueProb[0],trueProb[0]};
            res[0] = resultArray;
        }

        if (!valonly) {
            res[1] = null;
            if (returntype == ProbForm.RETURN_ARRAY)
                res[1] = new Gradient_Array(params);
            else
                res[1] = new Gradient_TreeMap(params);
        }

        return res;
    }

    @Override
    public double[] evalSample(RelStruc A, HashMap<String, PFNetworkNode> atomhasht, OneStrucData inst, HashMap<String,double[]> evaluated, long[] timers) throws RBNCompatibilityException {
        if (!isInitialized) {
            this.gnnPy.initGnnData(this, A, inst);
            isInitialized = true;
        }
        double[] resGnn = gnnPy.evalSample_gnn(this, atomhasht);
        if (this.numvals() == 1) {
            double[] resultArray =  new double[] {1-resGnn[0],resGnn[0]};
            return resultArray;
        }
        return resGnn;
    }

    @Override
    public ArgTerm[] freevars() {
        Vector<ArgTerm> freevars = new Vector<>();
        for (TorchInputPf inps: getTypedTorchPf().getCombines()) {
            ArgTerm[] res = inps.getCconstr().freevars();
            freevars.addAll(Arrays.asList(res));
        }
        return freevars.toArray(new ArgTerm[0]);
    }

    @Override
    public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone) throws RBNCompatibilityException {
        Vector result = new Vector();
        for (TorchInputPf inps: getTypedTorchPf().getCombines()) {
            CPModel nextprobform;
            int[][] subslist = A.allTrue(inps.getCconstr(), inps.getQuantvars());

            for (int i = 0; i < inps.getPfargs().length; i++) {
                for (int j = 0; j < subslist.length; j++) {
                    nextprobform = inps.getPfargs()[i].substitute(inps.getQuantvars(), subslist[j]);
                    result = rbnutilities.combineAtomVecs(result, nextprobform.makeParentVec(A, inst, macrosdone));
                }
            }
        }
        return result;
    }

    @Override
    public int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, HashMap<String, GroundAtom> atomhasht) throws RBNCompatibilityException {
        return 0;
    }

    @Override
    public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
        return 0;
    }

    @Override
    public boolean multlinOnly() {
        System.out.println("multlinOnly code");
        return true;
    }

    @Override
    public String[] parameters() {
//        System.out.println("parameters code");
        return new String[0];
    }

    /**
     * Returns a ProbForm in which the dependence on A
     * is already pre-evaluated (substitution lists in
     * combination functions, and values of ProbFormSFormula)
     */
    @Override
    public CPModel sEval(RelStruc A) throws RBNCompatibilityException {
//        System.out.println("sEval code");
        return this;
    }

    @Override
    public CPModel substitute(String[] vars, int[] args) {
        TypedTorchPf newpf = this.typedTorchPf.substitute(vars, args);

        CatGnn result;
        if (this instanceof CatGnnBool)
            result = new CatGnnBool(this.configModelPath, this.arguments, this.gnnInputs, newpf, this.outTypes, false);
        else
            result = new CatGnn(this.configModelPath, this.arguments, this.numvals, this.gnnInputs, newpf, this.outTypes, this.isOptimizeForOneInput(), false);

        result.setGnnPy(this.getGnnPy());

        if (vars.length != 0)
            result.arguments = rbnutilities.array_substitute(arguments, vars, args);
//        else
//            result.argument = Arrays.toString(new String[0]);

        if (this.alias != null)
            result.setAlias(this.alias.substitute(vars, args));

        return result;
    }

    @Override
    public CPModel substitute(String[] vars, String[] args) {
        TypedTorchPf newpf = this.typedTorchPf.substitute(vars, args);

        CatGnn result;
        if (this instanceof CatGnnBool)
            result = new CatGnnBool(this.configModelPath, this.arguments, this.gnnInputs, newpf, this.outTypes, false);
        else
            result = new CatGnn(this.configModelPath, this.arguments, this.numvals, this.gnnInputs, newpf, this.outTypes, this.isOptimizeForOneInput(), false);

        result.setGnnPy(this.getGnnPy());

        if (vars.length != 0)
            result.arguments = rbnutilities.array_substitute(arguments, vars, args);


        if (this.alias != null)
            result.setAlias(this.alias.substitute(vars, args));

        return result;
    }

    @Override
    public CPModel substitute(String[] vars, ArgTerm[] args) {
        TypedTorchPf newpf = this.typedTorchPf.substitute(vars, args);

        CatGnn result;
        if (this instanceof CatGnnBool)
            result = new CatGnnBool(this.configModelPath, this.arguments, this.gnnInputs, newpf, this.outTypes, false);
        else
            result = new CatGnn(this.configModelPath, this.arguments, this.numvals, this.gnnInputs, newpf, this.outTypes, this.isOptimizeForOneInput(), false);

        result.setGnnPy(this.getGnnPy());

        if (vars.length != 0)
            result.arguments = rbnutilities.array_substitute(arguments, vars, args);


        if (this.alias != null)
            result.setAlias(this.alias.substitute(vars, args));

        return result;
    }

    @Override
    public CPModel substitute(ArgTerm[] vars, ArgTerm[] args) {
        TypedTorchPf newpf = this.typedTorchPf.substitute(vars, args);

        CatGnn result;
        if (this instanceof CatGnnBool)
            result = new CatGnnBool(this.configModelPath, this.arguments, this.gnnInputs, newpf, this.outTypes, false);
        else
            result = new CatGnn(this.configModelPath, this.arguments, this.numvals, this.gnnInputs, newpf, this.outTypes, this.isOptimizeForOneInput(), false);

        result.setGnnPy(this.getGnnPy());

        if (vars.length != 0)
            result.arguments = rbnutilities.array_substitute(arguments, vars, args);


        if (this.alias != null)
            result.setAlias(this.alias.substitute(vars, args));

        return result;
    }

    @Override
    public CPModel substitute(ArgTerm[] vars, int[] args) {
        TypedTorchPf newpf = this.typedTorchPf.substitute(vars, args);

        CatGnn result;
        if (this instanceof CatGnnBool)
            result = new CatGnnBool(this.configModelPath, this.arguments, this.gnnInputs, newpf, this.outTypes, false);
        else
            result = new CatGnn(this.configModelPath, this.arguments, this.numvals, this.gnnInputs, newpf, this.outTypes, this.isOptimizeForOneInput(), false);

        result.setGnnPy(this.getGnnPy());

        if (vars.length != 0)
            result.arguments = rbnutilities.array_substitute(arguments, vars, args);


        if (this.alias != null)
            result.setAlias(this.alias.substitute(vars, args));

        return result;
    }

    @Override
    public void setCvals(String paramname, double val) {
        System.out.println("setCvals code");
    }

    @Override
    public TreeSet<Rel> parentRels() {
//        System.out.println("parentRels code 1");
        TreeSet<Rel> result = new TreeSet<Rel>();
        for (TorchInputPf inps: getTypedTorchPf().getCombines()) {
            result.addAll(inps.parentRels());
        }
        return result;
    }

    @Override
    public TreeSet<Rel> parentRels(TreeSet<String> processed) {
        System.out.println("parentRels code 2");
        TreeSet<Rel> result = new TreeSet<Rel>();
        assert !processed.isEmpty(); // when it is used?
        for (TorchInputPf inps: getTypedTorchPf().getCombines()) {
            result.addAll(inps.parentRels());
        }
        return result;
    }

    public TypedTorchPf getTypedTorchPf() {
        return typedTorchPf;
    }

    @Override
    public int numvals() {
        return numvals;
    }

    public GnnPy getGnnPy() {
        return gnnPy;
    }

    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }

    public List<TorchInputSpecs> getGnnInputs() {
        return this.gnnInputs;
    }

    public ArgTerm[] getArguments() {
        return arguments;
    }

    public String getGnnId() {
        return gnnId;
    }

    public boolean isOneHotEncoding() {
        return oneHotEncoding;
    }

    public boolean isBoolean() { return !categorical; }

    public Type[] getOutTypes() { return outTypes; }

    public boolean isOptimizeForOneInput() {
        return optimizeForInput;
    }
}
