/* SparseRelStruc.java
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
import RBNExceptions.*;
import RBNutilities.*;
import myio.StringOps;


public class SparseRelStruc extends RelStruc {




	/** Creates new SparseRelStruc */
	public SparseRelStruc(){
		super();
	}

	/** Creates new SparseRelStruc with n elements*/
	public SparseRelStruc(int n){
		super(n);
		/* Set elementnames to their defaults 0,1,... */
		elementnames = new Vector<String>(n);
		for (int i=0;i<n;i++)
			elementnames.insertElementAt(Integer.toString(i),i);
		/* Set coordinates to defaults (5,5),(5,10),(5,15),... */
		coordinates = new Vector<int[]>(n);
		for (int i=0;i<n;i++){
			int c[]={5,5*(i+1)};
			coordinates.insertElementAt(c,i);
		}
	}




	public SparseRelStruc(Vector<String> en, OneStrucData data, Vector<int[]> coords, Signature s) {
		super(en,data,coords,s);
	}

	/** @author keith cascio
    	@since 20060515 */
	public SparseRelStruc( SparseRelStruc toCopy ){
		//this as a constructor
		this( toCopy.elementnames, toCopy.mydata, toCopy.coordinates , toCopy.signature());
	}

	public SparseRelStruc(RelStruc toCast){
		this( toCast.getNames(), toCast.getData(), toCast.getCoords() , toCast.signature());
	}

	/** @author keith cascio
    	@since 20060515 */
	public Object clone(){
		return new SparseRelStruc( this );
	}


//	public void addRelation(Rel r)
//	{
//		mydata.addRelation(r, "false");
//	}

	public void addRelation(BoolRel r)
	{
		mydata.addRelation(r, "false");
	}
	public void addRelation(BoolRel r, String dv)
	{
		mydata.addRelation(r, dv);
	}
	
	
	public void addRelation(NumRel r)
	{
		mydata.addRelation(r, 0.0);
	}

	public void addOneStrucData(OneStrucData osd){
		mydata.add(osd);
	}

	//     public void deleteNode(int node){
	// 	if(node >= 0 && node < elementnames.size()){
	// 	    elementnames.removeElementAt(node);
	// 	    coordinates.removeElementAt(node);
	// 	    SparseRel ri;
	// 	    for (int i=0; i<relinterpretations.size(); ++i){
	// 		ri = (SparseRel)relinterpretations.elementAt(i);
	// 		ri.deleteNode(node);
	// 	    }
	// 	}
	//     }


	/* Overrides the default implementation
	 */
	public int[][] allTrue(ProbFormBool cc,String[] vars)// the elements of vars must be distinct!
	throws IllegalArgumentException,RBNCompatibilityException
	{

		TreeSet<int[]> prelimResult = allTrueAsTreeSet(cc,vars);

		int[][] result = new int[prelimResult.size()][vars.length];
		Iterator<int[]> it = prelimResult.iterator();
		int index = 0;
		int[] nextIntArr;
		while (it.hasNext()){
			nextIntArr=it.next();
			result[index]=nextIntArr;
			index++;
		}
		return result;   
	}


