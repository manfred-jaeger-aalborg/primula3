package Experiments.Homophily;

import RBNLearning.GradientGraph;
import RBNLearning.RelDataForOneInput;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNpackage.*;
import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;
import RBNutilities.rbnutilities;

import java.io.*;
import java.util.*;

public class ising {

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

//        String[] Ns = new String[]{"16", "32", "64"};
        String[] Ns = new String[]{"128"};
        long[] timers = new long[Ns.length];
        for (int nIdx = 0; nIdx < Ns.length; nIdx++) {
            String N = "64";
            String J = "-0.5";
            String Jb = "0.0";
            String temp = "0.4";
            Boolean node_const = true;
            String expName = "HP";
            String model = "GCN";
            String r = "0";
            int Nint = Integer.parseInt(Ns[nIdx]);
//        J = args[0];
//        Jb = args[1];
//        expName = args[2];
//        model = args[3];
//        r = args[4];

            System.out.println("**************************");
            System.out.println(J + " " + Jb + " " + expName + " " + model);
            System.out.println("**************************");

            Primula primula = new Primula();

//            File srsfile = new File("/Users/lz50rg/Dev/homophily/experiments/ising/rdef/ising_" + N + "_" + J + "_" + Jb + "_" + temp + "_" + "4.rdef");
//            File srsfile = new File("/Users/lz50rg/Dev/NeSy-for-graph-data/hetero-hom-experiments/ising/rdef/ising_" + Ns[nIdx] + "_-0.5_0.0_0.4_" + ((Nint*Nint)-1) + "_nodeconst_HP.rdef");
            File srsfile = new File("/Users/lz50rg/Dev/NeSy-for-graph-data/hetero-hom-experiments/ising/rdef/ising_128_0.5_0.0_0.4_16383_nodeconst_HP_noisy.rdef");
//            System.out.println(srsfile);
            primula.loadSparseRelFile(srsfile);

            File rbnfile = new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/GNN-homophily/Ising/ising_rbn_2path.rbn");
//            File rbnfile = new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/GNN-homophily/Ising/ising.rbn");
//            File rbnfile = new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/GNN-homophily/Ising/ising_rbn_2path.rbn");

            primula.loadRBNFunction(rbnfile);

            // the relation to query
            CatRel tmp_query = new CatRel("CAT", 1, typeStringToArray("node", 1), valStringToArray("POS,NEG"));
            tmp_query.setInout(1);

            GroundAtomList gal = new GroundAtomList();
            RelStruc input_struct = primula.getRels();
            RelDataForOneInput prob_data = primula.getReldata().elementAt(0);
//        try {
//            PrintStream fileOut = new PrintStream(new File("/Users/lz50rg/Dev/homophily/experiments/ising/output.txt"));
//            System.setOut(fileOut);
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }

            try {
                final long start = System.currentTimeMillis();

                InferenceModule im = primula.createInferenceModule();
                im.setVerbose(false);

                // retrieve the data to query
                OneBoolRelData query_nodes = prob_data.inputDomain().getData().findInBoolRel("query_nodes");
                TreeSet<int[]> true_data = query_nodes.allTrue();
                List<Integer> instantiated_nodes = new ArrayList<>();
                for (int[] node : true_data) {
                    instantiated_nodes.add(node[0]);
                }

                int[][] mat = input_struct.allTypedTuples(tmp_query.getTypes());
                for (int[] ints : mat) {
                    if (instantiated_nodes.contains(ints[0]))
                        gal.add(tmp_query, ints);
                }
                im.addQueryAtoms(tmp_query, gal);

                // perform map inference
                im.setNumRestarts(1);
                GradientGraph GG = im.startMapThread();
                im.getMapthr().join();

                // collect results
                HashMap<Rel, int[]> bestMapVals = im.getMapthr().getBestMapVals();

                String[] vals = new String[]{"POS", "NEG"};
                int[] res = bestMapVals.get(tmp_query);
                ArrayList<ArrayList<Integer>> pred_res = new ArrayList<>(vals.length);
                for (int i = 0; i < vals.length; i++)
                    pred_res.add(new ArrayList<>());

                // print results
//            System.out.println("\nMAP INFERENCE RESULTS:\n");
                for (int i = 0; i < gal.allAtoms().size(); i++) {
//                System.out.println(gal.atomAt(i).rel().toString() + "(" + gal.atomAt(i).args()[0] + "): " + res[i]);
                    pred_res.get(res[i]).add(Integer.valueOf(gal.atomAt(i).args()[0]));
                }

                // save results in the current Data
                OneStrucData result = new OneStrucData();
                if (GG != null) {
                    result.setParentRelStruc(primula.getRels());

                    for (int i = 0; i < gal.size(); i++) {
                        result.add(new GroundAtom(gal.atomAt(i).rel(), gal.atomAt(i).args), res[i], "?");
                    }

                    primula.getInstantiation().add(result);
                    im.updateInstantiationList();
                    primula.updateBavaria();
                }

                // compute accuracy
                OneStrucData onsd = new OneStrucData(primula.getRels().getmydata().copy());
                SparseRelStruc sampledRel = new SparseRelStruc(primula.getRels().getNames(), onsd, primula.getRels().getCoords(), primula.getRels().signature());
                sampledRel.getmydata().add(primula.getInstantiation().copy());

                OneBoolRelData[] gt_class = new OneBoolRelData[vals.length];
                for (int i = 0; i < vals.length; i++) {
                    gt_class[i] = sampledRel.getData().findInBoolRel("ground_" + vals[i]);
                }

                int correctPredictions = 0;
                int totalPredictions = gal.size();

                for (int i = 0; i < pred_res.size(); i++) {
                    for (Integer value : pred_res.get(i)) {
                        if (gt_class[i].allTrue().contains(new int[]{value})) {
                            correctPredictions++;
                        }
                    }
                }
                System.out.println(totalPredictions);
                System.out.println(correctPredictions);
                double accuracy = (double) correctPredictions / totalPredictions;
                System.out.println("Accuracy: " + accuracy);

                long end = System.currentTimeMillis();
                System.out.println("time: " + (float) ((end - start)));
                timers[nIdx] = end - start;
//            String pred_node_path = "/Users/lz50rg/Dev/homophily/experiments/ising/pred_labels/pred_labels_" + model + "_" + N + "_" + J + "_" + Jb + "_" + temp + expName + ".txt";
//
//            try (FileWriter writer = new FileWriter(pred_node_path)) {
//                for (int i = 0; i < gt_class.length; i++) {
//                    for (int[] el: gt_class[i].allTrue()) {
//                        int val = el[0];
//                        // find the value in the prediction
//                        for (ArrayList<Integer> pred_class: pred_res) {
//                            for (Integer node: pred_class) {
//                                if (val == node) {
//                                    // node,predicted,label
//                                    writer.write(val + "," + pred_res.indexOf(pred_class) + "," + i + "\n");
//                                }
//                            }
//                        }
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

//                try (BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/lz50rg/Dev/homophily/experiments/ising/ising_results.txt", true))) {
//                    writer.newLine();
//                    writer.write(J + " " + Jb + " " + expName + " " + model + " r" + r + "\n");
//                    writer.write("Accuracy: " + accuracy + "\n");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < Ns.length; i++) {
            System.out.println("N: " + Ns[i] + " time: " + (float) timers[i] / 1000);
        }
        System.exit(0);
    }
}
