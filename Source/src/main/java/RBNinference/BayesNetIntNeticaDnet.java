/*
* BayesNetIntNeticaDnet.java 
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
import java.io.*;
import RBNio.*;

public class BayesNetIntNeticaDnet implements BayesNetInt {

    BufferedWriter bufwr;
    private static int scale = 50;
    private static int baseoffset = 5;
    
    // Internal node identifiers are only allowed maximal
    // length of 31 characters.
    // Therefore simple substitution of illegal characters
    // is not enough to transform BNNode names into legal
    // identifiers. Instead original node names are collected
    // in vector. Internal node names are N<position in vector>
    private Vector nodenames;
    
    /** Creates new BayesNetIntNeticaDnet */
    public BayesNetIntNeticaDnet(File filename){
        bufwr = FileIO.openOutputFile(filename.getPath());
        nodenames = new Vector();  
        String netname = filename.getName();
        netname = netname.substring(0,netname.length()-4);
        try{
        bufwr.write("// ~->[DNET-1]->~ " + '\n');
            
        bufwr.write("bnet " + netname + "{" + '\n');
        bufwr.write('\n');
        bufwr.write("visual V1 {" + '\n');
        bufwr.write('\t' + "defdispform = LABELBOX;" + '\n');
        bufwr.write('\t' + "nodelabeling = TITLE;" + '\n');
        bufwr.write('\t' + "resolution = 72;" + '\n');
        // This should really be computed according to maximal x/y-coords appearing
        // in network!
        bufwr.write('\t' + "drawingbounds = (10000,2000);" + '\n');
        bufwr.write("};" + '\n');
        
        }
        catch (IOException e) {System.out.println(e);}
    }

    
    private String makeDnetName(String realname){
        nodenames.add(realname);
        int thisname = nodenames.size()-1;
        return("N"+thisname);
    }
    
    private String findDnetName(String realname){
        String result = "";
        boolean found = false;
        int currind = 0;
        while (!found){
            if (realname.equals(nodenames.elementAt(currind)))
                found = true;
            else currind++;
        }
        return("N"+currind);
    }
    
    public void addNode(SimpleBNNode node,int xoffset, int truthval){
        int[] pt = new int[2];
        pt[0] = scale*(node.xcoord + baseoffset + xoffset);
        pt[1] = scale*(node.level+2);
        addNode(node,pt,truthval);
    }
    
     /* Add a new BoolNode to the network.
      * Place node at coordinates coords
      * (if concrete BNS supports such 
      * placement) 
      */ 
    public void addNode(SimpleBNNode node,int[] coords, int truthval){
        ListIterator li;
        String internalname = makeDnetName(node.name);
        
        try{
            bufwr.write("node " + internalname + "{" + '\n');
	    if (node instanceof SimpleBNGroundAtomNode)
		bufwr.write('\t' + "title = \"" + node.name + "\";" + '\n');
	    else
		bufwr.write('\t' + "title = \"" + "aux" + "\";" + '\n');
            bufwr.write('\t' + "kind = NATURE;"+ '\n');    
            bufwr.write('\t' + "discrete = TRUE;"+ '\n');    
            bufwr.write('\t' + "states = (false,true);"+ '\n');  
            if (truthval == 0){
                bufwr.write('\t' + "evidence = false;"+ '\n');
            }
            if (truthval == 1){
                bufwr.write('\t' + "evidence = true;"+ '\n');
            }
            bufwr.write('\t' + "parents = ("); 
            if (node.parents.size()>0){
                li = node.parents.listIterator();
                while (li.hasNext()){
                    bufwr.write(findDnetName(((BNNode)li.next()).name));
                    if (li.hasNext()) bufwr.write(", ");
                }
            }
            bufwr.write(");" +'\n' );
            // The order of the cptentries in the Dnet format is as follows
            // 
            //  pa1  pa2 pa3  |  true false
            //  ----------------------------
            //  t    t    t   |   16(7)   15
            //  t    t    f   |   14(6)   13
            //  t    f    t   |   12(5)    11 
            //  .    .    .   |   .     .
            //  f    f    t   |   4(1)    3
            //  f    f    f   |   2(0)    1
            //
            // Numbers in parenthesis are the indices of the parameters
            // in the cptentries field of SimpleBNNode!
            bufwr.write('\t' + "probs = " + '\n');
            bufwr.write('\t');
            writesubcpt(node.cptentries);
            bufwr.write(";" + '\n');
            bufwr.write('\t' + "visual V1{"+ '\n');  
            bufwr.write('\t');
            bufwr.write('\t' + "center = ("+coords[0] +", " + coords[1] +");"+ '\n');
            bufwr.write('\t' + "};"+ '\n');  
            
            bufwr.write("};" +'\n' );
            bufwr.write('\n');
        }
        catch (IOException e) {System.out.println(e);}
    }
    
    private void writesubcpt(double[] subcpt){
        double trueval,falseval;
        try{
        if (subcpt.length == 1){ 
                trueval = subcpt[0] ;
                falseval = 1-trueval;
                bufwr.write("(" + falseval + ", " + trueval +  ")");
        }
        else {
            double[] lefthalf = new double[subcpt.length/2];
            double[] righthalf = new double[subcpt.length/2];
            for (int i=0;i<subcpt.length/2;i++){
                lefthalf[i]=subcpt[i];
                righthalf[i]=subcpt[subcpt.length/2+i];
            }
            bufwr.write("(");
            writesubcpt(lefthalf);
            bufwr.write(",");
            bufwr.write('\n');
            bufwr.write('\t');
            writesubcpt(righthalf);
            bufwr.write(")");
        }
        
        }catch (IOException e) {System.out.println(e);}
    }
    
    public void open(){
        try{
            bufwr.write("};");
            bufwr.close();
        }
        catch (IOException e) {System.out.println(e);}
    }

}
