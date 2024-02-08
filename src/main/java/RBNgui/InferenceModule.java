/*
 * InferenceModule.java
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

import RBNpackage.*;
import RBNinference.*;
import RBNExceptions.*;
import RBNLearning.*;
import RBNio.*;
import RBNutilities.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.util.*;
import myio.*;

import edu.ucla.belief.ui.primula.SamiamManager;
import edu.ucla.belief.ace.*;

public class InferenceModule extends JFrame implements Observer, 
ActionListener, MouseListener, Control.ACEControlListener, GradientGraphOptions{

	public static final int OPTION_SAMPLEORD_FORWARD = 0;
	public static final int OPTION_SAMPLEORD_RIPPLE = 1;
	public static final int OPTION_NOT_SAMPLE_ADAPTIVE = 0;
	public static final int OPTION_SAMPLE_ADAPTIVE = 1;

	private JTabbedPane inferencePane   = new JTabbedPane();

	/**
	 * @uml.property  name="attributesLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel attributesLabel               = new JLabel("Attributes");
	/**
	 * @uml.property  name="attributesList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList attributesList                 = new JList();
	/**
	 * @uml.property  name="attributesListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.Rel"
	 */
	private DefaultListModel attributesListModel = new DefaultListModel();
	/**
	 * keith cascio 20060511 ...
	 * @uml.property  name="attributesScrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane attributesScrollList;//     = new JScrollPane();
	/**
	 * ... keith cascio
	 * @uml.property  name="binaryLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */

	private JLabel binaryLabel                   = new JLabel("Binary relations");
	/**
	 * @uml.property  name="binaryList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList binaryList                     = new JList();
	/**
	 * @uml.property  name="binaryListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.Rel"
	 */
	private DefaultListModel binaryListModel     = new DefaultListModel();
	/**
	 * keith cascio 20060511 ...
	 * @uml.property  name="binaryScrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane binaryScrollList;//         = new JScrollPane();
	/**
	 * ... keith cascio
	 * @uml.property  name="arbitraryLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */

	private JLabel arbitraryLabel                = new JLabel("Arbitrary relations");
	/**
	 * @uml.property  name="arbitraryList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList arbitraryList                  = new JList();
	/**
	 * @uml.property  name="arbitraryListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.Rel"
	 */
	private DefaultListModel arbitraryListModel  = new DefaultListModel();
	/**
	 * keith cascio 20060511 ...
	 * @uml.property  name="arbitraryScrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane arbitraryScrollList;//      = new JScrollPane();
	/**
	 * ... keith cascio
	 * @uml.property  name="elementNamesLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */

	private JLabel elementNamesLabel               = new JLabel("Element names");
	/**
	 * @uml.property  name="elementNamesList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList elementNamesList                 = new JList();
	/**
	 * @uml.property  name="elementNamesListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	private DefaultListModel elementNamesListModel = new DefaultListModel();
	/**
	 * @uml.property  name="elementNamesScrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane elementNamesScrollList     = new JScrollPane();

	/**
	 * @uml.property  name="instantiationsLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel instantiationsLabel               = new JLabel("Instantiations");
	/**
	 * @uml.property  name="instantiationsList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList instantiationsList                 = new JList();
	/**
	 * @uml.property  name="instantiationsListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	private DefaultListModel instantiationsListModel = new DefaultListModel();
	/**
	 * @uml.property  name="instantiationsScrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane instantiationsScrollList     = new JScrollPane();

	/**
	 * @uml.property  name="queryatomsLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	//private JLabel queryatomsLabel           = new JLabel("Query atoms");
	//fields to display sample size and weight
	/**
	 * @uml.property  name="sampleSizeText"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel sampleSizeText = new JLabel("Sample Size");
	
	private JLabel mapRestartsText = new JLabel("Restarts");
	/**
	 * @uml.property  name="sampleSize"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField sampleSize = new JTextField();
	
	private JTextField mapRestarts = new JTextField();
	/**
	 * @uml.property  name="weightText"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel weightText	 = new JLabel("Weight");
	
	private JLabel mapLLText	 = new JLabel("Likelihood");
	/**
	 * @uml.property  name="weight"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField weight = new JTextField();

	private JTextField mapLL = new JTextField();
	
	/**
	 * @uml.property  name="queryatomsScrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane queryatomsScrollList = new JScrollPane();
	//den nye queryatom tabel
	/**
	 * @uml.property  name="dataModel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private QueryTableModel dataModel  = new QueryTableModel();;
	/**
	 * @uml.property  name="querytable"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTable querytable          = new JTable();
	/**
	 * @uml.property  name="trueButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton trueButton     = new JButton("True");
	/**
	 * @uml.property  name="falseButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton falseButton    = new JButton("False");
	/**
	 * @uml.property  name="queryButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton queryButton    = new JButton("Query");
	/**
	 * @uml.property  name="infoMessage"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel infoMessage     = new JLabel(" ");
	/**
	 * @uml.property  name="emptySpace"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private Border emptySpace      = BorderFactory.createEmptyBorder(10,25,5,25);
	//  final private Color backgroundColor;
	/**
	 * @uml.property  name="toggleTruthButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */


	ImageIcon toggleicon = new ImageIcon("src/main/java/Icons/toggle.png");
	private JButton  toggleTruthButton  		= new JButton(toggleicon);
	
	ImageIcon cwaicon = new ImageIcon("src/main/java/Icons/cwa.png");
	private JButton cwaButton = new JButton(cwaicon);
	
	
	ImageIcon deleteicon = new ImageIcon("src/main/java/Icons/delete.png");
	private JButton delInstButton      		= new JButton(deleteicon);
	
	ImageIcon clearicon = new ImageIcon("src/main/java/Icons/clear.png");
	private JButton delAllInstButton   		= new JButton(clearicon);
	
//	ImageIcon saveicon = new ImageIcon("./Icons/save.png");
//	private JButton saveInstButton    		= new JButton(saveicon);

//	ImageIcon loadicon = new ImageIcon("./Icons/load.png");
//	private JButton loadInstButton  		= new JButton(loadicon);

	/**
	 * @uml.property  name="delQueryAtomButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton delQueryAtomButton	  = new JButton("Delete");
	/**
	 * @uml.property  name="delAllQueryAtomButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton delAllQueryAtomButton = new JButton("Clear");

	/**
	 * @uml.property  name="sampleInfoPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel sampleInfoPanel         = new JPanel(new GridLayout(1,4,3,1));
	private JPanel mapInfoPanel         = new JPanel(new GridLayout(1,4,3,1));
	
	/**
	 * @uml.property  name="deletesamplePanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel deletesamplePanel   = new JPanel(new BorderLayout());

	/**
	 * @uml.property  name="attributesPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel attributesPanel     = new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="binaryPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel binaryPanel         = new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="arbitraryPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel arbitraryPanel      = new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="arityPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel arityPanel          = new JPanel(new GridLayout(1, 3, 0, 3));
	/**
	 * @uml.property  name="elementNamesPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel elementNamesPanel   = new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="instantiationsPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel instantiationsPanel = new JPanel(new BorderLayout());

	//  private JPanel listsPanel          = new JPanel(new GridLayout(1, 3, 10, 1));
	/**
	 * @uml.property  name="atomsPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel atomsPanel          		= new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="instButtonsPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel instButtonsPanel    		= new JPanel(new GridLayout(1, 3));
//	private JPanel truthButtonsPanel   		= new JPanel(new GridLayout(1, 2));
	/**
	 * @uml.property  name="buttonsPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel buttonsPanel        		= new JPanel(new FlowLayout());
	/**
	 * @uml.property  name="buttonsAndInfoPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel buttonsAndInfoPanel 		= new JPanel();
	
	
	/**
	 * @uml.property  name="queryatomsPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel queryatomsPanel 		 		= new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="queryatomsButtonsPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel queryatomsButtonsPanel = new JPanel(new GridLayout(1,2));
	/**
	 * @uml.property  name="qbPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel qbPanel = new JPanel(new BorderLayout());

	/**
	 * @uml.property  name="eiPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel eiPanel = new JPanel(new GridLayout(1,2));
	/**
	 * keith cascio 20060511 ...
	 * @uml.property  name="qeiPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel qeiPanel = new JPanel( new GridBagLayout() );//new GridLayout(2,1));
	/**
	 * ... keith cascio
	 * @uml.property  name="samplingPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */

	private JPanel samplingPanel = new JPanel(new GridLayout(1,4));
	/**
	 * @uml.property  name="settingsSampling"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton settingsSampling  = new JButton("Settings Sampling");
	/**
	 * @uml.property  name="startSampling"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton startSampling = new JButton("Start");
	/**
	 * @uml.property  name="pauseSampling"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton setPrediction  = new JButton("Predict");
	/**
	 * @uml.property  name="stopSampling"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton stopSampling = new JButton("Stop");
	//   private	JPanel samplingfile 			 = new JPanel(new GridLayout(2,1));

	
	private JPanel evalPanel = new JPanel(new GridLayout(1,4));
	private JButton startEval = new JButton("Test");
	
	private JPanel mapPanel = new JPanel( new GridLayout( 1, 4 ) );
	
	private JButton settingsMap = new JButton("Settings MAP");
	
	private JButton startMap = new JButton("Start");
	/**
	 * @uml.property  name="pauseSampling"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton setMapVals  = new JButton("Set MAP Vals");
	/**
	 * @uml.property  name="stopSampling"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton stopMap = new JButton("Stop");
	//   private	JPanel samplingfile 			 = new JPanel(new GridLayout(2,1));

	
	/**
	 * keith cascio 20060511 ...
	 * @uml.property  name="acePanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel  acePanel          = new JPanel( new GridLayout( 1, 4 ) );
	/**
	 * @uml.property  name="aceButtonSettings"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton aceButtonSettings = new JButton( "ACE settings" );
	//private JButton aceButtonCompile  = new JButton( "Compile" );
	//private JButton aceButtonLoad     = new JButton( "Load" );
	//private JButton aceButtonCompute  = new JButton( "Compute" );
	/**
	 * @uml.property  name="aceProgressBar"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JProgressBar aceProgressBar;

	/**
	 * @uml.property  name="myACESettingsPanel"
	 * @uml.associationEnd  
	 */
	private SettingsPanel                myACESettingsPanel;
	/**
	 * @uml.property  name="myACEControl"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private Control                      myACEControl;
	/**
	 * ... keith cascio
	 * @uml.property  name="first_bin"
	 */



	private boolean first_bin = true;  //user has selected the first element
	/**
	 * @uml.property  name="first_arb"
	 */
	private boolean first_arb = true;
	/**
	 * @uml.property  name="firstbinarystar"
	 */
	private boolean firstbinarystar = false;
	/**
	 * @uml.property  name="tuple" multiplicity="(0 -1)" dimension="1"
	 */
	private int[] tuple = new int[1];
	/**
	 * @uml.property  name="index"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	private int index;
	/**
	 * @uml.property  name="aritynumber"
	 */
	private int aritynumber;
	/**
	 * @uml.property  name="addedTuples"
	 */
	private String addedTuples = "";

	/**
	 * @uml.property  name="myprimula"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="evidenceModule:RBNgui.Primula"
	 */
	private Primula myprimula;

	/**
	 * @uml.property  name="instasosd"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private OneStrucData inst;
	/**
	 * @uml.property  name="queryatoms"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private GroundAtomList queryatoms;
	/**
	 * @uml.property  name="rel"
	 * @uml.associationEnd  
	 */
	private BoolRel rel;
	
	
	/* The gradient graph structure constructed in current inference 
	 * process
	 */
	private GradientGraph currentGG;
	
	/**
	 * @uml.property  name="selectedInstAtom"
	 * @uml.associationEnd  
	 */
	private InstAtom selectedInstAtom;
	/**
	 * @uml.property  name="selectedQueryAtom"
	 * @uml.associationEnd  
	 */
	private GroundAtom selectedQueryAtom;
	/**
	 * @uml.property  name="truthValue"
	 */
	private boolean truthValue = true;
	/**
	 * @uml.property  name="queryModeOn"
	 */
	private boolean queryModeOn = false;
	/**
	 * @uml.property  name="delAtom"
	 */
	private int delAtom;
	/**
	 * @uml.property  name="sampthr"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.Atom"
	 */
	private SampleThread sampthr;
	
	private MapThread mapthr;
	/**
	 * @uml.property  name="sampling"
	 */
	private boolean sampling;
	
	//private boolean maprestarts;
	/**
	 * @uml.property  name="pausemcmc"
	 */
	private boolean pausemcmc = false;
	/**
	 * @uml.property  name="evi"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	//private InferenceModule evi = this;
	
	/**
	 * @uml.property  name="savefile"
	 */
	private File savefile;
	/**
	 * @uml.property  name="fileChooser"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JFileChooser fileChooser = new JFileChooser( "." );
	/**
	 * @uml.property  name="myFilterRDEF"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private javax.swing.filechooser.FileFilter myFilterRDEF;
	


	/**
	 * @uml.property  name="instantiations" multiplicity="(0 -1)" dimension="2"
	 */
