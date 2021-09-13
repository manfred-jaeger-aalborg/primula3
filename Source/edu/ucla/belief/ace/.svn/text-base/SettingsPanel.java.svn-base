package edu.ucla.belief.ace;

import edu.ucla.belief.ace.AceInterfaceForPrimula.EncodingMethod;
import edu.ucla.belief.ace.AceInterfaceForPrimula.DtreeMethod;
import edu.ucla.belief.ace.Settings;

import java.awt.*;
import javax.swing.*;
import java.text.*;
import java.awt.event.*;
import java.io.File;

/** @author keith cascio
	@since 20060511 */
public class SettingsPanel extends JPanel implements ActionListener, Settings.ACESettingsListener
{
	public SettingsPanel(){
		super( new GridBagLayout() );
		this.init();
	}

	public static final double DOUBLE_MINUTES_PER_DAY = 1440;
	public static final String STR_TITLE              = Settings.STR_ACE_DISPLAY_NAME + " SettingsSampling";
	public static final String STR_QUERY_BASED        = "query based";

	public void commit( Settings acesettings ){
		acesettings.setCompileWithEvidence(             myCBCompileWithEvidence.isSelected() );
		acesettings.setEncodingMethod( (EncodingMethod) myCBEncoding.getSelectedItem() );
		acesettings.setDtreeMethod(       (DtreeMethod) myCBDtree.getSelectedItem() );
		acesettings.setTimeoutCompileMinutes( ((Number) myTFTimeout.getValue()).longValue() );
		acesettings.setCountPartitions(       ((Number) myTFPartitions.getValue()).intValue() );
		acesettings.setOutputDirectory(          (File) myTFOutputDirectory.getValue() );
	}

	public void assume( Settings acesettings ){
		myCBCompileWithEvidence.setSelected( acesettings.isCompileWithEvidence() );
		myTFTimeout.setValue(                acesettings.getTimeoutCompileMinutes() );
		myCBEncoding.setSelectedItem(        acesettings.getEncodingMethod() );
		myCBDtree.setSelectedItem(           acesettings.getDtreeMethod() );
		myTFPartitions.setValue(             acesettings.getCountPartitions() );
		myTFOutputDirectory.setValue(        acesettings.getOutputDirectory() );
		resetEnabledState();
	}

	/** show a modal dialog.
		note: reacts to external settings changes that happen while the panel is visible */
	public void show( Component parent, Settings acesettings ){
		this.assume( acesettings );
		JComponent msg = this;
		acesettings.addListener( (Settings.ACESettingsListener) this );
		int result = JOptionPane.showConfirmDialog( parent, msg, STR_TITLE, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );
		acesettings.removeListener( (Settings.ACESettingsListener) this );
		if( result == JOptionPane.OK_OPTION ) this.commit( acesettings );
	}

	private void resetEnabledState(){
		//boolean hypergraph = (myCBDtree != null) && (myCBDtree.getSelectedItem() == DtreeMethod.hypergraph);
		boolean hypergraph = (myCBDtree != null) && (myCBDtree.getSelectedItem() == DtreeMethod.HYPERGRAPH);
		myLabelPartitions.setEnabled( hypergraph );
		myTFPartitions.setEnabled(    hypergraph );
	}

	public void actionPerformed( ActionEvent event ){
		SettingsPanel.this.resetEnabledState();
	}

	/** interface SettingsSampling.ACESettingsListener */
	public void aceCompileSettingChanged( Settings settings ){
		SettingsPanel.this.assume( settings );
	}

	private void init(){
		GridBagConstraints c = new GridBagConstraints();
		c.anchor    = GridBagConstraints.WEST;
		c.fill      = GridBagConstraints.HORIZONTAL;

		/*c.weightx   = 0;
		c.gridwidth = 1;
		this.add( Box.createHorizontalGlue(), c );
		this.add( Box.createHorizontalGlue(), c );
		this.add( Box.createHorizontalGlue(), c );
		c.weightx   = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( Box.createHorizontalStrut( 512 ), c );*/

		c.weightx   = 0;
		c.gridwidth = 1;
		this.add( Box.createVerticalStrut( 32 ), c );
		this.add( new JLabel( STR_QUERY_BASED + "?" ), c );
		this.add( Box.createHorizontalStrut( 32 ), c );
		c.weightx   = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myCBCompileWithEvidence = new JCheckBox(), c );

