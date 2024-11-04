package Experiments.Homophily;

import Experiments.Misc.ValueObserver;
import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.GradientGraph;
import RBNgui.Bavaria;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNpackage.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class homophily_ex_rbn {
    static String primulahome = System.getenv("PRIMULAHOME");
    static String rbninputfilestring = "/Users/lz50rg/Dev/homophily/gnn_trained_model_log_2.rbn";
    static String rdefinputfilestring = "/Users/lz50rg/Dev/homophily/graph.rdef";
    public static void openBavaria(boolean open, Primula primula, File srsfile) {
        if (open) {
            SparseRelStruc temp = (SparseRelStruc) primula.getRels();
            if (temp.getCoords().size() == 0)
                temp.createCoords();
            new Bavaria(temp, srsfile, primula, false);
        }
    }

    public static void main(String[] args) {
        Primula primula = new Primula();

        File srsfile = new File(rdefinputfilestring);
        File rbnfile = new File(rbninputfilestring);

        primula.loadRBNFunction(rbnfile);
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
        try {
            GroundAtomList gal = new GroundAtomList();

            List<Integer> numberList = Arrays.asList(0,42);

            for (BoolRel brel: queryList) {
                int[][] mat = input_struct.allTypedTuples(brel.getTypes());
                for (int[] ints : mat) {
                    if (!numberList.contains(ints[0]))
                        gal.add(brel, ints);
                }
            }

            InferenceModule im = primula.openInferenceModule(false);

            im.setQueryAtoms(gal);

            primula.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
            primula.setScriptPath("/Users/lz50rg/Dev/primula-workspace/primula3/python/");
            primula.setScriptName("inference_test");

            im.setNumRestarts(100);

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
            if (GG != null){
                OneStrucData result = new OneStrucData();
                result.setParentRelStruc(primula.getRels());

                for (int i=0; i<gal.size(); i++) {
                    result.add(new GroundAtom(gal.atomAt(i).rel(), gal.atomAt(i).args), mapValues[i],"?");
                }

                primula.getInstantiation().add(result);
                im.updateInstantiationList();
                primula.updateBavaria();
            }

            openBavaria(true, primula, srsfile);

            OneStrucData onsd = new OneStrucData(primula.getRels().getmydata().copy());
            SparseRelStruc sampledRel = new SparseRelStruc(primula.getRels().getNames(), onsd, primula.getRels().getCoords(), primula.getRels().signature());
            sampledRel.getmydata().add(primula.getInstantiation().copy());

        } catch (RBNIllegalArgumentException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
