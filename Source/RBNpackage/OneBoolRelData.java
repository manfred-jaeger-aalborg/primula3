/* OneBoolRelData.java 
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
import RBNutilities.randomGenerators;
import RBNutilities.rbnutilities;
import org.dom4j.Element;

/** 
 * Instantiation of OneRelData for Boolean relations
 * 
 * @author jaeger
 *
 */
public class OneBoolRelData extends OneRelData {

	/**
	 * @uml.property  name="rel"
	 * @uml.associationEnd  
	 */
	//public BoolRel rel;
	/**
	 * The default value for atoms of this relation: 'false' or '?'
	 * @uml.property  name="defaultval"
	 */
	 //public String defaultval;

	
	 private TreeSet<int[]> trueAtoms;  
	 private TreeSet<int[]> falseAtoms; 
	 
	 protected  HashMap<Integer,TreeSet<int[]>>[] trueAtomsIndex;
	 protected  HashMap<Integer,TreeSet<int[]>>[] falseAtomsIndex; 
	 
	/* For relations of arity 0 (globals): r()=true is
	 * represented by trueAtoms = ([0]) , falseAtoms = ();
	 * r() = false is represented by trueAtoms = (), falseAtoms = ([0])
	 * r() uninstantiated is represented by trueAtoms = (), falseAtoms = ()
	 */

	public OneBoolRelData() {
	}
	
	
	public OneBoolRelData(BoolRel r, String dv)
	{
		
//		rel = r;
//		defaultval = dv;
		
		super(r,dv);
		
		trueAtoms = new TreeSet<int[]>(new IntArrayComparator());
		falseAtoms = new TreeSet<int[]>(new IntArrayComparator());
		trueAtomsIndex = (HashMap<Integer,TreeSet<int[]>>[]) new HashMap[r.arity];
		falseAtomsIndex = (HashMap<Integer,TreeSet<int[]>>[]) new HashMap[r.arity];
		for (int i=0;i<r.arity;i++) {
			trueAtomsIndex[i]=new HashMap<Integer,TreeSet<int[]>>();
			falseAtomsIndex[i]=new HashMap<Integer,TreeSet<int[]>>();
		}
		
		
	}

	public OneBoolRelData(BoolRel r, String dv, TreeSet<int[]> tats, TreeSet<int[]> fats){

		rel = r;
		defaultval = dv;
		trueAtoms = tats;
		falseAtoms = fats;
		trueAtomsIndex = (HashMap<Integer,TreeSet<int[]>>[]) new HashMap[r.arity];
		falseAtomsIndex = (HashMap<Integer,TreeSet<int[]>>[]) new HashMap[r.arity];
		for (int i=0;i<r.arity;i++) {
			trueAtomsIndex[i]=new HashMap<Integer,TreeSet<int[]>>();
			falseAtomsIndex[i]=new HashMap<Integer,TreeSet<int[]>>();
		}
		makeIndex(true);
		makeIndex(false);
		
	}
	
	public OneBoolRelData copy(){

		OneBoolRelData result = new OneBoolRelData(this.rel(),this.dv());

		for (Iterator<int[]> it = this.trueAtoms.iterator();it.hasNext();)
			result.trueAtoms.add(rbnutilities.clonearray(it.next()));
		for (Iterator<int[]> it = this.falseAtoms.iterator();it.hasNext();)
			result.falseAtoms.add(rbnutilities.clonearray(it.next()));
		result.makeIndex(true);
		result.makeIndex(false);
		return result;
	}
	
	
	/* Returns 1 if this global relation was not already set to
	 * tv; 0 else;
	 */
	int setGlobal(boolean tv){
		int result = 0;
		if (rel.arity != 0){
			throw new RuntimeException("setGlobal applied to relation of arity >0");
		}
		if (tv){
			if (trueAtoms.size()==0){
				falseAtoms = new TreeSet<int[]>(new IntArrayComparator());
				trueAtoms.add(new int[1]);
				result = 1;
			}
		}
		else {
			if (falseAtoms.size()==0){
				trueAtoms = new TreeSet<int[]>(new IntArrayComparator());
				falseAtoms.add(new int[1]);
				result = 1;
			}			

		}
		return result;
	}

