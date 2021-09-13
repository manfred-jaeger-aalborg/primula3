/*
* GradientGraphNode.java 
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
import java.io.*;
import RBNpackage.*;
import RBNgui.*;
import RBNExceptions.*;
import RBNutilities.*;
import RBNinference.*;

public abstract class GradientGraphNode implements Comparable<GradientGraphNode>{

	/** The gradient graph that this node belongs to */
	GradientGraph thisgg;
	

	Vector<GradientGraphProbFormNode> children;
	TreeSet<GradientGraphNode> parents;

	/** The value returned by the last call of evaluate(sno); null if node not 
	 * yet evaluated or method resetValue() has been called */
	Double value;

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
	Double[] gradient;




	public GradientGraphNode(GradientGraph gg){
		thisgg = gg;
		children = new Vector<GradientGraphProbFormNode>();
		parents = new TreeSet<GradientGraphNode>();
		value = null;
		gradient = new Double[gg.numberOfParameters()];
		for (int i=0; i< gradient.length; i++)
			gradient[i]=null;
	}

	public void addToChildren(GradientGraphProbFormNode ggpfn){
		children.add(ggpfn);
	}

	/** Evaluate this GradientGraphNode using current values of parameters
	 * and the current instantiation for unobserved atoms. Returns the
	 * value and sets the value field of the node.
	 * 
	 * If the value is not null, then the this value is assumed to be 
	 * the currently correct value, and is returned
	 * 
	 */
	public abstract double evaluate();

	public abstract double evaluateGrad(int param);

//	/** Returns 0 (1) if this node evaluates to 0 (1) given a current partial
//	* instantiation of the indicator nodes. Returns -1 if the current 
//	* partial instantiation of the indicators does not make this 
//	* node surely 0 or 1 valued.
//	*/
//	public abstract int evaluatesTo();


	public abstract void evaluateBounds();

	/** The name of this node. The name identifies the function represented
	 * by a node. Names are unique.
	 */
	public abstract String name();

//	public abstract String name(RelStruc A);

	public double value(){
		return value;
	}



	public Double[] gradient(){
		return gradient;
	}

	public void resetValue(){
		value = null;
	}



	public void resetGradient(){
		for (int i = 0; i<gradient.length; i++)
			gradient[i]=null;
	}

	public int childrenSize(){
		if (children == null)
			return 0;
		else
			return children.size();
	}
	
	public TreeSet<GradientGraphNode> parents(){
		return parents;
	}
	
	public void addToParents(GradientGraphNode ggn){
		parents.add(ggn);
	}
	
	public boolean equals(GradientGraphNode other){
		return this.name().equals(other.name());
	}
	
	
	public int compareTo(GradientGraphNode other){
		return this.name().compareTo(other.name());
	}
	
	/** Returns the set of all ancestors of this node
	 * in the Graph
	 * @return
	 */
	public TreeSet<GradientGraphNode> ancestors(){
		TreeSet<GradientGraphNode> result = new TreeSet<GradientGraphNode>();
		GradientGraphNode nextggn;
		for (Iterator<GradientGraphNode> it = parents.iterator(); it.hasNext();){
			nextggn = it.next();
			result.add(nextggn);
			nextggn.collectAncestors(result);
		}
		return result;
	}

	private void collectAncestors(TreeSet<GradientGraphNode> ancests){
		GradientGraphNode nextggn;
		for (Iterator<GradientGraphNode> it = parents.iterator(); it.hasNext();){
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
	 * a GradientGraphIndicatorNode, and the value of this
	 * indicator has been changed in Gibbs sampling. 
	 */
	public void reEvaluateUpstream(){
		TreeSet<GradientGraphNode> myancestors = this.ancestors();
		for (Iterator<GradientGraphNode> it = myancestors.iterator(); it.hasNext();)
			it.next().resetValue();
		for (Iterator<GradientGraphNode> it = myancestors.iterator(); it.hasNext();)
			it.next().evaluate();
	}
}
