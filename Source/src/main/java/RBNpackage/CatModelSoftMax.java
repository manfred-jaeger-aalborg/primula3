package RBNpackage;

import java.util.*;

import RBNExceptions.RBNCompatibilityException;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;

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
	
	public CatModelSoftMax(Vector<ProbForm> pfs) {
		probforms = pfs;
	}
	
	@Override
	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias) {
		if (usealias && this.getAlias() != null)
			return this.getAlias();

		String result = "SOFTMAX  \n";
		for (ProbForm pf: probforms) {
			result = result + pf.asString(syntax, 1, A, paramsAsValue, usealias);
			result = result + ",\n";
		}
		result = result.substring(0, result.length() - 2); // removing last ","
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
			Hashtable<Rel,GroundAtomList> mapatoms, 
			boolean useCurrentMvals,
			Hashtable<String, Object[]> evaluated, 
			Hashtable<String, Integer> params, 
			int returntype, 
			boolean valonly,
			Profiler profiler) throws RBNCompatibilityException {
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
			nextval = (double)pfval[0];
			evaluatedpfs[i][0]=nextval;
			evaluatedpfs[i][1]=pfval[1];
			if (Double.isNaN(nextval)) {
				Arrays.fill((double[])result[0],Double.NaN);
				return result;
			}
			valsum+=Math.exp(nextval);
		}

		double[] probabilities = (double[]) result[0];
		for (int i = 0; i < probforms.size(); i++) {
			probabilities[i] = Math.exp((double) evaluatedpfs[i][0]) / valsum;
		}

		if (valonly)
			return result;
		
		// Computing the gradient
		double derivsum =0;
		for (int k =0;k<params.size();k++) {
			for (int i=0;i<probforms.size();i++) {
				derivsum+=Math.exp((double)(evaluatedpfs[i][0]))*((double[])evaluatedpfs[i][1])[k];
			}
		}
		
		if (returntype == ProbForm.RETURN_ARRAY) {
			result[1]=new double[params.size()];
			for (int k =0;k<params.size();k++) {
				((double[])result[1])[k]=Math.exp(((double)evaluatedpfs[gradindx][0]))
						*(((double[])evaluatedpfs[gradindx][1])[k]*valsum-derivsum)/Math.pow(valsum,2);
			}
		}
		else { // returntype ProbForm.RETURN_SPARSE
			result[1]=new Hashtable<String,Double>();
			for (String nextpar: ((Hashtable<String,Double>)evaluatedpfs[gradindx][1]).keySet()) {
				((Hashtable<String,Double>)result[1]).put(nextpar,
						Math.exp(((double)evaluatedpfs[gradindx][0]))
						*(((double[])evaluatedpfs[gradindx][1])[params.get(nextpar)]*valsum-derivsum)/Math.pow(valsum,2));
			}
		}
		return result;
	}

	@Override
	public double[] evalSample(RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst,
			Hashtable<String, double[]> evaluated, long[] timers) throws RBNCompatibilityException {
		
		String key = null;
		
		if (evaluated != null) {
			key = this.makeKey(A);
			double[] d = evaluated.get(key);
			if (d!=null) {
				return d; 
			}
		}	
		
		double[] result=new double[probforms.size()];
		double valsum = 0.0;
		
		for (int i = 0;i<probforms.size();i++) {

			result[i] = probforms.elementAt(i).evalSample(A, 
					atomhasht,
					inst,
					evaluated,
					timers)[0];
			valsum+=Math.exp(result[i]);
		}
		
		
		for (int i = 0;i<probforms.size();i++){
			result[i]=Math.exp((Double)result[i])/valsum;
		}
		
		if (evaluated != null) {
			evaluated.put(key, result);
		}
		return result;
		
	}

	@Override
	public String[] freevars() {
		String result[] = new String[0];
		for (int i = 0;i<probforms.size();i++)
			result= rbnutilities.arraymerge(result,probforms.elementAt(i).freevars());
		return result;
	}

//	@Override
//	public Vector<GroundAtom> makeParentVec(RelStruc A) throws RBNCompatibilityException {
//		return makeParentVec(A,new OneStrucData(),null);
//	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
			throws RBNCompatibilityException {
		Vector<GroundAtom> result = new Vector<GroundAtom>();
		for (int i = 0;i<probforms.size();i++)
			result = rbnutilities.combineAtomVecs(result,probforms.elementAt(i).makeParentVec(A,inst,macrosdone));
		return result;
	}

	@Override
	public String[] parameters() {
		String result[] = new String[0];
		for (int i = 0;i<probforms.size();i++)
			result = rbnutilities.arraymerge(result,probforms.elementAt(i).parameters());
		return result;
	}

	@Override
	public CatModelSoftMax sEval(RelStruc A) throws RBNCompatibilityException {
		CatModelSoftMax result = new CatModelSoftMax();
		for (int i = 0;i<probforms.size();i++)
			result.addProbForm((ProbForm)probforms.elementAt(i).sEval(A));
		return result;
	}

	@Override
	public CatModelSoftMax substitute(String[] vars, int[] args) {
		CatModelSoftMax result = new CatModelSoftMax();
		for (int i = 0;i<probforms.size();i++)
			result.addProbForm((ProbForm)probforms.elementAt(i).substitute(vars,args));
		return result;
	}

	@Override
	public CatModelSoftMax substitute(String[] vars, String[] args) {
		CatModelSoftMax result = new CatModelSoftMax();
		for (int i = 0;i<probforms.size();i++)
			result.addProbForm((ProbForm)probforms.elementAt(i).substitute(vars,args));
		return result;
	}

//	@Override
//	public void updateSig(Signature s) {
//		for (int i = 0;i<probforms.size();i++)
//			probforms.elementAt(i).updateSig(s);
//	}

	@Override
	public void setCvals(String paramname, double val) {
		for (int i = 0;i<probforms.size();i++)
			probforms.elementAt(i).setCvals(paramname,val);
	}

	@Override
	public TreeSet<Rel> parentRels() {
		TreeSet<Rel> result = new TreeSet<Rel>();
		for (int i = 0;i<probforms.size();i++)
			result.addAll(probforms.elementAt(i).parentRels());
		return result;
	}

	@Override
	public TreeSet<Rel> parentRels(TreeSet<String> processed) {
		String mykey = this.makeKey(null,null,true);
		if (processed.contains(mykey))
			return new TreeSet<Rel>();
		else {
			processed.add(mykey);
			TreeSet<Rel> result = new TreeSet<Rel>();
			for (int i = 0;i<probforms.size();i++)
				result.addAll(probforms.elementAt(i).parentRels(processed));
			return result;
		}
	}
	
	public void addProbForm(ProbForm pf) {
		probforms.add(pf);
	}
	
	public int numvals() {
		return probforms.size();
	}
	
	public ProbForm pfAt(int i) {
		return probforms.elementAt(i);
	}

}
