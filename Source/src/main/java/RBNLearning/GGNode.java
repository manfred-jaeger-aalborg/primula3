/*
* GGNode.java 
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

package RBNLearning;

import java.util.*;

import RBNExceptions.*;

public abstract class GGNode implements Comparable<GGNode>{

	/** The gradient graph that this node belongs to */
	GradientGraphO thisgg;
	

	Vector<GGCPMNode> children;
	TreeSet<GGNode> parents;
	TreeSet<GGNode> ancestors;
	
	Integer identifier;
	
	/** The value returned by the last call of evaluate(); null if node not 
	 * yet evaluated or method resetValue() has been called */
//	Double value;

	// For nodes representing categorical variables: value contains the probabilities for all
	// possible values. For nodes representing Boolean variables: value is an array of length
	// 1 containing the probability of the 'true' value.
	Double[] value;

	Boolean is_evaluated = false;
	
	/**
	 * If there are sum atoms: the values obtained for the numchains*windowsize different
	 * settings of sample values at GGAtomSumNodes. Only used for nodes that are ancestors of 
	 * GGAtomSum nodes.
	 * 
	 * Dimensions: (numchains*windowsize)x numvalues (numvalues =1 when isBoolean==true).
	 */
	Double[][] values_for_samples;

//	/** The result of the most recent call to evaluatesTo()
//	*  0: evaluatesTo() = 0
//	*  1: evaluatesTo() = 1
//	*  -1: evaluatesTo() = -1
//	*  -2: evaluatesTo() has not been executed in the current
//	* setting of the indicator nodes
//	*/
//	int evaluatesToValue;





	/** The partial derivatives of this node as returned by the most recent 
	 * calls of evaluateGrad(param,sno); null if not yet evaluated or method 
	 * resetGrad(param) has been called */

	/* Treemap contains an entry for a key "pname" if 
	 * node depends on parameter "pname".
	 * 
	 * Value is NaN if currently not evaluated
	 */
	TreeMap<String,Double> gradient;

	// indicates whether this node computes a scalar value, or a vector
	// When the node computes the probability of an atom (as upper ground atom nodes)
	// then isScalar=true indicates that the atom is a Boolean relation, and the
	// returned value is the probability of 'true'
	boolean isScalar;

	// The dimension of the output computed by this GGNode. isScalar = true iff outDim=1.
	// If this is an upper ground atom node representing a categorical atom, then
	// outDim is equal to the number of possible values for the relation of the atom.
	int outDim;
	
	public GGNode(GradientGraphO gg){
		thisgg = gg;
		children = new Vector<GGCPMNode>();
		parents = new TreeSet<GGNode>();
		ancestors = null;
		identifier = Integer.valueOf(gg.getNextId());
		value = null;
		gradient = new TreeMap<String,Double>();
	}

	
	public void addToChildren(GGCPMNode ggpfn){
		children.add(ggpfn);
	}

	/** Evaluate this GGNode using current values of parameters
	 * and the current instantiation for unobserved atoms. Returns the
	 * value and sets the value field of the node.
	 * 
	 * If this.value is not null, then this value is assumed to be 
	 * the currently correct value, and is returned
	 * 
	 * When this GGNode depends on GGAtomSumNodes, then the evaluation is
	 * done for every sample number and the result stored in 
	 * values_for_samples
	 * 
	 */
	public Double[] evaluate() {
		if (this.values_for_samples==null) {
			value=this.evaluate(null);
		}
		else {
			for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
				this.values_for_samples[i]=this.evaluate(i);
			value=null;
		}
		is_evaluated=true;
		return value;	
	};
	
	/* For nodes depending on a sum node: evaluation relative 
	 * to the the values in the sample with index sno.
	 * 
	 * For nodes not depending on a sum node: call evaluate(null)
	 */
	public abstract Double[] evaluate(Integer sno);

	public abstract double evaluateGrad(String param) throws RBNNaNException;

//	/** Returns 0 (1) if this node evaluates to 0 (1) given a current partial
//	* instantiation of the indicator nodes. Returns -1 if the current 
//	* partial instantiation of the indicators does not make this 
//	* node surely 0 or 1 valued.
//	*/
//	public abstract int evaluatesTo();


	public abstract void evaluateBounds();



	public Double[] value(){
		return value;
	}


	public TreeMap<String,Double> gradient(){
		return gradient;
	}
	
	public void resetValue(){
		is_evaluated=false;
		value = null;
	}



	public void resetGradient(){
		for (String pname: gradient.keySet()){
			gradient.put(pname, Double.NaN);
		}
	}
	
