/* OneNumRelData.java 
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

import RBNutilities.*;

import mymath.MyMathOps;

import org.dom4j.Element;

/** 
 * Instantiation of OneRelData for numeric relations
 * 
 * @author jaeger
 *
 */
public class OneNumRelData extends OneRelData{

	/**
	 * @uml.property  name="rel"
	 * @uml.associationEnd  
	 */
	//NumRel rel;
	/**
	 * @uml.property  name="defaultval"
	 */

	private TreeMap<int[], Double> numAtoms ;

	private  HashMap<Integer,TreeSet<int[]>>[] numAtomsIndex;
	
	/* Stores the minimum minmax[0] and maximum minmax[1] 
	 * values contained in this relation
	 */
	double[] minmax;
	
	OneNumRelData()
	{
	}
	
	public NumRel rel(){
		return (NumRel)rel;
	}

	
	public OneNumRelData(NumRel r, Double v)
	{	
//		rel = r;
//		defaultval = String.valueOf(v);
		super(r,String.valueOf(v));
		numAtoms  = new TreeMap<int[], Double>(new IntArrayComparator());
		numAtomsIndex = (HashMap<Integer,TreeSet<int[]>>[]) new HashMap[r.arity];
		for (int i=0; i<r.arity; i++) {
			numAtomsIndex[i]=new HashMap<Integer,TreeSet<int[]>>();
		}
		minmax = new double[2];		
		minmax[0]=Double.NaN;
		minmax[1]=Double.NaN;
	}

	public OneNumRelData(NumRel r, String dv)
	{
//		rel = r;
//		defaultval = dv;
		super(r,dv);
		numAtoms  = new TreeMap<int[], Double>(new IntArrayComparator());
		numAtomsIndex = (HashMap<Integer,TreeSet<int[]>>[]) new HashMap[r.arity];
		for (int i=0; i<r.arity; i++) {
			numAtomsIndex[i]=new HashMap<Integer,TreeSet<int[]>>();
		}
		minmax = new double[2];
		minmax[0]=Double.NaN;
		minmax[1]=Double.NaN;
	}

	public OneNumRelData(NumRel r, String v, TreeMap<int[],Double> ht)
	{	
		rel = r;
		defaultval = v;
		numAtoms  = ht;
		numAtomsIndex = (HashMap<Integer,TreeSet<int[]>>[]) new HashMap[r.arity];
		for (int i=0; i<r.arity; i++) {
			numAtomsIndex[i]=new HashMap<Integer,TreeSet<int[]>>();
		}
		
		minmax = new double[2];		
		minmax[0]=Double.NaN;
		minmax[1]=Double.NaN;
	}
	
	/* Returns 1 if this global relation was not already set to
	 * tv; 0 else;
	 */

	//not modified yet...

	public OneNumRelData copy(){
		OneNumRelData result = new OneNumRelData(this.rel(),this.dv());
		TreeMap<int[], Double> ht = new TreeMap<int[], Double>(new IntArrayComparator());
//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{	
//			int[] key = e.nextElement();
//			ht.put(key, (Double)numAtoms.get(key));
//		}
		for (int[] k: numAtoms.keySet())
			ht.put(k, (Double)numAtoms.get(k));
		
		result.setNumAtoms(ht);
		result.setMinMax(this.minmax);
		return result;
	}

	int setGlobal(Double v){
		
		if (rel.arity != 0){
			throw new RuntimeException("setGlobal applied to relation of arity >0");
		}

		int[] key = new int[0];
		if (numAtoms.containsKey(key) && numAtoms.get(key)==v)
			return 1;
		
		numAtoms  = new TreeMap<int[], Double>(new IntArrayComparator());
		numAtoms.put(key,v);
		
		return -1;
	}


	void setNumAtoms(TreeMap<int[], Double> ht){
		numAtoms = ht;
		updateMinMax();
	}

	void setMinMax(double[] mm){
		minmax[0]=mm[0];
		minmax[1]=mm[1];
	}
	
