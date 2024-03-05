/*
 * SampleProbs.java
 * 
 * Copyright (C) 2005 Aalborg University
 *
 * contact:
 * jaeger@cs.auc.dk    www.cs.auc.dk/~jaeger/Primula.html
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


package RBNinference;





import java.util.*;

import RBNpackage.*;





public class SampleProbs extends Observable{


	/*
	 * Represents estimated probabilities for a list of query atoms
	 * of different relations.
	 * 
	 * For each query atom have arrays of length corresponding to the
	 * the number of values for the atom's relation
	 */
	
	/* class for holding the data for the queries about 
	 * one relation
	 */
	private class p_v_vals{
		
		private double[][] probs;
		private double[][] variance;
		
		public double[][] getProbs() {
			return probs;
		}

		public void setProbs(double[][] probs) {
			this.probs = probs;
		}

		public double[][] getVariance() {
			return variance;
		}

		public void setVariance(double[][] variance) {
			this.variance = variance;
		}
		
		
		public p_v_vals(int rows, int numvals) {
			probs = new double[rows][numvals];
			variance  = new double[rows][numvals];
		}
		
		public void setprob_row(int row, double[] p) {
			probs[row]=p;
		}
		public void setvar_row(int row, double[] v) {
			variance[row]=v;
		}
		
		
	}
	
	private Hashtable<Rel,p_v_vals> all_p_v_vals;
	private int size;
	private double weight;


	public SampleProbs(Hashtable<Rel,GroundAtomList> qatoms){
		all_p_v_vals = new Hashtable<Rel,p_v_vals>();
		for (Rel r: qatoms.keySet()) {
			all_p_v_vals.put(r,new p_v_vals(qatoms.get(r).size(),(int)r.numvals()));
		}
	}

	public void setProb(Rel r, double[] p, int i){
		all_p_v_vals.get(r).setprob_row(i, p);
		setChanged();
	}


	public void setVar(Rel r, double[] v, int i){
		all_p_v_vals.get(r).setvar_row(i, v);
		setChanged();
	}


	public double[][] getProbs(Rel r){
		return all_p_v_vals.get(r).getProbs();
	}
	
	public double[][] getVariance(Rel r){
		return all_p_v_vals.get(r).getVariance();
	}
	

	public int getSize(){
		return size;
	}


	public void setSize(int size){
		this.size = size;
	}


	public double getWeight(){
		return weight;
	}


	public void setWeight(double weight){
		this.weight = weight;
	}


}


