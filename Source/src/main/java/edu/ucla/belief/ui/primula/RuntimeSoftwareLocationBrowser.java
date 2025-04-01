package edu.ucla.belief.ui.primula;

//import edu.ucla.belief.ui.util.Util;

//import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.net.*;

/**
	Helps a user browse for software components not found in the runtime classpath.
	@author keith cascio
	@since 20040420
*/
public class RuntimeSoftwareLocationBrowser
{
	public RuntimeSoftwareLocationBrowser( SoftwareEntity[] packages, Component parent ){
		this.myPackages = packages;
		this.myParent = parent;
	}

	/** @since 20060602 */
	public boolean isResolved(){
		boolean ret = true;
		for( SoftwareEntity softwareentity : myPackages ){
			ret &= softwareentity.isResolved();
		}
		return ret;
	}

	public Object getInstance( SoftwareEntity softwarepackage ) throws UnsatisfiedLinkError
	{
		if( !contains( softwarepackage ) ) return null;

		Object myInstance = null;
		if( myInstance == null ){
			String strErrorMessage = null;
			try{
				Class classFound = findMainClass( softwarepackage );
				if( classFound != null ){
					myInstance = classFound.newInstance();
				}
			//}catch( ClassNotFoundException classnotfoundexception ){
			//	strErrorMessage = "class \""+softwarepackage.mainclassnamefull+"\" does not exist or not found.\n("+classnotfoundexception.getMessage()+")";
			}catch( Exception exception ){
				strErrorMessage = exception.getMessage();
			}catch( Error error ){
				strErrorMessage = error.getMessage();
			}finally{
				if( strErrorMessage != null ){
					System.err.println( "RuntimeSoftwareLocationBrowser.getInstance() failed because: " + strErrorMessage );
					throw new UnsatisfiedLinkError( strErrorMessage );
				}
			}
		}

		return myInstance;
	}

	public Class findMainClass( SoftwareEntity entity )
	{
		if( !contains( entity ) ) return null;

		Class classFound = loadClassWithMyLoader( entity.mainclassnamefull );
		if( classFound != null ) return classFound;

		if( myFlagNotCalledWithoutBrowsing ){
			myFlagLocatedWithoutBrowsing = locateEntitiesWithoutBrowsing();
			myFlagNotCalledWithoutBrowsing = false;
		}

		classFound = loadClassWithMyLoader( entity.mainclassnamefull );
		if( classFound != null ) return classFound;

		int ret = showConfirmDialog( /*UI.STR_SAMIAM_ACRONYM+" could*/"Could not find "+entity.displayname+".\nWould you like to browse for it?", entity.displayname+" not found" );
		if( ret == JOptionPane.YES_OPTION )
		{
			if( myFlagNotCalledByBrowsing ){
				myFlagNotCalledByBrowsing = false;
				myFlagLocatedByBrowsing = locateEntitiesByBrowsing();
				if( myFlagLocatedByBrowsing ){
					classFound = loadClassWithMyLoader( entity.mainclassnamefull );
					if( classFound != null ) return classFound;
				}
			}

			classFound = loadClassByBrowsing( entity );
			if( classFound != null ) return classFound;
		}

		return null;
	}

	/** @since 20060602 */
	public boolean locateEntities(){
		if( myFlagNotCalledWithoutBrowsing ){
			myFlagLocatedWithoutBrowsing = locateEntitiesWithoutBrowsing();
			myFlagNotCalledWithoutBrowsing = false;

			if( myFlagLocatedWithoutBrowsing ) return true;
		}

		int ret = showConfirmDialog( "Could not find "+myPackages[0].displayname+" or one of its dependencies.\nWould you like to browse for it?", myPackages[0].displayname+", or its dependency, not found" );
		if( ret == JOptionPane.YES_OPTION ){
			myFlagNotCalledByBrowsing = false;
			return myFlagLocatedByBrowsing = locateEntitiesByBrowsing();
		}
		else return false;
	}

	public boolean locateEntitiesByBrowsing()
	{
		boolean ret = true;
		SoftwareEntity last = null;
		for( int i=0; i<myPackages.length; i++ ){
			if( last != null ) myPackages[i].seed( last );
			if( myPackages[i].isJava() ){
				ret  &= ( loadClassByBrowsing(   last = myPackages[i] ) != null );
			}
			else ret &= ( resolveTargetedEntity( last = myPackages[i] ) != null );
		}
		return ret;
	}

