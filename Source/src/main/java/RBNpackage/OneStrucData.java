/* OneStrucData.java 
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


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import org.dom4j.Element;
import RBNExceptions.*;
import RBNLearning.RelData;
import RBNLearning.RelDataForOneInput;
import RBNio.FileIO;
import RBNutilities.rbnutilities;
import myio.StringOps;

/** An object of the class OneStrucData represents one (partial) observation of 
 * a given set of relations for one given input domain. The set of relations
 * can either be the set of predefined relations in an input domain (then 
 * OneStrucData is the main part of the specification of the input domain), or
 * the set of probabilistic relations (then OneStrucData is the main part of 
 * the specification of a data case, or of evidence).
 * 
 * @author jaeger
 *
 */

public class OneStrucData {

	/** Is set to the RelStruc of which this OneStrucData is 
	 * the mydata element, if such a RelStruc exists
	 */
	RelStruc parentrelstruc = null;
	
	/**
	 * @uml.property  name="allonebooldata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.OneBoolRelData"
	 */
	Vector<OneBoolRelData>allonebooldata;
	/**
	 * @uml.property  name="allonenumdata"
	 * @uml.associationEnd  multiplicity="(0 -1)" ordering="true" elementType="RBNpackage.OneNumRelData" qualifier="arrayToString:java.lang.String java.lang.Double"
	 */
	Vector<OneNumRelData>allonenumdata;
	
	Vector<OneCatRelData>allonecatdata;

	Vector<Vector<? extends OneRelData>> allreltypes;
	
	/**
	 * The weight can be used to represent a weight of this data in a data sample. 
	 * Used when this data does not represent a single observation, but multiple identical
	 * observations. The weights of multiple OneStrucData elements in the parentrelstruc should
	 * then sum to 1.
	 * (This seems so far unused and untested ...)
	 */
	double weight =1;
	
	public OneStrucData(){
		allonebooldata=new Vector<OneBoolRelData>();
		allonenumdata = new Vector<OneNumRelData>();
		allonecatdata = new Vector<OneCatRelData>();
		
		allreltypes = new Vector<Vector<? extends OneRelData>>();
		allreltypes.add(this.allonebooldata);
		allreltypes.add(this.allonenumdata);
		allreltypes.add(this.allonecatdata);
	}

	public OneStrucData(OneStrucData toCopy){
		allonebooldata=new Vector<OneBoolRelData>();
		allonenumdata = new Vector<OneNumRelData>();
		allonecatdata = new Vector<OneCatRelData>();
		if( toCopy != null ){
			for (int i=0;i<toCopy.allonebooldata.size();i++)
				allonebooldata.add(toCopy.allonebooldata.elementAt(i).copy());
			for (int i=0;i<toCopy.allonenumdata.size();i++)
				allonenumdata.add(toCopy.allonenumdata.elementAt(i).copy());
			for (int i=0;i<toCopy.allonecatdata.size();i++)
				allonecatdata.add(toCopy.allonecatdata.elementAt(i).copy());
		}
		allreltypes = new Vector<Vector<? extends OneRelData>>();
		allreltypes.add(this.allonebooldata);
		allreltypes.add(this.allonenumdata);
		allreltypes.add(this.allonecatdata);
		if (toCopy.getParentRelStruc() != null)
			parentrelstruc = toCopy.getParentRelStruc();
	}

	public OneStrucData(Vector<OneBoolRelData>allbooldat,
			Vector<OneNumRelData> allnumdata,
			Vector<OneCatRelData> allcatdata ){
		allonebooldata=allbooldat;
		allonenumdata =allnumdata;
		allonecatdata =allcatdata;
		allreltypes = new Vector<Vector<? extends OneRelData>>();
		allreltypes.add(this.allonebooldata);
		allreltypes.add(this.allonenumdata);
		allreltypes.add(this.allonecatdata);
	}

	public void add(OneBoolRelData ord){
		if (findInBoolRel(ord.rel())==null)
		allonebooldata.add(ord);
		else
			System.out.println("Attempt to add data for " + ord.rel().name() + 
					" to OneStrucData already containing data for this relation");
	}
	
	public void add(OneNumRelData ord){
		allonenumdata.add(ord);
	}

	public void add(OneCatRelData ord){
		if (findInCatRel(ord.rel())==null)
		allonecatdata.add(ord);
		else
			System.out.println("Attempt to add data for " + ord.rel().name() + 
					" to OneStrucData already containing data for this relation");
	}
	
	public void add(OneStrucData osd){
		Vector<OneBoolRelData>allboolrels = osd.getAllonebooldata();
		Vector<OneNumRelData>allnumrels = osd.getAllonenumdata();
		Vector<OneCatRelData>allcatrels = osd.getAllonecatdata();
		
		for (int i=0;i<allnumrels.size();i++){
			OneNumRelData addonrd = allnumrels.elementAt(i);
			OneNumRelData oldonrd = findInNumRel(addonrd.rel());
			if (oldonrd != null)
				oldonrd.add(addonrd);
			else
				allonenumdata.add(allnumrels.elementAt(i));
		}
		for (int i=0;i<allboolrels.size();i++){
			OneBoolRelData addobrd = allboolrels.elementAt(i);
			OneBoolRelData oldobrd = findInBoolRel(addobrd.rel());
			if (oldobrd != null)
				oldobrd.add(addobrd);
			else
				allonebooldata.add(allboolrels.elementAt(i));
		}
		for (int i=0;i<allcatrels.size();i++){
			OneCatRelData addocrd = allcatrels.elementAt(i);
			OneCatRelData oldocrd = findInCatRel(addocrd.rel());
			if (oldocrd != null)
				oldocrd.add(addocrd);
			else
				allonecatdata.add(allcatrels.elementAt(i));
		}
	}

	public void setParentRelStruc(RelStruc prs){
		parentrelstruc = prs;
	}
	
	public RelStruc getParentRelStruc(){
		return parentrelstruc;
	}
	
	public void setData(BoolRel r, int[] args, boolean tv)
	throws RBNIllegalArgumentException
	{
		OneBoolRelData ord = findInBoolRel(r);
		if (ord == null)
			throw new RBNIllegalArgumentException("Cannot find boolean relation " + r.printname());
		else 
			ord.add(args,tv);
	}

	
	//numeric version
	public void setData(NumRel r, int[] args, Double v)
	throws RBNIllegalArgumentException
	{
		OneNumRelData ord = findInNumRel(r);
		if (ord == null)
			throw new RBNIllegalArgumentException("Cannot find numeric relation " + r.printname());
		ord.add(args,v);
	}

