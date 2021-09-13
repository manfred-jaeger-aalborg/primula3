package RBNpackage;

import java.util.Hashtable;
import java.util.Vector;

import RBNExceptions.RBNCompatibilityException;
import RBNLearning.GradientGraph;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;
import RBNgui.Primula;

public  class ProbFormAtom extends ProbForm {


	private Rel relation;
	private String arguments[];

	public ProbFormAtom()
	{
//		SSymbs = new Rel[0];
//		RSymbs = new Rel[0];
		relation = new BoolRel();
		arguments = new String[0];
	}

	public ProbFormAtom(Rel r)
	{
//		SSymbs = new Rel[0];
//		RSymbs = new Rel[1];
//		RSymbs[0] = r;
		relation = r;
		arguments = new String[r.arity];
	}


	/** Creates new ProbFormAtom */
	public ProbFormAtom(Rel r, String[] args) 
	throws IllegalArgumentException
	{
//		SSymbs = new Rel[0];
//		RSymbs = new Rel[1];
//		RSymbs[0] = r;
		relation = r;
		if (args.length == r.arity){
			arguments = args;}
		else {
			throw new IllegalArgumentException("Error in constructing Indicator-Formula: arguments do not match arity of " + r.name);
		}
	}

	public ProbFormAtom(Rel r, int[] args) 
	throws IllegalArgumentException
	{
//		SSymbs = new Rel[0];
//		RSymbs = new Rel[1];
//		RSymbs[0] = r;
		relation = r;
		if (args.length == r.arity)
		{
			arguments = new String[args.length];
			for (int i=0; i<args.length; i++)
				arguments[i]=Integer.toString(args[i]);
		}
		else {
			throw new IllegalArgumentException("Error in constructing Indicator-Formula: arguments do not match arity of " + r.name);
		}
	}

	/** Returns the arguments as an array of integers if formula represents
	 * a ground atom. Throws an exception if formula not ground
	 */
	public int[] argsIfGround()
	throws RuntimeException
	{
		int[] result = new int[arguments.length];
		if (!this.isGround())
			throw new RuntimeException("ProbFormIndicator.argsIfGround() applied to non-ground indicator");
		else{
			for (int i=0; i<arguments.length; i++)
				result[i] = Integer.parseInt(arguments[i]);
		}
		return result;
	}

//	public String asString(int syntax, int depth)
//	{
//		String tabstring = "";
//		for (int i=0;i<depth;i++)
//			tabstring = tabstring +"  ";
//
//		String result = new String();
//		result = tabstring + this.asString(syntax);
//		return result;
//	}
//
//	public String asString(int syntax)
//	{
//		String result = new String();
//		result = relation.printname();
//		result = result.concat("(");
//		for (int i = 0; i<arguments.length-1; i++)
//			result = result.concat(arguments[i] + ",");
//		if (arguments.length >0)
//			result = result.concat(arguments[arguments.length-1]);
//		result = result.concat(")");
//		return result;
//	}

	public String asString(int syntax, int depth, RelStruc A,boolean paramsAsValue,boolean usealias)
	{
		if (usealias && this.getAlias() != null)
			return this.getAlias();
		String tabstring = "";
		for (int i=0;i<depth;i++)
			tabstring = tabstring +" ";

		String result = new String();
		result = this.asString(A);
		return result;
	}

	public String asString(RelStruc A)
	{
		String result = new String();
		result = relation.printname();
		result = result.concat("(");
		for (int i = 0; i<arguments.length-1; i++)
			if (rbnutilities.IsInteger(arguments[i]) && A!=null)
				result = result.concat(A.nameAt(Integer.parseInt(arguments[i])) + ",");
			else result = result.concat(arguments[i] + ",");
		if (arguments.length>0){
			if (rbnutilities.IsInteger(arguments[arguments.length-1])&& A!=null)
				result = result.concat(A.nameAt(Integer.parseInt(arguments[arguments.length-1])));
			else result = result.concat(arguments[arguments.length-1]);
		}
		result = result.concat(")");
		return result;
	}

	/** Returns the ground atom if this ProbForm represents a ground atom;
	 * otherwise returns null;
	 */
	public GroundAtom atom(){
		if (isGround())
			return new GroundAtom(relation, argsIfGround());
		else
			return null;
	}


