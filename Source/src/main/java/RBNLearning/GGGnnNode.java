package RBNLearning;

import PyManager.GnnPy;
import PyManager.PyUtils.EvalEntry;
import PyManager.TorchInputPf;
import PyManager.TorchInputSpecs;
import PyManager.TypedTorchPf;
import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNNaNException;
import RBNpackage.*;
import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;

import java.util.*;

public class GGGnnNode extends GGCPMNode {

    // Mode enum — replaces magic int constants 0 / 1 / 2
    public enum Mode {NODE, EDGE_ATTR, EDGE}

    private final CatGnn cpmgnn;
    private GnnPy gnnPy;

    private final RelStruc A;
    private final OneStrucData inst;

    // Per-type node mapping: typeName -> (nodeId -> compactIndex from 0)
    private final Map<String, Map<Integer, Integer>> nodeMappingByType = new HashMap<>();
    private final Map<String, Integer> nextIndexByType = new HashMap<>();

    // Internal storage: pftype -> subkey -> EvalEntry
    private final Map<String, Map<String, EvalEntry>> evalOfNodesByType = new HashMap<>();
    private final Map<String, Map<String, EvalEntry>> evalOfEdgeAttrByType = new HashMap<>();
    private final Map<String, Map<String, EvalEntry>> evalOfEdgeByType = new HashMap<>();

    private final Set<GGCPMNode> childrenSet = new LinkedHashSet<>();

    private final Map<String, double[][]> x_dict = new HashMap<>();
    private final Map<String, ArrayList<ArrayList<Integer>>> edge_dict = new HashMap<>();
    private final Map<String, double[][]> edgeAttr_dict = new HashMap<>();

    public Map<String, double[][]> getXDict() {
        return Collections.unmodifiableMap(x_dict);
    }
    public Map<String, ArrayList<ArrayList<Integer>>> getEdgeDict() {
        return Collections.unmodifiableMap(edge_dict);
    }
    public Map<String, double[][]> getEdgeAttrDict() {
        return Collections.unmodifiableMap(edgeAttr_dict);
    }

    // Increment whenever input matrices logically change
    private long inputVersion = 0;

    // Version when cache was computed
    private long cachedVersion = -1;

    // Cached output
    private double[] cachedResult = null;

    public GGGnnNode(GradientGraphO gg,
                     CPModel cpm,
                     HashMap allnodes,
                     RelStruc A,
                     OneStrucData I,
                     int inputcaseno,
                     int observcaseno,
                     HashMap<String, Integer> parameters,
                     boolean useCurrentPvals,
                     HashMap<Rel, GroundAtomList> mapatoms,
                     HashMap<String, Object[]> evaluated) throws RBNCompatibilityException {

        super(gg, cpm, A, I);

        if (!(cpm instanceof CatGnn))
            throw new RBNCompatibilityException("GGGnnNode requires a CatGnn CPModel, got: " + cpm);

        this.cpmgnn = (CatGnn) cpm;
        this.A = A;
        this.inst = I;

        setGnnPy(cpmgnn.getGnnPy());
        getGnnPy().setGradientGraph(gg);

        TypedTorchPf ttpf = cpmgnn.getTypedTorchPf();

        // construct the children of this Gradient Graph node by evaluating all the pf for node, edge, and edge attr

        // iterate over edges to understand which nodes actually belong to the sub-graph
        for (String pftype : ttpf.getTypedNames()) {
            // for each combine (layer definition)
            for (TorchInputPf tip : ttpf.getCombines(pftype)) {

                int[][] subslist = tip.tuplesSatisfyingCConstr(A, new ArgTerm[0], new int[0]);
                // for each pf of that layer for edge
                for (int i = 0; i < tip.getPfargsEdge().length; i++) {
                    evaluateForAllTuples(
                            Mode.EDGE, pftype, subslist, tip.getPfargsEdgeAt(i),
                            evalOfEdgeByType, tip, gg, i, allnodes, A, I,
                            inputcaseno, observcaseno, parameters,
                            useCurrentPvals, mapatoms, evaluated,
                            true);
                }
            }
        }

        edge_dict.clear();
        buildEdgeMatrices();
        pruneNonNaN(evalOfEdgeByType);

        // evaluate all the atom entries whose node ids are present in the subgraph
        for (String pftype : ttpf.getTypedNames()) {
            for (TorchInputPf tip : ttpf.getCombines(pftype)) {
                int[][] subslist = tip.tuplesSatisfyingCConstr(A, new ArgTerm[0], new int[0]);

                for (int i = 0; i < tip.getPfargsNode().length; i++) {
                    evaluateForAllTuples(
                            Mode.NODE, pftype, subslist, tip.getPfargsNodeAt(i),
                            evalOfNodesByType, tip, gg, i, allnodes, A, I,
                            inputcaseno, observcaseno, parameters,
                            useCurrentPvals, mapatoms, evaluated,
                            false);
                }

                for (int i = 0; i < tip.getPfargsEdgeAttr().length; i++) {
                    evaluateForAllTuples(
                            Mode.EDGE_ATTR, pftype, subslist, tip.getPfargsEdgeAttrAt(i),
                            evalOfEdgeAttrByType, tip, gg, i, allnodes, A, I,
                            inputcaseno, observcaseno, parameters,
                            useCurrentPvals, mapatoms, evaluated,
                            false);
                }
            }
        }


        edgeAttr_dict.clear();
        buildEdgeAttrMatrices(0);
        pruneNonNaN(evalOfEdgeAttrByType);

        // evaluate only this node
        if (nodeMappingByType.isEmpty()) {
            for (String pftype : ttpf.getTypedNames()) {
                for (TorchInputPf tip : ttpf.getCombines(pftype)) {
                    int[][] subslist = tip.tuplesSatisfyingCConstr(A, new ArgTerm[0], new int[0]);

                    for (int i = 0; i < tip.getPfargsNode().length; i++) {
                        evaluateForAllTuples(
                                Mode.NODE, pftype, subslist, tip.getPfargsNodeAt(i),
                                evalOfNodesByType, tip, gg, i, allnodes, A, I,
                                inputcaseno, observcaseno, parameters,
                                useCurrentPvals, mapatoms, evaluated,
                                true);
                    }
                }
            }
        }

        x_dict.clear();
        buildNodeFeatureMatrices(0);
        inputVersion++;
        // only keep NaN entries in the eval maps since those are the only ones we need to resolve during gradient computation
        retainOnlyNaNEntries();
    }


