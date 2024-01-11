package RBNpackage;

import RBNpackage.BoolRel;
import RBNpackage.ProbForm;

public class RBNPreldef extends RBNElement {
	
	public RBNPreldef(Rel r, CPModel pf) {
		super(r,pf);
		r.setInout(Rel.PROBABILISTIC);	
	}
	
	public RBNPreldef(Rel r, String[] args, CPModel pf) {	
		super(r,args,pf);
		r.setInout(Rel.PROBABILISTIC);	
	}

}
