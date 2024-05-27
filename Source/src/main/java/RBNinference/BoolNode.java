/*
* BoolNode.java 
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

import java.io.*;

public class BoolNode extends java.lang.Object {

    String varname;
    BoolNode[] parents;
    /* The cpt is represented as a vector of length
     * 2^|parents|. The ith component in this vector
     * is the conditional probability of this variable
     * being true, given the ith configuration of 
     * the parent variables. The ith configuration of 
     * the parents is the instantiation that corresponds 
     * to the binary representation of the integer i,
     * when "true" is represented by 1, "false" by 0, and
     * the values are listed in the order in which the 
     * parents appear in the array "parents". 
     */
    double[] cptentries;
    
    /** Creates new BayesNetNode */
    public BoolNode() {
    }

    public BoolNode(String n,BoolNode[] p)
    {
        varname = n;
        parents = p;
        cptentries = new double[(int)Math.pow(2,p.length)];
    }
    
    /* Set a cpt entry for parent configuration
     * given by its index
     */
    public void setCptEntry(int i, double val)
    {
        cptentries[i]=val;
    }
    
     /* Set a cpt entry for parent configuration
     * given by 0/1 - vector.
     */
    public void setCptEntry(int[] parvals, double val)
    {
        // No check performed whether parvals is 
        // a 0/1-vector of length parents.length !
        int i =0;
        for (int k=0;k<parvals.length;k++)
            i = i+(parvals[parvals.length-k-1])*2^k;
        cptentries[i]=val;
    }
    
    
}