		c.weightx   = 0;
		c.gridwidth = 1;
		this.add( Box.createVerticalStrut( 32 ), c );
		this.add( new JLabel( "compile timeout (minutes)" ), c );
		this.add( Box.createHorizontalGlue(), c );
		c.weightx   = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myTFTimeout = new JFormattedTextField( new DecimalFormat( "####################0" ) ), c );

		JFormattedTextField.AbstractFormatter formatter = new JFormattedTextField.AbstractFormatter(){
			public Object stringToValue( String text ){
				if( text == null ) return null;
				return new File( text );
			}

			public String valueToString( Object value ){
				if( value == null ) return "!null!";
				try{
					return ((File)value).getCanonicalFile().getAbsolutePath();
				}catch( Exception exception ){
					return "!error!";
				}
			}
		};
		c.weightx   = 0;
		c.gridwidth = 1;
		this.add( Box.createVerticalStrut( 32 ), c );
		this.add( new JLabel( "output directory" ), c );
		this.add( Box.createHorizontalGlue(), c );
		c.weightx   = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myTFOutputDirectory = new JFormattedTextField( formatter ), c );
		myTFOutputDirectory.setInputVerifier( new InputVerifier(){
			public boolean verify( JComponent input ){
				if( input != myTFOutputDirectory ) throw new IllegalArgumentException();

				String text = null, errmsg = null;
				try{
					File candidate = new File( text = myTFOutputDirectory.getText() );
					if(      !candidate.exists() ){
						errmsg = text + " does not exist. Please enter an existing directory.";
					}
					else if( !candidate.isDirectory() ){
						errmsg = text + " is not a directory. Please enter a directory.";
					}
					else return true;
				}catch( Exception exception ){
					errmsg = exception.toString();
				}
				JOptionPane.showMessageDialog( SettingsPanel.this, errmsg, "error: invalid output directory setting", JOptionPane.ERROR_MESSAGE );
				return false;
			}
		} );

		c.weightx   = 0;
		c.gridwidth = 1;
		this.add( Box.createVerticalStrut( 32 ), c );
		this.add( new JLabel( "encoding method" ), c );
		this.add( Box.createHorizontalGlue(), c );
		c.weightx   = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myCBEncoding = new JComboBox( EncodingMethod.values() ), c );

		c.weightx   = 0;
		c.gridwidth = 1;
		this.add( Box.createVerticalStrut( 32 ), c );
		this.add( new JLabel( "dtree method" ), c );
		this.add( Box.createHorizontalGlue(), c );
		c.weightx   = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myCBDtree = new JComboBox( DtreeMethod.values() ), c );

		c.weightx   = 0;
		c.gridwidth = 1;
		this.add( Box.createVerticalStrut( 32 ), c );
		this.add( myLabelPartitions = new JLabel( "        number of partitions " + Settings.STR_DESCRIBE_PARTITIONS_INTERVAL + " (hypergraph dtree method)" ), c );
		this.add( Box.createHorizontalGlue(), c );
		c.weightx   = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myTFPartitions = new JFormattedTextField( new DecimalFormat( "##0" ) ), c );

		/*
		JPanel dtree = new JPanel( new GridBagLayout() );
		GridBagConstraints cDtree = new GridBagConstraints();

		cDtree.weightx   = 0;
		cDtree.gridwidth = 1;
		dtree.add( new JLabel( "dtree method" ),   cDtree );
		dtree.add( Box.createHorizontalStrut( 3205 ), cDtree );
		cDtree.weightx   = 1;
		cDtree.gridwidth = GridBagConstraints.REMAINDER;
		dtree.add( myCBDtree = new JComboBox( DtreeMethod.values() ), cDtree );

		c.weightx   = 0;
		c.gridwidth = 1;
		this.add( Box.createVerticalStrut( 32 ), c );
		c.weightx   = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( dtree, c );
		*/

		myCBCompileWithEvidence.addActionListener( SettingsPanel.this );
		myTFTimeout.addActionListener(             SettingsPanel.this );
		myCBEncoding.addActionListener(            SettingsPanel.this );
		myCBDtree.addActionListener(               SettingsPanel.this );
		myTFPartitions.addActionListener(          SettingsPanel.this );
		myTFOutputDirectory.addActionListener(     SettingsPanel.this );
	}

	private JCheckBox           myCBCompileWithEvidence;
	private JFormattedTextField myTFTimeout;
	private JComboBox           myCBEncoding;
	private JComboBox           myCBDtree;
	private JFormattedTextField myTFPartitions;
	private JLabel              myLabelPartitions;
	private JFormattedTextField myTFOutputDirectory;
}
