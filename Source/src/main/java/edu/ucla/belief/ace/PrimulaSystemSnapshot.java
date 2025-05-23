package edu.ucla.belief.ace;

import RBNinference.BayesConstructor;
import RBNgui.Primula;
import RBNpackage.*;

import java.util.*;
import java.io.*;
import java.text.NumberFormat;
import javax.swing.JProgressBar;

/** Make a medium-depth copy of the state of the primula system
	just before constructing the bayesian network and
	writing the hugin .net file

	@author keith cascio
	@since 20060515 */
public class PrimulaSystemSnapshot
{
	/** makes a medium-depth copy of everything,
		i.e. not a truly deep copy, but not a shallow copy either,
		specifically, calls copy constructors for { RBN, RelStruc, Instantiation, AtomList }
	*/
	public PrimulaSystemSnapshot(
		Primula primula, // added 15092023 MJ -- this seems against the spirit of the snapshot, but needed for
		                 // for compatibility with updates of Primula methods
		RBN rbn,
		RelStruc rels,
		OneStrucData inst,
		GroundAtomList queryatoms,
		File srsfile,
		File rbnfile,
		File bnoutfile,
		int querymode,
		int evidencemode,
		int decomposemode,
		int isolatedzeronodesmode,
		int layoutmode,
		int bnsystem,
		Settings acesettings )
	{
		this.primula 			   = primula;
		this.rels                  = (   RelStruc     ) rels.clone();
		this.rbn                   = new RBN          ( rbn );
		this.inst                  = new OneStrucData( this.myCurrentEvidence = inst       );
		this.queryatoms            = new GroundAtomList     ( this.myCurrentQuery    = queryatoms );

		this.srsfile               = srsfile;
		this.rbnfile               = rbnfile;
		this.bnoutfile             = bnoutfile;

		this.querymode             = querymode;
		this.evidencemode          = evidencemode;
		this.decomposemode         = decomposemode;
		this.isolatedzeronodesmode = isolatedzeronodesmode;
		this.layoutmode            = layoutmode;
		this.bnsystem              = bnsystem;

		this.myACESettings         = new Settings( acesettings );

		this.myCreationTime        = System.currentTimeMillis();
	}

	public void die(){
		this.rels       = null;
		this.rbn        = null;
		this.inst       = null;
		this.queryatoms = null;
	}

	/** @since 20061023 */
	public OneStrucData getInstantiation(){
		return this.inst;
	}

	/** @since 20061023 */
	public boolean attemptSetInstantiation( OneStrucData newInst ){
		if( (this.inst != null) && (!(newInst.containsAll( this.inst ))) ) return false;
		this.inst                  = new OneStrucData( this.myCurrentEvidence = newInst );
		return true;
	}

	/** @since 20060622 */
	public class EvidenceInfo{
		public EvidenceInfo( Set<String> validIds ){
			EvidenceInfo.this.validIds        = validIds;
			EvidenceInfo.this.currentBayesian = EvidenceInfo.this.currentBayesianEvidence();
		}

		/** @since 20060620 */
		private Map<String,Integer> currentBayesianEvidence(){
			if( PrimulaSystemSnapshot.this.rels == null ) throw new IllegalStateException( "cannot generate current evidence because RelStruc is null" );

			if( PrimulaSystemSnapshot.this.myCurrentEvidence == null ) return Collections.emptyMap();

			int sizePrimula = PrimulaSystemSnapshot.this.myCurrentEvidence.size();
			if( sizePrimula < 1 ) return Collections.emptyMap();

			allInstAtoms = PrimulaSystemSnapshot.this.myCurrentEvidence.allInstAtoms();

			int numAtoms = allInstAtoms.size();
			Map<String,Integer> ret = new HashMap<String,Integer>( numAtoms );
			atomToId   = new HashMap<InstAtom,String>( numAtoms );
			atomToName = new HashMap<InstAtom,String>( numAtoms );
			idsAssertedFalseButPresent.clear();
			idsAssertedTrueButAbsent.clear();
			atomsAssertedFalseButPresent.clear();
			atomsAssertedTrueButAbsent.clear();

			Integer integerFalse = Integer.valueOf(0);
			Integer integerTrue  = Integer.valueOf(1);

			StringBuilder buffIden = new StringBuilder( 128 );
			StringBuilder buffName = new StringBuilder( 128 );
			String nameAt;
			String id;
			for( InstAtom atom : allInstAtoms ){
				buffIden.setLength(0);
				buffName.setLength(0);
				buffIden.append( atom.rel.name.name );
				buffName.append( atom.rel.name.name );

				buffIden.append( 'I' );
				buffName.append( '(' );
				if( (atom.args != null) && (atom.args.length > 0) ){
					for( int arg : atom.args ){
						buffIden.append( nameAt = PrimulaSystemSnapshot.this.rels.nameAt( arg ) );
						buffName.append( nameAt );
						buffIden.append( 'p' );
						buffName.append( ',' );
					}
					buffIden.setLength( buffIden.length() - 1 );
					buffName.setLength( buffName.length() - 1 );
				}
				buffIden.append( 'I' );
				buffName.append( ')' );

				atomToId.put(   atom, id = buffIden.toString() );
				atomToName.put( atom,      buffName.toString() );
				if( atom.isBooleanTrue() ){
					if( !validIds.contains( id ) ){
						idsAssertedTrueButAbsent.add(   id   );
						atomsAssertedTrueButAbsent.add( atom );
					}
					else ret.put( id, integerTrue );
				}
				else{
					if(  validIds.contains( id ) ){
						idsAssertedFalseButPresent.add(   id   );
						atomsAssertedFalseButPresent.add( atom );
						ret.put( id, integerFalse );
					}
				}
			}

			//System.out.println( "\ncurrent bayesian evidence:" );
			//for( String validated : ret.keySet() ){
			//	System.out.println( validated + " = " + ret.get( validated ) );
			//}
			//System.out.println();

			return ret;
		}

