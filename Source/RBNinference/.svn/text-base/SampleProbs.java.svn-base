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





public class SampleProbs extends Observable{


    private double[] probs;
    private double[] maxprobs;
    private double[] minprobs;
    private double[] variance;
    private int size;
    private double weight;


    public SampleProbs(int i){


	probs = new double[i];
	maxprobs = new double[i];
	minprobs = new double[i];
	variance = new double[i];

    }





    public void setProb(double p, int i){
	probs[i]=p;
	setChanged();
    }


    public void setMinProb(double p, int i){
	minprobs[i]=p;
	setChanged();
    }

    public void setMaxProb(double p, int i){
	maxprobs[i]=p;
	setChanged();
    }

    public void setVar(double p, int i){
	variance[i]=p;
	setChanged();
    }

    public void setProbs(double[] probs){
	this.probs = probs;
	setChanged();
    }


	


    public double[] getProbs(){
	return probs;
    }


     public double[] getMinProbs(){
	return minprobs;
    }

     public double[] getMaxProbs(){
	return maxprobs;
    }

    public double[] getVar(){
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


