/*
 * CConstrAtom.java
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
import RBNutilities.*;


public class CConstrAtom extends CConstr{

    public Rel relation;
    public String[] arguments;
    
    /** Creates new CConstrAtom */
    public CConstrAtom(Rel r, String[] args) {
        SSymbs = new Rel[1];
        SSymbs[0] = r;
        relation = r;
        if (args.length == r.arity){
            arguments = args;}
        else {
            throw new IllegalArgumentException("Error in constructing Indicator-Formula: arguments do not match arity of " + r.name);
        }
    }
    
    public String[] freevars()
    {
        return rbnutilities.NonIntOnly(arguments);
    }
    
    public CConstr substitute(String[] vars, int[] args)
    {
        CConstrAtom result;
        String[] resargs = new String[relation.arity];
        String nextarg;
        for (int i = 0; i<relation.arity; i++)
	    {
		nextarg = arguments[i];
		for (int j = 0; j<vars.length; j++)
		    {
			if (nextarg.equals(vars[j])) 
			    {
				nextarg = String.valueOf(args[j]);
			    }
                
		    }
		resargs[i] = nextarg;
	    }
        result= new CConstrAtom(relation,resargs);
        return result;
    }
    
    public CConstr substitute(String[] vars, String[] args)
    {
        CConstrAtom result;
        String[] resargs = new String[relation.arity];
        String nextarg;
        for (int i = 0; i<relation.arity; i++)
	    {
		nextarg = arguments[i];
		for (int j = 0; j<vars.length; j++)
		    {
			if (nextarg.equals(vars[j])) nextarg = args[j];
		    }
		resargs[i] = nextarg;
	    }
        result= new CConstrAtom(relation,resargs);
        return result;
    }
    
    
    public  String[] parameters(String[] parameternumrels){
    	String[] result;
    	if (rbnutilities.arrayContains(parameternumrels,relation.name())){
    		result = new String[1];
    		result[0]=this.asString();
    	}
    	else 
    		result = new String[0];
    	return result;
    }
     
    public String asString()
    {
        String result;
        result = relation.printname() + "(" + rbnutilities.arrayToString(arguments) + ")";
        return result;
    }

    public String asString(RelStruc A)
    {
    	if (A==null)
    		return this.asString();
    	else{
    		String result;
    		result = relation.printname() + "(";
    		for (int i = 0; i<arguments.length-1; i++)
    			if (rbnutilities.IsInteger(arguments[i]))
    				result = result.concat(A.nameAt(Integer.parseInt(arguments[i])) + ",");
    			else result = result.concat(arguments[i] + ",");
    		if (arguments.length>0){
    			if (rbnutilities.IsInteger(arguments[arguments.length-1]))
    				result = result.concat(A.nameAt(Integer.parseInt(arguments[arguments.length-1])));
    			else result = result.concat(arguments[arguments.length-1]);
    		}
    		result = result.concat(")");
    		return result;
    	}
    }

