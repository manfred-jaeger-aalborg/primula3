/*
 * SimplePFNetworkNode.java 
 * 
 * Copyright (C) 2003 Max-Planck-Institut fuer Informatik,
 *                    Helsinki Institute for Information Technology
 *
 * contact:
 * jaeger@cs.auc.dk    www.cs.auc.dk/~jaeger/Primula.html
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


package RBNinference;

import java.util.*;
import RBNpackage.*;
import RBNExceptions.*;
import RBNutilities.*;
import RBNgui.*;
import myio.*;

public class SimplePFNetworkNode extends PFNetworkNode{


	/*
	 * Maps integer index to a parentconfiguration
	 * As constructed by BayesConstructor.makeCPT
	 */
	private HashMap<Integer,String> indxToParconfig;

	/*
	 * maps instantiations of the parents represented as int[] to a probability vector cpt
	 */
	private HashMap<String,double[]> cpt;
	

	/** conditionalsampleweights.get(parconfig)[j]: sum of all weights of samples in which 
	 * parent configuration parconfig was sampled and j was the sampled state of this node
	 * (in a run of importance sampling).
	 * 
	 * Represented as SmallDoubles 
	 * 
	 * Initialized to [0,...,0].
	 * Only used in adaptive sampling
	 * 
	 * Marginal of conditionalsampleweights_subsample;
	 **/
	protected HashMap<String,double[][]> conditionalsampleweights;


	/** numvalsamples[i][j]: number of samples in which 
	 * i'th parent configuration was sampled and j was the sampled state of this node
	 *(in a run of importance sampling).
	 **/
	protected HashMap<String,int[]> numvalsamples;

	/* Number of subsamples for determining variance in adaptive sampling
	 * Need not be equal to number of subsamples used for variance 
	 * estimate for querynodes
	 */
	private int num_subsamples_adapt = 10;

	
	/** 
	 * conditionalsampleweightsfalse_subsample.get(parconfig)[k][j] contains the
	 * sum of weights (represented as SmallDouble; last dimension in the values is 2) 
	 * of all samples 
	 * assigned to the kth subsample 
	 * with parentconfig parconfig where this node is sampled to j
	 **/
	protected HashMap<String,double[][][]> conditionalsampleweights_subsample;

	
	/*
	 * Array of dimension num_samples x num_parentconfig
	 * 
	 */
	protected HashMap<String,int[][]> numvalsamples_subsample;
//	protected int[][] numfalsesamples_subsample;

	
	/*
	 * For importance sampling: maps parentconfigurations to 
	 * the importance sampling distributions 
	 */
	protected HashMap<String,double[]> importance;

	/*
	 * For importance sampling: maps parentconfigurations to 
	 * the variance of the importance sampling distributions defined
	 * by different subsamples
	 */
	protected HashMap<String,Double> importance_variance;

	/*
	 * Maintains for each parentconfiguration the index of the current
	 * subsample 0,...,num_subsamples_adapt-1
	 */
	protected HashMap<String,Integer> sampleindex;
	
	/*
	 * The number of values of the parents of this node 
	 * in the order as enumerated by parents
	 */
	private int[] parentnumvals;


	/* In adaptive importance sampling: sampling probability for state j of this node
	 * given parent configuration i is 
	 * 
	 * (1/numsamples[i])cptentries[i][j] + 
	 *                (numsamples[i]/(1+numsamples[i]))avweight[i][j]/(avweight_sum[i])
	 *
	 * where 
	 * numsamples[i]=sum_j numvalsamples[i][j]
	 * avweight[i][j]=conditionalsampleweights[i][j]/numvalsamples[i][j]
	 * avweight_sum[i]=sum_j avweight[i][j]
	 */

	public SimplePFNetworkNode(GroundAtom at){
		super(at);
		cpt = new HashMap<String,double[]>();
		conditionalsampleweights = new HashMap<String,double[][]>();
		numvalsamples = new HashMap<String,int[]>();
		conditionalsampleweights_subsample = new HashMap<String,double[][][]>();
		numvalsamples_subsample = new HashMap<String,int[][]>();
		importance = new HashMap<String,double[]>();
		importance_variance = new HashMap<String,Double>();
		sampleindex = new HashMap<String,Integer>();
	}

	/* Turn a ComplexPFNetworkNode cpfn into a simple one.
	 * The parent list of cpfn can be relative to an instantiation instasosd.
	 * instasosd is then used in the transformation for the evaluation
	 * of cpfn.probform
	 *
	 * parents/children are not copied! This constructor should only be 
	 * used when cpfn will no longer be used.
	 */
	public SimplePFNetworkNode(ComplexPFNetworkNode cpfn, OneStrucData inst, RelStruc A)
	throws RBNCompatibilityException
	{
		
		super(cpfn.myatom());
		/* Turn cpfn.parents into vector of atoms
		 * and construct cpt
		 */
		Vector<GroundAtom> atomvec = new Vector<GroundAtom>();
		for (BNNode pfnn: cpfn.parents)
			atomvec.add(((PFNetworkNode)pfnn).myatom());
		
		Object[] frombayescon = BayesConstructor.makeCPT(cpfn.cpmodel(),A,inst,atomvec);
		cpt = (HashMap<String,double[]>)frombayescon[2];
		indxToParconfig = (HashMap<Integer,String>)frombayescon[1];
		
		
		instantiated = cpfn.instantiated;
		sampleinst = cpfn.sampleinstVal();
		parents = cpfn.parents;
		children = cpfn.children;
		depth = cpfn.depth();
		parentnumvals = new int[parents.size()];
		for (int i=0; i<parents.size();i++) 
			parentnumvals[i]=parents.elementAt(i).getNumvalues();	
		
	}

