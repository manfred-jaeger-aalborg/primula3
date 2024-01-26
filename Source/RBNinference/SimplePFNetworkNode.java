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



	private double[][] cptentries;
	
	/* As constructed by BayesConstructor.makeCPT */
	private HashMap<Integer,int[]> indxToTuple = new HashMap<Integer,int[]>();

	/** conditionalsampleweights[i][j]: sum of all weights of samples in which 
	 * i'th parent configuration was sampled and j was the sampled state of this node
	 * (in a run of importance sampling).
	 * 
	 * Represented as SmallDoubles 
	 * 
	 * Initialized to [0,...,0].
	 * Only used in adaptive sampling
	 * 
	 * Marginal of conditionalsampleweights_subsample;
	 **/
	protected double[][][] conditionalsampleweights;

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
	protected int[][] numvalsamples;

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
	
	
	/** array of dimensions 2 x num_parentconfig x num states x num_samples
	 * conditionalsampleweightsfalse_subsample[i][j][k] contains the
	 * sum of weights (represented as SmallDouble) of all samples 
	 * assigned to the kth subsample 
	 * with parentconfig i where this node is sampled to j
	 **/
	protected double[][][][] conditionalsampleweights_subsample;

//	/** array of dimensions 2 x num_parentconfig x num_samples
//	 * conditionalsampleweightstrue_subsample[i][j] contains the
//	 * sum of weights (represented as SmallDouble) of all samples 
//	 * assigned to the jth subsample 
//	 * with parentconfig i where this node is sampled true
//	 **/
//	protected double[][][] conditionalsampleweightstrue_subsample;
	
	protected int[][][] numvalsamples_subsample;
