/*
 * Settings.java
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
import java.awt.*;
import javax.swing.*; 
import java.awt.event.*;
import java.util.Vector;
import java.io.*;

public class Settings extends JFrame implements ActionListener, ItemListener{

	/**
	 * @uml.property  name="cptparents"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel cptparents = new JLabel("Max CPT Parents");
	/**
	 * @uml.property  name="numsubsamples_minmax"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel numsubsamples_minmax  = new JLabel("Subsamples(Min,Max)");
	/**
	 * @uml.property  name="numsubsamples_adapt"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel numsubsamples_adapt  = new JLabel("Subsamples(Adaptation)");
//	private JLabel doubleparam = new JLabel("double parameter");
	/**
	 * @uml.property  name="textcptparents"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField textcptparents = new JTextField(3);
	/**
	 * @uml.property  name="textnumsubsamples_minmax"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField textnumsubsamples_minmax = new JTextField(3);
	/**
	 * @uml.property  name="textnumsubsamples_adapt"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField textnumsubsamples_adapt = new JTextField(3);


//	private JTextField textdoubleparam    = new JTextField(3);

	/**
	 * @uml.property  name="parameterpanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel parameterpanel = new JPanel(new GridLayout(3,2));
	/**
	 * @uml.property  name="samplingorder"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel samplingorder= new JPanel(new GridLayout(1,2));
	/**
	 * @uml.property  name="otheroptions"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel otheroptions = new JPanel(new GridLayout(1,2));

	/**
	 * @uml.property  name="forward"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButton forward = new JRadioButton("forward");
	/**
	 * @uml.property  name="ripple"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButton ripple	= new JRadioButton("ripple");

	/* Logging */
	/**
	 * @uml.property  name="logpanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel logpanel = new JPanel(new GridLayout(2,1));
	/**
	 * @uml.property  name="tracepanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel tracepanel  = new JPanel(new GridLayout(1,3));
	/**
	 * @uml.property  name="addlogpanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel addlogpanel  = new JPanel(new GridLayout(1,3));
	/**
	 * @uml.property  name=""
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButton tracenone = new JRadioButton("None");
	/**
	 * @uml.property  name="traceshort"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButton traceshort  = new JRadioButton("Normal");
	/**
	 * @uml.property  name="tracelong"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButton tracelong = new JRadioButton("Long");
	/**
	 * @uml.property  name="logsampleord"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBox logsampleord = new JCheckBox("Sample Order");
	/**
	 * @uml.property  name="logevidence"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBox logevidence = new JCheckBox("Evidence");
	/**
	 * @uml.property  name="lognetwstats"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBox lognetwstats = new JCheckBox("Network Stats");

	/**
	 * @uml.property  name="fileField"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField fileField = new JTextField(25);
	/**
	 * @uml.property  name="fileButton"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton fileButton	= new JButton("Browse");
	/**
	 * @uml.property  name="fileChooser"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JFileChooser fileChooser = new JFileChooser( "." );


	/**
	 * @uml.property  name="adaptive"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBox adaptive	= new JCheckBox("Adaptive sampling");

	/**
	 * @uml.property  name="evidence"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="swindow:RBNgui.InferenceModule"
	 */
	private  InferenceModule evidence;

	public Settings(final InferenceModule evidence){
		this.evidence = evidence;
		this.addWindowListener(
				new WindowAdapter(){
					public void windowClosing(WindowEvent e){
						evidence.setSettingsOpen(false);
						dispose();
					}
				}
		);
		parameterpanel.add(cptparents);
		parameterpanel.add(textcptparents);
		textcptparents.addActionListener(this);
		textcptparents.setText(""+evidence.getCPTParents());
		parameterpanel.add(numsubsamples_minmax);
		parameterpanel.add(numsubsamples_adapt);
		parameterpanel.add(textnumsubsamples_minmax);
		parameterpanel.add(textnumsubsamples_adapt);
		textnumsubsamples_minmax.addActionListener(this);
		textnumsubsamples_minmax.setText(""+evidence.getNumSubsamples_minmax());
		textnumsubsamples_adapt.addActionListener(this);
		textnumsubsamples_adapt.setText(""+evidence.getNumSubsamples_adapt());

		samplingorder.setBorder(BorderFactory.createTitledBorder("Sampling Order"));
		ButtonGroup sampling = new ButtonGroup();
		sampling.add(forward);
		samplingorder.add(forward);
		forward.addItemListener(this);
		sampling.add(ripple);
		samplingorder.add(ripple);
		ripple.addItemListener(this);

		int sampleorder = evidence.getSampleOrdMode();    

		if( sampleorder == InferenceModule.OPTION_SAMPLEORD_FORWARD ){
			forward.setSelected(true);
		}
		else if( sampleorder == InferenceModule.OPTION_SAMPLEORD_RIPPLE ){
			ripple.setSelected(true);
		}

		tracepanel.setBorder(BorderFactory.createTitledBorder("Trace"));
		ButtonGroup tracegroup = new ButtonGroup();
		tracegroup.add(tracenone);
		tracepanel.add(tracenone);
		tracenone.addItemListener(this);
		tracegroup.add(traceshort);
		tracepanel.add(traceshort);
		traceshort.addItemListener(this);
		tracegroup.add(tracelong);
		tracepanel.add(tracelong);
		tracelong.addItemListener(this);

		addlogpanel.add(logsampleord);
		logsampleord.addItemListener(this);
		addlogpanel.add(logevidence);
		logevidence.addItemListener(this);
		addlogpanel.add(lognetwstats);
		lognetwstats.addItemListener(this);


		logpanel.setBorder(BorderFactory.createTitledBorder("Log"));
		logpanel.add(tracepanel);
		logpanel.add(addlogpanel);



		boolean[] samplelog = evidence.getSampleLogMode();

		if( !samplelog[2] && !samplelog[3] ){
			tracenone.setSelected(true);
		}
		else if( samplelog[2]){
			traceshort.setSelected(true);
		}
		else if( samplelog[3]){
			tracelong.setSelected(true);
		}


		otheroptions.add(adaptive);
		adaptive.addItemListener(this);
		int adap = evidence.getAdaptiveMode();
		if( adap == InferenceModule.OPTION_SAMPLE_ADAPTIVE ){
			adaptive.setSelected(true);
		}

		JPanel filePanel = new JPanel();
		filePanel.setLayout(new BoxLayout(filePanel,BoxLayout.X_AXIS));
		JLabel fileLabel   = new JLabel("Log file");
		fileButton.addActionListener( this );
		fileLabel.setHorizontalAlignment( JLabel.RIGHT );
		filePanel.add( fileLabel);
		filePanel.add( fileField);
		filePanel.add( fileButton);
		fileField.setText(evidence.getLogfilename());

		JPanel radiopanel = new JPanel();
		radiopanel.setLayout(new BoxLayout(radiopanel,BoxLayout.Y_AXIS));


		radiopanel.add(samplingorder);
		radiopanel.add(logpanel);
		radiopanel.add(otheroptions);


		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));
		contentPane.add(parameterpanel);
		contentPane.add(radiopanel);
		contentPane.add(filePanel);

		ImageIcon icon = new ImageIcon("small_logo.jpg");
		if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) //image ok
			this.setIconImage(icon.getImage());
		this.setTitle("Sampling Settings");
		this.setSize(350, 300);
		this.setVisible(true);

	}

	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();	
		if( source == fileButton ){
			int value = fileChooser.showDialog(Settings.this, "Select");
			if (value == JFileChooser.APPROVE_OPTION){
				File file = fileChooser.getSelectedFile();
				String logfilename = file.getPath();
				fileField.setText(logfilename);
				evidence.setLogfilename(logfilename);
			}
		}
		else if( source == textcptparents ){
			try{
				Integer tempint = new Integer(textcptparents.getText());
				evidence.setCPTParents(tempint.intValue());  
			}
			catch(NumberFormatException exception){
			}
		}
		else if( source == textnumsubsamples_minmax ){
			try{
				Integer tempint = new Integer(textnumsubsamples_minmax.getText());
				evidence.setNumSubsamples_minmax(tempint.intValue());  
			}
			catch(NumberFormatException exception){
			}
		}
		else if( source == textnumsubsamples_adapt ){
			try{
				Integer tempint = new Integer(textnumsubsamples_adapt.getText());
				evidence.setNumSubsamples_adapt(tempint.intValue());  
			}
			catch(NumberFormatException exception){
			}
		}
		// 	else if( source == textdoubleparam ){
