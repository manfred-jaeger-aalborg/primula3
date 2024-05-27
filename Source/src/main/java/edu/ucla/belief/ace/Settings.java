package edu.ucla.belief.ace;

import edu.ucla.belief.ace.AceInterfaceForPrimula.EncodingMethod;
import edu.ucla.belief.ace.AceInterfaceForPrimula.DtreeMethod;

import java.util.*;
import java.io.File;
import java.io.IOException;

/** @author keith cascio
	@since 20060511 */
public class Settings
{
	public static final String         STR_ACE_DISPLAY_NAME                  = "ACE";
	public static final boolean        BOOLEAN_COMPILE_WITH_EVIDENCE_DEFAULT = false;
	public static final long           LONG_COMPILE_TIMEOUT_MINUTES_DEFAULT  = 60;//1440 minutes/day;
	public static final EncodingMethod ENCODINGMETHOD_DEFAULT                = EncodingMethod.CHAVIRA_DARWICHE_06;//cd06;
	public static final DtreeMethod    DTREEMETHOD_DEFAULT                   = DtreeMethod.HYPERGRAPH;//hypergraph;
	public static final int            INT_PARTITIONS_DEFAULT                = 3;
	public static final int            INT_PARTITIONS_MIN                    = 1;
	public static final int            INT_PARTITIONS_MAX                    = 128;
	public static final String         STR_DESCRIBE_PARTITIONS_INTERVAL      = "[" + Integer.toString( INT_PARTITIONS_MIN ) + "," + Integer.toString( INT_PARTITIONS_MAX ) + "]";

	//public static final String         "edu.ucla.belief.AceCompile";

	/** @since 20060727 */
	public enum Key{
		aceQueryBased{
			public Object get( Settings settings ){
				return settings.isCompileWithEvidence() ? Boolean.TRUE : Boolean.FALSE;
			}

			protected void setHook( Settings settings, CharSequence value ){
				settings.setCompileWithEvidence( Boolean.parseBoolean( value.toString() ) );
			}

			protected void setDefault( Settings settings ){
				settings.setCompileWithEvidence( BOOLEAN_COMPILE_WITH_EVIDENCE_DEFAULT );
			}
		},
		aceEncodingMethod{
			public Object get( Settings settings ){
				return settings.getEncodingMethod();
			}

			protected void setHook( Settings settings, CharSequence value ){
				settings.setEncodingMethod( EncodingMethod.valueOf( value.toString() ) );
			}

			protected void setDefault( Settings settings ){
				settings.setEncodingMethod( ENCODINGMETHOD_DEFAULT );
			}
		},
		aceDtreeMethod{
			public Object get( Settings settings ){
				return settings.getDtreeMethod();
			}

			protected void setHook( Settings settings, CharSequence value ){
				settings.setDtreeMethod( DtreeMethod.valueOf( value.toString() ) );
			}

			protected void setDefault( Settings settings ){
				settings.setDtreeMethod( DTREEMETHOD_DEFAULT );
			}
		},
		aceCountPartitions{
			public Object get( Settings settings ){
				return new Integer( settings.getCountPartitions() );
			}

			protected void setHook( Settings settings, CharSequence value ){
				settings.setCountPartitions( Integer.parseInt( value.toString() ) );
			}

			protected void setDefault( Settings settings ){
				settings.setCountPartitions( INT_PARTITIONS_DEFAULT );
			}
		},
		aceTimeoutCompileMinutes{
			public Object get( Settings settings ){
				return new Long( settings.getTimeoutCompileMinutes() );
			}

			protected void setHook( Settings settings, CharSequence value ){
				settings.setTimeoutCompileMinutes( Long.parseLong( value.toString() ) );
			}

			protected void setDefault( Settings settings ){
				settings.setTimeoutCompileMinutes( LONG_COMPILE_TIMEOUT_MINUTES_DEFAULT );
			}
		},
		aceOutputDirectory{
			public Object get( Settings settings ){
				return settings.getOutputDirectory();
			}

			protected void setHook( Settings settings, CharSequence value ){
				settings.setOutputDirectory( new File( value.toString() ) );
			}

			protected void setDefault( Settings settings ){
				settings.setOutputDirectory( PrimulaSystemSnapshot.FILE_SYSTEM_TEMP_DIR );
			}

			public String valueToString( Object value ){
				try{
					return ((File)value).getCanonicalFile().getAbsolutePath();
				}catch( Exception exception ){
					System.err.println( "warning! failed to canonicalize " + name() );
					return null;
				}
			}
		},
		aceSettings{
			public Object get( Settings settings ){
				return null;
			}

			protected void setHook( Settings settings, CharSequence value ){
				throw new UnsupportedOperationException();
			}

			protected void setDefault( Settings settings ){
				for( Key key : values() ){
					if( key != this ) key.setDefault( settings );
				}
			}
		};

