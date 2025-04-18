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

import RBNpackage.*;
import RBNgui.*;
import RBNExceptions.*;


public abstract class GGAtomNode extends GGCPMNode{

	/** Ground atom represented by this node */
	GroundAtom myatom;
	
	/** Index of RelDataCase from which this ground atom derives (only relevant when 
	 * learning from data consisting of multiple observations for the same ground 
	 * atom)
	 */
	int inputcaseno;
	int observcaseno;




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
//		currentInst = -1;
		myuppergroundatom = null;
		allugas = new Vector<GGCPMNode>();
		if (!(pf instanceof ProbFormAtom)){
			System.out.println("Cannot create GGAtomNode from ProbForm " + pf.asString(Primula.CLASSICSYNTAX,0,null,false,false));
		}
		myatom = ((ProbFormAtom)pf).atom();
	}




//	public void evaluateBounds(){
//		switch(currentInst){
//		case -1:
//			bounds[0]=0;
//			bounds[1]=1;
//			break;
//		case 0:
//			bounds[0]=0;
//			bounds[1]=0;
//			break;
//		case 1:
//			bounds[0]=1;
//			bounds[1]=1;
//			break;
//		}
//	}


	
	public Gradient evaluateGradient(Integer sno){
		return thisgg.zerograd;
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
	
	

//	/** Sets the current instantiation according to 
//	 * the truth value tv
//	 */
//	public void setCurrentInst(int val){
//			currentInst = val;
//	}
//
//	public int getCurrentInst(){
//		return currentInst;
//	}
//
//	public void toggleCurrentInst(){
//		if (currentInst==1)
//			currentInst=0;
//		else
//			currentInst=1;
//	}

//	/** Resets the currentInst field to -1, i.e. node 
//	 * becomes un-instantiated
//	 */
//	public void unset(){
//		currentInst = -1;
//	}
	
	public void setUGA(GGCPMNode uga){
		myuppergroundatom = uga;
//		System.out.println("setUGA: setting  " + uga.getMyatom() + " as uga for " + this.getMyatom() );
	}
	
	public abstract void addMeToIndicators(GGCPMNode ggpfn);
	
	public void setAllugas(){
		TreeSet<GGNode> ancs = this.ancestors();
		for (GGNode nextggn: ancs){
			if (nextggn instanceof GGCPMNode && ((GGCPMNode)nextggn).isuga()){
				allugas.add((GGCPMNode)nextggn);
				addMeToIndicators((GGCPMNode)nextggn);
			}
		}
		addMeToIndicators(myuppergroundatom);
		allugas.add(myuppergroundatom); // TODO check if this works
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
