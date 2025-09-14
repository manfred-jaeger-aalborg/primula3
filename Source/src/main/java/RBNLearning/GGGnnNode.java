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
    // we can have only one reference og gnnPy on the same thread
    // once the thread will create and use gnnPy it will set this variable
    private GnnPy gnnPy;
    // those two variable are "shared"
    private static RelStruc A;
    private static OneStrucData inst;

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
        // store the CPModel, and the "input" for the gnn (A and I)
        // those variables will be used in the evaluate function
        this.cpm = cpm;
        this.A = A;
        this.inst = I;

        if (this.cpm instanceof CatGnn) {
            setGnnPy(((CatGnn) cpm).getGnnPy()); // set the same GnnPy from the rel to the ggnode
            getGnnPy().setGradientGraph(gg); // save also the gradient graph

            CPModel nextsubpf;
            CPModel groundnextsubpf;
            double evalOfSubPF;
            GGCPMNode constructedchild;
            List<TorchInputRels> torchInputRels = ((CatGnn) this.cpm).getGnnGroundCombinedClauses();
            DoubleVector vals = new DoubleVector();

            // differently from all the other probability formulas here we evaluate
            // all the components of the GNN to see when it is not possible to evaluate.
            // the subpf which are not evaluatable will return NaN and added to the children vector
            for (TorchInputRels torchInputRel: torchInputRels) {
                int[][] subslist = torchInputRel.tuplesSatisfyingCConstr(A, new String[0], new int[0]);

                for (int i = 0; i < torchInputRel.numPFargs(); i++) {
                    nextsubpf = torchInputRel.probformAt(i);
                    for (int j = 0; j < subslist.length; j++) {
                        groundnextsubpf = nextsubpf.substitute(torchInputRel.getQuantvars(), subslist[j]);

                        evalOfSubPF = (double) groundnextsubpf.evaluate(A,
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
        if (cpm instanceof CatGnn)
            result = gnnPy.GGevaluate_gnnHetero(A, inst, (CatGnn) cpm, this);

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

        double[] values = values_for_samples[idx];

        Object[] outres = gnnPy.evaluate_gnnHetero(A, inst, (CatGnn) cpm, false);
        Map<String, double[][]> grads = (Map<String, double[][]>) outres[1];

        for (String param: this.myparameters) {
            double[] res = new double[values.length];
//            for (int i=0;i<values.length;i++) {
//                res[i] = values[i] * grads.get(param)[0][i];
//            }
//            result.set_part_deriv(param, res);
            result.set_part_deriv(param, grads.get(param)[0]);
        }

        is_evaluated_grad_for_samples[idx]=true;

        return result;
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
