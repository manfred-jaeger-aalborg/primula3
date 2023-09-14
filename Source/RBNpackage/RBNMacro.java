package RBNpackage;

import RBNpackage.BoolRel;
import RBNpackage.ProbForm;

public class RBNMacro extends RBNElement{
	
	/* Example: 
	 * 
	 * @macro(u,v) = WIF r(u) THEN 0.3 ELSE  t(v);
	 * 
	 * rel(w,z) =  .... @macro(z,w) ....
	 * 
	 * Here: this.arguments = [u,v].
	 * 
	 */
	
	public RBNMacro(BoolRel r, ProbForm pf) {
		super(r,pf);
	}
	
	public RBNMacro(BoolRel r, String[] args, ProbForm pf) {
		super(r,args,pf);
	}
	
	public RBNMacro(String name, String[] args, ProbForm pf) {
		super(new BoolRel(name,args.length),args,pf);
	}
}
