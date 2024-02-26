package Experiments;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNCyclicException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNInconsistentEvidenceException;
import RBNLearning.*;
import RBNgui.Bavaria;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNgui.QueryTableModel;
import RBNinference.*;
import RBNpackage.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

public class runexperiment_map {

    static String primulahome = System.getenv("PRIMULAHOME");

//    static String rbninputfilestring = "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/models/only_blue_alpha1_10_5_20240123-151213/RBN_acr_graph_alpha1_10_5.rbn";
    static String rbninputfilestring = "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/models/alpha1_64_64_64_20240126-144611/RBN_acr_graph_alpha1_64_64_64.rbn";

//    static String rdefinputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/alpha1-blue.rdef";
    static String rdefinputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/alpha1-blue.rdef";

    public static void main(String[] args) {
        File input_rbn = new File(rbninputfilestring);

        Primula primula = new Primula();
//        primula.loadRBNFunction(input_rbn);

        File srsfile = new File(rdefinputfilestring);
        primula.loadSparseRelFile(srsfile);

        RBNPreldef blue_pred = new RBNPreldef(new BoolRel("blue", 1), new String[]{"v"},  new ProbFormConstant(0.5));
//        RBNPreldef edge_pred = new RBNPreldef(new BoolRel("edge", 2), new String[]{"v", "w"},  new ProbFormConstant(0.5));
        RBNPreldef gnn_pred = new RBNPreldef(
                new BoolRel("alpha1", 1),
                new String[]{"v"},
                new ProbFormGnn("v",
                        new Rel[]{
                                blue_pred.rel()},
                        "edge",
                        "ABBA",
                        false
//                                edge_pred.rel()}
                )
        );

        RBN manual_rbn = new RBN(2, 0);
        manual_rbn.insertPRel(blue_pred, 0);
//        manual_rbn.insertPRel(edge_pred, 2);
        manual_rbn.insertPRel(gnn_pred, 1);

        // load the manually created RBN in the primula object without using a file
        primula.setRbn(manual_rbn);
        primula.getInstantiation().init(manual_rbn);

        if (false) {
            SparseRelStruc temp = (SparseRelStruc) primula.getRels();
            if (temp.getCoords().size() == 0)
                temp.createCoords();
            new Bavaria(temp, srsfile, primula, false);
        }

        BoolRel queryrel = new BoolRel("blue", 1);
//        BoolRel queryrel = new BoolRel("edge", 2);
        queryrel.setInout(1);
        RelStruc A = primula.getRels();
        try {
            GroundAtomList gal = new GroundAtomList();
            int[][] mat = A.allTypedTuples(queryrel.getTypes());
            for (int i = 0; i < mat.length; i++) {
//                if (i != 3)
                    gal.add(queryrel, mat[i]);
            }

            InferenceModule im = primula.openInferenceModule(false);

            im.setQueryAtoms(gal);

            primula.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
            primula.setModelPath("/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python/primula-gnn");
            primula.setScriptPath("/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python");
            primula.setScriptName("inference_test");

            im.setNumRestarts(10);

            ValueObserver valueObserver = new ValueObserver();
            im.setMapObserver(valueObserver);

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

            primula.exitProgram();

        } catch (RBNIllegalArgumentException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
