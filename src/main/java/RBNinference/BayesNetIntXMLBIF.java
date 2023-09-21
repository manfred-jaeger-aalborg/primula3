/*
* BayesNetIntXMLBIF.java 
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

// This class currently not fully implemented!
public abstract class BayesNetIntXMLBIF implements BayesNetInt {

    BufferedWriter bufwr;
    
    /** Creates new BayesNetIntXMLBIF */
    public BayesNetIntXMLBIF(String filename){
        bufwr = FileIO.openOutputFile(filename);
        try{
        bufwr.write(" <?xml version=\"1.0\" encoding=\"US-ASCII\"?>" + '\n');
        bufwr.write("<BIF VERSION=\"0.3\">" + '\n');
        bufwr.write("<NETWORK>" + '\n');
        bufwr.write("<NAME>InternalNetwork</NAME>" + '\n');
        bufwr.write("" + '\n');
        }
        catch (IOException e) {System.out.println(e);}
    }

    private String makeXMLName(String oldname){
        String result = new String();
        result = oldname.replace('(','I');
        result = result.replace(')','I');
        result = result.replace('.','_');
        result = result.replace(',','p');
        return result;
    }
    
    public void addNode(SimpleBNNode node,int xoffset){
        int[] pt = new int[2];
        pt[0] = 50*(node.xcoord + xoffset);
        pt[1] = 50*(node.level+2);
        addNode(node,pt);
    }
    
    public void addNode(SimpleBNNode node,boolean truthval,int xoffset){
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
    public void addNode(SimpleBNNode node,int[] coords){
        ListIterator li;
        double trueval,falseval;
        try{
            bufwr.write("<VARIABLE TYPE=\"nature\">" + '\n');
            bufwr.write('\t' + "<NAME>"+makeXMLName(node.name)+"</NAME>" + '\n');
            bufwr.write('\t' + "<OUTCOME>true</OUTCOME>" + '\n');
            bufwr.write('\t' + "<OUTCOME>false</OUTCOME>" + '\n');
            bufwr.write('\t' + "<PROPERTY>position = ("+coords[0] +"," + coords[1] +")</PROPERTY>" + '\n');
            bufwr.write("</VARIABLE>" + '\n' + '\n');
            
            bufwr.write("<DEFINITION>" + '\n');
            bufwr.write('\t' + "<FOR>"+makeXMLName(node.name)+"</FOR>" + '\n');
            li = node.parents.listIterator();
            while (li.hasNext()){
                bufwr.write('\t' + "<GIVEN>"+makeXMLName(((BNNode)li.next()).name)+"</GIVEN>" + '\n');
            }
            // The order of the cptentries in the xmlbif format is as follows
            // 
            //  pa1  pa2 pa3  |  true false
            //  ----------------------------
            //  t    t    t   |   1(7)     2
            //  t    t    f   |   3(6)    4
            //  t    f    t   |   5(5)     6
            //  .    .    .   |   .     .
            //  f    f    t   |   13(1)    14
            //  f    f    f   |   15(0)    16
            //
            // Numbers in parenthesis are the indices of the parameters
            // in the cptentries field of SimpleBNNode!
            bufwr.write('\t' +"<TABLE>" + '\n');
            for (int i=node.cptentries.length-1;i>=0;i--){
                trueval = node.cptentries[i] ;
                falseval = 1.0-trueval;
                //System.out.println("True: " +  trueval + " False:  " + falseval);  
                bufwr.write('\t');
                bufwr.write(trueval +  " " + falseval + '\n');
            }
            bufwr.write('\t' +"</TABLE>" + '\n');
            bufwr.write("</DEFINITION>" + '\n');
        }
        catch (IOException e) {System.out.println(e);}
    }
    
    /* Same as previous but now 
     * declare added node as instantiated
     * to truthval
     */
    public void addNode(SimpleBNNode node,int[] coords,boolean truthval){
        try{
        bufwr.write(node.name+ '\n');
        }
        catch (IOException e) {System.out.println(e);}
    }
    
    public void open(){
        try{
            bufwr.write("</NETWORK>" + '\n');
            bufwr.write("</BIF>" + '\n');
            bufwr.close();
        }
        catch (IOException e) {System.out.println(e);}
    }

}