//	protected int[][] numfalsesamples_subsample;


	protected double[] trueodds_variance;

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
		cptentries = new double[0][0];
		conditionalsampleweights = new double[0][0][2];
		numvalsamples = new int[0][0];
		conditionalsampleweights_subsample = new double[0][0][0][2];
		numvalsamples_subsample = new int[0][0][0];
		trueodds_variance = new double[0];
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
		Object[] cpt_and_indx = BayesConstructor.makeCPT(cpfn.cpmodel(),A,inst,atomvec);
		cptentries = (double[][])cpt_and_indx[0];
		indxToTuple = (HashMap<Integer,int[]>)cpt_and_indx[1];
		
		instantiated = cpfn.instantiated;
		sampleinst = cpfn.sampleinstVal();
		parents = cpfn.parents;
		children = cpfn.children;
		depth = cpfn.depth();
		parentnumvals = new int[parents.size()];
		for (int i=0; i<parents.size();i++) 
			parentnumvals[i]=parents.elementAt(i).getNumvalues();		
	}

	public double[][] mycpt(){
		return cptentries;
	}


	public double[][][] conditionalsampleweights(){
		return conditionalsampleweights;
	}


	/*
	 * Returns integer j if state j has probability 1 regardless of parent configuration (if 
	 * usesampleinst=true, assuming sampled states of parents). Otherwise, returns -1.
	 */
	public int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable atomhasht){
		return evaluatesTo(usesampleinst); //Dependence on A and inst here already compiled into the cpt 
	}

	public  int evaluatesTo(boolean usesampleinst)
	{
		int[] parentinst;
		boolean instconsistent;
		boolean terminate=false;
		PFNetworkNode nextpar;
		int evalsto = -1;
		int instval;

		for (int i=0;i<cptentries.length && !terminate;i++){
			/* test whether the parent configuration 
			 * corresponding to index i is consistent
			 * with instantiation
			 */
			parentinst = indxToTuple.get(i);
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
				for (int v=0;v<this.numvalues;v++) {
					if (cptentries[i][v]==1) {
						if (evalsto != -1 && evalsto!=v) {
							evalsto =-1;
							terminate = true;
						}
						if (evalsto ==-1 && !terminate)
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
		if (cptentries[0][0]!=1) return false;
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
			conditionalsampleweights =  new double[num_parentconfig][numvalues][2];
			numvalsamples = new int [num_parentconfig][numvalues];
			conditionalsampleweights_subsample = new double[num_parentconfig][numvalues][num_subsamples_adapt][2];
			numvalsamples_subsample = new int[num_parentconfig][numvalues][num_subsamples_adapt];
			trueodds_variance = new double[num_parentconfig];
			sampleindex = new int[num_parentconfig];
			for (int i =0;i<num_parentconfig;i++){
				trueodds_variance[i]=-1;
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
		double[] cpt = cptentries[sampleparentconfig];

		if (instantiated == -1){

			double rand = Math.random();
			if ( upstreamofevidence == false || 
					adaptivemode == InferenceModule.OPTION_NOT_SAMPLE_ADAPTIVE){
				if (rand < cpt){
					sampleinst = 1;
					thissampleprob = cpt; 
					thisdistrprob = cpt;
				}
				else{
					sampleinst = 0;
					thissampleprob = (1-cpt);
					thisdistrprob = (1-cpt);
				}
			}
			else{
				double prob =0;
				double[] avtrue = new double[2];
				double[] avfalse = new double[2];
				double[] avsum  = new double[2];
				//double allweight = 0;
				double trueodds = 0;
				//double numsamples = 0;
				double lambda=0;
//				double varmeasure = 0; // a combined measure that quantifies the variance 
//				// of allsampleweightsfalse_subsample and allsampleweightstrue_subsample
//				// relative to the absolute value of the estimates. Exact definition to be 
//				// experimented with!

				if (cpt == 0 || cpt == 1)
					prob = cpt;
				else{
					avtrue = SmallDouble.divide(conditionalsampleweightstrue[sampleparentconfig],
							numtruesamples[sampleparentconfig]);
					avfalse = SmallDouble.divide(conditionalsampleweightsfalse[sampleparentconfig],
							numfalsesamples[sampleparentconfig]);
					avsum = SmallDouble.add(avtrue,avfalse);
					if (avsum[0] > 0)
						trueodds = SmallDouble.toStandardDouble(SmallDouble.divide(avtrue,avsum));
					else trueodds = cpt;

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
	public int parentConfig(){
		ListIterator li = parents.listIterator();
		PFNetworkNode nextpar;
		int[] parinst = new int[parents.size()];
		int ind =0;
		int trueparentconfig;
		while (li.hasNext()){
			nextpar = (PFNetworkNode)li.next();
			parinst[ind]=nextpar.sampleinstVal();
			if (parinst[ind]==-1) System.out.println("this should not happen !!!!!");
			ind++;
		}
		trueparentconfig = rbnutilities.tupleToIndex(parinst,2);
		return trueparentconfig;
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

	/* argument A only for output strings */
	public void updateconditionalsampleweights(RelStruc A, double[] weight){
		//System.out.print("+"+ sampleinst );
		//System.out.print(myatom.asString(A) + " " );
		if (upstreamofevidence == true){
			switch (sampleinst){
			case 0: 
				numfalsesamples[sampleparentconfig]++;
				conditionalsampleweightsfalse[sampleparentconfig]=SmallDouble.add(conditionalsampleweightsfalse[sampleparentconfig],weight);
				conditionalsampleweightsfalse_subsample[sampleparentconfig][sampleindex[sampleparentconfig]] 
				                                                            = SmallDouble.add(conditionalsampleweightsfalse_subsample[sampleparentconfig][sampleindex[sampleparentconfig]],weight);
				numfalsesamples_subsample[sampleparentconfig][sampleindex[sampleparentconfig]]++;
				break;
			case 1: 
				numtruesamples[sampleparentconfig]++;
				conditionalsampleweightstrue[sampleparentconfig]=SmallDouble.add(conditionalsampleweightstrue[sampleparentconfig],weight);
				conditionalsampleweightstrue_subsample[sampleparentconfig][sampleindex[sampleparentconfig]] 
				                                                           = SmallDouble.add(conditionalsampleweightstrue_subsample[sampleparentconfig][sampleindex[sampleparentconfig]],weight);
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

		double trueodds = 0;
		double var = 0;

		double[] allweight = SmallDouble.add(conditionalsampleweightsfalse[sampleparentconfig],
					conditionalsampleweightstrue[sampleparentconfig]);
		double[] subsweight = new double[2];
		boolean allsubsampleshaveweight = true;

		if (allweight[0] == 0)
			return;
		else trueodds = SmallDouble.toStandardDouble(
				SmallDouble.divide(conditionalsampleweightstrue[sampleparentconfig],allweight));

		for (int i=0;i<num_subsamples_adapt;i++){
			subsweight = SmallDouble.add(conditionalsampleweightsfalse_subsample[sampleparentconfig][i] 
			                                                                                         ,conditionalsampleweightstrue_subsample[sampleparentconfig][i]);
			if (subsweight[0] == 0)
				allsubsampleshaveweight = false;
			else 
				var = var + Math.pow(SmallDouble.toStandardDouble(SmallDouble.divide(conditionalsampleweightstrue_subsample[sampleparentconfig][i],subsweight)) - trueodds,2.0);
		}
		if (allsubsampleshaveweight)
			var = var/num_subsamples_adapt;
		else var = -1;
		trueodds_variance[sampleparentconfig]=var;
	}
}

