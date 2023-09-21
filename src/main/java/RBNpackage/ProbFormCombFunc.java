/* ProbFormCombFunc.java
 * 
 * Copyright (C) 2003 Max-Planck-Institut fuer Informatik,
 *                    Helsinki Institute for Information Technology
 *
 * contact:
 * jaeger@cs.auc.dk   www.cs.auc.dk/~jaeger/Primula.html
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package RBNpackage;


import java.util.*;
import RBNExceptions.*;
import RBNLearning.GradientGraph;
import RBNgui.Primula;
import RBNutilities.*;
import RBNLearning.Profiler;


public class ProbFormCombFunc extends ProbForm{

	/**
	 * @uml.property  name="mycomb"
	 * @uml.associationEnd  
	 */
	private CombFunc mycomb;
	/**
	 * @uml.property  name="pfargs"
	 * @uml.associationEnd  multiplicity="(0 -1)"
	 */
	private ProbForm pfargs[];
	/**
	 * @uml.property  name="quantvars" multiplicity="(0 -1)" dimension="1"
	 */
	private String quantvars[];
	/**
	 * @uml.property  name="cconstr"
	 * @uml.associationEnd  
	 */
	private ProbFormBool cconstr;



	/**
	 * @uml.property  name="mycombInt"
	 */
	private int mycombInt;




	public CombFunc getMycomb() {
		return mycomb;
	}

	public ProbForm[] getPfargs() {
		return pfargs;
	}

	public String[] getQuantvars() {
		return quantvars;
	}

	public ProbFormBool getCconstr() {
		return cconstr;
	}

	public int getMycombInt() {
		return mycombInt;
	}

	public ProbFormCombFunc()
	{}

	/** Creates new ProbFormCombFunc */
	public ProbFormCombFunc(CombFunc mc,ProbForm[] pfa, String[] qvars, ProbFormBool cc) 
			throws IllegalArgumentException
			{
		// Construct SSymbs and RSymbs
		//		SSymbs = new Rel[0];
		//		RSymbs = new Rel[0];

		//		if (pfa.length > 0)
		//			for (int i = 0; i<pfa.length; i++)
		//			{
		//				rbnutilities.arraymerge(SSymbs,pfa[i].SSymbs);
		//				rbnutilities.arraymerge(RSymbs,pfa[i].RSymbs);
		//			}
		//		SSymbs = rbnutilities.arraymerge(SSymbs,cc.SSymbs);

		// Construct mycomb
		mycomb = mc;
		if (mc instanceof CombFuncNOr) mycombInt = CombFunc.NOR;
		if (mc instanceof CombFuncMean) mycombInt = CombFunc.MEAN;
		if (mc instanceof CombFuncInvsum) mycombInt = CombFunc.INVSUM;
		if (mc instanceof CombFuncESum) mycombInt = CombFunc.ESUM;
		if (mc instanceof CombFuncLReg) mycombInt = CombFunc.LREG;
		if (mc instanceof CombFuncLLReg) mycombInt = CombFunc.LLREG;
		if (mc instanceof CombFuncSum) mycombInt = CombFunc.SUM;
		if (mc instanceof CombFuncProd) mycombInt = CombFunc.PROD;


		// Construct pfargs
		pfargs = pfa; 
		// Construct quantvars
		quantvars = qvars;
		// Construct cconstr
		cconstr = cc;

			}

	public ProbFormCombFunc(String mc,ProbForm[] pfa, String[] qvars, ProbFormBool cc) 
			throws IllegalArgumentException
			{

		// Construct SSymbs and RSymbs
		//		SSymbs = new Rel[0];
		//		RSymbs = new Rel[0];
		//		
		//		if (pfa.length > 0)
		//			for (int i = 0; i<pfa.length; i++)
		//			{
		//				rbnutilities.arraymerge(SSymbs,pfa[i].SSymbs);
		//				rbnutilities.arraymerge(RSymbs,pfa[i].RSymbs);
		//			}
		//		SSymbs = rbnutilities.arraymerge(SSymbs,cc.SSymbs);

		// Construct z
		if (mc.equals("n-or")) mycombInt = CombFunc.NOR;
		if (mc.equals("mean")) mycombInt = CombFunc.MEAN;
		if (mc.equals("invsum")) mycombInt = CombFunc.INVSUM;
		if (mc.equals("esum")) mycombInt = CombFunc.ESUM;
		if (mc.equals("l-reg")) mycombInt = CombFunc.LREG;
		if (mc.equals("ll-reg")) mycombInt = CombFunc.LLREG;
		if (mc.equals("sum")) mycombInt = CombFunc.SUM;
		if (mc.equals("prod")) mycombInt = CombFunc.PROD;

		//if (mc.equals("logical")) sw = 3;
		//if (mc.equals("size")) sw = 4;
		switch(mycombInt)
		{
		case CombFunc.NOR:  mycomb = new CombFuncNOr(); break;
		case CombFunc.MEAN:  mycomb = new CombFuncMean(); break;
		case CombFunc.INVSUM:  mycomb = new CombFuncInvsum(); break;
		case CombFunc.ESUM:  mycomb = new CombFuncESum(); break;
		case CombFunc.LREG:  mycomb = new CombFuncLReg(); break;
		case CombFunc.LLREG:  mycomb = new CombFuncLLReg(); break;
		case CombFunc.SUM:  mycomb = new CombFuncSum(); break;
		case CombFunc.PROD:  mycomb = new CombFuncProd(); break;

		default: throw new IllegalArgumentException("Illegal Argument for construction of ProbFormCombFunc: " + mc);
		}
		// Construct pfargs
		pfargs = pfa; 
		// Construct quantvars
		quantvars = qvars;
		// Construct cconstr
		cconstr = cc;

			}

	public  String[] freevars()
	{
		String result[]={};
		// first collect all the free variables from the pfargs formulas
		for (int i = 0 ; i<pfargs.length ; i++)
			result = rbnutilities.arraymerge(result,pfargs[i].freevars());
		// add the variables in the constraint:
		result = rbnutilities.arraymerge(result,cconstr.freevars());
		// subtract the variables in quantvars
		result = rbnutilities.arraysubstraction(result,quantvars);
		return result;
	}

	public boolean multlinOnly(){	
		if (!(mycomb instanceof MultLinCombFunc)) 
			return false;
		boolean result = true;
		for (int i=0;i<pfargs.length;i++)
			if (!pfargs[i].multlinOnly()) 
				result = false;
		return result;
	}

	public ProbForm substitute(String[] vars, int[] args)
	{
		ProbFormCombFunc result;
		ProbFormBool subcconstr = null;
		/* Construct new substitution arguments by 
		 * eliminating the variables that appear in 
		 * quantvars and their associated 
		 * substitution values from vars and args
		 */
		String[] subsvars;
		subsvars = rbnutilities.arraysubstraction(vars,quantvars);
		int[] subsargs = rbnutilities.CorrArraySubstraction(subsvars,vars,args);


		// Perform substitution on pfargs
		ProbForm[]  subpfargs = new ProbForm[pfargs.length];
		for (int i = 0; i<pfargs.length; i++)
			subpfargs[i]=pfargs[i].substitute(subsvars,subsargs);
		//Perform substitution on cconstr

		subcconstr = (ProbFormBool)cconstr.substitute(vars,args);

		result = new ProbFormCombFunc(mycomb.name,subpfargs,quantvars,subcconstr);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	public ProbForm substitute(String[] vars, String[] args)
	{
		ProbFormCombFunc result;
		ProbForm[]  subpfargs = new ProbForm[pfargs.length];
		ProbFormBool subcconstr = null;


		// Rename all the variables bound 
		// by combination function
		String[] freev = freevars();
		String[] reserved = new String[vars.length+args.length+freev.length];
		for (int i = 0;i<vars.length;i++)
			reserved[i]=vars[i];
		for (int i = 0;i<args.length;i++)
			reserved[vars.length+i]=args[i];
		for (int i = 0;i<freev.length;i++)
			reserved[vars.length+args.length+i]=freev[i];

		String[] newquantvars = rbnutilities.NewVariables(reserved,quantvars.length);

		for (int i = 0; i<pfargs.length; i++)
		{
			subpfargs[i]=pfargs[i].substitute(quantvars,newquantvars);
		}


		subcconstr = (ProbFormBool)cconstr.substitute(quantvars,newquantvars);

		// Now perform the original substitution
		for (int i = 0; i<pfargs.length; i++)
		{
			subpfargs[i]=subpfargs[i].substitute(vars,args);
		}

		subcconstr = (ProbFormBool)subcconstr.substitute(vars,args);
		result = new ProbFormCombFunc(mycomb.name,subpfargs,newquantvars,subcconstr);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}


	public  Vector makeParentVec(RelStruc A)
			throws RBNCompatibilityException{
		return makeParentVec(A,new OneStrucData(),null);
	}

	public  Vector makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
			throws RBNCompatibilityException{
		//System.out.println("makeParentVec for " + this.asString() + ": ");
		Vector result = new Vector();
		ProbForm nextprobform;

		int[][] subslist = A.allTrue(cconstr,quantvars);

		for (int i=0; i<pfargs.length; i++)
		{
			for (int j=0; j<subslist.length; j++)
			{
				nextprobform = pfargs[i].substitute(quantvars,subslist[j]);
				result = rbnutilities.combineAtomVecs(result,nextprobform.makeParentVec(A,inst,macrosdone));
			}
		}
		return result;
	}


	//	public ProbForm conditionEvidence(OneStrucData instasosd)
	//	{
	//		ProbFormCombFunc result;
	//
	//		// Perform conditionEvidence on pfargs
	//		ProbForm[]  condpfargs = new ProbForm[pfargs.length];
	//		for (int i = 0; i<pfargs.length; i++)
	//			condpfargs[i]=pfargs[i].conditionEvidence(instasosd);
	//
	//		result = new ProbFormCombFunc(mycomb.name,condpfargs,quantvars,cconstr);
	//		return result;
	//	}


	public ProbForm conditionEvidence(RelStruc A, OneStrucData inst)
			throws RBNCompatibilityException
			{
		//System.out.println("condition Evidence for " + this.asString());
		ProbForm nextcondpfarg;

		int[][] subslist = A.allTrue(cconstr,quantvars);

		double[] condpfargs =new double[pfargs.length*subslist.length];
		boolean allconstant = true;
		int i = 0;
		int j = 0;
		int currentindex = 0;
		while (i<pfargs.length && allconstant){
			j=0;
			while (j<subslist.length && allconstant){
				nextcondpfarg = pfargs[i].substitute(quantvars,subslist[j]).conditionEvidence(A,inst);
				if (!(nextcondpfarg instanceof ProbFormConstant)) allconstant = false;
				else condpfargs[currentindex]=((ProbFormConstant)nextcondpfarg).cval;
				j++;
				currentindex++;
			}
			i++;
		}

		if (allconstant){
			//System.out.println("returned " + mycomb.evaluate(condpfargs));
			return new ProbFormConstant(mycomb.evaluate(condpfargs));
		}
		else{
			//System.out.println("returned old");
			return this;
		}
			}


	public boolean dependsOn(String variable, RelStruc A, OneStrucData data)
			throws RBNCompatibilityException
			{
		Boolean result = false;
		/* One should test whether any ground instances of the arguments depends on variable.
		 * E.g.:
		 *      n-or{(r(v):#t,0)|w: l(v,w)}
		 * This formula depends formally on #t.
		 * However, when there are no a,b in A with l(a,b), and r(a) true or undetermined in the
		 * data, then this formula (in this context) does not actually depend on #t
		 */
		int[][] subslist = A.allTrue(cconstr,quantvars);
		for (int i=0; i<pfargs.length; i++){
			for (int j=0; j<subslist.length; j++){
				result = (result || pfargs[i].substitute(quantvars,subslist[j]).dependsOn(variable,A,data)); 
			}
		}
		return result;
			}

//	public double evaluate(RelStruc A, 
//			OneStrucData inst, 
//			String[] vars, 
//			int[] tuple, 
//			boolean useCurrentCvals, 
//			String[] numrelparameters,
//			boolean useCurrentPvals,
//    		GroundAtomList mapatoms,
//    		boolean useCurrentMvals,
//    		Hashtable<String,Double> evaluated)
//					throws RBNCompatibilityException
//					{
//		
//		ProbFormCombFunc subspfcf = (ProbFormCombFunc)this.substitute(vars,tuple);
//
//		String key="";
//		if (evaluated != null) {
//			key = GradientGraph.makeKey(subspfcf, 0, 0, A);
//			
//			Double d = evaluated.get(key);
//			if (d!=null)
//				return d; 
//		}
//
//		//        CConstr subscc = this.cconstr.substitute(vars,tuple);
//
//		//		/*
//		//		* generate list of all substitution tuples for quantvars
//		//		* that satisfy the cconstr
//		//		*/
//		//		int[][] subslist = A.allTrue(subscc,quantvars);
//		int[][] subslist = tuplesSatisfyingCConstr(A, vars, tuple);
//
//		/* Initialize array of arguments for combination function */
//		double[] combargs = new double[subspfcf.pfargs.length*subslist.length];
//
//		/* Evaluate the probability formulas in pfargs and 
//		 * enter results into combargs
//		 */
//		int nextindex;
//		double nextvalue;
//		for (int i=0; i<subspfcf.pfargs.length; i++)
//		{
//			for (int j=0; j<subslist.length; j++)
//			{
//				nextindex = i*subslist.length + j;
//				nextvalue = subspfcf.pfargs[i].evaluate(A,inst,quantvars,subslist[j],useCurrentCvals,numrelparameters,
//						useCurrentPvals,
//						mapatoms,useCurrentMvals,evaluated);
//				//if (Double.isNaN(nextvalue)) return Double.NaN;
//				combargs[nextindex]=nextvalue;
//			}
//		}
//
//		/*
//        apply mycomb to the resulting array
//		 */
//		double result = mycomb.evaluate(combargs);
//		if (evaluated != null)
//			evaluated.put(key, result);
//		
//		return result;
//					}

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
					throws RBNCompatibilityException
	{			

		Boolean profile = (profiler != null);
		
		String key="";
		
		long timebeforelookup = System.currentTimeMillis();
		if (evaluated != null) {
			key = this.makeKey(vars,tuple,false);		
//			System.out.println("debug: looking for " + key);
			Object[] d = evaluated.get(key);
			if (d!=null) {
				//System.out.println("debug:  yes found");
				if (profile) {
					profiler.addTime(Profiler.NUM_EVALUATE_OLD, 1);
					profiler.addTime(Profiler.TIME_OLDLOOKUP, System.currentTimeMillis()-timebeforelookup);
				}
				return d; 
			}
		}
		if (profile)
			profiler.addTime(Profiler.TIME_OLDLOOKUP, System.currentTimeMillis()-timebeforelookup);
		//System.out.println("debug:   not found");
		
		long beforesat = System.currentTimeMillis();
		
		ProbFormCombFunc subspfcf = (ProbFormCombFunc)this.substitute(vars,tuple);
		
		int[][] subslist = tuplesSatisfyingCConstr(A, vars, tuple);

		if (profile)
			profiler.addTime(Profiler.TIME_GETSATISFYING, System.currentTimeMillis()-beforesat);
		
		/* Initialize array of arguments for combination function */
		Vector<Object[]> combargs = new Vector<Object[]>();

		/* Evaluate the probability formulas in pfargs and 
		 * enter results into combargs
		 */
		int nextindex;
		double[] nextvalue;
		
		
		for (int i=0; i<subspfcf.pfargs.length; i++)
		{
			for (int j=0; j<subslist.length; j++)
			{
				combargs.add(subspfcf.pfargs[i].evaluate(A,
						inst,
						quantvars,
						subslist[j],
						useCurrentCvals,
						useCurrentPvals,
						mapatoms,
						useCurrentMvals,
						evaluated,
						params,
						returntype,
						valonly,
						profiler));
			}
		}

	
		
		/*
        apply mycomb to the resulting array
		 */
		Object[] result = new Object[2];

		double[] vals = new double[combargs.size()];
		int i=0;
		for (Object[] d: combargs) {
			vals[i]= (Double)d[0];
			i++;
		}
		result[0]=mycomb.evaluate(vals);
		
	
		if (!valonly) {
			if (returntype == ProbForm.RETURN_ARRAY) {
				long timebeforearraycreate = System.currentTimeMillis();
				result[1]=new double[params.size()];
				for (int k=0; k<params.size();k++) {
					double[] derivs = new double[combargs.size()];
					i =0;
					for (Object[] d: combargs) {
						derivs[i]= ((double[])d[1])[k];
						i++;
					}
					((double[])result[1])[k]=mycomb.evaluateGrad(vals, derivs);
				}
				profiler.addTime(Profiler.TIME_ARRAYCREATE,System.currentTimeMillis()-timebeforearraycreate);
			}
			else {
				long timebeforehashcreate = System.currentTimeMillis();
				result[1]=new Hashtable<String,Double>();
				TreeSet<String> donekeys = new TreeSet<String>();
				for (int j=0; j<combargs.size(); j++) {
					for (String p: ((Hashtable<String,Double>)combargs.elementAt(j)[1]).keySet()) 
					{
						if (!donekeys.contains(p)) {
							double[] derivs = new double[combargs.size()];
							/* The elements in combargs.elementAt(0),...,combargs.elementAt(j-1) had a 
							 * 0 derivative for parameter p */
							for (int h=j; h<combargs.size(); h++) {
								Double di = ((Hashtable<String,Double>)combargs.elementAt(h)[1]).get(p);
								if (di==null)
									derivs[h]=0.0;
								else
									derivs[h]=di;
							}
							donekeys.add(p);
							((Hashtable<String,Double>)result[1]).put(p,mycomb.evaluateGrad(vals, derivs));
						}
					}
					
				}
				if (profile)
					profiler.addTime(Profiler.TIME_HASHCREATE, System.currentTimeMillis()-timebeforehashcreate);
			}// not (returntype == ProbForm.RETURN_ARRAY)

		} // if (!valonly) {
		if (evaluated != null) {
			//System.out.println("ProbFormCombFunc: adding to evaluated: " + key + " = " + result[0] );
			evaluated.put(key, result);
		}

		if (profile) {
			//System.out.println("debug: evaluated new " + key);
			profiler.addTime(Profiler.NUM_EVALUATE_NEW, 1);
		}
		return result;
	}	
	
	public  double evalSample(RelStruc A, Hashtable atomhasht, OneStrucData inst, long[] timers)
			throws RBNCompatibilityException
			{
		long inittime; 
		/* Same code as in evaluate and evaluatesTo: */
		ProbFormBool scc = this.cconstr;
		inittime=System.currentTimeMillis();
		int[][] subslist = A.allTrue(scc,quantvars);
		timers[3]=timers[3]+System.currentTimeMillis()-inittime;

		ProbForm groundpf;
		double[] combargs = new double[this.pfargs.length*subslist.length];
		int nextindex;
		double nextvalue;
		for (int i=0; i<this.pfargs.length; i++)
		{
			for (int j=0; j<subslist.length; j++)
			{
				nextindex = i*subslist.length + j;
				groundpf = this.pfargs[i].substitute(quantvars,subslist[j]);
				nextvalue = groundpf.evalSample(A,atomhasht,inst,timers);
				combargs[nextindex]=nextvalue;
			}
		}

		return mycomb.evaluate(combargs);
			}

	public int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable atomhasht)
			throws RBNCompatibilityException
			{
		/* First create an argument vector for the combination function
		 * as in evaluate(...). Arguments are 1,0,-1 according to 
		 * recursive calls on subformulas
		 */
		ProbFormBool scc = this.cconstr;
		/*
		 * generate list of all substitution tuples for quantvars
		 * that satisfy the cconstr
		 */

		int[][] subslist = A.allTrue(scc,quantvars);

		/* Initialize array of arguments for combination functin */
		int[] combargs = new int[this.pfargs.length*subslist.length];

		/* Recursive calls
		 */
		int nextindex;
		int nextvalue;
		for (int i=0; i<this.pfargs.length; i++)
		{
			for (int j=0; j<subslist.length; j++)
			{
				nextindex = i*subslist.length + j;
				nextvalue = this.pfargs[i].substitute(quantvars,subslist[j]).evaluatesTo(A,inst,usesampleinst,atomhasht);
				combargs[nextindex]=nextvalue;
			}
		}

		/* Now ask mycomb what it is going to do with this...
		 */
		return mycomb.evaluatesTo(combargs);
			}


	public int evaluatesTo(RelStruc A)
			throws RBNCompatibilityException
			{
		/* First create an argument vector for the combination function
		 * as in evaluate(...). Arguments are 1,0,-1 according to 
		 * recursive calls on subformulas
		 */
		ProbFormBool scc = this.cconstr;
		/*
		 * generate list of all substitution tuples for quantvars
		 * that satisfy the cconstr
		 */
		int[][] subslist = A.allTrue(scc,quantvars);

		/* Initialize array of arguments for combination functin */
		int[] combargs = new int[this.pfargs.length*subslist.length];

		/* Recursive calls
		 */
		int nextindex;
		int nextvalue;
		for (int i=0; i<this.pfargs.length; i++)
		{
			for (int j=0; j<subslist.length; j++)
			{
				nextindex = i*subslist.length + j;
				nextvalue = this.pfargs[i].substitute(quantvars,subslist[j]).evaluatesTo(A);
				combargs[nextindex]=nextvalue;
			}
		}

		/* Now ask mycomb what it is going to do with this...
		 */
		return mycomb.evaluatesTo(combargs);
			}


	public  String[] parameters()
	{
		String result[]={};
		for (int i = 0 ; i<pfargs.length ; i++)
			result = rbnutilities.arraymerge(result,pfargs[i].parameters());
		return result;
	}

	//	public String asString(int syntax, int depth)
	//	/* precedes string representation of formula with depth 
	//	 * tabs
	//	 */ 
	//	{
	//		String result;
	//		String tabstring = "";
	//
	//		for (int i=0;i<depth;i++)
	//			tabstring = tabstring +"  ";
	//
	//		result = tabstring + mycomb.name+"{";
	//		for (int i = 0; i<pfargs.length-1; i++)
	//		{
	//			result = result +'\n' + pfargs[i].asString(syntax, depth+1) + ",";
	//		}
	//		if (pfargs.length >= 1) 
	//			result = result +'\n' +pfargs[pfargs.length-1].asString(syntax, depth+1);
	//		result = result + '\n' +"  "+ tabstring + "| " 
	//		+ rbnutilities.arrayToString(quantvars) + " : " +cconstr.asString();
	//		result =  result +'\n' + tabstring + "}";
	//		return result;
	//	}

	//	public String asString(int syntax)
	//	{
	//		String result;
	//		result = mycomb.name+"{";
	//		for (int i = 0; i<pfargs.length-1; i++)
	//		{
	//			result = result +'\n' + pfargs[i].asString(1,0) + ",";
	//		}
	//		if (pfargs.length >= 1) 
	//			result = result +'\n'  +pfargs[pfargs.length-1].asString(syntax, 1);
	//		result = result +'\n'+ '\t' + "| " ;
	//		result = result +'\n'+ '\t' + rbnutilities.arrayToString(quantvars) + " : ";
	//		result =  result +'\n'+ '\t' + cconstr.asString();
	//		result =  result +'\n'+ "}";
	//		return result;
	//	}

	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue,boolean usealias)
	/* precedes string representation of formula with depth 
	 * tabs
	 */ 
	{
		if (usealias && this.getAlias() != null)
			return this.getAlias();
		String result="";
		String tabstring = "";
		for (int i=0;i<depth;i++)
			tabstring = tabstring +" ";

		int newdepth=0;
		String newtabstring = "";


		switch (syntax){
		case Primula.CLASSICSYNTAX:
			if (depth >= 0){
				newdepth = depth + mycomb.name.length();	
				for (int i=0;i<newdepth;i++)
					newtabstring = newtabstring +" ";
				result =   mycomb.name+"{";
				for (int i = 0; i<pfargs.length-1; i++)
				{
					result = result +'\n' + newtabstring + " " + pfargs[i].asString(syntax, newdepth ,A,paramsAsValue,usealias) + ",";
				}
				if (pfargs.length >= 1) 
					result = result +'\n' + newtabstring + " "+pfargs[pfargs.length-1].asString(syntax, newdepth + 1, A,paramsAsValue,usealias);
				result = result + '\n' + newtabstring + " " + "|" + rbnutilities.arrayToString(quantvars) + " : ";
				result =  result +cconstr.asString(Primula.CLASSICSYNTAX,0,A,paramsAsValue,usealias);
				result =  result +'\n' + newtabstring + "}";
			}
			else // depth =-1
			{
				result =   mycomb.name+"{";
				for (int i = 0; i<pfargs.length-1; i++)
				{
					result = result +" " + pfargs[i].asString(syntax, -1 ,A,paramsAsValue,usealias) + ",";
				}
				if (pfargs.length >= 1) 
					result = result + " " +pfargs[pfargs.length-1].asString(syntax, -1, A,  paramsAsValue,usealias);
				result = result + " " + "|" + rbnutilities.arrayToString(quantvars) + " : ";
				result =  result +cconstr.asString(Primula.CLASSICSYNTAX,0,A,paramsAsValue,usealias) + "}";
			}
			break;
		case Primula.CHERRYSYNTAX:
			result = "COMBINE " ;
			if (pfargs.length >= 1) 
				result = result  + pfargs[0].asString(syntax, depth+8 ,A,paramsAsValue,usealias);
			for (int i = 1; i<pfargs.length; i++)
			{
				result = result + "," +'\n' + tabstring + "        " + pfargs[i].asString(syntax, depth+8 ,A,  paramsAsValue,usealias) ;
			}
			result = result + '\n' + tabstring + "WITH " + mycomb.name;
			result = result + '\n' + tabstring + "FORALL " +  rbnutilities.arrayToString(quantvars);
			result = result + '\n' + tabstring + "WHERE " + cconstr.asString(Primula.CHERRYSYNTAX,0,A, paramsAsValue,usealias) ; 
		}
		return result;
	}


	/** Returns the combination function used in this probform according to 
	 * the integer encoding:
	 *  N-or = 1 (= static int ProbFormCombFunc.NOR);
	 *  Mean = 2 (= static int ProbFormCombFunc.MEAN);
	 * InvSum = 3 (= static int ProbFormCombFunc.INVSUM);
	 * ESUM = 3 (= static int ProbFormCombFunc.INVSUM);
	 */
	//	public int myCombInt(){
	//		return mycombInt;
	//	}

	/** Returns the number of probability formulas in the argument of this formula's
	 * combination function
	 */
	public int numPFargs(){
		return pfargs.length;
	}

	/** Returns the i'th probability formula in the argument of this formula's combination
	 * function
	 */
	public ProbForm probformAt(int i){
		return pfargs[i];
	}


	/** Returns the quantvars of this combination function */
	public String[] quantvars(){
		return quantvars;
	}

	public ProbForm sEval(RelStruc A)
			throws RBNCompatibilityException
			{
		int[][] subslist = A.allTrue(cconstr,quantvars);

		ProbForm[]  sevalpfargs = new ProbForm[pfargs.length*subslist.length];
		for (int i = 0; i<pfargs.length; i++){
			for (int j=0;j<subslist.length;j++){
				sevalpfargs[subslist.length*i+j]=pfargs[i].substitute(quantvars,subslist[j]).sEval(A);
			}
		}

		return new ProbFormCombFunc(mycomb,sevalpfargs,new String[0],new ProbFormBoolConstant(true));

			}

	/** Returns the set of all tuples in A that satisfy the CConstr of this formula
	 * after the substituion vars/tuple has been performed
	 */
	public int[][] tuplesSatisfyingCConstr(RelStruc A,  String[] vars, int[] tuple)
			throws RBNCompatibilityException
			{	
		ProbFormBool subscc = (ProbFormBool)this.cconstr.substitute(vars,tuple);
		return  A.allTrue(subscc,quantvars);

			}

