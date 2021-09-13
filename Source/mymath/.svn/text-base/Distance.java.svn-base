package mymath;

import java.util.*;
import myio.*;


public class Distance{


    public static void main(String[] args){
	DoubleVector vec;
	double[] darr;
	double dist=0;

	vec = StringOps.stringToDoubleVector(args[0]);
	darr = vec.asArray();

	dist = dist+Math.abs(0.5-darr[0]);
	dist = dist+Math.abs(0.99-darr[2]);
	dist = dist+Math.abs(0.6-darr[4]);
	dist = dist+Math.abs(0.4-darr[6]);
	dist = dist+Math.abs(0.01-darr[8]);

	dist = dist/5;

	System.out.println(dist);
    }

}