    private void evaluateForAllTuples(
            Mode mode,
            String pftype,
            int[][] tuples,
            CPModel nextsubpf,
            Map<String, Map<String, EvalEntry>> target,
            TorchInputPf tip,
            GradientGraphO gg,
            int probFormIdx,
            HashMap allnodes,
            RelStruc A,
            OneStrucData I,
            int inputcaseno,
            int observcaseno,
            HashMap<String, Integer> parameters,
            boolean useCurrentPvals,
            HashMap<Rel, GroundAtomList> mapatoms,
            HashMap<String, Object[]> evaluated,
            boolean assignNewIndices) throws RBNCompatibilityException {

        for (int[] tuple : tuples) {

            ArgTerm[] quantvars = tip.getQuantvars();
            CPModel groundsubpf = nextsubpf.substitute(quantvars, tuple);

            // take the argument of the specific atom
            List<int[]> argNodes = resolveArgNodes(mode, groundsubpf, nextsubpf, tuple, quantvars);
            // since the graph for the specific node can be a sub graph, we need to reconstruct the indexes for the edges
            // returns false when a required node is absent from the subgraph
            if (!remapToCompactIndices(mode, argNodes, pftype, nextsubpf, assignNewIndices)) {
                continue;
            }

            double evalValue = (double) groundsubpf.evaluate(
                    A, I, new ArgTerm[0], new int[0],
                    0, false, useCurrentPvals,
                    mapatoms, false, evaluated,
                    parameters, ProbForm.RETURN_ARRAY, true, null)[0];

            String subkey = groundsubpf.makeKey(A);

            String internedPftype = pftype.intern();
            Map<String, EvalEntry> typeMap = target.computeIfAbsent(internedPftype, k -> new HashMap<>());

            if (Double.isNaN(evalValue)) {
                GGCPMNode child = GGCPMNode.constructGGPFN(gg, groundsubpf, allnodes, A, I, inputcaseno, observcaseno, parameters, false, false, "", mapatoms, evaluated);
                typeMap = target.computeIfAbsent(internedPftype, k -> new HashMap<>());
                if (childrenSet.add(child)) {
                    children.add(child);
                    typeMap.put(subkey, new EvalEntry(child, argNodes, evalValue, tuple, probFormIdx));
                }
                child.addToParents(this);
            }
            else {
                typeMap.put(subkey, new EvalEntry(null, argNodes, evalValue, tuple, probFormIdx));
            }
        }
    }

    private void clearEvalMaps() {
        evalOfNodesByType.clear();
        evalOfEdgeAttrByType.clear();
        evalOfEdgeByType.clear();
    }

    // Call this right after buildInputMatrices(0) in the constructor
    private void retainOnlyNaNEntries() {
        pruneNonNaN(evalOfNodesByType);
        pruneNonNaN(evalOfEdgeAttrByType);
        pruneNonNaN(evalOfEdgeByType);
    }

    private static void pruneNonNaN(Map<String, Map<String, EvalEntry>> map) {
        Iterator<Map.Entry<String, Map<String, EvalEntry>>> outer = map.entrySet().iterator();
        while (outer.hasNext()) {
            Map<String, EvalEntry> inner = outer.next().getValue();
            inner.entrySet().removeIf(e -> !Double.isNaN(e.getValue().evalValue()));
            if (inner.isEmpty()) outer.remove();
        }
    }

