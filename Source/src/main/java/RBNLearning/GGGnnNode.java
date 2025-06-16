package RBNLearning;

import PyManager.GnnPy;
import PyManager.TorchInputRels;
import PyManager.TorchInputSpecs;
import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNNaNException;
import RBNpackage.*;
import RBNutilities.*;

import java.util.*;

// many of the object are static since GNN needs to have the whole graph as input,
// and it does not make sense to have separate data for the same thing
// if we remove the static keyword we will reach the heap memory limit easily with bigger graphs
public class GGGnnNode extends GGCPMNode {
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
        if (this.cpm instanceof CatGnn) {
            setGnnPy(((CatGnn) cpm).getGnnPy()); // set the same GnnPy from the rel to the ggnode
            getGnnPy().setGradientGraph(gg); // save also the gradient graph

            ProbForm nextsubpf;
            ProbForm groundnextsubpf;
            double evalOfSubPF;
            GGCPMNode constructedchild;
            List<TorchInputRels> torchInputRels = ((CatGnn) this.cpm).getGnnGroundCombinedClauses();
            DoubleVector vals = new DoubleVector();

            for (TorchInputRels torchInputRel: torchInputRels) {
                int[][] subslist = torchInputRel.tuplesSatisfyingCConstr(A, new String[0], new int[0]);

                for (int i = 0; i < torchInputRel.numPFargs(); i++) {
                    nextsubpf = torchInputRel.probformAt(i);
                    for (int j = 0; j < subslist.length; j++) {
                        groundnextsubpf = nextsubpf.substitute(torchInputRel.getQuantvars(), subslist[j]);

                        evalOfSubPF = (double) groundnextsubpf.evaluate(A, I, new String[0], new int[0], false, useCurrentPvals,
                                mapatoms, false, evaluated, parameters, ProbForm.RETURN_ARRAY, true, null)[0];

                        if (Double.isNaN(evalOfSubPF)) {
                            constructedchild = GGCPMNode.constructGGPFN(gg,
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
                            if (!children.contains(constructedchild))
                                children.add(constructedchild);
                            constructedchild.addToParents(this);
                        } else
                            vals.add(evalOfSubPF);
                    }
                }
            }
        } else {
            System.out.println("GGGnnNode cannot accept " + this.cpm.toString() + " as valid pf");
        }
        System.out.println(children);
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
            result = gnnPy.GGevaluate_gnnHetero(A, inst, thisgg, (CatGnn) cpm, this);


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
        return !(cpm instanceof CatGnn);
    } // for now, we return true if is not CatGnn


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