//		try{
//		Double tempdouble = new Double(textdoubleparam.getText());
//		evidence.setDummyDouble(tempdouble.doubleValue());  
//		}
//		catch(NumberFormatException exception){
//		}
//		}
	}

	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if( source == forward){
			if (e.getStateChange() == ItemEvent.SELECTED){
				evidence.setSampleOrdMode(InferenceModule.OPTION_SAMPLEORD_FORWARD);
			}
		}
		else if( source == ripple ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				evidence.setSampleOrdMode(InferenceModule.OPTION_SAMPLEORD_RIPPLE);
				evidence.setAdaptiveMode(InferenceModule.OPTION_NOT_SAMPLE_ADAPTIVE);
				adaptive.setSelected(false);
			}
		}
		else if( source == tracenone ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				evidence.setSampleLogMode(2,false);
				evidence.setSampleLogMode(3,false);
			}
		}
		else if( source == traceshort ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				evidence.setSampleLogMode(2,true);
				evidence.setSampleLogMode(3,false);
			}
		}
		else if( source == tracelong ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				evidence.setSampleLogMode(2,false);
				evidence.setSampleLogMode(3,true);
			}
		}
		else if( source == adaptive ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				evidence.setAdaptiveMode(InferenceModule.OPTION_SAMPLE_ADAPTIVE);
				evidence.setSampleOrdMode(InferenceModule.OPTION_SAMPLEORD_FORWARD);
				forward.setSelected(true);
			}
			else{
				evidence.setAdaptiveMode(InferenceModule.OPTION_NOT_SAMPLE_ADAPTIVE);
			}
		}
		else if( source == logsampleord ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				evidence.setSampleLogMode(0,true);
			}
			else{
				evidence.setSampleLogMode(0,false);
			}
		}
		else if( source == logevidence ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				evidence.setSampleLogMode(1,true);
			}
			else{
				evidence.setSampleLogMode(1,false);
			}
		}
		else if( source == lognetwstats ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				evidence.setSampleLogMode(4,true);
			}
			else{
				evidence.setSampleLogMode(4,false);
			}
		}

	}



}
