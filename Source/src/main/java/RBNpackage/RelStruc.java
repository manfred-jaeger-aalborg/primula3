/* RelStruc.java
 * 
 * Copyright (C) 2003 Max-Planck-Institut fuer Informatik,
 *                    Helsinki Institute for Information Technology
 *
 * contact:
 * jaeger@cs.auc.dk   www.cs.auc.dk/~jaeger/Primula.html
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
import java.awt.Color;
import java.io.*;

import RBNExceptions.*;
import RBNutilities.*;
import RBNio.*;
import RBNLearning.*;

//import com.mysql.jdbc.*;
import java.sql.*;

import myio.StringOps;

import org.dom4j.Element;

public abstract class RelStruc implements Cloneable{

	public static int BLP_FORMAT = 1;
	public static int MLN_FORMAT = 2;

	
	private Signature sig;

	/* Domain of structure is
	 * {0,...,dom-1}
	 */
	/**
	 * @uml.property  name="dom"
	 */
	public int dom;

	/**
	 * @uml.property  name="elementnames"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	Vector<String> elementnames; // Internally the domainelements of the
	// structure are taken to be the numbers
	// 0..n. 'elementnames[i]' provides an
	// alternative name for i, which has been
	// supplied by the user and is used to
	// denote i in all outputs.
	/**
	 * @uml.property  name="coordinates"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="I"
	 */
	Vector<int[]> coordinates; // stores (x,y)-coordinates for every domainelement
	// for use in graphical display of structure.

	static Rel[] OrdRels = new Rel[4];

	static{
		OrdRels[0] = new BoolRel("less",2);
		OrdRels[1] = new BoolRel("pred",2);
		OrdRels[2] = new BoolRel("zero",1);
		OrdRels[3] = new BoolRel("last",1);
	}

	//	Vector<RelInt> relinterpretations;  // Vector of RelInts

	OneStrucData mydata;
	
	/** Creates new RelStruc */
	public RelStruc(){
		elementnames       = new Vector<String>();
		coordinates        = new Vector<int[]>();
		mydata 			   = new OneStrucData();
		mydata.setParentRelStruc(this);
		sig = null;
	}

	/** Creates new RelStruc with a domain of n elements*/
	public RelStruc(int n){
		dom = n;
		elementnames       = new Vector<String>();
		coordinates        = new Vector<int[]>();
		mydata 			   = new OneStrucData();
		mydata.setParentRelStruc(this);
		sig = null;
	}

	public RelStruc(Vector<String> en, OneStrucData data, Vector<int[]> coords, Signature s) {
		//if( (en == null) || (srels == null) || (coords == null) ) throw new IllegalStateException();
		dom = en.size();
		elementnames = en;
		mydata = data;
		coordinates = coords;
		mydata.setParentRelStruc(this);
		sig = s;
	}

	/** @author keith cascio
    	@since 20060515 */
	public RelStruc( RelStruc toCopy ){
		this( toCopy.elementnames, toCopy.mydata, toCopy.coordinates , toCopy.signature());
	}

	/** @author keith cascio
    	@since 20060515 */
	abstract public Object clone();

	public int domSize(){
		return dom;
	}

	private Vector<int[]> domainAsIntVec(){
		Vector<int[]> result = new Vector<int[]>();
		for (int i=0;i<domSize();i++){
			int nxt[] ={i};
			result.add(nxt);
		}
		return result;
	}
	
	public String nameAt (int i){
		return (String)elementnames.get(i);
	}

	public String namesAt (int[] args){
		String result = "(";

		for (int i=0;i<args.length-1;i++)
			result = result + nameAt(args[i]) + ",";
		if (args.length>0)
			result = result + nameAt(args[args.length-1]);
		result = result +")";
		return result;
	}

	public String[] namesAtAsArray (int[] args){
		String[] result = new String[args.length];
		for (int i=0;i<args.length;i++)
			result[i]=nameAt(args[i]);
		return result;
	}

	private boolean nameExists(String name){
		for (int i=0;i<elementnames.size();i++){
			if (name.equals(nameAt(i))) return true;
		}
		return false;
	}

	public int setName(String name, int i){
		// returns 0 if name already used.
		// does not distinguish case where name is already
		// used at position i, and elementnames.setElementAt(name, i) would
		// only be redundant, but would not introduce
		// any duplicate names.
		if (nameExists(name)) return 0;
		else{
			elementnames.setElementAt(name, i);
			return 1;
		}
	}



	public void createCoords(){
		int counter = 15;
		while(coordinates.size() < elementnames.size()){
			int[] coords = {50+counter, 50};
			coordinates.add(coords);
			counter = counter + 15;
		}
	}

	public void addNode()
	{
		dom++;
		elementnames.add(Integer.toString(dom));
		coordinates.add(new int[2]);
	}

	public void addNode(String st)
	{
		dom++;
		elementnames.add(st);
		coordinates.add(new int[2]);
	}

	public void addNode(String st, int xc, int yc)
	{
		dom++;
		elementnames.add(st);
		int coords[] = {xc,yc};
		coordinates.add(coords);
	}

	public void addNode(int xc, int yc){
		elementnames.add(Integer.toString(dom));
		dom = dom + 1;
		int[] coords = {xc, yc};
		coordinates.add(coords);
	}