    // one function that will delegate which resolve to take
    // we need different methods for each different atom and type
    private List<int[]> resolveArgNodes(Mode mode, CPModel groundsubpf, CPModel nextsubpf, int[] tuple, ArgTerm[] quantvars)
            throws RBNCompatibilityException {
        return switch (mode) {
            case NODE -> resolveNodeArgs(groundsubpf, nextsubpf, tuple, quantvars);
            case EDGE_ATTR -> resolveEdgeAttrArgs(groundsubpf, tuple);
            case EDGE -> resolveEdgeArgs(groundsubpf);
        };
    }

    private List<int[]> resolveNodeArgs(CPModel groundsubpf, CPModel nextsubpf, int[] tuple, ArgTerm[] quantvars)
            throws RBNCompatibilityException {

        int nodeId;
        if (groundsubpf instanceof ProbFormAtom pfa) {
            nodeId = pfa.getArguments().length == 1 ? Integer.parseInt(pfa.getArguments()[0].argEval()) : -1;

        } else if (groundsubpf instanceof ProbFormMacroCall pmc) {
            nodeId = pmc.args().length == 1 ? Integer.parseInt(pmc.args()[0].argEval()) : -1;

        } else if (groundsubpf instanceof ProbFormCombFunc) {
            if (tuple.length == 1) {
                nodeId = tuple[0];
            } else if (tuple.length == 2) {
                nodeId = resolveReferringArg(groundsubpf, nextsubpf, quantvars, tuple);
            } else {
                throw new RBNCompatibilityException("Wrong number of arguments for substitution");
            }
        } else {
            nodeId = -1;  // ProbFormConstant and any unknown type
        }
        return List.of(new int[]{nodeId});
    }

    private int resolveReferringArg(CPModel groundsubpf, CPModel nextsubpf, ArgTerm[] quantvars, int[] tuple) {
        String groundStr = groundsubpf.asString(0, 0, A, false, false);
        ArgTerm[] qv00 = {quantvars[0], quantvars[0]};
        String g0Str = nextsubpf.substitute(qv00, tuple).asString(0, 0, A, false, false);
        ArgTerm[] qv11 = {new VarTerm(quantvars[0].argEval() + qv00[1].argEval()), new VarTerm(quantvars[0].argEval() + qv00[1].argEval())};
        String g1Str = nextsubpf.substitute(qv11, tuple).asString(0, 0, A, false, false);

        if (g0Str.equals(groundStr))
            return g0Str.equals(g1Str) ? -1 : tuple[0];
        return tuple[1];
    }

    private List<int[]> resolveEdgeAttrArgs(CPModel groundsubpf, int[] tuple) {
        if (groundsubpf instanceof ProbFormConstant)
            return List.of(new int[]{-1});

        // For any 2-argument form, store the raw node ids as (src, dst)
        if (tuple.length == 2)
            return List.of(new int[]{tuple[0], tuple[1]});

        return List.of(new int[]{-1});
    }

    private List<int[]> resolveEdgeArgs(CPModel groundsubpf) throws RBNCompatibilityException {
        if (groundsubpf instanceof ProbFormAtom pfa) {
            if (pfa.getArguments().length != 2)
                throw new RBNCompatibilityException("EDGEGRAPH ProbFormAtom must have exactly 2 arguments");

            ArgTerm[] args = pfa.getArguments();
            return List.of(new int[]{Integer.parseInt(args[0].argEval()), Integer.parseInt(args[1].argEval())});

        } else if (groundsubpf instanceof ProbFormBoolComposite composite) {
            List<int[]> result = new ArrayList<>();

            for (int i = 0; i < composite.numComponents(); i++) {
                if (composite.componentAt(i) instanceof ProbFormBoolAtom atom) {
                    ArgTerm[] args = atom.getArguments();
                    result.add(new int[]{Integer.parseInt(args[0].argEval()), Integer.parseInt(args[1].argEval())});
                }
            }
            return result;
        }
        throw new RBNCompatibilityException(
                "EDGEGRAPH supports ProbFormAtom or ProbFormBoolComposite with 2 arguments");
    }

