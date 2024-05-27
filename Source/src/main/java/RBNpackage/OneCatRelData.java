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
public class OneCatRelData extends OneRelData {


	/**
	 * The default value for atoms of this relation: '?' (usual) or an integer
	 * @uml.property  name="defaultval"
	 */
	 //public String defaultval;

	/* For relations of arity 0 (globals): values contains
	 * the single key [0] (=new int[1])
	 */
	 public TreeMap<int[],Integer> values;  
	 
	
	 /*
		 * Contains for each argument (position) of this relation
		 * a HashMap that maps node (integer) identifiers to the set of 
		 * tuples for which a tuple with the given node at the given
		 * position is in values.
		 * 
		 * Example: this.arity=2,
		 * Then numAtomsIndex[1].get(3) returns a the tree set of pairs 
		 * contained in values with 3 in the second position.
		 */
	 protected  HashMap<Integer,TreeSet<int[]>>[] knownValues;
	 

	public OneCatRelData() {
	}
	
	
	public OneCatRelData(CatRel r, String dv)
	{
		
		super(r,dv);
		
		values = new TreeMap<int[],Integer>(new IntArrayComparator());
		knownValues = (HashMap<Integer,TreeSet<int[]>>[])new HashMap[r.arity];
		for (int i=0;i<r.arity;i++) {
			knownValues[i]=new HashMap<Integer,TreeSet<int[]>>();
		}
	}

	public OneCatRelData(CatRel r, String dv, TreeMap<int[],Integer> vals){
		rel = r;
		defaultval = dv;
		values=vals;
		knownValues = makeIndex();
	}
	
	public OneCatRelData copy(){

		OneCatRelData result = new OneCatRelData(this.rel(),this.dv());
		result.knownValues = this.knownValues.clone();
		result.makeIndex();
		return result;
	}
	
	
	/* Returns 1 if this global relation was not already set to
	 * tv; 0 else;
	 */
	int setGlobal(Integer v){
		int result = 0;
		if (rel.arity != 0){
			throw new RuntimeException("setGlobal applied to relation of arity >0");
		}
		
		if (values.size()==0 || values.get(new int[1])!=v) 
			result=1;
		
		values.put(new int[1],v);
		return result;
	}

	void add(OneCatRelData obrd){
		if (! this.rel().equals(obrd.rel()))
			System.out.println("Warning: adding incompatible relation data in OneCatRelData");
		
		for (int[] k: obrd.values.keySet()) {
			this.values.put(k, obrd.values.get(k));
			this.addToIndex(k,knownValues);
		}

	}

	void add(int[][] tuples, int v){
		for (int i=0;i<tuples.length;i++){
			add(tuples[i],v);
		}
	}

	/* adds value v for tuple; 
	 * Returns -1 if tuple already had a value, otherwise 1
	 */
	public int add(int[] tuple, int v)
	{
		int retval = 1;
		
		if (this.values.get(tuple) != null)
			retval = -1;
		this.values.put(tuple, v);
		this.addToIndex(tuple,knownValues);
		
		return retval;
	}


	/** Returns all tuples for which a value is given ('allTrue' is a legacy name ...)
	 */ 
	public TreeSet<int[]> allTrue(){
		TreeSet<int[]> result = new TreeSet<int[]>(new IntArrayComparator());
		for (int[] k: this.values.keySet())
			result.add(k);
		return result;
	}

	public TreeSet<int[]> allTrue(String[] args){
		return this.allTrue(args, knownValues);
	}

	public Vector<String[]> allTrue(RelStruc A){
		
		Vector<String[]> result = new Vector<String[]>();

		for (int[] k: values.keySet())
			result.add(A.namesAtAsArray(k));
		return result;
	}