//	public int addTuple(Rel r, int[] tuple)
//	{
//		return mydata.add(r,tuple,true,"false");
//
//	}
	public int addTuple(BoolRel r, int[] tuple)
	{
		return mydata.add(r,tuple,true,"false");
	}
	public int addTuple(BoolRel r, int[] tuple, boolean tv)
	{
		return mydata.add(r,tuple,tv,"false");
	}

	public int addTuple(BoolRel r, String[] tuple, boolean tv)
	{
		return mydata.add(r,this.getIndexes(tuple),tv,"false");
	}
	public int addTuple(NumRel r, int[] tuple)
	{
		return mydata.add(r,tuple, (Double)1.0);

	}
	public int addTuple(CatRel r, int[] tuple, int v)
	{
		return mydata.add(r,tuple, v, "?");
	}
	
	/*Takes a vector of numerical relation atoms represented as strings, 
	* and a vector of values of corresponding length
	*/
	public void addTuples(Vector<String> numatomstrings,Vector<Double> values){
		String atstring;
		NumRel rel;
		int[] args;
		for (int i=0;i<numatomstrings.size();i++){
			addTuple(numatomstrings.elementAt(i),values.elementAt(i));
		}
	}
	
	public void addTuple(String numat, double val) {
		int leftpar = numat.indexOf("(");
    	String relname = numat.substring(0, leftpar);
    	int [] args =  this.getIndexes(StringOps.stringToStringArray(numat.substring(leftpar)));
    	int arity = args.length;
    	NumRel rel = new NumRel(relname,arity);
    	addTuple(rel,args,val);
	}
	
	public void addTuple(Rel r, String[] tuple){
		if (r instanceof BoolRel)
			this.addTuple((BoolRel)r, this.getIndexes(tuple));
		if (r instanceof NumRel)
			this.addTuple((NumRel)r, this.getIndexes(tuple));
//		if (r instanceof CatRel)
//			this.addTuple((CatRel)r, this.getIndexes(tuple));
	}

	public int addTuple(NumRel r, int[] tuple, double value)
	{
		return mydata.add(r,tuple, value);

	}

	public int addTuple(NumRel r, String[] tuple, double value)
	{
		return mydata.add(r, this.getIndexes(tuple), value);

	}

	
	public void deleteTuple(Rel r, int[] tuple)
	{
		mydata.delete(r, tuple);
	}
	
	public void deleteTuple(BoolRel r, int[] tuple)
	{
		mydata.delete(r, tuple, true);
	}
	
	public void deleteTuple(NumRel r, int[] tuple)
	{
		mydata.delete(r, tuple);
	}
	
	public void deleteTuple(CatRel r, int[] tuple)
	{
		mydata.delete(r, tuple);
	}