    // remap all node ids in argNodes to compact indices starting from 0, separately for each type, and according to the nodes present in the subgraph of this GGGnnNode
    private boolean remapToCompactIndices(Mode mode, List<int[]> argNodes, String pftype, CPModel nextsubpf, boolean assignNewIndices) {
        int compIdx = 0;
        for (int[] argNode : argNodes) {
            if (argNode[0] < 0) { compIdx++; continue; }

            switch (mode) {
                case NODE -> {
                    if (assignNewIndices) {
                        argNode[0] = getOrAssignNodeIndex(pftype, argNode[0]);
                    } else {
                        Integer mapped = getNodeIndexIfPresent(pftype, argNode[0]);
                        if (mapped == null) return false;  // outside this subgraph, skip tuple
                        argNode[0] = mapped;
                    }
                }
                case EDGE_ATTR -> {
                    if (argNode.length == 2 && argNode[0] >= 0) {
                        Integer src = getNodeIndexIfPresent(pftype, argNode[0]);
                        Integer dst = getNodeIndexIfPresent(pftype, argNode[1]);
                        if (src == null || dst == null) return false;
                        argNode[0] = src;
                        argNode[1] = dst;
                    }
                }
                case EDGE -> {
                    if (nextsubpf instanceof ProbFormAtom pfa && pfa.getArguments().length == 2) {
                        argNode[0] = assignNewIndices
                                ? getOrAssignNodeIndex(pfa.getRelation().getTypes()[0].getName(), argNode[0])
                                : lookupOrFail(pfa.getRelation().getTypes()[0].getName(), argNode[0]);
                        argNode[1] = assignNewIndices
                                ? getOrAssignNodeIndex(pfa.getRelation().getTypes()[1].getName(), argNode[1])
                                : lookupOrFail(pfa.getRelation().getTypes()[1].getName(), argNode[1]);

                        if (argNode[0] < 0 || argNode[1] < 0) return false;

                    } else if (nextsubpf instanceof ProbFormBoolComposite composite
                            && composite.componentAt(compIdx) instanceof ProbFormBoolAtom atom) {

                        argNode[0] = assignNewIndices
                                ? getOrAssignNodeIndex(atom.getRelation().getTypes()[0].getName(), argNode[0])
                                : lookupOrFail(atom.getRelation().getTypes()[0].getName(), argNode[0]);
                        argNode[1] = assignNewIndices
                                ? getOrAssignNodeIndex(atom.getRelation().getTypes()[1].getName(), argNode[1])
                                : lookupOrFail(atom.getRelation().getTypes()[1].getName(), argNode[1]);

                        if (argNode[0] < 0 || argNode[1] < 0) return false;
                    }
                }
            }
            compIdx++;
        }
        return true;
    }

    private int lookupOrFail(String type, int nodeId) {
        Integer v = getNodeIndexIfPresent(type, nodeId);
        return v != null ? v : -1;
    }


    @Override
    public double[] evaluate(Integer sno) {
        if (gnnPy == null) throw new RuntimeException("GnnPy is null in GGGnnNode");

        if (depends_on_sample && sno == null) {
            for (int i = 0; i < thisgg.numchains * thisgg.windowsize; i++)
                evaluate(i);
            return null;
        }

        if (this.depends_on_sample && is_evaluated_val_for_samples[sno])
            return this.values_for_samples[sno];
        if (!this.depends_on_sample && is_evaluated_val_for_samples[0])
            return this.values_for_samples[0];

        double[] result = null;

        if (cpmgnn instanceof CatGnn) {
            if (isCacheValid()) {
                result = cachedResult;
            } else {
                result = gnnPy.GGevaluate_gnnHetero(getXDict(), getEdgeDict(), getEdgeAttrDict(), cpmgnn, this);
                cachedResult = (result != null) ? result.clone() : null;
                cachedVersion = inputVersion;
            }
        }

        if (this.depends_on_sample) {
            if (cpmgnn instanceof CatGnn)
                values_for_samples[sno] = result;
            is_evaluated_val_for_samples[sno] = true;
        } else {
            values_for_samples[0] = result;
            is_evaluated_val_for_samples[0] = true;
        }

        return result;
    }

    private boolean isCacheValid() {
        return cachedResult != null && cachedVersion == inputVersion;
    }

    @Override
    public Gradient evaluateGradient(Integer sno) throws RBNNaNException {
        if (gnnPy == null) throw new RuntimeException("GnnPy is null in GGGnnNode");

        if (depends_on_sample && sno == null) {
            for (int i = 0; i < thisgg.numchains * thisgg.windowsize; i++)
                evaluateGradient(i);
            return null;
        }

        int idx = depends_on_sample ? sno : 0;
        if (is_evaluated_grad_for_samples[idx])
            return gradient_for_samples.get(idx);

        Gradient result = gradient_for_samples.get(idx);
        result.reset();

        // update matrices with the current sno so that NaN entries
        // (child nodes) are resolved against the correct sample
        resolveNaNEntries(sno);
        inputVersion++;

        Object[] outres = gnnPy.GGevaluate_gnnGradients(sno, A, inst, cpmgnn, this);
        @SuppressWarnings("unchecked")
        Map<String, double[][]> grads = (Map<String, double[][]>) outres[1];

        for (String param : myparameters)
            if (grads.containsKey(param))
                result.set_part_deriv(param, grads.get(param)[0]);

        is_evaluated_grad_for_samples[idx] = true;
        return result;
    }

    private void buildInputMatrices(Integer init) {
        x_dict.clear();
        edge_dict.clear();
        edgeAttr_dict.clear();

        buildEdgeMatrices();          // topology first, no sno needed
        buildNodeFeatureMatrices(init);
        buildEdgeAttrMatrices(init);
        inputVersion++;
    }

    // used to update only the elements that are NaN in the matrices
    // when computing the gradients, the new elements are not updated like MAP
    // we need to call the evaluate function
    private void resolveNaNEntries(Integer sno) {
        resolveNaNNodes(sno);
        resolveNaNEdgeAttrs(sno);
    }

