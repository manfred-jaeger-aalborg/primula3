/*
* LearnModule.java 
* 
* Copyright (C) 2009 Aalborg University
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
import RBNio.*;
import RBNutilities.*;
import RBNLearning.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import myio.StringOps;

import java.util.*;


public class LearnModule extends JFrame implements ActionListener,MouseListener,GradientGraphOptions, KeyListener
{
	public static final int UseLik = 0;
	public static final int UseLogLik = 1;
	public static final int UseSquaredError = 2;
	
	/* Options for the top-level learning strategy in LearnThread:*/
	public static final int AscentBatch =0;
	/* Ref: D. P. Kingma and J. L. Ba: ADAM: A Method for Stochastic Optimization
	 * arXiv: 1412.6980v8
	 */
	public static final int AscentAdam =1;
	public static final int AscentStochHeur1 =2;
	public static final int AscentStochHeur2 =3;
	public static final int AscentBlock =4;
	public static final int AscentTwoPhase =5;
	
	/* Options for a single parameter learning/update call to GradientGraph: */
	public static final int AscentLBFGS =0;
	public static final int AscentAdagrad =1;
	public static final int AscentFletcherReeves =2;
	public static final int AscentDirectGradient =3;
	
	public static final String threadstrategy[] = {"Batch","Adam"};
	public static final String ggstrategy[] = {"LBFGS","Adagrad","FletcherReeves","Greedy"};
	
	/**
	 * @uml.property  name="tabbedPane"
	 * @uml.associationEnd  multiplicity="(1 1)"ggrandominit
	 */
	private JTabbedPane tabbedPane   = new JTabbedPane();
	
	/**
	 * @uml.property  name="dataPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel dataPanel = new JPanel(new GridLayout(2,1));
	
	private JPanel numRelPanel = new JPanel(new BorderLayout());
	
	/**
	 * @uml.property  name="lowerlearnPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel lowerlearnPanel = new JPanel(new GridLayout(2,1));
	/**
	 * @uml.property  name="learnButtons"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel learnButtons = new JPanel(new FlowLayout());
	/**
	 * @uml.property  name="sampleoptions"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel sampleoptions = new JPanel(new GridLayout(2,1));
	/**
	 * @uml.property  name="samplesizepanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel samplesizepanel = new JPanel(new FlowLayout());
	/**
	 * @uml.property  name="percmisspanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel percmisspanel = new JPanel(new FlowLayout());
	/**
	 * @uml.property  name="restartspanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel restartspanel = new JPanel(new FlowLayout());
	/**
	 * @uml.property  name="datasrcPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel datasrcPanel = new JPanel(new FlowLayout());
	
	private JPanel paramInputFields  = new JPanel(new BorderLayout());
	
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
	 * @uml.property  name="samplesizelabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel samplesizelabel  = new JLabel("Sample size");
	/**
	 * @uml.property  name="percmisslabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel percmisslabel  = new JLabel("Percent missing");
	/**
	 * @uml.property  name="restartlabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel restartlabel  = new JLabel("Restarts");

	private JLabel paramfilelabel  = new JLabel("Read from File:");
	private JTextField paramsrcfilename      = new JTextField(15);
	private JButton paramsrcBrowseButton     = new JButton("Browse");
	protected File paramfile;
	
	/**
	 * @uml.property  name="dataFileName"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
//	private JTextField dataFileName        = new JTextField(15);
	/**
	 * @uml.property  name="textsamplesize"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField textsamplesize = new JTextField(15);
	/**
	 * @uml.property  name="textpercmiss"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField textpercmiss = new JTextField(3);
    /**
	 * @uml.property  name="textnumrestarts"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private JTextField textnumrestarts = new JTextField(5);
    
	/**
	 * @uml.property  name="loadDataButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
//	private JButton loadDataButton         = new JButton("Load");
	/**
	 * @uml.property  name="sampleDataButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton sampleDataButton       = new JButton("Sample");
	/**
	 * @uml.property  name="saveDataButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
//	private JButton saveDataButton         = new JButton("Save");
	/**
	 * @uml.property  name="learnButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton learnButton         = new JButton("Learn");
	/**
	 * @uml.property  name="stoplearnButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton stoplearnButton         = new JButton("Stop");
	/**
	 * @uml.property  name="setParamButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton setParamButton         = new JButton("Set");
	/**
	 * @uml.property  name="learnSettingButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton learnSettingButton         = new JButton("Settings");
	
	
	
	/**
	 * @uml.property  name="parametertable"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTable parametertable         = new JTable();
	
	private JList numrellist         = new JList();
	
	private DefaultListModel numRelListModel = new DefaultListModel();
	
	/**
	 * @uml.property  name="parammodel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private ParameterTableModel parammodel = new ParameterTableModel();
	/**
	 * @uml.property  name="parameterScrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane parameterScrollList = new JScrollPane();

	private JScrollPane numRelScrollList = new JScrollPane();
	
	/**
	 * @uml.property  name="learnsplitpane"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JSplitPane learnsplitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,parameterScrollList,lowerlearnPanel);

	/**
	 * @uml.property  name="myprimula"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="learnModule:RBNgui.Primula"
	 */
	private Primula myprimula;
	/**
	 * @uml.property  name="mystaticprimula"
	 * @uml.associationEnd  readOnly="true"
	 */
	private Primula mystaticprimula;
	/**
	 * @uml.property  name="data"
	 * @uml.associationEnd  
	 */
	private RelData data;
	/**
	 * @uml.property  name="datafile"
	 */
