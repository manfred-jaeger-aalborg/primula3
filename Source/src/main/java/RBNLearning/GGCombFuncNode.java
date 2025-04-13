/*
* GGCombFuncNode.java 
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


public class GGCombFuncNode extends GGCPMNode{

	int typeOfComb; /* The combination function used */

	/* Contains the values of those ProbForms in the argument
	 * of the combination function that do not depend on 
	 * unobserved probabilistic atoms or unknown parameters
	 */
	double[] valuesOfSubPFs; 

	/* A relevant aggregate value of the elements for valuesOfSubPFs
	 * Depends on typeOfComb. Used in the computations of the gradients.
	 */
	double aggregateOfSubPFs;
	
	public GGCombFuncNode(GradientGraphO gg,
			ProbForm pf, 
			Hashtable allnodes,
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
		
		
		DoubleVector vals = new DoubleVector();

		ProbFormCombFunc pfcomb = (ProbFormCombFunc)pf;
		typeOfComb = pfcomb.getMycombInt();

		//double starttime=System.currentTimeMillis();
		int[][] subslist = pfcomb.tuplesSatisfyingCConstr(A, new String[0], new int[0]);
		//thisgg.profiler.time1 +=(System.currentTimeMillis()-starttime);
		//System.out.println("tt1:" + thisgg.profiler.time1);
				
		children = new Vector<GGCPMNode>();


		/* For all probability formulas in the argument of the combination function, and all
		 * tuples in subslist: Check whether the formula evaluates for this tuple to a constant.
		 * If yes, add this value to vals; if no, create a new child node for this probability
		 * formula
		 */
		ProbForm nextsubpf;
		ProbForm groundnextsubpf;
		double evalOfSubPF;
		GGCPMNode constructedchild;
		
		for (int i=0; i<pfcomb.numPFargs(); i++)
		{
			nextsubpf = pfcomb.probformAt(i);
			for (int j=0; j<subslist.length; j++)
			{
				groundnextsubpf = nextsubpf.substitute(pfcomb.quantvars(),subslist[j]);
				
				double starttime = System.currentTimeMillis();
				
				evalOfSubPF = (double)groundnextsubpf.evaluate(A, I, new String[0], new int[0] , false,
						useCurrentPvals,
						mapatoms,false,evaluated,parameters,ProbForm.RETURN_ARRAY,true,null)[0];
				
				double midtime = System.currentTimeMillis();
				
				
				if (Double.isNaN(evalOfSubPF)){
					constructedchild = GGCPMNode.constructGGPFN(gg,
							groundnextsubpf,
							allnodes, 
							A, 
							I,
							inputcaseno,
							observcaseno,
							parameters,
							false,
							false,
							"",
							mapatoms,
							evaluated);
					children.add(constructedchild);
					constructedchild.addToParents(this);
				}
				else
					vals.add(evalOfSubPF);
				
				double endtime = System.currentTimeMillis();				
				

			} /* for (int j=0; j<subslist.length; j++) */
		}/* for (int i=0; i<pfcomb.numPFargs(); i++) */
		valuesOfSubPFs = vals.asArray();
		switch (typeOfComb){
		case CombFunc.NOR:
			aggregateOfSubPFs = 1;
			for (int i=0;i<valuesOfSubPFs.length;i++)
				aggregateOfSubPFs = aggregateOfSubPFs*(1-valuesOfSubPFs[i]);
			break;
		case CombFunc.LREG:
			aggregateOfSubPFs = 0;
			for (int i=0;i<valuesOfSubPFs.length;i++)
				aggregateOfSubPFs = aggregateOfSubPFs +valuesOfSubPFs[i] ;
			break;
		case CombFunc.LLREG: // same as for LREG
			aggregateOfSubPFs = 0;
			for (int i=0;i<valuesOfSubPFs.length;i++)
				aggregateOfSubPFs = aggregateOfSubPFs +valuesOfSubPFs[i] ;
			break;
		case CombFunc.PROD: 
			aggregateOfSubPFs = 1;
			for (int i=0;i<valuesOfSubPFs.length;i++)
				aggregateOfSubPFs = aggregateOfSubPFs*valuesOfSubPFs[i];
			break;	
		}	
		
	}


	private double computeCombFunc(double[] args){
		double result = 0;
		switch (typeOfComb){
		case CombFunc.NOR:
			result = thisgg.computeCombFunc(CombFunc.NOR,args);
			break;
		case CombFunc.MEAN: 
			result = thisgg.computeCombFunc(CombFunc.MEAN,args);
			break;
		case CombFunc.INVSUM:
			result = thisgg.computeCombFunc(CombFunc.INVSUM,args);;
			break;
		case CombFunc.ESUM:
			result = thisgg.computeCombFunc(CombFunc.ESUM,args);;
			break;
		case CombFunc.LREG:
			result = thisgg.computeCombFunc(CombFunc.LREG,args);;
			break;
		case CombFunc.LLREG:
			result = thisgg.computeCombFunc(CombFunc.LLREG,args);;
			break;
		case CombFunc.SUM:
			result = thisgg.computeCombFunc(CombFunc.SUM,args);;
			break;
		case CombFunc.PROD:
			result = thisgg.computeCombFunc(CombFunc.PROD,args);;
			break;
		}
		return result;
	}

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

		
		/* Construct an argument array for the combination function: */
		double[] args = new double[valuesOfSubPFs.length+ children.size()];
		for (int i=0;i<valuesOfSubPFs.length;i++)
			args[i]=valuesOfSubPFs[i];
		for (int i=0;i<children.size();i++)
			args[i+valuesOfSubPFs.length]= children.elementAt(i).evaluate(sno)[0];
		double r = computeCombFunc(args);
		if (Double.isNaN(r))
			System.out.println("result = NaN in evaluate for comb.func " );

		double[] result = new double[]{r};

		
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