    private void resolveNaNNodes(Integer sno) {
        for (TorchInputSpecs spec : cpmgnn.getGnnInputs()) {
            String pftype = spec.getType();
            Map<String, EvalEntry> nanEntries = evalOfNodesByType.get(pftype);
            if (nanEntries == null || nanEntries.isEmpty()) continue;

            double[][] mat = x_dict.get(pftype);
            if (mat == null) continue;

            @SuppressWarnings("unchecked")
            List<Rel> nodeAttrs = (List<Rel>) spec.getNodeAttributes();
            if (nodeAttrs == null || nodeAttrs.isEmpty()) continue;

            // pre-compute column offsets once
            int[] startIndices = computeStartIndices(nodeAttrs);

            for (EvalEntry entry : nanEntries.values()) {
                double value = resolveEvalValue(entry, sno);
                if (Double.isNaN(value)) continue;

                int argNode = entry.argNodes().get(0)[0];
                int pfIdx = (int) entry.probFormIdx();
                Rel r = nodeAttrs.get(pfIdx);

                if (argNode < 0) {
                    for (int row = 0; row < mat.length; row++)
                        writeFeatureCell(mat, row, startIndices[pfIdx], (int) value, r, cpmgnn.isOneHotEncoding());
                } else {
                    writeFeatureCell(mat, argNode, startIndices[pfIdx], (int) value, r, cpmgnn.isOneHotEncoding());
                }
            }
        }
    }

    private void resolveNaNEdgeAttrs(Integer sno) {
        for (TorchInputSpecs spec : cpmgnn.getGnnInputs()) {
            String pftype = spec.getType();
            Map<String, EvalEntry> nanEntries = evalOfEdgeAttrByType.get(pftype);
            if (nanEntries == null || nanEntries.isEmpty()) continue;

            double[][] mat = edgeAttr_dict.get(pftype);
            if (mat == null) continue;

            @SuppressWarnings("unchecked")
            List<Rel> edgeAttrs = (List<Rel>) spec.getEdgeAttributes();
            if (edgeAttrs == null || edgeAttrs.isEmpty()) continue;

            int[] startIndices = computeStartIndices(edgeAttrs);
            String edgeKey = spec.getEdgeRelation().name();

            for (EvalEntry entry : nanEntries.values()) {
                double value = resolveEvalValue(entry, sno);
                if (Double.isNaN(value)) continue;

                int pfIdx = (int) entry.probFormIdx();
                Rel r = edgeAttrs.get(pfIdx);
                int[] endpoints = entry.argNodes().get(0);

                if (endpoints.length < 2 || endpoints[0] < 0) {
                    for (int row = 0; row < mat.length; row++)
                        writeFeatureCell(mat, row, startIndices[pfIdx], (int) value, r, cpmgnn.isOneHotEncoding());
                } else {
                    int edgeRow = findEdgeRow(edgeKey, endpoints[0], endpoints[1]);
                    if (edgeRow >= 0)
                        writeFeatureCell(mat, edgeRow, startIndices[pfIdx], (int) value, r, cpmgnn.isOneHotEncoding());
                }
            }
        }
    }

    private int[] computeStartIndices(List<Rel> attrs) {
        int[] starts = new int[attrs.size()];
        int offset = 0;
        for (int i = 0; i < attrs.size(); i++) {
            starts[i] = offset;
            Rel r = attrs.get(i);
            offset += (cpmgnn.isOneHotEncoding() && r instanceof CatRel) ? (int) r.numvals() : 1;
        }
        return starts;
    }

    private void buildEdgeMatrices() {
        for (TorchInputSpecs spec : cpmgnn.getGnnInputs()) {
            String pftype = spec.getType();
            String edgeKey = spec.getEdgeRelation().name();   // matches edgesToDict / GGconstructInputGraph
            // Direct lookup into the nested map — no HashMap copy, no prefix scan
            Map<String, EvalEntry> edgeTable = evalOfEdgeByType.getOrDefault(pftype, Collections.emptyMap());

            ArrayList<Integer> sources = new ArrayList<>();
            ArrayList<Integer> dests = new ArrayList<>();

            for (EvalEntry entry : edgeTable.values()) {
                for (int[] endpoints : entry.argNodes()) {
                    if (endpoints.length == 2 && endpoints[0] >= 0 && endpoints[1] >= 0) {
                        sources.add(endpoints[0]);
                        dests.add(endpoints[1]);
                    }
                }
            }

            ArrayList<ArrayList<Integer>> edgeList = new ArrayList<>();
            edgeList.add(sources);
            edgeList.add(dests);
            edge_dict.put(edgeKey, edgeList);   // key = relation name, not pftype
        }
    }

