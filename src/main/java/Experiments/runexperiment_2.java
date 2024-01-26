package Experiments;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNCyclicException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNInconsistentEvidenceException;
import RBNLearning.Profiler;
import RBNgui.Bavaria;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNinference.BayesConstructor;
import RBNinference.PFNetwork;
import RBNinference.SampleProbs;
import RBNinference.SampleThread;
import RBNpackage.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

public class runexperiment_2 {

    static String primulahome = System.getenv("PRIMULAHOME");

//    static String rbninputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/rbn.rbn";
//    static String rdefinputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/test2.rdef";

//    static String rbninputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/purple.rbn";
//    static String rdefinputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/purple_data.rdef";
////
//    static String rbninputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/RBN_acr_graph_alpha1_10_5.rbn";
    static String rbninputfilestring = "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/models/only_blue_alpha1_10_5_20240123-151213/RBN_acr_graph_alpha1_10_5.rbn";
    static String rdefinputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/alpha1-blue.rdef";

    public static void main(String[] args) {
        File input_rbn = new File(rbninputfilestring);

        Primula primula = new Primula();
//        primula.loadRBNFunction(input_rbn);

        File srsfile = new File(rdefinputfilestring);
        primula.loadSparseRelFile(srsfile);

        RBNPreldef blue_pred = new RBNPreldef(new BoolRel("blue", 1), new String[]{"v"},  new ProbFormConstant(0.5));
        RBNPreldef edge_pred = new RBNPreldef(new BoolRel("edge", 2), new String[]{"v", "w"},  new ProbFormConstant(0.5));
        RBNPreldef gnn_pred = new RBNPreldef(
                new BoolRel("alpha1", 1),
                new String[]{"v"},
                new ProbFormGnn("v",
                        new ProbForm[]{
                                new ProbFormAtom(blue_pred.rel()),
                                new ProbFormAtom(edge_pred.rel())}
                )
        );

        RBN manual_rbn = new RBN(3, 0);
        manual_rbn.insertPRel(blue_pred, 0);
        manual_rbn.insertPRel(edge_pred, 2);
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
        queryrel.setInout(1);
        InferenceModule im = primula.openInferenceModule(false);
        GroundAtomList ga = new GroundAtomList();
        ga.add(queryrel, new int[] {0});
        ga.add(queryrel, new int[] {1});
        ga.add(queryrel, new int[] {2});
        ga.add(queryrel, new int[] {3});
        ga.add(queryrel, new int[] {4});
        im.setQueryAtoms(ga);

        System.out.println(primula.evidencemode());

        im.startSampleThread();

        long currtime = System.currentTimeMillis();
        long nexttime = currtime + 5000;
        System.out.println("Start sampling...");
        while(currtime < nexttime) {
            currtime = System.currentTimeMillis();
        }
        im.stopSampleThread();
        System.out.println("Stop sampling...");

        double[] probs = im.getSampthr().getSprobs().getProbs();

//        currtime = System.currentTimeMillis();
//        nexttime = currtime + 1000;
//        while(currtime < nexttime) {
//            currtime = System.currentTimeMillis();
//        }

        for (int i = 0; i < probs.length; i++) {
            System.out.println(i + ": " + probs[i]);
        }
        System.out.println(im.getSampthr().getSprobs().getSize());
    }

}
