/*
* TypeRel.java 
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

import RBNExceptions.*;

public class TypeRel extends Type{

	/**
	 * @uml.property  name="typerel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	BoolRel typerel;

	public TypeRel(String relname){
		typerel = new BoolRel(relname,1);
	}

	public TypeRel(BoolRel r){
		if (r.arity==1){
			name = r.name.name;
			typerel = r;
		}
		else
			throw new IllegalArgumentException("Attempt to define a non-unary relation as a type");
	}

	/** Returns all elements in the domain of rs that are of this 
	 *  type. 
	 **/
	public int[] allElements(RelStruc rs)
	throws RBNIllegalArgumentException
	{
		return rs.allElements(this);
	}

	public BoolRel getRel(){
		return typerel;
	}
	public String getName(){
		return typerel.name.name;
	}
}
