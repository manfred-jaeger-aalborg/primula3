
/*
 * SmallDouble.java 
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

package RBNutilities;

import java.util.*;

/** Provides static methods for handling very small double values d
 * represented as pairs d[0],d[1]. The value of d is equal
 * to d[0]*1E-d[1]
 */
public class SmallDouble implements Comparator{

	public SmallDouble(){
	}

	public static double[] asSmallDouble(double d){
		double[] sd = new double[2];
		sd[0]=d;
		sd[1]=0;
		return sd;
	}
	
	public static Double[] asSmallDDouble(double d){
		Double[] sd = new Double[2];
		sd[0]=d;
		sd[1]=0.0;
		return sd;
	}

	public static double[] add(double[] sd1, double[] sd2){
		//System.out.print("add " + rbnutilities.arrayToString(sd1) + " " + rbnutilities.arrayToString(sd2));
		if (sd1[0]==0)
			return sd2.clone();
		if (sd2[0]==0)
			return sd1.clone();
		double[] result = new double[2];
		if (sd1[1]<=sd2[1]){
			result[0]=sd1[0]+sd2[0]*Math.pow(10,-sd2[1]+sd1[1]);
			result[1]=sd1[1];
		}
		else{
			result[0]=sd2[0]+sd1[0]*Math.pow(10,-sd1[1]+sd2[1]);
			result[1]=sd2[1];
		}
		return result;
	}

	
	public static Double[] add(Double[] sd1, Double[] sd2){
		//System.out.print("add " + rbnutilities.arrayToString(sd1) + " " + rbnutilities.arrayToString(sd2));
		if (sd1[0]==0)
			return sd2.clone();
		if (sd2[0]==0)
			return sd1.clone();
		Double[] result = new Double[2];
		if (sd1[1]<=sd2[1]){
			result[0]=sd1[0]+sd2[0]*Math.pow(10,-sd2[1]+sd1[1]);
			result[1]=sd1[1];
		}
		else{
			result[0]=sd2[0]+sd1[0]*Math.pow(10,-sd1[1]+sd2[1]);
			result[1]=sd2[1];
		}
		return result;
	}
	
	public static double[] sumArray(double[][] sdarr) {
		/*
		 * sdarr is an n x 2 array of small doubles; Returns the sum 
		 * over all array elements.
		 */
		double[] result = new double[2];
		for (int i=0;i<sdarr.length;i++)
			result=SmallDouble.add(result, sdarr[i]);
		return result;
	}

	public static double[][] addArray(double[][] sdarr1, double[][] sdarr2) {
		/*
		 * adds componentwise two n x 2 arrays of small doubles; 
		 */
		
		double[][] result = new double[sdarr1.length][2];
		for (int i=0;i<sdarr1.length;i++)
			result[i]=SmallDouble.add(sdarr1[i], sdarr2[i]);
		return result;
	}
	
	public static double[] multiply(double[] sd1, double[] sd2){
		double[] result = new double[2];
		double mainprod = sd1[0]*sd2[0];
		int addtofactor=0;
		if (mainprod<1.0E-20 && mainprod>0){
			mainprod=mainprod*1.0E20;
			addtofactor=20;
		}
		result[0]=mainprod;
		result[1]=sd1[1]+sd2[1]+addtofactor;
		return result;
	}

	public static double[] multiply(double[] sd1, Double[] sd2){
		double[] result = new double[2];
		double mainprod = sd1[0]*sd2[0];
		int addtofactor=0;
		if (mainprod<1.0E-20 && mainprod>0){
			mainprod=mainprod*1.0E20;
			addtofactor=20;
		}
		result[0]=mainprod;
		result[1]=sd1[1]+sd2[1]+addtofactor;
		return result;
	}
	
	public static Double[] multiply(Double[] sd1, Double[] sd2){
		Double[] result = new Double[2];
		double mainprod = sd1[0]*sd2[0];
		int addtofactor=0;
		if (mainprod<1.0E-20 && mainprod>0){
			mainprod=mainprod*1.0E20;
			addtofactor=20;
		}
		result[0]=mainprod;
		result[1]=sd1[1]+sd2[1]+addtofactor;
		return result;
	}
	
	public static double[] multiply(double[] sd1, double d2){
		double sd2[]={d2,0};
		return multiply(sd1,sd2);
	}