//	private File datafile;
	/**
	 * @uml.property  name="settingswindow"
	 * @uml.associationEnd  inverse="learnmodule:RBNgui.SettingsLearn"
	 */
	private SettingsLearn settingswindow;
	
	/**
	 * @uml.property  name="settingswindowopen"
	 */
	private boolean settingswindowopen;
	/* Parameters for the data sampling process */
	/**
	 * @uml.property  name="samplesize"
	 */
	private int samplesize;
	/**
	 * @uml.property  name="percmiss"
	 */
	private double percmiss;
	/* Parameters for the learning process */
	/**
	 * @uml.property  name="numchains"
	 */
	private int numchains;
	/**
	 * @uml.property  name="restarts"
	 */
	private int restarts;
	
	/* Percentage by which to sub-sample false atoms */
	private int subsamples;
	
	/* Number of data mini-batches for stochastic gradient descent */
	private int numbatches;
	
	/* Type of datasplit RelData.SPLIT_BY_DOMAIN or RelData.SPLIT_ACROSS_DOMAINS */
	private int splitmode;
	
	/* Used in stochastic gradient descent */
	private double dampingfac;
	
	/* Number of parameter blocks for block gradient descent */
	private int numblocks;
	
	/**
	 * @uml.property  name="windowsize"
	 */
	private int windowsize;
	/**
	 * @uml.property  name="maxfails"
	 */
	private int maxfails;
	/**
	 * @uml.property  name="maxiterations"
	 */
	private int maxiterations;
	/**
	 * @uml.property  name="linedistancethresh"
	 */
	private double linedistancethresh;
	/**
	 * @uml.property  name="linelikelihoodthresh"
	 */
