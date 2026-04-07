package Experiments.Homophily;

import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.GradientGraph;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNpackage.*;
import RBNutilities.rbnutilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class NodeClassification {

    private static final String BASE_PATH =
            "/nfs/home/cs.aau.dk/lz50rg/dev/homophily/map-exp/all_datastes/";

    private static final String[] DATASET_NAMES = {
            "Wisconsin", "Texas", "Cornell", "Cora", "CiteSeer", "PubMed", "chameleon", "squirrel", "Actor"
    };
    private static final int[] DATASET_CLASSES = {
            5, 5, 5, 7, 6, 3, 5, 5, 5
    };
    private static final int SPLITS = 10;

    private static String[] buildClassNames(int numClasses) {
        String[] names = new String[numClasses];
        for (int i = 0; i < numClasses; i++) {
            names[i] = String.valueOf((char) ('A' + i));
        }
        return names;
    }

    private static Type[] typeStringToArray(String ts, int arity) {
        Type[] result = new Type[arity];
        for (int i = 0; i < arity; i++) {
            int nextcomma = ts.indexOf(",");
            String nexttype;
            if (nextcomma != -1) {
                nexttype = ts.substring(0, nextcomma);
                ts = ts.substring(nextcomma + 1);
            } else {
                nexttype = ts;
                ts = "";
            }
            result[i] = nexttype.equals("Domain") ? new TypeDomain() : new TypeRel(nexttype);
        }
        return result;
    }

    private static String[] valStringToArray(String vs) {
        return rbnutilities.stringToArray(vs, ",");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: RunOneJob <taskId>");
            System.exit(2);
        }

        int taskId = Integer.parseInt(args[0]);

        int datasetIdx = taskId / SPLITS;
        int index = taskId % SPLITS;
        String datasetName = DATASET_NAMES[datasetIdx];
        int numClasses = DATASET_CLASSES[datasetIdx];

        String[] class_names = buildClassNames(numClasses);
        String classValString = String.join(",", class_names);

        System.out.println("Task " + taskId + ": dataset=" + datasetName
                + ", split=" + index + ", classes=" + numClasses);

        Primula primula = new Primula();
        primula.loadSparseRelFile(new File(
                BASE_PATH + "rdef/" + datasetName + "_rdef_homProp_" + index + ".rdef"));
        primula.loadRBNFunction(new File(
                BASE_PATH + "rbn/" + datasetName + "_" + index + ".rbn"));

        CatRel tmp_query = new CatRel("CAT", 1,
                typeStringToArray("node", 1), valStringToArray(classValString));
        tmp_query.setInout(Rel.PROBABILISTIC);

        RelStruc input_struct = primula.getRels();
        Vector<GroundAtomList> gal_vec = new Vector<>();

        try {
            InferenceModule im = primula.createInferenceModule();

            OneBoolRelData query_nodes = primula.getRels().getData().findInBoolRel("test_nodes");
            Set<Integer> instantiated_nodes = new HashSet<>();
            for (int[] node : query_nodes.allTrue()) {
                instantiated_nodes.add(node[0]);
            }

            int[][] mat = input_struct.allTypedTuples(tmp_query.getTypes());
            gal_vec.add(new GroundAtomList());
            for (int[] ints : mat) {
                if (instantiated_nodes.contains(ints[0])) {
                    gal_vec.get(0).add(tmp_query, ints);
                }
            }

            System.out.println("Number of query nodes: " + gal_vec.get(0).size());

            im.addQueryAtoms(tmp_query, gal_vec.get(0));
            im.setNumRestarts(5);
            GradientGraph GG = im.startMapThread();
            im.getMapthr().join();

            HashMap<Rel, int[]> bestMapVals = im.getMapthr().getBestMapVals();
            int[] res = bestMapVals.get(tmp_query);

            ArrayList<ArrayList<Integer>> pred_res = new ArrayList<>();
            for (int i = 0; i < numClasses; i++) pred_res.add(new ArrayList<>());

            System.out.println("\nMAP INFERENCE RESULTS:\n");
            for (GroundAtomList gal : gal_vec) {
                for (int i = 0; i < gal.size(); i++) {
                    int nodeId = gal.atomAt(i).args()[0];
                    int classIdx = res[i];
                    System.out.println("Node " + nodeId + " -> Class " + class_names[classIdx]);
                    pred_res.get(classIdx).add(nodeId);
                }
            }

            System.out.println("\nFinal GG logLikelihood: " + GG.currentLogLikelihood());

            // Ground truth
            OneStrucData onsd = new OneStrucData(primula.getRels().getmydata().copy());
            SparseRelStruc sampledRel = new SparseRelStruc(primula.getRels().getNames(), onsd,
                    primula.getRels().getCoords(), primula.getRels().signature());
            sampledRel.getmydata().add(primula.getInstantiation().copy());

            OneBoolRelData[] gt_class = new OneBoolRelData[numClasses];
            for (int i = 0; i < numClasses; i++) {
                gt_class[i] = sampledRel.getData().findInBoolRel("ground_" + class_names[i]);
            }

            int correctPredictions = 0, totalPredictions = 0;
            for (int i = 0; i < pred_res.size(); i++) {
                for (Integer nodeVal : pred_res.get(i)) {
                    totalPredictions++;
                    if (gt_class[i].allTrue().contains(new int[]{nodeVal})) correctPredictions++;
                }
            }

            double accuracy = totalPredictions > 0
                    ? (double) correctPredictions / totalPredictions : 0.0;
            System.out.println("Correct: " + correctPredictions
                    + "  Total: " + totalPredictions
                    + "  Accuracy: " + String.format("%.4f", accuracy));

            // Write results
            String path_lab = BASE_PATH + "prim-res/" + datasetName + "_" + index + ".txt";
            try (FileWriter writer = new FileWriter(path_lab)) {
                writer.write("node,predicted_class,ground_class\n");
                for (int i = 0; i < numClasses; i++) {
                    if (gt_class[i] == null) continue;
                    for (int[] gtNode : gt_class[i].allTrue()) {
                        int nodeVal = gtNode[0];
                        int predictedClass = -1;
                        for (int j = 0; j < pred_res.size(); j++) {
                            if (pred_res.get(j).contains(nodeVal)) {
                                predictedClass = j;
                                break;
                            }
                        }
                        writer.write(primula.getRels().namesAtAsArray(new int[]{nodeVal})[0]
                                + "," + predictedClass + "," + i + "\n");
                    }
                }
                writer.write("\nSummary\n");
                writer.write("Total Predictions: " + totalPredictions + "\n");
                writer.write("Correct Predictions: " + correctPredictions + "\n");
                writer.write("Accuracy: " + String.format("%.4f", accuracy) + "\n");
                writer.write("Log-Likelihood: " + GG.currentLogLikelihood() + "\n");
            }
            System.out.println("Results saved to: " + path_lab);

        } catch (InterruptedException e) {
            System.err.println("Inference interrupted: " + e.getMessage());
            e.printStackTrace();
        } catch (RBNIllegalArgumentException e) {
            System.err.println("RBN error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }

        System.exit(0);
    }
}