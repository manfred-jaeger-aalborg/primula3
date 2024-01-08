/*
* ProbForm.java 
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
import RBNinference.*;
import RBNutilities.rbnutilities;
import RBNgui.Primula;
import RBNLearning.Profiler;

public abstract class ProbForm
{
    
    
	public static final int PFATOM = 0;
	public static final int PFBOOL = 1;
	public static final int PFCOMBFUNC = 2;
	public static final int PFCONVCOMB = 3;
	public static final int PFCONST = 4;
	
	public static final int RETURN_ARRAY=0;
	public static final int RETURN_SPARSE=1;
	
	/* An atomic representation of this probform that can be used to form keys etc. */ 
	public ProbFormAtom alias; 
	
    public ProbForm()
    {alias = null;}
        

    public abstract String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias);
    

  
    public void setAlias(ProbFormAtom pfa) {
    	alias = pfa;
    }
    
    public String getAlias() {
    	if (alias != null)
    		return alias.asString(Primula.CLASSICSYNTAX,0,null,false,false);
    	else
    		return null;
    }

    /** Simplify ProbForm by substituting  values of instantiated R-atoms
     * and evaluating subformulas no longer dependent on any uninstantiated
     * R-atom.
     */
    public abstract ProbForm conditionEvidence(RelStruc A, OneStrucData inst)
    throws RBNCompatibilityException;


    /** Checks whether this prob.form depends on the unknown parameter 'variable'
     * when prob.form is evaluated over input structure A and relative to 
     * instantiation (data) data. Only for ground probforms!
     * 
     * For argument variable = "unknown_atom" the method returns true if
     * this ProbForm depends on a ground atom not instantiated in data
     */
    public abstract boolean dependsOn(String variable, RelStruc A, OneStrucData data)
	throws RBNCompatibilityException;

