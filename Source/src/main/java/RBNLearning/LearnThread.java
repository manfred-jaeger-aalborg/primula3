/*
 * LearnThread.java 
 * 
 * Copyright (C) 2009 Aalborg University
 *
 * contact:
 * jaeger@cs.aau.dk   http://www.cs.aau.dk/~jaeger/Primula.html
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package RBNLearning;

import RBNExceptions.*;
import RBNpackage.*;
import RBNgui.*;
import RBNutilities.*;
import RBNinference.*;
import java.util.*;

import javax.swing.*;

import myio.StringOps;

import java.text.*;

public class LearnThread extends GGThread {

	Primula myprimula;
	LearnModule myLearnModule;


	/* databatches is a partitioning of alldata if
	 * some form of batch stochastic gradient ascent is 
	 * chosen in the LearnModule.
	 * 
	 * Otherwise, databatches[0]=alldata
	 */
	RelData[] databatches;
	RelData alldata;
	ParameterTableModel parammodel;
	JTable parametertable;
	JTextField numrestartsfield;
	DecimalFormat timeformat = new DecimalFormat("#.##");


	private int threadascentstrategy;
	//	private int ggascentstrategy;

	/* Auxiliary variables for different ascent strategies: 
	 * 
	 * Such variables need to be declared here, when the ascent 
	 * strategy is stochastic, and the auxiliary variables need to 
	 * persist over iterations (each time involving a new gradient graph)
	 * */

	/* for AscentAdam: */
	double[] firstmomentest = null;
	double[] secondmomentest = null;
	double[] mhat;
	double[] vhat;
	double beta1,beta2,epsilon,alpha;

	/* for AscentStochHeur1 and 2: */
	double[] resultvec = null;
	double[] resultsum;
	double[] incrementvec = null;
	double dampfac;
	double[] avec;
	double scalefac;
	double[] asums;

	/* for AscentBlock: */
	String[][] paramblocks = null;

	Profiler profiler;
	
	public LearnThread(Primula mypr,
			RelData ad,
			RelData[] d, 
			ParameterTableModel parmod,
			JTable partab,
			JTextField nrest,
			LearnModule mylm){
		myprimula = mypr;
		myLearnModule = mylm;
		databatches = d;
		alldata = ad;
		parammodel = parmod;
		parametertable = partab;
		numrestartsfield = nrest;
		threadascentstrategy = myLearnModule.threadascentstrategy();
		profiler = new Profiler();
		//		ggascentstrategy = myLearnModule.ggascentstrategy();
	}

	public void run() throws RBNRuntimeException
	{
		
		
		if (databatches != null)
		{

			
			
			
			
			/* Numerical relations to be learned, divided into blocks
			 * Some relations may also be given by enumeration of their ground
			 * atoms
			 * 
			 */
			String[][] parameternumrels = myprimula.getParamNumRels();

			Hashtable<String,Integer> parameters = myprimula.makeParameterIndex();
			
			double[][] minmaxbounds = myprimula.makeMinMaxBounds();

			parammodel.setParameters(parameters);
			parammodel.fireTableDataChanged();
			parametertable.updateUI();
			// boolean computeLikOnly = (parameters.length == 0);

			numrestartsfield.setText("" );
			double[] paramvals = new double[parameters.size()+1];

//			if (threadascentstrategy == LearnModule.AscentBlock){
//				/* Divide parameters into blocks for block gradient descent 
//				   Each of the blocks defined in nrelparamblocks is divided into
//				   numparamblocks sub-blocks. The RBN parameters form their own
//				   block.
//				 */
//				int numparamblocks = myLearnModule.getNumblocks(); 
//				int rbnparamindx = 0;
//				if (rbnparameters.length>0)
//					rbnparamindx =1;
//				paramblocks = new String[numparamblocks*nrelparamblocks.length+rbnparamindx][];
//
//				/* First the RBN parameters:*/
//				if (rbnparamindx == 1)
//					paramblocks[0]=rbnparameters;
//
//				int blocklength;
//				int lastblocklength;
//				int excess;
//				int offset =0;
//				int addone=0;

				//					for (int i=rbnparamindx;i<nrelparamblocks.length;i++){
				//						blocklength = nrelparamblocks[i].length/numparamblocks;
				//						for (int j=0;j<numparamblocks-1;j++){
				//							paramblocks[i*numparamblocks+j]=new String[blocklength];
				//							for (int h=0;h<blocklength;h++)
				//								paramblocks[i*numparamblocks+j][h]=nrelparamblocks[i][j*blocklength+h];
				//						}
				//						lastblocklength = nrelparamblocks[i].length-(numparamblocks-1)*blocklength;
				//						paramblocks[(i+1)*(numparamblocks)-1]=new String[lastblocklength];
				//						for (int h=0;h<lastblocklength;h++){
				//							paramblocks[(i+1)*(numparamblocks)-1][h]=nrelparamblocks[i][blocklength*(numparamblocks-1)+h];
				//						}
				//					}


//				for (int i=rbnparamindx;i<=nrelparamblocks.length;i++){
//					/* Divide evenly over the paramblocks; First ones may get one extra */
//					blocklength = nrelparamblocks[i-1].length/numparamblocks;
//					excess = nrelparamblocks[i-1].length - numparamblocks*blocklength;
//					offset = 0;
//					for (int j=0;j<numparamblocks;j++){
//						if (excess > 0){
//							addone = 1;
//							excess--;
//						}
//						paramblocks[i*numparamblocks+j]=new String[blocklength+addone];
//						for (int h=0;h<blocklength+addone;h++)
//							paramblocks[i*numparamblocks+j][h]=nrelparamblocks[i-1][offset+h];
//						offset = offset+blocklength+addone;
//						addone=0;
//					}
//
//				}
//
//			}

			/* Now we are really getting started */
			long timestart = System.currentTimeMillis();


			/* Current best log-likelihoods represented as pairs of 
			 * doubles (for use with SmallDouble methods)
			 */
			double currentbestlik = Double.NEGATIVE_INFINITY;
			double newlik;

			/* The sum of likelihood values obtained in several restarts.
			 * Used for pure likelihood computation only (computeLikOnly = true)
			 */
			//				double[] liksum = new double[2];

			/* rest is the number of completed restarts */
			int rest = 0;
			double[] results=null;

			GradientGraphO gg = null;
			if (threadascentstrategy == LearnModule.AscentBatch || 
					threadascentstrategy == LearnModule.AscentTwoPhase 
					//||(threadascentstrategy == LearnModule.AscentAdam && myLearnModule.getKeepGGs())
					) {
				/* In this case can build one gg once and for all outside the restart-loop */
				gg = buildGGO(parameters,minmaxbounds,true,databatches[0]);
				//gg.showAllNodes(6, A);
			}


			long beforerestarts = System.currentTimeMillis();
			
			while (!isstopped() && (rest < myLearnModule.getRestarts() 
					|| myLearnModule.getRestarts() == -1)){

				System.out.println("# ***** RESTART **********");
				
				/* 
				 * The RelStruc A only is needed for the case of learning NumRels
				 * In this case, only a single input domain is allowed, and alldata.caseAt(0) returns
				 * what we need.
				 */
				RelStruc A = alldata.caseAt(0).inputDomain();
				try {
					switch (threadascentstrategy){
					case LearnModule.AscentBatch:
						results = doOneRestartBatch(gg,A,parameternumrels,parameters,rest==0);
						System.out.println("Final: " +rbnutilities.arrayToString(results));
						break;
					case LearnModule.AscentAdam:
						results = doOneRestartStochGrad(A,parameternumrels,parameters,minmaxbounds,
								rest==0,myLearnModule.getUseGGs(),profiler);
						System.gc();
						break;
//					case LearnModule.AscentBlock:
//						results = doOneRestartBlock(A,parameternumrels,parameters,paramblocks,rest==0);
//						break;
//					case LearnModule.AscentTwoPhase:
//						results = doOneRestartBatch(gg,A,parameternumrels,parameters,rest==0);
					}
				}
				catch (RBNCompatibilityException e) {System.out.println(e);}
				catch (RBNNaNException e) {System.out.println(e);}

				newlik = results[results.length-1];
				System.out.println("# Likelihood: " + newlik);
				
				
				if (newlik > currentbestlik){
					currentbestlik = newlik;
					for (int i=0;i<parameters.size();i++)
						paramvals[i] = results[i];
					paramvals[paramvals.length-1]=results[results.length-1];
					parammodel.setEstimates(paramvals);
				}
				rest++;
				numrestartsfield.setText(""+rest);					
				parametertable.updateUI();
			} // while (!isstopped() && (rest < myLearnModule.getRestarts() 

			profiler.addTime(Profiler.TIME_RESTARTS, System.currentTimeMillis()-beforerestarts);
			profiler.addTime(Profiler.TIME_STOCH_GRAD, System.currentTimeMillis()-timestart);
			
			System.out.println(profiler.showTimers());

//			System.out.println("# Time per mini batch: " + 
//			profiler.getTime(Profiler.TIME_STOCH_GRAD)/(profiler.getTime(Profiler.NUM_EPOCHS)*databatches.length));
			
		} //	if (databatches != null)
		System.out.println("# ***** END **********");
		return;
	}



	private double[] doOneRestartStochGrad(RelStruc A,
			String[][] parameternumrels,
			Hashtable<String,Integer> parameters,
			double[][] minmaxbounds,
			Boolean isfirstrestart,
			Boolean usegradientgraphs,
			Profiler profiler)
					throws RBNNaNException,RBNCompatibilityException
	{
		Boolean isfirstloop = true;

		GradientGraphO gg = null;
		GradientGraphO[] allggs = null;
		if (myLearnModule.getUseGGs())
			allggs = new GradientGraphO[databatches.length];
		
		double[] bestresult = new double[parameters.size()+4];
		double[] gradient = new double[parameters.size()];
		double[] newparamvals = new double[parameters.size()];
		double[] oldparamvals = new double[parameters.size()];
		double[] beforeepochparamvals = new double[parameters.size()];
		
		if (!initParams(A,databatches[0],parameternumrels,null))
			return null;
	
		
		/* Initialize ascent strategy specific variables:*/
		switch(myLearnModule.threadascentstrategy()){	
		case LearnModule.AscentStochHeur1:
			dampfac = myLearnModule.getDampingFac();
			avec = new double[parameters.size()];
			break;
		case LearnModule.AscentAdam:
			firstmomentest = new double[parameters.size()];
			secondmomentest = new double[parameters.size()];
			beta1 = myLearnModule.adam_beta1();
			beta2 = myLearnModule.adam_beta2();
			epsilon = myLearnModule.adam_epsilon();
			alpha = myLearnModule.adam_alpha();
			System.out.println("# epsilon: " + epsilon + "  alpha: " +alpha);


		}

		double epochobj = Double.NEGATIVE_INFINITY;
		double[] epochconfusion = null; 
		double batchaccuracy;
		double batchobj = Double.NEGATIVE_INFINITY;
		double[] batchconfusion = null; 
		double lastobj = Double.NEGATIVE_INFINITY;

		boolean terminate = false;
		int itcount = 0;
		int batchcount = 0;


		int tries = 0;
		double bestobj = Double.NEGATIVE_INFINITY;

		switch (myLearnModule.threadascentstrategy()){
		case LearnModule.AscentStochHeur1:
			System.out.println("# Iteration  scalefac  stepsize   likelihood" );
			break;
		case LearnModule.AscentAdam:
			
			System.out.println("# Iteration" + '\t' + "Time (Epoch)"+ '\t' + "Time (Total)"+ '\t' +  "stepsize"   + '\t' +  "objective"+ '\t' +  "no progress (out of " + myLearnModule.getLikelihoodWindow() +")");
			break;

		}


		long startiterations = System.currentTimeMillis();
		
		while (!terminate && !isstopped()){
//			System.gc();
			itcount++;
			switch (myLearnModule.threadascentstrategy()){
			case LearnModule.AscentStochHeur1:
				asums = new double[parameters.size()];
				incrementvec = new double[parameters.size()];
				break;
			}


			epochobj =0;
			epochconfusion = new double[4];
			long startepoch = System.currentTimeMillis();
			
			for (int i=0;i<databatches.length && !isstopped();i++){
				if (usegradientgraphs) {
					// if (myLearnModule.getUseGGs()) {
						if (isfirstloop) {
							gg = buildGGO(parameters,minmaxbounds,
									isfirstrestart && isfirstloop,databatches[i]);
							System.out.println("done build GG");
							gg.evaluateLikelihoodAndPartDerivs(false);
							System.out.println("done evaluate GG");

							allggs[i]=gg;
						}
						else {
							gg=allggs[i];
							gg.setParametersFromAandRBN();
							gg.resetValues(null, false);
							gg.evaluateLikelihoodAndPartDerivs(false);
						}
//					}
//					else
//						gg = buildGGO(parameters,isfirstrestart && isfirstloop,databatches[i], myLearnModule.getObjective());
				}

				oldparamvals=myprimula.getParameterVals(parameters);
				
//				System.out.println("current parameters");
//				for (String p: parameters.keySet()) {
//					int pidx=parameters.get(p);
//					System.out.println(p + oldparamvals[pidx]);
//				}
//				
				if (isfirstloop)
					beforeepochparamvals=oldparamvals.clone();
				
				// At the moment only option Adam implemented in stochastic gradient !
				switch (myLearnModule.threadascentstrategy()){
				case LearnModule.AscentAdam:
					if (usegradientgraphs) {
						gradient = gg.getGradient();
						batchobj=gg.currentLogLikelihood();
//						batchconfusion = gg.getConfusionDouble();
//						batchaccuracy = gg.getAccuracy();
//						for (int j=0;j<parameters.length;j++)
//							System.out.println(parameters[j] + "  "  + gradient[j] + "  " + lossgrad[2][j]);
//						System.out.println();
					}
					else {
						
						double[][] lossgrad = getLossAndGradient(databatches[i],
								myprimula.getRBN(),
								parameters,
								false,
								profiler);

						gradient = lossgrad[1];
						batchobj= lossgrad[0][0];
					}
					epochobj+=batchobj;

					firstmomentest = rbnutilities.arrayAdd(
							rbnutilities.arrayScalMult(firstmomentest, beta1), 
							rbnutilities.arrayScalMult(gradient,1- beta1));
					/* update biased second raw moment estimate */
					secondmomentest = rbnutilities.arrayAdd(
							rbnutilities.arrayScalMult(secondmomentest,  beta2), 
							rbnutilities.arrayScalMult(rbnutilities.arrayCompMultiply(gradient, gradient),1- beta2));
					/* Compute bias-corrected first moment estimate: 
					 * We are here using itcount as the time counter. Thus, "time"
					 * is incremented only after completion of a full run through
					 * all data batches, not after every data batch */
					mhat = rbnutilities.arrayScalMult(firstmomentest, 1/(1-Math.pow( beta1,itcount)));
					//System.out.println("mhat: " + rbnutilities.arrayToString(mhat, 0, 10));
					
					/* Compute bias-corrected second moment estimate: */
					vhat = rbnutilities.arrayScalMult(secondmomentest, 1/(1-Math.pow( beta2,itcount)));
					//System.out.println("vhat: " + rbnutilities.arrayToString(vhat, 0, 10));
					incrementvec = rbnutilities.arrayCompDivide(mhat, 
							rbnutilities.arrayAddConst(rbnutilities.arraySQRT(vhat), epsilon));
					
					
					
//					System.out.print("cosine(increment,gradient) " 
//					+ rbnutilities.arrayDotProduct(rbnutilities.normalizeDoubleArray(incrementvec),rbnutilities.normalizeDoubleArray(gradient))); 
					
					/* Proper ADAM: */
					newparamvals = rbnutilities.arrayAdd(oldparamvals,
							rbnutilities.arrayScalMult(incrementvec,alpha));
					newparamvals=rbnutilities.clip(newparamvals,minmaxbounds);
					
//					System.out.println("old" +'\t' + "grad" + '\t' + "incr" + '\t' + "new");
//					for (int ii=0;ii<gradient.length;ii++)
//						System.out.println(oldparamvals[ii]+ "\t" + gradient[ii]+ "\t" + incrementvec[ii] + "\t" + newparamvals[ii]);

					//System.out.println(itcount + "\t" + rbnutilities.euclidDist(oldparamvals, newparamvals) + "\t" +   batchobj + "\t" + batchaccuracy );
					myprimula.setParameters(parameters,newparamvals);
					if (usegradientgraphs)
						gg.setParametersFromAandRBN();
				} // switch (myLearnModule.threadascentstrategy()){

				batchcount++;
			} // end for (int i=0;i<databatches.length;i++){

			profiler.addTime(Profiler.NUM_EPOCHS, 1);
			
			if (rbnutilities.hasNaNValues(newparamvals)){
				terminate = true;
				System.out.println("Warning: NaN values in current parameter values; terminate stochastic gradient");
			}

					
			if (epochobj > bestobj){
				bestobj = epochobj;
				bestresult = Arrays.copyOf(newparamvals, newparamvals.length+4);
			}
			if (lastobj<epochobj && Math.abs((epochobj-lastobj)/lastobj)> myLearnModule.getLLikThresh())
				tries = 0;
			else
				tries ++;

	
			switch (myLearnModule.threadascentstrategy()){
			case LearnModule.AscentStochHeur1:
				
				System.out.println(itcount + "     " + scalefac + "  " 
						+ rbnutilities.euclidDist(oldparamvals, newparamvals) +"  "  +  epochobj );
				break;
			case LearnModule.AscentAdam:
				long tick = System.currentTimeMillis();
				long totalt = tick-startiterations;
				long epocht = tick - startepoch;
				
				System.out.println(itcount + "\t" +  epocht +'\t' + totalt + "\t" + rbnutilities.euclidDist(beforeepochparamvals, newparamvals) 
				+ "\t" +   epochobj + "\t" + tries);
				break;

			}
		

			lastobj = epochobj;
			beforeepochparamvals = newparamvals.clone();
			isfirstloop = false;
			terminate = (terminate || tries == myLearnModule.getLikelihoodWindow() || itcount == myLearnModule.getMaxIterations());
		} // while (!terminate && !isstopped())

		
		System.out.println("#Iterations: " + batchcount);
		/* Don't use 3 components of resultvector here */
		bestresult[bestresult.length-4]=Double.NaN;
		bestresult[bestresult.length-3]=Double.NaN;
		bestresult[bestresult.length-2]=Double.NaN;
		bestresult[bestresult.length-1]=bestobj;
		return bestresult;

	}




	private double[] doOneRestartBatch(GradientGraph gg,
			RelStruc A,
			String[][] parameternumrels,
			Hashtable<String,Integer> parameters,
			Boolean isfirstrestart)
					throws RBNNaNException,RBNCompatibilityException
	{

		if (initParams(A,alldata,parameternumrels,null)){
			gg.setParametersFromAandRBN();
			return gg.learnParameters(this,GradientGraph.FullLearn,true);
		}
		else{
			System.out.println("Failed to find initial parameter setting");
			return null;
		}
	}

	/*
	 * 
	 * This should be completely redone. Creating one big GG for all parameters, and implementing
	 * a block gradient ascent using an implementation of learnParameters that takes a subset 
	 * of parameters as input
	 */
