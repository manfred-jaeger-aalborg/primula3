/*
 * rbnutilities.java
 * 
 * Copyright (C) 2005 Aalborg University
 *
 * contact:
 * jaeger@cs.aau.dk   www.cs.aau.dk/~jaeger/Primula.html
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

package RBNutilities;

import java.lang.*;
import java.util.*;

import PyManager.TorchInputSpecs;
import RBNgui.Primula;
import RBNpackage.*;
import RBNExceptions.*;
import mymath.*;

public class rbnutilities extends java.lang.Object
{
    public static boolean IsInteger(String argentry)
        /* returns true of argentry is a string
         * representing an integer. Needed in freevars()
         */
    {
    	if (argentry.length()==0) return false;
    	for (int i = 0; i < argentry.length(); i++)
    	{
    		if (!Character.isDigit(argentry.charAt(i)))
    		{
    			return false;
    		}
    	}
    	return true;
    }

    public static boolean IsInteger(String[] args){
    	boolean result = true;
    	for (int i=0;i<args.length;i++)
    		if (!IsInteger(args[i]))
    			result = false;
    	return result;
    }
    
    public static int IntPow(int k, int l)
        // returns k to the power of l
    {
	int result =1;
	for (int i =0 ; i<l; i++) result = result*k;
	return result;
    }
        
        

    
    
    public static boolean arrayEquals(int[] arr1, int[] arr2){
        boolean result = true;
        if (arr1.length != arr2.length)
            result = false;
        else{
            for (int i=0;i<arr1.length;i++)
                if (arr1[i] != arr2[i]) result = false;
        }
        return result;
    }
    
    
    public static boolean inArray(Rel[] relarr, Rel thisrel)
	/* checks wheter thisrel appears in relarr */
    {
        
        boolean result = false;
        for (int i = 0; i<relarr.length; i++)
            if (relarr[i].equals(thisrel))  result = true;
        return result;
    }
    
    public static void arrayShiftArgs(int[] array, int a){
        // replaces all components b>a in array with b-1
        for (int i=0;i<array.length;i++){
            if (array[i]>a) array[i]--;
        }
    }
    
    public static  boolean inArray(int[] intarr, int a){
        boolean result = false;
        for (int i = 0; i<intarr.length; i++)
            if (intarr[i] == a)  result = true;
        return result;
    }
    

    public static String[] arraymerge(String[] stringarg1, String[]stringarg2)
	/* takes two arrays stringarg1 and stringarg2 
	 * and returns an array that contains 
	 * their elements without repetitions
	 */
    {
        String[] result;
        String[] prelimarray = new String[stringarg1.length+stringarg2.length];
        int counter = 0;
        int i,j;
        boolean isnew;
        
        for (i=0; i<stringarg1.length; i++)
	    {
		isnew = true;
		j = 0; 
		while (j<counter && isnew) 
		    {
			if (prelimarray[j].equals(stringarg1[i])) isnew = false;
			j++;
		    }
		if (isnew) 
		    {
			prelimarray[counter] = stringarg1[i];
			counter++;
		    }
	    }
        for (i=0; i<stringarg2.length; i++)
	    {
		isnew = true;
		j = 0; 
		while (j<counter && isnew) 
		    {
			if (prelimarray[j].equals(stringarg2[i])) isnew = false;
			j++;
		    }
		if (isnew) 
		    {
			prelimarray[counter] = stringarg2[i];
			counter++;
		    }
	    }
        
        result = new String[counter];
	for (i=0; i<counter; i++)
	    {
		result[i] = prelimarray[i];
	    }
        return result;
    }
    
    
    /* Returns 0 if arr1 and arr2 are equal
     *         -1 if arr1 and arr2 are of equal length and arr1 > arr2 in lexical order
     *          1 if arr1 and arr2 are of equal length and arr1 < arr2 in lexical order
     *          2 else
     */
    public static int arrayCompare(int[] arr1, int[] arr2){

	int result = 2;
	boolean done = false;
	
	if (arr1.length != arr2.length){
	    done = true;
	}
	int i =0;    
	while (!done){
	    if (arr1[i]>arr2[i]){
		result = -1;
		done = true;
	    }
	    if (arr1[i]<arr2[i]){
		result = 1;
		done = true;
	    }
	    if (i == arr1.length-1 && arr1[i]==arr2[i]){
		result = 0;
		done = true;
	    }
	    if (!done)
		i++;
	}
	return result;
    }

    public static boolean arrayContains(int[] arr, int ii){
    	for (int i=0;i<arr.length;i++){
    		if (arr[i]==ii) return true;
    	}
    	return false;
    }

    public static boolean arrayContains(String[] arr, String ii){
    	for (int i=0;i<arr.length;i++){
    		if (arr[i].equals(ii)) return true;
    	}
    	return false;
    }
    
    public static int arrayContainsAt(String[] arr, String ii){
    	int result = -1;
    	for (int i=0;i<arr.length;i++){
    		if (arr[i].equals(ii)) result=i;
    	}
    	return result;
    }

    public static double[] arrayAdd(double[] arg1, double[] arg2){
    	if (arg1.length != arg2.length)
    		System.out.println("Cannot add arrays of unequal length!");
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=arg1[i]+arg2[i];
    	return result;
    }
    
    
    public static int[] arrayAdd(int[] arg1, int[] arg2){
    	if (arg1.length != arg2.length)
    		System.out.println("Cannot add arrays of unequal length!");
    	int[] result = new int[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=arg1[i]+arg2[i];
    	return result;
    }
    
    public static double[] arrayAddConst(double[] arg1, double c){
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=arg1[i]+c;
    	return result;
    }
    
    public static double[] arraySubtract(double[] arg1, double[] arg2){
    	if (arg1.length != arg2.length)
    		System.out.println("Cannot subtract arrays of unequal length!");
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=arg1[i]-arg2[i];
    	return result;
    }
    
    public static double arrayDotProduct(double[] arg1, double[] arg2){
    	if (arg1.length != arg2.length)
    		System.out.println("Cannot multiply arrays of unequal length!");
    	double result = 0;
    	for (int i=0;i<arg1.length;i++)
    		result= result + arg1[i]*arg2[i];
    	return result;
    }
    
    /* component-wise division */
    public static double[] arrayCompDivide(double[] arg1, double[] arg2){
    	if (arg1.length != arg2.length)
    		System.out.println("Cannot divide arrays of unequal length!");
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=arg1[i]/arg2[i];
    	return result;
    }
    
    /* component-wise multiplication */
    public static double[] arrayCompMultiply(double[] arg1, double[] arg2){
    	if (arg1.length != arg2.length)
    		System.out.println("Cannot multiply arrays of unequal length!");
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=arg1[i]*arg2[i];
    	return result;
    }
    
    /* component-wise power */
    public static double[] arrayCompPow(double[] arg, double po){
    	double[] result = new double[arg.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=Math.pow(arg[i],po);
    	return result;
    }
    
    
    /* component-wise max */
    public static double[] arrayCompMax(double[] arg1, double[] arg2){
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=Math.max(arg1[i],arg2[i]);
    	return result;
    }
    
    /* component-wise min */
    public static double[] arrayCompMin(double[] arg1, double[] arg2){
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=Math.min(arg1[i],arg2[i]);
    	return result;
    }
    
    /* component-wise square root */
    public static double[] arraySQRT(double[] arg1){
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=Math.sqrt(arg1[i]);
    	return result;
    }
    
    /* component-wise power */
    public static double[] arrayPow(double[] arg1, double p){
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=Math.pow(arg1[i],p);
    	return result;
    }
    
    
    public static double[] arrayScalMult(double[] arg1, double s){
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=s*arg1[i];
    	return result;
    }
    
    public static double[] arrayConvComb(double[] arg1, double[] arg2, double s){
    	double[] result = new double[arg1.length];
    	for (int i=0;i<result.length;i++)
    		result[i]=s*arg1[i]+(1-s)*arg2[i];
    	return result;
    }
    
    public static String[] arrayConcatenate(String[] stringarg1, String[] stringarg2){
    	String[] result = new String[stringarg1.length + stringarg2.length];
    	for (int i=0;i<stringarg1.length;i++)
    		result[i]=stringarg1[i];
    	for (int i=0;i<stringarg2.length;i++)
    		result[stringarg1.length + i]=stringarg2[i];
    	return result;
    }
    
    public static short[] arrayConcatenate(short[] stringarg1, short[] stringarg2){
    	short[] result = new short[stringarg1.length + stringarg2.length];
    	for (int i=0;i<stringarg1.length;i++)
    		result[i]=stringarg1[i];
    	for (int i=0;i<stringarg2.length;i++)
    		result[stringarg1.length + i]=stringarg2[i];
    	return result;
    }
    
    
    public static String[] arraysubstraction(String[] stringarg1, String[]stringarg2)
	/* takes two arrays stringarg1 and stringarg2 
	 * and returns an array that contains 
	 * the elements of stringarg1 that do not 
	 * occur in stringarg2. In the result the elements
	 * occur in the order of their first appearance 
	 * in stringarg1 
	 */
    {
        //System.out.println("Entered arraysubstraction with arguments "+ rbnutilities.arrayToString(stringarg1) + " and "+ rbnutilities.arrayToString(stringarg2)); 
        String[] result;
        String[] prelimarray = new String[stringarg1.length];
        int counter = 0;
        int i,j;
        boolean insert;
        
        for (i=0; i<stringarg1.length; i++)
	    {
		insert = true;
		j = 0; 
		while (j<stringarg2.length && insert) 
		    {
			if (stringarg1[i].equals(stringarg2[j])) insert = false;
			j++;
		    }
		// if string1[i] does not appear in string2, also check
		// whether it already is in  prelimarray
		if (insert)
		    {
			while (j<counter && insert)
			    {
				if (stringarg1[i].equals(prelimarray[j])) insert = false;
				j++;
			    }
		    }
		if (insert)
		    {
			prelimarray[counter]=stringarg1[i];
			counter++;
		    }
	    }
       
        result = new String[counter];
        for (i=0; i<counter; i++)
            result[i] = prelimarray[i];
        //System.out.println("Returned "+ rbnutilities.arrayToString(result) ); 
      
        return result;
    }
    
    public static Rel[] arraysubstraction(Rel[] relarg1, Rel[]relarg2)
	/* takes two arrays relarg1 and relarg2 
	 * and returns an array that contains 
	 * the elements of relarg1 that do not 
	 * occur in relarg2. In the result the elements
	 * occur in the order of their first appearance 
	 * in relarg1 
	 */
    {
        //System.out.println("Entered arraysubstraction with arguments "+ rbnutilities.arrayToString(relarg1) + " and "+ rbnutilities.arrayToString(relarg2)); 
        Rel[] result;
        Rel[] prelimarray = new Rel[relarg1.length];
        int counter = 0;
        int i,j;
        boolean insert;
        
        for (i=0; i<relarg1.length; i++)
	    {
		insert = true;
		j = 0; 
		while (j<relarg2.length && insert) 
		    {
			if (relarg1[i].equals(relarg2[j])) insert = false;
			j++;
		    }
		// if string1[i] does not appear in string2, also check
		// whether it already is in  prelimarray
		if (insert)
		    {
			while (j<counter && insert)
			    {
				if (relarg1[i].equals(prelimarray[j])) insert = false;
				j++;
			    }
		    }
		if (insert)
		    {
			prelimarray[counter]=relarg1[i];
			counter++;
		    }
	    }
       
        result = new Rel[counter];
        for (i=0; i<counter; i++)
            result[i] = prelimarray[i];
        //System.out.println("Returned "+ rbnutilities.arrayToString(result) ); 
      
        return result;
    }


    public static NumRel[] arraymerge(NumRel[] relarg1, NumRel[] relarg2)
	/* takes two arrays relarg1 and relarg2 
	 * and returns an array that contains 
	 * their elements without repetitions
	 */
    {
    	NumRel[] result;
    	NumRel[] prelimarray = new NumRel[relarg1.length+relarg2.length];
        int counter = 0;
        int i,j;
        boolean isnew;
        
        for (i=0; i<relarg1.length; i++)
	    {
		isnew = true;
		j = 0; 
		while (j<counter && isnew) 
		    {
			if (prelimarray[j].equals(relarg1[i])) isnew = false;
			j++;
		    }
		if (isnew) 
		    {
			prelimarray[counter] = relarg1[i];
			counter++;
		    }
	    }
        for (i=0; i<relarg2.length; i++)
	    {
		isnew = true;
		j = 0; 
		while (j<counter && isnew) 
		    {
			if (prelimarray[j].equals(relarg2[i])) isnew = false;
			j++;
		    }
		if (isnew) 
		    {
			prelimarray[counter] = relarg2[i];
			counter++;
		    }
	    }
        
        result = new NumRel[counter];
	for (i=0; i<counter; i++)
	    {
		result[i] = prelimarray[i];
	    }
        return result;
    }
    public static Rel[] arraymerge(Rel[] relarg1, Rel[] relarg2)
	/* takes two arrays relarg1 and relarg2 
	 * and returns an array that contains 
	 * their elements without repetitions
	 */
    {
    	Rel[] result;
    	Rel[] prelimarray = new Rel[relarg1.length+relarg2.length];
        int counter = 0;
        int i,j;
        boolean isnew;
        
        for (i=0; i<relarg1.length; i++)
	    {
		isnew = true;
		j = 0; 
		while (j<counter && isnew) 
		    {
			if (prelimarray[j].equals(relarg1[i])) isnew = false;
			j++;
		    }
		if (isnew) 
		    {
			prelimarray[counter] = relarg1[i];
			counter++;
		    }
	    }
        for (i=0; i<relarg2.length; i++)
	    {
		isnew = true;
		j = 0; 
		while (j<counter && isnew) 
		    {
			if (prelimarray[j].equals(relarg2[i])) isnew = false;
			j++;
		    }
		if (isnew) 
		    {
			prelimarray[counter] = relarg2[i];
			counter++;
		    }
	    }
        
        result = new Rel[counter];
	for (i=0; i<counter; i++)
	    {
		result[i] = prelimarray[i];
	    }
        return result;
    }


    public static String arrayToString (String arr[])
    {
        String result = "";
        for (int i = 0; i<arr.length-1; i++)
            result = result + arr[i] + ",";
        if (arr.length > 0) 
            result = result + arr[arr.length-1];
        return result;
    }
    
    public static String arrayToString (String arr[],int first, int last)
    {
        String result = "";
        for (int i = first; (i<last && i<arr.length-1) ; i++)
            result = result + arr[i] + ",";
            result = result + arr[last];
        return result;
    }

    public static String arrayToString (int arr[])
    {
        String result = "";
        for (int i = 0; i<arr.length-1; i++)
            result = result + arr[i] + ",";
        if (arr.length > 0) 
            result = result + arr[arr.length-1];
        return result;
    }
    
    public static String arrayToString (short arr[])
    {
        String result = "";
        for (int i = 0; i<arr.length-1; i++)
            result = result + arr[i] + ",";
        if (arr.length > 0) 
            result = result + arr[arr.length-1];
        return result;
    }
    
    /** prefixes every integer in the tuple with 'prefix' 
     */
    public static String arrayToString (int arr[], String prefix)
    {
        String result = "";
        for (int i = 0; i<arr.length-1; i++)
            result = result + prefix + arr[i] + ",";
        if (arr.length > 0) 
            result = result + prefix + arr[arr.length-1];
        return result;
    }
    
    public static String arrayToString (double arr[])
    {
        String result = "";
        for (int i = 0; i<arr.length-1; i++)
            result = result + arr[i] + ",";
        if (arr.length > 0) 
            result = result + arr[arr.length-1];
        return result;
    }

    
    public static String arrayToString (double arr[],String separator)
    {
        String result = "";
        for (int i = 0; i<arr.length-1; i++)
            result = result + arr[i] + separator;
        if (arr.length > 0) 
            result = result + arr[arr.length-1];
        return result;
    }

    public static String arrayToString (double arr[],int first, int last)
    {
        String result = "";
        for (int i = first; (i<last && i<arr.length-1) ; i++)
            result = result + arr[i] + ",";
            result = result + arr[last];
        return result;
    }

    
    
    public static String arrayToString (Rel arr[])
    {
        String result = "";
        for (int i = 0; i<arr.length-1; i++)
            result = result + arr[i].name+"/"+arr[i].arity+",";
        if (arr.length > 0) 
            result = result + arr[arr.length-1].name+"/"+arr[arr.length-1].arity;
        return result;
    }
    
//    public static int[][] cartesProd(Vector<int[]> factors){
//    	/** factors is vector of int[]. Returns the
//    	 * cartesian product of int[] arrays. Example:
//    	 * factors = {[0,4,2],[6],[9,3]}
//    	 * result = [[0,6,9],
//    	 *           [0,6,3],
//    	 *           [4,6,9],
//    	 *           [4,6,3],
//    	 *           [2,6,9],
//    	 *           [2,6,3]]
//    	 **/
//    	//System.out.println("cartesProd for");
//    	if (factors.size()==0){
//    		return new int[1][0];
//    	}
//        //Check if one of the factors is empty. Then return
//    	// empty result
//    	boolean isempty = false;
//    	for (int i=0;i<factors.size();i++){
//    		if (factors.elementAt(i).length ==0)
//    			isempty = true;
//    	}
//    	if (isempty)
//    		return new int[1][0];
//
//    	if (factors.size()==1){
//    		int fl = ((int[])factors.elementAt(0)).length;
//    		int[][] result = new int[fl][1];
//    		for (int i=0;i<fl;i++)
//    			result[i][0]=((int[])factors.elementAt(0))[i];
//    		return result;
//    	}
//    	else{
//    		int[] firstelement = (int[])factors.remove(0);
//    		//System.out.println("firstelement: " + rbnutilities.arrayToString(firstelement));
//    		int[][] restProd = cartesProd(factors);
//    		//System.out.println("restProd: " + rbnutilities.MatrixToString(restProd));
//    		//System.out.println("restProd.length: " + restProd.length);
//    		int[][] result = new int[restProd.length*firstelement.length][restProd[0].length+1];
//    		int row = 0;
//    		for (int i=0;i<firstelement.length;i++){
//    			for (int j=0;j<restProd.length;j++){
//    				result[row][0]=firstelement[i];
//    				for (int h=0;h<restProd[j].length;h++){
//    					result[row][h+1]=restProd[j][h];
//    				}
//    				row++;
//    			}
//
//    		}
//    		//System.out.println("cartes Prod: " + rbnutilities.MatrixToString(result));
//    		return result;
//    	}
//    }

	public static int[][] cartesProd(Vector<int[]> factors) {
		/** factors is vector of int[]. Returns the
		 * cartesian product of int[] arrays. Example:
		 * factors = {[0,4,2],[6],[9,3]}
		 * result = [[0,6,9],
		 *           [0,6,3],
		 *           [4,6,9],
		 *           [4,6,3],
		 *           [2,6,9],
		 *           [2,6,3]]
		 * This version should be a little faster than the previous
		 **/

		if (factors.isEmpty()) {
			return new int[1][0];
		}

		// Check if any factor is empty, return empty result
		for (int[] factor : factors) {
			if (factor.length == 0) {
				return new int[1][0];
			}
		}

		int numFactors = factors.size();
		int total = 1;
		for (int[] factor : factors) {
			total *= factor.length;
		}

		// Precompute strides for each factor
		int[] strides = new int[numFactors];
		strides[numFactors - 1] = 1;
		for (int i = numFactors - 2; i >= 0; i--) {
			strides[i] = strides[i + 1] * factors.get(i + 1).length;
		}

		// Create and fill the result array
		int[][] result = new int[total][numFactors];
		for (int i = 0; i < total; i++) {
			for (int j = 0; j < numFactors; j++) {
				int[] factor = factors.get(j);
				int pos = (i / strides[j]) % factor.length;
				result[i][j] = factor[pos];
			}
		}

		return result;
	}


    public static int[] clonearray(int[] clonethis){
	int[] result = new int[clonethis.length];
	for (int i=0;i<clonethis.length;i++)
	    result[i]=clonethis[i];
	return result;
    }
    
    public static double[] clonearray(double[] clonethis){
	double[] result = new double[clonethis.length];
	for (int i=0;i<clonethis.length;i++)
	    result[i]=clonethis[i];
	return result;
    }
    
    /** Clones  vector vec without cloning the components of the vector */
    public static Vector clonevector(Vector vec){
	Vector result = new Vector();
	for (int i=0;i<vec.size();i++)
	    result.add(vec.elementAt(i));
	return result;
    }

    public static Vector<GroundAtom> combineAtomVecs(Vector<GroundAtom> atoms1, Vector<GroundAtom> atoms2){
	/* atoms1 and atoms2 are vectors of Atoms
	 * without repetitions
	 * Construct new vector that contains all of 
	 * the atoms in atoms1 and atoms2 combined,
	 * again without repetitions
	 */
	Vector <GroundAtom>result = new Vector<GroundAtom>();
	boolean newatom = true;

	for (int i=0;i<atoms1.size();i++)
	    result.add(atoms1.elementAt(i));
	for (int j=0;j<atoms2.size();j++){
	    newatom = true;
	    for (int i=0;i<atoms1.size();i++){
		if (((GroundAtom)atoms1.elementAt(i)).equals((GroundAtom)atoms2.elementAt(j)))
		    newatom = false;
	    }
	    if (newatom) result.add(atoms2.elementAt(j));
	}
	return result;
    }
    
    /**  computes index of 0,1-vector in list of all 
	 *  0,1-vectors of length 'size' that has the same components 
	 *  as vector at 'index', except in coordinate 'pos'
	 *  0 <= pos < size
	 *  Example: 000   0
	 *           001   1
	 *           010   2
	 *           011   3
	 *           100   4
	 *           101   5
	 *           110   6 
	 *           111   7
	 *  computeCorrespondingIndex(3,1,4)=6
	 */
    public static int computeCorrespondingIndex(int size, int pos,  int index)

    {
	int result;
	//System.out.print("Corresponding index for size " + size + " position " + pos + " index " + index + " : ");
	int[] thistuple = indexToTuple(index,size,2);
	if (thistuple[pos]==0) result = index+(int)Math.pow(2,size-pos-1);
	else result =  index-(int)Math.pow(2,size-pos-1);
	//System.out.println(result);
	return result;
	
    }

    /** computes the index of permutation of tuple with index ind
     *  Example perm = (2,3,1), ind=5, range =2, dim = 3:
     *  tuple represented by index: (1,0,0)
     *  Permuted tuple: (0,0,1)
     *  Index: 2
     */
    public static int computePermutedIndex(int ind, int[] perm, int range, int dim){
        int[] oldtuple = indexToTuple(ind,dim,range);
        int[] newtuple = permuteTuple(oldtuple,perm);
        int newind = tupleToIndex(newtuple,range);
        return newind;
    }



    
    /** Tests whether ar1 and ar2 are equivalent modulo base in the following sense:
     * ar1 and ar2 have the same length, and
     * every number appearing in base appear only in identical places in 
     * ar1 and ar2. Example: 
     * ar1 = [1,4,8,2,1]
     * ar2 = [1,4,7,2,1]
     * ar3 = [2,4,7,2,1]
     * base = [1,4]
     * Then equivArray(ar1,ar2,base)=true
     * and  equivArray(ar1,ar2,base)=false
     * 
     * Result does not depend on order of elements in base
     */
    public static boolean equivArray(int[] ar1, int[] ar2, int[] base){
	boolean result = true;
	if (ar1.length!=ar2.length)
	    result = false;
	int ind = 0;
	while (result && ind<ar1.length){
	    if (arrayContains(base,ar1[ind]) && ar1[ind]!=ar2[ind])
		result = false;
	    if (arrayContains(base,ar2[ind]) && ar1[ind]!=ar2[ind])
		result = false;
	    ind++;
	}
	return result;
    }

    public static boolean equivAtoms(GroundAtom at1, GroundAtom at2, int[] base){
	boolean result;
	//System.out.print("equivAtoms for " + at1.asString() +" "+ at2.asString() +" " + arrayToString(base));
	if (at1.rel != at2.rel)
	    result = false;
	else result = equivArray(at1.args,at2.args,base);
	//System.out.println(" return: " + result);
	return result;
    }


    public static int[] CorrArraySubstraction(String[] vars1, String[] vars2, int[] args)
	/* vars2 and args represent a substitution (arrays have 
	 * the same length). vars1 contains a subset of vars2.  Method returns 
	 * array of the args-values that correspond to vars1-entries 
	 * of vars2
	 */
    {
	int[] result = new int[vars1.length];
	int j = 0;
	boolean found;
	for (int i = 0; i<vars1.length; i++)
	    {
		// find the position of vars1[i] in the 
		// original array and copy corresponding 
		// args - value to subsargs
		found = false;
		while (!found)
		    {
			if (vars1[i].equals(vars2[j]))
			    {
				result[i]=args[j];
				found = true;
			    }
			j++;
		    }
	    }
	return result;
    }
    
    public static String[] CorrArraySubstraction(String[] vars1, String[] vars2, String[] args)
	/* vars2 and args represent a substitution (arrays have 
	 * the same length). vars1 contains a subset of vars2.  Method returns 
	 * array of the args-values that correspond to vars1-entries 
	 * of vars2
	 */
    {
	String[] result = new String[vars1.length];
	int j = 0;
	boolean found;
	for (int i = 0; i<vars1.length; i++)
	    {
		// find the position of vars1[i] in the 
		// original array and copy corresponding 
		// args - value to subsargs
		found = false;
		while (!found)
		    {
			if (vars1[i].equals(vars2[j]))
			    {
				result[i]=args[j];
				found = true;
			    }
			j++;
		    }
	    }
	return result;
    }
    
    

    
    

    /** bv is a 0-1 vector representing integer n
	 * Method turns it into vector representing n+1
	 * (if n+1 < 2^bv.length, otherwise throws exception)
	 */
    public static void incrementBitVector(int[] bv)
    {
	int ind = bv.length-1;
	while (bv[ind]==1 && ind>0){
	    bv[ind]=0;
	    ind--;
	}
	if (ind==0 && bv[ind]==1)
	    throw new RuntimeException("Trying to increment maximal bit vector");
	else{
	    bv[ind]=1;
	}
    }

    /** 
     * maxvals is a vector representing the number of values 0,1,...,maxvals-1 
     * for any component
     * 
     * cv is a current vector of the same length as maxvals, where each
     * entry is <= the corresponding entry in maxvals.
     * 
	 * Method turns cv  into vector representing the "next" configuration
	 * 
	 * Example:
	 * maxvals = [2,3,4,2]
	 * 
	 * cv = [0,0,0,1] -> [0,0,1,0] -> [0,0,1,1] -> [0,0,2,1] ->
	 * 
	 * cv = [0,1,3,1] -> [0,2,0,0] -> ...
	 */
    public static void incrementCatVector(int[] maxvals, int[] cv)
    {
    	int ind = cv.length-1;
    	while (cv[ind]==maxvals[ind]-1 && ind>0){
    		cv[ind]=0;
    		ind--;
    	}
    	if (ind==0 && cv[ind]==maxvals[ind]-1)
    		throw new RuntimeException("Trying to increment maximal vector");
    	else{
    		cv[ind]+=1;
    	}
    }
    
    /** returns the tuple i_0,i_2,...,i_(dim-1) 
	 * that occurs in place 'ind' in a 
	 * lexicographic enumeration of all 
	 * 'dim'-tuples of integers in the range 
	 * [0..range-1]
	 * 0 <= ind <= range^dim 
	 */
    public static int[] indexToTuple(int ind, int dim, int range)
    {
        int [] result = new int[dim];
        for (int i=0; i<dim; i++)
	    {
		result[dim-1-i] = ind % range;
		ind = ind/range;
	    }
        
        return result;
    }


    
    /** Formats a vector of int[] as String */
    public static String intArrVecToString(Vector vec){
	String result = "";
	for (int i=0;i<vec.size();i++){
	    result = result + "[" + arrayToString((int[])vec.elementAt(i))+ "]" ;
	}
	return result;
    }

    public static Vector linkedListToVector(LinkedList lli){
	Vector result = new Vector();
	ListIterator li = lli.listIterator();
	while (li.hasNext()){
	    result.add(li.next());
	}
	return result;
    }

    public static String MatrixToString (int arr[][])
    {   
        String result = "";
        if (arr.length>0)
	    {
		result = arrayToString(arr[0]);
		for (int i = 1; i<arr.length; i++)
		    result = result + "\n" + arrayToString(arr[i]);
	    }
        return result;
    }
    
    public static String MatrixToString (double arr[][])
    {   
        String result = "";
        if (arr.length>0)
	    {
		result = arrayToString(arr[0]);
		for (int i = 1; i<arr.length; i++)
		    result = result + "\n" + arrayToString(arr[i]);
	    }
        return result;
    }
    
    public static String[] NewVariables(String[] vars, int k)
	// returns an array of k new variable names 
	// v<int+1>,v<int+2>,...,v<int+k>
	// where int is the largest integer such that
	// v<int> appears in vars
    {
        int max = 0;
        char first;
        String rest;
        for (int i=0;i<vars.length;i++)
	    {
		if (vars[i].length()==0)
		    throw new IllegalArgumentException("Empty variable string in NewVariables");
		first = vars[i].charAt(0);
		if (first == 'v')
		    {
			rest = vars[i].substring(1,vars[i].length());
			if (rbnutilities.IsInteger(rest))
			    max = Math.max(max,Integer.parseInt(rest));
		    }
	    }
        
        String[] result = new String[k];
        for (int i=0;i<k;i++)
            result[i]="v"+(max+1+i);
        return result;
    }
    
    public static String[] NonIntOnly(String[] arguments)
        /* returns an array containing all the 
         * non-integer strings of arguments 
         * without repetitions.
         * Used in base cases of freevars()
         */
    {
        String[] prelimarray;
        String[] result;
        
        int numfv = 0; // number of proper strings
        
        /* initialize prelimarray to array of length 
         * arguments.length 
         */
        prelimarray = new String[arguments.length];
        for (int i = 0; i<arguments.length; i++)
            prelimarray[i] = "0";
        /* now go through the arguments of the 
         * indicator and add every new variable
         * to prelimarray
         */
        for (int i = 0; i<arguments.length; i++)
	    {
		if (!IsInteger(arguments[i]))
		    {
			// check whether variable is new
			boolean isnew = true;
			for (int j = 0; j < numfv; j++)
			    {
				if(arguments[i].equals(prelimarray[j]))
				    isnew = false;
			    }
			// append to prelimarrays
			if (isnew)
			    {
				numfv++;
				prelimarray[numfv-1]=arguments[i];
			    }
		    }
	    }
        
        result = new String[numfv];
        for (int i = 0; i<numfv; i++) result[i]=prelimarray[i];
        return result;
    }


    /** Example: tup=(4,2,9), perm=(3,1,2) 
     * returns (9,4,2)
     */
    public static int[] permuteTuple(int[] tup, int[] perm){
        int[] result = new int[tup.length];
        for (int i=0;i<tup.length;i++)
            result[i] = tup[perm[i]];
        return result;
    }


    public static void printarray (String arr[])
    {
        System.out.print("[");
        for (int i = 0; i<arr.length-1; i++)
            System.out.print(arr[i]+",");
        System.out.print(arr[arr.length-1]+"]");
    }
    
    public static void printarray (int arr[])
    {
        System.out.print("[");
        for (int i = 0; i<arr.length-1; i++)
            System.out.print(arr[i]+",");
        System.out.print(arr[arr.length-1]+"]");
    }
    
    public static void printarray (double arr[])
    {
        System.out.print("[");
        for (int i = 0; i<arr.length-1; i++)
            System.out.print(arr[i]+",");
        System.out.print(arr[arr.length-1]+"]");
    }
    
    public static void printarray (Rel arr[])
    {
        if (arr.length>0)
	    {
		System.out.print("[");
		for (int i = 0; i<arr.length-1; i++)
		    System.out.print(arr[i].name+"/"+arr[i].arity+",");
		System.out.print(arr[arr.length-1].name+"/"+arr[arr.length-1].arity+"]");
	    }
        else System.out.print("[]");
    }
    
    public static int[] stringArrayToIntArray(String[] arr)
	/* Turn a String array all of whose elements 
	 * can be parsed as integers into an integer array
	 */
    {
    	int[] result = new int[arr.length];
    	for (int i=0;i<result.length;i++){
    		if (IsInteger(arr[i]))
    			result[i]=Integer.parseInt(arr[i]);
    		else return null;
    	}
    	return result;     
    }

    public static int tupleToIndex(int[] tuple, int range)
	/* returns the index of 'tuple': i_0,i_2,...,i_(tuple.length-1)
	 * in an enumeration of all tuples over range [0..range-1]
	 * Error if i_j>range-1 for some j
	 */
    {
        int result = 0;
        int k = tuple.length;
        int factor = 1;
        
        
	for (int i = 0; i < k  ; i++)
            {
                result = result + (tuple[k-1-i])*factor;
                factor = factor * range;
            }
        
        return result;
    }

    public static int tupleToIndex(int[] tuple, int[] ranges)
	/* returns the index of 'tuple': i_0,i_2,...,i_(tuple.length-1)
	 * in an enumeration of all tuples over 
	 * [0..ranges[0]-1] x [0..ranges[1]-1] x ... x [0..ranges[ranges.length-1]-1]
	 * Error if i_j>ranges[j]-1 for some j
	 * or ranges.length != tuple.length
	 */
    {
        int result = 0;
        int k = tuple.length;
        int factor = 1;
        if (ranges.length != tuple.length){
	    throw new IllegalArgumentException("Illegal arguments in tupleToIndex");
        }
	for (int i = 0; i < k  ; i++)
            {
		if (tuple[k-1-i]<0 || tuple[k-1-i] > ranges[k-1-i]-1){
		    throw new IllegalArgumentException("Illegal arguments in tupleToIndex");
		}
		else{
		    result = result + (tuple[k-1-i])*factor;
		    factor = factor * ranges[k-1-i];
		}
            }
        
        return result;
    }


    /** Checks whether all the components in sarray that are integers match the
     * integers in iarray at the same position */
    public static boolean integerMatch(String[] sarray, int[] iarray){
	boolean result = true;
	for (int i = 0; i < sarray.length  ; i++)
	    if (IsInteger(sarray[i]) && Integer.parseInt(sarray[i])!=iarray[i])
		result = false;
	return result;
	    
    }

    /** Returns the index of the (first occurrence) of s in sarray;
     * Returns -1 if s not in sarray
     */
    public static int indexInArray(String[] sarray,String s){
	boolean found = false;
	int result = -1;
	for (int i = 0; i < sarray.length && !found  ; i++){
	    if (sarray[i].equals(s)){
		result = i;
		found = true;
	    }
	}
	return result;	    
    }
  
    public static int[] indexArray(int n) {
    	int[] result = new int[n];
    	for (int i=0;i<result.length;i++)
    		result[i]=i;
    	return result;
    }

    /** Inserts into ts all integer tuples 'ints' of length vars.length, such that
     *  mixedvec[vars/ints]=intvec.
     *
     * Example: mixedvec is (2,x,y,x), intvec is (2,3,4,3), vars is 
     * (y,x). Then method will insert the tuple (4,3) into ts.
     * 
     * If mixedvec is (2,x,y,x), tup is (2,3,4,3), vars is 
     * (y,x,z), then method will insert all tuples (4,3,i) with i=0,...,d-1
     * into ts.
     * 
     * If mixedvec is (2,x,y,x), tup is (1,3,4,3), vars is 
     * (y,x,z), then method will not insert any tuples into ts.
     */
    public static void  allSatisfyingTuples(String[] mixedvec, int[] intvec, String[] vars, TreeSet ts, int d){
//    		System.out.println("allSatisfyingTuples(" + arrayToString(mixedvec) + " "  + 
//    				   arrayToString(intvec) + " "  + arrayToString(vars) ); 
    	if (intvec.length != mixedvec.length)
    		throw new IllegalArgumentException("Tuple of wrong length!");
    	/* Test whether the integer components in mixedvec match with
    	 * intvec */
    	if (!rbnutilities.integerMatch(mixedvec,intvec))
    		return;
    	/* Construct an array indexInVars of size intvec.length that for each variable 
    	 * argument of mixedvec gives the index of this variable
    	 * in vars. Example: mixedvec = (1,x,y,x), vars = (y,x)
    	 * then indexInVars = (-1,2,1,2) (-1 represents a vacuous entry).
    	 *
    	 * Throws IllegalArgumentException if not all variables in mixedvec
    	 * appear in vars
    	 */
    	int[] indexInVars = new int[intvec.length];
    	int nextindex;
    	for (int i = 0; i<indexInVars.length; i++)
    		indexInVars[i]=-1;
    	for (int i = 0; i<mixedvec.length; i++){
    		if (!rbnutilities.IsInteger(mixedvec[i])){
    			nextindex = rbnutilities.indexInArray(vars,mixedvec[i]);
    			if (nextindex == -1)
    				throw new IllegalArgumentException();
    			else
    				indexInVars[i]=nextindex;
    		}		
    	}
    	//System.out.println("    indexInVars: (" + rbnutilities.arrayToString(indexInVars) + ")");
    	/* Now construct the 'pattern' of all tuples to be inserted into
    	 * ts. If mixedvec = (1,x,y,x), vars = (y,x), and intvec=(1,3,4,3)
    	 * then pattern = (4,3); if mixedvec = (1,x,y,x), vars = (y,x), and intvec=(1,3,4,5),
    	 * then the construction of pattern fails, and the method terminates.
    	 * If mixedvec = (1,x,y,x), vars = (y,x,z), and intvec=(1,3,4,3), 
    	 * then pattern=(4,3,-1).
    	 */
    	int[] pattern = new int[vars.length];
    	int wildCardCount = pattern.length;
    	for (int i = 0; i<pattern.length; i++)
    		pattern[i]=-1;
    	for (int i = 0; i<intvec.length; i++){
    		if (indexInVars[i] != -1){
    			if (pattern[indexInVars[i]]!= -1 && pattern[indexInVars[i]]!= intvec[i])
    				return;
    			else {
    				if (pattern[indexInVars[i]]==-1)
    					wildCardCount--;
    				pattern[indexInVars[i]]=intvec[i];
    			}
    		}
    	}
    	//System.out.println("    pattern (" + rbnutilities.arrayToString(pattern) + ")");
    	/* Fill in all possible substitutions for the wildcards (-1 entries) 
    	 * in pattern, and add to ts
    	 */
    	// int[] wildCardPositions = new int[wildCardCount];
    	// 	int nextpos = 0;
    	// 	for (int i = 0; i<pattern.length; i++)
    	// 	    if (pattern[i]==-1){
    	// 		wildCardPostions[nextpos]=i;
    	// 		nextpos++;
    	// 	    }
    	int[] nextsmalltuple;
    	int[] nextbigtuple;
    	for (int i = 0; i<MyMathOps.intPow(d,wildCardCount); i++){
    		nextsmalltuple = rbnutilities.indexToTuple(i,wildCardCount,d);
    		nextbigtuple = new int[pattern.length];
    		nextindex=0;
    		for (int j = 0; j< nextbigtuple.length; j++){
    			if (pattern[j]==-1){
    				nextbigtuple[j]=nextsmalltuple[nextindex];
    				nextindex++;
    			}
    			else
    				nextbigtuple[j]=pattern[j];
    		}
    		//System.out.println("     add (" + rbnutilities.arrayToString(nextbigtuple) + ")");
    		ts.add(nextbigtuple);
    	}

    }

    public static double[] normalizeDoubleArray(double[] ar){
    	double[] result = new double[ar.length];
    	double length = euclidNorm(ar);
    	for (int i=0;i<ar.length;i++)
    		if (length > 0)
    			result[i]=ar[i]/length;
    		else
    			result[i]=ar[i];
    	return result;
    }

    
    public static double euclidNorm(double[] ar){
    	double norm = 0;
    	for (int i=0;i<ar.length;i++)
    		norm = norm + Math.pow(ar[i],2);
    	norm = Math.sqrt(norm);
    	return norm;
    }
    
    public static double squaredNorm(double[] ar){
    	double norm = 0;
    	for (int i=0;i<ar.length;i++)
    		norm = norm + Math.pow(ar[i],2);
    	return norm;
    }
    
    public static double arrayAverage(double[] ar){
    	double result = 0;
    	for (int i=0;i<ar.length;i++)
    		result = result+ar[i];
    	return result/ar.length;
    }
    
    public static double arraySum(double[] ar){
    	double result = 0;
    	for (int i=0;i<ar.length;i++)
    		result = result+ar[i];
    	return result;
    }
    
    public static int arraySum(int[] ar){
    	int result = 0;
    	for (int i=0;i<ar.length;i++)
    		result = result+ar[i];
    	return result;
    }
    
    public static double euclidDist(double[] ar1, double[] ar2){
    	if (ar1.length != ar2.length)
    		System.out.println("Warning: arrays of unequal lengths in rbnutilities.euclidDist  ");
    	return euclidNorm(arrayAdd(ar1,arrayScalMult(ar2,-1)));
    }
    
    /* Performs an integer-valued "min-max normalization" of v, with the following
     * extras and parameters:
     * 
     * inminmax[0] contains the minimum and inminmax[1] the maximum value within the
     * dataset to be normalized
     * 
     * the value will be scaled into the intervall [0..outmax]
     * 
     * shape is a parameter controlling the non-linearity of the transformation: for 
     * shape close to 0, the transformation is nearly linear. Larger values of shape (maximum
     * is pi/2) lead to a larger middle range of the input range being mapped to a small
     * region near the middle point of the output range. I.e., the regions close to the
     * endpoints of the input range are 'magnified' (note that this is the opposite of what 
     * one would like to do to reduce the influence of outliers). 
     * 
     * 
     */
    public static int minMaxNormalize(double v, double[] inminmax, int outmax, double shape){
       	if (inminmax[0]==inminmax[1])
    		return outmax;  	
    	else {
    		// First scale the value linearly into the interval [-shape,shape]
    		double nv = 2*shape*(v-inminmax[0])/(inminmax[1]-inminmax[0])-shape;
    		// Apply tan for a nonlinear 'distortion':
    		nv = Math.tan(nv);
    		// re-scale into interval [0,1]:
    		nv = (nv-Math.tan(-shape))/(Math.tan(shape)-Math.tan(-shape)); 
    		int result = (int)(outmax*nv);
    		//System.out.println("   out: " + result);
    		return result;
    	}
    }
    
    
    public static int sigmoidNormalize(double v, double[] inminmax, int outmax, double shape){
       	if (inminmax[0]==inminmax[1])
    		return outmax;  	
    	else {
    		// First scale the value linearly into the interval [-shape,shape]
    		double nv = 2*shape*(v-inminmax[0])/(inminmax[1]-inminmax[0])-shape;
    		// Apply sigmoid for a nonlinear 'distortion':
    		nv =1/(1+ Math.exp(-3*nv));
    		int result = (int)(outmax*nv);
    		//System.out.println("   out: " + result);
    		return result;
    	}
    }
    
    /* Turns a vector of int[] into an int[]. Intended for 
     * input where each array in intvec is of length 1. Then:
     * [2],[5],...,[3] -> [2,5,...,3]
     */
    public static int[] intArrVecToArr(Vector<int[]> intvec){
    	int[] result = new int[intvec.size()];
    	for (int i=0;i<result.length;i++)
    		result[i]=intvec.elementAt(i)[0];
    	return result;
    }
    
    
    /* at is a string representing a ground atom r(x,y) 
     * returns "(x,y)"
     */
    public static String getRelnameFromAtom(String at){
    	return at.substring(0,at.indexOf("("));
    }
    
    /* at is a string representing a ground atom r(x,y) 
     * returns "(x,y)"
     */
    public static String getArgsFromAtom(String at){
    	return at.substring(at.indexOf("("), at.length());
    }
    
    public static short[] castToShort(int[] intarr){
    	short[] result = new short[intarr.length];
    	for (int i=0;i<result.length;i++)
    		result[i] = (short)intarr[i];
    	return result;
    }
    
    public static boolean hasNaNValues(double[] a){
    	boolean result = false;
    	for (int i=0;i<a.length;i++)
    		if (Double.isNaN(a[i]))
    			result = true;
    	return result;
    }
    
    public static TreeSet treeSetIntersection(TreeSet ts1, TreeSet ts2) {
    	if (!ts1.comparator().getClass().equals( ts2.comparator().getClass()))
    		throw new RBNRuntimeException("Cannot intersect two tree sets with different comparators");
    	TreeSet result = new TreeSet(ts1.comparator());
		for (Iterator<int[]> it = ts1.iterator(); it.hasNext();){
			Object nextel = it.next();
			if( ts2.contains(nextel))
				result.add(nextel);
		}
    	return result;
    }
    
    public static Vector treeSetToVector(TreeSet ts) {
    	Vector result = new Vector();
    	for (Iterator it = ts.iterator();it.hasNext();) {
    		result.add(it.next());
    	}
    	return result;
    }
    
    /* Returns list of domain element names corresponding to
     * the indices in idxs as a single comma-separated string
     * (used in BayesConstructor).
     */
    public static String namestring(int[] idxs, RelStruc A) {
    	String result = "";
		if (idxs.length > 0){
			result = result + A.nameAt(idxs[0]);
			for (int k=1;k<idxs.length;k++)
				result = result + "," + A.nameAt(idxs[k]);
		}
		return result;
    }
    
    public static String[] array_substitute(String[] arr, String[] olds , String[] news) {
    	/* Performs a substitution defined by olds/news on the array arr.
    	 * 
    	 * Example: array_substiture([u,z],[x,y,z],[a,b,c) = [u,c]
    	 * 
    	 */
    	if (olds.length != news.length)
    		System.out.println("calling rbnutilities.array_substitute with unmatched arguments");
    	Hashtable<String,String> substitution = new Hashtable<String,String>();
    	for (int i = 0;i<olds.length;i++)
    		substitution.put(olds[i],news[i]);
    	String[] result = new String[arr.length];
    	for (int i=0;i<result.length;i++) {
    		String s = substitution.get(arr[i]);
    		if (s!= null)
    			result[i]=s;
    		else
    			result[i]=arr[i];
    	}
    	return result;
    }
    
    public static String[] array_substitute(String[] arr, String[] olds , int[] news) {
    	/* Performs a substitution defined by olds/news on the array arr.
    	 * 
    	 * Example: array_substiture([u,z],[x,y,z],[1,2,3]) = [u,3]
    	 * (all elements in result of type String).
    	 */
    	if (olds.length != news.length)
    		System.out.println("calling rbnutilities.array_substitute with unmatched arguments");
    	Hashtable<String,Integer> substitution = new Hashtable<String,Integer>();
    	for (int i = 0;i<olds.length;i++)
    		substitution.put(olds[i],news[i]);
    	String[] result = new String[arr.length];
    	for (int i=0;i<result.length;i++) {
    		Integer s = substitution.get(arr[i]);
    		if (s!= null)
    			result[i]=String.valueOf(s);
    		else
    			result[i]=arr[i];
    	}
    	return result;
    }
    
    public static String asString(Vector<String> sv) {
    	String result = "";
    	for (String s: sv)
    		result += " " +s;
    	return result;
    }
    
	public static String[] stringToArray(String ts , String sep){
		/*
		 * splits the string ts into the substrings defined by the
		 * separator sep and returns them as an array
		 */
		Vector<String> result = new Vector<String>();
		String next;
		int nextsep;
		
		while (ts.length()>0) {
			nextsep = ts.indexOf(sep);
			if (nextsep!= -1){
				next = ts.substring(0,nextsep);
				ts = ts.substring(nextsep+1);
			}
			else{
				next = ts;
				ts = "";
			}
			result.add(next);
		}

		return result.toArray(new String[result.size()]);

	}
	
	public static int[] stringToIntArray(String ts , String sep){
		/*
		 * splits the string ts into the substrings defined by the
		 * separator sep and returns an array consisting of the
		 * substrings parsed as integers:
		 * 
		 * "4,17,5" -> [4,17,5]
		 */
		Vector<Integer> result_vec = new Vector<Integer>();
		String next;
		int nextsep;
		
		while (ts.length()>0) {
			nextsep = ts.indexOf(sep);
			if (nextsep!= -1){
				next = ts.substring(0,nextsep);
				ts = ts.substring(nextsep+1);
			}
			else{
				next = ts;
				ts = "";
			}
			result_vec.add(Integer.parseInt(next));
		}

		int[] result = new int[result_vec.size()];
		for (int i=0; i<result_vec.size(); i++)
			result[i]=result_vec.elementAt(i);
		return result;

	}
	
	
	public static int sampledValue(double[] cpr) {
		/* For probability vector cpr, returns the index of the value sampled
		 * according to the cpr probabilities.
		 * 
		 */
		int result=0;
		double probsum =cpr[0];
		double rand=Math.random();
		for (int i=0; i< cpr.length && probsum<rand;i++) { 
			result++;
			probsum+=cpr[i+1];
		}
		return result;
	}
	
	public static int sampledValue(Double[] cpr) {
		/* For probability vector cpr, returns the index of the value sampled
		 * according to the cpr probabilities.
		 * 
		 */
		int result=0;
		double probsum =cpr[0];
		double rand=Math.random();
		for (int i=0; i< cpr.length && probsum<rand;i++) { 
			result++;
			probsum+=cpr[i+1];
		}
		return result;
	}
	
	public static boolean isDeterministic(double[] cpt) {
		for (int i=0; i<cpt.length; i++) {
			if (cpt[i] != 0 && cpt[i]!=1)
				return false;
		}
		return true;
	}
	
	public static double[] softmax(double[] weights) {
		double[] result= new double[weights.length];
		double sum=0;
		for (int i=0;i<weights.length;i++) {
			result[i]=Math.exp(weights[i]);
			sum+=result[i];
		}
		for (int i=0;i<weights.length;i++) 
			result[i]=result[i]/sum;
		return result;
	}
	
	public static double[] clip(double[] vec,double[][] bounds) {
		/*
		 * Clips the components of vec to the max/min values specified in bounds
		 * Must have: length of vec equal to the length of bounds
		 */
		if (vec.length != bounds.length)
			System.out.println("Warning: incompatible inputs in rbnutilities.clip");
		double[] result = new double[vec.length];
		for (int i=0 ;i < vec.length;i++) {
			result[i]=Math.max(vec[i],bounds[i][0]);
			result[i]=Math.min(result[i],bounds[i][1]);
		}
		return result;
	}

	public static int argmax(double[] arr) {
		int result =0;
		for (int i=1; i<arr.length; i++) {
			if (arr[i]>arr[result])
				result = i;
		}
		return result;
	}

	public static Set<Integer> getNodesInDepth(RelStruc rels, int maxDepth, int nodeArg, CatGnn cpmGnn) {
		Set<Integer> allReached = new LinkedHashSet<>();
		Queue<Integer> queue = new LinkedList<>();
		Map<Integer, Integer> nodeLayer = new HashMap<>();
		try {
			queue.add(nodeArg);
			nodeLayer.put(nodeArg, 0);
			while (!queue.isEmpty()) {
				int current = queue.poll();
				int currentLayer = nodeLayer.get(current);

				if (currentLayer >= maxDepth) {
					continue;
				}

				for (TorchInputSpecs pair : cpmGnn.getGnnInputs()) {
					Rel edge = pair.getEdgeRelation();
					ProbFormBoolAtom temp = new ProbFormBoolAtom(new ProbFormAtom(edge, new String[]{Integer.toString(current), "z"}), true);
					int[][] res = rels.allTrue(temp, new String[]{"z"});
					for (int[] node : res) {
						if (!allReached.contains(node[0])) {
							allReached.add(node[0]);
							queue.add(node[0]);
							nodeLayer.put(node[0], currentLayer + 1);
						}
					}

				}
			}
			return allReached;
		} catch (RBNCompatibilityException e) {
			throw new RuntimeException(e);
		}
	}
}
