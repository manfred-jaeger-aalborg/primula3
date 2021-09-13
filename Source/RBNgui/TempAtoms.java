/*
 * TempAtoms.java
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

import RBNpackage.*;
import java.util.*;

class TempAtoms{
	
	/**
	 * @uml.property  name="rel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private Rel rel;
	/**
	 * @uml.property  name="args"
	 */
	private int [] args;
	
	public TempAtoms(Rel rel, int[] args){
		this.rel = rel;
		this.args = args;
	}
	
	/**
	 * @return
	 * @uml.property  name="rel"
	 */
	public Rel getRel(){
		return rel;
	}

	/**
	 * @return
	 * @uml.property  name="args"
	 */
	public int[] getArgs(){
		return args;
	}
}