//	public void resetGradient(){
//		for (int i = 0; i<gradient.length; i++)
//			gradient[i]=null;
//	}
//	
	
	public void resetGradient(String p){
		if (gradient.containsKey(p))
			gradient.put(p, Double.NaN);
	}
	

	public int childrenSize(){
		if (children == null)
			return 0;
		else
			return children.size();
	}
	
	public TreeSet<GGNode> parents(){
		return parents;
	}
	
	public void addToParents(GGNode ggn){
		parents.add(ggn);
	}
	
//	public boolean equals(GGNode other){
//		return this.name().equals(other.name());
//	}
//	
//	
//	public int compareTo(GGNode other){
//		return this.name().compareTo(other.name());
//	}
	
	public boolean equals(GGNode other){
		return (this.identifier == other.identifier());
	}


	public int compareTo(GGNode other){
		return this.identifier.compareTo(other.identifier());
	}
	
	/** Returns the set of all ancestors of this node
	 * in the Graph
	 * @return
	 */
	public TreeSet<GGNode> ancestors(){
		TreeSet<GGNode> result = new TreeSet<GGNode>();
		GGNode nextggn;
		for (Iterator<GGNode> it = parents.iterator(); it.hasNext();){
			nextggn = it.next();
			result.add(nextggn);
			nextggn.collectAncestors(result);
		}
		return result;
	}

	public void setAncestors(){
		ancestors = ancestors();
	}
	
	public void deleteAncestors(){
		ancestors = null;
	}
	
	private void collectAncestors(TreeSet<GGNode> ancests){
		GGNode nextggn;
		for (Iterator<GGNode> it = parents.iterator(); it.hasNext();){
			nextggn = it.next();
			if (!ancests.contains(nextggn)){
				ancests.add(nextggn);
				nextggn.collectAncestors(ancests);
			}
		}
	}
	
	/** Re-evaluates all ancestor nodes of this node. 
	 * Used to propagate value changes when the value of this
	 * node has been changed. Mostly applied when this is 
	 * a GGAtomNode, and the value of this
	 * indicator has been changed in Gibbs sampling or MAP inference
	 */
	public void reEvaluateUpstream(){
		
		if (ancestors == null) 
			ancestors = ancestors();
		
		for (GGNode anc: ancestors)
			anc.resetValue();
		for (GGNode anc: ancestors)
			anc.evaluate();
	}
	
	/* Re-evaluate values and partial derivatives for parameter param 
	 * Usually called when in coordinate-gradient descent a single 
	 * parameter has changed value */
	public void reEvaluateUpstream(String param)
			throws RBNNaNException
	{
		TreeSet<GGNode> myancestors;
		if (ancestors != null) 
			myancestors = ancestors;
		else
			myancestors = ancestors();
		GGNode nextggn;
		for (Iterator<GGNode> it = myancestors.iterator(); it.hasNext();){
			nextggn = it.next();
			nextggn.resetValue();
			nextggn.resetGradient(param);
		}
		for (Iterator<GGNode> it = myancestors.iterator(); it.hasNext();){
			nextggn = it.next();
			nextggn.evaluate();
			nextggn.evaluateGrad(param);
		}
			
	}
	
//	public void setDependsOn(int param){
//		if (this instanceof GGProbFormNode)
//		 ((GGProbFormNode)this).dependsOnParam[param] = true;
//	}

	public void setDependsOn(String param){
		if (this instanceof GGCPMNode)
			gradient.put(param, Double.NaN);
	}

	public int identifier(){
		return identifier;
	}
	
	public void printParents(){
		for (Iterator<GGNode> e=parents.iterator() ; e.hasNext();)
			System.out.print(e.next().identifier() + " ");
	}
	
	public void printChildren(){
		for (Iterator<GGCPMNode> e=children.iterator() ; e.hasNext();){
			Object o = e.next();
			if (! (o == null)) // GGConvCombNodes contain null objects in their children vectors!
				System.out.print(((GGNode)o).identifier() + " ");
		}
	}
	public void init_values_for_samples() {
		if (this.isScalar)
			values_for_samples = new Double[thisgg.numchains*thisgg.windowsize][1];
		else
			values_for_samples = new Double[thisgg.numchains*thisgg.windowsize][this.outDim()];
	}
	
	public abstract int outDim();
	
}
