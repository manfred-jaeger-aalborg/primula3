/*
* RelDataForOneInput.java 
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

package RBNLearning;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.io.File;

import RBNpackage.*;
import myio.*;

import org.dom4j.*;

public class RelDataForOneInput {
	RelStruc A;
	Vector<OneStrucData> D;
	
	public RelDataForOneInput(){
		A=null;
		D=new Vector<OneStrucData>();
	}
	public RelDataForOneInput(RelStruc Aarg){
		A=Aarg;
		D=new Vector<OneStrucData>();
	}
	
	public RelStruc inputDomain(){
		return A;
	}
	
	public Vector<OneStrucData> observedAtoms(){
		return D;
	}
	
	public OneStrucData oneStrucDataAt(int i){
		if (D.size()>i)
			return D.elementAt(i);
		else return null;
	}
	
	public Vector<OneStrucData> allOneStrucData(){
		return D;
	}
	
	public int numObservations(){
		return D.size();
	}
	
//	public Vector<Rel> getProbRels(){
//		return D.elementAt(0).getRels();
//	}
	
	public void addCase(OneStrucData osd){
		D.add(osd);
	}


	public void addToElement(Element el){
		A.addDomainDec(el); // here the function addDomainDec has been commented
		Element prelel = el.addElement("PredefinedRels");
		A.getData().addAtomsToElement(prelel,A);
		for (int i=0;i<D.size();i++){
			Element probrelel = el.addElement("ProbabilisticRelsCase");
			D.elementAt(i).addAtomsToElement(probrelel,A);
		}
	}
	
	public void setA(RelStruc Aarg){
		A=Aarg;
	}
	
	public boolean hasProbData(){
		return (D.size()>0);
	}
	
	 public RelDataForOneInput subSampleData(int pc){
		 RelDataForOneInput result = new RelDataForOneInput(this.A);
		 
		 for (Iterator<OneStrucData> it = D.iterator(); it.hasNext();)
			 result.addCase(it.next().negativeSampleData(pc));
		 return result;
	 }

	 public RelDataForOneInput[] randomSplit(int numfolds){
		 RelDataForOneInput[] result = new RelDataForOneInput[numfolds];
		 for (int i=0;i<numfolds;i++)
			 result[i] = new RelDataForOneInput(this.A);
		 
		 /* First determine whether the split can be performed by just 
		  * dividing the observed cases in D (if cases are not of approximately
		  * equal size, then division is uneven)
		  */
		 
		 if (D.size() >= numfolds){
			 for (int i=0;i<D.size();i++){
				 result[i % numfolds].addCase(D.elementAt(i));
			 }
		 }
		 else{
			 int remainingfolds = numfolds;
			 int nextsplitsize;
			 OneStrucData[] osdsplits;
			 
			 for (int i=D.size();i>0;i--){
				 nextsplitsize = Math.round(remainingfolds/i);
				 osdsplits = D.elementAt(i-1).randomSplit(nextsplitsize);
				 
				 for (int j=0;j<nextsplitsize;j++)
					 result[remainingfolds-1-j].addCase(osdsplits[j]);
				 remainingfolds=remainingfolds-nextsplitsize;
			 }
			 
		 }
		 return result;
	 }
	 
//   Needs update to work again:	 
//	 public void saveToFOIL(File filename){
//		 try{	  
//			 FileWriter thiswriter = new FileWriter(filename);
//
//			 Rel nextrel;
//			 Type[] reltypes;
//			 String rname;
//			 Vector<int[]> truetuples;
//			 Vector<int[]> falsetuples;
//
//			 Vector elnames = A.getNames();
//			 thiswriter.write("DOMAIN: ");
//			 for (int i=0;i<elnames.size()-1;i++)
//				 thiswriter.write(elnames.elementAt(i)+",");
//			 thiswriter.write(elnames.elementAt(elnames.size()-1)+".");
//			 thiswriter.write('\n');
//			 thiswriter.write('\n');
//			 
//			 /* All unary predefined relations of A are interpreted as types: */
//			 Vector<Rel> predefrels = A.getRels();
//			 for (int i=0;i<predefrels.size();i++){
//				 nextrel = predefrels.elementAt(i);
//				 if (nextrel.getArity()==1){
//					 thiswriter.write(nextrel.name.name + ": ");
//					 Vector<int[]> allts = A.allTrue(nextrel);
//					 for (int j=0;j<allts.size()-1;j++)
//						 thiswriter.write(A.nameAt(allts.elementAt(j)[0]) + ",");
//					 thiswriter.write(A.nameAt(allts.elementAt(allts.size()-1)[0]) + "." + '\n' + '\n' );
//				 }
//			 }
//
//			 /* All non-unary predefined relations: */
//
//			 for (int i=0;i<predefrels.size();i++){
//				 nextrel = predefrels.elementAt(i);
//				 reltypes = nextrel.getTypes();
//				 if (nextrel.getArity()!=1){
//					 rname = nextrel.name.name;
//					 thiswriter.write("*" + rname + "(");
//					 for (int j=0;j<reltypes.length-1;j++)
//						 thiswriter.write(reltypes[j].getName() + ",");
//					 thiswriter.write(reltypes[reltypes.length-1].getName() );
//					 thiswriter.write(")" + '\n');
//					 truetuples = A.allTrue(nextrel);
//					 for (int j=0;j<truetuples.size();j++){
//						 thiswriter.write(  StringOps.arrayToString(A.namesAtAsArray(truetuples.elementAt(j)),"","") );
//						 thiswriter.write('\n');
//					 }
//					 thiswriter.write("." + '\n' + '\n');
//				 } 
//			 }
//			 
//			 /* The probabilistic relations */
//			 
//			 OneStrucData osd = D.elementAt(0);
//			 thiswriter.write('\n');
//			 
//			 for (int i=0;i<osd.boolsize();i++){
//				 nextrel = osd.boolRelAt(i);
//				 reltypes = nextrel.getTypes();
//				 rname = nextrel.name.name;
//				 thiswriter.write("*" + rname + "(");
//				 for (int j=0;j<reltypes.length-1;j++)
//					 thiswriter.write(reltypes[j].getName() + ",");
//				 thiswriter.write(reltypes[reltypes.length-1].getName() );
//				 thiswriter.write(")" + '\n');
//				 truetuples = osd.allTrue(nextrel);
//				 for (int j=0;j<truetuples.size();j++){
//					 thiswriter.write(  StringOps.arrayToString(A.namesAtAsArray(truetuples.elementAt(j)),"","") );
//					 thiswriter.write('\n');
//				 }
//				 thiswriter.write(";" + '\n');
//				 falsetuples = osd.allFalse((BoolRel)nextrel); 
//				 for (int j=0;j<falsetuples.size();j++){
//					 thiswriter.write(  StringOps.arrayToString(A.namesAtAsArray(falsetuples.elementAt(j)),"","") );
//					 thiswriter.write('\n');
//				 }
//				 thiswriter.write("." + '\n');
//			 }
//
//	
//			 thiswriter.flush();
//			 thiswriter.close();
//		 }
//		 catch (IOException e){System.out.println(e);}
//
//	 }


	 public int numberOfObservations(){
		 return D.size();
	 }
	 
}
