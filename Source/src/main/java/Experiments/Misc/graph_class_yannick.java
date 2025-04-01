package Experiments.Misc;

import RBNLearning.GradientGraph;
import RBNLearning.RelDataForOneInput;
import RBNgui.Bavaria;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNpackage.*;
import RBNutilities.rbnutilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

public class graph_class_yannick {

    public static void openBavaria(boolean open, Primula primula, File srsfile) {
        if (open) {
            SparseRelStruc temp = (SparseRelStruc) primula.getRels();
            if (temp.getCoords().size() == 0)
                temp.createCoords();
            new Bavaria(temp, srsfile, primula.getPrimulaGUI(), false);
        }
    }

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

    public static void main(String[] args) {
        Primula primula = new Primula();
        primula.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
        primula.setScriptPath("/Users/lz50rg/Dev/football/overlapping_cat/");
        primula.setScriptName("load_model");

        File srsfile = new File("/Users/lz50rg/Dev/football/overlapping_2024_12_12/2024-12-12_move_to_cat/10-47-26-12a63b46/overlapping.rdef");
        primula.loadSparseRelFile(srsfile);

        // create rbn
        CatRel catRelA =  new CatRel("a", 1, typeStringToArray("node",1), valStringToArray("ball,player"));
        CatRel catRelX =  new CatRel("x", 1, typeStringToArray("node",1), valStringToArray("x1,x2,x3,x4,x5,x6,x7,x8,x9,x10"));
        CatRel catRelY =  new CatRel("y", 1, typeStringToArray("node",1), valStringToArray("y1,y2,y3,y4,y5,y6,y7,y8"));
        CatRel catRelD =  new CatRel("d", 1, typeStringToArray("node",1), valStringToArray("d1,d2,d3,d4,d5,d6,d7,d8"));
        CatRel catRelS =  new CatRel("s", 1, typeStringToArray("node",1), valStringToArray("s1,s2,s3,s4,s5"));
        BoolRel edgeRel = new BoolRel("edge", 2, typeStringToArray("node,node",2));

        ArrayList<ArrayList<Rel>> attrs_rels = new ArrayList<>();
        attrs_rels.add(
                new ArrayList<Rel>(
                        Arrays.asList(
                                new CatRel("a", 1, typeStringToArray("node",1), valStringToArray("ball,player")),
                                new CatRel("x", 1, typeStringToArray("node",1), valStringToArray("x1,x2,x3,x4,x5,x6,x7,x8,x9,x10")),
                                new CatRel("y", 1, typeStringToArray("node",1), valStringToArray("y1,y2,y3,y4,y5,y6,y7,y8")),
                                new CatRel("d", 1, typeStringToArray("node",1), valStringToArray("d1,d2,d3,d4,d5,d6,d7,d8")),
                                new CatRel("s", 1, typeStringToArray("node",1), valStringToArray("s1,s2,s3,s4,s5"))
                        )
                )
        );

        attrs_rels.get(0).get(0).setInout(Rel.PREDEFINED);
        attrs_rels.get(0).get(1).setInout(Rel.PROBABILISTIC);
        attrs_rels.get(0).get(2).setInout(Rel.PROBABILISTIC);
        attrs_rels.get(0).get(3).setInout(Rel.PROBABILISTIC);
        attrs_rels.get(0).get(4).setInout(Rel.PROBABILISTIC);

//        new BoolRel("edge", 2, typeStringToArray("node,node",2))
//        attrs_rels.get(0).get(5).setInout(Rel.PROBABILISTIC);

        ArrayList<Rel> edge_attr = new ArrayList<>();
        edge_attr.add(edgeRel);
        edge_attr.get(0).setInout(Rel.PROBABILISTIC);
//        edge_attr.add("edge");

        Vector<ProbForm> softmaxA = new Vector<>();
        for (int i = 0; i < 2; i++) {
            softmaxA.add(new ProbFormConstant(1));
        }
        Vector<ProbForm> softmaxX = new Vector<>();
        for (int i = 0; i < 10; i++) {
            softmaxX.add(new ProbFormConstant(1));
        }
        Vector<ProbForm> softmaxY = new Vector<>();
        for (int i = 0; i < 8; i++) {
            softmaxY.add(new ProbFormConstant(1));
        }
        Vector<ProbForm> softmaxD = new Vector<>();
        for (int i = 0; i < 8; i++) {
            softmaxD.add(new ProbFormConstant(1));
        }
        Vector<ProbForm> softmaxS = new Vector<>();
        for (int i = 0; i < 5; i++) {
            softmaxS.add(new ProbFormConstant(1));
        }

        // Define the probabilistic relations
//        RBNPreldef cat_predA = new RBNPreldef(new CatRel("a", 1, typeStringToArray("node",1), valStringToArray("ball,player")), new String[]{"v"},  new CatModelSoftMax(softmaxA));
        RBNPreldef cat_predX = new RBNPreldef(new CatRel("x", 1, typeStringToArray("node",1), valStringToArray("x1,x2,x3,x4,x5,x6,x7,x8,x9,x10")), new String[]{"v"},  new CatModelSoftMax(softmaxX));
        RBNPreldef cat_predY = new RBNPreldef(new CatRel("y", 1, typeStringToArray("node",1), valStringToArray("y1,y2,y3,y4,y5,y6,y7,y8")), new String[]{"v"},  new CatModelSoftMax(softmaxY));
        RBNPreldef cat_predD = new RBNPreldef(new CatRel("d", 1, typeStringToArray("node",1), valStringToArray("d1,d2,d3,d4,d5,d6,d7,d8")), new String[]{"v"},  new CatModelSoftMax(softmaxD));
        RBNPreldef cat_predS = new RBNPreldef(new CatRel("s", 1, typeStringToArray("node",1), valStringToArray("s1,s2,s3,s4,s5")), new String[]{"v"},  new CatModelSoftMax(softmaxS));
        RBNPreldef edge_pred = new RBNPreldef(new BoolRel("edge", 2, typeStringToArray("node,node",2)), new String[]{"v", "w"},  new ProbFormConstant(0.5));

        RBNPreldef gnn_rbn = new  RBNPreldef(
            new CatRel("output", 0, typeStringToArray("",0), valStringToArray("T,F")),
            new String[0],
            new CatGnnOld("",
                "GCNgraph",
                true,
                2,
                attrs_rels,
                edge_attr,
                "graph",
                true
            )
        );

        RBN manual_rbn = new RBN(6, 0);
        manual_rbn.insertPRel(gnn_rbn,0);
        manual_rbn.insertPRel(cat_predX,1);
        manual_rbn.insertPRel(cat_predY,2);
        manual_rbn.insertPRel(cat_predD,3);
        manual_rbn.insertPRel(cat_predS,4);
        manual_rbn.insertPRel(edge_pred, 5);

        // add the rbn to primula
        primula.setRbn(manual_rbn);
        primula.getInstantiation().init(manual_rbn);
        primula.setRbnparameters(manual_rbn.parameters());

        // the relation to query
        CatRel tmp_query = new CatRel("output", 0, typeStringToArray("",0), valStringToArray("T,F"));
        tmp_query.setInout(1);

        Vector<GroundAtomList> gal_vec = new Vector<>();
        RelStruc input_struct = primula.getRels();
        RelDataForOneInput prob_data = primula.getReldata().elementAt(0);

        try {
            InferenceModule im = primula.createInferenceModule();

            int[][] mat = input_struct.allTypedTuples(catRelX.getTypes());
            catRelX.setInout(1);
            gal_vec.add(new GroundAtomList());
            for (int[] ints : mat) gal_vec.get(0).add(catRelX, ints);
            im.addQueryAtoms(catRelX, gal_vec.get(0));

            mat = input_struct.allTypedTuples(catRelY.getTypes());
            catRelY.setInout(1);
            gal_vec.add(new GroundAtomList());
            for (int[] ints : mat) gal_vec.get(1).add(catRelY, ints);
            im.addQueryAtoms(catRelY, gal_vec.get(1));

            mat = input_struct.allTypedTuples(catRelD.getTypes());
            catRelD.setInout(1);
            gal_vec.add(new GroundAtomList());
            for (int[] ints : mat) gal_vec.get(2).add(catRelD, ints);
            im.addQueryAtoms(catRelD, gal_vec.get(2));

            mat = input_struct.allTypedTuples(catRelS.getTypes());
            catRelS.setInout(1);
            gal_vec.add(new GroundAtomList());
            for (int[] ints : mat) gal_vec.get(3).add(catRelS, ints);
            im.addQueryAtoms(catRelS, gal_vec.get(3));

            mat = input_struct.allTypedTuples(edgeRel.getTypes());
            edgeRel.setInout(1);
            gal_vec.add(new GroundAtomList());
            for (int[] ints : mat)  gal_vec.get(4).add(edgeRel, ints);
            im.addQueryAtoms(edgeRel, gal_vec.get(4));

            im.toggleAtom(tmp_query, 0);
            // perform map inference
            im.setNumRestarts(1);
            im.setMapSearchAlg(2);
            im.setNumChains(0);
            im.setWindowSize(0);

            GradientGraph GG = im.startMapThread();
            im.getMapthr().join();

            // collect results
            Hashtable<Rel, int[]> bestMapVals = im.getMapthr().getBestMapVals();
            // print results
            System.out.println("\nMAP INFERENCE RESULTS:\n");
            for (GroundAtomList gal: gal_vec) {
                for (int i = 0; i < gal.size(); i++) {
                    System.out.println(gal.atomAt(i).rel + Arrays.toString(gal.atomAt(i).args) + ": " + bestMapVals.get(gal.atomAt(i).rel)[i]);
                }
            }

            // save results in the current Data
//            OneStrucData result = new OneStrucData();
//            if (GG != null){
//                result.setParentRelStruc(primula.getRels());
//
//                for (GroundAtomList gal: gal_vec) {
//                    for (int i = 0; i < gal.size(); i++) {
//                        result.add(new GroundAtom(gal.atomAt(i).rel(), gal.atomAt(i).args), bestMapVals.get(gal.atomAt(i).rel)[i], "?");
//                    }
//                }
//
//                primula.getInstantiation().add(result);
//                im.updateInstantiationList();
//                primula.updateBavaria();
//            }
//
//            OneStrucData onsd = new OneStrucData(primula.getRels().getmydata().copy());
//            SparseRelStruc sampledRel = new SparseRelStruc(primula.getRels().getNames(), onsd, primula.getRels().getCoords(), primula.getRels().signature());
//            sampledRel.getmydata().add(primula.getInstantiation().copy());
//
//            PyTorchExport pye = new PyTorchExport(sampledRel, manual_rbn);
//            pye.writePythonDataOnFile("/Users/lz50rg/Dev/logic-gnn/python_data.txt");

        } catch (Exception e) {
            System.out.println(e);
        }

    }
}
