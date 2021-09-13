/*
 * BinaryPanel.java 
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

import RBNpackage.*;

public class BinaryPanel extends JPanel implements MouseListener, KeyListener{

	/**
	 * @uml.property  name="binaryLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel binaryLabel         = new JLabel("Binary relations");
	/**
	 * @uml.property  name="binaryList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList binaryList           = new JList();
	/**
	 * @uml.property  name="listModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.NumRel"
	 */
	private DefaultListModel listModel = new DefaultListModel();
	/**
	 * @uml.property  name="addBinLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel addBinLabel         = new JLabel("Add:");
	/**
	 * @uml.property  name="addBinField"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField addBinField     = new JTextField(10);
	/**
	 * @uml.property  name="addPanel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JPanel addPanel            = new JPanel(new BorderLayout());
	/**
	 * @uml.property  name="scrollList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JScrollPane scrollList     = new JScrollPane();

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
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="binaryPanel:RBNgui.Bavaria"
	 */
	private Bavaria bavaria;

	public BinaryPanel(Bavaria b){

		bavaria = b;

		addBinField.setBackground(Color.white);
		binaryList.setBackground(Color.white);

		addPanel.add(addBinLabel, BorderLayout.WEST);
		addPanel.add(addBinField, BorderLayout.CENTER);

		binaryList.setModel(listModel);
		binaryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		binaryList.setCellRenderer(new MyListCellRenderer());

		scrollList.getViewport().add(binaryList);

		this.setLayout(new BorderLayout());
		this.add(binaryLabel, BorderLayout.NORTH);
		this.add(scrollList , BorderLayout.CENTER);
		this.add(addPanel, BorderLayout.SOUTH);


		binaryList.addMouseListener( this );
		addBinField.addKeyListener( this );
	}

	public void keyPressed(KeyEvent e) {
		Object source = e.getSource();
		JFrame frame = new JFrame();
		if( source == addBinField ){
			char c = e.getKeyChar();
			if(c == KeyEvent.VK_ENTER){
				if(bavaria.getStruc().getmydata().relExist( addBinField.getText())){
					System.out.print("Relation exists");
					JOptionPane.showMessageDialog(frame,
							"Relation "+ addBinField.getText() + " already exists",
							"Warning",
							JOptionPane.WARNING_MESSAGE);
				}
				else{	
					if(bavaria.getboolNumSelectionPanel().getboolboxselection()){
						BoolRel r = new BoolRel(addBinField.getText(), 2);
						bavaria.addRelation(r);
						listModel.addElement(r);
						addBinField.setText("");
						binaryList.ensureIndexIsVisible(listModel.size()-1);

					}
					else{
						NumRel r = new NumRel(addBinField.getText(), 2);
						bavaria.addRelation(r);
						listModel.addElement(r);
						addBinField.setText("");
						binaryList.ensureIndexIsVisible(listModel.size()-1);
					}
				}
			}

		}
	}
	public void keyReleased(KeyEvent e){
		Object source = e.getSource();
	}
	public void keyTyped(KeyEvent e){
		Object source = e.getSource();
	}

	public void mouseClicked(MouseEvent e){
		Object source = e.getSource();
	}
	public void mouseEntered(MouseEvent e){
		Object source = e.getSource();
	}
	public void mouseExited(MouseEvent e){
		Object source = e.getSource();
	}
	public void mousePressed(MouseEvent e){
		Object source = e.getSource();
		if( source == binaryList ){
			int index = binaryList.locationToIndex(e.getPoint());
			if(e.getButton() == 3 ){
				Object obj = listModel.get(index);
				if(obj instanceof BoolRel){
					BoolRel r = (BoolRel)listModel.get(index);
					Color old = r.getColor();
					Color ny = JColorChooser.showDialog( BinaryPanel.this, "Choose a color", old );
					if(ny != null){
						r.setColor( ny );
					}
				}
				else{
					NumRel r = (NumRel)listModel.get(index);
					Color old = r.getColor();
					Color ny = JColorChooser.showDialog( BinaryPanel.this, "Choose a color", old );
					if(ny != null){
						r.setColor( ny );
					}
				}

				repaint();
				bavaria.repaint();
			}
			else if (mode == ADDNODE || mode == MOVENODE || mode == DELETENODE){
				binaryList.clearSelection();
				addBinLabel.requestFocus();
			}
			else if (mode == ADDTUPLE){
				if (index >= 0){
					bavaria.clearSelections(2);
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
					bavaria.addTuple(new Rel());  //so that arity equals 0
			}
			else if (mode == DELETERELATION){
				if(index >= 0){
					Object obj = listModel.elementAt(index);

					if(obj instanceof BoolRel){
						String relName = ((BoolRel)listModel.elementAt(index)).name.name;
						int result = JOptionPane.showConfirmDialog(bavaria,
								"Do you really want to delete the whole relation " + relName + "?",
								"Delete Relation", JOptionPane.YES_NO_OPTION);
						if(result == JOptionPane.YES_OPTION){
							BoolRel r = (BoolRel)listModel.remove(index);
							bavaria.deleteRelation(r);
						}
						else if(result == JOptionPane.NO_OPTION){
							binaryList.clearSelection();
							addBinLabel.requestFocus();
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
							binaryList.clearSelection();
							addBinLabel.requestFocus();
						}
					}

				}
			}
		}
	}
	public void mouseReleased(MouseEvent e){
		Object source = e.getSource();
		if( source == binaryList ){
			int index = binaryList.locationToIndex(e.getPoint());
			if (mode == ADDNODE || mode == MOVENODE || mode == DELETENODE || mode == DELETERELATION){
				binaryList.clearSelection();
				addBinLabel.requestFocus();
			}
			if (mode == ADDTUPLE && index >= 0){
				binaryList.setSelectedIndex(index);
				binaryList.ensureIndexIsVisible(index);
			}
		}
	}


	public void setBinaryrelsNames(Vector<BoolRel> boolbinaryrels,Vector<NumRel> numbinaryrels ){
		for(int i=0; i<boolbinaryrels.size(); ++i){
			listModel.addElement(boolbinaryrels.elementAt(i));
		}
		for(int i=0; i<numbinaryrels.size(); ++i){
			listModel.addElement(numbinaryrels.elementAt(i));
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
		binaryList.clearSelection();
	}

	public void empty(){
		listModel.clear();
	}

}
