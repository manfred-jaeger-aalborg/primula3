package RBNpackage;

import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;

public class RBNMacro extends RBNElement{
	
	/* Can only have a ProbForm (not other CPModel) on the right side.
	 * 
	 * Example: 
	 * 
	 * @macro(u,v) = WIF r(u) THEN 0.3 ELSE  t(v);
	 * 
	 * rel(w,z) =  .... @macro(z,w) ....
	 * 
	 * Here: this.arguments = [u,v].
	 * 
	 */
	
	public RBNMacro(BoolRel r, CPModel pf) {
		super(r,pf);
	}
	
	public RBNMacro(BoolRel r, ArgTerm[] args, CPModel pf) {
		super(r,args,pf);
	}

//	public RBNMacro(String name, String[] args, CPModel pf) {
//		super(new BoolRel(name,args.length),args,pf);
//	}
}
