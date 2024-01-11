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

public abstract class ProbForm extends CPModel
{
    
    
	public static final int PFATOM = 0;
	public static final int PFBOOL = 1;
	public static final int PFCOMBFUNC = 2;
	public static final int PFCONVCOMB = 3;
	public static final int PFCONST = 4;
	
	public static final int RETURN_ARRAY=0;
	public static final int RETURN_SPARSE=1;
	
	
    public ProbForm()
    {alias = null;}  


    /** Returns 0 if this probform evaluates to zero over 
     * structure A and with respect to instantiation inst, but
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



    
//    /** Transforms this PF into a normal form with regard to the names
//     * of variables.
//     * 
//     * Example: (r(v):t(u,v),s(v)) and (r(x):t(z,x),s(x)) are both 
//     * turned into (r(v1):t(v2,v1),s(v1))
//     */
//    public ProbForm normalForm(){
//    	String[] fvs = this.freevars();
//    	String[] newvars = new String[fvs.length];
//    	for (int j=0;j<fvs.length;j++){
//    		newvars[j]="Z" + j;
//    	}
//    	return this.substitute(fvs,newvars);
//    }
 
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
    
    public abstract ProbForm conditionEvidence(RelStruc A, OneStrucData inst)
    	    throws RBNCompatibilityException;
    
}