//	public void deleteTuple(NumRel r, int[] tuple, double value)
//	{
//		mydata.delete(r, tuple, value);
//
//	}

	public abstract void addRelation(BoolRel r)
	throws RBNCompatibilityException;
	/* Adds r to the RelStruc by initializing a new RelInt for r */

	public abstract void addRelation(NumRel r)
	throws RBNCompatibilityException;
	
	public abstract void addRelation(CatRel r)
			throws RBNCompatibilityException;
	
	public void deleteRelation(Rel r)
	{
		mydata.delete(r);
	}
	public void deleteRelation(BoolRel r)
	{
		mydata.delete(r);
	}
	public void deleteRelation(NumRel r)
	{
		mydata.delete(r);
	}

	public Vector<int[]> getCoords(){
		return coordinates;
	}

	public Vector<String> getNames(){
		return elementnames;
	}

	//	 public Vector getRels(){
	//		 return relinterpretations;
	//	 }

	public int getSize(){
		return dom;
	}

	//returns the colours of the attributes of the specified node
	public Vector<Color> getAttributesColors(int n){
		BoolRel brel;
		NumRel nrel;
		CatRel crel;
		int[] node = {n};
		Vector<Color> colors = new Vector<Color>();
		//Distinguish between types

		Vector<BoolRel> boolattributes = getBoolAttributes();
		Vector<NumRel> numattributes = getNumAttributes();
		Vector<CatRel> catattributes = getCatAttributes();

		for(int i=0; i<boolattributes.size(); ++i){
			brel = boolattributes.elementAt(i);
			if (mydata.truthValueOf(brel,node) == 1){
				colors.addElement(brel.color);
			}
		}
		for(int i=0; i<numattributes.size(); ++i){
			nrel = numattributes.elementAt(i);
			if (mydata.truthValueOf(nrel,node) == 1){
				colors.addElement(nrel.color);
			}
		}
		for(int i=0; i<catattributes.size(); ++i){
			crel = catattributes.elementAt(i);
			if (mydata.valueOf(crel,node) != -1){
				colors.addElement(crel.color);
			}
		}
		if(colors.size() == 0)
			colors.addElement(Color.white);
		return colors;
	}

	/* Returns for all attributes of node an intensity/saturation 
	 * value that corresponds for numeric attributes to the normalized
	 * value of this nodes attribute value. For boolean attributes =1.
	 * The order of the values must correspond to the order in which
	 * the attribute base colors are returned by getAttributeColors. 
	 */
	public Vector<Integer> getAttributesIntensity(int n){
		BoolRel brel;
		NumRel nrel;
		CatRel crel;
		
		int[] node = {n};
		Vector<Integer> intens = new Vector<Integer>();
		//Distinguish between boolean and numeric 

		Vector<BoolRel> boolattributes = getBoolAttributes();
		Vector<NumRel> numattributes = getNumAttributes();
		Vector<CatRel> catattributes = getCatAttributes();

		for(int i=0; i<boolattributes.size(); ++i){
			brel = boolattributes.elementAt(i);
			if (mydata.truthValueOf(brel,node) == 1){
				intens.addElement(255);
			}
		}
		for(int i=0; i<numattributes.size(); ++i){
			nrel = numattributes.elementAt(i);
			double[] lowerupper = mydata.findInNumRel(nrel).minMax();
			if (mydata.truthValueOf(nrel,node) == 1){
				//intens.addElement(rbnutilities.minMaxNormalize(mydata.valueOf(nrel, node),lowerupper,255,1.5));
				intens.addElement(rbnutilities.sigmoidNormalize(mydata.valueOf(nrel, node),lowerupper,255,1.5));
			}
		}
		for(int i=0; i<catattributes.size(); ++i){
			crel = catattributes.elementAt(i);
			int v = mydata.valueOf(crel, node);
			if (v != -1){
				//intens.addElement(rbnutilities.minMaxNormalize(mydata.valueOf(nrel, node),lowerupper,255,1.5));
				intens.addElement((int)(30+(v/(crel.numvals()-1))*225));
			}
		}
		if(intens.size() == 0)
			intens.addElement(255);
		return intens;
	}


	/** Returns the binary relation tuples from this node to some other node and
	 * the colors of the relations 
	 */
	public Vector[] getBinaryColors(int node){

		OneBoolRelData obrd;
		OneNumRelData onrd;

		Vector<int[]> nodes = new Vector<int[]>();
		Vector<Color> colors = new Vector<Color>();

		for (int i=0;i<mydata.boolsize();i++){
			obrd = mydata.booldataAt(i);

			if ( obrd.rel().arity == 2){
				Vector<?> temp = obrd.getBinDirs(node);
				for (int j=0; j<temp.size(); ++j){
					nodes.addElement((int[])temp.elementAt(j));
					colors.addElement(obrd.rel().color);
				}
			}
		}
		for (int i=0;i<mydata.numsize();i++){
			onrd = mydata.numdataAt(i);

			if ( onrd.rel().arity == 2){
				Vector<?> temp = onrd.getBinDirs(node);
				for (int j=0; j<temp.size(); ++j){
					nodes.addElement((int[])temp.elementAt(j));
					colors.addElement(onrd.rel().color);
				}
			}
		}


		Vector[] tuplesAndColors = {nodes, colors};
		return tuplesAndColors;
	}


	//returns all the relations with arity 1

	public Vector<BoolRel> getBoolAttributes(){
		return mydata.getBoolAttributes();
	}
	public Vector<NumRel> getNumAttributes(){
		return mydata.getNumAttributes();
	}
	public Vector<CatRel> getCatAttributes(){
		return mydata.getCatAttributes();
	}
	//returns all the relations with arity 2
	public Vector<BoolRel> getBoolBinaryRelations(){
		return mydata.getBoolBinaryRelations();
	}
	public Vector<NumRel> getNumBinaryRelations(){
		return mydata.getNumBinaryRelations();
	}
	public Vector<CatRel> getCatBinaryRelations(){
		return mydata.getCatBinaryRelations();
	}
	//returns all the relations with arity >= 3



	public Vector<BoolRel> getBoolArbitraryRelations(){
		return mydata.getBoolArbitraryRelations();
	}
	public Vector<NumRel> getNumArbitraryRelations(){
		return mydata.getNumArbitraryRelations();
	}
	public Vector<CatRel> getCatArbitraryRelations(){
		return mydata.getCatArbitraryRelations();
	}
	/** Returns true if r is one of the OrdRels **/
	public static boolean isOrdRel(Rel r){
		boolean result = false;
		for (int i=0;i<OrdRels.length;i++)
			if (r.equals(OrdRels[i])) 
				result = true;
		return result;
	}




	public int trueOrdAtom(Rel ordrel, String[] args){
		// check whether at is ground:
		boolean isground = true;
		int firstarg;
		int secondarg;	
			
			for (int i=0;i<args.length;i++)
				if (!rbnutilities.IsInteger(args[i])) isground = false;
			if (!isground)
				throw new IllegalArgumentException("Attempt to evaluate non-ground atom " + ordrel.name() + rbnutilities.arrayToString(args));
			if (ordrel.equals(OrdRels[0])){
				firstarg = Integer.parseInt(args[0]);
				secondarg = Integer.parseInt(args[1]);
				if (firstarg < secondarg) return 1;
				else return 0;
			}
			if (ordrel.equals(OrdRels[1])){
				firstarg = Integer.parseInt(args[0]);
				secondarg = Integer.parseInt(args[1]);
				if (firstarg +1 == secondarg) return 1;
				else return 0;
			}
			if (ordrel.equals(OrdRels[2])){
				firstarg = Integer.parseInt(args[0]);
				if (firstarg  == 0) return 1;
				else return 0;
			}
			if (ordrel.equals(OrdRels[3])){
				firstarg = Integer.parseInt(args[0]);
				if (firstarg  == dom-1) return 1;
				else return 0;
			}
			
		
		throw new RuntimeException("Program should never reach this line!");
	}



	
	/** Returns all elements in the domain of type rtype.
	 * Throws RBNIllegalArgumentException if RelStruc does
	 * not contain a unary relation corresponding to the
	 * relation defining rtype.
	 * 
	 * @param rtype
	 * @return
	 * @throws RBNIllegalArgumentException
	 */
	public int[] allElements(Type rtype)
	throws RBNIllegalArgumentException
	{
		int[] result = null;
		if (rtype instanceof TypeInteger)
			throw new RBNIllegalArgumentException("Cannot handle Integer Type yet");

		if (rtype instanceof TypeDomain){
			result = new int[dom];
			for (int i=0;i<dom;i++)
				result[i]=i;
		}

		if (rtype instanceof TypeRel){
			Rel r;
			int reltype = mydata.findType(((TypeRel)rtype).getRel());
			if(reltype == Rel.BOOLEAN){
				r = new BoolRel(((TypeRel)rtype).getRel());
			}
			else{
				r = new NumRel(((TypeRel)rtype).getRel());
			}
			
			
			Vector<int[]> alltrue = mydata.allTrue(r);
			result = new int[alltrue.size()];
			for (int i=0;i<result.length;i++)
				result[i]=alltrue.elementAt(i)[0];
		}
		return result;
	}


	
	/* The following is only a default implementation of
	 * allTrue that does not make use of the specific representation
	 * of relations in RelStruc
	 */
	public int[][] allTrue(ProbFormBool cc,String[] vars)// the elements of vars must be distinct!
	throws IllegalArgumentException,RBNCompatibilityException
	{
		int k = vars.length;
		int m = rbnutilities.IntPow(dom,k);
		Vector<int[]> prelimarray = new Vector<int[]>();

		for (int i=0; i<m ; i++)
		{
			int[] thistuple = rbnutilities.indexToTuple(i,k,dom);

			if (cc.evaluatesTo(this) == 1)
				prelimarray.add(thistuple);
		}

		int[][] result = new int[prelimarray.size()][k];
		for (int i =0; i<result.length; i++) result[i]=prelimarray.elementAt(i);
		return result;
	}
	


	public Vector<int[]> allTrue(Rel r){
		return mydata.allTrue(r);
	}

	public Vector<int[]> allTrue(Type t){
		if (t.getName().equals("Domain"))
			return domainAsIntVec();
		else
			return mydata.allTrue(sig.getRelByName(t.getName()));
	}

	public Vector<String[]> allTrue(Rel r,RelStruc A){
		return mydata.allTrue(r,A);
	}

	public Vector<String[]> allTrue(String rname,RelStruc A){
		return mydata.allTrue(rname,A);
	}

	
	
	/** Returns an array of integer tuples of length
	 * types.length. The array contains all tuples of 
	 * domainelements such that the i'th component in
	 * the tuple is an element in the relation types[i];
	 * 
	 * Throws an error if types contains a type not 
	 * corresponding to a unary relation in this RelStruc
	 * 
	 * @param types
	 * @return
	 */
	public int[][] allTypedTuples(Type[] types)
	throws RBNIllegalArgumentException
	{
		Vector<int[]> domains = new Vector<int[]>();
		for (int i=0;i<types.length;i++){
			domains.add(allElements(types[i]));
		}
		return rbnutilities.cartesProd(domains);

	}

	public int[][] allArgTuples(Rel r)
	throws RBNIllegalArgumentException
	{
		return allTypedTuples(r.getTypes());
	}




	/** Saves the RelStruc into a file using the BLP or MLN syntax 
	 * for logical specifications
	 *  ONLY BOOLEAN RELATIONS ARE SAVED
	 *   */
	public void saveToAtomFile(String filename, int format){
		try{	  
			BufferedWriter thiswriter = FileIO.openOutputFile(filename);
			if (format == BLP_FORMAT)
				thiswriter.write("Prolog {" + '\n');

			Rel nextrel;
			String rname;
			Vector<int[]> truetuples;


			for (int i=0;i<mydata.boolsize();i++){
				nextrel = mydata.boolRelAt(i);
				rname = nextrel.name.name;
				truetuples = mydata.allTrue(nextrel);
				for (int j=0;j<truetuples.size();j++){
					thiswriter.write(rname + 
							"(" +  rbnutilities.arrayToString(truetuples.elementAt(j),"o") 
							+ ")");
					if (format == BLP_FORMAT)
						thiswriter.write(".");
					thiswriter.write('\n');
				}
			}

			if (format == BLP_FORMAT)
				thiswriter.write("}");
			thiswriter.flush();
			thiswriter.close();
		}
		catch (IOException e){System.out.println(e);}

	}


	public void saveToRDEF(File f){
		RelData thisasdata = new RelData();
		thisasdata.add(new RelDataForOneInput(this));
		thisasdata.saveToRDEF(f);
	}


