/*
 * ProbFormConvComb.java
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
import java.io.*;
import RBNExceptions.*;
import RBNutilities.*;
import RBNgui.Primula;


public class ProbFormConvComb extends ProbForm {

	/**
	 * @uml.property  name="f1"
	 * @uml.associationEnd  
	 */
	ProbForm F1;
	/**
	 * @uml.property  name="f2"
	 * @uml.associationEnd  
	 */
	ProbForm F2;
	/**
	 * @uml.property  name="f3"
	 * @uml.associationEnd  
	 */
	ProbForm F3;

	public ProbFormConvComb()
	{}

	/** Creates new ProbFormConvComb */
	public ProbFormConvComb(ProbForm f1, ProbForm f2, ProbForm f3) {
//		SSymbs = rbnutilities.arraymerge(f1.SSymbs,f2.SSymbs);
//		SSymbs = rbnutilities.arraymerge(SSymbs, f3.SSymbs);
//		RSymbs = rbnutilities.arraymerge(f1.RSymbs,f2.RSymbs);
//		RSymbs = rbnutilities.arraymerge(RSymbs, f3.RSymbs);
		F1 = f1;
		F2 = f2;
		F3 = f3;
	}


	public String[] freevars()
	{
		String result[];
		result = rbnutilities.arraymerge(F1.freevars(),F2.freevars());
		result = rbnutilities.arraymerge(result,F3.freevars());
		return result;
	}


	public boolean multlinOnly(){
		return (F1.multlinOnly() && F2.multlinOnly() && F3.multlinOnly());
	}

	public ProbForm substitute(String[] vars, int[] args)
	
	{   
		ProbFormConvComb result = new ProbFormConvComb(F1.substitute(vars,args),F2.substitute(vars,args),F3.substitute(vars,args));
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	public ProbForm substitute(String[] vars, String[] args)
	{
		ProbFormConvComb result = new ProbFormConvComb(F1.substitute(vars,args),F2.substitute(vars,args),F3.substitute(vars,args));
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}


	public  Vector makeParentVec(RelStruc A)
	throws RBNCompatibilityException
	{
		return makeParentVec(A,new OneStrucData());
	}

	public  Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst)
	throws RBNCompatibilityException
	{
		Vector<GroundAtom> atomvec1 = F1.makeParentVec(A,inst);
		Vector<GroundAtom> atomvec2 = F2.makeParentVec(A,inst);
		Vector<GroundAtom> atomvec3 = F3.makeParentVec(A,inst);
		Vector<GroundAtom> result = atomvec1;
		double v1,v2,v3;
		v1=F1.evaluate(A,inst,new String[0],new int[0],false,new String[0],false,new GroundAtomList(),false,null);
		v2=F2.evaluate(A,inst,new String[0],new int[0],false,new String[0],false,new GroundAtomList(),false,null);
		v3=F3.evaluate(A,inst,new String[0],new int[0],false,new String[0],false,new GroundAtomList(),false,null);

		if (!Double.isNaN(v1))
			atomvec1 = new Vector<GroundAtom>();

		if (v1==0)
			atomvec2 = new Vector<GroundAtom>();
		if (v1==1)
			atomvec3 = new Vector<GroundAtom>();

		if ((v2==v3) && !Double.isNaN(v2))
		{
			atomvec1 = new Vector<GroundAtom>();
			atomvec2 = new Vector<GroundAtom>();
			atomvec3 = new Vector<GroundAtom>();
		}

		result = atomvec1;
		result = rbnutilities.combineAtomVecs(result,atomvec2);
		result = rbnutilities.combineAtomVecs(result,atomvec3);
		return result;
	}

	public ProbForm conditionEvidence(RelStruc A, OneStrucData inst)
	throws RBNCompatibilityException
	{
		ProbForm newF1 = F1.conditionEvidence(A,inst);
		ProbForm newF2 = F2.conditionEvidence(A,inst);
		ProbForm newF3 = F3.conditionEvidence(A,inst);
		if ((newF1 instanceof ProbFormConstant)&&
				(newF2 instanceof ProbFormConstant)&&
				(newF3 instanceof ProbFormConstant))
		{
			double value = ((ProbFormConstant)newF1).cval*((ProbFormConstant)newF2).cval+(1-((ProbFormConstant)newF1).cval)*((ProbFormConstant)newF3).cval;
			return new ProbFormConstant(value);									     
		}
		else return new ProbFormConvComb(newF1,newF2,newF3);

	}

