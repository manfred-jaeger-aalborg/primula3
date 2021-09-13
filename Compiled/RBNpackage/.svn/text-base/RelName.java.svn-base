/*
 * RelName.java
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

import RBNutilities.*;



public class RelName{

    /**
	 * @uml.property  name="name"
	 */
    public String name;

    /** Creates new RelName */
    public RelName() {
        name = "";
    }

    public RelName(String str) {
        if (legalRelName(str)) name = str;
        else throw new IllegalArgumentException("Illegal relation name: " + str);
    }

    private static boolean isLegal(char c){
        boolean result = false;
        if (Character.isLetterOrDigit(c)) result = true;
        if (c == '_' || c == '-' || c=='@') result = true;
        //if (c == '.') result = true;
        return result;
    }

    public static boolean legalRelName(String str){
        boolean result = true;
        for (int i =0 ; i<str.length(); i++)
            if (!isLegal(str.charAt(i))) result = false;
        if (str.equals("sformula")) result =false;
        if (str.equals("function")) result =false;
        if (str.equals("mean")) result =false;
        if (str.equals("n-or")) result =false;
        if (str.equals("invsum")) result =false;
        if (str.equals("esum")) result =false;
        if (str.equals("l-reg")) result =false;
        if (str.equals("sum")) result =false;
        if (str.equals("Integer")) result =false;
        if (str.equals("Domain")) result =false;
        return result;
    }

    public boolean equals( RelName rn ){
        return this.name.equals( rn.name );
    }
}