//	public TreeSet<int[]> allTrueAsTreeSet(CConstr cc,String[] vars)
//	throws IllegalArgumentException,RBNCompatibilityException
//	{
//		//System.out.println("allTrueAsTreeSet for " + cc.asString());
//		TreeSet<int[]> result = new TreeSet<int[]>(new IntArrayComparator());
//
//		if (cc instanceof CConstrEmpty){
//			for (int i = 0; i<mymath.MyMathOps.intPow(dom,vars.length); i++)
//				result.add(rbnutilities.indexToTuple(i,vars.length,dom));	
//		}
//		if (cc instanceof CConstrEq)
//		{
//
//			int[][] alltrue = new int[dom][2];
//			for (int i=0;i<dom;i++){
//				alltrue[i][0]=i;
//				alltrue[i][1]=i;
//			}
//
//			for (int i = 0;i<alltrue.length;i++)
//				rbnutilities.allSatisfyingTuples(((CConstrEq)cc).arguments,alltrue[i], vars, result ,  dom);
//
//		}
//
//
//		if (cc instanceof CConstrAtom)
//		{
//			Rel crel = (Rel) ((CConstrAtom)cc).relation;
//			int[][] alltrue = null;
//
//			if (isOrdRel(crel)){
//				int firstarg = 0;
//				int secondarg = 0;		     
//				boolean firstargint = rbnutilities.IsInteger(((CConstrAtom)cc).arguments[0]);
//				boolean secondargint = false;
//				if (firstargint) firstarg = Integer.parseInt(((CConstrAtom)cc).arguments[0]);
//				if (crel.getArity()>1){
//					secondargint = rbnutilities.IsInteger(((CConstrAtom)cc).arguments[1]);		    
//					if (secondargint) secondarg = Integer.parseInt(((CConstrAtom)cc).arguments[1]);
//				}
//				if (crel.equals(OrdRels[0])){ // 'less' OrdRel
//
//					if (firstargint && secondargint){
//						if (firstarg < secondarg){
//							alltrue = new int[1][];
//							alltrue[0] = new int[2];
//							alltrue[0][0]=firstarg;
//							alltrue[0][1]=secondarg;
//						}
//						else alltrue = new int[0][];
//					}
//					if (firstargint && !secondargint){
//						alltrue = new int[dom-firstarg-1][];
//						for (int i=0;i<dom-firstarg-1;i++){
//							alltrue[i]=new int[2];
//							alltrue[i][0]=firstarg;
//							alltrue[i][1]=firstarg+i+1;
//						}
//					}
//					if (!firstargint && secondargint){
//						alltrue = new int[secondarg][];
//						for (int i=0;i<secondarg;i++){
//							alltrue[i]=new int[2];
//							alltrue[i][0]=i;
//							alltrue[i][1]=secondarg;
//						}
//					}
//					if (!firstargint && !secondargint){
//						alltrue = new int[dom*(dom-1)/2][];
//						int k=0;
//						for (int i=0;i<dom-1;i++)
//							for (int j=i+1;j<dom;j++){
//								alltrue[k]= new int[2];
//								alltrue[k][0]=i;
//								alltrue[k][1]=j;
//								k++;
//							}
//					}			
//				} // end 'less' OrdRel
//				if (crel.equals(OrdRels[1])){ // 'pred' OrdRel
//					if (firstargint && secondargint){
//						if (firstarg == secondarg-1){
//							alltrue = new int[1][];
//							alltrue[0] = new int[2];
//							alltrue[0][0]=firstarg;
//							alltrue[0][1]=secondarg;
//						}
//						else alltrue = new int[0][];
//					}
//					if (firstargint && !secondargint){
//						if (firstarg != dom-1){
//							alltrue = new int[1][];
//							alltrue[0] = new int[2];
//							alltrue[0][0]=firstarg;
//							alltrue[0][1]=firstarg+1;
//						}	
//						else alltrue = new int[0][];
//					}
//					if (!firstargint && secondargint){
//						if (secondarg != 0){
//							alltrue = new int[1][];
//							alltrue[0] = new int[2];
//							alltrue[0][0]=secondarg-1;
//							alltrue[0][1]=secondarg;
//						}			    
//						else alltrue = new int[0][];
//					}
//					if (!firstargint && !secondargint){
//						alltrue = new int[dom-1][];
//						int k=0;
//						for (int i=0;i<dom-1;i++){
//							alltrue[i]= new int[2];
//							alltrue[i][0]=i;
//							alltrue[i][1]=i+1;
//							k++;
//						}
//					}			
//				} // end 'pred' OrdRel
//				if (crel.equals(OrdRels[2])){ // 'zero' OrdRel
//					if (firstargint){
//						if (firstarg==0){
//							alltrue = new int[1][];
//							alltrue[0] = new int[1];
//							alltrue[0][0]=0;
//						}
//						else alltrue = new int[0][];
//					}
//					if (!firstargint){
//						alltrue = new int[1][];
//						alltrue[0] = new int[1];
//						alltrue[0][0]=0;
//					}
//				} // end 'zero' OrdRel
//				if (crel.equals(OrdRels[2])){ // 'last' OrdRel
//					if (firstargint){
//						if (firstarg==dom-1){
//							alltrue = new int[1][];
//							alltrue[0] = new int[1];
//							alltrue[0][0]=dom-1;
//						}
//						else alltrue = new int[0][];
//					}
//					if (!firstargint){
//						alltrue = new int[1][];
//						alltrue[0] = new int[1];
//						alltrue[0][0]=dom-1;
//					}
//				} // end 'last' OrdRel
//			} // if (isOrdRel(crel)){ 
//			else { // not an OrdRel				
//				alltrue = StringOps.intarrVectorToArray(mydata.allTrue(crel));
//			}
//			for (int i = 0;i<alltrue.length;i++)
//				rbnutilities.allSatisfyingTuples(((CConstrAtom)cc).arguments, alltrue[i], vars, result , dom);
//
//		};
//
//		if (cc instanceof CConstrAnd)
//		{
//			TreeSet<int[]> treeset1 = allTrueAsTreeSet(((CConstrAnd)cc).C1,vars);
//			if (treeset1.isEmpty())
//				return result; /* result is empty! */
//			TreeSet<int[]> treeset2 = allTrueAsTreeSet(((CConstrAnd)cc).C2,vars);
//			if (treeset2.isEmpty())
//				return result; /* result is empty! */
//
//			Iterator<int[]> it1 = treeset1.iterator();
//			Iterator<int[]> it2 = treeset2.iterator();
//
//			int[] ts1tuple;
//			int[] ts2tuple;
//			int comparets1ts2;
//
//
//			while (it1.hasNext()){
//				ts1tuple = it1.next();
//				if (treeset2.contains(ts1tuple))
//					result.add(ts1tuple);
//			};
//		}
//		if (cc instanceof CConstrOr)
//		{
//			TreeSet<int[]> treeset1 = allTrueAsTreeSet(((CConstrOr)cc).C1,vars);
//			TreeSet<int[]> treeset2 = allTrueAsTreeSet(((CConstrOr)cc).C2,vars);
//
//			result = treeset1;
//			result.addAll(treeset2);
//		};
//		if (cc instanceof CConstrNeg)
//		{
//			TreeSet<int[]> treeset1 = allTrueAsTreeSet(((CConstrNeg)cc).C1,vars);
//			int[] nexttup;
//			for (int i = 0; i<mymath.MyMathOps.intPow(dom,vars.length); i++){
//				nexttup = rbnutilities.indexToTuple(i,vars.length,dom);
//				if (!treeset1.contains(nexttup))
//					result.add(nexttup);
//			}
//
//		}
//		//printTS(result);
//		return result;
//	}

	public TreeSet<int[]> allTrueAsTreeSet(ProbFormBool cc,String[] vars)
			throws IllegalArgumentException,RBNCompatibilityException
			{
		//System.out.println("allTrueAsTreeSet for " + cc.asString());
		TreeSet<int[]> result = new TreeSet<int[]>(new IntArrayComparator());

		if (cc instanceof ProbFormBoolConstant){
			for (int i = 0; i<mymath.MyMathOps.intPow(dom,vars.length); i++)
				result.add(rbnutilities.indexToTuple(i,vars.length,dom));	
		}

		if (cc instanceof ProbFormBoolAtom)
		{
			Vector<int[]> alltrue;
			Rel crel = ((ProbFormBoolAtom) cc).getRelation();
			String[] args = ((ProbFormBoolAtom) cc).getArguments();
			if (RelStruc.isOrdRel(crel)){
				alltrue = mydata.allTrueOrdRel(crel, args);
			}
			else 
				//alltrue = mydata.allTrue(crel);
				alltrue=mydata.allTrue(crel,args);
			for (int i = 0;i<alltrue.size();i++)
				rbnutilities.allSatisfyingTuples(args, alltrue.elementAt(i), vars, result , dom);
		};

		if (cc instanceof ProbFormBoolEquality)
		{
			int[][] alltrue = new int[dom][2];
			for (int i=0;i<dom;i++){
				alltrue[i][0]=i;
				alltrue[i][1]=i;
			}
			for (int i = 0;i<alltrue.length;i++)
				rbnutilities.allSatisfyingTuples(((ProbFormBoolEquality)cc).terms(), alltrue[i], vars, result ,  dom);
		}
		if (cc instanceof ProbFormBoolComposite)
		{
			for (int i=0;i < ((ProbFormBoolComposite)cc).numComponents();i++){
				TreeSet<int[]> nexttreeset = allTrueAsTreeSet(((ProbFormBoolComposite) cc).componentAt(i),vars);
				if (i==0)
					result = nexttreeset;
				else{
					switch (((ProbFormBoolComposite) cc).operator()){
					case ProbFormBool.OPERATOROR:
						result.addAll(nexttreeset);
						break;
					case ProbFormBool.OPERATORAND:
						if (!result.isEmpty()){
//							TreeSet<int[]> intersection = new TreeSet<int[]>(new IntArrayComparator());
//
//							for (Iterator<int[]> it = result.iterator(); it.hasNext();){
//								int[] nexttup = it.next();
//								if( nexttreeset.contains(nexttup))
//									intersection.add(nexttup);
//							}
							result= (TreeSet<int[]>)rbnutilities.treeSetIntersection(result, nexttreeset);
						}
					}
				}
			}
		}
		if (cc.sign() == false)
		{
			TreeSet<int[]> notresult = new TreeSet<int[]>(new IntArrayComparator());
			int[] nexttup;
			for (int i = 0; i<mymath.MyMathOps.intPow(dom,vars.length); i++){
				nexttup = rbnutilities.indexToTuple(i,vars.length,dom);
				if (!result.contains(nexttup))
					notresult.add(nexttup);
			}
			result = notresult;
		}
		return result;
			}

	//	private void printTS(TreeSet ts){
	//		Iterator it = ts.iterator();
	//		while (it.hasNext())
	//			System.out.print(rbnutilities.arrayToString((int[])it.next()) + " " );
	//		System.out.println();
	//	}

	public void deleteNode(int node){
		if(node >= 0 && node < elementnames.size()){
			elementnames.removeElementAt(node);
			coordinates.removeElementAt(node);
			mydata.deleteShift(node);
			dom--;
		}
		else
			throw new IllegalArgumentException("Index out of bounds");
	}




	/** returns this node's attributes and tuples **/
	public Vector[] getAttrBoolRelsAndTuples(int node){
		Vector[] result = {new Vector<BoolRel>(), new Vector<int[]>()};
		Vector<BoolRel> attributes = mydata.getBoolAttributes();
		Vector<int[]> tuples;
		int[] temp;
		for(int i=0; i<attributes.size(); ++i){
			tuples = mydata.allTrue(attributes.elementAt(i));
			for (int j=0;j<tuples.size();j++){
				temp = tuples.elementAt(j);
				if (temp[0]==node){
					result[0].addElement(attributes.elementAt(i));
					result[1].addElement(temp);
				}
			}

		}
		return result;
	}

	public Vector[] getAttrNumRelsAndTuples(int node){
		Vector[] result = {new Vector<NumRel>(), new Vector<int[]>()};
		Vector<NumRel> attributes = mydata.getNumAttributes();
		Vector<int[]> tuples;
		int[] temp;
		for(int i=0; i<attributes.size(); ++i){
			tuples = mydata.allTrue(attributes.elementAt(i));
			for (int j=0;j<tuples.size();j++){
				temp = tuples.elementAt(j);
				if (temp[0]==node){
					result[0].addElement(attributes.elementAt(i));
					result[1].addElement(temp);
				}
			}

		}
		return result;
	}

	/** returns binary and arbitrary relations and corresponding tuples 
	 * which include this node */


	public Vector[] getOtherBoolRelsAndTuples(int node){
		Vector[] result = {new Vector<BoolRel>(), new Vector<Vector<int[]>>()};
		Vector<BoolRel> otherrels = mydata.getBoolBinaryRelations();
		otherrels.addAll(mydata.getBoolArbitraryRelations());
		Vector<int[]> tuples;
		Vector<int[]> tupleswithnode; 
		int[] temp;
		for(int i=0; i<otherrels.size(); ++i){
			tuples = mydata.allTrue(otherrels.elementAt(i));
			tupleswithnode = new Vector<int[]>(); 
			for (int j=0;j<tuples.size();j++){
				temp = tuples.elementAt(j);
				if (rbnutilities.inArray(temp, node)){
					tupleswithnode.add(temp);					
				}

			}
			if(tupleswithnode.size()!=0){
				result[0].addElement(otherrels.elementAt(i));
				result[1].addElement(tupleswithnode);
			}
		}
		return result;
	}

	public Vector[] getOtherNumRelsAndTuples(int node){
		Vector[] result = {new Vector<NumRel>(), new Vector<Vector<int[]>>(),new Vector<Vector<Double>>()};
		Vector<NumRel> otherrels = mydata.getNumBinaryRelations();
		otherrels.addAll(mydata.getNumArbitraryRelations());
		Vector<int[]> tuples;
		Vector<int[]> tupleswithnode; 
		Vector<Double> valueswithnode;
		int[] temp;
		for(int i=0; i<otherrels.size(); ++i){
			tuples = mydata.allTrue(otherrels.elementAt(i));
			tupleswithnode = new Vector<int[]>(); 
			valueswithnode = new Vector<Double>();
			for (int j=0;j<tuples.size();j++){
				temp = tuples.elementAt(j);
				if (rbnutilities.inArray(temp, node)){
					tupleswithnode.add(temp);	
					valueswithnode.add(mydata.valueOf(otherrels.elementAt(i), temp));
				}
				
			}
			if(tupleswithnode.size()!=0){
				result[0].addElement(otherrels.elementAt(i));
				result[1].addElement(tupleswithnode);
				result[2].addElement(valueswithnode);
			}
		}
		return result;
	}

	//getNumValuesAndTuples
	public Vector<Double> getNumBinValues(int node){
		Vector<Double> result = new Vector<Double>() ;
		result = mydata.getNumBinValues(node);
		return result;
	}