	public boolean dependsOn(String variable, RelStruc A, OneStrucData data)
			throws RBNCompatibilityException
			{
		if (relation.isprobabilistic() && variable.equals("unknown_atom")){
			double v = this.evaluate(A,data,new String[0], new int[0], false,
					new String[0],false,new GroundAtomList(),false,null);
			if (Double.isNaN(v))
				return true;
			else return false;
		}
		else return false;
			}

	public boolean equals(ProbFormAtom pfi)
	{
		boolean result = true;
		if(!relation.equals(pfi.relation)) return false;
		for (int i=0; i<relation.arity; i++)
			if (!arguments[i].equals(pfi.arguments[i])) result = false;
		return result;
	}


	public  int evaluatesTo(RelStruc A){
		if (A.isOrdRel(relation))
			return A.trueOrdAtom(relation,arguments);
		if (relation.ispredefined()){
			int[] argsasints = rbnutilities.stringArrayToIntArray(arguments);
			if (argsasints == null)
				throw new IllegalArgumentException("Attempt to evaluate non-ground atom " + this.asString(A));		
			return A.truthValueOf(relation,argsasints);
		}
		return -1; // not predefined 
	}

	public  int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable atomhasht)
			throws RBNCompatibilityException{
		if (relation.ispredefined())
			return evaluatesTo(A);
		
		int[] argsasints = rbnutilities.stringArrayToIntArray(arguments);
		if (argsasints == null)
			throw new IllegalArgumentException("Attempt to evaluate non-ground atom " + this.asString(A));		
		
		if (!usesampleinst)
			return inst.truthValueOf(relation,argsasints);
		
		else{
			GroundAtom myatom = new GroundAtom(relation,argsasints);
			String myatomname = myatom.asString();
			PFNetworkNode gan = (PFNetworkNode)atomhasht.get(myatomname);
			return gan.sampleinstVal();
		}
	}

	public String[] freevars()
	{
		return rbnutilities.NonIntOnly(arguments);
	}

	public boolean isGround()
	{
		if (this.freevars().length>0){
			return false;}
		else return true;
	}


	public  Vector<GroundAtom> makeParentVec(RelStruc A){
		return makeParentVec(A, new OneStrucData());
	}

	public  Vector<GroundAtom> makeParentVec(RelStruc A,OneStrucData inst){
		if (!this.isGround())
			throw new RuntimeException("Detected dependency on non-ground atom");
		Vector<GroundAtom> result = new Vector<GroundAtom>();
		if (relation.ispredefined())
			return result;
		if (inst.truthValueOf(relation,rbnutilities.stringArrayToIntArray(arguments))==-1){
			result.add(new GroundAtom(relation,rbnutilities.stringArrayToIntArray(arguments)));
		}
		return result;
	}

	public boolean multlinOnly(){
		return true;
	}

	public String[] parameters(){
		return new String[0];
	}

