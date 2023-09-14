
/*
* ParameterTableModel.java 
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

package RBNgui;

import javax.swing.table.*;
import java.util.Hashtable;


public class ParameterTableModel extends AbstractTableModel {

    /**
	 * @uml.property  name="column"
	 */
    int column = 2;
    /**
	 * @uml.property  name="rownum"
	 */
    int rownum = 0;
    
    
    /**
	 * @uml.property  name="parameters"
	 */
    String[] parameters;
    /**
	 * @uml.property  name="estimates"
	 */
    double[] estimates;
    
    public int getColumnCount(){
    	return column;
    }

    public int getRowCount(){
    	return rownum;
    }

    public Object getValueAt( int row, int col) 
    {
    	switch( col ){
    	case 0:
    		if(      parameters.length > row )
    			return parameters[row];
    		break;
    	case 1:
    		if(      estimates.length > row )
    			return (Double)estimates[row];
    		break;
    	}
    	return new String("");
    }
    
//    public void setParameters(String[] params){
//    	parameters = new String[params.length+2];
//    	for (int i=0;i<params.length;i++)
//    		parameters[i]=params[i];
//    	parameters[parameters.length-2]="LIK: per node";
//    	parameters[parameters.length-1]="LOG-LIK: data";
//    	
//    	rownum = parameters.length;
//    	estimates = new double[parameters.length];
//    }
    
    /**
	 * @param params
	 * @uml.property  name="parameters"
	 */
    public void setParameters(Hashtable<String,Integer> params){
    	parameters = new String[params.size()+1];
    	for (String par: params.keySet())
    		parameters[params.get(par)]=par;
    	parameters[parameters.length-1]="LOG-LIK: data";
    	
    	rownum = parameters.length;
    	estimates = new double[parameters.length];
    }
    
    
    public void setEstimates(double[] vals){
    	for (int i=0;i<estimates.length;i++)
    		estimates[i]=vals[i];
    }
    
    public void setParameterEstimates(double[] vals){
    	for (int i=0;i<estimates.length-1;i++)
    		estimates[i]=vals[i];
    }
    
    /**
	 * @return
	 * @uml.property  name="parameters"
	 */
    public String[] getParameters(){
    	return parameters;
    }
    
    /**
	 * @return
	 * @uml.property  name="estimates"
	 */
    public double[] getEstimates(){
    	return estimates;
    }
    
//    public void initEstimates(int size){
//    	estimates = new double[size];
//    }
}
