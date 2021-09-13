package mymath;

public class BinaryOptimizer {
	
	
	public BinaryOptimizer(){
	}
	
	/** find maximum of func insice [leftbound,rightbound] using binary search. 
	 * Unbounded search if leftbound = rightbound = null.
	 * Search starts at init.
	 * 
	 * Search terminates when optimum bounded inside interval of length <= precision
	 * 
	 * result[0] contains the maximizing argument
	 * result[1] contains the function value
	 * 
	 * @param func
	 * @param leftbound
	 * @param rightbound
	 * @param init
	 * @return
	 */
	public double[] binaryOptimization(BinaryOptimizable func, Double leftbound, Double rightbound, double init, double precision){
		double[] result = new double[2];
		/* Find initial left and right bounds */
		if (leftbound != null && init < leftbound)
			throw new IllegalArgumentException(" Initial point outside search bounds in binary optimization");
		if (rightbound != null && init > rightbound)
			throw new IllegalArgumentException(" Initial point outside search bounds in binary optimization");
		
		double currentleft = init;
		double nextleft;
		
		if (leftbound != null){
			nextleft = 0.5*(leftbound + currentleft);
		}
		else nextleft = currentleft - 2*Math.abs(currentleft);
		
		while (func.value(currentleft)<func.value(nextleft)){
			currentleft = nextleft;
			if (leftbound != null){
				nextleft = 0.5*(leftbound + currentleft);
			}
			else nextleft = currentleft - 2*Math.abs(currentleft);
		}
			
		double currentright = init;
		double nextright;
		
		if (rightbound != null){
			nextright = 0.5*(rightbound + currentright);
		}
		else nextright = currentright + 2*Math.abs(currentright);
		
		while (func.value(currentright)<func.value(nextright)){
			currentright = nextright;
			if (rightbound != null){
				nextright = 0.5*(rightbound + currentright);
			}
			else nextright = currentright + 2*Math.abs(currentright);
		}
	
		double midleftarg,midrightarg;
		double midleftval,midrightval;
		
		while (rightbound-leftbound > precision){
			
			midleftarg = 0.25*rightbound+0.75*leftbound;
			midrightarg = 0.75*rightbound+0.25*leftbound;
			midleftval = func.value(midleftarg);
			midrightval = func.value(midrightarg);
//			System.out.println("bounds: [" + leftbound + "," + rightbound +"]" + " middlevalues: " + midleftval +"," + midrightval);
			if (midleftval < midrightval){
				leftbound = midleftarg;
			}
			else rightbound = midrightarg;
		}
		
		double leftval  = func.value(leftbound);
		double rightval  = func.value(rightbound);
		
		if (leftval>rightval){
			result[0]=leftbound;
			result[1]=leftval;
		}
		else{
			result[0]=rightbound;
			result[1]=rightval;
		}
		return result;
		
	}
}
