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
import RBNgui.*;
import RBNExceptions.*;


/** In a GGConvCombNode the children vector has exactly
 *  3 elements, one for each of the three subformulas
 *  An element is set to null if the formula  does not actually
 *  need to be contained in this vector.
 */
public class GGConvCombNode extends GGCPMNode{


	double[] evalOfSubPFs;


	/** pf must be a ground ProbForm ! */
	public GGConvCombNode(GradientGraphO gg,
			ProbForm pf, 
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
		super(gg,pf,A,I);
	
		evalOfSubPFs = new double[3];
		
		for (int i = 0; i<3; i++){
			evalOfSubPFs[i]= (double)((ProbFormConvComb)pf).subPF(i+1).evaluate(A, 
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
					null)[0];
		}


		boolean constructthis;
		GGCPMNode constructedchild;

		for (int i = 0; i<3; i++){
			constructthis = true;
			if (!Double.isNaN(evalOfSubPFs[i]))
				constructthis = false;
			if (i==1 && evalOfSubPFs[0]==0)
				constructthis = false;
			if (i==2 && evalOfSubPFs[0]==1)
				constructthis = false;
			if (constructthis){
				constructedchild = GGCPMNode.constructGGPFN(gg,
						((ProbFormConvComb)pf).subPF(i+1),
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
				//System.out.println("add child " + constructedchild.name());
				
				children.add(constructedchild);
				constructedchild.addToParents(this);
			}
			else{
				children.add(null);
			}
		}


		if (!(pf instanceof ProbFormConvComb)){
			System.out.println("Cannot create GGConvCombNode from ProbForm " + pf.asString(Primula.CLASSICSYNTAX,0,null,false,false));
		}
					}




	public double[] evaluate(Integer sno){

		if (this.depends_on_sample && sno==null) {
			for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
				this.evaluate(i);
			return null;
		}			
		if (this.depends_on_sample && is_evaluated_for_samples[sno]) 
				return this.values_for_samples[sno];
		if (!this.depends_on_sample && is_evaluated_for_samples[0])
			return this.values_for_samples[0];

		
		double r = 0;
		GGCPMNode F0 = children.elementAt(0);
		GGCPMNode F1 = children.elementAt(1);
		GGCPMNode F2 = children.elementAt(2);

		double f0val;
		double f1val;
		double f2val;

		if (F0 != null) {
			f0val = F0.evaluate(sno)[0];
		}
		else
			f0val = evalOfSubPFs[0];

		if (F1 != null)
			f1val = F1.evaluate(sno)[0];
		else
			f1val = evalOfSubPFs[1];

		if (F2 != null)
			f2val = F2.evaluate(sno)[0];
		else
			f2val = evalOfSubPFs[2];

		if (f0val != 0)
			r = f0val*f1val;
		if (f0val != 1)
			r = r + (1-f0val)*f2val;
		
		if (Double.isNaN(r))
			System.out.println("result = NaN in evaluate for convcomb.func " );

		double[] result = new double[]{r};

		if (this.depends_on_sample) {
			values_for_samples[sno] = result;
			is_evaluated_for_samples[sno]=true;
		}
		else {
			values_for_samples[0] = result;
			is_evaluated_for_samples[0]=true;
		}
		
		return result;
	}



//	public void evaluateBounds(){
//		if (bounds[0]==-1){ /* Not yet evaluated for current indicator setting */
//			//	    System.out.println("convcombnode.evaluateBounds");
//			GGCPMNode F0 = children.elementAt(0);
//			GGCPMNode F1 = children.elementAt(1);
//			GGCPMNode F2 = children.elementAt(2);
//			double lowF0;
//			double uppF0;
//			double lowF1;
//			double uppF1;
//			double lowF2;
//			double uppF2;
//			if (F0!=null){
//				F0.evaluateBounds();
//				lowF0=F0.lowerBound();
//				uppF0=F0.upperBound();
//			}
//			else{
//				lowF0=evalOfSubPFs[0];
//				uppF0=evalOfSubPFs[0];
//			}
//			if (F1!=null){
//				F1.evaluateBounds();
//				lowF1=F1.lowerBound();
//				uppF1=F1.upperBound();
//			}
//			else{
//				lowF1=evalOfSubPFs[1];
//				uppF1=evalOfSubPFs[1];
//			}
//			if (F2!=null){
//				F2.evaluateBounds();
//				lowF2=F2.lowerBound();
//				uppF2=F2.upperBound();
//			}
//			else{
//				lowF2=evalOfSubPFs[2];
//				uppF2=evalOfSubPFs[2];
//			}
//			/* Find lower bound. The bound is not necessarily achievable,
//			 * since the bounds on 3 sub-formulas may not be independently
//			 * achievable
//			 */
//			if (lowF1 > lowF2)
//				bounds[0]=lowF0*lowF1 + (1-lowF0)*lowF2;
//			else
//				bounds[0]=uppF0*lowF1 + (1-uppF0)*lowF2;
//			/* Similar for the upper bound */
//			if (uppF1 > uppF2)
//				bounds[1]=uppF0*uppF1 + (1-uppF0)*uppF2;
//			else
//				bounds[1]=lowF0*uppF1 + (1-lowF0)*uppF2;
//		}
//	}

	public TreeMap<String,double[]> evaluateGradient(Integer sno)
	throws RBNNaNException
	{
//		String label="";
//		if (this.isuga())
//			label=this.getMyatom();
//		else
//			label=Integer.toString(this.identifier());
//		System.out.println("(Conv)evalPD for " + label + " sno " + sno);
		

		if (this.depends_on_sample && sno==null) {
			for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
				this.evaluateGradient(i);
			return null;
		}
		
		int idx=0;
		if (this.depends_on_sample)
			idx=sno;
		
		TreeMap<String,double[]> g = gradient_for_samples.get(sno);
		if (g!=null)
			return g;
		
		TreeMap<String,double[]> result = null;	
		

		double[] childvals=new double[3];
		Vector<TreeMap<String,double[]>> childgradients = new Vector<TreeMap<String,double[]>>();
		for (int i=0;i<3;i++) {
			if (children.elementAt(i)!=null) {
				childvals[i]=children.elementAt(i).evaluate(idx)[0];
				childgradients.add(children.elementAt(i).evaluateGradient(sno));
			}
			else {
				childvals[i]=evalOfSubPFs[i];
				childgradients.add(new TreeMap<String,double[]>());
			}
		}
		
		


		for (String param: this.myparameters) {
			double partderiv = 0;
			/* F0'F1: */
			partderiv += childgradients.elementAt(0).get(param)[0]*childvals[1];
			/* +F0F1': */
			partderiv +=childvals[0]*childgradients.elementAt(1).get(param)[0];
			/* -F0'F2: */
			partderiv -=childgradients.elementAt(0).get(param)[0]*childvals[2];
			/* -F0F2': */
			partderiv -=childvals[0]*childgradients.elementAt(2).get(param)[0];
			/* +F2' */
			partderiv +=childgradients.elementAt(2).get(param)[0];

			result.put(param, new double[] {partderiv});
		}

		gradient_for_samples.remove(idx);
		gradient_for_samples.add(idx,result);

		return result;
	}

	@Override
	public boolean isBoolean() {
		return true;
	}
}
