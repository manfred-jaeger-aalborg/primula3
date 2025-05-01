package RBNgui;

import javax.swing.*;

import RBNutilities.rbnutilities;
import myio.StringOps;

import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import MLNParser.MLNParserFacade;
import RBNpackage.*;
import RBNio.*;
import RBNinference.*;
import RBNExceptions.*;
import RBNLearning.*;
import edu.ucla.belief.ui.primula.*;
import edu.ucla.belief.ace.PrimulaSystemSnapshot;
public class PrimulaGUI extends JFrame implements PrimulaUIInt, ActionListener, ItemListener, KeyListener {

    public Primula primula;
    InferenceModuleGUI evidenceModuleGUI;
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

    private JMenuItem gnnSettingsModule = new JMenuItem("GNN settings");

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
    JLabel rstsrc                 = new JLabel("");
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
    private static boolean isGNNSettingsOpen = false;
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
     * @uml.property  name="btnDebugAceCompile"
     * @uml.associationEnd  multiplicity="(1 1)"
     */
    private JMenuItem btnDebugAceCompile = new JMenuItem( "DEBUG ace compile" );

    /**
     * @uml.property  name="sTRUCTURE_MODIFIED"
     */
    private final String STRUCTURE_MODIFIED = "Current structure modified. Continue?";
    /**
     * @uml.property  name="iNST_AND_QUERIES_LOST"
     */
    private final String INST_AND_QUERIES_LOST = "This action will cause current instantiations and queries to be lost. Continue?";

    private final String UNSAVED_DATA_LOST = "This action will cause all unsaved data to be lost. Continue?";

    public PrimulaGUI(Primula primul) {
        primula = primul;
        primula.myprimulaGUI = this;
        setupUI();
    }

