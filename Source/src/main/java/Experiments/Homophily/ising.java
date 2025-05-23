package Experiments.Homophily;

import RBNLearning.GradientGraph;
import RBNLearning.RelDataForOneInput;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNpackage.*;
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
        String N = "32";
        String J = "0.5";
        String Jb = "0.0";
        String temp = "0.4";
        Boolean node_const= true;
        String expName = "HP_noisy";
        String model = "GGCN_raf";
        String r = "0";

//        J = args[0];
//        Jb = args[1];
//        expName = args[2];
//        model = args[3];
//        r = args[4];

        System.out.println("**************************");
        System.out.println(J + " " + Jb + " " + expName + " " + model);
        System.out.println("**************************");

        Primula primula = new Primula();
        primula.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
        primula.setScriptPath("/Users/lz50rg/Dev/primula-workspace/primula3/Source/python/");
        primula.setScriptName("load_gnn");

        Map<String, Object> load_gnn_set = new HashMap<>();
        load_gnn_set.put("sdataset", "ising");
        load_gnn_set.put("base_path", "/Users/lz50rg/Dev/homophily/experiments/ising/trained/");
//        load_gnn_set.put("model", "GGCN_raf");
//        load_gnn_set.put("model", "GraphNet");
//        load_gnn_set.put("model", "MLP");
        load_gnn_set.put("model", model);
        load_gnn_set.put("nfeat", 1);
        load_gnn_set.put("nlayers", 2);
        load_gnn_set.put("nclass", 2);

        if (model.equals("MLP"))
            load_gnn_set.put("nhid", 32);
        else
            load_gnn_set.put("nhid", 16);

        load_gnn_set.put("N", Integer.valueOf(N));
        load_gnn_set.put("J", Double.valueOf(J));
        load_gnn_set.put("Jb", Double.valueOf(Jb));
        load_gnn_set.put("temp", Double.valueOf(temp));
        load_gnn_set.put("iter", 4);
        load_gnn_set.put("r", r);
        load_gnn_set.put("noisy", false);

        primula.setLoadGnnSet(load_gnn_set);

        File srsfile = null;
        if (node_const)
            srsfile = new File("/Users/lz50rg/Dev/homophily/experiments/ising/rdef/ising_" + N + "_" + J + "_" + Jb + "_" + temp + "_" + "4_nodeconst_" + expName + ".rdef");
        else
            srsfile = new File("/Users/lz50rg/Dev/homophily/experiments/ising/rdef/ising_" + N + "_" + J + "_" + Jb + "_" + temp + "_" + "4.rdef");
        System.out.println(srsfile);
        primula.loadSparseRelFile(srsfile);

        // create rbn
        ArrayList<ArrayList<Rel>> attrs_rels = new ArrayList<>();
        Rel[] inp_rel = new Rel[1];
        for (int i = 0; i < 1; i++) {
            inp_rel[i] = new NumRel("attr" + i, 1);
        }
        attrs_rels.add(
                new ArrayList<Rel>(
                        Arrays.asList(
                                inp_rel
                        )
                )
        );

        BoolRel edgeRel = new BoolRel("edge", 2, typeStringToArray("node,node",2));
        ArrayList<Rel> edge_attr = new ArrayList<>();
        edge_attr.add(edgeRel);
        edge_attr.get(0).setInout(Rel.PREDEFINED);

        RBNPreldef gnn_rbn = new  RBNPreldef(
                new CatRel("CAT", 1, typeStringToArray("node",1), valStringToArray("POS,NEG")),
                new String[]{"v"},
                new CatGnn("v",
                        load_gnn_set.get("model")+"ising",
                        -1,
                        2,
                        attrs_rels,
                        edge_attr,
                        "node",
                        true
                )
        );

        File input_file = null;
        if (node_const)
//            input_file = new File("/Users/lz50rg/Dev/homophily/experiments/ising/const_ising_glob.rbn");
            input_file = new File("/Users/lz50rg/Dev/homophily/experiments/rbn_constraints/const_nodeconst.rbn");
        else
            input_file = new File("/Users/lz50rg/Dev/homophily/experiments/ising/const_ising.rbn");

        System.out.println(input_file);
        RBN file_rbn = new RBN(input_file, primula.getSignature());
        RBNPreldef[] preledef = file_rbn.prelements();

        RBN manual_rbn = new RBN(2, 0);
        manual_rbn.insertPRel(gnn_rbn, 0);
        manual_rbn.insertPRel(preledef[0], 1);

//        RBN manual_rbn = new RBN(1, 0);
//        manual_rbn.insertPRel(gnn_rbn, 0);

        // add the rbn to primula
        primula.setRbn(manual_rbn);
        primula.getInstantiation().init(manual_rbn);
        primula.setRbnparameters(manual_rbn.parameters());

        // the relation to query
        CatRel tmp_query = new CatRel("CAT", 1, typeStringToArray("node",1), valStringToArray("POS,NEG"));
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

            // retrieve the data to query
            OneBoolRelData query_nodes = prob_data.inputDomain().getData().findInBoolRel("query_nodes");
            TreeSet<int[]> true_data = query_nodes.allTrue();
            List<Integer> instantiated_nodes = new ArrayList<>();
            for (int[] node: true_data) {
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
            im.setNumChains(0);
            im.setWindowSize(0);
            im.setMapSearchAlg(2);
            im.setNumIterGreedyMap(50000);
            GradientGraph GG = im.startMapThread();
            im.getMapthr().join();

            // collect results
            Hashtable<Rel, int[]> bestMapVals = im.getMapthr().getBestMapVals();

            String[] vals = new String[]{"POS","NEG"};
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
            if (GG != null){
                result.setParentRelStruc(primula.getRels());

                for (int i=0; i<gal.size(); i++) {
                    result.add(new GroundAtom(gal.atomAt(i).rel(), gal.atomAt(i).args), res[i],"?");
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
                for (Integer value: pred_res.get(i)) {
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
            System.out.println("time: " + (float)((end - start)));

            String pred_node_path = null;
            if (node_const)
                pred_node_path = "/Users/lz50rg/Dev/homophily/experiments/ising/pred_labels/pred_labels_" + load_gnn_set.get("model") + "_" + N + "_" + J + "_" + Jb + "_" + temp + "_nodeconst_" + expName + ".txt";
            else
                pred_node_path = "/Users/lz50rg/Dev/homophily/experiments/ising/pred_labels/pred_labels_" + load_gnn_set.get("model") + "_" + N + "_" + J + "_" + Jb + "_" + temp + ".txt";

            try (FileWriter writer = new FileWriter(pred_node_path)) {
                for (int i = 0; i < gt_class.length; i++) {
                    for (int[] el: gt_class[i].allTrue()) {
                        int val = el[0];
                        // find the value in the prediction
                        for (ArrayList<Integer> pred_class: pred_res) {
                            for (Integer node: pred_class) {
                                if (val == node) {
                                    // node,predicted,label
                                    writer.write(val + "," + pred_res.indexOf(pred_class) + "," + i + "\n");
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/lz50rg/Dev/homophily/experiments/ising/ising_results.txt", true))) {
                writer.newLine();
                writer.write(J + " " + Jb + " " + expName + " " + model + " r" + r + "\n");
                writer.write("Accuracy: " + accuracy + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
