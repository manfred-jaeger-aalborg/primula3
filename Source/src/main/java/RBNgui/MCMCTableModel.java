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
//	LinkedList<String>[] vardata;
	/**
	 * keith cascio 20060511 ...
	 * @uml.property  name="acedata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */

	/**
	 * @uml.property  name="column"
	 */
	private int column;
	

	public MCMCTableModel(QueryTableModel qtm, Rel r){
		super();
		rownum=qtm.getRowCount();
		column = 1+2*(int)r.numvals(); // first column for query atoms
		p_v_data = new String[rownum][2*(int)r.numvals()];
		this.setQuery(qtm.getQuery());
		
	}

	public int getColumnCount(){
		return column;
	}

	public Object getValueAt( int row, int col )
	{
		if (row < this.getRowCount() && col<column) {
			if (col==0) // query atom
				return queryatomdata.get(row);
			else 
				return p_v_data[row][col-1];
		}
		else {
			System.err.println( "row " + row + " or column " + col + " out of range" );
			return STR_EMPTY;
		}

	}

	public void setProb(String[] probs, int row) {
		for (int i=0;i<probs.length;i++) {
			p_v_data[row][2*i]=myio.StringOps.doubleConverter(probs[i]);
		}
	}
	
	public void setVar(String[] vars, int row) {
		for (int i=0;i<vars.length;i++) {
			p_v_data[row][2*i+1]=myio.StringOps.doubleConverter(vars[i]);
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
		p_v_data = new String[rownum][column-1];
	}



}
