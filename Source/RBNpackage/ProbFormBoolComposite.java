package RBNpackage;

import java.util.Hashtable;
import java.util.Vector;
import java.util.TreeSet;

import RBNExceptions.RBNCompatibilityException;
import RBNLearning.*;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;

/* A conjunction or disjunction of subformulas contained in the
 * array components
 */

public class ProbFormBoolComposite extends ProbFormBool {
	
	int operator; // as defined in ProbFormBool
	ProbFormBool[] components;
	
	public ProbFormBoolComposite(Vector<ProbFormBool> con, int op, boolean s){
		super();
		
		components = new ProbFormBool[con.size()];
		for (int i=0;i<components.length;i++){
			components[i]=con.elementAt(i);
		}
		operator = op;
		sign =s;
	}

	public ProbFormBoolComposite(ProbFormBool[] con, int op, boolean s){
		components = con;
		operator = op;
		sign =s;
	}
	
	@Override
	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue,boolean usealias) {
		if (usealias && this.getAlias() != null)
			return this.getAlias();
		String result ="";
		if (!sign) 
			result = result + "~";
		result = result + "(" + components[0].asString(syntax,depth,A,paramsAsValue,usealias);
		for (int i=1; i< components.length; i++){
			switch (operator){
			case ProbFormBool.OPERATORAND:
				result = result + "&";
				break;
			case ProbFormBool.OPERATOROR:
				result = result + "|";
			}
			result = result + components[i].asString(syntax,depth,A,paramsAsValue,usealias);
		}
		result = result + ")";
		return result;
	}

	@Override
	public ProbFormBool conditionEvidence(RelStruc A, OneStrucData inst)
			throws RBNCompatibilityException {
		ProbFormBool[] condarray = new ProbFormBool[components.length];
		for (int i=0;i<components.length;i++)
			condarray[i]=(ProbFormBool)components[i].conditionEvidence(A, inst);
		return new ProbFormBoolComposite(condarray,operator,sign);
	}

