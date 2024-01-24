/*
 * Primula.java
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



import javax.swing.*;

import myio.StringOps;

import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Vector;

import MLNParser.MLNParserFacade;
import RBNpackage.*;
import RBNio.*;
import RBNinference.*;
import RBNExceptions.*;
import RBNLearning.*;
import edu.ucla.belief.ui.primula.*;
import edu.ucla.belief.ace.PrimulaSystemSnapshot;

public class Primula extends JFrame implements PrimulaUIInt, ActionListener, ItemListener, KeyListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final Color COLOR_YELLOW          = new Color(189, 187, 127);
	public static final Color COLOR_YELLOW_SELECTED = new Color(249, 245, 107);//58,57,98
	public static final Color COLOR_BLUE            = new Color(114, 122, 136);//218,17,53
	public static final Color COLOR_BLUE_SELECTED   = new Color(162, 195, 255);//218,36,100
	public static final Color COLOR_GREEN           = new Color(129, 166, 135);//129,22,65
	public static final Color COLOR_GREEN_SELECTED  = new Color(128, 255, 128);//129,93,90
	// public static final Color COLOR_RED             = new Color(189, 127, 127);//0,33,74
	public static final Color COLOR_RED             = new Color(159, 135, 135);//0, 15, 63
	public static final Color COLOR_RED_SELECTED    = new Color(255, 181, 181);//0, 29, 100

	/**
	 * @uml.property  name="mb"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuBar mb               = new JMenuBar();
	/**
	 * @uml.property  name="moduleMenu"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenu moduleMenu            = new JMenu("Modules");
	/**
	 * @uml.property  name="evModule"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuItem evModule        = new JMenuItem("Inference Module");
	// +Learn
	/**
	 * @uml.property  name="lrnModule"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuItem lrnModule     = new JMenuItem("Learn Module");
	/**
	 * @uml.property  name="loadOrdered"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
//	private JMenuItem loadOrdered     = new JMenuItem("Create OrderedStruc");
	/**
	 * @uml.property  name="loadSparse"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
//	private JMenuItem loadSparse      = new JMenuItem("Load Relational Structure");
	/**
	 * @uml.property  name="startBavaria"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuItem startBavaria    = new JMenuItem("Bavaria");
	/**
	 * @uml.property  name="runmenu"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenu runmenu             = new JMenu("Run");
	/**
	 * @uml.property  name="constructCPTBN"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuItem constructCPTBN    = new JMenuItem("Construct Bayesian Network");
	/**
	 * @uml.property  name="saveRBN"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuItem saveRBN   = new JMenuItem("Save RBN");
	/**
	 * @uml.property  name="dataConvert"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	
	private JMenuItem saveData   = new JMenuItem("Save Data");
	
	private JMenuItem dataConvert   = new JMenuItem("Convert Relational Data");


	/**
	 * @uml.property  name="itemInvokeSamIam"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuItem itemInvokeSamIam= new JMenuItem("Open SamIam");
	/**
	 * @uml.property  name="itemForgetAll"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuItem itemForgetAll   = new JMenuItem("Reset external software locations");
	/**
	 * @uml.property  name="exit"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuItem exit            = new JMenuItem("Exit");
	/**
	 * @uml.property  name=""
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenu optionsmenu         = new JMenu("Options");
	/**
	 * @uml.property  name="rbnSystems"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenu rbnSystems          = new JMenu("Bayes Network System");
	/**
	 * @uml.property  name="decMode"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenu decMode             = new JMenu("Decompose Mode");
	/**
	 * @uml.property  name="helpmenu"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenu helpmenu            = new JMenu("Help");
	/**
	 * @uml.property  name="itemabout"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuItem itemabout           = new JMenuItem("About Primula");
	//    private JMenu sampleOrd            = new JMenu("Sampling Order");
	private static JTextArea messageArea  = new JTextArea(15, 20);
	/**
	 * @uml.property  name="rstsrcLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
//	private JLabel rstsrcLabel            = new JLabel("Domain source:");
	
	/**
	 * @uml.property  name="rstsrc"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel rstsrc                 = new JLabel("");
	/**
	 * @uml.property  name="rbnsrcLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel rbnsrcLabel            = new JLabel("Model source:");
	/**
	 * @uml.property  name="bnoutLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	//private JLabel bnoutLabel             = new JLabel("BN output:");
	private JLabel datasrcLabel = new JLabel("Data source:");
	
	/**
	 * @uml.property  name="rbnfilename"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField rbnfilename        = new JTextField(15);
	/**
	 * @uml.property  name="bnoutfilename"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
//	private JTextField bnoutfilename      = new JTextField(15);
	private JTextField datasrcfilename      = new JTextField(15);
	
	/**
	 * @uml.property  name="loadRBN"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton loadRBN               = new JButton("Browse");
	/**
	 * @uml.property  name="saveBN"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
//	private JButton saveBN                = new JButton("Browse");
	private JButton datasrcBrowseButton                = new JButton("Browse");
	
	
	/**
	 * @uml.property  name="scrollPane"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane scrollPane        = new JScrollPane();
	/**
	 * @uml.property  name="bnetFileChooser"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JFileChooser bnetFileChooser      = new JFileChooser( "." );
	/**
	 * @uml.property  name="relmodelFileChooser"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JFileChooser relmodelFileChooser      = new JFileChooser( "." );
	/**
	 * @uml.property  name="domainFileChooser"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JFileChooser domainFileChooser      = new JFileChooser( "." );
	/**
	 * @uml.property  name="myFilterRST"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private javax.swing.filechooser.FileFilter myFilterRST; //keith cascio 20061201
	/**
	 * @uml.property  name="myFilterRBN"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private javax.swing.filechooser.FileFilter myFilterRBN;
	/**
	 * @uml.property  name="myFilterRDEF"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private javax.swing.filechooser.FileFilter myFilterRDEF;
	/**
	 * @uml.property  name="myFilterMLN"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private javax.swing.filechooser.FileFilter myFilterMLN;
	/**
	 * @uml.property  name="myFilterPL"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private javax.swing.filechooser.FileFilter myFilterPL;
	/**
	 * @uml.property  name="myFilterDB"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private javax.swing.filechooser.FileFilter myFilterDB;
	/**
	 * @uml.property  name="myFilterFOIL"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private javax.swing.filechooser.FileFilter myFilterFOIL;
	
	private javax.swing.filechooser.FileFilter myFilterNET;
	private javax.swing.filechooser.FileFilter myFilterBIF;
	private javax.swing.filechooser.FileFilter myFilterDNE;
	
	/**
	 * @uml.property  name="messages"
	 */
	private String messages               = "";
	private static boolean isBavariaOpen  = false;
	private static boolean isEvModuleOpen = false;
	private static boolean isLrnModuleOpen = false;
	/**
	 * @uml.property  name="strucEdited"
	 */
	private boolean strucEdited           = false; //edited in bavaria

	/**
	 * @uml.property  name="srcLabels"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel srcLabels      = new JPanel(new GridLayout(3, 1));
	/**
	 * @uml.property  name="rbnInputFields"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel rbnInputFields = new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="bnoutInputFields"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
//	private JPanel bnoutInputFields  = new JPanel(new BorderLayout());
	private JPanel datasrcInputFields  = new JPanel(new BorderLayout());
	
	/**
	 * @uml.property  name="inputFields"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel inputFields    = new JPanel(new GridLayout(3, 1));
	/**
	 * @uml.property  name="southPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel southPanel     = new JPanel(new BorderLayout());



	public static final int OPTION_NOT_EVIDENCE_CONDITIONED = 0;
	public static final int OPTION_EVIDENCE_CONDITIONED     = 1;
	public static final int OPTION_NOT_QUERY_SPECIFIC       = 0;
	public static final int OPTION_QUERY_SPECIFIC           = 1;

	public static final int OPTION_DECOMPOSE  = 0;
	public static final int OPTION_DECOMPOSE_DETERMINISTIC        = 1;
	public static final int OPTION_NOT_DECOMPOSE  = 2;
	public static final int OPTION_NOT_ELIMINATE_ISOLATED_ZERO_NODES  = 0;
	public static final int OPTION_ELIMINATE_ISOLATED_ZERO_NODES      = 1;
	public static final int OPTION_NO_LAYOUT = 0;
	public static final int OPTION_LAYOUT = 1;
	public static final int OPTION_JAVABAYES                = 0;
	public static final int OPTION_HUGIN                    = 1;
	public static final int OPTION_NETICA                   = 2;
	public static final int OPTION_SAMIAM                   = 3;

	public static final int CLASSICSYNTAX                   = 0;
	public static final int CHERRYSYNTAX                   = 1;

	/**
	 * @uml.property  name="javaBayes"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButtonMenuItem javaBayes;
	/**
	 * @uml.property  name="hugin"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButtonMenuItem hugin;
	/**
	 * @uml.property  name="netica"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButtonMenuItem netica;
	/**
	 * @uml.property  name="samiam"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButtonMenuItem samiam;
	/**
	 * @uml.property  name="decnone"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButtonMenuItem decnone;
	/**
	 * @uml.property  name="decstandard"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButtonMenuItem decstandard;
	/**
	 * @uml.property  name="decdet"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButtonMenuItem decdet;
	/**
	 * @uml.property  name="synclassic"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButtonMenuItem synclassic;
	/**
	 * @uml.property  name="syncherry"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButtonMenuItem syncherry;


	/**
	 * @uml.property  name="querySpecific"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBoxMenuItem querySpecific;
	/**
	 * @uml.property  name="evidenceConditioned"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBoxMenuItem evidenceConditioned;
	/**
	 * @uml.property  name="layoutItem"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBoxMenuItem layoutItem;
	/**
	 * @uml.property  name="eliminateIsolatedZeroNodes"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBoxMenuItem eliminateIsolatedZeroNodes;


	/**
	 * @uml.property  name="querymode"
	 */
	protected int querymode = OPTION_NOT_QUERY_SPECIFIC ;
	/**
	 * @uml.property  name="evidencemode"
	 */
	protected int evidencemode = OPTION_EVIDENCE_CONDITIONED;
	/**
	 * @uml.property  name="decomposemode"
	 */
	protected int decomposemode = OPTION_NOT_DECOMPOSE;
	/**
	 * @uml.property  name="isolatedzeronodesmode"
	 */
	protected int isolatedzeronodesmode = OPTION_ELIMINATE_ISOLATED_ZERO_NODES;
	/**
	 * @uml.property  name="layoutmode"
	 */
	protected int layoutmode = OPTION_LAYOUT;
	/**
	 * @uml.property  name="bnsystem"
	 */
	protected int bnsystem = OPTION_SAMIAM;
	/**
	 * @uml.property  name="rbnsyntax"
	 */
	protected int rbnsyntax = CLASSICSYNTAX;

	/**
	 * @uml.property  name="sTRUCTURE_MODIFIED"
	 */
	private final String STRUCTURE_MODIFIED = "Current structure modified. Continue?";
	/**
	 * @uml.property  name="iNST_AND_QUERIES_LOST"
	 */
	private final String INST_AND_QUERIES_LOST = "This action will cause current instantiations and queries to be lost. Continue?";

	private final String UNSAVED_DATA_LOST = "This action will cause all unsaved data to be lost. Continue?";
	
	/** @author keith cascio
	@since  20061105 */
	public static final String  STR_OPTION_DEBUG = "debug";
	public static       boolean FLAG_DEBUG       = false;

	/**
	 * @uml.property  name="srsfile"
	 */
	protected File srsfile;
	
	protected File rdeffile;
	/**
	 * @uml.property  name="rbnfile"
	 */
	protected File rbnfile;
	/**
	 * @uml.property  name="bnoutfile"
	 */
	protected File bnoutfile;
	/**
	 * @uml.property  name="evidenceModule"
	 * @uml.associationEnd  inverse="myprimula:RBNgui.InferenceModule"
	 */
	protected InferenceModule evidenceModule;
	// +Learn
	/**
	 * @uml.property  name="learnModule"
	 * @uml.associationEnd  inverse="myprimula:RBNgui.LearnModule"
	 */
	protected LearnModule learnModule;
	/**
	 * @uml.property  name="bavaria"
	 * @uml.associationEnd  inverse="mainWindow:RBNgui.Bavaria"
	 */
	protected Bavaria bavaria;
	/**
	 * @uml.property  name="rels"
	 * @uml.associationEnd  
	 */
	protected RelStruc rels;
	
	protected Signature sig;
	/**
	 * @uml.property  name="rbn"
	 * @uml.associationEnd  
	 */
	protected RBN rbn;
	/**
	 * @uml.property  name="instasosd"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	protected OneStrucData instasosd = new OneStrucData();
	
	
	/* All data defining input domain, instantiation, and learning cases.
	 * First input domain specified in rdata is equal to rels, 
	 * and first observed data case for first input domain is equal to 
	 * instasosd
	 */
	RelData rdata;
	
	/**
	 * @uml.property  name="queryatoms"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	protected GroundAtomList queryatoms = new GroundAtomList();

	/** @author keith cascio
	@since 20060728 */
	public RBN getRBN(){
		return Primula.this.rbn;
	}

	public void setRBN(RBN newrbn){
		rbn = newrbn;
	}

	/**
	 * @author  keith cascio
	 * @since  20060728
	 * @uml.property  name="rels"
	 */
	public RelStruc getRels(){
		return Primula.this.rels;
	}

	public RelData getReldata(){
		return rdata;
	}
	
	public Signature getSignature(){
		return sig;
	}
	
	/** @author keith cascio
	@since 20060515 */
	public PrimulaSystemSnapshot snapshot(){
		if( (this.rbn == null) || (this.rels == null) ) return null;

		PrimulaSystemSnapshot ret = new PrimulaSystemSnapshot(
				this,
				this.rbn,
				this.rels,
				this.instasosd,
				this.queryatoms,
				this.srsfile,
				this.rbnfile,
				this.bnoutfile,
				this.querymode,
				this.evidencemode,
				this.decomposemode,
				this.isolatedzeronodesmode,
				this.layoutmode,
				this.bnsystem,
				this.getPreferences().getACESettings()
		);
		return ret;
	}

	/** @author keith cascio
	@since  20061201 */
	public void setDecomposeMode( int mode ){
		Primula.this.decomposemode = mode;
	}

	/**
	 * @uml.property  name="mySamiamManager"
	 * @uml.associationEnd  
	 */
	private SamiamManager mySamiamManager;
	/**
	 * @uml.property  name="myFlagSystemExitEnabled"
	 */
	private boolean myFlagSystemExitEnabled = true;
	public static final String STR_FILENAME_LOGO = "Icons/small_logo.jpg";
	/**
	 * @uml.property  name="myPreferences"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private Preferences myPreferences;
	/**
	 * @uml.property  name="btnDebugAceCompile"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JMenuItem btnDebugAceCompile = new JMenuItem( "DEBUG ace compile" );

	/**
       @author Keith Cascio
       @since 040804
	 */
	public void setTheSamIamUI( edu.ucla.belief.ui.primula.SamiamUIInt ui ){
		//THE_SAMIAM_UI = ui;
		getSamiamManager().setSamiamUIInstance( ui );
	}

	/**
       @author Keith Cascio
       @since 050404
	 */
	public SamiamManager getSamiamManager(){
		if( mySamiamManager == null ) mySamiamManager = new SamiamManager( this );
		return mySamiamManager;
	}

	/** @author keith cascio
	@since 20060602 */
	public void forgetAll(){
		if( myPreferences != null ) myPreferences.forgetAll();
		if( mySamiamManager != null ) mySamiamManager.forgetAll();
		if( evidenceModule != null ) evidenceModule.forgetAll();
	}

	/**
       @author Keith Cascio
       @since 040804
	 */
	public JFrame asJFrame(){
		return this;
	}

	/**
       @arg flag Sets whether the JVM should terminate when the user closes Primula.  Set this to false if you call Primula from another Java program and you want to prevent Java from exiting when the user exits Primula.
       @since 040804
	 */
	public void setSystemExitEnabled( boolean flag ){
		myFlagSystemExitEnabled = flag;
	}

	/**
       @ret true if a user action that closes Primula will cause the JVM to terminate as well.
       @since 040804
	 */
	public boolean isSystemExitEnabled(){
		return myFlagSystemExitEnabled;
	}

	/**
       @author Keith Cascio
       @since 040804
	 */
	public void exitProgram(){
		myPreferences.saveOptionsToFile();
		if( isSystemExitEnabled() ) System.exit( 0 );
		else setVisible( false );
	}

	/**
       @author Keith Cascio
       @since 040804
	 */
	private void init()
	{
		//ImageIcon icon = new ImageIcon("small_logo.jpg");
		ImageIcon icon = getIcon( STR_FILENAME_LOGO );

		if( icon.getImageLoadStatus() == MediaTracker.COMPLETE ){//image ok
			this.setIconImage(icon.getImage());
		}
		this.setTitle("Primula");
		this.pack();

		myPreferences = new Preferences( true );
	}

	/**
       @author Keith Cascio
       @since 042104
	 */
	public ImageIcon getIcon( String fileName ){
		ClassLoader myLoader = this.getClass().getClassLoader();
		java.net.URL urlImage = myLoader.getResource( fileName );
		if( urlImage == null ){
			System.err.println( "Warning: loader.getResource(\""+fileName+"\") failed." );
			return new ImageIcon( fileName );
		}
		else return new ImageIcon( urlImage );
	}

	/**
       @author Keith Cascio
       @since 040504
	 */
	public String makeNetworkName()
	{
		if( bnoutfile == null ) return makeAlternateName();
		else return bnoutfile.getPath();
	}

	/**
       @author Keith Cascio
       @since 040504
	 */
	public String makeAlternateName()
	{
		String strRST = (rstsrc == null ) ? "no_RST" : rstsrc.getText();
		if( strRST.length() < (int)1 ) strRST = "no_RST";
		String strRBN = (rbnfilename == null ) ? "no_RBN" : rbnfilename.getText();
		if( strRBN.length() < (int)1 ) strRBN = "no_RBN";

		return pluckNameFromPath( strRST ) + "_" + pluckNameFromPath( strRBN ) + ".net";
	}

	/**
       @author Keith Cascio
       @since 040504
	 */
	public static String pluckNameFromPath( String path )
	{
		int index0 = path.lastIndexOf( File.separator );
		if( index0 < (int)0 ) index0 = (int)0;
		else ++index0;
		int index1 = path.lastIndexOf( "." );
		if( index1 < (int)0 ) index1 = path.length();
		return path.substring( index0, index1 );
	}

	/**
       @author Keith Cascio
       @since 040804
	 */
	public edu.ucla.belief.ui.primula.SamiamUIInt getSamIamUIInstanceThis(){
		//edu.ucla.belief.ui.primula.SamiamUIInt ui = getSamIamUIInstance();
		//if( ui != null ) ui.setPrimulaUIInstance( this );
		//return ui;
		return getSamiamManager().getSamIamUIInstance();
	}

	/**
       @author Keith Cascio
       @since 050404
	 */
	public Preferences getPreferences(){
		return myPreferences;
	}

	public Primula()
	{
		//Creates the menus

		moduleMenu.add(startBavaria);
		moduleMenu.add(evModule);
		moduleMenu.add(lrnModule);
		
		mb.add(moduleMenu);

		runmenu.add(constructCPTBN);
		runmenu.add(saveRBN);
		runmenu.add(saveData);
		runmenu.add(dataConvert);
		runmenu.add(itemInvokeSamIam);
		if( Primula.FLAG_DEBUG ) runmenu.add( btnDebugAceCompile );//keith cascio 20060516
		runmenu.add(exit);
		mb.add(runmenu);

		ButtonGroup rbnGroup = new ButtonGroup();
		javaBayes = new JRadioButtonMenuItem("Java Bayes");
		//javaBayes.setSelected(true);
		rbnGroup.add(javaBayes);
		rbnSystems.add(javaBayes);
		hugin = new JRadioButtonMenuItem("Hugin");
		rbnGroup.add(hugin);
		rbnSystems.add(hugin);
		netica = new JRadioButtonMenuItem("Netica");
		rbnGroup.add(netica);
		rbnSystems.add(netica);
		samiam = new JRadioButtonMenuItem("To SamIam");
		samiam.setSelected(true);
		rbnGroup.add(samiam);
		rbnSystems.add(samiam);

		ButtonGroup decmGroup = new ButtonGroup();
		decnone =  new JRadioButtonMenuItem("none");
		decnone.setSelected(true);
		decmGroup.add(decnone);
		decMode.add(decnone);
		decstandard =  new JRadioButtonMenuItem("normal");
		
		decmGroup.add(decstandard);
		decMode.add(decstandard);
		decdet =  new JRadioButtonMenuItem("deterministic");
		decmGroup.add(decdet);
		decMode.add(decdet);

		ButtonGroup syntaxGroup = new ButtonGroup();
		synclassic = new JRadioButtonMenuItem("Classic");
		synclassic.setSelected(true);
		syncherry = new JRadioButtonMenuItem("Cherry");

		syntaxGroup.add(synclassic);
		syntaxGroup.add(syncherry);


		JMenu inferenceOptions = new JMenu("Construction Mode");
		querySpecific = new JCheckBoxMenuItem("Query specific");
		inferenceOptions.add(querySpecific);
		evidenceConditioned = new JCheckBoxMenuItem("Evidence conditioned");
		evidenceConditioned.setSelected(true);
		inferenceOptions.add(evidenceConditioned);
		layoutItem = new JCheckBoxMenuItem("Skip layout",false);
		inferenceOptions.add(layoutItem);
		eliminateIsolatedZeroNodes = new JCheckBoxMenuItem("Show isolated prob.0 nodes",false);
		inferenceOptions.add(eliminateIsolatedZeroNodes);
		

		inferenceOptions.add(decMode);
		JMenu syntaxOptions = new JMenu("RBN Syntax (save)");
		syntaxOptions.add(synclassic);
		syntaxOptions.add(syncherry);


		optionsmenu.add(rbnSystems);
		optionsmenu.add(inferenceOptions);
		optionsmenu.add(itemForgetAll);
		optionsmenu.add(syntaxOptions);
		itemForgetAll.setToolTipText( "Forget the locations of all external software dependencies, i.e. samiam, inflib, ace, etc." );
		mb.add(optionsmenu);
		setJMenuBar(mb);

		helpmenu.add(itemabout);
		mb.add(helpmenu);

		myFilterRST = new Filter_rst() ;
		myFilterRDEF = new Filter_rdef();
		myFilterPL = new Filter_pl() ;
		myFilterFOIL = new Filter_foil() ;
		myFilterDB = new Filter_db(false); 
		myFilterRBN = new Filter_rbn(); 
		myFilterMLN = new Filter_mln();
		myFilterNET = new Filter_net();
		myFilterBIF = new Filter_bif();
		myFilterDNE = new Filter_dne();
		

		domainFileChooser.addChoosableFileFilter( myFilterRST);
		domainFileChooser.addChoosableFileFilter( myFilterRDEF );
		domainFileChooser.addChoosableFileFilter( myFilterPL);
		domainFileChooser.addChoosableFileFilter( myFilterRDEF);
		domainFileChooser.addChoosableFileFilter( myFilterFOIL);

		relmodelFileChooser.addChoosableFileFilter( myFilterRBN);
		relmodelFileChooser.addChoosableFileFilter(myFilterMLN );
		bnetFileChooser.addChoosableFileFilter(new Filter_bif());
		bnetFileChooser.addChoosableFileFilter(new Filter_net());
		bnetFileChooser.addChoosableFileFilter(new Filter_dne());

		bnetFileChooser.setAcceptAllFileFilterUsed( true );
		domainFileChooser.setAcceptAllFileFilterUsed( true );
		relmodelFileChooser.setAcceptAllFileFilterUsed( true );

		//actionlistener for loading ordered strucs
//		loadOrdered.addActionListener( this );


		//actionlistener for loading sparserelstrucs from the file
//		loadSparse.addActionListener( this );


		//actionlistener for loading rbn from the file (via browse-button)
		loadRBN.addActionListener( this );
		//keylistener for loading rbn from the file (via textfield)
		rbnfilename.addKeyListener( this );

		//actionlistener for choosing data source file (via browse-button)
		datasrcBrowseButton.addActionListener( this );


		//keylistener for for choosing data source file (via textfield)
//		bnoutfilename.addKeyListener( this );
		datasrcfilename.addKeyListener( this );

		//actionlistener for starting the Bavaria editor
		startBavaria.addActionListener( this );


		//actionlistener for constructing standard BN
		constructCPTBN.addActionListener( this );

		//actionlistener for saving the RBN
		saveRBN.addActionListener( this );

		saveData.addActionListener( this );
		
		dataConvert.addActionListener( this );
		itemInvokeSamIam.addActionListener( this );
		itemForgetAll.addActionListener( this );

		//actionlistener for opening the evidence module
		evModule.addActionListener( this );
		// +Learn
		lrnModule.addActionListener( this );

		javaBayes.addItemListener( this );

		hugin.addItemListener( this );

		netica.addItemListener( this );
		samiam.addItemListener( this );

		decnone.addItemListener( this );

		decstandard.addItemListener( this );
		decdet.addItemListener( this );
		querySpecific.addItemListener( this );
		evidenceConditioned.addItemListener( this );

		eliminateIsolatedZeroNodes.addItemListener( this );

		synclassic.addItemListener( this );
		syncherry.addItemListener( this );


		//	adaptiveSampling.addItemListener( this );
		layoutItem.addItemListener( this );

		//actionlistener for exiting the program
		exit.addActionListener( this );

		btnDebugAceCompile.addActionListener( this );

		itemabout.addActionListener( this );

		//creating the layout
//		srcLabels.add(rstsrcLabel);
		srcLabels.add(rbnsrcLabel);
		// srcLabels.add(bnoutLabel);
		srcLabels.add(datasrcLabel);
		
		rbnfilename.setBackground(Color.white);
		rbnInputFields.add(rbnfilename, BorderLayout.CENTER);
		rbnInputFields.add(loadRBN, BorderLayout.EAST);

//		bnoutfilename.setBackground(Color.white);
//		bnoutInputFields.add(bnoutfilename, BorderLayout.CENTER);
//		bnoutInputFields.add(saveBN, BorderLayout.EAST);

		datasrcfilename.setBackground(Color.white);
		datasrcInputFields.add(datasrcfilename, BorderLayout.CENTER);
		datasrcInputFields.add(datasrcBrowseButton, BorderLayout.EAST);
		
		
		rstsrc.setForeground(Color.black);
//		inputFields.add(rstsrc);
		inputFields.add(rbnInputFields);
		inputFields.add(datasrcInputFields);

		southPanel.add(srcLabels, BorderLayout.WEST);
		southPanel.add(inputFields, BorderLayout.CENTER);

		scrollPane.getViewport().add(messageArea);
		messageArea.setEditable(false);

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(scrollPane, BorderLayout.CENTER);
		contentPane.add(southPanel, BorderLayout.SOUTH);


		//inner class for closing the window
		this.addWindowListener(
				new WindowAdapter(){
					public void windowClosing(WindowEvent e){
						exitProgram();
					}
				}
		);

		this.init();
	}

	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();

	 if( source == datasrcBrowseButton ){
			domainFileChooser.setFileFilter( myFilterRDEF );
			int value = domainFileChooser.showDialog(Primula.this, "Select");
			if (value == JFileChooser.APPROVE_OPTION){
				rdeffile = domainFileChooser.getSelectedFile();
				if (confirm(UNSAVED_DATA_LOST)){
					loadSparseRelFile(rdeffile);
					datasrcfilename.setText(rdeffile.getName());
					

				}
			}
		}

		else if( source == loadRBN ){
			relmodelFileChooser.resetChoosableFileFilters();
			relmodelFileChooser.addChoosableFileFilter(myFilterRBN);
			relmodelFileChooser.addChoosableFileFilter(myFilterMLN);
			relmodelFileChooser.setFileFilter(myFilterRBN);
			int value = relmodelFileChooser.showDialog(Primula.this, "Load");
			if (value == JFileChooser.APPROVE_OPTION) {
				File selectedFile = relmodelFileChooser.getSelectedFile();
				if (myFilterRBN.accept(selectedFile))
					loadRBNFunction(selectedFile);
				else if (myFilterMLN.accept(selectedFile)){
					File mlnFile = selectedFile;
					relmodelFileChooser.resetChoosableFileFilters();
					Filter_db cwdbFilter = new Filter_db(false);
					relmodelFileChooser.addChoosableFileFilter(cwdbFilter);
					relmodelFileChooser.setFileFilter(cwdbFilter);
					value = relmodelFileChooser.showDialog(Primula.this, "Load domain data");

					File cwdbfile = null;
					if (value == JFileChooser.APPROVE_OPTION) 
						cwdbfile = relmodelFileChooser.getSelectedFile();

					relmodelFileChooser.resetChoosableFileFilters();
					Filter_db owdbFilter = new Filter_db(true);
					relmodelFileChooser.addChoosableFileFilter(owdbFilter);
					value = relmodelFileChooser.showDialog(Primula.this, "Load evidence data");
					File owdbfile = null;
					if (value == JFileChooser.APPROVE_OPTION) 
						owdbfile = relmodelFileChooser.getSelectedFile();

					this.LoadMLN(mlnFile,cwdbfile,owdbfile);
					if (cwdbfile != null)
						this.rstsrc.setText(" "+cwdbfile.getName());
					else
						this.rstsrc.setText(" "+mlnFile.getName());

				}/* end extension mln */
			}
		}
		else if( source == startBavaria ){
			//actionlistener for starting the Bavaria editor
			//rst loaded from file
			if(rels != null && rels instanceof SparseRelStruc && !isBavariaOpen && srsfile != null){
				SparseRelStruc temp = (SparseRelStruc)rels;
				if(temp.getCoords().size() == 0)
					temp.createCoords();
				bavaria = new Bavaria(temp, srsfile, Primula.this, strucEdited);
				isBavariaOpen = true;
				rstsrc.setText("Bavaria RelStruc Editor");
			}


			//rst file created in Bavaria
			else if (rels != null && rels instanceof SparseRelStruc && !isBavariaOpen && srsfile == null){
				bavaria = new Bavaria((SparseRelStruc)rels, null, Primula.this, strucEdited);
				isBavariaOpen = true;
				rstsrc.setText("Bavaria RelStruc Editor");
			}

			//create a new rst file
			else if (rels == null && !isBavariaOpen){
				rels = new SparseRelStruc();
				bavaria = new Bavaria((SparseRelStruc)rels, Primula.this, strucEdited);
				isBavariaOpen = true;
				rstsrc.setText("Bavaria RelStruc Editor");
			}
		}
		else if( source == constructCPTBN ){
			//actionlistener for constructing standard BN
			boolean nogo = false;
			String message = "";
			if (rbn == null){
				nogo = true;
				message = message + " Please load rbn first";
			}
			if (rels == null){
				nogo = true;
				message = message + " Please load RelStruc first";
			}
			if (rbn != null){
				if (!rbn.multlinOnly() & decomposemode != OPTION_NOT_DECOMPOSE){
					nogo = true;
					message = message + " Please choose decompose:none for rbn containing non multilinear comb. functions";
				}
			}
			if(!nogo){
				try {
					BayesConstructor constructor = null;
					if( bnsystem == OPTION_SAMIAM )
						constructor = new BayesConstructor(Primula.this ,
								instasosd,
								queryatoms,
								makeNetworkName());

					else { setBnoutfile();
					constructor = new BayesConstructor(rbn,
							rels,
							instasosd,
							queryatoms,
							bnoutfile);
					}
					constructor.constructCPTNetwork(evidencemode,
							querymode,
							decomposemode,
							isolatedzeronodesmode,
							layoutmode,
							bnsystem);
				}
				catch(RBNCyclicException ex) {this.showMessage(ex.toString());}
				catch(RBNCompatibilityException ex){this.showMessage(ex.toString());}
				catch(RBNIllegalArgumentException ex){this.showMessage(ex.toString());};
			}
			else this.showMessage(message);
		}
		else if (source == saveRBN){
			File rbnfile;
			relmodelFileChooser.setFileFilter(myFilterRBN);
			int value = relmodelFileChooser.showDialog(Primula.this, "Save");
			if (value == JFileChooser.APPROVE_OPTION) {
				rbnfile = relmodelFileChooser.getSelectedFile();
				rbn.saveToFile(rbnfile,rbnsyntax,true);
			}
		}
		else if (source == saveData){
			domainFileChooser.setFileFilter( myFilterRDEF );
			int value = domainFileChooser.showDialog(Primula.this, "Save");
			if (value == JFileChooser.APPROVE_OPTION) {
				File savefile = domainFileChooser.getSelectedFile();
				rdata.saveToRDEF(savefile);
			}
		}
		else if (source == dataConvert){
			File sourcefile;
			File targetfile;
			RelData inrdata = new RelData(); 

			domainFileChooser.resetChoosableFileFilters();
			//			domainFileChooser.addChoosableFileFilter(myFilterRDEF);
			domainFileChooser.addChoosableFileFilter(myFilterPL);
			int value = domainFileChooser.showDialog(Primula.this, "Load");
			if (value == JFileChooser.APPROVE_OPTION) {
				try{
					sourcefile = domainFileChooser.getSelectedFile();
					//					if (myFilterRDEF.accept(sourcefile)){
					//						RDEFReader rdefreader = new RDEFReader();
					//						rdata = (RelData)rdefreader.readRDEF(sourcefile.getPath(),null);
					//					}
					if (myFilterPL.accept(sourcefile)){
						AtomListReader alreader = new AtomListReader();
						inrdata = alreader.readAL(sourcefile);
					}
				}
				catch (Exception ex){ ex.printStackTrace();}
			}
			domainFileChooser.resetChoosableFileFilters();
			domainFileChooser.addChoosableFileFilter(myFilterRDEF);
			//			domainFileChooser.addChoosableFileFilter(myFilterFOIL);	
			value = domainFileChooser.showDialog(Primula.this, "Save");
			if (value == JFileChooser.APPROVE_OPTION) {
				try{
					targetfile = domainFileChooser.getSelectedFile();
					if (myFilterRDEF.accept(targetfile))
						inrdata.saveToRDEF(targetfile);
					//					if (myFilterFOIL.accept(targetfile))
					//						rdata.saveToFOIL(targetfile);

				}
				catch (Exception ex){ ex.printStackTrace();}
			}	
		}
		else if( source == itemInvokeSamIam ){
			//SamiamUIInt ui = getSamIamUIInstanceThis();
			//if( ui != null ) ui.asJFrame().setVisible( true );
			getSamiamManager().openSamiam();
		}
		else if( source == itemForgetAll ){
			Primula.this.forgetAll();
		}
		else if( source == evModule ){
			//actionlistener for opening the evidence module
			if(!isEvModuleOpen){
				evidenceModule = new InferenceModule(this
				);
				isEvModuleOpen = true;
			}
		}
		// +Learn
		else if( source == lrnModule ){
			//actionlistener for opening the learn module
			openLearnModule(true);
		}
		else if( source == exit ){
			if(strucEdited){
				if(confirm(STRUCTURE_MODIFIED) == true)
					exitProgram();
			}
			else
				exitProgram();
		}
		else if( source == btnDebugAceCompile ){
			//String pathRST = "./blockmap_large.rst";
			//String pathRBN = "./randblock_trans.rbn";
			String pathRST = "./holmes_2.rst";
			String pathRBN = "./holmes_2.rbn";
			loadSparseRelFile( srsfile = new File( pathRST ) );
			loadRBNFunction(             new File( pathRBN ) );
			evModule.doClick();
			//evidenceModule.aceCompile();
			//evidenceModule.getACEControl().getActionCompile().actionPerformed( null );
		}
		else if( source == itemabout ){
			JOptionPane.showMessageDialog(null,"Primula version 2.2 " + '\n' + "(C) 2009");
		}
	}

	public void keyPressed(KeyEvent e){
		//Invoked when a key has been pressed.
//		Object source = e.getSource();
//		if( source == rbnfilename ){
//			char c = e.getKeyChar();
//			if(c == KeyEvent.VK_ENTER){
//				loadRBNFunction(new File(rbnfilename.getText()));
//			}
//		}
//		else if( source == datasrcfilename ){
//			//keylistener for for choosing bn-output file (via textfield)
//			char c = e.getKeyChar();
//			if(c == KeyEvent.VK_ENTER){
//				bnoutfile = new File(datasrcfilename.getText());
//			}
//		}
	}
	public void keyReleased(KeyEvent e){
		//Invoked when a key has been released.
	}
	public void keyTyped(KeyEvent e){
		//Invoked when a key has been typed.
	}



	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if( source == javaBayes ){
			if (e.getStateChange() == ItemEvent.SELECTED)
				bnsystem = OPTION_JAVABAYES;
		}
		else if( source == hugin ){
			if (e.getStateChange() == ItemEvent.SELECTED)
				bnsystem = OPTION_HUGIN;
		}
		else if( source == netica ){
			if (e.getStateChange() == ItemEvent.SELECTED)
				bnsystem = OPTION_NETICA;
		}
		else if( source == samiam ){
			if (e.getStateChange() == ItemEvent.SELECTED)
				bnsystem = OPTION_SAMIAM;
		}
		else if( source == decnone ){
			if (e.getStateChange() == ItemEvent.SELECTED)
				decomposemode = OPTION_NOT_DECOMPOSE;
		}
		else if( source == decstandard ){
			if (e.getStateChange() == ItemEvent.SELECTED)
				decomposemode = OPTION_DECOMPOSE;
		}
		else if( source == decdet ){
			if (e.getStateChange() == ItemEvent.SELECTED)
				decomposemode = OPTION_DECOMPOSE_DETERMINISTIC;
		}
		else if( source == querySpecific){
			if (e.getStateChange() == ItemEvent.SELECTED)
				querymode = OPTION_QUERY_SPECIFIC;
			else querymode = OPTION_NOT_QUERY_SPECIFIC;
		}
		else if( source == evidenceConditioned ){
			if (e.getStateChange() == ItemEvent.SELECTED)
				evidencemode = OPTION_EVIDENCE_CONDITIONED;
			else evidencemode = OPTION_NOT_EVIDENCE_CONDITIONED;
		}
		else if( source == eliminateIsolatedZeroNodes ) {
			if (e.getStateChange() == ItemEvent.SELECTED)
				isolatedzeronodesmode = OPTION_NOT_ELIMINATE_ISOLATED_ZERO_NODES;
			else isolatedzeronodesmode = OPTION_ELIMINATE_ISOLATED_ZERO_NODES;
		}
		else if( source == synclassic ) {
			if (e.getStateChange() == ItemEvent.SELECTED)
				rbnsyntax = CLASSICSYNTAX;
		}
		else if( source == syncherry ) {
			if (e.getStateChange() == ItemEvent.SELECTED)
				rbnsyntax = CHERRYSYNTAX;
		}
		else if( source == layoutItem ){
			if (e.getStateChange() == ItemEvent.SELECTED)
				layoutmode = OPTION_NO_LAYOUT;
			else layoutmode = OPTION_LAYOUT;
		}

	}

	//creates a new ordered structure
	public void newOrdStruc(int dom){
		rels = new SparseRelStruc(dom);
		strucEdited = false;
		srsfile = null;
		if(isEvModuleOpen)
			evidenceModule.newElementNames();
		else{
			instasosd.clear();
			queryatoms.reset();
		}
		rstsrc.setText("Ordered Structure size "+dom);
	}


	//loads the sparserel structure from file
	public void loadSparseRelFile(File srsfile){
		strucEdited = false;
		try{
			datasrcfilename.setText("");
			Rel.resetTheColorCounters();
			if (myFilterRDEF.accept(srsfile)){
				RDEFReader rdefreader = new RDEFReader(this);
				rdata = rdefreader.readRDEF(srsfile.getPath(),null);
				rels = rdata.caseAt(0).inputDomain();
//				rels = new SparseRelStruc(rdata.caseAt(0).inputDomain());
				getInstFromReldata();
			}
//			if (myFilterRST.accept(srsfile)){
//				SparseRelStrucReader relreader = new SparseRelStrucReader();
//				rels = relreader.readSparseRelStrucFromFile(srsfile.getPath());
//			}
			datasrcfilename.setText(srsfile.getName());
		}catch (Exception ex){
			rels = null;
			srsfile = null;
			rstsrc.setText("");
			this.showMessage(ex.toString());
		}
		if(isEvModuleOpen)
			evidenceModule.newElementNames();
		else{
//			instasosd.clear();
			queryatoms.reset();
		}
		if(isLrnModuleOpen){
			learnModule.getRelDataFromPrimula();
		}
		if(isBavariaOpen){
			bavaria.update();
		}
		if (rbn != null) {
			rbn.updateSig(sig); // sig has been updated by rdefreader.readRDEF
		}

	}


	//loads the rbn file
	public void loadRBNFunction(File input_file){
		if(instasosd.isEmpty() && queryatoms.isEmpty()){
		
				rbn = new RBN(input_file,this.sig);
			
			rbnfile = input_file;
			rbnfilename.setText(rbnfile.getName());

			Rel.resetTheColorCounters();
			if(isEvModuleOpen)
				evidenceModule.updateRBNRelations();
		}
		else{
//			if(confirm(INST_AND_QUERIES_LOST)){
				try{
					rbn = new RBN(input_file,this.sig);
					rbnfile = input_file;
					rbnfilename.setText(rbnfile.getPath());
				}catch (Exception ex){
					rbn = null;
					rbnfile = null;
					rbnfilename.setText("");
					this.showMessage(ex.toString());
				}
				
//				Rel.resetTheColorCounters();
//				instasosd.clear();
//				queryatoms.reset();
				

				if(isEvModuleOpen)
					evidenceModule.updateRBNRelations();
				//				else{
				//					instasosd.reset();
				//					queryatoms.reset();
				//				}
//			}
//			else //replace the current text with the real filename
//				rbnfilename.setText(rbnfile.getPath());
		}
		
//		System.out.println("Init Inst.");
		instasosd.init(rbn);
		
	}


	/** @author Keith Cascio */
	public void showMessageThis(String message){
		showMessage( message );
	}

	/** @author Keith Cascio */
	public void appendMessageThis(String message){
		appendMessage( message );
	}

	/** @author Keith Cascio */
	public void setIsBavariaOpenThis(boolean b){
		setIsBavariaOpen( b );
	}

	/** @author Keith Cascio */
	public void setIsEvModuleOpenThis(boolean b){
		setIsEvModuleOpen( b );
	}

	
	//shows the messages
	public static void showMessage(String message){
		messageArea.append("\n"+message);
		messageArea.repaint((long)10.00);
	}

	// without starting a newline:
	public static void appendMessage(String message){
		messageArea.append(message);
		messageArea.repaint((long)10.00);
	}

	//sets the state of the Bavaria window
	public static void setIsBavariaOpen(boolean b){
		isBavariaOpen = b;
	}

	//	public  int adaptivemode(){
	//	return adaptivemode;
	//	}

	//sets the state of the evidence module window
	public static void setIsEvModuleOpen(boolean b){
		isEvModuleOpen = b;
	}

	//sets the state of the evidence module window
	public static void setIsLearnModuleOpen(boolean b){
		isLrnModuleOpen = b;
	}


	//sets the current rel struc
	public void setRelStruc(RelStruc srel){
		rels = srel;
		initRelData(rels);
		if(isEvModuleOpen)
			evidenceModule.newElementNames();
		instasosd.clear();
		if (rbn != null)
			instasosd.init(rbn);
		queryatoms.reset();
		if (isBavariaOpen)
			bavaria.displayRelStruc((SparseRelStruc)srel);
	}

	public void setRelData(RelData rd){
		rdata = rd;
		rels = rdata.caseAt(0).inputDomain();
	}

	public void setSignature(Signature s){
		sig=s;
		if (rbn != null)
			rbn.updateSig(sig);
	}
	
	//sets the current input file
	public void setInputFile(File inputFile){
		if(inputFile == null)
			srsfile = null;
		else
			srsfile = inputFile;
	}


	private void setBnoutfile(){
		bnetFileChooser.resetChoosableFileFilters();
		switch (bnsystem){
		case OPTION_HUGIN:
			bnetFileChooser.setFileFilter(myFilterNET);
			break;
		case OPTION_NETICA:
			bnetFileChooser.setFileFilter(myFilterDNE);
			break;
		case OPTION_JAVABAYES:
			bnetFileChooser.setFileFilter(myFilterBIF);
			break;				
		}

		int value = bnetFileChooser.showDialog(Primula.this, "Save");
		if (value == JFileChooser.APPROVE_OPTION) {
			try{
				bnoutfile = bnetFileChooser.getSelectedFile();
			}
			catch (Exception ex){ ex.printStackTrace();}
		}
	}

	//user adds or renames a node in the Bavaria window
	public void addOrRenameEvidenceModuleNode(){
		if(isEvModuleOpen)
			evidenceModule.addOrRenameElementName();
	}


	//user deletes a node in the Bavaria window
	public void deleteElementFromEvidenceModule(int node){
		if(isEvModuleOpen)
			evidenceModule.deleteElementName(node);
	}


	//ask confirmation
	public boolean confirm(String text){
		int result = JOptionPane.showConfirmDialog(this, text, "Confirmation", JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION)
			return true;
		else //result == JOptionPane.NO_OPTION
			return false;
	}

	/** @author keith cascio
	@since 20061023 */
	public OneStrucData getInstantiation(){
		return this.instasosd;
	}

	/** Returns this Instantiation as a SparseRelStruc **/
	public SparseRelStruc getInstantiationAsSRS(){
		return new SparseRelStruc(rels.getNames(),instasosd.copy(),rels.getCoords(),sig);
	}

	public String[][] getParamNumRels(){
		if (isLrnModuleOpen)
			return learnModule.getSelectedNumRels();
		else
			return new String[0][0];
	}

	/** @author keith cascio
	@since 20061023 */
	public boolean instContainsAll( OneStrucData old ){
		if( instasosd == null ) return (old == null) || old.isEmpty();
		else               return instasosd.containsAll( old );
	}

	//returns true if the instantiation is empty (used by Bavaria)
	public boolean isInstEmpty(){
		return instasosd.isEmpty();
	}

	//returns true if the atomlist is empty (used by Bavaria)
	public boolean isQueryatomsEmpty(){
		return queryatoms.isEmpty();
	}

	//user has edited the structure in the Bavaria
	/**
	 * @param b
	 * @uml.property  name="strucEdited"
	 */
	public void setStrucEdited(boolean b){
		strucEdited = b;
		if( isEvModuleOpen ) evidenceModule.relationalStructureEdited();//keith cascio 20060725
	}


	public int evidencemode(){
		return evidencemode;
	}

	/** Opens Bavaria with the current rels */
	//	public Bavaria openBavaria(){
	//		return new Bavaria(new SparseRelStruc(), Primula.this, strucEdited);
	//	}

	/**
	 * @author Alberto García Collado
	 * @param mln the file where the mln is stored
	 * @param owdb the file where the open world assuptions are declared
	 * @param cwdb the file where the close world assuptions are declared
	 */
	public void LoadMLN(File mln, File cwdb, File owdb) {
		MLNParserFacade facade = new MLNParserFacade();
		facade.ReadMLN(mln, cwdb, owdb);
		this.rbn = facade.getRBN();
		this.rels = facade.getRelStruc();
		this.instasosd = facade.getInstantiation();
		instasosd.setParentRelStruc(rels);
		rdata = new RelData(rels,instasosd);
		this.rbnfilename.setText(mln.getPath());
		if(isEvModuleOpen){
			evidenceModule.updateRBNRelations();
			evidenceModule.newElementNames();
			evidenceModule.updateInstantiationList();
		}
	}

	public void getInstFromReldata(){
		instasosd = rdata.caseAt(0).oneStrucDataAt(0);
		if (instasosd==null){
			instasosd = new OneStrucData();
			instasosd.setParentRelStruc(rels);
		}
	}

	public void updateInstantiationInEM(){
		if(isEvModuleOpen)
			evidenceModule.updateInstantiationList();
	}
	
	public void updateMessageArea(){
		messageArea.repaint();
	}

	public void updateBavaria(){
		if (isBavariaOpen)
			bavaria.update();
	}
	
	/* Initializes the RelData with rs as input domain
	 * and empty instantiations
	 */
	public void initRelData(RelStruc rs){
		instasosd = new OneStrucData();
		rdata = new RelData(rs, instasosd);
	}
	
	/* paramnames contains string representations of rbn parameters, and
	 * numerical atoms. paramvalues is an array of corresponding length.
	 * Method sets all rbn parameters to their given values in the rbn, and
	 * all the numerical atoms to their values in rels
	 */
	public void setParameters(String[] paramnames, double[] paramvalues){

		if (paramnames.length != paramvalues.length)
			System.out.println("Warning: un-matched arguments in Primula.setParameters");
		// first separate the RBN model parameters from 
		// the numerical relation parameters
		Vector<String> rbnparams = new Vector<String>();
		Vector<String> nrelparams = new Vector<String>();
		Vector<Double> rbnvalues = new Vector<Double>();
		Vector<Double> nrelvalues = new Vector<Double>();
		
	
		for (int i=0;i<paramnames.length;i++)
			if (isRBNParameter(paramnames[i])){
				rbnparams.add(paramnames[i]);
				rbnvalues.add(paramvalues[i]);
			}
			else{
				nrelparams.add(paramnames[i]);
				nrelvalues.add(paramvalues[i]);
			}
				
		// Setting the RBN parameters:
		rbn.setParameters(StringOps.stringVectorToArray(rbnparams),StringOps.doubleVectorToArray(rbnvalues));
		
		// Setting the numerical relations:
		rels.addTuples(nrelparams,nrelvalues);
	}

	public void setParameters(Hashtable<String,Integer> paramidx,double[]paramvalues) {
		for (String par: paramidx.keySet()) {
			if (isRBNParameter(par))
				rbn.setParameter(par, paramvalues[paramidx.get(par)]);
			else
				rels.addTuple(par,paramvalues[paramidx.get(par)] );
		}
	}
	
	public double[] getParameterVals(String[] paramnames) {
		double[] result = new double[paramnames.length];
		for (int i=0;i<result.length;i++) {
			if (isRBNParameter(paramnames[i]))
				result[i]=rbn.getParameterValue(paramnames[i]);
			else
				result[i]=rels.getNumAtomValue(paramnames[i]);
		}
		return result;
	}
	
	public double[] getParameterVals(Hashtable<String,Integer> paramidx) {
		double[] result = new double[paramidx.size()];
		for (String par: paramidx.keySet()) {
			if (isRBNParameter(par))
				result[paramidx.get(par)]=rbn.getParameterValue(par);
			else
				result[paramidx.get(par)]=rels.getNumAtomValue(par);
		}
		return result;
	}
	
	public Boolean isRBNParameter(String str){
		if (str.charAt(0)=='#' || str.charAt(0)=='$')
			return true;
		else
			return false;		
	}
