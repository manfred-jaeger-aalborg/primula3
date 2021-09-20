package Experiments;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import RBNgui.*;

public class runexperiment {

	static String primulahome = System.getenv("PRIMULAHOME");

	static String rbnbase = primulahome + "/Examples/GraphNN/alpha2-2-1-2";
	static String rbninputfilestring = rbnbase +".rbn";
	
	static String rdefinputfilestring = primulahome + "/Examples/GraphNN/GNNdata/p2/test-random-erdos-500-40-50.rdef";

	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Calendar mycal = new GregorianCalendar();
		SimpleDateFormat mydateform = new SimpleDateFormat("dd.MM.yyyy'-'HH:mm:ss");
		String timestamp = mydateform.format(mycal.getTime());
		
		String rbnoutputfilestring = rbnbase + "-" + timestamp	+ ".rbn";
		
		Primula primula = new Primula();
		LearnModule lm = primula.openLearnModule(false);
		
		int restarts = 1;
		int numbatches = 20; 
		double alpha = 0.05;
		double beta1 = 0.9;
		double beta2= 0.999;
		boolean keepggs = false;
		int maxits = 200;
		
		lm.setRestarts(restarts);
		lm.setNumbatches(numbatches);
		lm.setAdamAlpha(alpha);
		lm.setAdamBeta1(beta1);
		lm.setAdamBeta2(beta2);
		lm.setKeepGGs(keepggs);
		lm.setMaxIterations(maxits);
		
		File srsfile = new File(rdefinputfilestring);
		File rbnfile = new File(rbninputfilestring);
		File rbnoutputfile = new File(rbnoutputfilestring);

		primula.loadRBNFunction(rbnfile);
		primula.loadSparseRelFile(srsfile);
		
		System.out.println("# " + rbninputfilestring);
		System.out.println("# " + rdefinputfilestring);
		
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

		System.out.println("done");
	}

}
