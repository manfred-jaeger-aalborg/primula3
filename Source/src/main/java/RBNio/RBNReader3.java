package RBNio;

import RBNpackage.*;
import RBNExceptions.*;
import myio.StringOps;
import java.io.*;
import java.util.*;

public class RBNReader3{

//	class ElementDef{
//		BoolRel rel;
//		Vector<String> args;
//		ProbForm pf;
//		
//		ElementDef(ParsedTypedAtom pta, ProbForm f){
//			rel = pta.rel();
//			rel.setInout(Rel.PROBABILISTIC);
//			args = pta.args();
//			pf =f;
//		}
//		
//		protected Rel rel(){
//			return rel;
//		}
//		
//		protected ProbForm pf(){
//			return pf;
//		}
//		
//		protected Vector<String> args(){
//			return args;
//		}
//		
//		protected void setPF(ProbForm f){
//			pf = f;
//		}
//	}
	
	class ParsedTypedAtom{
		Rel rel;
		ParsedTypedArguments pargs;
		
		ParsedTypedAtom(String relname, ParsedTypedArguments pa, boolean isMacro)
				throws RBNIllegalArgumentException
		{
			Rel r = null;
			if (isMacro)
				r = new BoolRel(relname,pa.arity());
			else {
				r = sig.getRelByName(relname);
				if (r==null) {
					System.out.println("x");
					throw new RBNIllegalArgumentException("Relation with name " + relname + "not declared in signature");
				}
			}
			rel = r;
			pargs = pa;
		}
		
		int arity(){
			return pargs.arity();
		}
		
		Rel rel(){
			return rel;
		}
		
		void set_relname(String rn) {
			rel.set_name(rn);
		}
		
		String[] args(){
			return StringOps.stringVectorToArray(pargs.args());
		}
	}
	
	class ParsedUnTypedAtom{
		Rel rel;
		Vector<String> args;;
		
		ParsedUnTypedAtom(String relname, Vector<String> a,boolean isMacro)
				throws RBNIllegalArgumentException
				{
			Rel r = null;
			if (isMacro)
				r = new BoolRel(relname,a.size());
			else {
				r = sig.getRelByName(relname);
				if (r==null) {
					throw new RBNIllegalArgumentException("Relation with name " + relname + "not declared in signature");
				}
			}
			rel = r;
			args = a;
				}
		
		Rel rel(){
			return rel;
		}
		
		void set_relname(String rn) {
			rel.set_name(rn);
		}
		
		String[] args(){
			return StringOps.stringVectorToArray(args);
		}
		
	}
	
	class ParsedTypedArguments{
		Vector<Type> types;
		Vector<String> args;
		
		ParsedTypedArguments(Vector<Type> typ, Vector<String> ar){
			types = typ;
			args = ar;
		}
		
		int arity(){
			if (types.size()==args.size())
				return types.size();
			else throw new RBNioException("Inconsistent arities in ParsedTypedArguments");
		}
		
		Type[] types(){
			Type[] result = new Type[types.size()];
			for (int i=0;i<result.length;i++)
				result[i]=types.elementAt(i);
			return result;
		}
		
		Vector<String> args(){
			return args;
		}
	}
	

	Signature sig;
	
	public Signature getSig() {
		return sig;
	}

	public void setSig(Signature sig) {
		this.sig = sig;
	}

	/**
	 * Contains the probabilistic relations defined in the RBN	
	 * 
	 * The key of a probrel is its name
	 **/
	Hashtable<String, RBNPreldef> probrelsdefined = new Hashtable<String, RBNPreldef>();
	
	/**
	 * Contains the macros defined in the RBN	
	 * 
	 * The key of a macro is its name
	 **/
	Hashtable<String, RBNMacro> macrosdefined = new Hashtable<String, RBNMacro>();
	
	
	/**
	 * Contains all the relations found either as probrels being defined,
	 * or probabilistic/predefined relations found in the probability formulas
	 */
	Hashtable<String,Rel> allrels = new Hashtable<String,Rel>();
	
	/**
	 * Contains the types referenced in the RBN	
	 * 
	 * The key of a type is the string <name>
	 **/
	Hashtable<String, Rel> typesreferenced = new Hashtable<String, Rel>();
	
	public RBNReader3(Signature s) {
		sig =s;
	}
	
	public RBN ReadRBNfromFile(File input_file)
	throws RBNSyntaxException,IOException,RBNIllegalArgumentException{


		RBNParser3 parser = 
			new RBNParser3(new java.io.FileInputStream(input_file));
		try{
			parser.setReader(this);
			parser.ReadRBN();
			
			parser.setParseno(2);
			parser.ReInit(new java.io.FileInputStream(input_file));
			parser.ReadRBN();
			
		}
		catch (ParseException ex){System.out.println(ex);}


		RBN result = new RBN(probrelsdefined.size(),macrosdefined.size());
		

		
		Enumeration<RBNMacro> em = macrosdefined.elements();
		RBNMacro nextmacrodef;
		int index = 0;
		while (em.hasMoreElements()){
			nextmacrodef = em.nextElement();
//			result.insertMacro(new RBNMacro((BoolRel)nextelementdef.rel(),
//											  nextelementdef.arguments(),
//											  nextelementdef.pform()), 
//								index);
			result.insertMacro(nextmacrodef, index);
			index++;
		}
		
		
		Enumeration<RBNPreldef> ep = probrelsdefined.elements();
		RBNPreldef nextprdef;
		index = 0;
		while (ep.hasMoreElements()){
			nextprdef = ep.nextElement();
////			result.insertPRel(new RBNPreldef((BoolRel)nextprdef.rel(),
////											  nextprdef.arguments(),
////											  nextprdef.pform()), 
////								index);
			result.insertPRel(nextprdef, index);
			index++;
		}
		return result;
		
	}

	protected void addProbRelDefined(RBNPreldef eldef){
		probrelsdefined.put(eldef.rel().toStringWArity(), eldef);
	}
	
	protected void addMacroDefined(RBNMacro eldef){
		macrosdefined.put(eldef.rel().toStringWArity(), eldef);
	}
	
	protected void addAllRel(Rel r){
		allrels.put(r.toStringWArity(), r);
	}
	
	protected Rel getProbRel(Rel r){
		RBNElement prdef = probrelsdefined.get(r.toStringWArity());
		if (prdef != null)
			return prdef.rel();
		else return null;
	}
	
	protected Rel getAllRel(Rel r){
		return allrels.get(r.toStringWArity());
	}
	
	
	protected RBNMacro getMacroDef(Rel r){
		return macrosdefined.get(r.toStringWArity());
	}
	
	protected void setMacroPF(Rel r, ProbForm pf){
		RBNMacro prdef = macrosdefined.get(r.toStringWArity());
		prdef.set_cpmod(pf);
		pf.setAlias(new ProbFormAtom(prdef.rel(),prdef.arguments()));
	}
	
	protected void setProbRelPF(Rel r, CPModel cpm){
		RBNPreldef prdef = probrelsdefined.get(r.toStringWArity());
		prdef.set_cpmod(cpm);
	}
}
