package Experiments.Water;

import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.GradientGraph;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNinference.SampleProbs;
import RBNpackage.*;
import RBNutilities.rbnutilities;

import java.io.File;
import java.util.*;

public class RiverPollutionMAP {

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

        Map<String, Object> load_gnn_set = new HashMap<>();
        load_gnn_set.put("model", "riverGNN");
        load_gnn_set.put("sdataset", "pollution");
        load_gnn_set.put("base_path", "/Users/lz50rg/Dev/water-hawqs/models/");
        primula.setLoadGnnSet(load_gnn_set);

//        File srsfile = new File("/Users/lz50rg/Dev/water-hawqs/src/test.rdef");
        File srsfile = new File("/Users/lz50rg/Dev/water-hawqs/river_test_const.rdef");
        primula.loadSparseRelFile(srsfile);

        String val_name = "CORN,COSY,PAST,SOYB";

        ArrayList<ArrayList<Rel>> attrs_rels = new ArrayList<>();
        attrs_rels.add(
                new ArrayList<Rel>(
                        Arrays.asList(
                            new CatRel("LandUse", 1, typeStringToArray("hru_agr", 1), valStringToArray(val_name)),
                            new NumRel("AreaAgr", 1, typeStringToArray("hru_agr", 1))
                        )
                )
        );
        attrs_rels.add(
                new ArrayList<Rel>(
                        Arrays.asList(
                                new CatRel("LandUseUrb", 1, typeStringToArray("hru_urb", 1), valStringToArray("BERM,FESC,FRSD,FRST,RIWF,RIWN,UPWF,UPWN,WATR")),
                                new NumRel("AreaUrb", 1, typeStringToArray("hru_urb", 1))
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

        BoolRel agrsub = new BoolRel("hru_agr_to_sub", 2, typeStringToArray("hru_agr,sub",2));
        BoolRel urbsub = new BoolRel("hru_urb_to_sub", 2, typeStringToArray("hru_urb,sub",2));
        BoolRel subsub = new BoolRel("sub_to_sub", 2, typeStringToArray("sub,sub",2));
        ArrayList<Rel> edge_attr = new ArrayList<>();
        edge_attr.add(agrsub);
        edge_attr.add(urbsub);
        edge_attr.add(subsub);
        edge_attr.get(0).setInout(Rel.PREDEFINED);
        edge_attr.get(1).setInout(Rel.PREDEFINED);
        edge_attr.get(2).setInout(Rel.PREDEFINED);

        RBNPreldef gnn_rbn = new  RBNPreldef(
                new CatRel("Pollution", 1, typeStringToArray("sub",1), valStringToArray("LOW,MED,HIG")),
                new String[]{"v"},
                new CatGnnHetero("v",
                        "HeteroGraphpollution",
                        2,
                        3,
                        attrs_rels,
                        edge_attr,
                        "node",
                        true
                )
        );

        Vector<ProbForm> softmax = new Vector<>();
        for (int i = 0; i < 4; i++) {
            softmax.add(new ProbFormConstant(0.5));
        }

        RBNPreldef gnn_attr = new  RBNPreldef(
                new CatRel("LandUse", 1, typeStringToArray("hru_agr", 1), valStringToArray(val_name)),
                new String[]{"v"},
                new CatModelSoftMax(softmax)
        );

        RBN file_rbn = new RBN(new File("/Users/lz50rg/Dev/water-hawqs/water_count_sub.rbn"), primula.getSignature());
        RBNPreldef[] riverrbn = file_rbn.prelements();
        RBN manual_rbn = new RBN(3, 0);
        manual_rbn.insertPRel(gnn_rbn, 0);
        manual_rbn.insertPRel(gnn_attr, 1);
        manual_rbn.insertPRel(riverrbn[0], 2);

//        manual_rbn.insertPRel(riverrbn[1], 3);

//        RBN file_rbn = new RBN(new File("/Users/lz50rg/Dev/water-hawqs/water_rbn.rbn"), primula.getSignature());
//        RBNPreldef[] riverrbn = file_rbn.prelements();
//        RBN manual_rbn = new RBN(3, 0);
//        for (int i = 0; i < 3; i++) {
//            manual_rbn.insertPRel(riverrbn[i], i);
//        }

        primula.setRbn(manual_rbn);
        primula.getInstantiation().init(manual_rbn);
        primula.setRbnparameters(manual_rbn.parameters());

        Vector<GroundAtomList> gal_vec = new Vector<>();
        RelStruc input_struct = primula.getRels();
        CatRel tmp_query = new CatRel("LandUse", 1, typeStringToArray("hru_agr", 1), valStringToArray(val_name));
        CatRel pollRel = new CatRel("Pollution", 1, typeStringToArray("sub",1), valStringToArray("LOW,MED,HIG"));
        tmp_query.setInout(Rel.PROBABILISTIC);
        pollRel.setInout(Rel.PROBABILISTIC);


        try {
            InferenceModule im = primula.createInferenceModule();

            int[][] mat = input_struct.allTypedTuples(tmp_query.getTypes());
            gal_vec.add(new GroundAtomList());
            for (int[] ints: mat) gal_vec.get(0).add(tmp_query, ints);
            im.addQueryAtoms(tmp_query, gal_vec.get(0));

            mat = input_struct.allTypedTuples(pollRel.getTypes());
            gal_vec.add(new GroundAtomList());
            for (int[] ints: mat) gal_vec.get(1).add(pollRel, ints);
            im.addQueryAtoms(pollRel, gal_vec.get(1));

//            im.toggleAtom(tmp_query, 0);
            im.setMapSearchAlg(3);
            im.setNumIterGreedyMap(4000);
            im.setNumRestarts(1);
            im.setWindowSize(100);
            im.setNumChains(0);
            GradientGraph GG = im.startMapThread();
            im.getMapthr().join();

            Hashtable<Rel, int[]> bestMapVals = im.getMapthr().getBestMapVals();

            String[] vals = new String[]{val_name};
            int[] res = bestMapVals.get(tmp_query);
            ArrayList<ArrayList<Integer>> pred_res = new ArrayList<>(vals.length);
            for (int i = 0; i < vals.length; i++)
                pred_res.add(new ArrayList<>());

            // count how many crops type has been assigned
            Map<String, Integer> values_count = new HashMap<>();
            List<String> crops = Arrays.asList(val_name.split(","));
            for (int i = 0; i < 4; i++) {
                values_count.put(crops.get(i), 0);
            }

//            PrintWriter writer = new PrintWriter("final-graph.txt", "UTF-8");
            System.out.println("\nMAP INFERENCE RESULTS:\n");
            for (GroundAtomList gal: gal_vec) {
                for (int i = 0; i < gal.size(); i++) {
                    System.out.println(gal.atomAt(i).rel + Arrays.toString(gal.atomAt(i).args) + ": " + bestMapVals.get(gal.atomAt(i).rel)[i]);
                    if (gal.atomAt(i).relname().equals("LandUse"))
                        values_count.put(crops.get(res[i]), values_count.get(crops.get(res[i]))+1);
                }
            }
//            writer.close();

            System.out.println("Final GG logLikelihood: " + GG.currentLogLikelihood());

            System.out.println(values_count);

            // Save values


            System.exit( 0 );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (RBNIllegalArgumentException e) {
            throw new RuntimeException(e);
        }
//        catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
    }
}