//	public ProbForm sEval(RelStruc A){
//		if (relation.ispredefined())
//			return new ProbFormConstant((double)this.evaluatesTo(A));
//		else return this;
//	}

	public ProbForm sEval(RelStruc A){
		double val= evaluate(A,
				new OneStrucData(),
				new String[0],
				new int[0],
				false,
				new String[0],
				false,
				new GroundAtomList(),
				false,
				null
				);

		if (relation.ispredefined()){
			int[] argsasints = rbnutilities.stringArrayToIntArray(arguments);
			if (argsasints == null)
				throw new IllegalArgumentException("Attempt to evaluate non-ground atom " + this.asString(A) + " in ProbFormAtom.sEval");		
			return new ProbFormConstant(val);
		}
		else return this; 
	}

	
	public ProbForm substitute(String[] vars, int[] args)
	{
		ProbFormAtom result = new ProbFormAtom(relation);
		String nextarg;
		for (int i = 0; i<relation.arity; i++)
		{
			nextarg = arguments[i];
			for (int j = 0; j<vars.length; j++)
			{
				if (nextarg.equals(vars[j])) nextarg = String.valueOf(args[j]);
			}
			result.arguments[i] = nextarg;
		}
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	public ProbForm substitute(String[] vars, String[] args)
	{
		ProbFormAtom result = new ProbFormAtom(relation);
		String nextarg;
		for (int i = 0; i<relation.arity; i++)
		{
			nextarg = arguments[i];
			for (int j = 0; j<vars.length; j++)
			{
				if (nextarg.equals(vars[j])) nextarg = args[j];
			}
			result.arguments[i] = nextarg;
		}
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	public void setParameters(String[] params,  double[] values){
	}
	
	public ProbForm conditionEvidence(RelStruc A, OneStrucData inst){
		if (!this.isGround()) return new ProbFormAtom(relation,arguments);
		else {
			int truth = inst.truthValueOf(this.relation, rbnutilities.stringArrayToIntArray(this.arguments));
			switch (truth){
			case -1 : return new ProbFormAtom(relation,arguments); 
			case 0 : return new ProbFormConstant(0);
			case 1 : return new ProbFormConstant(1);
			default : return new ProbFormConstant(1);
			}
		}
	}
	
	public double evaluate(RelStruc A, 
			OneStrucData inst, 
			String[] vars, 
			int[] tuple, 
			boolean useCurrentCvals, 
    		String[] numrelparameters,
    		boolean useCurrentPvals,
    		GroundAtomList mapatoms,
    		boolean useCurrentMvals,
    		Hashtable<String,Double> evaluated)
	{			
		String key="";
		
		ProbFormAtom substituted = (ProbFormAtom)this.substitute(vars,tuple);
		if (!substituted.isGround())
			throw new IllegalArgumentException("Attempt to evaluate non-ground atom");
		
		if (evaluated != null) {
			key = GradientGraph.makeKey(substituted, 0, 0, A);
			Double d = evaluated.get(key);
			if (d!=null)
				return d; 
		}
		if (RelStruc.isOrdRel(relation))
			return A.trueOrdAtom(relation,arguments);
		
		
		
		double value,result;

		if (relation.isprobabilistic()){
			value = inst.valueOf(substituted.relation, rbnutilities.stringArrayToIntArray(substituted.getArguments()));
			if (value != -1 && (mapatoms == null || !mapatoms.contains(relation,substituted.argsIfGround())))
				result = value;
			else
				result = Double.NaN;
			if (evaluated != null)
				evaluated.put(key, result);
			return result;
		}

		if (relation.ispredefined() && !rbnutilities.arrayContains(numrelparameters, this.asString(A))){
			return A.valueOf(relation, rbnutilities.stringArrayToIntArray(substituted.getArguments()));
		}
		//System.out.println("Evaluate: " + this.asString(0) +" return NaN");
		return Double.NaN;
	}


	public  double evalSample(RelStruc A, Hashtable<String,PFNetworkNode> atomhasht, OneStrucData inst, long[] timers)
			throws RBNCompatibilityException{
		double result;
		if (!isGround())
			throw new IllegalArgumentException("Attempt to sample-evaluate non-ground atom");
		else{
			if (relation.ispredefined()){
				return this.evaluate(A,null,new String[0], new int[0], false, new String[0], false,null,false,null);
			}
			if (relation.isprobabilistic()){
				GroundAtom myatom = new GroundAtom(relation,rbnutilities.stringArrayToIntArray(arguments));
				String myatomname = myatom.asString();
				if (atomhasht.get(myatomname) == null)	/* myatom is not in the network, because 
				 * it has become an isolated prob. zero 
				 * node due to the instantiation instasosd. 
				 * Its truth value is found in instasosd
				 */
				{
					return (double)inst.truthValueOf(myatom);  
				}
				PFNetworkNode gan = (PFNetworkNode)atomhasht.get(myatomname);
				result = (double)gan.sampleinstVal();
				if (result != 0 && result !=1)
					System.out.println("-------in Indicator.evalSample: " +gan.myatom().asString(A) + " = " + result);
				return (double)gan.sampleinstVal();
			}
		}
		return Double.NaN;
	}

	public Rel getRelation(){
		return relation;
	}
	
	public String[] getArguments(){
		return arguments;
	}
	
	public void updateSig(Signature s){
		Rel relinsig = s.getRelByName(relation.name());
		if (relinsig == null){
			System.out.println("Warning: relation '" + relation.name() + "' of ProbFormAtom not contained in current signature" );
		}
		if (relinsig.getInout()!=relation.getInout())
			System.out.println("Warning: changing inout type for relation " + relation.name() 
			                    + " from " + relation.getInout_string() + " to " + relinsig.getInout_string());
		relation = relinsig;
	}
	
	public void setCvals(String paramname, double val) {
	}
}
