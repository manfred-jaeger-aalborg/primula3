/*
* Filter_rdef.java 
* 
* Copyright (C) 2009 Aalborg University
*
* contact:
* jaeger@cs.aau.dk   http://www.cs.aau.dk/~jaeger/Primula.html
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
package RBNgui;

import java.io.File;
import javax.swing.filechooser.*;


public class Filter_rdef extends FileFilter{
  
  public final static String rdef = "rdef";
  
    // Accept all directories and all rdef files.
    public  boolean accept(File f) {
      if (f.isDirectory()) {
        return true;
      }

      String filename = f.getName();
      int i = filename.lastIndexOf('.');

      if (i>0 && i<filename.length()-1){
        String extension = filename.substring(i+1).toLowerCase();
        if (extension.equals(rdef))
          return true;
        else
          return false;
      }

      return false;
  }
      
    // The description of this filter
    public String getDescription() {
        return "RDEF Files (*.rdef)";
    }
 
      
}
