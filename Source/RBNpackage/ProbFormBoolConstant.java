package RBNpackage;

import java.util.Hashtable;
import java.util.Vector;

import RBNExceptions.RBNCompatibilityException;

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
	public ProbForm conditionEvidence(RelStruc A, OneStrucData inst)
			throws RBNCompatibilityException {
		return new ProbFormBoolConstant(sign);
	}

	@Override
	public double evaluate(RelStruc A, OneStrucData inst, String[] vars,
			int[] tuple, boolean useCurrentCvals, String[] numrelparameters,
			boolean useCurrentPvals,
    		GroundAtomList mapatoms,
    		boolean useCurrentMvals,
    		Hashtable<String,Double> evaluated) throws RBNCompatibilityException {
		if (sign)
			return 1;
		else
			return 0;
	}

	public double value(){
		if (sign)
			return 1;
		else
			return 0;
	}
	
	@Override
	public double evalSample(RelStruc A, Hashtable atomhasht,
			OneStrucData inst, long[] timers) throws RBNCompatibilityException {
		if (sign)
			return 1;
		else
			return 0;
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
	public String[] freevars() {
		return new String[0];
	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A)
			throws RBNCompatibilityException {
		return new Vector<GroundAtom>();
	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst)
			throws RBNCompatibilityException {
		return new Vector<GroundAtom>();
	}


	@Override
	public ProbForm sEval(RelStruc A) throws RBNCompatibilityException {
		return new ProbFormBoolConstant(sign);
	}

	@Override
	public ProbForm substitute(String[] vars, int[] args) {
		ProbFormBoolConstant result = new ProbFormBoolConstant(sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public ProbForm substitute(String[] vars, String[] args) {
		ProbFormBoolConstant result = new ProbFormBoolConstant(sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	public ProbForm toStandardPF(boolean recursive)  {
		double value = 0;
		if (sign) value =1;
		return new ProbFormConstant(value);
	}
	
	public ProbFormBoolConstant clone(){
		return new ProbFormBoolConstant(sign);
	}
	
	public  void updateSig(Signature s){
	}
}
