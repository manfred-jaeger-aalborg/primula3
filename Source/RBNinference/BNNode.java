/*
 * BNNode.java
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

/**
 *
 * @author  administrator
 * @version
 */
//public class BNNode implements BNNodeInt{
public class BNNode{
    public String name;
    public LinkedList<BNNode> parents;
    public LinkedList<BNNode> children;

    /* Set to 0 resp. 1 if node instantiated to false, resp. true
     * Set to -1 if node not instantiated
     */
    public int instantiated;

    protected int depth;
    int height;
    int level;
    boolean depthset;

    int xcoord;
	int ycoord;
    int posnumber;// auxiliary field for layout
    double weight;


    static int vissize = 5; // length of visited array
    boolean[] visited;
    BNNode conncompof; // pointer to a BNNode that is the representative
    // for the connected component that this BNNode
    // belongs to (= first BNNode of this connected
    // component in exportnodes list of BayesConstructor
    /** Creates new BNNode */
    public BNNode() {}

    public BNNode(String n) {
        name = n;
        parents = new LinkedList<BNNode>();
        children = new LinkedList<BNNode>();
        instantiated = -1;
        xcoord = 0;
        ycoord = 0;
        posnumber = 0;
        weight=0;

        depth = -1;
        height = 0;
        level = 0;
        visited = new boolean[vissize];
        for (int i=0;i<vissize;i++) visited[i] = false;
        conncompof = null;
    }

    public BNNode(String n, int val) {
        name = n;
        parents = new LinkedList<BNNode>();
        children = new LinkedList<BNNode>();
        instantiated = val;
        xcoord = 0;
        ycoord = 0;
        posnumber = 0;
        weight=0;
        depth = -1;
        height = 0;
        level = 0;
        visited = new boolean[vissize];
        for (int i=0;i<vissize;i++) visited[i] = false;
        conncompof = null;
    }


    public BNNode(String n,LinkedList<BNNode> par, LinkedList<BNNode> chil) {
        name = n;
        parents = par;
        children = chil;
        instantiated = -1;
        xcoord = 0;
        ycoord = 0;
        posnumber = 0;
        weight = 0;
        depth = -1;
        height = 0;
        level = 0;
        visited = new boolean[vissize];
        for (int i=0;i<vissize;i++) visited[i] = false;
        conncompof = null;
    }

    /** Adds newchild to children if not in
     * children list already
     */
    public void addToChildren(BNNode newchild){
        if (!children.contains(newchild)) children.add(newchild);
    }

    public void addToParents(BNNode newpar){
        if (!parents.contains(newpar)) parents.add(newpar);
    }



    public Vector<BNNode> buildNodeStack()
	/* Returns Vector of nodes that are in the connected component
	   of this BNNode
	*/
    {
	Vector<BNNode> nodestack = new Vector<BNNode>();
	BNNode nextnode;
	if (!visited[4]){
            nodestack.add(this);
            this.visited[4]=true;
            ListIterator<BNNode> li = parents.listIterator();
            while (li.hasNext()){
            	nextnode = (BNNode)li.next();
            	nextnode.buildNodeStack(nodestack);
            }
            li = children.listIterator();
            while (li.hasNext()){
            	((BNNode)li.next()).buildNodeStack(nodestack);
            }
	}
	resetVisitedUpDownstream(4);
	return nodestack;
    }

    private void buildNodeStack(Vector<BNNode> nodestack)
    {
    	if (!visited[4]){
    		nodestack.add(this);
    		visited[4]=true;
    		ListIterator<BNNode> li = parents.listIterator();
    		while (li.hasNext()){
    			((BNNode)li.next()).buildNodeStack(nodestack);
    		}
    		li = children.listIterator();
    		while (li.hasNext()){
    			((BNNode)li.next()).buildNodeStack(nodestack);
    		}
    	}
    }

    public int depth(){
	return depth;
    }

      public void instantiate(int truthval){
        instantiated = truthval;
    }

    public int instantiatedTo(){
	return instantiated;
    }


    public void resetParents(){
	parents = new LinkedList<BNNode>();
    }

    public void replaceInParentList(BNNode oldpar,BNNode newpar)
	/* Replace oldpar with newpar in parents at same position
	 */
    {
        boolean found = false;
        ListIterator<BNNode> li = parents.listIterator();
        while (li.hasNext() && !found)
	    {
		if (((BNNode)li.next()).name == oldpar.name)
		    {
			found = true;
			li.remove();
			li.add(newpar);
		    }
	    }
    }

