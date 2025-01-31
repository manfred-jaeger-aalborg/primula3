package RBNLearning;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNNaNException;
import RBNpackage.CPModel;
import RBNpackage.GnnPy;
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
    public Double[] evaluatePartDeriv(Integer sno, String param) throws RBNNaNException {
        throw new UnsupportedOperationException("Unimplemented method 'evaluatePartDeriv'");
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
    public double evaluateGrad(Integer sno, String param) throws RBNNaNException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'evaluateGrad'");
    }
}
