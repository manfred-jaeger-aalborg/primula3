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
import RBNpackage.*;
import RBNinference.BayesNetIntHuginNet;

public class QueryTableModel extends AbstractTableModel{

	/**
	 * @uml.property  name="queryatomdata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	LinkedList<String> queryatomdata;
	

	public static final String STR_EMPTY = "";

	/**
	 * @uml.property  name="column"
	 */
	private int column = 1;
	/**
	 * @uml.property  name="rownum"
	 */
	protected int rownum = 0;
	/** ... keith cascio */

	public QueryTableModel(){
		queryatomdata = new LinkedList<String>();
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
				default:
			System.err.println( "column " + col + " out of range" );
		return STR_EMPTY;
		}
		System.err.println( "row " + row + " out of range at column " + col );
		return STR_EMPTY;
	}



	public void addQuery(GroundAtomList gal) {
		for (GroundAtom ga: gal.allAtoms()) {
			addQuery(ga.asString());
		}
	}

	public void addQuery(String query){
		rownum++;
		queryatomdata.add(query);
	}

	public void addQuery(LinkedList query){
		rownum = query.size();
		queryatomdata = query;

	}

	public void reset(){
		rownum = 0;
		queryatomdata = new LinkedList();
	}


	public void removeAllQueries(){
		queryatomdata = new LinkedList();
		rownum = 0;
	}

	public void removeQuery(int query){
		queryatomdata.remove(query);
		rownum--;
	}

	public LinkedList getQuery(){
		return queryatomdata;
	}



}
