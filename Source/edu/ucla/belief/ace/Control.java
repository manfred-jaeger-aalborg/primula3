package edu.ucla.belief.ace;

import edu.ucla.belief.ui.primula.*;
import edu.ucla.belief.ui.primula.Preferences.Key;
import edu.ucla.belief.ui.primula.PlatformUtilities.Platform;
import edu.ucla.belief.ace.AceInterfaceForPrimula.DtreeMethod;
import edu.ucla.belief.ace.PrimulaSystemSnapshot.RunWriteHuginNet;

import RBNinference.BayesConstructor;
import RBNgui.Primula;
import RBNgui.QueryTableModel;
import RBNpackage.OneStrucData;

import java.util.List;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.awt.*;
import javax.swing.*;
import java.text.*;
import java.util.regex.*;
import java.awt.event.*;
import javax.swing.Action;
import javax.swing.AbstractAction;
//import java.awt.datatransfer.*;//clipboard shite

/** @author keith cascio
	@since 20060511 */
public class Control implements Settings.ACESettingsListener
{
	public static final String STR_DISPLAY_NAME_ACE         = "Ace";
	public static final String STR_PACKAGENAME_ACE          = "edu.ucla.belief.ace";
	public static final String STR_CLASSNAME_ACE            = "AceInterfaceForPrimulaImpl";
	public static final String STR_FILENAME_JAR_DEFAULT_ACE = "releaseAc.jar";
	public static final String STR_CLASSNAME_FULL_ACE       = STR_PACKAGENAME_ACE+"."+STR_CLASSNAME_ACE;
	public static final String STR_FILENAME_ACE             = STR_CLASSNAME_ACE+".class";

    public static final String   STR_MSG_ACE_COMPILE     = Settings.STR_ACE_DISPLAY_NAME + ", compiling the model... ";
    public static final String   STR_MSG_ACE_LOAD        = Settings.STR_ACE_DISPLAY_NAME + ", loading the arithmetic circuit... ";
    public static final String   STR_MSG_ACE_COMPUTE     = Settings.STR_ACE_DISPLAY_NAME + ", computing the answer... ";
    public static final String   STR_MSG_ACE_FASTFORWARD = Settings.STR_ACE_DISPLAY_NAME + ", fast forward... ";
    public static final String[] ARRAY_MESSAGES_ACE      = new String[] { STR_MSG_ACE_COMPILE, STR_MSG_ACE_LOAD, STR_MSG_ACE_COMPUTE, STR_MSG_ACE_FASTFORWARD };

    /** If true, when the user adds a assertion to the evidence,
    	thus inducing a superset,
    	we keep the { bn + ace compilation } and use it for subsequent
    	queries.
    	If false, any evidence change forces us to discard
    	the current { bn + ace compilation }.
		As of 20061027, this implementation is "impatient" and
    	"destructive" in the sense that, once the user makes an
    	evidence change that discards the current compilation, subsequent
    	evidence changes cannot restore the current compilation, even
    	if there has been no intervening action.

    	@since 20061027
    	@see   primulaEvidenceChanged() */
    public static final boolean  FLAG_REUSE_COMPILATION  = false;

	public Control( Primula primula ){
		myPrimula = primula;
	}

	/** @since 20060601 */
	public SoftwareEntity getAcePackage(){
		if( myPackageAce == null ){
			myPackageAce = new SoftwareEntity( STR_DISPLAY_NAME_ACE, STR_PACKAGENAME_ACE, STR_CLASSNAME_ACE, STR_FILENAME_JAR_DEFAULT_ACE );
			myPackageAce.setCodeLocation( myPrimula.getPreferences().getFile( Key.UserAceLocation ) );
		}
		return myPackageAce;
	}

	/** @since 20060602 */
	public SoftwareEntity getInflibPackage(){
		if( myPackageInflib == null ){
			myPackageInflib = new SoftwareEntity( "Inflib for " + Settings.STR_ACE_DISPLAY_NAME, "edu.ucla.belief", "BeliefNetwork", "inflib.jar" );
			myPackageInflib.setCodeLocation( myPrimula.getPreferences().getFile( Key.InflibForAceLocation ) );
		}
		return myPackageInflib;
	}

	/** @since 20060602 */
	public SoftwareEntity getC2DPackage(){
		if( myPackageC2D == null ){
			Platform platform = PlatformUtilities.getInstance().getPlatform();
			String filename  = null;

			if(      platform.isWindows()       ) filename = "c2d_windows.exe";
			else if( platform == Platform.linux ) filename = "c2d_linux";
			else                                  filename = "c2d_" + platform.name() + platform.getExecutableExtension();

			myPackageC2D = new SoftwareEntity( "C2D Executable", platform.getExecutableExtension(), filename );
			myPackageC2D.setCodeLocation( myPrimula.getPreferences().getFile( Key.C2DLocation ) );
		}
		return myPackageC2D;
	}

	/** @since 20060601 */
	public RuntimeSoftwareLocationBrowser getBrowser(){
		if( myRuntimeSoftwareLocationBrowser == null ){
			myRuntimeSoftwareLocationBrowser = new RuntimeSoftwareLocationBrowser( new SoftwareEntity[]{ getAcePackage(), getInflibPackage(), getC2DPackage() }, myParentComponent );
		}
		return myRuntimeSoftwareLocationBrowser;
	}

