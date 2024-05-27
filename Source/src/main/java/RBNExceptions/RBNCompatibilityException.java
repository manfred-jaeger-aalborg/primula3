/*
 * RBNCompatibilityException.java
* authors: Manfred Jaeger
* Copyright (C) 2005 Aalborg University
*
* contact:
* jaeger@cs.aau.dk    www.cs.aau.dk/~jaeger/Primula.html
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

package RBNExceptions;


public class RBNCompatibilityException extends java.lang.Exception {

    /**
 * Creates new <code>RBNCompatibilityException</code> without detail message.
     */
    public RBNCompatibilityException() {
    }


    /**
 * Constructs an <code>RBNCompatibilityException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public RBNCompatibilityException(String msg) {
        super(msg);
    }
}


