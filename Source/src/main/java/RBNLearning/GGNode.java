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
	
	Integer identifier;
	

//	// Array of length this.outDim(). Most GGNodes compute just a scalar value. 
//	// Cases where this.outDim()>1: GGSoftMaxNodes and some GGGnnNodes
//	Double[] value;
//
//	// A flag that indicates whether in the current computation this node has already been
//	// evaluated (in order to avoid redundant calls to evaluate the same node several times)
//	Boolean is_evaluated = false;
	/*
	 * All the RBN parameters (or numerical max. atoms) that this
	 * Node depends on
	 */
	private TreeSet<String> myparameters;
	
	/**
	 * Flag for whether this node depends on an unknown atom, i.e., has a AtomSumNode as a descendant
	 */
	Boolean depends_on_sample = false;
	
	public Boolean getDepends_on_sample() {
		return depends_on_sample;
	}

	public void setDepends_on_sample(Boolean depends_on_sample) {
		this.depends_on_sample = depends_on_sample;
	}

	/**
	 * The values for this node under different sampled instantiations of GGAtomSumNodes that this 
	 * node depends on.
	 * 
	 * if this.depends_on_sample == true, then the dimensions are 
	 * (numchains*windowsize)x this.outDim
	 * 
	 * if this.depends_on_sample == false, then the dimensions are
	 * 1 x this.outDim
	 */
	double[][] values_for_samples;

	/**
	 * Flags that indicates whether in the current computation this node has already been
	 * evaluated for a given sample number. Dimension: 
	 * 
	 * if this.depends_on_sample == true, then the dimension is
	 * (numchains*windowsize)
	 * 
	 * if this.depends_on_sample == false, then the dimension is
	 * 1 
	 */	
	Boolean[] is_evaluated_for_samples;
	
	/* For nodes depending on a sum node: evaluation relative 
	 * to the the values in the sample with index sno.
	 * 
	 * For nodes not depending on a sum node: call evaluate(null)
	 * 
	 * evaluate(null) at nodes with values_for_samples: evaluate for all samples!
	 * 
	 * Return array has different structure/content for different types of GGNodes:
	 * - likelihood node: likelihood expressed as a small double
	 * - 'scalar' GGNodes (the most common case): 1-dim array containing the scalar value  of the probability (sub-) formula represented by this node
	 * - GGNodes representing categorical atoms: array whose dimension is equal to the number of possible values of the relation
	 */
	public abstract double[] evaluate(Integer sno);
	
//	/** The result of the most recent call to evaluatesTo()
//	*  0: evaluatesTo() = 0
//	*  1: evaluatesTo() = 1
//	*  -1: evaluatesTo() = -1
//	*  -2: evaluatesTo() has not been executed in the current
//	* setting of the indicator nodes
//	*/
//	int evaluatesToValue;






	/* Treemap contains an entry for a key "pname" if 
	 * node depends on parameter "pname".
	 * 
	 * Values are Arrays of length this.outDim() containing the partial 
	 * derivatives for all output values
	 * 
	 */

	//TreeMap<String,Double[]>[] gradient_for_samples;
	ArrayList<TreeMap<String,Double[]>> gradient_for_samples;


	// The dimension of the output computed by this GGNode. isScalar = true iff outDim=1.
	// If this is an upper ground atom node representing a categorical atom, then
	// outDim is equal to the number of possible values for the relation of the atom.
	int outDim;
	
	public GGNode(GradientGraphO gg){
		thisgg = gg;
		children = new Vector<GGCPMNode>();
		identifier = Integer.valueOf(gg.getNextId());
		values_for_samples = null;
		myparameters=new TreeSet<String>();
		depends_on_sample=false;
		outDim = 1; // The default value for ProbForm nodes. Needs to be overridden for categorical
	}

	
	public void addToChildren(GGCPMNode ggpfn){
		children.add(ggpfn);
	}

//	/** Evaluate this GGNode using current values of parameters
//	 * and the current instantiation for unobserved atoms. Returns the
//	 * value and sets the value field of the node.
//	 * 
//	 * If this.value is not null, then this value is assumed to be 
//	 * the currently correct value, and is returned
//	 * 
//	 * When this GGNode depends on GGAtomSumNodes, then the evaluation is
//	 * done for every sample number and the result stored in 
//	 * values_for_samples
//	 * 
//	 */
//	public Double[] evaluate() {
//		if (this.values_for_samples==null) {
//			value=this.evaluate(null);
//		}
//		else {
//			for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
//				this.evaluate(i);
//			value=null;
//		}
//		is_evaluated=true;
//		return value;	
//	};
	


//	public abstract double evaluateGrad(Integer sno, String param) throws RBNNaNException;

