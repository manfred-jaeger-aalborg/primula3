package Experiments.Water;

import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.GradientGraph;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNpackage.*;
import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;
import RBNutilities.rbnutilities;

import java.io.File;
import java.util.*;

public class RiverPollutionMAP {

    // functions copied from RDEFReader.java
    private static Type[] typeStringToArray(String ts, int arity){
        Type[] result = new Type[arity];
        String nexttype;
        int nextcomma;
        for (int i=0;i<arity;i++)
        {
            nextcomma = ts.indexOf(",");
            if (nextcomma != -1){
                nexttype = ts.substring(0,nextcomma);
                ts = ts.substring(nextcomma+1);
            }
            else{
                nexttype = ts;
                ts = "";
            }
            if (nexttype.equals("Domain"))
                result[i]=new TypeDomain();
            else
                result[i]=new TypeRel(nexttype);
        }
        return result;
    }

    private static String[] valStringToArray(String vs) {
        return rbnutilities.stringToArray(vs,",");
    }

    public static void main(String[] args) {
        Primula primula = new Primula();

        primula.loadSparseRelFile(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/simple_subbasin_new.rdef"));
        primula.loadRBNFunction(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/water_pollution_model.rbn"));


        Vector<GroundAtomList> gal_vec = new Vector<>();
        RelStruc input_struct = primula.getRels();

        String val_name = "CORN,COSY,PAST,SOYB";
        CatRel tmp_query = new CatRel("LandUse", 1, typeStringToArray("agr", 1), valStringToArray(val_name));
        tmp_query.setInout(Rel.PROBABILISTIC);

        try {
            InferenceModule im = primula.createInferenceModule();

            int[][] mat = input_struct.allTypedTuples(tmp_query.getTypes());
            gal_vec.add(new GroundAtomList());
            for (int[] ints: mat) gal_vec.get(0).add(tmp_query, ints);
            im.addQueryAtoms(tmp_query, gal_vec.get(0));

            im.setNumRestarts(1);
            GradientGraph GG = im.startMapThread();
            im.getMapthr().join();

            HashMap<Rel, int[]> bestMapVals = im.getMapthr().getBestMapVals();

            String[] vals = new String[]{val_name};
            int[] res = bestMapVals.get(tmp_query);
            ArrayList<ArrayList<Integer>> pred_res = new ArrayList<>(vals.length);
            for (int i = 0; i < vals.length; i++)
                pred_res.add(new ArrayList<>());

            // count how many crops type has been assigned
            Map<String, Integer> values_count = new HashMap<>();
            List<String> crops = Arrays.asList(val_name.split(","));
            for (int i = 0; i < 4; i++) {
                values_count.put(crops.get(i), 0);
            }

            System.out.println("\nMAP INFERENCE RESULTS:\n");
            for (GroundAtomList gal: gal_vec) {
                for (int i = 0; i < gal.size(); i++) {
                    System.out.println(gal.atomAt(i).rel + Arrays.toString(gal.atomAt(i).args) + ": " + bestMapVals.get(gal.atomAt(i).rel)[i]);
                    if (gal.atomAt(i).relname().equals("LandUse"))
                        values_count.put(crops.get(res[i]), values_count.get(crops.get(res[i]))+1);
                }
            }

            System.out.println("Final GG logLikelihood: " + GG.currentLogLikelihood());

            System.out.println(values_count);

            System.exit( 0 );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (RBNIllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }
}

