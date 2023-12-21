package RBNpackage;

import java.util.Hashtable;
import java.util.Vector;
import java.util.TreeSet;


import RBNExceptions.RBNCompatibilityException;
import RBNLearning.GradientGraph;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;
import RBNLearning.Profiler;

/* This is basically a wrapper class around
 * ProbFormAtom. Needed to make ProbFormAtom available 
 * as  a subclass of ProbFormBool. Note that the atom of 
 * this formula need not be of Boolean type. It can also 
 * be numeric, but then will be evaluated as a Boolean.
 */
public class ProbFormBoolAtom extends ProbFormBool {
	
	
	private ProbFormAtom pfatom;
	
	public ProbFormBoolAtom(Rel r, String[] args, boolean s) {
		sign =s;
		pfatom = new ProbFormAtom(r,args);
	}
	
	public ProbFormBoolAtom(Rel r, int[] args, boolean s){
		sign =s;
		pfatom = new ProbFormAtom(r,args);
	}

	public ProbFormBoolAtom(ProbFormAtom pfatm, boolean s){
		sign =s;
		pfatom = pfatm;
	}
	
	@Override
	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue,boolean usealias){
		String result ="";
		if (!sign) 
			result = result + "~";
		if (usealias && this.getAlias() != null)
			return result+this.getAlias();
		return result+pfatom.asString(syntax, depth, A, paramsAsValue,usealias) ;
	}
	
	@Override
//	public double evaluate(RelStruc A, 
//			OneStrucData inst, 
//			String[] vars, 
//			int[] tuple, 
//			boolean useCurrentCvals, 
//    		String[] numrelparameters,
//    		boolean useCurrentPvals,
//    		GroundAtomList mapatoms,
//    		boolean useCurrentMvals,
//    		Hashtable<String,Double> evaluated)
//	{			
//		
//		double result = pfatom.evaluate(A, inst, vars, tuple, useCurrentCvals, 
//				numrelparameters, useCurrentPvals,
//				mapatoms,useCurrentMvals,evaluated);
//		if (pfatom.getRelation().valtype() == Rel.NUMERIC)
//		{
//			if (result == Double.NaN)
//				result = 0.0;
//			else result = 1.0;
//		}
//		if (!sign) result = 1- result;
//		
//		return result;
//	}

	

	public Object[] evaluate(RelStruc A, 
			OneStrucData inst, 
			String[] vars, 
			int[] tuple, 
			boolean useCurrentCvals, 
    		boolean useCurrentPvals,
    		GroundAtomList mapatoms,
    		boolean useCurrentMvals,
    		Hashtable<String,Object[]> evaluated,
    		Hashtable<String,Integer>params,
    		int returntype,
    		boolean valonly,
    		Profiler profiler)
	{			
//		if (!valonly)
//			System.out.println("Warning: trying to evaluate gradient for Boolean ProbForm" + this.makeKey(A));
		Object[] result = pfatom.evaluate(A, 
				inst, 
				vars, 
				tuple, 
				useCurrentCvals, 
				useCurrentPvals, 
				mapatoms, 
				useCurrentMvals, 
				evaluated, 
				params, 
				returntype,
				valonly,
				profiler);
		
		if (pfatom.getRelation().valtype() == Rel.NUMERIC)
		{
			double val = (double)result[0];
			
			if (val == Double.NaN)
				result[0] = 0.0;
			else result[0] = 1.0;
		}
		if (!sign) result[0] = 1.0 - (double)result[0];
		
		return result;
	}
	
	@Override
	public int evaluatesTo(RelStruc A, OneStrucData inst,
			boolean usesampleinst, Hashtable<String,GroundAtom> atomhasht)
					throws RBNCompatibilityException {
		int result =  pfatom.evaluatesTo(A,inst,usesampleinst,atomhasht);
		if (result == -1)
			return result;
		else{
			if (sign)
				return result;
			else return 1- result;
		}
	}

	@Override
	public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
		int result =  pfatom.evaluatesTo(A);
		if (result == -1)
			return result;
		else{
			if (sign)
				return result;
			else return 1- result;
		}
	}

	@Override
	public  Double evalSample(RelStruc A, 
			Hashtable<String,PFNetworkNode> atomhasht, 
			OneStrucData inst, 
    		Hashtable<String,Double> evaluated,
			long[] timers)
			throws RBNCompatibilityException{
		String key = null;
		
		if (evaluated != null) {
			key = this.makeKey(A);
			Double d = evaluated.get(key);
			if (d!=null) {
				return d; 
			}
		}	
		
		Double result = pfatom.evalSample(A,atomhasht,inst,evaluated,timers);
		if (pfatom.getRelation().valtype()==Rel.NUMERIC){
			if (result == Double.NaN)
				result = 0.0;
			else result = 1.0;
		}
		if (!sign)
			result=1-result;
		if (evaluated != null) {
			evaluated.put(key, result);
		}
		return result;
	}
	
	@Override
	public ProbForm conditionEvidence(RelStruc A, OneStrucData inst){
		return new ProbFormBoolAtom((ProbFormAtom)pfatom.conditionEvidence(A, inst),sign);
	}
	
	@Override
	public String[] freevars() {
		return pfatom.freevars();
	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A)
			throws RBNCompatibilityException {
		return pfatom.makeParentVec(A);
	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
			throws RBNCompatibilityException {
		return pfatom.makeParentVec(A,inst,macrosdone);
	}

	@Override
	public ProbForm sEval(RelStruc A) throws RBNCompatibilityException {
		ProbForm sevalOfpfatom = pfatom.sEval(A);
		if (sevalOfpfatom instanceof ProbFormAtom)
			return new ProbFormBoolAtom((ProbFormAtom)pfatom.sEval(A),sign);   
		else // sevalOfpfatom is ProbFormConstant
		{
			double truthval = ((ProbFormConstant)sevalOfpfatom).cval;
			if (truthval == 1.0)
				return new ProbFormBoolConstant(sign);
			else
				return new ProbFormBoolConstant(!sign);
		}
	}

	@Override
	public ProbForm substitute(String[] vars, int[] args) {
		ProbFormBoolAtom result = new ProbFormBoolAtom((ProbFormAtom)pfatom.substitute(vars,args),sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public ProbForm substitute(String[] vars, String[] args) {
		ProbFormBoolAtom result = new ProbFormBoolAtom((ProbFormAtom)pfatom.substitute(vars,args),sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}
	
	public Rel getRelation(){
		return pfatom.getRelation();
	}
	
	public String[] getArguments(){
		return pfatom.getArguments();
	}

	public ProbForm toStandardPF(boolean recursive){
		return this;
	}
	
	public ProbFormBoolAtom clone(){
		return new ProbFormBoolAtom(this.getRelation(),this.getArguments(),sign);
	}
	
	public void updateSig(Signature s){
		pfatom.updateSig(s);
	}
	
	public TreeSet<Rel> parentRels(){
		return pfatom.parentRels();
	}
	public TreeSet<Rel> parentRels(TreeSet<String> processed){
		return pfatom.parentRels(processed);
	}
}
