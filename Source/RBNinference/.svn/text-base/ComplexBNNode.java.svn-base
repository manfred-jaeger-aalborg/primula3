/*
* ComplexBNNode.java 
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
import RBNExceptions.*;


public class ComplexBNNode extends BNNode implements ComplexBNNodeInt {

    ProbForm probform;
    
    /** Creates new ComplexBNNode */
    public ComplexBNNode() {
    }
    
     public ComplexBNNode(String name, ProbForm pf) {
        super (name);
        probform = pf;
    }
    
    public ComplexBNNode(String name, ProbForm pf, int val) {
        super (name,val);
        probform = pf;
    }
    
 
    public ComplexBNNode(String name, ProbForm pf, LinkedList parents, LinkedList children ) {
        super (name,parents,children);
        probform = pf;
    }



    public boolean isIsolatedZeroNode(RelStruc rels)
    		throws RBNCompatibilityException{
    	if (parents.size()!=0) return false;
    	if (children.size()!=0) return false;
    	if (probform.evaluatesTo(rels)!=0) return false;
    	if (instantiated != -1) return false;
    	return true;
    }

    public ProbForm probform(){
    	return probform;
    }
    
    public void setProbForm(ProbForm pf){
    	probform = pf;
    }
}
