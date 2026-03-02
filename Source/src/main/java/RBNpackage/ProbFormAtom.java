package RBNpackage;

import java.lang.invoke.VarHandle;
import java.util.*;


import RBNExceptions.RBNCompatibilityException;
import RBNLearning.*;
import RBNinference.PFNetworkNode;
import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;
import RBNutilities.rbnutilities;

public class ProbFormAtom extends CPModel implements ProbForm {

	private Rel relation;
	private ArgTerm arguments[];

	public ProbFormAtom()
	{
		relation = new BoolRel();
		arguments = new ArgTerm[0];
	}

	public ProbFormAtom(Rel r)
	{
		relation = r;
		arguments = new ArgTerm[r.arity];
	}

	public ProbFormAtom(Rel rel, ArgTerm[] arguments) {
		this.relation = rel;
		this.arguments = (arguments == null) ? new ArgTerm[0] : arguments;
	}

	/** Creates new ProbFormAtom */
	public ProbFormAtom(Rel r, String[] args) 
	throws IllegalArgumentException
	{
		relation = r;
		if (args.length == r.arity){
			arguments = argsFromStrings(args);
		}
		else {
			throw new IllegalArgumentException("Error in constructing Indicator-Formula: arguments do not match arity of " + r.name);
		}
	}

	public ProbFormAtom(Rel r, int[] args) 
	throws IllegalArgumentException
	{
		relation = r;
		if (args.length == r.arity)
		{
			arguments = new ArgTerm[args.length];
			for (int i=0; i<args.length; i++)
				arguments[i]= new VarTerm(Integer.toString(args[i]));
		}
		else {
			throw new IllegalArgumentException("Error in constructing Indicator-Formula: arguments do not match arity of " + r.name);
		}
	}


	public ArgTerm[] argsFromStrings(String[] strargs) {
		if (strargs == null)
			return new ArgTerm[0];
		ArgTerm[] args = new ArgTerm[strargs.length];
		for (int i = 0; i < args.length; i++)
			args[i] = new VarTerm(strargs[i]);
		return args;
	}

	public void setArgumentsFromStrings(String[] strargs) {
		if (strargs == null) {
			this.arguments = new ArgTerm[0];
			return;
		}
		this.arguments = new ArgTerm[strargs.length];
		for (int i = 0; i < strargs.length; i++)
			this.arguments[i] = new VarTerm(strargs[i]);
	}

	public String[] getArgumentsAsString() {
		String[] out = new String[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			out[i] = arguments[i].argEval();
		}
		return out;
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
		else {
			for (int i = 0; i < arguments.length; i++) {
				result[i] = Integer.parseInt(arguments[i].argEval());
			}
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

	public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean useAlias) {
		String alias = this.getAlias();
		if (useAlias && alias != null) {
			return alias;
		}
		String base = this.asString(A);
		if (depth <= 0) {
			return base;
		}

		StringBuilder sb = new StringBuilder(depth + base.length());
		for (int i = 0; i < depth; i++) {
			sb.append(' ');
		}
		sb.append(base);
		return sb.toString();
	}

	public String asString(RelStruc A) {
		StringBuilder sb = new StringBuilder();
		sb.append(relation.printname());
		sb.append("(");
		for (int i = 0; i < arguments.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			String argStr = arguments[i].argEval();
			if (argStr != null
					&& rbnutilities.IsInteger(argStr)
					&& A != null
					&& !(getRelation().getTypes()[i] instanceof TypeInteger)) {
				sb.append(A.nameAt(Integer.parseInt(argStr)));
			} else {
				sb.append(arguments[i].toString());
			}
		}
		sb.append(")");
		return sb.toString();
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
			double v = (double)this.evaluate(A, data, new ArgTerm[0], new int[0], 0, false, false, null , false, null, null, ProbForm.RETURN_ARRAY, true,null)[0];
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
			return A.trueOrdAtom(relation, arguments);
		if (relation.ispredefined()){
			int[] argsasints = rbnutilities.argTermArrayToIntArray(arguments);
			if (argsasints == null)
				throw new IllegalArgumentException("Attempt to evaluate non-ground atom " + this.asString(A));
			return A.truthValueOf(relation, argsasints);
		}
		return -1; // not predefined 
	}

	public int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, HashMap atomhasht)
			throws RBNCompatibilityException{
		if (relation.ispredefined())
			return evaluatesTo(A);

		int[] argsasints = rbnutilities.argTermArrayToIntArray(arguments);
		if (argsasints == null)
			throw new IllegalArgumentException("Attempt to evaluate non-ground atom " + this.asString(A));

		if (!usesampleinst)
			return inst.truthValueOf(relation, argsasints);
		else {
			GroundAtom myatom = new GroundAtom(relation, argsasints);
			String myatomname = myatom.asString();
			PFNetworkNode gan = (PFNetworkNode) atomhasht.get(myatomname);
			return gan.sampleinstVal();
		}
	}

