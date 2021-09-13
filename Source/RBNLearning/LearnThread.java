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
		//		ggascentstrategy = myLearnModule.ggascentstrategy();
	}

	public void run() throws RBNRuntimeException 
	{
		if (databatches != null){

			double timestart,timediff,timeperrs;


			/* Determines the parameters contained in the rbn model. */
			String[] rbnparameters = myprimula.getRBN().parameters();

			/* Numerical relations to be learned, divided into blocks
			 * Some relations may also be given by enumeration of their ground
			 * atoms
			 * 
			 */
			String[][] parameternumrels = myprimula.getParamNumRels();

			/* All parameters to be optimized (RBN params and ground numerical relation atoms)*/
			String[] parameters = new String[0];
			/* The ground numerical relation atoms divided into blocks corresponding to the blocks in paramnumrels */
			String[][] nrelparamblocks =new String[parameternumrels.length][];

			/* Cannot handle learning numerical input relations for
			 * data with multiple input domains: check this and throw
			 * exception
			 */
			if (parameternumrels[0].length > 0 && alldata.size()>1)
				throw new RBNRuntimeException("Cannot handle learning numerical relations with multiple input domains");

			/* Construct the parameters corresponding to ground numrel atoms */
			String[] nrparams = new String[0];
			RelDataForOneInput rdoi = databatches[0].caseAt(0);
			RelStruc A = rdoi.inputDomain();
			String nextp;
			String[] nextparams;
			for (int i=0;i<parameternumrels.length;i++){
				nrelparamblocks[i] = new String[0];
				for (int j=0;j<parameternumrels[i].length;j++){
					nextp = parameternumrels[i][j];
					// The following a bit crude: distinguish relation names from ground atoms
					// just by occurrence of "("
					if (!nextp.contains("(")){
						Vector<String[]> alltuples = A.allTrue(nextp,A);
						nextparams = new String[alltuples.size()];
						for (int k=0;k<nextparams.length;k++)
							nextparams[k]=parameternumrels[i][j]+StringOps.arrayToString(alltuples.elementAt(k),"(",")");
					}
					else{
						nextparams = new String[1];
						nextparams[0]=nextp;
					}
					nrelparamblocks[i]=	rbnutilities.arrayConcatenate(nrelparamblocks[i],nextparams);
					nrparams = rbnutilities.arraymerge(nrparams,nextparams);
				}
			}
			parameters = rbnutilities.arrayConcatenate(rbnparameters, nrparams);
			Arrays.sort(parameters);

			parammodel.setParameters(parameters);
			parammodel.fireTableDataChanged();
			parametertable.updateUI();
			// boolean computeLikOnly = (parameters.length == 0);

			numrestartsfield.setText("" );
			double[] paramvals = new double[parameters.length+1];

			if (threadascentstrategy == LearnModule.AscentBlock){
				/* Divide parameters into blocks for block gradient descent 
				   Each of the blocks defined in nrelparamblocks is divided into
				   numparamblocks sub-blocks. The RBN parameters form their own
				   block.
				 */
				int numparamblocks = myLearnModule.getNumblocks(); 
				int rbnparamindx = 0;
				if (rbnparameters.length>0)
					rbnparamindx =1;
				paramblocks = new String[numparamblocks*nrelparamblocks.length+rbnparamindx][];

				/* First the RBN parameters:*/
				if (rbnparamindx == 1)
					paramblocks[0]=rbnparameters;

				int blocklength;
				int lastblocklength;
				int excess;
				int offset =0;
				int addone=0;

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


				for (int i=rbnparamindx;i<=nrelparamblocks.length;i++){
					/* Divide evenly over the paramblocks; First ones may get one extra */
					blocklength = nrelparamblocks[i-1].length/numparamblocks;
					excess = nrelparamblocks[i-1].length - numparamblocks*blocklength;
					offset = 0;
					for (int j=0;j<numparamblocks;j++){
						if (excess > 0){
							addone = 1;
							excess--;
						}
						paramblocks[i*numparamblocks+j]=new String[blocklength+addone];
						for (int h=0;h<blocklength+addone;h++)
							paramblocks[i*numparamblocks+j][h]=nrelparamblocks[i-1][offset+h];
						offset = offset+blocklength+addone;
						addone=0;
					}

				}

			}

			/* Now we are really getting started */
			timestart = System.currentTimeMillis();


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
				gg = buildGGO(parameters,true,databatches[0],myLearnModule.getObjective());
				//gg.showAllNodes(6, A);
			}


			while (!isstopped() && (rest < myLearnModule.getRestarts() 
					|| myLearnModule.getRestarts() == -1)){

				System.out.println("# ***** RESTART **********");
				try {
					switch (threadascentstrategy){
					case LearnModule.AscentBatch:
						results = doOneRestartBatch(gg,A,parameternumrels,parameters,rest==0);
						break;
					case LearnModule.AscentAdam:
						Vector<double[]> adamparams = new Vector<double[]>();
						// Pairs of adam (epsilon,alpha) parameters
//						double addp[][] = {{1.0E-8,0.01},{1.0E-6,0.01},{1.0E-4,0.01}};
//						for (int i=0;i<addp.length;i++) {
//							myLearnModule.setAdamEpsilon(addp[i][0]);
//							myLearnModule.setAdamAlpha(addp[i][1]);
//							System.out.println("Epsilon: " + addp[i][0] + "  Alpha: " + addp[i][1]);
//							results = doOneRestartStochGrad(A,parameternumrels,parameters,rest==0);
//						}
						results = doOneRestartStochGrad(A,parameternumrels,parameters,rest==0);
						break;
					case LearnModule.AscentBlock:
						results = doOneRestartBlock(A,parameternumrels,parameters,paramblocks,rest==0);
						break;
					case LearnModule.AscentTwoPhase:
						results = doOneRestartBatch(gg,A,parameternumrels,parameters,rest==0);
					}

					newlik = results[results.length-1];
					System.out.println("# Likelihood: " + newlik);

					if (newlik > currentbestlik){
						currentbestlik = newlik;
						for (int i=0;i<parameters.length;i++)
							paramvals[i] = results[i];
						paramvals[paramvals.length-1]=results[results.length-1];
						parammodel.setEstimates(paramvals);
					}
					rest++;
					numrestartsfield.setText(""+rest);					
					parametertable.updateUI();
				}
				catch (RBNNaNException e) 
				{
					System.out.println(e);
					System.out.println("Restart aborted");
				}
			}


			timediff = System.currentTimeMillis()-timestart;
			timeperrs = timediff/(1000*(rest+1));
			myprimula.showMessageThis("done. Time per restart: " + timeformat.format(timeperrs) +"s");
			
		}	
	}


	private double[] doOneRestartStochGrad(RelStruc A,
			String[][] parameternumrels,
			String[] parameters,
			Boolean isfirstrestart)
					throws RBNNaNException
	{

		
		if (!initParams(A,alldata,parameternumrels))
			return null;

		Boolean isfirstloop = true;

		GradientGraphO gg;
		GradientGraphO[] allggs = null;
		if (myLearnModule.getKeepGGs())
			allggs = new GradientGraphO[databatches.length];
		
		double[] bestresult = new double[parameters.length+4];
		double[] gradient = new double[parameters.length];
		double[] newparamvals = new double[parameters.length];
		double[] oldparamvals = new double[parameters.length];
		double[] beforeepochparamvals = new double[parameters.length];

		/* Initialize ascent strategy specific variables:*/
		switch(myLearnModule.threadascentstrategy()){	
		case LearnModule.AscentStochHeur1:
			dampfac = myLearnModule.getDampingFac();
			avec = new double[parameters.length];
			break;
		case LearnModule.AscentAdam:
			firstmomentest = new double[parameters.length];
			secondmomentest = new double[parameters.length];
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
		int itcount = 1;


		int tries = 0;
		double bestobj = Double.NEGATIVE_INFINITY;

		switch (myLearnModule.threadascentstrategy()){
		case LearnModule.AscentStochHeur1:
			System.out.println("# Iteration  scalefac  stepsize   likelihood" );
			break;
		case LearnModule.AscentAdam:
			
			System.out.println("# Iteration" + '\t' +  "stepsize"  + '\t' +  "objective" + '\t' + "accuracy");
			break;

		}


		long timestart = System.currentTimeMillis();
		
		while (!terminate && !isstopped()){

			switch (myLearnModule.threadascentstrategy()){
			case LearnModule.AscentStochHeur1:
				asums = new double[parameters.length];
				incrementvec = new double[parameters.length];
				break;
			}


			epochobj =0;
			epochconfusion = new double[4];
			
			for (int i=0;i<databatches.length && !isstopped();i++){
				
				
				if (myLearnModule.getKeepGGs()) {
					if (isfirstloop) {
						gg = buildGGO(parameters,isfirstrestart && isfirstloop,databatches[i], myLearnModule.getObjective());
						allggs[i]=gg;
					}
					else {
						gg=allggs[i];
						gg.setParametersFromAandRBN();
						gg.evaluateLikelihoodAndPartDerivs(false);
					}
				}
				else
					gg = buildGGO(parameters,isfirstrestart && isfirstloop,databatches[i], myLearnModule.getObjective());


				oldparamvals = gg.currentParameters();
				if (isfirstloop)
					beforeepochparamvals=oldparamvals.clone();
				
				//System.out.println(rbnutilities.arrayToString(oldparamvals,0,10));
				switch (myLearnModule.threadascentstrategy()){
				case LearnModule.AscentStochHeur1:
					//					gradient = gg.getGradient();
					//					resultvec = gg.learnParameters(this,GradientGraph.OneLineSearch,false);
					//					resultvec = Arrays.copyOfRange(resultvec, 0, resultvec.length-4);
					//					for (int j=0;j<parameters.length;j++){
					//						avec[j] = gradient[j]/(2*(resultvec[j]-oldparamvals[j]));
					//						asums[j] = asums[j] + avec[j];
					//					}
					//					incrementvec = rbnutilities.arrayAdd(incrementvec, rbnutilities.arrayCompMultiply(resultvec,avec));
					//					break;
					resultvec = gg.learnParameters(this,GradientGraph.OneLineSearch,false);
					resultvec = Arrays.copyOfRange(resultvec, 0, resultvec.length-4);
					myprimula.setParameters(parameters,resultvec);
					break;
				case LearnModule.AscentAdam:
					gradient = gg.getGradient();
					batchobj=gg.currentLogLikelihood();
					epochobj+=batchobj;
					batchconfusion = gg.getConfusionDouble();
					batchaccuracy = gg.getAccuracy();
					epochconfusion = rbnutilities.arrayAdd(epochconfusion, batchconfusion);
					
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
					//System.out.println("increment: " + rbnutilities.arrayToString(incrementvec, 0, 20));
					//System.out.print("last/current increment: " + rbnutilities.arrayDotProduct(rbnutilities.normalizeDoubleArray(incrementvec), 
					
					/* Proper ADAM: */
					newparamvals = rbnutilities.arrayAdd(oldparamvals,
							rbnutilities.arrayScalMult(incrementvec,alpha));
					newparamvals=gg.clipToFeasible(newparamvals);
					
//					/* ADAM with linesearch: */
//					newparamvals = gg.linesearch(oldparamvals, incrementvec, this);
//					
					
					System.out.println(itcount + "\t" + rbnutilities.euclidDist(oldparamvals, newparamvals) + "\t" +   batchobj + "\t" + batchaccuracy );
					myprimula.setParameters(parameters,newparamvals);
				}

			} // end for (int i=0;i<databatches.length;i++){



			//			switch (myLearnModule.threadascentstrategy()){
			//			case LearnModule.AscentStochHeur1:
			//				scalefac = Math.pow(dampfac, itcount);
			//				for (int j=0;j<parameters.length;j++)
			//					incrementvec[j] = incrementvec[j]/asums[j];
			//				newparamvals = rbnutilities.arrayConvComb( incrementvec, oldparamvals, scalefac);
			//				myprimula.setParameters(parameters,newparamvals);
			//				break;
			//			}

			if (rbnutilities.hasNaNValues(newparamvals)){
				terminate = true;
				System.out.println("Warning: NaN values in current parameter values; terminate stochastic gradient");
			}

//			double[] allobjs = getDataLikelihood(alldata);
//			
//			
//			
//			currobj = allobjs[1];
//			double curracc = (allobjs[2]+allobjs[5])/(allobjs[2]+allobjs[3]+allobjs[4]+allobjs[5]);
			
			double curracc = (epochconfusion[0]+epochconfusion[3])/(epochconfusion[0]+epochconfusion[1]+epochconfusion[2]+epochconfusion[3]);
					
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
//				if (lastobj>currobj) {
//					alpha=alpha/2;
//					System.out.println("New alpha: " + alpha);
//				}
				System.out.println(itcount + "\t" + rbnutilities.euclidDist(beforeepochparamvals, newparamvals) + "\t" +   epochobj + "\t" + curracc );
				break;

			}

			itcount++;

			lastobj = epochobj;
			beforeepochparamvals = newparamvals.clone();
			isfirstloop = false;
			terminate = (terminate || tries == myLearnModule.getLikelihoodWindow() || itcount == myLearnModule.getMaxIterations());
		}

		long timeend = System.currentTimeMillis();
		
		System.out.println("# Time per iteration: " + (timeend-timestart)/itcount);
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
			String[] parameters,
			Boolean isfirstrestart)
					throws RBNNaNException
	{

		if (initParams(A,alldata,parameternumrels)){
			gg.setParametersFromAandRBN();
			return gg.learnParameters(this,GradientGraph.FullLearn,true);
		}
		else{
			System.out.println("Failed to find initial parameter setting");
			return null;
		}
	}

	private double[] doOneRestartBlock(RelStruc A,
			String[][] parameternumrels,
			String[] parameters,
			String[][] paramblocks,
			Boolean isfirstrestart)
					throws RBNNaNException
	{

		if (!initParams(A,alldata,parameternumrels))
			return null;
		else{
			GradientGraphO gg;
			Boolean isfirstloop = true;
			double[] paramvals = new double[parameters.length+4];
			double[] newparamvals = new double[parameters.length+4];
			double currlik = Double.NEGATIVE_INFINITY;
			double lastlik = Double.NEGATIVE_INFINITY;
			double likgain;
			Boolean terminate = false;
			double[] blockresult = new double[0];
			int offset;
			GradientGraphO[] allggs = null;
			if (myLearnModule.getKeepGGs()) {
				allggs = new GradientGraphO[paramblocks.length];
			}

			while (!terminate && !isstopped()){
				offset = 0;
				for (int i=0;i<paramblocks.length && !isstopped();i++){
					gg = null;
					if (!myLearnModule.getKeepGGs() || isfirstloop) {
						gg = buildGGO(paramblocks[i],isfirstrestart && isfirstloop,databatches[0],myLearnModule.getObjective());
						if (myLearnModule.getKeepGGs() && isfirstloop)
							allggs[i]=gg;
					}			
					else {
						gg=allggs[i];
						gg.setParametersFromAandRBN();
					}

					blockresult = gg.learnParameters(this,GradientGraph.FullLearn,false);
					myprimula.setParameters(paramblocks[i], 
							Arrays.copyOfRange(blockresult,0,blockresult.length-4));

					/* Insert learned block of values in full array*/
					for (int j=0; j<paramblocks[i].length; j++)
						paramvals[offset+j]=blockresult[j];
					offset = offset + paramblocks[i].length;
					//System.out.println("blocklik " + i + " : " + blockresult[blockresult.length-1] + StringOps.arrayToString(paramblocks[i], "[", "]"));
					System.out.println("blocklik " + i + " : " + blockresult[blockresult.length-1] + "   " + paramblocks[i][0]);
				}
				currlik = blockresult[blockresult.length-1];
				likgain=Math.abs(1-lastlik/currlik);
				System.out.println("currlik: " + currlik + " gain: " + likgain + " " + myLearnModule.getLLikThresh()) ;
				if ( likgain < myLearnModule.getLLikThresh())
					terminate = true; 
				lastlik = currlik;
				isfirstloop = false;
			}
			System.out.println();
			for (int i=0;i<4;i++)
				paramvals[parameters.length+i]=blockresult[blockresult.length-4+i];
			return paramvals;
		}
	}


	private GradientGraphO buildGGO(String[] parameters,Boolean showInfoInPrimula,RelData datafold,int obj){
		GradientGraphO gg = null;
		if (showInfoInPrimula)
			myprimula.showMessageThis("Building Gradient Graph ...");
		double timestart=System.currentTimeMillis();
		try{
			gg = new GradientGraphO(myprimula,
					datafold,
					parameters,
					myLearnModule, 
					null,
					GradientGraphO.LEARNMODE,
					obj,
					showInfoInPrimula);
		}
		catch (RBNCompatibilityException ex){System.out.println(ex);}
		double timediff = (System.currentTimeMillis()-timestart)/1000;

		if (showInfoInPrimula)
			myprimula.showMessageThis("Construction time: " + timeformat.format(timediff) +"s" +'\n' );
		return gg;
	}

	// For future use !!!!!
	//private GradientGraphT buildGGT(String[] parameters,Boolean showInfoInPrimula,RelData datafold){
	//	GradientGraphT gg = null;
	//	if (showInfoInPrimula)
	//		myprimula.showMessageThis("Building Gradient Graph ...");
	//	double timestart=System.currentTimeMillis();
	//	try{
	//		gg = new GradientGraphT(myprimula, 
	//			datafold,
	//			parameters,
	//			myLearnModule, 
	//			null,
	//			GradientGraphO.LEARNMODE,
	//			showInfoInPrimula);
	//	}
	//	catch (RBNCompatibilityException ex){System.out.println(ex);}
	//	double timediff = (System.currentTimeMillis()-timestart)/1000;
	//
	//	if (showInfoInPrimula)
	//		myprimula.showMessageThis("Construction time: " + timeformat.format(timediff) +"s");
	//	return gg;
	//}

	private double[] getDataLikelihood(RelData data)
			throws RBNNaNException{
		GradientGraphO gg = buildGGO(new String[0], false, data, myLearnModule.getObjective());
		return gg.computeObjectiveandConfusion(this);
	}

	/* Find initial parameter settings such that the likelihood
	 * of data is nonzero
	 */
	private boolean initParams(RelStruc A, RelData data, String[][] parameternumrels)
			throws RBNNaNException{
		double scale = 1.0;
		boolean success = false;
		int tries =0;
		double lik;
		while (!success && tries < 20){
			System.out.println("# Try init with scale " + scale);		
			myprimula.getRBN().setRandomParameterVals();
			if (myLearnModule.ggrandominit())
				A.setRandom(parameternumrels,scale);
//			lik = getDataLikelihood(data)[1];
//			System.out.println("log-likelihood " + lik);	
//			if (lik == Double.NEGATIVE_INFINITY){
//				tries++;
//				scale=0.5*scale;
//			}
//			else
//				success = true;
			success = true;
		}
		//	A.printNumRelVals();
		return success;
	}

}
