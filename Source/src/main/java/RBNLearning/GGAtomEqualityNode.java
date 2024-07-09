package RBNLearning;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNNaNException;
import RBNpackage.*;

import java.util.Hashtable;
import java.util.Vector;

public class GGAtomEqualityNode extends GGCPMNode{

    private Object arg1, arg2;
    Vector<Double> evalOfPFs;
    public GGAtomEqualityNode(GradientGraphO gg,
                              ProbForm pf,
                              Hashtable<String,GGCPMNode> allnodes,
                              RelStruc A,
                              OneStrucData I,
                              int inputcaseno,
                              int observcaseno,
                              Hashtable<String,Integer> parameters,
                              boolean useCurrentPvals,
                              Hashtable<Rel, GroundAtomList> mapatoms,
                              Hashtable<String,Object[]>  evaluated) throws RBNCompatibilityException {
        super(gg,pf,A,I);

        arg1 = ((ProbFormBoolAtomEquality) pf).args()[0];
        arg2 = ((ProbFormBoolAtomEquality) pf).args()[1];
        evalOfPFs = new Vector<>();
        Double res;
        res = (Double)((ProbForm)arg1).evaluate(A,
                I ,
                new String[0],
                new int[0] ,
                false,
                useCurrentPvals,
                mapatoms,
                false,
                evaluated,
                parameters,
                ProbForm.RETURN_ARRAY,
                true,
                null)[0];

        GGCPMNode constructedchild;
        if (Double.isNaN(res)) {
            constructedchild = GGCPMNode.constructGGPFN(gg,
                    (ProbFormAtom) arg1,
                    allnodes,
                    A,
                    I,
                    inputcaseno,observcaseno,
                    parameters,
                    false,
                    false,
                    "",
                    mapatoms,
                    evaluated);
            children.add(constructedchild);
            constructedchild.addToParents(this);
        } else {
            evalOfPFs.add(res);
        }

        res = (Double) ((ProbForm)arg2).evaluate(A,
                I ,
                new String[0],
                new int[0] ,
                false,
                useCurrentPvals,
                mapatoms,
                false,
                evaluated,
                parameters,
                ProbForm.RETURN_ARRAY,
                true,
                null)[0];

        if (Double.isNaN(res)) {
            constructedchild = GGCPMNode.constructGGPFN(gg,
                    (ProbFormAtom) arg2,
                    allnodes,
                    A,
                    I,
                    inputcaseno,observcaseno,
                    parameters,
                    false,
                    false,
                    "",
                    mapatoms,
                    evaluated);
            children.add(constructedchild);
            constructedchild.addToParents(this);
        } else {
            evalOfPFs.add(res);
        }

        System.out.println();
    }

    @Override
    public Double[] evaluate(Integer sno) {
        if (is_evaluated) {
            if (this.values_for_samples==null)
                return value;
            else
                return this.values_for_samples[sno];
        }

        double[] final_res = new double[2];

        for (int i = 0; i < evalOfPFs.size(); i++) {
               final_res[i] = (double) evalOfPFs.get(i);
        }
        for (int i = 0; i < children.size(); i++) {
            final_res[i+evalOfPFs.size()] = (double) children.get(i).evaluate(sno)[0];
        }


        if (final_res[0] == final_res[1])
            value = new Double[]{1.0};
        else
            value = new Double[]{0.0};

        return value;
    }

    @Override
    public double evaluateGrad(String param) throws RBNNaNException {
        return 0;
    }

    @Override
    public void evaluateBounds() {

    }

    @Override
    public boolean isBoolean() {
        return false;
    }
}
