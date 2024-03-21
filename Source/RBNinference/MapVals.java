package RBNinference;

import java.util.*;

import RBNinference.SampleProbs.p_v_vals;
import RBNpackage.GroundAtomList;
import RBNpackage.Rel;

public class MapVals extends Observable {
	
	/* class for holding the data for the queries about 
	 * one relation
	 */
	private class m_vals{
		private int[] mapvals;
		
		public m_vals(int size) {
			mapvals = new int[size];
		}
		
		protected void setMV(int val, int i){
			mapvals[i]=val;
			setChanged();
		}
		
		protected int[] getMV() {
			return mapvals;
		}
		
		protected void setMV(int[] vals){
			if (vals.length != mapvals.length)
				System.out.println("Inconsistent integer arrays in MapVals");
			for (int i=0;i<mapvals.length;i++)
				mapvals[i]=vals[i];
			setChanged();
		}
	}
	
	private Hashtable<Rel,m_vals> all_m_vals;
	
	private int restarts;
	private String llstring;
	
	
	public MapVals(Hashtable<Rel,GroundAtomList> qatoms){
	    restarts = 0;
	    llstring = "";
		all_m_vals = new Hashtable<Rel,m_vals>();
		for (Rel r: qatoms.keySet()) {
			all_m_vals.put(r,new m_vals(qatoms.get(r).size()));
		}
	}

	

	
	public void setRestarts(int r){
		restarts = r;
		setChanged();
	}
	
	public void setLL(String s){
		llstring = s;
		setChanged();
	}
	
	public int[] getMVs(Rel r){
		return all_m_vals.get(r).getMV();
	}
	
	public void setMVs(Rel r,int val, int i){
		all_m_vals.get(r).setMV(val,i);
	}
	
	
	public int getRestarts(){
		return restarts;
	}
	
	public String getLLstring(){
		return llstring;
	}

}