	public VarTerm[] freevars() {
		HashSet<String> s = new HashSet<>();
		for (ArgTerm t : arguments) {
			Set<String> vars = t.getVariables();
			if (vars != null) {
				s.addAll(vars);
			}
		}

		VarTerm[] out = new VarTerm[s.size()];
		int idx = 0;
		for (String v : s)
			out[idx++] = new VarTerm(v);

		return rbnutilities.NonIntOnly(out);
	}

	public boolean isGround()
	{
		for (ArgTerm t : arguments) {
			if (!t.isGround())
				return false;
		}
		return true;
	}


	public  Vector<GroundAtom> makeParentVec(RelStruc A){
		return makeParentVec(A, new OneStrucData(),null);
	}

	public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone) {
		Vector<GroundAtom> result = new Vector<GroundAtom>();
		if (this.getRelation().ispredefined())
			return result;
		if (!this.isGround())
			throw new RuntimeException("Detected dependency on non-ground atom");
		if (inst.truthValueOf(relation, rbnutilities.argTermArrayToIntArray(arguments)) == -1) {
			result.add(new GroundAtom(relation, rbnutilities.argTermArrayToIntArray(arguments)));
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


	public CPModel sEval(RelStruc A){
		double val= (double)evaluate(A,
				new OneStrucData(),
				new ArgTerm[0],
				new int[0],
				0,
				false,
				false,
				new HashMap<Rel,GroundAtomList>(),
				false,
				null,
				null,
				ProbForm.RETURN_ARRAY,
				true,
				null
				)[0];

		if (relation.ispredefined()){
			int[] argsasints = rbnutilities.argTermArrayToIntArray(arguments);
			if (argsasints == null)
				throw new IllegalArgumentException("Attempt to evaluate non-ground atom " + this.asString(A) + " in ProbFormAtom.sEval");
			return new ProbFormConstant(val);
		}
		else return this; 
	}

	
	public ProbFormAtom substitute(String[] vars, int[] args)
	{
		ProbFormAtom result = new ProbFormAtom(relation);
		result.arguments = rbnutilities.array_substitute(arguments,vars,args);
		
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	public ProbFormAtom substitute(String[] vars, String[] args)
	{
		ProbFormAtom result = new ProbFormAtom(relation);
		result.arguments = rbnutilities.array_substitute(arguments,vars,args);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public ProbFormAtom substitute(String[] vars, ArgTerm[] args) {
		ProbFormAtom result = new ProbFormAtom(relation);
		result.arguments = rbnutilities.array_substitute(arguments,vars,args);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public ProbFormAtom substitute(ArgTerm[] vars, ArgTerm[] args) {
		ProbFormAtom result = new ProbFormAtom(relation);
		result.arguments = rbnutilities.array_substitute(arguments,vars,args);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	@Override
	public ProbFormAtom substitute(ArgTerm[] vars, int[] args) {
		ProbFormAtom result = new ProbFormAtom(relation);
		result.arguments = rbnutilities.array_substitute(arguments,vars,args);
		if (this.alias != null)
			result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
		return result;
	}

	public void setParameters(String[] params, double[] values){
	}
	
	public CPModel conditionEvidence(RelStruc A, OneStrucData inst){
		if (!this.isGround()) return new ProbFormAtom(relation,arguments);
		else {
			int truth = inst.truthValueOf(this.relation, rbnutilities.argTermArrayToIntArray(this.arguments));
			switch (truth){
			case -1 : return new ProbFormAtom(relation,arguments); 
			case 0 : return new ProbFormConstant(0);
			case 1 : return new ProbFormConstant(1);
			default : return new ProbFormConstant(1);
			}
		}
	}
	
//	public double evaluate(RelStruc A, 
//			OneStrucData inst, 
//			String[] vars, 
//			int[] tuple, 
//			boolean useCurrentCvals, 
//    		String[] numrelparameters,
//    		boolean useCurrentPvals,
//    		GroundAtomList mapatoms,
//    		boolean useCurrentMvals,
//    		HashMap<String,Double> evaluated)
//	{			
//		String key="";
//		
//		ProbFormAtom substituted = (ProbFormAtom)this.substitute(vars,tuple);
//		if (!substituted.isGround())
//			throw new IllegalArgumentException("Attempt to evaluate non-ground atom");
//		
//		if (evaluated != null) {
//			key = GradientGraph.makeKey(substituted, 0, 0, A);
//			Double d = evaluated.get(key);
//			if (d!=null)
//				return d; 
//		}
//		if (RelStruc.isOrdRel(relation))
//			return A.trueOrdAtom(relation,arguments);
//		
//		
//		
//		double value,result;
//
//		if (relation.isprobabilistic()){
//			value = inst.valueOf(substituted.relation, rbnutilities.stringArrayToIntArray(substituted.getArguments()));
//			if (value != -1 && (mapatoms == null || !mapatoms.contains(relation,substituted.argsIfGround())))
//				result = value;
//			else
//				result = Double.NaN;
//			if (evaluated != null)
//				evaluated.put(key, result);
//			return result;
//		}
//
//		if (relation.ispredefined() && !rbnutilities.arrayContains(numrelparameters, this.asString(A))){
//			return A.valueOf(relation, rbnutilities.stringArrayToIntArray(substituted.getArguments()));
//		}
//		//System.out.println("Evaluate: " + this.asString(0) +" return NaN");
//		return Double.NaN;
//	}
//

	public Object[] evaluate(RelStruc A, 
			OneStrucData inst, 
			ArgTerm[] vars,
			int[] tuple,
			int gradindx,
			boolean useCurrentCvals, 
    		// String[] numrelparameters,
    		boolean useCurrentPvals,
    		HashMap<Rel,GroundAtomList> mapatoms,
    		boolean useCurrentMvals,
    		HashMap<String,Object[]> evaluated,
    		HashMap<String,Integer> params,
    		int returntype,
    		boolean valonly,
    		Profiler profiler)
	{			
		Boolean profile = (profiler != null);
				
		Object[] result= new Object[2];
		double value;

		String key="";
		
		ProbFormAtom substituted = (ProbFormAtom)this.substitute(vars,tuple);
		if (!substituted.isGround())
			throw new IllegalArgumentException("Attempt to evaluate non-ground atom");
		
		if (evaluated != null) {
			key = substituted.makeKey(A);
			//System.out.print("looking for " + key);
			Object[] d = evaluated.get(key);
			if (d!=null) {
				//System.out.println("  yes found returning " + d[0]);
				return d.clone(); 
			}
			//System.out.println("   not found");		
		}	
		
		/* The main cases: */
		if (RelStruc.isOrdRel(relation)) {
			result[0]= A.trueOrdAtom(relation,arguments);
		}
		else if (relation.isprobabilistic()){
			value = inst.valueOf(substituted.relation, rbnutilities.argTermArrayToIntArray(substituted.getArguments()));
			if (value != -1 )
				result[0] = value;
			else
				result[0] = Double.NaN;
			//			else if (!useCurrentMvals
			//					&& mapatoms != null
			//					&&  mapatoms.get(relation)!= null
			//					&&  mapatoms.get(relation).contains(relation,substituted.argsIfGround())
			//					result[0] = Double.NaN;

			if (!valonly) {
				if (returntype == ProbForm.RETURN_ARRAY)
					result[1] = new Gradient_Array(params);
				else
					result[1] = new Gradient_TreeMap(params);
			}
		}
		else if (relation.ispredefined()) {
			String thisstr = "";
			Integer i = null;
			if (params != null && params.size()>0) {
				thisstr = substituted.asString(A);
				i = params.get(thisstr);
			}

			if (i==null || useCurrentPvals)
				result[0] = A.valueOf(relation, rbnutilities.argTermArrayToIntArray(substituted.getArguments()));
			else
				result[0] = Double.NaN;

			if (!valonly) {
				if (returntype == ProbForm.RETURN_ARRAY)
					result[1] = new Gradient_Array(params);
				else
					result[1] = new Gradient_TreeMap(params);
				if (i != null) {
					if (thisstr.equals(""))
						thisstr = substituted.asString(A);
					((Gradient) result[1]).set_part_deriv(thisstr, new double[]{1.0});
				}
//				if (returntype==ProbForm.RETURN_ARRAY) {
//					result[1]=new double[params.size()];
//					if (i!=null)
//						((double[])result[1])[i] = 1.0;
//				}
//				else {
//					result[1]=new HashMap<String,Double>();
//					if (i!= null)
//						((HashMap<String,Double>)result[1]).put(thisstr, 1.0);
//				}
			}
		} // else if (relation.ispredefined())
		
		if (evaluated != null) {
			//System.out.println("putting " + key + " = " + result[0]);
			evaluated.put(key, result.clone());
		}
		
		return result;
	}


	public double[] evalSample(RelStruc A,
			HashMap<String,PFNetworkNode> atomhasht, 
			OneStrucData inst, 
    		HashMap<String,double[]> evaluated,
			long[] timers)
			throws RBNCompatibilityException{
		
		String key = null;
		
		if (evaluated != null) {
			key = this.makeKey(A);
			double[] d = evaluated.get(key);
			if (d!=null) {
				return d; 
			}
		}	
		
		double[] result = null;
		if (!isGround())
			throw new IllegalArgumentException("Attempt to sample-evaluate non-ground atom");
		else{
			if (relation.ispredefined()){
				result = new double[] {this.evaluate(A,inst)};
			}
			if (relation.isprobabilistic()){
				GroundAtom myatom = new GroundAtom(relation,rbnutilities.argTermArrayToIntArray(arguments));
				String myatomname = myatom.asString();
				if (atomhasht.get(myatomname) == null)	/* myatom is not in the network, because
				 * it has become an isolated prob. zero
				 * node due to the instantiation instasosd.
				 * Its truth value is found in instasosd
				 */
				{
					result = new double[]{(double) inst.truthValueOf(myatom)};
				}
				else {
					PFNetworkNode gan = (PFNetworkNode) atomhasht.get(myatomname);
					result = new double[]{(double) gan.sampleinstVal()};
				}
			}
		}
		if (evaluated != null) {
			evaluated.put(key, result);
		}
		
		return result;
	}

	public Rel getRelation(){
		return relation;
	}

	// if it args can be evaluated, make them all general varterm
	public void evalArgs() {
		if (!isGround())
			throw new IllegalArgumentException("Attempt to evaluate non-ground atom");
		for (int i = 0; i < arguments.length; i++)
			arguments[i] = new VarTerm(arguments[i].argEval());
	}

	public ArgTerm[] getArguments(){
		return arguments;
	}

	public void updateSig(Signature s){
		Rel relinsig = s.getRelByName(relation.name());
		if (relinsig == null){
			System.out.println("Warning: relation '" + relation.name() + "' of ProbFormAtom not contained in current signature" );
		}
//		if (relinsig.getInout()!=relation.getInout())
//			System.out.println("Warning: changing inout type for relation " + relation.name() 
//			                    + " from " + relation.getInout_string() + " to " + relinsig.getInout_string());
		relation = relinsig;
	}
	
	public void setCvals(String paramname, double val) {
	}
	
	public TreeSet<Rel> parentRels(){
		TreeSet<Rel> result = new TreeSet<Rel>();
		if (relation.isprobabilistic())
			result.add(relation);
		return result;
	}
	
	public TreeSet<Rel> parentRels(TreeSet<String> processed){
		String mykey=this.makeKey((String[]) null,null,true);
		if (processed.contains(mykey))
			return new TreeSet<Rel>();
		else {
			processed.add(mykey);
			return this.parentRels();
		}
					
	}

	@Override
	public int numvals() {
		return 2;
	}

}
