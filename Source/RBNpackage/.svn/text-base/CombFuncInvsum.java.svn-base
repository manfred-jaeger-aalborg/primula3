/*
 * CombFuncMean.java
* 
* Copyright (C) 2004 Aalborg University
*                    
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

public class CombFuncInvsum extends CombFunc{

    /** Creates new CombFuncInvsum */
    public CombFuncInvsum() {
        name = "invsum";
    }
    
    public double evaluate(double[] args)
    {
        /* Returns 1 for empty argument and arguments 
	 * consisting only of zeros! 
	 */
        
        double sum = 0;
        for (int i=0; i<args.length; i++)
	    sum = sum + args[i];
	return 1/Math.max(1,sum);
     }
     
    public  int evaluatesTo(int[] args){
	if (args.length == 0 ) return 1;
	if (args.length ==1 && args[0]==1) return 1;
	else{
	    boolean allzeros = true;
	    for (int i=0;i<args.length;i++){
		if (args[i]!=0) allzeros = false;
	    }
	    if (allzeros) return 1;
	    return -1;
	}
    }

}
