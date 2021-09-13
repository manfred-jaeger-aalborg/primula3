/*
* BayesNetIntSamIam.java
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

import javax.swing.JOptionPane;
import javax.swing.Icon;
import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

import RBNio.*;
import RBNgui.*;

import edu.ucla.belief.ui.primula.*;

/**
	@author Keith Cascio
	@since 040804
*/
public class BayesNetIntSamIam extends BayesNetIntHuginNet implements BayesNetInt
{
	public static final String STR_FILE_EXTENSION_NET = ".net";
	public static final String STR_ID_DEFAULT = "untitled_primula_generated_model";
	public static final String STR_REPLACE_OPTION = "Replace";
	public static final String STR_RENAME_OPTION = "Auto Rename";
	public static final Object[] ARRAY_OPTIONS = new Object[] { STR_REPLACE_OPTION, STR_RENAME_OPTION };
	public static final String STR_PATTERN_DECIMAL_FORMAT = "000";

	//protected BufferedWriter bufwr;
	private StringWriter myStringWriter;
	private String myNetworkName;
	private Primula myPrimula;
	private static int scale = 50;
	private static DecimalFormat DECIMAL_FORMAT;

	/** Creates new BayesNetIntSamIam */
	public BayesNetIntSamIam( Primula primula, String netname )
	{
		super();

		myPrimula = primula;
		myNetworkName = netname;
		if( myNetworkName.length() < (int)1 ) myNetworkName = STR_ID_DEFAULT + STR_FILE_EXTENSION_NET;

		myStringWriter = new StringWriter();
		bufwr = new BufferedWriter( myStringWriter );//FileIO.openOutputFile(filename.getPath());
		//String netname = filename.getName();

		String id = createValidNetID( netname );
		//System.out.println( "Writing Hugin 6.1 net with id: \"" + id + "\"" );
		try{
			bufwr.write( "class " + id +  '\n' );
			bufwr.write( "{" + '\n' );
		}
		catch (IOException e) {System.out.println(e);}
	}

	public static String createValidNetID( String netname )
	{
		String id = netname;
		int index0 = id.lastIndexOf( File.separator );
		if( index0 < (int)0 ) index0 = (int)0;
		else ++index0;

		int index1 = id.length();
		if( id.endsWith( STR_FILE_EXTENSION_NET ) ) index1 -= STR_FILE_EXTENSION_NET.length();

		id = id.substring( index0, index1 );

		id = id.replaceAll( "\\s", "_" );

		if( id.length() < (int)1 ) return STR_ID_DEFAULT;
		else return id;
	}

	public void open()
	{
		try{
			bufwr.write("}");
			bufwr.close();

			StringReader reader = new StringReader( myStringWriter.toString() );

			String pathEffective = myNetworkName;

			SamiamUIInt ui = myPrimula.getSamIamUIInstanceThis();
			if( ui != null ){
				while( ui.pathConflicts( pathEffective ) ){
					int result = JOptionPane.showOptionDialog( myPrimula,
						"Model with name \"" + pathEffective + "\" already open in SamIam.  What would you like to do?",
						"File name conflict",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE,
						(Icon)null,
						ARRAY_OPTIONS,
						STR_RENAME_OPTION );
					Object objResult = ARRAY_OPTIONS[result];
					if( objResult == STR_REPLACE_OPTION ){
						ui.closeFilePath( pathEffective );
					}
					else if( objResult == STR_RENAME_OPTION ){
						while( ui.pathConflicts( pathEffective ) ) pathEffective = rename( pathEffective );
					}
				}
				ui.openHuginNet( reader, pathEffective );
				ui.asJFrame().setVisible( true );
			}
		}
		catch( IOException e ) { System.out.println(e); }
	}

	/**
		@author Keith Cascio
		@since 042604
	*/
	public static String rename( String pathEffective )
	{
		if( DECIMAL_FORMAT == null ) DECIMAL_FORMAT = new DecimalFormat( STR_PATTERN_DECIMAL_FORMAT );
		int index = pathEffective.lastIndexOf( '.' );
		String prefix = pathEffective.substring( 0, index );
		String ext = pathEffective.substring( index );
		return prefix + "_" + DECIMAL_FORMAT.format( INT_RENAME_COUNTER++ ) + ext;
	}

	private static int INT_RENAME_COUNTER = (int)1;
}
