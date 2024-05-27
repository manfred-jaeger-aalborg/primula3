package mymath;

import java.util.*;
import myio.*;


public class Normalize{


    public static void main(String[] args){
	DoubleVector vec;
	double[] darr;

	double normalizer=0;

	vec = StringOps.stringToDoubleVector(args[0]);
	darr = vec.asArray();

	for (int i=0;i<darr.length;i++){
	    normalizer = normalizer + Math.pow(darr[i],2.0);
	}
	normalizer = Math.sqrt(normalizer);
	for (int i=0;i<darr.length;i++){
	    darr[i]=darr[i]/normalizer;
	}
	System.out.println(myio.StringOps.arrayToString(darr,"",""));
    }

}