//	private double linelikelihoodthresh;
	private int likelihoodwindow;
	private double llikhoodthresh;
	/**
	 * @uml.property  name="gradientdistancethresh"
	 */
	private double gradientdistancethresh;
	/**
	 * @uml.property  name="paramratiothresh"
	 */
	private double paramratiothresh;
	/**
	 * @uml.property  name="omitrounds"
	 */
	private int omitrounds;
	/**
	 * @uml.property  name="verbose"
	 */
	private boolean useggs;
	
	private boolean usememoize;
	
	private boolean learnverbose;
	private int objective;
	private boolean ggrandominit;
	private boolean gg2phase;
	
	private boolean numrelsfromfile;
	private String[][] numrelblocks;
	

	
	protected int ggascentstrategy;
	protected int threadascentstrategy;
	protected int lbfgsmemory;
	
	double adagradfade;
	double adagradepsilon;
	double adam_beta1;
	double adam_beta2;
	double adam_epsilon;
	double adam_alpha;
	
	private int type_of_gradient; // one of ProbForm.RETURN_ARRAY or  ProbForm.RETURN_SPARSE
	
	/**
	 * @uml.property  name="aca"
	 */
	private boolean aca;
	
	/**
	 * @uml.property  name="lthread"
	 * @uml.associationEnd  
	 */
	LearnThread lthread;
	
	//private Vector<String> selectednumrels;
	
	public LearnModule(Primula mypr,boolean visible){

		myprimula = mypr;
		data = mypr.getReldata();
		
		
		threadascentstrategy = AscentAdam;
		ggascentstrategy = AscentLBFGS;

		
		
		lbfgsmemory = 10;
		
//		datafile = null;
		settingswindowopen = false;
		samplesize = 1;
		restarts = 1; /*-1 is for open-ended restarts */
		subsamples = 100;
		numblocks = 1;
		numbatches =50;
		splitmode = RelData.SPLIT_BY_DOMAIN;
		dampingfac =0.99;
		numchains = 2;
		windowsize = 2;
		maxfails = 5;
		maxiterations = 20;
		linedistancethresh = 0.0001;
//		linelikelihoodthresh = 0.001;
		likelihoodwindow = 5;
		llikhoodthresh = 0.00005;
		gradientdistancethresh = 0.001;
		paramratiothresh = 0.0;
		omitrounds = 3;
		percmiss = 0.0;
		learnverbose = false;
		objective = UseLogLik;
		gg2phase = false;
		ggrandominit = true;
		numrelsfromfile = false;
		aca = false;
		readNumRels();
		useggs=false;
		usememoize=true;
		
		//selectednumrels = new Vector<String>();
		type_of_gradient=ProbForm.RETURN_ARRAY;
		
		adagradfade = 0.5;
		adagradepsilon = 1.0E-10;
		adam_beta1=0.9;
		adam_beta2=0.999;
		adam_epsilon = 1.0E-8;
		adam_alpha = 0.01;
		
		fileChooser.addChoosableFileFilter(myFilterRDEF = new Filter_rdef());

		/* Data Tab */
		/* Data File Load */
//		loadDataButton.addActionListener(this);
//		loadDataButton.setBackground(Primula.COLOR_BLUE);
		sampleDataButton.addActionListener(this);
		sampleDataButton.setBackground(Primula.COLOR_RED);
//		saveDataButton.addActionListener(this);
//		saveDataButton.setBackground(Primula.COLOR_GREEN);
		textsamplesize.addKeyListener(this);
		textpercmiss.addKeyListener(this);
		
//		datasrcPanel.add(loadDataButton);
		datasrcPanel.add(sampleDataButton);
//		datasrcPanel.add(saveDataButton);

		samplesizepanel.add(samplesizelabel);
		samplesizepanel.add(textsamplesize);
		percmisspanel.add(percmisslabel);
		percmisspanel.add(textpercmiss);

		sampleoptions.setBorder(BorderFactory.createTitledBorder("Sampling Options"));
		textsamplesize.setText("" + samplesize);
		textpercmiss.setText("" + percmiss);
		
		
		dataPanel.add(datasrcPanel);
		sampleoptions.add(samplesizepanel);
		sampleoptions.add(percmisspanel);
		dataPanel.add(sampleoptions);
		
		/* Num Rel Tab*/		
		
		numrellist.addMouseListener( this );
		numrellist.setModel(numRelListModel);
		numrellist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		numRelScrollList.getViewport().add(numrellist);
		numRelPanel.add(numRelScrollList,BorderLayout.NORTH);
		numRelPanel.add(paramfilelabel,BorderLayout.CENTER);
		paramInputFields.add(paramsrcfilename, BorderLayout.CENTER);
		paramInputFields.add(paramsrcBrowseButton, BorderLayout.EAST);
		numRelPanel.add(paramInputFields,BorderLayout.SOUTH);
		paramsrcfilename.addKeyListener(this);
		paramsrcBrowseButton.addActionListener(this);
		
		/* Learn Tab */
		/* Parameter Table */
		parametertable.setModel(parammodel);
	  	parametertable.getColumnModel().getColumn(0).setHeaderValue("Parameter");
	  	parametertable.getColumnModel().getColumn(1).setHeaderValue("Value");
	  	parameterScrollList.getViewport().add(parametertable);
		parameterScrollList.setMinimumSize(new Dimension(0,100));
		learnButton.addActionListener(this);
	  	learnButton.setBackground(Primula.COLOR_GREEN);
	  	stoplearnButton.addActionListener(this);
	  	stoplearnButton.setBackground(Primula.COLOR_RED);
	  	setParamButton.addActionListener(this);
	  	setParamButton.setBackground(Primula.COLOR_YELLOW);
	  	learnSettingButton.addActionListener(this);
	  	learnSettingButton.setBackground(Primula.COLOR_BLUE);
	  	learnButtons.add(learnButton);
	  	learnButtons.add(stoplearnButton);
	  	learnButtons.add(setParamButton);
	  	learnButtons.add(learnSettingButton);
	  	
	  	textnumrestarts.setEditable(false);
	  	restartspanel.add(restartlabel);
	  	restartspanel.add(textnumrestarts);
	  	
		//learnPanel.add(parameterScrollList);
		lowerlearnPanel.add(learnButtons);
		lowerlearnPanel.add(restartspanel);

		/* Loading parameters into table! */
		//parammodel.setParameters(mypr.getRBN().parameters());
		
		
		/* Main Pane */
		
		
		tabbedPane.add("Learning",learnsplitpane);
		tabbedPane.add("Relation Parameters", numRelPanel);
		tabbedPane.add("Sample Data",dataPanel);
		
		//Inner class for closing the window
		this.addWindowListener(
				new WindowAdapter(){
					public void windowClosing(WindowEvent e){
						dispose();
						Primula.setIsLearnModuleOpen(false);
					}
				}
		);


		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));
		contentPane.add(tabbedPane);
		
		
		if (visible) {
			ImageIcon icon = myprimula.getIcon( Primula.STR_FILENAME_LOGO );
			if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) //image ok
				this.setIconImage(icon.getImage());
			this.setTitle("Learn Module");
			this.setSize(400, 300);
			this.setVisible(true);
		}

	}
	
	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();	

		if (source == sampleDataButton){
			if (!myprimula.getReldata().hasProbData() || confirm("Unsaved sampled data or evidence will be lost. Continue?")){				
				Sampler sampl = new Sampler();
				sampl.makeSampleStruc(myprimula);
				mystaticprimula.showMessage("Sampling ... 0% ");
				data = new RelData();
				RelDataForOneInput dataforinput = new RelDataForOneInput(myprimula.getRels());
				int completion = 0;
				for (int i=0;i<samplesize;i++){
					dataforinput.addCase(sampl.sampleOneStrucData(percmiss));
					if (10*i/samplesize>completion){
						mystaticprimula.appendMessage("X");
						completion++;
					}					
				}
				data.add(dataforinput);
				mystaticprimula.appendMessage("100%");
				myprimula.setRelData(data);
				myprimula.getInstFromReldata();
				myprimula.updateBavaria();
				myprimula.updateInstantiationInEM();
			}
		}	


		if (source == learnButton){
			this.startLearning();
//			RelData beforesplitdata;
//			RelData[] learndata;
//			if (this.getSubsamples()<100){
//				beforesplitdata = data.subSampleData(this.getSubsamples());
//			}
//			else{
//				beforesplitdata = data;
//			}
//			if (ascentIsStochastic()){
//				learndata = beforesplitdata.randomSplit(this.getNumBatches());
//			}
//			else{
//				learndata = new RelData[1];
//				learndata[0]=beforesplitdata;
//				}
//			
//			lthread = new LearnThread(myprimula, 
//					data,
//					learndata, 
//					parammodel, 
//					parametertable,
//					textnumrestarts,
//					this);
//			lthread.start(); 
		}
		if (source == stoplearnButton){
			lthread.setStopped();
		}
		if (source == setParamButton){
			setParametersPrimula();
		}
		
		if( source == paramsrcBrowseButton ){
			int value = fileChooser.showDialog(LearnModule.this, "Select");
			if (value == JFileChooser.APPROVE_OPTION){
				paramfile = fileChooser.getSelectedFile();
				ParamListReader plr = new ParamListReader();
				numrelblocks = plr.readPList(paramfile);
				paramsrcfilename.setText(paramfile.getName());
				numrelsfromfile = true;
			}
		}
			
		if (source == learnSettingButton){
			if (!settingswindowopen){
				settingswindow = new RBNgui.SettingsLearn(this);
				settingswindowopen = true;
			}
		}

		
		
	}
	
    //  Invoked when the mouse button has been clicked (pressed and released) on a component.
	public void mouseClicked(MouseEvent e) {
		Object source = e.getSource();
	}

