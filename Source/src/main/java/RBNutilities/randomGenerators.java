/*
* randomGenerators.java 
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

public  class randomGenerators {
	
	/** Returns a random integer between min and max (inclusive)
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	public static int randInt(int min, int max){
		double rand = Math.random();
		double diff = max-min+1;
		double ddiff = 1/diff;
		int offset = (int)Math.floor(rand/ddiff);
		return min+offset;
	}
	
	public static int randSign(){
		double rand = Math.random();
		if (rand>0.5)
			return 1;
		else return -1;
	}
	
	/** Returns an array of num random integers between min and
	 * max without repetitions
	 * 
	 * Very inefficient implementation unless num << max-min
	 * 
	 * @param min
	 * @param max
	 * @param num
	 * @return
	 */
	public static int[] multRandInt(int min, int max, int num){
		int[] result = new int[num];
		TreeSet<Integer> ts = new TreeSet<Integer>();
		boolean newint;
		int ri=0;
		for (int i=0;i<num;i++){
			newint = false;
			while (!newint){
				ri = randInt(min,max);
				if (!ts.contains(ri)) {
					newint=true;
					ts.add(ri);
				}
			}
		}
		int i =0;
		for (Iterator<Integer> it = ts.iterator(); it.hasNext();i++) {
			result[i] = it.next();
		}

		return result;
	}

	/*
	 * scale (should be <= 1) is a parameter that biases the random number to be 
	 * - closer to the given bound, if only one of the two bounds min,max is given
	 * - closer to 0 if the given bounds include 0
	 * 
	 */
	public static double getRandom(double min, double max, double scale){
		double u = Math.random();
		if (min!=Double.NEGATIVE_INFINITY && max != Double.POSITIVE_INFINITY){
			if (min <= 0 && max >= 0)
				return scale*(min + u*(max-min));
			else
				return min + u*(max-min);
		}
		else{
			u=-Math.log(u);
			if (min==Double.NEGATIVE_INFINITY && max != Double.POSITIVE_INFINITY)
				return max-scale*u;
			if (min!=Double.NEGATIVE_INFINITY && max == Double.POSITIVE_INFINITY)
				return min + scale*u;
			if (min==Double.NEGATIVE_INFINITY && max == Double.POSITIVE_INFINITY)
				return randSign()*scale*u;
		}
		
		return Double.NaN;
	}


}
