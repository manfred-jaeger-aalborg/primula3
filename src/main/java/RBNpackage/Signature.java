package RBNpackage;

import java.util.Vector;
import java.util.HashMap;
import java.util.AbstractList;

public class Signature {
	
	HashMap<String,Rel> rels;
	
	public Signature(){
		rels = new HashMap<String,Rel>();
	}
	
	public void addRel(Rel r){
		switch (r.valtype()){
		case Rel.BOOLEAN:
			rels.put(r.name(), new BoolRel(r));
			break;
		case Rel.NUMERIC:
			rels.put(r.name(), new NumRel(r));
			break;
		case Rel.UNKNOWN:
			rels.put(r.name(), r);
		}
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
}