	//categorical version
	public void setData(CatRel r, int[] args, Integer v)
			throws RBNIllegalArgumentException
	{
		OneCatRelData ord = findInCatRel(r);
		if (ord == null)
			throw new RBNIllegalArgumentException("Cannot find categorical relation " + r.printname());
		ord.add(args,v);
	}
	
	public OneRelData find(Rel r){
		OneNumRelData nr = findInNumRel(r);
		if ( nr != null)
			return nr;
		OneBoolRelData br = findInBoolRel(r);
		if (br != null)
			return br;
		OneCatRelData cr = findInCatRel(r);
		if (cr != null)
			return cr;
		return null;
	}

	public OneRelData find(String rname){
		OneNumRelData nr = findInNumRel(rname);
		if ( nr != null)
			return nr;
		OneBoolRelData br = findInBoolRel(rname);
		if (br != null)
			return br;
		OneCatRelData cr = findInCatRel(rname);
		if (cr != null)
			return cr;
		return null;
	}
 
	public OneNumRelData findInNumRel(Rel r){
		for (int i=0;i<allonenumdata.size();i++){
			if (allonenumdata.elementAt(i).rel().equals(r))
				return allonenumdata.elementAt(i);
		}
		return null;
	}
	
	public OneBoolRelData findInBoolRel(Rel r){
		for (int i=0;i<allonebooldata.size();i++){
			if (allonebooldata.elementAt(i).rel().equals(r))
				return allonebooldata.elementAt(i);
		}
		return null;
	}

	public OneCatRelData findInCatRel(Rel r){
		for (int i=0;i<allonecatdata.size();i++){
			if (allonecatdata.elementAt(i).rel().equals(r))
				return allonecatdata.elementAt(i);
		}
		return null;
	}
	/* finding relations based only on name --
	 * unsafe if there are two relations with the same name 
	 * but different arities
	 */
	
	public OneNumRelData findInNumRel(String rname){
		for (int i=0;i<allonenumdata.size();i++){
			if (allonenumdata.elementAt(i).rel().name().equals(rname))
				return allonenumdata.elementAt(i);
		}
		return null;
	}
	
	public OneBoolRelData findInBoolRel(String rname){
		for (int i=0;i<allonebooldata.size();i++){
			if (allonebooldata.elementAt(i).rel().name().equals(rname))
				return allonebooldata.elementAt(i);
		}
		return null;
	}
	
	public OneCatRelData findInCatRel(String rname){
		for (int i=0;i<allonecatdata.size();i++){
			if (allonecatdata.elementAt(i).rel().name().equals(rname))
				return allonecatdata.elementAt(i);
		}
		return null;
	}
	
	public void add(Rel r, int[] args, int v) {
		if ((r instanceof BoolRel)) {
			if (v==0) {
				add((BoolRel)r,args,false,"?");
			}
			else {
				add((BoolRel)r,args,true,"?");
			}
		}
		if ((r instanceof CatRel)) {
			add((CatRel)r,args,v,"?");
		}
	}
	
	public void add(GroundAtom at, int tv, String dv){
		Rel r = at.rel;
		if (r instanceof BoolRel) {
			switch(tv){
			case 0:
				add((BoolRel)r,at.args,false,dv);
				break;
			case 1:
				add((BoolRel)r,at.args,true,dv);
				break;
			default:
				System.out.println("Cannot add truthvalue " + tv + " to instantiation");
			}
		}
		else {  // CatRel
			add((CatRel)r,at.args,tv,dv);
		}
	}




	public void add(GroundAtom at, boolean tv,String dv){
		add((BoolRel)at.rel,at.args,tv,dv);
	}

	/** Returns 1 if r,tuple,tv was not already in the data;
	 * 0 otherwise.
	 * 
	 * The dv argument only plays a role when the relation r so far
	 * is not present, and a new OneRelData element has to be created.
	 * @param r
	 * @param tuple
	 * @param tv
	 * @param dv
	 * @return
	 */
	public int add(BoolRel r, int[] tuple, boolean tv, String dv)
	{
		int temp;
		OneBoolRelData thisrelinst = findInBoolRel(r);
		if (thisrelinst == null){
			thisrelinst = new OneBoolRelData(r,dv);
			allonebooldata.add(thisrelinst);
		}
		if (r.arity == 0)
			return thisrelinst.setGlobal(tv);
		else{
			temp = thisrelinst.add(tuple,tv);
			if (temp >= 0)
				return 1;
			else return 0;
		}
	}
	

	public int add(NumRel r, int[] tuple, Double v)
	{
		int temp;
		OneNumRelData thisrelinst = findInNumRel(r);
		if (thisrelinst == null){
			thisrelinst = new OneNumRelData(r,v);
			allonenumdata.add(thisrelinst);
		}
		if (r.arity == 0)
			return thisrelinst.setGlobal(v);
		else{
			temp = thisrelinst.add(tuple,v);
			if (temp >= 0)
				return 1;
			else return 0;
		}
	}

	public int add(CatRel r, int[] tuple, int v,String dv)
	{
		int temp;
		OneCatRelData thisrelinst = findInCatRel(r);
		if (thisrelinst == null){
			thisrelinst = new OneCatRelData(r,dv);
			allonecatdata.add(thisrelinst);
		}
		if (r.arity == 0)
			return thisrelinst.setGlobal(v);
		else{
			temp = thisrelinst.add(tuple,v);
			if (temp >= 0)
				return 1;
			else return 0;
		}
	}
	
	public void add(CatRel r, int[][] tuples, int v, String dv)
	{
		OneCatRelData thisrelinst = findInCatRel(r);
		if (thisrelinst != null){
			thisrelinst.add(tuples,v);
		}
		else{
			thisrelinst = new OneCatRelData(r,dv);
			allonecatdata.add(thisrelinst);
			thisrelinst.add(tuples,v);
		}
	}
	
	
	public void add(BoolRel r, int[][] tuples, boolean tv,String dv)
	{
		OneBoolRelData thisrelinst = findInBoolRel(r);
		if (thisrelinst != null){
			thisrelinst.delete(tuples,!tv);
			thisrelinst.add(tuples,tv);
		}
		else{
			thisrelinst = new OneBoolRelData(r,dv);
			allonebooldata.add(thisrelinst);
			thisrelinst.add(tuples,tv);
		}
	}
	