	/** @since 20060608 */
	public edu.ucla.belief.ace.AceInterfaceForPrimula getFullyResolvedAceInstance( String title ) throws UnsatisfiedLinkError {
		AceInterfaceForPrimula ace = getAceInstance();
		if( (ace == null) || (!getBrowser().isResolved()) ){
			myProgressBar.setString( title );
			showDialog( "Location of " + Settings.STR_ACE_DISPLAY_NAME + " or one of its dependencies is unknown.", title, JOptionPane.INFORMATION_MESSAGE );
			return null;
		}
		return ace;
	}

	/** @since 20060601 */
	public edu.ucla.belief.ace.AceInterfaceForPrimula getAceInstance() throws UnsatisfiedLinkError
	{
		try{
			if( !getBrowser().isResolved() ) getBrowser().locateEntities();
			if( !getBrowser().isResolved() ) return null;
			else rememberLocations();
		}catch( Throwable throwable ){
			System.err.println( "warning: Control.getAceInstance() caught " + throwable );
			throwable.printStackTrace();
			return null;
		}

		if( myAceInt == null ){
			String strErrorMessage = null;
			try{
				Object ace = getBrowser().getInstance( getAcePackage() );
				if( ace instanceof edu.ucla.belief.ace.AceInterfaceForPrimula ){
					myAceInt = (edu.ucla.belief.ace.AceInterfaceForPrimula) ace;
					//myAceInt.setSystemExitEnabled( false );
					//myAceInt.setInvokerName( "Primula" );
					//myAceInt.setPrimulaUIInstance( myPrimula );
				}
				else{
					if( ace != null ){
						strErrorMessage = STR_DISPLAY_NAME_ACE+" class \""+STR_CLASSNAME_FULL_ACE+"\" does not implement interface \"edu.ucla.belief.ace.AceInterfaceForPrimula\".";
					}
					return null;
				}
			}catch( Throwable throwable ){
				strErrorMessage = throwable.getMessage();
			}finally{
				if( strErrorMessage == null ) rememberLocations();
				else{
					System.err.println( "Control.getAceInstance() failed because: " + strErrorMessage );
					throw new UnsatisfiedLinkError( strErrorMessage );
				}
			}
		}

		//if( myAceInt != null ) centerWindow( myAceInt.asJFrame() );
		return myAceInt;
	}

	/** @since 20060602 */
	private void rememberLocations(){
		Preferences prefs = myPrimula.getPreferences();
		File filePackageCodeLocation = getAcePackage().getCodeLocation();
		if( prefs.getFile( Key.UserAceLocation      ) != filePackageCodeLocation ) prefs.setFile( Key.UserAceLocation,      filePackageCodeLocation );
		filePackageCodeLocation      = getInflibPackage().getCodeLocation();
		if( prefs.getFile( Key.InflibForAceLocation ) != filePackageCodeLocation ) prefs.setFile( Key.InflibForAceLocation, filePackageCodeLocation );
		filePackageCodeLocation      = getC2DPackage().getCodeLocation();
		if( prefs.getFile( Key.C2DLocation          ) != filePackageCodeLocation ) prefs.setFile( Key.C2DLocation,          filePackageCodeLocation );
	}

	/** @since 20060602 */
	public void forgetAll(){
		if( myPackageAce    != null ) myPackageAce.setCodeLocation(   null );
		if( myPackageInflib != null ) myPackageInflib.setCodeLocation( null );
		if( myRuntimeSoftwareLocationBrowser != null ) myRuntimeSoftwareLocationBrowser.forgetAll();
	}

	public interface ACEControlListener{
		public void aceStateChange( Control control );
	}

	/*public void setPrimula( Primula primula ){
		myPrimula = primula;
	}*/

	/** @since 20060608 */
	public void setDataModel( QueryTableModel dataModel ){
		myDataModel = dataModel;
	}

	public void setState( PrimulaSystemSnapshot primulasystemstate ){
		myState = primulasystemstate;
	}

	/** @since 20060728 */
	public void setInfoMessage( JLabel label ){
		myInfoMessage = label;
	}

	/** @since 20060606 */
	private void clearInfoMessage(){
		if( myInfoMessage == null ) return;
		String text = myInfoMessage.getText();
		for( String msg : ARRAY_MESSAGES_ACE ){
			if( text == msg ){
				myInfoMessage.setText( " " );
				return;
			}
		}
	}

	public void setParentComponent( Component component ){
		myParentComponent = component;
	}

	/** @since 20060601 */
	private void showDialog( Object msg, String title, int messageType ){
		if( myParentComponent == null ) return;

		JOptionPane.showMessageDialog( myParentComponent, msg, title, messageType );
	}

	/** @since 20060622 */
	private int showConfirmDialog( Object msg, String title, int optionType, int messageType ){
		if( myParentComponent == null ) return Integer.MIN_VALUE;

		return JOptionPane.showConfirmDialog( myParentComponent, msg, title, optionType, messageType );
	}

