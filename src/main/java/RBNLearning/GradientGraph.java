/*
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

import java.util.*;
import java.io.*;
import RBNpackage.*;
import RBNgui.*;
import RBNExceptions.*;
import RBNutilities.*;
import RBNinference.*;
import mymath.MyMathOps.*;
import myio.StringOps;

/** Main class for RBN parameter learning. The Gradient Graph is a representation of the
 * likelihood function given data consisting of pairs of relational input domains (objects of
 * type RelStruc), and observed values of the probabilistic relations (given as objects of type
 * Instantiation). Each pair may contain a different input domain, or there may be multiple 
 * observations of the probabilistic relations for one input domain. 
 * 
 * Nodes in the gradient graph correspond to ground probability formulas obtained from
 * recursively evaluating the probability formulas corresponding to the ground atoms in the 
 * Instantiations.  Identical ground (sub-) formulas obtained from the evaluation of different 
 * instantiated ground atoms are included only once in the GradientGraphO. For this purpose a 
 * hashtable allNodes for the nodes is maintained. The keys for the nodes are constructed as 
 * strings consisting of a concatenation of the index of the data case with the string representation
 * of the ground probability formula.
 * 
 * Example: the probabilistic relation r(x,y) is defined by F(x,y) = (s(x,y):t(y),0.6).
 * In both the first and the second data pair the ground atom r(4,7) is observed to be true.
 * Then two nodes will be constructed, one with key 1.(s(4,7):t(7),0.6), and one with key
 * 2.(s(4,7):t(7),0.6). Since the sub-formulas s(4,7) and t(7) may evaluate differently 
 * in the two data pairs, these formulas have to be distinguished. If, for example, s(4,7)
 * is observed to be true in the first data pair, and false in the second, then a further 
 * nodes with key 1.t(7) will be constructed, but no node 2.t(7).  
 * 
 * 
 * 
 * @author jaeger
 *
 */
public abstract class GradientGraph{

	/* Gradient Graphs can operate in different modes:
	 * Learn: parameter learning only; graph contains parameter nodes and
	 * IndicatorSumNodes.
	 * 
	 * Map inference: graph contains IndicatorSumNodes and IndicatorMaxNodes.
	 * 
	 * LearnAndMap: graph supports both parameter learning and map inference. Needed 
	 * for clustering tasks.
	 */
	public static int LEARNMODE = 0 ;
	public static int MAPMODE =1 ;
	public static int LEARNANDMAPMODE =2;
	
	public static int CompareIndicatorMaxNodesByScore =0;
	public static int CompareIndicatorMaxNodesByIndex =1;
	
	
	
	protected int objective; /* One of UseLik, UseLogLik, UseSquaredError */
	
	/* The following constants are used to define
	 * options for the main parameter learning functions of 
	 * the GradientGraph
	 */
	public static int FullLearn =0; /* Parameters are learned */
 	public static int OneLineSearch =1; /* Only an incremental update is provided (one iteration of linesearch */
	
 	/* One of this.LEARNMODE, this. MAPMODE,  this.LEARNANDMAPMODE*/
	protected int mode;
	

	
	protected Primula myPrimula;
	protected GradientGraphOptions myggoptions;

	private Hashtable<String,GGProbFormNode> allNodes;
	
	/* Maximum identifier value currently assigned to a node;
	 * 
	 */
	private int maxid;
	
	protected CombFuncNOr combFuncNOr;
	protected CombFuncMean combFuncMean;
	protected CombFuncInvsum combFuncInvsum;
	protected CombFuncESum combFuncESum;
	protected CombFuncLReg combFuncLReg;
	protected CombFuncLLReg combFuncLLReg;
	protected CombFuncSum combFuncSum;
	protected CombFuncProd combFuncProd;


	GGLikelihoodNode llnode;
	Vector<GGAtomSumNode> sumindicators; /* All the indicators for atoms to be summed over */
	Vector<GGAtomMaxNode> maxindicators; /* All the indicators for atoms to be maximized */
	Vector<GGConstantNode> paramNodes; /* All the constant (i.e. parameter) nodes */