	public void add(Rel r, int[][] tuples, int v ,String dv)
	{
		if (r instanceof BoolRel) {
			switch(v) {
			case 0: 
				add((BoolRel)r,tuples,false,dv);
				break;
			case 1: 
				add((BoolRel)r,tuples,true,dv);
				break;
			}
		}
		if (r instanceof CatRel)
			add((CatRel)r, tuples, v ,dv);
	}

	/* Initializes an empty OneBoolRelData for the
	 * relation r, if there is none already
	 */
	public void add(BoolRel r, String dv){
		if (findInBoolRel(r)==null){
			allonebooldata.add(new OneBoolRelData(r,dv));
		}
	}
	
	public void add(CatRel r, String dv){
		if (findInCatRel(r)==null){
			allonecatdata.add(new OneCatRelData(r,dv));
		}
	}
	
	public Vector<int[]> allTrue(Rel r){
		if (RelStruc.isOrdRel(r))
			return allTrueOrdRel(r, new String[r.getArity()]); // this case should never occur
		else {
			OneRelData ori = find(r);
			if (ori != null)
				return (Vector<int[]>) rbnutilities.treeSetToVector(find(r).allTrue());
			else return new Vector<int[]>();
		}
	}

	public Vector<int[]> allTrue(Rel r,String[] args){
		if (RelStruc.isOrdRel(r))
			return allTrueOrdRel(r, new String[r.getArity()]); // this case should never occur
		else {
			OneRelData ori = find(r);
			if (ori != null)
				return (Vector<int[]>) rbnutilities.treeSetToVector(ori.allTrue(args));
			else return new Vector<int[]>();
		}
	}

	/* Returns all tuples that are true for the order relation orel. 
	 * Only returns tuples that match template, whose length
	 * equals orel.arity, and which can contain integer and string
	 * entries. Example: if orel = less, and template = [x,5], then 
	 * method returns [0,5],[1,5],[2,5],[3,5],[4,5].
	 * 
	 */
	public Vector<int[]> allTrueOrdRel(Rel orel, String[] template){

			Vector<int[]> alltrue = new Vector<int[]>();
			int firstarg = 0;
			int secondarg = 0;		     
			boolean firstargint = rbnutilities.IsInteger(template[0]);
			boolean secondargint = false;
			if (firstargint) firstarg = Integer.parseInt(template[0]);
			if (orel.getArity()>1){
				secondargint = rbnutilities.IsInteger(template[1]);		    
				if (secondargint) secondarg = Integer.parseInt(template[1]);
			}
			if (orel.equals(RelStruc.OrdRels[0])){ // 'less' OrdRel
				if (firstargint && secondargint){
					if (firstarg < secondarg){
						int[] toadd = new int[2];
						toadd[0]=firstarg;
						toadd[1]=secondarg;
						alltrue.add(toadd);
					}
				}
				if (firstargint && !secondargint){
					for (int i=0;i<parentrelstruc.domSize()-firstarg-1;i++){
						int[] toadd=new int[2];
						toadd[0]=firstarg;
						toadd[1]=firstarg+i+1;
						alltrue.add(toadd);
					}
				}
				if (!firstargint && secondargint){
					for (int i=0;i<secondarg;i++){
						int[] toadd=new int[2];
						toadd[0]=i;
						toadd[1]=secondarg;
						alltrue.add(toadd);
					}
				}
				if (!firstargint && !secondargint){
					for (int i=0;i<parentrelstruc.domSize()-1;i++)
						for (int j=i+1;j<parentrelstruc.domSize();j++){
							int[] toadd=new int[2];
							toadd[0]=i;
							toadd[1]=j;
							alltrue.add(toadd);
						}
				}			
			} // end 'less' OrdRel
			if (orel.equals(RelStruc.OrdRels[1])){ // 'pred' OrdRel
				if (firstargint && secondargint){
					if (firstarg == secondarg-1){
						int[] toadd = new int[2];
						toadd[0]=firstarg;
						toadd[1]=secondarg;
						alltrue.add(toadd);
					}
				}
				if (firstargint && !secondargint){
					if (firstarg != parentrelstruc.domSize()-1){
						int[] toadd = new int[2];
						toadd[0]=firstarg;
						toadd[1]=firstarg+1;
						alltrue.add(toadd);
					}	
				}
				if (!firstargint && secondargint){
					if (secondarg != 0){
						int[] toadd = new int[2];
						toadd[0]=secondarg-1;
						toadd[1]=secondarg;
						alltrue.add(toadd);
					}	
				}
				if (!firstargint && !secondargint){
					for (int i=0;i<parentrelstruc.domSize()-1;i++){
						int[] toadd = new int[2];
						toadd[0]=i;
						toadd[1]=i+1;
						alltrue.add(toadd);
					}
				}			
			} // end 'pred' OrdRel
			if (orel.equals(RelStruc.OrdRels[2])){ // 'zero' OrdRel
				if ((firstargint && firstarg==0) || !firstargint ){
					int[] toadd = new int[1];
					toadd[0]=0;
					alltrue.add(toadd);

				}
			} // end 'zero' OrdRel
			if (orel.equals(RelStruc.OrdRels[3])){ // 'last' OrdRel
				if ( (firstargint && firstarg==parentrelstruc.domSize()-1) || !firstargint){

					int[] toadd = new int[1];
					toadd[0]=parentrelstruc.domSize()-1;
					alltrue.add(toadd);
				}
			}
			return alltrue;
	} 


	/** Applies the closed-world assumption: sets all tuples not
	 * currently 'true' to false
	 * @param r
	 * @param tv
	 */
	public void applyCWA(BoolRel r){
		OneBoolRelData rdata = findInBoolRel(r);
		if (rdata != null)
			rdata.setDV("false");
		else
			add(r,"false");
	}

	/** Returns all tuples that are instantiated to false in relation r 
	 * Tuples represented as integer arrays, using the internal indices of 
	 * objects
	 * 
	 * d denotes a domainsize for which all false atoms should 
	 * be returned in case r in this OneStrucData has defaultvalue "false"
	 */
	public Vector<int[]> allFalse(BoolRel r){
		OneBoolRelData ori = findInBoolRel(r);
		if (ori != null)
			return (Vector<int[]>) rbnutilities.treeSetToVector(ori.allFalse(this.parentrelstruc));
		else
			return new Vector<int[]>();

	}

