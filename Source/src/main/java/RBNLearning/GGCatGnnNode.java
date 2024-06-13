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
    public Double[] evaluate(Integer sno) {
        return new Double[0];
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

    @Override
    public GnnPy getGnnPy() {
        return null;
    }

    @Override
    public void setGnnPy(GnnPy gnnPy) {

    }

    @Override
    public void setValue(Double[] value) {
        this.value = value;
    }
}
