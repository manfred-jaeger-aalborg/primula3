package RBNpackage;

import java.util.Hashtable;
import java.util.Vector;

import RBNExceptions.RBNCompatibilityException;

/*
 * ProbForms that are guaranteed to evaluate to 
 * a boolean value 0 or 1. Boolean expression of 
 * Boolean atoms 
 */
public abstract class ProbFormBool extends ProbForm {
	
	public static final int OPERATORAND = 0;
	public static final int OPERATOROR = 1;
	
	/* If sign = true, then the ProbFormBool represents the Boolean formula represented
	* by its Name and structure. If sign = false, then the ProbFormBool represents the 
	* negation of this formula
	*/ 
	protected boolean sign;
	
	public ProbFormBool(){
		sign = true;
	}
	
	public abstract int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable<String,GroundAtom> atomhasht) 
	throws RBNCompatibilityException;

    public abstract int evaluatesTo(RelStruc A) throws RBNCompatibilityException;
	
	@Override
	public boolean dependsOn(String variable, RelStruc A, OneStrucData data)
			throws RBNCompatibilityException {
		return false;
	}

	@Override
	public boolean multlinOnly() {
		return true;
	}

	@Override
	public String[] parameters() {
		return new String[0];
	}
	
//	@Override
//	public void setParameters(String[] params, double[] values) {
//	}

	public void toggleSign(){
		sign = !sign;
	}
	
	public boolean sign(){
		return sign;
	}
	
	/*
	 * Transforms this into a non-Bool ProbForm using convex combinations
	 * and n-or combination functions. Only in the case of ProbFormBoolAtom is 
	 * the returned ProbForm still a ProbFormBoolAtom (to maintain the 0/1-valued 
	 * interpretations of numerical atoms), and in the case of ProbFormBoolEquality
	 * also nothing is changed
	 */
	public abstract ProbForm toStandardPF(boolean recursive);
	
	public abstract ProbFormBool clone();
	
	public void setCvals(String paramname, double val) {
	}
	
}
