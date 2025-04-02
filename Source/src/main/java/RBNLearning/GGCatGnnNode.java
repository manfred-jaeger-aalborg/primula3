package RBNLearning;

import java.util.*;
import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNNaNException;
import RBNpackage.CPModel;
import PyManager.GnnPy;
import RBNpackage.OneStrucData;
import RBNpackage.RelStruc;

public class GGCatGnnNode extends GGCPMNode implements GGCPMGnn {


    public GGCatGnnNode(GradientGraphO gg, CPModel cpm, RelStruc A, OneStrucData I) throws RBNCompatibilityException {
        super(gg, cpm, A, I);
    }

    @Override
    public double[] evaluate(Integer sno) {
        return new double[0];
    }

    @Override
    public Gradient evaluateGradient(Integer sno) throws RBNNaNException {
        throw new UnsupportedOperationException("Unimplemented method 'evaluateGradient in GGCATGnnNode'");
    }

    // @Override
    // public void evaluateBounds() {

    // }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public GnnPy getGnnPy() {
        return null;
    }

    @Override
    public void setGnnPy(GnnPy gnnPy) {

    }

//    @Override
//    public void setValue(Double[] value) {
//        this.value = value;
//    }

//    @Override
    public double evaluateGrad(Integer sno) throws RBNNaNException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'evaluateGrad'");
    }
}
