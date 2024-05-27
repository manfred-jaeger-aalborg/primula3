/* ArbitraryPanel.java 
 * 
 * Copyright (C) 2005 Aalborg University
 *
 * contact:
 * jaeger@cs.aau.dk    www.cs.aau.dk/~jaeger/Primula.html
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

import RBNpackage.*;

public class ArbitraryPanel extends JPanel implements MouseListener, KeyListener {//, FocusListener

	/**
	 * @uml.property  name="arbitraryLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel arbitraryLabel        = new JLabel("Higher arities");
	/**
	 * @uml.property  name="arbitraryList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList arbitraryList          = new JList();
	/**
	 * @uml.property  name="listModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.NumRel"
	 */
	private DefaultListModel listModel   = new DefaultListModel();
	/**
	 * @uml.property  name="addArbitraryLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel addArbitraryLabel     = new JLabel("Add:");
	/**
	 * @uml.property  name="addArbitraryField"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField addArbitraryField = new JTextField(10);
	/**
	 * @uml.property  name="addArityLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel addArityLabel         = new JLabel("Arity:");
	/**
	 * @uml.property  name="addArityField"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField addArityField     = new JTextField(10);
	/**
	 * @uml.property  name="labelPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel labelPanel            = new JPanel(new GridLayout(2,1));
	/**
	 * @uml.property  name="fieldPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel fieldPanel            = new JPanel(new GridLayout(2,1));
	/**
	 * @uml.property  name="addAndArity"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel addAndArity           = new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="scrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane scrollList       = new JScrollPane();
	/**
	 * @uml.property  name="arity"
	 */
	private int arity   = 0;
	/**
	 * @uml.property  name="name"
	 */
	private String name = "";

	/**
	 * @uml.property  name="mode"
	 */
	private int mode;
	/**
	 * @uml.property  name="aDDNODE"
	 */
	private final int ADDNODE        = 1;
	/**
	 * @uml.property  name="mOVENODE"
	 */
	private final int MOVENODE       = 5;
	/**
	 * @uml.property  name="dELETENODE"
	 */
	private final int DELETENODE     = 2;
	/**
	 * @uml.property  name="aDDTUPLE"
	 */
	private final int ADDTUPLE       = 3;
	/**
	 * @uml.property  name="dELETERELATION"
	 */
	private final int DELETERELATION = 4;

	/**
	 * @uml.property  name="bavaria"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="arbitraryPanel:RBNgui.Bavaria"
	 */
	private Bavaria bavaria;

	public ArbitraryPanel(Bavaria b){

		bavaria = b;

		arbitraryList.setBackground(Color.white);
		addArbitraryField.setBackground(Color.white);
		addArityField.setBackground(Color.white);

		labelPanel.add(addArbitraryLabel);
		labelPanel.add(addArityLabel);

		fieldPanel.add(addArbitraryField);
		fieldPanel.add(addArityField);

		addAndArity.add(labelPanel, BorderLayout.WEST);
		addAndArity.add(fieldPanel, BorderLayout.CENTER);

		arbitraryList.setModel(listModel);
		arbitraryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		scrollList.getViewport().add(arbitraryList);

		this.setLayout(new BorderLayout());
		this.add(arbitraryLabel, BorderLayout.NORTH);
		this.add(scrollList , BorderLayout.CENTER);
		this.add(addAndArity, BorderLayout.SOUTH);


		arbitraryList.addMouseListener( this );


		addArbitraryField.addKeyListener( this );


		//addArbitraryField.addFocusListener( this );

		addArityField.addKeyListener( this );

		//addArityField.addFocusListener( this );
	}

	public void focusGained(FocusEvent e){
		Object source = e.getSource();
	}

	public void focusLost(FocusEvent e) {
		Object source = e.getSource();
		if( source == addArbitraryField ){
			name = addArbitraryField.getText();
			if (arity > 2 && (name.trim()).length() > 0){
				if(bavaria.getboolNumSelectionPanel().getboolboxselection()){
					BoolRel r = new BoolRel(name+"_"+arity, arity);
					bavaria.addRelation(r);
					listModel.addElement(r);
					arbitraryList.ensureIndexIsVisible(listModel.size()-1);
					addArbitraryField.setText("");
					addArityField.setText("");
					name = "";
					arity = 0;
				}
				else{
					NumRel r = new NumRel(name+"_"+arity, arity);
					bavaria.addRelation(r);
					listModel.addElement(r);
					arbitraryList.ensureIndexIsVisible(listModel.size()-1);
					addArbitraryField.setText("");
					addArityField.setText("");
					name = "";
					arity = 0;
				}
			}
		}
		else if( source == addArityField){
			try {
				arity = (new Integer(addArityField.getText())).intValue();
			} catch (Exception ex){
				arity = 0;
			}
			if (arity > 2 && (name.trim()).length() > 0){
				if(bavaria.getboolNumSelectionPanel().getboolboxselection()){
					BoolRel r = new BoolRel(name+"_"+arity, arity);
					bavaria.addRelation(r);
					listModel.addElement(r);
					arbitraryList.ensureIndexIsVisible(listModel.size()-1);
					addArbitraryField.setText("");
					addArityField.setText("");
					name = "";
					arity = 0;
				}
				else{
					NumRel r = new NumRel(name+"_"+arity, arity);
					bavaria.addRelation(r);
					listModel.addElement(r);
					arbitraryList.ensureIndexIsVisible(listModel.size()-1);
					addArbitraryField.setText("");
					addArityField.setText("");
					name = "";
					arity = 0;
				}
			}
		}

	}

	public void keyPressed(KeyEvent e){ 
		JFrame frame = new  JFrame();
		Object source = e.getSource();
		if( source == addArbitraryField ){
			char c = e.getKeyChar();
			if(c == KeyEvent.VK_ENTER){
				name = addArbitraryField.getText();
				if(bavaria.getStruc().getmydata().relExist( name)){
					System.out.print("Relation exists");
					JOptionPane.showMessageDialog(frame,
							"Relation "+ addArbitraryField.getText() + " already exists.",
							"Warning",
							JOptionPane.WARNING_MESSAGE);
				}
				else{			
					if (arity > 2 && (name.trim()).length() > 0){
						if(bavaria.getboolNumSelectionPanel().getboolboxselection()){					
							BoolRel r = new BoolRel(name, arity);
							bavaria.addRelation(r);
							listModel.addElement(r);
							arbitraryList.ensureIndexIsVisible(listModel.size()-1);
							addArbitraryField.setText("");  
							addArityField.setText("");
							name = "";
							arity = 0;
						}
						else{
							NumRel r = new NumRel(name, arity);
							bavaria.addRelation(r);
							listModel.addElement(r);
							arbitraryList.ensureIndexIsVisible(listModel.size()-1);
							addArbitraryField.setText("");  
							addArityField.setText("");
							name = "";
							arity = 0;
						}	

					}

				}

			}
		}
		else if( source == addArityField ){
			char c = e.getKeyChar();
			if(c == KeyEvent.VK_ENTER){
				try {
					arity = (new Integer(addArityField.getText())).intValue();
				} catch (Exception ex){
					arity = 0;
				}

				if (arity > 2 && (name.trim()).length() > 0){
					if(bavaria.getboolNumSelectionPanel().getboolboxselection()){
						BoolRel r = new BoolRel(name, arity);
						bavaria.addRelation(r);
						listModel.addElement(r);
						arbitraryList.ensureIndexIsVisible(listModel.size()-1);
						addArbitraryField.setText("");
						addArityField.setText("");
						name = "";
						arity = 0;
						addArbitraryField.requestFocus();
					}
					else{
						NumRel r = new NumRel(name, arity);
						bavaria.addRelation(r);
						listModel.addElement(r);
						arbitraryList.ensureIndexIsVisible(listModel.size()-1);
						addArbitraryField.setText("");
						addArityField.setText("");
						name = "";
						arity = 0;
						addArbitraryField.requestFocus();
					}
				}

			}
		}
	}

	public void keyReleased(KeyEvent e){ 
		Object source = e.getSource();
		if( source == addArityField ){
			
				try {
					arity = (new Integer(addArityField.getText())).intValue();
				} catch (Exception ex){
					arity = 0;
				}

		}
	}

	public void keyTyped(KeyEvent e){

		
	}


	public void mouseClicked(MouseEvent e){
	}
	public void mouseEntered(MouseEvent e){
	}
	public void mouseExited(MouseEvent e){
	}
	public void mousePressed(MouseEvent e){ 
		Object source = e.getSource();		
		if( source == arbitraryList ){
			int index;
			index = arbitraryList.locationToIndex(e.getPoint());
			if (mode == ADDNODE || mode == MOVENODE || mode == DELETENODE){
				arbitraryList.clearSelection();
				addArbitraryLabel.requestFocus();
			}
			if (mode == ADDTUPLE){
				if (index >= 0){
					bavaria.clearSelections(3);
					Object obj = listModel.elementAt(index);
					if(obj instanceof BoolRel){
						BoolRel r = (BoolRel)listModel.elementAt(index);
						bavaria.addTuple(r);
					}
					else{
						NumRel r = (NumRel)listModel.elementAt(index);
						bavaria.addTuple(r);
					}
				}
				else
					bavaria.addTuple(new BoolRel());  //so that arity equals 0
			}
			if (mode == DELETERELATION){
				if (index >= 0){
					Object obj = listModel.elementAt(index);

					if(obj instanceof BoolRel)
					{
						String relName = ((BoolRel)listModel.elementAt(index)).name.name;
						int result = JOptionPane.showConfirmDialog(bavaria,
								"Do you really want to delete the whole relation " + relName + "?",
								"Delete Relation", JOptionPane.YES_NO_OPTION);
						if(result == JOptionPane.YES_OPTION){
							BoolRel r = (BoolRel)listModel.remove(index);
							bavaria.deleteRelation(r);
						}
						else if(result == JOptionPane.NO_OPTION){
							arbitraryList.clearSelection();
							addArityLabel.requestFocus();
						}
					}
					else{
						String relName = ((NumRel)listModel.elementAt(index)).name.name;
						int result = JOptionPane.showConfirmDialog(bavaria,
								"Do you really want to delete the whole relation " + relName + "?",
								"Delete Relation", JOptionPane.YES_NO_OPTION);
						if(result == JOptionPane.YES_OPTION){
							NumRel r = (NumRel)listModel.remove(index);
							bavaria.deleteRelation(r);
						}
						else if(result == JOptionPane.NO_OPTION){
							arbitraryList.clearSelection();
							addArityLabel.requestFocus();
						}
					}


				}
			}
		}
	}
	public void mouseReleased(MouseEvent e){
		Object source = e.getSource();

		if( source == arbitraryList ){
			int index = arbitraryList.locationToIndex(e.getPoint());

			if (mode == ADDNODE || mode == MOVENODE || mode == DELETENODE || mode == DELETERELATION){
				arbitraryList.clearSelection();
				addArbitraryLabel.requestFocus();
			}
			if (mode == ADDTUPLE && index >= 0){
				arbitraryList.setSelectedIndex(index);
				arbitraryList.ensureIndexIsVisible(index);
			}
		}
	} 



	public void setArbitraryNames(Vector<BoolRel> boolarbitraryrels, Vector<NumRel> numarbitraryrels){
		for(int i=0; i<boolarbitraryrels.size(); ++i){
			listModel.addElement(boolarbitraryrels.elementAt(i));
		}
		for(int i=0; i<numarbitraryrels.size(); ++i){
			listModel.addElement(numarbitraryrels.elementAt(i));
		}
	}


	/**
	 * @param mode
	 * @uml.property  name="mode"
	 */
	public void setMode(int mode){
		this.mode = mode;
	}

	public void clearSelections(){
		arbitraryList.clearSelection();
	}

	public void empty(){
		listModel.clear();
	}


}