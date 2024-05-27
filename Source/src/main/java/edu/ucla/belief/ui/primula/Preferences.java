package edu.ucla.belief.ui.primula;

import java.io.*;
import java.util.*;

/**
	Based on edu.ucla.belief.ui.preference.SamiamPreferences.

	@author keith cascio
	@since 20020712
*/
public class Preferences
{
	public static final String STR_PREFERENCES_FILE_NAME = "primularc.xml";
	public static final String STR_DEFAULT_PATH          = ".";

	/** @since 20060602 */
	public enum Key{
		DefaultPath,UserSamiamLocation,UserInflibLocation,UserAceLocation,InflibForAceLocation,C2DLocation,PrimulaPreferences;

		public void open( Appendable appendable ) throws IOException {
			appendable.append( myOpen );
		}
		public void close( Appendable appendable ) throws IOException {
			appendable.append( myClose );
		}
		public void write( CharSequence data, Appendable appendable ) throws IOException {
			open(  appendable );
			appendable.append( data );
			close( appendable );
		}

		private String myOpen  = "<"  + name() + ">";
		private String myClose = "</" + name() + ">";
	}

	public static boolean FLAG_DEBUG_PREFERENCES = false;
	private static boolean flagAutoPreferenceFile = false;


	public Preferences( boolean doFileIO )
	{
		this( decideAutoFile( doFileIO ) );
	}

	public Preferences( File fileInput )
	{
		setFile( Key.DefaultPath, new File( STR_DEFAULT_PATH ) );
		if( fileInput != null ) myFlagFileIOSuccess = readOptionsFromFile( decideAutoFile( fileInput ) );
	}

	/** @since 20060602 */
	public File getFile( Key key ){
		return myFiles.get( key );
	}

	/** @since 20060602 */
	public File setFile( Key key, File file ){
		return myFiles.put( key, file );
	}

	public static File decideAutoFile( boolean doFileIO ){
		if( doFileIO ){
			flagAutoPreferenceFile = true;
			return new File( STR_PREFERENCES_FILE_NAME );
		}
		else return null;
	}

	public static File decideAutoFile( File fileInput ){
		if( !flagAutoPreferenceFile && (fileInput == null || (!fileInput.exists())) ){
			System.err.println( "Warning: Primula preferences file " + fileInput.getPath() + " does not exist." );
			flagAutoPreferenceFile = true;
			return new File( STR_PREFERENCES_FILE_NAME );
		}
		else return fileInput;
	}

	public boolean wasFileIOSuccessful()
	{
		return myFlagFileIOSuccess;
	}

	public String toStringXML()
	{
		StringBuilder buff = new StringBuilder( 256 );
		try{
			append( buff );
		}catch( Throwable throwable ){
			System.err.println( "warning: Preferences.toStringXML() caught " + throwable );
		}
		return buff.toString();
	}

	/** @since 20060602 */
	public Appendable append( Appendable buff ) throws IOException
	{
		Key.PrimulaPreferences.open( buff ); buff.append( '\n' );
		File file = null;
		for( Key key : myFiles.keySet() ){
			file = myFiles.get( key );
			if( (file != null) && file.exists() ){
				buff.append( "  " );
				key.write(   file.getCanonicalFile().getAbsolutePath(), buff );
				buff.append( '\n' );
			}
		}
		if( myACESettings != null ) myACESettings.append( buff );
		Key.PrimulaPreferences.close( buff ); buff.append( '\n' );
		return buff;
	}

	private boolean readOptionsFromFile( File fileInput )
	{
		if( fileInput == null || (!fileInput.exists()) ) return false;
		else myFilePreferences = fileInput;

		boolean useSAX = true;
		try{
			Class.forName( "javax.xml.parsers.SAXParser" );
		}catch( ClassNotFoundException e ){
			useSAX = false;
		}

		if( useSAX ) return readOptionsFromFileSAX( fileInput );
		else return false;
	}

	private boolean readOptionsFromFileSAX( File fileInput )
	{
		if( FLAG_DEBUG_PREFERENCES ) System.out.println( "Preferences.readOptionsFromFileSAX()" );

		PreferencesHandler ph = new PreferencesHandler();
		ph.setPreferences( this );

		try{
			ph.parse( fileInput );
		}catch( IOException e ){
			if( FLAG_DEBUG_PREFERENCES )
			{
				System.err.println( "WARNING: Primula preferences file read error, using defaults.");
				System.err.println( e );
			}
			return false;
		}

		return true;
	}

	public void saveOptionsToFile()
	{
		if( FLAG_DEBUG_PREFERENCES ) System.out.println( "Preferences.saveOptionsToFile()" );
		try {
			if( myFilePreferences == null ) myFilePreferences = decideAutoFile( true );
			BufferedWriter out = new BufferedWriter(new FileWriter( myFilePreferences ));
			append( out );//out.write( toStringXML() );
			out.flush();
			out.close();
		}
		catch( Exception e) {
			System.err.println("WARNING: Primula preferences could not be written to the file.");
		}
	}

	/** @since 20060602 */
	public void forgetAll(){
		myFiles.clear();
	}

    /** @author keith cascio
		@since 20060727 */
    public edu.ucla.belief.ace.Settings getACESettings(){
    	if( myACESettings == null ) myACESettings = new edu.ucla.belief.ace.Settings();
    	return myACESettings;
    }

	private   Map<Key,File> myFiles             = new EnumMap<Key,File>( Key.class );//(5);
	protected boolean       myFlagFileIOSuccess = false;
	private   File          myFilePreferences;
	private   edu.ucla.belief.ace.Settings myACESettings;
}