//  Invoked when the mouse enters a component.
	public void mouseEntered(MouseEvent e) {
		Object source = e.getSource();
	}
	
	public void mouseExited(MouseEvent e) {
		Object source = e.getSource();
	}
	//           Invoked when a mouse button has been pressed on a component.
	public void mousePressed(MouseEvent e) {
		Object source = e.getSource();
		if(source == numrellist){
			int index = numrellist.locationToIndex(e.getPoint());
//			System.out.println("current: " + StringOps.arrayToString(numrellist.getSelectedIndices(), "[", "]")  +" index: " + index);
//			if(index >= 0){
//				if (numrellist.isSelectedIndex(index)){
//					System.out.println("removing");
//					numrellist.removeSelectionInterval(index,index);
//				}
//				else{
//					System.out.println("adding");
//					numrellist.addSelectionInterval(index,index);
//				}
//			}
			
//			System.out.println("selection: " + StringOps.arrayToString(numrellist.getSelectedIndices(), "[", "]")); 
	
		}
	}


	//          Invoked when a mouse button has been pressed on a component.
	public void mouseReleased(MouseEvent e) {
		Object source = e.getSource();
	}
	//          Invoked when a mouse button has been released on a component.



	public void keyPressed(KeyEvent e){
		//Invoked when a key has been pressed.
		Object source = e.getSource();
		if( source == paramsrcfilename ){
			char c = e.getKeyChar();
			if(c == KeyEvent.VK_ENTER){
				//(new File(paramsrcfilename.getText()));
			}
		}
	}
	
	public void keyTyped(KeyEvent e){
		//Invoked when a key has been released.
	}
	public void keyReleased(KeyEvent e){
		Object source = e.getSource();
		
		if( source == textsamplesize ){
			try{
				samplesize = new Integer(textsamplesize.getText());
			}
			catch(NumberFormatException exception){
			}
		}
	
		else if( source == textpercmiss ){
			try{
				percmiss = new Double(textpercmiss.getText());
			}
			catch(NumberFormatException exception){
			}
		}
	}
	
	public void setLearnSampleSize(Integer lss){
		numchains = lss;
	}
	public void setWindowSize(Integer gr){
		windowsize = gr;
	}

	public void setMaxIterations(Integer mi){
		maxiterations = mi;
	}
	
	public void setMaxFails(Integer mf){
		maxfails = mf;
	}

	public void setLineDistThresh(double d){
		linedistancethresh = d;
	}

	public void setLLikThresh(double d){
		llikhoodthresh = d;
	}

	public void setLikelihoodWindow(int d){
		likelihoodwindow = d;
	}

	public void setGradDistThresh(double d){
		gradientdistancethresh = d;
	}

	
	public int getMaxIterations(){
		return maxiterations;
	}
	
	public int getMaxFails(){
		return maxfails;
	}

	public double getLineDistThresh(){
		return linedistancethresh;
	}

