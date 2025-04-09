package RBNgui;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

public class SettingsMAP extends JFrame implements ActionListener, ItemListener, KeyListener{

	private JLabel samplesizelabel = new JLabel("Gibbs chains");
	private JLabel restartslabel = new JLabel("Restarts (-1 = until stopped)");
	private JLabel gibbsroundslabel = new JLabel("Gibbs Window Size (>=2)");
	private JLabel maxiterationslabel = new JLabel("Max. iterations gradient search");
	private JLabel linedistancelabel = new JLabel("Distance threshold");
	private JLabel linelikelihoodlabel = new JLabel("Likelihood threshold (linesearch)");
	private JLabel maxfailslabel = new JLabel("Max. fails (Sample missing)");
	private JLabel lookaheadLabel = new JLabel("Lookahead");

	private JTextField samplesizetext = new JTextField(5);
	private JTextField restartstext = new JTextField(5);
	private JTextField gibbsroundstext = new JTextField(5);
	private JTextField maxiterationstext = new JTextField(5);
	private JTextField linedistancetext = new JTextField(5);
	private JTextField linelikelihoodtext = new JTextField(5);
	private JTextField maxfailstext = new JTextField(5);
	private JTextField lookaheadtext = new JTextField(5);

	private JPanel samplesizepanel = new JPanel(new FlowLayout());
	private JPanel restartspanel = new JPanel(new FlowLayout());
	private JPanel gibbsroundspanel = new JPanel(new FlowLayout());
	private JPanel maxiterationspanel = new JPanel(new FlowLayout());
	private JPanel linedistancepanel = new JPanel(new FlowLayout());
	private JPanel linelikelihoodpanel = new JPanel(new FlowLayout());
	private JPanel maxfailspanel = new JPanel(new FlowLayout());
	private JPanel lookaheadpanel = new JPanel(new FlowLayout());

	private JLabel sampleSizeScoringLabel = new JLabel("Size for scoring (<=chain*window)");
	private JTextField sampleSizeScoringText = new JTextField(5);
	private JPanel sampleSizeScoringPanel = new JPanel(new FlowLayout());

	private JLabel firstBatchSizeLabel = new JLabel("First batch size");
	private JTextField firstBatchSizeText = new JTextField(5);
	private JPanel firstBatchSizePanel = new JPanel(new FlowLayout());

	private JPanel generaloptions = new JPanel(new GridLayout(3,1));
	private JPanel incompleteoptions = new JPanel(new GridLayout(4,1));
	private JPanel terminateoptions = new JPanel(new GridLayout(3,1));

	private JCheckBox verbosecheckbox = new JCheckBox("Verbose");

	private JRadioButton algorithm1Radio = new JRadioButton("Algorithm 1 (with lookahead)");
	private JRadioButton algorithm2Radio = new JRadioButton("Algorithm 2");
	private ButtonGroup algorithmGroup = new ButtonGroup();
	private JPanel algorithmPanel = new JPanel(new FlowLayout());

	private InferenceModule infmodule;

