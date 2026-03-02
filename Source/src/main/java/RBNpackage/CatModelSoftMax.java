package RBNpackage;

import java.util.*;

import RBNExceptions.RBNCompatibilityException;
import RBNLearning.Gradient;
import RBNLearning.Gradient_Array;
import RBNLearning.Gradient_TreeMap;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;
import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;
import RBNutilities.rbnutilities;

public class CatModelSoftMax extends CPModel {


	/**
	 * Implementation of CatModel where the distribution for a k-valued variable is
	 * defined by k probability formulas (not necessarily constrained to [0,1] values), and
	 * the conditional probability distribution is the softmax over the values of the formulas
	 */
	
	Vector<CPModel> probforms;
	
	public CatModelSoftMax() {
		probforms = new Vector<CPModel>();
	}
	
	public CatModelSoftMax(Vector<CPModel> pfs) {
		probforms = pfs;
	}
	
	@Override
	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias) {
		if (usealias && this.getAlias() != null)
			return this.getAlias();

		String result = "SOFTMAX  \n";
		for (CPModel pf: probforms) {
			result = result + pf.asString(syntax, 1, A, paramsAsValue, usealias);
			result = result + ",\n";
		}
		result = result.substring(0, result.length() - 2); // removing last ","
		return result;
	}
	
	public boolean multlinOnly() {
		Boolean result = true;
		for (CPModel pf : probforms) {
			if (pf.multlinOnly()==false)
				result=false;
		}
		return result;
	}

	@Override
	public CatModelSoftMax conditionEvidence(RelStruc A, OneStrucData inst) throws RBNCompatibilityException {
		CatModelSoftMax result = new CatModelSoftMax();
		for (CPModel pf: this.probforms) {
			result.addProbForm(pf.conditionEvidence(A, inst));
		}
		return result;
	}

	@Override
	public boolean dependsOn(String variable, RelStruc A, OneStrucData data) throws RBNCompatibilityException {
		Boolean result = false;
		for (CPModel pf: this.probforms) {
			if (pf.dependsOn(variable, A, data))
				result = true;
		}
		return result;
	}

	@Override
	public Object[] evaluate(RelStruc A, 
			OneStrucData inst, 
			ArgTerm[] vars,
			int[] tuple,
			int gradindx,
			boolean useCurrentCvals,
			boolean useCurrentPvals, 
			HashMap<Rel,GroundAtomList> mapatoms, 
			boolean useCurrentMvals,
			HashMap<String, Object[]> evaluated, 
			HashMap<String, Integer> params, 
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
					gradindx,
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

		if (!valonly) {
			result[1] = null;
			if (returntype == ProbForm.RETURN_ARRAY)
				result[1] = new Gradient_Array(params);
			else
				result[1] = new Gradient_TreeMap(params);

			// Computing the gradient

			double derivsum = 0;

			for (String par : params.keySet()) {
				derivsum = 0;
				for (int i = 0; i < probforms.size(); i++) {
					double deriv_ik = ((Gradient) evaluatedpfs[i][1]).get_part_deriv(par)[0];
					derivsum += Math.exp((double) (evaluatedpfs[i][0])) * deriv_ik;
				}

				Gradient grad= (Gradient)evaluatedpfs[gradindx][1];
				double pd=Math.exp(((double) evaluatedpfs[gradindx][0]))
						* (grad.get_part_deriv(par)[0]* valsum - derivsum) / Math.pow(valsum, 2);
				((Gradient)result[1]).set_part_deriv(par,new double[] {pd});
			}
//			if (returntype == ProbForm.RETURN_ARRAY) {
//				result[1] = new double[params.size()];
//				for (int k = 0; k < params.size(); k++) {
//					((double[]) result[1])[k] = Math.exp(((double) evaluatedpfs[gradindx][0]))
//							* (((double[]) evaluatedpfs[gradindx][1])[k] * valsum - derivsum) / Math.pow(valsum, 2);
//				}
//			} else { // returntype ProbForm.RETURN_SPARSE
//				result[1] = new HashMap<String, Double>();
//				for (String nextpar : ((HashMap<String, Double>) evaluatedpfs[gradindx][1]).keySet()) {
//					((HashMap<String, Double>) result[1]).put(nextpar,
//							Math.exp(((double) evaluatedpfs[gradindx][0]))
//									* (((double[]) evaluatedpfs[gradindx][1])[params.get(nextpar)] * valsum - derivsum) / Math.pow(valsum, 2));
//				}
//			}
		}
		return result;
	}

	@Override
	public double[] evalSample(RelStruc A, HashMap<String, PFNetworkNode> atomhasht, OneStrucData inst,
			HashMap<String, double[]> evaluated, long[] timers) throws RBNCompatibilityException {
		
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
	public VarTerm[] freevars() {
		VarTerm result[] = new VarTerm[0];
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
	public int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, HashMap<String, GroundAtom> atomhasht) throws RBNCompatibilityException {
		return 0;
	}

	@Override
	public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
		return 0;
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
			result.addProbForm(probforms.elementAt(i).sEval(A));
		return result;
	}

	@Override
	public CatModelSoftMax substitute(String[] vars, int[] args) {
		CatModelSoftMax result = new CatModelSoftMax();
		for (int i = 0;i<probforms.size();i++)
			result.addProbForm(probforms.elementAt(i).substitute(vars,args));
		return result;
	}

	@Override
	public CatModelSoftMax substitute(String[] vars, String[] args) {
		CatModelSoftMax result = new CatModelSoftMax();
		for (int i = 0;i<probforms.size();i++)
			result.addProbForm(probforms.elementAt(i).substitute(vars,args));
		return result;
	}

	@Override
	public CPModel substitute(String[] vars, ArgTerm[] args) {
		CatModelSoftMax result = new CatModelSoftMax();
		for (int i = 0;i<probforms.size();i++)
			result.addProbForm(probforms.elementAt(i).substitute(vars,args));
		return result;
	}

	@Override
	public CPModel substitute(ArgTerm[] vars, ArgTerm[] args) {
		CatModelSoftMax result = new CatModelSoftMax();
		for (int i = 0;i<probforms.size();i++)
			result.addProbForm(probforms.elementAt(i).substitute(vars,args));
		return result;
	}

	@Override
	public CPModel substitute(ArgTerm[] vars, int[] args) {
		CatModelSoftMax result = new CatModelSoftMax();
		for (int i = 0;i<probforms.size();i++)
			result.addProbForm(probforms.elementAt(i).substitute(vars,args));
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
		String mykey = this.makeKey((String[]) null,null,true);
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
	
	public void addProbForm(CPModel pf) {
		probforms.add(pf);
	}
	
	public int numvals() {
		return probforms.size();
	}
	
	public CPModel pfAt(int i) {
		return probforms.elementAt(i);
	}

}