	void add(OneBoolRelData obrd){
		if (! this.rel().equals(obrd.rel()))
			System.out.println("Warning: adding incompatible relation data in OneNumRelData");
		
		TreeSet<int[]> obrdalltrue = obrd.allTrue();
		for (Iterator<int[]> i = obrdalltrue.iterator(); i.hasNext();)
			add(i.next(),true);


		if (!this.defaultval.equals("false")){
			TreeSet<int[]> obrdallfalse = obrd.allFalse(null);
			for (Iterator<int[]> i = obrdallfalse.iterator(); i.hasNext();)
				add(i.next(),false);
		}

	}

	void add(int[][] tuples, boolean tv){
		for (int i=0;i<tuples.length;i++){
			add(tuples[i],tv);
		}
	}

	/* adds tuple; 
	 * Returns -1 if tuple was already there, otherwise 1
	 */
	public int add(int[] tuple, boolean tv)
	{
		delete(tuple,!tv);
		TreeSet<int[]> atoms;
		HashMap<Integer,TreeSet<int[]>>[] index;
		if (tv) {
			atoms = trueAtoms;
			index=trueAtomsIndex;
		}
		else {
			atoms = falseAtoms;
			index=falseAtomsIndex;
		}
		if (atoms.contains(tuple))
			return -1;
		else {
			atoms.add(tuple);
			addToIndex(tuple,index);
			return 1;
		}
	}


	/** Returns all the atoms instantiated to true as 
	 * a vector of int[]. Objects are represented by
	 * their internal index
	 * 
	 * NOTE: calling classes should be modified so that this can 
	 * be returned as 
	 * the TreeSet
	 */ 
	public TreeSet<int[]> allTrue(){
		return trueAtoms;
	}