//	public void evaluateBounds(){
//		if (bounds[0]== -1){
//			//	    System.out.println("combfuncnode.evaluateBounds");
//			/* First set bounds at children: */
//			for (int i=0;i<children.size();i++)
//				children.elementAt(i).evaluateBounds();
//			/* Determine arrays of lower and upper bounds for sub-formulas */
//			double[] lowargs = new double[valuesOfSubPFs.length+ children.size()];
//			for (int i=0;i<valuesOfSubPFs.length;i++)
//				lowargs[i]=valuesOfSubPFs[i];
//			for (int i=0;i<children.size();i++)
//				lowargs[i+valuesOfSubPFs.length]= children.elementAt(i).lowerBound();
//			double[] uppargs = new double[valuesOfSubPFs.length+ children.size()];
//			for (int i=0;i<valuesOfSubPFs.length;i++)
//				uppargs[i]=valuesOfSubPFs[i];
//			for (int i=0;i<children.size();i++)
//				uppargs[i+valuesOfSubPFs.length]= children.elementAt(i).upperBound();
//			/* NOR and MEAN are monotone in their arguments, INVSUM anti-monotone */
//			switch (typeOfComb){
//			case CombFunc.NOR:
//				bounds[0] = thisgg.computeCombFunc(CombFunc.NOR,lowargs);
//				bounds[1] = thisgg.computeCombFunc(CombFunc.NOR,uppargs);
//				break;
//			case CombFunc.MEAN: 
//				bounds[0] = thisgg.computeCombFunc(CombFunc.MEAN,lowargs);
//				bounds[1] = thisgg.computeCombFunc(CombFunc.MEAN,uppargs);
//				break;
//			case CombFunc.INVSUM:		
//				bounds[0] = thisgg.computeCombFunc(CombFunc.INVSUM,uppargs);
//				bounds[1] = thisgg.computeCombFunc(CombFunc.INVSUM,lowargs);
//				break;
//			}
//		}
//	}

	public Gradient evaluateGradient(Integer sno)
			throws RBNNaNException{
		
//		String label="";
//		if (this.isuga())
//			label=this.getMyatom();
//		else
//			label=Integer.toString(this.identifier());
//		System.out.println("(Comb)evalPD for " + label + " sno " + sno);
		
//		if (!dependsOn(param)) {
//			System.out.println("... no dependence");
//			return new Double[] {0.0}; // In this case need not fill the gradient_for_samples array
//		}


		if (this.depends_on_sample && sno==null) {
			for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
				this.evaluateGradient(i);
			return null;
		}

		/* obtain the relevant index for the gradient_for_samples array
		 */
		int idx=0;
		if (this.depends_on_sample)
			idx=sno;

		if (is_evaluated_grad_for_samples[idx])
			return  gradient_for_samples.get(idx);


	/*
	 * Now the 'main' case: evaluated the gradient for a specific sample number sno.
	 * (sno=0 if no dependence on samples)
		 */

		Gradient result = null;
		switch (typeOfComb){
		case CombFunc.NOR:
			result = computeGradientNOR(idx);
			break;

		case CombFunc.MEAN:
			result = computeGradientMEAN(idx);
			break;

		case CombFunc.INVSUM:
			result = computeGradientINVSUM(idx);
			break;

		case CombFunc.ESUM:
			result = computeGradientESUM(idx);
			break;

		case CombFunc.LREG:
			result = computeGradientLREG(idx);
			break;
			
		case CombFunc.LLREG:
			result = computeGradientLLREG(idx);
			break;
			
		case CombFunc.SUM:
			result = computeGradientSUM(idx);
			break;
		case CombFunc.PROD:
			result = computeGradientPROD(idx);
			break;
		}

		is_evaluated_grad_for_samples[idx]=true;

		return result;
	}



	private Gradient computeGradientNOR(Integer idx)
	throws RBNNaNException
	{
		Gradient result = gradient_for_samples.get(idx);
		result.reset();

        /* First compute \prod (1-Fi) over all subformulas */
        double factor = aggregateOfSubPFs;
        Vector<Gradient> childgrads = new Vector<Gradient>();
        double[] childvals = new double[children.size()];
        for (int i=0;i<children.size();i++) {
        		childvals[i]=children.elementAt(i).evaluate(idx)[0];
                factor = factor*(1-childvals[i]);
                childgrads.add(children.elementAt(i).evaluateGradient(idx));
        }
        if (factor == 0)
        	return result;
        
        /* Now compute the partial derivatives as
         *
         * \sum_{F_i} (factor/(1-F_i))*(F_i')
         */
        
        for (String param: this.myparameters) {
        	double partderiv = 0;
        	for (int i=0;i<children.size();i++){
        		double[] childpartderiv = childgrads.elementAt(i).get_part_deriv(param); // will be null or 1-dim array
        		if (childpartderiv != null)
        			partderiv = partderiv + 
        			(factor/(1-childvals[i])*childpartderiv[0]);
        	}
        	result.set_part_deriv(param, new double[] {partderiv});
        }
		return result;
	}

	private Gradient  computeGradientPROD(Integer idx)
			throws RBNNaNException{
		Gradient result = gradient_for_samples.get(idx);
		result.reset();


		/* \prod Fi over all subformulas */
        double factor = aggregateOfSubPFs;
        Vector<Gradient> childgrads = new Vector<Gradient>();
        double[] childvals = new double[children.size()];
        for (int i=0;i<children.size();i++) {
        		childvals[i]=children.elementAt(i).evaluate(idx)[0];
                factor = factor*childvals[i];
                childgrads.add(children.elementAt(i).evaluateGradient(idx));
        }
        
        
        if (factor == 0)
        	return result;
        
        /* Now compute the partial derivative as
         *
         * \sum_{F_i} (factor/F_i)*(F_i')
         */
 
        for (String param: this.myparameters) {
        	double partderiv = 0;
        	for (int i=0;i<children.size();i++){
        		double[] childpartderiv = childgrads.elementAt(i).get_part_deriv(param); // will be null or 1-dim array
        		if (childpartderiv != null)
        			partderiv = partderiv + 
        			(factor/(childvals[i])*childpartderiv[0]);
        	}
        	result.set_part_deriv(param, new double[] {partderiv});
        }
		return result;
	}

	private Gradient  computeGradientMEAN(Integer idx)
			throws RBNNaNException{

		Gradient result = gradient_for_samples.get(idx);
		result.reset();


		for (String param: this.myparameters) {
			double partderiv = 0;
			for (int i=0;i<children.size();i++)
				partderiv = partderiv + children.elementAt(i).evaluateGradient(idx).get_part_deriv(param)[0];

			partderiv = partderiv/(valuesOfSubPFs.length + children.size());
        	result.set_part_deriv(param, new double[] {partderiv});
		}

		return result;
	}

	private Gradient  computeGradientLREG(Integer idx)
			throws RBNNaNException{

		Gradient result = gradient_for_samples.get(idx);
		result.reset();

		double sum = aggregateOfSubPFs;
        Vector<Gradient> childgrads = new Vector<Gradient>();
        double[] childvals = new double[children.size()];
        
		for (int i=0;i<children.size();i++){
			childvals[i]=children.elementAt(i).evaluate(idx)[0];
			sum = sum + childvals[i];
			childgrads.add(children.elementAt(i).evaluateGradient(idx));
		}
		double esum = Math.exp(sum);
		// Watch: can be issues with infinite values ...?
		
		for (String param: this.myparameters) {
			double partderiv = 0;
			for (int i=0;i<children.size();i++){
				double[] childgrad = childgrads.elementAt(i).get_part_deriv(param);
				if (childgrad != null)
					partderiv+=childgrad[0];
			}
			partderiv *= (esum/Math.pow(1+esum,2));
			result.set_part_deriv(param, new double[] {partderiv});
		}

		return result;
		
	}

	private Gradient computeGradientLLREG(Integer idx)
			throws RBNNaNException{
		System.out.println("Gradient for INVSUM not implemented");
		return null;
//		TreeMap<String,double[]> result = new TreeMap<String,double[]>();
//		double sum = aggregateOfSubPFs;
//		double sumpr = 0;
//		for (int i=0;i<children.size();i++){
//			sum = sum + children.elementAt(i).evaluate(idx)[0];
//			sumpr = sumpr + children.elementAt(i).evaluateGradient(idx,param)[0];
//		}
//		
//		return sumpr/Math.pow(1+sum,2);
		
	}



	private Gradient  computeGradientINVSUM(Integer idx)
	
			throws RBNNaNException{
		System.out.println("Gradient for INVSUM not implemented");
		return null;
//		TreeMap<String,double[]> result = new TreeMap<String,double[]>();
//		double val = this.evaluate(idx)[0];
//		if (val == 1.0)
//			return 0;
//		else{
//			double derivsum = 0;
//			result = -Math.pow(val,-2);
//			for (int i=0;i<children.size();i++)
//				derivsum = derivsum + children.elementAt(i).evaluateGradient(idx,param)[0];
//			result = result*derivsum;
//		}
//		return result;
	}

	private Gradient computeGradientESUM(Integer idx )
			throws RBNNaNException{
		System.out.println("Gradient for ESUM not implemented");
		return null;
//		TreeMap<String,double[]> result = new TreeMap<String,double[]>();
//		double val = this.evaluate(idx)[0];
//
//		double derivsum = 0;
//		result = -val;
//		for (int i=0;i<children.size();i++)
//			derivsum = derivsum + children.elementAt(i).evaluateGradient(idx,param)[0];
//		result = result*derivsum;
//
//		return result;
	}

	private Gradient computeGradientSUM(Integer idx )
			throws RBNNaNException{
		Gradient result = gradient_for_samples.get(idx);
		result.reset();

		
		for (String param: this.myparameters) {
			double partderiv = 0;
			for (int i=0;i<children.size();i++) {
				double[] childderiv = children.elementAt(i).evaluateGradient(idx).get_part_deriv(param);
				if (childderiv != null)
					partderiv = partderiv + childderiv[0];
			}
        	result.set_part_deriv(param, new double[] {partderiv});
		}

		return result;
		
	}

	@Override
	public boolean isBoolean() {
		return true;
	}
}