	public void setProgressBar( JProgressBar jprogressbar ){
		this.myProgressBar = jprogressbar;
		if( jprogressbar != null ) this.setProgressModel( jprogressbar.getModel() );
	}

	public void setProgressModel( BoundedRangeModel boundedrangemodel ){
		myProgressModel = boundedrangemodel;
	}

	/** @since 20060606 */
	public Action getActionCancel(){
		if( myActionCancel == null ){
			myActionCancel = new AbstractAction( "Cancel" ){
				{
					putValue(   Action.SHORT_DESCRIPTION, "Cancel the ace task, if one is running" );
					setEnabled( false );
				}

				public void actionPerformed( ActionEvent actionevent ){
					Control.this.cancel();
				}
			};
			Control.this.resetACEEnabledState();
		}
		return myActionCancel;
	}

	public void set( Settings settings ){
		if( mySettings != settings ){
			if( mySettings != null ) mySettings.removeListener( (Settings.ACESettingsListener)this );
			(mySettings = settings).addListener( (Settings.ACESettingsListener)this );
		}
	}

	public void addListener( ACEControlListener acelistener ){
		if( myListeners == null ) myListeners = new LinkedList<ACEControlListener>();
		if( !myListeners.contains( acelistener ) ) myListeners.add( acelistener );
	}

	private void fireStateChanged(){
		clearInfoMessage();
		resetACEEnabledState();
		if( myListeners == null ) return;
		for( ACEControlListener acelistener : myListeners ) acelistener.aceStateChange( this );
	}

	/** @since 20060511 */
	private void resetACEEnabledState(){
		boolean enabled = false, enabledFastForward = false;
		enabledFastForward |= enabled = Control.this.isReadyCompile();
		if( myActionCompile     != null ) myActionCompile.setEnabled( enabled );
		enabledFastForward |= enabled = Control.this.isReadyLoad();
		if( myActionLoad        != null ) myActionLoad.setEnabled(    enabled );
		enabledFastForward |= enabled = Control.this.isReadyCompute();
		if( myActionCompute     != null ) myActionCompute.setEnabled( enabled );
		if( myActionFastForward != null ) myActionFastForward.setEnabled( enabledFastForward );
	}

	/** (a) cancel any work in progress, (b) if the compilation is stale, throw it away
		interface SettingsSampling.ACESettingsListener */
	public void aceCompileSettingChanged( Settings settings ){
		cancel();

		if( (myCompilation != null) && (myCompilation.effective.isCompileStale( settings )) ) clear();
	}

    /** 20061027: this implementation is "impatient" and
    	"destructive", i.e. once the user makes an
    	evidence change that discards the current compilation, subsequent
    	evidence changes cannot restore the current compilation, even
    	if there has been no intervening action.

    	@since 20060620
    	@see   Control#FLAG_REUSE_COMPILATION */
	public Control primulaEvidenceChanged(){
		cancel();
		if( (myCompilation != null) && (myCompilation.effective.isCompileWithEvidence()) ){
			if( FLAG_REUSE_COMPILATION && myCompilation.attemptEvidenceChange( myPrimula.getInstantiation() ) ){
				if( myDataModel != null ) myDataModel.resetACE();
			}
			else clear();
		}
		return Control.this;
	}

	/** @since 20060620 */
	public Control primulaQueryChanged(){
		cancel();
		if( (myCompilation != null) && (myCompilation.effective.isCompileWithEvidence()) ) clear();
		return Control.this;
	}

	/** discard any current compilation */
	public Control clear(){
		cancel();

		try{
			while( isActive() ) Thread.sleep( 0x100 );
		}catch( InterruptedException interruptedexception ){
			Thread.currentThread().interrupt();
			return Control.this;
		}

		if( myCompilation != null ) myCompilation.die();
		myCompilation      = null;

		myFlagReadyCompile = true;
		myFlagReadyLoad    = false;
		myFlagReadyCompute = false;

		fireStateChanged();

		return Control.this;
	}

	/** cancel any work in progress */
	public void cancel(){
		myThreadGroup.interrupt();

		Thread.yield();

		if( isActive() ){
			myThreadGroupCancel.interrupt();
			new Thread( myThreadGroupCancel, runCancel, "ace cancel " + Integer.toString( INT_COUNTER++ ) ).start();
		}
	}

	private Runnable runCancel = new Runnable(){
		public void run(){
			try{
				for( int i=0; (i<8) && Control.this.isActive(); i++ ){
					Thread.sleep( 512 );
				}

				if( Control.this.isActive() ){
					System.err.println( "interruption insufficient to cancel ace, forced to destroy thread(s)" );
					myThreadGroup.list();
					if( Thread.interrupted() ) return;
					Control.this.stop();
				}
			}catch( InterruptedException interruptedexception ){
				Thread.currentThread().interrupt();
				return;
			}
		}
	};

	public boolean isActive(){
		return myThreadGroup.activeCount() > 0;
	}

	public void stop(){
		myThreadGroup.stop();
	}

