/*
 * ComplexPFNetworkNode.java 
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
import RBNutilities.rbnutilities;
import RBNExceptions.*;


public class ComplexPFNetworkNode extends PFNetworkNode{

	private CPModel cpmodel;
	

	public ComplexPFNetworkNode(ComplexBNGroundAtomNode cgan){
		super(cgan.myatom());
		cpmodel = cgan.cpmodel;
	}

	public ComplexPFNetworkNode(Rel r,String arnames, int[] ar,CPModel pf) {
		super(new GroundAtom(r,ar));
		cpmodel = pf;
		sampleinst = -1;
		valsampleweight = new double[this.numvalues][2];
	}

	public ComplexPFNetworkNode(GroundAtom ga, CPModel pf) {
		super(ga);
		cpmodel=pf;
	}
	
	/* Computes the conditional probability of the instantiation of this node
	 * in the current sample, given the instantiations in this sample of 
	 * the parent nodes
	 */
	private double[] condProb(RelStruc A,
			Hashtable<String,PFNetworkNode> atomhasht,
			OneStrucData inst,
    		Hashtable<String,double[]> evaluated,
			long[] timers)
			throws RBNCompatibilityException
    {
		if (cpmodel instanceof CatGnn) {
			if (((CatGnn) cpmodel).getGnnPy() == null)
				((CatGnn) cpmodel).setGnnPy(gnnPy);
		}
        double[] result = cpmodel.evalSample(A,atomhasht,inst,evaluated,timers);
        //System.out.print(" cP: " + result);
        return result;
    }


	public  int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable atomhasht )
			throws RBNCompatibilityException
	{
		if (!(this.cpmodel() instanceof ProbForm))
			throw new RBNCompatibilityException();
		int result = ((ProbForm)cpmodel).evaluatesTo(A,inst,usesampleinst,atomhasht);
		//System.out.print(" " + result);
		return result;
	}




	public void initializeForSampling(int sampleordmode, 
			int adaptivemode,
			Hashtable<Rel,GroundAtomList> queryatoms,
			int num_subsamples_minmax,
			int num_subsamples_adapt){
		super.initializeForSampling(sampleordmode,
				adaptivemode,
				queryatoms,
				num_subsamples_minmax,
				num_subsamples_adapt);
	}

	public boolean isIsolatedZeroNode(RelStruc rels)
			throws RBNCompatibilityException{
		if (!(this.cpmodel() instanceof ProbForm))
			throw new RBNCompatibilityException();
		if (parents.size()!=0) return false;
		if (children.size()!=0) return false;
		if (((ProbForm)cpmodel).evaluatesTo(rels)!=0) return false;
		if (instantiated != -1) return false;
		return true;
	}


	public CPModel cpmodel(){
		return cpmodel;
	}


	public void sampleForward(RelStruc A,
			Hashtable<String,PFNetworkNode> atomhasht,
			OneStrucData inst,
			int adaptivemode,
			Hashtable<String,double[]> evaluated,
			long[] timers)
					throws RBNCompatibilityException
					/* adaptivemode argument not used (adaptive=non-adaptive in forward sampling for ComplexPFNNodes) */
	{
		// System.out.println("<" + myatom().asString(A));



		//	double[] prob = condProb(A,atomhasht,inst,evaluated,timers);
		double[] probs = null;
		if (cpmodel instanceof ProbForm) {
			double p = condProb(A,atomhasht,inst,evaluated,timers)[0];
			probs = new double[] {1-p,p};
		}
		else
			probs = condProb(A,atomhasht,inst,evaluated,timers);

		if (instantiated == -1){
			sampleinst = rbnutilities.sampledValue(probs);
			thissampleprob = probs[sampleinst];
			thisdistrprob = thissampleprob;
		}
		else {
			sampleinst = instantiated;
			thissampleprob = 1;
			thisdistrprob = probs[instantiated];
		}
	}

	public  void setDistrProb(RelStruc A,
			Hashtable<String,PFNetworkNode> atomhasht,
			OneStrucData inst,
			Hashtable<String,double[]> evaluated,
			long[] timers)
					throws RBNCompatibilityException
	{
		if (thisdistrprob == -1) {
			double[] probs = null;
		if (cpmodel instanceof ProbForm) {
			double p = condProb(A,atomhasht,inst,evaluated,timers)[0];
			probs = new double[] {1-p,p};
		}
		else
			probs = condProb(A,atomhasht,inst,evaluated,timers);

		thisdistrprob = probs[sampleinst];
		}
	}

	public void sEval(RelStruc A)
			throws RBNCompatibilityException
			{
		cpmodel = cpmodel.sEval(A);
			}
	
	public int numvals() {
		if (cpmodel instanceof ProbForm)
			return 2;
		else return ((CatModel)cpmodel).numvals();
	}

}