//	private double[] doOneRestartBlock(RelStruc A,
//			String[][] parameternumrels,
//			String[] parameters,
//			String[][] paramblocks,
//			Boolean isfirstrestart)
//					throws RBNNaNException,RBNCompatibilityException
//	{
//
//		if (!initParams(A,alldata,parameternumrels))
//			return null;
//		else{
//			GradientGraphO gg;
//			Boolean isfirstloop = true;
//			double[] paramvals = new double[parameters.length+4];
//			double[] newparamvals = new double[parameters.length+4];
//			double currlik = Double.NEGATIVE_INFINITY;
//			double lastlik = Double.NEGATIVE_INFINITY;
//			double likgain;
//			Boolean terminate = false;
//			double[] blockresult = new double[0];
//			int offset;
//			GradientGraphO[] allggs = null;
//			if (myLearnModule.getKeepGGs()) {
//				allggs = new GradientGraphO[paramblocks.length];
//			}
//
//			while (!terminate && !isstopped()){
//				offset = 0;
//				for (int i=0;i<paramblocks.length && !isstopped();i++){
//					gg = null;
//					if (!myLearnModule.getKeepGGs() || isfirstloop) {
//						gg = buildGGO(paramblocks[i],isfirstrestart && isfirstloop,databatches[0],myLearnModule.getObjective());
//						if (myLearnModule.getKeepGGs() && isfirstloop)
//							allggs[i]=gg;
//					}			
//					else {
//						gg=allggs[i];
//						gg.setParametersFromAandRBN();
//					}
//
//					blockresult = gg.learnParameters(this,GradientGraph.FullLearn,false);
//					myprimula.setParameters(paramblocks[i], 
//							Arrays.copyOfRange(blockresult,0,blockresult.length-4));
//
//					/* Insert learned block of values in full array*/
//					for (int j=0; j<paramblocks[i].length; j++)
//						paramvals[offset+j]=blockresult[j];
//					offset = offset + paramblocks[i].length;
//					//System.out.println("blocklik " + i + " : " + blockresult[blockresult.length-1] + StringOps.arrayToString(paramblocks[i], "[", "]"));
//					System.out.println("blocklik " + i + " : " + blockresult[blockresult.length-1] + "   " + paramblocks[i][0]);
//				}
//				currlik = blockresult[blockresult.length-1];
//				likgain=Math.abs(1-lastlik/currlik);
//				System.out.println("currlik: " + currlik + " gain: " + likgain + " " + myLearnModule.getLLikThresh()) ;
//				if ( likgain < myLearnModule.getLLikThresh())
//					terminate = true; 
//				lastlik = currlik;
//				isfirstloop = false;
//			}
//			System.out.println();
//			for (int i=0;i<4;i++)
//				paramvals[parameters.length+i]=blockresult[blockresult.length-4+i];
//			return paramvals;
//		}
//	}


	private GradientGraphO buildGGO(Hashtable<String,Integer> parameters,double[][] minmaxbounds,
			Boolean showInfoInPrimula,RelData datafold){
		GradientGraphO gg = null;
		if (showInfoInPrimula && myprimula.getPrimulaGUI() != null)
			myprimula.getPrimulaGUI().showMessageThis("Building Gradient Graph ...");
		double timestart=System.currentTimeMillis();
		try{
			gg = new GradientGraphO(myprimula,
					datafold,
					parameters,
					minmaxbounds,
					myLearnModule, 
					null,
					GradientGraphO.LEARNMODE,
					showInfoInPrimula);
		}
		catch (RBNCompatibilityException ex){System.out.println(ex);}
		double timediff = (System.currentTimeMillis()-timestart)/1000;

		if (showInfoInPrimula && myprimula.getPrimulaGUI() != null)
			myprimula.getPrimulaGUI().showMessageThis("Construction time: " + timeformat.format(timediff) +"s" +'\n' );
		return gg;
	}



