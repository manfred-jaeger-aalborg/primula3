package RBNpackage;

import java.util.*;

public class Signature {
	
	/*
	 * Relations keyed by their names
	 */
	private HashMap<String,Rel> rels;
	
	public Signature(){
		rels = new HashMap<String,Rel>();
	}
	
	public void addRel(Rel r){
			rels.put(r.name(), r);
	}
	
	public Rel getRelByName(String rname){
		return rels.get(rname);
	}

	public Vector<Rel> getProbRels(){
		Vector<Rel> result = new Vector<Rel>();
		
		for (Rel r : rels.values()) {
			if (r.isprobabilistic())
				result.add(r);
		}
		return result;
		
	}
	
	public Collection<Rel> getAllRels(){
		return rels.values();
	}
	
	public boolean isExtension(Signature sig) {
		/* 
		 * Returns true if all relations of sig are also contained in this
		 * (with consistent arity, type, etc.
		 * 
		 */
		if (sig==null)
			return true;
		for (Rel r: sig.getAllRels()) {
			Rel rr = rels.get(r.name());
			if (rr==null)
				return false;
			if (!rr.equals(r))
				return false;
		}
		return true;
	}
}
