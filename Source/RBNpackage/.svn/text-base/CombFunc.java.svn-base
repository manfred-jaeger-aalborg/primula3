package RBNpackage;

public abstract class CombFunc extends Object 
{
	/**
	 * @uml.property  name="name"
	 */
	public String name;

	public static final int NOR = 0;
	public static final int MEAN = 1;
	public static final int INVSUM = 2;
	public static final int ESUM = 3;
	public static final int LREG = 4;
	public static final int LLREG = 5;
	public static final int SUM = 6;
	public static final int PROD = 7;
	
	public static boolean isCombFuncName(String str){
		boolean result = false;
		if (str.equals("mean")) result = true;
		if (str.equals("n-or")) result = true;
		if (str.equals("invsum")) result = true;
		if (str.equals("esum")) result = true;
		if (str.equals("l-reg")) result = true;
		if (str.equals("ll-reg")) result = true;
		if (str.equals("sum")) result = true;
		if (str.equals("prod")) result = true;
		return result;
	}

	public abstract  double evaluate(double[] args);

	/* args is a vector with 1,0,-1 entries.
	 * checks whether any vector that has a 1 where
	 * args is 1, a 0 where args is 0, and some
	 * arbitrary value where args is -1 will be 
	 * evaluated to 1 (return 1), to 0 (return 0),
	 * or is not guaranteed to evaluate to either
	 * 1 or 0 (return -1)
	 */
	public  abstract int evaluatesTo(int[] args);


}