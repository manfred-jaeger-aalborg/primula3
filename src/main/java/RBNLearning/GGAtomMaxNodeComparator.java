package RBNLearning;

import java.util.Comparator;

public class GGAtomMaxNodeComparator implements Comparator<GGAtomMaxNode>{
	
	private int compareby;
	
	public GGAtomMaxNodeComparator(int cb){
		compareby = cb;
	}
	
	public int compare(GGAtomMaxNode n1, GGAtomMaxNode n2){
		if (compareby == GradientGraphO.CompareIndicatorMaxNodesByScore){
			if( n1.getScore() < n2.getScore()) return -1;
			if( n1.getScore() > n2.getScore()) return 1;
			return 0;
		}
		if (compareby == GradientGraphO.CompareIndicatorMaxNodesByIndex){
			if( n1.getIndex() < n2.getIndex()) return -1;
			if( n1.getIndex() > n2.getIndex()) return 1;
			return 0;
		}
		return 0;
	}

}
