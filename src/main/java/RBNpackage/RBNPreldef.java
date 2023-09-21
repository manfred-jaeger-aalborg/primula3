package RBNpackage;

import RBNpackage.BoolRel;
import RBNpackage.ProbForm;

public class RBNPreldef extends RBNElement {
	
	public RBNPreldef(BoolRel r, ProbForm pf) {
		super(r,pf);
		r.setInout(Rel.PROBABILISTIC);	
	}
	
	public RBNPreldef(BoolRel r, String[] args, ProbForm pf) {	
		super(r,args,pf);
		r.setInout(Rel.PROBABILISTIC);	
	}

}
