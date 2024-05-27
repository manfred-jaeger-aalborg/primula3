/*
 * CConstrEmpty.java
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


public class CConstrEmpty extends CConstr {

 
    /** Creates new CConstrEmpty */
    public CConstrEmpty() 
    {
	SSymbs = new Rel[0];
    }
    
    public String[] freevars()
    {
        return new String[0];
        
    }
    
    public CConstr substitute(String[] vars, int[] args)
    {
        return this;
    }
    
    public CConstr substitute(String[] vars, String[] args)
    {
        return this;
    }
    
    
    public String asString()
    {
        
        return new String();
    }
    
    public String asString(RelStruc A)
    {
        
        return new String();
    }
    
    public String[] parameters(String[] parameternumrels){
    	return new String[0];
    }
}