	/** @since 20060728 */
	private Thread startUserTask( Runnable task, String namePrefix, String message ){
		if( (myInfoMessage != null) && (message != null) ) myInfoMessage.setText( message );
		Thread ret = null;
		synchronized( mySynchUserTasks ){
			myThreadGroup.interrupt();
			while( isActive() ) Thread.yield();
			(ret = new Thread( myThreadGroup, task, namePrefix + Integer.toString( INT_COUNTER++ ) )).start();
		}
		return ret;
	}

	/** @since 20060728 */
	public Action getActionFastForward(){
		if( myActionFastForward == null ){
			myActionFastForward = new AbstractAction( "Compute" ){
				{
					putValue(   Action.SHORT_DESCRIPTION, Settings.STR_ACE_DISPLAY_NAME + ", fast forward: compile, load, compute" );
					setEnabled( false );
				}

				public void actionPerformed( ActionEvent actionevent ){
					Control.this.fastforward();
				}
			};
			Control.this.resetACEEnabledState();
		}
		return myActionFastForward;
	}

	/** @since 20060728 */
	public Thread fastforward(){
		return Control.this.startUserTask( runFastForward, "ace fast forward ", STR_MSG_ACE_FASTFORWARD );
	}

	/** @since 20060728 */
	private Runnable runFastForward = new Runnable(){
		public void run(){
			while( Control.this.nextSynchronous() ){
				if( Thread.interrupted() ) return;
				Thread.yield();
			}
		}
	};

	/** @return true if we have completed a step and anticipate another step, false if we failed to complete a step or we are finished with the last step
		@since 20060728 */
	public boolean nextSynchronous(){
		if(      isReadyCompute() ){ runCompute.run();
		     return false;
		}
		else if( isReadyLoad()    ){ runLoad.run();
		     return runLoad.success;
		}
		else if( isReadyCompile() ){ runCompile.run();
		     return runCompile.success;
		}
		else return false;
	}

	public boolean isReadyCompile(){
		boolean readycompile = ( (myPrimula.getRBN() != null) && (myPrimula.getRels() != null) );
		return myFlagReadyCompile && readycompile;

		/*
		if( (!myFlagReadyCompile) || (myState == null) ) return false;

		if( myPrimula.rbn                   == null ) return false;
		if( myPrimula.rels                  == null ) return false;
		if( myPrimula.inst                  == null ) return false;
		if( myPrimula.queryatoms            == null ) return false;
		if( myPrimula.evidencemode          == null ) return false;
		if( myPrimula.decomposemode         == null ) return false;
		if( myPrimula.isolatedzeronodesmode == null ) return false;
		if( myPrimula.layoutmode            == null ) return false;

		return true;*/
	}

	/** @since 20060728 */
	public Action getActionCompile(){
		if( myActionCompile == null ){
			myActionCompile = new AbstractAction( "Compile" ){
				{
					putValue(   Action.SHORT_DESCRIPTION, Settings.STR_ACE_DISPLAY_NAME + ", compile network" );
					setEnabled( false );
				}

				public void actionPerformed( ActionEvent actionevent ){
					Control.this.compile();
				}
			};
			Control.this.resetACEEnabledState();
		}
		return myActionCompile;
	}

	public Thread compile(){
		return Control.this.startUserTask( runCompile, "ace compile ", STR_MSG_ACE_COMPILE );

		//Control.this.fireStateChanged();//debug

		/* StringBuilder builder = new StringBuilder( 64 );
		builder.append( (char) 0x000a );
		builder.append( "/u/guest/keith" );
		copyToSystemClipboard( builder.toString() );*/
	}

	public boolean isReadyLoad(){
		return myFlagReadyLoad;
	}

	/** @since 20060728 */
	public Action getActionLoad(){
		if( myActionLoad == null ){
			myActionLoad = new AbstractAction( "Load" ){
				{
					putValue(   Action.SHORT_DESCRIPTION, Settings.STR_ACE_DISPLAY_NAME + ", load arithmetic circuit" );
					setEnabled( false );
				}

				public void actionPerformed( ActionEvent actionevent ){
					Control.this.load();
				}
			};
			Control.this.resetACEEnabledState();
		}
		return myActionLoad;
	}

	public Thread load(){
		return Control.this.startUserTask( runLoad, "ace load ", STR_MSG_ACE_LOAD );
	}

	public boolean isReadyCompute(){
		return myFlagReadyCompute;
	}

	/** @since 20060728 */
	public Action getActionCompute(){
		if( myActionCompute == null ){
			myActionCompute = new AbstractAction( "Compute" ){
				{
					putValue(   Action.SHORT_DESCRIPTION, Settings.STR_ACE_DISPLAY_NAME + ", run inference" );
					setEnabled( false );
				}

				public void actionPerformed( ActionEvent actionevent ){
					Control.this.compute();
				}
			};
			Control.this.resetACEEnabledState();
		}
		return myActionCompute;
	}

	public Thread compute(){
		return Control.this.startUserTask( runCompute, "ace inference ", STR_MSG_ACE_COMPUTE );
	}

