//package Experiments.Misc;
//
//import RBNLearning.*;
//import RBNgui.InferenceModule;
//import RBNgui.Primula;
//import RBNpackage.*;
//import RBNutilities.rbnutilities;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.*;
//
//public class gnn_categorical {
//
//    // functions copied from RDEFReader.java
//    private static Type[] typeStringToArray(String ts, int arity){
//        Type[] result = new Type[arity];
//        String nexttype;
//        int nextcomma;
//        for (int i=0;i<arity;i++)
//        {
//            nextcomma = ts.indexOf(",");
//            if (nextcomma != -1){
//                nexttype = ts.substring(0,nextcomma);
//                ts = ts.substring(nextcomma+1);
//            }
//            else{
//                nexttype = ts;
//                ts = "";
//            }
//            if (nexttype.equals("Domain"))
//                result[i]=new TypeDomain();
//            else
//                result[i]=new TypeRel(nexttype);
//        }
//        return result;
//    }
//
//    private static String[] valStringToArray(String vs) {
//        return rbnutilities.stringToArray(vs,",");
//    }
//
//    public static void main(String[] args) {
//        Primula primula = new Primula();
//        primula.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
//        primula.setScriptPath("/Users/lz50rg/Dev/primula-workspace/primula3/Source/python/");
//        primula.setScriptName("load_gnn");
//
//        File srsfile = new File("/Users/lz50rg/Dev/homophily/categorical_gnn/homophily_2.rdef");
//        primula.loadSparseRelFile(srsfile);
//
//        // create rbn
//        int num_attr = 2;
//        Rel[] attrs_rels = new Rel[num_attr];
//        for (int i = 1; i <= num_attr; i++) {
//            attrs_rels[i-1] = new NumRel("attr" + i, 1);
//        }
//
//        RBNPreldef gnn_rbn = new  RBNPreldef(
//                new CatRel("CAT", 1, typeStringToArray("node",1), valStringToArray("A,B,C,D")),
//                new String[]{"v"},
//                new CatGnn("v",
//                        "GCNcat",
//                        true,
//                        4,
//                        attrs_rels,
//                        "edge",
//                        "ABBA",
//                        "node",
//                        true
//                )
//        );
//
//        File input_file = new File("/Users/lz50rg/Dev/homophily/categorical_gnn/const.rbn");
//        RBN file_rbn = new RBN(input_file, primula.getSignature());
//        RBNPreldef[] preledef = file_rbn.prelements();
//
////        RBN manual_rbn = new RBN(1, 0);
////        manual_rbn.insertPRel(gnn_rbn, 0);
//
//        RBN manual_rbn = new RBN(2, 0);
//        manual_rbn.insertPRel(gnn_rbn, 0);
//        manual_rbn.insertPRel(preledef[0], 1);
//
//        // add the rbn to primula
//        primula.setRbn(manual_rbn);
//        primula.getInstantiation().init(manual_rbn);
//
//        // the relation to query
//        CatRel tmp_query = new CatRel("CAT", 1, typeStringToArray("node",1), valStringToArray("A,B,C,D"));
//        tmp_query.setInout(1);
//
//        GroundAtomList gal = new GroundAtomList();
//        RelStruc input_struct = primula.getRels();
//        RelDataForOneInput prob_data = primula.getReldata().elementAt(0);
//
//        try {
//            InferenceModule im = primula.openInferenceModule(false);
//
//            // retrieve the data to query
//            OneBoolRelData query_nodes = prob_data.inputDomain().getData().findInBoolRel("query_nodes");
//            TreeSet<int[]> true_data = query_nodes.allTrue();
//            List<Integer> instantiated_nodes = new ArrayList<>();
//            for (int[] node: true_data) {
//                instantiated_nodes.add(node[0]);
//            }
//
//            int[][] mat = input_struct.allTypedTuples(tmp_query.getTypes());
//            for (int[] ints : mat) {
//                if (instantiated_nodes.contains(ints[0]))
//                    gal.add(tmp_query, ints);
//            }
//            im.addQueryAtoms(tmp_query, gal);
//
//            // perform map inference
//            im.setNumRestarts(1);
//            GradientGraph GG = im.startMapThread();
//            im.getMapthr().join();
//
//            // collect results
//            Hashtable<Rel, int[]> bestMapVals = im.getMapthr().getBestMapVals();
//
//            String[] vals = new String[]{"A","B","C","D"};
//            int[] res = bestMapVals.get(tmp_query);
//            ArrayList<ArrayList<Integer>> pred_res = new ArrayList<>(vals.length);
//            for (int i = 0; i < vals.length; i++)
//                pred_res.add(new ArrayList<>());
//
//            // print results
//            System.out.println("\nMAP INFERENCE RESULTS:\n");
//            for (int i = 0; i < gal.allAtoms().size(); i++) {
//                System.out.println(gal.atomAt(i).rel().toString() + "(" + gal.atomAt(i).args()[0] + "): " + res[i]);
//                pred_res.get(res[i]).add(Integer.valueOf(gal.atomAt(i).args()[0]));
//            }
//
//            // save results in the current Data
//            OneStrucData result = new OneStrucData();
//            if (GG != null){
//                result.setParentRelStruc(primula.getRels());
//
//                for (int i=0; i<gal.size(); i++) {
//                    result.add(new GroundAtom(gal.atomAt(i).rel(), gal.atomAt(i).args), res[i],"?");
//                }
//
//                primula.getInstantiation().add(result);
//                im.updateInstantiationList();
//                primula.updateBavaria();
//            }
//
//            // compute accuracy
//            OneStrucData onsd = new OneStrucData(primula.getRels().getmydata().copy());
//            SparseRelStruc sampledRel = new SparseRelStruc(primula.getRels().getNames(), onsd, primula.getRels().getCoords(), primula.getRels().signature());
//            sampledRel.getmydata().add(primula.getInstantiation().copy());
//
//            OneBoolRelData[] gt_class = new OneBoolRelData[vals.length];
//            for (int i = 0; i < vals.length; i++) {
//                gt_class[i] = sampledRel.getData().findInBoolRel("ground_pos_" + vals[i]);
//            }
//
//            int correctPredictions = 0;
//            int totalPredictions = gal.size();
//
//            for (int i = 0; i < pred_res.size(); i++) {
//                for (Integer value: pred_res.get(i)) {
//                    if (gt_class[i].allTrue().contains(new int[]{value})) {
//                        correctPredictions++;
//                    }
//                }
//            }
//
//            double accuracy = (double) correctPredictions / totalPredictions;
//            System.out.println("Accuracy: " + accuracy);
//
//            // write on file the results
//            try (FileWriter writer = new FileWriter("/Users/lz50rg/Dev/homophily/categorical_gnn/pred_labels.txt")) {
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
//
//
//        } catch (Exception e) {
//            System.out.println(e);
//        }
//
//    }
//}