	void add(OneNumRelData onrd){
		if (! this.rel().equals(onrd.rel()))
			System.out.println("Warning: adding incompatible relation data in OneNumRelData");
		

//		for (Enumeration<int[]> onrdkeys = onrd.getKeys(); onrdkeys.hasMoreElements();){
//			int[] k=onrdkeys.nextElement();
//			numAtoms.put(k, onrd.valueOf(k));
//			addToIndex(k,numAtomsIndex);
//		}
		for (int[] k: onrd.getKeys()) {
			numAtoms.put(k, onrd.valueOf(k));
			addToIndex(k,numAtomsIndex);
		}
	}
	
	void add(int[][] tuples, Double v){
		for (int i=0;i<tuples.length;i++){
			add(tuples[i],v);
		}
	}

	/* adds tuple; 
	 * Returns -1 if tuple was already there, otherwise 1
	 */
	public int add(int[] tuple, Double v)
	{
		if (rel.getArity()==0){
			return setGlobal(v);
		}

		if (!numAtoms.containsKey(tuple) ) {
			numAtoms.put(tuple, v);
			addToIndex(tuple,numAtomsIndex);
			updateMinMax(v,true);
			return 1;
		}
		else if(numAtoms.containsKey(tuple) && numAtoms.get(tuple) == v){
			return 1;
		}
		else if(numAtoms.containsKey(tuple) && !(numAtoms.get(tuple) == v)){
			double oldval = numAtoms.get(tuple);
			numAtoms.remove(tuple);	
			updateMinMax(oldval,false);					
			numAtoms.put(tuple, v);
			addToIndex(tuple,numAtomsIndex);
			updateMinMax(v,true);
			return 1;
		}
		return 0;
	}


	/** Returns all the atoms in hashtable as 
	 * a vector of int[]. Objects are represented by
	 * their internal index
	 * 
	 */
	public TreeSet<int[]> allTrue(){

		TreeSet<int[]> result = new TreeSet<int[]>(new IntArrayComparator());

//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{
//			result.add(e.nextElement());
//		}
		for (int[] k: numAtoms.keySet())
			result.add(k);
		return result;
	}
	
