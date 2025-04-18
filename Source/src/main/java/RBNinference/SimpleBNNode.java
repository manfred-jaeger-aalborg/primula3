/*
* SimpleBNNode.java 
* 
* Class representing BN nodes in which the conditional probability distribution
* is represented in tabular form. 
*/

package RBNinference;

import java.util.*;
import RBNpackage.*;
import RBNutilities.*; 


public class SimpleBNNode extends BNNode {
    
    /* cptentries contains the probabilities for this node being
     * true in the following order (where parents are enumerated 
     * according to their order in the parents Vector):
     *
     *  pa1  pa2 pa3  |  true false
     *  ----------------------------
     *  t    t    t   |   7   
     *  t    t    f   |   6   
     *  t    f    t   |   5    
     *  .    .    .   |   .     
     *  f    f    t   |   1    
     *  f    f    f   |   0    
     *
     * 
     * 
     */
    double[][] cptentries;

    boolean exported;
    
    /** Creates new SimpleBNNode */
    
    public SimpleBNNode(){}
    
    public SimpleBNNode(String name) {
        super (name); 
        cptentries = new double[0][0];
        depthset = false;

        exported = false;
    }
    
    public SimpleBNNode(String name, double[][] cpt, Vector<BNNode> parents, Vector<BNNode> children ) {
        super (name,parents,children); 
        cptentries = cpt;
        depthset =false;

        exported = false;
    }
    
    public void setCPT(double[][] cpt){
        cptentries = cpt;
    }

    

    
    public boolean isIsolatedZeroNode()
    {
	if (parents.size()!=0) return false;
	if (children.size()!=0) return false;
	if (!this.isIsboolean()) return false;
	if (cptentries[0][0]!=1) return false; // No parents -> only cptentries[0] exists
	if (instantiated != -1) return false; // instantiated nodes should be shown, even if they are isolated zero!
	return true;
    }
    
//    /** returns true if this and sbnn are equivalent deterministic nodes,
//     * i.e. they are both deterministic, with the same set of parents, and
//     * equivalent cpts.
//     */
//    Temporarily disabled ....(needs update for categorical case)    
//    public boolean isDetEquivalent(SimpleBNNode sbnn){
//        boolean result = true;
//        if (!this.parentsSubset(sbnn)) result = false;
//        if (!sbnn.parentsSubset(this)) result = false;
//        if (result){
//            // Compute the permutation vector of parents:
//            // Example: this.parents = (A,B,C)
//            //          sbnn.parents = (B,C,A)
//            // Compute perm = (3,1,2)
//            int[] perm = new int[parents.size()];
//            for (int i=0;i<parents.size();i++)
//                perm[i]=sbnn.parents.indexOf(parents.get(i));
//            // Now compare the cpts (and check that they are 0,1-valued):
//            for (int i=0;i<cptentries.length;i++){
//                if (cptentries[i] != 0 && cptentries[i]!= 1) result = false;
//                if (cptentries[i]!=sbnn.cptentries[rbnutilities.computePermutedIndex(i, perm, 2, parents.size())])
//                    result = false;
//            }
//        }
//        return result;
//    }
}






