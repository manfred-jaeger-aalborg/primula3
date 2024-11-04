package Experiments.Homophily;

import Experiments.Misc.ValueObserver;
import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.GradientGraph;
import RBNLearning.RelDataForOneInput;
import RBNgui.Bavaria;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNpackage.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public class homophily_GCN {
    static String primulahome = System.getenv("PRIMULAHOME");
    static public RBN createRBN(Primula primula, boolean rbn) {
        int num_attr = 2;
        Rel[] attrs_rels = new Rel[num_attr];
        for (int i = 0; i < num_attr; i++) {
            attrs_rels[i] = new NumRel("attr"+i, 1);
        }

        RBNPreldef gnn_pos = new RBNPreldef(
                new BoolRel("pos", 1),
                new String[]{"v"},
                new ProbFormGnn("v",
                        "GCNHomophily",
                        attrs_rels,
                        "edge",
                        "AB", // in this case the adjacency matrix in the dataset is symmetric !
                        "node",
                        true,
                        0
                )
        );

        if (rbn) {
            File input_file = new File("/Users/lz50rg/Dev/homophily/experiments/graphs/const/const_1.0.rbn");
            RBN file_rbn = new RBN(input_file);

            RBNPreldef[] preledef = file_rbn.prelements();
            RBN manual_rbn = new RBN(2, 0);

            manual_rbn.insertPRel(gnn_pos, 0);
            manual_rbn.insertPRel(preledef[0], 1);

            primula.setRbn(manual_rbn);
            primula.getInstantiation().init(manual_rbn);
            return manual_rbn;
        } else {
            RBN manual_rbn = new RBN(1, 0);

            manual_rbn.insertPRel(gnn_pos, 0);

            primula.setRbn(manual_rbn);
            primula.getInstantiation().init(manual_rbn);
            return manual_rbn;
        }
    }

    public static void openBavaria(boolean open, Primula primula, File srsfile) {
        if (open) {
            SparseRelStruc temp = (SparseRelStruc) primula.getRels();
            if (temp.getCoords().size() == 0)
                temp.createCoords();
            new Bavaria(temp, srsfile, primula, false);
        }
    }

    public static TreeSet<Integer> convertToInArray(TreeSet<int[]> treeSet) {
        TreeSet<Integer> int_set = new TreeSet<>();
        for (int[] nodeArray : treeSet) {
            int_set.add(nodeArray[0]);
        }
        return int_set;
    }

    public static double computeAccuracy(TreeSet<Integer> true_pred,
                                         TreeSet<Integer> false_pred,
                                         TreeSet<Integer> true_gt,
                                         TreeSet<Integer> false_gt){
        int truePositives = 0;
        int trueNegatives = 0;

        for (int entry : true_gt) {
            if (true_pred.contains(entry)) {
                truePositives++;
            }
        }

        for (int entry : false_gt) {
            if (false_pred.contains(entry)) {
                trueNegatives++;
            }
        }
        // total is still based on the predicted size
        int totalExamples = true_gt.size() + false_gt.size();
//        System.out.println(truePositives);
//        System.out.println(trueNegatives);
//        System.out.println(totalExamples);
        return (double)(truePositives + trueNegatives) / totalExamples;
    }

    public static void main(String[] args) {
        Primula primula = new Primula();

        RBN rbn = createRBN(primula, true);
        primula.setRbn(rbn);
        primula.getInstantiation().init(rbn);
//        primula.loadRBNFunction(new File("/Users/lz50rg/Dev/homophily/gnn_trained_model_log.rbn"));

        File srsfile = new File("/Users/lz50rg/Dev/homophily/experiments/graphs/lam_1.0_g.rdef");
        primula.loadSparseRelFile(srsfile);

//        openBavaria(true, primula, srsfile);

        ArrayList<BoolRel> queryList = new ArrayList<>();
        String[] queryName = new String[]{"pos"};

        for (String s : queryName) {
            BoolRel tmp_query = new BoolRel(s, 1);
            tmp_query.setInout(1);
            queryList.add(tmp_query);
        }

        RelStruc input_struct = primula.getRels();
        RelDataForOneInput prob_data = primula.getReldata().elementAt(0);

        int num_nodes = input_struct.domSize();
        try {
            GroundAtomList gal = new GroundAtomList();

            // include only the test nodes (not validation)

            OneBoolRelData query_nodes = prob_data.inputDomain().getData().findInBoolRel("all_unknown_nodes");
            TreeSet<int[]> true_data = query_nodes.allTrue();
            List<Integer> instantiated_nodes = new ArrayList<>();
            for (int[] node: true_data) {
                instantiated_nodes.add(node[0]);
            }

            for (BoolRel brel: queryList) {
                int[][] mat = input_struct.allTypedTuples(brel.getTypes());
                for (int[] ints : mat) {
                    if (instantiated_nodes.contains(ints[0]))
                        gal.add(brel, ints);
                }
            }

            // ****************************************************

            InferenceModule im = primula.openInferenceModule(false);

            im.setQueryAtoms(gal);

            primula.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
            primula.setScriptPath("/Users/lz50rg/Dev/primula-workspace/primula3/python/");
            primula.setScriptName("inference_test");

            im.setNumRestarts(20);

            ValueObserver valueObserver = new ValueObserver();
            im.setValueObserver(valueObserver);

            GradientGraph GG = im.startMapThread();
            im.getMapthr().join();

            int[] mapValues = valueObserver.getMapVals();
            String mapLikelihood = valueObserver.getLikelihood();
            System.out.println("\n---------------------------------------");
            System.out.println("Query atoms results:");
            for (int i=0; i<gal.size(); i++) {
                System.out.println(gal.atomAt(i).rel + Arrays.toString(gal.atomAt(i).args) + ": " + mapValues[i]);
            }
            System.out.println("\nLikelihood: " + mapLikelihood);
            System.out.println("---------------------------------------\n");

            // assign the map values to the current data
            OneStrucData result = new OneStrucData();
            if (GG != null){
                result.setParentRelStruc(primula.getRels());

                for (int i=0; i<gal.size(); i++) {
                    result.add(new GroundAtom(gal.atomAt(i).rel(), gal.atomAt(i).args), mapValues[i],"?");
                }

                primula.getInstantiation().add(result);
                im.updateInstantiationList();
                primula.updateBavaria();
            }

            openBavaria(false, primula, srsfile);

            OneStrucData onsd = new OneStrucData(primula.getRels().getmydata().copy());
            SparseRelStruc sampledRel = new SparseRelStruc(primula.getRels().getNames(), onsd, primula.getRels().getCoords(), primula.getRels().signature());
            sampledRel.getmydata().add(primula.getInstantiation().copy());

//            PyTorchExport pye = new PyTorchExport(sampledRel, rbn);
//            pye.writePythonDataOnFile("/Users/lz50rg/Dev/primula-workspace/test_rbn_files/python_data.txt");

            // compute final accuracy for all the nodes
            OneBoolRelData all_pred_pos = sampledRel.getData().findInBoolRel("pos");
            OneBoolRelData gt_pos = sampledRel.getData().findInBoolRel("ground_pos");
            OneBoolRelData test_nodes = sampledRel.getData().findInBoolRel("test_nodes");

            TreeSet<Integer> int_true_gt = convertToInArray(gt_pos.allTrue());
            TreeSet<Integer> int_false_gt = new TreeSet<>();
            for (int i = 0; i < num_nodes; i++) {
                int_false_gt.add(i);
            }
            int_false_gt.removeAll(int_true_gt);

            TreeSet<Integer> int_true_tn = convertToInArray(test_nodes.allTrue());
            TreeSet<Integer> int_false_tn = convertToInArray(test_nodes.allFalse());


            if ((all_pred_pos.numtrue() + all_pred_pos.numfalse()) != num_nodes) {
                System.out.println("NOT ALL MAP NODES INSTANTIATED!");
            }

            // compute final accuracy for the map nodes
            OneBoolRelData pred_pos = result.findInBoolRel("pos");
            TreeSet<Integer> true_pred = convertToInArray(pred_pos.allTrue());
            TreeSet<Integer> false_pred = convertToInArray(pred_pos.allFalse());

            double accuracy = computeAccuracy(true_pred, false_pred, int_true_tn, int_false_tn);
            System.out.println("Test node accuracy: " + accuracy);

//            primula.exitProgram();

        } catch (RBNIllegalArgumentException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