	public static Double[] multiply(Double[] sd1, double d2){
		Double sd2[]={d2,0.0};
		return multiply(sd1,sd2);
	}
	
	/** computes sd1/sd2 */
	public static double[] divide(double[] sd1, double[] sd2){
		//System.out.print("divide " + rbnutilities.arrayToString(sd1) + " " + rbnutilities.arrayToString(sd2));
		double[] result = new double[2];
		double mainquot = sd1[0]/sd2[0];
		int addtofactor=0;
		if (mainquot<1.0E-20 && mainquot >0){
			mainquot=mainquot*1.0E20;
			addtofactor=20;
		}
		result[0]=mainquot;
		result[1]=sd1[1]-sd2[1]+addtofactor;
		//	if (Double.isNaN(result[0]) || Double.isInfinite(result[0]))
		//		System.out.println("  in " + StringOps.arrayToString(sd1,"[","]") + " " + StringOps.arrayToString(sd2,"[","]")); 
		return result;
	}

	public static double[][] divide(double[][] sdarray, double[] sd){
		/* Divides array of small doubles component-wise by small double
		 * 
		 */
		double[][] result = new double[sdarray.length][2];
		for (int i=0;i<sdarray.length;i++)
			result[i]=SmallDouble.divide(sdarray[i], sd);
		return result;
	}

	public static double[][] divide(double[][] sdarray, int i2){
		/* Divides array of small doubles component-wise by int i2
		 * 
		 */
		double[][] result = new double[sdarray.length][2];
		for (int i=0;i<sdarray.length;i++)
			result[i]=SmallDouble.divide(sdarray[i], i2);
		return result;
	}
	public static double[][] divide(double[][] sdarray, double i2){
		/* Divides array of small doubles component-wise by double i2
		 *
		 */
		double[][] result = new double[sdarray.length][2];
		for (int i=0;i<sdarray.length;i++)
			result[i]=SmallDouble.divide(sdarray[i], i2);
		return result;
	}

	public static double[] divide(double[] sd1, double d2){

		double sd2[]={d2,0};
		return divide(sd1,sd2);
	}

	public static double[] divide(double[] sd1, int i2){
		double sd2[]={(double)i2,0};
		//    	if (Double.isNaN(i2) || Double.isInfinite(i2))
		//    		System.out.println("i2: " + i2);
		return divide(sd1,sd2);
	}

	public static double[] subtract(double[] sd1, double[] sd2){
		double minussd2[]={-sd2[0],sd2[1]};
		//	if (SmallDouble.add(sd1,minussd2)[0] == 0)
		//		System.out.println('\n' + "++++ In: " + StringOps.arrayToString(sd1,"(",")") + 
		//				"  " + StringOps.arrayToString(sd2,"(",")") + 
		//				" Out: " + StringOps.arrayToString(SmallDouble.add(sd1,minussd2),"(",")"));
		return SmallDouble.add(sd1,minussd2);
	}

	public static Double[] subtract(Double[] sd1, Double[] sd2){
		Double minussd2[]={-sd2[0],sd2[1]};
		//	if (SmallDouble.add(sd1,minussd2)[0] == 0)
		//		System.out.println('\n' + "++++ In: " + StringOps.arrayToString(sd1,"(",")") + 
		//				"  " + StringOps.arrayToString(sd2,"(",")") + 
		//				" Out: " + StringOps.arrayToString(SmallDouble.add(sd1,minussd2),"(",")"));
		return SmallDouble.add(sd1,minussd2);
	}

	
	public static double toStandardDouble(double[] sd){
		double result;
		if (sd[0]==0)
			result = 0;
		else 
			result = sd[0]*Math.pow(10,-sd[1]);
		return result;
	}

	public static double toStandardDouble(Double[] sd){
		double result;
		if (sd[0]==0)
			result = 0;
		else 
			result = sd[0]*Math.pow(10,-sd[1]);
		return result;
	}
	
