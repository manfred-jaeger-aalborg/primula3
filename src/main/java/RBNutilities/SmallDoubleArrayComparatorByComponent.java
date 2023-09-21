/*
* SmallDoubleArrayComparator.java 
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

/** Compares two double arrays according to their components 
 * baseindex and factorindex, which are interpreted as a 
 * representation for a SmallDouble.
 * 
 * IT SEEMS THAT THIS CLASS IS CURRENTLY NOT NEEDED -- METHODS COMMENTED OUT
 */
public class SmallDoubleArrayComparatorByComponent implements Comparator{

    int baseindex;
    int factorindex;

    public SmallDoubleArrayComparatorByComponent(int bi, int fi){
	baseindex = bi;
	factorindex = fi;
    }


    public  int compare( Object arr1, Object arr2 ){ 
	double[] sd1 = new double[2];
	double[] sd2 = new double[2];
	sd1[0]=((double[])arr1)[baseindex];
	sd1[1]=((double[])arr1)[factorindex];
	sd2[0]=((double[])arr2)[baseindex];
	sd2[1]=((double[])arr2)[factorindex];
	return SmallDouble.compareSD(sd1,sd2);
    }
}
