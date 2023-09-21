package RBNpackage;

import RBNpackage.BoolRel;
import RBNpackage.ProbForm;
import RBNpackage.Type;

public class RBNElement 
{
	/*
	 * An RBNelement represents a single definition of either a macro,
	 * or a probabilistic relation. In both cases the "left side" of the definition
	 * is represented by BoolRel and a list of arguments. In the case of macro
	 * definitions, the BoolRel has a name starting with '@'. 
	 * The right side of the definition is given by a probability formula.
	 */

	BoolRel prel;           // Relation

	String[] arguments; // Argument list. Must be superset of free variables of prfrm.

	ProbForm prfrm;

	public RBNElement()
	{
	}

	public RBNElement(BoolRel r, ProbForm pf)
	{
		prel =r;
		prfrm = pf;
	}

	public RBNElement(BoolRel r, String[] args, ProbForm pf)
	{
		prel =r;
		arguments = args;
		prfrm = pf;
	}
	
	public String[] arguments() {
		return arguments;
	}

	public void set_args(String[] a) {
		arguments=a;
	}
	
	public Type[] types(){
		return prel.getTypes();
	}

	public BoolRel rel() {
		return prel;
	}

	public void set_rel(BoolRel r) {
		prel=r;
	}
	
	public void set_pform(ProbForm pf) {
		prfrm=pf;
	}
	
	public ProbForm pform(){
		return prfrm;
	}

	public void setRel(BoolRel br){
		prel = br;
	}

}

