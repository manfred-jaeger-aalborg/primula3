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
        } else if (this.cpm instanceof CatGnnHetero) {
            ArrayList<ArrayList <Rel>> pfargs = ((CatGnnHetero) this.cpm).getInput_attr();
            for (int i = 0; i < pfargs.size(); i++) {
                for (int j = 0; j < pfargs.get(i).size(); j++) {
                    if (!pfargs.get(i).get(j).ispredefined()) { // do not add predefined values
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
        // Temp fix
        if (cpm instanceof CatGnnHetero)
            this.value = gnnPy.GGevaluate_gnnHetero(A, thisgg, (CPMGnn) cpm, this);
        else
            this.value = gnnPy.GGevaluate_gnn(A, thisgg, (CPMGnn) cpm, this);

        return this.value;
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

    @Override
    public void setValue(Double[] value) {
        this.value = value;
    }

    @Override
    public double evaluatePartDeriv(Integer sno, String param) throws RBNNaNException {
        return 0;
    }

    public int outDim() {
    	System.out.println("outDim still needs to be implemented for GGGnnNode");
    	return 0;
    }
}