	/** @since 20060602 */
	public File resolveTargetedEntity( SoftwareEntity entity ){
		if( entity.isJava() ) throw new RuntimeException();

		try{
			return entity.browseForMe( myParent );
		}catch( Exception e ){
			showMessageDialog( e.getMessage(), "Error browsing for " + entity.displayname );
			return null;
		}
	}

	public Class loadClassByBrowsing( SoftwareEntity entity )
	{
		Class classFound = null;
		File locationMyClass = null;
		while( true ){
			try{
				locationMyClass = entity.browseForMyClass( myParent );
				if( locationMyClass == null ) break;
			}catch( Exception e ){
				//if( edu.ucla.belief.ui.UI.DEBUG_VERBOSE ){
				//	Util.STREAM_VERBOSE.println( edu.ucla.belief.ui.UI.STR_VERBOSE_TRACE_MESSAGE );
				//	e.printStackTrace();
				//}
				showMessageDialog( e.getMessage(), "Error browsing for " + entity.displayname );
				continue;
			}
			classFound = loadClassAtFile( entity.mainclassnamefull, locationMyClass );
			if( classFound != null ){
				entity.setCodeLocation( locationMyClass );
				return classFound;
			}
			else{
				entity.setCodeLocation( null );
				showMessageDialog( locationMyClass.getPath() + " does not contain class " + entity.mainclassnamefull + ".", entity.displayname+" not found" );
				continue;
			}
		}

		return null;
	}

	public boolean locateEntitiesWithoutBrowsing()
	{
		boolean ret           = true;
		SoftwareEntity entity = null;
		for( int i=0; i<myPackages.length; i++ ){
			entity = myPackages[i];
			if( entity.isJava() ){
				if( loadClassAtFile( entity.mainclassnamefull, entity.getCodeLocation() ) == null ){
					entity.setCodeLocation( (File)null );
					ret = false;
				}
			}
			else if( !entity.isResolved() ) ret = false;
		}
		return ret;
	}

	public Class loadClassWithMyLoader( String strclassname )
	{
		try{
			return myClassLoader.loadClass( strclassname );
		}catch( ClassNotFoundException e ){
			return null;
		}
	}

	public Class loadClassAtPath( String strclassname, String strCodeLocationPath )
	{
		if( strCodeLocationPath == null ) return null;
		else return loadClassAtFile( strclassname, new File( strCodeLocationPath ) );
	}

	public Class loadClassAtFile( String strclassname, File fileCodeLocation )
	{
		if( fileCodeLocation == null || !fileCodeLocation.exists() ) return null;
		else{
			URL urlCodeLocation = null;
			try{
				urlCodeLocation = fileCodeLocation.toURL();
			}catch( MalformedURLException e ){
				return null;
			}
			return loadClassAtURL( strclassname, urlCodeLocation );
		}
	}

	public Class loadClassAtURL( String strclassname, URL urlCodeLocation )
	{
		if( urlCodeLocation == null ) return null;

		if( myURLClassLoader == null ) myClassLoader = myURLClassLoader = new HackedLoader( urlCodeLocation );
		else myURLClassLoader.addURL( urlCodeLocation );

		return loadClassWithMyLoader( strclassname );
	}

	private class HackedLoader extends URLClassLoader{
		public HackedLoader( URL url ){
			super( new URL[] { url } );
		}

		public void addURL( URL url ){
			super.addURL( url );
		}
	}

	public boolean contains( SoftwareEntity softwarepackage )
	{
		if( softwarepackage == null ) return false;
		for( int i=0; i<myPackages.length; i++ ){
			if( myPackages[i] == softwarepackage ) return true;
		}
		return false;
	}

	private void showMessageDialog( String msg, String title ){
		JOptionPane.showMessageDialog( myParent, msg, title, JOptionPane.WARNING_MESSAGE );
	}

	private int showConfirmDialog( String msg, String title ){
		return JOptionPane.showConfirmDialog( myParent, msg, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
	}

	/** @since 20060602 */
	public void forgetAll(){
		myFlagNotCalledWithoutBrowsing = true;
		myFlagLocatedWithoutBrowsing   = false;
		myFlagNotCalledByBrowsing      = true;
		myFlagLocatedByBrowsing        = false;
	}

	private Component        myParent;
	private ClassLoader      myClassLoader         = ClassLoader.getSystemClassLoader();
	private HackedLoader     myURLClassLoader      = null;
	private SoftwareEntity[] myPackages;
	private boolean myFlagNotCalledWithoutBrowsing = true;
	private boolean myFlagLocatedWithoutBrowsing   = false;
	private boolean myFlagNotCalledByBrowsing      = true;
	private boolean myFlagLocatedByBrowsing        = false;
}
