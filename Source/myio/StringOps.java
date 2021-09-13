package myio;
import mymath.*;
import java.io.*;
import java.util.*;

public class StringOps{

	public static String arrayToString(double[] arr, String leftpar, String rightpar){
		String result = leftpar ;
		for (int i=0;i<arr.length-1;i++) result = result + arr[i] + ",";
		if (arr.length>0) result=result+arr[arr.length-1];
		result = result + rightpar;
		return result;
	}

	public static String arrayToString(double[] arr, int digits, String leftpar, String rightpar){ 
		String result = leftpar;
		for (int i=0;i<arr.length-1;i++) result = result + MyMathOps.formatDouble(arr[i],digits) + ", ";
		if (arr.length>0) result=result+MyMathOps.formatDouble(arr[arr.length-1],digits);
		result = result + rightpar;
		return result;
	}

	public static DoubleVector stringToDoubleVector(String str){
		DoubleVector result = new DoubleVector();
		try{
			StreamTokenizer tokenzer = new StreamTokenizer(new StringReader(str));
			tokenzer.resetSyntax();
			tokenzer.wordChars(47,57); // 0-9
			tokenzer.wordChars(46,46); // .
			tokenzer.whitespaceChars(44,44); // ,
			tokenzer.whitespaceChars(91,91); // [
			tokenzer.whitespaceChars(93,93); // ]

			tokenzer.parseNumbers();

			double nextdouble;
			while (tokenzer.nextToken() != StreamTokenizer.TT_EOF){
				nextdouble = tokenzer.nval;
				//System.out.println(nextdouble);
				result.add(nextdouble);
			}
		}
		catch (java.io.IOException e){System.out.println(e);};
		return result;
	}

	
	public static double[] stringToDoubleArray(String str){
		return stringToDoubleVector(str).asArray();
	}
	
	public static int[] stringToIntegerArray(String str){
		return stringToIntegerVector(str).asArray();
	}
	
	/* If e.g. arity=2, expects a string (s_1,s_2)(s_3,s_4)(s_5,s_6)
	 * and returns the 2-dim array [[s_1,s_2],[s_3,s_4][s_5,s_6]]
	 * 
	 */
//	public static String[][] stringToStringMatrix(String str){
//		Vector<String[]> tuples = new Vector<String[]>();
//		while (str.length()!=0){
//			String firsttup = str.substring(0, str.indexOf(")")+1);
//			str = str.substring(str.indexOf(")")+1,str.length());
//			tuples.add(stringToStringArray(firsttup));
//		}
//		String[][] result = new String[tuples.size()][];
//		int i=0;
//		for (Iterator<String[]> it = tuples.iterator(); it.hasNext(); ){
//			String[] nexttup = it.next();
//			result[i]=nexttup;
//			i++;
//		}
//		return result;
//	}
//	
	/* If e.g. arity=2, expects a string (s_1,s_2)(s_3,s_4)(s_5,s_6)
	 * and returns the 2-dim array [[s_1,s_2],[s_3,s_4][s_5,s_6]]
	 * 
	 */
	public static String[][] stringToStringMatrix(String str){
		String[] strarr = str.split("\\)");
		String[][] result = new String[strarr.length][];
		for (int i=0;i<strarr.length;i++) {
			result[i]=stringToStringArray(strarr[i]+")");
		}
		return result;
	}
	
	
	
	public static IntVector stringToIntegerVector(String str){
		IntVector result = new IntVector();

		try{
			StreamTokenizer tokenzer = new StreamTokenizer(new StringReader(str));
			tokenzer.resetSyntax();
			tokenzer.wordChars(47,57); // 0-9
			tokenzer.whitespaceChars(44,44); // ,
			tokenzer.whitespaceChars(46,46); // .
			tokenzer.whitespaceChars(65,90); // A-Z
			tokenzer.whitespaceChars(97,122); // a-z
			tokenzer.whitespaceChars(91,91); // [
			tokenzer.whitespaceChars(93,93); // ]
			tokenzer.whitespaceChars(40,41); // ()
			tokenzer.whitespaceChars(61,61); // =

			tokenzer.parseNumbers();

			int nextint;
			while (tokenzer.nextToken() != tokenzer.TT_EOF){
				nextint = (int)tokenzer.nval;
				//System.out.println(nextdouble);
				result.add(nextint);
			}
		}
		catch (java.io.IOException e){System.out.println(e);};
		return result;
	}

	public static int[] stringToIntArray(String str){
		return stringToIntegerVector(str).asArray();
	}
	
