package Experiments;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNCyclicException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNInconsistentEvidenceException;
import RBNLearning.RelData;
import RBNgui.InferenceModule;
import RBNgui.LearnModule;
import RBNgui.Primula;
import RBNinference.BayesConstructor;
import RBNinference.PFNetwork;
import RBNinference.SampleThread;
import RBNpackage.*;
import RBNutilities.SmallDouble;

import java.io.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

public class runexperiment {

    static String primulahome = System.getenv("PRIMULAHOME");

    static String rbninputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/rbn.rbn";

    static String rdefinputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/test2.rdef";

    public static void main(String[] args) {

        // ProbFormGnn gnn_prob = new ProbFormGnn();

        // gnn_prob.evaluate(rel, )


        File input_rbn = new File(rbninputfilestring);
        RBN rbn = new RBN(input_rbn);

        Primula primula = new Primula();
        // primula.loadRBNFunction(input_rbn);

        File srsfile = new File(rdefinputfilestring);
        primula.loadSparseRelFile(srsfile);

        RelStruc rel = primula.getRels(); // A, get the structure of the rdef

        // create the RBN "manually", it is composed by two prob relations: red and blue
        // blue is a combination formula
        /**
         * red(v) = 0.5;
         *
         * blue(v) = COMBINE red(w)
         * 		     WITH l-reg
         * 		     FORALL w
         * 		     WHERE edge(v,w);
         */
        RBN manual_rbn = new RBN(3, 0);

        RBNPreldef gnn_pred = new RBNPreldef(new BoolRel("gnn", 1), new String[]{"v"}, new ProbFormGnn("v", "1", new Rel[]{}, "edge", "ABBA", false));

        RBNPreldef red_pred = new RBNPreldef(new BoolRel("red", 1), new String[]{"v"}, new ProbFormConstant(0.5));

        ProbFormCombFunc blue_comb = new ProbFormCombFunc(
                new CombFuncLReg(),
                new ProbForm[]{
                        new ProbFormAtom(
                                new Rel("red", 1),
                                new String[]{"w"})
                },
                new String[]{"w"},
                new ProbFormBoolAtom(
                        new ProbFormAtom(
                                new Rel("edge", 2),
                                new String[]{"v", "w"}),
                        true)
        );
        RBNPreldef blue_pred = new RBNPreldef(new BoolRel("blue", 1), new String[]{"v"}, blue_comb);

        manual_rbn.insertPRel(red_pred, 0);
        manual_rbn.insertPRel(blue_pred, 1);
        manual_rbn.insertPRel(gnn_pred, 2);

        // load the manually created RBN in the primula object without using a file
        primula.setRbn(manual_rbn);
        primula.getInstantiation().init(manual_rbn);

        Rel[] rbnrels = manual_rbn.Rels();
        BoolRel redrel = (BoolRel) rbnrels[0];
        BoolRel bluerel = (BoolRel) rbnrels[1];
        BoolRel gnnrel = (BoolRel) rbnrels[2];
        OneStrucData inst = primula.getInstantiation();
        GroundAtomList queryatoms = new GroundAtomList();

        for (int i = 0; i < 1; i++) {
            queryatoms.add(gnnrel, new int[]{i});
        }

//        for (int i = 0; i < 1; i++) {
//            queryatoms.add(redrel, new int[]{i});
//        }
//
//        for (int i = 0; i < 1; i++) {
//            queryatoms.add(bluerel, new int[]{i});
//        }


        // COMPUTE MCMC
        /**
         * 1 find rels what exacltly is
         * 2 inst
         * 3 queryatoms
         *  plug in bayesconstroctor
         */
        BayesConstructor constructor = new BayesConstructor(manual_rbn, rel, inst, queryatoms, primula);
        /**
         * samopleordermode = 0
         *      OPTION_SAMPLEORD_FORWARD = 0;
         *      OPTION_SAMPLEORD_RIPPLE = 1;
         *
         * adaptivemode = 0
         *      OPTION_NOT_SAMPLE_ADAPTIVE = 0;
         *      OPTION_SAMPLE_ADAPTIVE = 1;
         *
         * samplelogmode = [false, false, false, false]
         * cptparents = 3 // Max. number of parents for nodes with standard cpt
         * queryatoms = {GroundAtomList} =>
         * num_subsamples_minmax = 10 ??
         * num_subsamples_adapt = 10 ??
         * logwriter = null
         */
        int sampleordmode = 0;
        int adaptivemode = 0;
        boolean[] samplelogmode = new boolean[]{false, false, false, false};
        int cptparents = 0;
        int num_subsamples_minmax = 10;
        int num_subsamples_adapt = 10;
        BufferedWriter logwriter = null;

        try {
            PFNetwork pfn = constructor.constructPFNetwork(primula.evidencemode(),
                    Primula.OPTION_QUERY_SPECIFIC,
                    Primula.OPTION_ELIMINATE_ISOLATED_ZERO_NODES);

            pfn.prepareForSampling(sampleordmode,
                    adaptivemode,
                    samplelogmode,
                    cptparents,
                    queryatoms,
                    num_subsamples_minmax,
                    num_subsamples_adapt,
                    logwriter);

            InferenceModule im = primula.openInferenceModule(false);
            SampleThread sampthr = new SampleThread(im, pfn, queryatoms, num_subsamples_minmax, samplelogmode, logwriter);
            sampthr.start();
            long currtime = System.currentTimeMillis();
            long nexttime = currtime + 3000;
            while(currtime < nexttime) {
                currtime = System.currentTimeMillis();
            }
            sampthr.setRunning(false);
            double[] probs = sampthr.getSprobs().getProbs();
            for (int i = 0; i < probs.length; i++) {
                System.out.println(i + ": " + probs[i]);
            }

        } catch (RBNCompatibilityException | RBNCyclicException | RBNIllegalArgumentException | RBNInconsistentEvidenceException | IOException e) {
            throw new RuntimeException(e);
        }


    }

}
