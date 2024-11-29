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

import RBNpackage.*;
import RBNExceptions.*;


public class GGAtomSumNode extends GGAtomNode{


	/** Represents sample values for this node.
	 * 
	 * Summation over all configurations of IndicatorSumNodes is approximated
	 * by summation over the configurations defined by the sampledVals.
	 * 
	 * Array of length numchains*windowsize (as defined in GradientGraph)
	 * Values for the k'th chain are stored at indices k*windowsize,...,(k+1)*windowsize-1
	 * The oldest sample for chain k is stored at k*windowsize+windowindex, where windowindex is 
	 * defined by the GradientGraph. New samples overwrite the oldest sample (cyclic in the
	 * part of the array allocated to the chain)
	 * 
	 */


	public GGAtomSumNode(GradientGraphO gg,
			ProbForm pf,  
			RelStruc A,
			OneStrucData I,
			int inputcasenoarg,
			int observcasenoarg)
	throws RuntimeException, RBNCompatibilityException
	{
		super(gg,pf,A,I,inputcasenoarg,observcasenoarg);
		depends_on_sample = true;
		gg.addToSumIndicators(this);
	}


//	/** Sets the current instantiation according to 
//	 * the value in the sno's sample
//	 */
//	public void setCurrentInst(int sno){
//		currentInst = values_for_samples[sno][0].intValue();
//	}

	/** Sets value in sno's sample to tv */
	public void setSampleVal(int sno, int val){
		values_for_samples[sno] = new Double[] {(double) val};
	}

//	/** Sets value in sno's sample to current instantiation */
//	public void setSampleVal(int sno){
//		setSampleVal(sno,currentInst);
//	}

	/** Toggles value in sno's sample */
	public void toggleSampleVal(int sno){
		values_for_samples[sno][0] = 1 - values_for_samples[sno][0];
	}


	public void addMeToIndicators(GGCPMNode ggpfn){
		ggpfn.addToSumIndicators(this);
	}

	public Double[] evaluate(Integer sno){
		if (sno==null) {
			System.out.println("Called GGAtomSumNode.evaluate without sample number!");
			return null;
		}
		else
			return values_for_samples[sno];
	}

	@Override
	public boolean isBoolean() {
		return !(myatom.rel() instanceof CatRel);
	}
	
	public int[] getSampledVals() {
		int[] result  = new int[values_for_samples.length];
		for (int i=0;i<result.length;i++)
			result[i]=values_for_samples[i][0].intValue();
		return result;
	}
	
	public int getSampledVals(int sno) {
		return values_for_samples[sno][0].intValue();
	}
}
