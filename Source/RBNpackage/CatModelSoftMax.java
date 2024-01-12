package RBNpackage;

import java.util.*;

import RBNExceptions.RBNCompatibilityException;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;

public class CatModelSoftMax extends CPModel {


	/**
	 * Implementation of CatModel where the distribution for a k-valued variable is
	 * defined by k probability formulas (not necessarily constrained to [0,1] values), and
	 * the conditional probability distribution is the softmax over the values of the formulas
	 */
	
	Vector<ProbForm> probforms;
	
	public CatModelSoftMax() {
		probforms = new Vector<ProbForm>();
	}
	
	@Override
	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias) {
		if (usealias && this.getAlias() != null)
			return this.getAlias();
		
		String result = "SOFTMAX [ \n";
		for (ProbForm pf: probforms) {
			result = result + pf.asString(syntax, 1, A, paramsAsValue, usealias);
			result = result + ",";
		}
		result = result.substring(0, result.length() - 1); // removing last ","
		result = result + "/n ]";
		return result;
	}
	
	public boolean multlinOnly() {
		Boolean result = true;
		for (ProbForm pf : probforms) {
			if (pf.multlinOnly()==false)
				result=false;
		}
		return result;
	}

	@Override
	public CatModelSoftMax conditionEvidence(RelStruc A, OneStrucData inst) throws RBNCompatibilityException {
		CatModelSoftMax result = new CatModelSoftMax();
		for (ProbForm pf: this.probforms) {
			result.addProbForm((ProbForm)pf.conditionEvidence(A, inst));
		}
		return result;
	}

	@Override
	public boolean dependsOn(String variable, RelStruc A, OneStrucData data) throws RBNCompatibilityException {
		Boolean result = false;
		for (ProbForm pf: this.probforms) {
			if (pf.dependsOn(variable, A, data))
				result = true;
		}
		return result;
	}

	@Override
	public Object[] evaluate(RelStruc A, 
			OneStrucData inst, 
			String[] vars, 
			int[] tuple, 
			int gradindx,
			boolean useCurrentCvals,
			boolean useCurrentPvals, 
			GroundAtomList mapatoms, 
			boolean useCurrentMvals,
			Hashtable<String, Object[]> evaluated, 
			Hashtable<String, Integer> params, 
			int returntype, 
			boolean valonly,
			Profiler profiler) throws RBNCompatibilityException {
		// TODO Auto-generated method stub
		Object[][] evaluatedpfs = new Object[probforms.size()][2];
		Object[] result = new Object[2];
		result[0]=new double[probforms.size()];
		double nextval;
		double valsum =0;
		
		for (int i = 0;i<probforms.size();i++) {
			
			Object[] pfval = probforms.elementAt(i).evaluate(A, 
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
			nextval = (Double)pfval[0];
			evaluatedpfs[i][0]=nextval;
			evaluatedpfs[i][1]=pfval[1];
			if (nextval==Double.NaN) {
				Arrays.fill((double[])result[0],Double.NaN);
				return result;
			}
			valsum+=nextval;
		}
		for (int i = 0;i<probforms.size();i++){
			((double[])result[0])[i]=((Double)evaluatedpfs[i][0])/valsum;
		}
		if (valonly)
			return result;
		x
		return null;
	}

	@Override
	public Double evalSample(RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst,
			Hashtable<String, Double> evaluated, long[] timers) throws RBNCompatibilityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] freevars() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A) throws RBNCompatibilityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
			throws RBNCompatibilityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] parameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProbForm sEval(RelStruc A) throws RBNCompatibilityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProbForm substitute(String[] vars, int[] args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProbForm substitute(String[] vars, String[] args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateSig(Signature s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCvals(String paramname, double val) {
		// TODO Auto-generated method stub

	}

	@Override
	public String makeKey(String[] vars, int[] args, Boolean nosub) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TreeSet<Rel> parentRels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TreeSet<Rel> parentRels(TreeSet<String> processed) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void addProbForm(ProbForm pf) {
		probforms.add(pf);
	}

}
