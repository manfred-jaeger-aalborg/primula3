package RBNLearning;

import java.util.Hashtable;
import java.util.Vector;

public class Gradient_Array extends Gradient{

    /* Array of dimension k x dim where k is the number of parameters, and dim
    is the (output) dimension of the node/function for which the gradient is computed
    gradient[idx] should be null if the gradient for parameter idx is the zero vector.
     */
    double[][] gradient;

    public Gradient_Array(Hashtable<String,Integer> params){
        parameter_to_idx = params;
        gradient = new double[params.size()][];
    }

    @Override
    public void set_part_deriv(String param, double[] value) {
        gradient[parameter_to_idx.get(param)] = value;
    }

    @Override
    public double[] get_part_deriv(String param) {
        return gradient[parameter_to_idx.get(param)];
    }

    @Override
    public double[] get_part_deriv(int indx) {
        return gradient[indx];
    }

    @Override
    public Vector<IdxPD> as_idxpd_list() {
        Vector<IdxPD> result = new Vector<IdxPD>();
        for (int i = 0; i < gradient.length; i++) {
            if (gradient[i] != null) {
                result.add(new IdxPD(i, gradient[i]));
            }
        }
        return result;
    }

    @Override
    public void reset() {
        gradient = new double[gradient.length][];
    }
}
