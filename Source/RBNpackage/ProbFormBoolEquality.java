package RBNpackage;

import java.util.Hashtable;
import java.util.Vector;
import java.util.TreeSet;

import RBNExceptions.RBNCompatibilityException;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;
import RBNLearning.*;

public class ProbFormBoolEquality extends ProbFormBool {

	private String term1,term2;
	
	public ProbFormBoolEquality(String t1, String t2, boolean s){
		term1=t1;
		term2=t2;
		sign = s;
	}
	
	
	@Override
	public int evaluatesTo(RelStruc A, OneStrucData inst,
			boolean usesampleinst, Hashtable<String, GroundAtom> atomhasht)
			throws RBNCompatibilityException {
		return evaluatesTo(A);
	}

	@Override
	public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
		if (!isGround())
			return -1;
		boolean tv = (Integer.parseInt(term1) == Integer.parseInt(term2));
		if ((tv && sign)||(!tv && !sign))
			return 1;
		else
			return 0;
	}

	@Override
	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue,boolean usealias) {
		if (usealias && this.getAlias() != null)
			return this.getAlias();
		return "[" + term1 + "=" + term2 + "]";
	}

	@Override
	public ProbForm conditionEvidence(RelStruc A, OneStrucData inst)
			throws RBNCompatibilityException {
		return this;
	}

//	@Override
//	public double evaluate(RelStruc A, OneStrucData inst, String[] vars,
//			int[] tuple, boolean useCurrentCvals, String[] numrelparameters,
//			boolean useCurrentPvals,
//    		GroundAtomList mapatoms,
//    		boolean useCurrentMvals,
//    		Hashtable<String,Double> evaluated) throws RBNCompatibilityException {
//		ProbFormBoolEquality thissubstituted = (ProbFormBoolEquality)this.substitute(vars,tuple);
//		if (!thissubstituted.isGround())
//			throw new IllegalArgumentException("Attempt to evaluate non-ground equality");
//		if (Integer.parseInt(thissubstituted.term1)==Integer.parseInt(thissubstituted.term2)) return 1.0;
//		else return 0.0;
//	}

	public Object[] evaluate(RelStruc A, 
			OneStrucData inst, 
			String[] vars, 
			int[] tuple, 
			boolean useCurrentCvals, 
    		// String[] numrelparameters,
    		boolean useCurrentPvals,
    		GroundAtomList mapatoms,
    		boolean useCurrentMvals,
    		Hashtable<String,Object[]> evaluated,
    		Hashtable<String,Integer> params,
    		int returntype,
    		boolean valonly,
    		Profiler profiler)
	{			
//		if (!valonly)
//			System.out.println("Warning: trying to evaluate gradient for Boolean ProbForm" + this.makeKey(A));
		Object[] result = new Object[2];
		
		if (returntype == ProbForm.RETURN_SPARSE)
			result[1] = new Hashtable<String,Double>();
		else result[1] = new double[0];
		
		ProbFormBoolEquality thissubstituted = (ProbFormBoolEquality)this.substitute(vars,tuple);
		if (!thissubstituted.isGround())
			throw new IllegalArgumentException("Attempt to evaluate non-ground equality");
		if (Integer.parseInt(thissubstituted.term1)==Integer.parseInt(thissubstituted.term2)) result[0] =1.0;
		else result[0]=0.0;
		return result;
	}	
	
	
	@Override
	public double evalSample(RelStruc A,
			Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst,
			long[] timers) throws RBNCompatibilityException {
		return evaluate(A, null);
	}

	@Override
	public String[] freevars() {
		String bothterms[] = {term1,term2}; 
		return rbnutilities.NonIntOnly(bothterms);
	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A)
			throws RBNCompatibilityException {
		return new Vector<GroundAtom>();
	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
			throws RBNCompatibilityException {
		return new Vector<GroundAtom>();
	}

	@Override
	public ProbForm sEval(RelStruc A) throws RBNCompatibilityException {
		
		double value = evaluate(A, null);
		if (value == 1) 
			return new ProbFormBoolConstant(false);
		else 
			return new ProbFormBoolConstant(true);
	}

	@Override
	public ProbForm substitute(String[] vars, int[] args) {
		if (vars.length != args.length)
			System.out.println("ProbFormBoolEquality.substitute: vars: " + rbnutilities.arrayToString(vars) + "   args: " + rbnutilities.arrayToString(args));
		String termx=term1;
		String sterm1 = term1;
		String sterm2 = term2;
			for (int j = 0; j<vars.length; j++)
			{
				if (termx.equals(vars[j])) sterm1 = String.valueOf(args[j]);
			}
			termx = term2;
			for (int j = 0; j<vars.length; j++)
			{
				if (termx.equals(vars[j])) sterm2 = String.valueOf(args[j]);
			}
		ProbFormBoolEquality result = 	new ProbFormBoolEquality(sterm1, sterm2,sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public ProbForm substitute(String[] vars, String[] args) {
		String termx=term1;
		String sterm1 = term1;
		String sterm2 = term2;
		for (int j = 0; j<vars.length; j++)
		{
			if (termx.equals(vars[j])) sterm1 = args[j];
		}
		termx = term2;
		for (int j = 0; j<vars.length; j++)
		{
			if (termx.equals(vars[j])) sterm2 = args[j];
		}
		ProbFormBoolEquality result = 	new ProbFormBoolEquality(sterm1, sterm2,sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;	

	}
	
	public String term1(){
		return term1;
	}
	
	public String term2(){
		return term2;
	}
	
	private boolean isGround(){
		return (rbnutilities.IsInteger(term1) && rbnutilities.IsInteger(term2));
	}

	public ProbForm toStandardPF(boolean recursive){
		return this;
	}
	
	public String[] terms(){
		String[] result = new String[2];
		result[0]=term1;
		result[1]=term2;
		return result;
	}
	
	public ProbFormBoolEquality clone(){
		return new ProbFormBoolEquality(term1,term2,sign);
	}
	
	public void updateSig(Signature s){
	}
	
	public TreeSet<Rel> parentRels(){
		return new TreeSet<Rel>();
	}
	
	public TreeSet<Rel> parentRels(TreeSet<String> processed){
		return new TreeSet<Rel>();	
	}
}
