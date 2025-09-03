package Experiments.Learn;

import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.GradientGraph;
import RBNLearning.RelData;
import RBNgui.InferenceModule;
import RBNgui.LearnModule;
import RBNgui.Primula;
import RBNpackage.*;
import org.dom4j.swing.LeafTreeNode;

import java.io.File;

public class LearnTest {

    static public void main(String[] args) {

        Primula primula = new Primula();
        primula.loadSparseRelFile(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/Community/zachary.rdef"));
        primula.loadRBNFunction(new File("/Users/lz50rg/Dev/primula-workspace/primula3/Examples/Community/community_softclus_2c.rbn"));

        LearnModule lm = primula.createLearnModule();
        InferenceModule im = primula.createInferenceModule();

        im.setNumRestarts(1);

        lm.setRestarts(1);
        lm.setThreadStrategy(LearnModule.AscentBatch);
        lm.setGGStrategy(LearnModule.AscentLBFGS);

        String[][] paramnumrels = new String[][]{{"alpha", "c1", "c2"}};

        primula.setParamNumRels(paramnumrels);

        RelStruc input_struct = primula.getRels();

        BoolRel tmp_query = new BoolRel("link", 2);
        tmp_query.setInout(1);

        int[][] mat;
        GroundAtomList gal = new GroundAtomList();

        try {
            mat = input_struct.allTypedTuples(tmp_query.getTypes());
        } catch (RBNIllegalArgumentException e) {
            throw new RuntimeException(e);
        }

        for (int[] ints : mat)
            gal.add(tmp_query, ints);
        im.addQueryAtoms(tmp_query, gal);

        GradientGraph GG = im.startMapThread();
        try {
            im.getMapthr().join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        im.getMapthr().getParameterTable().printParameterTable();
    }

}