		public String namesPretty( Collection<InstAtom> atoms ){
			int size = atoms.size();
			StringBuilder buff = new StringBuilder( size * 0x80 );
			if( size > 8 ){
				buff.append( "{ " );
				names( atoms, buff, ", " );
				buff.append( " }" );
			}
			else{
				names( atoms, buff, "\n" );
			}
			return buff.toString();
		}

		private StringBuilder names( Collection<InstAtom> atoms, StringBuilder buff, String delimiter ){
			if( (atoms == null) || (atoms.isEmpty()) ) return buff;

			if( (atomToName == null) || (atomToName.isEmpty()) ) throw new RuntimeException( "cannot map atoms to names, map \"atomToName\" not available" );

			//StringBuilder buff = new StringBuilder( atoms.size() * 0x80 );
			for( InstAtom atom : atoms ){
				buff.append( atomToName.get( atom ) );
				buff.append( delimiter );
			}
			buff.setLength( buff.length() - delimiter.length() );
			return buff;
		}

		public Map<String,Integer>  currentBayesian;
		public Set<String>          validIds;
		public List<InstAtom>       allInstAtoms;
		public Map<InstAtom,String> atomToId;
		public Map<InstAtom,String> atomToName;
		public Collection<String>   idsAssertedFalseButPresent   = new LinkedList<String>();
		public Collection<String>   idsAssertedTrueButAbsent     = new LinkedList<String>();
		public Collection<InstAtom> atomsAssertedFalseButPresent = new LinkedList<InstAtom>();
		public Collection<InstAtom> atomsAssertedTrueButAbsent   = new LinkedList<InstAtom>();
	}

	public File validateEvidenceFile(){
		if( this.myFileEvidence == null ){
			if( this.inst == null ){
				System.err.println( "warning: PrimulaSystemSnapshot.validateEvidenceFile() called but this.inst == null" );
				return null;
			}
		}
		return this.myFileEvidence;
	}

	public File validateOutputFile(){
		if( this.bnoutfile == null ){
			if( (this.srsfile == null) || (this.rbnfile == null) ){
				System.err.println( "warning: PrimulaSystemSnapshot.validateOutputFile() called but (this.srsfile == null) || (this.rbnfile == null)" );
				return null;
			}

			StringBuilder buff = new StringBuilder( 256 );
			buff.append( basename( srsfile ).replace( CHAR_DOT, CHAR_UNDERSCORE ) );
			buff.append( CHAR_UNDERSCORE );
			buff.append( basename( rbnfile ).replace( CHAR_DOT, CHAR_UNDERSCORE ) );

			buff.append( "_Ix" );
			if( this.evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED ){
				buff.append( this.inst.size() );
			}
			else buff.append( "NA" );

			buff.append( "_Qx" );
			buff.append( this.queryatoms.size() );
			buff.append( CHAR_UNDERSCORE );
			DateFormatFilename.getInstance().format( new Date( myCreationTime ), buff );
			buff.append( ".primula.net" );

			this.bnoutfile = new File( myACESettings.getOutputDirectory(), buff.toString() );
		}
		return this.bnoutfile;
	}

