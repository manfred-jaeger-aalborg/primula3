package RBNpackage;

import RBNExceptions.*;

public class CombFuncLLReg extends CombFunc{
	
	public CombFuncLLReg() {
		name = "ll-reg";
	}

	public  double evaluate(double[] args)
	{
		/* Returns 0 for empty argument! */


		double result = 0;
		double sum = 0;

		for (int i=0; i<args.length; i++)
		{
			sum = sum + args[i];
		}

		if (sum >= 0)
			result = sum/(1+sum);
		else throw new RBNRuntimeException ("Illegal application of combination function ll-reg to negative arguments");

		return result;
	}


	public int evaluatesTo(int[] args){
		return -1;
	}

}