	/*private static ClipboardOwner CLIPBOARDOWNER;
	public static void copyToSystemClipboard( String strSelection )
	{
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable transferable = new StringSelection( strSelection );
		if( CLIPBOARDOWNER == null ) CLIPBOARDOWNER = new ClipboardOwner(){
			public void lostOwnership( Clipboard clipboard, Transferable contents ){}
		};
		clipboard.setContents( transferable, CLIPBOARDOWNER );
	}*/

	/** @author keith cascio
		@since  20060608 */
	private abstract class RunAce implements Runnable
	{
		abstract public boolean doTask( AceInterfaceForPrimula ace ) throws Throwable;

		public boolean interrupted;
		public boolean success;
		public boolean okay;
		public boolean timedout;

		public RunAce( String noun, String note, String msgFinished, boolean[] flags, String infoMessage ){
			myNoun        = noun;
			myNote        = note;
			myFlags       = flags;

			myMsgAborted  = Settings.STR_ACE_DISPLAY_NAME + ", "+myNoun+" aborted";
			myMsgError    = Settings.STR_ACE_DISPLAY_NAME + ", "+myNoun+" error";
			myMsgTimeout  = Settings.STR_ACE_DISPLAY_NAME + ", "+myNoun+" timed out";
			myMsgFinished = Settings.STR_ACE_DISPLAY_NAME + ", "+msgFinished;
			myMsgInfo     = infoMessage;
		}

		public void run(){
			long start = System.currentTimeMillis();
			interrupted = false;
			success     = false;
			okay        = false;
			timedout    = false;
			try{
				AceInterfaceForPrimula ace = getFullyResolvedAceInstance( myMsgAborted );
				if( ace == null ){
					okay = true;
					return;
				}

				if( (Control.this.myInfoMessage != null) && (myMsgInfo != null) ) Control.this.myInfoMessage.setText( myMsgInfo );
				myProgressBar.setString( myNote );
				myProgressBar.setIndeterminate( true );

				getActionCancel().setEnabled( true );

				if( success = RunAce.this.doTask( ace ) ){
					myFlagReadyCompile = myFlags[0];
					myFlagReadyLoad    = myFlags[1];
					myFlagReadyCompute = myFlags[2];
					myProgressBar.setString( myMsgFinished );
					myProgressBar.setValue(  myProgressBar.getMaximum() );
				}
				else myProgressBar.setString( myMsgError );

				okay = true;
			}catch( InterruptedException interruptedexception ){
				okay = interrupted = true;
				myProgressBar.setString( myMsgAborted );
			}catch( Throwable throwable ){
				showDialog( throwable.toString(), myMsgError, JOptionPane.ERROR_MESSAGE );
				System.err.println( "Control.RunAce.run() caught:" );
				throwable.printStackTrace();
			}finally{
				myProgressBar.setIndeterminate( false );
				getActionCancel().setEnabled( false );

				if( timedout ) myProgressBar.setString( myMsgTimeout + " after " + (System.currentTimeMillis() - start) + " ms" );
				else if( !okay ) myProgressBar.setString( myMsgError );

				Control.this.fireStateChanged();

				if( interrupted ) Thread.currentThread().interrupt();
			}
		}

		private String    myNoun;
		private String myNote;
		private String myMsgAborted;
		private String myMsgError;
		private String myMsgFinished;
		private String myMsgTimeout;
		private String myMsgInfo;
		private boolean[] myFlags;
	};

	private RunAce runCompile = new RunAce( "compilation", "compiling bayesian network...", "done compiling", new boolean[] { false, true, false }, STR_MSG_ACE_COMPILE ){
		public boolean doTask( AceInterfaceForPrimula ace ) throws Throwable{
			if( mySettings.isCompileWithEvidence() && myPrimula.isQueryatomsEmpty() ){
				String msg = "ACE setting \"" + SettingsPanel.STR_QUERY_BASED + "\" is selected, but the query is empty.\nPlease add at least one query atom.";
				showDialog( msg, "please add query atoms", JOptionPane.ERROR_MESSAGE );
				return false;
			}

			PrimulaSystemSnapshot snapshot = myPrimula.snapshot();
			if( snapshot == null ) throw new IllegalStateException( "not ready for ace compile" );
			Control.this.setState( snapshot );

			System.setProperty( "ACEC2D", getC2DPackage().getCodeLocation().getCanonicalFile().getAbsolutePath() );

			Settings         effective        = new Settings( mySettings );
			RunTimeout       timeout          = new RunTimeout( effective.getTimeoutCompileMinutes() );
			timeout.start( myThreadGroup );
			RunWriteHuginNet runwritehuginnet = Control.this.myState.new RunWriteHuginNet( effective );
			runwritehuginnet.start( myThreadGroup );

			int shiftMaxLeftBy = 2;
			if( !runwritehuginnet.monitorWhileJoining( myProgressBar, shiftMaxLeftBy ) ){
				Throwable error = runwritehuginnet.getError();
				String msg = "Failed to write bayesian network";
				if( error != null ){
					msg += ":\n" + error.toString();
					error.printStackTrace();
				}
				showDialog( msg, "error writing hugin net", JOptionPane.ERROR_MESSAGE );
				return false;
			}

			myProgressBar.setIndeterminate( false );
			RunAceCompile runacecompile;
			(runacecompile = new RunAceCompile( effective )).start().monitorWhileJoining( myProgressBar );

			timeout.finishedTask();

			Object bayesiannetwork = runacecompile.getBayesianNetwork();
			if( (!runacecompile.success) || (bayesiannetwork == null) ) return false;
			else Control.this.myCompilation = new Compilation( effective, Control.this.myState, bayesiannetwork );

			return true;
		}
	};

