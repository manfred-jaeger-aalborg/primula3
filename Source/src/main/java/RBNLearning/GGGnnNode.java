package RBNLearning;

import PyManager.GnnPy;
import PyManager.TorchInputPf;
import PyManager.TypedTorchPf;
import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNNaNException;
import RBNpackage.*;

import java.util.*;

public class GGGnnNode extends GGCPMNode {
    private CatGnn cpmgnn;
    // we can have only one reference og gnnPy on the same thread
    // once the thread will create and use gnnPy it will set this variable
    private GnnPy gnnPy;
    private Map<Integer, Integer> nodeMapping;
    // those two variable are "shared"
    private static RelStruc A;
    private static OneStrucData inst;

    // Per-type node mapping: for each node "type" keep an independent mapping nodeId -> index (starting at 0)
    private final Map<String, Map<Integer, Integer>> nodeMappingByType = new HashMap<>();
    private final Map<String, Integer> nextIndexByType = new HashMap<>();

    private Vector<Object> evaluated_children;

    // the graph constructed
    private HashMap<String, HashMap<String, Object[]>> evalOfNodes;
    private HashMap<String, HashMap<String, Object[]>> evalOfEdgeAttr;
    private HashMap<String, HashMap<String, Object[]>> evalOfEdge;
//    private Hashtable<String, int[][]> evalOfEdge;

