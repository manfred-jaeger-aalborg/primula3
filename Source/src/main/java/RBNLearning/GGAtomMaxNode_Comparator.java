package RBNLearning;

import java.util.Comparator;

public class GGAtomMaxNode_Comparator implements Comparator<GGAtomMaxNode> {

	@Override
	public int compare(GGAtomMaxNode arg0, GGAtomMaxNode arg1) {
		int scoreComparison = Double.compare(arg1.getScore(), arg0.getScore());
		if(scoreComparison != 0)
			return scoreComparison;
		// Secondary comparison based on unique id
		return arg0.getMyatom().compareTo(arg1.getMyatom());
	}

}