		public             Object revert(     Settings settings ){
			setDefault( settings );
			return get( settings );
		}
		abstract protected void   setDefault( Settings settings );
		abstract public    Object get(        Settings settings );
		abstract protected void   setHook(    Settings settings, CharSequence value );
		public             Object set(        Settings settings, CharSequence value ){
			try{
				setHook( settings, value );
			}catch( Exception exception ){
				System.err.println( "warning! failed to set " + name() + " to " + value + " because: " + exception );
				revert( settings );
			}
			return get( settings );
		}
		public String valueToString( Object value ){
			return value.toString();
		}

		public Appendable open( Appendable appendable ) throws IOException {
			return appendable.append( myOpen );
		}
		public Appendable close( Appendable appendable ) throws IOException {
			return appendable.append( myClose );
		}
		public void write( Settings settings, Appendable appendable, String prefix, String postfix ) throws IOException {
			Object value = get( settings );
			if( value == null ) return;
			String data = valueToString( value );
			if( data == null ) return;
			close( open( appendable.append( prefix ) ).append( data ) ).append( postfix );
		}

		private String myOpen  = "<"  + name() + ">";
		private String myClose = "</" + name() + ">";
	}

	/** @since 20060727 */
	public Appendable append( Appendable buff ) throws IOException
	{
		buff.append( "  " ); Key.aceSettings.open( buff ); buff.append( '\n' );
		for( Key key : Key.values() ){
			key.write( Settings.this, buff, "    ", "\n" );
		}
		buff.append( "  " ); Key.aceSettings.close( buff ); buff.append( '\n' );
		return buff;
	}

	public Settings(){
	}

	/** automatically marks new SettingsSampling as read-only */
	public Settings( Settings toCopy ){
		this.copy( toCopy );
		this.setReadOnly( true );
	}

	public String[] toCommandLine( PrimulaSystemSnapshot snapshot ){
		String[] ret = null;
		try{
			int count = 3;
			if( myFlagCompileWithEvidence ) count += 2;

			ret = new String[count];

			int index = 0;
			ret[ index++ ] = myEncodingMethod.toString();
			ret[ index++ ] = myDtreeMethod.toString();

			if( myFlagCompileWithEvidence ){
				ret[ index++ ] = "-e";
				ret[ index++ ] = snapshot.validateEvidenceFile().getCanonicalFile().getAbsolutePath();
			}

			ret[ index++ ] = snapshot.validateOutputFile().getCanonicalFile().getAbsolutePath();
		}catch( Exception exception ){
			System.err.println( "warning: SettingsSampling.toCommandLine() caught " + exception );
			return null;
		}
		return ret;
	}

	public void setCompileWithEvidence( boolean flag ){
		checkReadOnly();
		if( myFlagCompileWithEvidence != flag ){
			myFlagCompileWithEvidence = flag;
			fireCompileSettingChanged();
		}
	}

	public boolean isCompileWithEvidence(){
		return myFlagCompileWithEvidence;
	}

	public void setEncodingMethod( EncodingMethod method ){
		checkReadOnly();
		if( myEncodingMethod != method ){
			myEncodingMethod = method;
			fireCompileSettingChanged();
		}
	}

	public EncodingMethod getEncodingMethod(){
		return myEncodingMethod;
	}

	public void setDtreeMethod( DtreeMethod method ){
		checkReadOnly();
		if( myDtreeMethod != method ){
			myDtreeMethod = method;
			fireCompileSettingChanged();
		}
	}

	public DtreeMethod getDtreeMethod(){
		return myDtreeMethod;
	}

	/** validates */
	public void setCountPartitions( int count ){
		checkReadOnly();
		if( myCountPartitions != count ){
			if( (count < INT_PARTITIONS_MIN) || (INT_PARTITIONS_MAX < count) ) throw new IllegalArgumentException( count + " out of range " + STR_DESCRIBE_PARTITIONS_INTERVAL );
			myCountPartitions = count;
			fireCompileSettingChanged();
		}
	}

	public int getCountPartitions(){
		return myCountPartitions;
	}

	public void setTimeoutCompileMinutes( long minutes ){
		checkReadOnly();
		if( myTimeoutCompileMinutes != minutes ){
			myTimeoutCompileMinutes = minutes;
			fireCompileSettingChanged();
		}
	}

