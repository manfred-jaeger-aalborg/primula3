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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNCyclicException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNInconsistentEvidenceException;
import RBNLearning.GradientGraph;
import RBNLearning.GradientGraphO;
import RBNLearning.RelData;
import RBNLearning.RelDataForOneInput;
import RBNinference.BayesConstructor;
import RBNinference.MapThread;
import RBNinference.MapVals;
import RBNinference.PFNetwork;
import RBNinference.SampleProbs;
import RBNinference.SampleThread;
import RBNpackage.BoolRel;
import RBNpackage.CPModel;
import RBNpackage.GroundAtom;
import RBNpackage.GroundAtomList;
import RBNpackage.InstAtom;
import RBNpackage.OneStrucData;
import RBNpackage.ProbForm;
import RBNpackage.RBN;
import RBNpackage.Rel;
import RBNpackage.RelStruc;
import RBNpackage.Signature;
import RBNpackage.SparseRelStruc;
import RBNutilities.rbnutilities;
import edu.ucla.belief.ace.Control;
import edu.ucla.belief.ace.SettingsPanel;
import edu.ucla.belief.ui.primula.SamiamManager;
import myio.StringOps;

public class InferenceModule extends JFrame implements Observer, 
ActionListener, MouseListener, Control.ACEControlListener, GradientGraphOptions, ChangeListener{

	public static final int OPTION_SAMPLEORD_FORWARD = 0;
	public static final int OPTION_SAMPLEORD_RIPPLE = 1;
	public static final int OPTION_NOT_SAMPLE_ADAPTIVE = 0;
	public static final int OPTION_SAMPLE_ADAPTIVE = 1;

	private JTabbedPane inferencePane   = new JTabbedPane();

	/**
	 * @uml.property  name="relationsLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel relationsLabel               = new JLabel("Relations");
	/**
	 * @uml.property  name="relationsList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList relationsList                 = new JList();
	/**
	 * @uml.property  name="relationsListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.Rel"
	 */
	private DefaultListModel relationsListModel = new DefaultListModel();
	/**
	 * keith cascio 20060511 ...
	 * @uml.property  name="relationsScrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane relationsScrollList;//     = new JScrollPane();
	/**
	 * ... keith cascio
	 * @uml.property  name="valuesLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */

	private JLabel valuesLabel                   = new JLabel("Values");
	/**
	 * @uml.property  name="valuesList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList valuesList                     = new JList();
	/**
	 * @uml.property  name="valuesListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.Rel"
	 */
	private DefaultListModel valuesListModel     = new DefaultListModel();
	/**
	 * keith cascio 20060511 ...
	 * @uml.property  name="valuesScrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane valuesScrollList;//         = new JScrollPane();
	/**
	 * ... keith cascio
	 * @uml.property  name="arbitraryLabel"
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
	
	
	/**
	 * One JScrollPane for each relation for which we have a query atom
	 * 
	 * Each JScrollPane contains a JTable associated with a QueryTableModel
	 * or a subclass thereof (MCMCTableModel, MAPTableModel, ...)
	 * 
	 * The queryatomsScrolllists are embedded in the queryAtomsPanel
	 */
	private Vector<JScrollPane> queryatomsScrolllists = new Vector<JScrollPane>(); 
	
	
	//den nye queryatom tabel
	/**
	 * @uml.property  name="dataModel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private Vector<QueryTableModel> queryModels  = new Vector<QueryTableModel>();;
	
	private Vector<MAPTableModel> mapModels = new Vector<MAPTableModel>();
	
	private Vector<MCMCTableModel> mcmcModels = new Vector<MCMCTableModel>();
	
	private Vector<ACETableModel> aceModels = new Vector<ACETableModel>();
	
	private Vector<TestTableModel> testModels = new Vector<TestTableModel>();
	
	/**
	 * @uml.property  name="querytable"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private Vector<JTable> querytables = new Vector<JTable>();
	
	/**
	 * @uml.property  name="trueButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton instButton     = new JButton("Instantiation");
	
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


	ImageIcon toggleicon = new ImageIcon("./Icons/toggle.png");
	private JButton  toggleTruthButton  		= new JButton(toggleicon);
	
	ImageIcon cwaicon = new ImageIcon("./Icons/cwa.png");
	private JButton cwaButton = new JButton(cwaicon);
	
	
	ImageIcon deleteicon = new ImageIcon("./Icons/delete.png");
	private JButton delInstButton      		= new JButton(deleteicon);
	
	ImageIcon clearicon = new ImageIcon("./Icons/clear.png");
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
	 * @uml.property  name="relationsPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel relationsPanel     = new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="valuesPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel valuesPanel         = new JPanel(new BorderLayout());
	
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
	private JPanel queryatomsPanel 		 		= new JPanel(new FlowLayout());
	private JScrollPane outerQueryPane ;// = new JScrollPane();
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


//
//	private boolean first_bin = true;  //user has selected the first element
//	/**
//	 * @uml.property  name="first_arb"
//	 */
//	private boolean first_arb = true;
	/**
	 * @uml.property  name="firstbinarystar"
	 */
	private boolean firstbinarystar = false;
	/**
	 * the tuple of element identifiers (including wildcards) selected from the element names list
	 */
	private int[] element_tuple = new int[1];
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
	private Hashtable<Rel,GroundAtomList> queryatoms;
	
//	/**
//	 * Maps a string representation of a query atom to a two-part index: 
//	 * first index is the index of the table for the relation (as an element of
//	 * queryatomsScrollists), the second
//	 * is the index for this tuple in that table
//	 */
//	private Hashtable<String,int[]> groundAtomIndex;
	
	public Hashtable<Rel, GroundAtomList> getQueryatoms() {
		return queryatoms;
	}

	/**
	 * Maps a relation (identified by its name) to the index of the query atoms
	 * for this relation in the queryatomsScrolllists
	 */
	private Hashtable<String,Integer> relIndex = new Hashtable<String,Integer>(); 
	/**
	 * Vector of relations defining their order in queryatomsScrolllists:
	 * 
	 * relArray[i]= r  <=> relIndex.get(r.name)==i
	 */
	private Vector<Rel> relList; 
	
//	public GroundAtomList getQueryatoms() {
//		return queryatoms;
//	}
//
//	public void setQueryatoms(GroundAtomList queryatoms) {
//		this.queryatoms = queryatoms;
//	}

	/**
	 * @uml.property  name="rel"
	 * @uml.associationEnd  
	 * 
	 * The currently selected relation (for defining queries or evidence)
	 */
	private Rel selected_rel;
	
	/*
	 * The selected value for defining evidence (using the internal integer representation)
	 */
	private int selected_val;
	
	/*
	 * The position of the currently selected element in the argument list 
	 * of selected_rel (when building up a query or evidence)
	 */
	private int el_pos;
	
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



	public InferenceModule( Primula myprimula_param ){

		myprimula = myprimula_param;
		sampling = false;
//		maprestarts = false;
		inst = myprimula.instasosd;
		
		sampleordmode = OPTION_SAMPLEORD_FORWARD;
		adaptivemode = OPTION_NOT_SAMPLE_ADAPTIVE;
		for (int i=0;i<samplelogmode.length;i++)
			samplelogmode[i]=false;

		numchains = 2;
		windowsize = 2;
		numrestarts = 1;
		
		readElementNames();
		readRBNRelations();

		updateInstantiationList();
		
		queryatoms=myprimula.queryatoms.asHashTable();
		relIndex = new Hashtable<String,Integer>();
		relList = new Vector<Rel>();
		int idx =0;
		
		for (Rel r: queryatoms.keySet()) {
			relIndex.put(r.name(), (Integer)idx);
			relList.add(r);
			queryModels.add(new QueryTableModel());
			idx++;
		}
			
		buildQueryatomsTables(queryModels);
			
		/* Top panel with list of attributes/binary relations/arbitrary relations */
		relationsList.setModel(relationsListModel);
		relationsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		/** keith cascio 20060511 ... */
		relationsScrollList = new JScrollPane( relationsList );//relationsScrollList.getViewport().add(relationsList);
		Dimension sizePreferred = relationsScrollList.getPreferredSize();
		sizePreferred.height = 64;
		relationsScrollList.setPreferredSize( sizePreferred );
		/** ... keith cascio */
		relationsPanel.add(relationsLabel, BorderLayout.NORTH);
		relationsPanel.add(relationsScrollList, BorderLayout.CENTER);

		valuesList.setModel(valuesListModel);
		valuesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		/** keith cascio 20060511 ... */
		valuesScrollList = new JScrollPane( valuesList );//valuesScrollList.getViewport().add(binaryList);
		valuesScrollList.setPreferredSize( sizePreferred );
		/** ... keith cascio */
		valuesPanel.add(valuesLabel, BorderLayout.NORTH);
		valuesPanel.add(valuesScrollList, BorderLayout.CENTER);



		arityPanel.add(relationsPanel);
		arityPanel.add(valuesPanel);
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
		
		/* Panel with buttons underneath querytable */
		queryatomsButtonsPanel.add(delQueryAtomButton);
		delQueryAtomButton.setToolTipText("Delete query atom");
		queryatomsButtonsPanel.add(delAllQueryAtomButton);
		delAllQueryAtomButton.setToolTipText("Delete all query atoms");
		deletesamplePanel.add(queryatomsButtonsPanel, BorderLayout.NORTH);
		/* ************************************ */
		inferencePane.add("Query",queryatomsButtonsPanel);
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
		
		
//		atomsPanel.add(instantiationsPanel, BorderLayout.CENTER);
		outerQueryPane=new JScrollPane();
		outerQueryPane.getViewport().add(queryatomsPanel);
		atomsPanel.add(outerQueryPane, BorderLayout.SOUTH);
		
		//MouseListeners
		relationsList.	addMouseListener( this );
		valuesList.addMouseListener( this );
		elementNamesList.addMouseListener( this );
		instantiationsList.addMouseListener( this );
		//querytable.addMouseListener( this); // TODO
		//ActionListerners
		instButton.addActionListener( this );
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
		
		inferencePane.addChangeListener(this);
		
		//setting background color
		instButton.setBackground(Primula.COLOR_BLUE_SELECTED);
		instButton.setToolTipText("Add atoms instantiated to true");
		queryButton.setBackground(Primula.COLOR_BLUE);
		queryButton.setToolTipText("Add atoms to query list");

		toggleTruthButton.setBackground(Primula.COLOR_YELLOW);
		cwaButton.setBackground(Primula.COLOR_YELLOW);
		delInstButton.setBackground(Primula.COLOR_YELLOW);
		delAllInstButton.setBackground(Primula.COLOR_YELLOW);
//		saveInstButton.setBackground(Primula.COLOR_RED);
//		loadInstButton.setBackground(Primula.COLOR_RED);
		delQueryAtomButton.setBackground(Primula.COLOR_GREEN);
		delAllQueryAtomButton.setBackground(Primula.COLOR_GREEN);
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

		buttonsPanel.add(instButton);
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
		JSplitPane querySampleSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,buttonsAndInfoPanel,outerQueryPane);
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
		this.setVisible(true);
	}

	public void actionPerformed( ActionEvent e ) 
	{
		Object source = e.getSource();

		if( source == instButton ){
			el_pos=0;
			instButton.setBackground(Primula.COLOR_BLUE_SELECTED);
			queryButton.setBackground(Primula.COLOR_BLUE);
			elementNamesList.clearSelection();
			queryModeOn = false;
			infoMessage.setText(" ");
		}

		else if( source == queryButton ){
			el_pos=0;
			queryButton.setBackground(Primula.COLOR_BLUE_SELECTED);
			instButton.setBackground(Primula.COLOR_BLUE);
			toggleTruthButton.setEnabled(false);
			cwaButton.setEnabled(false);
			delInstButton.setEnabled(false);
			delAllInstButton.setEnabled(false);
			elementNamesList.clearSelection();
			queryModeOn = true;
			infoMessage.setText(" ");
		}
		else if( source == toggleTruthButton ){
			if(selectedInstAtom != null && selectedInstAtom.rel instanceof BoolRel){
				if(selectedInstAtom.val == 1){
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
			if (selected_rel != null && selected_rel instanceof BoolRel){
				inst.applyCWA((BoolRel)selected_rel);
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

//		else if( source == delQueryAtomButton ){
//			if(selectedQueryAtom != null){
//				queryModel.removeQuery(delAtom);
//				generateQueryatoms();
//				updateQueryatomsList(queryModel);
//				Vector queries = queryatoms.allAtoms();
//				int listsize = queries.size()-1;
//				if( delAtom >= listsize ){
//					delAtom--;
//				}
//				if(delAtom != -1){
//					delAtom--;
//					if( delAtom == -1 ){
//						delAtom++;
//						querytable.setRowSelectionInterval(delAtom, delAtom);
//						selectedQueryAtom = (GroundAtom)queries.elementAt(delAtom );
//					}
//					else{
//						querytable.setRowSelectionInterval(delAtom, delAtom);
//						selectedQueryAtom = (GroundAtom)queries.elementAt(delAtom );
//					}
//				}
//			}
//		}
//		else if(source == delAllQueryAtomButton){
//			queryModel.removeAllQueries();
//			generateQueryatoms();
//			updateQueryatomsList(queryModel);
//		}
		else if( source == settingsSampling ){
			if (!settingssamplingwindowopen){
				swindow = new RBNgui.SettingsSampling( InferenceModule.this );
				settingssamplingwindowopen = true;
			}
		}
		else if( source == startSampling ){
			startSampleThread();
		}
//		else if( source == setPrediction){
//			// mostly copy from (source == setMapVals)
//			LinkedList<String> probvals = mcmcModel.getProbabilities();
//			LinkedList<String> queryats = mcmcModel.getQuery();
//			OneStrucData result = new OneStrucData();
//			result.setParentRelStruc(myprimula.getRels());
//			
//			Iterator<String> itq = queryats.iterator();
//			
//			for (Iterator<String> itprob = probvals.iterator(); itprob.hasNext();) {
//				double p = Double.parseDouble(itprob.next());
//				if (p>=0.5)
//					result.add(new GroundAtom(itq.next(),myprimula.getRels(),Rel.BOOLEAN),true,"?");
//				else
//					result.add(new GroundAtom(itq.next(),myprimula.getRels(),Rel.BOOLEAN),false,"?");
//			}
//			inst.add(result);
//			updateInstantiationList();
//			myprimula.updateBavaria();
//		}
		else if( source == stopSampling){
			stopSampling();
			
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
			instButton.setEnabled( true );
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
//		else if (source == setMapVals){
//			if (currentGG != null){
//				LinkedList<String> mapvals = mapModel.getMapValues();
//				LinkedList<String> queryats = mapModel.getQuery();
//				OneStrucData result = new OneStrucData();
//				result.setParentRelStruc(myprimula.getRels());
//				
//				Iterator<String> itq = queryats.iterator();
//				
//				for (Iterator<String> itmap = mapvals.iterator(); itmap.hasNext();) {
//					//System.out.println(itq.next() + " " + itmap.next());
//					result.add(new GroundAtom(itq.next(),myprimula.getRels(),Rel.BOOLEAN),Integer.parseInt(itmap.next()),"?");
//				}
//				inst.add(result);
//				updateInstantiationList();
//				myprimula.updateBavaria();
//			}
//			else System.out.println("Do not have GradientGraph defining Map values!");
//		}
		else if( source == stopMap){
//			maprestarts = false;
			mapthr.setRunning(false);
			infoMessage.setText(" Stop MAP ");
			startSampling.setEnabled( true );
			instButton.setEnabled( true );
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

	public SampleThread startSampleThread(){

		queryatomsPanel.updateUI(); // may need updateUI on the individual tables
		
		sampling = true;
		PFNetwork pfn = null;
		if (!noLog()){
			if (logfilename != "")
				logwriter = myio.FileIO.openOutputFile(logfilename);
			else logwriter = new BufferedWriter(new OutputStreamWriter(System.out));

		}

		try{
			BayesConstructor constructor = null;
			constructor = new BayesConstructor(myprimula.rbn,myprimula.rels,inst,myprimula.queryatoms,myprimula);
			pfn = constructor.constructPFNetwork(myprimula.evidencemode,
					Primula.OPTION_QUERY_SPECIFIC,
					myprimula.isolatedzeronodesmode);
			pfn.prepareForSampling(sampleordmode,
					adaptivemode,
					samplelogmode,
					cptparents,
					this.queryatoms,
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
				this.queryatoms,
				samplelogmode,
				logwriter);
		sampthr.start();
		infoMessage.setText(" Starting Sampling ");
		startSampling.setEnabled( false );
		instButton.setEnabled( false );
		queryButton.setEnabled( false );
		toggleTruthButton.setEnabled( false );
		delInstButton.setEnabled( false );
		delAllInstButton.setEnabled( false );
		delQueryAtomButton.setEnabled( false );
		delAllQueryAtomButton.setEnabled( false );
		
		return sampthr;
	}

	public void stopSampling() {
		sampling = false;
		sampthr.setRunning(false);
	}
	private GradientGraph startMapThread(){
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
			
			gg = new GradientGraphO(myprimula, 
					 								evidence, 
					 								rbnparamidx,
					 								this ,
					 								queryatoms,
					 								mode,
					 								0,
					 								true);
			mapthr = new MapThread(this,myprimula,(GradientGraphO)gg);
			mapthr.start();
			instButton.setEnabled( false );
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
			//myACEControl.setDataModel( InferenceModule.this.queryModel ); //TODO
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
		// if( !control.isReadyCompute() ) aceModel.resetACE(); //TODO
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

		if(source == relationsList){
			
			valuesListModel.removeAllElements();
			elementNamesList.clearSelection();
			int index = relationsList.locationToIndex(e.getPoint());
			if(index >= 0){
				selected_rel = (Rel)relationsListModel.elementAt(index);
				if (selected_rel instanceof BoolRel)
					cwaButton.setEnabled( true );
				else
					cwaButton.setEnabled( false );
				el_pos=0;
				element_tuple = new int[selected_rel.getArity()];
				addedTuples ="";
				for (int i=0;i<selected_rel.numvals();i++)
					valuesListModel.addElement(selected_rel.get_String_val(i));
				infoMessage.setText(selected_rel.name.name);
			}
		}
		else if( source == valuesList ){
			el_pos=0;
			elementNamesList.clearSelection();
			int index = valuesList.locationToIndex(e.getPoint());
			if(index >= 0){
				String valstring = (String)valuesListModel.elementAt(index);
				selected_val=selected_rel.get_Int_val(valstring);
			}
		}

		else if( source == elementNamesList ){
			int selected_element = elementNamesList.locationToIndex(e.getPoint());
			if(!sampling){
				if(selected_rel != null && selected_rel.getArity()>0){  //relation should be selected first
					element_tuple[el_pos] = selected_element;

					if (el_pos<selected_rel.getArity()-1) {
						el_pos++;
						addedTuples += (String)elementNamesListModel.elementAt(element_tuple[index]) +", ...";
					}
					else { // tuple now complete
						addedTuples += (String)elementNamesListModel.elementAt(element_tuple[index]);
						if(queryModeOn){
							addQueryAtoms(selected_rel, element_tuple);		
							infoMessage.setText(selected_rel.name.name+" ("+addedTuples+")");
						}
						else{
							int[][] instantiations = allMatchingTuples(element_tuple);
							inst.add(selected_rel, instantiations, selected_val,"?");
							updateInstantiationList();
							infoMessage.setText(selected_rel.name.name+"("+addedTuples+ ") = "
									+selected_rel.get_String_val(selected_val));
						}

						// re-init for next tuple construction
						element_tuple = new int[selected_rel.getArity()];
						addedTuples = "";
					}

				} 
				else{// if(selected_rel != null && selected_rel.getArity()>0)
					infoMessage.setText("Please, choose the relation first");
				}
			}
			else { // if(!sampling)
				JOptionPane.showMessageDialog(null, "Stop sampling before adding a new query", "Stop sampling", JOptionPane.ERROR_MESSAGE);
			}
			myprimula.updateBavaria();
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
		else if( source == querytables.elementAt(0) ){ // TODO this now dummy solution only for the first table
			int index = querytables.elementAt(0).rowAtPoint(e.getPoint());
			System.out.println("select in on query table: effect not yet implemented");
			//			if(index>=0){
//				delAtom = index;
//				Vector queries = queryatoms.allAtoms();
//				selectedQueryAtom = (GroundAtom)queries.elementAt(index);
//			}
//			else{
//				selectedQueryAtom = null;
//			}
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
		queryatoms=new Hashtable<Rel,GroundAtomList>();
		queryModels=new Vector<QueryTableModel>();
		infoMessage.setText(" ");
		el_pos=0;
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
		buildQueryatomsTables(queryModels);
		if(selected != -1)
			elementNamesList.setSelectedIndex(selected);
	}


	//user deletes the element (in Bavaria)
	public void deleteElementName(int node){
		elementNamesListModel.clear();
		readElementNames();
		inst.deleteShift(node);
		updateInstantiationList();
		for (GroundAtomList qats: queryatoms.values()) {
			qats.delete(node);
			qats.shiftArgs(node);
		}
		buildQueryatomsTables(queryModels);
		for(int i=0; i<element_tuple.length; ++i){
			if(element_tuple[i] == node){
				infoMessage.setText("Tuple cancelled (included a deleted node)");
				el_pos=0;
			}
		}
	}

	//reads the relation names from the rbn-file
	public void readRBNRelations(){
		if(myprimula.rbn != null){
			Rel[] rels = myprimula.rbn.Rels();
			for(int i=0; i<rels.length; ++i){
					relationsListModel.addElement(rels[i]);
			}
		}
	}


	//new RBN file loaded
	public void updateRBNRelations(){
		relationsListModel.clear();
		valuesListModel.clear();
		readRBNRelations();
		//instasosd.reset();
		instantiationsListModel.clear();
		queryatoms=new Hashtable<Rel,GroundAtomList>();
		queryModels=new Vector<QueryTableModel>();
		elementNamesList.clearSelection();
		infoMessage.setText(" ");
		el_pos=0;
		selected_rel = null;
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
				Vector<int[]> elementsOfAttr = myprimula.getRels().allTrue(myprimula.sig.getRelByName(attrname));
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
		for(int i=0; i<element_tuple.length; i++){
			nextstr = strtuple[i];
			if(nextstr.equals("*")){
				nextComponent = new int[myprimula.getRels().domSize()];
				for(int j=0; j<nextComponent.length; j++)
					nextComponent[j]=j;
			}
			else if(nextstr.startsWith("[")){
				String attrname = nextstr.substring(1,nextstr.length()-2);
				Vector<int[]> elementsOfAttr = myprimula.getRels().allTrue(myprimula.sig.getRelByName(attrname));
				/* Turn vector of int[1] into int[]:*/
				nextComponent = rbnutilities.intArrVecToArr(elementsOfAttr);
			}
			else{ /* tuple[i] is the name of a domain element*/
				nextComponent = new int[1];
				nextComponent[0]=element_tuple[i];
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
			String listItem = (String)(temp.rel.name.name)  + names + " = " + temp.val_string();
			instantiationsListModel.addElement(listItem);
		}
		instantiationsPanel.updateUI();
		if( myACEControl != null ) myACEControl.primulaEvidenceChanged();//keith cascio 20061010
	}


	/**
	 * The int[] 'tuple' denotes the indices of object identifiers
	 * in the 'elements' list of the GUI. This may include general (*) 
	 * or type ([person]) wildcards. 
	 * @param rel
	 * @param tuple
	 */
	private void addQueryAtoms(Rel rel,int[] tuple) {
		GroundAtomList atstoadd = buildAtoms(rel,tuple);
		Integer idx = relIndex.get(rel.toString());
		if (idx != null) {
			queryatoms.get(rel).add(atstoadd);
		}
		else {
			idx=relIndex.size();
			relIndex.put(rel.name(), (Integer)idx);
			relList.add(rel);
			queryatoms.put(rel, atstoadd);
			queryModels.add(new QueryTableModel());
		}
		queryModels.elementAt(idx).addQuery(atstoadd);
		myprimula.queryatoms.add(atstoadd);	
		this.buildQueryatomsTables(queryModels);
	}

	//updates the query atoms list
//	private void addAtoms(Rel rel, int[] tuple){
//		SparseRelStruc rstnew = new SparseRelStruc();
//		rstnew = (SparseRelStruc)myprimula.rels;
//
//		int[] temp = new int[tuple.length];
//		int pos = 0;
//		int length = tuple.length;
//		for(int x=0; x<tuple.length; x++){
//			temp[x] = tuple[x];
//		}
//		for(int i=0; i<length; i++){
//			if(elementNamesListModel.elementAt(tuple[i]).equals("*")){
//				Vector v = rstnew.getNames();
//				for(int j=0; j<v.size(); j++){
//					temp[pos] = j;
//					addAtoms(rel, temp);
//				}
//			}
//			else if(((String)elementNamesListModel.elementAt(tuple[i])).startsWith("[")){
//				Vector<BoolRel> attributeNames = rstnew.getBoolAttributes();
//				BoolRel nextattr;
//				for(int j =0; j<attributeNames.size();j++){
//					nextattr = attributeNames.elementAt(j);
//					if(((String)elementNamesListModel.elementAt(tuple[i])).equals("["+ nextattr +"*]")){
//						Vector<int[]> tuples = rstnew.allTrue(nextattr);
//						for(int k =0; k<tuples.size(); k++){
//							int[] temp2 = tuples.elementAt(k);
//							temp[pos] = temp2[0];
//							addAtoms(rel, temp);
//						}
//					}
//				}
//			}
//			else{
//				if(pos == length-1){
//					queryatoms.add(rel, temp);
//				}
//			}
//			pos++;
//		}
//		updateQueryatomsList(queryModel);
//		infoMessage.setText(rel.name.name+" ("+addedTuples+") added");
//		temp = null;
//	}

	/*
	 * Takes an  atom specification with (typed) wildcards (as specified by their
	 * position in the elements list), and 
	 * returns a GroundAtomList with all matching atoms
	 */
	private GroundAtomList buildAtoms(Rel rel, int[] tuple){
		SparseRelStruc rstnew = new SparseRelStruc();
		rstnew = (SparseRelStruc)myprimula.rels;

		GroundAtomList result = new GroundAtomList();
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
					result.add(buildAtoms(rel, temp));
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
							result.add(buildAtoms(rel, temp));
						}
					}
				}
			}
			else{
				if(pos == length-1){
					result.add(rel, temp);
				}
			}
			pos++;
		}
		
		infoMessage.setText(rel.name.name+" ("+addedTuples+") added");
		temp = null;
		return result;
	}

//	public void updateQueryatomsList(QueryTableModel qtm){
//		selectedQueryAtom = null;
//		qtm.reset();
//		Vector queries = queryatoms.allAtoms();
//		for(int i=0; i<queries.size(); ++i){
//			GroundAtom temp = (GroundAtom)queries.elementAt(i);
//			int nodes[] = temp.args;
//			Rel rel = temp.rel;
//			String names = ""+rel.name.name + "(";
//			for(int j=0; j<nodes.length; ++j){
//				if(j+1 < nodes.length){
//					names = names + elementNamesListModel.elementAt(nodes[j]) + ",";
//				}
//				else { //last item
//					names = names + elementNamesListModel.elementAt(nodes[j]);
//				}
//			}
//			names = names + ")";
//			String listItem = names;
//			qtm.addQuery(listItem);
//		}
//		querytable.updateUI();
//
//		if( myACEControl != null ) myACEControl.primulaQueryChanged();//keith cascio 20060620
//	}

	/**
	 * Before calling this method this.queryatoms and this.relIndex must contain current
	 * and consistent data
	 */
//	private void rebuildQueryAtomsPanel() {
//		queryatomsPanel.removeAll();
//		queryatomsScrolllists=new Vector<JScrollPane>();
//		querytables = new Vector<JTable>();
//		// First construct the appropriate number of gui elements:
//		for (int i =0;i<relIndex.size();i++) {
//			JScrollPane nextjsp = new JScrollPane();
//			JTable nextjt = new JTable();
//			queryatomsScrolllists.add(nextjsp);
//			queryatomsPanel.add(nextjsp);
//			nextjsp.getViewport().add(nextjt);
//		}
//		// Now connect to the data:
//		for (String r: relIndex.keySet()) {
//			int idx = relIndex.get(r);
//			querytables.elementAt(idx).setModel(queryModels.elementAt(idx));
//		}
//			
//		queryatomsPanel.updateUI();
//	}

	/**
	 * It is required that a vector 'qtm' of (empty) QueryTableModels is already 
	 * initialized. That makes it easier to call this method from different contexts
	 * when 'qtm' consists of different subclasses of QueryTableModel.
	 * @param qtm
	 */
	private void buildQueryatomsTables(Vector<? extends QueryTableModel> qtm) {
		// Initialize the GUI components:
		queryatomsPanel.removeAll();
		querytables = new Vector<JTable>();
		queryatomsScrolllists = new Vector<JScrollPane>();
		for (int i=0;i<queryModels.size();i++) {
			JScrollPane nextjsp = new JScrollPane();
			JTable nextjt = new JTable();
			querytables.add(nextjt);
			nextjt.setModel(qtm.elementAt(i));
			nextjt.setShowHorizontalLines(false);
			nextjt.setPreferredScrollableViewportSize(new Dimension(146, 100));
			nextjt.getColumnModel().getColumn(0).setHeaderValue("Query Atoms");
			nextjt.getColumnModel().getColumn(0).setPreferredWidth(100);
			nextjsp.getViewport().add(nextjt);
			queryatomsScrolllists.add(nextjsp);
			queryatomsPanel.add(nextjsp);
			
		}
		// Customize the table models
		for (Rel r: queryatoms.keySet()) {
			int idx = relIndex.get(r.name()); 
			buildQueryatomsTable(qtm.elementAt(idx),queryatoms.get(r));
		}
//		for (JTable jt: querytables)
//			jt.updateUI();
//		for (JScrollPane jsp: queryatomsScrolllists)
//			jsp.updateUI();
		queryatomsPanel.updateUI();
	}
	
	/**
	 * Sets the QueryTableModel 'qtm' to contain the atoms in 'queries'.
	 * All atoms should be of one relation only
	 * 
	 * Callers of this method must ensure that qtm is the appropriate 
	 * QueryTableModel for this relation.
	 * 
	 * @param qtm
	 * @param queries
	 */
	private void buildQueryatomsTable(QueryTableModel qtm, GroundAtomList queries){
		selectedQueryAtom = null;
		qtm.reset();
		
		for(int i=0; i<queries.size(); ++i){
			GroundAtom temp = (GroundAtom)queries.atomAt(i);
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
			qtm.addQuery(listItem);
		}
		//querytables.updateUI();

		if( myACEControl != null ) myACEControl.primulaQueryChanged();//keith cascio 20060620
	}
	
	
//	private void generateQueryatoms(){
//		LinkedList relstruct = new LinkedList();
//		queryatoms.reset();
//		LinkedList queryatoms = queryModel.getQuery();
//		for(int i=0; i<queryatoms.size(); i++){
//			String atom = ""+queryatoms.get(i);
//			//System.out.println("in generateQueryAtoms: " + atom);
//			String rel = atom.substring(0, atom.indexOf("("));
//			//rel = rel.substring(0, atom.indexOf(" "));
//			LinkedList elementNames = new LinkedList();
//			int comma = atom.indexOf("(")+1;
//			for(int j = atom.indexOf("("); j<atom.length(); j++){
//				String temp =""+ atom.charAt(j);
//				if(temp.equals(",")){
//					String element = atom.substring(comma, j);
//					elementNames.add(element);
//					comma = j+2;
//				}
//			}
//			String element = atom.substring(comma, atom.indexOf(")"));
//			elementNames.add(element);
//			int[] tuple = new int[elementNames.size()];
//			Rel relnew = null;
//			if(elementNames.size() == 1){
//				for(int m=0;m<attributesListModel.size();m++){
//					if(attributesListModel.get(m).toString().equals(rel)){
//						relnew = (Rel)attributesListModel.get(m);
//					}
//				}
//			}
//			else if(elementNames.size() == 2){
//				for(int m=0;m<binaryListModel.size();m++){
//					if(binaryListModel.get(m).toString().equals(rel)){
//						relnew = (Rel)binaryListModel.get(m);
//						//System.out.println("binaryListModel: "+binaryListModel.get(m).toString());
//					}
//				}
//			}
//			else {
//				for(int m=0;m<arbitraryListModel.size();m++){
//					if(((Rel)arbitraryListModel.get(m)).printname().equals(rel)){
//						relnew = (Rel)arbitraryListModel.get(m);
//					}
//				}
//			}
//			int [] args = new int [elementNames.size()];
//			for(int n=0; n<elementNames.size(); n++){
//				for(int o=0; o<elementNamesListModel.size(); o++){
//					if(elementNamesListModel.get(o).equals(elementNames.get(n))){
//						args[n] = o;
//					}
//				}
//			}
//			TempAtoms temp = new TempAtoms(relnew, args);
//			relstruct.add(temp);
//		}
//		for(int t=0; t<relstruct.size(); t++){
//			TempAtoms temp = (TempAtoms)relstruct.get(t);
//			addAtoms(temp.getRel(), temp.getArgs());
//		}
//	}

//	private double[][] computeQueryBatch(){
//		/* Computes the probability of each query atom in all
//		 * data cases contained in myprimula.rdata
//		 * 
//		 * Assumes that all probabilities can be computed by just
//		 * evaluating the probability formula, i.e., no dependence on 
//		 * unobserved atoms. 
//		 * 
//		 * Returns a queryatoms.length x 7 double matrix, containing for each
//		 * query atom:
//		 * 
//		 * count of true positives
//		 * count of false positives
//		 * count of false negatives
//		 * count of true negatives
//		 * count of atoms for which probability was not computed, because of dependence on unobserved atom
//		 * count of atoms for which a truth value was not given in the data case
//		 * average log-likelihood 
//		 */
//		
//		double[][] result = new double[queryatoms.size()][7];
//		
//		RelData rdata = myprimula.getReldata();
//		
//		if (rdata.size() > 1){
//			System.out.println("Warning: data available for more than one input domain. Will evaluate queries only "
//					+ "for first input domain");
//		}
//		RelDataForOneInput rdoi = rdata.caseAt(0);
//		RelStruc A = rdoi.inputDomain();
//		OneStrucData osd;
//		
//		GroundAtom gat;
//		CPModel pf;
//		String[] varargs;
//		int[] intargs;
//		double prob=0;
//		int tv;
//		RBN rbn = myprimula.rbn;
//		Boolean predpos=false;
//		for (int i=0;i<rdoi.numberOfObservations();i++){
//			osd = rdoi.oneStrucDataAt(i);
//			for (int j=0;j<queryatoms.size();j++){
//				gat=queryatoms.atomAt(j);
//				pf = rbn.cpmodel(gat.rel());
//				varargs = rbn.args(gat.rel());
//				intargs = gat.args();
//				try{
//					prob = (double)pf.evaluate(A, 
//							osd, 
//							varargs, 
//							intargs,
//							0,
//							true,  
//							true, 
//							null, 
//							false,
//							null,
//							null,
//							ProbForm.RETURN_ARRAY,
//							true,
//							null)[0];
//				}
//				catch (RBNCompatibilityException ex){System.out.println(ex);}
//				if (prob!=Double.NaN)
//					predpos = (prob>0.5);
//				else {
//					result[j][4]++; // no prediction
//					break;
//				}
//				result[j][6]+= prob;
//				// Now get the actual truth value
//				tv=osd.truthValueOf(gat);
//				switch (tv){
//				case 1:
//					if (predpos)
//						result[j][0]++; // true positive
//					else
//						result[j][2]++; // false negative
//					break;
//				case 0:
//					if (predpos)
//						result[j][1]++; // false positive
//					else
//						result[j][3]++; // true negative
//					break;
//				case -1:
//					result[j][5]++; // no ground truth
//				}
//			} //for (int j=0;j<queryatoms.size();j++)
//		} // (int i=0;i<rdoi.numberOfObservations();i++)
//		
//		/* Normalize the likelihood */
//		double numevaluated;
//		for (int j=0;j<queryatoms.size();j++){
//			numevaluated = result[j][0]+result[j][1]+result[j][2]+result[j][3];
//			result[j][6]=result[j][6]/numevaluated;
//		}
//		
//		// Temporary:
//		double TP=0;
//		double FP=0;
//		double FN=0;
//		double TN=0;
//		
//		System.out.println("[TP,FP,FN,TN,Pred. not evaluated,Ground truth unknown, P(positive)]");
//		for (int i=0;i< result.length;i++){
//			System.out.println(queryatoms.atomAt(i).asString(A)+'\t'+StringOps.arrayToString(result[i], "[", "]"));
//			TP=TP+result[i][0];
//			FP=FP+result[i][1];
//			FN=FN+result[i][2];
//			TN=TN+result[i][3];
//		}
//		
//		double acc= (TP+TN)/(TP+TN+FP+FN);
//		
//		System.out.println("TP: " + TP +" FP: " + FP + " FN: " + FN + " TN: "+ TN);
//		System.out.println("Accuracy: " + acc );
//		return result;
//	}
//	
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
			CPModel pf = rbn.cpmodel(r);
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
										0,
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
										0,
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
		// TODO: make this work again!
		if (o instanceof SampleProbs){
			for (Rel r: queryatoms.keySet()) {
				MCMCTableModel mcmct = mcmcModels.elementAt(relIndex.get(r.name()));
				
				double [][] prob= ((SampleProbs)o).getProbs(r);
				for(int i=0; i<prob.length; i++){
					String[] probsasstring = new String[prob[i].length];
					for (int j=0; j<probsasstring.length; j++)
						probsasstring[j]=""+prob[i][j];
					mcmct.setProb(probsasstring,i);
				}
				double [][] var= ((SampleProbs)o).getVariance(r);
				for(int i=0; i<var.length; i++){
					String[] varsasstring = new String[var[i].length];
					for (int j=0; j<varsasstring.length; j++)
						varsasstring[j]=""+var[i][j];
					mcmct.setVar(varsasstring,i);
				}
				sampleSize.setText(""+((SampleProbs)o).getSize());
				Double dweight = new Double(((SampleProbs)o).getWeight());
				weight.setText(""+ myio.StringOps.doubleConverter(dweight.toString()));
				}
		}
		
		if (o instanceof MapVals){
			for (Rel r: queryatoms.keySet()) {
				MAPTableModel mapt = mapModels.elementAt(relIndex.get(r.name()));
				int[] mvals = ((MapVals) o).getMVs(r);
				for(int i=0; i<mvals.length; i++){
					mapt.setValue(r.get_String_val(mvals[i]), i);
				}
			}
			mapRestarts.setText("" +((MapVals)o).getRestarts());
			mapLL.setText("" +((MapVals)o).getLLstring());
		}
//		
//		/** keith cascio 20060511 ... */
//		//dataModel.resetACE();
//		/** ... keith cascio */
//		
		queryatomsPanel.updateUI();
	}
	
//	public OneStrucData getMapValuesAsInst(){
//		LinkedList<String> mapvals = mapModel.getMapValues();
//		LinkedList<String> queryats = mapModel.getQuery();
//		OneStrucData result = new OneStrucData();
//		result.setParentRelStruc(myprimula.getRels());
//		
//		Iterator<String> itq = queryats.iterator();
//		
//		for (Iterator<String> itmap = mapvals.iterator(); itmap.hasNext();) {
//			System.out.println(itq.next() + " " + itmap.next());
//		}
////		for (int i=0;i< mapatoms.size();i++){
////			result.add(mapatoms.atomAt(i),instvals[i],"?");
////		}
//		return result;
//	}
	


	private void buildMAPTables() {
		/* creating MapModels
		 * It is required that querytables and queryatomsScrolllists exist and have the 
		 * same length as relList
		 */
		mapModels=new Vector<MAPTableModel>();
		for (int i=0;i<querytables.size();i++) {
			
			Rel r = relList.elementAt(i);
			MAPTableModel maptm = new MAPTableModel(queryModels.elementAt(i),relList.elementAt(i));
			
			mapModels.add(maptm);
			JTable qt = querytables.elementAt(i);
			
			qt.setModel(maptm);
			qt.getColumnModel().getColumn(0).setPreferredWidth(150);
			qt.getColumnModel().getColumn(1).setPreferredWidth(100);
			qt.setShowHorizontalLines(false);
			qt.setPreferredScrollableViewportSize(new Dimension(300, 100));
			//table header values
			qt.getColumnModel().getColumn(0).setHeaderValue("Query");
			qt.getColumnModel().getColumn(1).setHeaderValue("MAP value");
		}
	}
	
	
	private void buildMCMCTables() {
		/* creating mcmcModels
		 * It is required that querytables and queryatomsScrolllists exist and have the 
		 * same length as relList
		 */
		mcmcModels=new Vector<MCMCTableModel>();
		//Vector<JTable> newquerytables = new Vector<JTable>();
		for (int i=0;i<querytables.size();i++) {
			
			Rel r = relList.elementAt(i);
			MCMCTableModel mcmctm = new MCMCTableModel(queryModels.elementAt(i),relList.elementAt(i));
			
			mcmcModels.add(mcmctm);
			JTable qt = querytables.elementAt(i);
			//newquerytables.add(qt);
			qt.setModel(mcmctm);
			qt.getColumnModel().getColumn(0).setPreferredWidth(250);
			for (int c=1;c<qt.getColumnCount();c++)
				qt.getColumnModel().getColumn(c).setPreferredWidth(100);
			qt.setShowHorizontalLines(false);
			qt.setPreferredScrollableViewportSize(new Dimension(100+80*(int)r.numvals(), 100));
			//table header values
			qt.getColumnModel().getColumn(0).setHeaderValue("Query");
			for (int j=0;j<r.numvals();j++) {
				qt.getColumnModel().getColumn(2*j+1).setHeaderValue(r.get_String_val(j));
				qt.getColumnModel().getColumn(2*(j+1)).setHeaderValue("+/-");
			}
//			qt.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
//			qt.doLayout();
		}
	}
	
//	private void setMAPTable() {
//		for (int i=0;i<querytables.size();i++) {
//			JTable qt = querytables.elementAt(i);
//			qt.setModel(mapModels.elementAt(i));
//			qt.setShowHorizontalLines(false);
//			qt.setPreferredScrollableViewportSize(new Dimension(146, 100));
//			//table header values
//			qt.getColumnModel().getColumn(0).setHeaderValue("Query Atoms");
//			qt.getColumnModel().getColumn(1).setHeaderValue("MAP");
//			qt.getColumnModel().getColumn(0).setPreferredWidth(150);
//		}
//	}
	
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == inferencePane) {
			switch (inferencePane.getSelectedIndex()) {
			case 0: // Query tab
				buildQueryatomsTables(queryModels);
				queryatomsPanel.updateUI();
				outerQueryPane.updateUI();
				break;
			case 1: // MCMC tab
				buildMCMCTables();
				queryatomsPanel.updateUI();
				outerQueryPane.updateUI();
				break;
			case 2: // Test tab
				break;
			case 3: // MAP tab
				buildMAPTables();
				queryatomsPanel.updateUI();
				outerQueryPane.updateUI();
				break;
			case 4: // ACE tab
				break;
			}
		}
	}
	
}