	 /* Hashtable containing all the parameters as keys, with an integer index as value. 
	  * This  is constructed
		 * before the vector paramNodes is constructed. It is needed already
		 * in the construction of the nodes of the graph. The Integer indices in this 
		 * hashtable must correspond to the order in paramNodes: 
		 * paramNodes.elementAt(parameters.get("pname")).paramname() = "pname" 
		 */
	Hashtable<String,Integer> parameters;

	/* Minima and maxima for the parameters */
	double[][] minmaxbounds;
	
//	/* The parameters array contains first the model parameters from the 
//	 * rbn, and then (if any) the parameters from numerical relations;
//	 * maxrbnparam is the index of the last rbn parameter.
//	 */
//	int maxrbnparam;
	

	/* list of atoms for which MAP inference is to be performed
	 * 
	 */
	GroundAtomList mapatoms;
	
	/* For estimating likelihood and gradient from Gibbs sampling
	 * values for unobserved atoms: 'numchains' Markov chains are 
	 * sampled in parallel. For each chain, values are estimated based
	 * on the past 'windowsize' states (=instantiatiations of 
	 * unobserved atoms) of the chain. 
	 */
	int numchains;
	int windowsize;

	/* windowindex is the index of the oldest among the this.windowsize samples
	 * that are being stored at the GradientGraphIndicatorSumNodes
	 */
	int windowindex = 0;
	
	/* Contains the log-likelihood of that part of the data that
	 * does not generate an upper ground atom node in the gradient
	 * graph. Used to display the overall objective function value of the model.
	 */
	double objectiveconstant;
	
	/* Array of size 4 containing the confusion matrix values [TP,FP,FN,TN]
	 * for those atoms of the data that do not generate an upper ground atom node in the gradient
	 * graph.
	 * 
	 */
	double[] confusionconst;

	/* True if the data consists of a single observation for 
	 * a single input domain. In this case can omit inputcasenumbers
	 * and observedcasenumbers.
	 */
	boolean singlecasedata;

	/* Auxiliary variables for different ascent strategies: */
	double[] gradmemory = null;
	double[][] thetadiffhistory = null;
	double[][] graddiffhistory = null;
	double[] rhos = null;

	public GradientGraph(Primula mypr, 
			RelData data, 
			Hashtable<String,Integer> params,
			GradientGraphOptions go, 
			GroundAtomList maxats, 
			int m,
			int obj,
			Boolean showInfoInPrimula)
	throws RBNCompatibilityException
	{
		myPrimula = mypr;
		parameters = params;
		myggoptions = go;
		mapatoms = maxats;
		mode = m;
		objective = obj;
		combFuncNOr = new CombFuncNOr();
		combFuncMean = new CombFuncMean();
		combFuncInvsum = new CombFuncInvsum();
		combFuncESum = new CombFuncESum();
		combFuncLReg = new CombFuncLReg();
		combFuncLLReg = new CombFuncLLReg();
		combFuncSum = new CombFuncSum();
		combFuncProd = new CombFuncProd();
		singlecasedata = data.singleObservation();
		
		objectiveconstant = 0;
		confusionconst=new double[4];
	}
	
	
	protected double computeCombFunc(int cf, double[] args){
		switch (cf){
		case CombFunc.NOR:
			return combFuncNOr.evaluate(args);
		case CombFunc.MEAN:
			return combFuncMean.evaluate(args);
		case CombFunc.INVSUM:
			return combFuncInvsum.evaluate(args);
		case CombFunc.ESUM:
			return combFuncESum.evaluate(args);
		case CombFunc.LREG:
			return combFuncLReg.evaluate(args);
		case CombFunc.LLREG:
			return combFuncLLReg.evaluate(args);
		case CombFunc.SUM:
			return combFuncSum.evaluate(args);
		case CombFunc.PROD:
			return combFuncProd.evaluate(args);
		
		}
		return 0;
	}

	public abstract double[] currentLikelihood();

	public abstract double currentLogLikelihood();


	public abstract double[] currentParameters();


	public int numberOfParameters(){
		return parameters.size();
	}

	public Hashtable<String,Integer> parameters(){
		return parameters;
	}

//	public String parameterAt(int i){
//		return parameters[i];
//	}