//	@Override
//	public double evaluate(RelStruc A, OneStrucData inst, String[] vars,
//			int[] tuple, boolean useCurrentCvals, String[] numrelparameters,
//			boolean useCurrentPvals,
//    		GroundAtomList mapatoms,
//    		boolean useCurrentMvals,
//    		Hashtable<String,Double> evaluated) 
//    	throws RBNCompatibilityException {
//		
//		double result =0;
//		switch (operator){
//		case ProbFormBool.OPERATORAND:
//			result=1;
//			for (int i=0;i<components.length;i++)
//				result = result*components[i].evaluate(A, inst, vars, tuple, useCurrentCvals, 
//						numrelparameters, useCurrentPvals,
//						mapatoms,useCurrentMvals,evaluated);
//			break;
//		case ProbFormBool.OPERATOROR:
//			result=0;
//			for (int i=0;i<components.length;i++)
//				result = Math.max(result,components[i].evaluate(A, inst, vars, tuple, useCurrentCvals, 
//						numrelparameters, useCurrentPvals,
//						mapatoms,useCurrentMvals,evaluated));
//		}
//		if (!sign)
//			result = 1-result;
//		
//		return result;
//	}

	public Object[] evaluate(RelStruc A, 
			OneStrucData inst, 
			String[] vars, 
			int[] tuple, 
			boolean useCurrentCvals, 
    		// String[] numrelparameters,
    		boolean useCurrentPvals,
    		Hashtable<Rel,GroundAtomList> mapatoms,
    		boolean useCurrentMvals,
    		Hashtable<String,Object[]> evaluated,
    		Hashtable<String,Integer> params,
    		int returntype,
    		boolean valonly,
    		Profiler profiler){
//		if (!valonly)
//			System.out.println("Warning: trying to evaluate gradient for Boolean ProbForm" + this.makeKey(A));
		
		Object[] result = new Object[2];
		
		if (returntype == ProbForm.RETURN_SPARSE)
			result[1] = new Hashtable<String,Double>();
		else result[1] = new double[0];
		
		try {
			switch (operator){
			case ProbFormBool.OPERATORAND:
				result[0]=1.0;
				for (int i=0;(i<components.length && (double)result[0] != 0);i++) {
					double nextfac = (double)components[i].evaluate(A,inst,vars,tuple,useCurrentCvals,useCurrentPvals,
							mapatoms,useCurrentMvals,evaluated,params,returntype,valonly,null)[0];
					if (nextfac==0)
						result[0]=0.0; // This allows to overwrite a previous result[0] = NaN with a clean 0
					else	
						result[0] = (double)result[0]*nextfac;
					
				}
				break;
			case ProbFormBool.OPERATOROR:
				result[0]=0.0;
				for (int i=0;(i<components.length && (double)result[0]<1);i++)
					result[0] = Math.max((double)result[0],(double)components[i].evaluate(A,inst,vars,tuple,useCurrentCvals,useCurrentPvals,
							mapatoms,useCurrentMvals,evaluated,params,returntype,valonly,null)[0]);
			}
			if (!sign)
				result[0] = 1-(double)result[0];
		}
		catch (RBNCompatibilityException e) {System.out.println(e);};
		return result;
	}

	
	
	public Double evalSample(RelStruc A, 
			Hashtable<String,PFNetworkNode>  atomhasht,
			OneStrucData inst, 
    		Hashtable<String,Double> evaluated,
			long[] timers) 
		throws RBNCompatibilityException {
		
		
		String key = null;
		
		if (evaluated != null) {
			key = this.makeKey(A);
			Double d = evaluated.get(key);
			if (d!=null) {
				return d; 
			}
		}
		
		double result =0;
		switch (operator){
		case ProbFormBool.OPERATORAND:
			result=1;
			for (int i=0;i<components.length;i++)
				result = result*components[i].evalSample(A, atomhasht, inst, evaluated, timers);
		case ProbFormBool.OPERATOROR:
			result=0;
			for (int i=0;i<components.length;i++)
				result = Math.max(result,components[i].evalSample(A, atomhasht, inst, evaluated, timers));
		}
		if (!sign)
			result = 1- result;
		if (evaluated != null) {
			evaluated.put(key, result);
		}
		return result;
	}

	@Override
	public int evaluatesTo(RelStruc A, OneStrucData inst,
			boolean usesampleinst, Hashtable atomhasht)
					throws RBNCompatibilityException {
		boolean evalTo0 = false;
		boolean evalTo1 = false;

		switch (operator){
		case ProbFormBool.OPERATORAND:
			evalTo0 = false;
			evalTo1 = true;
			for (int i=0;i<components.length;i++){
				int eval = components[i].evaluatesTo(A, inst, usesampleinst, atomhasht);
				if (eval == 0)
					evalTo0=true;
				if (eval != 1)
					evalTo1=false;
			}
			break;
		case ProbFormBool.OPERATOROR:
			evalTo0 = true;
			evalTo1 = false;
			for (int i=0;i<components.length;i++){
				int eval = components[i].evaluatesTo(A, inst, usesampleinst, atomhasht);
				if (eval == 1)
					evalTo1=true;
				if (eval != 0)
					evalTo0=false;
			}
		}
		if (evalTo0){
			if (sign)
				return 0;
			else return 1;
		}
		if (evalTo1){
			if (sign)
				return 1;
			else return 0;
		}
		return -1;
	}

	@Override
	public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
		boolean evalTo0 = false;
		boolean evalTo1 = false;

		switch (operator){
		case ProbFormBool.OPERATORAND:
			evalTo0 = false;
			evalTo1 = true;
			for (int i=0;i<components.length;i++){
				int eval = components[i].evaluatesTo(A);
				if (eval == 0)
					evalTo0=true;
				if (eval != 1)
					evalTo1=false;
			}
			break;
		case ProbFormBool.OPERATOROR:
			evalTo0 = true;
			evalTo1 = false;
			for (int i=0;i<components.length;i++){
				int eval = components[i].evaluatesTo(A);
				if (eval == 1)
					evalTo1=true;
				if (eval != 0)
					evalTo0=false;
			}
		}
		if (evalTo0){
			if (sign)
				return 0;
			else return 1;
		}
		if (evalTo1){
			if (sign)
				return 1;
			else return 0;
		}
		return -1;
	}

	@Override
	public String[] freevars() {
		String[] result = components[0].freevars();
		for (int i=1;i<components.length;i++)
			result = rbnutilities.arraymerge(result,components[i].freevars());
		return result;
	}