//	/** Saves the RelStruc to a collection of text file in 
//	 * Proximity format
//	 * 
//	 * ONLY BOOLEAN RELATIONS ARE SAVED
//	 * 
//	 * path is a directory path
//	 */
//
//	public void saveToProximityText(String path)
//	throws RBNioException
//	{
//		try{
//			String domainfile = path + "objects.data";
//			String linksfile = path + "links.data";
//			String attributefile = path + "attributes.data";
//			String linksvaluefile = path + "L_attr_linktype.data";
//			Rel rel;
//			int ar;
//			BufferedWriter domainwriter = FileIO.openOutputFile(domainfile);
//			for (int i=0;i<dom;i++)
//				domainwriter.write(Integer.toString(i) +'\n');
//			domainwriter.close();
//
//			BufferedWriter linkswriter = FileIO.openOutputFile(linksfile);
//			BufferedWriter attributewriter = FileIO.openOutputFile(attributefile);
//			BufferedWriter linksvaluewriter = FileIO.openOutputFile(linksvaluefile);
//
//			attributewriter.write("linktype" + '\t' + "L" + '\t' + "str" 
//					+ '\t' + "L_attr_linktype.data" + '\n');
//			int linkindex = 0;
//
//			for (int i=0;i<mydata.boolsize();i++){
//				rel = mydata.boolRelAt(i);
//				ar = rel.getArity();
//				String relname = rel.name.name;
//				if (ar>2 || ar==0)
//					throw new RBNioException("Cannot save relation with arity" 
//							+ rel.getArity() + " to Proximity text format");
//				if (ar == 1){					 
//					String thisattrfile = "O_attr_" + relname + ".data";
//					BufferedWriter thisattrwriter = FileIO.openOutputFile(path + thisattrfile);
//					attributewriter.write(relname + '\t' + "O" + '\t' 
//							+ "int" + '\t' + thisattrfile+ '\n');
//					Vector<int[]> trueobjs = this.allTrue(rel);
//					for (int h=0;h<trueobjs.size();h++){
//						thisattrwriter.write(Integer.toString(trueobjs.elementAt(h)[0]) + '\t' +  "1"+ '\n');
//					}
//					/* The objects for which this attribute is false cannot be 
//					 * recovered with this.allFalse(rel), because this would require
//					 * that objects are explicitly instantiated to false in this.mydata,
//					 * but this is not the case. Instead retrieve objects with 
//					 * thisattr=false via allTrue for CConstraints
//					 */
//					String vars[] = {"x"};
//					CConstrAtom  cat = new CConstrAtom(rel,vars); 
//					CConstrNeg cneg = new CConstrNeg(cat);
//					int[][] falseobjs = this.allTrue(cneg,vars);
//					for (int h=0;h<falseobjs.length;h++){
//						thisattrwriter.write(Integer.toString(falseobjs[h][0]) + '\t' +  "0"+ '\n');
//					}
//					thisattrwriter.close();
//				}
//				else{ // ar=2
//					Vector<int[]> truelinks = this.allTrue(rel);
//					for (int h=0;h<truelinks.size();h++){
//						int[] nextlink = truelinks.elementAt(h);
//						linkswriter.write(Integer.toString(linkindex) + '\t' 
//								+ Integer.toString(nextlink[0]) + '\t' 
//								+ Integer.toString(nextlink[1]) + '\n');
//						linksvaluewriter.write(linkindex + '\t' + "\"" + relname + "\"" + '\n');
//						linkindex++;
//					}
//				}
//			}
//			linkswriter.close();
//			attributewriter.close();
//			linksvaluewriter.close();
//		}
//		catch (IOException e){System.out.println(e);}
//		catch (RBNCompatibilityException e){System.out.println(e);}
//	}


	/** Saves the RelStruc to a new MySQL database 
	 * 
	 * 
	 * ONLY BOOLEAN RELATIONS ARE SAVED
	 * 
	 * **/
	public void saveToMysql(String dbname){
		//java.sql.Statement stm;
		java.sql.PreparedStatement pst;
		String commandstring;
		Rel rel;
		Vector<int[]> truetuples;
		int[] nexttup; 

		try{
			Class.forName("com.mysql.jdbc.Driver");
			Properties props = new Properties();
			props.setProperty("user","mysql");
			//	            Enumeration menum = DriverManager.getDrivers();
			//	            while (menum.hasMoreElements())
			//	                System.out.println(menum.nextElement().getClass().getName());

			java.sql.Connection myconnection = DriverManager.getConnection("jdbc:mysql://localhost" ,"root","mysqlroot");
			//	            if (myconnection != null)
			//	                myconnection.setCatalog(dbname);
			//	            else System.out.println("null connection");

			pst = myconnection.prepareStatement("CREATE DATABASE " + dbname);
			pst.execute();

			pst  = myconnection.prepareStatement("USE " + dbname);
			pst.execute();

			for (int i=0;i<mydata.boolsize();i++){
				/* Create table for relations */
				rel = mydata.boolRelAt(i);

				commandstring = "CREATE TABLE " + rel.name.name + "(";
				for (int j=1;j<=rel.arity;j++)
					commandstring = commandstring + "arg" + j  + " INT ," ;
				/* Remove the last comma: */
				commandstring = commandstring.substring(0,commandstring.length()-1);
				commandstring = commandstring + ")";


				pst = myconnection.prepareStatement(commandstring);
				pst.execute();


				//	            	pst = myconnection.prepareStatement("FLUSH TABLES");
				//	            	pst.execute();

				/* Now fill the table with the tuples: */
				truetuples = mydata.allTrue(rel);
				for (int j=0;j<truetuples.size();j++){
					nexttup = truetuples.elementAt(j);
					commandstring = "INSERT INTO " + rel.name.name + " VALUES (";
					for (int k=0;k<rel.arity;k++)
						commandstring = commandstring + nexttup[k] + ",";
					/* Remove the last comma: */
					commandstring = commandstring.substring(0,commandstring.length()-1);
					commandstring = commandstring + ")";
					pst = myconnection.prepareStatement(commandstring);
					pst.execute();
				}

			}
			//stm = myconnection.createStatement();


		}
		catch(java.sql.SQLException e){System.out.println(e);}
		catch(java.lang.ClassNotFoundException e){System.out.println(e);}



	}

	/** Saves structure into collection of text files into directory path
	 * 
	 * ONLY BOOLEAN RELATIONS ARE SAVED
	 * 
	 * @param path
	 */
	public void saveToTextFiles(String path){
		Rel rel;
		String filename;
		BufferedWriter writer;
		Vector<int[]> truetuples;
		int[] nexttup; 

		try{

			for (int i=0;i<mydata.boolsize();i++){
				/* Create file for relations */
				rel = mydata.boolRelAt(i);
				filename = path + rel.name.name;
				writer = FileIO.openOutputFile(filename);

				truetuples = mydata.allTrue(rel);
				for (int j=0;j<truetuples.size();j++){
					nexttup = truetuples.elementAt(j);
					for (int h=0;h<nexttup.length-1;h++)
						writer.write(Integer.toString(nexttup[h])+'\t');
					writer.write(Integer.toString(nexttup[nexttup.length-1])+'\t');
					writer.write('\n');
				}
				writer.close();
			}
		}
		catch(IOException e){System.out.println(e);}
	}



	public OneStrucData getData(){
		return mydata;
	}

	protected void setData(OneStrucData dat){
		mydata = dat;
	}

	public void addDomainDec(Element el){
		Element domel = el.addElement("Domain");
		boolean havenames = (elementnames.size() == dom);
		boolean havecoords = (coordinates.size() == dom);
		for (int i=0;i<dom;i++){
			Element nextdel = domel.addElement("obj");
			nextdel.addAttribute("ind",Integer.toString(i));
			if (havenames)
				nextdel.addAttribute("name",nameAt(i));
			if (havecoords)
				nextdel.addAttribute("coords",rbnutilities.arrayToString(coordinates.elementAt(i)));
		}

	}


	
	public int[] getIndexes(String[] tuple){
		int[] args = new int[tuple.length];
		for(int i=0;i<tuple.length; i++){
			if (elementnames.indexOf(tuple[i])==-1)
				this.addNode( tuple[i]);
			args[i]= elementnames.indexOf(tuple[i]);
		}
		return args;
	}