    public GGGnnNode(GradientGraphO gg,
                     CPModel cpm,
                     Hashtable allnodes,
                     RelStruc A,
                     OneStrucData I,
                     int inputcaseno,
                     int observcaseno,
                     Hashtable<String,Integer> parameters,
                     boolean useCurrentPvals,
                     Hashtable<Rel,GroundAtomList> mapatoms,
                     Hashtable<String,Object[]>  evaluated ) throws RBNCompatibilityException {
        super(gg, cpm, A, I);

        if (!(cpm instanceof CatGnn)) {
            throw new RBNCompatibilityException("GGGnnNode cannot accept " + this.cpmgnn.toString() + " as valid pf");
        }

        // store the CPModel, and the "input" for the gnn (A and I)
        // those variables will be used in the evaluate function
        // the entire input for the gnn will be constructed later in evaluate()
        this.cpmgnn = (CatGnn) cpm;
        this.A = A;
        this.inst = I;

        this.evaluated_children = new Vector<>();

        this.evalOfNodes = new HashMap<>();
        this.evalOfEdgeAttr = new HashMap<String, HashMap<String, Object[]>>();
        this.evalOfEdge = new HashMap<String, HashMap<String, Object[]>>();
        this.nodeMapping = new HashMap<>();

        setGnnPy(cpmgnn.getGnnPy()); // set the same GnnPy from the rel to the ggnode
        getGnnPy().setGradientGraph(gg); // save also the gradient graph

        CPModel nextsubpf;
        TypedTorchPf ttpf = cpmgnn.getGroundTypedTorchPf();

        // differently from all the other probability formulas here we evaluate
        // all the components of the GNN to see when it is not possible to evaluate.
        // the subpfs which are not evaluatable will return NaN and added to the children vector
        for (String pftype: ttpf.getTypedNames()) { // loop through all the TYPEDICT

            HashMap<String, Object[]> evalOfNodesForType = this.evalOfNodes.get(pftype);
            if (evalOfNodesForType == null) {
                evalOfNodesForType = new HashMap<>();
                this.evalOfNodes.put(pftype, evalOfNodesForType);
            }

            HashMap<String, Object[]> evalOfEdgeForType = this.evalOfEdge.get(pftype);
            if (evalOfEdgeForType == null) {
                evalOfEdgeForType = new HashMap<>();
                this.evalOfEdge.put(pftype, evalOfEdgeForType);
            }

            HashMap<String, Object[]> evalOfEdgeAttrForType = this.evalOfEdgeAttr.get(pftype);
            if (evalOfEdgeAttrForType == null) {
                evalOfEdgeAttrForType = new HashMap<>();
                this.evalOfEdgeAttr.put(pftype, evalOfEdgeAttrForType);
            }

            List<TorchInputPf> torchInputPfListType = ttpf.getCombines(pftype);
            int[][] edgeList = new int[0][];

            for (int c = 0; c < torchInputPfListType.size(); c++) { // loop through all the COMBINE
                TorchInputPf torchInputPf = torchInputPfListType.get(c);

                int[][] subslist = torchInputPf.tuplesSatisfyingCConstr(A, new String[0], new int[0]);
//                edgeList = merge2DArrays(edgeList, subslist);

                // Node attributes
                for (int i = 0, probFormIdx = 0; i < torchInputPf.getPfargsNode().length; i++, probFormIdx++) {
                    nextsubpf = torchInputPf.getPfargsNodeAt(i);
                    evaluateForAllTuples(0, pftype, subslist, nextsubpf, evalOfNodesForType, torchInputPf, gg, probFormIdx, allnodes, A, I, inputcaseno, observcaseno, parameters, useCurrentPvals, mapatoms, evaluated);
                }

                // Edge attributes
                for (int i = 0, probFormIdx = 0; i < torchInputPf.getPfargsEdgeAttr().length; i++, probFormIdx++) {
                    nextsubpf = torchInputPf.getPfargsEdgeAttrAt(i);
                    evaluateForAllTuples(1, pftype, subslist, nextsubpf, evalOfEdgeAttrForType, torchInputPf, gg, probFormIdx, allnodes, A, I, inputcaseno, observcaseno, parameters, useCurrentPvals, mapatoms, evaluated);
                }

                // create the edges
                for (int i = 0, probFormIdx = 0; i < torchInputPf.getPfargsEdge().length; i++, probFormIdx++) {
                    nextsubpf = torchInputPf.getPfargsEdgeAt(i);
                    evaluateForAllTuples(2, pftype, subslist, nextsubpf, evalOfEdgeForType, torchInputPf, gg, probFormIdx, allnodes, A, I, inputcaseno, observcaseno, parameters, useCurrentPvals, mapatoms, evaluated);
                }

                // create the edges
//                for (int i = 0; i < torchInputPf.getPfargsEdge().length; i++) {
//                    nextsubpf = torchInputPf.getPfargsEdgeAt(i);
//                    for (int j = 0; j < subslist.length; j++) {
//                        String[] quantvars = torchInputPf.getQuantvars();
//                        CPModel groundnextsubpf = nextsubpf.substitute(quantvars, subslist[j]);
//                        if (groundnextsubpf instanceof ProbFormAtom) {
//                            String[] args = ((ProbFormAtom) groundnextsubpf).getArguments();
//                            int[] intArray = new int[args.length];
//                            Type[] t = ((ProbFormAtom) groundnextsubpf).atom().rel.getTypes();
//                            for (int k = 0; k < args.length; k++) {
//                                int nodeId = Integer.parseInt(args[k]);
//                                // Per-type mapping to a compact, sequential index
//                                intArray[k] = getOrAssignNodeIndex(t[k].getName(), nodeId);
//                            }
//                            edgeList = mergeArrays(edgeList, intArray);
//                        }
//                    }
//                }
            }
//            this.evalOfEdge.put(pftype, edgeList);
        }
    }

