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
	 * Maps inter index to a parentconfiguration
	 */
	private HashMap<Integer,int[]> indxToParconfig;

	/*
	 * maps instantiations of the parents represented as int[] to a probability vector cpt
	 */
	private HashMap<int[],double[]> cpt;
	
//	/* As constructed by BayesConstructor.makeCPT */
//	private HashMap<Integer,int[]> indxToTuple = new HashMap<Integer,int[]>();

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
	protected HashMap<int[],double[][]> conditionalsampleweights;

//	/** conditionalsampleweightstrue[i]: sum of all weights of samples in which 
//	 * i'th parent configuration was sampled and this node was sampled true
//	 *(in a run of importance sampling).
//	 * Initialized to [0,...,0].
//	 * Only used in adaptive sampling
//	 **/
//	protected double[][] conditionalsampleweightstrue;

	/** numvalsamples[i][j]: number of samples in which 
	 * i'th parent configuration was sampled and j was the sampled state of this node
	 *(in a run of importance sampling).
	 **/
	protected HashMap<int[],int[]> numvalsamples;

//	/** numtruesamples[i]: number of samples in which 
//	 * i'th parent configuration was sampled and this node was sampled false
//	 *(in a run of importance sampling).
//	 * Initialized to [0,...,0].
//	 * Only used in adaptive sampling
//	 **/
//	protected int[] numfalsesamples;


	private int num_subsamples_adapt = 10;
	/* Number of subsamples for determining variance in adaptive sampling
	 * Need not be equal to number of subsamples used for variance 
	 * estimate for querynodes
	 */
	
	
	/** 
	 * conditionalsampleweightsfalse_subsample.get(parconfig)[j][k] contains the
	 * sum of weights (represented as SmallDouble) of all samples 
	 * assigned to the kth subsample 
	 * with parentconfig parconfig where this node is sampled to j
	 **/
	protected HashMap<int[],double[][][]> conditionalsampleweights_subsample;

//	/** array of dimensions 2 x num_parentconfig x num_samples
//	 * conditionalsampleweightstrue_subsample[i][j] contains the
//	 * sum of weights (represented as SmallDouble) of all samples 
//	 * assigned to the jth subsample 
//	 * with parentconfig i where this node is sampled true
//	 **/
//	protected double[][][] conditionalsampleweightstrue_subsample;
	
	protected HashMap<int[],int[][]> numvalsamples_subsample;
