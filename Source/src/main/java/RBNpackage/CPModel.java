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
import RBNgui.Primula;
import RBNLearning.Profiler;

/**
 * Abstract class representing conditional probability models. Implementing 
 * classes: ProbForm (for Bernoulli models) and CatModel (for categorical variables).
 * @author jaeger
 *
 */
public abstract class CPModel
{
    
	
	/* An atomic representation of this probform that can be used to form keys etc. */ 
	public ProbFormAtom alias; 
	

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

    /** returns true if Model only contains
     * multilinear combination functions
     */
    public abstract boolean multlinOnly();
    
    /** Simplify Model by substituting  values of instantiated R-atoms
     * and evaluating subformulas no longer dependent on any uninstantiated
     * R-atom.
     */
    public abstract CPModel conditionEvidence(RelStruc A, OneStrucData inst)
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

    
    /** Evaluate this CPModel for input structure A, instantiation (data/evidence) inst, under the
     * substitution tuple for vars. Returns Double.NaN if the value is not defined
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
     * -the first component is the Double value of this formula (for ProbForm), or the array of probabilities (for CatModel).
     * -if returntype==this.RETURN_ARRAY, then the second component is an array containing the gradient with components 
     *  according to the order defined in params
     * -if returntype==this.RETURN_SPARSE, then the second component is a Hashtable<String,double> with parameter names as keys,
     *  and partial derivatives as values 
     *   
     * gradindx is the index of the categorical value for which the gradient is calculated. This is only relevant when
     * valonly = false. For Bernoulli models (i.e., ProbForm instances of this class) by default gradinx=1 (=true), i.e., 
     * the gradient for the probability of 'true' is computed.
     * 
     * If valonly==true, then only the value (no gradient) is computed
     */
    public abstract Object[] evaluate(
			RelStruc A,
    		OneStrucData inst, 
    		String[] vars,
    		int[] tuple,
    		int gradindx,
    		boolean useCurrentCvals, 
    		//String[] numrelparameters,
    		boolean useCurrentPvals,
    		Hashtable<Rel,GroundAtomList> mapatoms,
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
    	return (double)this.evaluate(A,inst,new String[0], new int[0], 0, false, false,null,false,null,null,ProbForm.RETURN_ARRAY,true,null)[0];
    }
    
    /** Evaluate this probform over RelStruc A. For ground atoms on which probform
     * depends, a ComplexBNGroundAtomNode is accessible via 
     * atomhasht (using Atom.asString() as hashcode)
     * If this ComplexBNGroundAtomNode is not instantiated, then
     * the sample method of that node has to be called
     *
     */
    public abstract Double evalSample(RelStruc A, 
    		Hashtable<String,PFNetworkNode> atomhasht, 
    		OneStrucData inst, 
    		Hashtable<String,Double> evaluated,
    		long[] timers)
	throws RBNCompatibilityException;

    /** returns the free variables of the model */
    public abstract String[] freevars();
    
//    /** returns the vector of (ground!) Atoms on which the
//     * evaluation of the model depends
//     */
//    public abstract Vector<GroundAtom> makeParentVec(RelStruc A, TreeSet<String> macrosdone)
//	throws RBNCompatibilityException;

    /** same as previous but with respect to the given
     * truth values in the Instantiation argument
     */
    public abstract Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
	throws RBNCompatibilityException;

 
    /** Returns all the parameters that this Model depends on 
     * Macro calls are not expanded!
     * 
     * Example: 
     * @macro(v) = WIF $p THEN 0.3 ELSE blue(v)
     * 
     * then parameters() of 'WIF @macro(w) THEN $s ELSE $t' is [$s,$t] (not including $p).
     * */
    public abstract String[] parameters();

    /**
     * Returns a Model in which the dependence on A
     * is already pre-evaluated (substitution lists in 
     * combination functions, and values of CConstr's)
     */  
    public abstract CPModel sEval(RelStruc A) throws RBNCompatibilityException;


    /** returns the model obtained by substituting
     * args for the vars.
     * Produces an error if vars are not among 
     * the free variables of the model
     */
    public abstract CPModel substitute(String[] vars, int[] args); 
    
    public abstract CPModel substitute(String[] vars, String[] args);
    
    
//    /**
//     * See updateSig in RBN class
//     * @param s
//     */
//    public abstract void updateSig(Signature s);
    
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
    
    public String makeKey(String[] vars, int[] args, Boolean nosub){
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
    
    
    /* Returns set of relations that evaluation of this Model depends on */
    public abstract TreeSet<Rel> parentRels();
    
    /* Returns set of relations that evaluation of this Model depends on, checks in recursion whether sub-formula has already
     * been processed */
    public abstract TreeSet<Rel> parentRels(TreeSet<String> processed);
    
    public abstract int numvals();
    
}

