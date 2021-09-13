package RBNinference;

import java.util.*;

public class MapVals extends Observable {
	
	private int[] mapvals;
	private int restarts;
	private String llstring;
	
	public MapVals(int i){
		mapvals = new int[i];
	    restarts = 0;
	    llstring = "";
	}

	public void setMV(int val, int i){
		mapvals[i]=val;
		setChanged();
	}
	
	public void setMV(int[] vals){
		if (vals.length != mapvals.length)
			System.out.println("Inconsistent integer arrays in MapVals");
		for (int i=0;i<mapvals.length;i++)
			mapvals[i]=vals[i];
		setChanged();
	}
	
	public void setRestarts(int r){
		restarts = r;
		setChanged();
	}
	
	public void setLL(String s){
		llstring = s;
		setChanged();
	}
	
	public int[] getMVs(){
		return mapvals;
	}
	
	public int getRestarts(){
		return restarts;
	}
	
	public String getLLstring(){
		return llstring;
	}

}
