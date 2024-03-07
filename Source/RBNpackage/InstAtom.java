/*
 * InstAtom.java
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



public class InstAtom extends GroundAtom {

    /**
	 * @uml.property  name="truthval"
	 */
    public int val;

    /** Creates new InstAtom */
    public InstAtom() {
    }

    public InstAtom(Rel r, int[] a, int tv){
        super(r,a);
        val = tv;
    }

    /** @author keith cascio
	@since 20061020 */
    public boolean equals( Object instatom ){
    	if(            !( instatom instanceof GroundAtom     ) ) return false;
    	boolean truth = ( instatom instanceof InstAtom ) ? (this.val == ((InstAtom)instatom).val) : true;
    	//System.out.println( this + ".equals( " + instatom + " )?" + (super.isIdenticalTo( instatom ) && (this.truthval == instatom.truthval)) );
	return super.isIdenticalTo( (GroundAtom)instatom ) && truth;
    }

    /** @author keith cascio
	@since 20061020 */
    public int hashCodeImpl(){
    	return super.hashCodeImpl() + (this.val);
    }

    /** @author keith cascio
	@since 20061023 */
    public String toString(){
    	return super.asString() + "=" + (this.rel.get_String_val(val)) + "h" + hashCode() + "ih" + System.identityHashCode( this );
    }
    
    public String val_string() {
    	return this.rel.get_String_val(val);
    }
}
