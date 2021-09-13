/*
 *ProbFormConstant.java 
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
import RBNutilities.*;



/** A ProbFormConstant represents a numerical constant. The formula can
 * also represent an unknown parameter. In this case, the string paramname 
 * contains the name of the parameter. In all cases, a  numeric value is
 * associated with the ProbFormConstant, so that it is always a valid probability
 * formula that can be evaluated. When the formula represents a parameter, then
 * this numeric value is a current estimate of the parameter value
 * 
 * Constants can be of type 'Bernoulli' and then are constrained to range in [0,1].
 * They can be of type 'General' with unconstrained range
 * 
 * String identifiers of unknown Bernoulli constants start with '#'. Identifiers
 * of general constants start with '$'
 */
public class ProbFormConstant extends ProbForm
{
	public static int CONSTANT_BERNOULLI =0;
	public static int CONSTANT_GENERAL =1;
	
	public int ctype; // one of Bernoulli or General
	/**
	 * @uml.property  name="cval"
	 */
	public double cval;
	/**
	 * @uml.property  name="paramname"
	 */
	private String paramname;

	public ProbFormConstant(){
//		SSymbs = new Rel[0];
//		RSymbs = new Rel[0];
		ctype=CONSTANT_BERNOULLI; 
		cval = 0;
		paramname = "";
	}

	public ProbFormConstant(double v){
//		SSymbs = new Rel[0];
//		RSymbs = new Rel[0];
		cval = v;
		paramname = "";
		if (v>=0 && v<=1) {
			ctype=CONSTANT_BERNOULLI;
		}
		else
			ctype=CONSTANT_GENERAL;
	}


	public ProbFormConstant(String pn){
		
		paramname = pn;
		if (pn.charAt(0)=='#') {
			cval = 0.5;
			ctype=CONSTANT_BERNOULLI;
		}
		else if (pn.charAt(0)=='$') {
			cval = 0.0;
			ctype=CONSTANT_GENERAL;
		}
		else
			System.out.println("Illegal identifier for rbn parameter: "+pn);
	}

	public String[] freevars()
	{
		String[] result = new String[0];
		return result;
	}

	public boolean multlinOnly(){
		return true;
	}

	public ProbForm substitute(String[] vars, int[] args)
	{
		ProbFormConstant result;
		if (paramname == "")
			result = new ProbFormConstant(cval);
		else {
			result = new ProbFormConstant(paramname);
			result.setCvals(paramname, cval);
		}
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	public ProbForm substitute(String[] vars, String[] args)
	{
		if (paramname == "")
			return new ProbFormConstant(cval);
		else {
			ProbFormConstant result = new ProbFormConstant(paramname);
			result.setCvals(paramname, cval);
			if (this.alias != null)
				result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
			return result;
		}
	}

	public ProbForm conditionEvidence(RelStruc A, OneStrucData inst)
	{
		return this;
	}


	public  Vector makeParentVec(RelStruc A){
		return new Vector();
	}

	public  Vector makeParentVec(RelStruc A, OneStrucData inst){
		return new Vector();
	}


	public ProbForm conditionEvidence(OneStrucData inst){
		return new ProbFormConstant(cval);
	}


	public boolean dependsOn(String variable, RelStruc A, OneStrucData data){
		//System.out.println("dependsOn for " + this.asString());
		if ( paramname != "" && paramname.equals(variable)){
			//  System.out.println("return true");
			return true;}
		else 
			return false;
	}

	public double evaluate(RelStruc A, OneStrucData inst, String[] vars, int[] tuple, 
			boolean useCurrentCvals, 
    		String[] parameters,
    		boolean useCurrentPvals,
    		GroundAtomList mapatoms,
    		boolean useCurrentMvals,
    		Hashtable<String,Double> evaluated)
	{
		if (paramname != "" && !useCurrentCvals && rbnutilities.arrayContains(parameters, paramname))
			return Double.NaN;
		else 
			return cval;
	}


	public  double evalSample(RelStruc A,Hashtable atomhasht,OneStrucData inst, long[] timers){
		return cval;
	}


	public  int evaluatesTo(RelStruc A){
		if (cval == 0) return 0;
		if (cval == 1) return 1;
		else
			return -1;
	}

	public  int evaluatesTo(RelStruc A,OneStrucData inst, boolean usesampleinst, Hashtable atomhasht){
		return evaluatesTo(A);
	}

	public ProbForm sEval(RelStruc A){
		return this;
	}


	public String getParamName(){
		return paramname;
	}

	/**
	 * @return
	 * @uml.property  name="cval"
	 */
	public double getCval(){
		return cval;
	}

	public String[] parameters(){
		if (paramname=="")
			return new String[0];
		else{
			String[] result = new String[1];
			result[0]=paramname;
			return result;
		}
	}



	public String asString(int syntax, int depth,RelStruc A, boolean paramsAsValue, boolean usealias)
	{
		if (usealias && this.getAlias() != null)
			return this.getAlias();
		String result = new String();
		if (paramname == "" || paramsAsValue)
			result = String.valueOf(cval);
		else 
			result = paramname;
		return result;
	}

//	public String asString(int syntax, RelStruc A){
//		return this.asString(syntax);
//	}



	public void setRandom(){
		if (paramname != "") 
			cval = Math.random();
	}
	
	public void updateSig(Signature s){
	}
	
	public void setCvals(String paramname, double val) {
		if (paramname==this.paramname)
			cval=val;
	}
}