	public Vector<int[]> allInstantiated(Rel r){
		Vector<int[]> result = new Vector<int[]>();

		if (r instanceof BoolRel) {
			result = allTrue(r);
			result.addAll(allFalse((BoolRel)r));
		}
		if (r instanceof CatRel) {
			OneCatRelData ocrd = this.findInCatRel(r);
			if (ocrd!=null)
				result = rbnutilities.treeSetToVector(ocrd.allTrue());
		}
		return result;
	}

	/** Returns all tuples that are instantiated to true in relation r 
	 * Tuples represented as string arrays, using the names of objects
	 * as defined in A
	 */
	public Vector<String[]> allTrue(Object r, RelStruc A){
		OneRelData ori = null;
		if ( r instanceof Rel)
			ori = find((Rel)r);
		if ( r instanceof String)
			ori = find((String)r);
		if (ori != null)
			return ori.allTrue(A);
		else
			return new Vector<String[]>();
	}
	


	public boolean isEmpty(){
		boolean result = true;
		if (allonebooldata != null && !allonebooldata.isEmpty())
			result = false;
		if (allonenumdata != null && !allonenumdata.isEmpty())
			result = false;
		if (allonecatdata != null && !allonecatdata.isEmpty())
			result = false;
		return result;
	}


	public void delete(GroundAtom at){
		delete(at.rel,at.args);
	}


	public void delete(Rel r, int[] tuple)
	{
		if ( r instanceof BoolRel){
			delete((BoolRel)r,tuple,true);
			delete((BoolRel)r,tuple,false);
		}
		if ( r instanceof CatRel){
			delete((CatRel)r,tuple);
		}
		if ( r instanceof NumRel){
			System.out.println("Removing numeric tuple not implemented in OneStrucData.delete");
		}
	}

	public void delete(CatRel r, int[] tuple)
	{
		OneCatRelData thisrelinst = findInCatRel(r);
		if (thisrelinst != null)
			thisrelinst.delete(tuple);
	}
	
	public void delete(BoolRel r, int[] tuple, boolean tv){
		OneBoolRelData thisrelinst = findInBoolRel(r);

		if (thisrelinst != null){
			thisrelinst.delete(tuple,tv);
			//if (thisrelinst.isEmpty()) allonebooldata.remove(thisrelinst);
		}
	}
	
	public void delete(NumRel r, int[] tuple){
		OneNumRelData thisrelinst = findInNumRel(r);

		if (thisrelinst != null){
			thisrelinst.delete(tuple);
			//if (thisrelinst.isEmpty()) allonenumdata.remove(thisrelinst);
		}
	}



	/** Delete all atoms containing a and subtract 1 from
	 * all elements with index > a
	 * @param a
	 */
	public void deleteShift(int a){
		Vector<Integer> emptyrels = new Vector<Integer>();
		
		for (Vector<? extends OneRelData> ord: allreltypes) {
			for (int i=0;i<ord.size();i++){
				OneRelData thisrelinst = ord.elementAt(i);

				thisrelinst.delete(a);
				thisrelinst.shiftArgs(a);

				if (thisrelinst.isEmpty()){ 
					emptyrels.add(i);
				}
			}
			for(int i=0;i < emptyrels.size();i++){
				ord.remove(emptyrels.elementAt(i));
			}
			emptyrels.clear();
		}
	}


	/** delete all instantiations of the relation relname */
	public void delete(Rel r){
		OneRelData thisrelinst = find(r);
		if (thisrelinst != null){
			if (thisrelinst instanceof OneBoolRelData)
				allonebooldata.remove(thisrelinst);
			if (thisrelinst instanceof OneNumRelData)
				allonenumdata.remove(thisrelinst);
			if (thisrelinst instanceof OneCatRelData)
				allonecatdata.remove(thisrelinst);
		}
		else
			System.out.println("relation not found");
	}
	
	public void delete(BoolRel r){
		OneBoolRelData thisrelinst = findInBoolRel(r);
		if (thisrelinst != null)
			allonebooldata.remove(find(r));
		else
			System.out.println("relation not found");
	}
	
	public void delete(NumRel r){
		OneNumRelData thisrelinst = findInNumRel(r);
		if (thisrelinst != null)
			allonenumdata.remove(find(r));
		else
			System.out.println("relation not found");
	}

	public void delete(CatRel r){
		OneCatRelData thisrelinst = findInCatRel(r);
		if (thisrelinst != null)
			allonecatdata.remove(find(r));
		else
			System.out.println("relation not found");
	}
	
	public void shiftArgs(int a){
		// Replaces all arguments b of trueAtoms and falseAtoms lists
		// by b-1 if b>a (needed after the deletion of node with index a from
		// the underlying SparseRelStruc)
		for (Vector<? extends OneRelData> ord: allreltypes) {
			for (int k=0;k<ord.size();k++){
				OneRelData thisrelinst = ord.elementAt(k);
				thisrelinst.shiftArgs(a);
			}	
		}
	}



	public OneStrucData copy(){
		OneStrucData result = new OneStrucData();
		for (int i=0;i<this.allonebooldata.size();i++)
			result.allonebooldata.add(this.allonebooldata.elementAt(i).copy());

		for (int i=0;i<this.allonenumdata.size();i++)
			result.allonenumdata.add(this.allonenumdata.elementAt(i).copy());
		
		for (int i=0;i<this.allonecatdata.size();i++)
			result.allonecatdata.add(this.allonecatdata.elementAt(i).copy());

		result.allreltypes = new Vector<Vector<? extends OneRelData>>();
		result.allreltypes.add(result.allonebooldata);
		result.allreltypes.add(result.allonenumdata);
		result.allreltypes.add(result.allonecatdata);
		return result;
	}

	public String printAsString(RelStruc A,String pref){
		String result = "";
		for (Vector<? extends OneRelData> ord: allreltypes) {
			for (int i=0;i<ord.size();i++){
				OneRelData thisrelinst = ord.elementAt(i);
				result = result + thisrelinst.printAsString(A,pref);
			}	
		}
		
		return result;
	}

