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

import RBNpackage.*;
import RBNExceptions.*;
import RBNutilities.*;


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
	
	
//	/*
//	 * The current values of all probability formulas. Equal to evalOfPFs in components where
//	 * evalOfPFs != NaN
//	 */
//	double[] current_evalofpfs;
	

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
		outDim = cpmsm.numvals();

		evalOfPFs = new Double[outDim];
//		current_evalofpfs = new double[cpmsm.numvals()];

		for (int i = 0; i<outDim; i++){
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
//				current_evalofpfs[i]=evalOfPFs[i];
			}
		}
	}



//	private void evaluatePFs(Integer sno) {
//		for (int i=0;i<current_evalofpfs.length;i++) {
//			if (children.elementAt(i)!=null)
//				current_evalofpfs[i] = children.elementAt(i).evaluate(sno)[0]; // return the first element, all the children must be scalar! (scalar values for the softmax function)
//		}
//	}

	public double[] evaluate(Integer sno){
		
		if (this.depends_on_sample && sno==null) {
			for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
				this.evaluate(i);
			return null;
		}			
		if (this.depends_on_sample && is_evaluated_val_for_samples[sno])
				return this.values_for_samples[sno];
		if (!this.depends_on_sample && is_evaluated_val_for_samples[0])
			return this.values_for_samples[0];

		
		double[] valsofpfs = new double[this.outDim];
		
		for (int i=0;i<this.outDim;i++) {
			if (children.elementAt(i)!=null)
				valsofpfs[i] = children.elementAt(i).evaluate(sno)[0]; // return the first element, all the children 
			                                                           // must be scalar! (scalar values for the softmax function)
			else
				valsofpfs[i] = evalOfPFs[i];
		}

		double[] result = rbnutilities.softmax(valsofpfs);

		
		if (this.depends_on_sample) {
			values_for_samples[sno] = result;
			is_evaluated_val_for_samples[sno]=true;
		}
		else {
			values_for_samples[0] = result;
			is_evaluated_val_for_samples[0]=true;
		}
		return result;
	}

	@Override
	public boolean isBoolean() {
		return false;
	}


//	public void evaluateBounds(){
//		System.out.println("GGSoftMaxNode.evaluateBounds is called but not implemented!");
//	}

	public Gradient evaluateGradient(Integer sno)
			throws RBNNaNException
	{

		if (this.depends_on_sample && sno==null) {
			for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
				this.evaluateGradient(i);
			return null;
		}

		int idx=0;
		if (this.depends_on_sample)
			idx=sno;

		if (is_evaluated_grad_for_samples[idx])
			return  gradient_for_samples.get(idx);

		Gradient result = gradient_for_samples.get(idx);
		result.reset();

		double[] values = values_for_samples[idx];



		for (String param: this.myparameters) {
			double[][] childpds = new double[children.size()][];
			for (int i=0;i<children.size();i++) {
				if (children.elementAt(i)!=null)
					childpds[i]=children.elementAt(i).evaluateGradient(sno).get_part_deriv(param);
			}

			double[] partderiv = new double[this.outDim];
			double derivsum = 0;
			for (int i=0;i<this.outDim();i++) {
				if (childpds[i]!=null)
					derivsum+=values[i]*childpds[i][0];
			}

			for (int i=0;i<this.outDim;i++) {
				if (childpds[i]!=null)
					partderiv[i]=values[i]*(childpds[i][0]-derivsum);
			}
			result.set_part_deriv(param, partderiv);
		}

		//System.out.println("Gradient value (GGConvC): " + result);
		return result;
	}

}
