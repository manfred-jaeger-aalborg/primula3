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
import RBNExceptions.*;


public class ComplexPFNetworkNode extends PFNetworkNode{

	private ProbForm probform;
	

	public ComplexPFNetworkNode(ComplexBNGroundAtomNode cgan){
		super(cgan.myatom());
		probform = cgan.probform;
	}

	public ComplexPFNetworkNode(Rel r,String arnames, int[] ar,ProbForm pf) {
		super(new GroundAtom(r,ar));
		probform = pf;
		sampleinst = -1;
		truesampleweight = new double[2];
	}

	public ComplexPFNetworkNode(GroundAtom ga, ProbForm pf) {
		super(ga);
		probform=pf;
	}
	
	/* Computes the conditional probability of the instantiation of this node
	 * in the current sample, given the instantiations in this sample of 
	 * the parent nodes
	 */
	private double condProb(RelStruc A,Hashtable atomhasht,OneStrucData inst,long[] timers)
			throws RBNCompatibilityException
			{
		double result = probform.evalSample(A,atomhasht,inst,timers);
		//System.out.print(" cP: " + result);
		return result;
			}


	public  int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable atomhasht )
			throws RBNCompatibilityException
	{
		//System.out.print(" ceto " + this.myatom().asString(A));
		int result = probform.evaluatesTo(A,inst,usesampleinst,atomhasht);
		//System.out.print(" " + result);
		return result;
	}




	public void initializeForSampling(int sampleordmode, 
			int adaptivemode,
			GroundAtomList queryatoms, 
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
		if (parents.size()!=0) return false;
		if (children.size()!=0) return false;
		if (probform.evaluatesTo(rels)!=0) return false;
		if (instantiated != -1) return false;
		return true;
	}


	public ProbForm probform(){
		return probform;
	}


	public void sampleForward(RelStruc A,Hashtable atomhasht,OneStrucData inst,int adaptivemode,long[] timers)
			throws RBNCompatibilityException
			/* adaptivemode argument not used (adaptive=non-adaptive in forward sampling for ComplexPFNNodes) */
			{
		//System.out.println("<" + myatom().asString(A));
		double prob = condProb(A,atomhasht,inst,timers);
		if (prob > 1 || prob <0)
			System.out.println("#####################found prob " + prob);
		if (instantiated == -1){

			double rand = Math.random();
			if (rand < prob){
				sampleinst = 1;
				thissampleprob = prob;
				thisdistrprob = prob; 
			}
			else{
				sampleinst = 0;
				thissampleprob = 1-prob;
				thisdistrprob = 1-prob; 
			}
		}
		else {
			sampleinst = instantiated;
			thissampleprob = 1;
			switch (instantiated){
			case 1: thisdistrprob = prob;
			break;
			case 0: thisdistrprob = (1-prob);
			}
		}
			}





	public  void setDistrProb(RelStruc A, Hashtable atomhasht,OneStrucData inst,long[] timers)
			throws RBNCompatibilityException
			{
		if (thisdistrprob == -1)
			if (sampleinst == 1)
				thisdistrprob = condProb( A,atomhasht,inst, timers);
			else thisdistrprob = 1-condProb( A,atomhasht,inst, timers);
			}

	public void sEval(RelStruc A)
			throws RBNCompatibilityException
			{
		probform = probform.sEval(A);
			}
	

}
