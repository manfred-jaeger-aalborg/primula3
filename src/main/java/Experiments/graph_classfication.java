package Experiments;

import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.GradientGraph;
import RBNgui.Bavaria;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNpackage.*;

import java.io.File;
import java.util.*;

public class graph_classfication {
    static String primulahome = System.getenv("PRIMULAHOME");
//    static String rbninputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/rbn_acr_graph_triangle_10_8_6_add.rbn";
    static String rbninputfilestring = "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/models/triangle_10_8_6_20230725-152135/exp_41/rbn_acr_graph_triangle_10_8_6_add.rbn";
    static String rdefinputfilestring = "/Users/lz50rg/Dev/primula-workspace/test_rbn_files/base_class_0_n6_0_const.rdef";

    static public RBN createRBN() {
        RBNPreldef A_pred = new RBNPreldef(new BoolRel("A", 1), new String[]{"v"},  new ProbFormConstant(0.5));
        RBNPreldef B_pred = new RBNPreldef(new BoolRel("B", 1), new String[]{"v"},  new ProbFormConstant(0.5));
        RBNPreldef C_pred = new RBNPreldef(new BoolRel("C", 1), new String[]{"v"},  new ProbFormConstant(0.5));
        RBNPreldef D_pred = new RBNPreldef(new BoolRel("D", 1), new String[]{"v"},  new ProbFormConstant(0.5));
        RBNPreldef E_pred = new RBNPreldef(new BoolRel("E", 1), new String[]{"v"},  new ProbFormConstant(0.5));
        RBNPreldef F_pred = new RBNPreldef(new BoolRel("F", 1), new String[]{"v"},  new ProbFormConstant(0.5));
        RBNPreldef G_pred = new RBNPreldef(new BoolRel("G", 1), new String[]{"v"},  new ProbFormConstant(0.5));

        RBNPreldef edge_pred = new RBNPreldef(new BoolRel("edge", 2), new String[]{"v", "w"},  new ProbFormConstant(0.5));

        RBNPreldef gnn_class_0 = new RBNPreldef(
                new BoolRel("CLASS_0", 0),
                new String[0],
                new ProbFormGnn("v",
                        new Rel[]{
                                A_pred.rel(),
                                B_pred.rel(),
                                C_pred.rel(),
                                D_pred.rel(),
                                E_pred.rel(),
                                F_pred.rel(),
                                G_pred.rel(),
                                edge_pred.rel()
                        },
                        "edge",
                        "ABBA",
                        true,
                        0
                )
        );

        RBNPreldef gnn_class_1 = new RBNPreldef(
                new BoolRel("CLASS_1", 0),
                new String[0],
                new ProbFormGnn("v",
                        new Rel[]{
                                A_pred.rel(),
                                B_pred.rel(),
                                C_pred.rel(),
                                D_pred.rel(),
                                E_pred.rel(),
                                F_pred.rel(),
                                G_pred.rel(),
                                edge_pred.rel()
                        },
                        // TODO add unique identifier to gnn
                        "edge",
                        "ABBA",
                        true,
                        1
                )
        );

        RBN manual_rbn = new RBN(32, 0);

        manual_rbn.insertPRel(A_pred, 0);
        manual_rbn.insertPRel(B_pred, 1);
        manual_rbn.insertPRel(C_pred, 2);
        manual_rbn.insertPRel(D_pred, 3);
        manual_rbn.insertPRel(E_pred, 4);
        manual_rbn.insertPRel(F_pred, 5);
        manual_rbn.insertPRel(G_pred, 6);
        manual_rbn.insertPRel(edge_pred, 7);
        manual_rbn.insertPRel(gnn_class_0, 8);
        manual_rbn.insertPRel(gnn_class_1, 9);

        int idx = 10;
        String[] alphabet = {"A", "B", "C", "D", "E", "F", "G"};
        RBNPreldef const_perm = null;
        for (int i = 0; i < alphabet.length - 1; i++) {
            for (int j = i + 1; j < alphabet.length; j++) {
//                System.out.println((i+1) + ". " + alphabet[i] + "_" + alphabet[j]);
                const_perm = new RBNPreldef(new BoolRel("const_" + alphabet[i] + "_" + alphabet[j], 1), new String[]{"v"},
                        new ProbFormCombFunc(
                                "prod",
                                new ProbForm[] {
                                        new ProbFormBoolComposite(new ProbFormBool[]{
                                                new ProbFormBoolAtom(new ProbFormAtom(new BoolRel(alphabet[i], 1), new String[]{"v"}), true),
                                                new ProbFormBoolAtom(new ProbFormAtom(new BoolRel(alphabet[j], 1), new String[]{"v"}), true)
                                        }, 0, true),
                                        new ProbFormConstant(0.99)},
                                new String[0],
                                new ProbFormBoolConstant(true))
                );
                manual_rbn.insertPRel(const_perm, idx);
                idx++;
            }
        }

        RBNPreldef all_const = new RBNPreldef(new BoolRel("all_const", 1),  new String[]{"v"},
                new ProbFormCombFunc(
                        "prod",
                        new ProbForm[] {
                                new ProbFormBoolComposite(new ProbFormBool[]{
                                        new ProbFormBoolAtom(new ProbFormAtom(new BoolRel("A", 1), new String[]{"v"}), true),
                                        new ProbFormBoolAtom(new ProbFormAtom(new BoolRel("B", 1), new String[]{"v"}), true),
                                        new ProbFormBoolAtom(new ProbFormAtom(new BoolRel("C", 1), new String[]{"v"}), true),
                                        new ProbFormBoolAtom(new ProbFormAtom(new BoolRel("D", 1), new String[]{"v"}), true),
                                        new ProbFormBoolAtom(new ProbFormAtom(new BoolRel("E", 1), new String[]{"v"}), true),
                                        new ProbFormBoolAtom(new ProbFormAtom(new BoolRel("F", 1), new String[]{"v"}), true),
                                        new ProbFormBoolAtom(new ProbFormAtom(new BoolRel("G", 1), new String[]{"v"}), true)
                                }, 1, true),
                                new ProbFormConstant(0.99)},
                        new String[0],
                        new ProbFormBoolConstant(true)));
        manual_rbn.insertPRel(all_const, idx);

        return manual_rbn;
    }

