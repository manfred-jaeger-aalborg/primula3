package edu.ucla.belief.ui.primula;

import RBNgui.Primula;

import java.io.*;
import java.awt.*;

/**
	Based on PrimulaManager
	@author Keith Cascio
	@since 040804
*/
public class SamiamManager
{
	public static final String STR_DISPLAY_NAME_SAMIAM = "SamIam";
	public static final String STR_PACKAGENAME_SAMIAM = "edu.ucla.belief.ui";
	public static final String STR_CLASSNAME_SAMIAM = "UI";
	public static final String STR_CLASSNAME_FULL_SAMIAM = STR_PACKAGENAME_SAMIAM+"."+STR_CLASSNAME_SAMIAM;
	public static final String STR_FILENAME_SAMIAM = STR_CLASSNAME_SAMIAM+".class";
	public static final String STR_FILENAME_JAR_DEFAULT_SAMIAM = "samiam.jar";

	public SamiamManager( Primula primula )
	{
		myPrimula = primula;

		myPackageInflib = new SoftwareEntity( "Inflib", "edu.ucla.belief", "BeliefNetwork", "inflib.jar" );
		myPackageInflib.setCodeLocation( myPrimula.getPreferences().getFile( Preferences.Key.UserInflibLocation ) );
		myPackageSamiam = new SoftwareEntity( STR_DISPLAY_NAME_SAMIAM, STR_PACKAGENAME_SAMIAM, STR_CLASSNAME_SAMIAM, STR_FILENAME_JAR_DEFAULT_SAMIAM );
		myPackageSamiam.setCodeLocation( myPrimula.getPreferences().getFile( Preferences.Key.UserSamiamLocation ) );

		myRuntimeSoftwareLocationBrowser = new RuntimeSoftwareLocationBrowser( new SoftwareEntity[]{ myPackageInflib, myPackageSamiam }, myPrimula );
	}

	public void openSamiam() throws UnsatisfiedLinkError
	{
		SamiamUIInt ui = getSamIamUIInstance();
		if( ui != null ){
			centerWindow( ui.asJFrame() );
			ui.asJFrame().setVisible( true );
		}
	}

	/**
		From Primula.java
		@author Keith Cascio
		@since 040504
	*/
	/*
	public edu.ucla.belief.ui.primula.SamiamUIInt getSamIamUIInstance()
	{
		if( mySamiamUIInt == null ){
			String strErrorMessage = null;
			try{
				Class classUI = Class.forName( "edu.ucla.belief.ui.UI" );
				if( classUI != null ){
					Object ui = classUI.newInstance();
					if( ui instanceof edu.ucla.belief.ui.primula.SamiamUIInt ){
						mySamiamUIInt = (edu.ucla.belief.ui.primula.SamiamUIInt) ui;
						mySamiamUIInt.setSystemExitEnabled( false );
						mySamiamUIInt.setInvokerName( "Primula" );
						mySamiamUIInt.setPrimulaUIInstance( myPrimula );
					}else{
						strErrorMessage = "SamIam class \""+STR_CLASSNAME_FULL_SAMIAM+"\" does not implement interface \"edu.ucla.belief.ui.primula.SamiamUIInt\".";
						return null;
					}
				}
			}catch( ClassNotFoundException classnotfoundexception ){
				strErrorMessage = "SamIam class \""+STR_CLASSNAME_FULL_SAMIAM+"\" does not exist.";
			}catch( ClassCastException classcastexception ){
				strErrorMessage = "SamIam class \""+STR_CLASSNAME_FULL_SAMIAM+"\" does not implement interface \"edu.ucla.belief.ui.primula.SamiamUIInt\".";
			}catch( Exception exception ){
				strErrorMessage = exception.getMessage();
			}catch( Error error ){
				strErrorMessage = error.getMessage();
			}finally{
				if( strErrorMessage != null ){
					System.err.println( "Primula.getSamIamUIInstance() failed because: " + strErrorMessage );
					return null;
				}
			}
		}

		if( mySamiamUIInt != null ) centerWindow( mySamiamUIInt.asJFrame() );
		return mySamiamUIInt;
	}*/