	public String printSummary(){
		String result ="Summary without categorical variables: \n" ;

		OneBoolRelData thisrelinst = new OneBoolRelData();
		for (int i=0;i<allonebooldata.size();i++){
			thisrelinst = (OneBoolRelData)allonebooldata.elementAt(i);
			result = result + thisrelinst.rel().printname() +" true: " 
			+ thisrelinst.numtrue() + " false: " + thisrelinst.numfalse() + '\n'; 
		}
		OneNumRelData numthisrelinst = new OneNumRelData();

		for (int i=0;i<allonenumdata.size();i++){
			numthisrelinst = (OneNumRelData)allonenumdata.elementAt(i);
			result = result + numthisrelinst.rel().printname() +" true: " + '\n'; 
		}
		return result;
	}

	/** Returns 1,0, or -1 according to whether at is true, false, or 
	 * undefined according to this instantiation.
	 */
	public int truthValueOf(GroundAtom at){
		return truthValueOf(at.rel,at.args);
	}


	/** Returns 1,0, or -1 according to whether r(tuple) is true, false, or 
	 * undefined according to this instantiation.
	 */
	public int truthValueOf(Object r, int[] tuple)
	{
		if(r instanceof BoolRel){
			return truthValueOf((BoolRel)r,tuple);
		}
		else if(r instanceof NumRel){
			return truthValueOf((NumRel)r,tuple);
		}
		else if(r instanceof CatRel){
			return valueOf((CatRel)r,tuple);
		}
		else{
			return -1;
		}
	}
	
	public int truthValueOf(Rel r, int[] tuple)
	{
		OneRelData thisrelinst = find(r);
		if (thisrelinst != null){
			if (thisrelinst instanceof OneBoolRelData)
				return ((OneBoolRelData)thisrelinst).valueOf(tuple);
			else if (thisrelinst instanceof OneCatRelData)
				return ((OneCatRelData)thisrelinst).valueOf(tuple);
			else 
				return ((OneNumRelData)thisrelinst).truthValueOf(tuple);
		}
		else return -1;
	}
	
	
	public int truthValueOf(BoolRel r, int[] tuple)
	{
		OneBoolRelData thisrelinst = findInBoolRel(r);
		if (thisrelinst != null)
		{
			return thisrelinst.valueOf(tuple);
		}
		else {
			return -1;
		}
	}
	public int truthValueOf(NumRel r, int[] tuple)
	{
		OneNumRelData thisrelinst = findInNumRel(r);
		if (thisrelinst != null)
		{
			return thisrelinst.truthValueOf(tuple);
		}
		else {
			return -1;
		}
	}
	
	public int valueOf(CatRel r, int[] tuple)
	{
		OneCatRelData thisrelinst = findInCatRel(r);
		if (thisrelinst != null)
		{
			return thisrelinst.valueOf(tuple);
		}
		else {
			return -1;
		}
	}
	
	/* Returns the value of atom r(tuple), or NaN if 
	 * no value for tuple is given.
	 */
	public double valueOf(Rel r, int[] tuple)
	{
		OneNumRelData thisnumrelinst = findInNumRel(r);
		if (thisnumrelinst != null)
		{
			Double n = thisnumrelinst.valueOf(tuple);
			if(n != null)
				return n;
			else 
				return Double.NaN;
				
		}
		OneBoolRelData thisboolrelinst = findInBoolRel(r);
		if (thisboolrelinst != null)
		{
			int tv = thisboolrelinst.valueOf(tuple);
			if (tv != -1)
				return (double)tv;
			else 
				return Double.NaN;
		}
		OneCatRelData thiscatrelinst = findInCatRel(r);
		if (thiscatrelinst != null)
		{
			int tv = thiscatrelinst.valueOf(tuple);
			if (tv != -1)
				return (double)tv;
			else 
				return Double.NaN;
		}
		return Double.NaN;
	}


	public Vector<InstAtom> allInstAtoms(){
		Vector<InstAtom>  result = new Vector<InstAtom> ();
		for (int i=0;i<allonebooldata.size();i++){
			OneBoolRelData thisrelinst = (OneBoolRelData)allonebooldata.elementAt(i);
			Vector<int[]> alltrues = (Vector<int[]>) rbnutilities.treeSetToVector(thisrelinst.allTrue());
			Vector<int[]> allfalse = (Vector<int[]>) rbnutilities.treeSetToVector(thisrelinst.allFalse(this.parentrelstruc));
			if (thisrelinst.rel.arity == 0){
				if (alltrues.size() > 0)
					result.add(new InstAtom(thisrelinst.rel,new int[0],1));
				if (allfalse.size() > 0)
					result.add(new InstAtom(thisrelinst.rel,new int[0],0));

			}
			else{
				for (Iterator<int[]> it = alltrues.iterator();it.hasNext();)
					result.add(new InstAtom(thisrelinst.rel,it.next(),1));
				for (Iterator<int[]> it = allfalse.iterator();it.hasNext();)
					result.add(new InstAtom(thisrelinst.rel,it.next(),0));
			}
		}
		for (int i=0;i<allonecatdata.size();i++){
			OneCatRelData thisrelinst = (OneCatRelData)allonecatdata.elementAt(i);
			TreeSet<int[]> alltups = thisrelinst.allTrue();
			for (int[] t: alltups)
				result.add(new InstAtom(thisrelinst.rel,t,thisrelinst.valueOf(t)));
		}
		return result;
	}

	public void saveToBLPDatFile(String filename,int domsize){
		try{
			System.out.println("Warning: saving to BLP file -- Boolean relations only!");
			BufferedWriter logwriter = FileIO.openOutputFile(filename);
			OneBoolRelData ri;
			Rel r;
			String rname;
			Vector<int[]> atomlist;
			int[] nexttup;
			logwriter.write("begin(1)." + '\n');
			for (int i=0;i< allonebooldata.size();i++){
				ri = (OneBoolRelData)allonebooldata.elementAt(i);
				r = ri.rel();
				rname = r.printname();
				atomlist = (Vector<int[]>) rbnutilities.treeSetToVector(ri.allTrue());
				for (int j=0;j< atomlist.size();j++){
					nexttup = (int[])atomlist.elementAt(j);
					logwriter.write(rname + "(" + rbnutilities.arrayToString(nexttup,"o") + ") = true." + '\n');
				}
				atomlist = (Vector<int[]>) rbnutilities.treeSetToVector(ri.allFalse(this.parentrelstruc));
				for (int j=0;j< atomlist.size();j++){
					nexttup = (int[])atomlist.elementAt(j);
					logwriter.write(rname + "(" + rbnutilities.arrayToString(nexttup,"o") + ") = false." + '\n');
				}
				atomlist = ri.allUnInstantiated(domsize);
				for (int j=0;j< atomlist.size();j++){
					nexttup = (int[])atomlist.elementAt(j);
					logwriter.write(rname + "(" + rbnutilities.arrayToString(nexttup,"o") + ") = '$unknown'." + '\n');
				}
			}

			logwriter.write("end(1).");
			logwriter.close();
		}
		catch (IOException e){System.out.println(e);}
	}

