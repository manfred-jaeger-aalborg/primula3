/*
 * FileIO.java 
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

package RBNio;

import java.io.*;


public class FileIO extends java.lang.Object {

	/** Creates new OpenInputFile */
	public FileIO() {
	}


	public static BufferedWriter openOutputFile(String filename){
		BufferedWriter stream = null;

		try{
			stream = new BufferedWriter(new FileWriter(filename));

		}
		catch (FileNotFoundException e){System.out.println(e);}
		catch (StreamCorruptedException  e){System.out.println(e);}
		catch (IOException e){System.out.println(e);}

		return stream;
	}

	public static BufferedReader openInputFile(String filename){
		BufferedReader stream = null;

		try{
			stream = new BufferedReader(new FileReader(filename));

		}
		catch (FileNotFoundException e){System.out.println(e);}

		return stream;
	}
}
