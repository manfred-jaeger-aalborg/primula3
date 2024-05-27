/*
 * CConstrNeg.java
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


public class CConstrNeg extends CConstr {

    /**
	 * @uml.property  name="c1"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    CConstr C1;
    
    /** Creates new CConstrNeg */
    public CConstrNeg(CConstr c1) 
    {
        SSymbs = c1.SSymbs;
        C1 = c1;
    }
    
     public String[] freevars()
    {
        return C1.freevars();
        
    }
    
    public CConstr substitute(String[] vars, int[] args)
    {
        return new CConstrNeg(C1.substitute(vars,args));
    }
    
     public CConstr substitute(String[] vars, String[] args)
    {
        return new CConstrNeg(C1.substitute(vars,args));
    }
    
    /*
     public boolean satisfied(RelStruc relstr, int[] args)
    {
        return (!C1.satisfied(relstr,args));
    }
    */
    
    
    public String asString()
    {
        String result;
        result = "~" + C1.asString();
        return result;
    }
       
    public String asString(RelStruc A)
    {
        String result;
        result = "~" + C1.asString(A);
        return result;
    }
    
//    public  String[] parameters(String[] parameternumrels){
//    	return C1.parameters(parameternumrels);
//    }
}
