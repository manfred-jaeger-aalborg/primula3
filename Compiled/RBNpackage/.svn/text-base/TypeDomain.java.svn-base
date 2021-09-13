/*
* TypeDomain.java 
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

package RBNpackage;


/** A "vacuous" type that represents all elements 
 * of a domain 
 **/
public class TypeDomain extends Type{
    
    
    public TypeDomain(){
	name = "Domain";
    }

    /** Returns integer vector [0,1,...,dom-1]
     * where dom is the domainsize of rs
     **/
    public int[] allElements(RelStruc rs){
	int[] result = new int[rs.dom];
	for (int i=0;i<rs.dom;i++)
	    result[i]=i;
	return result;
    }

    public String getName(){
    	return "Domain";
    }
}
