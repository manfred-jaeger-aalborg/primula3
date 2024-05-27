package RBNpackage;

import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Vector;

import RBNExceptions.RBNCompatibilityException;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;

public class ProbFormMacroCall extends ProbForm {

	private RBNMacro macro;
	private String arguments[]; // The argument list when macro is called
	/* Example: 
	 * 
	 * @macro(u,v) = WIF r(u) THEN 0.3 ELSE  t(v);
	 * 
	 * rel(w,z) =  .... @macro(z,w) ....
	 * 
	 * Here: arguments = [z,w].
	 * 
	 */
	private ProbForm pf_sub; // the probform of macro addressed by this call with variables substituted
	
	public ProbFormMacroCall(RBNMacro m, String[] args) {
		macro = m;
		arguments=args;
		pf_sub = null;
	}
	
	private void setpf() {
		if (pf_sub == null)
			pf_sub = ((ProbForm)macro.cpmod()).substitute(macro.arguments(), arguments);
	}
	
	public void setpf(ProbForm pf) {
		pf_sub=pf;
	}
	
	@Override
	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias) {
		String result = new String();
		result = macro.rel().printname();
		result = result.concat("(");
		for (int i = 0; i<arguments.length-1; i++)
			if (rbnutilities.IsInteger(arguments[i]) && A!=null)
				result = result.concat(A.nameAt(Integer.parseInt(arguments[i])) + ",");
			else result = result.concat(arguments[i] + ",");
		if (arguments.length>0){
			if (rbnutilities.IsInteger(arguments[arguments.length-1])&& A!=null)
				result = result.concat(A.nameAt(Integer.parseInt(arguments[arguments.length-1])));
			else result = result.concat(arguments[arguments.length-1]);
		}
		result = result.concat(")");
		return result;
	}

	@Override
	public ProbForm conditionEvidence(RelStruc A, OneStrucData inst) throws RBNCompatibilityException {
		// conditionEvidence is only called for ground ProbForms by BayesConstructor
		setpf();
		return pf_sub.conditionEvidence(A, inst);
	}

	@Override
	public boolean dependsOn(String variable, RelStruc A, OneStrucData data) throws RBNCompatibilityException {
		return macro.cpmod().dependsOn(variable, A, data);
	}

	@Override
	public Object[] evaluate(RelStruc A, OneStrucData inst, String[] vars, int[] tuple, boolean useCurrentCvals,
			boolean useCurrentPvals, Hashtable<Rel,GroundAtomList> mapatoms, boolean useCurrentMvals,
			Hashtable<String, Object[]> evaluated, Hashtable<String, Integer> params, int returntype, boolean valonly,
			Profiler profiler) throws RBNCompatibilityException {
			setpf();

			Object[] result = pf_sub.evaluate(A, 
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
		

		return result;
	}

	@Override
	public Double evalSample(RelStruc A, 
			Hashtable<String,PFNetworkNode> atomhasht, 
			OneStrucData inst, 
    		Hashtable<String,Double> evaluated,
			long[] timers)
			throws RBNCompatibilityException {
		setpf();
		return pf_sub.evalSample(A, atomhasht, inst, evaluated,timers);
	}

	@Override
	public int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst,
			Hashtable<String, GroundAtom> atomhasht) throws RBNCompatibilityException {
		setpf();
		return pf_sub.evaluatesTo(A, inst, usesampleinst, atomhasht);
	}

	@Override
	public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
		setpf();
		return pf_sub.evaluatesTo(A);
	}

	@Override
	public String[] freevars() {
			return rbnutilities.NonIntOnly(macro.arguments);
	}

//	@Override
//	public Vector<GroundAtom> makeParentVec(RelStruc A) throws RBNCompatibilityException {
//		setpf();
//		return pf_sub.makeParentVec(A);
//	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone) 
			throws RBNCompatibilityException {
		String mykey = this.makeKey(null,null,true);
		if (macrosdone.contains(mykey))
			return new Vector<GroundAtom>();
		else {
			macrosdone.add(mykey);
			setpf();
			return pf_sub.makeParentVec(A,inst,macrosdone);
		}
		
	}

	@Override
	public boolean multlinOnly() {
		setpf();
		return pf_sub.multlinOnly();
	}

	@Override
	public String[] parameters() {
		return new String[0];
	}

//	@Override
//	public ProbForm sEval(RelStruc A) throws RBNCompatibilityException {
//		setpf();
//		return pf_sub.sEval(A);
//	}

	@Override
	public ProbForm sEval(RelStruc A) throws RBNCompatibilityException {
		setpf();
		pf_sub.sEval(A);
		ProbFormMacroCall returnval = new ProbFormMacroCall(this.macro,this.arguments);
		returnval.setpf(pf_sub);
		return returnval;
	}
	
	@Override
	public ProbForm substitute(String[] vars, int[] args) {
		return new ProbFormMacroCall(this.macro,rbnutilities.array_substitute(arguments, vars, args));
	}

	@Override
	public ProbForm substitute(String[] vars, String[] args) {
		return new ProbFormMacroCall(this.macro,rbnutilities.array_substitute(arguments, vars, args));
	}

//	@Override
//	public void updateSig(Signature s) {
//		/* Do nothing here. Signature update should be performed directly by iterating in the 
//		* RBN over the probability formulas in the macro definitions.
//		*/
//	}

	@Override
	public void setCvals(String paramname, double val) {
		/* Do nothing here. Cvals should be set directly by iterating in the 
		* RBN over the probability formulas in the macro definitions.
		*/
	}

	@Override
	public TreeSet<Rel> parentRels() {
		return macro.cpmod().parentRels(); // No substitution required here
	}
	
	public TreeSet<Rel> parentRels(TreeSet<String> processed){
		String mykey = this.makeKey(null,null,true);
		if (processed.contains(mykey))
			return new TreeSet<Rel>();
		else {
			processed.add(mykey);
			return macro.cpmod().parentRels(processed);
		}
	}
	
	public ProbForm pform() {
		return (ProbForm)macro.cpmod();
	}
	
	public RBNMacro macro() {
		return macro;
	}
	
	public String[] args(){
		return arguments;
	}

}
