/*
* ComplexBNGroundAtomNode.java 
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



public class ComplexBNGroundAtomNode extends ComplexBNNode implements GroundAtomNodeInt {

    private GroundAtom myatom;

    /* String of length myatom.rel.arity
     * contains the variables in probform
     * for which the arguments of myatom.args 
     * have to be substituted
     *
     * Example: myatom =  r[1,2,1]
     *          probform = r(u)+(1-r(u))s(v,w)
     *          vars = [v,w,u]
     * then probability of this atom is given by
     *       r(1)+(1-r(1))s(1,2)
     */
    private String[] vars;
    
    /** Creates new ComplexBNGroundAtomlNode */

    public ComplexBNGroundAtomNode(){
    }

     public ComplexBNGroundAtomNode(GroundAtom at,CPModel pf) {
        super(at.asString(),pf);
        myatom = at;
        this.setIsboolean(at.rel().valtype()==Rel.BOOLEAN);
    }

    public ComplexBNGroundAtomNode(GroundAtom at,CPModel pf,int inst) {
        super(at.asString(),pf,inst);
        myatom = at;
        this.setIsboolean(at.rel().valtype()==Rel.BOOLEAN);
    }

    
    public ComplexBNGroundAtomNode(Rel r,int[] ar,CPModel pf) {
        super(r.printname() + '(' + rbnutilities.arrayToString(ar) + ')',pf);
        myatom = new GroundAtom(r,ar);
        this.setIsboolean(r.valtype()==Rel.BOOLEAN);
    }
    

     public ComplexBNGroundAtomNode(GroundAtom at,String arnames,CPModel pf) {
        super(at.relname() + '(' + arnames + ')',pf);
        myatom = at;
        this.setIsboolean(at.rel().valtype()==Rel.BOOLEAN);
    }

     public ComplexBNGroundAtomNode(Rel r,String arnames, int[] ar,CPModel pf) {
        super(r.printname() + '(' + arnames + ')',pf);
         myatom = new GroundAtom(r,ar);
         this.setIsboolean(r.valtype()==Rel.BOOLEAN);
    }
    
    public ComplexBNGroundAtomNode(GroundAtom at, CPModel pf,Vector<BNNode> parents,Vector<BNNode> children) {
        super(at.asString(),pf,parents,children);
        myatom = at;
        this.setIsboolean(at.rel().valtype()==Rel.BOOLEAN);
    }

    public ComplexBNGroundAtomNode(Rel r,int[] ar,CPModel pf, Vector<BNNode> parents,Vector<BNNode> children) {
        super(r.printname() + '(' + rbnutilities.arrayToString(ar) + ')',pf,parents,children);
        myatom = new GroundAtom(r,ar);
        this.setIsboolean(r.valtype()==Rel.BOOLEAN);
    }
    

    public GroundAtom myatom(){
	return myatom;
    }
    
    public String[] vars() {
    	return vars;
    }

}