//	public Vector<Double> numbinAndArityValues(int node){
//		Vector<Double> result = new Vector<Double>() ;
//		result = mydata.numbinAndArityValues(node);
//		return result;
//	}
	public Vector<Double> numattributesValues(int node){
		Vector<Double> result = new Vector<Double>() ;
		result = mydata.numattributesValues(node);
		return result;
	}


	public void setData(NumRel r, int[] tuple, Double v) throws RBNIllegalArgumentException{
		mydata.setData(r,tuple,v);
	}

	/** Returns a new SparseRelStruc containing all the objects of this 
	 *  SparseRelStruc. If clonerelations=true then also 
	 *  the relations in this structure will be cloned.
	 *  
	 *  Object names, coordinates and relations are
	 * not cloned themselves, so that changes to these in the original 
	 * structure will also affect the clone.
	 */
	public SparseRelStruc cloneDomain(Boolean clonerelations){
		SparseRelStruc result = new SparseRelStruc(this.elementnames,
				new OneStrucData(),
				this.coordinates,
				this.signature());
		if (clonerelations)
			result.setData(mydata);
		return result;
	}
	public Vector<BoolRel> getBoolAttributes(){
		return mydata.getBoolAttributes();
	}
	public Vector<NumRel> getNumAttributes(){
		return mydata.getNumAttributes();
	}
	public Vector<NumRel> getNumGlobals(){
		return mydata.getNumGlobals();
	}
	
	public Vector<BoolRel> getBoolBinaryRelations(){
		return mydata.getBoolBinaryRelations();
	}
	public Vector<NumRel> getNumBinaryRelations(){
		return mydata.getNumBinaryRelations();
	}
	public Vector<BoolRel> getBoolArbitraryRelations(){
		return mydata.getBoolArbitraryRelations();
	}
	public Vector<NumRel> getNumArbitraryRelations(){
		return mydata.getNumArbitraryRelations();
	}

}