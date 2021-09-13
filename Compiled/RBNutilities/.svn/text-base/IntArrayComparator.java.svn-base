/*
* IntArrayComparator.java 
* 
* Copyright (C) 2009 Aalborg University
*
* contact:
* jaeger@cs.aau.dk   http://www.cs.aau.dk/~jaeger/Primula.html
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

import java.util.*;

public class IntArrayComparator implements Comparator{

    public IntArrayComparator(){};

    /** Returns 0 if arr1 and arr2 are equal
     *         -1 if arr1.length <  arr2.length or arr1.length =  arr2.length and arr1 < arr2 in lexical order
     *          1 otherwise
     */
    public  int compare( Object arr1, Object arr2 ){    		
	if (((int[])arr1).length < ((int[])arr2).length)
	    return -1;
	if (((int[])arr1).length > ((int[])arr2).length)
	    return 1;
	int result = 0;
	boolean done = false;
	int i =0;    
	while (!done && i<((int[])arr1).length){
	    if (((int[])arr1)[i]<((int[])arr2)[i]){
		result = -1;
		done = true;
	    }
	    if (((int[])arr1)[i]>((int[])arr2)[i]){
		result = 1;
		done = true;
	    }
	    i++;
	}
	return result;
    }
}