	/** @since 20060608 */
	public File validateNNF() throws FileNotFoundException{
		boolean exists = false;
		try{
			if( this.nnfoutfile == null ){
				File bnout = validateOutputFile();
				if( bnout == null ) return this.nnfoutfile = null;

				this.nnfoutfile = new File( bnout.getCanonicalFile().getAbsolutePath() + ".ac" );
			}
			exists = this.nnfoutfile.exists();
		}catch( Throwable throwable ){
			System.err.println( "warning: PrimulaSystemSnapshot.validateNNF() caught " + throwable );
			return this.nnfoutfile = null;
		}
		if( exists ) return this.nnfoutfile;
		else throw new FileNotFoundException( "nnf file \"" +this.nnfoutfile+ "\" not found" );
	}

	/** @since 20060608 */
	public File validateLmap() throws FileNotFoundException{
		boolean exists = false;
		try{
			if( this.lmapoutfile == null ){
				File bnout = validateOutputFile();
				if( bnout == null ) return this.lmapoutfile = null;

				this.lmapoutfile = new File( bnout.getCanonicalFile().getAbsolutePath() + ".lmap" );
			}
			exists = this.lmapoutfile.exists();
		}catch( Throwable throwable ){
			System.err.println( "warning: PrimulaSystemSnapshot.validateLmap() caught " + throwable );
			return this.lmapoutfile = null;
		}
		if( exists ) return this.lmapoutfile;
		else throw new FileNotFoundException( "lmap file \"" +this.lmapoutfile+ "\" not found" );
	}

	public static String basename( File aFile ){
		String path = aFile.getPath();
		String basename = path.substring( path.lastIndexOf( File.separatorChar ) + 1 );
		return basename;
	}

	public static String basenameSansExtension( File aFile ){
		String basename = basename( aFile );
		String ret = basename.substring( 0, basename.lastIndexOf( CHAR_DOT ) );
		return ret;
	}

	public /*static*/ class RunWriteHuginNet implements Runnable{
		public RunWriteHuginNet( Settings settings ){// PrimulaSystemSnapshot snapshot ){
			//this.snapshot = snapshot;
			this.mySettings = settings;
		}

		public void run(){
			try{
				RunWriteHuginNet.this.success = doWriteHuginNet();
			}catch( Throwable throwable ){
				RunWriteHuginNet.this.myError = throwable;
				RunWriteHuginNet.this.success = false;
			}
		}

		public boolean doWriteHuginNet() throws Throwable{
			if( validateOutputFile() == null ){
				System.err.println( "warning: could not validate bn output file" );
				return false;
			}

			int layout = Primula.OPTION_NO_LAYOUT;//this.layoutmode

			int evidence_mode = -1;
			int query_mode    = -1;
			if( mySettings.isCompileWithEvidence() ){
				evidence_mode = Primula.OPTION_EVIDENCE_CONDITIONED;
				query_mode    = Primula.OPTION_QUERY_SPECIFIC;
			}
			else{
				evidence_mode = Primula.OPTION_NOT_EVIDENCE_CONDITIONED;
				query_mode    = Primula.OPTION_NOT_QUERY_SPECIFIC;
			}

			this.myConstructor = new BayesConstructor(
					PrimulaSystemSnapshot.this.rbn,
				PrimulaSystemSnapshot.this.rels,
				PrimulaSystemSnapshot.this.inst,
				PrimulaSystemSnapshot.this.queryatoms,
				PrimulaSystemSnapshot.this.bnoutfile,
				PrimulaSystemSnapshot.this.primula
				);

			synchronized( RunWriteHuginNet.this ){
				RunWriteHuginNet.this.notifyAll();
			}

			return this.myConstructor.constructCPTNetwork(
				//[MJ: 030425]
					// evidence_mode,
					PrimulaSystemSnapshot.this.evidencemode,
				query_mode,
				PrimulaSystemSnapshot.this.decomposemode,
				PrimulaSystemSnapshot.this.isolatedzeronodesmode,
				layout,
				Primula.OPTION_HUGIN );
		}

		public BayesConstructor waitForBayesConstructor() throws InterruptedException {
			BayesConstructor ret = null;
			Thread.yield();
			long begin = System.currentTimeMillis();
			while( (ret = this.myConstructor) == null ){
				if( (System.currentTimeMillis() - begin) > 0x1000 ) return null;
				synchronized( RunWriteHuginNet.this ){
					RunWriteHuginNet.this.wait( 0x200 );//Thread.sleep( 0x80 );
				}
			}
			return ret;
		}

		public Thread start( ThreadGroup threadgroup ){
			myThread = new Thread( threadgroup, RunWriteHuginNet.this, "write hugin net " + Integer.toString( INT_COUNTER++ ) );
			myThread.start();
			return myThread;
		}

		public boolean succeeded(){
			return RunWriteHuginNet.this.success;
		}

		public Throwable getError(){
			return myError;
		}

		public boolean monitorWhileJoining( JProgressBar pbar, int shiftMaxLeft ) throws InterruptedException{
			File fileHuginNet = validateOutputFile();
			String filename   = basename( fileHuginNet );
			String note       = "writing " + filename;
			//System.out.println( note );

			pbar.setString( Control.prettyForProgressBar( note, 80 ) );

			BayesConstructor constructor = waitForBayesConstructor();
			pbar.setIndeterminate( false );

			int max = 0x40;//any medium-size integer will work here
			if( constructor != null ){
				while( myThread.isAlive() ){
					Thread.sleep( 0x80 );
					pbar.setMaximum( constructor.getProgressMax() << shiftMaxLeft );
					pbar.setValue(   constructor.getProgress()                    );
				}
				max = constructor.getProgressMax();
			}

			myThread.join();

			if( succeeded() ){
				pbar.setMaximum( max << shiftMaxLeft );
				pbar.setValue(   max                 );
			}

			return succeeded();
		}

		private BayesConstructor myConstructor;
		private boolean          success = false;
		private Thread           myThread;
		private Throwable        myError;
		private Settings         mySettings;
	};

