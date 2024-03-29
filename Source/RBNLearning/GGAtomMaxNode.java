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
import RBNExceptions.*;


public class GGAtomMaxNode extends GGAtomNode{

public static int USEMINSCORE = 0;
public static int USEAVGSCORE = 1;
public static int USELLSCORE = 2;

/* A value that represents the contribution of this node with its
 * current instantiation value to the likelihood. Used as a selection
 * heuristic for flipping instantiation values during MAP inference  
 */
private double score;

/* The index of this.myatom in the list GradientGraphO.maxatoms.
 * Needed in order to sort the GradientGraphIndicatorMaxNodes according to
 * the queryatom list in the InferenceModule 
 */
private int index;



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
	}

	public double getScore(){
		return score;
	}
	
	public void setIndex(int i){
		index = i;
	}
	
	public int getIndex(){
		return index;
	}
	

	public void setScore(int scoremode){

		GGProbFormNode nextuga;
		double nextscore;

//		if ((scoremode == USEMINSCORE || scoremode == USEAVGSCORE)){
//			score = 0;
//			for (int i=0; i<allugas.size();i++){
//				nextuga = allugas.elementAt(i);
//				nextscore = nextuga.value();
//				if (nextuga.instval()==1)
//					nextscore = nextuga.value();
//				if (nextuga.instval()==0)
//					nextscore = 1 - nextuga.value();
//				if (scoremode == USEMINSCORE)
//					score = Math.min(score, nextscore);
//				if (scoremode == USEAVGSCORE)
//					score = score + nextscore;
//			}
//			if (scoremode == USEAVGSCORE)
//				score = score/(1+allugas.size());
//		}
		
		if (scoremode == USELLSCORE){
			System.out.println("Compute score for " + this.getMyatom());
			
			double[] oldvalues = new double[allugas.size()];
			double oldll = GradientGraphO.computePartialLikelihood(allugas,oldvalues);
			
			
			
			System.out.println("values for ugas: old="  
			+ StringOps.arrayToString(oldvalues, "(", ")") );
			toggleCurrentInst();
			reEvaluateUpstream();
			
			double[] newvalues = new double[allugas.size()];		
			double newll = GradientGraphO.computePartialLikelihood(allugas,newvalues);
			
			System.out.println("   new="  
					+ StringOps.arrayToString(newvalues, "(", ")")  );
			
			toggleCurrentInst();
			reEvaluateUpstream();
			
			score=0;
			for (int i=0;i<allugas.size();i++){
				score = score + Math.log( oldvalues[i]/newvalues[i]);
			}
			System.out.println("result = " + score);
		}
	}

	public void addMeToIndicators(GGProbFormNode ggpfn){
		ggpfn.addToMaxIndicators(this);
	}
}