	public TreeSet<int[]> allTrue(String[] args){
		
		Vector<TreeSet<int[]>> slices = new Vector<TreeSet<int[]>>();
		boolean existsnull=false;
		
		for (int i=0;i<args.length;i++) {
			if (rbnutilities.IsInteger(args[i])) {
				TreeSet<int[]> slicefori = numAtomsIndex[i].get(Integer.parseInt(args[i]));
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
//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{
//			result.add(A.namesAtAsArray(e.nextElement()));
//		}
		for (int[] k: numAtoms.keySet())
			result.add(A.namesAtAsArray(k));
		return result;
	}
	
	
	/* Differs from allTrue(RelStruc) slightly w.r.t
	 * format of return value (String vs. String[])!
	 */
//	public Vector<String> allTrueAtoms(RelStruc A){
//
//		Vector<String> result = new Vector<String>();
//
//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{
//			result.add(A.namesAt(e.nextElement()));
//		}
//		return result;
//
//	}
	
//	public Vector<int[]> allNumAttr(){
//
//		Vector<int[]> result = new Vector<int[]>();
//
//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{
//			String key = e.nextElement().toString();
//			result.add(myio.StringOps.stringToIntArray(key));
//		}
//		return result;
//
//	}
	
	public double[] minMax(){
		return minmax.clone();
	}
	
	private void updateMinMax(){
		if (numAtoms.size()==0){
			minmax[0]=Double.NaN;
			minmax[1]=Double.NaN;
		}
		else {
			minmax[0]=Double.MAX_VALUE;
			minmax[1]=Double.MIN_VALUE;
//			for(Enumeration<Double>  e = numAtoms.elements(); e.hasMoreElements();)
//			{
//				Double nextval = e.nextElement();
//				minmax[0]=Math.min(minmax[0], nextval);
//				minmax[1]=Math.max(minmax[1], nextval);
//			}
			for (double v: numAtoms.values()) {
				minmax[0]=Math.min(minmax[0], v);
				minmax[1]=Math.max(minmax[1], v);
			}
				
		}
	}

	private void updateMinMax(double v, boolean addition){
		if (addition == true){ // value v is added
			if (!Double.isNaN(minmax[0]))
				minmax[0]=Math.min(minmax[0], v);
			else minmax[0]=v;
			if (!Double.isNaN(minmax[1]))
				minmax[1]=Math.max(minmax[1], v);
			else minmax[1]=v;
		}
		else // value v is deleted
			if (minmax[0]==v || minmax[1]== v)
				updateMinMax();
	}

	/** Returns all the atoms in hashtable that have values greater than a value v as 
	 * a vector of int[]. Objects are represented by
	 * their internal index
	 * 
	 */
	public Vector<int[]> greaterThan(Double v){

		Vector<int[]> result = new Vector<int[]>();

//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{
//			int[] k = e.nextElement();
//			Double value = numAtoms.get(k);
//			if( value > v){
//				result.add(k);
//			}
//		}
		for (int[] k: numAtoms.keySet()) {
			if (numAtoms.get(k)>v)
				result.add(k);
		}
		return result;

	}
	/** Returns all the atoms in hashtable that have lower greater than a value v as 
	 * a vector of int[]. Objects are represented by
	 * their internal index
	 * 
	 */
	public Vector<int[]> lowerThan(Double v){

		Vector<int[]> result = new Vector<int[]>();

//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{
//			int[] k = e.nextElement();
//			Double value = numAtoms.get(k);
//			if( value > v){
//				result.add(k);
//			}
//		}
		for (int[] k: numAtoms.keySet()) {
			if (numAtoms.get(k)<v)
				result.add(k);
		}
		return result;
	}

	/** Returns all the atoms in hashtable that have values equal to a value v as 
	 * a vector of int[]. Objects are represented by
	 * their internal index
	 * 
	 */	
	public Vector<int[]> equals(Double v){

		Vector<int[]> result = new Vector<int[]>();

//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{
//			int[] k = e.nextElement();
//			Double value = numAtoms.get(k);
//			if( value > v){
//				result.add(k);
//			}
//		}
		for (int[] k: numAtoms.keySet()) {
			if (numAtoms.get(k)==v)
				result.add(k);
		}
		return result;
	}
	public int numOfNumAtoms(){
		return numAtoms.size();
	}

	/*
	 * Returns all the atoms for which numAtoms contains
	 * no value. d is the domainsize, i.e.
	 * the maximal index of an object to be considered.
	 */	 

	public Vector<int[]>  allUnInstantiated(int d){

		Vector<int[]>  result = new Vector<int[]> ();
		int[] nextatom;
		for (int i=0;i< MyMathOps.intPow(d,rel.getArity());i++){
			nextatom = rbnutilities.indexToTuple(i,rel.getArity(),d);
			if (!numAtoms.containsKey(nextatom) )
				result.add(nextatom);
		}
		return result;
	}



//	/** Returns all the atoms instantiated to numeric as 
//	 * a vector of strings. Objects are represented by
//	 * their name in structure A
//	 */ 
//	public Vector<String> allNum(RelStruc A){
//		Vector<String> result = new Vector<String>();
//
//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{
//			int[] k = e.nextElement();
//			result.add(A.namesAt(k));	
//		}
//		return result;
//	}

	/** Delete all atoms containing a 
	 * @param a
	 */
	public void delete(int a){
		int[] nextatom;

//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{
//			int[] k = e.nextElement();
//			
//			if (rbnutilities.inArray(k,a)){
//				if(numAtoms.containsKey(k)){
//					updateMinMax(numAtoms.get(k),false);
//					numAtoms.remove(k);
//				}
//			}
//		}
		for (int[] k: numAtoms.keySet() ) {
			if (rbnutilities.inArray(k,a)){
				if(numAtoms.containsKey(k)){
					updateMinMax(numAtoms.get(k),false);
					numAtoms.remove(k);
				}
			}
		}
	}

	public void delete(int[] tuple)
	{
		
		if(numAtoms.containsKey(tuple)){
			double value = numAtoms.get(tuple);
			numAtoms.remove(tuple);
			updateMinMax(value,false);
		}
	}


	public void delete(int[][] tuples)
	{
		for (int i=0;i<tuples.length;i++){
			delete(tuples[i]);
		}

	}


	public String printAsString(RelStruc A, String pref){
		/* pref is a string prefixed to every result line
		 * used for example to prefix the gnuplot comment symbol
		 * when result is written into a logfile used for plotting
		 */
		String result = "";


//		for (Enumeration<int[]> e = numAtoms.keys();e.hasMoreElements();) {
//			int[] k = e.nextElement();
//			
//			result = result + pref +  rel.name.name
//			+ A.namesAt(k)+ numAtoms.get(k)
//			+ '\n';
//		}

		for (int [] k: numAtoms.keySet()) {
			result = result + pref +  rel.name.name
					+ A.namesAt(k)+ numAtoms.get(k)
					+ '\n';
		}
		return result;
	}

	int truthValueOf(int[] tuple)
	{
		if (rel.arity ==0){
			if (numAtoms.size() > 0){
				return 1;
			}
			else{
				return -1;
			}
		}
		else {
			int result = -1;
			
			if (numAtoms.containsKey(tuple)) result = 1;
			if (result == -1 && defaultval.equals(0.0)){
				result =0;
			}
			return result;
		}
	}

	public double valueOf(int[] key){
		if (numAtoms.containsKey(key))
			return numAtoms.get(key);
		else {
			if (defaultval != "?")
				return Double.valueOf(defaultval);
		}

		return Double.NaN;

	}



//	public double valueOf(int[] tuple){
//		String key;
//		if (rel.getArity() > 0)
//			key = rbnutilities.arrayToString(tuple);
//		else 
//			key = "";
//		return valueOf(key);
//	}
	
	public boolean isEmpty(){
		if (numAtoms.size()>0 ) return false;
		else return true;
	}

	/**Returns the binary tuples from the specified node to some other node
	 *This method is usable ONLY with binary relations
	 */
	public Vector<int[]> getBinDirs(int node){
		Vector<int[]> hits = new Vector<int[]>();

//		for (Enumeration<int[]> e = numAtoms.keys();e.hasMoreElements();) {
//			int[] temp = e.nextElement();
//			if(temp[0] == node)
//				hits.addElement(temp);
//		}
		for (int[] k: numAtoms.keySet()) {
			if(k[0] == node)
				hits.addElement(k);
		}
		return hits;
	}

	public void addRelData(Element el, RelStruc struc){
//		for (Enumeration <int[]> e = numAtoms.keys();e.hasMoreElements();) {
//			int[] k = e.nextElement();
//			Double value = numAtoms.get(k);
//			Element dl = el.addElement("d");
//			dl.addAttribute("rel", rel.name.name);
//			dl.addAttribute("args", struc.namesAt(k));
//			dl.addAttribute("val", value.toString());
//		}

		for (int[] k: numAtoms.keySet()) {
			Double value = numAtoms.get(k);
			Element dl = el.addElement("d");
			dl.addAttribute("rel", rel.name.name);
			dl.addAttribute("args", struc.namesAt(k));
			dl.addAttribute("val", value.toString());
		}
	}


	//shared
	/**
	 * Replaces all arguments b of trueAtoms and falseAtoms lists
	 * by b-1 if b>a (needed after the deletion of node with index a from
	 * the underlying SparseRelStruc)
	 * @param a
	 */
	public void shiftArgs(int a){
		//int[] currtuple;
		int[] oldcurrtuple;
		Vector<int[]> tuplesforremoval = new Vector<int[]>();
		Vector<int[]> tuplesforinsertion = new Vector<int[]>();
		Vector<Double> valuesforinsertion = new Vector<Double>();
		
		if (rel.arity != 0){
//			for (Enumeration<int[]> e = numAtoms.keys();e.hasMoreElements();) {
//				currtuple = e.nextElement();
//				Double value = numAtoms.get(currtuple);
//
//				oldcurrtuple = (int[])currtuple.clone();
//				rbnutilities.arrayShiftArgs(currtuple,a);
//				if(rbnutilities.arrayCompare(oldcurrtuple, currtuple) !=0){
//					tuplesforremoval.add(oldcurrtuple);
//					tuplesforinsertion.add(currtuple);	
//					valuesforinsertion.add(value);
//				}

//			}
			for (int[] currtuple: numAtoms.keySet()) {
				Double value = numAtoms.get(currtuple);

				oldcurrtuple = (int[])currtuple.clone();
				rbnutilities.arrayShiftArgs(currtuple,a);
				if(rbnutilities.arrayCompare(oldcurrtuple, currtuple) !=0){
					tuplesforremoval.add(oldcurrtuple);
					tuplesforinsertion.add(currtuple);	
					valuesforinsertion.add(value);
				}
			}
			for(int i=0;i <tuplesforremoval.size();i++ ){
				numAtoms.remove(tuplesforremoval.elementAt(i));
			}
			for(int i=0;i <tuplesforinsertion.size();i++ ){
				numAtoms.put(tuplesforinsertion.elementAt(i), valuesforinsertion.elementAt(i));
			}
		}
	}
	
	public Set<int[]> getKeys(){
		return numAtoms.keySet();
	}
	
//	public void delete(int[] tuple, Double v) {
//		//if(numAtoms.get(rbnutilities.arrayToString(tuple))==v){
//		numAtoms.remove(rbnutilities.arrayToString(tuple));
//		//}
//
//	}
	
	/* Resets values of all atoms in numAtoms to random values */
	public void setRandom(double scale){
		TreeMap<int[], Double> newht = new TreeMap<int[],Double>(new IntArrayComparator());

//		for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		{
//			int[] k = e.nextElement();
//			newht.put(k, randomGenerators.getRandom(((NumRel)rel).minval(),((NumRel)rel).maxval(),scale));
//		}
		for (int[] k: numAtoms.keySet()) {
			newht.put(k, randomGenerators.getRandom(((NumRel)rel).minval(),((NumRel)rel).maxval(),scale));
		}
		setNumAtoms(newht);
	}
	
	/* Resets value for the atom with key args to random value */
	public void setRandom(int[] args, double scale){
		numAtoms.remove(args);
		numAtoms.put(args,randomGenerators.getRandom(((NumRel)rel).minval(),((NumRel)rel).maxval(),scale) );
	}
	
	
	 public OneNumRelData[] randomSplit(int numfolds, RelStruc rs){
		 OneNumRelData[] result = new OneNumRelData[numfolds];
		 
		 /* Could not construct an array of TreeSet<int[]> ! 
		  * Therefore vector, even though length is known to
		  * be numfolds
		  */
		 Vector<TreeMap<int[],Double>> numats = new Vector<TreeMap<int[],Double>>();	
		 for (int i=0;i<numfolds;i++)
			 numats.add(new TreeMap<int[],Double>(new IntArrayComparator()));
		 
//		 for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();)
//		 {
//			 int[] k  = e.nextElement();
//			 numats.elementAt(randomGenerators.randInt(0, numfolds-1)).put(k,numAtoms.get(k));
//		 }
		 for (int[] k: numAtoms.keySet())
			 numats.elementAt(randomGenerators.randInt(0, numfolds-1)).put(k,numAtoms.get(k));

		 for (int i=0;i<numfolds;i++){
			 result[i] = new OneNumRelData((NumRel)this.rel,this.dv(),numats.elementAt(i));
		 }

		 return result;
	 }



	 public void print(){
		 System.out.println("Relation: " + rel.name());

//		 for(Enumeration<int[]>  e = numAtoms.keys();e.hasMoreElements();){
//			 int[] k = e.nextElement();
//			 System.out.println("key " + k + " value: " + numAtoms.get(k));
//		 }
		 for (int[] k: numAtoms.keySet())
			 System.out.println("key " + k + " value: " + numAtoms.get(k));
	 }
}