	/** @since 20060622 */
	public class RunTimeout implements Runnable{
		public RunTimeout( long minutes ){
			timeout = minutes * 60L * 1000L;
		}

		public synchronized void finishedTask(){
			RunTimeout.this.flagDone = true;
			this.notifyAll();
		}

		public void run(){
			long remaining = timeout;
			long start     = System.currentTimeMillis();
			try{
				synchronized( RunTimeout.this ){
					while( !RunTimeout.this.flagDone ){
						remaining = timeout - (System.currentTimeMillis() - start);
						if( remaining <= 0 ){
							Thread.yield();
							if( !RunTimeout.this.flagDone ){
								if( Thread.interrupted() ) return;
								runCompile.timedout = true;
								runLoad.timedout    = true;
								runCompute.timedout = true;
								Control.this.cancel();
								return;
							}
						}
						this.wait( remaining );
					}
				}
			}catch( InterruptedException interruptedexception ){
				//System.err.println( "warning: RunTimeout.run() caught " + interruptedexception );
				Thread.currentThread().interrupt();
			}
		}

		public Thread start( ThreadGroup group ){
			if( group == null ) group = Thread.currentThread().getThreadGroup();
			Thread ret = new Thread( group, RunTimeout.this, "ace timeout" + Integer.toString( INT_COUNTER++ ) );
			ret.start();
			return ret;
		}

		private long    timeout  =    -1;
		private boolean flagDone = false;
	};

	/** @since 20060603 */
	private class RunAceCompile implements Runnable{
		public RunAceCompile( Settings effective ){
			this.effective = effective;
		}

		public boolean success = false;

		public void run(){
			try{
				RunAceCompile.this.success = false;
				AceInterfaceForPrimula ace = Control.this.getAceInstance();

				File fileHuginNet = Control.this.myState.validateOutputFile();
				String outPrefix  = fileHuginNet.getCanonicalFile().getAbsolutePath();

				RunAceCompile.this.bn = ace.readNetwork( outPrefix );

				boolean retainFiles = false;
				boolean simplify    = false;
				boolean compile     = true;
				boolean noisymax    = false;
				String  instFile    = null;

				PrintStream out = new PrintStream( myAccumulator = new StringOutputStream( 0x800 ) );

				ace.compile(
					new Random(),
					RunAceCompile.this.bn,
					instFile,
					effective.getEncodingMethod(),
					effective.getDtreeMethod(),
					effective.getCountPartitions(),
					retainFiles, /*simplify,*/ compile, /*noisymax,*/
					outPrefix,
					out
				);

				out.flush();
				out.close();
				if( myStream != null ) myStream.close();
				RunAceCompile.this.success = true;
			}catch( InterruptedException interruptedexception ){
				Thread.currentThread().interrupt();
			}catch( Throwable throwable ){
				RunAceCompile.this.success = false;
				System.err.println( "warning: RunAceCompile.run() caught " + throwable );
				throwable.printStackTrace();
			}
		}

		public RunAceCompile start(){
			myThread = new Thread( Control.this.myThreadGroup, RunAceCompile.this, Thread.currentThread().getName() + ", calling ace" );
			myThread.start();
			return RunAceCompile.this;
		}

		public Object getBayesianNetwork(){
			return RunAceCompile.this.bn;
		}