//	public double[][] mycpt(){
//		return cptentries;
//	}


//	public double[][][] conditionalsampleweights(){
//		return conditionalsampleweights;
//	}


	/*
	 * Returns integer j if state j has probability 1 regardless of parent configuration (if 
	 * usesampleinst=true, assuming sampled states of parents). Otherwise, returns -1.
	 */
	public int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable atomhasht){
		return evaluatesTo(usesampleinst); //Dependence on A and inst here already compiled into the cpt 
	}

	public  int evaluatesTo(boolean usesampleinst)
	{
		boolean instconsistent;
		boolean terminate=false;
		PFNetworkNode nextpar;
		int evalsto = -1;
		int instval;

		for (String parentinst: cpt.keySet()){
			/* test whether the parent configuration 
			 * corresponding to index i is consistent
			 * with instantiation
			 */
			int[] parentinst_array=rbnutilities.stringToIntArray(parentinst,",");
			instconsistent = true;

			for (int p=0;p<parents.size();p++){
				nextpar=(PFNetworkNode)parents.elementAt(p);
				if (!usesampleinst)
					instval = nextpar.instantiated;
				else instval = nextpar.sampleinstVal();
				if (instval != -1 & instval != parentinst_array[p])
					instconsistent = false;
			}
			
			/* now check the probability value
			 */
			if (instconsistent){
				double[] cptrow = cpt.get(parentinst);
				for (int v=0;v<this.numvalues;v++) {
					if (cptrow[v] != 1 && cptrow[v]!=0)
						return -1; // Non deterministic cpt row -> no deterministic value for this node
					if (cptrow[v]==1) {
						if (evalsto != -1 && evalsto!=v) { // Another cptrow defines a different value for this node
							return -1;
						}
						if (evalsto == -1)  // this is the -1 from initialization!
							evalsto = v;
					}
				}
			} // if (instconsistent)
		}
		
		return evalsto;
	}

	public boolean isIsolatedZeroNode(RelStruc rels)
	throws RBNCompatibilityException{
		if (!isboolean) return false;
		if (parents.size()!=0) return false;
		if (children.size()!=0) return false;
		if (cpt.get(new int[] {})[0]!=1) return false;
		if (instantiated != -1) return false;
		return true;
	}


	public void initializeForSampling(int sampleordmode, 
			int adaptivemode, 
			Hashtable<Rel,GroundAtomList> queryatoms, 
			int nsm,
			int nsa){
		//truesampleweight = 0;
		super.initializeForSampling(sampleordmode,adaptivemode, queryatoms, nsm, nsa);
		int num_parentconfig = mymath.MyMathOps.intPow(2,parents.size());
		if (sampleordmode == InferenceModule.OPTION_SAMPLEORD_FORWARD 
				&& adaptivemode == InferenceModule.OPTION_SAMPLE_ADAPTIVE
				&& upstreamofevidence == true)
		{
			num_subsamples_adapt=nsa;
			
			initMaps();
		}
	}



	public void sampleForward(RelStruc A,
			Hashtable<String,PFNetworkNode>  atomhasht,
			OneStrucData inst,
			int adaptivemode,
    		Hashtable<String,double[]> evaluated,
			long[] timers){
		sampleForward(A,adaptivemode);
	}

	public void sampleForward(RelStruc A,int adaptivemode)
	{	    
		sampleparentconfig = parentConfig();
		sampleparentconfig_string = rbnutilities.arrayToString(sampleparentconfig);
		double[] cptrow = cpt.get(sampleparentconfig_string);

		
		if (instantiated == -1){

			double rand = Math.random();
			if ( upstreamofevidence == false || 
					adaptivemode == InferenceModule.OPTION_NOT_SAMPLE_ADAPTIVE){ // non-adaptive sampling
				sampleinst=rbnutilities.sampledValue(cptrow);
				thissampleprob=cptrow[sampleinst];
				thisdistrprob=cptrow[sampleinst];
			}
			else{  // adaptive sampling
				double[] importancerow = importance.get(sampleparentconfig_string);
				double[] sampleprob = new double[numvalues];
				double lambda=0;

				if (rbnutilities.isDeterministic(cptrow))
					sampleprob = cptrow;
				else{
					if (importance_variance.get(sampleparentconfig_string) != -1) // variance defined
						if (! rbnutilities.isDeterministic(importancerow))
							lambda = 1/Math.exp(importance_variance.get(sampleparentconfig_string));
						else lambda = 1-1/rbnutilities.arraySum(numvalsamples.get(sampleparentconfig_string));
					else lambda = 0;

					sampleprob = rbnutilities.arrayAdd(
							rbnutilities.arrayScalMult(cptrow, 1-lambda),
							rbnutilities.arrayScalMult(importancerow, lambda)
							);

				}
				
				sampleinst=rbnutilities.sampledValue(sampleprob);
				thissampleprob=sampleprob[sampleinst];
				thisdistrprob=cptrow[sampleinst];
			}
		}
		else{  // instantiated != -1 
			sampleinst = instantiated;
			thissampleprob = 1;
			thisdistrprob=cptrow[sampleinst];
		}
			}


	/* Computes the index of the parent configuration of current sample
	 */
	public int[] parentConfig(){
		int[] parinst = new int[parents.size()];
		for (int i=0;i<parents.size();i++){
			parinst[i]=((PFNetworkNode)parents.elementAt(i)).sampleinstVal();
			if (parinst[i]==-1) System.out.println("SimplePFNetworkNode.parentConfig(): this should not happen !!!!!");
		}
		return parinst;
	}


	public  void setDistrProb(RelStruc A, 
			Hashtable<String,PFNetworkNode> atomhasht,
			OneStrucData inst,
    		Hashtable<String,double[]> evaluated,
			long[] timers)
	{
		if (thisdistrprob == -1)
			thisdistrprob = cpt.get(rbnutilities.arrayToString(sampleparentconfig))[sampleinst];
	}


