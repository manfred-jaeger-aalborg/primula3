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

	Rel prel;           // Relation

	String[] arguments; // Argument list. Must be superset of free variables of prfrm.

	CPModel cpmod;

	public RBNElement()
	{
	}

	public RBNElement(Rel r, CPModel pf)
	{
		prel =r;
		cpmod = pf;
	}

	public RBNElement(Rel r, String[] args, CPModel pf)
	{
		prel =r;
		arguments = args;
		cpmod = pf;
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

	public Rel rel() {
		return prel;
	}

	public void set_rel(BoolRel r) {
		prel=r;
	}
	
	public void set_pform(ProbForm pf) {
		cpmod=pf;
	}
	
	public CPModel cpmod(){
		return cpmod;
	}

	public void setRel(BoolRel br){
		prel = br;
	}

}

