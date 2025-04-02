package RBNLearning;

import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Vector;

public class Gradient_TreeMap extends Gradient{


    TreeMap<String,double[]> gradient;

    public Gradient_TreeMap(Hashtable<String,Integer> params){
        parameter_to_idx = params;
        gradient = new TreeMap<String,double[]>();
    }

    @Override
    public void set_part_deriv(String param, double[] value) {
        gradient.put(param,value);
    }

    @Override
    public double[] get_part_deriv(String param) {
     //   System.out.print("get" + param  + "size: " + gradient.size());
 //       double[] result = gradient.get(param);
//        if(param.equals("us2(s796)") && gradient.size()==7){
//            System.out.println("stop");
//        }
 //       System.out.println("   ... " + result);
        return gradient.get(param);
    }

    @Override
    /* This is not needed/supported in this implementation of Gradient*/
    public double[] get_part_deriv(int indx) {
        return null;
    }

    @Override
    public Vector<IdxPD> as_idxpd_list() {
        Vector<IdxPD> result = new Vector<IdxPD>();
        for(String key : gradient.keySet()){
            result.add(new IdxPD(parameter_to_idx.get(key),gradient.get(key) ));
        }
        return result;
    }

    @Override
    public void reset() {
        gradient.clear();
    }
}
