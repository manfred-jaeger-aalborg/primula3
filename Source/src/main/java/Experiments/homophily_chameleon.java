package Experiments;

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

public class homophily_chameleon {
    static String primulahome = System.getenv("PRIMULAHOME");
    static public RBN createRBN(Primula primula, boolean rbn) {
        int num_attr = 2325;
        Rel[] attrs_rels = new Rel[num_attr];
        for (int i = 0; i < num_attr; i++) {
            attrs_rels[i] = new BoolRel("attr_" + i, 1);
        }

        int num_classes = 1;
        RBNPreldef[] gnn_rbn = new RBNPreldef[num_classes];
        for (int i = 0; i < num_classes; i++) {
            gnn_rbn[i] = new  RBNPreldef(
                    new BoolRel("class_"+i, 1),
                    new String[]{"v"},
                    new ProbFormGnn("v",
                            "GCNcha",
                            attrs_rels,
                            "edge",
                            "AB",
                            "node",
                            true,
                            1
                    )
            );
        }

        if (rbn) {
            File input_file = new File("/Users/lz50rg/Dev/homophily/experiments_wiki/const_cham.rbn");
            RBN file_rbn = new RBN(input_file);

            RBNPreldef[] preledef = file_rbn.prelements();
            RBN manual_rbn = new RBN(2, 0);

            for (int i = 0; i < num_classes; i++) {
                manual_rbn.insertPRel(gnn_rbn[i], i);
            }
            manual_rbn.insertPRel(preledef[0], 1);

            primula.setRbn(manual_rbn);
            primula.getInstantiation().init(manual_rbn);
            return manual_rbn;
        } else {
            RBN manual_rbn = new RBN(1, 0);

            for (int i = 0; i < num_classes; i++) {
                manual_rbn.insertPRel(gnn_rbn[i], i);
            }

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

    // Helper method to count intersection of two sets
    private static int countIntersection(TreeSet<Integer> set1, TreeSet<Integer> set2) {
        TreeSet<Integer> intersection = new TreeSet<>(set1);
        intersection.retainAll(set2);
        return intersection.size();
    }

    public static double computeAccuracy(TreeSet<Integer>[] all_true_pred, TreeSet<Integer>[] all_false_pred, TreeSet<Integer>[] all_true_gt, TreeSet<Integer>[] all_false_gt, TreeSet<Integer> test_nodes) {
        int total_TP = 0;
        int total_FP = 0;
        int total_TN = 0;
        int total_FN = 0;

        // Calculate TP, FP, TN, FN for each class
        for (int i = 0; i < all_true_pred.length; i++) {
            TreeSet<Integer> truePred = all_true_pred[i];
            TreeSet<Integer> falsePred = all_false_pred[i];
            TreeSet<Integer> trueGT = all_true_gt[i];
            TreeSet<Integer> falseGT = all_false_gt[i];

            // Filter sets based on test_nodes
            truePred.retainAll(test_nodes);
            falsePred.retainAll(test_nodes);
            trueGT.retainAll(test_nodes);
            falseGT.retainAll(test_nodes);

            int TP = countIntersection(truePred, trueGT);
            int FP = countIntersection(truePred, falseGT);
            int TN = countIntersection(falsePred, falseGT);
            int FN = countIntersection(falsePred, trueGT);

            total_TP += TP;
            total_FP += FP;
            total_TN += TN;
            total_FN += FN;
        }

        return (double)(total_TP + total_TN) / (total_TP + total_TN + total_FP + total_FN);
    }

    public static void main(String[] args) {
        Primula primula = new Primula();

        RBN rbn = createRBN(primula, true);
        primula.setRbn(rbn);
        primula.getInstantiation().init(rbn);
//        primula.loadRBNFunction(new File("/Users/lz50rg/Dev/homophily/gnn_trained_model_log.rbn"));

        File srsfile = new File("/Users/lz50rg/Dev/homophily/experiments_wiki/chameleon_small.rdef");
        primula.loadSparseRelFile(srsfile);

//        openBavaria(true, primula, srsfile);

        ArrayList<BoolRel> queryList = new ArrayList<>();
        String[] queryName = new String[1];
        int num_classes = 1;
        for (int i = 0; i < num_classes; i++) {
            queryName[i] = "class_"+i;
        }

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

            OneBoolRelData query_nodes = prob_data.inputDomain().getData().findInBoolRel("query_nodes");
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
            primula.setScriptPath("/Users/lz50rg/Dev/primula-workspace/primula3/Source/python/");
            primula.setScriptName("inference_test");

            im.setNumRestarts(10);

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

//            openBavaria(true, primula, srsfile);

            OneStrucData onsd = new OneStrucData(primula.getRels().getmydata().copy());
            SparseRelStruc sampledRel = new SparseRelStruc(primula.getRels().getNames(), onsd, primula.getRels().getCoords(), primula.getRels().signature());
            sampledRel.getmydata().add(primula.getInstantiation().copy());

//            PyTorchExport pye = new PyTorchExport(sampledRel, rbn);
//            pye.writePythonDataOnFile("/Users/lz50rg/Dev/primula-workspace/test_rbn_files/python_data.txt");

            // compute final accuracy for all the nodes
            OneBoolRelData[] all_pred_class = new OneBoolRelData[num_classes];
            OneBoolRelData[] gt_class = new OneBoolRelData[num_classes];
            TreeSet<Integer>[] all_true_gt = new TreeSet[num_classes];
            TreeSet<Integer>[] all_false_gt = new TreeSet[num_classes];
            TreeSet<Integer>[] all_true_pred_class = new TreeSet[num_classes];
            TreeSet<Integer>[] all_false_pred_class = new TreeSet[num_classes];
            for (int i = 0; i < num_classes; i++) {
                all_pred_class[i] = sampledRel.getData().findInBoolRel("class_"+i);
                gt_class[i] = sampledRel.getData().findInBoolRel("ground_class_"+i);
                all_true_pred_class[i] = convertToInArray(all_pred_class[i].allTrue());
                all_false_pred_class[i] = convertToInArray(all_pred_class[i].allFalse());
                all_true_gt[i] = convertToInArray(gt_class[i].allTrue());

                all_false_gt[i] = new TreeSet<Integer>();
                for (int j = 0; j < num_nodes; j++) {
                    if (!all_true_gt[i].contains(j))
                        all_false_gt[i].add(j);
                }
            }
            OneBoolRelData test_nodes_r = sampledRel.getData().findInBoolRel("test_nodes");
            TreeSet<Integer> test_nodes =  convertToInArray(test_nodes_r.allTrue());

            double accuracy = computeAccuracy(all_true_pred_class, all_false_pred_class, all_true_gt, all_false_gt, test_nodes);
            System.out.println("Test accuracy: " + accuracy);

//            primula.exitProgram();

        } catch (RBNIllegalArgumentException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
