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

public class MAPTableModel extends QueryTableModel{


	/**
	 * @uml.property  name="mapdata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	String[] mapdata;

	public static final String STR_EMPTY = "";

	/**
	 * @uml.property  name="column"
	 */
	int column = 2;
	/**
	 * @uml.property  name="rownum"
	 */


	public MAPTableModel(){
		super();
	}

	public MAPTableModel(QueryTableModel qtm, Rel r){
		super();
		rownum=qtm.getRowCount();
		column = 2; // first column for query atoms
		mapdata = new String[rownum];
		this.setQuery(qtm.getQuery());
	}
	
	public int getColumnCount(){
		return column;
	}

	public Object getValueAt( int row, int col )
	{
		switch( col ){
		case 0:
			if(      queryatomdata.size() > row )
				return queryatomdata.get(     row );
			break;
		case 1:
			return mapdata[row];
			/** ... keith cascio */
		default:
			System.err.println( "column " + col + " out of range" );
		return STR_EMPTY;
		}
		System.err.println( "row " + row + " out of range at column " + col );
		return STR_EMPTY;
	}

	
	public void reset(){
		super.reset();
	}

	public void resetMapVals(){
		mapdata = new String[rownum];
	}
	
	public void setValue(String val, int row) {
		mapdata[row]=val;
	}
	
}
