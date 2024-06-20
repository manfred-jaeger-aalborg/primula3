package RBNLearning;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNNaNException;
import RBNpackage.*;

import java.util.*;

// many of the object are static since GNN needs to have the whole graph as input,
// and it does not make sense to have separate data for the same thing
// if we remove the static keyword we will reach the heap memory limit easily with bigger graphs
public class GGGnnNode extends GGCPMNode implements GGCPMGnn {
    private CPModel cpm;
    private RelStruc A;
    private OneStrucData inst;
    private GnnPy gnnPy;
    private boolean oneHotEncoding;
    static private OneStrucData onsd;
    static private SparseRelStruc sampledRel;
    private TreeSet<Rel> attr_parents;
    static private String x;
    static private int num_nodes = -1;
    static private Vector<BoolRel> boolrel;
    static private String edge_index;
    private boolean edge_pred; // true if the edges are predefined --> avoid to reconstruct again in evaluate
    private boolean savedData;
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
        this.cpm = cpm;
        this.A = A;
        this.inst = I;
        savedData = false;
        if (this.cpm instanceof ProbFormGnn || this.cpm instanceof CatGnn) {
            Rel[] pfargs = ((CPMGnn) this.cpm).getGnnattr();
            for (int i = 0; i < pfargs.length; i++) {
                if (!pfargs[i].ispredefined()) { // do not add predefined values
                    try {
                        int[][] mat = A.allTypedTuples(pfargs[i].getTypes());
                        for (int j = 0; j < mat.length; j++) {
                            ProbFormAtom atomAsPf = new ProbFormAtom(pfargs[i], mat[j]);
                            GGCPMNode ggmn = gg.findInAllnodes(atomAsPf, 0, 0, A);
                            if (ggmn == null) {
                                ggmn = GGCPMNode.constructGGPFN(
                                        gg,
                                        atomAsPf,
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
//                        ggmn = new GGAtomMaxNode(gg, atomAsPf, A, I, 0, 0);
                                allnodes.put(gg.makeKey(atomAsPf, 0, 0, A), ggmn);
                                //            fnode.setInstvalToIndicator();
                                //            ggmn.setUGA(fnode);
                                this.children.add(ggmn);
                                ggmn.addToParents(this);
                            }
                            this.children.add(ggmn);
                            ggmn.addToParents(this);
                        }

                    } catch (RBNIllegalArgumentException e) {
                        throw new RuntimeException(e);
                    }
                } else {
//                    System.out.println(pfargs[i].toString() + " non prob - skipped");
                }
            }
        } else {
            System.out.println("GGGnnNode cannot accept " + this.cpm.toString() + " as valid pf");
        }
    }

