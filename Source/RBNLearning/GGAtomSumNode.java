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


public class GGAtomSumNode extends GGAtomNode{


	/** Represents sample values for this node.
	 * 
	 * Summation over all configurations of IndicatorSumNodes is approximated
	 * by summation over the configurations defined by the sampledVals.
	 */
	//boolean[] sampledVals;

	public GGAtomSumNode(GradientGraphO gg,
			ProbForm pf,  
			RelStruc A,
			OneStrucData I,
			int inputcasenoarg,
			int observcasenoarg)
	throws RuntimeException, RBNCompatibilityException
	{
		super(gg,pf,A,I,inputcasenoarg,observcasenoarg);
		gg.addToSumIndicators(this);
	}


	/** Sets the current instantiation according to 
	 * the value in the sno's sample
	 */
	public void setCurrentInst(int sno){
		currentInst = (int)values_for_samples[sno];
	}

	/** Sets value in sno's sample to tv */
	public void setSampleVal(int sno, int val){
		values_for_samples[sno]=val;
	}

	/** Sets value in sno's sample to current instantiation */
	public void setSampleVal(int sno){
		setSampleVal(sno,currentInst);
	}

	/** Toggles value in sno's sample */
	public void toggleSampleVal(int sno){
		values_for_samples[sno]=1-values_for_samples[sno];
	}


//	/** initializes  sampledVals to an array of size 'size' */
//	public void initSampledVals(int size){
//			sampledVals = new boolean[size];
//	}

	public void addMeToIndicators(GGCPMNode ggpfn){
		ggpfn.addToSumIndicators(this);
	}

}
