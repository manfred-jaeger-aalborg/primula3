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

/**
 * Class representing BN nodes in which the conditional 
 * probability distribution is represented by a probability formula
 * 
 * @author jaeger
 *
 */

public class ComplexBNNode extends BNNode{

    CPModel cpmodel;
    
    /** Creates new ComplexBNNode */
    public ComplexBNNode() {
    }
    
     public ComplexBNNode(String name, CPModel pf) {
        super (name);
        cpmodel = pf;
    }
    
    public ComplexBNNode(String name, CPModel pf, int val) {
        super (name,val);
        cpmodel = pf;
    }
    
 
    public ComplexBNNode(String name, CPModel pf, Vector<BNNode> parents, Vector<BNNode> children ) {
        super (name,parents,children);
        cpmodel = pf;
    }



    public boolean isIsolatedZeroNode(RelStruc rels)
    		throws RBNCompatibilityException{
    	if (parents.size()!=0) return false;
    	if (children.size()!=0) return false;
    	if (cpmodel instanceof ProbForm && ((ProbForm)cpmodel).evaluatesTo(rels)!=0) return false;
    	if (instantiated != -1) return false;
    	return true;
    }

    public CPModel cpmodel(){
    	return cpmodel;
    }
    
    public void setCPModel(ProbForm pf){
    	cpmodel = pf;
    }
    

}
