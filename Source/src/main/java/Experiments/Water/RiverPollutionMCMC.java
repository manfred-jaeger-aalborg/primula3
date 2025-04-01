package Experiments.Water;

import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNinference.SampleProbs;
import RBNpackage.*;
import RBNutilities.rbnutilities;

import java.io.*;
import java.util.*;

public class RiverPollutionMCMC {
    public static int EXPNUM = 0;

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

        String expNum = args[0];
        String restart = args[1];
        String basePath = args[2];
        EXPNUM = Integer.parseInt(expNum);
        System.out.println("exp: " + expNum + " restart: " + restart);
//        int expNum = 18;
//        int restart = 1;
//        EXPNUM = expNum;

        double constStrength = 0.3;

        Primula primula = new Primula();
        primula.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
        primula.setScriptPath("/Users/lz50rg/Dev/primula-workspace/primula3/Source/python");
        primula.setScriptName("load_gnn");

        Map<String, Object> load_gnn_set = new HashMap<>();
        load_gnn_set.put("model", "riverGNN");
        load_gnn_set.put("sdataset", "pollution");
        load_gnn_set.put("base_path", "/Users/lz50rg/Dev/water-hawqs/models/");
        primula.setLoadGnnSet(load_gnn_set);

        File srsfile = new File(basePath + "redef_graph_" + expNum + "_" + restart + ".rdef");
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
                new CatGnn("v",
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

        RBN file_rbn = new RBN(new File("/Users/lz50rg/Dev/water-hawqs/water_count_sub_mcmc.rbn"), primula.getSignature());
        RBNPreldef[] riverrbn = file_rbn.prelements();
        RBN manual_rbn = new RBN(3, 0);
        manual_rbn.insertPRel(gnn_rbn, 0);
        manual_rbn.insertPRel(gnn_attr, 1);
        manual_rbn.insertPRel(riverrbn[0], 2);

        primula.setRbn(manual_rbn);
        primula.getInstantiation().init(manual_rbn);
        primula.setRbnparameters(manual_rbn.parameters());

        try {
            InferenceModule im = primula.createInferenceModule();
            BoolRel queryRel = new BoolRel("constr", 0);

            GroundAtomList queryGround = new GroundAtomList();
            if (queryRel.getArity()==0) {
                queryGround.add(new GroundAtom(queryRel,new int[0]));
            }
            im.addQueryAtom(queryRel, queryGround, 0);

            im.startSampleThread();
            System.out.println("Start sampling...");

            double size = 0;
            double oldsize = -1;
            System.out.println("Sampling ...");
            while (size < 30000) {
                size = im.getSamThr().getNumsamp();
                if (oldsize != size) {
                    oldsize = size;
                    if (size % 10000 == 0)
                        System.out.println("Sample size: " + size);
                }
            }

            im.stopSampleThread();
            im.getSampthr().join();
            SampleProbs finalSprobs = im.getSampthr().getSprobs();
            System.out.println(Arrays.deepToString(finalSprobs.getProbs(queryRel)));

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(basePath + "txt_graph_" + expNum + "_" + restart +".txt", true))) {
                writer.newLine();
                writer.write(Arrays.deepToString(finalSprobs.getProbs(queryRel)));
                System.out.println("Line appended successfully!");
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.exit( 0 );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