    private void buildNodeFeatureMatrices(Integer sno) {
        for (TorchInputSpecs spec : cpmgnn.getGnnInputs()) {
            String pftype = spec.getType();
            @SuppressWarnings("unchecked")
            ArrayList<Rel> subList = (ArrayList<Rel>) spec.getNodeAttributes();
            if (subList == null || subList.isEmpty()) continue;

            // Compute column layout and validate that all attributes share the same node type.
            int num_col = 0;
            int startIndex = 0;
            int[] startIndices = new int[subList.size()];
            Rel[] rels = new Rel[subList.size()];
            Type nodeType = null;

            for (int i = 0; i < subList.size(); i++) {
                Rel r = subList.get(i);
                num_col += (r instanceof CatRel && cpmgnn.isOneHotEncoding()) ? (int) r.numvals() : 1;

                if (r.getTypes().length > 1)
                    throw new RuntimeException("More than one type for node attribute " + r.name());
                if (nodeType == null)
                    nodeType = r.getTypes()[0];
                else if (!nodeType.equals(r.getTypes()[0]))
                    throw new RuntimeException("Not all the same type for node attribute " + r.name() + " typed: " + r.getTypesAsString() + " vs " + nodeType.getName());

                startIndices[i] = startIndex;
                rels[i] = r;
                startIndex += (r instanceof CatRel) ? (int) r.numvals() : 1;
            }

            // Direct lookup — no HashMap copy, no prefix scan
            Map<String, EvalEntry> nodesTable = evalOfNodesByType.getOrDefault(pftype, Collections.emptyMap());
            int uniqueNodes = nextIndexByType.getOrDefault(pftype, 0);
            if (uniqueNodes == 0) uniqueNodes = 1;  // always allocate at least one row

            if (!nodesTable.isEmpty() && nodesTable.size() % subList.size() != 0)
                throw new RuntimeException("Node eval-table size " + nodesTable.size() + " is not divisible by attribute count " + subList.size());

            double[][] matrix = createZeroMatrix(uniqueNodes, num_col);

            for (EvalEntry entry : nodesTable.values()) {
                int argNode = entry.argNodes().get(0)[0];
                double value = resolveEvalValue(entry, sno);
                if (Double.isNaN(value)) {
                    // stop constructing for now, we need to wait for the child node to be evaluated to get the value
                    continue;
                }

                int pfIdx = (int) entry.probFormIdx();
                int startingIdx = startIndices[pfIdx];
                Rel r = rels[pfIdx];
                int col = startingIdx;
                double cellValue = 1.0;

                if (r instanceof CatRel && value >= 0) {
                    col = (int) (value + startingIdx);
                } else if (r.valtype() == Rel.NUMERIC || r.valtype() == Rel.BOOLEAN) {
                    cellValue = value;
                }

                if (argNode < 0) {
                    for (int row = 0; row < uniqueNodes; row++)
                        matrix[row][col] = cellValue;
                } else {
                    matrix[argNode][col] = cellValue;
                }
            }
            x_dict.put(pftype, matrix);
        }
    }

    private void buildEdgeAttrMatrices(Integer sno) {
        for (TorchInputSpecs spec : cpmgnn.getGnnInputs()) {
            String pftype = spec.getType();
            @SuppressWarnings("unchecked")
            ArrayList<Rel> subList = (ArrayList<Rel>) spec.getEdgeAttributes();
            if (subList == null || subList.isEmpty()) continue;

            // Compute column layout.
            int num_col = 0;
            int startIndex = 0;
            int[] startIndices = new int[subList.size()];
            Rel[] rels = new Rel[subList.size()];

            for (int i = 0; i < subList.size(); i++) {
                Rel r = subList.get(i);
                num_col += (r instanceof CatRel && cpmgnn.isOneHotEncoding()) ? (int) r.numvals() : 1;
                startIndices[i] = startIndex;
                rels[i] = r;
                startIndex += (r instanceof CatRel) ? (int) r.numvals() : 1;
            }

            String edgeKey = spec.getEdgeRelation().name();
            ArrayList<ArrayList<Integer>> edgeList = edge_dict.get(edgeKey);
            int num_edges = (edgeList != null && !edgeList.isEmpty()) ? edgeList.get(0).size() : 1;
            if (num_edges == 0) num_edges = 1;

            Map<String, EvalEntry> edgeAttrTable = evalOfEdgeAttrByType.getOrDefault(pftype, Collections.emptyMap());
            double[][] matrix = createZeroMatrix(num_edges, num_col);

            for (EvalEntry entry : edgeAttrTable.values()) {
                int[] endpoints = entry.argNodes().get(0);
                double value = resolveEvalValue(entry, sno);

                if (Double.isNaN(value)) // stop constructing for now, we need to wait for the child node to be evaluated to get the value
                    continue;

                int pfIdx = (int) entry.probFormIdx();
                int startingIdx = startIndices[pfIdx];
                Rel r = rels[pfIdx];
                int col = startingIdx;
                double cellValue = 1.0;

                if (r instanceof CatRel) {
                    col = (int) (value + startingIdx);
                } else if (r.valtype() == Rel.NUMERIC || r.valtype() == Rel.BOOLEAN) {
                    cellValue = value;
                }

                if (endpoints.length < 2 || endpoints[0] < 0) {
                    // constant — broadcast to all edge rows
                    for (int row = 0; row < num_edges; row++)
                        matrix[row][col] = cellValue;
                } else {
                    int edgeRow = findEdgeRow(edgeKey, endpoints[0], endpoints[1]);
                    if (edgeRow >= 0)
                        matrix[edgeRow][col] = cellValue;
                }
            }
            edgeAttr_dict.put(pftype, matrix);
        }
    }