	public SettingsMAP(InferenceModule im){

		infmodule = im;
		this.addWindowListener(
				new WindowAdapter(){
					public void windowClosing(WindowEvent e){
						infmodule.setSettingsOpen(false);
						dispose();
					}
				}
		);
		samplesizepanel.add(samplesizelabel);
		samplesizepanel.add(samplesizetext);

		restartspanel.add(restartslabel);
		restartspanel.add(restartstext);

		gibbsroundspanel.add(gibbsroundslabel);
		gibbsroundspanel.add(gibbsroundstext);

		maxiterationspanel.add(maxiterationslabel);
		maxiterationspanel.add(maxiterationstext);

		linedistancepanel.add(linedistancelabel);
		linedistancepanel.add(linedistancetext);

		linelikelihoodpanel.add(linelikelihoodlabel);
		linelikelihoodpanel.add(linelikelihoodtext);

		maxfailspanel.add(maxfailslabel);
		maxfailspanel.add(maxfailstext);

		sampleSizeScoringPanel.add(sampleSizeScoringLabel);
		sampleSizeScoringPanel.add(sampleSizeScoringText);

		firstBatchSizePanel.add(firstBatchSizeLabel);
		firstBatchSizePanel.add(firstBatchSizeText);

		lookaheadpanel.add(lookaheadLabel);
		lookaheadpanel.add(lookaheadtext);

		algorithmGroup.add(algorithm1Radio);
		algorithmGroup.add(algorithm2Radio);
		algorithm1Radio.setSelected(true);
		algorithmPanel.add(algorithm1Radio);
		algorithmPanel.add(algorithm2Radio);
		algorithm1Radio.addActionListener(this);
		algorithm2Radio.addActionListener(this);

		generaloptions.add(restartspanel);
		generaloptions.add(verbosecheckbox);
		generaloptions.add(algorithmPanel);
		generaloptions.add(firstBatchSizePanel);
		generaloptions.add(lookaheadpanel);

		generaloptions.setBorder(BorderFactory.createTitledBorder("General"));

		incompleteoptions.add(samplesizepanel);
		incompleteoptions.add(gibbsroundspanel);
		incompleteoptions.add(maxfailspanel);
		incompleteoptions.add(sampleSizeScoringPanel);
		incompleteoptions.setBorder(BorderFactory.createTitledBorder("Gibbs Sampling"));

		terminateoptions.add(maxiterationspanel);
		terminateoptions.add(linedistancepanel);
		terminateoptions.add(linelikelihoodpanel);
		terminateoptions.setBorder(BorderFactory.createTitledBorder("Termination"));

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));

		contentPane.add(generaloptions);
		contentPane.add(incompleteoptions);
		contentPane.add(terminateoptions);

		samplesizetext.setText(""+infmodule.getNumChains());
		samplesizetext.addKeyListener(this);

		gibbsroundstext.setText(""+infmodule.getWindowSize());
		gibbsroundstext.addKeyListener(this);

		maxfailstext.setText(""+infmodule.getMaxFails());
		maxfailstext.addKeyListener(this);

		restartstext.setText(""+infmodule.getMAPRestarts());
		restartstext.addKeyListener(this);

		lookaheadtext.setText(""+infmodule.getLookaheadSearch());
		lookaheadtext.addKeyListener(this);

		sampleSizeScoringText.setText(""+infmodule.getSampleSizeScoring());
		sampleSizeScoringText.addKeyListener(this);
		firstBatchSizeText.setText(""+infmodule.getBatchSearchSize());
		firstBatchSizeText.addKeyListener(this);

		verbosecheckbox.setSelected(infmodule.getVerbose());
		verbosecheckbox.addItemListener(this);

		firstBatchSizeText.setEnabled(false);

		ImageIcon icon = new ImageIcon("small_logo.jpg");
		if (icon.getImageLoadStatus() == MediaTracker.COMPLETE)
			this.setIconImage(icon.getImage());
		this.setTitle("MAP Settings");
		this.setSize(550, 700);
		this.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if(source == algorithm1Radio){
			infmodule.setMapSearchAlg(2);
			firstBatchSizeText.setEnabled(false);
		}
		else if(source == algorithm2Radio){
			infmodule.setMapSearchAlg(3);
			firstBatchSizeText.setEnabled(true);
			lookaheadtext.setEnabled(false);
		}
	}


	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if ( source == verbosecheckbox ){
			if (e.getStateChange() == ItemEvent.SELECTED){
				infmodule.setVerbose(true);
			}
			else
				infmodule.setVerbose(false);
		}
	}

	public void keyPressed(KeyEvent e){
	}

	public void keyTyped(KeyEvent e){
	}

	public void keyReleased(KeyEvent e){
		Object source = e.getSource();
		if( source == samplesizetext ){
			try{
				Integer tempint = Integer.valueOf(samplesizetext.getText());
				infmodule.setLearnSampleSize(tempint.intValue());
			}
			catch(NumberFormatException exception){
			}
		}
		else if ( source == gibbsroundstext ){
			try{
				Integer tempint = Integer.valueOf(gibbsroundstext.getText());
				infmodule.setWindowSize(tempint.intValue());
			}
			catch(NumberFormatException exception){
			}
		}
		else if ( source == maxfailstext ){
			try{
				Integer tempint = Integer.valueOf(maxfailstext.getText());
				infmodule.setMaxFails(tempint.intValue());
			}
			catch(NumberFormatException exception){
			}
		}
		else if ( source == restartstext ){
			try{
				Integer tempint = Integer.valueOf(restartstext.getText());
				System.out.println("Settings: " + tempint);
				infmodule.setNumRestarts(tempint.intValue());
			}
			catch(NumberFormatException exception){
			}
		}
		else if(source == sampleSizeScoringText){
			try{
				Integer tempint = Integer.valueOf(sampleSizeScoringText.getText());
				infmodule.setSampleSizeScoring(tempint.intValue());
			}
			catch(NumberFormatException exception){
			}
		}
		else if(source == firstBatchSizeText){
			try{
				Integer tempint = Integer.valueOf(firstBatchSizeText.getText());
				infmodule.setBatchSearchSize(tempint.intValue());
			}
			catch(NumberFormatException exception){
			}
		}
		else if(source == lookaheadtext){
			try{
				Integer tempint = Integer.valueOf(lookaheadtext.getText());
				infmodule.setLookaheadSearch(tempint.intValue());
			}
			catch(NumberFormatException exception){
			}
		}
	}
}
