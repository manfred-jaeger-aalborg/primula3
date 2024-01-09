/*
* OneRelData.java 
* 
* Copyright (C) 2009 Aalborg University
*
* contact:
* jaeger@cs.aau.dk   http://www.cs.aau.dk/~jaeger/Primula.html
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

package RBNpackage;

import java.util.*;

import mymath.MyMathOps;
import RBNutilities.IntArrayComparator;
import RBNutilities.rbnutilities;
import myio.*;

import org.dom4j.Element;

/** An object of the class OneRelData represents a complete or incomplete
 * interpretation of one relation over a given domain
 * 
 * @author jaeger
 *
 */
public abstract class OneRelData {
	
	/**
	 * @uml.property  name="rel"
	 * @uml.associationEnd  
	 */
	Rel rel;
	/**
	 * The default value for atoms of this relation: 'false' (or 0.0 for numeric relations) or '?' 
	 * @uml.property  name="defaultval"
	 */
	String defaultval;
	
//	/**
//	 * Vector of int[]; elements are maintained in lexical order: 001 < 010 < 020 etc.
//	 * @uml.property  name="trueAtoms"
//	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="[I"
//	 */
//	TreeSet<int[]> trueAtoms;  
//	/**
//	 * @uml.property  name="falseAtoms"
//	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="[I"
//	 */
//	TreeSet<int[]> falseAtoms; 
//	/* For relations of arity 0 (globals): r()=true is
//	 * represented by trueAtoms = ([0]) , falseAtoms = ();
//	 * r() = false is represented by trueAtoms = (), falseAtoms = ([0])
//	 * r() uninstantiated is represented by trueAtoms = (), falseAtoms = ()
//	 */

	OneRelData()
	{
	}

	public OneRelData(Rel r, String dv)
	{
		rel = r;
		defaultval = dv;
	}



	/** Delete all atoms containing a 
	 * @param a
	 */
	//public abstract void delete(int a); TODO: not needed?
	
	public abstract OneRelData copy();
	
	public abstract TreeSet<int[]> allTrue();
	
	public abstract TreeSet<int[]> allTrue(String[] args);
	
	public abstract Vector<String[]> allTrue(RelStruc A);
	
	/**
	 * For a given array args that contains integers in some positons: args=[*,1,*,7,*]:
	 * return all the tuples that are 'true' (for num and cat rels: have a value) with
	 * the given arguments in the specified postions.
	 * @param args
	 * @param trueIndex
	 * @return
	 */
	public TreeSet<int[]> allTrue(String[] args, HashMap<Integer,TreeSet<int[]>>[] trueIndex){
		
		Vector<TreeSet<int[]>> slices = new Vector<TreeSet<int[]>>();
		boolean existsnull=false;
		
		for (int i=0;i<args.length;i++) {
			if (rbnutilities.IsInteger(args[i])) {
				TreeSet<int[]> slicefori = trueIndex[i].get(Integer.parseInt(args[i]));
				if (slicefori==null)
					existsnull=true;
				slices.add(slicefori);
			}
		}
		if (slices.size()==0) {
			return this.allTrue();
		}
		if (existsnull) {
			return new TreeSet<int[]>(new IntArrayComparator());
		}
		else {
			TreeSet<int[]> result = slices.elementAt(0);
			for (int i=1; i < slices.size(); i++)
				result = rbnutilities.treeSetIntersection(result, slices.elementAt(i));
			return result;
		}
	}

	public Rel rel(){
		return rel;
	}

	public String dv(){
		return defaultval;
	}


	public abstract String printAsString(RelStruc A, String pref);


	public abstract boolean isEmpty();

	/**Returns the binary tuples from the specified node to some other node
    *This method is usable ONLY with binary relations
    */
    public abstract Vector<int[]> getBinDirs(int node); //TODO: not needed?

    public abstract void addRelData(Element el, RelStruc struc);

	/** Delete all atoms containing a 
	 * @param a
	 */
	public abstract void delete(int a);
	
	/** Delete all tuples containing integer a
	 * from valmap. The HashMap index returns for each position in the
	 * relation a set of tuples with a in that position.
	 * @param a
	 * @param index
	 * @param valmap
	 */
	public void delete(int a, HashMap<Integer,TreeSet<int[]> >[] index, Map<int[],? extends Object> valmap) {
		for (int i=0;i<rel.arity;i++) {
			TreeSet<int[]> tuples = index[i].get(a);
			for (int[] t: tuples) {
				valmap.remove(t);
				removeFromIndex(t,index);
			}
		}
	}
	
	
    /**
     * Replaces all arguments b of trueAtoms and falseAtoms lists
     * by b-1 if b>a (needed after the deletion of node with index a from
     * the underlying SparseRelStruc)
     * @param a
     */
    public abstract void shiftArgs(int a);

    protected void addToIndex(int[] tup, HashMap<Integer,TreeSet<int[]>>[] idx) {
    	// tup and idx have the same length rel.arity!
    	for (int i=0;i<rel.arity;i++) {
    		TreeSet<int[]>ts=idx[i].get(tup[i]);
    		if (ts==null)
    			idx[i].put(tup[i],new TreeSet<int[]>(new IntArrayComparator()));
    		idx[i].get(tup[i]).add(tup);
    	}
    }

    protected void removeFromIndex(int[] tup, HashMap<Integer,TreeSet<int[]>>[] idx) {
    	for (int i=0;i<rel.arity;i++) {
    		boolean t = idx[0].isEmpty();
    		TreeSet<int[]>ts=idx[i].get(tup[i]);
    		if (ts!=null)
    			ts.remove(tup);
    	}
    }

    /**Returns the binary tuples from the specified node to some other node
	 *This method is usable ONLY with binary relations
	 */
	public Vector<int[]> getBinDirs(int node, HashMap<Integer,TreeSet<int[]> >[] index){
		Vector<int[]> result = new Vector<int[]>();
		for (int[] t : index[0].get(node))
			result.addElement(t);
		return result;
	}


    
    public String getDefaultVal(){
    	return defaultval;
    }
}