    @Override
    public Double[] evaluate(Integer sno) {
        if (!savedData) {
            this.gnnPy.saveGnnData((CPMGnn) cpm, A, inst);
            savedData = true;
        }

        if (this.value != null) {
            return this.value;
        }
        this.value = gnnPy.GGevaluate_gnn(A, thisgg, (CPMGnn) cpm, this);

        if (this.isuga() && isBoolean()) {
            int iv = this.instval(); // Can only be 0,1, or -1, because if a relation is defined by ProbFormCombFunc
            // it can only be Boolean
            if (iv == -1)
                System.out.println("Warning: undefined instantiation value in GGCombFuncNode.evaluate()");
            if (iv == 0)
                this.value[0] = 1 - this.value[0];
        }

        return this.value;

//        if (this.gnnPy == null) {
//            throw new NullPointerException("GnnPy object null!");
//        } else {
//
//            if (this.cpm instanceof ProbFormGnn gnnpf) {
//                // this first part set the x and edge_index object once only if predefined
//                if (attr_parents.isEmpty() && Objects.equals(x, "")) {
//                    x = this.gnnPy.stringifyGnnFeatures(num_nodes, sampledRel, ((ProbFormGnn)this.cpm).getGnnattr(), this.oneHotEncoding);
//                }
//                if (this.edge_pred && Objects.equals(edge_index, "")) {
//                    for (BoolRel element : boolrel) {
//                        if (sampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
//                            edge_index = "";
//                            break;
//                        } else {
//                            if (Objects.equals(element.name(), gnnpf.getEdge_name())) {
//                                this.edge_pred = true;
//                                if (Objects.equals(gnnpf.getEdge_direction(), "ABBA"))
//                                    edge_index = this.gnnPy.stringifyGnnEdgesABBA(sampledRel, element);
//                                if (Objects.equals(gnnpf.getEdge_direction(), "AB"))
//                                    edge_index = this.gnnPy.stringifyGnnEdgesAB(sampledRel, element);
//                                if (Objects.equals(gnnpf.getEdge_direction(), "BA"))
//                                    edge_index = this.gnnPy.stringifyGnnEdgesBA(sampledRel, element);
//                                break;
//                            }
//                        }
//                    }
//                }
//                // else this
//                try {
//                    for (Rel parent : attr_parents) {
//                        int[][] mat = A.allTypedTuples(parent.getTypes());
//                        for (int i = 0; i < mat.length; i++) {
//                            // find the nodes that have no values (-1) and assign the values form the currentInst in the maxIndicator to the sampledRel
//                            if (Objects.equals(sampledRel.getData().find(parent).dv(), "?")) {
////                            if (sampledRel.truthValueOf(parent, mat[i]) == -1) {
//                                GroundAtom myatom = new GroundAtom(parent, mat[i]);
//                                GGAtomMaxNode currentMaxNode = this.thisgg.findInMaxindicators(myatom); // I don't like this so much, I keep now for greediness
//                                boolean sampledVal = false;
//                                if (currentMaxNode.getCurrentInst() == 1)
//                                    sampledVal = true;
//                                sampledRel.getmydata().findInBoolRel(parent).add(mat[i], sampledVal);
//                            }
//                        }
//                    }
//                    if (!attr_parents.isEmpty())
//                        x = this.gnnPy.stringifyGnnFeatures(num_nodes, sampledRel, gnnpf.getGnnattr(), this.oneHotEncoding);
//                } catch (RBNIllegalArgumentException e) {
//                    throw new RuntimeException(e);
//                }
//
//                // take only the "edge" relation
//                // this can be later changed to a more general approach
//                // find the boolean relations that should represent edges
//                double result;
//                if (!edge_pred) {
//                    boolrel = sampledRel.getBoolBinaryRelations();
//                    for (BoolRel element : boolrel) {
//                        if (sampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
//                            edge_index = "";
//                            break;
//                        } else {
//                            if (Objects.equals(element.name(), gnnpf.getEdge_name())) {
//                                if (Objects.equals(gnnpf.getEdge_direction(), "ABBA"))
//                                    edge_index = this.gnnPy.stringifyGnnEdgesABBA(sampledRel, element);
//                                if (Objects.equals(gnnpf.getEdge_direction(), "AB"))
//                                    edge_index = this.gnnPy.stringifyGnnEdgesAB(sampledRel, element);
//                                if (Objects.equals(gnnpf.getEdge_direction(), "BA"))
//                                    edge_index = this.gnnPy.stringifyGnnEdgesBA(sampledRel, element);
//                                break;
//                            }
//                        }
//                    }
//                }
//
//                if (Objects.equals(((ProbFormGnn) this.cpm).getGnn_inference(), "node"))
//                    this.value = this.gnnPy.inferModelNodeDouble(Integer.parseInt(gnnpf.getArgument()), gnnpf.getClassId(), x, edge_index, ((ProbFormGnn) this.cpm).getIdGnn(), "", true);
//                else if (Objects.equals(((ProbFormGnn) this.cpm).getGnn_inference(), "graph")) {
//                    this.value = this.gnnPy.inferModelGraphDouble(gnnpf.getClassId(), x, edge_index, ((ProbFormGnn) this.cpm).getIdGnn(),"");
//                } else
//                    throw new IllegalArgumentException("not valid keyword used: " + ((ProbFormGnn) this.cpm).getGnn_inference());
//
//                if (this.isuga()) {
//                    int iv = this.instval(); // Can only be 0,1, or -1, because if a relation is defined by ProbFormCombFunc
//                    // it can only be Boolean
//                    if (iv == -1)
//                        System.out.println("Warning: undefined instantiation value in GGCombFuncNode.evaluate()");
//                    if (iv == 0)
//                        this.value = 1 - this.value;
//                }
//
//                return this.value;
//            } else {
//                System.out.println("Not a correct instance of PF");
//                return -1;
//            }
//        }
    }

    @Override
    public boolean isBoolean() {
        return !(cpm instanceof CatGnn);
    }

    @Override
    public double evaluateGrad(String param) throws RBNNaNException {
        System.out.println("evaluateGrad code");
        return -1;
    }

    @Override
    public void evaluateBounds() {
        System.out.println("evaluateBounds code");
    }

    @Override
    public GnnPy getGnnPy() {
        return null;
    }

    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }

    @Override
    public void setValue(Double[] value) {
        this.value = value;
    }
}
