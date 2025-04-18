/*
* SimpleBNGroundAtomNode.java 
* 
* Copyright (C) 2003 Max-Planck-Institut fuer Informatik,
*                    Helsinki Institute for Information Technology
*
* contact:
* jaeger@cs.auc.dk    www.cs.auc.dk/~jaeger/Primula.html
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

package RBNinference;

import java.util.*;
import RBNpackage.*;
import RBNutilities.*; 


public class SimpleBNGroundAtomNode extends SimpleBNNode implements GroundAtomNodeInt{

    public GroundAtom myatom;
    
    /** Creates new SimpleBNGroundAtomlNode */
   
    public SimpleBNGroundAtomNode(String name){
        super(name);
    }
    
    public SimpleBNGroundAtomNode(GroundAtom at) {
        super(at.asString());
        myatom = at;
        this.setIsboolean(at.rel().valtype()==Rel.BOOLEAN);
    }

    public SimpleBNGroundAtomNode(Rel r,int[] ar) {
        super(r.name.name + '(' + rbnutilities.arrayToString(ar) + ')');
        myatom = new GroundAtom(r,ar);
        this.setIsboolean(r.valtype()==Rel.BOOLEAN);
    }
    
     public SimpleBNGroundAtomNode(GroundAtom at, String name) {
        super(name);
        myatom = at;
        this.setIsboolean(at.rel().valtype()==Rel.BOOLEAN);
    }

     public SimpleBNGroundAtomNode(Rel r, String name, int[] ar) {
        super(name);
        myatom = new GroundAtom(r,ar);
        this.setIsboolean(r.valtype()==Rel.BOOLEAN);
    }
    
    public SimpleBNGroundAtomNode(GroundAtom at, String name, double[][] cpt,Vector parents,Vector children) {
        super(name,cpt,parents,children);
         myatom = at;
         this.setIsboolean(at.rel().valtype()==Rel.BOOLEAN);
    }

    public SimpleBNGroundAtomNode(Rel r, String name, int[] ar,double[][] cpt,Vector parents,Vector children) {
        super(name,cpt,parents,children);
         myatom = new GroundAtom(r,ar);
         this.setIsboolean(r.valtype()==Rel.BOOLEAN);
    }

    public GroundAtom myatom(){
	return myatom;
    }
}