	/** Computes the empirical likelihood and empirical partial derivatives 
	 * of the current sample.
	 * The value and gradient fields contain the values for the last sample.
	 *
	 * When numchains=0, then value = likelihoodsum and gradient = gradientsum 
	 * are the correct values.
	 *
	 */ 
	public abstract void evaluateLikelihoodAndPartDerivs(boolean likelihoodonly)
			throws RBNNaNException;

	public abstract void evaluateBounds();




	/* Tries to randomly generate numchains instantiations of the
	 * indicator variables with nonzero probability given the
	 * current parameter values. Returns true if successful.
	 */
	public abstract boolean initIndicators(Thread mythread);

	/** Performs one round of Gibbs sampling. 
	 * Each variable is resampled once.
	 * 
	 * windowindex is the index of the oldest among the this.windowsize samples
	 * that are being stored. In the GGAtomSumNode.sampledVals
	 * arrays the values windowindex+0,...,windowindex+numchains-1 are 
	 * overwritten
	 */
	public abstract void gibbsSample(Thread mythread);
	
//	/* Similar to gibbsSample, but operates on the maxindicators, and
//	 * greedily  toggles truth values if it leads to an improvement in 
//	 * likelihood 
//	 */
//	public void mapStep(){
//		double[] oldlik;
//		double[] newlik;
//
//		GGAtomMaxNode ggin;
//		
//			for (int i=0;i<maxindicators.size();i++){
//				evaluateLikelihoodAndPartDerivs(true);
//				oldlik=llnode.likelihoodsum();
//				
//				ggin = (GGAtomMaxNode)maxindicators.elementAt(i);
//				
//				ggin.toggleCurrentInst();
//				
//				evaluateLikelihoodAndPartDerivs(true);
//				newlik=llnode.likelihoodsum();
//				
//				if (SmallDouble.compareSD(newlik, oldlik) == -1){
//					ggin.toggleCurrentInst();
//				}
//				
//			}
//		
//	}

	

	

	
	/** Sets the truthval fields in the ProbFormNodes corresponding
	 * to unobserved atoms to the truthvalues in the sno's sample
	 *
	 * If sno<0 do nothing!
	 */
	public abstract void setTruthVals(int sno);

	public void setLearnModule(LearnModule lm){
		myggoptions = lm;
	}

	public abstract void showLikelihoodNode(RelStruc A);
	

//	public void showParameterValues(String prefix){
//		double[] paramvals = currentParameters();
//		System.out.print(prefix);
//		for (int i=0;i<parameters.size();i++){
//			System.out.print(parameters[i] + ": " + paramvals[i] + "; ");
//		}
//		System.out.println();
//	}



	/** Returns lambda*firstpoint + (1-lambda)*secondpoint */
	protected double[] midpoint(double[] firstpoint, double[] secondpoint, double lambda){
		double[] result = new double[firstpoint.length];
		for (int i=0;i<result.length;i++)
			result[i]=lambda*firstpoint[i]+(1-lambda)*secondpoint[i];
		return result;
	}

	/** Determines the direction for the linesearch given a current theta 
	 * and gradient
	 */
	protected double[] constrainedDirection(double[] theta, double[] gradient){
		double[] result = new double[gradient.length];
		/* Penalize the gradient components that are leading towards the
		 * boundary of the parameter space:
		 */
		double disttobound;
		
		for (int i=0 ;i < parameters.size();i++){
			if (gradient[i]<0){
				if (minmaxbounds[i][0] != Double.NEGATIVE_INFINITY ) {
					disttobound = Math.min(theta[i]-minmaxbounds[i][0], 1.0);
				}
				else disttobound =1.0;
				result[i]=gradient[i]*disttobound;
			}
			else{ // gradient > 0
				if (minmaxbounds[i][1] != Double.POSITIVE_INFINITY ) {
					disttobound = Math.min(minmaxbounds[i][1]-theta[i], 1.0);
				}
				else disttobound =1.0;
				result[i]=gradient[i]*disttobound;
			}
		}
		return result;
	}
	