    public static void openBavaria(boolean open, Primula primula, File srsfile) {
        if (open) {
            SparseRelStruc temp = (SparseRelStruc) primula.getRels();
            if (temp.getCoords().size() == 0)
                temp.createCoords();
            new Bavaria(temp, srsfile, primula, false);
        }
    }

    public static void main(String[] args) {
        File input_rbn = new File(rbninputfilestring);
        Primula primula = new Primula();
//        primula.loadRBNFunction(input_rbn);

        File srsfile = new File(rdefinputfilestring);
        primula.loadSparseRelFile(srsfile);
        RBN rbn = createRBN();

        primula.setRbn(rbn);
        primula.getInstantiation().init(rbn);

//        openBavaria(false, primula, srsfile);

        ArrayList<BoolRel> queryList = new ArrayList<>();
        String[] queryName = new String[]{"A", "B", "C", "D", "E", "F", "G"};

        for (String s : queryName) {
            BoolRel tmp_query = new BoolRel(s, 1);
            tmp_query.setInout(1);
            queryList.add(tmp_query);
        }
        queryList.add(new BoolRel("edge", 2));

        RelStruc input_struct = primula.getRels();
        try {
            GroundAtomList gal = new GroundAtomList();

            for (BoolRel brel: queryList) {
                int[][] mat = input_struct.allTypedTuples(brel.getTypes());
                for (int[] ints : mat) gal.add(brel, ints);
            }

            InferenceModule im = primula.openInferenceModule(false);

            im.setQueryAtoms(gal);
            // set as true the class we want condition
            im.setBoolInstArbitrary(new BoolRel("CLASS_0", 0), true);

            primula.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
            primula.setModelPath("/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python/primula-gnn");
            primula.setScriptPath("/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python");
            primula.setScriptName("inference_test");

            im.setNumRestarts(5);

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

            PyTorchExport pye = new PyTorchExport(sampledRel, rbn);
            pye.writePythonDataOnFile("/Users/lz50rg/Dev/primula-workspace/test_rbn_files/python_data.txt");

        } catch (RBNIllegalArgumentException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
