package RBNLearning;

import PyManager.GnnPy;
import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNNaNException;
import RBNpackage.*;
import RBNutilities.*;

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
    private static boolean xPred;
    private static boolean edgePred; // true if the edges are predefined --> avoid to reconstruct again in evaluate
    private static boolean savedData = false;
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
        if (this.gnnPy == null)
            savedData = false;
        xPred = false; edgePred = false;
        if (this.cpm instanceof ProbFormGnn) {
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
        } else if (this.cpm instanceof CatGnn || this.cpm instanceof CatGnnOld) {
            setGnnPy(((CatGnn) cpm).getGnnPy()); // set the same GnnPy from the rel to the ggnode
            getGnnPy().setGradientGraph(gg); // save also the gradient graph
            for (Pair<BoolRel, ArrayList<Rel>> pair : ((CPMGnn) this.cpm).getGnnInputs()) {
                ArrayList<Rel> pfargs = pair.getSecond();
                for (Rel pfargRel: pfargs) {
                    if (!pfargRel.ispredefined()) { // do not add predefined values
                        xPred = true;
                        try {
                            int[][] mat = A.allTypedTuples(pfargRel.getTypes());
                            for (int k = 0; k < mat.length; k++) {
                                ProbFormAtom atomAsPf = new ProbFormAtom(pfargRel, mat[k]);

                                // we add as children only atoms that are influenced up to a max layer
                                Set<Integer> allReached = null;
                                if (((CPMGnn) cpm).getNumLayers() > 0)
                                    allReached = rbnutilities.getNodesInDepth(thisgg.myPrimula.getRels(), ((CPMGnn) cpm).getNumLayers(), mat[k][0], (CPMGnn) cpm);

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
                                    allnodes.put(gg.makeKey(atomAsPf, 0, 0, A), ggmn);
                                    this.children.add(ggmn);
                                    ggmn.addToParents(this);
                                }
                                if (((CPMGnn) cpm).getNumLayers() > 0 && allReached != null && allReached.contains(Integer.parseInt(((CPMGnn) this.cpm).getArgument())) ) {
                                    this.children.add(ggmn);
                                     ggmn.addToParents(this);
                                } else if (((CPMGnn) cpm).getNumLayers() <= 0){
                                    this.children.add(ggmn);
                                    ggmn.addToParents(this);
                                } else if (allReached == null && ((CPMGnn) cpm).getNumLayers() > 0)
                                    throw new RuntimeException("Something bad happened in the construction of GGGnnNode");
                            }

                        } catch (RBNIllegalArgumentException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                // also for the edges
                Rel edge = pair.getFirst();
                if (!edge.ispredefined()) {
                    edgePred = true;
                    try {
                        // TODO we can optimize the child with the layers depth also for the edges
                        int[][] mat = A.allTypedTuples(edge.getTypes());
                        for (int k = 0; k < mat.length; k++) {
                            ProbFormAtom atomAsPf = new ProbFormAtom(edge, mat[k]);
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
                                allnodes.put(gg.makeKey(atomAsPf, 0, 0, A), ggmn);
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
            }
        } else {
            System.out.println("GGGnnNode cannot accept " + this.cpm.toString() + " as valid pf");
        }
    }

    @Override
    public double[] evaluate(Integer sno) {
//        if (!savedData) {
//            this.gnnPy.saveGnnData((CPMGnn) cpm, A, inst);
//            savedData = true;
//        }

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
        if (cpm instanceof CatGnn)
            result = gnnPy.GGevaluate_gnnHetero(A, inst, thisgg, (CPMGnn) cpm, this);
        else
            result = gnnPy.GGevaluate_gnn(A, thisgg, (CPMGnn) cpm, this);

        if (this.depends_on_sample) {
            if (cpm instanceof CatGnn)
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
        throw new RuntimeException("evaluatePartDeriv(Integer sno, String param) NOT IMPLEMENTED in GGGnnNode");
    }

    @Override
    public boolean isBoolean() {
        return !(cpm instanceof CatGnnOld || cpm instanceof CatGnn);
    } // for now, we return true if is not CatGnn

    @Override
    public GnnPy getGnnPy() {
        return this.gnnPy;
    }

    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }

//    @Override
//    public void setValue(Double[] value) {
//        this.value = value;
//    }


    public CPModel getCpm() {
        return cpm;
    }

    public boolean isXPred() {
        return xPred;
    }

    public boolean isEdgePred() {
        return edgePred;
    }


    public int outDim() {
    	System.out.println("outDim still needs to be implemented for GGGnnNode");
    	return 0;
    }
}
