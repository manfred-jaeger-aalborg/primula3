package RBNgui;

import RBNinference.MapVals;
import RBNinference.SampleProbs;
import RBNpackage.*;
import edu.ucla.belief.ace.Control;
import edu.ucla.belief.ace.SettingsPanel;
import edu.ucla.belief.ui.primula.SamiamManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

public class InferenceModuleGUI extends JFrame implements Observer, ActionListener, MouseListener, Control.ACEControlListener, ChangeListener {

    InferenceModule inferenceModuleCore;
    Primula myprimula;

    PrimulaGUI myprimulaGUI;

    private JTabbedPane inferencePane   = new JTabbedPane();
    /**
     * @uml.property name="relationsLabel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JLabel relationsLabel = new JLabel("Relations");
    /**
     * @uml.property name="relationsList"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JList relationsList = new JList();

    /**
     * keith cascio 20060511 ...
     *
     * @uml.property name="relationsScrollList"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JScrollPane relationsScrollList;//     = new JScrollPane();
    /**
     * ... keith cascio
     *
     * @uml.property name="valuesLabel"
     * @uml.associationEnd multiplicity="(1 1)"
     */

    private JLabel valuesLabel = new JLabel("Values");
    /**
     * @uml.property name="valuesList"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JList valuesList = new JList();

    /**
     * keith cascio 20060511 ...
     *
     * @uml.property name="valuesScrollList"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JScrollPane valuesScrollList;//         = new JScrollPane();
    /**
     * ... keith cascio
     *
     * @uml.property name="arbitraryLabel"
     * @uml.associationEnd multiplicity="(1 1)"
     */

    private JLabel elementNamesLabel = new JLabel("Element names");
    /**
     * @uml.property name="elementNamesList"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JList elementNamesList = new JList();

    /**
     * @uml.property name="elementNamesScrollList"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JScrollPane elementNamesScrollList = new JScrollPane();

    /**
     * @uml.property name="instantiationsLabel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JLabel instantiationsLabel = new JLabel("Instantiations");
    /**
     * @uml.property name="instantiationsList"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JList instantiationsList = new JList();

    /**
     * @uml.property name="instantiationsScrollList"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JScrollPane instantiationsScrollList = new JScrollPane();

    /**
     * @uml.property name="queryatomsLabel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    //private JLabel queryatomsLabel           = new JLabel("Query atoms");
    //fields to display sample size and weight
    /**
     * @uml.property name="sampleSizeText"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JLabel sampleSizeText = new JLabel("Sample Size");

    private JLabel mapRestartsText = new JLabel("Restarts");
    /**
     * @uml.property name="sampleSize"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JTextField sampleSize = new JTextField();

    private JTextField mapRestarts = new JTextField();
    /**
     * @uml.property name="weightText"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JLabel weightText = new JLabel("Weight");

    private JLabel mapLLText = new JLabel("Likelihood");
    /**
     * @uml.property name="weight"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JTextField weight = new JTextField();

    private JTextField mapLL = new JTextField();

    /**
     * @uml.property name="queryatomsScrollList"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JScrollPane queryatomsScrollList = new JScrollPane();

    /**
     * One JScrollPane for each relation for which we have a query atom
     * <p>
     * Each JScrollPane contains a JTable associated with a QueryTableModel
     * or a subclass thereof (MCMCTableModel, MAPTableModel, ...)
     * <p>
     * The queryatomsScrolllists are embedded in the queryAtomsPanel
     */
    private Vector<JScrollPane> queryatomsScrolllists = new Vector<JScrollPane>();

    /**
     * @uml.property name="querytable"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private Vector<JTable> querytables = new Vector<JTable>();

    /**
     * @uml.property name="trueButton"
     * @uml.associationEnd multiplicity="(1 1)"
     */
//	private JButton instButton     = new JButton("Instantiation");