//	public ProbForm conditionEvidence(OneStrucData instasosd){
//		return new ProbFormConvComb(F1.conditionEvidence(instasosd),F2.conditionEvidence(instasosd),F3.conditionEvidence(instasosd));
//	}

	public ProbForm f1(){
		return F1;
	}

	public ProbForm f2(){
		return F2;
	}

	public ProbForm f3(){
		return F3;
	}


	public boolean dependsOn(String variable, RelStruc A, OneStrucData data)
	throws RBNCompatibilityException
	{
		int e1 = F1.evaluatesTo(A,data,false,null);
		if (e1==1) 
			return F2.dependsOn(variable,A,data);
		if (e1==0)
			return F3.dependsOn(variable,A,data);
		else
			return (F1.dependsOn(variable,A,data) || F2.dependsOn(variable,A,data)  || F3.dependsOn(variable,A,data));
	}


	public double evaluate(RelStruc A, OneStrucData inst, String[] vars, int[] tuple, 
			boolean useCurrentCvals,
			String[] numrelparameters,
    		boolean useCurrentPvals,
    		GroundAtomList mapatoms,
    		boolean useCurrentMvals,
    		Hashtable<String,Double> evaluated)
	throws RBNCompatibilityException
	{
		double ev1 = F1.evaluate(A,inst,vars,tuple,useCurrentCvals,numrelparameters,useCurrentPvals,
				mapatoms,useCurrentMvals,evaluated);
		double ev2 = F2.evaluate(A,inst,vars,tuple,useCurrentCvals,numrelparameters,useCurrentPvals,
				mapatoms,useCurrentMvals,evaluated);
		double ev3 = F3.evaluate(A,inst,vars,tuple,useCurrentCvals,numrelparameters,useCurrentPvals,
				mapatoms,useCurrentMvals,evaluated);


		if (Double.isNaN(ev1)) {
			if (ev2==ev3)
				return ev2;
			else
				return Double.NaN;
		}

		if ((ev1 != 0) && (Double.isNaN(ev2)) )
			return Double.NaN;

		if ((ev1 != 1) && (Double.isNaN(ev3)) )
			return Double.NaN;

		double firstterm =0;
		double secondterm =0;
		
		if (ev1==0 || ev2==0)
			firstterm =0;
		else firstterm = ev1*ev2;
		
		if (ev1 == 1 || ev3==0)
			secondterm =0;
		else secondterm = (1-ev1)*ev3;
		
	
		return firstterm + secondterm;

	}


	public  double evalSample(RelStruc A,Hashtable atomhasht,OneStrucData inst, long[] timers)
	throws RBNCompatibilityException
	{
		double v1;
		double v2 =0;
		double v3 =0;
		v1 =  F1.evalSample(A,atomhasht,inst,timers);
		if (v1 != 0.0)
			v2 =  F2.evalSample(A,atomhasht,inst,timers);
		if (v1 != 1.0)
			v3 =  F3.evalSample(A,atomhasht,inst,timers);
		return v1*v2+(1-v1)*v3;
	}


	public int evaluatesTo(RelStruc A)
	throws RBNCompatibilityException
	{	
		int e1 = F1.evaluatesTo(A);
		int e2 = F2.evaluatesTo(A);
		int e3 = F3.evaluatesTo(A);
		if (e1==1 && e2==0) return 0;
		if (e1==1 && e2==1) return 1;
		if (e1==0 && e3==0) return 0;
		if (e1==0 && e3==1) return 1;
		if (e2==0 && e3==0) return 0;
		if (e2==1 && e3==1) return 1;
		return -1;
	}


	public int evaluatesTo(RelStruc A,OneStrucData inst, boolean usesampleinst, Hashtable atomhasht)
	throws RBNCompatibilityException
	{	
		//System.out.println("evaluatesTo for " + this.asString());
		int e1 = F1.evaluatesTo(A,inst,usesampleinst,atomhasht);
		int e2 = F2.evaluatesTo(A,inst,usesampleinst,atomhasht);
		int e3 = F3.evaluatesTo(A,inst,usesampleinst,atomhasht);
		if (e1==1 && e2==0) return 0;
		if (e1==1 && e2==1) return 1;
		if (e1==0 && e3==0) return 0;
		if (e1==0 && e3==1) return 1;
		if (e2==0 && e3==0) return 0;
		if (e2==1 && e3==1) return 1;
		return -1;
	}

	public String[] parameters()
	{
		String result[];
		result = rbnutilities.arraymerge(F1.parameters(),F2.parameters());
		result = rbnutilities.arraymerge(result,F3.parameters());
		return result;
	}


	
	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias)
	{

		if (usealias && this.getAlias() != null)
			return this.getAlias();
		String tabstring = "";
		for (int i=0;i<depth;i++)
			tabstring = tabstring +" ";
		String result = "";
		switch (syntax){
		case Primula.CLASSICSYNTAX:
			if (depth >=0){
			result = "( " +  F1.asString(syntax,depth+2,A,paramsAsValue,usealias) + ":"  +'\n'+ 
			tabstring + " " + F2.asString(syntax,depth+2,A,paramsAsValue,usealias)  +'\n'+ 
			tabstring + "," +'\n'+ 
			tabstring + " " + F3.asString(syntax,depth+2,A,paramsAsValue,usealias) +'\n'+ 
			tabstring + ")";
			}
			else // depth =-1
			{
				result = "( " +  F1.asString(syntax,depth,A,paramsAsValue,usealias) + ":" + F2.asString(syntax,depth,A,paramsAsValue,usealias)   + "," 
						+ F3.asString(syntax,depth,A,paramsAsValue,usealias) + ")";
			}
			break;
		
		case Primula.CHERRYSYNTAX:
			result =  "WIF  " + F1.asString(syntax,depth+5,A,paramsAsValue,usealias) +'\n'
			+ tabstring + "THEN  " + F2.asString(syntax,depth+6,A,paramsAsValue,usealias)  +'\n' 
			+ tabstring + "ELSE  " + F3.asString(syntax,depth+6,A,paramsAsValue,usealias) ;
		}
		
		return result;

	}

	public ProbForm sEval(RelStruc A)
	throws RBNCompatibilityException
	{
		ProbForm f1 = F1.sEval(A);
		ProbForm f2 = F2.sEval(A);
		ProbForm f3 = F3.sEval(A);
		return new ProbFormConvComb(f1,f2,f3);
	}

	public ProbForm subPF(int i){
		switch (i){
		case 1: return F1;
		case 2: return F2;
		case 3: return F3;
		default: return null;
		}
	}

//	public void setParameters(String[] params,  double[] values){
//		F1.setParameters(params,values);
//		F2.setParameters(params,values);
//		F3.setParameters(params,values);
//	}
//	
//	public void setRandomParameterVals(){
//		F1.setRandomParameterVals();
//		F2.setRandomParameterVals();
//		F3.setRandomParameterVals();
//	}
	
	public void updateSig(Signature s){
		F1.updateSig(s);
		F2.updateSig(s);
		F3.updateSig(s);
	}
	
	public void setCvals(String paramname, double val) {
		F1.setCvals(paramname, val);
		F2.setCvals(paramname, val);
		F3.setCvals(paramname, val);
	}
}