	public TreeSet<int[]> allTrue(String[] args){
		Vector<TreeSet<int[]>> slices = new Vector<TreeSet<int[]>>();
		boolean existsnull=false;
		
		for (int i=0;i<args.length;i++) {
			if (rbnutilities.IsInteger(args[i])) {
				TreeSet<int[]> slicefori = trueAtomsIndex[i].get(Integer.parseInt(args[i]));
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

	public Vector<String[]> allTrue(RelStruc A){
		Vector<String[]> result = new Vector<String[]>();
		for (Iterator<int[]> it = trueAtoms.iterator();it.hasNext();){
			result.add(A.namesAtAsArray(it.next()));
		}
		return result;
	}

	public int numtrue(){
		return trueAtoms.size();
	}

	public int numfalse(){
		return falseAtoms.size();
	}

	/** Returns all the atoms instantiated to false as 
	 * a vector of int[]. Objects are represented by
	 * their internal index
	 * 
	 * For the case that the defaultvalue of this relation is "false",
	 * one needs to supply as argument the input structure
	 */ 
	public TreeSet<int[]> allFalse(RelStruc rs){
		TreeSet<int[]> result = new TreeSet<int[]>(new IntArrayComparator());
		if (defaultval.equals("?"))
			result=falseAtoms; 
		else { // defaultval = "false"
			if (rs != null){
				Vector<int[]> elementsForCoordinate = new Vector<int[]>();
				Type[] types = rel.getTypes();
				for (int i=0;i<rel.arity;i++){
					elementsForCoordinate.add( rbnutilities.intArrVecToArr(rs.allTrue(types[i])));
				}

				int[][] candidatetuples = rbnutilities.cartesProd(elementsForCoordinate);
				int[] nextatom;
				for (int i=0;i< candidatetuples.length ;i++){
					nextatom = candidatetuples[i];
					if (!trueAtoms.contains(nextatom))
						result.add(nextatom);
				}
			}
			else // rs == null
				System.out.println("Warning: trying to compute allFalse for relation with default 'false' without RelStruc. Returning empty result.");
		}
		return result;
	}

	/** Returns all the atoms which are not instantiated
	 * to either true or false. d is the domainsize, i.e.
	 * the maximal index of an object to be considered.
	 */
	public Vector<int[]>  allUnInstantiated(int d){
		Vector<int[]>  result = new Vector<int[]> ();
		int[] nextatom;
		for (int i=0;i< MyMathOps.intPow(d,rel.getArity());i++){
			nextatom = rbnutilities.indexToTuple(i,rel.getArity(),d);
			if (!trueAtoms.contains(nextatom) && !falseAtoms.contains(nextatom))
				result.add(nextatom);
		}
		return result;
	}

	/** Returns all the atoms instantiated to true as 
	 * a vector of strings. Objects are represented by
	 * their name in structure A
	 */ 
	public Vector<String> allTrueAtoms(RelStruc A){
		Vector<String>  result = new Vector<String> ();
		for (Iterator<int[]> it = trueAtoms.iterator();it.hasNext();){
			result.add(A.namesAt(it.next()));
		}
		return result;
	}

	/** Returns all the atoms instantiated to false as 
	 * a vector of strings. Objects are represented by
	 * their name in structure A
	 */ 
//	public Vector<String>  allFalse(RelStruc A){
//		Vector<String>  result = new Vector<String> ();
//		for (Iterator<int[]> it = falseAtoms.iterator();it.hasNext();){
//			result.add(A.namesAt(it.next()));
//		}
//		return result;
//	}

	/** Delete all atoms containing a 
	 * @param a
	 */
	

	// TODO this should be re-implemented using the index
	public void delete(int a){
		int[] nextatom;
		Vector<int[]> atomsforremoval = new Vector<int[]>();

		for (Iterator<int[]> it = trueAtoms.iterator();it.hasNext();){
			nextatom = it.next();
			if (rbnutilities.inArray(nextatom,a)) //it.remove();
				atomsforremoval.add(nextatom);
				
			
		}
		for (Iterator<int[]> it = falseAtoms.iterator();it.hasNext();){
			nextatom = it.next();
			if (rbnutilities.inArray(nextatom,a))
				it.remove();
		}
		
		
		for(int i=0;i<atomsforremoval.size();i++){
			trueAtoms.remove(atomsforremoval.elementAt(i));
		
		}
		makeIndex(true);
		makeIndex(false);
	}

	protected void delete(int[] tuple,boolean tv)
	{
		TreeSet<int[]> atoms;
		HashMap<Integer,TreeSet<int[]>>[] index;
		if (tv) {
			atoms = trueAtoms;
			index=trueAtomsIndex;
		}
		else {
			atoms = falseAtoms;
			index=falseAtomsIndex;
		}
		atoms.remove(tuple);
		removeFromIndex(tuple, index);
	}


	public void delete(int[][] tuples,boolean tv)
	{
		TreeSet<int[]> atoms;
		if (tv) atoms = trueAtoms;
		else atoms = falseAtoms;
		for (int i=0;i<tuples.length;i++)
			atoms.remove(tuples[i]);
	}


	public BoolRel rel(){
		return (BoolRel)rel;
	}

	public String dv(){
		return defaultval;
	}

	public void setDV(String newdv){
		defaultval = newdv;
		if (newdv.equals("false"))
				falseAtoms = new TreeSet<int[]>(new IntArrayComparator());
	}

	public String printAsString(RelStruc A, String pref){
		/* pref is a string prefixed to every result line
		 * used for example to prefix the gnuplot comment symbol
		 * when result is written into a logfile used for plotting
		 */
		String result = "";
		for (Iterator<int[]> it = trueAtoms.iterator();it.hasNext();){
			result = result + pref +  rel.name.name
			+ A.namesAt(it.next())+ " = true"
			+ '\n';
		}
		for (Iterator<int[]> it = falseAtoms.iterator();it.hasNext();){
			result = result + pref +  rel.name.name
			+ A.namesAt(it.next())+ " = false"
			+ '\n';
		}
		return result;
	}

	int truthValueOf(int[] tuple)
	{
		if (rel.arity ==0){
			if (trueAtoms.size() > 0)
				return 1;
			if (falseAtoms.size() >0)
				return 0;
			return -1;
		}
		else {
			int result = -1;
			if (trueAtoms.contains(tuple))
				result = 1;
			if (falseAtoms.contains(tuple))
				result = 0;
			if (result == -1 && defaultval.equals("false"))
				result =0;
			return result;
		}
	}

//	public double valueOf(int[] tuple){
//		return (double)truthValueOf(tuple);
//	}
	
	public boolean isEmpty(){
		if (trueAtoms.size()>0 || falseAtoms.size()>0) return false;
		else return true;
	}

	/**Returns the binary tuples from the specified node to some other node
	 *This method is usable ONLY with binary relations
	 */
	// TODO this should be replaced by new index method
	public Vector<int[]> getBinDirs(int node){
		Vector<int[]> hits = new Vector<int[]>();
		for (Iterator<int[]> it = trueAtoms.iterator();it.hasNext();){
			int[] temp = it.next();
			if(temp[0] == node)
				hits.addElement(temp);
		}
		return hits;
	}

	public void addRelData(Element el, RelStruc struc){
		String argstring ="";
		// adding the true atoms:
		if (trueAtoms.size()>0){
			Element dl = el.addElement("d");
			dl.addAttribute("rel", rel.name.name);
			for (Iterator<int[]> it = trueAtoms.iterator();it.hasNext();){		
				if (rel.arity > 0)
					argstring=argstring+   struc.namesAt(it.next()) ;
				else
				{
					argstring=argstring+ "()";
					it.next();
				}
				dl.addAttribute("args", argstring);
				dl.addAttribute("val", "true");
			}
		}
		// adding the false atoms

		if (defaultval != "false" && falseAtoms.size()>0){
			Element df = el.addElement("d");
			df.addAttribute("rel", rel.name.name);
			argstring = "";
			for (Iterator<int[]> it = falseAtoms.iterator();it.hasNext();){
				if (rel.arity > 0)
					argstring=argstring+ struc.namesAt(it.next()) ;
				else
				{
					argstring=argstring+ "()";
					it.next();
				}		
			}
			df.addAttribute("args", argstring);
			df.addAttribute("val", "false");
		}

	}
	
	

	/**
	 * Replaces all arguments b of trueAtoms and falseAtoms lists
	 * by b-1 if b>a (needed after the deletion of node with index a from
	 * the underlying SparseRelStruc)
	 * @param a
	 */
	public void shiftArgs(int a){
		int[] currtuple;
		int[] oldcurrtuple;
		
		
		ArrayList<Boolean> bools = new ArrayList<Boolean>();
		bools.add(true);
		bools.add(false);

		for (Iterator<Boolean> b=bools.iterator();b.hasNext();) {
			Vector<int[]> tuplesforremoval = new Vector<int[]>();
			Vector<int[]> tuplesforinsertion = new Vector<int[]>();
			
			boolean bval = b.next();
			TreeSet<int[]> atoms = null;
			if (bval) {
				atoms = trueAtoms;
			}
			else
				atoms = falseAtoms;
			for (Iterator<int[]> it = atoms.iterator();it.hasNext();){
				currtuple = it.next();
				oldcurrtuple = (int[])currtuple.clone();
				rbnutilities.arrayShiftArgs(currtuple,a);

				if(rbnutilities.arrayCompare(oldcurrtuple, currtuple) !=0){
					tuplesforremoval.add(oldcurrtuple);
					tuplesforinsertion.add(currtuple);	
				}
			}
			for(int i=0;i <tuplesforremoval.size();i++ ){
				atoms.remove(tuplesforremoval.elementAt(i));
			}
			for(int i=0;i <tuplesforinsertion.size();i++ ){
				atoms.add(tuplesforinsertion.elementAt(i));
			}
		}
		
		makeIndices();
	}
	
	 public OneBoolRelData subSample(int pc, RelStruc rs){
		 if (this.defaultval.equals("false")){
			 	int[] nexttup;
			 	double rand;
			 	TreeSet<int[]> newFalseAtoms = new TreeSet<int[]>(new IntArrayComparator());
			 	TreeSet<int[]> falseats = this.allFalse(rs);
			 	
			 	for (Iterator <int[]> it = falseats.iterator(); it.hasNext(); ){
			 		nexttup = it.next();
			 		rand = Math.random();
			 		if (rand < ((double)pc)/100){
			 			newFalseAtoms.add(nexttup);
			 		}
			 	}
			 	OneBoolRelData result =  new OneBoolRelData((BoolRel)this.rel, "?", this.trueAtoms, newFalseAtoms);
			 	result.makeIndex(false);
			 	result.makeIndex(true);
			 	return result;
		 }
		 else {
			 System.out.println("Warning: subsampling for relation " + this.rel.name() + " with default value =/= false: no subsampling performed");
			 return this;
		 }
	 }
	 
	 public OneBoolRelData[] randomSplit(int numfolds, RelStruc rs){
		 OneBoolRelData[] result = new OneBoolRelData[numfolds];
		 
		 Vector<TreeSet<int[]>> trueats = new Vector<TreeSet<int[]>>();	
		 for (int i=0;i<numfolds;i++)
			 trueats.add(new TreeSet<int[]>(new IntArrayComparator()));
		 Vector<TreeSet<int[]>> falseats = new Vector<TreeSet<int[]>>();	
		 for (int i=0;i<numfolds;i++)
			 falseats.add(new TreeSet<int[]>(new IntArrayComparator()));
		 
		 for (Iterator<int[]> it = trueAtoms.iterator(); it.hasNext();){
			 trueats.elementAt(randomGenerators.randInt(0, numfolds-1)).add(it.next());
		 }
		 
		 if (this.defaultval.equals("?")){
			 for (Iterator<int[]> it = falseAtoms.iterator(); it.hasNext();){
				 falseats.elementAt(randomGenerators.randInt(0, numfolds-1)).add(it.next());
			 }
		 }
			 
		 if (this.defaultval.equals("false")){
			 TreeSet<int[]> falseatsasvec = this.allFalse(rs);
			 for (Iterator <int[]> it = falseatsasvec.iterator(); it.hasNext(); )
				 falseats.elementAt(randomGenerators.randInt(0, numfolds-1)).add(it.next());
		 }
		 for (int i=0;i<numfolds;i++){
			 result[i] = new OneBoolRelData((BoolRel)this.rel,"?",trueats.elementAt(i),falseats.elementAt(i));
			 result[i].makeIndex(false);
			 result[i].makeIndex(true);
		 }
		 
		 return result;
	 }

//	 private void addToIndex(int[] tup, HashMap<Integer,TreeSet<int[]>>[] idx) {
//		 // tup and idx have the same length rel.arity!
//		 for (int i=0;i<rel.arity;i++) {
//			 TreeSet<int[]>ts=idx[i].get(tup[i]);
//			 if (ts==null)
//				 idx[i].put(tup[i],new TreeSet<int[]>(new IntArrayComparator()));
//			 idx[i].get(tup[i]).add(tup);
//		 }
//	 }
//
//	 private void removeFromIndex(int[] tup, HashMap<Integer,TreeSet<int[]>>[] idx) {
//		 for (int i=0;i<rel.arity;i++) {
//			 TreeSet<int[]>ts=idx[i].get(tup[i]);
	 //			 if (ts!=null)
	 //				 ts.remove(tup);
	 //		 }
	 //	 }

	 private HashMap<Integer,TreeSet<int[]>>[] makeIndex(TreeSet<int[]> atoms) {
		 HashMap<Integer,TreeSet<int[]>>[] result= new HashMap[rel.arity];
		 for (int i=0;i<rel.arity;i++) {
			 result[i]=new HashMap<Integer,TreeSet<int[]>>();
		 }
		 for (Iterator<int[]> it = atoms.iterator(); it.hasNext();){
			 int[] tup = it.next();
			 addToIndex(tup,result);
		 } 
		 return result;
	 }

	 private void makeIndex(boolean tv) {
		 if (tv) {
			 trueAtomsIndex= makeIndex(trueAtoms);
		 }
		 else{
			 falseAtomsIndex= makeIndex(falseAtoms);
		 }
	 }

	 private void makeIndices() {
		 makeIndex(true);
		 makeIndex(false);
	 }
//	 private void addToIndex(int[] tup, HashMap<Integer,TreeSet<int[]>>[] idx) {
//		 // tup and idx have the same length rel.arity!
//		 for (int i=0;i<rel.arity;i++) {
//			 TreeSet<int[]>ts=idx[i].get(tup[i]);
//			 if (ts==null)
//				 idx[i].put(tup[i],new TreeSet<int[]>(new IntArrayComparator()));
//			 idx[i].get(tup[i]).add(tup);
//		 }
//	 }
	 
//	 private void removeFromIndex(int[] tup, HashMap<Integer,TreeSet<int[]>>[] idx) {
//		 for (int i=0;i<rel.arity;i++) {
//				 TreeSet<int[]>ts=idx[i].get(tup[i]);
//				 if (ts!=null)
//					 ts.remove(tup);
//			 }
//	 }
}