	/**
		Based on PrimulaManager.getPrimulaUIInstance()
		@author Keith Cascio
		@since 050304
	*/
	public edu.ucla.belief.ui.primula.SamiamUIInt getSamIamUIInstance() throws UnsatisfiedLinkError
	{
		if( mySamiamUIInt == null ){
			String strErrorMessage = null;
			try{
				Object ui = myRuntimeSoftwareLocationBrowser.getInstance( myPackageSamiam );
				if( ui instanceof SamiamUIInt ){
					mySamiamUIInt = (edu.ucla.belief.ui.primula.SamiamUIInt) ui;
					mySamiamUIInt.setSystemExitEnabled( false );
					mySamiamUIInt.setInvokerName( "Primula" );
					mySamiamUIInt.setPrimulaUIInstance( myPrimula );
				}
				else{
					if( ui != null ){
						strErrorMessage = STR_DISPLAY_NAME_SAMIAM+" class \""+STR_CLASSNAME_FULL_SAMIAM+"\" does not implement interface \"edu.ucla.belief.ui.primula.SamiamUIInt\".";
					}
					return null;
				}
			}catch( Exception exception ){
				strErrorMessage = exception.getMessage();
			}catch( Error error ){
				strErrorMessage = error.getMessage();
			}finally{
				if( strErrorMessage == null ){
					Preferences prefs = myPrimula.getPreferences();
					File filePackageCodeLocation = myPackageSamiam.getCodeLocation();
					if( prefs.getFile( Preferences.Key.UserSamiamLocation ) != filePackageCodeLocation ) prefs.setFile( Preferences.Key.UserSamiamLocation, filePackageCodeLocation );
					filePackageCodeLocation = myPackageInflib.getCodeLocation();
					if( prefs.getFile( Preferences.Key.UserInflibLocation ) != filePackageCodeLocation ) prefs.setFile( Preferences.Key.UserInflibLocation, filePackageCodeLocation );
				}
				else{
					System.err.println( "SamiamManager.getSamIamUIInstance() failed because: " + strErrorMessage );
					throw new UnsatisfiedLinkError( strErrorMessage );
				}
			}
		}

		if( mySamiamUIInt != null ) centerWindow( mySamiamUIInt.asJFrame() );
		return mySamiamUIInt;
	}

	public void setSamiamUIInstance( SamiamUIInt ui ){
		mySamiamUIInt = ui;
	}

	/** @since 20060602 */
	public void forgetAll(){
		if( myPackageSamiam != null ) myPackageSamiam.setCodeLocation( null );
		if( myPackageInflib != null ) myPackageInflib.setCodeLocation( null );
		if( myRuntimeSoftwareLocationBrowser != null ) myRuntimeSoftwareLocationBrowser.forgetAll();
	}

	/**
		@author Keith Cascio
		@since 042202
		A utility method.

		Center a Window on the screen.
	*/
	public static void centerWindow( Window w, Rectangle wrt )
	{
		Dimension windowSize = w.getSize();

		int screenWidth = wrt.width;
		int screenHeight = wrt.height;

		Point upperleft = wrt.getLocation();

		int xCoordinate = (screenWidth - windowSize.width)/2 + upperleft.x;
		int yCoordinate = (screenHeight - windowSize.height)/2 + upperleft.y;

		Point newCoordinate = new Point( xCoordinate, yCoordinate );

		w.setLocation( newCoordinate );
	}

	/**
		@author Keith Cascio
		@since 021403
	*/
	public static void centerWindow( Window w )
	{
		centerWindow( w, getScreenBounds() );
	}

	/**
		@author Keith Cascio
		@since 042202
		A utility method.

		Returns the bounds of the GraphicsConfiguration in the device coordinates.
	*/
	public static Rectangle getScreenBounds()
	{
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
	}

	private SamiamUIInt mySamiamUIInt;
	private Primula myPrimula;
	private SoftwareEntity myPackageSamiam;
	private SoftwareEntity myPackageInflib;
	private RuntimeSoftwareLocationBrowser myRuntimeSoftwareLocationBrowser;
}