    public void replaceInChildrenList(BNNode oldchil,BNNode newchil)
	/* Replace oldchil with newchil in parents at same position
	 */
    {
        boolean found = false;
        ListIterator<BNNode> li = children.listIterator();
        while (li.hasNext() && !found)
	    {
		if (((BNNode)li.next()).name == oldchil.name)
		    {
			found = true;
			li.remove();
			li.add(newchil);
		    }
	    }
    }


    public void resetVisited(int ind)
	// Resets visited[i] to false for all reachable nodes.
	// Condition: i != 4
	// When ind < 0 resets visited[j] for j=0,...,3
    {
	if (ind == 4){
	    System.out.println("Warning: resetVisited called for visited[4]!");
	}
	else{
	    Vector<BNNode> nodestack = buildNodeStack();
	    for (int i=0;i<nodestack.size();i++){
		if (ind >=0)
		    ((BNNode)nodestack.elementAt(i)).visited[ind]=false;
		else
		    for (int j=0; j<4; j++)
			((BNNode)nodestack.elementAt(i)).visited[j]=false;
	    }

	}
    }

    public void resetVisitedUpstream(int ind){
        // Resets visited[i] to false at all upstream nodes reachable
        // from this node. Condition: all visited[i] must
        // be true before method is called.
        // if ind < 0 then method resets all components of visited
	BNNode nextnode;
	if (ind >= 0)
	    visited[ind] = false;
	else for (int i=0;i<visited.length;i++) visited[i] = false;
	ListIterator<BNNode> li = parents.listIterator();
	while (li.hasNext()){
	    nextnode = (BNNode)li.next();
	    if (ind >= 0){
		if (nextnode.visited[ind]) nextnode.resetVisitedUpstream(ind); }
	    else if (nextnode.visited[0]) nextnode.resetVisitedUpstream(ind);
	    // second case: visited[0],visited[1],... all have the same
	    // value, so can take visited[0]
	}
    }

    public void resetVisitedUpDownstream(int ind){
        // Resets visited[i] to false at all nodes reachable
        // from this node. Condition: all visited[i] must
        // be true before method is called.
	BNNode nextnode;
	if (ind >= 0)
	    visited[ind] = false;
	else for (int i=0;i<visited.length;i++) visited[i] = false;
	ListIterator<BNNode> li = parents.listIterator();
	while (li.hasNext()){
	    nextnode = (BNNode)li.next();
	    if (ind >= 0){
		if (nextnode.visited[ind]) nextnode.resetVisitedUpDownstream(ind); }
	    else if (nextnode.visited[0]) nextnode.resetVisitedUpDownstream(ind);

	}
	li = children.listIterator();
	while (li.hasNext()){
	    nextnode = (BNNode)li.next();
	    if (ind >= 0){
		if (nextnode.visited[ind]) nextnode.resetVisitedUpDownstream(ind); }
	    else if (nextnode.visited[0]) nextnode.resetVisitedUpDownstream(ind);
	}

    }

    public void setDepth(int d){
	depth=d;
    }


    /** Prints names of all nodes connected to this one
     */
    public void showAllReachable(){
        Vector<BNNode> nodestack = buildNodeStack();
        System.out.println("Nodes connected to "+ this.name);
        for (int i=0;i<nodestack.size();i++){
            System.out.println(((BNNode)nodestack.elementAt(i)).name);
        }
    }




    /**
       returns true if  this.parents is subset of bnn.parents
    */
    public boolean parentsSubset(BNNode bnn){
        boolean result = true;
        ListIterator<BNNode> li = parents.listIterator();
        while (li.hasNext() && result){
            if (!bnn.parents.contains(li.next())) result = false;
        }
        return result;
    }

    /** @author keith cascio
    	@since 20061010 */
    public int sizeFamily(){
    	return ((this.parents  == null) ? 0 : this.parents.size() ) +
	       ((this.children == null) ? 0 : this.children.size());
    }

    /** Computes the mean of the (current) xcoords of parents
	and children. Subroutine of balanceLevels
	Returns -1 if node has no family. */
    public double familyXcenter(){
	int sizeFamily = this.sizeFamily();
	return (sizeFamily < 1) ? -1 : (xSum( this.parents ) + xSum( this.children )) / sizeFamily;
    }

    /** @author keith cascio
    	@since 20061010 */
    public static double xSum( Collection<BNNode> bnnodes ){
    	double xsum = 0;
    	if( bnnodes == null ) return xsum;
    	for( BNNode next : bnnodes ) xsum += next.xcoord;
    	return xsum;
    }
    
    public ProbForm probform() {
    	return null;
    }
}
