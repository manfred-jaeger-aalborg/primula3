package mymath;

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