//	public String showAllTrueWeights(){
//		return rbnutilities.arrayToString(conditionalsampleweightstrue);
//	}
//
//	public String showAllFalseWeights(){
//		return rbnutilities.arrayToString(conditionalsampleweightsfalse);
//	}

//	public String showNumTrue(){
//		return rbnutilities.arrayToString(numtruesamples);
//	}
//
//	public String showNumFalse(){
//		return rbnutilities.arrayToString(numfalsesamples);
//	}

//	/* argument A only for output strings */
//	public void updateconditionalsampleweights(RelStruc A, double[] weight){
//		//System.out.print("+"+ sampleinst );
//		//System.out.print(myatom.asString(A) + " " );
//		if (upstreamofevidence == true){
//			switch (sampleinst){
//			case 0: 
//				numfalsesamples[sampleparentconfig]++;
//				conditionalsampleweightsfalse[sampleparentconfig]=SmallDouble.add(conditionalsampleweightsfalse[sampleparentconfig],weight);
//				conditionalsampleweightsfalse_subsample[sampleparentconfig][sampleindex[sampleparentconfig]] 
//				                                                            = SmallDouble.add(conditionalsampleweightsfalse_subsample[sampleparentconfig][sampleindex[sampleparentconfig]],weight);
//				numfalsesamples_subsample[sampleparentconfig][sampleindex[sampleparentconfig]]++;
//				break;
//			case 1: 
//				numtruesamples[sampleparentconfig]++;
//				conditionalsampleweightstrue[sampleparentconfig]=SmallDouble.add(conditionalsampleweightstrue[sampleparentconfig],weight);
//				conditionalsampleweightstrue_subsample[sampleparentconfig][sampleindex[sampleparentconfig]] 
//				                                                           = SmallDouble.add(conditionalsampleweightstrue_subsample[sampleparentconfig][sampleindex[sampleparentconfig]],weight);
//				numtruesamples_subsample[sampleparentconfig][sampleindex[sampleparentconfig]]++;
//				break;
//			}
//			if (sampleindex[sampleparentconfig] == num_subsamples_adapt -1)
//			{
//				sampleindex[sampleparentconfig] = 0;
//				updateVariance();
//			}
//			else
//				sampleindex[sampleparentconfig]++;
//		}
//
//	}

	public void updateconditionalsampleweights(){
		//System.out.print("+"+ sampleinst );
		//System.out.print(myatom.asString(A) + " " );
		double weight = thisdistrprob()/thissampleprob() ;
		
		for (int i=0;i<this.children.size();i++) {
			PFNetworkNode nextpfnn = (PFNetworkNode)children.elementAt(i);
			weight = weight * nextpfnn.thisdistrprob()/nextpfnn.thissampleprob();
		}
		
		double smallweight[] = {weight,0};
		
		if (upstreamofevidence == true){ // otherwise no importance distribution for this node needed
			int[] nvs = numvalsamples.get(sampleparentconfig_string);
			nvs[sampleinst]++;
			double[][] cs = conditionalsampleweights.get(sampleparentconfig_string);
			cs[sampleinst]=
					SmallDouble.add(cs[sampleinst],smallweight);
			numvalsamples.get(sampleparentconfig_string)[sampleinst]++;
			
			
			numvalsamples_subsample.get(sampleparentconfig_string)[sampleindex.get(sampleparentconfig_string)][sampleinst]++;
			
			if (sampleindex.get(sampleparentconfig_string) == num_subsamples_adapt -1)
			{
				sampleindex.put(sampleparentconfig_string,Integer.parseInt("0"));
				updateImportance();
			}
			else
				sampleindex.put(sampleparentconfig_string,sampleindex.get(sampleparentconfig_string)+1);
		}
	}
	
	private void updateImportance(){

		double[] importancerow = new double[numvalues];
		double importancevar=0;

		double[] allweight = SmallDouble.sumArray(conditionalsampleweights.get(sampleparentconfig_string));
		double[] subsweight = new double[2];
		boolean allsubsampleshaveweight = true;

		// Updating the importance function
		if (allweight[0] == 0) { // no data to construct an importance distribution for this sampleparentconfig
			importance.put(sampleparentconfig_string,cpt.get(sampleparentconfig_string).clone());
			return;
		}
		else importancerow = SmallDouble.toStandardDoubleArray(
				SmallDouble.divide(conditionalsampleweights.get(sampleparentconfig_string),allweight));
		importance.put(sampleparentconfig_string, importancerow);
		
		// Updating the variance
		for (int i=0;i<num_subsamples_adapt;i++){
			double[][][] csw_s = conditionalsampleweights_subsample.get(sampleparentconfig_string);
			

			subsweight = SmallDouble.sumArray(csw_s[i]);
			if (subsweight[0] == 0)
				allsubsampleshaveweight = false;
			else 
				importancevar+= 
				rbnutilities.squaredNorm(
						rbnutilities.arraySubtract(
								SmallDouble.toStandardDoubleArray(
								    SmallDouble.divide(
								    		conditionalsampleweights_subsample.get(sampleparentconfig_string)[i]
								    				,subsweight)
								                                 )
								, importancerow));
						
		}
		if (allsubsampleshaveweight)
			importancevar = importancevar*num_subsamples_adapt;
		else 
			importancevar = -1;
		
		importance_variance.put(sampleparentconfig_string,importancevar);
	}
	
	private void initMaps() {
		conditionalsampleweights = new HashMap<String,double[][]>();
		numvalsamples = new HashMap<String,int[]>();
		conditionalsampleweights_subsample = new HashMap<String,double[][][]>();
		numvalsamples_subsample = new HashMap<String,int[][]>();
		importance = new HashMap<String,double[]>();
		importance_variance = new HashMap<String,Double>();
		sampleindex = new HashMap<String,Integer>();
		
		for (String parstring: indxToParconfig.values()) {
			conditionalsampleweights.put(parstring, new double[this.getNumvalues()][2]);
			numvalsamples.put(parstring, new int[this.getNumvalues()]);
			conditionalsampleweights_subsample.put(parstring, new double[num_subsamples_adapt][this.getNumvalues()][2]);
			numvalsamples_subsample.put(parstring, new int[num_subsamples_adapt][this.getNumvalues()]);
			importance.put(parstring,new double[2]);
			importance_variance.put(parstring, Double.valueOf(-1));
			sampleindex.put(parstring, Integer.valueOf(0));
		}
	}
}

