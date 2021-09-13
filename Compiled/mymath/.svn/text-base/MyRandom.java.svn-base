package mymath;

import java.io.*;
import java.util.*;
import myio.*;

public class MyRandom {

    public static boolean toss(double bias){
	double r = Math.random();
	if (r < bias) return true; else return false;
    }

    public static void randSeqToFile(double bias, String filename, int length){
	try{
	    BufferedWriter bw = FileIO.openOutputFile(filename);
	    for (int i=0; i<length; i++)
		if (toss(bias)) bw.write("h"); else bw.write("t");
	    bw.close();
	}
	catch (IOException e){System.out.println(e);};
	
    }

    public  static int randomInteger(int max){
    	/* Generates a random integer, uniformly distributed between 0 and max */
    	double rand = Math.random();
    	double l = (double)1/(max+1);
    	return (int)Math.floor(rand/l);
    }


    public  static long randomInteger(long max){
    	/* Generates a random integer, uniformly distributed between 0 and max */
    	double rand = Math.random();
    	double l = (double)1/(max+1);
    	return (int)Math.floor(rand/l);
    }

    /* Generates a random probability vector of length length
     * 
     * For bias = 1.0 the vector is uniformly sampled (actually not! already biased towards uniform).
     * For bias < 1.0 the sampling is more concentrated around the uniform distribution
     * For bias > 1.0 the sampling is more concentrated around imbalanced distributions
     * 
     * Samples with length = 5 and bias = 0.3:
     * [0.14493223221809137,0.216212197523146,0.19951837323923716,0.19583577569570346,0.24350142132382205]
       [0.23164564241663504,0.11156035822802976,0.18294663436157044,0.24259347508314774,0.23125388991061693]
       [0.16110995866875927,0.21035303247770543,0.21588484324619547,0.1854531285122817,0.22719903709505807]
       [0.21266550913555804,0.18255118264581977,0.2340740624473452,0.1793457899500187,0.19136345582125824]
       [0.25913796635582975,0.28511409724651254,0.2387093500300839,0.12874335376583104,0.0882952326017428]
  
     * Samples with length = 5 and bias = 5.0:
     * [0.3114440676168235,5.028872146266795E-7,0.26646456130120344,0.2958692005529344,0.12622166764182421]
       [0.06931895558428992,0.6105881873119627,2.520277436193265E-5,0.016561734385580187,0.30350591994380516]
       [0.3585018159384516,3.4219532402512715E-4,1.9585077371356385E-11,0.6393655322381313,0.001790456479806901]
       [0.03884852941463115,0.01548480979376715,5.925920506385862E-7,0.03389171502796306,0.9117743531715881]
       [0.2946052499014072,0.6685310378411734,0.0047767303714095245,2.6884814255127886E-4,0.031818133743458606]
     */
    public static double[] randomCPTRow(int length, double bias){
    	double[] result = new double[length];
	double sum = 0;
	for (int i=0;i<length;i++){
	    result[i] = Math.pow(Math.random(),bias);
	    sum = sum + result[i];
	}
	for (int i=0;i<length;i++)
	    result[i] = result[i]/sum;
	return result;
    }

    public static int[] randomIntArray(int length, int maxindex){
	/* Creates an integer array of length 'length'. Entries are 
	 * randomly selected integers from 0 to maxindex - 1, without repetitions
	 * If length > maxindex, then return only array of length 
	 * maxindex (containing all integers from 0 to maxindex - 1)
	 */
	int resultlength = Math.min(length,maxindex);
	int[] result = new int[resultlength];
	Vector intUrn = new Vector(); /* Vector of Integer */
	int nextdraw;
	Integer drawnint;
	for (int i=0;i<maxindex;i++)
	    intUrn.add(new Integer(i));
	for (int i=0;i<resultlength;i++){
	    nextdraw = randomInteger(intUrn.size()-1);
	    drawnint = (Integer)intUrn.remove(nextdraw);
	    result[i]=drawnint.intValue();
	}
	return result;
    }
    
    public static int randomPoisson(double lambda){
    	double L = Math.exp(-lambda);
    	double p = 1;
    	int counter;
    	double rand;
    	for (counter = 0; p >= L; counter++){
    		rand = Math.random();
    		p = p*rand;
    	}
    	return counter-1;
    }

	/** Sample a parameter p in [0,1] according to a beta
	 * distribution that has mean e and variance v.
	 * e and v must be such that appropriate parameters alpha,beta for
	 * the beta distribution can be found.
	 * Implementation  is only efficient for
	 * small values of alpha and beta
	 **/
	public static double sampleBeta(double e, double v){

		double alpha = Math.pow(e,2)*(1-e)/v-e;
		double beta = alpha*(1-e)/e;

		if (alpha >0 && beta >0){
			double x1 = Math.random();
			double x2 = Math.random();
			double y1;
			double y2;
			boolean done = false;
			while (!done){
				x1 = Math.random();
				x2 = Math.random();
				y1 = Math.pow(x1,1/alpha);
				y2 = Math.pow(x2,1/beta);
				if (y1+y2 <= 1){
					done = true;
					return y1/(y1+y2);
				}
			}
		}
		else
			System.out.println("Cannot find beta distribution with mean " + e + " and variance " + v);
		return 1;

	}
	

	public static void main(String[] args){
		double[] sampled=null;
		for (int i=0;i<10;i++){
			sampled = randomCPTRow(5,5.0);
		System.out.println(StringOps.arrayToString(sampled,"[","]"));
		}
	}
	
	/* generates a random integer from 0 to probs.length-1 with
	 * probabilities according to probs;
	 */
	public static int randomMultinomial(double[] probs){
		double rand = Math.random();
		int result =0;
		double totalprob=probs[0];
		while (totalprob<rand){
			result++;
			totalprob=totalprob+probs[result];
		}
		return result;
	}
}
