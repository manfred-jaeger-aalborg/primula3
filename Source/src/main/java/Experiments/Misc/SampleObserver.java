package Experiments.Misc;

import java.util.*;

import RBNinference.SampleProbs;
import RBNutilities.rbnutilities;

public class SampleObserver implements Observer{

	public void update(Observable o, Object arg){
		if (o instanceof SampleProbs){
			double [][] prob= ((SampleProbs)o).getProbs();
				System.out.println("Number of samples: " + ((SampleProbs)o).getSize());
				System.out.println(rbnutilities.MatrixToString(prob));
				System.out.println();
		}
	}
}
