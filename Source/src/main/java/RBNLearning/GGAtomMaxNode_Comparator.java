package RBNLearning;

import java.util.Comparator;

public class GGAtomMaxNode_Comparator implements Comparator<GGAtomMaxNode> {

	@Override
	public int compare(GGAtomMaxNode arg0, GGAtomMaxNode arg1) {
		if (arg0.getScore()==arg1.getScore())
			return 0;
		if (arg0.getScore()<arg1.getScore())
			return 1;
		return -1;
	}

}