    /**
     * Returns the eval value for an entry, delegating to the child GGCPMNode
     * when the stored value is NaN (i.e. the value depends on the current sample).
     */
    private double resolveEvalValue(EvalEntry entry, Integer sno) {
        double value = entry.evalValue();
        if (Double.isNaN(value)) {
            GGCPMNode child = (GGCPMNode) entry.evaluatedNode();
            try {
                // can happen that the child cannot be evaluated because the child is not still initialized
                value = child.evaluate(sno)[0];
            } catch (Exception e) {
                value = Double.NaN;
            }
        }
        return value;
    }

    private static double[][] createZeroMatrix(int rows, int cols) {
        return new double[rows][cols];
    }

    // this function returns the next available index starting from zero
    public int getOrAssignNodeIndex(String type, int nodeId) {
        Objects.requireNonNull(type, "type must not be null");
        String internedType = type.intern();
        return nodeMappingByType
                .computeIfAbsent(internedType, t -> new HashMap<>())
                .computeIfAbsent(nodeId, id -> {
                    int next = nextIndexByType.getOrDefault(internedType, 0);
                    nextIndexByType.put(internedType, next + 1);
                    return next;
                });
    }

    public Integer getNodeIndexIfPresent(String type, int nodeId) {
        Map<Integer, Integer> mapping = nodeMappingByType.get(type);
        return mapping != null ? mapping.get(nodeId) : null;
    }

    /**
     * Incremental matrix updates (called from GnnPy.setCurrentInstPy)
     * Updates exactly the one cell (or one-hot slice) that corresponds to
     * currentMaxNode's atom in whichever of the three feature matrices
     * owns it. This saves computations instead of reconstructing the entire matrices every time an atom changes
     */
    public void setCurrentInstPy(int currentInst, GGAtomMaxNode currentMaxNode) {
        Rel rel = currentMaxNode.myatom().rel();
        boolean oneHot = cpmgnn.isOneHotEncoding();
        inputVersion++;
        if (updateNodeAttribute(rel, currentMaxNode, currentInst, oneHot)) return;
        if (updateEdgeIndex(rel, currentMaxNode, currentInst)) return;
        updateEdgeAttribute(rel, currentMaxNode, currentInst, oneHot);
    }

    /**
     * Updates a cell in x_dict when rel is a node-attribute relation
     * (unary atom, 1-argument)
     * return early if the GGAtomMaxNode's atom is not in the GnnInputs or if the corresponding matrix
     */
    private boolean updateNodeAttribute(Rel rel, GGAtomMaxNode currentMaxNode, int currentInst, boolean oneHot) {
        if (currentMaxNode.myatom().args().length != 1) return false;

        for (TorchInputSpecs spec : cpmgnn.getGnnInputs()) {
            @SuppressWarnings("unchecked")
            List<Rel> nodeAttrs = (List<Rel>) spec.getNodeAttributes();
            if (nodeAttrs == null) continue;

            int colOffset = 0;
            for (Rel r : nodeAttrs) {
                if (r.equals(rel)) {
                    String pftype = spec.getType();
                    double[][] mat = x_dict.get(pftype);
                    if (mat == null) return false;

                    int nodeId = currentMaxNode.myatom().args()[0];
                    Integer row = getNodeIndexIfPresent(pftype, nodeId);
                    if (row == null) return false;

                    writeFeatureCell(mat, row, colOffset, currentInst, rel, oneHot);
                    return true;
                }
                colOffset += (oneHot && r instanceof CatRel) ? (int) r.numvals() : 1;
            }
        }
        return false;
    }

    /**
     * Updates the edge_index when rel is a binary edge-defining relation.
     * Adds or removes the edge (src->dst) based on currentInst:
     * 1 / true -> ensure the edge is present; 0 / false -> ensure it is absent.
     */
    private boolean updateEdgeIndex(Rel rel, GGAtomMaxNode currentMaxNode, int currentInst) {
        if (currentMaxNode.myatom().args().length != 2) return false;

        for (TorchInputSpecs spec : cpmgnn.getGnnInputs()) {
            @SuppressWarnings("unchecked")
            List<Rel> edgeRels = (List<Rel>) spec.getEdgeRelation();
            if (edgeRels == null || !edgeRels.contains(rel)) continue;

            String edgeKey = spec.getEdgeRelation().name();
            ArrayList<ArrayList<Integer>> edgeList = edge_dict.get(edgeKey);
            if (edgeList == null || edgeList.size() < 2) return false;

            int rawSrc = currentMaxNode.myatom().args()[0];
            int rawDst = currentMaxNode.myatom().args()[1];

            // Translate original ids to compact indices
            String srcType = rel.getTypes()[0].getName();
            String dstType = rel.getTypes()[1].getName();
            Integer src = getNodeIndexIfPresent(srcType, rawSrc);
            Integer dst = getNodeIndexIfPresent(dstType, rawDst);
            if (src == null || dst == null) return false;

            ArrayList<Integer> sources = edgeList.get(0);
            ArrayList<Integer> dests = edgeList.get(1);

            // Find existing position (if any)
            int existingIdx = -1;
            for (int i = 0; i < sources.size(); i++) {
                if (sources.get(i) == src && dests.get(i) == dst) {
                    existingIdx = i;
                    break;
                }
            }

            boolean edgeShouldExist = (currentInst != 0);
            if (edgeShouldExist && existingIdx < 0) {
                sources.add(src);
                dests.add(dst);
            } else if (!edgeShouldExist && existingIdx >= 0) {
                sources.remove(existingIdx);
                dests.remove(existingIdx);
            }
            return true;
        }
        return false;
    }