    private void evaluateForAllTuples(int mode, //0:node, 1:edge attr, 2:edge
                                      String pftype,
                                      int[][] tuples,
                                      CPModel nextsubpf,
                                      HashMap<String, Object[]> evalOfPFs,
                                      TorchInputPf torchInputPf,
                                      GradientGraphO gg,
                                      int probFormIdx,
                                      Hashtable allnodes,
                                      RelStruc A,
                                      OneStrucData I,
                                      int inputcaseno,
                                      int observcaseno,
                                      Hashtable<String,Integer> parameters,
                                      boolean useCurrentPvals,
                                      Hashtable<Rel,GroundAtomList> mapatoms,
                                      Hashtable<String,Object[]>  evaluated)
            throws RBNCompatibilityException {

        for (int j = 0; j < tuples.length; j++) {
            // this part is used to understand which of the quantvars are used for the unary attributes
            // NOT FULLY TESTED
            String[] quantvars = torchInputPf.getQuantvars();
            CPModel groundnextsubpf = nextsubpf.substitute(quantvars, tuples[j]);
            int referringArg = 0;
            Vector<int[]> argNodes = new Vector<>();

            if (mode==0) {
                // node attr
                // if it is an atom take the args directly
                int[] argNode = new int[1];
                if (groundnextsubpf instanceof ProbFormAtom) {
                    if (((ProbFormAtom) groundnextsubpf).getArguments().length == 1)
                        argNode[0] = Integer.parseInt(((ProbFormAtom) groundnextsubpf).getArguments()[0]);
                    else
                        argNode[0] = -1;
                } else if (groundnextsubpf instanceof ProbFormMacroCall) {
                    if (((ProbFormMacroCall) groundnextsubpf).args().length == 1)
                        argNode[0] = Integer.parseInt(((ProbFormMacroCall) groundnextsubpf).args()[0]);
                    else
                        argNode[0] = -1;
                } else if (groundnextsubpf instanceof ProbFormCombFunc) {
                    if (tuples[j].length == 1)
                        argNode[0] = tuples[j][0];
                    if (tuples[j].length == 2) {
                        String groundnextsubpf_str = groundnextsubpf.asString(0, 0, A, false, false);
                        System.out.println(groundnextsubpf_str);
                        String[] quantvars_0 = new String[]{quantvars[0], quantvars[0]};
                        CPModel ground_0 = nextsubpf.substitute(quantvars_0, tuples[j]);
                        String ground_0_str = ground_0.asString(0, 0, A, false, false);
                        System.out.println(ground_0_str);
                        String[] quantvars_1 = new String[]{quantvars[0] + quantvars_0[1], quantvars[0] + quantvars_0[1]};
                        CPModel ground_1 = nextsubpf.substitute(quantvars_1, tuples[j]);
                        String ground_1_str = ground_1.asString(0, 0, A, false, false);
                        // if the string matches (with the same first quantvar repeted) then the first is the one used
                        if (ground_0_str.equals(groundnextsubpf_str)) {
                            if (ground_0_str.equals(ground_1_str))
                                referringArg = -1;
                            else
                                referringArg = 0;
                        } else
                            referringArg = 1;
                        argNode[0] = tuples[j][referringArg];
                    } else if (tuples[j].length != 1 || tuples[j].length != 2)
                        throw new RBNCompatibilityException("Wrong number of arguments for substitution");
                } else if (groundnextsubpf instanceof ProbFormConstant) {
                    argNode[0] = -1;
                }
                argNodes.add(argNode);
            } else if (mode==1) {
                int[] argNode = new int[1];
                // edge attr
                argNode[0] = -1;
                if (groundnextsubpf instanceof ProbFormAtom) {
                    // there should be a check somewhere that does not allow to use relations with one arg...
                    if (((ProbFormAtom) groundnextsubpf).getArguments().length == 2)
                        argNode[0] = j;
                    else
                        argNode[0] = -1;
                } else if (groundnextsubpf instanceof ProbFormMacroCall) {
                    if (((ProbFormMacroCall) groundnextsubpf).args().length == 2)
                        argNode[0] = j;
                    else
                        argNode[0] = -1;
                } else if (groundnextsubpf instanceof ProbFormCombFunc) {
                    if (tuples[j].length == 2)
                        argNode[0] = j;
                    else if (tuples[j].length == 1)
                        argNode[0] = j;
                    else
                        throw new RBNCompatibilityException("Wrong number of arguments for substitution (edge attributes)");
                } else if (groundnextsubpf instanceof ProbFormConstant) {
                    argNode[0] = -1;
                } else {
                    argNode[0] = j;
                }
                argNodes.add(argNode);
            } else if (mode==2) {
                // edges
                if (groundnextsubpf instanceof ProbFormAtom) {
                    int[] argNode = new int[2];
                    // there should be a check somewhere that does not allow to use relations with one arg...
                    if (((ProbFormAtom) groundnextsubpf).getArguments().length == 2) {
                        String[] args = ((ProbFormAtom) groundnextsubpf).getArguments();
                        argNode[0] = Integer.parseInt(args[0]);
                        argNode[1] = Integer.parseInt(args[1]);
                        argNodes.add(argNode);
                    } else // there could be more cases here
                        throw new RBNCompatibilityException("EDGEGRAPH is not a ProbFormArom with 2 arguments");
                } else if (groundnextsubpf instanceof ProbFormBool) {
                    ProbFormBoolComposite groundnextsubpf_bool = (ProbFormBoolComposite) groundnextsubpf;
                    int numComponents = groundnextsubpf_bool.numComponents();
                    for (int i=0; i<numComponents; i++) {
                        if (groundnextsubpf_bool.componentAt(i) instanceof ProbFormBoolAtom) {
                            int[] argNode = new int[2];
                            String[] args = ((ProbFormBoolAtom) groundnextsubpf_bool.componentAt(i)).getArguments();
                            argNode[0] = Integer.parseInt(args[0]);
                            argNode[1] = Integer.parseInt(args[1]);
                            argNodes.add(argNode);
                        }
                    }
                } else
                    throw new RBNCompatibilityException("Currently EDGEGRAPH supports ProbFormAtom,ProbFormBool,ProbFormBoolComposite with 2 arguments");
            }

            // Map node indices to sequential numbers starting from 0
            int numComponents = 0;
            for (int[] argNode : argNodes) {
                if (argNode[0] >= 0 && mode != 2) {
                    argNode[0] = getOrAssignNodeIndex(pftype, argNode[0]);
                } else if (argNode[0] >= 0 && mode == 2 && nextsubpf instanceof ProbFormAtom && ((ProbFormAtom) nextsubpf).getArguments().length == 2) {
                    // remap the edges args
                    argNode[0] = getOrAssignNodeIndex(((ProbFormAtom) nextsubpf).getRelation().getTypes()[0].getName(), argNode[0]);
                    argNode[1] = getOrAssignNodeIndex(((ProbFormAtom) nextsubpf).getRelation().getTypes()[1].getName(), argNode[1]);
                } else if (argNode[0] >= 0 && mode == 2 && nextsubpf instanceof ProbFormBoolComposite) {
                    if (((ProbFormBoolComposite) nextsubpf).componentAt(numComponents) instanceof ProbFormBoolAtom) {
                        ProbFormBoolAtom nextsubpf_atom = (ProbFormBoolAtom) ((ProbFormBoolComposite) nextsubpf).componentAt(numComponents);
                        argNode[0] = getOrAssignNodeIndex(nextsubpf_atom.getRelation().getTypes()[0].getName(), argNode[0]);
                        argNode[1] = getOrAssignNodeIndex(nextsubpf_atom.getRelation().getTypes()[1].getName(), argNode[1]);
                    }
                    numComponents++;
                }
            }

            double evalOfSubPF = (double) groundnextsubpf.evaluate(
                    A,
                    I,
                    new String[0],
                    new int[0],
                    0,
                    false,
                    useCurrentPvals,
                    mapatoms,
                    false,
                    evaluated,
                    parameters,
                    ProbForm.RETURN_ARRAY,
                    true,
                    null)[0];

            if (Double.isNaN(evalOfSubPF)) {
                GGCPMNode constructedchild = GGCPMNode.constructGGPFN(
                        gg,
                        groundnextsubpf,
                        allnodes,
                        A,
                        I,
                        inputcaseno,
                        observcaseno,
                        parameters,
                        false,
                        false,
                        "",
                        mapatoms,
                        evaluated);
                if (!children.contains(constructedchild)) {
                    children.add(constructedchild);
                    String key = groundnextsubpf.makeKey(A);
                    // store the child, the value, the element for substitution, the position in the column
                    evalOfPFs.put(key, new Object[]{constructedchild, argNodes, evalOfSubPF, tuples[j], probFormIdx});
                }
                constructedchild.addToParents(this);
            } else {
                String key = groundnextsubpf.makeKey(A);
                evalOfPFs.put(key, new Object[]{groundnextsubpf, argNodes, evalOfSubPF, tuples[j], probFormIdx});
            }
            evaluated_children.add(groundnextsubpf);
        }
    }