	/* returns 1 if sd1>sd2; -1 if sd1<sd2; 0 if sd1=sd2 */
	public static int compareSD(Object sd1, Object sd2){
		double[] diff = new double[2];

		if (sd1 instanceof double[]) { //sd2 assumed to be the same type
			if (((double[])sd1)[0]==Double.NEGATIVE_INFINITY && ((double[])sd2)[0]==Double.NEGATIVE_INFINITY )
				return 0;
			if (((double[])sd1)[0]==Double.POSITIVE_INFINITY && ((double[])sd2)[0]==Double.POSITIVE_INFINITY )
				return 0;
			diff = SmallDouble.subtract( (double[])sd1,(double[])sd2);
		}
		if (sd1 instanceof Double[]) {
			if (((Double[])sd1)[0]==Double.NEGATIVE_INFINITY && ((Double[])sd2)[0]==Double.NEGATIVE_INFINITY )
				return 0;
			if (((Double[])sd1)[0]==Double.POSITIVE_INFINITY && ((Double[])sd2)[0]==Double.POSITIVE_INFINITY )
				return 0;
			Double[] Diff = SmallDouble.subtract( (Double[])sd1,(Double[])sd2);
			diff[0]=Diff[0];
			diff[1]=Diff[1];
		}

		if (diff[0]==0.0)
			return 0;
		if (diff[0]>0)
			return 1;
		else
			return -1;	
	}

	public int compare(Object sd1, Object sd2){
		return compareSD(sd1,sd2);
	}


	public static double nthRoot(double[] sd, double n){
		double factor = sd[1]/n;
		double basis = Math.pow(sd[0],1/n);
		double rootsd[]={basis,factor};
		return SmallDouble.toStandardDouble(rootsd);
	}

	public static double log(double[] sd){
		if (sd[0]==0)
			return Double.NEGATIVE_INFINITY;
		return Math.log(sd[0]) - sd[1]*Math.log(10);
	}

	public static double log(Double[] sd){
		return Math.log(sd[0]) - sd[1]*Math.log(10);
	}
	/** Takes an array sdarr of SmallDoubles and returns an array of doubles
	 * representing a scaled version of the vector represented by sdarr
	 * 
	 * Example: 
	 * 
	 * sdarr=
	 * 
	 * (1.2E-4,20.0)
	 * (4.3E-7,40.0)
	 * (6.4E-2,140.0)
	 * 
	 * return
	 * 
	 * toStandardDouble(1.2E-4,0)=1.2E-4
	 * toStandardDouble(4.3E-7,20.0)=4.3E-27
	 * toStandardDouble(6.4E-2,120)=0	
	 * 
	 * @param sdarr
	 * @return
	 */
	public static double[] toStandardDoubleArray(double[][] sdarr){

		double result[] = new double[sdarr.length];
		int minfactor = Integer.MAX_VALUE;
		double[] prelim = new double[2];
		for (int i=0;i<sdarr.length;i++)
			if (sdarr[i][0]!=0.0)
				minfactor=Math.min(minfactor,(int)sdarr[i][1]);
		for (int i=0;i<sdarr.length;i++){
			prelim[0]=sdarr[i][0];
			//System.out.print(rbnutilities.arrayToString(prelim)+ " " );
			prelim[1]=sdarr[i][1]-minfactor;
			result[i]=SmallDouble.toStandardDouble(prelim);

		}
//		System.out.println("************************8");
//		for (int i=0;i<sdarr.length;i++)
//		  	System.out.println(rbnutilities.arrayToString(sdarr[i]) +"\t" + result[i]);
		return result;
	}

	
	
	//    public static long mantissa(double x){
	//    	long lbits = Double.doubleToLongBits(x);
	//    	long lmantissa = lbits & ((1L << 52) - 1);
	//    	return lmantissa;
	//    }
	//    
	//    
	//    public static long exponent(double x){
	//    	long lbits = Double.doubleToLongBits(x);
	//    	long lexponent = (lbits >>> 52 & ((1 << 11) - 1)) - ((1 << 10) - 1);
	//    	return lexponent;
	//    }
	//    

	public static String asString(double[] sd){
		double d = Math.ceil(-Math.log10(sd[0]));
		double lead = sd[0]*Math.pow(10,d);
		double exp = d+sd[1];
		return String.format("%.4f",lead) + "E-" + (int)exp;
	}
	
	public static double[] toProbabilityArray(double[][] sdarr) {
		/*
		 * Takes an array of small doubles, normalizes to a probability vector, and returns as standard double
		 */
		double[] result = new double[sdarr.length];
		double[] sum = new double[2];
		for (int i=0;i<sdarr.length;i++)
			sum=add(sum,sdarr[i]);
		for (int i=0;i<sdarr.length;i++)
			result[i]=toStandardDouble(divide(sdarr[i],sum));
		return result;
	}
}
