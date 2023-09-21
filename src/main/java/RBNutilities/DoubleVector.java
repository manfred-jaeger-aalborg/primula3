/*
* DoubleVector.java 
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

public class DoubleVector{

    Vector vec;

    public DoubleVector(){
	vec = new Vector();
    }


    public void add(double i){
	Double ii = new Double(i);
	vec.add(ii);
    }

    // if vec is sorted, then this insertion will 
    // maintain the order
    public void addSorted(double i){
	Double ii = new Double(i);
	int k;
	if (vec.size()==0) vec.add(ii);
	else{
	    k = 0;
	    while (k<vec.size()  && ii.compareTo(((Double)vec.elementAt(k)))>0) k++;
	    if (k==vec.size()) vec.add(ii);
	    else vec.add(k,ii);
	}
    }

    public double[] asArray(){
	double[] result = new double[vec.size()];
	for (int i=0;i<vec.size();i++)
	    result[i]=((Double)vec.elementAt(i)).doubleValue();
	return result;
    }

    // The following is not linear time!
    public void mergeSorted(DoubleVector ivec){
	for (int i = 0; i<ivec.size(); i++)
	    addSorted(ivec.elementAt(i));
    }

    public int size(){
	return vec.size();
    }

    public double elementAt(int i){
	return ((Double)vec.elementAt(i)).doubleValue();
    }

    public int indexOf(double i){
	int ind = 0;
	while (elementAt(ind) != i) ind++;
	if (ind < vec.size()) return ind;
	else return -1;	
    }

    public double set(int ind, double d){
	Double dd = new Double(d);
	Double old = (Double)vec.set(ind, dd);
	return old.doubleValue();
    } 
}
