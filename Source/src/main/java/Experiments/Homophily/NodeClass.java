package Experiments.Homophily;

import RBNLearning.GradientGraph;
import RBNLearning.RelDataForOneInput;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNpackage.*;
import RBNutilities.rbnutilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NodeClass {

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

    public static String generateCommaSeparatedString(int length) {
        return IntStream.range(0, length)
                .mapToObj(i -> String.valueOf((char) (65 + i)))
                .collect(Collectors.joining(","));
    }

    public static void main(String[] args) {

        int ij = Integer.parseInt(args[0]);
        String datasetName = args[1];
        int NUM_ATTR = Integer.parseInt(args[2]);
        String modelName = args[3];
        int nhidd = Integer.parseInt(args[4]);
        int nlayers = Integer.parseInt(args[5]);
        int NUM_CLASS = Integer.parseInt(args[6]);
        double decayRate = Double.parseDouble(args[7]);
        boolean useSparse = Boolean.parseBoolean(args[8]);
        String expName = args[9];
        String homType = args[10];

//        int ij = 0;
//        String datasetName = "cora";
//        int NUM_ATTR = 1433;
//        String modelName = "GGCN";
//        int nhidd = 16;
//        int nlayers = 32;
//        int NUM_CLASS = 7;
//        double decayRate = 0.9;
//        String expName = "realDataset";
//        String homType = "homProp";

//        String modelName = "GGCN";
        String base_path = "/Users/lz50rg/Dev/homophily/experiments/";

        Map<String, Object> load_gnn_set = new HashMap<>();
        load_gnn_set.put("sdataset", datasetName);
        load_gnn_set.put("base_path", "/nfs/home/cs.aau.dk/lz50rg/dev/Heterophily_and_oversmoothing-clus/pretrained/");
        load_gnn_set.put("model", modelName);
        load_gnn_set.put("nfeat", NUM_ATTR);
        load_gnn_set.put("nlayers", nlayers);
        load_gnn_set.put("nclass", NUM_CLASS);
        load_gnn_set.put("nhid", nhidd);
        load_gnn_set.put("decayRate", decayRate);
        load_gnn_set.put("use_sparse", useSparse);
        load_gnn_set.put("exp", expName);

        Primula primula = new Primula();
        primula.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
        primula.setScriptPath("/Users/lz50rg/Dev/primula-workspace/primula3/Source/python/");
        primula.setScriptName("load_gnn");
        primula.setLoadGnnSet(load_gnn_set);

        String index = Integer.toString(ij);
        Boolean loc_h = false;
        Boolean count_h = false;
        Boolean node_const = true;

        File srsfile = null;
        if (loc_h)
            srsfile = new File(base_path + datasetName + "/rdef/" + datasetName + "_loc_" + index + ".rdef");
        else if (node_const)
            srsfile = new File(base_path + datasetName + "/rdef/" + datasetName + "_nodeconst_" + homType + "_" + index + ".rdef");
        primula.loadSparseRelFile(srsfile);

        // create rbn
        ArrayList<ArrayList<Rel>> attrs_rels = new ArrayList<>();
        Rel[] inp_rel = new Rel[NUM_ATTR];
        for (int i = 0; i < NUM_ATTR; i++) {
            if (datasetName == "pubmed")
                inp_rel[i] = new NumRel("attr" + i, 1);
            else
            inp_rel[i] = new BoolRel("attr" + i, 1);
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

        String sclass = generateCommaSeparatedString(NUM_CLASS);

        RBNPreldef gnn_rbn = new RBNPreldef(
                new CatRel("CAT", 1, typeStringToArray("node", 1), valStringToArray(sclass)),
                new String[]{"v"},
                new CatGnn("v",
                        modelName + datasetName + index,
                        true,
                        NUM_CLASS,
                        attrs_rels,
                        edge_attr,
                        "node",
                        true
                )
        );

        String const_path = null;
        if (loc_h)
            const_path = base_path + datasetName + "/const_" + datasetName + "_loc.rbn";
        else if (count_h)
            const_path = base_path + datasetName + "/const_" + datasetName + "_count.rbn";
        else if (node_const)
            const_path = base_path + "rbn_constraints/const_nodeconst.rbn";
        else
            const_path = base_path + datasetName + "/const_" + datasetName + "_.rbn";

        File input_file = new File(const_path);
        RBN file_rbn = new RBN(input_file, primula.getSignature());
        RBNPreldef[] preledef = file_rbn.prelements();

        RBN manual_rbn = null;
        if (count_h) {
            manual_rbn = new RBN(2, 0);
            manual_rbn.insertPRel(gnn_rbn, 0);
//            manual_rbn.insertPRel(preledef[0], 1);
            manual_rbn.insertPRel(preledef[1], 2);
        } else {
            manual_rbn = new RBN(2, 0);
            manual_rbn.insertPRel(gnn_rbn, 0);
            manual_rbn.insertPRel(preledef[0], 1);
        }

//        RBN manual_rbn = new RBN(1, 0);
//        manual_rbn.insertPRel(gnn_rbn, 0);

        // add the rbn to primula
        primula.setRbn(manual_rbn);
        primula.getInstantiation().init(manual_rbn);
        primula.setRbnparameters(manual_rbn.parameters());

        // the relation to query
        CatRel tmp_query = new CatRel("CAT", 1, typeStringToArray("node", 1), valStringToArray(sclass));
        tmp_query.setInout(1);

        GroundAtomList gal = new GroundAtomList();
        RelStruc input_struct = primula.getRels();
        RelDataForOneInput prob_data = primula.getReldata().elementAt(0);

        try {
            InferenceModule im = primula.createInferenceModule();
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
            im.setNumRestarts(5);
            im.setMapSeachAlg(0);
            im.setNumIterGreedyMap(10000);
            GradientGraph GG = im.startMapThread();
            im.getMapthr().join();

            // collect results
            Hashtable<Rel, int[]> bestMapVals = im.getMapthr().getBestMapVals();

            String[] vals = valStringToArray(sclass);
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
//            int totalPredictions = gal.size();
            int totalPredictions = 0;
            for (int i = 0; i < pred_res.size(); i++) {
                for (Integer value : pred_res.get(i)) {
                    totalPredictions++;
                    if (gt_class[i].allTrue().contains(new int[]{value})) {
                        correctPredictions++;
                    }
                }
            }
            System.out.println(totalPredictions);
            System.out.println(correctPredictions);
            double accuracy = (double) correctPredictions / totalPredictions;
            System.out.println("Accuracy: " + accuracy);

            String path_lab = null;
            if (loc_h)
                path_lab = base_path + datasetName + "/pred_labels/pred_labels_" + modelName + "_" + datasetName + "_loc_" + index + ".txt";
            else if (count_h)
                path_lab = base_path + datasetName + "/pred_labels/pred_labels_" + modelName + "_" + datasetName + "_count_" + index + ".txt";
            else if (node_const)
                path_lab = base_path + datasetName + "/pred_labels/pred_labels_" + modelName + "_" + datasetName + "_nodeconst_" + homType + "_" + expName + "_" + index + ".txt";
            else
                path_lab = base_path + datasetName + "/pred_labels/pred_labels_" + modelName + "_" + datasetName + "_" + index + ".txt";

            try (FileWriter writer = new FileWriter(path_lab)) {
                for (int i = 0; i < gt_class.length; i++) {
                    for (int[] el : gt_class[i].allTrue()) {
                        int val = el[0];
                        // find the value in the prediction
                        for (ArrayList<Integer> pred_class : pred_res) {
                            for (Integer node : pred_class) {
                                if (val == node) {
                                    // node,predicted,label
                                    writer.write(val + "," + pred_res.indexOf(pred_class) + "," + i + "\n");
                                }
                            }
                        }
                    }
                }
                writer.write("\n");
                writer.write("Acc: " + Double.toString(accuracy));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        primula = null;
        System.exit(0);
    }
}
