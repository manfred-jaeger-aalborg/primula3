package RBNLearning;

import java.util.Hashtable;
import java.util.Vector;

public abstract class Gradient {

    /* mapping of parameter names to integer indices. Usually same as Primula.parameters
     */
    Hashtable<String,Integer> parameter_to_idx;

    public class IdxPD {
        /* an instance of this class is a pair (idx,[pd]) of an integer index for a specific parameter,
        and a double array [pd] with the partial derivative for this parameter.
         */
        int idx;
        double[] pd;

        public IdxPD(int idx, double[] pd) {
            this.idx = idx;
            this.pd = pd;
        }

        public int getIdx() {
            return idx;
        }
        public double[] getPd() {
            return pd;
        }
    }

    public abstract void set_part_deriv(String param, double[] value);
//    public abstract void set_part_deriv(int idx, double[] value);

    public abstract double[] get_part_deriv(String param);
    public abstract double[] get_part_deriv(int indx);

    /*Returns an array of size k x 2 where k is the number of parameters with nonzero partial
    * derivative. A row [i,d] in the return array contains the partial derivative d for the
    * parameter with index i */
    public abstract Vector<IdxPD> as_idxpd_list();


    public abstract void reset();
}
