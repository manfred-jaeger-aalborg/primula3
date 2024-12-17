//package Experiments.Misc;
//
//import Experiments.Misc.PyTorchExport;
//import RBNLearning.*;
//import RBNgui.Bavaria;
//import RBNgui.InferenceModule;
//import RBNgui.Primula;
//import RBNpackage.*;
//import RBNutilities.rbnutilities;
//
//import java.io.File;
//import java.util.*;
//
//public class graph_class {
//
//    public static void openBavaria(boolean open, Primula primula, File srsfile) {
//        if (open) {
//            SparseRelStruc temp = (SparseRelStruc) primula.getRels();
//            if (temp.getCoords().size() == 0)
//                temp.createCoords();
//            new Bavaria(temp, srsfile, primula, false);
//        }
//    }
//
//    // functions copied from RDEFReader.java
//    private static Type[] typeStringToArray(String ts, int arity){
//        Type[] result = new Type[arity];
//        String nexttype;
//        int nextcomma;
//        for (int i=0;i<arity;i++)
//        {
//            nextcomma = ts.indexOf(",");
//            if (nextcomma != -1){
//                nexttype = ts.substring(0,nextcomma);
//                ts = ts.substring(nextcomma+1);
//            }
//            else{
//                nexttype = ts;
//                ts = "";
//            }
//            if (nexttype.equals("Domain"))
//                result[i]=new TypeDomain();
//            else
//                result[i]=new TypeRel(nexttype);
//        }
//        return result;
//    }
//
//    private static String[] valStringToArray(String vs) {
//        return rbnutilities.stringToArray(vs,",");
//    }
//
//    public static void main(String[] args) {
//        Primula primula = new Primula();
//        primula.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
//        primula.setScriptPath("/Users/lz50rg/Dev/primula-workspace/primula3/Source/python/");
//        primula.setScriptName("load_gnn");
//
//        File srsfile = new File("/Users/lz50rg/Dev/logic-gnn/base_6.rdef");
//        primula.loadSparseRelFile(srsfile);
//
//        // create rbn
//        CatRel catRelIN =  new CatRel("IN", 1, typeStringToArray("node",1), valStringToArray("A,B,C,D,E,F,G"));
//        RBNPreldef edge_pred = new RBNPreldef(new BoolRel("edge", 2), new String[]{"v", "w"},  new ProbFormConstant(0.5));
//
//        Vector<ProbForm> softmax = new Vector<>();
//        for (int i = 0; i < 7; i++) {
//            softmax.add(new ProbFormConstant(1));
//        }
//
//        RBNPreldef cat_pred = new RBNPreldef(new CatRel("IN", 1, typeStringToArray("node",1), valStringToArray("A,B,C,D,E,F,G")), new String[]{"v"},  new CatModelSoftMax(softmax));
//        BoolRel edgeRel = new BoolRel("edge", 2);
//        edgeRel.setInout(1);
//
//        RBNPreldef gnn_rbn = new  RBNPreldef(
//            new CatRel("OUT", 0, typeStringToArray("node",0), valStringToArray("T,F")),
//            new String[0],
//            new CatGnn("v",
//                "GCNgraph",
//                true,
//                7,
//                new Rel[]{
//                    catRelIN,
//                    edgeRel
//                },
//                "edge",
//                "ABBA",
//                "graph",
//                true
//            )
//        );
//
//        RBN manual_rbn = new RBN(3, 0);
//        manual_rbn.insertPRel(gnn_rbn,0);
//        manual_rbn.insertPRel(edge_pred,1);
//        manual_rbn.insertPRel(cat_pred,2);
//
//        // add the rbn to primula
//        primula.setRbn(manual_rbn);
//        primula.getInstantiation().init(manual_rbn);
//
//        // the relation to query
//        CatRel tmp_query = new CatRel("OUT", 0, typeStringToArray("",0), valStringToArray("T,F"));
//        tmp_query.setInout(1);
//
//        Vector<GroundAtomList> gal_vec = new Vector<>();
//        RelStruc input_struct = primula.getRels();
//        RelDataForOneInput prob_data = primula.getReldata().elementAt(0);
//
//        try {
//            InferenceModule im = primula.openInferenceModule(false);
//
//            int[][] mat = input_struct.allTypedTuples(catRelIN.getTypes());
//            catRelIN.setInout(1);
//            gal_vec.add(new GroundAtomList());
//            for (int[] ints : mat) gal_vec.get(0).add(catRelIN, ints);
//            im.addQueryAtoms(catRelIN, gal_vec.get(0));
//
//            gal_vec.add(new GroundAtomList());
//            mat = input_struct.allTypedTuples(edgeRel.getTypes());
//            for (int[] ints : mat)  gal_vec.get(1).add(edgeRel, ints);
//            im.addQueryAtoms(edgeRel, gal_vec.get(1));
//            im.toggleAtom(tmp_query, 0);
//
//            // perform map inference
//            im.setNumRestarts(1);
//            GradientGraph GG = im.startMapThread();
//            im.getMapthr().join();
//
//            // collect results
//            Hashtable<Rel, int[]> bestMapVals = im.getMapthr().getBestMapVals();
//            // print results
//            System.out.println("\nMAP INFERENCE RESULTS:\n");
//            for (GroundAtomList gal: gal_vec) {
//                for (int i = 0; i < gal.size(); i++) {
//                    System.out.println(gal.atomAt(i).rel + Arrays.toString(gal.atomAt(i).args) + ": " + bestMapVals.get(gal.atomAt(i).rel)[i]);
//                }
//            }
//
//            // save results in the current Data
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
//
//        } catch (Exception e) {
//            System.out.println(e);
//        }
//
//    }
//}