    /**
     * @uml.property name="queryButton"
     * @uml.associationEnd multiplicity="(1 1)"
     */
//	private JButton queryButton    = new JButton("Query");
    /**
     * @uml.property name="infoMessage"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JLabel infoMessage = new JLabel(" ");

    /**
     * @uml.property name="emptySpace"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private Border emptySpace = BorderFactory.createEmptyBorder(10, 25, 5, 25);

    ImageIcon toggleicon = new ImageIcon("src/main/java/Icons/toggle.png");

    ImageIcon cwaicon = new ImageIcon("src/main/java/Icons/cwa.png");

    ImageIcon deleteicon = new ImageIcon("src/main/java/Icons/delete.png");

    ImageIcon clearicon = new ImageIcon("src/main/java/Icons/clear.png");

    private JButton toggleTruthButton = new JButton(toggleicon);

    private JButton cwaButton = new JButton(cwaicon);

    private JButton delInstButton = new JButton(deleteicon);

    private JButton delAllInstButton = new JButton(clearicon);

//	ImageIcon saveicon = new ImageIcon("./Icons/save.png");
//	private JButton saveInstButton    		= new JButton(saveicon);

//	ImageIcon loadicon = new ImageIcon("./Icons/load.png");
//	private JButton loadInstButton  		= new JButton(loadicon);

    /**
     * @uml.property name="delQueryAtomButton"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JButton delQueryAtomButton = new JButton("Delete");
    /**
     * @uml.property name="delAllQueryAtomButton"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JButton delAllQueryAtomButton = new JButton("Clear");

    /**
     * @uml.property name="sampleInfoPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel sampleInfoPanel = new JPanel(new GridLayout(1, 4, 3, 1));
    private JPanel mapInfoPanel = new JPanel(new GridLayout(1, 4, 3, 1));

//	/**
//	 * @uml.property  name="deletesamplePanel"
//	 * @uml.associationEnd  multiplicity="(1 1)"
//	 */
//	private JPanel deletesamplePanel   = new JPanel(new BorderLayout());

    /**
     * @uml.property name="relationsPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel relationsPanel = new JPanel(new BorderLayout());
    /**
     * @uml.property name="valuesPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel valuesPanel = new JPanel(new BorderLayout());

    /**
     * @uml.property name="arityPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel arityPanel = new JPanel(new GridLayout(1, 3, 0, 3));
    /**
     * @uml.property name="elementNamesPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel elementNamesPanel = new JPanel(new BorderLayout());
    /**
     * @uml.property name="instantiationsPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel instantiationsPanel = new JPanel(new BorderLayout());

    //  private JPanel listsPanel          = new JPanel(new GridLayout(1, 3, 10, 1));
    /**
     * @uml.property name="atomsPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel atomsPanel = new JPanel(new BorderLayout());
    /**
     * @uml.property name="instButtonsPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel instButtonsPanel = new JPanel(new GridLayout(1, 3));
//	private JPanel truthButtonsPanel   		= new JPanel(new GridLayout(1, 2));
    /**
     * @uml.property name="buttonsPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
//	private JPanel buttonsPanel        		= new JPanel(new FlowLayout());
    /**
     * @uml.property name="buttonsAndInfoPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel buttonsAndInfoPanel = new JPanel();


    /**
     * @uml.property name="queryatomsPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel queryatomsPanel = new JPanel(new FlowLayout());
    private JScrollPane outerQueryPane;// = new JScrollPane();
    /**
     * @uml.property name="queryatomsButtonsPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel queryatomsButtonsPanel = new JPanel(new GridLayout(1, 2));
    /**
     * @uml.property name="qbPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel qbPanel = new JPanel(new BorderLayout());

    /**
     * @uml.property name="eiPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel eiPanel = new JPanel(new GridLayout(1, 2));
    /**
     * keith cascio 20060511 ...
     *
     * @uml.property name="qeiPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel qeiPanel = new JPanel(new GridBagLayout());//new GridLayout(2,1));
    /**
     * ... keith cascio
     *
     * @uml.property name="samplingPanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */

    private JPanel samplingPanel = new JPanel(new GridLayout(1, 4));
    /**
     * @uml.property name="settingsSampling"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JButton settingsSampling = new JButton("Settings Sampling");
    /**
     * @uml.property name="startSampling"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JButton startSampling = new JButton("Start");
    /**
     * @uml.property name="pauseSampling"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JButton setPrediction = new JButton("Predict");
    /**
     * @uml.property name="stopSampling"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JButton stopSampling = new JButton("Stop");
    //   private	JPanel samplingfile 			 = new JPanel(new GridLayout(2,1));


    private JPanel evalPanel = new JPanel(new GridLayout(1, 4));
    private JButton startEval = new JButton("Test");

    private JPanel mapPanel = new JPanel(new GridLayout(1, 4));

    private JButton settingsMap = new JButton("Settings MAP");

    private JButton startMap = new JButton("Start");
    /**
     * @uml.property name="pauseSampling"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JButton setMapVals = new JButton("Set MAP Vals");
    /**
     * @uml.property name="stopSampling"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JButton stopMap = new JButton("Stop");
    //   private	JPanel samplingfile 			 = new JPanel(new GridLayout(2,1));


    /**
     * keith cascio 20060511 ...
     *
     * @uml.property name="acePanel"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JPanel acePanel = new JPanel(new GridLayout(1, 4));
    /**
     * @uml.property name="aceButtonSettings"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JButton aceButtonSettings = new JButton("ACE settings");
    //private JButton aceButtonCompile  = new JButton( "Compile" );
    //private JButton aceButtonLoad     = new JButton( "Load" );
    //private JButton aceButtonCompute  = new JButton( "Compute" );
    /**
     * @uml.property name="aceProgressBar"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JProgressBar aceProgressBar;

    /**
     * @uml.property  name="myACESettingsPanel"
     * @uml.associationEnd
     */
    private SettingsPanel myACESettingsPanel;

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

