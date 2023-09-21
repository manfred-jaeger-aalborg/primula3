package Experiments;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import RBNLearning.RelData;
import RBNgui.*;
import RBNpackage.ProbForm;

public class runexperiment {

	static String primulahome = System.getenv("PRIMULAHOME");

	static String rbnbase = primulahome + "/Examples/GraphNN-develop/alpha1-[4]";
	static String rbninputfilestring = rbnbase +".rbn";
	
	static String rdefinputfilestring = primulahome + "/Examples/GraphNN-develop/GNNdata/p1/test-random-erdos-500-40-50.rdef";

	
	
	public static void main(String[] args) {
		Calendar mycal = new GregorianCalendar();
		SimpleDateFormat mydateform = new SimpleDateFormat("dd.MM.yyyy'-'HH:mm:ss");
		String timestamp = mydateform.format(mycal.getTime());
		
		String rbnoutputfilestring = rbnbase + "-" + timestamp	+ ".rbn";
		
		Primula primula = new Primula();
		LearnModule lm = primula.openLearnModule(false);
		
		int threadascentstrategy = LearnModule.AscentAdam;
		int ggascentstrategy = LearnModule.AscentAdagrad;
				
		int restarts = 2;
		int numbatches = 50; 
		double alpha = 0.01;
		double beta1 = 0.9;
		double beta2= 0.999;
		boolean memoize = true;
		boolean useggs = false;
		boolean keepggs = false;
		int maxits = 20;
		int splitmode = RelData.SPLIT_BY_DOMAIN;
		int gradienttype = ProbForm.RETURN_ARRAY;
		
		lm.setGGStrategy(ggascentstrategy);
		lm.setThreadStrategy(threadascentstrategy);
		
		lm.setRestarts(restarts);
		lm.setNumbatches(numbatches);
		lm.setAdamAlpha(alpha);
		lm.setAdamBeta1(beta1);
		lm.setAdamBeta2(beta2);
		lm.setUseGGs(keepggs);
		lm.setUseGGs(useggs);
		lm.setUseMemoize(memoize);
		lm.setMaxIterations(maxits);
		lm.setSplitmode(splitmode);
		lm.setType_of_gradient(gradienttype);
		
		File srsfile = new File(rdefinputfilestring);
		File rbnfile = new File(rbninputfilestring);
		File rbnoutputfile = new File(rbnoutputfilestring);

		primula.loadRBNFunction(rbnfile);
		primula.loadSparseRelFile(srsfile);
		
		System.out.println("# " + rbninputfilestring);
		System.out.println("# " + rdefinputfilestring);
		
		System.out.println("# Optimize: " + LearnModule.threadstrategy[threadascentstrategy] + "  " + LearnModule.ggstrategy[ggascentstrategy]);
		System.out.println("# Restarts: " + restarts );
		System.out.println("# NumBatches: " + numbatches );
		System.out.println("# alpha: " + alpha);
		System.out.println("# beta1: " + beta1 );
		System.out.println("# beta2: " + beta2 );
		System.out.println("# maxIts: " + maxits );
		
		
		
		lm.startLearning();
		
		try {		 
			lm.getLearnThread().join();
		} catch (InterruptedException ex) {
			// do nothing
		}

		lm.setParametersPrimula();
		primula.getRBN().saveToFile(rbnoutputfile, Primula.CHERRYSYNTAX, true);

		System.out.println("# done");
	}

}
