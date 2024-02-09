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
	 * For each query atom have arrays of length corresponding to the
	 * the number of values for the atom's relation
	 */
	private double[][] probs;
	private double[][] maxprobs;
	private double[][] minprobs;
	private double[][] variance;
	private int size;
	private double weight;


	public SampleProbs(GroundAtomList queryatoms){
		int l = queryatoms.size();
		probs = new double[l][];
		maxprobs = new double[l][];
		minprobs = new double[l][];
		variance = new double[l][];
		
		for (int i =0;i<l;i++) {
			int v = (int)queryatoms.atomAt(i).rel().numvals();
			probs[i]=new double[v];
			maxprobs[i]=new double[v];
			minprobs[i]=new double[v];
			variance[i]=new double[v];
		}
	}





	public void setProb(double p, int i, int val){
		probs[i][val]=p;
		setChanged();
	}

	public void setProbs(double[] p, int i){
		probs[i]=p;
		setChanged();
	}

	public void setMinProb(double p, int i, int val){
		minprobs[i][val]=p;
		setChanged();
	}

	public void setMinProbs(double[] p, int i){
		minprobs[i]=p;
		setChanged();
	}

	public void setMaxProb(double p, int i, int val){
		maxprobs[i][val]=p;
		setChanged();
	}
	public void setMaxProbs(double[] p, int i){
		maxprobs[i]=p;
		setChanged();
	}
	public void setVar(double p, int i, int val){
		variance[i][val]=p;
		setChanged();
	}

	public void setVars(double[] p, int i){
		variance[i]=p;
		setChanged();
	}

	public void setProbs(double[][] probs){
		this.probs = probs;
		setChanged();
	}





	public double[][] getProbs(){
		return probs;
	}


	public double[][] getMinProbs(){
		return minprobs;
	}

	public double[][] getMaxProbs(){
		return maxprobs;
	}

	public double[][] getVar(){
		return variance;
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