	public long getTimeoutCompileMinutes(){
		return myTimeoutCompileMinutes;
	}

	/** @since 20060724 */
	public void setOutputDirectory( File dir ){
		checkReadOnly();
		if( (dir == null) || (!dir.isDirectory()) || (!dir.exists()) ) throw new IllegalArgumentException();
		if( (myOutputDirectory == null) || (!myOutputDirectory.equals( dir )) ){
			myOutputDirectory = dir;
			fireCompileSettingChanged();
		}
	}

	/** @since 20060724 */
	public File getOutputDirectory(){
		return myOutputDirectory;
	}

	public ACESettingsListener addListener( ACESettingsListener acesettingslistener ){
		if( myListeners == null ) myListeners = new LinkedList<ACESettingsListener>();
		if( !myListeners.contains( acesettingslistener ) ) myListeners.add( acesettingslistener );
		return acesettingslistener;
	}

	public boolean removeListener( ACESettingsListener acesettingslistener ){
		if( myListeners == null ) return false;
		return myListeners.remove( acesettingslistener );
	}

	private void fireCompileSettingChanged(){
		if( myListeners == null ) return;
		for( ACESettingsListener listener : myListeners ) listener.aceCompileSettingChanged( this );
	}

	public interface ACESettingsListener{
		public void aceCompileSettingChanged( Settings settings );
	}

	public Settings copy( Settings other ){
		boolean changed = false;

		changed |= (this.myFlagCompileWithEvidence != other.myFlagCompileWithEvidence);
		changed |= (this.myEncodingMethod          != other.myEncodingMethod);
		changed |= (this.myDtreeMethod             != other.myDtreeMethod);
		changed |= (this.myTimeoutCompileMinutes   != other.myTimeoutCompileMinutes);
		changed |= (this.myCountPartitions         != other.myCountPartitions);
		changed |= (this.myOutputDirectory         != other.myOutputDirectory);

		this.myFlagCompileWithEvidence = other.myFlagCompileWithEvidence;
		this.myEncodingMethod          = other.myEncodingMethod;
		this.myDtreeMethod             = other.myDtreeMethod;
		this.myTimeoutCompileMinutes   = other.myTimeoutCompileMinutes;
		this.myCountPartitions         = other.myCountPartitions;
		this.myOutputDirectory         = other.myOutputDirectory;

		if( changed ) fireCompileSettingChanged();

		return this;
	}

	/** detect whether the settings have changed in a way that necessitates throwing out an old compilation */
	public boolean isCompileStale( Settings newSettings ){
		boolean stale = false;

		stale |= (this.myFlagCompileWithEvidence != newSettings.myFlagCompileWithEvidence);
		stale |= (this.myEncodingMethod          != newSettings.myEncodingMethod);
		stale |= (this.myDtreeMethod             != newSettings.myDtreeMethod);
		stale |= (this.myCountPartitions         != newSettings.myCountPartitions);
		stale |= (this.myOutputDirectory         != newSettings.myOutputDirectory);

		return stale;
	}

	public void setReadOnly( boolean flag ){
		myFlagReadOnly = flag;
	}

	private void checkReadOnly(){
		if( myFlagReadOnly ) throw new IllegalStateException( "this edu.ucla.belief.ace.Settings marked read-only" );
	}

	private boolean        myFlagCompileWithEvidence = BOOLEAN_COMPILE_WITH_EVIDENCE_DEFAULT;
	private EncodingMethod myEncodingMethod          = ENCODINGMETHOD_DEFAULT;
	private DtreeMethod    myDtreeMethod             = DTREEMETHOD_DEFAULT;
	private long           myTimeoutCompileMinutes   = LONG_COMPILE_TIMEOUT_MINUTES_DEFAULT;
	private int            myCountPartitions         = INT_PARTITIONS_DEFAULT;//[1,128]
	private File           myOutputDirectory         = PrimulaSystemSnapshot.FILE_SYSTEM_TEMP_DIR;

	private LinkedList<ACESettingsListener> myListeners;
	private boolean myFlagReadOnly = false;

	/*public enum EncodingMethod{
		//iip, ii, resolve,
		d02, sbk05, cd05, cd06
	}

	public enum DtreeMethod{
		hypergraph, bnminfill, clauseminfill
	}*/

	//4 Usage for compile

	//compile
	//[-version]
	//[-s]
	//[-retainFiles]
	//[-noCompile]
	//[-iip | -ii | -resolve]
	//[-dtHypergraph <count> | -dtBnMinfill | -dtClauseMinfill]
	//[-e <evidenceFile>]
	//<networkFile>
}