	public int numtrue(){
		return values.size();
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
			if (!values.containsKey(nextatom) )
				result.add(nextatom);
		}
		return result;

	}


	public void delete(int a){
		this.delete(a, knownValues, values);
	}


	public void delete(int[] tuple)
	{
		values.remove(tuple);
		removeFromIndex(tuple,knownValues);
	}

	public void delete(int[][] tuples)
	{
		for (int i=0;i<tuples.length;i++){
			delete(tuples[i]);
		}

	}


	public CatRel rel(){
		return (CatRel)rel;
	}

	public String dv(){
		return defaultval;
	}

	public void setDV(String newdv){
		defaultval = newdv;
	}

	public String printAsString(RelStruc A, String pref){
		/* pref is a string prefixed to every result line
		 * used for example to prefix the gnuplot comment symbol
		 * when result is written into a logfile used for plotting
		 */
		String result = "";

		for (int [] k: values.keySet()) {
			result = result + pref +  rel.name.name
					+ A.namesAt(k)+ values.get(k)
					+ '\n';
		}
		return result;
	}

	int valueOf(int[] tuple)
	{
		if (values.containsKey(tuple)) 
			return values.get(tuple);
		else
			return -1;

	}


	public boolean isEmpty(){
		if (values.size()>0) return false;
		else return true;
	}

	/**Returns the binary tuples from the specified node to some other node
	 *This method is usable ONLY with binary relations
	 */
	public Vector<int[]> getBinDirs(int node){
		return getBinDirs(node,knownValues);
	}

	public void addRelData(Element el, RelStruc struc){
		String[] argstrings = new String[(int)((CatRel)rel).numvals()];
		for (int i=0;i<argstrings.length;i++)
			argstrings[i]="";
		
		for (int[] t: values.keySet()) {
			argstrings[values.get(t)]=argstrings[values.get(t)] + struc.namesAt(t);
		}

		for (int i=0; i<(int)((CatRel)rel).numvals();i++) {
			if (!argstrings[i].contentEquals("")) {
				Element dl = el.addElement("d");
				dl.addAttribute("rel", rel.name.name);
				dl.addAttribute("args", argstrings[i]);
				dl.addAttribute("val", ((CatRel)rel).get_String_val(i));
			}
		}
	}
	

	/**
	 * Replaces all arguments b of values 
	 * by b-1 if b>a (needed after the deletion of node with index a from
	 * the underlying SparseRelStruc)
	 * @param a
	 */
	public void shiftArgs(int a){
		int[] oldcurrtuple;
		Vector<int[]> tuplesforremoval = new Vector<int[]>();
		Vector<int[]> tuplesforinsertion = new Vector<int[]>();
		Vector<Integer> valuesforinsertion = new Vector<Integer>();
		
		if (rel.arity != 0){
			for (int[] currtuple: values.keySet()) {
				Integer value = values.get(currtuple);
				oldcurrtuple = (int[])currtuple.clone();
				rbnutilities.arrayShiftArgs(currtuple,a);
				if(rbnutilities.arrayCompare(oldcurrtuple, currtuple) !=0){
					tuplesforremoval.add(oldcurrtuple);
					tuplesforinsertion.add(currtuple);	
					valuesforinsertion.add(value);
				}
			}
			for(int i=0;i <tuplesforremoval.size();i++ ){
				values.remove(tuplesforremoval.elementAt(i));
				removeFromIndex(tuplesforremoval.elementAt(i), knownValues);
			}
			for(int i=0;i <tuplesforinsertion.size();i++ ){
				values.put(tuplesforinsertion.elementAt(i), valuesforinsertion.elementAt(i));
				addToIndex(tuplesforinsertion.elementAt(i), knownValues); 
			}
		}
		makeIndex();
	}
	
	 
	 public OneCatRelData[] randomSplit(int numfolds, RelStruc rs){
		 OneCatRelData[] result = new OneCatRelData[numfolds];
		 
		 /* Could not construct an array of TreeSet<int[]> ! 
		  * Therefore vector, even though length is known to
		  * be numfolds
		  */
		 Vector<TreeMap<int[],Integer>> split_values = new Vector<TreeMap<int[],Integer>>();
		 
		 for (int i=0;i<numfolds;i++)
			 split_values.add(new TreeMap<int[],Integer>(new IntArrayComparator()));
		 
		 for (int[] k: values.keySet())
			 split_values.elementAt(randomGenerators.randInt(0, numfolds-1)).put(k,values.get(k));

		 for (int i=0;i<numfolds;i++){
			 result[i] = new OneCatRelData((CatRel)this.rel,this.dv(),split_values.elementAt(i));
		 }
		 return result;
	 }



	 private HashMap<Integer,TreeSet<int[]>>[] makeIndex() {
		 HashMap<Integer,TreeSet<int[]>>[] result= new HashMap[rel.arity];
		 for (int i=0;i<rel.arity;i++) {
			 result[i]=new HashMap<Integer,TreeSet<int[]>>();
		 }
		 for (int[] k : this.values.keySet()){
			 addToIndex(k,result);
		 } 
		 return result;
	 }



}