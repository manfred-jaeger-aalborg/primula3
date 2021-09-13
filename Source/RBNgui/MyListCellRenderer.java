/*
* MyListCellRenderer.java 
* 
* Copyright (C) 2005 Aalborg University
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

import java.awt.*;
import javax.swing.*;

import RBNpackage.*;

public class MyListCellRenderer extends DefaultListCellRenderer{


  public Component getListCellRendererComponent(
      JList list,
      Object value,         // value to display
      int index,            // cell index
      boolean isSelected,   // is the cell selected
      boolean cellHasFocus) // the list and the cell have the focus
  {
   Component c = super.getListCellRendererComponent(
                 list, value, index, isSelected, cellHasFocus);

   if (value instanceof Rel){
    Rel rel = (Rel)value;
    c.setForeground(rel.color);
   }

   return c;
  }
}