	/** Maps theta into the feasible region
	 */
	protected double[] clipToFeasible(double[] theta){
		double[] result = new double[theta.length];
	
		
		for (int i=0 ;i < parameters.size();i++) {
			result[i]=Math.max(theta[i],minmaxbounds[i][0]);
			result[i]=Math.min(result[i],minmaxbounds[i][1]);
		}
		return result;
	}

	/** Searches for likelihood-optimizing parameters, starting at
	 * currenttheta
	 *
	 * Returns array of length n+4, where n is the number of parameter nodes
	 * in the Gradient Graph. 
	 * 
	 * The result array contains:
	 * 
	 * [0..n-1]: the current parameter values at the end of thetasearch
	 * 
	 * [n,n+1]: the likelihood value of the current parameters expressed 
	 * as a 'SmallDouble'
	 * 
	 * [n+2]: the kth root of the likelihood value, for k the number of 
	 * children of the likelihood node. This gives a 'per observed atom'
	 * likelihood value that is more useful than the overall likelihood.
	 * 
	 * [n+3]: the log-likelihood of the whole data computed as 
	 * k*log(result[n+2])+this.likelihoodconst
	 * 
	 */
	protected abstract double[] thetasearch(double[] currenttheta, 
			GGThread mythread, 
			int fullorincremental,
			boolean verbose)
					throws RBNNaNException;

	public abstract double[] learnParameters(GGThread mythread, int fullorincremental,boolean verbose)
			throws RBNNaNException;

	/** Performs a linesearch for parameter settings optimizing 
	 * log-likelihood starting from oldthetas in the direction
	 * gradient
	 * 
	 * returns new parameter settings
	 */
	protected abstract double[] linesearch(double[] oldthetas, 
			double[] gradient, 
			GGThread mythread)
					throws RBNNaNException;
	
	
	/** Sets the parameter values in the nodes to thetas. thetas[i] will be the
	 * value of the parameter in the i'th position in this.paramNodes
	 */
	protected abstract void setParameters(double[] thetas);

	public abstract double[] getParameters();

	public abstract double[] getGradient()
			throws RBNNaNException;
	
	protected abstract void setParametersRandom();
	
	public abstract void setParametersFromAandRBN();


	

	/** Prints a list of  likelihood values for all possible parameter settings
	 * obtained by varying each parameter from 0.0 to 1.0 using a stepsize of incr
	 * (debugging method, not in use)
	 */
	public void showAllLikelihoods(double incr)
			throws RBNNaNException{
		double[] nextsetting = new double[paramNodes.size()];
		for (int i=0;i<nextsetting.length;i++)
			nextsetting[i] = 0.0;
		int nextindex = nextsetting.length - 1;
		double nextll;
		double max = 0;
		double[] best = nextsetting.clone();
		while (nextindex >= 0){
			setParameters(nextsetting);
			evaluateLikelihoodAndPartDerivs(true);
			nextll = llnode.value();
			if (nextll > max){
				max = nextll;
				best = nextsetting.clone();
			}
			//System.out.println(rbnutilities.arrayToString(nextsetting)+": " + nextll);
			/* Find the next parameter setting */
			nextindex = nextsetting.length - 1;
			while (nextindex >= 0 && nextsetting[nextindex]>=0.9999)
				nextindex--;
			if (nextindex >=0){
				nextsetting[nextindex]=nextsetting[nextindex]+incr;	
				for (int i=nextsetting.length - 1;i>nextindex;i--)
					nextsetting[i]=0.0;
			}
		}	   
		System.out.println("Best: " + rbnutilities.arrayToString(best)+": " + max);
	}

//	private void unsetSumIndicators(){
//		for (int i=0;i<sumindicators.size();i++)
//			sumindicators.elementAt(i).unset();	
//	}


	/** Returns true if  l1 represents a larger or equal
	 * likelihood than l2. 
	 * Likelihoodvalues are given by l[0] * 1El[1]
	 */
	protected boolean likelihoodGreaterEqual(double[] l1, double[] l2){
		return (SmallDouble.compareSD(l1, l2) != -1);
	}

	protected boolean likelihoodGreater(double[] l1, double[] l2){
		return (SmallDouble.compareSD(l1, l2) == 1);
	}

