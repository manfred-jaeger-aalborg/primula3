package Experiments.Water;

import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.GradientGraph;
import RBNgui.*;
import RBNinference.SampleProbs;
import RBNpackage.*;
import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;
import RBNutilities.rbnutilities;

import java.io.File;
import java.util.*;

public class RiverPollution {
    public static int EXPNUM = 0;

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

//        String expNum = args[0];
//        double constStrength = Double.parseDouble(args[1]);
//        EXPNUM = Integer.parseInt(expNum);

        int expNum = 18;
        int restart = 1;
        double constStrength = 0.5;
        EXPNUM = expNum;

        System.out.println("exp: " + expNum + " constr: " + constStrength);
        Primula primula = new Primula();

        primula.loadSparseRelFile(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/water_network_gibbs.rdef"));
//        primula.loadSparseRelFile(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/simple_subbasin.rdef"));
        primula.loadRBNFunction(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/water_pollution-gibbs.rbn"));

        String val_name = "CORN,COSY,PAST,SOYB";
        
        CatRel tmp_query = new CatRel("LandUse", 1, typeStringToArray("agr", 1), valStringToArray(val_name));
        tmp_query.setInout(Rel.PROBABILISTIC);

        try {
            InferenceModule im = primula.createInferenceModule();
            final long start = System.currentTimeMillis();

            RelStruc input_struct = primula.getRels();
            int[][] mat = input_struct.allTypedTuples(tmp_query.getTypes());

            GroundAtomList gal = new GroundAtomList();
            for (int i = 0; i < mat.length; i++) {
                    gal.add(tmp_query, new int[]{mat[i][0]});
            }
            im.addQueryAtoms(tmp_query, gal);
            im.setMapSearchAlg(2);
            im.setBatchSearchSize(10);
            im.setCandidateSampleSize(400);

            im.setNumRestarts(1);
            im.setWindowSize(50);
            im.setNumChains(5);
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
            Map<String, Double> areas_per_crop = new HashMap<>();
            List<String> crops = Arrays.asList(val_name.split(","));
            for (int i = 0; i < 4; i++) {
                values_count.put(crops.get(i), 0);
                areas_per_crop.put(crops.get(i), 0.);
            }


//            PrintWriter writer = new PrintWriter("best_txt_graph_" + expNum + ".txt", "UTF-8");
//            writer.println("constr: " + constStrength);

            OneNumRelData areas=null;
            for (OneNumRelData onr: input_struct.getmydata().getAllonenumdata()) {
                if (onr.rel().name().contains("AreaAgr")) {
                    areas = onr;
                    break;
                }
            }

            System.out.println("\nMAP INFERENCE RESULTS:\n");
            for (int i = 0; i < gal.size(); i++) {
//                writer.println(gal.atomAt(i).args()[0] + " : " + res[i]);
                System.out.println(gal.atomAt(i).rel().toString() + "(" + gal.atomAt(i).args()[0] + "): " + res[i]);
                values_count.put(crops.get(res[i]), values_count.get(crops.get(res[i]))+1);
                double ar = areas.valueOf(gal.atomAt(i).args());
                areas_per_crop.put(crops.get(res[i]), areas_per_crop.get(crops.get(res[i])) + ar);
            }

            System.out.println("Final GG logLikelihood: " + GG.currentLogLikelihood());

            long end = System.currentTimeMillis();
            System.out.println("time: " + (float)((end - start)));

            System.out.println(values_count);
            System.out.println(areas_per_crop);

            // Save values
//            OneStrucData result = new OneStrucData();
//            result.setParentRelStruc(primula.getRels());
//            Enumeration<Rel> e = (Enumeration<Rel>) bestMapVals.keySet();
//            while (e.hasMoreElements()) {
//                Rel rel = e.nextElement();
//                int[] nodes = bestMapVals.get(rel);
//                for (int i = 0; i < nodes.length; i++) {
//                    result.add(new GroundAtom(gal.atomAt(i).rel(), gal.atomAt(i).args), bestMapVals.get(rel)[i],"?");
//                }
//            }
//            primula.getInstantiation().add(result);
//            // ------------------------------------
//
//            im.deleteQueryAtoms();
//            BoolRel queryRel = new BoolRel("constr", 0);
//            primula.getInstantiation().delete(queryRel, new int[0]);
//
//            GroundAtomList queryGround = new GroundAtomList();
//            if (queryRel.getArity()==0) {
//                queryGround.add(new GroundAtom(queryRel,new int[0]));
//            }
//            im.addQueryAtom(queryRel, queryGround, 0);
//
//            im.startSampleThread();
//            System.out.println("Start sampling...");
//
//            double size = 0;
//            double oldsize = -1;
//            System.out.println("Sampling ...");
//            while (size < 40000) {
//                size = im.getSamThr().getNumsamp();
//                if (oldsize != size) {
//                    oldsize = size;
//                    if (size % 10000 == 0)
//                        System.out.println("Sample size: " + size);
//                }
//            }
//
//            im.stopSampleThread();
//            im.getSampthr().join();
//            SampleProbs finalSprobs = im.getSampthr().getSprobs();
//            System.out.println(Arrays.deepToString(finalSprobs.getProbs(queryRel)));
////            writer.println("constr: " + Arrays.deepToString(finalSprobs.getProbs(queryRel)));
////            writer.close();
//            System.exit( 0 );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (RBNIllegalArgumentException e) {
            throw new RuntimeException(e);
        }
//        catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
    }
}

