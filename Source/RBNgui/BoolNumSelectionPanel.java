package RBNgui;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.*;

public  class BoolNumSelectionPanel extends JPanel implements KeyListener{
	private static final long serialVersionUID = 1L;
	/**
	 * @uml.property  name="bngroup"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private ButtonGroup  bngroup 	     = new ButtonGroup();
	/**
	 * @uml.property  name="boolbox"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButton boolbox 	     = new JRadioButton("Bool",true);
	/**
	 * @uml.property  name="numbox"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JRadioButton numbox 	     = new JRadioButton("Numeric",false);
	/**
	 * @uml.property  name="valuefield"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField 	 valuefield 	 = new JTextField(10);

	/**
	 * @uml.property  name="label"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel  label = new JLabel("Value");
	/**
	 * @uml.property  name="typeselectlabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel  typeselectlabel = new JLabel("Type Selection");
	/**
	 * @uml.property  name="boolnumselect"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel  boolnumselect   = new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="valuepanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel  valuepanel      = new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="bavaria"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="boolNumSelectionPanel:RBNgui.Bavaria"
	 */
	private Bavaria bavaria;

	public BoolNumSelectionPanel(Bavaria b){
		bavaria = b;
		bngroup.add(boolbox);
		bngroup.add(numbox);
		boolnumselect.add(boolbox,BorderLayout.WEST);
		boolnumselect.add(numbox,BorderLayout.EAST);
		valuepanel.add(new JLabel("Numeric Value"), BorderLayout.NORTH);
		//valuepanel.add(label,BorderLayout.WEST);
		valuefield.setText("1.0");
		valuepanel.add(valuefield,BorderLayout.SOUTH);

		this.setLayout(new GridLayout(3,1));		
		this.add(typeselectlabel);
		this.add(boolnumselect);
		this.add(valuepanel);

	}

	public boolean getboolboxselection(){
		return boolbox.isSelected();
	}
	public String getvalue(){
		return valuefield.getText();
	}
	public BoolNumSelectionPanel getBoolNumSelectionPanel(){
		return this;

	}
	public void setvaluefieldfocus(){
		valuefield.requestFocus();

	}
	public void setvaluefield(String s){
		valuefield.setText(s);

	}
	public JTextField getvaluefield(){
		return valuefield;
	}
	public String getvaluefieldvalue(){
		return valuefield.getText();
	}


	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}