		public void monitorWhileJoining( JProgressBar pbar ) throws InterruptedException{
			//Thread.yield();
			while( myAccumulator == null ){
				Thread.sleep( 0x40 );
			}

			int workCompleted  = pbar.getValue();
			int workToDo       = pbar.getMaximum() - workCompleted;
			int steps          = -1;
			int stepCallAce    = -1;
			int stepFirstDtree = -1;
			int stepLastDtree  = -1;

			if(      effective.getDtreeMethod() == DtreeMethod.HYPERGRAPH ){
				int partitions = effective.getCountPartitions();
				stepFirstDtree = 1;
				stepLastDtree  = partitions + stepFirstDtree;
				steps          = stepLastDtree + 2;
				stepCallAce    = 0;
			}
			else if( effective.getDtreeMethod() == DtreeMethod.BN_MINFILL ){
				steps          = 3;
				stepCallAce    = 1;
			}
			else if( effective.getDtreeMethod() == DtreeMethod.CLAUSE_MINFILL ){
				steps          = 2;
				stepCallAce    = 0;
			}

			int workPerStep    = workToDo / steps;
			int stepCompiling  = steps - 1;

			long begin = System.currentTimeMillis();
			LinkedList<CharSequence> messages = new LinkedList<CharSequence>();
			LinkedList<Long>         times    = new LinkedList<Long>();

			times.add( (long)0 );
			messages.add( "*** begin timing ***" );

			CharSequence line = null;
			long now = -1;
			String strLine;
			Matcher matcher;
			String group;
			while( myThread.isAlive() ){
				line = myAccumulator.getLastLine();
				now  = System.currentTimeMillis() - begin;
				if( !line.equals( messages.getLast() ) ){
					times.add( now );
					messages.add( line );
					//System.out.println( line );
					strLine = line.toString();
					if( strLine.indexOf( "Compiling..." ) >= 0 ){
						pbar.setValue( (stepCompiling*workPerStep)+workCompleted );
					}
					//else if( strLine.indexOf( "Constructing BN dtree..." ) >= 0 ){}
					else if( (matcher = PATTERN_PROGRESS_GENERATING_DTREE.matcher( line )).find() ){
						group = matcher.group(1);
						pbar.setValue( (Math.min( group.length()+stepFirstDtree, stepLastDtree )*workPerStep)+workCompleted );
					}
					else if( (matcher = PATTERN_PROGRESS_INVOKE_C2D.matcher(       line )).find() ){
						group   = matcher.group(2);
						strLine = "c2d " + group;
						pbar.setValue( (stepCallAce*workPerStep)+workCompleted );
					}
					pbar.setString( prettyForProgressBar( strLine, 80 ) );
				}
				Thread.sleep( 0x100 );
			}

			double end = (double) times.getLast();

			/*Iterator<CharSequence> itMess = messages.iterator();
			Iterator<Long>         itTime = times.iterator();
			long time = -1;
			while( itMess.hasNext() && itTime.hasNext() ){
				format( itTime.next(), end, System.out );
				//System.out.print( ' ' );
				//System.out.println( itMess.next() );
			}*/

			myThread.join();
		}

		private void format( long time, double end, PrintStream out ){
			out.print( removeLeadingZeros( format.format(time), 0 ) );
			out.print( ' ' );
			out.print( removeLeadingZeros( percent.format((double)time/end), 1 ) );
		}

		private String removeLeadingZeros( String data, int tail ){
			buff.setLength(0);
			buff.append( data );
			int limit = buff.length()-(1+tail);
			for( int i=0; i<limit; i++ ){
				if( buff.charAt(i) == '0' ) buff.setCharAt(i,' ');
				else break;
			}
			return buff.toString();
		}

		private Settings effective;
		private Object   bn;
		private Thread myThread;
		private BufferedReader myStream;
		private StringOutputStream myAccumulator;
		private DecimalFormat format  = new DecimalFormat( "000000" );
		private DecimalFormat percent = new DecimalFormat( "000%" );
		private StringBuilder buff = new StringBuilder( 16 );
	}

	public static final String  REGEXPR_PROGRESS_GENERATING_DTREE = "\\QGenerating dtree...\\E.*: (.*)$";
	public static final Pattern PATTERN_PROGRESS_GENERATING_DTREE = Pattern.compile( REGEXPR_PROGRESS_GENERATING_DTREE, Pattern.CASE_INSENSITIVE );

	public static final String  REGEXPR_PROGRESS_INVOKE_C2D       = "\\QCompiling: \\E'([^']*)' (.*)$";
	public static final Pattern PATTERN_PROGRESS_INVOKE_C2D       = Pattern.compile( REGEXPR_PROGRESS_INVOKE_C2D, Pattern.CASE_INSENSITIVE );

	/** @since 20060606 */
	public static String prettyForProgressBar( String raw, int maxLength ){
		int lenRaw = raw.length();
		if( lenRaw <= maxLength ) return raw;

		if( maxLength < 16 ) return raw.substring( 0, maxLength );

		int half = (maxLength-3)/2;
		return raw.substring( 0, half ) + "..." + raw.substring( lenRaw - half );
	}

	/** @since 20060605 */
	public static class StringOutputStream extends OutputStream{
		public StringOutputStream( int size ){
			builder = new StringBuilder( size );
		}

		public void write( int b ) throws IOException{
			char c = (char)b;
			if( c == '\n' ){
				myFlagSawNewline = true;
				myTentative      = builder.length()+1;
			}
			else myIndexLastLine = myTentative;
			builder.append( c );
		}

		public StringBuilder getBuffer(){
			return builder;
		}

		public boolean sawNewline(){
			boolean ret = myFlagSawNewline;
			myFlagSawNewline = false;
			return ret;
		}

		public int getIndexLastLine(){
			return myIndexLastLine;
		}

		public CharSequence getLastLine(){
			int end = builder.length();
			if( (end>0) && (builder.charAt(end-1) == '\n') ) --end;
			return builder.subSequence( StringOutputStream.this.getIndexLastLine(), end );
		}

		private StringBuilder builder;
		private boolean       myFlagSawNewline = false;
		private int           myIndexLastLine  =     0;
		private int           myTentative      =     0;
	}

	public class Compilation{
		public Compilation( Settings effective, PrimulaSystemSnapshot snapshot, Object bn ){
			Compilation.this.effective = effective;
			Compilation.this.snapshot  = snapshot;
			Compilation.this.bn        = bn;
		}

		public void die(){
			this.effective    = null;
			this.snapshot.die();
			this.snapshot     = null;
			this.bn           = null;
		}