    // Updates a cell in edgeAttr_dict when rel is an edge-attribute relation (binary atom, 2-arguments)
    private boolean updateEdgeAttribute(Rel rel, GGAtomMaxNode currentMaxNode, int currentInst, boolean oneHot) {
        if (currentMaxNode.myatom().args().length != 2) return false;

        for (TorchInputSpecs spec : cpmgnn.getGnnInputs()) {
            @SuppressWarnings("unchecked")
            List<Rel> edgeAttrRels = (List<Rel>) spec.getEdgeAttributes();
            if (edgeAttrRels == null) continue;

            int colOffset = 0;
            for (Rel r : edgeAttrRels) {
                if (r.equals(rel)) {
                    String edgeKey = spec.getEdgeRelation().name();
                    double[][] mat = edgeAttr_dict.get(spec.getType());
                    if (mat == null) return false;

                    // Locate the edge row by matching compact src/dst in edge_dict
                    int rawSrc = currentMaxNode.myatom().args()[0];
                    int rawDst = currentMaxNode.myatom().args()[1];
                    String srcType = rel.getTypes()[0].getName();
                    String dstType = rel.getTypes()[1].getName();
                    Integer src = getNodeIndexIfPresent(srcType, rawSrc);
                    Integer dst = getNodeIndexIfPresent(dstType, rawDst);
                    if (src == null || dst == null) return false;

                    int edgeRow = findEdgeRow(edgeKey, src, dst);  // key = relation name
                    if (edgeRow < 0) return false;

                    writeFeatureCell(mat, edgeRow, colOffset, currentInst, rel, oneHot);
                    return true;
                }
                colOffset += (oneHot && r instanceof CatRel) ? (int) r.numvals() : 1;
            }
        }
        return false;
    }


    private void writeFeatureCell(double[][] mat, int row, int colOffset, int value, Rel rel, boolean oneHot) {
        double[] matRow = mat[row];
        if (rel instanceof CatRel) {
            if (oneHot) {
                int numVals = (int) rel.numvals();
                Arrays.fill(matRow, colOffset, colOffset + numVals, 0.0);
                matRow[colOffset + value] = 1.0;
            } else {
                matRow[colOffset] = value;
            }
        } else {
            // BoolRel or numeric
            matRow[colOffset] = value;
        }
    }

    private int findEdgeRow(String pftype, int src, int dst) {
        ArrayList<ArrayList<Integer>> edgeList = edge_dict.get(pftype);
        if (edgeList == null || edgeList.size() < 2) return -1;
        ArrayList<Integer> sources = edgeList.get(0);
        ArrayList<Integer> dests   = edgeList.get(1);
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i) == src && dests.get(i) == dst) return i;
        }
        return -1;
    }

    public GnnPy getGnnPy() {
        return gnnPy;
    }

    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }

    public CPModel getCpm() {
        return cpmgnn;
    }

    @Override
    public boolean isBoolean() { return cpmgnn.numvals() == 1; }

    public int outDim() {
        System.out.println("outDim still needs to be implemented for GGGnnNode");
        return 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GGGnnNode o = (GGGnnNode) obj;
        return Objects.equals(nodeMappingByType, o.nodeMappingByType)
                && Objects.equals(nextIndexByType, o.nextIndexByType)
                && Objects.equals(evalOfNodesByType, o.evalOfNodesByType)
                && Objects.equals(evalOfEdgeAttrByType, o.evalOfEdgeAttrByType)
                && Objects.equals(evalOfEdgeByType, o.evalOfEdgeByType);
    }

    @Override
    public int hashCode() {
        int h = super.hashCode();
        h = 31 * h + Objects.hashCode(cpmgnn);
        h = 31 * h + Objects.hashCode(gnnPy);
        h = 31 * h + Objects.hashCode(nodeMappingByType);
        h = 31 * h + Objects.hashCode(nextIndexByType);
        h = 31 * h + Objects.hashCode(evalOfNodesByType);
        h = 31 * h + Objects.hashCode(evalOfEdgeAttrByType);
        h = 31 * h + Objects.hashCode(evalOfEdgeByType);
        return h;
    }
}