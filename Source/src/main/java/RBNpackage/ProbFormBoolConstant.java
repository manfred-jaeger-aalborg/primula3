package RBNpackage;

import java.util.Hashtable;
import java.util.Vector;
import java.util.TreeSet;

import RBNExceptions.RBNCompatibilityException;
import RBNLearning.*;
import RBNinference.PFNetworkNode;
import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;

/* represents 'false' if sign=true, else represents
 * 'true'
 */
public class ProbFormBoolConstant extends ProbFormBool {


	
	public ProbFormBoolConstant(boolean sig){
		sign = sig;
	}
	
	@Override
	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue,boolean usealias) {
		if (usealias && this.getAlias() != null)
			return this.getAlias();
		if (sign)
			return "true";
		else
			return "false";
	}

	@Override
	public CPModel conditionEvidence(RelStruc A, OneStrucData inst)
			throws RBNCompatibilityException {
		return new ProbFormBoolConstant(sign);
	}

//	@Override
//	public double evaluate(RelStruc A, OneStrucData inst, String[] vars,
//			int[] tuple, boolean useCurrentCvals, String[] numrelparameters,
//			boolean useCurrentPvals,
//    		GroundAtomList mapatoms,
//    		boolean useCurrentMvals,
//    		Hashtable<String,Double> evaluated) throws RBNCompatibilityException {
//		if (sign)
//			return 1;
//		else
//			return 0;
//	}

	public Object[] evaluate(RelStruc A, 
			OneStrucData inst,
			ArgTerm[] vars,
			int[] tuple,
			int gradindx,
			boolean useCurrentCvals, 
    		// String[] numrelparameters,
    		boolean useCurrentPvals,
    		Hashtable<Rel,GroundAtomList> mapatoms,
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
		if (sign)
			result[0] =1.0;
		else
			result[0] =0.0;

		if (!valonly) {
			if (returntype == ProbForm.RETURN_SPARSE)
				result[1] = new Gradient_TreeMap(params);
			else result[1] = new Gradient_Array(params);
		}

		return result;
	}
	
	public double value(){
		if (sign)
			return 1;
		else
			return 0;
	}
	
	@Override
	public double[] evalSample(RelStruc A, 
			Hashtable<String,PFNetworkNode> atomhasht, 
			OneStrucData inst, 
    		Hashtable<String,double[]> evaluated,
			long[] timers)
		throws RBNCompatibilityException {
		if (sign)
			return new double[] {Double.valueOf(1)};
		else
			return new double[] {Double.valueOf(0)};
	}

	@Override
	public int evaluatesTo(RelStruc A, OneStrucData inst,
			boolean usesampleinst, Hashtable atomhasht)
			throws RBNCompatibilityException {
		if (sign)
			return 1;
		else
			return 0;
	}

	@Override
	public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
		if (sign)
			return 1;
		else
			return 0;	
	}

	@Override
	public VarTerm[] freevars() {
		return new VarTerm[0];
	}

//	@Override
//	public Vector<GroundAtom> makeParentVec(RelStruc A)
//			throws RBNCompatibilityException {
//		return new Vector<GroundAtom>();
//	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
			throws RBNCompatibilityException {
		return new Vector<GroundAtom>();
	}


	@Override
	public CPModel sEval(RelStruc A) throws RBNCompatibilityException {
		return new ProbFormBoolConstant(sign);
	}

	@Override
	public CPModel substitute(String[] vars, int[] args) {
		ProbFormBoolConstant result = new ProbFormBoolConstant(sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public CPModel substitute(String[] vars, String[] args) {
		ProbFormBoolConstant result = new ProbFormBoolConstant(sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public CPModel substitute(String[] vars, ArgTerm[] args) {
		ProbFormBoolConstant result = new ProbFormBoolConstant(sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public CPModel substitute(ArgTerm[] vars, ArgTerm[] args) {
		ProbFormBoolConstant result = new ProbFormBoolConstant(sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public CPModel substitute(ArgTerm[] vars, int[] args) {
		ProbFormBoolConstant result = new ProbFormBoolConstant(sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	public CPModel toStandardPF(boolean recursive)  {
		double value = 0;
		if (sign) value =1;
		return new ProbFormConstant(value);
	}
	
	public ProbFormBoolConstant clone(){
		return new ProbFormBoolConstant(sign);
	}
	
	public  void updateSig(Signature s){
	}
	
	public TreeSet<Rel> parentRels(){
		return new TreeSet<Rel>();
	}
	
	public TreeSet<Rel> parentRels(TreeSet<String> processed){
		return new TreeSet<Rel>();	
	}

	@Override
	public int numvals() {
		return 2;
	}
}
