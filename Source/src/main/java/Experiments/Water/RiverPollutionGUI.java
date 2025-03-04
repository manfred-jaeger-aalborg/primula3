package Experiments.Water;

import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.GradientGraph;
import RBNgui.InferenceModule;
import RBNgui.Primula;
import RBNgui.PrimulaGUI;
import RBNpackage.*;
import RBNutilities.rbnutilities;

import javax.swing.*;
import java.io.File;
import java.util.*;

public class RiverPollutionGUI {

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

//        File srsfile = new File("/Users/lz50rg/Dev/water-hawqs/test_small_new.rdef");
        File srsfile = new File("/Users/lz50rg/Dev/water-hawqs/src/test.rdef");
        primula.loadSparseRelFile(srsfile);

        String val_name = "CORN,COSY,PAST,SOYB";

        ArrayList<ArrayList<Rel>> attrs_rels = new ArrayList<>();
        attrs_rels.add(
                new ArrayList<Rel>(
                        Arrays.asList(
//                            new CatRel("LandUse", 1, typeStringToArray("hru", 1), valStringToArray("APPL,RIWN,WATR,COSY,PAST,BERM,RIWF,CORN,UPWN,FESC,FRST,UPWF,SOYB,FRSD"))
                            new CatRel("LandUse", 1, typeStringToArray("hru_agr", 1), valStringToArray(val_name)),
                            new NumRel("AreaAgr", 1, typeStringToArray("hru_agr", 1))
                        )
                )
        );
        attrs_rels.add(
                new ArrayList<Rel>(
                        Arrays.asList(
//                            new CatRel("LandUse", 1, typeStringToArray("hru", 1), valStringToArray("APPL,RIWN,WATR,COSY,PAST,BERM,RIWF,CORN,UPWN,FESC,FRST,UPWF,SOYB,FRSD"))
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
                        1,
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

        RBN file_rbn = new RBN(new File("/Users/lz50rg/Dev/water-hawqs/water_count_linear.rbn"), primula.getSignature());
        RBNPreldef[] riverrbn = file_rbn.prelements();
        RBN manual_rbn = new RBN(5, 0);
        manual_rbn.insertPRel(gnn_rbn, 0);
        manual_rbn.insertPRel(gnn_attr, 1);
        manual_rbn.insertPRel(riverrbn[0], 2);
        manual_rbn.insertPRel(riverrbn[1], 3);
        manual_rbn.insertPRel(riverrbn[2], 4);

//        RBN file_rbn = new RBN(new File("/Users/lz50rg/Dev/water-hawqs/water_rbn.rbn"), primula.getSignature());
//        RBNPreldef[] riverrbn = file_rbn.prelements();
//        RBN manual_rbn = new RBN(4, 0);
//        for (int i = 0; i < 4; i++) {
//            manual_rbn.insertPRel(riverrbn[i], i);
//        }

        primula.setRbn(manual_rbn);
        primula.getInstantiation().init(manual_rbn);
        primula.setRbnparameters(manual_rbn.parameters());

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        for( String arg : args ){
            if( Primula.STR_OPTION_DEBUG.equals( arg ) ) Primula.FLAG_DEBUG = true;
        }
        PrimulaGUI win = new PrimulaGUI(primula);
        win.show();

    }
}