//    /** Evaluate this ProbForm for input structure A, instantiation (data/evidence) inst, under the
//     * substitution tuple for vars. Returns Double.NaN if the value of probform is not defined
//     * because 
//     * a) it depends on a probabilistic atom not instantiated in inst.
//     * b) useCurrentCvals=false and probform depends on an unknown parameter.
//     * c) useCurrentPvals=false, and probform depends on a numeric input relation atom that is contained in numrelparameters
//     * d) useCurrentMvals=false, and probform depends on a boolean probabilistic relation atom that is contained in mapatoms
//     * 
//     * If useCurrentCvals=true, then evaluation at ProbFormConstant's is done with regard to
//     * their cval field, even when their paramname != "".
//     * If useCurrentPvals=true, then evaluation for numeric input relations is performed 
//     * according to their value given in A
//     * 
//     * If the Hashtable evalutated is not null, then first the value for this ProbForm
//     * is looked up using the GradientGraph.makeKey method for key generation.
//     */
//    public abstract double evaluate(RelStruc A, 
//    		OneStrucData inst, 
//    		String[] vars, 
//    		int[] tuple, 
//    		boolean useCurrentCvals, 
//    		String[] numrelparameters,
//    		boolean useCurrentPvals,
//    		GroundAtomList mapatoms,
//    		boolean useCurrentMvals,
//    		Hashtable<String,Double> evaluated)
//    throws RBNCompatibilityException;  
    
    /** Evaluate this ProbForm for input structure A, instantiation (data/evidence) inst, under the
     * substitution tuple for vars. Returns Double.NaN if the value of probform is not defined
     * because 
     * a) it depends on a probabilistic atom not instantiated in inst.
     * b) useCurrentCvals=false and probform depends on an unknown parameter.
     * c) useCurrentPvals=false, and probform depends on a numeric input relation atom that is contained in numrelparameters
     * d) useCurrentMvals=false, and probform depends on a boolean probabilistic relation atom that is contained in mapatoms
     * 
     * Throws exception if formula under the given substitution is not ground
     * 
     * If useCurrentCvals=true, then evaluation at ProbFormConstant's is done with regard to
     * their cval field, even when their paramname != "".
     * If useCurrentPvals=true, then evaluation for numeric input relations is performed 
     * according to their value given in A
     * 
     * If the Hashtable evalutated is not null, then first the value for this ProbForm
     * is looked up using the GradientGraph.makeKey method for key generation.
     * 
     * Returns an array of Object, where 
     * -the first component is the Double value of this formula
     * -if returntype==this.RETURN_ARRAY, then the second component is an array containing the gradient with components 
     *  according to the order defined in params
     * -if returntype==this.RETURN_SPARSE, then the second component is a Hashtable<String,double> with parameter names as keys,
     *  and partial derivatives as values 
     *   
     * 
     * If valuonly==true, then only the value is computed
     */
    public abstract Object[] evaluate(RelStruc A, 
    		OneStrucData inst, 
    		String[] vars, 
    		int[] tuple, // until here useful
    		boolean useCurrentCvals, 
    		//String[] numrelparameters,
    		boolean useCurrentPvals,
    		GroundAtomList mapatoms,
    		boolean useCurrentMvals,
    		Hashtable<String,Object[]> evaluated,
    		Hashtable<String,Integer> params,
    		int returntype,
    		boolean valonly, // true for gnn
    		Profiler profiler)
    throws RBNCompatibilityException;  
    
    public double evaluate(
    		RelStruc A, 
    		OneStrucData inst
    		)  throws RBNCompatibilityException
    {
    	return (double)this.evaluate(A,inst,new String[0], new int[0], false, false,null,false,null,null,ProbForm.RETURN_ARRAY,true,null)[0];
    }
    
    /** Evaluate this probform over RelStruc A. For ground atoms on which probform
     * depends, a ComplexBNGroundAtomNode is accessible via 
     * atomhasht (using Atom.asString() as hashcode)
     * If this ComplexBNGroundAtomNode is not instantiated, then
     * the sample method of that node has to be called
     *
     */
    public abstract double evalSample(RelStruc A, Hashtable<String,PFNetworkNode> atomhasht, OneStrucData inst, long[] timers)
	throws RBNCompatibilityException;


    /** Returns 0 if this probform evaluates to zero over 
     * structure A and with respect to instantiation instasosd, but
     * irrespective of any instantiation of other
     * probabilistic atoms. When probform contains unknown parameters,
     * then evaluatesTo is computed with regard to the current
     * setting of cval at the parameter ProbFormConstants.
     * Returns 1 if ... evaluates to one ....
     * Returns -1 if neither of the above
     *
     * When usesampleinst = true, then evaluation is not w.r.t.
     * instantiation instasosd, but w.r.t. to sampleinst fields at 
     * PFNetworkNodes which are accessible via atomhasht
     */
    public abstract int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable<String,GroundAtom> atomhasht) 
	throws RBNCompatibilityException;

    public abstract int evaluatesTo(RelStruc A) throws RBNCompatibilityException;


    /** returns the free variables of the formula */
    public abstract String[] freevars();
    

    /** returns the vector of (ground!) Atoms on which the
     * evaluation of the probform depends
     */
    public abstract Vector<GroundAtom> makeParentVec(RelStruc A)
	throws RBNCompatibilityException;

    /** same as previous but with respect to the given
     * truth values in the Instantiation argument
     */
    public abstract Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
	throws RBNCompatibilityException;

    /** returns true if ProbForm only contains
     * multilinear combination functions
     */
    public abstract boolean multlinOnly();
 
    /** Returns all the parameters that this ProbForm depends on 
     * Macro calls are not expanded!
     * 
     * Example: 
     * @macro(v) = WIF $p THEN 0.3 ELSE blue(v)
     * 
     * then parameters() of WIF @macro(w) THEN $s ELSE $t is [$s,$t] (not including $p).
     * */
    public abstract String[] parameters();

    /**
     * Returns a ProbForm in which the dependence on A
     * is already pre-evaluated (substitution lists in 
     * combination functions, and values of ProbFormSFormula)
     */  
    public abstract ProbForm sEval(RelStruc A) throws RBNCompatibilityException;


    /** returns the formula obtained by substituting
     * args for the vars in the formula.
     * Produces an error if vars are not among 
     * the free variables of the formula
     */
    public abstract ProbForm substitute(String[] vars, int[] args); 
    
    public abstract ProbForm substitute(String[] vars, String[] args);
    
    /** Sets all occurrences of parameters appearing in params 
     * to their corresponding value in values. params and values
     * must be arrays of the same length
     * @param params
     * @param values
     */
    //public abstract void setParameters(String[] params,  double[] values);
    
    /** Transforms this PF into a normal form with regard to the names
     * of variables.
     * 
     * Example: (r(v):t(u,v),s(v)) and (r(x):t(z,x),s(x)) are both 
     * turned into (r(v1):t(v2,v1),s(v1))
     */
    public ProbForm normalForm(){
    	String[] fvs = this.freevars();
    	String[] newvars = new String[fvs.length];
    	for (int j=0;j<fvs.length;j++){
    		newvars[j]="Z" + j;
    	}
    	return this.substitute(fvs,newvars);
    }
 
    public static int typeOfPf(ProbForm pf){
    	if (pf instanceof ProbFormAtom)
    		return ProbForm.PFATOM;
    	if (pf instanceof ProbFormBool)
    		return ProbForm.PFBOOL;
    	if (pf instanceof ProbFormCombFunc)
    		return ProbForm.PFCOMBFUNC;
    	if (pf instanceof ProbFormConvComb)
    		return ProbForm.PFCONVCOMB;
    	if (pf instanceof ProbFormConstant)
    		return ProbForm.PFCONST;
    	
    	return 0;
    }
    
    
    /**
     * See updateSig in RBN class
     * @param s
     */
    public abstract void updateSig(Signature s);
    
    /** 
     * Sets the cval field of parameter with name=paramname to the
     * value val.
     * @param paramname
     * @param val
     */
    public abstract void setCvals(String paramname, double val);
    
    public String makeKey(RelStruc A) {
    	return this.asString(Primula.CLASSICSYNTAX, 0, A, false, true);

    }
    
    public String makeKey(String[] vars, int[] args, Boolean nosub) {
    	if (nosub) {
    		if (this.alias != null)
    			return this.alias.getRelation().name();
    		if (this instanceof ProbFormAtom)
    			return ((ProbFormAtom)this).getRelation().name();
    		return this.asString(Primula.CLASSICSYNTAX, 0, null, false, true);
    	}
    	if (this.alias != null) {
    		ProbFormAtom groundalias = this.alias.substitute(vars, args);
    		return groundalias.asString(Primula.CLASSICSYNTAX, 0, null, false, true);
    	}
    	else return this.substitute(vars,args).asString(Primula.CLASSICSYNTAX, 0, null, false, true);
    }
    
    /* Returns set of relations that evaluation of this ProbForm depends on */
    public abstract TreeSet<Rel> parentRels();
    
    /* Returns set of relations that evaluation of this ProbForm depends on, checks in recursion whether sub-formula has already
     * been processed */
    public abstract TreeSet<Rel> parentRels(TreeSet<String> processed);
    
}

