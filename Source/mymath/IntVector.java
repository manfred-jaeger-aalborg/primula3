package mymath;

import java.util.*;

public class IntVector{

    Vector vec;

    public IntVector(){
	vec = new Vector();
    }

    public void add(int i){
	Integer ii = new Integer(i);
	vec.add(ii);
    }

    public void add(int k,int i){
	// add at position k
	Integer ii = new Integer(i);
	vec.add(k,ii);
    }   
    


    // if vec is sorted, then this insertion will 
    // maintain the order
    public void addSorted(int i){
	Integer ii = new Integer(i);
	int k;
	if (vec.size()==0) vec.add(ii);
	else{
	    k = 0;
	    while (k<vec.size()  && ii.compareTo(((Integer)vec.elementAt(k)))>0) k++;
	    if (k==vec.size()) vec.add(ii);
	    else vec.add(k,ii);
	}
    }


    public int[] asArray(){
	int[] result = new int[vec.size()];
	for (int i=0;i<vec.size();i++)
	    result[i]=((Integer)vec.elementAt(i)).intValue();
	return result;
    }

    // The following is not linear time!
    public void mergeSorted(IntVector ivec){
	for (int i = 0; i<ivec.size(); i++)
	    addSorted(ivec.elementAt(i));
    }

    public int size(){
	return vec.size();
    }

    public int elementAt(int i){
	return ((Integer)vec.elementAt(i)).intValue();
    }

    public int indexOf(int i){
	int ind = 0;
	while (elementAt(ind) != i) ind++;
	if (ind < vec.size()) return ind;
	else return -1;	
    }


    public int set(int ind, int i){
	Integer ii = new Integer(i);
	Integer old = (Integer)vec.set(ind, ii);
	return old.intValue();
    } 
}