//	public Vector<Rel> getRels(){
//		return mydata.getRels();
//	}
	
	public Vector<BoolRel> getBoolRels(){
		return mydata.getBoolRels();
	}
	public Vector<NumRel> getNumRels(){
		return mydata.getNumRels();
	}
	public Vector<CatRel> getCatRels(){
		return mydata.getCatRels();
	}
	
	public NumRel getNumRel(String name){
		return mydata.findInNumRel(name).rel();
	}
	public BoolRel getBoolRel(String name){
		return mydata.findInBoolRel(name).rel();
	}
	public CatRel getCatRel(String name){
		return mydata.findInCatRel(name).rel();
	}
	
	
	public int truthValueOf(Rel r, int[] args){
		return mydata.truthValueOf(r,args);
	}
	public double valueOf(Rel r, int[] args){
		return mydata.valueOf(r,args);
	}
	public double getNumAtomValue(String nratom){
		String relname = rbnutilities.getRelnameFromAtom(nratom);
		String args  = rbnutilities.getArgsFromAtom(nratom);
		String[] stringargs = StringOps.stringToStringArray(args);
		int[] intargs = this.getIndexes(stringargs);
		NumRel nr = this.getNumRel(relname);
		return this.valueOf(nr, intargs);
	}
	
	public OneStrucData getmydata(){
		return mydata;
	}

	public String printSummary(){
		return mydata.printSummary();
	}

	public void setRandom(String[][] rellist,double scale){
		mydata.setRandom(rellist,scale);
	}
	
	public void setSiog(Signature s){
		sig =s;
	}
	
	public Signature signature(){
		return sig;
	}
	

}