/*
* BayesNetInt.java 
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

/**
 *
 * @author  administrator
 * @version 
 */

/* Serves as an interface between RBNs and 
 * Bayesian network systems (BNS, e.g. JavaBayes, Hugin,...).
 * Implementations of interface must 
 * initialize the private datastructures
 * for the BNS,
 * and incrementally build a network through 
 * an implementation of the addNode methods.
 * The open() method starts the graphical user 
 * interface of the BNS with
 * the constructed network. Alternatively, open() 
 * can just save the network in some format to
 * a file.
 */
public interface BayesNetInt{

    /* Add a new BoolNode to the network.
     * Leave placement of node to method 
     * provided by implementation (possibly
     * no placement specified)
     * if truthval =  0: add node instantiated to false
     * if truthval =  1: add node instantiated to true
     * else : add node as uninstantiated
     */
    public abstract void addNode(SimpleBNNode node,  int xoffset, int truthval);
    
    /** Add node to the network.
      Place node at coordinates coords
      (if concrete BNS supports such 
      placement) */ 
    public abstract void addNode(SimpleBNNode node, int[] coords, int truthval);
   
    
    public abstract void open();

}
