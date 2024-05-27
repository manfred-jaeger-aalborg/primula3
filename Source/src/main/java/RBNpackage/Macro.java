package RBNpackage;

public class Macro {

	private BoolRel relation; // A 'dummy' relation whose name starts with '@'
	
	private String arguments[]; // The argument list of this macro; must be superset of the free variables of pform
	
	private ProbForm pform; 
	
	/* Example: 
	 * 
	 * @macro(u,v) = WIF r(u) THEN 0.3 ELSE  t(v);
	 * 
	 * rel(w,z) =  .... @macro(z,w) ....
	 * 
	 * Here: arguments = [u,v].
	 * 
	 */
	
	public Macro(String name, String[] args, ProbForm pf) {
		relation = new BoolRel(name,args.length);
		arguments = args;
		pform=pf;
	}
	
	public void set_rel(BoolRel r) {
		relation = r;
	}
	
	public BoolRel get_rel() {
		return relation;
	}
	
	public void set_pform(ProbForm pf) {
		pform =  pf;
	}
	
	public ProbForm get_pform() {
		return pform;
	}
	
	public void set_args(String[] a) {
		arguments=a;
	}
	
	public String[] get_args() {
		return arguments;
	}
}
