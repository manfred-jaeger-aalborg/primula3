package RBNLearning;

import PyManager.GnnPy;
import PyManager.TorchInputRels;
import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNNaNException;
import RBNpackage.*;
import RBNutilities.*;

import java.util.*;

public class GGGnnNode extends GGCPMNode {
    private CPModel cpm;
    private RelStruc A;
    private OneStrucData inst;
    private GnnPy gnnPy;

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
    }

    @Override
    public double[] evaluate(Integer sno) {
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
        return cpm.numvals()==1;
    }


    public GnnPy getGnnPy() {
        return this.gnnPy;
    }

    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }

    public CPModel getCpm() {
        return cpm;
    }

    public int outDim() {
    	System.out.println("outDim still needs to be implemented for GGGnnNode");
    	return 0;
    }
}