    private void setupUI() {
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
        optionsmenu.add(gnnSettingsModule);
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

        gnnSettingsModule.addActionListener(this);

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

    /**
     @author Keith Cascio
     @since 040804
     */
    private void init()
    {
        //ImageIcon icon = new ImageIcon("small_logo.jpg");
        ImageIcon icon = new ImageIcon( Primula.STR_FILENAME_LOGO );

        if( icon.getImageLoadStatus() == MediaTracker.COMPLETE ){//image ok
            this.setIconImage(icon.getImage());
        }
        this.setTitle("Primula");
        this.pack();

        primula.myPreferences = new Preferences( true );
    }

    /**
     @author Keith Cascio
     @since 040804
     */
    public void exitProgram(){
        primula.myPreferences.saveOptionsToFile();
        if( isSystemExitEnabled() ) System.exit( 0 );
        else setVisible( false );
    }


    //ask confirmation
    public boolean confirm(String text){
        int result = JOptionPane.showConfirmDialog(this, text, "Confirmation", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION)
            return true;
        else //result == JOptionPane.NO_OPTION
            return false;
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

    /** @author Keith Cascio */
    public void showMessageThis(String message){
        showMessage( message );
    }

    /** @author Keith Cascio */
    public void appendMessageThis(String message){
        appendMessage( message );
    }


    private void setBnoutfile(){
        bnetFileChooser.resetChoosableFileFilters();
        switch (primula.bnsystem){
            case Primula.OPTION_HUGIN:
                bnetFileChooser.setFileFilter(myFilterNET);
                break;
            case Primula.OPTION_NETICA:
                bnetFileChooser.setFileFilter(myFilterDNE);
                break;
            case Primula.OPTION_JAVABAYES:
                bnetFileChooser.setFileFilter(myFilterBIF);
                break;
        }

        int value = bnetFileChooser.showDialog(this, "Save");
        if (value == JFileChooser.APPROVE_OPTION) {
            try{
                primula.bnoutfile = bnetFileChooser.getSelectedFile();
            }
            catch (Exception ex){ ex.printStackTrace();}
        }
    }

    /**
     @author Keith Cascio
     @since 040504
     */
    public String makeNetworkName()
    {
        if( primula.bnoutfile == null ) return makeAlternateName();
        else return primula.bnoutfile.getPath();
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

        return primula.pluckNameFromPath( strRST ) + "_" + primula.pluckNameFromPath( strRBN ) + ".net";
    }

    //sets the current rel struc
    public void setRelStruc(RelStruc srel){
        primula.rels = srel;
        primula.initRelData(primula.rels);
        if(primula.isEvModuleOpen)
            evidenceModuleGUI.newElementNames();
        primula.instasosd.clear();
        if (primula.rbn != null)
            primula.instasosd.init(primula.rbn);
        primula.queryatoms.reset();
        if (primula.isBavariaOpen)
            primula.bavaria.displayRelStruc((SparseRelStruc)srel);
    }

    //user adds or renames a node in the Bavaria window
    public void addOrRenameEvidenceModuleNode(){
        if(primula.isEvModuleOpen)
            evidenceModuleGUI.addOrRenameElementName();
    }

    //user deletes a node in the Bavaria window
    public void deleteElementFromEvidenceModule(int node){
        if(primula.isEvModuleOpen)
            evidenceModuleGUI.deleteElementName(node);
    }

    //user has edited the structure in the Bavaria
    /**
     * @param b
     * @uml.property  name="strucEdited"
     */
    public void setStrucEdited(boolean b){
        strucEdited = b;
        if( primula.isEvModuleOpen ) evidenceModuleGUI.relationalStructureEdited();//keith cascio 20060725
    }

    /** @author keith cascio
     @since 20060602 */
    public void forgetAll(){
        if( primula.myPreferences != null ) primula.myPreferences.forgetAll();
        if( primula.mySamiamManager != null ) primula.mySamiamManager.forgetAll();
        if( primula.evidenceModule != null ) evidenceModuleGUI.forgetAll();
    }

    public void updateMessageArea(){
        messageArea.repaint();
    }

    /**
     * @author Alberto García Collado
     * @param mln the file where the mln is stored
     * @param owdb the file where the open world assuptions are declared
     * @param cwdb the file where the close world assuptions are declared
     */
    public void LoadMLN(File mln, File cwdb, File owdb) {
        MLNParserFacade facade = new MLNParserFacade();
        facade.ReadMLN(mln, cwdb, owdb);
        primula.rbn = facade.getRBN();
        primula.rels = facade.getRelStruc();
        primula.instasosd = facade.getInstantiation();
        primula.instasosd.setParentRelStruc(primula.rels);
        primula.rdata = new RelData(primula.rels,primula.instasosd);
        this.rbnfilename.setText(mln.getPath());
        if(primula.isEvModuleOpen){
            evidenceModuleGUI.updateRBNRelations();
            evidenceModuleGUI.newElementNames();
            primula.evidenceModule.updateInstantiationList();
        }
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        Object source = e.getSource();

        if( source == datasrcBrowseButton ){
            strucEdited = false;
            domainFileChooser.setFileFilter( myFilterRDEF );
            int value = domainFileChooser.showDialog(this, "Select");
            if (value == JFileChooser.APPROVE_OPTION){
                primula.rdeffile = domainFileChooser.getSelectedFile();
                if (confirm(UNSAVED_DATA_LOST)){
                    datasrcfilename.setText("");
                    Rel.resetTheColorCounters();
                    primula.loadSparseRelFile(primula.rdeffile);
                    datasrcfilename.setText(primula.rdeffile.getName());

                    if(primula.isEvModuleOpen)
                        evidenceModuleGUI.newElementNames();
                    else{
                        primula.queryatoms.reset();
                    }
                    if(primula.isLrnModuleOpen){
                        primula.learnModule.getRelDataFromPrimula();
                    }
                    if(primula.isBavariaOpen){
                        primula.bavaria.update();
                    }
                }
            }
        }

        else if( source == loadRBN ){
            relmodelFileChooser.resetChoosableFileFilters();
            relmodelFileChooser.addChoosableFileFilter(myFilterRBN);
            relmodelFileChooser.addChoosableFileFilter(myFilterMLN);
            relmodelFileChooser.setFileFilter(myFilterRBN);
            int value = relmodelFileChooser.showDialog(this, "Load");
            if (value == JFileChooser.APPROVE_OPTION) {
                File selectedFile = relmodelFileChooser.getSelectedFile();
                if (myFilterRBN.accept(selectedFile))
                    primula.loadRBNFunction(selectedFile);
                else if (myFilterMLN.accept(selectedFile)){
                    File mlnFile = selectedFile;
                    relmodelFileChooser.resetChoosableFileFilters();
                    Filter_db cwdbFilter = new Filter_db(false);
                    relmodelFileChooser.addChoosableFileFilter(cwdbFilter);
                    relmodelFileChooser.setFileFilter(cwdbFilter);
                    value = relmodelFileChooser.showDialog(this, "Load domain data");

                    File cwdbfile = null;
                    if (value == JFileChooser.APPROVE_OPTION)
                        cwdbfile = relmodelFileChooser.getSelectedFile();

                    relmodelFileChooser.resetChoosableFileFilters();
                    Filter_db owdbFilter = new Filter_db(true);
                    relmodelFileChooser.addChoosableFileFilter(owdbFilter);
                    value = relmodelFileChooser.showDialog(this, "Load evidence data");
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
            if(primula.rels != null && primula.rels instanceof SparseRelStruc && !primula.isBavariaOpen && primula.srsfile != null){
                SparseRelStruc temp = (SparseRelStruc)primula.rels;
                if(temp.getCoords().size() == 0)
                    temp.createCoords();
                primula.bavaria = new Bavaria(temp, primula.srsfile, this, strucEdited);
                primula.isBavariaOpen = true;
                rstsrc.setText("Bavaria RelStruc Editor");
            }

            //rst file created in Bavaria
            else if (primula.rels != null && primula.rels instanceof SparseRelStruc && !primula.isBavariaOpen && primula.srsfile == null){
                primula.bavaria = new Bavaria((SparseRelStruc)primula.rels, null, this, strucEdited);
                primula.isBavariaOpen = true;
                rstsrc.setText("Bavaria RelStruc Editor");
            }

            //create a new rst file
            else if (primula.rels == null && !primula.isBavariaOpen){
                primula.rels = new SparseRelStruc();
                primula.bavaria = new Bavaria((SparseRelStruc)primula.rels, this, strucEdited);
                primula.isBavariaOpen = true;
                rstsrc.setText("Bavaria RelStruc Editor");
            }
        }
        else if( source == constructCPTBN ){
            //actionlistener for constructing standard BN
            boolean nogo = false;
            String message = "";
            if (primula.rbn == null){
                nogo = true;
                message = message + " Please load rbn first";
            }
            if (primula.rels == null){
                nogo = true;
                message = message + " Please load RelStruc first";
            }
            if (primula.rbn != null){
                if (!primula.rbn.multlinOnly() & primula.decomposemode != primula.OPTION_NOT_DECOMPOSE){
                    nogo = true;
                    message = message + " Please choose decompose:none for rbn containing non multilinear comb. functions";
                }
            }
            if(!nogo){
                try {
                    BayesConstructor constructor = null;
                    if( primula.bnsystem == primula.OPTION_SAMIAM )
                        constructor = new BayesConstructor(this ,
                                primula.instasosd,
                                primula.queryatoms,
                                makeNetworkName());

                    else { setBnoutfile();
                        constructor = new BayesConstructor(primula.rbn,
                                primula.rels,
                                primula.instasosd,
                                primula.queryatoms,
                                primula.bnoutfile);
                    }
                    constructor.constructCPTNetwork(primula.evidencemode,
                            primula.querymode,
                            primula.decomposemode,
                            primula.isolatedzeronodesmode,
                            primula.layoutmode,
                            primula.bnsystem);
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
            int value = relmodelFileChooser.showDialog(this, "Save");
            if (value == JFileChooser.APPROVE_OPTION) {
                rbnfile = relmodelFileChooser.getSelectedFile();
                primula.rbn.saveToFile(rbnfile,primula.rbnsyntax,true);
            }
        }
        else if (source == saveData){
            domainFileChooser.setFileFilter( myFilterRDEF );
            int value = domainFileChooser.showDialog(this, "Save");
            if (value == JFileChooser.APPROVE_OPTION) {
                File savefile = domainFileChooser.getSelectedFile();
                primula.rdata.saveToRDEF(savefile);
            }
        }
        else if (source == dataConvert){
            File sourcefile;
            File targetfile;
            RelData inrdata = new RelData();

            domainFileChooser.resetChoosableFileFilters();
            //			domainFileChooser.addChoosableFileFilter(myFilterRDEF);
            domainFileChooser.addChoosableFileFilter(myFilterPL);
            int value = domainFileChooser.showDialog(this, "Load");
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
            value = domainFileChooser.showDialog(this, "Save");
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
            this.forgetAll();
        }
        else if( source == evModule ){
            //actionlistener for opening the evidence module
            if(!primula.isEvModuleOpen){
                primula.evidenceModule = new InferenceModule(primula);
                evidenceModuleGUI = new InferenceModuleGUI(primula.evidenceModule);
                primula.isEvModuleOpen = true;
            }
        }
        // +Learn
        else if( source == lrnModule ){
            //actionlistener for opening the learn module
            primula.openLearnModule(true);
            primula.learnModule.setMyprimulaGUI(this);
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
            primula.loadSparseRelFile( primula.srsfile = new File( pathRST ) );
            primula.loadRBNFunction( new File( pathRBN ) );
            evModule.doClick();
            //evidenceModule.aceCompile();
            //evidenceModule.getACEControl().getActionCompile().actionPerformed( null );
        }
        else if( source == itemabout ){
            JOptionPane.showMessageDialog(null,"Primula version 2.2 " + '\n' + "(C) 2009");
        }
        else if (source == gnnSettingsModule) {
            System.out.println("ciao");
//            openGNNSettings();
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();
        if( source == javaBayes ){
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.bnsystem = primula.OPTION_JAVABAYES;
        }
        else if( source == hugin ){
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.bnsystem = primula.OPTION_HUGIN;
        }
        else if( source == netica ){
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.bnsystem = primula.OPTION_NETICA;
        }
        else if( source == samiam ){
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.bnsystem = primula.OPTION_SAMIAM;
        }
        else if( source == decnone ){
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.decomposemode = primula.OPTION_NOT_DECOMPOSE;
        }
        else if( source == decstandard ){
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.decomposemode = primula.OPTION_DECOMPOSE;
        }
        else if( source == decdet ){
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.decomposemode = primula.OPTION_DECOMPOSE_DETERMINISTIC;
        }
        else if( source == querySpecific){
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.querymode = primula.OPTION_QUERY_SPECIFIC;
            else primula.querymode = primula.OPTION_NOT_QUERY_SPECIFIC;
        }
        else if( source == evidenceConditioned ){
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.evidencemode = primula.OPTION_EVIDENCE_CONDITIONED;
            else primula.evidencemode = primula.OPTION_NOT_EVIDENCE_CONDITIONED;
        }
        else if( source == eliminateIsolatedZeroNodes ) {
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.isolatedzeronodesmode = primula.OPTION_NOT_ELIMINATE_ISOLATED_ZERO_NODES;
            else primula.isolatedzeronodesmode = primula.OPTION_ELIMINATE_ISOLATED_ZERO_NODES;
        }
        else if( source == synclassic ) {
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.rbnsyntax = primula.CLASSICSYNTAX;
        }
        else if( source == syncherry ) {
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.rbnsyntax = primula.CHERRYSYNTAX;
        }
        else if( source == layoutItem ){
            if (e.getStateChange() == ItemEvent.SELECTED)
                primula.layoutmode = primula.OPTION_NO_LAYOUT;
            else primula.layoutmode = primula.OPTION_LAYOUT;
        }

    }

    public JTextField getDatasrcfilename() {
        return datasrcfilename;
    }

    public JTextField getRbnfilename() {
        return rbnfilename;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    /**
     @author Keith Cascio
     @since 040804
     */
    public JFrame asJFrame(){
        return this;
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
        if( primula.mySamiamManager == null ) primula.mySamiamManager = new SamiamManager( primula );
        return primula.mySamiamManager;
    }

    /**
     @arg flag Sets whether the JVM should terminate when the user closes Primula.  Set this to false if you call Primula from another Java program and you want to prevent Java from exiting when the user exits Primula.
     @since 040804
     */
    public void setSystemExitEnabled( boolean flag ){
        primula.myFlagSystemExitEnabled = flag;
    }

    /**
     @ret true if a user action that closes Primula will cause the JVM to terminate as well.
     @since 040804
     */
    public boolean isSystemExitEnabled(){
        return primula.myFlagSystemExitEnabled;
    }

//    public void openGNNSettings() {
//        if (!isGNNSettingsOpen) {
//            gnnSettings = new GNNSettings(this);
//            isGNNSettingsOpen = true;
//        }
//    }

    public LearnModule openLearnModule(boolean visible){
        if(!primula.isLrnModuleOpen){
            primula.learnModule = new LearnModule(primula, true);
            primula.isLrnModuleOpen = true;
        }
        return primula.learnModule;
    }

    private void loadDefaults() throws RBNIllegalArgumentException {


//        String rbninputfilestring = "/Users/lz50rg/Dev/homophily/experiments/rbn_constraints/const_nodeconst_gnn.rbn";
//        String rstinputfilestring = "/Users/lz50rg/Dev/homophily/experiments/ising/rdef/ising_32_0.5_0_0.4_4_nodeconst.rdef";


        String rbninputfilestring = "/home/jaeger/B/Primula/primula3/Examples/InformationDiffusion/independent_cascade.rbn";
        String rstinputfilestring = "/home/jaeger/B/Primula/primula3/Examples/InformationDiffusion/zachary_fragment.rdef";


//      String rbninputfilestring = "/home/jaeger/B/Primula/Examples/HAWQS/water_rbn_parseminus.rbn";
//      String rstinputfilestring = "/home/jaeger/B/Primula/Examples/HAWQS/river_with_data_train.rdef";


//        String rbninputfilestring = "/Users/lz50rg/Dev/water-hawqs/water_rbn_2.rbn";
//        String rstinputfilestring = "/Users/lz50rg/Dev/water-hawqs/test_small_new_sampled.rdef";

//        String rbninputfilestring = "/Users/lz50rg/Dev/football/rbn_file.rbn";
//        String rstinputfilestring = "/Users/lz50rg/Dev/football/overlapping_2024_12_12/2024-12-12_move_to_cat/10-47-26-12a63b46/overlapping.rdef";

//        String rbninputfilestring = "/Users/lz50rg/Dev/water-hawqs/water_rbn.rbn";
//        String rstinputfilestring = "/Users/lz50rg/Dev/water-hawqs/test_small_new.rdef";

//        String rbninputfilestring = "/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/water_pollution_model.rbn";
//        String rstinputfilestring = "/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/simple_subbasin.rdef";

//        String rbninputfilestring = "/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/water_pollution-gibbs.rbn";
//        String rstinputfilestring = "/Users/lz50rg/Dev/primula-workspace/primula3/Examples/WaterPollution/water_network_gibbs.rdef";

        primula.srsfile = new File(rstinputfilestring);
        primula.rbnfile = new File(rbninputfilestring);

        primula.loadSparseRelFile(primula.srsfile);
        primula.loadRBNFunction(primula.rbnfile);
    }

    public static void main( String[] args ) throws RBNIllegalArgumentException {
        // cross platform look
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        for( String arg : args ){
            if( Primula.STR_OPTION_DEBUG.equals( arg ) ) Primula.FLAG_DEBUG = true;
        }
        PrimulaGUI win = new PrimulaGUI(new Primula());
//		SamiamManager.centerWindow( win );
//        win.loadDefaults();
		win.show();
    }
}
