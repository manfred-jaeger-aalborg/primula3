/*
* GGConstantNode.java 
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


public class GGConstantNode extends GGCPMNode{

	/** True if this is a parameter to be learned */
	Boolean isUnknown;

	/** The value of this constant if it is known */
	double cval;

	/** Current parameter estimate if isUnknown = true */
	double currentParamVal;

	/** The name of this parameter if isUnknown = true */
	String paramname;

	public GGConstantNode(GradientGraphO gg,ProbForm pf,RelStruc A, OneStrucData data)
	throws RBNCompatibilityException
	{
		super(gg,pf,A,data);
		
		if (!(pf instanceof ProbFormConstant)){
			System.out.println("Cannot create GGConstantNode from ProbForm " + pf.asString(Primula.CLASSICSYNTAX,0,null,false,false));
		}
		
		String parname = ((ProbFormConstant)pf).getParamName();
		
		if (parname != ""){ // This represents an unknown parameter
			isUnknown = true;
			cval = 0;
			currentParamVal = 0.5;
			values_for_samples = new double[1][];
			values_for_samples[0]=new double[] {currentParamVal};
		}
		else{ // This represents a known constant 
			isUnknown = false;
			cval = ((ProbFormConstant)pf).getCval();
			currentParamVal = 0.5;
		}
		paramname = parname;

		
	}

	public double[] evaluate(Integer sno){
		double result = 0;
		if (!isUnknown){
			result = cval;
		}
		else{
			result = currentParamVal;
			if (Double.isNaN(currentParamVal))
				System.out.println("evaluate constant for  " + this.paramname()+  " gives " + currentParamVal);
		}

		values_for_samples[0] = new double[]{result};
		return values_for_samples[0];
	}

//	public void evaluateBounds(){
//		//	System.out.println("constantnode.evaluateBounds with currentParamVal " + currentParamVal);
//		if (!isUnknown){
//			bounds[0]=cval;
//			bounds[1]=cval;
//		}
//		else{
//			bounds[0]=currentParamVal;
//			bounds[1]=currentParamVal;
//		}
//	}

//	public double evaluateGrad(int param){
//		if (isUnknown){
//			if (paramname.equals(thisgg.parameterAt(param))){
//				gradient[param] = 1.0;
//				return 1.0;
//			}
//			else {
//				gradient[param] = 0.0;
//				return 0.0;
//			}
//		}
//		else {
//			gradient[param] = 0.0;
//			return 0.0;
//		}
//	}

	public Gradient evaluateGradient(Integer idx){

		if (!isUnknown)
			return thisgg.zerograd;

		if (is_evaluated_grad_for_samples[idx])
			return gradient_for_samples.get(idx);

		/* Much redundancy here, because the gradient always is the same
		 */
		Gradient result = gradient_for_samples.get(idx);
		result.reset();

		result.set_part_deriv(paramname, new double[] {1.0});

		is_evaluated_grad_for_samples[idx] = true;
		return result;
	}

	public String paramname(){
		return paramname;
	}

	public void setparamname(String pn){
		paramname = pn;
	}
	
	public void setCurrentParamVal(double cv){
		currentParamVal = cv;
		/* The following assumes that setCurrentParamVal is only called when 
		 * isUnknown=true 
		 */
		values_for_samples[0] = new double[]{currentParamVal};
	}

	public double getCurrentParamVal(){
		return currentParamVal;
	}

	@Override
	public boolean isBoolean() {
		return true;
	}
}
