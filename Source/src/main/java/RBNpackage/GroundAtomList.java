/*
* AtomList.java
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

import java.util.*;
import RBNutilities.*;


public class GroundAtomList extends java.lang.Object {

    /**
	 * @uml.property  name="atoms"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.Atom"
	 */
    private Vector<GroundAtom> atoms;  // Vector of Atom


    /** Creates new AtomList */
    public GroundAtomList() {
        atoms = new Vector<GroundAtom>();
    }

    /** @author keith cascio
    	@since 20060515 */
    public GroundAtomList( GroundAtomList toCopy ){
    	if( toCopy.atoms != null ) this.atoms = new Vector<GroundAtom>( toCopy.atoms );
    	else this.atoms = new Vector<GroundAtom>();
    }

    /** @author keith cascio
    	@since 20060515 */
    public int size(){
    	return (this.atoms == null) ? 0 : this.atoms.size();
    }

    public void reset(){
        atoms = new Vector<GroundAtom>();
    }

    public boolean isEmpty(){
        if (atoms.size() > 0) return false;
        else return true;
    }

    public void delete(Rel r, int[] a){
        GroundAtom thisatom = new GroundAtom(r,a);
        int pos = 0;
        while (pos < atoms.size()){
                if(((GroundAtom)atoms.elementAt(pos)).equals(thisatom))
                    atoms.remove(pos);
                else
                    pos++;
         }
    }
//Thrane
    public void delete(String owner){
      GroundAtom thisatom;
      int pos = 0;
      System.out.println("owner "+owner);
      while (pos < atoms.size()){
      	thisatom = (GroundAtom)atoms.elementAt(pos);
      	if((thisatom.getOwner()).equals(owner)){
      		atoms.remove(pos);
      	}
        else
          pos++;
      }
    }

//Thrane

    public void delete(int a){
        // delete all atoms with a in the arguments
        int pos =0;
        while (pos < atoms.size())
            if (((GroundAtom)atoms.elementAt(pos)).inArgument(a))
                atoms.removeElementAt(pos);
            else pos++;
    }

    public void shiftArgs(int a){
        // Replaces all arguments b of trueAtoms and falseAtoms lists
        // by b-1 if b>a (needed after the deletion of node with index a from
        // the underlying SparseRelStruc)
        GroundAtom curratom;
        for (int i=0;i<atoms.size();i++){
            curratom = (GroundAtom)atoms.elementAt(i);
            rbnutilities.arrayShiftArgs(curratom.args,a);
        }
    }


    public void add(Rel r, int[] a){
      if (!this.contains(r,a))
        atoms.add(new GroundAtom(r,a));
    }
//THrane
    public void add(Rel r, int[] a, String owner){
        if (!this.contains(r,a)) 
        	atoms.add(new GroundAtom(r,a,owner));
    }


    public GroundAtom atomAt(int i){
	return (GroundAtom)atoms.elementAt(i);
    }

    public boolean contains(Rel r, int[] a, String owner){
        GroundAtom thisatom = new GroundAtom(r,a, owner);
         boolean found = false;
         for (int i = 0;i<atoms.size();i++){
                if(((GroundAtom)atoms.elementAt(i)).equals(thisatom))
                        found = true;
         }
         return found;
    }

//THrane

    public boolean contains(GroundAtom at){
      boolean found = false;
      for (int i = 0;i<atoms.size();i++){
	      if(((GroundAtom)atoms.elementAt(i)).equals(at)){
	      	found = true;
	      }
      }
      return found;
    }


    public boolean contains(Rel r, int[] a){
      GroundAtom thisatom = new GroundAtom(r,a);
      return contains(thisatom);
    }


    public Vector<GroundAtom> allAtoms(){
        return atoms;
    }

}
