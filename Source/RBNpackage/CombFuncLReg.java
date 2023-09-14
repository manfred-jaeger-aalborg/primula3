/*
 * CombFuncLReg.java
 * 
 * Copyright (C) 2003 Max-Planck-Institut fuer Informatik,
 *                    Helsinki Institute for Information Technology
 *
 * contact:
 * jaeger@cs.auc.dk   www.cs.auc.dk/~jaeger/Primula.html
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

package RBNpackage;

public  class CombFuncLReg extends CombFunc{

	/** Creates new CombFuncMean */
	public CombFuncLReg() {
		name = "l-reg";
	}

	public  double evaluate(double[] args)
	{
		/* Returns 0.5 for empty argument! */


		double result = 0;
		double sum = 0;

		for (int i=0; i<args.length; i++)
		{
			sum = sum + args[i];
		}

		
		if (sum > 100)
			result = 1; //Avoid overflow when evaluating Math.exp(sum)
		else
			result = Math.exp(sum)/(1+Math.exp(sum));

	
		return result;
	}


	public int evaluatesTo(int[] args){
		return -1;
	}
	
	  public double evaluateGrad(double[] vals, double[] derivs) {
			double sum = 0;
			double sumpr = 0;
			for (int i=0;i<vals.length;i++){
				sum = sum + vals[i];
				sumpr = sumpr + derivs[i];
			}
			double esum = Math.exp(sum);
			
			if (Double.isInfinite(Math.pow(1+esum,2)))
					return 0;
			double result = (esum*sumpr)/Math.pow(1+esum,2);
			
			return result;
			
	  }
}
