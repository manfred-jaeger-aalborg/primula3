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

public class QueryTableModel extends AbstractTableModel{

	/**
	 * @uml.property  name="queryatomdata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	LinkedList queryatomdata = new LinkedList();
	/**
	 * @uml.property  name="mapdata"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	LinkedList<String> mapdata = new LinkedList<String>();
	
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
	List       acedata = new LinkedList();
	public static final String STR_EMPTY = "";

	/**
	 * @uml.property  name="column"
	 */
	int column = 7;
	/**
	 * @uml.property  name="rownum"
	 */
	int rownum = 0;
	/** ... keith cascio */

	public QueryTableModel(){
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
			if(      mapdata.size() > row )
				return mapdata.get(     row );
			break;
		case 2:
			if(      probabilitydata.size() > row )
				return probabilitydata.get(     row );
			break;
		case 3:
			if(      minprobabilitydata.size() > row )
				return minprobabilitydata.get(     row );
			break;
		case 4:
			if(      maxprobabilitydata.size() > row )
				return maxprobabilitydata.get(     row );
			break;
		case 5:
			if(      vardata.size() > row )
				return vardata.get(     row );
			break;
			/** keith cascio 20060511 ... */
		case 6:
			if(      acedata.size() > row )
				return acedata.get(     row );
			break;
			/** ... keith cascio */
		default:
			System.err.println( "column " + col + " out of range" );
		return STR_EMPTY;
		}
		System.err.println( "row " + row + " out of range at column " + col );
		return STR_EMPTY;
	}




	public void addQuery(String query){
		rownum++;
		queryatomdata.add(query);
		mapdata.add(STR_EMPTY);
		probabilitydata.add(STR_EMPTY);
		minprobabilitydata.add(STR_EMPTY);
		maxprobabilitydata.add(STR_EMPTY);
		vardata.add(STR_EMPTY);
		/** keith cascio 20060511 ... */
		acedata.add(STR_EMPTY);
		/** ... keith cascio */
	}

	public void addQuery(LinkedList query){
		rownum = query.size();
		queryatomdata = query;

	}

	public void addProb(String prob){
		probabilitydata.add(myio.StringOps.doubleConverter(prob));
	}

	public void addProb(LinkedList prob){
		probabilitydata = prob;
	}

	public void addMapVal(String mv){
		mapdata.add(mv);
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

	/** @author keith cascio
	@since 20060511 */
	public void addACE(String prob){
		acedata.add( myio.StringOps.doubleConverter( prob ) );
	}

	public void reset(){
		rownum = 0;
		queryatomdata = new LinkedList();
		probabilitydata = new LinkedList();
	}


	public void removeAllQueries(){
		queryatomdata = new LinkedList();
		mapdata = new LinkedList();
		probabilitydata = new LinkedList();
		minprobabilitydata = new LinkedList();
		maxprobabilitydata = new LinkedList();
		vardata = new LinkedList();
		/** keith cascio 20060511 ... */
		acedata = new LinkedList();
		/** ... keith cascio */
		rownum = 0;
	}

	public void removeQuery(int query){
		queryatomdata.remove(query);
		mapdata.remove(query);
		probabilitydata.remove(query);
		minprobabilitydata.remove(query);
		maxprobabilitydata.remove(query);
		vardata.remove(query);
		/** keith cascio 20060511 ... */
		acedata.remove(query);
		/** ... keith cascio */

		// 	if( query != 0 && probabilitydata.size() >= query ){
		// 	    probabilitydata.remove(query);
		// 	}
		// 	if( query != 0 && minprobabilitydata.size() >= query ){
		// 	    minprobabilitydata.remove(query);
		// 	}
		// 	if( query != 0 && maxprobabilitydata.size() >= query ){
//		maxprobabilitydata.remove(query);
//		}
//		if( query != 0 && vardata.size() >= query ){
//		vardata.remove(query);
//		}
		rownum--;
	}

	public LinkedList getQuery(){
		return queryatomdata;
	}

	public void resetProb(){
		probabilitydata = new LinkedList();
	}
	
	
	public void resetMapVals(){
		mapdata = new LinkedList();
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

	/** @author keith cascio
	@since 20060511 */
	public void resetACE(){
		if( acedata != null ){
			int len = acedata.size();
			for( int i=0; i<len; i++ ){
				acedata.set( i, "" );
			}
			fireTableDataChanged();
		}
	}

	/** @author keith cascio
	@since 20060608 */
	public void updateACE( Map<String,double[]> marginals ){
		//System.out.println( "QueryTableModel.updateACE( |"+marginals.size()+"| )" );
		acedata = new ArrayList( queryatomdata.size() );
		double[] prob;
		String strAtom, translation, strProb;
		for( Object atom : queryatomdata ){
			translation = BayesNetIntHuginNet.makeIDFromDisplayName( strAtom = atom.toString() );
			//System.out.println( "atom \"" +strAtom+ "\" == \"" + translation + "\"" );
			prob        = marginals.get( translation );
			strProb     = (prob == null) ? "0.0" : myio.StringOps.doubleConverter( Double.toString( prob[1] ) );
			acedata.add( strProb );
		}
		fireTableDataChanged();
	}
	
	public LinkedList<String> getMapValues() {
		return mapdata;
	}
	
	public LinkedList<String> getProbabilities(){
		return probabilitydata;
	}
}
