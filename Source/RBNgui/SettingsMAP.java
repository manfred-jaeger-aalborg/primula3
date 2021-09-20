package RBNgui;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SettingsMAP extends JFrame implements ActionListener, ItemListener, KeyListener{

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
	private JLabel linelikelihoodlabel = new JLabel("Likelihood threshold (linesearch)");
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
	private JTextField linelikelihoodtext = new JTextField(5);

	/**
	 * @uml.property  name="maxfailstext"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField maxfailstext = new JTextField(5);

	
	/**
	 * @uml.property  name="samplesizepanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel samplesizepanel = new JPanel(new FlowLayout());
	/**
	 * @uml.property  name="restartspanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel restartspanel = new JPanel(new FlowLayout());
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
	private JPanel linelikelihoodpanel = new JPanel(new FlowLayout());

	/**
	 * @uml.property  name="maxfailspanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel maxfailspanel = new JPanel(new FlowLayout());
	
	/**
	 * @uml.property  name="generaloptions"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel generaloptions = new JPanel(new GridLayout(3,1));
	/**
	 * @uml.property  name="incompleteoptions"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel incompleteoptions = new JPanel(new GridLayout(4,1));
	/**
	 * @uml.property  name="terminateoptions"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel terminateoptions = new JPanel(new GridLayout(3,1));
	
	/**
	 * @uml.property  name="verbosecheckbox"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JCheckBox verbosecheckbox = new JCheckBox("Verbose");


	/**
	 * @uml.property  name="infmodule"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="settingswindow:RBNgui.infmodule"
	 */
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
		
		generaloptions.add(restartspanel);
		generaloptions.add(verbosecheckbox);
		generaloptions.setBorder(BorderFactory.createTitledBorder("General"));
		
		terminateoptions.add(maxiterationspanel);
		terminateoptions.add(linedistancepanel);
		terminateoptions.add(linelikelihoodpanel);
		terminateoptions.setBorder(BorderFactory.createTitledBorder("Termination"));
		
		incompleteoptions.add(samplesizepanel);
		incompleteoptions.add(gibbsroundspanel);
		incompleteoptions.add(maxfailspanel);
		incompleteoptions.setBorder(BorderFactory.createTitledBorder("Gibbs Sampling"));
		
		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));

		
		contentPane.add(generaloptions);
		contentPane.add(incompleteoptions);
		
		samplesizetext.setText(""+infmodule.getNumChains());
		samplesizetext.addKeyListener(this);

		gibbsroundstext.setText(""+infmodule.getWindowSize());
		gibbsroundstext.addKeyListener(this);


		maxfailstext.setText(""+infmodule.getMaxFails());
		maxfailstext.addKeyListener(this);
		
		restartstext.setText(""+infmodule.getMAPRestarts());
		restartstext.addKeyListener(this);
		
		verbosecheckbox.setSelected(false);
		verbosecheckbox.addItemListener(this);

		ImageIcon icon = new ImageIcon("small_logo.jpg");
		if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) //image ok
			this.setIconImage(icon.getImage());
		this.setTitle("MAP Settings");
		this.setSize(350, 500);
		this.setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();	


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
				Integer tempint = new Integer(samplesizetext.getText());
				infmodule.setLearnSampleSize(tempint.intValue());  
			}
			catch(NumberFormatException exception){
			}
		}

		else if ( source == gibbsroundstext ){
			try{
				Integer tempint = new Integer(gibbsroundstext.getText());
				infmodule.setWindowSize(tempint.intValue());  
			}
			catch(NumberFormatException exception){
			}
		}		

		else if ( source == maxfailstext ){
			try{
				Integer tempint = new Integer(maxfailstext.getText());
				infmodule.setMaxFails(tempint.intValue());  
			}
			catch(NumberFormatException exception){
			}
		}
		
		else if ( source == restartstext ){
			try{
				Integer tempint = new Integer(restartstext.getText());
				System.out.println("Settings: " + tempint);
				infmodule.setNumRestarts(tempint.intValue());  
			}
			catch(NumberFormatException exception){
			}
		}
		

	}
}