//     /** Example: this CConstrAtom is r(2,x,y,x), tup is (2,3,4,3), vars is 
//      * (y,x). Then method will insert the tuple (4,3) into ts.
//      * 
//      * If this CConstrAtom is r(2,x,y,x), tup is (2,3,4,3), vars is 
//      * (y,x,z), then method will insert all tuples (4,3,i) with i=0,...,d-1
//      * into ts.
//      * 
//      * If this CConstrAtom is r(2,x,y,x), tup is (1,3,4,3), vars is 
//      * (y,x,z), then method will not insert any tuples into ts.
//      */
//     public void  allSatisfyingTuples(int[] tup, String[] vars, TreeSet ts, int d){
// // 	System.out.println("allSatisfyingTuples((" + rbnutilities.arrayToString(tup) + ")" + 
// // 			   "," + "(" + rbnutilities.arrayToString(vars) +"))" + " in " +
// // 			   this.asString());
// 	if (tup.length != arguments.length)
// 	    throw new IllegalArgumentException("Tuple of wrong length!");
// 	/* Test whether the integer components in this.arguments match with
// 	 * tup */
// 	if (!rbnutilities.integerMatch(arguments,tup))
// 	    return;
// 	/* Construct an array indexInVars of size tup.length that for each variable 
// 	 * argument of this.arguments gives the index of this variable
// 	 * in vars. Example: this.arguments = (1,x,y,x), vars = (y,x)
// 	 * then indexInVars = (-1,2,1,2) (-1 represents a vacuous entry).
// 	 *
// 	 * Throws IllegalArgumentException if not all variables in this.arguments
// 	 * appear in vars
// 	 */
// 	int[] indexInVars = new int[tup.length];
// 	int nextindex;
// 	for (int i = 0; i<indexInVars.length; i++)
// 	    indexInVars[i]=-1;
// 	for (int i = 0; i<arguments.length-1; i++){
// 	    if (!rbnutilities.IsInteger(arguments[i])){
// 		nextindex = rbnutilities.indexInArray(vars,arguments[i]);
// 		if (nextindex == -1)
// 		    throw new IllegalArgumentException();
// 		else
// 		    indexInVars[i]=nextindex;
// 	    }		
// 	}
// 	//System.out.println("    indexInVars: (" + rbnutilities.arrayToString(indexInVars) + ")");
// 	/* Now construct the 'pattern' of all tuples to be inserted into
// 	 * ts. If this.arguments = (1,x,y,x), vars = (y,x), and tup=(1,3,4,3)
// 	 * then pattern = (4,3); if this.arguments = (1,x,y,x), vars = (y,x), and tup=(1,3,4,5),
// 	 * then the construction of pattern fails, and the method terminates.
// 	 * If this.arguments = (1,x,y,x), vars = (y,x,z), and tup=(1,3,4,3), 
// 	 * then pattern=(4,3,-1).
// 	 */
// 	int[] pattern = new int[vars.length];
// 	int wildCardCount = pattern.length;
// 	for (int i = 0; i<pattern.length; i++)
// 	    pattern[i]=-1;
// 	for (int i = 0; i<tup.length-1; i++){
// 	    if (indexInVars[i] != -1){
// 		if (pattern[indexInVars[i]]!= -1 && pattern[indexInVars[i]]!= tup[i])
// 		    return;
// 		else {
// 		    pattern[indexInVars[i]]=tup[i];
// 		    wildCardCount--;
// 		}
// 	    }
// 	}
// 	//System.out.println("    pattern (" + rbnutilities.arrayToString(pattern) + ")");
// 	/* Fill in all possible substitutions for the wildcards (-1 entries) 
// 	 * in pattern, and add to ts
// 	 */
// 	// int[] wildCardPositions = new int[wildCardCount];
// 	// 	int nextpos = 0;
// 	// 	for (int i = 0; i<pattern.length; i++)
// 	// 	    if (pattern[i]==-1){
// 	// 		wildCardPostions[nextpos]=i;
// 	// 		nextpos++;
// 	// 	    }
// 	int[] nextsmalltuple;
// 	int[] nextbigtuple;
// 	for (int i = 0; i<MyMathOps.intPow(d,wildCardCount); i++){
// 	    nextsmalltuple = rbnutilities.indexToTuple(i,wildCardCount,d);
// 	    nextbigtuple = new int[pattern.length];
// 	    nextindex=0;
// 	    for (int j = 0; j< nextbigtuple.length; j++){
// 		if (pattern[j]==-1){
// 		    nextbigtuple[j]=nextsmalltuple[nextindex];
// 		    nextindex++;
// 		}
// 		else
// 		    nextbigtuple[j]=pattern[j];
// 	    }
// 	    //System.out.println("     add (" + rbnutilities.arrayToString(nextbigtuple) + ")");
// 	    ts.add(nextbigtuple);
// 	}
	
//     }
}