    @Override
    public double[] evaluate(Integer sno) {
        if (this.gnnPy==null)
            throw new RuntimeException("GnnPy is null in GGGnnNode");

        if (this.depends_on_sample && sno==null) {
            for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
                this.evaluate(i);
            return null;
        }
        if (this.depends_on_sample && is_evaluated_val_for_samples[sno])
            return this.values_for_samples[sno];
        if (!this.depends_on_sample && is_evaluated_val_for_samples[0])
            return this.values_for_samples[0];

        double[] result = null;
        if (cpmgnn instanceof CatGnn)
            result = gnnPy.GGevaluate_gnnHetero(sno, A, inst, cpmgnn, this);

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

    @Override
    public Gradient evaluateGradient(Integer sno) throws RBNNaNException {
        if (this.gnnPy==null)
            throw new RuntimeException("GnnPy is null in GGGnnNode");

        if (this.depends_on_sample && sno==null) {
            for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
                this.evaluateGradient(i);
            return null;
        }

        int idx=0;
        if (this.depends_on_sample)
            idx=sno;

        if (is_evaluated_grad_for_samples[idx])
            return  gradient_for_samples.get(idx);

        Gradient result = gradient_for_samples.get(idx);
        result.reset();

//        Object[] outres = gnnPy.evaluate_gnnGradients(A, inst, cpmgnn, this);
        Object[] outres = gnnPy.GGevaluate_gnnGradients(sno, A, inst, cpmgnn, this);
        Map<String, double[][]> grads = (Map<String, double[][]>) outres[1];

        for (String param: this.myparameters) {
            if (grads.containsKey(param))
                result.set_part_deriv(param, grads.get(param)[0]);
//            else
//                System.out.println(param + " not found");
        }

        is_evaluated_grad_for_samples[idx]=true;

        return result;
    }

    @Override
    public boolean isBoolean() {
        return cpmgnn.numvals()==1;
    }

    public static int[][] mergeArrays(int[][] a, int[] b) {
        if (b == null) return a;
        if (a == null) {
            int[][] result = new int[1][];
            result[0] = Arrays.copyOf(b, b.length);
            return result;
        }

        int validRowsA = 0;
        for (int[] row : a) if (row != null) validRowsA++;

        int[][] result = new int[validRowsA + 1][];

        int index = 0;
        for (int[] row : a) {
            if (row != null) {
                result[index++] = Arrays.copyOf(row, row.length);
            }
        }

        result[index] = Arrays.copyOf(b, b.length);
        return result;
    }

    public static int[][] merge2DArrays(int[][] a, int[][] b) {
        int validRowsA = 0;
        int validRowsB = 0;

        for (int[] row : a) if (row != null) validRowsA++;
        for (int[] row : b) if (row != null) validRowsB++;

        int[][] result = new int[validRowsA + validRowsB][];

        int index = 0;
        for (int[] row : a) {
            if (row != null) {
                result[index++] = row;
            }
        }
        for (int[] row : b) {
            if (row != null) {
                result[index++] = row;
            }
        }
        return result;
    }

    // Returns the existing index for (type,nodeId) or assigns the next per-type index starting at 0
    public int getOrAssignNodeIndex(String type, int nodeId) {
        Objects.requireNonNull(type, "type must not be null");
        Map<Integer, Integer> mapping = nodeMappingByType.computeIfAbsent(type, t -> new HashMap<>());
        Integer idx = mapping.get(nodeId);
        if (idx != null) {
            return idx;
        }
        int next = nextIndexByType.getOrDefault(type, 0);
        mapping.put(nodeId, next);
        nextIndexByType.put(type, next + 1);
        return next;
    }

    public Integer getNodeIndexIfPresent(String type, int nodeId) {
        Map<Integer, Integer> mapping = nodeMappingByType.get(type);
        return mapping != null ? mapping.get(nodeId) : null;
    }

    public GnnPy getGnnPy() {
        return this.gnnPy;
    }

    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }

