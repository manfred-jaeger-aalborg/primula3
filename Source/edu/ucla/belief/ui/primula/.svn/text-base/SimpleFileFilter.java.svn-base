package edu.ucla.belief.ui.primula;

import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
	Based on InflibFileFilter
	@author Keith Cascio
	@since 121302
*/
public class SimpleFileFilter extends FileFilter
{
	public SimpleFileFilter( String[] extensions, String description )
	{
		myExtensions = extensions;
		myDescription = description;
	}

	public boolean accept( java.io.File file )
	{
		if( file == null ) return false;
		else if( file.isDirectory() ) return true;
		else
		{
			String strFileName = file.getName();
			if( myFlagNotCaseSensitive ) strFileName = strFileName.toLowerCase();

			for( int i=0; i<myExtensions.length; i++ ){
				if( strFileName.endsWith( myExtensions[i] ) ) return true;
			}
		}
		return false;
	}

	public boolean getCaseSensitive(){
		return !myFlagNotCaseSensitive;
	}

	public void setCaseSensitive( boolean flag ){
		myFlagNotCaseSensitive = !flag;
	}

	public File validateExtension( File selectedFile ){
		String path = selectedFile.getPath();
		if( !path.endsWith(myExtensions[0]) && path.length() > 0 ) return new File( path + myExtensions[0] );
		else return selectedFile;
	}

	public String getDescription(){
		return myDescription;
	}

	public String[] getExtensions(){
		return myExtensions;
	}

	/**
		Taken from NetworkIO
		@since 050404
	*/
	public static String extractFileNameFromPath( String path )
	{
		int index = path.lastIndexOf( File.separatorChar );
		++index;
		if( index < path.length() ) return path.substring( index );
		else return "";
	}

	private boolean myFlagNotCaseSensitive;
	private String[] myExtensions;
	private String myDescription;
}
