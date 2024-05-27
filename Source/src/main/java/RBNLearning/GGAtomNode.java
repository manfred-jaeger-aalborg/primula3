/*
* GGAtomNode.java 
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


public abstract class GGAtomNode extends GGCPMNode{

	/** Ground atom represented by this node */
	GroundAtom myatom;
	
	/** Index of RelDataCase from which this ground atom derives (only relevant when 
	 * learning from data consisting of multiple observations for the same ground 
	 * atom)
	 */
	int inputcaseno;
	int observcaseno;


	/** The current instantiation for this atom (used for computing
	 * likelihood at the root of the gradient graph);
	 * currentInst = -1 if not currently instantiated
	 */
	int currentInst;

	/* Pointer to the Upper Ground Atom Node defining the probability of this.myatom 
	 * 
	 */
	protected GGCPMNode myuppergroundatom;
	
	/* The set of all Upper Ground Atom Nodes whose value depend on the 
	 * value of this indicator. The converse of GGProbFormNode.mymaxindicators 
	 * and GGProbFormNode.mysumindicators 
	 */
	protected Vector<GGCPMNode> allugas;
	
	public GGAtomNode(GradientGraphO gg,
			ProbForm pf,  
			RelStruc A,
			OneStrucData I,
			int inputcasenoarg,
			int observcasenoarg)
	throws RuntimeException, RBNCompatibilityException
	{
		super(gg,pf,A,I);
		inputcaseno = inputcasenoarg;
		observcaseno = observcasenoarg;
		currentInst = -1;
		myuppergroundatom = null;
		allugas = new Vector<GGCPMNode>();
		if (!(pf instanceof ProbFormAtom)){
			System.out.println("Cannot create GGAtomNode from ProbForm " + pf.asString(Primula.CLASSICSYNTAX,0,null,false,false));
		}
		myatom = ((ProbFormAtom)pf).atom();
	}


	public double evaluate(Integer sno){
		/*
		 * A GGAtomNode that also is uga would mean that one relation is defined as a copy
		 * of another relation, i.e., in the rbn:
		 * 
		 * rel1(v) = rel2(v);
		 * 
		 * So this case should rarely ever happen ...
		 */
		if (this.isuga()) {
			int iv = this.instval(); // Can only be 0,1, or -1, because if a relation is defined by ProbFormAtom
			                         // it can only be Boolean
			if (iv == -1)
				System.out.println("Warning: undefined instantiation value in GGAtomNode.evaluate()");
			if (iv == 0)
				return 1-currentInst;
			if (iv == 1)
				return currentInst;
		}
		
		return currentInst;
	}


	public void evaluateBounds(){
		switch(currentInst){
		case -1:
			bounds[0]=0;
			bounds[1]=1;
			break;
		case 0:
			bounds[0]=0;
			bounds[1]=0;
			break;
		case 1:
			bounds[0]=1;
			bounds[1]=1;
			break;
		}
	}

	public double evaluateGrad(String param){
		return 0.0;
	}

	public GroundAtom myatom(){
		return myatom;
	}

	public String getMyatom(){
		return myatom.asString();
	}
	
	public int inputcaseno(){
		return inputcaseno;
	}
	
	public int observcaseno(){
		return observcaseno;
	}
	
	

	/** Sets the current instantiation according to 
	 * the truth value tv
	 */
	public void setCurrentInst(int val){
			currentInst = val;
	}

	public int getCurrentInst(){
		return currentInst;
	}

	public void toggleCurrentInst(){
		if (currentInst==1)
			currentInst=0;
		else
			currentInst=1;
	}

	/** Resets the currentInst field to -1, i.e. node 
	 * becomes un-instantiated
	 */
	public void unset(){
		currentInst = -1;
	}
	
	public void setUGA(GGCPMNode uga){
		myuppergroundatom = uga;
		//System.out.println("setUGA: setting  " + uga.getMyatom() + " as uga for " + this.getMyatom() );
	}
	
	public abstract void addMeToIndicators(GGCPMNode ggpfn);
	
	public void setAllugas(){
		TreeSet<GGNode> ancs = this.ancestors();
		GGNode nextggn;
		for (Iterator<GGNode> it = ancs.iterator(); it.hasNext();){
			nextggn = it.next();
			if ((nextggn instanceof GGCPMNode) && ((GGCPMNode)nextggn).isuga()){
				allugas.add((GGCPMNode)nextggn);
				addMeToIndicators((GGCPMNode)nextggn);
			}
		}
		addMeToIndicators(myuppergroundatom);
		allugas.add(myuppergroundatom);
	}
	
	public Vector<GGCPMNode> getAllugas(){
		return allugas;
	}
	
	public GGCPMNode getMyUga(){
		return myuppergroundatom;
	}
	
	public void printAllUgas(){
		GGCPMNode nextuga;
		System.out.print("My own uga: ");
		System.out.println(myuppergroundatom.getMyatom());
		System.out.println("My other ugas:  " );
		for (Iterator<GGCPMNode> it = allugas.iterator(); it.hasNext();){
			nextuga = it.next();
			System.out.print("UGA: ");
			System.out.println(nextuga.getMyatom());
		}
	}

}