	public int numRels(){
		return allonebooldata.size()+allonenumdata.size()+allonecatdata.size();
	}

//	/** returns all the relations with arity 1 */
//	public Vector<Rel> getAttributes(){
//		Vector<Rel> attributes = new Vector<Rel>();
//		Rel rel;
//		for (Vector<? extends OneRelData> ord: allreltypes) {
//			for(int i=0; i<ord.size(); ++i){
//				rel = ord.elementAt(i).rel();
//				if (rel.arity == 1)  //is an attribute
//					attributes.addElement(rel);
//			}
//		}
//		return attributes;
//	}
		
	public Vector<BoolRel> getBoolAttributes(){
		BoolRel rel;
		Vector<BoolRel> attributes = new Vector<BoolRel>();
		for(int i=0; i<allonebooldata.size(); ++i){
			rel = allonebooldata.elementAt(i).rel();
			if (rel.arity == 1)  //is an attribute
				attributes.addElement(rel);
		}
		return attributes;
	}
	
	public Vector<NumRel> getNumAttributes(){
		NumRel rel;
		Vector<NumRel> attributes = new Vector<NumRel>();
		for(int i=0; i<allonenumdata.size(); ++i){
			rel = allonenumdata.elementAt(i).rel();
			if (rel.arity == 1)  //is an attribute
				attributes.addElement(rel);
		}
		return attributes;
	}

	public Vector<CatRel> getCatAttributes(){
		CatRel rel;
		Vector<CatRel> attributes = new Vector<CatRel>();
		for(int i=0; i<allonecatdata.size(); ++i){
			rel = allonecatdata.elementAt(i).rel();
			if (rel.arity == 1)  //is an attribute
				attributes.addElement(rel);
		}
		return attributes;
	}
	
	
//	/**  returns all the relations with arity 2 */
//	public Vector<Rel> getBinaryRelations(){
//		Vector<Rel> binaries = new Vector<Rel>();
//		Rel rel;
//		for (Vector<? extends OneRelData> ord: allreltypes) {
//			for(int i=0; i<ord.size(); ++i){
//				rel = ord.elementAt(i).rel();
//				if (rel.arity == 2)  //is a binary relation
//					binaries.addElement(rel);
//			}
//		}
//		return binaries;
//	}

	public Vector<BoolRel> getBoolBinaryRelations(){
		BoolRel rel;
		Vector<BoolRel> binaries = new Vector<BoolRel>();
		for(int i=0; i<allonebooldata.size(); ++i){
			rel = allonebooldata.elementAt(i).rel();
			if (rel.arity == 2)  
				binaries.addElement(rel);
		}
		return binaries;
	}
	
	public Vector<NumRel> getNumBinaryRelations(){
		NumRel rel;
		Vector<NumRel> binaries = new Vector<NumRel>();
		for(int i=0; i<allonenumdata.size(); ++i){
			rel = allonenumdata.elementAt(i).rel();
			if (rel.arity == 2)  
				binaries.addElement(rel);
		}
		return binaries;
	}

	public Vector<CatRel> getCatBinaryRelations(){
		CatRel rel;
		Vector<CatRel> binaries = new Vector<CatRel>();
		for(int i=0; i<allonecatdata.size(); ++i){
			rel = allonecatdata.elementAt(i).rel();
			if (rel.arity == 2)  
				binaries.addElement(rel);
		}
		return binaries;
	}
	
	public Vector<NumRel> getNumGlobals(){
		NumRel rel;
		Vector<NumRel> binaries = new Vector<NumRel>();
		for(int i=0; i<allonenumdata.size(); ++i){
			rel = allonenumdata.elementAt(i).rel();
			if (rel.arity == 0)  
				binaries.addElement(rel);
		}
		return binaries;
	}
	
	/**  returns all the relations with arity >=3 */

//	public Vector<Rel> getArbitraryRelations(){
//		Vector<Rel> arbrels = new Vector<Rel>();
//		Rel rel;
//		for(int i=0; i<allonebooldata.size(); ++i){
//			rel = allonebooldata.elementAt(i).rel();
//			if (rel.arity == 2)  //is a binary rel
//				arbrels.addElement(rel);
//		}
//		for(int i=0; i<allonenumdata.size(); ++i){
//			rel = allonenumdata.elementAt(i).rel();
//			if (rel.arity == 2)  //is a binary rel
//				arbrels.addElement(rel);
//		}
//		return arbrels;
//		
//	}
	
	public Vector<BoolRel> getBoolArbitraryRelations(){
		BoolRel rel;
		Vector<BoolRel> attributes = new Vector<BoolRel>();
		for(int i=0; i<allonebooldata.size(); ++i){
			rel = allonebooldata.elementAt(i).rel();
			if (rel.arity >= 3)  
				attributes.addElement(rel);
		}
		return attributes;
	}
	
	public Vector<NumRel> getNumArbitraryRelations(){
		NumRel rel;
		Vector<NumRel> attributes = new Vector<NumRel>();
		for(int i=0; i<allonenumdata.size(); ++i){
			rel = allonenumdata.elementAt(i).rel();
			if (rel.arity >= 3)  
				attributes.addElement(rel);
		}
		return attributes;
	}

