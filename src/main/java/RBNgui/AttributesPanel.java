/* AttributesPanel.java 
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

public class AttributesPanel extends JPanel implements MouseListener, KeyListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * @uml.property  name="attributesLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel attributesLabel     = new JLabel("Attributes");
	/**
	 * @uml.property  name="attributesList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList attributesList       = new JList();
	/**
	 * @uml.property  name="listModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.NumRel"
	 */
	private DefaultListModel listModel = new DefaultListModel();
	/**
	 * @uml.property  name="addAttrLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel addAttrLabel        = new JLabel("Add:");
	/**
	 * @uml.property  name="addAttrField"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField addAttrField    = new JTextField(10);
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
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="attributesPanel:RBNgui.Bavaria"
	 */
	private Bavaria bavaria;
	/**
	 * @uml.property  name="font"
	 */
	private Font font = new Font("Serif", Font.BOLD,10);
	public AttributesPanel(Bavaria b){

		bavaria = b;

		addAttrField.setBackground(Color.white);
		attributesList.setBackground(Color.white);

		addPanel.add(addAttrLabel, BorderLayout.WEST);
		addPanel.add(addAttrField, BorderLayout.CENTER);

		attributesList.setModel(listModel);
		attributesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		attributesList.setCellRenderer(new MyListCellRenderer());

		scrollList.getViewport().add(attributesList);

		this.setLayout(new BorderLayout());
		this.add(attributesLabel, BorderLayout.NORTH);
		this.add(scrollList, BorderLayout.CENTER);
		this.add(addPanel, BorderLayout.SOUTH);
		//this.add(bnPanel);


		attributesList.addMouseListener( this );
		addAttrField.addKeyListener( this );

	}
	//needs mod
	public void keyPressed(KeyEvent e){
		JFrame frame = new JFrame();
		Object source = e.getSource();
		if( source == addAttrField ){
			char c = e.getKeyChar();
			if (c == KeyEvent.VK_ENTER){
				if(bavaria.getStruc().getmydata().relExist( addAttrField.getText())){
					System.out.print("Relation exists");
					JOptionPane.showMessageDialog(frame,
						    "Relation "+ addAttrField.getText() + " already exists",
						    "Warning",
						    JOptionPane.WARNING_MESSAGE);
				}
				else{						
					if(bavaria.getboolNumSelectionPanel().getboolboxselection()){
						BoolRel r = new BoolRel(addAttrField.getText(), 1);
						bavaria.addRelation(r);
						listModel.addElement(r);
						addAttrField.setText("");
						attributesList.ensureIndexIsVisible(listModel.size()-1);
					}
					else{
						NumRel r = new NumRel(addAttrField.getText(), 1);
						bavaria.addRelation(r);
						listModel.addElement(r);
						addAttrField.setText("");
						attributesList.ensureIndexIsVisible(listModel.size()-1);
					}
				}

			}
		}
	}
	public void keyReleased(KeyEvent e){ 
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
		if( source == attributesList ){
			int index = attributesList.locationToIndex(e.getPoint());
			if(e.getButton() == 3 ){
				Object obj = listModel.elementAt(index);
				if(obj instanceof BoolRel ){
					BoolRel r = (BoolRel)listModel.get(index);
					Color old = r.getColor();
					Color ny = JColorChooser.showDialog( AttributesPanel.this, "Choose a color", old );
					if(ny != null){
						r.setColor( ny );
					}
				}
				else{
					NumRel r = (NumRel)listModel.get(index);
					Color old = r.getColor();
					Color ny = JColorChooser.showDialog( AttributesPanel.this, "Choose a color", old );
					if(ny != null){
						r.setColor( ny );
					}
				}

				repaint();
				bavaria.repaint();
			}
			else if (mode == ADDNODE || mode == MOVENODE || mode == DELETENODE){
				attributesList.clearSelection();
				addAttrLabel.requestFocus();
			}
			else if (mode == ADDTUPLE){
				if (index >= 0){
					bavaria.clearSelections(1);

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
							attributesList.clearSelection();
							addAttrLabel.requestFocus();
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
							attributesList.clearSelection();
							addAttrLabel.requestFocus();
						}
					}
				}
			}
		}
	}

	public void mouseReleased(MouseEvent e){
		Object source = e.getSource();
		if( source == attributesList ){
			int index = attributesList.locationToIndex(e.getPoint());
			if (mode == ADDNODE || mode == MOVENODE || mode == DELETENODE || mode == DELETERELATION){
				attributesList.clearSelection();
				addAttrLabel.requestFocus();
			}
			if (mode == ADDTUPLE && index >= 0){
				attributesList.setSelectedIndex(index);
				attributesList.ensureIndexIsVisible(index);
			}
		}
	}



	public void setAttributesNames(Vector<BoolRel> boolattributes, Vector<NumRel> numattributes){
		for(int i=0; i<boolattributes.size(); ++i){
			listModel.addElement(boolattributes.elementAt(i));
		}
		for(int i=0; i<numattributes.size(); ++i){
			listModel.addElement(numattributes.elementAt(i));
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
		attributesList.clearSelection();
	}

	public void empty(){
		listModel.clear();
	}

}
