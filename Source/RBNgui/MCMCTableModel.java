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

public class MCMCTableModel extends AbstractQueryTableModel{

	/**
	 * @uml.property  name="probabilitydata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	LinkedList probabilitydata = new LinkedList();
	/**
	 * @uml.property  name="minprobabilitydata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	LinkedList minprobabilitydata = new LinkedList();
	/**
	 * @uml.property  name="maxprobabilitydata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	LinkedList maxprobabilitydata = new LinkedList();
	/**
	 * @uml.property  name="vardata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	LinkedList vardata = new LinkedList();
	/**
	 * keith cascio 20060511 ...
	 * @uml.property  name="acedata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */

	/**
	 * @uml.property  name="column"
	 */
	int column = 5;
	/**
	 * @uml.property  name="rownum"
	 */
	int rownum = 0;
	/** ... keith cascio */

	public MCMCTableModel(){
		super();
	}

	public int getColumnCount(){
		return column;
	}

	public int getRowCount(){
		return rownum;
	}

	public Object getValueAt( int row, int col )
	{
		switch( col ){
		case 0:
			if(      queryatomdata.size() > row )
				return queryatomdata.get(     row );
			break;
		case 1:
			if(      probabilitydata.size() > row )
				return probabilitydata.get(     row );
			break;
		case 2:
			if(      minprobabilitydata.size() > row )
				return minprobabilitydata.get(     row );
			break;
		case 3:
			if(      maxprobabilitydata.size() > row )
				return maxprobabilitydata.get(     row );
			break;
		case 4:
			if(      vardata.size() > row )
				return vardata.get(     row );
			break;
			/** keith cascio 20060511 ... */

			/** ... keith cascio */
		default:
			System.err.println( "column " + col + " out of range" );
		return STR_EMPTY;
		}
		System.err.println( "row " + row + " out of range at column " + col );
		return STR_EMPTY;
	}




	public void addQuery(String query){
		super.addQuery(query);
		probabilitydata.add(STR_EMPTY);
		minprobabilitydata.add(STR_EMPTY);
		maxprobabilitydata.add(STR_EMPTY);
		vardata.add(STR_EMPTY);
		/** ... keith cascio */
	}


	public void addProb(String prob){
		probabilitydata.add(myio.StringOps.doubleConverter(prob));
	}

	public void addProb(LinkedList prob){
		probabilitydata = prob;
	}
	
	public void addMinProb(String prob){
		minprobabilitydata.add(myio.StringOps.doubleConverter(prob));
	}
	public void addMaxProb(String prob){
		maxprobabilitydata.add(myio.StringOps.doubleConverter(prob));
	}
	public void addVar(String prob){
		vardata.add(myio.StringOps.doubleConverter(prob));
	}


	public void reset(){
		super.reset();
		probabilitydata = new LinkedList();
	}


	public void removeAllQueries(){
		super.removeAllQueries();
		probabilitydata = new LinkedList();
		minprobabilitydata = new LinkedList();
		maxprobabilitydata = new LinkedList();
		vardata = new LinkedList();
	}

	public void removeQuery(int query){
		super.removeQuery(query);
		probabilitydata.remove(query);
		minprobabilitydata.remove(query);
		maxprobabilitydata.remove(query);
		vardata.remove(query);
	}

	public void resetProb(){
		probabilitydata = new LinkedList();
	}
	
	
	public void resetMinProb(){
		minprobabilitydata = new LinkedList();
	}

	public void resetMaxProb(){
		maxprobabilitydata = new LinkedList();
	}

	public void resetVar(){
		vardata = new LinkedList();
	}


	
	public LinkedList<String> getProbabilities(){
		return probabilitydata;
	}
}