	public Vector<CatRel> getCatArbitraryRelations(){
		CatRel rel;
		Vector<CatRel> attributes = new Vector<CatRel>();
		for(int i=0; i<allonecatdata.size(); ++i){
			rel = allonecatdata.elementAt(i).rel();
			if (rel.arity >= 3)  
				attributes.addElement(rel);
		}
		return attributes;
	}




//	// TODO this needs to be redone!
//	public  Vector<Double> numattributesValues(int node){
//		Vector<Double> result = new Vector<Double>() ;
//
//		NumRel rel;
//		Double v;
//		Vector<int[]> temptuples;
//		for(int i=0; i<allonenumdata.size(); ++i){
//			rel = allonenumdata.elementAt(i).rel();
//			if(rel.arity == 1){
//				temptuples = rbnutilities.treeSetToVector(allonenumdata.elementAt(i).allTrue());
//				for(int j=0;j<temptuples.size();j++){
//					if(rbnutilities.inArray(temptuples.elementAt(j), node)){
//						int[] tuple = temptuples.elementAt(j);
//						//System.out.println(rbnutilities.arrayToString(tuple));
//						v = allonenumdata.elementAt(i).valueOf(tuple);
//						result.add(v);
//						//System.out.println(v);
//
//					}
//				}
//			}
//		}
//
//		return result;
//
//	}
	
	// TODO this needs to be tested (handling/role of NaN values?)!
	public  Vector<Double> numattributesValues(int node){
		Vector<Double> result = new Vector<Double>() ;
        Vector<NumRel> numatts = this.getNumAttributes();
        int[] arg = {node};
        for (NumRel r: numatts) {
        	OneNumRelData ond = this.findInNumRel(r);
        	Double val = ond.valueOf(arg);
        	if (val!=Double.NaN)
        		result.add(val);
        }
		return result;
	}
	
	public  Vector<String> catattributesStringValues(int node){
		Vector<String> result = new Vector<String>() ;
        Vector<CatRel> numatts = this.getCatAttributes();
        int[] arg = {node};
        for (CatRel r: numatts) {
        	OneCatRelData ond = this.findInCatRel(r);
        	int val = ond.valueOf(arg);
        	if (val!=Double.NaN)
        		result.add(r.get_String_val(val));
        }
		return result;
	}
//	//TODO needs to be redone!
//	public  Vector<Double> getNumBinValues(int node){
//		Vector<Double> result = new Vector<Double>() ;
//
//		NumRel rel;
//		Double v;
//		Vector<int[]> temptuples;
//		for(int i=0; i<allonenumdata.size(); ++i){
//			rel = allonenumdata.elementAt(i).rel();
//			if (rel.arity == 2){
//
//				temptuples = rbnutilities.treeSetToVector(allonenumdata.elementAt(i).allTrue());
//				for(int j=0;j<temptuples.size();j++){
//					if(rbnutilities.inArray(temptuples.elementAt(j), node)){
//						int[] tuple = temptuples.elementAt(j);
//						//System.out.println(rbnutilities.arrayToString(tuple));
//						v = allonenumdata.elementAt(i).valueOf(tuple);
//						result.add(v);
//						//System.out.println(v);
//
//					}
//				}
//
//			}
//		}
//
//		return result;
//
//	}
	
//	// TODO this needs to be tested (handling of NaN values?)!
//	public  Vector<Double> getNumBinValues(int node){
//		Vector<Double> result = new Vector<Double>() ;
//        Vector<NumRel> numbins = this.getNumBinaryRelations();
//        int[] arg = {node};
//        for (NumRel r: numatts) {
//        	OneNumRelData ond = this.findInNumRel(r);
//        	Double val = ond.valueOf(arg);
//        	if (val!=Double.NaN)
//        		result.add(val);
//        }
//		return result;
//	}
//	


	public int size(){
		return allonebooldata.size() + allonenumdata.size()+ allonecatdata.size();
	}
	
	public int boolsize(){
		return allonebooldata.size();
	}
	
	public int numsize(){
		return allonenumdata.size();
	}
	
	public int catsize(){
		return allonecatdata.size();
	}
	


	public BoolRel boolRelAt(int i){
		return allonebooldata.elementAt(i).rel();
	}
	public NumRel numRelAt(int i){
		return allonenumdata.elementAt(i).rel();
	}
	public CatRel catRelAt(int i){
		return allonecatdata.elementAt(i).rel();
	}



	
	public void addRelation(BoolRel r, String dv){
		if (find(r) == null)
			allonebooldata.add(new OneBoolRelData(r,dv));
	}
	public void addRelation(NumRel r, Double v){
		if (find(r) == null)
			allonenumdata.add(new OneNumRelData(r,v));
	}
	public void addRelation(CatRel r, String dv){
		if (find(r) == null)
			allonecatdata.add(new OneCatRelData(r,dv));
	}

	public OneBoolRelData booldataAt(int i){
		return allonebooldata.elementAt(i);
	}
	public OneNumRelData numdataAt(int i){
		return allonenumdata.elementAt(i);
	}
	public OneCatRelData catdataAt(int i){
		return allonecatdata.elementAt(i);
	}


	public void addAtomsToElement(Element el, RelStruc struc){
		for (Vector<? extends OneRelData> ordtype: allreltypes) {
			for (OneRelData ord: ordtype) {
				ord.addRelData(el, struc);
			}
		}
	}

	
//	public Vector<Rel> getRels(){
//		Vector<Rel> result = new Vector<Rel>();
//		Rel rel;
//		for(int i=0; i<allonebooldata.size(); ++i){
//			rel = allonebooldata.elementAt(i).rel();
//			if (rel.arity == 2)  //is a binary rel
//				result.addElement(rel);
//		}
//		for(int i=0; i<allonenumdata.size(); ++i){
//			rel = allonenumdata.elementAt(i).rel();
//			if (rel.arity == 2)  //is a binary rel
//				result.addElement(rel);
//		}
//		return result;
//	}
	
	public Vector<BoolRel> getBoolRels(){
		Vector<BoolRel> result = new Vector<BoolRel>();
		for (OneBoolRelData obrd: allonebooldata){
			result.add(obrd.rel()); 
		}
		return result;
	}
	public Vector<NumRel> getNumRels(){
		Vector<NumRel> result = new Vector<NumRel>();
		for (OneNumRelData onrd: allonenumdata){
			result.add(onrd.rel()); 
		}
		return result;
	}	
	public Vector<CatRel> getCatRels(){
		Vector<CatRel> result = new Vector<CatRel>();
		for (OneCatRelData ocrd: allonecatdata){
			result.add(ocrd.rel()); 
		}
		return result;
	}