		public boolean attemptEvidenceChange( OneStrucData newInst ){
			Compilation.this.marginals    = null;

			return snapshot.attemptSetInstantiation( newInst );
		}

		public Object readAc( AceInterfaceForPrimula ace ) throws Exception{
			File nnf  = snapshot.validateNNF();
			File lmap = snapshot.validateLmap();
			return this.ac = ace.readAc( this.bn, nnf.getPath(), lmap.getPath() );
		}

		public Map<String,double[]> marginals( AceInterfaceForPrimula ace ) throws Exception{
			Random random = new Random();

			Map<String,Integer> evidence = null;
			//if( effective.isCompileWithEvidence() )
			//	evidence = Collections.emptyMap();
			//else{
				PrimulaSystemSnapshot.EvidenceInfo info = snapshot.new EvidenceInfo( ace.networkVariables( bn ) );
				evidence = info.currentBayesian;

				String msg = null, title = null;
				//if( !info.atomsAssertedFalseButPresent.isEmpty() ){
				//	msg   = "The following false evidence is present in the bayesian network:\n" + info.namesPretty( info.atomsAssertedFalseButPresent );
				//	title = "warning: false evidence present in bayesian network";
				//}

				if( !info.atomsAssertedTrueButAbsent.isEmpty() ){
					msg   = "The following true evidence is missing in the bayesian network.\nYou might have asserted contradictory evidence.\n" + info.namesPretty( info.atomsAssertedTrueButAbsent );
					title = "warning: evidence missing in bayesian network";
				}

				if( msg != null ){
					int result = showConfirmDialog( msg, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );
					if( result == JOptionPane.CANCEL_OPTION ) return (Map<String,double[]>)null;
				}
			//}

			//System.out.println( "ace.assertEvidence( "+evidence+" )" );

			ace.assertEvidence(     random, bn, ac, evidence );
			if( ace.probOfEvidence( random, bn, ac ) <= 0 ){
				Control.this.showDialog( STR_DISPLAY_NAME_ACE + " detected inconsistent evidence.  Please review the current instantiation and try again.", "Contradictory (Inconsistent) Evidence", JOptionPane.ERROR_MESSAGE );
				return (Map<String,double[]>)null;
			}
			return ace.marginals(   random, bn, ac );
		}

		public Settings              effective;
		public PrimulaSystemSnapshot snapshot;
		public Object                bn;
		public Object                ac;
		public Map<String,double[]>  marginals;
	}

	private RunAce runLoad = new RunAce( "load", "loading arithmetic circuit...", "done loading", new boolean[] { false, false, true }, STR_MSG_ACE_LOAD ){
		public boolean doTask( AceInterfaceForPrimula ace ) throws Throwable{
			if( Control.this.myCompilation == null ){
				throw new IllegalStateException( "trying to load, but compilation is null" );
			}

			return Control.this.myCompilation.readAc( ace ) != null;
		}
	};

	private RunAce runCompute = new RunAce( "inference", "performing inference...", "finished inference", new boolean[] { false, false, true }, STR_MSG_ACE_COMPUTE ){
		public boolean doTask( AceInterfaceForPrimula ace ) throws Throwable{
			if( Control.this.myCompilation == null ){
				throw new IllegalStateException( "trying to perform inference, but compilation is null" );
			}

			Map<String,double[]> marginals = Control.this.myCompilation.marginals( ace );
			if( marginals == null ) return false;

			if( myDataModel != null ) myDataModel.updateACE( marginals );
			return true;
		}
	};

	private ProgressMonitor getProgressMonitor( String message, String note, int max ){
		ProgressMonitor monitor = new ProgressMonitor( myParentComponent, message, note, 1, max );

		monitor.setMillisToDecideToPopup( 256 );
		monitor.setMillisToPopup( 512 );

		return monitor;
	}

	private Primula myPrimula;
	private PrimulaSystemSnapshot myState;
	private Component myParentComponent;
	private JLabel myInfoMessage;
	private BoundedRangeModel myProgressModel;
	private JProgressBar myProgressBar;
	private Settings mySettings;
	private Compilation myCompilation;
	private boolean myFlagReadyCompile = true;
	private boolean myFlagReadyLoad = false;
	private boolean myFlagReadyCompute = false;
	private LinkedList<ACEControlListener> myListeners;
	private SoftwareEntity                 myPackageAce;
	private SoftwareEntity myPackageInflib;
	private SoftwareEntity myPackageC2D;
	private RuntimeSoftwareLocationBrowser myRuntimeSoftwareLocationBrowser;
	private AceInterfaceForPrimula                   myAceInt;
	private QueryTableModel                myDataModel;
	private Action                         myActionCancel;
	private Action myActionFastForward;
	private Action myActionCompile;
	private Action myActionLoad;
	private Action myActionCompute;

	private ThreadGroup myThreadGroup       = new ThreadGroup( "ace control" );
	private ThreadGroup myThreadGroupCancel = new ThreadGroup( "ace cancel" );
	private Object      mySynchUserTasks    = new Object();
	private static int INT_COUNTER = 0;
}
