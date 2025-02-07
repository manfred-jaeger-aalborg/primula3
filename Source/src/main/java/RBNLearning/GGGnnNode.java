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
    private boolean xPred;
    private boolean edgePred; // true if the edges are predefined --> avoid to reconstruct again in evaluate
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
        } else if (this.cpm instanceof CatGnnHetero || this.cpm instanceof CatGnn) {
            ArrayList<ArrayList <Rel>> pfargs = ((CPMGnn) this.cpm).getInput_attr();
            for (int i = 0; i < pfargs.size(); i++) {
                for (int j = 0; j < pfargs.get(i).size(); j++) {
                    if (!pfargs.get(i).get(j).ispredefined()) { // do not add predefined values
                        xPred = true;
                        try {
                            int[][] mat = A.allTypedTuples(pfargs.get(i).get(j).getTypes());
                            for (int k = 0; k < mat.length; k++) {
                                ProbFormAtom atomAsPf = new ProbFormAtom(pfargs.get(i).get(j), mat[k]);
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
            }
            // also for the edges
            ArrayList <Rel> pfargs_edges = ((CPMGnn) this.cpm).getEdge_attr();
            for (Rel edge : pfargs_edges) {
                if (!edge.ispredefined()) {
                    edgePred = true;
                    try {
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
        if (!savedData) {
            this.gnnPy.saveGnnData((CPMGnn) cpm, A, inst);
            savedData = true;
        }

        if (this.depends_on_sample && sno==null) {
            for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
                this.evaluate(i);
            return null;
        }
        if (this.depends_on_sample && is_evaluated_for_samples[sno])
            return this.values_for_samples[sno];
        if (!this.depends_on_sample && is_evaluated_for_samples[0])
            return this.values_for_samples[0];

        double[] result = null;
        if (cpm instanceof CatGnnHetero)
            result = gnnPy.GGevaluate_gnnHetero(A, thisgg, (CPMGnn) cpm, this);
        else
            result = gnnPy.GGevaluate_gnn(A, thisgg, (CPMGnn) cpm, this);

        if (this.depends_on_sample) {
            if (cpm instanceof CatGnnHetero)
                values_for_samples[sno] = result;
            is_evaluated_for_samples[sno] = true;
        } else {
            values_for_samples[0] = result;
            is_evaluated_for_samples[0] = true;
        }

        return result;
    }

    @Override
    public boolean isBoolean() {
        return !(cpm instanceof CatGnn || cpm instanceof CatGnnHetero);
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


    public boolean isXPred() {
        return xPred;
    }

    public boolean isEdgePred() {
        return edgePred;
    }

    @Override
    public Double[] evaluateGradient(Integer sno, String param) throws RBNNaNException {
        throw new RuntimeException("evaluatePartDeriv(Integer sno, String param) NOT IMPLEMENTED in GGGnnNode");
    }

    public int outDim() {
    	System.out.println("outDim still needs to be implemented for GGGnnNode");
    	return 0;
    }
}