	public static class DateFormatFilename{
		private static DateFormatFilename INSTANCE;
		private DateFormatFilename(){}
		public static DateFormatFilename getInstance(){
			if( INSTANCE == null ) INSTANCE = new DateFormatFilename();
			return INSTANCE;
		}

		public static String now(){
			return getInstance().format( new Date( System.currentTimeMillis() ) );
		}

		public String format( Date date ){
			myBuffer.setLength(0);
			return format( date, myBuffer ).toString();
		}

		//public StringBuffer format( Date date, StringBuffer toAppendTo ){
		public Appendable format( Date date, Appendable toAppendTo ){
			Calendar calendar = Calendar.getInstance();
			calendar.setTime( date );

			int year   = calendar.get( Calendar.YEAR );
			int month  = calendar.get( Calendar.MONTH );
			int day    = calendar.get( Calendar.DAY_OF_MONTH );
			int hour24 = calendar.get( Calendar.HOUR_OF_DAY );
			int minute = calendar.get( Calendar.MINUTE );
			int second = calendar.get( Calendar.SECOND );
			int millis = calendar.get( Calendar.MILLISECOND );

			try{
				toAppendTo.append( myNumberFormat.format( year ) );
				toAppendTo.append( myNumberFormat.format( month+1 ) );
				toAppendTo.append( myNumberFormat.format( day ) );
				toAppendTo.append( '_' );
				toAppendTo.append( myNumberFormat.format( hour24 ) );
				toAppendTo.append( myNumberFormat.format( minute ) );
				toAppendTo.append( myNumberFormat.format( second ) );
				toAppendTo.append( myNumberFormat.format( millis ) );
			}catch( IOException ioexception ){
				System.err.println( "warning: PrimulaSystemSnapshot.DateFormatFilename.format() caught " + ioexception );
			}

			return toAppendTo;
		}

		//private StringBuffer myBuffer = new StringBuffer( 32 );
		private StringBuilder myBuffer = new StringBuilder( 32 );
		private NumberFormat myNumberFormat = new java.text.DecimalFormat( "##00" );
	}

	public static final File FILE_SYSTEM_TEMP_DIR = new File( System.getProperty( "java.io.tmpdir" ) );
	public static final char CHAR_DOT        = '.';
	public static final char CHAR_UNDERSCORE = '_';

	private static int INT_COUNTER = 0;

	private long myCreationTime;
	private File myFileEvidence;

	private Primula primula;
	private RBN rbn;
	private RelStruc rels;
	private OneStrucData inst;
	private OneStrucData myCurrentEvidence;
	private GroundAtomList queryatoms;
	private GroundAtomList myCurrentQuery;

	private File srsfile;
	private File rbnfile;
	private File bnoutfile;
	private File nnfoutfile;
	private File lmapoutfile;

	private int querymode;
	private int evidencemode;
	private int decomposemode;
	private int isolatedzeronodesmode;
	private int layoutmode;
	private int bnsystem;

	private Settings myACESettings;
}
