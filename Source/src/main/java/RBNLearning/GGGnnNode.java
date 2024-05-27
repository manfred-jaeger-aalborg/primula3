package RBNLearning;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNNaNException;
import RBNpackage.*;

import java.util.*;

// many of the object are static since GNN needs to have the whole graph as input,
// and it does not make sense to have separate data for the same thing
// if we remove the static keyword we will reach the heap memory limit easily with bigger graphs
public class GGGnnNode extends GGProbFormNode {
    private ProbForm pf;
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
    public GGGnnNode(GradientGraphO gg,
                     ProbForm pf,
                     Hashtable allnodes,
                     RelStruc A,
                     OneStrucData I,
                     int inputcaseno,
                     int observcaseno,
                     Hashtable<String,Integer> parameters,
                     boolean useCurrentPvals,
                     GroundAtomList mapatoms,
                     Hashtable<String,Object[]>  evaluated ) throws RBNCompatibilityException {
        super(gg, pf, A, I);
        this.pf = pf;
        this.A = A;
        this.inst = I;

        if (pf instanceof ProbFormGnn) {
            if (onsd == null && sampledRel == null) {
                onsd = new OneStrucData(this.A.getmydata().copy()); // only one copy per time
                sampledRel = new SparseRelStruc(this.A.getNames(), onsd, this.A.getCoords(), this.A.signature());
                sampledRel.getmydata().add(this.inst.copy());
            }
            this.attr_parents = this.pf.parentRels();
            this.oneHotEncoding = ((ProbFormGnn) pf).isOneHotEncoding();
            x = "";

            if (num_nodes == -1) { // we do not need to compute it again
                for (Rel attr : ((ProbFormGnn) this.pf).getGnnattr()) {
                    try {
                        int[][] mat = A.allTypedTuples(attr.getTypes());
                        // maybe there could be attributes with different number, we keep the biggest
                        if (attr.arity == 1 && mat.length >= this.num_nodes)
                            this.num_nodes = mat.length;
                    } catch (RBNIllegalArgumentException e) {
                        throw new RuntimeException("Error in GGGnnNode for features creation: " + e);
                    }
                }
            }

            // if the edge relations are predefined compute only once
            if (boolrel == null)
                boolrel = sampledRel.getBoolBinaryRelations();
            edge_index = "";
            this.edge_pred = false;
            if (this.pf instanceof ProbFormGnn gnnpf) {
                for (BoolRel element : boolrel) {
                    if (element.ispredefined()) {
                        if (Objects.equals(element.name(), gnnpf.getEdge_name()))
                            this.edge_pred = true;
                    }
                }
            }

            Rel[] pfargs = ((ProbFormGnn) this.pf).getGnnattr();
            for (int i = 0; i < pfargs.length; i++) {
                if (!pfargs[i].ispredefined()) { // do not add predefined values
                    try {
                        int[][] mat = A.allTypedTuples(pfargs[i].getTypes());
                        for (int j = 0; j < mat.length; j++) {
                            ProbFormAtom atomAsPf = new ProbFormAtom(pfargs[i], mat[j]);
                            GGProbFormNode ggmn = gg.findInAllnodes(atomAsPf, 0, 0, A);
                            if (ggmn == null) {
                                ggmn = GGProbFormNode.constructGGPFN(
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
            System.out.println("GGGnnNode cannot accept " + pf.toString() + " as valid pf");
        }
    }

    @Override
    public double evaluate() {
        if (this.value != null) {
            return this.value;
        }

        // careful this is not a copy! use currentLikelihood for a copy!
        if (this.gnnPy == null) {
            throw new NullPointerException("GnnPy object null!");
        } else {

            if (this.pf instanceof ProbFormGnn gnnpf) {
                // this first part set the x and edge_index object once only if predefined
                if (attr_parents.isEmpty() && Objects.equals(x, "")) {
                    x = this.gnnPy.stringifyGnnFeatures(num_nodes, sampledRel, ((ProbFormGnn)this.pf).getGnnattr(), this.oneHotEncoding);
                }
                if (this.edge_pred && Objects.equals(edge_index, "")) {
                    for (BoolRel element : boolrel) {
                        if (sampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
                            edge_index = "";
                            break;
                        } else {
                            if (Objects.equals(element.name(), gnnpf.getEdge_name())) {
                                this.edge_pred = true;
                                if (Objects.equals(gnnpf.getEdge_direction(), "ABBA"))
                                    edge_index = this.gnnPy.stringifyGnnEdgesABBA(sampledRel, element);
                                if (Objects.equals(gnnpf.getEdge_direction(), "AB"))
                                    edge_index = this.gnnPy.stringifyGnnEdgesAB(sampledRel, element);
                                if (Objects.equals(gnnpf.getEdge_direction(), "BA"))
                                    edge_index = this.gnnPy.stringifyGnnEdgesBA(sampledRel, element);
                                break;
                            }
                        }
                    }
                }
                // else this
                try {
                    for (Rel parent : attr_parents) {
                        int[][] mat = A.allTypedTuples(parent.getTypes());
                        for (int i = 0; i < mat.length; i++) {
                            if (sampledRel.truthValueOf(parent, mat[i]) == -1) {
                                GroundAtom myatom = new GroundAtom(parent, mat[i]);
                                GGAtomMaxNode currentMaxNode = this.thisgg.findInMaxindicators(myatom); // I don't like this so much, I keep now for greediness
                                boolean sampledVal = false;
                                if (currentMaxNode.getCurrentInst() == 1)
                                    sampledVal = true;
                                sampledRel.getmydata().findInBoolRel(parent).add(mat[i], sampledVal);
                            }
                        }
                    }
                    if (!attr_parents.isEmpty())
                        x = this.gnnPy.stringifyGnnFeatures(num_nodes, sampledRel, gnnpf.getGnnattr(), this.oneHotEncoding);
                } catch (RBNIllegalArgumentException e) {
                    throw new RuntimeException(e);
                }

                // take only the "edge" relation
                // this can be later changed to a more general approach
                // find the boolean relations that should represent edges
                double result;
                if (!edge_pred) {
                    boolrel = sampledRel.getBoolBinaryRelations();
                    for (BoolRel element : boolrel) {
                        if (sampledRel.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
                            edge_index = "";
                            break;
                        } else {
                            if (Objects.equals(element.name(), gnnpf.getEdge_name())) {
                                if (Objects.equals(gnnpf.getEdge_direction(), "ABBA"))
                                    edge_index = this.gnnPy.stringifyGnnEdgesABBA(sampledRel, element);
                                if (Objects.equals(gnnpf.getEdge_direction(), "AB"))
                                    edge_index = this.gnnPy.stringifyGnnEdgesAB(sampledRel, element);
                                if (Objects.equals(gnnpf.getEdge_direction(), "BA"))
                                    edge_index = this.gnnPy.stringifyGnnEdgesBA(sampledRel, element);
                                break;
                            }
                        }
                    }
                }

                if (Objects.equals(((ProbFormGnn) this.pf).getGnn_inference(), "node"))
                    this.value = this.gnnPy.inferModelNodeDouble(Integer.parseInt(gnnpf.getArgument()), gnnpf.getClassId(), x, edge_index, ((ProbFormGnn) this.pf).getIdGnn(), "");
                else if (Objects.equals(((ProbFormGnn) this.pf).getGnn_inference(), "graph")) {
                    this.value = this.gnnPy.inferModelGraphDouble(gnnpf.getClassId(), x, edge_index, ((ProbFormGnn) this.pf).getIdGnn(),"");
                } else
                    throw new IllegalArgumentException("not valid keyword used: " + ((ProbFormGnn) this.pf).getGnn_inference());

//                if (((ProbFormGnn) this.pf).getClassId() != -1)
//                    result = this.gnnPy.inferModelGraphDouble(gnnpf.getClassId(), x, this.edge_index, ((ProbFormGnn) this.pf).getIdGnn(), "");
//                else
//                    result = this.gnnPy.inferModelNodeDouble(Integer.parseInt(gnnpf.getArgument()), gnnpf.getClassId(), x, this.edge_index, ((ProbFormGnn) this.pf).getIdGnn(), "");
//                this.value = result;
                return this.value;
            } else {
                System.out.println("Not a correct instance of PF");
                return -1;
            }
        }
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
    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }
}