//	/** Returns 0 (1) if this node evaluates to 0 (1) given a current partial
//	* instantiation of the indicator nodes. Returns -1 if the current 
//	* partial instantiation of the indicators does not make this 
//	* node surely 0 or 1 valued.
//	*/
//	public abstract int evaluatesTo();


//	public abstract void evaluateBounds();



//	public Double[] value(Integer sno){
//		return values_for_samples[sno];
//	}

	public TreeMap<String,Double[]> gradient(Integer sno){
		return gradient_for_samples.get(sno);
	}

	public void resetValue(Integer sno){
		if (this instanceof GGGnnNode)
			((GGGnnNode) this).getGnnPy().resetDict(((GGGnnNode) this).isXPred(), ((GGGnnNode) this).isEdgePred());
		if (depends_on_sample) { 
			if (sno==null) {
				for (int i=0;i<values_for_samples.length;i++)
					resetValue(i);
			}
			else {
				is_evaluated_for_samples[sno]=false;
				values_for_samples[sno] = null;
			}
		}
		else {
			is_evaluated_for_samples[0]=false;
			values_for_samples[0] = null;
		}
	}


	public void resetGradient(Integer sno){
		for (String pname: gradient_for_samples.get(0).keySet()){ // gradients for all samples are for the same parameters
			resetGradient(sno,pname);
		}
	}

	public void resetGradient(Integer sno,String p){
		if (depends_on_sample) { 
			if (sno==null) {
				for (int i=0;i<gradient_for_samples.size();i++)
					resetGradient(i,p);
			}
			else {
				is_evaluated_for_samples[sno]=false;
				gradient_for_samples.remove((int)sno);
				gradient_for_samples.add(sno,new TreeMap<String,Double[]>());
			}
		}
		else {
			is_evaluated_for_samples[0]=false;
			gradient_for_samples.remove(0);
			gradient_for_samples.add(0,new TreeMap<String,Double[]>());
		}
	}


	public int childrenSize(){
		if (children == null)
			return 0;
		else
			return children.size();
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
	

	
//	/* Re-evaluate values and partial derivatives for parameter param 
//	 * Usually called when in coordinate-gradient descent a single 
//	 * parameter has changed value */
//	public void reEvaluateUpstream(String param)
//			throws RBNNaNException
//	{
//		TreeSet<GGNode> myancestors;
//		if (ancestors != null) 
//			myancestors = ancestors;
//		else
//			myancestors = ancestors();
//		GGNode nextggn;
//		for (Iterator<GGNode> it = myancestors.iterator(); it.hasNext();){
//			nextggn = it.next();
//			nextggn.resetValue();
//			nextggn.resetGradient(param);
//		}
//		for (Iterator<GGNode> it = myancestors.iterator(); it.hasNext();){
//			nextggn = it.next();
//			nextggn.evaluate();
//			nextggn.evaluateGrad(param);
//		}
//			
//	}
	
//	public void setDependsOn(int param){
	//		if (this instanceof GGProbFormNode)
	//		 ((GGProbFormNode)this).dependsOnParam[param] = true;
	//	}

	public void setDependsOn(String param){
		myparameters.add(param);
	}

	public boolean dependsOn(String param){
		return (myparameters.contains(param));
	}
	public int identifier(){
		return identifier;
	}
	
	
	public void printChildren(){
		for (Iterator<GGCPMNode> e=children.iterator() ; e.hasNext();){
			Object o = e.next();
			if (! (o == null)) // GGConvCombNodes contain null objects in their children vectors!
				System.out.print(((GGNode)o).identifier() + " ");
		}
	}
	
	public void init_values_and_grad(Boolean valuesonly) {
		int dim;
		if (this.depends_on_sample)
			dim = thisgg.numchains*thisgg.windowsize;
		else
			dim =1;
		values_for_samples = new double[dim][];
		is_evaluated_for_samples = new Boolean[dim];
		for (int i=0;i<dim;i++) {
			values_for_samples[i]=null;
			is_evaluated_for_samples[i]=false; 
		}
		if (!valuesonly) { // Also need gradients
			gradient_for_samples =  new ArrayList<TreeMap< String,Double[]>>();
			for (int i=0;i<dim;i++) {
				gradient_for_samples.add(new TreeMap<String,Double[]>());
			}
			
		}
//		if (this.isScalar)
//			values_for_samples = new Double[thisgg.numchains*thisgg.windowsize][1];
//		else
//			values_for_samples = new Double[thisgg.numchains*thisgg.windowsize][this.outDim()];
	}
	
	public int outDim() {
		return outDim;
	};
	
	public boolean isScalar() {
		return (outDim==1);
	}

	public Vector<GGCPMNode> getChildren() { return children; }
}
