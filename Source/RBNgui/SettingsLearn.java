/*
* SettingsLearn.java 
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.*;

import javax.swing.*; 

import javax.swing.border.*;

import RBNLearning.GradientGraph;
import RBNLearning.RelData;
import RBNpackage.ProbForm; 

public class SettingsLearn extends JFrame implements ActionListener, ItemListener, KeyListener{

	
	private JTabbedPane tabbedPaneTop   = new JTabbedPane();
	
	/**
	 * @uml.property  name="samplesizelabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel samplesizelabel = new JLabel("Gibbs chains");
	/**
	 * @uml.property  name="restartslabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel restartslabel = new JLabel("Restarts (-1 = until stopped)");
	
	private JLabel subsamplelabel = new JLabel("Sub-sample 'false' atoms");
	
	private JLabel numblocklabel = new JLabel("Number of parameter blocks");
	
	private JLabel numbatchlabel = new JLabel("Number of mini batches");
	
	private JLabel dampinglabel = new JLabel("Damping factor");

	/**
	 * @uml.property  name="gibbsroundslabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel gibbsroundslabel = new JLabel("Gibbs Window Size");
	/**
	 * @uml.property  name="maxiterationslabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel maxiterationslabel = new JLabel("Max. iterations gradient search");
	/**
	 * @uml.property  name="linedistancelabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel linedistancelabel = new JLabel("Distance threshold");
	
	/**
	 * @uml.property  name="linelikelihoodlabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel llikelihoodlabel = new JLabel("Log-Likelihood threshold");
	private JLabel likelihoodwindowlabel = new JLabel("Likelihood window");
	
	
	private JLabel adambeta1label = new JLabel("Adam - beta1");
	private JLabel adambeta2label = new JLabel("Adam - beta2");
	private JLabel adamepsilonlabel = new JLabel("Adam - epsilon");
	private JLabel adamalphalabel = new JLabel("Adam - alpha");
	
//	private JLabel gradientdistancelabel = new JLabel("Distance threshold (gradient search)");
	/**
	 * @uml.property  name="maxfailslabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel maxfailslabel = new JLabel("Max. fails (Sample missing)");
//	private JLabel verboselabel = new JLabel("Verbose");
	
	/**
	 * @uml.property  name="samplesizetext"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField samplesizetext = new JTextField(5);
	/**
	 * @uml.property  name="restartstext"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField restartstext = new JTextField(5);
	private JTextField subsampletext = new JTextField(3);
	private JTextField numblocktext = new JTextField(3);
	private JTextField numbatchtext = new JTextField(3);
	private JTextField dampingtext = new JTextField(3);
	
	/**
	 * @uml.property  name="gibbsroundstext"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField gibbsroundstext = new JTextField(5);
	/**
	 * @uml.property  name="maxiterationstext"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField maxiterationstext = new JTextField(5);
	/**
	 * @uml.property  name="linedistancetext"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField linedistancetext = new JTextField(5);
	/**
	 * @uml.property  name="linelikelihoodtext"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField llikelihoodtext = new JTextField(5);
	private JTextField likelihoodwindowtext = new JTextField(2);
	
	
	/**
	 * @uml.property  name="gradientdistancetext"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField gradientdistancetext = new JTextField(5);
	/**
	 * @uml.property  name="maxfailstext"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField maxfailstext = new JTextField(5);

	private JTextField adambeta1text = new JTextField(5);
	private JTextField adambeta2text = new JTextField(5);
	private JTextField adamepsilontext = new JTextField(5);
	private JTextField adamalphatext = new JTextField(5);
	
	
	/**
	 * @uml.property  name="samplesizepanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel samplesizepanel = new JPanel(new FlowLayout());
	/**
	 * @uml.property  name=""
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	//private JPanel restartspanel = new JPanel(new FlowLayout());
	private JPanel restartspanel = new JPanel(new FlowLayout());
	private JPanel subsamplepanel = new JPanel(new FlowLayout());
	
	private JPanel generalbuttonspanel = new JPanel(new GridLayout(3,1));
	/**
	 * @uml.property  name="gibbsroundspanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel gibbsroundspanel = new JPanel(new FlowLayout());
	/**
	 * @uml.property  name="maxiterationspanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel maxiterationspanel = new JPanel(new FlowLayout());
	/**
	 * @uml.property  name="linedistancepanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel linedistancepanel = new JPanel(new FlowLayout());
	
	/**
	 * @uml.property  name="linelikelihoodpanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel llikelihoodpanel = new JPanel(new FlowLayout());
	
	private JPanel numblockpanel = new JPanel(new FlowLayout());
	
	private JPanel adambeta1panel = new JPanel(new FlowLayout());
	private JPanel adambeta2panel = new JPanel(new FlowLayout());
	private JPanel adamepsilonpanel = new JPanel(new FlowLayout());
	private JPanel adamalphapanel = new JPanel(new FlowLayout());
	
	
	private JPanel numbatchpanel = new JPanel(new FlowLayout());
	private JPanel dampingpanel = new JPanel(new FlowLayout());
	private JPanel likelihoodwindowpanel = new JPanel(new FlowLayout());
	/**
	 * @uml.property  name="gradientdistancepanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel gradientdistancepanel = new JPanel(new FlowLayout());
	/**
	 * @uml.property  name="maxfailspanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel maxfailspanel = new JPanel(new FlowLayout());
	
	/**
	 * @uml.property  name="generaloptions"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel generaloptions = new JPanel();
	
	
	
	/**
	 * @uml.property  name="incompleteoptions"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel incompleteoptions = new JPanel(new GridLayout(4,1));
	/**
	 * @uml.property  name="terminateoptions"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	//private JPanel ascentoptions = new JPanel();
	private JTabbedPane ascentoptionstabs = new JTabbedPane();

	private JPanel ascentoptions = new JPanel();
	private JPanel batchoptions = new JPanel();
	private JPanel adamoptions = new JPanel();
	
	
	private JPanel ascentstrategies = new JPanel(new GridLayout(1,2));
	
	private ButtonGroup topascent_menu_bg        = new ButtonGroup();
	private JPanel topascent_menu       = new JPanel(new GridLayout(5,1));
		
	private JRadioButton asc_batch = new JRadioButton("Use Batch");
	private JRadioButton asc_adam = new JRadioButton("Use Adam");
	private JRadioButton asc_heur1 = new JRadioButton("Heur1");
	private JRadioButton asc_block = new JRadioButton("Block");
	private JRadioButton asc_twophase = new JRadioButton("Two Phase");
	
	private ButtonGroup subascent_menu_bg        = new ButtonGroup();
	private JPanel subascent_menu       = new JPanel(new GridLayout(4,1));
		
	private JRadioButton asc_lbfgs = new JRadioButton("LBFGS");
	private JRadioButton asc_adagrad = new JRadioButton("Adagrad");
	private JRadioButton asc_fletcherreeves = new JRadioButton("Fletcher Reeves");
	private JRadioButton asc_direct = new JRadioButton("Greedy");
	
	private ButtonGroup lossfunc_bg = new ButtonGroup();
	private JPanel lossfunc_menu = new JPanel(new GridLayout(3,1));
	
	private ButtonGroup divideoptions_bg = new ButtonGroup();
	private JRadioButton div_across = new JRadioButton("Split across domains");
	private JRadioButton div_by = new JRadioButton("Split by domains");
	
	
	private JPanel divideoptions_menu = new JPanel(new GridLayout(2,1));
	private JPanel divideoptions_menu_buttons = new JPanel(new GridLayout(1,2));
	
	private ButtonGroup gradientrep_bg = new ButtonGroup();
	private JRadioButton grad_array = new JRadioButton("Dense");
	private JRadioButton grad_sparse = new JRadioButton("Sparse");
	
	
	private JPanel gradoptions_menu = new JPanel(new GridLayout(1,2));
	
	private JRadioButton loss_lik = new JRadioButton("Likelihood (use with incomplete data)");
	private JRadioButton loss_loglik = new JRadioButton("Log-Likelihood");
	private JRadioButton loss_se = new JRadioButton("Squared Error");
	
	
//	private JMenuItem asc_batch         = new JMenuItem("Batch");
//	private JMenuItem asc_adam         = new JMenuItem("Adam");
//	private JMenuItem asc_heur1        = new JMenuItem("Heur1");
//	private JMenuItem asc_block        = new JMenuItem("Block");
//	
//	
	

	/**
	 * @uml.property  name="verbosecheckbox"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBox verbosecheckbox = new JCheckBox("Verbose");
	private JCheckBox loglikcheckbox = new JCheckBox("Use Log Likelihood");
	private JCheckBox useggscheckbox = new JCheckBox("Use Gradient Graphs");
	private JCheckBox usememoizecheckbox = new JCheckBox("With Memoization");

	private JCheckBox randominitcheckbox = new JCheckBox("Random initialization of numeric relations");
	/**
	 * @uml.property  name="acacheckbox"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBox acacheckbox = new JCheckBox("ACA");

	/**
	 * @uml.property  name="learnmodule"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="settingswindow:RBNgui.LearnModule"
	 */
	private LearnModule learnmodule;
	
	public SettingsLearn(LearnModule lm){
		
		learnmodule = lm;
		this.addWindowListener(
				new WindowAdapter(){
					public void windowClosing(WindowEvent e){
						learnmodule.setSettingsOpen(false);
						dispose();
					}
				}
		);
		samplesizepanel.add(samplesizelabel);
		samplesizepanel.add(samplesizetext);
		
		restartspanel.add(restartslabel);
		restartspanel.add(restartstext);
		
		numblockpanel.add(numblocklabel);
		numblockpanel.add(numblocktext);
		
		subsamplepanel.add(subsamplelabel);
		subsamplepanel.add(subsampletext);
		
		generalbuttonspanel.add(verbosecheckbox);
//		generalbuttonspanel.add(loglikcheckbox);
		generalbuttonspanel.add(randominitcheckbox);
		
		lossfunc_menu.add(loss_lik);
		lossfunc_menu.add(loss_loglik);
		lossfunc_menu.add(loss_se);
		
		lossfunc_bg.add(loss_lik);
		lossfunc_bg.add(loss_loglik);
		lossfunc_bg.add(loss_se);
		
		generaloptions.setLayout(new BoxLayout(generaloptions,BoxLayout.Y_AXIS));
		lossfunc_menu.setBorder(new TitledBorder("Objective Function"));
		generalbuttonspanel.add(lossfunc_menu);
		
		
		
		
		
		gibbsroundspanel.add(gibbsroundslabel);
		gibbsroundspanel.add(gibbsroundstext);
		
		
		maxiterationspanel.add(maxiterationslabel);
		maxiterationspanel.add(maxiterationstext);
		
		linedistancepanel.add(linedistancelabel);
		linedistancepanel.add(linedistancetext);
		
		llikelihoodpanel.add(llikelihoodlabel);
		llikelihoodpanel.add(llikelihoodtext);
		
		numbatchpanel.add(numbatchlabel);
		numbatchpanel.add(numbatchtext);
		
		dampingpanel.add(dampinglabel);
		dampingpanel.add(dampingtext);
	
		likelihoodwindowpanel.add(likelihoodwindowlabel);
		likelihoodwindowpanel.add(likelihoodwindowtext);
		
		maxfailspanel.add(maxfailslabel);
		maxfailspanel.add(maxfailstext);
		
		generaloptions.add(restartspanel);
		generaloptions.add(subsamplepanel);
		generaloptions.add(generalbuttonspanel);
		
		adambeta1panel.add(adambeta1label);
		adambeta1panel.add(adambeta1text);
		
		adambeta2panel.add(adambeta2label);
		adambeta2panel.add(adambeta2text);
		
		adamepsilonpanel.add(adamepsilonlabel);
		adamepsilonpanel.add(adamepsilontext);
		
		adamalphapanel.add(adamalphalabel);
		adamalphapanel.add(adamalphatext);
		
//		generaloptions.setBorder(BorderFactory.createTitledBorder("General"));
		
		
		
		ascentoptions.setLayout(new BoxLayout(ascentoptions,BoxLayout.Y_AXIS));
		batchoptions.setLayout(new BoxLayout(batchoptions,BoxLayout.Y_AXIS));
		adamoptions.setLayout(new BoxLayout(adamoptions,BoxLayout.Y_AXIS));
		
		
		batchoptions.add(asc_batch);
		adamoptions.add(asc_adam);
		topascent_menu.add(asc_heur1);
		topascent_menu.add(asc_block);
		topascent_menu.add(asc_twophase);
//		topascent_menu.setBorder(BorderFactory.createLineBorder(Color.black));
		topascent_menu.setBorder(new TitledBorder("Top Strategy"));
		
		asc_batch.addItemListener(this);
		asc_adam.addItemListener(this);
		asc_heur1.addItemListener(this);
		asc_block.addItemListener(this);	
		asc_twophase.addItemListener(this);	
		
		loss_lik.addItemListener(this);
		loss_loglik.addItemListener(this);
		loss_se.addItemListener(this);
		
		
		
		switch (learnmodule.getObjective()){
		case LearnModule.UseLik:
			loss_lik.setSelected(true);
			break;
		case LearnModule.UseLogLik:
			loss_loglik.setSelected(true);
			break;
		case LearnModule.UseSquaredError:
			loss_se.setSelected(true);
			break;
		}

		switch (learnmodule.getThreadStrategy()){
		case LearnModule.AscentBatch:
			asc_batch.setSelected(true);
			break;
		case LearnModule.AscentAdam:
			asc_adam.setSelected(true);
			break;
		case LearnModule.AscentStochHeur1:
			asc_heur1.setSelected(true);
			break;
		case LearnModule.AscentBlock:
			asc_block.setSelected(true);
			break;
		case LearnModule.AscentTwoPhase:
			asc_block.setSelected(true);
			break;
		}
		
		switch (learnmodule.getSplitmode()) {
		case RelData.SPLIT_BY_DOMAIN:
			div_by.setSelected(true);
			break;
		case RelData.SPLIT_ACROSS_DOMAINS:
			div_across.setSelected(true);
			break;
		}
		
		topascent_menu_bg.add(asc_batch);
		topascent_menu_bg.add(asc_adam);
//		topascent_menu_bg.add(asc_heur1);
//		topascent_menu_bg.add(asc_block);
//		topascent_menu_bg.add(asc_twophase);
	
		
		subascent_menu.add(asc_lbfgs);
		subascent_menu.add(asc_direct);
		subascent_menu.add(asc_adagrad);
		subascent_menu.add(asc_fletcherreeves);
		subascent_menu.setBorder(new TitledBorder("Method"));
		
		asc_lbfgs.addItemListener(this);
		asc_adagrad.addItemListener(this);
		asc_fletcherreeves.addItemListener(this);
		asc_direct.addItemListener(this);	
		
		switch (learnmodule.getGGStrategy()){
		case LearnModule.AscentLBFGS:
			asc_lbfgs.setSelected(true);
			break;
		case LearnModule.AscentAdagrad:
			asc_adagrad.setSelected(true);
			break;
		case LearnModule.AscentFletcherReeves:
			asc_fletcherreeves.setSelected(true);
			break;
		case LearnModule.AscentDirectGradient:
			asc_direct.setSelected(true);
			break;
		}

		switch (learnmodule.getType_of_gradient()){
		case ProbForm.RETURN_ARRAY:
			grad_array.setSelected(true);
			break;
		case ProbForm.RETURN_SPARSE:
			grad_sparse.setSelected(true);
			break;		
		}
		
		useggscheckbox.setSelected(learnmodule.getUseGGs());
		usememoizecheckbox.setSelected(learnmodule.getUseMemoize());
		usememoizecheckbox.setToolTipText("Use in presence of shared sub-formulas in RBN; requires extra space");

		subascent_menu_bg.add(asc_lbfgs);
		subascent_menu_bg.add(asc_adagrad);
		subascent_menu_bg.add(asc_fletcherreeves);
		subascent_menu_bg.add(asc_direct);

		ascentstrategies.add(topascent_menu);
		batchoptions.add(subascent_menu);
	
	
	
		batchoptions.add(linedistancepanel);
		
//		ascentoptions.add(numblockpanel);
//		ascentoptions.add(dampingpanel);
		
		divideoptions_menu.setBorder(new TitledBorder("Batch division"));
		divideoptions_bg.add(div_across);
		divideoptions_bg.add(div_by);
		divideoptions_menu.add(numbatchpanel);
		divideoptions_menu_buttons.add(div_across);
		divideoptions_menu_buttons.add(div_by);
		divideoptions_menu.add(divideoptions_menu_buttons);

		gradientrep_bg.add(grad_array);
		gradientrep_bg.add(grad_sparse);
		grad_array.setToolTipText("use: few parameters or ground atoms depending on most parameters");
		grad_sparse.setToolTipText("use: many parameters and single ground atom depends only on few parameters");
		gradoptions_menu.setBorder(new TitledBorder("Gradient representation"));
		gradoptions_menu.add(grad_array);
		gradoptions_menu.add(grad_sparse);
		
		adamoptions.add(divideoptions_menu);
		adamoptions.add(adambeta1panel);
		adamoptions.add(adambeta2panel);
		adamoptions.add(adamepsilonpanel);
		adamoptions.add(adamalphapanel);
		adamoptions.add(useggscheckbox);
		adamoptions.add(usememoizecheckbox);
		adamoptions.add(gradoptions_menu);
		
		ascentoptions.add(maxiterationspanel);
		ascentoptions.add(llikelihoodpanel);
		ascentoptions.add(likelihoodwindowpanel);
		
		ascentoptions.add(ascentoptionstabs);
		
//		terminateoptions.setBorder(BorderFactory.createTitledBorder("Termination"));
		
		incompleteoptions.add(samplesizepanel);
		incompleteoptions.add(gibbsroundspanel);
		incompleteoptions.add(maxfailspanel);
		incompleteoptions.add(acacheckbox);
		incompleteoptions.setBorder(BorderFactory.createTitledBorder("Incomplete Data"));
		

		
		
		tabbedPaneTop.add("General",generaloptions);
		tabbedPaneTop.add("Gradient Ascent",ascentoptions);
		tabbedPaneTop.add("Incomplete Data",incompleteoptions);
		
		ascentoptionstabs.add("Batch",batchoptions);
		ascentoptionstabs.add("Adam",adamoptions);

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));
		contentPane.add(tabbedPaneTop);


		likelihoodwindowlabel.setToolTipText("Likelihood gain is averaged over this many iterations");
		linedistancelabel.setToolTipText("Distance threshold for termination of linesearch");
		llikelihoodlabel.setToolTipText("Average per-node absolute likelihood gain over the last 'windowsize' iterations");
		
		samplesizetext.setText(""+learnmodule.getNumChains());
		samplesizetext.addKeyListener(this);
		restartstext.setText(""+learnmodule.getRestarts());
		restartstext.addKeyListener(this);
		subsampletext.setText(""+learnmodule.getSubsamples());
		subsampletext.addKeyListener(this);
		numblocktext.setText(""+learnmodule.getNumblocks());
		numblocktext.addKeyListener(this);
		numbatchtext.setText(""+learnmodule.getNumBatches());
		numbatchtext.addKeyListener(this);
		dampingtext.setText(""+learnmodule.getDampingFac());
		dampingtext.addKeyListener(this);
	
		
		gibbsroundstext.setText(""+learnmodule.getWindowSize());
		gibbsroundstext.addKeyListener(this);
		maxiterationstext.setText(""+learnmodule.getMaxIterations());
		maxiterationstext.addKeyListener(this);
		linedistancetext.setText(""+learnmodule.getLineDistThresh());
		linedistancetext.addKeyListener(this);
		llikelihoodtext.setText(""+learnmodule.getLLikThresh());
		llikelihoodtext.addKeyListener(this);
		likelihoodwindowtext.setText(""+learnmodule.getLikelihoodWindow());
		likelihoodwindowtext.addKeyListener(this);
		gradientdistancetext.setText(""+learnmodule.getGradDistThresh());
		gradientdistancetext.addKeyListener(this);
		maxfailstext.setText(""+learnmodule.getMaxFails());
		maxfailstext.addKeyListener(this);
		verbosecheckbox.setSelected(false);
		verbosecheckbox.addItemListener(this);
		
		
		acacheckbox.setSelected(false);
		acacheckbox.addItemListener(this);
		
		useggscheckbox.addItemListener(this);
		usememoizecheckbox.addItemListener(this);
		
		randominitcheckbox.setSelected(learnmodule.ggrandominit());
		randominitcheckbox.addItemListener(this);
		
		adambeta1text.setText("" + learnmodule.getAdamBeta1());
		adambeta1text.addKeyListener(this);
		adambeta2text.setText("" + learnmodule.getAdamBeta2());
		adambeta2text.addKeyListener(this);
		adamepsilontext.setText("" + learnmodule.getAdamEpsilon());
		adamepsilontext.addKeyListener(this);
		adamalphatext.setText("" + learnmodule.getAdamAlpha());
		adamalphatext.addKeyListener(this);
		
		div_across.addItemListener(this);
		div_by.addItemListener(this);
		
		grad_array.addItemListener(this);
		grad_sparse.addItemListener(this);
		
		ImageIcon icon = new ImageIcon("small_logo.jpg");
		if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) //image ok
			this.setIconImage(icon.getImage());
		this.setTitle("Learning Settings");
		this.setSize(450, 600);
		this.setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e) {
		
	}

	public void keyPressed(KeyEvent e){
	}

	public void keyTyped(KeyEvent e){

	}

	public void keyReleased(KeyEvent e){
		Object source = e.getSource();	
		if ( source == restartstext ){
			try{
				Integer tempint = new Integer(restartstext.getText());
				learnmodule.setRestarts(tempint.intValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if( source == samplesizetext ){
			try{
				Integer tempint = new Integer(samplesizetext.getText());
				learnmodule.setLearnSampleSize(tempint.intValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == subsampletext ){
			try{
				Integer tempint = new Integer(subsampletext.getText());
				learnmodule.setSubsamples(tempint.intValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == numblocktext ){
			try{
				Integer tempint = new Integer(numblocktext.getText());
				learnmodule.setNumblocks(tempint.intValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == numbatchtext ){
			try{
				Integer tempint = new Integer(numbatchtext.getText());
				learnmodule.setNumbatches(tempint.intValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == dampingtext ){
			try{
				Double tempdoub = new Double(dampingtext.getText());
				learnmodule.setDampingFac(tempdoub.doubleValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == gibbsroundstext ){
			try{
				Integer tempint = new Integer(gibbsroundstext.getText());
				learnmodule.setWindowSize(tempint.intValue());  
			}
			catch(NumberFormatException exception){
			}
		}		
		else if ( source == maxiterationstext ){
			try{
				Integer tempint = new Integer(maxiterationstext.getText());
				learnmodule.setMaxIterations(tempint.intValue());  
			}
			catch(NumberFormatException exception){
			}
		}
		else if ( source == linedistancetext ){
			try{
				Double tempdoub = new Double(linedistancetext.getText());
				learnmodule.setLineDistThresh(tempdoub.doubleValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == llikelihoodtext ){
			try{
				Double tempdoub = new Double(llikelihoodtext.getText());
				learnmodule.setLLikThresh(tempdoub.doubleValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == likelihoodwindowtext ){
			try{
				Integer tempint = new Integer(likelihoodwindowtext.getText());
				learnmodule.setLikelihoodWindow(tempint.intValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}		}
		else if ( source == gradientdistancetext ){
			try{
				Double tempdoub = new Double(gradientdistancetext.getText());
				learnmodule.setGradDistThresh(tempdoub.doubleValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == maxfailstext ){
			try{
				Integer tempint = new Integer(maxfailstext.getText());
				learnmodule.setMaxFails(tempint.intValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == adambeta1text ){
			try{
				Double tempdoub = new Double(adambeta1text.getText());
				learnmodule.setAdamBeta1(tempdoub.doubleValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == adambeta2text ){
			try{
				Double tempdoub = new Double(adambeta2text.getText());
				learnmodule.setAdamBeta2(tempdoub.doubleValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == adamepsilontext ){
			try{
				Double tempdoub = new Double(adamepsilontext.getText());
				learnmodule.setAdamEpsilon(tempdoub.doubleValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}
		else if ( source == adamalphatext ){
			try{
				Double tempdoub = new Double(adamalphatext.getText());
				learnmodule.setAdamAlpha(tempdoub.doubleValue());  
			}
			catch(NumberFormatException exception){
				System.out.println(exception);
			}
		}

	}
	
	
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if ( source == verbosecheckbox ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setVerbose(true);
			}
			else
				learnmodule.setVerbose(false);
		}
		if ( source == loss_lik) {
			if (e.getStateChange() == ItemEvent.SELECTED){
			learnmodule.setObjectivek(LearnModule.UseLik);
			}
		}
		if ( source == loss_loglik) {
			if (e.getStateChange() == ItemEvent.SELECTED){
			learnmodule.setObjectivek(LearnModule.UseLogLik);
			}
		}
		if ( source == loss_se) {
			if (e.getStateChange() == ItemEvent.SELECTED){
			learnmodule.setObjectivek(LearnModule.UseSquaredError);
			}
		}
		if ( source == randominitcheckbox ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setRandomInit(true);
			}
			else
				learnmodule.setRandomInit(false);
		}
		if ( source == acacheckbox ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setAca(true);
			}
			else
				learnmodule.setAca(false);
		}
		if ( source == useggscheckbox ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setUseGGs(true);
			}
			else
				learnmodule.setUseGGs(false);
		}
		if ( source == usememoizecheckbox ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setUseMemoize(true);
			}
			else
				learnmodule.setUseMemoize(false);
		}
		if ( source == asc_batch ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setThreadStrategy(LearnModule.AscentBatch);
				
				maxiterationstext.setEditable(true);
				linedistancetext.setEditable(true);
				llikelihoodtext.setEditable(true);
				likelihoodwindowtext.setEditable(true);
				numbatchtext.setEditable(false);
				numblocktext.setEditable(false);
				dampingtext.setEditable(false);
				
				adambeta1text.setEditable(false);
				adambeta2text.setEditable(false);
				adamepsilontext.setEditable(false);
				adamalphatext.setEditable(false);
				

				asc_lbfgs.setEnabled(true);
				asc_adagrad.setEnabled(true);
				asc_fletcherreeves.setEnabled(true);
				asc_direct.setEnabled(true);
			}
		}
		if ( source == asc_adam ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setThreadStrategy(LearnModule.AscentAdam);
				
				maxiterationstext.setEditable(true);
				linedistancetext.setEditable(false);
				llikelihoodtext.setEditable(true);
				likelihoodwindowtext.setEditable(true);
				numbatchtext.setEditable(true);
				numblocktext.setEditable(false);
				dampingtext.setEditable(false);
				
				adambeta1text.setEditable(true);
				adambeta2text.setEditable(true);
				adamepsilontext.setEditable(true);
				adamalphatext.setEditable(true);
				
				asc_lbfgs.setEnabled(false);
				asc_adagrad.setEnabled(false);
				asc_fletcherreeves.setEnabled(false);
				asc_direct.setEnabled(false);
			}
		}
		if ( source == asc_heur1 ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setThreadStrategy(LearnModule.AscentStochHeur1);

				maxiterationstext.setEditable(true);
				linedistancetext.setEditable(true);
				llikelihoodtext.setEditable(true);
				likelihoodwindowtext.setEditable(true);
				numbatchtext.setEditable(true);
				numblocktext.setEditable(false);
				dampingtext.setEditable(true);
				
				adambeta1text.setEditable(false);
				adambeta2text.setEditable(false);
				adamepsilontext.setEditable(false);
				adamalphatext.setEditable(false);
				
				asc_lbfgs.setEnabled(false);
				asc_adagrad.setEnabled(false);
				asc_fletcherreeves.setEnabled(false);
				asc_direct.setEnabled(false);
				
				
			}
		}
		if ( source == asc_block ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setThreadStrategy(LearnModule.AscentBlock);
				
				maxiterationstext.setEditable(true);
				linedistancetext.setEditable(true);
				llikelihoodtext.setEditable(true);
				likelihoodwindowtext.setEditable(true);
				numbatchtext.setEditable(false);
				numblocktext.setEditable(true);
				dampingtext.setEditable(false);
				
				adambeta1text.setEditable(false);
				adambeta2text.setEditable(false);
				adamepsilontext.setEditable(false);
				adamalphatext.setEditable(false);

				asc_lbfgs.setEnabled(true);
				asc_adagrad.setEnabled(true);
				asc_fletcherreeves.setEnabled(true);
				asc_direct.setEnabled(true);
			}
		}
		if ( source == asc_twophase ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setThreadStrategy(LearnModule.AscentTwoPhase);
				
				maxiterationstext.setEditable(true);
				linedistancetext.setEditable(true);
				llikelihoodtext.setEditable(true);
				likelihoodwindowtext.setEditable(true);
				numbatchtext.setEditable(false);
				numblocktext.setEditable(false);
				dampingtext.setEditable(false);
				
				adambeta1text.setEditable(false);
				adambeta2text.setEditable(false);
				adamepsilontext.setEditable(false);
				adamalphatext.setEditable(false);

				asc_lbfgs.setEnabled(true);
				asc_adagrad.setEnabled(true);
				asc_fletcherreeves.setEnabled(true);
				asc_direct.setEnabled(true);
			}
		}
		if ( source == asc_lbfgs ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setGGStrategy(LearnModule.AscentLBFGS);
			}
		}
		if ( source == asc_adagrad ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setGGStrategy(LearnModule.AscentAdagrad);
			}
		}
		if ( source == asc_fletcherreeves){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setGGStrategy(LearnModule.AscentFletcherReeves);
			}
		}
		if ( source == asc_direct ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setGGStrategy(LearnModule.AscentDirectGradient);
			}
		}
		if ( source == div_across ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setSplitmode(RelData.SPLIT_ACROSS_DOMAINS);
			}
		}
		if ( source == div_by){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setSplitmode(RelData.SPLIT_BY_DOMAIN);
			}
		}
		if ( source == grad_array){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setType_of_gradient(ProbForm.RETURN_ARRAY);
			}
		}
		if ( source == grad_sparse){
			if (e.getStateChange() == ItemEvent.SELECTED){
				learnmodule.setType_of_gradient(ProbForm.RETURN_SPARSE);
			}
		}
	}

	
	
}