    //  final private Color backgroundColor;
    /**
     * @uml.property  name="toggleTruthButton"
     * @uml.associationEnd  multiplicity="(1 1)"
     */

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

    /**
     * @uml.property  name="swindow"
     * @uml.associationEnd  inverse="evidence:RBNgui.SettingsSampling"
     */
    private RBNgui.SettingsSampling swindow;
    private RBNgui.SettingsMAP mapwindow;

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
     * selQueryRel is the index of the query table that contains the currently selected atom
     *
     */
    private int selQueryRel;
    /**
     * the selected atom in the selected query table
     *
     */
    private int selQueryAtom;


    /**
     * @uml.property  name="addedTuples"
     */
    private String addedTuples = "";

    /**
     * @uml.property  name="index"
     * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
     */
    private int index;

    public void setupUI() {
        buildQueryatomsTables(inferenceModuleCore.queryModels);

        /* Top panel with list of attributes/binary relations/arbitrary relations */
        relationsList.setModel(inferenceModuleCore.relationsListModel);
        relationsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        /** keith cascio 20060511 ... */
        relationsScrollList = new JScrollPane( relationsList );//relationsScrollList.getViewport().add(relationsList);
        Dimension sizePreferred = relationsScrollList.getPreferredSize();
        sizePreferred.height = 64;
        relationsScrollList.setPreferredSize( sizePreferred );
        /** ... keith cascio */
        relationsPanel.add(relationsLabel, BorderLayout.NORTH);
        relationsPanel.add(relationsScrollList, BorderLayout.CENTER);

        valuesList.setModel(inferenceModuleCore.valuesListModel);
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


        elementNamesList.setModel(inferenceModuleCore.elementNamesListModel);
        elementNamesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        elementNamesScrollList.getViewport().add(elementNamesList);
        elementNamesPanel.add(elementNamesLabel, BorderLayout.NORTH);
        elementNamesPanel.add(elementNamesScrollList, BorderLayout.CENTER);
        eiPanel.add(elementNamesPanel);

        instantiationsList.setModel(inferenceModuleCore.instantiationsListModel);
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

//		instantiationsPanel.add(instButtonsPanel, BorderLayout.SOUTH);



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
//		deletesamplePanel.add(queryatomsButtonsPanel, BorderLayout.NORTH);

        inferencePane.add("Evidence",instButtonsPanel);

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


        toggleTruthButton.setBackground(PrimulaGUI.COLOR_YELLOW);
        cwaButton.setBackground(PrimulaGUI.COLOR_YELLOW);
        delInstButton.setBackground(PrimulaGUI.COLOR_YELLOW);
        delAllInstButton.setBackground(PrimulaGUI.COLOR_YELLOW);
//		saveInstButton.setBackground(Primula.COLOR_RED);
//		loadInstButton.setBackground(Primula.COLOR_RED);
        delQueryAtomButton.setBackground(PrimulaGUI.COLOR_GREEN);
        delAllQueryAtomButton.setBackground(PrimulaGUI.COLOR_GREEN);
        settingsSampling.setBackground(PrimulaGUI.COLOR_BLUE);
        startSampling.setBackground(PrimulaGUI.COLOR_GREEN);
        stopSampling.setBackground(PrimulaGUI.COLOR_GREEN);
        setPrediction.setBackground(PrimulaGUI.COLOR_GREEN);
        startEval.setBackground(PrimulaGUI.COLOR_GREEN);
        settingsMap.setBackground(PrimulaGUI.COLOR_BLUE);
        startMap.setBackground(PrimulaGUI.COLOR_GREEN);
        stopMap.setBackground(PrimulaGUI.COLOR_GREEN);
        setMapVals.setBackground(PrimulaGUI.COLOR_GREEN);

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
            button.setBackground( PrimulaGUI.COLOR_GREEN );
        }

