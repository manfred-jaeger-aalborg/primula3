/*
 * CConstrAtom.java
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
import RBNutilities.*;


public class CConstrEq extends CConstr{

    /**
	 * @uml.property  name="arguments" multiplicity="(0 -1)" dimension="1"
	 */
    String[] arguments;
    
    /** Creates new CConstrAtom */
    public CConstrEq(String arg1, String arg2) {
        SSymbs = new Rel[0];
        arguments = new String[2];
        arguments[0]=arg1;
        arguments[1]=arg2;
    }
    
     public String[] freevars()
    {
        return rbnutilities.NonIntOnly(arguments);
    }
    
    public CConstr substitute(String[] vars, int[] args)
    {
        CConstrEq result;
        String[] resargs = new String[2];
        String nextarg;
        for (int i = 0; i<2; i++)
        {
            nextarg = arguments[i];
            for (int j = 0; j<vars.length; j++)
            {
                if (nextarg.equals(vars[j])) nextarg = String.valueOf(args[j]);
            }
            resargs[i] = nextarg;
        }
        result= new CConstrEq(resargs[0],resargs[1]);
        return result;
    }
    
     public CConstr substitute(String[] vars, String[] args)
    {
        CConstrEq result;
        String[] resargs = new String[2];
        String nextarg;
        for (int i = 0; i<2; i++)
        {
            nextarg = arguments[i];
            for (int j = 0; j<vars.length; j++)
            {
                if (nextarg.equals(vars[j])) nextarg = args[j];
            }
            resargs[i] = nextarg;
        }
        result= new CConstrEq(resargs[0],resargs[1]);
        return result;
    }
    
    
  
     
    public String asString()
    {
        String result;
        result = arguments[0] + "=" + arguments[1];
        return result;
    }

    public String asString(RelStruc A)
    {
        String result="";
	if (rbnutilities.IsInteger(arguments[0]))
	    result = result + A.nameAt(Integer.parseInt(arguments[0])) + "=" ;
	else result = result + arguments[0] + "=" ;
	if (rbnutilities.IsInteger(arguments[1]))
	    result = result + A.nameAt(Integer.parseInt(arguments[1]));
	else result = result + arguments[1] ;
	return result;
    }

    public String[] parameters(String[] parameternumrels){
    	return new String[0];
    }
}
