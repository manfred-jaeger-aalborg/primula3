package Experiments.Misc;


import java.io.File;


import RBNgui.*;
import RBNpackage.*;
import RBNinference.*;

public class testcategorical {
	
	static String primulahome = System.getenv("PRIMULAHOME");

	static String rbnbase = primulahome + "/Examples/Categorical/";
	static String rbninputfilestring = rbnbase +"catrbn.rbn";
//	static String rbnoutputfilestring = rbnbase +"catrbn_debug.rbn";
	
	static String rdefinputfilestring = primulahome + "/Examples/Categorical/inputstruc-1n.rdef";
	


	public static void main(String[] args) {
		

		
		File srsfile = new File(rdefinputfilestring);

		Primula primula = new Primula();
		
		primula.loadSparseRelFile(srsfile);
		System.out.println("read rdef! ");
		
		
		primula.loadRBNFunction(new File(rbninputfilestring));
		System.out.println("read rbn! ");


		BayesConstructor bc = new BayesConstructor(primula ,
				null,
				null,
				primulahome + "/Examples/Categorical/bnforsamiam.net");
		
		System.out.println("Got BayesConstructor!");
//		
//		try {
//		bc.constructCPTNetwork(Primula.OPTION_NOT_EVIDENCE_CONDITIONED,
//				Primula.OPTION_NOT_EVIDENCE_CONDITIONED,
//				Primula.OPTION_NOT_DECOMPOSE,
//				Primula.OPTION_NOT_ELIMINATE_ISOLATED_ZERO_NODES,
//				Primula.OPTION_LAYOUT,
//				Primula.OPTION_SAMIAM);
//		}
//		catch (Exception e) {System.out.println(e);}

//		InferenceModule infmod = new InferenceModule(primula);
		
		GroundAtomList gal = new GroundAtomList();
		gal.add(primula.getSignature().getRelByName("blue"), new int[] {0});
		gal.add(primula.getSignature().getRelByName("red"), new int[] {0});
//		gal.add(primula.getSignature().getRelByName("rank"), new int[] {0});
//		gal.add(primula.getSignature().getRelByName("rank"), new int[] {1});
//		gal.add(primula.getSignature().getRelByName("rank"), new int[] {2});
//		gal.add(primula.getSignature().getRelByName("rank"), new int[] {3});
//		
		bc.setQueryatoms(gal);
		bc.setInstarg(primula.getInstantiation());
		bc.setStrucarg(primula.getRels());
		
		PFNetwork pfn = null;
		
		try {
		pfn = bc.constructPFNetwork(Primula.OPTION_EVIDENCE_CONDITIONED,
				Primula.OPTION_QUERY_SPECIFIC,
				Primula.OPTION_ELIMINATE_ISOLATED_ZERO_NODES);
		
		
		pfn.prepareForSampling(InferenceModule.OPTION_SAMPLEORD_FORWARD,
				InferenceModule.OPTION_SAMPLE_ADAPTIVE,
				null,
				3,  //max parents
				gal,
				5, //num_subsamples_minmax
				4, //num_subsamples_adapt
				null);
		
		} catch (Exception e) {System.out.println(e);}
		
		
		System.out.println("Got PFNetwork!");
		
		SampleObserver sobs = new SampleObserver();
		
		SampleThread sampthread = new SampleThread(sobs, 
															pfn, 
															gal, 
															null,
															null);

		sampthread.run();
		
		System.out.println("success");

		primula.exitProgram();
	}

}