//	@Override
//	public Vector<GroundAtom> makeParentVec(RelStruc A) throws RBNCompatibilityException {
//		
//		return makeParentVec(A, new OneStrucData(),null);
//	}

	@Override
	public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
			throws RBNCompatibilityException {
		int evalto = evaluatesTo(A,inst,false,null);
		
		if (evalto != -1)
			return new Vector<GroundAtom>();
		
		Vector<GroundAtom> result = components[0].makeParentVec(A, inst,macrosdone);
		for (int i=1;i<components.length;i++)
			result = rbnutilities.combineAtomVecs(result,components[i].makeParentVec(A, inst,macrosdone));
		return result;
	}



	@Override
	public ProbForm sEval(RelStruc A) 
			throws RBNCompatibilityException {

		int evalto = evaluatesTo(A);

		switch (evalto){
		case 0:
			if (sign)
				return new ProbFormBoolConstant(false);
			else return new ProbFormBoolConstant(true);
		case 1: 
			if (sign)
				return new ProbFormBoolConstant(true);
			else return new ProbFormBoolConstant(false);
		case -1:
			ProbFormBool[] sevalarray = new ProbFormBool[components.length];
			for (int i=0;i<components.length;i++)
				sevalarray[i]=(ProbFormBool)components[i].sEval(A);
			return new ProbFormBoolComposite(sevalarray,operator,sign);
		default: 
			return this;
		}
	}

	@Override
	public ProbForm substitute(String[] vars, int[] args) {
		ProbFormBool[] substarray = new ProbFormBool[components.length];
		for (int i=0;i<components.length;i++)
			substarray[i]=(ProbFormBool)components[i].substitute(vars,args);
		ProbFormBoolComposite result = new ProbFormBoolComposite(substarray,operator,sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public ProbForm substitute(String[] vars, String[] args) {
		ProbFormBool[] substarray = new ProbFormBool[components.length];
		for (int i=0;i<components.length;i++)
			substarray[i]=(ProbFormBool)components[i].substitute(vars,args);
		ProbFormBoolComposite result = new ProbFormBoolComposite(substarray,operator,sign);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	public int numComponents(){
		return components.length;
	}
	
	public ProbFormBool componentAt(int i){
		return components[i];
	}
	
	public int operator(){
		return operator;
	}
	
	public ProbForm toStandardPF(boolean recursive){
		/* returns a ProbFormCombFunc or ProbFormConvComb
		 * If sign == true:
		 *     operator == and: return (n-or{negated components}:0,1)
		 *     operator == or : return n-or{components}
		 *    sign == false:
		 *     operator == and: return n-or{negated components}
		 *     operator == or : return (n-or{components:0,1) 
		 *     
		 *  If recursive == true, then components are recursively cast
		 *  as standard (non ProbFormBool) formulas   
		 */
		
		
		ProbForm[] pfargs = new ProbForm[components.length];
		for (int i=0;i<components.length;i++){
			if (operator==ProbFormBool.OPERATORAND){
				if (recursive)
					pfargs[i]= new ProbFormConvComb(components[i].toStandardPF(true),new ProbFormConstant(0),new ProbFormConstant(1));
				else{
					pfargs[i] = components[i].clone();
					((ProbFormBool)pfargs[i]).toggleSign();
				}
		
			}
			if (operator==ProbFormBool.OPERATOROR){
				if (recursive)
					pfargs[i]= components[i].toStandardPF(true);
				else 
					pfargs[i]= components[i].clone();   
			}
		}
		ProbFormCombFunc pfcomb = new ProbFormCombFunc("n-or",pfargs,new String[0],new ProbFormBoolConstant(true));
		if ( (sign && operator==ProbFormBool.OPERATOROR) || ( !sign && operator==ProbFormBool.OPERATORAND)) {
			return pfcomb;		
		}
		else {
			ProbFormConvComb result = new ProbFormConvComb(pfcomb,new ProbFormConstant(0),new ProbFormConstant(1));
			return result;
		}
	}
	
	public ProbFormBoolComposite clone(){
		ProbFormBool[] clonedcomps = new ProbFormBool[components.length];
		for (int i=0;i<components.length; i++)
			clonedcomps[i]=components[i].clone();
		return new ProbFormBoolComposite(clonedcomps,operator,sign);
	}
	
//	public void updateSig(Signature s){
//		for (int i=0;i<components.length;i++)
//			components[i].updateSig(s);	
//		}
//	
	public TreeSet<Rel> parentRels(){
		TreeSet<Rel> result = new TreeSet<Rel>();
		for (int i=0;i<components.length; i++)
			result.addAll(components[i].parentRels());
		return result;
	}
	
	public TreeSet<Rel> parentRels(TreeSet<String> processed){
		String mykey = this.makeKey(null,null,true);
		if (processed.contains(mykey))
			return new TreeSet<Rel>();
		else {
			processed.add(mykey);
			TreeSet<Rel> result = new TreeSet<Rel>();
			for (int i=0;i<components.length; i++)
				result.addAll(components[i].parentRels(processed));
			return result;
		}
	}
}