//	/*
//	 * Looks whether Rel r is 
//	 */
//	public void updateRelProperties(Rel r){
//		
//	}

	public LearnModule openLearnModule(boolean visible){
		if(!isLrnModuleOpen){
			learnModule = new LearnModule(this,visible);
			isLrnModuleOpen = true;
		}
		return learnModule;
	}
	
	private void loadDefaults(){
		String rbninputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/Categorical/firstcat.rbn";
		String rstinputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/Categorical/inputstruc.rdef";
	
		
		
//		String rbninputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/InformationDiffusion/independent_cascade.rbn";
//		String rstinputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/InformationDiffusion/independent_cascade.rdef";
		
//		String rbninputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/Community/community_softclus_2c.rbn";
//		String rstinputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/Community/zachary.rdef";
	
	

//		String rbninputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/GNN-RBN-alpha/rbn_acr_3layers.rbn";
//		String rstinputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/GNN-RBN-alpha/alpha1-blue.rdef"; 
		
		
//		String rbninputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/Mutag/MAP/manual_no2.rbn";
//		String rstinputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/Mutag/MAP/base_class_0_n3.rdef"; 
		
		
//		String rbninputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/SWF/swf_nodefeat.rbn";
//		String rstinputfilestring = "/home/jaeger/B/Primula-Develop/New/Primula-beta/Primula3/Examples/SWF/swf_coordinates_nodefeat_learned-ods-sheet2-l47.rdef";

			
		
	
		srsfile = new File(rstinputfilestring);
		rbnfile = new File(rbninputfilestring);

		loadSparseRelFile(srsfile);
		loadRBNFunction(rbnfile);
		


	}

	
	public static void main( String[] args ){
		
		for( String arg : args ){
			if( STR_OPTION_DEBUG.equals( arg ) ) FLAG_DEBUG = true;
		}

		Primula win = new Primula();
		SamiamManager.centerWindow( win );
		win.show();
		win.loadDefaults();

	}
	


}
