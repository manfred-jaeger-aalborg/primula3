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
			
		if (this.depends_on_sample && is_evaluated_for_samples[sno]) 
				return this.values_for_samples[sno];
		
		if (!this.depends_on_sample && is_evaluated_for_samples[0])
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
			is_evaluated_for_samples[sno]=true;
		}
		else {
			values_for_samples[0] = result;
			is_evaluated_for_samples[0]=true;
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

	public Double[] evaluatePartDeriv(Integer sno, String param)
			throws RBNNaNException{
		String label="";
		if (this.isuga())
			label=this.getMyatom();
		else
			label=Integer.toString(this.identifier());
//		System.out.println("evalPD for " + label + " param " +param);
		
		if (!dependsOn(param)) {
//			System.out.println("no rec 1");
			return new Double[] {0.0}; // In this case need not fill the gradient_for_samples array
		}
		
		if (this.depends_on_sample && sno==null) {
			for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
				this.evaluatePartDeriv(i,param);
			return null;
		}

		Double[] g;
		if (this.depends_on_sample) 
			g = gradient_for_samples.get(sno).get(param);
		else
			g = gradient_for_samples.get(0).get(param);
		if (g!=null && g[0] != Double.NaN) {
//			System.out.println("no rec 2");
			return g;
		}
	

	/*
	 * Now the 'main' case: evaluated the gradient for a specific sample number sno.
	 * (sno=0 if no dependence on samples)
		 */
		double result = 0;
		switch (typeOfComb){
		case CombFunc.NOR:
			result = computeDerivNOR(sno,param);
			break;

		case CombFunc.MEAN:
			result = computeDerivMEAN(sno,param);
			break;

		case CombFunc.INVSUM:
			result = computeDerivINVSUM(sno,param);
			break;

		case CombFunc.ESUM:
			result = computeDerivESUM(sno,param);
			break;

		case CombFunc.LREG:
			result = computeDerivLREG(sno,param);
			break;
			
		case CombFunc.LLREG:
			result = computeDerivLLREG(sno,param);
			break;
			
		case CombFunc.SUM:
			result = computeDerivSUM(sno,param);
			break;
		case CombFunc.PROD:
			result = computeDerivPROD(sno,param);
			break;
		}
		
		Double[] resultarr = new Double[] {result};
		
		if (sno != null)
			gradient_for_samples.get(sno).put(param,resultarr);
		else
			gradient_for_samples.get(0).put(param,resultarr);
		
		return resultarr;
	}



	private double computeDerivNOR(Integer sno, String param)
	throws RBNNaNException
	{
		double result = 0;
        /* First compute \prod (1-Fi) over all subformulas */
        double factor = aggregateOfSubPFs;
        
        for (int i=0;i<children.size();i++)
                factor = factor*(1-children.elementAt(i).evaluate(sno)[0]);
        
        if (factor == 0)
        	return 0.0;
        
        /* Now compute the partial derivative as
         *
         * \sum_{F_i\in fthetalist} (factor/(1-F_i))*(F_i')
         */
        for (int i=0;i<children.size();i++){
                if (children.elementAt(i).dependsOn(param))
                        result = result + 
                        (factor/(1-children.elementAt(i).evaluate(sno)[0]))*(children.elementAt(i).evaluatePartDeriv(sno,param)[0]);
        }
        
		return result;
	}

	private double computeDerivPROD(Integer sno, String param)
			throws RBNNaNException{
		double result = 0;
        /* First compute \prod Fi over all subformulas */
        double factor = aggregateOfSubPFs;
        
        for (int i=0;i<children.size();i++)
                factor = factor*children.elementAt(i).evaluate(sno)[0];
        
        if (factor == 0)
        	return 0.0;
        
        /* Now compute the partial derivative as
         *
         * \sum_{F_i\in fthetalist} (factor/F_i)*(F_i')
         */
        for (int i=0;i<children.size();i++){
                if (children.elementAt(i).dependsOn(param))
                        result = result + (factor/children.elementAt(i).evaluate(sno)[0])*(children.elementAt(i).evaluatePartDeriv(sno,param)[0]);
        }
		return result;
	}

	private double computeDerivMEAN(Integer sno,String param )
			throws RBNNaNException{
		double result = 0;
		for (int i=0;i<children.size();i++)
			result = result + children.elementAt(i).evaluatePartDeriv(sno,param)[0];
		result = result/(valuesOfSubPFs.length + children.size());
		return result;
	}

	private double computeDerivLREG(Integer sno, String param )
			throws RBNNaNException{
		
		double sum = aggregateOfSubPFs;
		double sumpr = 0;
		for (int i=0;i<children.size();i++){
			sum = sum + children.elementAt(i).evaluate(sno)[0];
			sumpr = sumpr + children.elementAt(i).evaluatePartDeriv(sno,param)[0];
		}
		double esum = Math.exp(sum);
		//if (Double.isInfinite(esum))
		if (Double.isInfinite(Math.pow(1+esum,2)))
				return 0;
		double result = (esum*sumpr)/Math.pow(1+esum,2);
		if (Double.isNaN(result)){
			System.out.println("NaN in ggCombFuncNode.computeDerivLREG");
		}
		return result;
		
	}

	private double computeDerivLLREG(Integer sno, String param )
			throws RBNNaNException{
		
		double sum = aggregateOfSubPFs;
		double sumpr = 0;
		for (int i=0;i<children.size();i++){
			sum = sum + children.elementAt(i).evaluate(sno)[0];
			sumpr = sumpr + children.elementAt(i).evaluatePartDeriv(sno,param)[0];
		}
		
		return sumpr/Math.pow(1+sum,2);
		
	}

	
//	private double computeValueINVSUM(Integer sno){
//		double[] args = new double[valuesOfSubPFs.length+ children.size()];
//		for (int i=0;i<valuesOfSubPFs.length;i++)
//			args[i]=valuesOfSubPFs[i];
//		for (int i=0;i<children.size();i++)
//			args[i+valuesOfSubPFs.length]= children.elementAt(i).evaluate(sno)[0];
//		return thisgg.computeCombFunc(CombFunc.INVSUM,args);
//	}

	private double computeDerivINVSUM(Integer sno,String param )
			throws RBNNaNException{
		double result = 0;
		double val = this.evaluate(sno)[0];
		if (val == 1.0)
			return 0;
		else{
			double derivsum = 0;
			result = -Math.pow(val,-2);
			for (int i=0;i<children.size();i++)
				derivsum = derivsum + children.elementAt(i).evaluatePartDeriv(sno,param)[0];
			result = result*derivsum;
		}
		return result;
	}

	private double computeDerivESUM(Integer sno,String param )
			throws RBNNaNException{
		double result = 0;
		double val = this.evaluate(sno)[0];

		double derivsum = 0;
		result = -val;
		for (int i=0;i<children.size();i++)
			derivsum = derivsum + children.elementAt(i).evaluatePartDeriv(sno,param)[0];
		result = result*derivsum;

		return result;
	}

	private double computeDerivSUM(Integer sno,String param )
			throws RBNNaNException{

		double derivsum = 0;
		for (int i=0;i<children.size();i++)
			derivsum = derivsum + children.elementAt(i).evaluatePartDeriv(sno,param)[0];
		return derivsum;
	}

	@Override
	public boolean isBoolean() {
		return true;
	}
}