//	public double getLineLikThresh(){
//		return llikhoodthresh;
//	}

	public double getLikelihoodWindow(){
		return likelihoodwindow;
	}
	
	public double getLLikThresh(){
		return llikhoodthresh;
	}
	
	public double getGradDistThresh(){
		return gradientdistancethresh;
	}

	
	public int getWindowSize(){
		return windowsize;
	}

	public void setRestarts(Integer rs){
		restarts = rs;
	}

	public void setSubsamples(Integer rs){
		subsamples = rs;
	}

	public void setNumblocks(Integer nb){
		numblocks = nb;
	}

	public void setNumbatches(Integer nb){
		numbatches = nb;
	}

	public void setSplitmode(Integer sm){
		splitmode=sm;
	}

	public void setDampingFac(Double df){
		dampingfac = df;
	}

	
	
	/**
	 * @return
	 * @uml.property  name="restarts"
	 */
	public int getRestarts(){
		return restarts;
	}
	
	public int getSubsamples(){
		return subsamples;
	}
	
	public int getNumBatches(){
		return numbatches;
	}
	
	public int getSplitmode(){
		return splitmode;
	}

	
	public double getDampingFac(){
		return dampingfac;
	}
	
	public int getNumblocks(){
		return numblocks;
	}
	
	public boolean gguse2phase(){
		return gg2phase;
	}
	
	
	public void setSettingsOpen(boolean b){
		settingswindowopen = b;
	}
	
	public int getNumChains(){
		return numchains;
	}

	public int getType_of_gradient() {
		return type_of_gradient;
	}
	
	public void setType_of_gradient(int t) {
		type_of_gradient =t;
	}
	
	
	public String[][] getSelectedNumRels(){
		if (numrelsfromfile)
			return numrelblocks;
		else{
			int[] selindices = numrellist.getSelectedIndices();
			String[][] result = new String[1][selindices.length];
			for (int i=0;i<selindices.length;i++)
				result[0][i]=(String)numRelListModel.elementAt(selindices[i]);
			return result;
		}
	}

	/**
	 * @param v
	 * @uml.property  name="verbose"
	 */
	public void setVerbose(boolean v){
		learnverbose = v;
	}
	
	public void setObjectivek(int v){
		objective = v;
	}
	
	public void set2phase(boolean v){
		gg2phase = v;
	}
	
	public void setRandomInit(boolean v){
		ggrandominit = v;
	}
	
	public boolean learnverbose(){
		return learnverbose;
	}
	

	public int getObjective(){
		return objective;
	}
	
	public boolean ggrandominit(){
		return ggrandominit;
	}
	
	/**
	 * @param v
	 * @uml.property  name="aca"
	 */
	public void setAca(boolean v){
		aca = v;
	}
	
	public void setUseGGs(boolean v){
		useggs = v;
	}
	
	public void setUseMemoize(boolean v){
		usememoize = v;
	}
	
	public boolean getUseGGs() {
		return useggs;
	}
	
	public boolean getUseMemoize() {
		return usememoize;
	}
	
	public boolean aca(){
		return aca;
	}
	
	
	/**
	 * @return
	 * @uml.property  name="paramratiothresh"
	 */
	public double getParamratiothresh(){
		return paramratiothresh;
	}
	
	public int omitRounds(){
		return omitrounds;
	}
	
	public void getRelDataFromPrimula(){
		data=myprimula.getReldata();
	}
	
	public void disableDataTab(){
		tabbedPane.setSelectedIndex(tabbedPane.indexOfComponent(learnsplitpane));
		tabbedPane.setEnabledAt(tabbedPane.indexOfComponent(dataPanel),false);
	}
	
	public void setParameters(Hashtable<String,Integer> params){
		parammodel.setParameters(params);
		parametertable.updateUI();
	}
	
	public void setParameterValues(double[] pvals){
		parammodel.setParameterEstimates(pvals);
		parametertable.updateUI();
	}
	
	public void setParametersPrimula() {
		String[] pars = parammodel.getParameters();
		double[] vals = parammodel.getEstimates();
		/* Need to cut off last element, which is the likelihood: */
		myprimula.setParameters(Arrays.copyOfRange(pars,0,pars.length-1),
				         		Arrays.copyOfRange(vals,0,vals.length-1));
		myprimula.updateBavaria();
	}
	
	private void readNumRels(){
		if(myprimula.rels instanceof SparseRelStruc){
			SparseRelStruc sparserst = (SparseRelStruc)myprimula.rels;
			Vector<NumRel> nextrels;
			nextrels = sparserst.getNumGlobals();
			for (int i=0;i<nextrels.size();i++)
				numRelListModel.addElement(nextrels.elementAt(i).name());
			
			
			nextrels = sparserst.getNumAttributes();
			for (int i=0;i<nextrels.size();i++)
				numRelListModel.addElement(nextrels.elementAt(i).name());
			
			nextrels = sparserst.getNumBinaryRelations();
			for (int i=0;i<nextrels.size();i++)
				numRelListModel.addElement(nextrels.elementAt(i).name());
			
			nextrels = sparserst.getNumArbitraryRelations();
			for (int i=0;i<nextrels.size();i++)
				numRelListModel.addElement(nextrels.elementAt(i).name());
			}
			
		}
	
	
	public boolean confirm(String text){
		int result = JOptionPane.showConfirmDialog(this, text, "Confirmation", JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION)
			return true;
		else //result == JOptionPane.NO_OPTION
			return false;
	}
	
	/* Transforms the data as follows:
	 * For all (Boolean) probabilistic relations with default value 'false'
	 * Default value is turned into '?', and pc% of the atoms that had
	 * 'false' values according to the default are explicitly added as 
	 * 'false' 
	 */
	private void subSampleData(int pc){
		data.subSampleData(pc);
	}
	
	public int lbfgsmemory(){
		return lbfgsmemory;
	}
	
	public int ggascentstrategy(){
		return ggascentstrategy;
	}
	
	public int threadascentstrategy(){
		return threadascentstrategy;
	}
	
	
	public double adam_beta1(){
		return adam_beta1;
	}
	
	public double adam_beta2(){
		return adam_beta2;
	}
	
	public double adam_epsilon(){
		return adam_epsilon;
	}
	
	public double adam_alpha(){
		return adam_alpha;
	}
	
	public double adagradfade(){
		return adagradfade;
	}
	
	public double adagradepsilon(){
		return adagradepsilon;
	}
	
	public void setThreadStrategy(int s){
		threadascentstrategy = s;
	}
	
	public int getThreadStrategy(){
		return threadascentstrategy;
	}
	
	public GGThread getLearnThread() {
		return lthread;
	}
	
	public void setGGStrategy(int s){
		ggascentstrategy = s;
	}
	
	public int getGGStrategy(){
		return ggascentstrategy;
	}
	
	public double getAdamBeta1(){
		return adam_beta1;
	}
	
	public void setAdamBeta1(double b){
		adam_beta1 = b;
	}
	
	public double getAdamBeta2(){
		return adam_beta2;
	}
	
	public void setAdamBeta2(double b){
		adam_beta2 = b;
	}
	public double getAdamEpsilon(){
		return adam_epsilon;
	}
	
	public void setAdamEpsilon(double b){
		adam_epsilon = b;
	}
	public double getAdamAlpha(){
		return adam_alpha;
	}
	
	public void setAdamAlpha(double b){
		adam_alpha = b;
	}
	
	public boolean ascentIsStochastic(){
		return (threadascentstrategy == AscentAdam || threadascentstrategy == AscentStochHeur1);
	}
	
	public void startLearning() {
		RelData beforesplitdata;
		RelData[] learndata = null;
		if (this.getSubsamples()<100){
			beforesplitdata = data.subSampleData(this.getSubsamples());
		}
		else{
			beforesplitdata = data;
		}
		if (ascentIsStochastic()){
			try {
				learndata = beforesplitdata.randomSplit(this.getNumBatches(),this.getSplitmode());
			}
			catch(RBNRuntimeException e) {System.out.println(e);}
		}
		else{
			learndata = new RelData[1];
			learndata[0]=beforesplitdata;
			}
		
		lthread = new LearnThread(myprimula, 
				data,
				learndata, 
				parammodel, 
				parametertable,
				textnumrestarts,
				this);
		lthread.start(); 
	}
}
