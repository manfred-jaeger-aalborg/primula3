package RBNLearning;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNNaNException;
import RBNinference.PFNetworkNode;
import RBNpackage.*;

import java.util.*;

public class GGGnnNode extends GGProbFormNode {

    private ProbForm pf;
    private RelStruc A;
    private OneStrucData inst;
    private GnnPy gnnPy;
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
            Rel[] pfargs = ((ProbFormGnn) this.pf).getGnnattr();
            for (int i = 0; i < pfargs.length; i++) {
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
            OneStrucData onsd = new OneStrucData(this.A.getmydata().copy());
            int num_features = 0;
            SparseRelStruc sampledRel = new SparseRelStruc(this.A.getNames(), onsd, this.A.getCoords(), this.A.signature());
            sampledRel.getmydata().add(this.inst.copy());
            for (Rel parent : this.pf.parentRels()) {
                try {
                    int[][] mat = A.allTypedTuples(parent.getTypes());
                    // maybe there could be attributes with different number, we keep the biggest
                    if (parent.arity == 1 && mat.length >= num_features)
                        num_features = mat.length;

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

                } catch (RBNIllegalArgumentException e) {
                    throw new RuntimeException(e);
                }
            }

            // take only the "edge" relation
            // this can be later changed to a more general approach
            // find the boolean relations that should represent edges
            Vector<BoolRel> boolrel = sampledRel.getBoolBinaryRelations();
            String edge_index = "";
            if (this.pf instanceof ProbFormGnn gnnpf) {
                for (BoolRel element : boolrel) {
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

                String x = this.gnnPy.stringifyGnnFeatures(num_features, sampledRel, gnnpf.getGnnattr());
                double result = this.gnnPy.inferModelNodeDouble(Integer.parseInt(gnnpf.getArgument()), x, edge_index, "");
                this.value = result;
                return result;
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
