/*
* DoubleArrayComparatorByComponent.java 
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

/** Compares two double arrays according to their value in 
 * component 'index'
 */
public class DoubleArrayComparatorByComponent implements Comparator{

    int compareindex; 
    public DoubleArrayComparatorByComponent(int index){
	compareindex = index;
    }

    /** Returns 0 if arr1 and arr2 are equal
     *         -1 if arr1.length <  arr2.length or arr1.length =  arr2.length and arr1 > arr2 in lexical order
     *          1 otherwise
     */
    public  int compare( Object arr1, Object arr2 ){    		
	if ( ((double[])arr1)[compareindex] == ((double[])arr1)[compareindex])
	    return 0;
	if ( ((double[])arr1)[compareindex] < ((double[])arr1)[compareindex])
	    return -1;
	else return 1;
    }
}
