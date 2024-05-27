package RBNpackage;

public class CombFuncProd extends CombFunc {

	public CombFuncProd() {
		name = "prod";
	}

	@Override
	public double evaluate(double[] args) {
		double prod = 1;
		for (int i=0; i<args.length; i++) {
			if (args[i]==0)
				return 0;
			prod = prod * args[i];
		}
		return prod;
	}

	@Override
	public int evaluatesTo(int[] args) {

		if (args.length == 0 ) return 1;
		if (args.length ==1 && args[0]==1) return 1;
		else{
			boolean existzero = false;
			boolean allones = true;
			for (int i=0;i<args.length;i++){
				if (args[i]!=1) allones = false;
				if (args[i]==0) existzero = true;
			}
			if (allones) return 1;
			if (existzero) return 0;
			return -1;
		}
	}

	  public double evaluateGrad(double[] vals, double[] derivs) {
			double result = 0;
	        /* First compute \prod Fi over all subformulas */
			
			/* Need special consideration of the case where exactly one of the vals is zero */
			int zerocount = 0;
			int zeroidx = 0; // init value irrelevant
			
	        double factor = 1;
	        
	        for (int i=0;i<vals.length;i++) {
	        	if (vals[i]==0) {
	        		zerocount ++;
	        		zeroidx = i;
	        	}
	        	else
	        		factor = factor*vals[i];
	        }
	        
	        if (zerocount > 1)
	        	return 0.0;
	        
	        if (zerocount ==1)
	        	return factor*derivs[zeroidx];
	        
	        /* Now compute the partial derivative as
	         *
	         * \sum_{F_i\in fthetalist} (factor/F_i)*(F_i')
	         */
	        for (int i=0;i<vals.length;i++){
	                        result = result + (factor/vals[i])*(derivs[i]);
	        }
			return result;
	  }
}
