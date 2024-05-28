/*
 * GGConvCombNode.java 
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

import myio.StringOps;
import RBNpackage.*;
import RBNgui.*;
import RBNExceptions.*;
import RBNutilities.*;
import RBNinference.*;



/** In a GGSoftMaxNode the children vector has as many
 * elements as the categorical variable has values.
 *  A child element is set to null if the probabilityformula  for 
 *  a value is constant
 */
public class GGSoftMaxNode extends GGCPMNode{


	/*
	 * Stores for each probability formula defining the softmax: 
	 * 
	 *  - its fixed value if it does not depend on any unknown atoms or parameters
	 *  - NaN otherwise
	 */
	Double[] evalOfPFs; 
	/*
	 * The current values of all probability formulas. Equal to evalOfPFs in components where
	 * evalOfPFs != NaN
	 */
	double[] current_evalofpfs;
	

	/** cpmsm must be ground ! */
	public GGSoftMaxNode(GradientGraphO gg,
			CatModelSoftMax cpmsm, 
			Hashtable<String,GGCPMNode> allnodes, 
			RelStruc A, 
			OneStrucData I,
			int inputcaseno,
			int observcaseno,
			Hashtable<String,Integer> parameters,
			boolean useCurrentPvals,
			Hashtable<Rel,GroundAtomList> mapatoms,
			Hashtable<String,Object[]>  evaluated )
					throws RBNCompatibilityException
	{
		super(gg,cpmsm,A,I);

		evalOfPFs = new Double[cpmsm.numvals()];
		current_evalofpfs = new double[cpmsm.numvals()];

		for (int i = 0; i<cpmsm.numvals(); i++){
			ProbForm pf = cpmsm.pfAt(i);
			evalOfPFs[i]= (double)(pf.evaluate(A, 
					I , 
					new String[0], 
					new int[0] , 
					false,
					useCurrentPvals,
					mapatoms,
					false,
					evaluated,
					parameters,
					ProbForm.RETURN_ARRAY,
					true,
					null)[0]);

			if (evalOfPFs[i].isNaN()) {
				GGCPMNode nextchild = GGCPMNode.constructGGPFN(gg,
						pf,
						allnodes, 
						A, 
						I,
						inputcaseno,observcaseno,
						parameters,
						false,
						false,
						"",
						mapatoms,
						evaluated);
				children.add(nextchild);
				nextchild.addToParents(this);
			}
			else {
				children.add(null);
				current_evalofpfs[i]=evalOfPFs[i];
			}
		}
	}



	private void evaluatePFs(Integer sno) {
		for (int i=0;i<current_evalofpfs.length;i++) {
			if (children.elementAt(i)!=null)
				current_evalofpfs[i]=children.elementAt(i).evaluate(sno);		
		}
	}

	public double evaluate(Integer sno){

		// Returns the probability value for the this.instval value
		// Since GGSoftMaxNodes can only be upper ground atom nodes, this.instval
		// always is defined
		
		if (is_evaluated) {
			if (this.values_for_samples==null)
				return (double)value;
			else
				return this.values_for_samples[sno];
		}
		
		this.evaluatePFs(sno);
		double[] softm_pfs=rbnutilities.softmax(current_evalofpfs);
		
		double result =softm_pfs[this.instval()];
		
		if (sno==null)
			value = result;
		else
			values_for_samples[sno]=result;
		return result;
	}



	public void evaluateBounds(){
		System.out.println("GGSoftMaxNode.evaluateBounds is called but not implemented!");
	}

	public double evaluateGrad(String param)
	throws RBNNaNException
	{
		if (gradient.get(param)== null){
			return 0.0;
		}
		else {
			double currval = gradient.get(param);
			if (!Double.isNaN(currval)){
				return currval;
			}
		}
		
		//TODO: for now prioritize transparency over efficiency; should check whether 
		// better arrangements can be found for combining evaluate and evaluateGrad
		// and simultaneous gradient evaluation for all parameters
		this.evaluatePFs(0); //TODO: this must be fixed!
		double pfsum = rbnutilities.arraySum(current_evalofpfs);
		
		double derivsum = 0;
		for (int i=0;i<current_evalofpfs.length && children.elementAt(i)!=null;i++) {
			derivsum+=Math.exp(current_evalofpfs[i])*children.elementAt(i).evaluateGrad(param);
		}
		
		double result= 0;
		if (children.elementAt(this.instval())!=null)
			result += children.elementAt(this.instval()).evaluateGrad(param)*pfsum;
		result -= current_evalofpfs[this.instval()]*derivsum;
		result /= Math.pow(derivsum,2);

		gradient.put(param,result);
		
		//System.out.println("Gradient value (GGConvC): " + result);
		return result;
	}

}