//	public void setParameters(String[] params,  double[] values){
//		for (int i = 0; i<pfargs.length; i++)
//			pfargs[i].setParameters(params,values);
//	}
//
//	public void setRandomParameterVals(){
//		for (int i = 0; i<pfargs.length; i++)
//			pfargs[i].setRandomParameterVals();
//	}
	
	public void updateSig(Signature s){
		for (int i=0;i<pfargs.length;i++)
			pfargs[i].updateSig(s);
		cconstr.updateSig(s);
	}
	public void setCvals(String paramname, double val) {
		for (int i=0;i<pfargs.length;i++)
			pfargs[i].setCvals(paramname,val);
	}
	
	public TreeSet<Rel> parentRels(){
		TreeSet<Rel> result = new TreeSet<Rel>();
		for (int i=0;i<pfargs.length;i++)
			result.addAll(pfargs[i].parentRels());
		return result;
	}
	
	public TreeSet<Rel> parentRels(TreeSet<String> processed){
		String mykey = this.makeKey(null,null,true);
		if (processed.contains(mykey))
			return new TreeSet<Rel>();
		else {
			processed.add(mykey);
			TreeSet<Rel> result = new TreeSet<Rel>();
			for (int i=0;i<pfargs.length;i++)
				result.addAll(pfargs[i].parentRels(processed));
			return result;
		}
	}
}