	public Vector<Rel> getBoolAndCatRels(){
		Vector<Rel> result = new Vector<Rel>();
		for (OneBoolRelData obrd: allonebooldata){
			result.add(obrd.rel()); 
		}
		for (OneCatRelData ocrd: allonecatdata){
			result.add(ocrd.rel()); 
		}
		return result;
	}
	
	public String boolDvAt(int i){
		return allonebooldata.elementAt(i).dv();
	}

	public String numDvAt(int i){
		return allonenumdata.elementAt(i).dv();
	}


	public Vector<OneBoolRelData> getAllonebooldata(){
		return allonebooldata;
	}
	public Vector<OneNumRelData> getAllonenumdata(){
		return allonenumdata;
	}
	public Vector<OneCatRelData> getAllonecatdata(){
		return allonecatdata;
	}

	
	public int findType(Rel r){
		Vector<BoolRel>  brel = getBoolRels();
		for (Iterator<BoolRel> e = brel.iterator(); e.hasNext();){
			if (e.next().equals(r))
				return Rel.BOOLEAN;
		}
		Vector<NumRel>  nrel = getNumRels();
		for (Iterator<NumRel> e = nrel.iterator(); e.hasNext();){
			if (e.next().equals(r))
				return Rel.NUMERIC;
		}
		Vector<CatRel>  crel = getCatRels();
		for (Iterator<CatRel> e = crel.iterator(); e.hasNext();){
			if (e.next().equals(r))
				return Rel.CATEGORICAL;
		}
		System.out.println("Could not find type of relation " + r.name());
		return -1;
	}
	
	public TreeSet<String> relTree(){
		TreeSet<String> reltree = new TreeSet<String>();
		for (Vector<? extends OneRelData> ordtype: allreltypes) {
			for (OneRelData ord: ordtype) {
				reltree.add(ord.rel().name.name);
			}
		}
		return reltree;

	}
	public boolean relExist(String name){
		TreeSet<String> reltree = this.relTree();
		return reltree.contains(name);
	}
	
	public void clear(){
		System.out.println("Clearing Inst");
		allonebooldata=new Vector<OneBoolRelData>();
		allonenumdata = new Vector<OneNumRelData>();
		allonecatdata = new Vector<OneCatRelData>();
		allreltypes = new Vector<Vector<? extends OneRelData>>();
		allreltypes.add(this.allonebooldata);
		allreltypes.add(this.allonenumdata);
		allreltypes.add(this.allonecatdata);
	}
	
	/* Initialize  with empty interpretations of the
	 * probabilistic relations defined by rbn
	 */
	public void init(RBN rbn){
		Rel[] rels = rbn.Rels();
		for(int i=0; i<rels.length; ++i){
			if (rels[i] instanceof BoolRel)
				this.add((BoolRel)rels[i],"?");
			if (rels[i] instanceof CatRel)
				this.add((CatRel)rels[i],"?");
		}
	}

	public boolean containsAll( OneStrucData other ){
		if(other.isEmpty() ) return true;
		else if(this.isEmpty() ) return false;

		return new HashSet(  this.allInstAtoms() ).containsAll( new HashSet( other.allInstAtoms() ) );
	}
	
	 public OneStrucData negativeSampleData(int pc){
		 OneStrucData result = new OneStrucData();
		 /* Add all NumRelData and CatRelData without copying or subsampling:*/
		 for (OneNumRelData onrd:  allonenumdata)
			 result.add(onrd);
		 for (OneBoolRelData obrd:  allonebooldata)
			 result.add(obrd.negativeSample(pc,parentrelstruc));
		 for (OneCatRelData ocrd:  allonecatdata)
			 result.add(ocrd);
		 return result;
	 }

	 public OneStrucData[] randomSplit(int numfolds){
		 OneStrucData[] result = new OneStrucData[numfolds];
		 for (int i=0;i<numfolds;i++){
			 result[i]=new OneStrucData();
			 result[i].setParentRelStruc(this.parentrelstruc);
		 }
		 /* Add all NumRelData without copying or subsampling -- this is a placeholder for
		  * eventual use of splitting OneStrucData's that also contain numerical 
		  * relations
		  */
		 for (OneNumRelData onrd:  allonenumdata){
			 OneNumRelData[] splitofond = onrd.randomSplit(numfolds,parentrelstruc);
			 for (int i=0;i<numfolds;i++)
				 result[i].add(splitofond[i]);
		 }
		 for (OneBoolRelData obrd: allonebooldata){
			 OneBoolRelData[] splitofobd = obrd.randomSplit(numfolds,parentrelstruc);
			 for (int i=0;i<numfolds;i++)
				 result[i].add(splitofobd[i]);
		 }
		 for (OneCatRelData ocrd: allonecatdata){
			 OneCatRelData[] splitofobd = ocrd.randomSplit(numfolds,parentrelstruc);
			 for (int i=0;i<numfolds;i++)
				 result[i].add(splitofobd[i]);
		 }
		 return result;
	 }


	 public void saveToRDEF(File savefile,RelStruc rs){
		 RelData thisasdata = new RelData();
		RelDataForOneInput rdfoi = new RelDataForOneInput(rs);
		rdfoi.addCase(this);
		thisasdata.add(rdfoi);
		thisasdata.saveToRDEF(savefile);
	}
	
	public void setRandom(String[][] rellist, double scale){
		String nextstr;
		OneNumRelData onrd;
		for(int i=0;i<rellist.length;i++){
			for (int j=0;j<rellist[i].length;j++){
				nextstr = rellist[i][j];
				/* Distinguish whether nextstr is a ground atom or
				 * a relation name
				 */
				if (nextstr.contains("(")){ // atom case
					String relname = nextstr.substring(0,nextstr.indexOf("("));
					String args = nextstr.substring(nextstr.indexOf("("),nextstr.length());
					/* It is assumed that the arguments are names as specified 
					 * in parentrelstruc. These have to be transformed into integer 
					 * identifiers first.
					 */
					int[] intargs = parentrelstruc.getIndexes(StringOps.stringToStringArray(args));
					onrd = findInNumRel(relname);
					onrd.setRandom(intargs,scale);
				}
				else{ // relation name
					onrd = findInNumRel(nextstr);
					if (onrd != null)
						onrd.setRandom(scale);
				}

			}
		}
	}
	
	public void setWeight(double w) {
		weight=w;
	}
	
	public double getWeight() {
		return weight;
	}
	
	
}