	protected double likelihoodRatio(double[] l1, double[] l2){
		int power1;
		int power2;
		double decims1;
		double decims2;

		if (l1[0]>0){	
			power1 = (int)(Math.log(l1[0])/Math.log(10));
			decims1 = l1[0]/Math.pow(10,power1);
		}
		else{
			power1 = 0;
			decims1=0;
		}
		if (l2[0]>0){	
			power2 = (int)(Math.log(l2[0])/Math.log(10));
			decims2 = l2[0]/Math.pow(10,power2);
		}
		else{
			power2 = 0;
			decims2=0;
		}

		power1=power1-(int)l1[1];
		power2=power2-(int)l2[1];
		//System.out.println( "p1: " + power1 +   "p2: " + power2 + "d1: " + decims1 + "d2: " + decims2);
		if (decims2>0)
			return (decims1/decims2)*Math.pow(10,power1-power2);
		else return Double.POSITIVE_INFINITY;

	}


	/** Determines whether grad is the zero
	 * vector (or sufficiently close to zero).
	 * @param grad
	 * @return
	 */
	protected boolean iszero(double[] grad){
		boolean result = true;
		for (int i=0;i<grad.length;i++){
			if (Math.abs(grad[i])>0)
				result = false;
		}
		return result;
	}
	
	/* check whether the gradient is of the form (0,...,0,1,0,...0), i.e.,
	 * optimzation is w.r.t. to the single parameter at the '1' index
	 * 
	 * Returns the '1' index if this is the case, otherwise returns -1
	 * 
	 */
	protected int isPartDeriv(double[] grad){
		int result =-1;
		double sum =0;
		boolean zeroone = true;
		for (int i=0;i<grad.length;i++){
			sum = sum+grad[i];
			if (grad[i] != 0 && grad[i] != 1)
				zeroone = false;
			if (grad[i] == 1)
				result = i;
		}
		if (zeroone && sum == 1)
			return result;
		else return -1;
	}

	/** Computes the objective function value given the current parameter setting
	 * 
	 * Returns double array with result[0]=per node likelihood, 
	 * result[1]= objective function value
	 * result[2:5] = confusion matrix 
	 * 
	 * @return
	 */	
	public abstract double[] computeObjectiveandConfusion(GGThread mythread)
			throws RBNNaNException;

//	public String makeKey(ProbForm pf, int inputcaseno, int observcaseno, RelStruc A){
//		String key;
//		//System.out.println("make key for " + pf.toString() + " " + inputcaseno  + " " + observcaseno);
//		if (pf instanceof ProbFormConstant)
//			key = pf.asString(Primula.CLASSICSYNTAX,0,A,false,false);
//		else 
//			key = inputcaseno + "."  + observcaseno + "."  +  pf.asString(Primula.CLASSICSYNTAX,-1,A,false,false);
//		//System.out.println("return " + key); 
//		return key;
//	}

	public static String makeKey(ProbForm pf, int inputcaseno, int observcaseno, RelStruc A){
		String key;
//		System.out.println("make key for " + pf.toString() + " " + inputcaseno  + " " + observcaseno);
		String pfstring= pf.asString(Primula.CLASSICSYNTAX,0,A,false,false);
//		if (pf.getAlias()!=null)
//			pfstring=pf.getAlias();
//		else 
//			pfstring = pf.asString(Primula.CLASSICSYNTAX,0,A,false,false);
		if (pf instanceof ProbFormConstant)
			key = pfstring;
		else 
			key = inputcaseno + "."  + observcaseno + "."  +  pfstring;
//		System.out.println("return " + key); 
		return key;
	}
	
	public GroundAtomList maxatoms(){
		return mapatoms;
	}
	
	
	public int mode(){
		return mode;
	}
	

	public abstract int[] getMapVals();
	
	public OneStrucData getMapValuesAsInst(int[] instvals){
//		int[] instvals = getMapVals();
		OneStrucData result = new OneStrucData();
		result.setParentRelStruc(myPrimula.getRels());
		for (int i=0;i< mapatoms.size();i++){
			result.add(mapatoms.atomAt(i),instvals[i],"?");
		}
		return result;
	}
	public int objective(){
		return objective;
	}
	
	public double likelihoodconst(){
		return objectiveconstant;
	}
}
