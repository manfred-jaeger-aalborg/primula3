/*
 * QueryTableModel.java
 * 
 * Copyright (C) 2005 Aalborg University
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

package RBNgui;

import java.util.*;
import javax.swing.table.*;
import RBNutilities.*;
//import RBNpackage.Atom;
import RBNinference.BayesNetIntHuginNet;
import RBNpackage.Rel;

public class MCMCTableModel extends QueryTableModel{

	/**
	 * Contains the 
	 */
	String[][] p_v_data ;
	/**
	 * @uml.property  name="minprobabilitydata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
//	LinkedList<String> minprobabilitydata;
//	/**
//	 * @uml.property  name="maxprobabilitydata"
//	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
//	 */
//	LinkedList<String> maxprobabilitydata;
//	/**
//	 * @uml.property  name="vardata"
//	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
//	 */
	LinkedList<String>[] vardata;
	/**
	 * keith cascio 20060511 ...
	 * @uml.property  name="acedata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */

	/**
	 * @uml.property  name="column"
	 */
	private int column;
	

	public MCMCTableModel(Rel r){
		super();
		column = 1+2*(int)r.numvals(); // first column for query atoms
		probabilitydata = new Object[(int)r.numvals()];
		vardata = (LinkedList<String>[])new Object[(int)r.numvals()];
		rownum=super.getRowCount();
	}

	public int getColumnCount(){
		return column;
	}

	public Object getValueAt( int row, int col )
	{
		if (row < this.getRowCount()) {
			if (col==0) // query atom
				return queryatomdata.get(     row );
			else {
				if (col%2 == 1) // a probability column
					return probabilitydata[(col+1)/2].get(row);
				else // a variance column
					return vardata[col/2].get(     row );
			}
		}
		else {
			System.err.println( "row " + row + " out of range at column " + col );
			return STR_EMPTY;
		}

	}



//	public void addProb(String prob){
//		probabilitydata.add(myio.StringOps.doubleConverter(prob));
//	}
//
//	public void addProb(LinkedList prob){
//		probabilitydata = prob;
//	}
//	
//	public void addMinProb(String prob){
//		minprobabilitydata.add(myio.StringOps.doubleConverter(prob));
//	}
//	public void addMaxProb(String prob){
//		maxprobabilitydata.add(myio.StringOps.doubleConverter(prob));
//	}
//	public void addVar(String prob){
//		vardata.add(myio.StringOps.doubleConverter(prob));
//	}


	public void reset(){
		super.reset();
		probabilitydata = (LinkedList<String>[])new Object[(column-1)/2];
		vardata = (LinkedList<String>[])new Object[(column-1)/2];
	}


//	public void removeAllQueries(){
//		super.removeAllQueries();
//		probabilitydata = new LinkedList();
//		vardata = new LinkedList();
//	}
//
//	public void removeQuery(int query){
//		super.removeQuery(query);
//		probabilitydata.remove(query);
//		vardata.remove(query);
//	}
//
//	public void resetProb(){
//		probabilitydata = new LinkedList();
//	}
//
//	public void resetVar(){
//		vardata = new LinkedList();
//	}


	
	public LinkedList<String>[] getProbabilities(){
		return probabilitydata;
	}
}