	public static String arrayToString(int[] arr, String leftpar, String rightpar){
		String result = leftpar;
		for (int i=0;i<arr.length-1;i++) result = result + arr[i] + ",";
		if (arr.length>0) result=result+arr[arr.length-1];
		result = result + rightpar;
		return result;
	}


	public static String arrayToString(String[] arr, String leftpar, String rightpar){
		String result = leftpar;
		for (int i=0;i<arr.length-1;i++) result = result + arr[i] + ",";
		if (arr.length>0) result=result+arr[arr.length-1];
		result = result + rightpar;
		return result;
	}

	public static String arrayToStringParenth(String[] arr, String leftpar, String rightpar){
		String result = leftpar;
		for (int i=0;i<arr.length-1;i++) result = result + arr[i] + ",";
		if (arr.length>0) result=result+arr[arr.length-1];
		result = result + rightpar;
		return result;
	}

	public static String matrixToString(int[][] mat, String leftpar, String rightpar){
		String result = "";
		for (int i=0;i<mat.length;i++)
			result = result + arrayToString(mat[i],leftpar,rightpar) + '\n';
		return result;
	}

	public static String matrixToString(double[][] mat, String leftpar, String rightpar){
		String result = "";
		for (int i=0;i<mat.length;i++)
			result = result + arrayToString(mat[i],leftpar,rightpar) + '\n';
		return result;
	}

	public static double[] sortArrayCopy(double[] ar){
		// mergesort. returns new array
		double[] result = new double[ar.length];
		if (ar.length > 1){
			int halflength = ar.length/2;
			double[] lar = new double[halflength];
			double[] rar = new double[ar.length-halflength];
			for (int i=0;i<halflength;i++){
				lar[i]=ar[i];
			}
			for (int i=0;i<ar.length-halflength;i++){
				rar[i]=ar[halflength+i];
			}
			double[] lsorted = sortArrayCopy(lar);
			double[] rsorted = sortArrayCopy(rar);
			int lc=0;
			int rc=0;
			for (int c=0;c<ar.length;c++){
				if (rc==rar.length || (lc < lar.length && lar[lc]<=rar[rc])){
					result[c]=lar[lc];
					lc++;
				}
				else{
					result[c]=rar[rc];
					rc++;
				}
			}
		}
		else{
			result[0]=ar[0];
		}
		return result;
	}

	public static void sortArray(double[] ar){
		// mergesort. sorts the argument array
		if (ar.length > 1){
			int halflength = ar.length/2;
			double[] lar = new double[halflength];
			double[] rar = new double[ar.length-halflength];
			for (int i=0;i<halflength;i++){
				lar[i]=ar[i];
			}
			for (int i=0;i<ar.length-halflength;i++){
				rar[i]=ar[halflength+i];
			}
			sortArray(lar);
			sortArray(rar);
			int lc=0;
			int rc=0;
			for (int c=0;c<ar.length;c++){
				if (rc==rar.length || (lc < lar.length && lar[lc]<=rar[rc])){
					ar[c]=lar[lc];
					lc++;
				}
				else{
					ar[c]=rar[rc];
					rc++;
				}
			}
		}
	}

	public static void sortArray(int[] ar){
		// mergesort. sorts the argument array
		if (ar.length > 1){
			int halflength = ar.length/2;
			int[] lar = new int[halflength];
			int[] rar = new int[ar.length-halflength];
			for (int i=0;i<halflength;i++){
				lar[i]=ar[i];
			}
			for (int i=0;i<ar.length-halflength;i++){
				rar[i]=ar[halflength+i];
			}
			sortArray(lar);
			sortArray(rar);
			int lc=0;
			int rc=0;
			for (int c=0;c<ar.length;c++){
				if (rc==rar.length || (lc < lar.length && lar[lc]<=rar[rc])){
					ar[c]=lar[lc];
					lc++;
				}
				else{
					ar[c]=rar[rc];
					rc++;
				}
			}
		}
	}


	public static int[] indexToTuple(int ind, int dim, int range)
	/* returns the tuple i_0,i_2,...,i_(dim-1) 
	 * that occurs in place 'ind' in a 
	 * lexicographic enumeration of all 
	 * 'dim'-tuples of integers in the range 
	 * [0..range-1]
	 * 0 <= ind <= range^dim 
	 */
	{
		int [] result = new int[dim];
		for (int i=0; i<dim; i++)
		{
			result[dim-1-i] = ind % range;
			ind = ind/range;
		}

		return result;
	}


