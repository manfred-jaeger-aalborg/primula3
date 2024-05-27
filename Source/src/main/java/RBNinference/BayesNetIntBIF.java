/*
* BayesNetIntBIF.java 
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

import java.util.*;
import java.io.*;
import RBNio.*;

public class BayesNetIntBIF implements BayesNetInt {

    BufferedWriter bufwr;
    
    /** Creates new BayesNetIntXMLBIF */
    public BayesNetIntBIF(File filename){
        bufwr = FileIO.openOutputFile(filename.getPath());
        try{
        bufwr.write("//Bayesian network created by Primula" + '\n');
        bufwr.write("//BIF format" + '\n');
        bufwr.write("network \"NoName\" { " + '\n' + "}" + '\n'+ '\n');
        }
        catch (IOException e) {System.out.println(e);}
    }

    /*
    private String makeXMLName(String oldname){
        String result = new String();
        result = oldname.replace('(','I');
        result = result.replace(')','I');
        result = result.replace('.','_');
        result = result.replace(',','p');
        return result;
    }
    */
    
    /*
    public void addNode(SimpleBNNode node,int xoffset){
        int[] pt = new int[2];
        pt[0] = 50*(node.xcoord + xoffset);
        pt[1] = 50*(node.level+2);
        addNode(node,pt);
    }
    */
    
    public void addNode(SimpleBNNode node,int xoffset, int truthval){
        int[] pt = new int[2];
        pt[0] = 50*(node.xcoord + xoffset);
        pt[1] = 50*(node.level+2);
        addNode(node,pt,truthval);
    }
    
     /* Add a new BoolNode to the network.
      * Place node at coordinates coords
      * (if concrete BNS supports such 
      * placement) 
      */ 
    public void addNode(SimpleBNNode node,int[] coords, int truthval){
        ListIterator li;
        double trueval,falseval;
        try{
            bufwr.write("variable \"" + node.name + "\" {" + '\n');
            bufwr.write('\t' + "type discrete[2] { \"true\" \"false\" };" + '\n');
            bufwr.write('\t' + "property \"position = ("+coords[0] +"," + coords[1] +")\";"+ '\n');
            if (truthval == 0){
                bufwr.write('\t' + "property \"observed false\";"+ '\n');
            }
            if (truthval == 1){
                bufwr.write('\t' + "property \"observed true\";"+ '\n');
            }
            bufwr.write("}" + '\n' + '\n');
            
            bufwr.write("probability ( \"" + node.name + "\" " );
            li = node.parents.listIterator();
            while (li.hasNext()){
                bufwr.write(" \"" + ((BNNode)li.next()).name + "\" " );
            }
                bufwr.write("  ) { " +'\n' );
            // The order of the cptentries in the bif format is as follows
            // 
            //  pa1  pa2 pa3  |  true false
            //  ----------------------------
            //  t    t    t   |   1(7)     9
            //  t    t    f   |   2(6)    10
            //  t    f    t   |   3(5)    11 
            //  .    .    .   |   .     .
            //  f    f    t   |   7(1)    15
            //  f    f    f   |   8(0)    16
            //
            // Numbers in parenthesis are the indices of the parameters
            // in the cptentries field of SimpleBNNode!
            bufwr.write('\t' +"table" + '\n' + '\t');
            for (int i=node.cptentries.length-1;i>=0;i--){
                trueval = node.cptentries[i] ;
                bufwr.write(trueval + " ");
            }
            for (int i=node.cptentries.length-1;i>=0;i--){
                falseval = 1-node.cptentries[i] ;
                bufwr.write(falseval + " ");
            }
            bufwr.write(";" + '\n');
            bufwr.write("}" + '\n' + '\n');
        }
        catch (IOException e) {System.out.println(e);}
    }
    
    public void open(){
        try{
            bufwr.close();
        }
        catch (IOException e) {System.out.println(e);}
    }

}