//	protected int[][] numfalsesamples_subsample;


	protected HashMap<int[],double[]> odds_variance;

	protected int[] sampleindex;
	
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
		cpt = new HashMap<int[],double[]>();
		conditionalsampleweights = new HashMap<int[],double[][]>();
		numvalsamples = new HashMap<int[],int[]>();
		conditionalsampleweights_subsample = new HashMap<int[],double[][][]>();
		numvalsamples_subsample = new HashMap<int[],int[][]>();
		odds_variance = new HashMap<int[],double[]>();
		sampleindex = new int[0];
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
		cpt = (HashMap<int[],double[]>)frombayescon[2];
		indxToParconfig = (HashMap<Integer,int[]>)frombayescon[1];
		
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

		for (int[] parentinst: cpt.keySet()){
			/* test whether the parent configuration 
			 * corresponding to index i is consistent
			 * with instantiation
			 */
			instconsistent = true;

			for (int p=0;p<parents.size();p++){
				nextpar=(PFNetworkNode)parents.elementAt(p);
				if (!usesampleinst)
					instval = nextpar.instantiated;
				else instval = nextpar.sampleinstVal();
				if (instval != -1 & instval != parentinst[p])
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
			GroundAtomList queryatoms, 
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
			conditionalsampleweights =  new HashMap<int[],double[][]>();
			numvalsamples = new HashMap<int[],int[]>();
			conditionalsampleweights_subsample = new HashMap<int[],double[][][]>();
			numvalsamples_subsample = new HashMap<int[],int[][]>();
			odds_variance = new HashMap<int[],double[]>();
			sampleindex = new int[num_parentconfig];
			for (int[] parconfig: indxToParconfig.values()) {
				double[] initov = new double[numvalues];
				for (int i=0;i<numvalues;i++)
					initov[i]=-1;
				odds_variance.put(parconfig,initov);
			}
		}
	}



	public void sampleForward(RelStruc A,
			Hashtable<String,PFNetworkNode>  atomhasht,
			OneStrucData inst,
			int adaptivemode,
    		Hashtable<String,Double> evaluated,
			long[] timers){
		sampleForward(A,adaptivemode);
	}

	public void sampleForward(RelStruc A,int adaptivemode)
	{	    
		sampleparentconfig = parentConfig();
		double[] cptrow = cpt.get(sampleparentconfig);

		if (instantiated == -1){

			double rand = Math.random();
			if ( upstreamofevidence == false || 
					adaptivemode == InferenceModule.OPTION_NOT_SAMPLE_ADAPTIVE){
				sampleinst=rbnutilities.sampledValue(cptrow);
				thissampleprob=cptrow[sampleinst];
				thisdistrprob=cptrow[sampleinst];
			}
			else{
				double[] prob = new double[numvalues];
				double[][] av = new double[numvalues][2]; // small double representation
				double[] avsum  = new double[2]; // small double representation
				double[] odds = new double[numvalues];
				double lambda=0;

				if (rbnutilities.isDeterministic(cptrow))
					prob = cptrow;
				else{
					for (int i=0;i<numvalues;i++) {
						av[i]=SmallDouble.divide(conditionalsampleweights.get(sampleparentconfig)[i],
								numvalsamples.get(sampleparentconfig)[i]);
					}
					avsum = SmallDouble.sumArray(av);
					if (avsum[0] > 0)
						odds = SmallDouble.toStandardDoubleArray(SmallDouble.divide(av,avsum));
					else odds = cptrow;

					/* Compute lambda */ 
//					if (truesampleweights_variance[sampleparentconfig] != -1 
//					&& falsesampleweights_variance[sampleparentconfig] != -1 )
//					{
//					if (avtrue > 0)
//					varmeasure = truesampleweights_variance[sampleparentconfig]/avtrue ;
//					if (avfalse > 0)
//					varmeasure = varmeasure + falsesampleweights_variance[sampleparentconfig]/avtrue ;
//					}
//					else 
//					varmeasure = -1;
//					if (varmeasure >= 0)
//					lambda = 1/Math.exp(varmeasure);

					if (trueodds_variance[sampleparentconfig] != -1)
						if (trueodds != 0 && trueodds != 1)
							lambda = 1/Math.exp(trueodds_variance[sampleparentconfig]/trueodds);
						else lambda = 1-1/((double)numtruesamples[sampleparentconfig]+(double)numfalsesamples[sampleparentconfig]);
					else lambda = 0;

					prob = (1-lambda)*cpt + (lambda)*(trueodds);

				}

				if (rand < prob){
					sampleinst = 1;
					thissampleprob = prob;
					thisdistrprob = cpt;
				}
				else{
					sampleinst = 0;
					thissampleprob = (1-prob);
					thisdistrprob = (1-cpt);
				}
			}   
		}
		else{  // instantiated != -1 
			sampleinst = instantiated;
			thissampleprob = 1;
			switch (instantiated){
			case 1: 
				thisdistrprob = cpt;
				break;
			case 0: thisdistrprob = (1-cpt);
			}
		}
		//System.out.print("si: " + sampleinst + " ");
	}


	/* Computes the index of the parent configuration of current sample
	 */
	public int[] parentConfig(){
		
		PFNetworkNode nextpar;
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
    		Hashtable<String,Double> evaluated,
			long[] timers)
	{
		if (thisdistrprob == -1)
			if (sampleinst == 1)
				thisdistrprob = cptentries[parentConfig()];
			else thisdistrprob = 1-cptentries[parentConfig()];
	}