	public static int tupleToIndex(int[] tuple, int range)
	/* returns the index of 'tuple': i_0,i_2,...,i_(tuple.length-1)
	 * in an enumeration of all tuples over range [0..range-1]
	 * Error if i_j>range-1 for some j
	 */
	{
		int result = 0;
		int k = tuple.length;
		int factor = 1;


		for (int i = 0; i < k  ; i++)
		{
			result = result + (tuple[k-1-i])*factor;
			factor = factor * range;
		}

		return result;
	}

	public static double[] intArrayToDoubleArray(int[] intarray){
		double[] result = new double[intarray.length];
		for (int i=0;i<result.length;i++)
			result[i]=(double)intarray[i];
		return result;
	}

	public static String doubleConverter(String oldstring)
	/* Converts oldstring representation of a double number
	 * into a shorter string
	 */
	{
		int indexE = oldstring.lastIndexOf("E");
		int indexdot = oldstring.lastIndexOf(".");
		int max = 0;
		String newstring;
		if(indexE == -1){
			max = Math.min(oldstring.length(),indexdot+5);
			newstring = oldstring.substring(0,max);
		}
		else{
			newstring = oldstring.substring(0,indexdot+3)+oldstring.substring(indexE,oldstring.length());
		}
		return newstring;

	}

	public static String doubleConverter(double d){
		return doubleConverter(""+d);
	}

	/* Converts a str of the form "(str1,...,strk)" into an array
	 * with entries str1,...,strk (input can also use different kind
	 * of parentheses)
	 */
	public static String[] stringToStringArray(String str){
		/* First remove parantheses */
//		boolean iszero = false;
//		if (str.equals("()")){
//			System.out.println("string to array with empty ()");
//			iszero = true;
//		}
		str = str.substring(1,str.length()-1);
		Vector<String> svec = new Vector<String>();
		int nextcomma;
		while (str.length()>0){
			nextcomma = str.indexOf(",");
			if (nextcomma != -1){
				svec.add(str.substring(0,nextcomma));
				str = str.substring(nextcomma+1);
			}
			else{
				svec.add(str);
				str ="";
			}
		}
		String[] result = new String[svec.size()];
		for (int i=0;i<result.length;i++){
			result[i]=svec.elementAt(i);
		}
//		if (iszero)
//			System.out.println("returned array of length" + result.length);
		return result;
	}
	
	public static String[] stringToStringArray(String str, int separator){
		/* First remove parantheses */
		str = str.substring(1,str.length()-1);
		Vector<String> svec = new Vector<String>();
		int nextcomma;
		while (str.length()>0){
			nextcomma = str.indexOf(separator);
			if (nextcomma != -1){
				svec.add(str.substring(0,nextcomma).trim());
				str = str.substring(nextcomma+1);
			}
			else{
				svec.add(str.trim());
				str ="";
			}
		}
		String[] result = new String[svec.size()];
		for (int i=0;i<result.length;i++){
			result[i]=svec.elementAt(i);
		}
		return result;
	}

	
//	public static String vectorToString(Vector<String> vec){
//		String result = "";
//		if (vec.size()>0)
//			result = vec.elementAt(0);
//		for (int i=1;i<vec.size();i++){
//			result = result + ","+ vec.elementAt(i);
//		}
//		return result;
//	}

	public static String[] stringVectorToArray(Vector<String> vec){
		String[] result = new String[vec.size()];
		for (int i=0;i<result.length;i++)
			result[i]=vec.elementAt(i);
		return result;

	}
	
	   public static int[][] intarrVectorToArray(Vector<int[]> vec){
	        int[][] result = new int[vec.size()][];
	        for (int i=0;i<result.length;i++)
	        	result[i]=vec.elementAt(i);
	        return result;
	        }

	    public static int[] intVectorToArray(Vector<Integer> vec){
	        int[] result = new int[vec.size()];
	        for (int i=0;i<result.length;i++)
	        	result[i]=vec.elementAt(i);
	        return result;
	        }

	    public static double[] doubleVectorToArray(Vector<Double> vec){
	        double[] result = new double[vec.size()];
	        for (int i=0;i<result.length;i++)
	        	result[i]=vec.elementAt(i);
	        return result;
	        }

		public static String[][] vectorTo2DArray(Vector<String[]> vec){
			String[][] result = new String[vec.size()][];
			for (int i=0;i<result.length;i++)
				result[i]=vec.elementAt(i);
			return result;

		}
}