//	private int [][] instantiations;


	/* in adaptive sampling and for the query nodes the samples are assigned in a cyclic
	 * fashion to num_subsamples_adapt, resp. num_subsamples_minmax.
	 * For adaptive sampling: Variance of sampleweights in
	 * the different subsamples is used to determine
	 * the weight with which the current estimated probabilities
	 * are used for the sampling probabilities
	 * For querynodes: variance (and max/min values) in the
	 * different subsamples is displayed to provide some error estimate
	 *
	 */
	/**
	 * @uml.property  name="adaptivemode"
	 */
	private int adaptivemode;
	/**
	 * @uml.property  name="sampleordmode"
	 */
	private int sampleordmode;
	/**
	 * @uml.property  name="cptparents"
	 */
	private int cptparents = 3; // Max. number of parents for nodes with standard cpt
	/**
	 * @uml.property  name="num_subsamples_minmax"
	 */
	private int num_subsamples_minmax = 10;
	/**
	 * @uml.property  name="num_subsamples_adapt"
	 */
	private int num_subsamples_adapt = 10;

	
	/* Options for MAP inference */
	
	private int windowsize;
	private int numchains;
	private int numrestarts;
	private boolean ggverbose;
	private int maxfails;
	
	/**
	 * @uml.property  name="samplelogmode" multiplicity="(0 -1)" dimension="1"
	 */
	private boolean[] samplelogmode = new boolean[5];
	/* True components of samplelogmode determine what is to be logged:
	 * [0]: Sampling order
	 * [1]: Current Evidence
	 * [2]: Compact Trace
	 * [3]: Full Trace (only one of [2] or [3] can be true)
	 * [4]: Network statistics
	 */

	/**
	 * @uml.property  name="settingssamplingwindowopen"
	 */
	private boolean settingssamplingwindowopen = false;
	private boolean settingsmapwindowopen = false;
	
	/**
	 * @uml.property  name="swindow"
	 * @uml.associationEnd  inverse="evidence:RBNgui.SettingsSampling"
	 */
	private RBNgui.SettingsSampling swindow;
	private RBNgui.SettingsMAP mapwindow;
	
	/**
	 * @uml.property  name="logwriter"
	 */
	private BufferedWriter logwriter = null;
	/**
	 * @uml.property  name="logfilename"
	 */
	private String logfilename = "";

	private String modelPath;
	private String scriptPath;
	private String scriptName;
	private String pythonHome;

	public InferenceModule( Primula myprimula_param ){

		myprimula = myprimula_param;
		sampling = false;
//		maprestarts = false;
		inst = myprimula.instasosd;
		queryatoms = myprimula.queryatoms;
		sampleordmode = OPTION_SAMPLEORD_FORWARD;
		adaptivemode = OPTION_NOT_SAMPLE_ADAPTIVE;
		for (int i=0;i<samplelogmode.length;i++)
			samplelogmode[i]=false;

		numchains = 2;
		windowsize = 2;
		numrestarts = 10;
		
		readElementNames();
		readRBNRelations();

		updateInstantiationList();
		updateQueryatomsList();


		
		/* Top panel with list of attributes/binary relations/arbitrary relations */
		attributesList.setModel(attributesListModel);
		attributesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		/** keith cascio 20060511 ... */
		attributesScrollList = new JScrollPane( attributesList );//attributesScrollList.getViewport().add(attributesList);
		Dimension sizePreferred = attributesScrollList.getPreferredSize();
		sizePreferred.height = 64;
		attributesScrollList.setPreferredSize( sizePreferred );
		/** ... keith cascio */
		attributesPanel.add(attributesLabel, BorderLayout.NORTH);
		attributesPanel.add(attributesScrollList, BorderLayout.CENTER);

		binaryList.setModel(binaryListModel);
		binaryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		/** keith cascio 20060511 ... */
		binaryScrollList = new JScrollPane( binaryList );//binaryScrollList.getViewport().add(binaryList);
		binaryScrollList.setPreferredSize( sizePreferred );
		/** ... keith cascio */
		binaryPanel.add(binaryLabel, BorderLayout.NORTH);
		binaryPanel.add(binaryScrollList, BorderLayout.CENTER);

		arbitraryList.setModel(arbitraryListModel);
		arbitraryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		/** keith cascio 20060511 ... */
		arbitraryScrollList = new JScrollPane( arbitraryList );//arbitraryScrollList.getViewport().add(arbitraryList);
		arbitraryScrollList.setPreferredSize( sizePreferred );
		/** ... keith cascio */
		arbitraryPanel.add(arbitraryLabel, BorderLayout.NORTH);
		arbitraryPanel.add(arbitraryScrollList, BorderLayout.CENTER);

		arityPanel.add(attributesPanel);
		arityPanel.add(binaryPanel);
		arityPanel.add(arbitraryPanel);
		/* ************************************ */

		
		elementNamesList.setModel(elementNamesListModel);
		elementNamesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		elementNamesScrollList.getViewport().add(elementNamesList);
		elementNamesPanel.add(elementNamesLabel, BorderLayout.NORTH);
		elementNamesPanel.add(elementNamesScrollList, BorderLayout.CENTER);
		eiPanel.add(elementNamesPanel);
		
		instantiationsList.setModel(instantiationsListModel);
		instantiationsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		instantiationsScrollList.getViewport().add(instantiationsList);
		instantiationsPanel.add(instantiationsLabel, BorderLayout.NORTH);
		instantiationsPanel.add(instantiationsScrollList, BorderLayout.CENTER);
		instButtonsPanel.add(toggleTruthButton);
		toggleTruthButton.setToolTipText("Toggle truth value of selected atom");
		instButtonsPanel.add(cwaButton);
		cwaButton.setToolTipText("Apply closed-world assumption to selected relation");
		instButtonsPanel.add(delInstButton);
		delInstButton.setToolTipText("Delete selected atom");
		instButtonsPanel.add(delAllInstButton);
		delAllInstButton.setToolTipText("Clear all evidence");
//		instButtonsPanel.add(saveInstButton);
//		saveInstButton.setToolTipText("Save instantiations to file");
//		instButtonsPanel.add(loadInstButton);
//		loadInstButton.setToolTipText("Load instantiations and input domain from file");

		instantiationsPanel.add(instButtonsPanel, BorderLayout.SOUTH);
		


		sampleInfoPanel.add(sampleSizeText);
		sampleSizeText.setHorizontalAlignment( JLabel.RIGHT );
		sampleInfoPanel.add(sampleSize);
		sampleSize.setHorizontalAlignment( JTextField.LEFT );
		sampleSize.setEditable(false);
		sampleSize.setBackground(new Color(255, 255, 255));
		sampleInfoPanel.add(weightText);
		weightText.setHorizontalAlignment( JLabel.RIGHT );
		sampleInfoPanel.add(weight);
		weight.setEditable(false);
		weight.setHorizontalAlignment( JTextField.LEFT );
		weight.setBackground(new Color(255, 255, 255));

		mapInfoPanel.add(mapRestartsText);
		mapRestartsText.setHorizontalAlignment( JLabel.RIGHT );
		mapInfoPanel.add(mapRestarts);
		mapRestarts.setHorizontalAlignment( JTextField.LEFT );
		mapRestarts.setEditable(false);
		mapRestarts.setBackground(new Color(255, 255, 255));
		mapInfoPanel.add(mapLLText);
		mapLLText.setHorizontalAlignment( JLabel.RIGHT );
		mapInfoPanel.add(mapLL);
		mapLL.setEditable(false);
		mapLL.setHorizontalAlignment( JTextField.LEFT );
		mapLL.setBackground(new Color(255, 255, 255));
		
		/* Panel for Importance Sampling */
		JPanel panelSamplingOuter = new JPanel( new GridBagLayout() );
		GridBagConstraints cSampling = new GridBagConstraints();

		cSampling.fill      = GridBagConstraints.BOTH;
		cSampling.gridwidth = GridBagConstraints.REMAINDER;
		cSampling.weightx   = cSampling.weighty = 1;
		panelSamplingOuter.add( sampleInfoPanel,                  cSampling );
		panelSamplingOuter.add( Box.createVerticalStrut( 8 ), cSampling );
		panelSamplingOuter.add( samplingPanel,                cSampling );
		panelSamplingOuter.setBorder(BorderFactory.createTitledBorder("Importance Sampling"));
		
		inferencePane.add("MCMC", panelSamplingOuter);
		/* ************************************ */

		/* Panel for Test set evaluation */
		JPanel panelEvalOuter = new JPanel( new GridBagLayout() );
		
		GridBagConstraints cEval = new GridBagConstraints();

		cSampling.fill      = GridBagConstraints.BOTH;
		cSampling.gridwidth = GridBagConstraints.REMAINDER;
		cSampling.weightx   = cSampling.weighty = 1;
		panelEvalOuter.add( evalPanel,                cSampling );
		panelEvalOuter.setBorder(BorderFactory.createTitledBorder("Test Set Evaluation"));
		
		inferencePane.add("Test", panelEvalOuter);
		/* ************************************ */
		
		
		/* Panel for Map inference */
		JPanel panelMapOuter = new JPanel( new GridBagLayout() );
		GridBagConstraints cMap = new GridBagConstraints();

		cMap.fill      = GridBagConstraints.BOTH;
		cMap.gridwidth = GridBagConstraints.REMAINDER;
		cMap.weightx   = cSampling.weighty = 1;
		panelMapOuter.add(mapInfoPanel, cMap);
		panelMapOuter.add( Box.createVerticalStrut( 8 ), cSampling );
		panelMapOuter.add( mapPanel,                  cMap);
		panelMapOuter.setBorder(BorderFactory.createTitledBorder("MAP Inference"));
		
		inferencePane.add("MAP", panelMapOuter);
		/* ************************************ */


		/* Panel with buttons underneath querytable */
		queryatomsButtonsPanel.add(delQueryAtomButton);
		delQueryAtomButton.setToolTipText("Delete query atom");
		queryatomsButtonsPanel.add(delAllQueryAtomButton);
		delAllQueryAtomButton.setToolTipText("Delete all query atoms");
		deletesamplePanel.add(queryatomsButtonsPanel, BorderLayout.NORTH);
		/* ************************************ */

		/* Setting up the Query table */
		querytable.setModel(dataModel);
		querytable.setShowHorizontalLines(false);
		querytable.setPreferredScrollableViewportSize(new Dimension(146, 100));
		//table header values
		querytable.getColumnModel().getColumn(0).setHeaderValue("Query Atoms");
		querytable.getColumnModel().getColumn(1).setHeaderValue("MAP");
		querytable.getColumnModel().getColumn(2).setHeaderValue("P");
		querytable.getColumnModel().getColumn(3).setHeaderValue("Min");
		querytable.getColumnModel().getColumn(4).setHeaderValue("Max");
		querytable.getColumnModel().getColumn(5).setHeaderValue("Var");
		/** keith cascio 20060511 ... */
		querytable.getColumnModel().getColumn(6).setHeaderValue( edu.ucla.belief.ace.Settings.STR_ACE_DISPLAY_NAME );
		/** ... keith cascio */

		querytable.getColumnModel().getColumn(0).setPreferredWidth(150);
		/* ************************************ */
		
		
		/* Panel consisting of query table and buttons underneath */
		queryatomsScrollList.getViewport().add(querytable);
		//queryatomsPanel.add(queryatomsLabel, BorderLayout.NORTH);
		queryatomsPanel.add(queryatomsScrollList, BorderLayout.CENTER);
		queryatomsPanel.add(deletesamplePanel, BorderLayout.SOUTH);
		/* ************************************ */
		
//		atomsPanel.add(instantiationsPanel, BorderLayout.CENTER);
//		atomsPanel.add(queryatomsPanel, BorderLayout.SOUTH);
		
		//MouseListeners
		attributesList.	addMouseListener( this );
		binaryList.addMouseListener( this );
		arbitraryList.addMouseListener( this );
		elementNamesList.addMouseListener( this );
		instantiationsList.addMouseListener( this );
		querytable.addMouseListener( this);
		//ActionListerners
		trueButton.addActionListener( this );
		falseButton.addActionListener( this );
		queryButton.addActionListener( this );
		toggleTruthButton.addActionListener( this );
		cwaButton.addActionListener( this );
		delInstButton.addActionListener( this );
		delAllInstButton.addActionListener( this );
//		saveInstButton.addActionListener( this );
//		loadInstButton.addActionListener( this );
		delQueryAtomButton.addActionListener( this );
		delAllQueryAtomButton.addActionListener(this);
		settingsSampling.addActionListener( this );
		startSampling.addActionListener( this );
		setPrediction.addActionListener( this );
		stopSampling.addActionListener( this );
		startEval.addActionListener( this );
		settingsMap.addActionListener( this );
		startMap.addActionListener( this );
		setMapVals.addActionListener( this );
		stopMap.addActionListener( this );
		//setting background color
		trueButton.setBackground(Primula.COLOR_BLUE_SELECTED);
		trueButton.setToolTipText("Add atoms instantiated to true");
		falseButton.setBackground(Primula.COLOR_BLUE);
		falseButton.setToolTipText("Add atoms instantiated to false");
		queryButton.setBackground(Primula.COLOR_BLUE);
		queryButton.setToolTipText("Add atoms to query list");

		toggleTruthButton.setBackground(Primula.COLOR_YELLOW);
		cwaButton.setBackground(Primula.COLOR_YELLOW);
		delInstButton.setBackground(Primula.COLOR_YELLOW);
		delAllInstButton.setBackground(Primula.COLOR_YELLOW);
//		saveInstButton.setBackground(Primula.COLOR_RED);
//		loadInstButton.setBackground(Primula.COLOR_RED);
		delQueryAtomButton.setBackground(Primula.COLOR_YELLOW);
		delAllQueryAtomButton.setBackground(Primula.COLOR_YELLOW);
		settingsSampling.setBackground(Primula.COLOR_BLUE);
		startSampling.setBackground(Primula.COLOR_GREEN);
		stopSampling.setBackground(Primula.COLOR_GREEN);
		setPrediction.setBackground(Primula.COLOR_GREEN);
		startEval.setBackground(Primula.COLOR_GREEN);
		settingsMap.setBackground(Primula.COLOR_BLUE);
		startMap.setBackground(Primula.COLOR_GREEN);
		stopMap.setBackground(Primula.COLOR_GREEN);
		setMapVals.setBackground(Primula.COLOR_GREEN);

		//    backgroundColor = falseButton.getBackground();


		
		

		/** keith cascio 20060511 ... */
		//samplingPanel.setBorder(BorderFactory.createTitledBorder("Sampling"));
		/** ... keith cascio */
		samplingPanel.add(settingsSampling);
		samplingPanel.add(startSampling);
		startSampling.setToolTipText("Start sampling");
		samplingPanel.add(stopSampling);
		stopSampling.setToolTipText("Stop sampling");
		samplingPanel.add(setPrediction);
		setPrediction.setToolTipText("Set predicted values as evidence");

		evalPanel.add(startEval);
		
		mapPanel.add(settingsMap);
		mapPanel.add(startMap);
		startMap.setToolTipText("Start map");
		mapPanel.add(setMapVals);
		setMapVals.setToolTipText("Add current MAP values to instantiation");
		mapPanel.add(stopMap);
		stopMap.setToolTipText("Stop map");

		
		
		/** keith cascio 20060511 ... */
		//acePanel.setBorder( BorderFactory.createTitledBorder( edu.ucla.belief.ace.Settings.STR_ACE_DISPLAY_NAME ) );
		acePanel.add( aceButtonSettings );
		JButton[] buttons = new JButton[] {
				//new JButton( getACEControl().getActionCompile()     ),
				//new JButton( getACEControl().getActionLoad()        ),
				//new JButton( getACEControl().getActionCompute()     ),
				new JButton( getACEControl().getActionFastForward() ) };

		for( JButton button : buttons ){
			if( buttons.length == 1 ){
				for( int i=0; i<2; i++ ) acePanel.add( Box.createHorizontalStrut(8) );
			}
			acePanel.add( button );
			button.setBackground( Primula.COLOR_GREEN );
		}

		aceButtonSettings.addActionListener( this );
		aceButtonSettings.setBackground( Primula.COLOR_BLUE );
		aceButtonSettings.setToolTipText( edu.ucla.belief.ace.Settings.STR_ACE_DISPLAY_NAME + ", settings" );

		JPanel pnlAceOuter = new JPanel( new GridBagLayout() );
		pnlAceOuter.setBorder( BorderFactory.createTitledBorder( edu.ucla.belief.ace.Settings.STR_ACE_DISPLAY_NAME ) );
		GridBagConstraints cAceOuter = new GridBagConstraints();

		cAceOuter.fill      = GridBagConstraints.BOTH;
		cAceOuter.gridwidth = GridBagConstraints.REMAINDER;
		cAceOuter.weightx   = cAceOuter.weighty = 1;
		pnlAceOuter.add( acePanel,                            cAceOuter );
		pnlAceOuter.add( Box.createVerticalStrut( 4 ),        cAceOuter );

		cAceOuter.gridwidth = 1;
		pnlAceOuter.add( getACEProgressBar(), cAceOuter );
		cAceOuter.fill      = GridBagConstraints.NONE;
		cAceOuter.weightx   = cAceOuter.weighty = 0;
		pnlAceOuter.add( Box.createHorizontalStrut( 4 ),      cAceOuter );

		cAceOuter.gridwidth = GridBagConstraints.REMAINDER;
		JButton btn = new JButton( getACEControl().getActionCancel() );
		pnlAceOuter.add( btn,                                 cAceOuter );
		btn.setMargin(     new Insets(0,0,0,0) );
		btn.setBackground( Primula.COLOR_GREEN.brighter() );
		btn.setFont( btn.getFont().deriveFont( (float)(btn.getFont().getSize() - 1) ) );
		pnlAceOuter.add( Box.createVerticalStrut( 4 ),        cAceOuter );

		inferencePane.add("ACE", pnlAceOuter);
		
		JPanel pnlInferenceAlternatives = new JPanel( new GridBagLayout() );
		GridBagConstraints cAlternatives = new GridBagConstraints();

		//cAlternatives.fill      = GridBagConstraints.BOTH;
		//cAlternatives.gridwidth = GridBagConstraints.REMAINDER;
		//cAlternatives.weightx   = cAlternatives.weighty = 1;
		//pnlInferenceAlternatives.add( Box.createVerticalStrut( 8 ), cAlternatives );
		//pnlInferenceAlternatives.add( panelSamplingOuter,           cAlternatives );
		//pnlInferenceAlternatives.add( panelMapOuter,           cAlternatives );
		//pnlInferenceAlternatives.add( pnlAceOuter,                  cAlternatives );

		//resetACEEnabledState( getACEControl() );
		/** ... keith cascio */

		buttonsPanel.add(trueButton);
		buttonsPanel.add(falseButton);
		buttonsPanel.add(queryButton);
		
		eiPanel.add(instantiationsPanel);

		/** keith cascio 20060511 ... */
//		GridBagConstraints cQEI = new GridBagConstraints();
//		cQEI.fill      = GridBagConstraints.BOTH;
//		cQEI.gridwidth = GridBagConstraints.REMAINDER;
//		cQEI.weightx   = cQEI.weighty = 1;
		JSplitPane qeiSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,arityPanel,eiPanel);
		//qeiPanel.add(qeiSplit);
		/** ... keith cascio */

		//qbPanel.add(queryatomsPanel ,BorderLayout.CENTER);
		/** keith cascio 20060511 ... */
		//qbPanel.add( pnlInferenceAlternatives, BorderLayout.SOUTH );
		//qbPanel.add(inferencePane, BorderLayout.SOUTH );
		/** ... keith cascio */

		buttonsAndInfoPanel.setLayout(new BoxLayout(buttonsAndInfoPanel,BoxLayout.Y_AXIS));
		qeiSplit.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonsAndInfoPanel.add(qeiSplit);
		buttonsAndInfoPanel.add(buttonsPanel);

		//Creates the main layout
		Container contentPane = this.getContentPane();
		//JPanel lowerPanel = new JPanel(new BorderLayout());
		//JSplitPane lowerPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,queryatomsPanel,inferencePane);
		//lowerPanel.add(queryatomsPanel,BorderLayout.NORTH);
		//lowerPanel.add(qbPanel, BorderLayout.CENTER );
		//lowerPanel.add(inferencePane, BorderLayout.CENTER );
		//lowerPanel.add(infoMessage, BorderLayout.SOUTH );
		JSplitPane querySampleSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,buttonsAndInfoPanel,queryatomsPanel);
		//contentPane.setLayout(new BorderLayout());
		contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));
		//contentPane.add(querySampleSplit);
		querySampleSplit.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPane.add(querySampleSplit);		
		inferencePane.setAlignmentX(Component.CENTER_ALIGNMENT);
		infoMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPane.add(inferencePane);
		contentPane.add(infoMessage);
		
		//Inner class for closing the window
		this.addWindowListener(
				new WindowAdapter(){
					public void windowClosing(WindowEvent e){
						if (settingssamplingwindowopen)
							swindow.dispose();
						if (settingsmapwindowopen)
							mapwindow.dispose();
						dispose();
						Primula.setIsEvModuleOpen(false);
					}
				}
		);

		fileChooser.addChoosableFileFilter(myFilterRDEF = new Filter_rdef());
		fileChooser.setFileFilter( myFilterRDEF );
		
		ImageIcon icon = myprimula.getIcon( Primula.STR_FILENAME_LOGO );
		if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) //image ok
			this.setIconImage(icon.getImage());