//	private double[] getDataLikelihood(RelData data)
//			throws RBNNaNException{
//		GradientGraphO gg = buildGGO(null, false, data, myLearnModule.getObjective());
//		return gg.computeObjectiveandConfusion(this);
//	}

	
	/* Find initial parameter settings such that the likelihood
	 * of data is nonzero
	 */
	private boolean initParams(RelStruc A, RelData data, String[][] parameternumrels,Profiler profiler)
			throws RBNNaNException,RBNCompatibilityException
	{
		double scale = 0.1;
		boolean success = false;
		int tries =0;
		double lik;
		while (!success && tries < 20){
			System.out.println("# Try init with scale " + scale);		
			myprimula.getRBN().setRandomParameterVals();
			if (myLearnModule.ggrandominit())
				A.setRandom(parameternumrels,scale);
			lik = getLossAndGradient(data, myprimula.getRBN(), new Hashtable<String,Integer>(),true ,profiler)[0][0];
			System.out.println("# log-likelihood " + lik);	
			if (lik == Double.NEGATIVE_INFINITY){
				tries++;
				scale=0.5*scale;
			}
			else
				success = true;
			success = true;
		}
		return success;
	}

	/*
	 * Only applicable when data is complete! No dependence of observed data atoms on unobserved atoms.
	 */
	private double[][] getLossAndGradient(RelData data, 
			RBN rbn, 
			Hashtable<String,Integer> parameters, 
			boolean lossonly, 
			Profiler profiler) 
			throws RBNCompatibilityException
	{
		//System.out.println("debug: getLossAndGradient for data with cases: " + data.size());
		double[][] result = new double[2][];
		result[0] = new double[1]; // for the likelihood value -- may need to be changed to double[2] if SmallDoubles are needed for plain likelihood objective
		if (!lossonly)
			result[1] = new double[parameters.size()]; // for the gradient
		else 
			result[1] = new double[0];


		Object[] lg;
		double pval =0;
		Gradient grad = null;
		OneStrucData osd;
		
		for (int inputcaseno=0; inputcaseno<data.size(); inputcaseno++){
		 	RelDataForOneInput rdoi = data.caseAt(inputcaseno);
		 	RelStruc A = rdoi.inputDomain();
		 	for (int observcaseno=0; observcaseno<rdoi.numObservations(); observcaseno++){
		 		osd = rdoi.oneStrucDataAt(observcaseno);

				Hashtable<String,Object[]>  evaluated = null;
				if (myLearnModule.getUseMemoize())
					evaluated = new Hashtable<String,Object[]>();
				for (int i=0; i<rbn.NumPFs(); i++){
					CPModel nextcpm = rbn.cpmod_prelements_At(i);
					String[] vars = rbn.arguments_prels_At(i);
					Rel nextrel = rbn.relAt(i);
					Vector<int[]> inrel = osd.allInstantiated(nextrel);
					for (int[] tuple: inrel) {
						int val = (int)osd.valueOf(nextrel,tuple);
						lg = nextcpm.evaluate(A, 
								osd, 
								vars, 
								tuple, 
								val, 
								true,  
								true, 
								null,
								false,
								evaluated,
								parameters,
								myLearnModule.getType_of_gradient(),
								lossonly,
								profiler);
					
					
					
//							System.out.println("tuple: " +rbnutilities.arrayToString(tuple) + "value: " + val);
//							for (String p: parameters.keySet()) {
//								int pidx=parameters.get(p);
//								System.out.println(p + ((double[])lg[1])[pidx]);
//							}
			                // Getting the actual probability of the ground atom nextrel(tuple):
							
							if (nextrel instanceof BoolRel) {
								if (val == 1) 
									pval = (double)lg[0];
								else 
									pval = 1- (double)lg[0];
							}
							if (nextrel instanceof CatRel) {
								pval = ((double[])lg[0])[val];
							}

							result[0][0]+=Math.log(pval);


							if (!lossonly) {
								grad = (Gradient)lg[1];
								double pdp; // partial derivative for parameter p
								for (String par: parameters.keySet()){
									pdp=grad.get_part_deriv(par)[0];
									if (nextrel instanceof BoolRel && val==0)
										pdp*=-1;
									int ii = parameters.get(par);
									result[1][ii]+=pdp/pval;
								}
//								if (myLearnModule.getType_of_gradient()== ProbForm.RETURN_ARRAY) {
//									for (int ii=0; ii<parameters.size(); ii++) {
//										pdp = ((double[])grad)[ii];
//										if (nextrel instanceof BoolRel && val==0)
//											pdp*=-1;
//										result[1][ii]+=pdp/pval;
//									}
//								}
//								else { //ProbForm.RETURN_SPARSE
//									for (String par: ((Hashtable<String,Double>)grad).keySet()){
//										pdp = ((Hashtable<String,Double>)grad).get(par);
//										if (nextrel instanceof BoolRel && val==0)
//											pdp*=-1;
//										int ii = parameters.get(par);
//										result[1][ii]+=pdp/pval;
//									} //for par:
//								} //ProbForm.RETURN_SPARSE
							} // if (!lossonly)
					} // for (int[] tuple: inrel) {
				} // for (int i=0; i<rbn.NumPFs(); i++){
		 	} // for (int observcaseno=0; observcaseno<rdoi.numObservations(); observcaseno++){
		} // for (int inputcaseno=0; inputcaseno<data.size(); inputcaseno++){
	
		return result;
	}
 		
	private double[] linesearch(
			double[] current, 
			double[] gradient, 
			RelData data, 
			Hashtable<String,Integer> parameters,
			RBN rbn, 
			int lossfunc, 
			Profiler profiler) {
		
		return null;
	}
}
