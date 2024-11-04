package Experiments.Water;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNCyclicException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.GradientGraph;
import RBNLearning.RelDataForOneInput;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNinference.BayesConstructor;
import RBNpackage.*;
import RBNutilities.rbnutilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RiverPollution {

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
        primula.setScriptPath("/Users/lz50rg/Dev/primula-workspace/primula3/Source/python");
        primula.setScriptName("load_gnn");

        File srsfile = new File("/Users/lz50rg/Dev/water-hawqs/test_small.rdef");
        primula.loadSparseRelFile(srsfile);

        ArrayList<ArrayList<Rel>> attrs_rels = new ArrayList<>();
        attrs_rels.add(
                new ArrayList<Rel>(
                        Arrays.asList(
//                            new CatRel("LandUse", 1, typeStringToArray("hru", 1), valStringToArray("APPL,RIWN,WATR,COSY,PAST,BERM,RIWF,CORN,UPWN,FESC,FRST,UPWF,SOYB,FRSD"))
                            new CatRel("LandUse", 1, typeStringToArray("hru", 1), valStringToArray("CORN,COSY,OTHER,PAST,SOYB")),
                            new NumRel("Area", 1, typeStringToArray("hru", 1))
                        )
                )
        );
        attrs_rels.add(
                new ArrayList<Rel>(
                    Arrays.asList(
                        new CatRel("SubType", 1, typeStringToArray("sub", 1), valStringToArray("RES,SUB"))
                    )
                )
        );

        // set LandUse as probabilistic
        attrs_rels.get(0).get(0).setInout(Rel.PROBABILISTIC);

        ArrayList<String> edge_attr = new ArrayList<>();
        edge_attr.add("hru_to_sub");
        edge_attr.add("sub_to_sub");

        RBNPreldef gnn_rbn = new  RBNPreldef(
                new CatRel("Pollution", 1, typeStringToArray("sub",1), valStringToArray("LOW,MED,HIG")),
                new String[]{"v"},
                new CatGnnHetero("v",
                        "riverGNN",
                        3,
                        attrs_rels,
                        edge_attr,
                        "node",
                        true
                )
        );

        Vector<ProbForm> softmax = new Vector<>();
        for (int i = 0; i < 5; i++) {
            softmax.add(new ProbFormConstant(0.5));
        }

        RBNPreldef gnn_attr = new  RBNPreldef(
                new CatRel("LandUse", 1, typeStringToArray("hru", 1), valStringToArray("CORN,COSY,OTHER,PAST,SOYB")),
                new String[]{"v"},
                new CatModelSoftMax(softmax)
        );

//        File input_file = new File("/Users/lz50rg/Dev/water-hawqs/water.rbn");
        File input_file = new File("/Users/lz50rg/Dev/water-hawqs/water_rbn.rbn");
        RBN file_rbn = new RBN(input_file, primula.getSignature());
        RBNPreldef[] riverrbn = file_rbn.prelements();

        RBN manual_rbn = new RBN(4, 0);
        for (int i = 0; i < 4; i++) {
            manual_rbn.insertPRel(riverrbn[i], i);
        }

//        manual_rbn.insertPRel(gnn_rbn, 0);
//        manual_rbn.insertPRel(gnn_attr, 1);
//        manual_rbn.insertPRel(riverrbn[0], 2);
//        manual_rbn.insertPRel(riverrbn[1], 3);

        primula.setRbn(manual_rbn);
        primula.getInstantiation().init(manual_rbn);

        CatRel tmp_query = new CatRel("LandUse", 1, typeStringToArray("hru", 1), valStringToArray("CORN,COSY,OTHER,PAST,SOYB"));
        tmp_query.setInout(Rel.PROBABILISTIC);

        BayesConstructor constructor = new BayesConstructor(
                primula,
                primula.getInstantiation(), // onestructdata
                new GroundAtomList(), // groundatom list (empty)
                "river.net"); // something.net
        try {
            constructor.constructCPTNetwork(
                    0,
                    0,
                    2,
                    1,
                    0,
                    3);
        } catch (RBNCompatibilityException e) {
            throw new RuntimeException(e);
        } catch (RBNCyclicException e) {
            throw new RuntimeException(e);
        } catch (RBNIllegalArgumentException e) {
            throw new RuntimeException(e);
        }

//        try {
//            InferenceModule im = primula.openInferenceModule(false);
//
//            // do not query for already instantiated values (in this case if a node is OTHER)
//            OneStrucData inst = primula.getInstantiation();
//            Vector<OneCatRelData> catInst = inst.getAllonecatdata();
//            TreeMap<int[],Integer> instantiated = new TreeMap<>();
//            for (OneCatRelData catData: catInst) {
//                if (catData.rel().name().equals("LandUse")) {
//                    instantiated = catData.values;
//                    break;
//                }
//            }
//
//            GroundAtomList gal = new GroundAtomList();
////            for (int i = 23; i < 989; i++) {
//            for (int i = 2; i < 8; i++) {
////                if (!instantiated.containsKey(new int[]{i}))
//                    gal.add(tmp_query, new int[]{i});
//            }
//            im.addQueryAtoms(tmp_query, gal);
//
//            im.setNumRestarts(1);
//            GradientGraph GG = im.startMapThread();
//            im.getMapthr().join();
//
//            Hashtable<Rel, int[]> bestMapVals = im.getMapthr().getBestMapVals();
//
////            String[] vals = new String[]{"LOW","MED","HIG"};
////            String[] vals = new String[]{"APPL,RIWN,WATR,COSY,PAST,BERM,RIWF,CORN,UPWN,FESC,FRST,UPWF,SOYB,FRSD"};
//            String[] vals = new String[]{"CORN,COSY,OTHER,PAST,SOYB"};
//            int[] res = bestMapVals.get(tmp_query);
//            ArrayList<ArrayList<Integer>> pred_res = new ArrayList<>(vals.length);
//            for (int i = 0; i < vals.length; i++)
//                pred_res.add(new ArrayList<>());
//
//            System.out.println("\nMAP INFERENCE RESULTS:\n");
//            for (int i = 0; i < gal.size(); i++) {
//                System.out.println(gal.atomAt(i).rel().toString() + "(" + gal.atomAt(i).args()[0] + "): " + res[i]);
////                pred_res.get(res[i]).add(Integer.valueOf(gal.atomAt(i).args()[0]));
//            }
//            System.out.println("GG logLikelihood: " + GG.currentLogLikelihood());
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }
}