        aceButtonSettings.addActionListener( this );
        aceButtonSettings.setBackground( PrimulaGUI.COLOR_BLUE );
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
        btn.setBackground( PrimulaGUI.COLOR_GREEN.brighter() );
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
//		buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsAndInfoPanel.add(qeiSplit);
//		buttonsAndInfoPanel.add(buttonsPanel);

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
                        if (inferenceModuleCore.settingssamplingwindowopen)
                            swindow.dispose();
                        if (inferenceModuleCore.settingsmapwindowopen)
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
        this.setVisible(true);
        /** keith cascio 20060511 ... */
        SamiamManager.centerWindow( this );
        /** ... keith cascio */
        inferenceModuleCore.mapSearchAlg = 3;
    }

    public InferenceModuleGUI(InferenceModule evidenceModule) {
        inferenceModuleCore = evidenceModule;
        inferenceModuleCore.setInferenceModuleGUI(this);
        myprimula = evidenceModule.myprimula;
        myprimulaGUI = evidenceModule.myprimula.myprimulaGUI;
        setupUI();
    }

    public void actionPerformed( ActionEvent e )
    {
        Object source = e.getSource();

//		if( source == instButton ){
//			el_pos=0;
//			instButton.setBackground(Primula.COLOR_BLUE_SELECTED);
//			queryButton.setBackground(Primula.COLOR_BLUE);
//			elementNamesList.clearSelection();
//			queryModeOn = false;
//			infoMessage.setText(" ");
//		}

        if( source == toggleTruthButton ){
            if(selectedInstAtom != null && selectedInstAtom.rel instanceof BoolRel){
                if(selectedInstAtom.val == 1){
                    inferenceModuleCore.inst.add((BoolRel)selectedInstAtom.rel, selectedInstAtom.args, false,"?");
                }
                else{
                    inferenceModuleCore.inst.add((BoolRel)selectedInstAtom.rel, selectedInstAtom.args, true,"?");
                }
                inferenceModuleCore.updateInstantiationList();
                instantiationsPanel.updateUI();
                myprimula.updateBavaria();
            }
        }
        else if( source == cwaButton ){
            if (selected_rel != null && selected_rel instanceof BoolRel){
                inferenceModuleCore.inst.applyCWA((BoolRel)selected_rel);
                inferenceModuleCore.updateInstantiationList();
                instantiationsPanel.updateUI();
                myprimula.updateBavaria();
            }
            //xxx
        }
        else if( source == delInstButton){
            if(selectedInstAtom != null){
                int selected = instantiationsList.getSelectedIndex();
                inferenceModuleCore.inst.delete(selectedInstAtom.rel, selectedInstAtom.args);
                inferenceModuleCore.updateInstantiationList();
                instantiationsPanel.updateUI();
                int listsize = instantiationsList.getModel().getSize()-1;

                if(selected >= listsize ){
                    selected--;
                }
                if(selected != -1){
                    instantiationsList.setSelectedIndex(selected);
                    Vector instAtoms = inferenceModuleCore.inst.allInstAtoms();
                    selectedInstAtom = (InstAtom) instAtoms.elementAt(selected);
                }
                else selectedInstAtom = null;
            }
        }
        else if( source == delAllInstButton ){
            inferenceModuleCore.inst.clear();
            inferenceModuleCore.updateInstantiationList();
            instantiationsPanel.updateUI();
            myprimula.updateBavaria();
        }

        else if( source == delQueryAtomButton ){
            if(selQueryRel>=0){
                QueryTableModel qtm = inferenceModuleCore.queryModels.elementAt(selQueryRel);
                if (selQueryAtom >= 0)
                    qtm.removeQuery(selQueryAtom);
                selQueryAtom = -1;
                this.buildQueryatomsTables(inferenceModuleCore.queryModels);
            }
        }
        else if(source == delAllQueryAtomButton){
            inferenceModuleCore.queryModels=new Vector<QueryTableModel>();
            inferenceModuleCore.queryatoms = new Hashtable<Rel, GroundAtomList>();
            inferenceModuleCore.relList = new Vector<Rel>();
            inferenceModuleCore.relIndex = new Hashtable<String,Integer>();
            this.buildQueryatomsTables(inferenceModuleCore.queryModels);
        }
        else if( source == settingsSampling ){
            if (!inferenceModuleCore.settingssamplingwindowopen){
                swindow = new RBNgui.SettingsSampling( inferenceModuleCore );
                inferenceModuleCore.settingssamplingwindowopen = true;
            }
        }
        else if( source == startSampling ){
            inferenceModuleCore.startSampleThread();
            // Now update UI on the Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                queryatomsPanel.updateUI();
                infoMessage.setText(" Starting Sampling ");
                startSampling.setEnabled( false );
            });
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
//          instantiationsPanel.updateUI();
//			myprimula.updateBavaria();
//		}
        else if( source == stopSampling){
            inferenceModuleCore.stopSampling();

            if (!inferenceModuleCore.noLog()){
                try{
                    inferenceModuleCore.logwriter.flush();
                    if (inferenceModuleCore.logfilename != "")
                        inferenceModuleCore.logwriter.close();
                }
                catch (java.io.IOException ex){System.err.println(ex);};
            }
            infoMessage.setText(" Stop Sampling ");
            inferenceModuleCore.pausemcmc = false;
            startSampling.setEnabled( true );
        }
        else if (source == startEval){
            //computeQueryBatch();
            inferenceModuleCore.evaluateAccuracy();
        }
        else if (source == startMap){
            inferenceModuleCore.startMapThread();
        }
        else if (source == setMapVals){
            OneStrucData result = new OneStrucData();
            result.setParentRelStruc(myprimula.getRels());
            for (int i=0;i<querytables.size();i++) {
                Rel r = inferenceModuleCore.relList.elementAt(i);
                MAPTableModel mtm = inferenceModuleCore.mapModels.elementAt(i);
                for (int j=0;j<mtm.rownum;j++) {
                    GroundAtom gat = inferenceModuleCore.queryatoms.get(r).atomAt(j);
                    String mv = (String)mtm.getValueAt(j,1);
                    int v = r.get_Int_val(mv);
                    result.add(gat, v, "?");
                }
            }
            inferenceModuleCore.inst.add(result);
            inferenceModuleCore.updateInstantiationList();
            instantiationsPanel.updateUI();
            myprimula.updateBavaria();
        }
        else if( source == stopMap){
            //			maprestarts = false;
            inferenceModuleCore.getMapthr().setRunning(false);
            infoMessage.setText(" Stop MAP ");
        }
        else if( source == settingsMap ){
            if (!inferenceModuleCore.settingsmapwindowopen){
                mapwindow = new RBNgui.SettingsMAP( inferenceModuleCore );
                inferenceModuleCore.settingsmapwindowopen = true;
            }
        }
        /** keith cascio 20060511 ... */
        else if( source == aceButtonSettings ) doAceSettings();
        /** ... keith cascio */
    }

    /** @author keith cascio
     @since 20060602 */
    public void forgetAll(){
        if( inferenceModuleCore.myACEControl != null ) inferenceModuleCore.myACEControl.forgetAll();
    }

    /** @author keith cascio
     @since  20060725 */
    public void relationalStructureEdited(){
        if( inferenceModuleCore.myACEControl != null ) inferenceModuleCore.myACEControl.clear();
    }

    /** @author keith cascio
     @since 20060511 */
    private void doAceSettings(){
//        if( myACESettingsPanel == null ) myACESettingsPanel = new SettingsPanel();
//        myACESettingsPanel.show( (Component)InferenceModule.this, myprimula.getPreferences().getACESettings() );
    }

    /** @author keith cascio
     @since 20060511 */
    public Control getACEControl(){
        if( inferenceModuleCore.myACEControl == null ){
            inferenceModuleCore.myACEControl = new Control( myprimula );
            inferenceModuleCore.myACEControl.setParentComponent( (Component) this );
            inferenceModuleCore.myACEControl.setProgressBar( InferenceModuleGUI.this.getACEProgressBar() );
            inferenceModuleCore.myACEControl.set( myprimula.getPreferences().getACESettings() );
            inferenceModuleCore.myACEControl.addListener( (Control.ACEControlListener) this );
            //myACEControl.setDataModel( InferenceModule.this.queryModel ); //TODO
            inferenceModuleCore.myACEControl.setInfoMessage( InferenceModuleGUI.this.infoMessage );
        }
        return inferenceModuleCore.myACEControl;
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
                    names = names + inferenceModuleCore.elementNamesListModel.elementAt(nodes[j]) + ",";
                }
                else { //last item
                    names = names + inferenceModuleCore.elementNamesListModel.elementAt(nodes[j]);
                }
            }
            names = names + ")";
            String listItem = names;
            qtm.addQuery(listItem);
        }
        //querytables.updateUI();

        if( inferenceModuleCore.myACEControl != null ) inferenceModuleCore.myACEControl.primulaQueryChanged();//keith cascio 20060620
    }

    public void update(Observable o, Object arg){
        // TODO: make this work again! --
        if (o instanceof SampleProbs){
            for (Rel r: inferenceModuleCore.getQueryatoms().keySet()) {
                MCMCTableModel mcmct = inferenceModuleCore.mcmcModels.elementAt(inferenceModuleCore.relIndex.get(r.name()));

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
                Double dweight = Double.valueOf(((SampleProbs)o).getWeight());
                weight.setText(""+ myio.StringOps.doubleConverter(dweight.toString()));
            }
        }

        if (o instanceof MapVals){
            if (!inferenceModuleCore.mapModels.isEmpty()) { // if we do not use the GUI mapModels can or should be empty
                for (Rel r : inferenceModuleCore.getQueryatoms().keySet()) {
                    MAPTableModel mapt = inferenceModuleCore.mapModels.elementAt(inferenceModuleCore.relIndex.get(r.name()));
                    int[] mvals = ((MapVals) o).getMVs(r);
                    for (int i = 0; i < mvals.length; i++) {
                        mapt.setValue(r.get_String_val(mvals[i]), i);
                    }
                }
                mapRestarts.setText("" + ((MapVals) o).getRestarts());
                mapLL.setText("" + ((MapVals) o).getLLstring());
            }
        }
//
//		/** keith cascio 20060511 ... */
//		//dataModel.resetACE();
//		/** ... keith cascio */
//
        queryatomsPanel.updateUI();
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

            inferenceModuleCore.valuesListModel.removeAllElements();
            elementNamesList.clearSelection();
            int index = relationsList.locationToIndex(e.getPoint());
            if(index >= 0){
                selected_rel = (Rel)inferenceModuleCore.relationsListModel.elementAt(index);
                if (selected_rel instanceof BoolRel)
                    cwaButton.setEnabled( true );
                else
                    cwaButton.setEnabled( false );
                el_pos=0;
                inferenceModuleCore.element_tuple = new int[selected_rel.getArity()];
                addedTuples ="";
                for (int i=0;i<selected_rel.numvals();i++)
                    inferenceModuleCore.valuesListModel.addElement(selected_rel.get_String_val(i));
                infoMessage.setText(selected_rel.name.name);
            }
            if(selected_rel.getArity()==0 && queryModeOn) {
                addQueryAtoms(selected_rel, new int[0]);
                infoMessage.setText(selected_rel.name.name+" ("+addedTuples+")");
            }
        }

        else if( source == valuesList ){
            el_pos=0;
            elementNamesList.clearSelection();
            int index = valuesList.locationToIndex(e.getPoint());
            if(index >= 0){
                String valstring = (String)inferenceModuleCore.valuesListModel.elementAt(index);
                selected_val=selected_rel.get_Int_val(valstring);
            }
            if(selected_rel.getArity()==0 && !queryModeOn) {
                inferenceModuleCore.inst.add( selected_rel, new int[1][0], selected_val,"?");
                inferenceModuleCore.updateInstantiationList();
                instantiationsPanel.updateUI();
            }
        }

        else if( source == elementNamesList ){
            int selected_element = elementNamesList.locationToIndex(e.getPoint());
            if(!inferenceModuleCore.sampling){
                if(selected_rel != null && selected_rel.getArity()>0){  //relation should be selected first
                    inferenceModuleCore.element_tuple[el_pos] = selected_element;

                    if (el_pos<selected_rel.getArity()-1) {
                        el_pos++;
                        addedTuples += (String)inferenceModuleCore.elementNamesListModel.elementAt(inferenceModuleCore.element_tuple[index]) +", ...";
                    }
                    else { // tuple now complete
                        addedTuples += (String)inferenceModuleCore.elementNamesListModel.elementAt(inferenceModuleCore.element_tuple[index]);
                        if(queryModeOn){
                            addQueryAtoms(selected_rel, inferenceModuleCore.element_tuple);
                            infoMessage.setText(selected_rel.name.name+" ("+addedTuples+")");
                        }
                        else{
                            int[][] instantiations = inferenceModuleCore.allMatchingTuples(inferenceModuleCore.element_tuple);
                            inferenceModuleCore.inst.add(selected_rel, instantiations, selected_val,"?");
                            inferenceModuleCore.updateInstantiationList();
                            instantiationsPanel.updateUI();
                            infoMessage.setText(selected_rel.name.name+"("+addedTuples+ ") = "
                                    +selected_rel.get_String_val(selected_val));
                        }

                        // re-init for next tuple construction
                        inferenceModuleCore.element_tuple = new int[selected_rel.getArity()];
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
                Vector instAtoms = inferenceModuleCore.inst.allInstAtoms();
                selectedInstAtom = (InstAtom)instAtoms.elementAt(index);
            }
            else
                selectedInstAtom = null;
        }
        else {
            for (int i=0;i<querytables.size();i++) {
                if ( source == querytables.elementAt(i) ){
                    selQueryRel=i;
                    int index = querytables.elementAt(i).rowAtPoint(e.getPoint());
                    if(index>=0)
                        selQueryAtom = index;
                }
            }
        }

    }
    //          Invoked when a mouse button has been pressed on a component.
    public void mouseReleased(MouseEvent e) {
        Object source = e.getSource();
    }
    //          Invoked when a mouse button has been released on a component.

    //user adds an new element or renames the element name (in Bavaria)
    public void addOrRenameElementName(){
        int selected = elementNamesList.getSelectedIndex();
        inferenceModuleCore.elementNamesListModel.clear();
        inferenceModuleCore.readElementNames();
        inferenceModuleCore.updateInstantiationList();
        instantiationsPanel.updateUI();
        buildQueryatomsTables(inferenceModuleCore.queryModels);
        if(selected != -1)
            elementNamesList.setSelectedIndex(selected);
    }

    /**
     * It is required that a vector 'qtm' of (empty) QueryTableModels is already
     * initialized. That makes it easier to call this method from different contexts
     * when 'qtm' consists of different subclasses of QueryTableModel.
     * @param qtm
     */
    private void  buildQueryatomsTables(Vector<? extends QueryTableModel> qtm) {
        // Initialize the GUI components:
        queryatomsPanel.removeAll();
        querytables = new Vector<JTable>();
        queryatomsScrolllists = new Vector<JScrollPane>();
        for (int i=0;i<inferenceModuleCore.queryModels.size();i++) {
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
        for (Rel r: inferenceModuleCore.queryatoms.keySet()) {
            int idx = inferenceModuleCore.relIndex.get(r.name());
            buildQueryatomsTable(qtm.elementAt(idx),inferenceModuleCore.queryatoms.get(r));
        }
//		for (JTable jt: querytables)
//			jt.updateUI();
//		for (JScrollPane jsp: queryatomsScrolllists)
//			jsp.updateUI();
        queryatomsPanel.updateUI();
    }

    //user deletes the element (in Bavaria)
    public void deleteElementName(int node){
        inferenceModuleCore.elementNamesListModel.clear();
        inferenceModuleCore.readElementNames();
        inferenceModuleCore.inst.deleteShift(node);
        inferenceModuleCore.updateInstantiationList();
        instantiationsPanel.updateUI();
        for (GroundAtomList qats: inferenceModuleCore.queryatoms.values()) {
            qats.delete(node);
            qats.shiftArgs(node);
        }
        buildQueryatomsTables(inferenceModuleCore.queryModels);
        for(int i=0; i<inferenceModuleCore.element_tuple.length; ++i){
            if(inferenceModuleCore.element_tuple[i] == node){
                infoMessage.setText("Tuple cancelled (included a deleted node)");
                el_pos=0;
            }
        }
    }

    //new RBN file loaded
    public void updateRBNRelations(){
        inferenceModuleCore.relationsListModel.clear();
        inferenceModuleCore.valuesListModel.clear();
        inferenceModuleCore.readRBNRelations();
        //instasosd.reset();
        inferenceModuleCore.instantiationsListModel.clear();
        inferenceModuleCore.queryatoms=new Hashtable<Rel,GroundAtomList>();
        inferenceModuleCore.queryModels=new Vector<QueryTableModel>();
        elementNamesList.clearSelection();
        infoMessage.setText(" ");
        el_pos=0;
        selected_rel = null;
        selectedInstAtom = null;
        selectedQueryAtom = null;

        if( inferenceModuleCore.myACEControl != null ) inferenceModuleCore.myACEControl.clear();//keith cascio 20060515
    }

    /** interface Control.ACEControlListener
     @author keith cascio
     @since 20060511 */
    public void aceStateChange( Control control ){
        //InferenceModule.this.resetACEEnabledState( control );
        // if( !control.isReadyCompute() ) aceModel.resetACE(); //TODO
        //clearACEMessage();
    }

    /*
     * Takes an  atom specification with (typed) wildcards (as specified by their
     * position in the elements list), and
     * returns a GroundAtomList with all matching atoms
     */
    private GroundAtomList buildAtoms(Rel rel, int[] tuple){
        SparseRelStruc rstnew = new SparseRelStruc();
        rstnew = (SparseRelStruc)myprimula.rels;

        GroundAtomList result = new GroundAtomList();

        if (rel.getArity()==0) {
            result.add(new GroundAtom(rel,new int[0]));
            return result;
        }

        int[] temp = new int[tuple.length];
        int pos = 0;
        int length = tuple.length;
        for(int x=0; x<tuple.length; x++){
            temp[x] = tuple[x];
        }
        for(int i=0; i<length; i++){
            if(inferenceModuleCore.elementNamesListModel.elementAt(tuple[i]).equals("*")){
                Vector v = rstnew.getNames();
                for(int j=0; j<v.size(); j++){
                    temp[pos] = j;
                    result.add(buildAtoms(rel, temp));
                }
            }
            else if(((String)inferenceModuleCore.elementNamesListModel.elementAt(tuple[i])).startsWith("[")){
                Vector<BoolRel> attributeNames = rstnew.getBoolAttributes();
                BoolRel nextattr;
                for(int j =0; j<attributeNames.size();j++){
                    nextattr = attributeNames.elementAt(j);
                    if(((String)inferenceModuleCore.elementNamesListModel.elementAt(tuple[i])).equals("["+ nextattr +"*]")){
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

    /**
     * The int[] 'tuple' denotes the indices of object identifiers
     * in the 'elements' list of the GUI. This may include general (*)
     * or type ([person]) wildcards.
     * @param rel
     * @param tuple
     */
    private void addQueryAtoms(Rel rel,int[] tuple) {
        GroundAtomList atstoadd = buildAtoms(rel,tuple);
        Integer idx = inferenceModuleCore.relIndex.get(rel.toString());
        if (idx != null) {
            inferenceModuleCore.queryatoms.get(rel).add(atstoadd);
        }
        else {
            idx=inferenceModuleCore.relIndex.size();
            inferenceModuleCore.relIndex.put(rel.name(), (Integer)idx);
            inferenceModuleCore.relList.add(rel);
            inferenceModuleCore.queryatoms.put(rel, atstoadd);
            inferenceModuleCore.queryModels.add(new QueryTableModel());
        }
        inferenceModuleCore.queryModels.elementAt(idx).addQuery(atstoadd);
        myprimula.queryatoms.add(atstoadd);
        this.buildQueryatomsTables(inferenceModuleCore.queryModels);
    }

    //new rst-file loaded or OrdStruc created
    public void newElementNames(){
        inferenceModuleCore.elementNamesListModel.clear();
        inferenceModuleCore.readElementNames();
        //instasosd.reset();
        inferenceModuleCore.instantiationsListModel.clear();
        inferenceModuleCore.queryatoms=new Hashtable<Rel,GroundAtomList>();
        inferenceModuleCore.queryModels=new Vector<QueryTableModel>();
//		infoMessage.setText(" ");
        el_pos=0;
        selectedInstAtom = null;
        selectedQueryAtom = null;

        if( inferenceModuleCore.myACEControl != null ) inferenceModuleCore.myACEControl.clear();//keith cascio 20061201
    }

    private void buildMAPTables() {
        /* creating MapModels
         * It is required that querytables and queryatomsScrolllists exist and have the
         * same length as relList
         */
        inferenceModuleCore.mapModels=new Vector<MAPTableModel>();
        for (int i=0;i<querytables.size();i++) {

            Rel r = inferenceModuleCore.relList.elementAt(i);
            MAPTableModel maptm = new MAPTableModel(inferenceModuleCore.queryModels.elementAt(i),inferenceModuleCore.relList.elementAt(i));

            inferenceModuleCore.mapModels.add(maptm);
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
        inferenceModuleCore.mcmcModels=new Vector<MCMCTableModel>();
        //Vector<JTable> newquerytables = new Vector<JTable>();
        for (int i=0;i<querytables.size();i++) {

            Rel r = inferenceModuleCore.relList.elementAt(i);
            MCMCTableModel mcmctm = new MCMCTableModel(inferenceModuleCore.queryModels.elementAt(i),inferenceModuleCore.relList.elementAt(i));

            inferenceModuleCore.mcmcModels.add(mcmctm);
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

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == inferencePane) {
            switch (inferencePane.getSelectedIndex()) {
                case 0: // Evidence tab
                    queryModeOn = false;
                    buildQueryatomsTables(inferenceModuleCore.queryModels);
                    el_pos=0;
                    elementNamesList.clearSelection();
                    infoMessage.setText("Select Relation - Value - Element name(s) ");
                    break;
                case 1: // Query tab
                    queryModeOn = true;
                    el_pos=0;
                    elementNamesList.clearSelection();
                    infoMessage.setText("Select Relation - Element name(s) ");
                    buildQueryatomsTables(inferenceModuleCore.queryModels);
                    queryatomsPanel.updateUI();
                    outerQueryPane.updateUI();
                    break;
                case 2: // MCMC tab
                    buildMCMCTables();
                    queryatomsPanel.updateUI();
                    outerQueryPane.updateUI();
                    break;
                case 3: // Test tab
                    break;
                case 4: // MAP tab
                    buildMAPTables();
                    queryatomsPanel.updateUI();
                    outerQueryPane.updateUI();
                    break;
                case 5: // ACE tab
                    break;
            }
        }
    }

    public void setMyprimulaGUI(PrimulaGUI myprimulaGUI) { this.myprimulaGUI = myprimulaGUI; }
}
