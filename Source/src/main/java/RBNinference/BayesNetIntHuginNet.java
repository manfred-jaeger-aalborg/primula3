/*
* BayesNetIntHuginNet.java
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

import edu.ucla.belief.ui.primula.*;

public class BayesNetIntHuginNet implements BayesNetInt {

    BufferedWriter bufwr;
    private static int scalex = 1;
    private static int scaley = 100;

    /** Creates new BayesNetIntHuginNet */
    public BayesNetIntHuginNet(File filename){
        bufwr = FileIO.openOutputFile(filename.getPath());
        String netname = filename.getName();
        netname = netname.substring(0,netname.length()-4);
        try{
        bufwr.write("class " + netname +  '\n');
        bufwr.write("{" + '\n');
        }
        catch (IOException e) {System.out.println(e);}
    }

    /** @author keith cascio
        @since 20040409 */
	protected BayesNetIntHuginNet(){
	}

    public static String makeNetName(String oldname){
        //String result = new String();
        String
        result = oldname;
        result = result.replace('(','I');
        result = result.replace(')','I');
        result = result.replace('.','_');
        result = result.replace(',','p');
        return result;
    }

    /** @author keith cascio
        @since 20060609 */
    public static String makeIDFromDisplayName( String displayName ){
    	BUFF.setLength(0);
    	int len = displayName.length();
    	char original;
    	char translation;
    	for( int i=0; i<len; i++ ){
    		switch( original = displayName.charAt(i) )
    		{
    			case '(':
    			case ')':
    				translation = 'I';
    				break;
    			case '.':
    				translation = '_';
    				break;
    			case ',':
    				translation = 'p';
    				break;
    			case ' ':
    				continue;
    			default:
    				translation = original;
    				break;
    		}
    		BUFF.append( translation );
    	}
    	return BUFF.toString();
    }
    private static StringBuilder BUFF = new StringBuilder( 128 );

    /** @author keith cascio
        @since 20040413 */
    protected void writeTruthValue( int truthval ) throws IOException
    {
        if( truthval >= (int)0 )
        {
            String strValue = Integer.toString( truthval );//(truthval == (int)0) ? SamiamUIInt.STR_VALUE_FALSE : SamiamUIInt.STR_VALUE_TRUE;
            bufwr.write( '\t' + SamiamUIInt.KEY_EXTRADEFINITION_SETASDEFAULT + " = \"" + SamiamUIInt.STR_VALUE_TRUE + "\";\n" );
            bufwr.write( '\t' + SamiamUIInt.KEY_EXTRADEFINITION_DEFAULT_STATE + " = \"" + strValue + "\";\n" );
        }
    }

    public void addNode(SimpleBNNode node,int xoffset, int truthval){
        int[] pt = new int[2];
        System.out.println(node.xcoord + " " + xoffset);
        pt[0] = scalex*node.xcoord + xoffset;
        pt[1] = scaley*(node.level+2);
        addNode(node,pt,truthval);
    }

     /* Add a new BoolNode to the network.
      * Place node at coordinates coords
      * (if concrete BNS supports such
      * placement)
      */
    public void addNode(SimpleBNNode node,int[] coords, int truthval){
        ListIterator li;
        String internalname = makeNetName(node.name);
        double trueval,falseval;
        try{
            bufwr.write("node " + internalname + '\n');
            bufwr.write("{" + '\n');
	    if (node instanceof SimpleBNGroundAtomNode)
		bufwr.write('\t' + "label = \"" + node.name + "\";" + '\n');
	    else
		bufwr.write('\t' + "label = \"" + "aux" + "\";" + '\n');
            bufwr.write('\t' + "position = ("+coords[0] +" " + coords[1] +");"+ '\n');
            bufwr.write('\t' + "states = (\"false\" \"true\");" + '\n');
            bufwr.write('\t' + "subtype = boolean;" + '\n');
            writeTruthValue( truthval );
            bufwr.write("}" + '\n' + '\n');

            bufwr.write("potential (" + internalname );
            if (node.parents.size()>0){
                bufwr.write(" | ");
                li = node.parents.listIterator();
                while (li.hasNext()){
                    bufwr.write(makeNetName(((BNNode)li.next()).name) + " ");
                }
            }
            bufwr.write(")" +'\n' );
            // The order of the cptentries in the net format is as follows
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
            bufwr.write("{" + '\n');
            bufwr.write('\t' + "data = (");
            for (int i=0; i<node.cptentries.length;i++){
                trueval = node.cptentries[i] ;
                falseval = 1-trueval;
                bufwr.write(falseval + " " + trueval);
                if (i<node.cptentries.length-1) {
                    bufwr.write('\n');
                    bufwr.write('\t');
                }
            }
            bufwr.write(" );" + '\n');
            bufwr.write("}" + '\n' + '\n');
        }
        catch (IOException e) {System.out.println(e);}
    }

    public void open(){
        try{
            bufwr.write("}");
            bufwr.close();
        }
        catch (IOException e) {System.out.println(e);}
    }

}
