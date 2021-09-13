/*
 * CombFuncMean.java
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
import RBNgui.*;
import RBNinference.*;



public  class CombFuncMean extends MultLinCombFunc{

    /** Creates new CombFuncMean */
    public CombFuncMean() {
        name = "mean";
    }
    
    public  double evaluate(double[] args)
    {
        /* Returns 0 for empty argument! */
        
	
        double result = 0;
        
        for (int i=0; i<args.length; i++)
        {
            result = result+args[i];
        }
        
        if (args.length > 0){
            result = (1/((double)args.length))*result;
        }

        return result;
     }
     

    public int evaluatesTo(int[] args){
	if (args.length == 0 ) return 0;
	else{
	    boolean allones = true;
	    boolean allzeros = true;
	    for (int i=0;i<args.length;i++){
		if (args[i]!=1) allones = false;
		if (args[i]!=0) allzeros = false;
	    }
	    if (allones) return 1;
	    if (allzeros) return 0;
	    return -1;
	}
    }
   

     public void insertCompNetwork(LinkedList parnodes,SimpleBNNode targetnode, int decomposemode){
        switch (parnodes.size()){
            case 0:
                double cpt[] = {0};
                targetnode.setCPT(cpt);
                break;
            default:
                ListIterator li = parnodes.listIterator();
                int count = 1;
                BNNode nextparnode = (BNNode)li.next();
                LinkedList parents,children;
                parents = new LinkedList();
                parents.add(nextparnode);
                children = new LinkedList();
                double firstmean[] = {0,1};
                BNNode lastmeannode = new SimpleBNNode("mean."+ nextparnode.name + "." + targetnode.name,firstmean,parents,children);
                nextparnode.children.add(lastmeannode);
                BNNode nextmeannode;
		SimpleBNNode r1,r2;
	       	LinkedList r1parents,r1children,r2parents,r2children;
		double[] r1cpt,r2cpt; 
                count++;
                
                while (li.hasNext()){
		    double[] nextcpt;
		    switch(decomposemode){
		    case Primula.OPTION_DECOMPOSE: 
			nextparnode = (BNNode)li.next();
			nextcpt = new double[4];
			nextcpt[0] = 0;
			nextcpt[1] = (double)1.0/count;
			nextcpt[2] = (double)(count - 1.0)/count;
			nextcpt[3] = 1;
			parents = new LinkedList();
			parents.add(lastmeannode);
			parents.add(nextparnode);
			children = new LinkedList();
			nextmeannode = new SimpleBNNode("mean."+ nextparnode.name + "." + targetnode.name,nextcpt,parents,children);
                    
			lastmeannode.children.add(nextmeannode);
			nextparnode.children.add(nextmeannode);
			lastmeannode = nextmeannode;
			count++;
			break;
// 		    case Primula.OPTION_DECOMPOSE_DETERMINISTIC: //debug: bypass real case
// 			nextparnode = (BNNode)li.next();
// 			nextcpt = new double[4];
// 			nextcpt[0] = 0;
// 			nextcpt[1] = (double)1.0/count;
// 			nextcpt[2] = (double)(count - 1.0)/count;
// 			nextcpt[3] = 1;
// 			parents = new LinkedList();
// 			parents.add(lastmeannode);
// 			parents.add(nextparnode);
// 			children = new LinkedList();
// 			nextmeannode = new SimpleBNNode("mean."+ nextparnode.name + "." + targetnode.name,nextcpt,parents,children);
                    
// 			lastmeannode.children.add(nextmeannode);
// 			nextparnode.children.add(nextmeannode);
// 			lastmeannode = nextmeannode;
// 			count++;
// 			break;
		    case Primula.OPTION_DECOMPOSE_DETERMINISTIC: 
			// Introduce new 'randomizing root nodes'  for the two
			// non 0/1 valued cpt entries. New nodes are named
			// <nextmeannode.name>.r1 and <nextmeannode.name>.r2
			// New cpt for nextmeannode:
			// lastmeannnode   nextparnode  r1 r2 | 
			// --------------------------------------
			//      0               0        *  * | 0
			//      0               1        0  * | 0
			//      0               1        1  * | 1
			//      1               0        *  0 | 0
			//      1               0        *  1 | 1
			//      1               1        *  * | 1
			nextparnode = (BNNode)li.next();

			// define all cpts
			r1cpt = new double[1];
			r2cpt = new double[1];
			r1cpt[0] = (double)1.0/count;
			r2cpt[0] = (double)(count - 1.0)/count;
			nextcpt = new double[16];
			nextcpt[0] = 0;  // 0000
			nextcpt[1] = 0;  // 0001
			nextcpt[2] = 0;  // 0010
			nextcpt[3] = 0;  // 0011
			nextcpt[4] = 0;  // 0100
			nextcpt[5] = 0;  // 0101
			nextcpt[6] = 1;  // 0110
			nextcpt[7] = 1;  // 0111
			nextcpt[8] = 0;  // 1000 
			nextcpt[9] = 1;  // 1001
			nextcpt[10] = 0;  // 1010
			nextcpt[11] = 1;  // 1011
			nextcpt[12] = 1;  // 1100
			nextcpt[13] = 1;  // 1101
			nextcpt[14] = 1;  // 1110
			nextcpt[15] = 1;  // 1111
			// construct randomizing nodes	
			r1parents = new LinkedList();
			r2parents = new LinkedList();
			r1children = new LinkedList();
			r2children = new LinkedList();
			r1 = new SimpleBNNode("mean."+ nextparnode.name + "." + targetnode.name + ".r1",r1cpt,r1parents,r1children);
			r2 = new SimpleBNNode("mean."+ nextparnode.name + "." + targetnode.name + ".r2",r2cpt,r2parents,r2children);
			// construct new meannode
			parents = new LinkedList();
			parents.add(lastmeannode);
			parents.add(nextparnode);
			parents.add(r1);
			parents.add(r2);
			children = new LinkedList();
			nextmeannode = new SimpleBNNode("mean."+ nextparnode.name + "." + targetnode.name,nextcpt,parents,children);
                        // add missing links
			r1.children.add(nextmeannode);
			r2.children.add(nextmeannode);
 			lastmeannode.children.add(nextmeannode);
			nextparnode.children.add(nextmeannode);
			// children of nextmeannode are set in next iteration!

			lastmeannode = nextmeannode;
			count++;
			break;
		    }
                }
        targetnode.parents.add(lastmeannode);
        lastmeannode.children.add(targetnode);
        double targetcpt[] = {0,1};
        targetnode.setCPT(targetcpt);
        }
     }
}