//	public String showAllTrueWeights(){
//		return rbnutilities.arrayToString(conditionalsampleweightstrue);
//	}
//
//	public String showAllFalseWeights(){
//		return rbnutilities.arrayToString(conditionalsampleweightsfalse);
//	}

	public String showNumTrue(){
		return rbnutilities.arrayToString(numtruesamples);
	}

	public String showNumFalse(){
		return rbnutilities.arrayToString(numfalsesamples);
	}

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

	public void updateconditionalsampleweightsnew(){
		//System.out.print("+"+ sampleinst );
		//System.out.print(myatom.asString(A) + " " );
		double weight = thisdistrprob()/thissampleprob() ;
		PFNetworkNode nextpfnn;
		
		for (Iterator<BNNode> it = children.iterator(); it.hasNext();){
			nextpfnn = (PFNetworkNode)it.next();
			weight = weight * nextpfnn.thisdistrprob()/nextpfnn.thissampleprob();
		}
		
		double smallweight[] = {weight,0};
		
		if (upstreamofevidence == true){
			switch (sampleinst){
			case 0: 
				numfalsesamples[sampleparentconfig]++;
				conditionalsampleweightsfalse[sampleparentconfig]=SmallDouble.add(conditionalsampleweightsfalse[sampleparentconfig],smallweight);
				conditionalsampleweightsfalse_subsample[sampleparentconfig][sampleindex[sampleparentconfig]] 
				                                                            = SmallDouble.add(conditionalsampleweightsfalse_subsample[sampleparentconfig][sampleindex[sampleparentconfig]],smallweight);
				numfalsesamples_subsample[sampleparentconfig][sampleindex[sampleparentconfig]]++;
				break;
			case 1: 
				numtruesamples[sampleparentconfig]++;
				conditionalsampleweightstrue[sampleparentconfig]=SmallDouble.add(conditionalsampleweightstrue[sampleparentconfig],smallweight);
				conditionalsampleweightstrue_subsample[sampleparentconfig][sampleindex[sampleparentconfig]] 
				                                                           = SmallDouble.add(conditionalsampleweightstrue_subsample[sampleparentconfig][sampleindex[sampleparentconfig]],smallweight);
				numtruesamples_subsample[sampleparentconfig][sampleindex[sampleparentconfig]]++;
				break;
			}
			if (sampleindex[sampleparentconfig] == num_subsamples_adapt -1)
			{
				sampleindex[sampleparentconfig] = 0;
				updateVariance();
			}
			else
				sampleindex[sampleparentconfig]++;
		}

	}
	private void updateVariance(){

		double[] odds = new double[numvalues];
		double[] var = new double[numvalues];

		double[] allweight = SmallDouble.sumArray(conditionalsampleweights.get(sampleparentconfig));
		double[] subsweight = new double[2];
		boolean allsubsampleshaveweight = true;

		if (allweight[0] == 0)
			return;
		else odds = SmallDouble.toStandardDoubleArray(
				SmallDouble.divide(conditionalsampleweights.get(sampleparentconfig),allweight));

		for (int i=0;i<num_subsamples_adapt;i++){
			subsweight = SmallDouble.sumArray(conditionalsampleweights_subsample.get(sampleparentconfig)[i]);
			if (subsweight[0] == 0)
				allsubsampleshaveweight = false;
			else 
				var = rbnutilities.arrayAdd(var, 
						rbnutilities.arrayPow(
								rbnutilities.arraySubtract(
								SmallDouble.toStandardDoubleArray(
										SmallDouble.divide(conditionalsampleweights_subsample.get(sampleparentconfig)[i],subsweight))
								,odds)
						,2.0));
		}
		if (allsubsampleshaveweight)
			var = rbnutilities.arrayScalMult(var, num_subsamples_adapt);
		else 
			for (int i=0;i<numvalues;i++)
				var[i]=-1; //TODO: a component wise treatment might be possible 
		odds_variance.put(sampleparentconfig,var);
	}
}

