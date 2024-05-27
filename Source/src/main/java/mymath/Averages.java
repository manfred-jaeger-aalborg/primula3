package mymath;

import java.util.*;
import myio.*;


public class Averages{


    public static void main(String[] args){

	int numvecs = args.length;
	System.out.println("args: " + numvecs);
	double[] avges = new double[myio.StringOps.stringToDoubleVector(args[0]).size()];
	double[] nextvec;

	
	for (int i=0;i<numvecs;i++){
	    nextvec = myio.StringOps.stringToDoubleVector(args[i]).asArray();
	    System.out.println("arg no: " + i);
	    for (int j=0;j<avges.length;j++){
		avges[j]=avges[j]+nextvec[j];
	    }
	}
	
	for (int j=0;j<avges.length;j++)
	    avges[j]=avges[j]/numvecs;

	System.out.println(myio.StringOps.arrayToString(avges,"",""));
    }

}