    public CPModel getCpm() {
        return cpmgnn;
    }

    public int outDim() {
    	System.out.println("outDim still needs to be implemented for GGGnnNode");
    	return 0;
    }

    public HashMap<String, HashMap<String, Object[]>> getEvalOfNodes() {
        return evalOfNodes;
    }

    public HashMap<String, HashMap<String, Object[]>> getEvalOfEdgeAttr() {
        return evalOfEdgeAttr;
    }

//    public Hashtable<String, int[][]> getEvalOfEdge() {
//        return evalOfEdge;
//    }


    public HashMap<String, HashMap<String, Object[]>> getEvalOfEdge() {
        return evalOfEdge;
    }

    public boolean comapre(Object obj) {
        // Fast path: reference equality
        if (this == obj) return true;

        // Null check and type check
        if (obj == null || getClass() != obj.getClass()) return false;

        GGGnnNode other = (GGGnnNode) obj;

        // Compare node mapping structures
        if (!Objects.equals(nodeMapping, other.nodeMapping)) return false;
        if (!Objects.equals(nodeMappingByType, other.nodeMappingByType)) return false;
        if (!Objects.equals(nextIndexByType, other.nextIndexByType)) return false;

        // Compare evaluated children
//        if (!Objects.equals(evaluated_children, other.evaluated_children)) return false;

        // Compare evaluation structures
        if (!Objects.equals(evalOfNodes, other.evalOfNodes)) return false;
        if (!Objects.equals(evalOfEdgeAttr, other.evalOfEdgeAttr)) return false;

        // Deep comparison for 2D arrays in evalOfEdge
//        if (!compareEvalOfEdge(evalOfEdge, other.evalOfEdge)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        // Start with parent hashCode
        int result = super.hashCode();

        // Combine with this class's fields
        result = 31 * result + Objects.hashCode(cpmgnn);
        result = 31 * result + Objects.hashCode(gnnPy);
        result = 31 * result + Objects.hashCode(nodeMapping);
        result = 31 * result + Objects.hashCode(nodeMappingByType);
        result = 31 * result + Objects.hashCode(nextIndexByType);
        result = 31 * result + Objects.hashCode(evaluated_children);
        result = 31 * result + Objects.hashCode(evalOfNodes);
        result = 31 * result + Objects.hashCode(evalOfEdgeAttr);
//        result = 31 * result + hashCodeEvalOfEdge(evalOfEdge);
        result = 31 * result + Objects.hashCode(evalOfEdge);

        return result;
    }

    // Helper method for deep comparison of evalOfEdge
    private boolean compareEvalOfEdge(Hashtable<String, int[][]> map1, Hashtable<String, int[][]> map2) {
        if (map1 == map2) return true;
        if (map1 == null || map2 == null) return false;
        if (map1.size() != map2.size()) return false;

        for (String key : map1.keySet()) {
            int[][] arr1 = map1.get(key);
            int[][] arr2 = map2.get(key);

            if (!Arrays.deepEquals(arr1, arr2)) return false;
        }

        return true;
    }

    // Helper method for hashing evalOfEdge
    private int hashCodeEvalOfEdge(Hashtable<String, int[][]> map) {
        if (map == null) return 0;

        int result = 0;
        for (Map.Entry<String, int[][]> entry : map.entrySet()) {
            result += Objects.hashCode(entry.getKey()) + Arrays.deepHashCode(entry.getValue());
        }

        return result;
    }

}
