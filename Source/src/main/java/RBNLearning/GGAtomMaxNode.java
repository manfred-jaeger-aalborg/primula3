/*
* GGAtomMaxNode.java 
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



import java.util.Vector;

import myio.StringOps;

import RBNpackage.*;
import RBNutilities.SmallDouble;
import RBNExceptions.*;


public class GGAtomMaxNode extends GGAtomNode{

public static int USEMINSCORE = 0;
public static int USEAVGSCORE = 1;
public static int USELLSCORE = 2;

/* 
 * The value if this is instantiated by the evidence.
 * If mapinstVal!=-1, then no MAP inference is performed on 
 * this GGAtomMaxNode
 */
int mapInstVal=-1;


public int getmapInstVal() {
	return mapInstVal;
}

public void setmapInstVal(int v) {
	this.mapInstVal = v;
}

/** The current instantiation 
 * 
 */
int currentInst=-1;


public int getCurrentInst() {
	return currentInst;
}

public void setCurrentInst(int currentInst) {
	this.currentInst = currentInst;
}

///* A value that represents the contribution of this node with its
//* current instantiation value to the likelihood. Used as a selection
//* heuristic for flipping instantiation values during MAP inference  
//*/
//private double score;

/* The log-likelihood change induced by changing the current instantiation
 * value of this Atom to one of the alternative.
 * 
 * Note: the flipscores array is currently only produced as a by-product of
 * computing highscore/highvalue. May be used in future developments.
 */
private double[] flipscores;

/* The maximum flipscore value, without the flipscore corresponding to the current value
 * Thus, highscore can be <0 (if all possible changes of the current instantiation lead to a 
 * decrease in likelihood.
 */
private double highscore; 

/* The value for which highscore is obtained */
private int highvalue;

/* The index of this.myatom in the list GradientGraphO.maxatoms.
 * Needed in order to sort the GradientGraphIndicatorMaxNodes according to
 * the queryatom list in the InferenceModule 
 */
//private int index;



	public GGAtomMaxNode(GradientGraphO gg,
			ProbForm pf,  
			RelStruc A,
			OneStrucData I,
			int inputcasenoarg,
			int observcasenoarg)
	throws RuntimeException, RBNCompatibilityException
	{
		super(gg,pf,A,I,inputcasenoarg,observcasenoarg);
		gg.addToMaxIndicators(this);
		int tv = I.truthValueOf(myatom);
		if (tv !=-1) {
			this.setmapInstVal(tv);
			this.setCurrentInst(tv);
		}
	}

	public double getScore(){
		return highscore;
	}
	
	public int getHighvalue() {
		return highvalue;
	}
	
//	public void setIndex(int i){
//		index = i;
//	}
//	
//	public int getIndex(){
//		return index;
//	}
//	

	public void setScore(Thread mythread) {
		if (this.flipscores == null) { // First time we do MAP inference on this node
			flipscores = new double[(int)this.myatom().rel().numvals()];
		}
		
		double oldll = SmallDouble.log(thisgg.llnode.evaluate(null,allugas,true,false,null));
		double newll,fs;
		highscore = Double.NEGATIVE_INFINITY;
		highvalue = 0;
		int ci = this.currentInst; // Remember the current value
		for (int v=0; v< (int)this.myatom().rel().numvals(); v++) {
			if (v==ci) {
				fs=0.0;
			}
			else {
				this.setCurrentInst(v);

				// sample again the nodes after flipping. TODO: a more clever sampling could be implemented (sample only the parents of the flipped nodes)
				if (thisgg.sumindicators.size() > 0) {
					for (int j = 0; j < thisgg.windowsize; j++) {
						thisgg.gibbsSample(mythread);
					}
					System.out.println("New sampled values for setScore():");
					thisgg.showSumAtomsVals();
				}

				// trick for faster computation, we create only once the input for all the parents.
				// The parents will check only if the dictionaries are empty or not
				// if empty, create the input. Otherwise, keep the same input.
				// This assumes that maxatoms of CatRel have all the parents connected
				if (this.myatom.rel instanceof CatRel) {
					for (GGCPMNode ggcpmNode: this.parents()) {
						if (ggcpmNode instanceof GGGnnNode) {
							GGGnnNode gggnn = (GGGnnNode) ggcpmNode;
							gggnn.getGnnPy().resetDict();
						}
					}
				}

				reEvaluateUpstream(null);
				
				newll = SmallDouble.log(thisgg.llnode.evaluate(null,allugas,true,false,null));
				fs=newll-oldll;
				if (fs>highscore) {
					highscore = fs;
					highvalue = v;
				}
			}
			flipscores[v]=fs;
		}
		// Reset to original configuration
		this.setCurrentInst(ci);
		reEvaluateUpstream(null);
	}
	
//	public void setScore(int scoremode){
//
//		GGCPMNode nextuga;
//		double nextscore;
//		
//		if (scoremode == USELLSCORE){
//			System.out.println("Compute score for " + this.getMyatom());
//			
//			double[] oldvalues = new double[allugas.size()];
//			double oldll = GradientGraphO.computePartialLikelihood(allugas,oldvalues);
//			
//			
//			System.out.println("values for ugas: old="  
//			+ StringOps.arrayToString(oldvalues, "(", ")") );
//			toggleCurrentInst();
//			reEvaluateUpstream();
//			
//			double[] newvalues = new double[allugas.size()];		
//			double newll = GradientGraphO.computePartialLikelihood(allugas,newvalues);
//			
//			System.out.println("   new="  
//					+ StringOps.arrayToString(newvalues, "(", ")")  );
//			
//			toggleCurrentInst();
//			reEvaluateUpstream();
//			
//			score=0;
//			for (int i=0;i<allugas.size();i++){
//				score = score + Math.log( oldvalues[i]/newvalues[i]);
//			}
//			System.out.println("result = " + score);
//		}
//	}

	public Double[] evaluate(Integer sno) {
		return new Double[] {Double.valueOf(currentInst)};
	}

	public void addMeToIndicators(GGCPMNode ggpfn){
		ggpfn.addToMaxIndicators(this);
	}
	
	public void reEvaluateUpstream(Integer sno){
		super.reEvaluateUpstream(sno);
		// Also need to re-evaluate the upper ground atom node, which
		// usually is not an ancestor of this in the gradient graph
		this.getMyUga().resetValue(sno);
		this.getMyUga().evaluate(sno);
	}

	@Override
	public boolean isBoolean() {
		return !(myatom.rel() instanceof CatRel);
	}
	
	public void setRandomInst() {
		currentInst = (int)(Math.random()*myatom.rel().numvals());
	}
}