//		ImageIcon icon = new ImageIcon("small_logo.jpg");
//		if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
//			this.setIconImage(icon.getImage());		//image ok
//		}
		this.setTitle("Inference Module");
		this.setSize(600, 680);
		/** keith cascio 20060511 ... */
		SamiamManager.centerWindow( this );
		/** ... keith cascio */
		this.setVisible(false);

		this.setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
		this.setModelPath("/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python/primula-gnn");
		this.setScriptPath("/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python");
		this.setScriptName("inference_test");
	}

	public void setVisibility(boolean visibility) {
		this.setVisible(visibility);
	}

	public void actionPerformed( ActionEvent e )
	{
		Object source = e.getSource();

		if( source == trueButton ){
			first_bin = first_arb = true;
			trueButton.setBackground(Primula.COLOR_BLUE_SELECTED);
			falseButton.setBackground(Primula.COLOR_BLUE);
			queryButton.setBackground(Primula.COLOR_BLUE);
			elementNamesList.clearSelection();
			truthValue = true;
			queryModeOn = false;
			infoMessage.setText(" ");
		}
		else if( source == falseButton ){
			first_bin = first_arb = true;
			falseButton.setBackground(Primula.COLOR_BLUE_SELECTED);
			trueButton.setBackground(Primula.COLOR_BLUE);
			queryButton.setBackground(Primula.COLOR_BLUE);
			elementNamesList.clearSelection();
			truthValue = false;
			queryModeOn = false;
			infoMessage.setText(" ");
		}
		else if( source == queryButton ){
			first_bin = first_arb = true;
			queryButton.setBackground(Primula.COLOR_BLUE_SELECTED);
			trueButton.setBackground(Primula.COLOR_BLUE);
			falseButton.setBackground(Primula.COLOR_BLUE);
			elementNamesList.clearSelection();
			queryModeOn = true;
			infoMessage.setText(" ");
		}
		else if( source == toggleTruthButton ){
			if(selectedInstAtom != null){
				if(selectedInstAtom.truthval == true){
					inst.add((BoolRel)selectedInstAtom.rel, selectedInstAtom.args, false,"?");
				}
				else{
					inst.add((BoolRel)selectedInstAtom.rel, selectedInstAtom.args, true,"?");
				}
				updateInstantiationList();
				myprimula.updateBavaria();
			}
		}
		else if( source == cwaButton ){
			if (rel != null){
				inst.applyCWA(rel);
				updateInstantiationList();
				myprimula.updateBavaria();
			}
			//xxx
		}
		else if( source == delInstButton){
			if(selectedInstAtom != null){
				int selected = instantiationsList.getSelectedIndex();
				inst.delete(selectedInstAtom.rel, selectedInstAtom.args);
				updateInstantiationList();
				int listsize = instantiationsList.getModel().getSize()-1;

				if(selected >= listsize ){
					selected--;
				}
				if(selected != -1){
					instantiationsList.setSelectedIndex(selected);
					Vector instAtoms = inst.allInstAtoms();
					selectedInstAtom = (InstAtom) instAtoms.elementAt(selected);
				}
				else selectedInstAtom = null;
			}
		}
		else if( source == delAllInstButton ){
			inst.clear();
			updateInstantiationList();
			myprimula.updateBavaria();
		}

		else if( source == delQueryAtomButton ){
			if(selectedQueryAtom != null){
				dataModel.removeQuery(delAtom);
				generateQueryatoms();
				updateQueryatomsList();
				Vector queries = queryatoms.allAtoms();
				int listsize = queries.size()-1;
				if( delAtom >= listsize ){
					delAtom--;
				}
				if(delAtom != -1){
					delAtom--;
					if( delAtom == -1 ){
						delAtom++;
						querytable.setRowSelectionInterval(delAtom, delAtom);
						selectedQueryAtom = (GroundAtom)queries.elementAt(delAtom );
					}
					else{
						querytable.setRowSelectionInterval(delAtom, delAtom);
						selectedQueryAtom = (GroundAtom)queries.elementAt(delAtom );
					}
				}
			}
		}
		else if(source == delAllQueryAtomButton){
			dataModel.removeAllQueries();
			generateQueryatoms();
			updateQueryatomsList();
		}
		else if( source == settingsSampling ){
			if (!settingssamplingwindowopen){
				swindow = new RBNgui.SettingsSampling( InferenceModule.this );
				settingssamplingwindowopen = true;
			}
		}
		else if( source == startSampling ){
			startSampleThread();
		}
		else if( source == setPrediction){
			// mostly copy from (source == setMapVals)
			LinkedList<String> probvals = dataModel.getProbabilities();
			LinkedList<String> queryats = dataModel.getQuery();
			OneStrucData result = new OneStrucData();
			result.setParentRelStruc(myprimula.getRels());
			
			Iterator<String> itq = queryats.iterator();
			
			for (Iterator<String> itprob = probvals.iterator(); itprob.hasNext();) {
				double p = Double.parseDouble(itprob.next());
				if (p>=0.5)
					result.add(new GroundAtom(itq.next(),myprimula.getRels(),Rel.BOOLEAN),true,"?");
				else
					result.add(new GroundAtom(itq.next(),myprimula.getRels(),Rel.BOOLEAN),false,"?");
			}
			inst.add(result);
			updateInstantiationList();
			myprimula.updateBavaria();
		}
		else if( source == stopSampling){
			sampling = false;
			sampthr.setRunning(false);
			if (!noLog()){
				try{
					logwriter.flush();
					if (logfilename != "")
						logwriter.close();
				}
				catch (java.io.IOException ex){System.err.println(ex);};
			}
			infoMessage.setText(" Stop Sampling ");
			pausemcmc = false;
			startSampling.setEnabled( true );
			trueButton.setEnabled( true );
			falseButton.setEnabled( true );
			queryButton.setEnabled( true );
			toggleTruthButton.setEnabled( true );
			delInstButton.setEnabled( true );
			delAllInstButton.setEnabled( true );
			delQueryAtomButton.setEnabled( true );
			delAllQueryAtomButton.setEnabled( true );
		}
		else if (source == startEval){
			//computeQueryBatch();
			evaluateAccuracy();
		}
		else if (source == startMap){
			currentGG = startMapThread();
		}
		else if (source == setMapVals){
			if (currentGG != null){
				LinkedList<String> mapvals = dataModel.getMapValues();
				LinkedList<String> queryats = dataModel.getQuery();
				OneStrucData result = new OneStrucData();
				result.setParentRelStruc(myprimula.getRels());
				
				Iterator<String> itq = queryats.iterator();
				
				for (Iterator<String> itmap = mapvals.iterator(); itmap.hasNext();) {
					//System.out.println(itq.next() + " " + itmap.next());
					result.add(new GroundAtom(itq.next(),myprimula.getRels(),Rel.BOOLEAN),Integer.parseInt(itmap.next()),"?");
				}
				inst.add(result);
				updateInstantiationList();
				myprimula.updateBavaria();
			}
			else System.out.println("Do not have GradientGraph defining Map values!");
		}
		else if( source == stopMap){
//			maprestarts = false;
			mapthr.setRunning(false);
			infoMessage.setText(" Stop MAP ");
			startSampling.setEnabled( true );
			trueButton.setEnabled( true );
			falseButton.setEnabled( true );
			queryButton.setEnabled( true );
			toggleTruthButton.setEnabled( true );
			delInstButton.setEnabled( true );
			delAllInstButton.setEnabled( true );
			delQueryAtomButton.setEnabled( true );
			delAllQueryAtomButton.setEnabled( true );
		}
		else if( source == settingsMap ){
			if (!settingsmapwindowopen){
				mapwindow = new RBNgui.SettingsMAP( InferenceModule.this );
				settingsmapwindowopen = true;
			}
		}
		/** keith cascio 20060511 ... */
		else if( source == aceButtonSettings ) doAceSettings();
		/** ... keith cascio */
	}

	// set to public for experiments
	public void startSampleThread(){
		sampling = true;
		PFNetwork pfn = null;
		if (!noLog()){
			if (logfilename != "")
				logwriter = myio.FileIO.openOutputFile(logfilename);
			else logwriter = new BufferedWriter(new OutputStreamWriter(System.out));

		}

		try{
			BayesConstructor constructor = null;
			constructor = new BayesConstructor(myprimula.rbn,myprimula.rels,inst,queryatoms,myprimula);
			pfn = constructor.constructPFNetwork(myprimula.evidencemode,
					Primula.OPTION_QUERY_SPECIFIC,
					myprimula.isolatedzeronodesmode);
			pfn.prepareForSampling(sampleordmode,
					adaptivemode,
					samplelogmode,
					cptparents,
					queryatoms,
					num_subsamples_minmax,
					num_subsamples_adapt,
					logwriter);
		}
		catch(RBNCompatibilityException ex){System.out.println(ex.toString());}
		catch(RBNIllegalArgumentException ex){System.out.println(ex.toString());}
		catch(RBNCyclicException ex){System.out.println(ex.toString());}
		catch (RBNInconsistentEvidenceException ex){System.out.println("Inconsistent Evidence");}
		catch (IOException ex){System.out.println(ex.toString());}


		sampthr = new SampleThread(this, 
				pfn, 
				queryatoms,
				num_subsamples_minmax,
				samplelogmode,
				logwriter);
		if (sampthr.isGnnIntegration()) {
			sampthr.setPythonHome(this.pythonHome);
			sampthr.setModelPath(this.modelPath);
			sampthr.setScriptPath(this.scriptPath);
			sampthr.setScriptName(this.scriptName);
		}
		sampthr.start();
		infoMessage.setText(" Starting Sampling ");
		startSampling.setEnabled( false );
		trueButton.setEnabled( false );
		falseButton.setEnabled( false );
		queryButton.setEnabled( false );
		toggleTruthButton.setEnabled( false );
		delInstButton.setEnabled( false );
		delAllInstButton.setEnabled( false );
		delQueryAtomButton.setEnabled( false );
		delAllQueryAtomButton.setEnabled( false );
	}

	public void setQueryAtoms(GroundAtomList atomsList) {
		this.queryatoms = atomsList;
	}

	public void stopSampleThread(){
		sampling = false;
		sampthr.setRunning(false);
		if (!noLog()){
			try{
				logwriter.flush();
				if (logfilename != "")
					logwriter.close();
			}
			catch (java.io.IOException ex){System.err.println(ex);};
		}
		infoMessage.setText(" Stop Sampling ");
		pausemcmc = false;
		startSampling.setEnabled( true );
		trueButton.setEnabled( true );
		falseButton.setEnabled( true );
		queryButton.setEnabled( true );
		toggleTruthButton.setEnabled( true );
		delInstButton.setEnabled( true );
		delAllInstButton.setEnabled( true );
		delQueryAtomButton.setEnabled( true );
		delAllQueryAtomButton.setEnabled( true );
	}

	public SampleThread getSampthr() {
		return sampthr;
	}

	// set to public for experiments
	public GradientGraph startMapThread(){
		
		GradientGraph gg = null;
		try{
//			maprestarts = true;
			RelData evidence = new RelData(myprimula.getRels(),myprimula.getInstantiation());
			int mode;
			String[] rbnparams = myprimula.getRBN().parameters();
			Hashtable<String,Integer> rbnparamidx = new Hashtable<String,Integer>();
			for (int i=0;i<rbnparams.length;i++)
				rbnparamidx.put(rbnparams[i], i);
			
			if (rbnparams.length >0)
				mode = GradientGraphO.LEARNANDMAPMODE;
			else 
				mode = GradientGraphO.MAPMODE;
			
//			public GradientGraphO(Primula mypr, 
//					RelData data, 
//					Hashtable<String,Integer> params,
//					GradientGraphOptions go, 
//					GroundAtomList maxats, 
//					int m,
//					int obj,
//					Boolean showInfoInPrimula)
			
			gg = new GradientGraphO(myprimula, 
					 								evidence, 
					 								rbnparamidx,
					 								this ,
					 								queryatoms,
					 								mode,
					 								0,
					 								true);
			mapthr = new MapThread(this,myprimula,(GradientGraphO)gg);
			if (mapthr.isGnnIntegration()) {
				mapthr.setPythonHome(this.pythonHome);
				mapthr.setModelPath(this.modelPath);
				mapthr.setScriptPath(this.scriptPath);
				mapthr.setScriptName(this.scriptName);
				((GradientGraphO) gg).setGnnPy(mapthr.getGnnPy());
			}
			mapthr.start();
			trueButton.setEnabled( false );
			falseButton.setEnabled( false );
			queryButton.setEnabled( false );
			toggleTruthButton.setEnabled( false );
			delInstButton.setEnabled( false );
			delAllInstButton.setEnabled( false );
			delQueryAtomButton.setEnabled( false );
			delAllQueryAtomButton.setEnabled( false );
		}
		catch (RBNCompatibilityException ex){System.out.println(ex.toString());}
		return gg;
	}

	public void stopMapThread() {
		mapthr.setRunning(false);
		infoMessage.setText(" Stop MAP ");
		startSampling.setEnabled( true );
		trueButton.setEnabled( true );
		falseButton.setEnabled( true );
		queryButton.setEnabled( true );
		toggleTruthButton.setEnabled( true );
		delInstButton.setEnabled( true );
		delAllInstButton.setEnabled( true );
		delQueryAtomButton.setEnabled( true );
		delAllQueryAtomButton.setEnabled( true );
	}

	
	/** @author keith cascio
	@since 20060602 */
	public void forgetAll(){
		if( myACEControl != null ) myACEControl.forgetAll();
	}

	/** @author keith cascio
	@since 20060511 */
	private void doAceSettings(){
		if( myACESettingsPanel == null ) myACESettingsPanel = new SettingsPanel();
		myACESettingsPanel.show( (Component)InferenceModule.this, myprimula.getPreferences().getACESettings() );
	}

	/** @author keith cascio
	@since 20060511 */
	public Control getACEControl(){
		if( myACEControl == null ){
			myACEControl = new Control( myprimula );
			myACEControl.setParentComponent( (Component) this );
			myACEControl.setProgressBar( InferenceModule.this.getACEProgressBar() );
			myACEControl.set( myprimula.getPreferences().getACESettings() );
			myACEControl.addListener( (Control.ACEControlListener) this );
			myACEControl.setDataModel( InferenceModule.this.dataModel );
			myACEControl.setInfoMessage( InferenceModule.this.infoMessage );
		}
		return myACEControl;
	}

	/** @author keith cascio
	@since  20060728 */
	private JProgressBar getACEProgressBar(){
		if( aceProgressBar == null ){
			aceProgressBar = new JProgressBar();
			aceProgressBar.setStringPainted( true );
		}
		return aceProgressBar;
	}

	/** interface Control.ACEControlListener
    	@author keith cascio
	@since 20060511 */
	public void aceStateChange( Control control ){
		//InferenceModule.this.resetACEEnabledState( control );
		if( !control.isReadyCompute() ) dataModel.resetACE();
		//clearACEMessage();
	}

	/** @author keith cascio
	@since  20060725 */
	public void relationalStructureEdited(){
		if( myACEControl != null ) myACEControl.clear();
	}

	public void mouseClicked(MouseEvent e) {
		Object source = e.getSource();
	}

	//          Invoked when the mouse button has been clicked (pressed and released) on a component.
	public void mouseEntered(MouseEvent e) {
		Object source = e.getSource();
	}

	//          Invoked when the mouse enters a component.
	public void mouseExited(MouseEvent e) {
		Object source = e.getSource();
	}
	//          Invoked when the mouse exits a component.
	public void mousePressed(MouseEvent e) {
		Object source = e.getSource();

		if(source == attributesList){
			first_bin = first_arb = true;
			binaryList.clearSelection();
			arbitraryList.clearSelection();
			elementNamesList.clearSelection();
			int index = attributesList.locationToIndex(e.getPoint());
			if(index >= 0){
				rel = (BoolRel)attributesListModel.elementAt(index);
				infoMessage.setText(rel.name.name);
			}
		}
		else if( source == binaryList ){
			first_bin = first_arb = true;
			attributesList.clearSelection();
			arbitraryList.clearSelection();
			elementNamesList.clearSelection();
			int index = binaryList.locationToIndex(e.getPoint());
			if(index >= 0){
				rel = (BoolRel)binaryListModel.elementAt(index);
				infoMessage.setText(rel.name.name);
			}
		}
		else if( source == arbitraryList ){
			first_bin = first_arb = true;
			attributesList.clearSelection();
			binaryList.clearSelection();
			elementNamesList.clearSelection();
			int index = arbitraryList.locationToIndex(e.getPoint());
			if(index >= 0){
				rel = (BoolRel)arbitraryListModel.elementAt(index);
				infoMessage.setText(rel.name.name);
			}
			if (rel.arity == 0){
				infoMessage.setText(rel.name.name+"()");
				if(queryModeOn){
					queryatoms.add(rel,new int[0]);
					updateQueryatomsList();
				}
				else{
					inst.add(new GroundAtom(rel,new int[0]),truthValue,"?");
					updateInstantiationList();
				}
			}
		}
		else if( source == elementNamesList ){
			int selected;
			if(!sampling){
				if(rel != null){  //relation should be selected first
					selected = elementNamesList.locationToIndex(e.getPoint());
					if(selected >= 0){
						//an attribute
						if(rel.arity == 1){
							//MJ->
							tuple = new int[1];
							//<-MJ
							int[] node = {selected};
							addedTuples = (String)elementNamesListModel.elementAt(selected);
							if(queryModeOn){
								addAtoms(rel, node);
							}
							else{
								int [][] instantiations = new int[1][tuple.length];
								//System.out.println("tuple.length: " + tuple.length);
//								firstrunstar = true;
//								instantiationpos = 0;
//								size = 1;
								instantiations=allMatchingTuples(node);
								inst.add(rel, instantiations, truthValue,"?");
								updateInstantiationList();
								infoMessage.setText(rel.name.name+" ("+addedTuples+") "+truthValue+" added");
								
							}
						}
						//a binary relation
						else if(rel.arity == 2){
							if(first_bin){
								tuple = new int[2];
								tuple[0] = selected;
								addedTuples  = (String)elementNamesListModel.elementAt(tuple[0]);
								first_bin = false;
								if(elementNamesListModel.elementAt(selected).equals("*")){
									firstbinarystar=true;
								}
								if(queryModeOn){
									infoMessage.setText(rel.name.name+" ("+addedTuples+",...)");
								}
								else{
									infoMessage.setText(rel.name.name+" ("+addedTuples+",...) "+truthValue);
								}
							}
							else if(!first_bin){
								tuple[1] = selected;
								first_bin = true;
								addedTuples = addedTuples + ", " + (String)elementNamesListModel.elementAt(tuple[1]);
								if(queryModeOn){
									addAtoms(rel, tuple);
								}
								else{
									int[][] instantiations = allMatchingTuples(tuple);
									inst.add(rel, instantiations, truthValue,"?");
									updateInstantiationList();
									infoMessage.setText(rel.name.name+" ("+addedTuples+") "+truthValue+" added");
									tuple = new int[0];
								}
							}
							firstbinarystar = false;
						}
						//an arbitrary relation
						else if(rel.arity >= 3){
							if(first_arb){
								aritynumber = rel.arity;
								tuple = new int[aritynumber];
								index = 0;
								tuple[index] = selected;
								addedTuples = (String)elementNamesListModel.elementAt(tuple[index]);
								++index;
								--aritynumber;
								first_arb = false;
								if(queryModeOn){
									infoMessage.setText(rel.name.name+" ("+addedTuples+",...)");
								}
								else{
									infoMessage.setText(rel.name.name+" ("+addedTuples+",...) "+truthValue);
								}
							}
							else if(!first_arb){
								tuple[index] = selected;
								addedTuples = addedTuples + ", " + (String)elementNamesListModel.elementAt(tuple[index]);
								++index;
								--aritynumber;
								if(aritynumber==0){
									first_arb = true;
									if(queryModeOn){
										infoMessage.setText("This can take a few minuts, please wait.");
										addAtoms(rel, tuple);
										tuple = new int[0];
									}
									else{
										infoMessage.setText("This can take a few minuts, please wait.");
										//************
//										size = 1;
	//									int[][] instantiations = new int[1][tuple.length];
//										firstrunstar = true;
//										instantiationpos = 0;

										int[][] instantiations = allMatchingTuples(tuple);
										inst.add(rel, instantiations, truthValue,"?");
										updateInstantiationList();
										infoMessage.setText(rel.name.name+" ("+addedTuples+") "+truthValue+" added");
										tuple = new int[0];
									}
									addedTuples = "";
								}
								else{
									if(queryModeOn)
										infoMessage.setText(rel.name.name+" ("+addedTuples+",...) ");
									else
										infoMessage.setText(rel.name.name+" ("+addedTuples+",...) "+truthValue);
								}
							}
						}
						myprimula.updateBavaria();
					}
				}
				else
					infoMessage.setText("Please, choose the relation first");
			}
			else{
				JOptionPane.showMessageDialog(null, "Stop sampling before adding a new query", "Stop sampling", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if( source == instantiationsList){
			int index = instantiationsList.locationToIndex(e.getPoint());
			if(index >= 0){
				Vector instAtoms = inst.allInstAtoms();
				selectedInstAtom = (InstAtom)instAtoms.elementAt(index);
			}
			else
				selectedInstAtom = null;
		}
		else if( source == querytable ){
			int index = querytable.rowAtPoint(e.getPoint());
			if(index>=0){
				delAtom = index;
				Vector queries = queryatoms.allAtoms();
				selectedQueryAtom = (GroundAtom)queries.elementAt(index);
			}
			else{
				selectedQueryAtom = null;
			}
		}
	}
	//          Invoked when a mouse button has been pressed on a component.
	public void mouseReleased(MouseEvent e) {
		Object source = e.getSource();
	}
	//          Invoked when a mouse button has been released on a component.




	//reads the element names from the relstruc
	private void readElementNames(){
		if(myprimula.rels instanceof SparseRelStruc){
			SparseRelStruc sparserst = (SparseRelStruc)myprimula.rels;
			Vector elementNames = sparserst.getNames();
			for(int i=0; i<elementNames.size(); ++i){
				elementNamesListModel.addElement((String)elementNames.elementAt(i));
			}
			Vector<BoolRel> attributeNames = sparserst.getBoolAttributes();
			for(int j =0; j<attributeNames.size();j++){
				elementNamesListModel.addElement("["+attributeNames.elementAt(j)+"*]");
			}
		}
//		if(myprimula.rels instanceof OrdStruc){
//			OrdStruc ordStruc = (OrdStruc)myprimula.rels;
//			for(int i=0; i<ordStruc.dom; ++i){
//				elementNamesListModel.addElement(ordStruc.nameAt(i));
//			}
//		}
		elementNamesListModel.addElement("*");
	}

	//new rst-file loaded or OrdStruc created
	public void newElementNames(){
		elementNamesListModel.clear();
		readElementNames();
		//instasosd.reset();
		instantiationsListModel.clear();
		queryatoms.reset();
		dataModel.reset();
		infoMessage.setText(" ");
		first_bin = first_arb = true;
		selectedInstAtom = null;
		selectedQueryAtom = null;

		if( myACEControl != null ) myACEControl.clear();//keith cascio 20061201
	}

	public void newAdaptiveMode(int admode){
		adaptivemode = admode;
	}

	public void newSampleordMode(int sordmode){
		sampleordmode = sordmode;
	}


	//user adds an new element or renames the element name (in Bavaria)
	public void addOrRenameElementName(){
		int selected = elementNamesList.getSelectedIndex();
		elementNamesListModel.clear();
		readElementNames();
		updateInstantiationList();
		updateQueryatomsList();
		if(selected != -1)
			elementNamesList.setSelectedIndex(selected);
	}


	//user deletes the element (in Bavaria)
	public void deleteElementName(int node){
		elementNamesListModel.clear();
		readElementNames();
		inst.deleteShift(node);
		updateInstantiationList();
		queryatoms.delete(node);
		queryatoms.shiftArgs(node);
		updateQueryatomsList();
		for(int i=0; i<tuple.length; ++i){
			if(tuple[i] == node){
				infoMessage.setText("Tuple cancelled (included a deleted node)");
				first_bin = first_arb = true;
			}
		}
	}

	//reads the relation names from the rbn-file
	public void readRBNRelations(){
		if(myprimula.rbn != null){
			Rel[] rels = myprimula.rbn.Rels();
			for(int i=0; i<rels.length; ++i){
				if(rels[i].arity == 1)
					attributesListModel.addElement(rels[i]);
				else if(rels[i].arity == 2)
					binaryListModel.addElement(rels[i]);
				//else if(rels[i].arity >= 3)
				else
					arbitraryListModel.addElement(rels[i]);
			}
		}
	}


	//new RBN file loaded
	public void updateRBNRelations(){
		attributesListModel.clear();
		binaryListModel.clear();
		arbitraryListModel.clear();
		readRBNRelations();
		//instasosd.reset();
		instantiationsListModel.clear();
		queryatoms.reset();
		dataModel.reset();
		elementNamesList.clearSelection();
		infoMessage.setText(" ");
		first_bin = first_arb = true;
		rel = null;
		selectedInstAtom = null;
		selectedQueryAtom = null;

		if( myACEControl != null ) myACEControl.clear();//keith cascio 20060515
	}


	/* Computes all tuples of domain elements that match
	 * the sequence of elementNamesListModel entries at 
	 * the indices given by tuple (either single domain 
	 * element indices, or *-expressions)
	 */
	private int[][] allMatchingTuples(int[] tuple){
		Vector<int[]> elementsForCoordinate = new Vector<int[]>();
		int[] nextComponent;
		String stringAtTupleIndex;
		for(int i=0; i<tuple.length; i++){
			stringAtTupleIndex = (String)elementNamesListModel.elementAt(tuple[i]);
			if(stringAtTupleIndex.equals("*")){
				nextComponent = new int[myprimula.getRels().domSize()];
				for(int j=0; j<nextComponent.length; j++)
					nextComponent[j]=j;
			}
			else if(stringAtTupleIndex.startsWith("[")){
				String attrname = stringAtTupleIndex.substring(1,stringAtTupleIndex.length()-2);
				Vector<int[]> elementsOfAttr = myprimula.getRels().allTrue(new Rel(attrname,1));
				/* Turn vector of int[1] into int[]:*/
				nextComponent = rbnutilities.intArrVecToArr(elementsOfAttr);
			}
			else{ /* tuple[i] is the domain element with index i */
				nextComponent = new int[1];
				nextComponent[0]=tuple[i];
			}
			elementsForCoordinate.add(nextComponent);
		}
		return rbnutilities.cartesProd(elementsForCoordinate);
	}

	private int[][] allMatchingTuples(String[] strtuple){
		Vector<int[]> elementsForCoordinate = new Vector<int[]>();
		int[] nextComponent;
		String nextstr;
		for(int i=0; i<tuple.length; i++){
			nextstr = strtuple[i];
			if(nextstr.equals("*")){
				nextComponent = new int[myprimula.getRels().domSize()];
				for(int j=0; j<nextComponent.length; j++)
					nextComponent[j]=j;
			}
			else if(nextstr.startsWith("[")){
				String attrname = nextstr.substring(1,nextstr.length()-2);
				Vector<int[]> elementsOfAttr = myprimula.getRels().allTrue(new Rel(attrname,1));
				/* Turn vector of int[1] into int[]:*/
				nextComponent = rbnutilities.intArrVecToArr(elementsOfAttr);
			}
			else{ /* tuple[i] is the name of a domain element*/
				nextComponent = new int[1];
				nextComponent[0]=tuple[i];
			}
			elementsForCoordinate.add(nextComponent);
		}
		return rbnutilities.cartesProd(elementsForCoordinate);
	}

	public void updateInstantiationList(){
		//		selectedInstAtom = null;
		instantiationsListModel.clear();
		inst = myprimula.instasosd;
		Vector instAtoms = inst.allInstAtoms();
		for(int i=0; i<instAtoms.size(); ++i){
			InstAtom temp = (InstAtom)instAtoms.elementAt(i);
			int[] nodes = temp.args;
			String names = "(";
			for(int j=0; j<nodes.length; ++j){
				if(j+1 < nodes.length){
					names = names + elementNamesListModel.elementAt(nodes[j]) + ", ";
				}
				else{  //last item
					names = names + elementNamesListModel.elementAt(nodes[j]);
				}
			}
			names = names + ")";
			String listItem = (String)(temp.rel.name.name)  + names + " = " + temp.truthval;
			instantiationsListModel.addElement(listItem);
		}

		if( myACEControl != null ) myACEControl.primulaEvidenceChanged();//keith cascio 20061010
	}



	//updates the query atoms list
	private void addAtoms(Rel rel, int[] tuple){
		SparseRelStruc rstnew = new SparseRelStruc();
		rstnew = (SparseRelStruc)myprimula.rels;

		int[] temp = new int[tuple.length];
		int pos = 0;
		int length = tuple.length;
		for(int x=0; x<tuple.length; x++){
			temp[x] = tuple[x];
		}
		for(int i=0; i<length; i++){
			if(elementNamesListModel.elementAt(tuple[i]).equals("*")){
				Vector v = rstnew.getNames();
				for(int j=0; j<v.size(); j++){
					temp[pos] = j;
					addAtoms(rel, temp);
				}
			}
			else if(((String)elementNamesListModel.elementAt(tuple[i])).startsWith("[")){
				Vector<BoolRel> attributeNames = rstnew.getBoolAttributes();
				BoolRel nextattr;
				for(int j =0; j<attributeNames.size();j++){
					nextattr = attributeNames.elementAt(j);
					if(((String)elementNamesListModel.elementAt(tuple[i])).equals("["+ nextattr +"*]")){
						Vector<int[]> tuples = rstnew.allTrue(nextattr);
						for(int k =0; k<tuples.size(); k++){
							int[] temp2 = tuples.elementAt(k);
							temp[pos] = temp2[0];
							addAtoms(rel, temp);
						}
					}
				}
			}
			else{
				if(pos == length-1){
					queryatoms.add(rel, temp);
				}
			}
			pos++;
		}
		updateQueryatomsList();
		infoMessage.setText(rel.name.name+" ("+addedTuples+") added");
		temp = null;
	}

	public void updateQueryatomsList(){
		selectedQueryAtom = null;
		dataModel.reset();
		Vector queries = queryatoms.allAtoms();
		for(int i=0; i<queries.size(); ++i){
			GroundAtom temp = (GroundAtom)queries.elementAt(i);
			int nodes[] = temp.args;
			Rel rel = temp.rel;
			String names = ""+rel.name.name + "(";
			for(int j=0; j<nodes.length; ++j){
				if(j+1 < nodes.length){
					names = names + elementNamesListModel.elementAt(nodes[j]) + ",";
				}
				else { //last item
					names = names + elementNamesListModel.elementAt(nodes[j]);
				}
			}
			names = names + ")";
			String listItem = names;
			dataModel.addQuery(listItem);
		}
		querytable.updateUI();

		if( myACEControl != null ) myACEControl.primulaQueryChanged();//keith cascio 20060620
	}

	private void generateQueryatoms(){
		LinkedList relstruct = new LinkedList();
		queryatoms.reset();
		LinkedList queryatoms = dataModel.getQuery();
		for(int i=0; i<queryatoms.size(); i++){
			String atom = ""+queryatoms.get(i);
			//System.out.println("in generateQueryAtoms: " + atom);
			String rel = atom.substring(0, atom.indexOf("("));
			//rel = rel.substring(0, atom.indexOf(" "));
			LinkedList elementNames = new LinkedList();
			int comma = atom.indexOf("(")+1;
			for(int j = atom.indexOf("("); j<atom.length(); j++){
				String temp =""+ atom.charAt(j);
				if(temp.equals(",")){
					String element = atom.substring(comma, j);
					elementNames.add(element);
					comma = j+2;
				}
			}
			String element = atom.substring(comma, atom.indexOf(")"));
			elementNames.add(element);
			int[] tuple = new int[elementNames.size()];
			Rel relnew = new Rel();
			if(elementNames.size() == 1){
				for(int m=0;m<attributesListModel.size();m++){
					if(attributesListModel.get(m).toString().equals(rel)){
						relnew = (Rel)attributesListModel.get(m);
					}
				}
			}
			else if(elementNames.size() == 2){
				for(int m=0;m<binaryListModel.size();m++){
					if(binaryListModel.get(m).toString().equals(rel)){
						relnew = (Rel)binaryListModel.get(m);
						//System.out.println("binaryListModel: "+binaryListModel.get(m).toString());
					}
				}
			}
			else {
				for(int m=0;m<arbitraryListModel.size();m++){
					if(((Rel)arbitraryListModel.get(m)).printname().equals(rel)){
						relnew = (Rel)arbitraryListModel.get(m);
					}
				}
			}
			int [] args = new int [elementNames.size()];
			for(int n=0; n<elementNames.size(); n++){
				for(int o=0; o<elementNamesListModel.size(); o++){
					if(elementNamesListModel.get(o).equals(elementNames.get(n))){
						args[n] = o;
					}
				}
			}
			TempAtoms temp = new TempAtoms(relnew, args);
			relstruct.add(temp);
		}
		for(int t=0; t<relstruct.size(); t++){
			TempAtoms temp = (TempAtoms)relstruct.get(t);
			addAtoms(temp.getRel(), temp.getArgs());
		}
	}

	private double[][] computeQueryBatch(){
		/* Computes the probability of each query atom in all
		 * data cases contained in myprimula.rdata
		 * 
		 * Assumes that all probabilities can be computed by just
		 * evaluating the probability formula, i.e., no dependence on 
		 * unobserved atoms. 
		 * 
		 * Returns a queryatoms.length x 7 double matrix, containing for each
		 * query atom:
		 * 
		 * count of true positives
		 * count of false positives
		 * count of false negatives
		 * count of true negatives
		 * count of atoms for which probability was not computed, because of dependence on unobserved atom
		 * count of atoms for which a truth value was not given in the data case
		 * average log-likelihood 
		 */
		
		double[][] result = new double[queryatoms.size()][7];
		
		RelData rdata = myprimula.getReldata();
		
		if (rdata.size() > 1){
			System.out.println("Warning: data available for more than one input domain. Will evaluate queries only "
					+ "for first input domain");
		}
		RelDataForOneInput rdoi = rdata.caseAt(0);
		RelStruc A = rdoi.inputDomain();
		OneStrucData osd;
		
		GroundAtom gat;
		ProbForm pf;
		String[] varargs;
		int[] intargs;
		double prob=0;
		int tv;
		RBN rbn = myprimula.rbn;
		Boolean predpos=false;
		for (int i=0;i<rdoi.numberOfObservations();i++){
			osd = rdoi.oneStrucDataAt(i);
			for (int j=0;j<queryatoms.size();j++){
				gat=queryatoms.atomAt(j);
				pf = rbn.probForm(gat.rel());
				varargs = rbn.args(gat.rel());
				intargs = gat.args();
				try{
					prob = (double)pf.evaluate(A, 
							osd, 
							varargs, 
							intargs, 
							true,  
							true, 
							null, 
							false,
							null,
							null,
							ProbForm.RETURN_ARRAY,
							true,
							null)[0];
				}
				catch (RBNCompatibilityException ex){System.out.println(ex);}
				if (prob!=Double.NaN)
					predpos = (prob>0.5);
				else {
					result[j][4]++; // no prediction
					break;
				}
				result[j][6]+= prob;
				// Now get the actual truth value
				tv=osd.truthValueOf(gat);
				switch (tv){
				case 1:
					if (predpos)
						result[j][0]++; // true positive
					else
						result[j][2]++; // false negative
					break;
				case 0:
					if (predpos)
						result[j][1]++; // false positive
					else
						result[j][3]++; // true negative
					break;
				case -1:
					result[j][5]++; // no ground truth
				}
			} //for (int j=0;j<queryatoms.size();j++)
		} // (int i=0;i<rdoi.numberOfObservations();i++)
		
		/* Normalize the likelihood */
		double numevaluated;
		for (int j=0;j<queryatoms.size();j++){
			numevaluated = result[j][0]+result[j][1]+result[j][2]+result[j][3];
			result[j][6]=result[j][6]/numevaluated;
		}
		
		// Temporary:
		double TP=0;
		double FP=0;
		double FN=0;
		double TN=0;
		
		System.out.println("[TP,FP,FN,TN,Pred. not evaluated,Ground truth unknown, P(positive)]");
		for (int i=0;i< result.length;i++){
			System.out.println(queryatoms.atomAt(i).asString(A)+'\t'+StringOps.arrayToString(result[i], "[", "]"));
			TP=TP+result[i][0];
			FP=FP+result[i][1];
			FN=FN+result[i][2];
			TN=TN+result[i][3];
		}
		
		double acc= (TP+TN)/(TP+TN+FP+FN);
		
		System.out.println("TP: " + TP +" FP: " + FP + " FN: " + FN + " TN: "+ TN);
		System.out.println("Accuracy: " + acc );
		return result;
	}
	
//	private void evaluateAccuracy(){
//		/* Computes the accuracy of the current rbn 
//		 * for all probabilistic relations w.r.t. the 
//		 * probabilistic relations contained in myprimula.getRelData()
//		 * 
//		 * Assumes that all probabilities can be computed by just
//		 * evaluating the probability formula, i.e., no dependence on 
//		 * unobserved atoms. 
//		 * 
//		 * Returns for each relation a double matrix of length 5, containing:
//		 * 
//		 * count of true positives
//		 * count of false positives
//		 * count of false negatives
//		 * count of true negatives
//		 * count of atoms for which probability was not computed, because of dependence on unobserved atom
//		 */
//		
//		
//		Signature sig = myprimula.getSignature();
//		RelData rdata = myprimula.getReldata();
//		RBN rbn = myprimula.rbn;
//		
//		for (Rel r : sig.getProbRels()) {
//			System.out.println("Evaluate relation " + r.name() + " for " + rdata.cases().size() + " input domains");
//			ProbForm pf = rbn.probForm(r);
//			String[] varargs = rbn.args(r);
//			
//			double[] result = new double[7];
//
//			for (RelDataForOneInput rdoi: rdata.cases()) {
//				System.out.print(".");
//				RelStruc A = rdoi.inputDomain();
//				for (OneStrucData osd: rdoi.allOneStrucData()) {
//					double prob=Double.NaN;
//					/* First the true cases: */
//					Vector<int[]> at = osd.allTrue(r);
//
//					for (int[] intargs : at){
//							
//							boolean predpos;
//							
//							try{
//								prob = pf.evaluate(A, 
//										osd, 
//										varargs, 
//										intargs, 
//										true, 
//										new String[0], 
//										true, 
//										null, 
//										false,
//										null);
//							}
//							catch (RBNCompatibilityException ex){System.out.println(ex);}
//							if (prob!=Double.NaN)
//								if (prob > 0.5) // true positive
//									result[0]++;
//								else //false negative
//									result[2]++;
//							else {
//								result[4]++; // no prediction
//								break;
//							}
//					}
//					
//					/* Now the false cases (near duplicate code): */
//					at = osd.allFalse((BoolRel)r);
//
//					for (int[] intargs : at){
//							boolean predpos;
//
//							try{
//								prob = pf.evaluate(A, 
//										osd, 
//										varargs, 
//										intargs, 
//										true, 
//										new String[0], 
//										true, 
//										null, 
//										false,
//										null);
//							}
//							catch (RBNCompatibilityException ex){System.out.println(ex);}
//							if (prob!=Double.NaN)
//								if (prob < 0.5) // true negative
//									result[3]++;
//								else //false positive
//									result[1]++;
//							else {
//								result[4]++; // no prediction
//								break;
//							}
//					}		
//	
//	
//
//				
//	
//				} // for (OneStrucData osd: rdoi.allOneStrucData())
//			} // for (RelDataForOneInput rdoi: rdata.cases())
//			double acc= (result[0]+result[3])/(result[0]+result[1]+result[2]+result[3]);
//
//			System.out.println("TP: " + result[0]+" FP: " + result[1] + " FN: " + result[2] + " TN: "+ result[3]);
//			System.out.println("Accuracy: " + acc );
//		} // for (Rel r : : sig.getProbRels()) 
//
//
//	}
//	

	private void evaluateAccuracy(){
		/* Computes the accuracy of the current rbn 
		 * for all probabilistic relations w.r.t. the 
		 * probabilistic relations contained in myprimula.getRelData()
		 * 
		 * Assumes that all probabilities can be computed by just
		 * evaluating the probability formula, i.e., no dependence on 
		 * unobserved atoms. 
		 * 
		 * Returns for each relation a double matrix of length 5, containing:
		 * 
		 * count of true positives
		 * count of false positives
		 * count of false negatives
		 * count of true negatives
		 * count of atoms for which probability was not computed, because of dependence on unobserved atom
		 */
		
		
		Signature sig = myprimula.getSignature();
		RelData rdata = myprimula.getReldata();
		RBN rbn = myprimula.rbn;
		
		for (Rel r : sig.getProbRels()) {
			System.out.println("Evaluate relation " + r.name() + " for " + rdata.cases().size() + " input domains");
			ProbForm pf = rbn.probForm(r);
			String[] varargs = rbn.args(r);
			
			double[] result = new double[5];

			for (RelDataForOneInput rdoi: rdata.cases()) {
				System.out.print(".");
				RelStruc A = rdoi.inputDomain();
				for (OneStrucData osd: rdoi.allOneStrucData()) {
					double prob=Double.NaN;
					Hashtable<String,Object[]> evaluated = new Hashtable<String,Object[]>();
					//Hashtable<String,Double> evaluated =null;
					/* First the true cases: */
					Vector<int[]> at = osd.allTrue(r);

					for (int[] intargs : at){
						
							try{
								
								prob = (double)pf.evaluate(A, 
										osd, 
										varargs, 
										intargs, 
										true, 
										true, 
										null, 
										false,
										evaluated,
										null,
										ProbForm.RETURN_ARRAY,
										true,
										null)[0];
							}
							catch (RBNCompatibilityException ex){System.out.println(ex);}
							if (prob!=Double.NaN)
								if (prob > 0.5) // true positive
									result[0]++;
								else //false negative
									result[2]++;
							else {
								result[4]++; // no prediction
								break;
							}
					}
					
					/* Now the false cases (near duplicate code): */
					at = osd.allFalse((BoolRel)r);

					for (int[] intargs : at){
							boolean predpos;

							try{
								prob = (double)pf.evaluate(A, 
										osd, 
										varargs, 
										intargs, 
										true, 
										true, 
										null, 
										false,
										evaluated,
										null,
										ProbForm.RETURN_ARRAY,
										true,
										null)[0];
							}
							catch (RBNCompatibilityException ex){System.out.println(ex);}
							if (prob!=Double.NaN)
								if (prob < 0.5) // true negative
									result[3]++;
								else //false positive
									result[1]++;
							else {
								result[4]++; // no prediction
								break;
							}
					}		
	
	

				
	
				} // for (OneStrucData osd: rdoi.allOneStrucData())
			} // for (RelDataForOneInput rdoi: rdata.cases())
			double acc= (result[0]+result[3])/(result[0]+result[1]+result[2]+result[3]);
			System.out.println();
			System.out.println("TP: " + result[0]+" FP: " + result[1] + " FN: " + result[2] + " TN: "+ result[3]);
			System.out.println("Accuracy: " + acc );
		} // for (Rel r : : sig.getProbRels()) 


	}
	

	/**
	 * @return
	 * @uml.property  name="logfilename"
	 */
	public String getLogfilename(){
		return logfilename;
	}
	/**
	 * @param logfilename
	 * @uml.property  name="logfilename"
	 */
	public void setLogfilename(String logfilename){
		this.logfilename = logfilename;
	}

	public int getSampleOrdMode(){
		return sampleordmode;
	}

	public void setSampleOrdMode(int sampleordmode){
		this.sampleordmode = sampleordmode;
	}

	public void setLearnSampleSize(Integer lss){
		numchains = lss;
	}
	public void setWindowSize(Integer gr){
		windowsize = gr;
	}

	public void setVerbose(boolean v){
		ggverbose = v;
	}
	
	public void setMaxFails(Integer mf){
		maxfails = mf;
	}
	
	public void setNumRestarts(Integer mf){
		numrestarts = mf;
	}
	
	public int getAdaptiveMode(){
		return adaptivemode;
	}

	public void setAdaptiveMode(int adaptivemode){
		this.adaptivemode = adaptivemode;
	}

	public void setSettingsOpen(boolean b){
		settingssamplingwindowopen = b;
	}

	public boolean[] getSampleLogMode(){
		return samplelogmode;
	}

	public boolean getSampleLogMode(int i){
		return samplelogmode[i];
	}

	public void setCPTParents(int np){
		this.cptparents = np;
	}

	public int getCPTParents(){
		return cptparents;
	}

	public void setNumSubsamples_minmax(int nss){
		this.num_subsamples_minmax = nss;
	}

	public int getNumSubsamples_minmax(){
		return num_subsamples_minmax;
	}

	public void setNumSubsamples_adapt(int nss){
		this.num_subsamples_adapt = nss;
	}

	public int getNumSubsamples_adapt(){
		return num_subsamples_adapt;
	}

	/* aca = true only interesting for learning.
	 * Here this function is just required for implementing
	 * GradientGraphOptions
	 */
	public boolean aca(){
		return false;
	}

	
	
	public int getNumChains(){
		return numchains;
	}
	
	public int getWindowSize(){
		return windowsize;
	}
	
	public int getMaxFails(){
		return maxfails;
	}
	
	public int getMAPRestarts(){
		return numrestarts;
	}
	
	public boolean ggverbose(){
		return ggverbose;
	}
	
	public boolean ggrandominit(){
		return true;
	}
	
	
	/* Following some functions that are only relevant for learning.
	 * Here  just required for implementing
	 * GradientGraphOptions
	 */
	public boolean gguse2phase(){
		return false;
	}


	public int threadascentstrategy(){
		return 0;
	}

	public  int ggascentstrategy(){
		return 0;
	}

	public int lbfgsmemory(){
		return 0;
	}

	public int getMaxIterations(){
		return 0;
	}

	public  double getLLikThresh(){
		return 0;
	}

	public double getLineDistThresh(){
		return 0;
	}

	public double adagradepsilon(){
		return 0;
	}

	public  double adagradfade(){
		return 0;
	}

	public boolean learnverbose(){
		return false;
	}

	private boolean noLog(){
		boolean result = true;
		for (int i=0;i<samplelogmode.length;i++){
			if (samplelogmode[i])
				result = false;
		}
		return result;
	}

	public void setSampleLogMode(int i, boolean b){
		this.samplelogmode[i] = b;
	}


	//     public void setDummyDouble(double dummydouble ){
		// 	this.dummydouble = dummydouble;
	//     }

	//     public double getDummyDouble(){
	// 	return dummydouble;
	//     }


	public void update(Observable o, Object arg){
		if (o instanceof SampleProbs){
			dataModel.resetProb();
			double [] prob= ((SampleProbs)o).getProbs();
			for(int i=0; i<prob.length; i++){
				dataModel.addProb(""+prob[i]);
			}
			dataModel.resetMinProb();
			double [] minprob = ((SampleProbs)o).getMinProbs();
			for(int i=0; i<minprob.length; i++){
				dataModel.addMinProb(""+minprob[i]);
			}
			dataModel.resetMaxProb();
			double [] maxprob = ((SampleProbs)o).getMaxProbs();
			for(int i=0; i<maxprob.length; i++){
				dataModel.addMaxProb(""+maxprob[i]);
			}
			dataModel.resetVar();
			double [] var = ((SampleProbs)o).getVar();
			for(int i=0; i<var.length; i++){
				dataModel.addVar(""+var[i]);
			}
			sampleSize.setText(""+((SampleProbs)o).getSize());
			Double dweight = new Double(((SampleProbs)o).getWeight());
			weight.setText(""+ myio.StringOps.doubleConverter(dweight.toString()));
		}
		
		if (o instanceof MapVals){
			dataModel.resetMapVals();
			int [] mapvals = ((MapVals)o).getMVs();
			for(int i=0; i<mapvals.length; i++){
				dataModel.addMapVal(""+mapvals[i]);
			}
			mapRestarts.setText("" +((MapVals)o).getRestarts());
			mapLL.setText("" +((MapVals)o).getLLstring());
			
		}
		
		/** keith cascio 20060511 ... */
		//dataModel.resetACE();
		/** ... keith cascio */
	
		querytable.updateUI();
	}
	
	public OneStrucData getMapValuesAsInst(){
		LinkedList<String> mapvals = dataModel.getMapValues();
		LinkedList<String> queryats = dataModel.getQuery();
		OneStrucData result = new OneStrucData();
		result.setParentRelStruc(myprimula.getRels());
		
		Iterator<String> itq = queryats.iterator();
		
		for (Iterator<String> itmap = mapvals.iterator(); itmap.hasNext();) {
			System.out.println(itq.next() + " " + itmap.next());
		}
//		for (int i=0;i< mapatoms.size();i++){
//			result.add(mapatoms.atomAt(i),instvals[i],"?");
//		}
		return result;
	}

	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}

	public void setScriptPath(String scriptPath) {
		this.scriptPath = scriptPath;
	}

	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
	}

	public void setPythonHome(String pythonHome) {
		this.pythonHome = pythonHome;
	}

	public QueryTableModel getDataModel() {
		return dataModel;
	}

	public MapThread getMapthr() {
		return mapthr;
	}

}
