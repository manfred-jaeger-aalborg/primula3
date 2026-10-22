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
import java.io.FileWriter;
import java.io.IOException;
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

//        primula.loadSparseRelFile(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/water_network_gibbs.rdef"));
//        primula.loadRBNFunction(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/water_pollution-gibbs.rbn"));
        primula.loadSparseRelFile(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/water_network_gibbs.rdef"));
        primula.loadRBNFunction(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/water_pollution-gibbs.rbn"));

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
            im.setWindowSize(100);
            im.setNumChains(20);
//            im.setMapSearchAlg(3);
//            im.setNumIterGreedyMap(150);

//            im.setSampleSizeScoring(2);
            im.setBatchSearchSize(1);
//            im.setScoreNegative(true);

            Long time = System.currentTimeMillis();

            GradientGraph GG = im.startMapThread();
            im.getMapthr().join();

            Long endTime = System.currentTimeMillis() - time;
            System.out.println("Time: " + endTime / 1000.0);


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
                areas_per_crop.put(crops.get(i), 0.0);
            }

            OneNumRelData areas = null;
            for (OneNumRelData onr : input_struct.getmydata().getAllonenumdata()) {
                if (onr.rel().name().contains("AreaAgr")) {
                    areas = onr;
                    break;
                }
            }

            // profit per unit area for each crop (see @profit_land in water_pollution-gibbs.rbn)
            Map<String, Double> profitPerArea = new HashMap<>();
            profitPerArea.put("CORN", 5.0);
            profitPerArea.put("COSY", 2.0);
            profitPerArea.put("PAST", 1.0);
            profitPerArea.put("SOYB", 4.0);

            System.out.println("\nMAP INFERENCE RESULTS:\n");
            StringBuilder nodeResults = new StringBuilder();
            nodeResults.append("node\tcrop\tarea\n");
            double totalProfit = 0.0;
            for (GroundAtomList gal: gal_vec) {
                for (int i = 0; i < gal.size(); i++) {
                    if (gal.atomAt(i).relname().equals("LandUse")) {
                        String crop = crops.get(res[i]);
                        values_count.put(crop, values_count.get(crop) + 1);
                        double ar = 0.0;
                        if (areas != null) {
                            ar = areas.valueOf(gal.atomAt(i).args());
                            areas_per_crop.put(crop, areas_per_crop.get(crop) + ar);
                        }
                        totalProfit += ar * profitPerArea.get(crop);

                        String node = input_struct.nameAt(gal.atomAt(i).args()[0]);
                        nodeResults.append(node)
                                .append("\t")
                                .append(crop)
                                .append("\t")
                                .append(ar)
                                .append("\n");
                    }
                }
            }
            nodeResults.append("\nTotal profit: ").append(totalProfit).append("\n");

            System.out.println("Final GG logLikelihood: " + GG.currentLogLikelihood());

            System.out.println(values_count);
            System.out.println(areas_per_crop);
            System.out.println("Total profit: " + totalProfit);

            String outFile = "/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/map_results.txt";
            try (FileWriter writer = new FileWriter(outFile)) {
                writer.write(nodeResults.toString());
                System.out.println("Saved MAP inference results per node to: " + outFile);
            } catch (IOException e) {
                System.err.println("IO error while saving MAP inference results");
                e.printStackTrace();
            }

            System.exit( 0 );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (RBNIllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }
}

