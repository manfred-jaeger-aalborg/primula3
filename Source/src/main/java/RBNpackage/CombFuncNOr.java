/*
 * CombFuncNOr.java
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
import RBNinference.*;



public class CombFuncNOr extends MultLinCombFunc{

        /** Creates new CombFuncNOr */
    public CombFuncNOr() {
        name = "n-or";
    }
    
    public double evaluate(double[] args){
        double result=1;
        for (int i=0; i<args.length; i++)
        {
            result = result*(1-args[i]);
        }
        
        result = 1-result;
        return result;
    }
    
    public int evaluatesTo(int[] args){
	if (args.length == 0 ) return 0;
	else{
	    boolean existone = false;
	    boolean allzeros = true;
	    for (int i=0;i<args.length;i++){
		if (args[i]==1) existone = true;
		if (args[i]!=0) allzeros = false;
	    }
	    if (existone) return 1;
	    if (allzeros) return 0;
	    return -1;
	}
    }

    public double evaluateGrad(double[] vals, double[] derivs) {
		double result = 0;
        
        double factor = 1;
        
        for (int i=0;i<vals.length;i++)
                factor = factor*(1-vals[i]);
        
        if (factor == 0)
        	return 0.0;
        
        /* Now compute the partial derivative as
         *
         * \sum_{F_i\in fthetalist} (factor/(1-F_i))*(F_i')
         */
        for (int i=0;i<vals.length;i++){
                        result = result + (factor/(1-vals[i])*derivs[i]);
        }
        
		return result;
    }
    
    public void insertCompNetwork(LinkedList parnodes,SimpleBNNode targetnode,int decomposemode){
        // parameter decomposemode is irrelevant in the noisy-or implementation of this method!
        switch (parnodes.size()){
            case 0:
                double cpt[] = {0};
                targetnode.setCPT(cpt);
                break;
            default:
                ListIterator li = parnodes.listIterator();
                BNNode nextparnode = (BNNode)li.next();
                LinkedList parents,children;
                parents = new LinkedList();
                parents.add(nextparnode);
                children = new LinkedList();
                double firstor[] = {0,1};
                BNNode lastornode = new SimpleBNNode("or."+ nextparnode.name + "." + targetnode.name,firstor,parents,children);
                nextparnode.children.add(lastornode);
                BNNode nextornode;
               
                double orcpt[] = {0,1,1,1};
                while (li.hasNext()){
                    nextparnode = (BNNode)li.next();
                    parents = new LinkedList();
                    parents.add(lastornode);
                    parents.add(nextparnode);
                    children = new LinkedList();
                    nextornode = new SimpleBNNode("or."+ nextparnode.name + "." + targetnode.name,orcpt,parents,children);
                    //nextornode = new SimpleBNNode("or",orcpt,parents,children);
                    
                    lastornode.children.add(nextornode);
                    nextparnode.children.add(nextornode);
                    lastornode = nextornode;
                }
        targetnode.parents.add(lastornode);
        lastornode.children.add(targetnode);
        double targetcpt[] = {0,1};
        targetnode.setCPT(targetcpt);
        
        }
    }

}
