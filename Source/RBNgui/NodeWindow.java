/* NodeWindow.java 
 * x
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
import javax.swing.event.*;
import java.util.*;

import RBNExceptions.RBNIllegalArgumentException;
import RBNpackage.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;


public class NodeWindow extends JFrame implements KeyListener, ActionListener, 
MouseListener, ChangeListener,TableModelListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * @uml.property  name="nodeNameField"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTextField nodeNameField = new JTextField(10);
	/**
	 * @uml.property  name="nodeNameLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel nodeNameLabel     = new JLabel("Node name:");
	/**
	 * @uml.property  name="nodeNameReserved"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel nodeNameReserved  = new JLabel(" ");
	/**
	 * @uml.property  name="attributesLabel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JLabel attributesLabel   = new JLabel("Attributes");
	/**
	 * @uml.property  name="tabbedPane"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTabbedPane tabbedPane   = new JTabbedPane();
	/**
	 * @uml.property  name="attrTabbedPane"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTabbedPane attrTabbedPane = new JTabbedPane();
	/**
	 * @uml.property  name="attributesList"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JList attributesList;
	/**
	 * @uml.property  name="bavaria"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private Bavaria bavaria;
	/**
	 * @uml.property  name="editPanel"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="openNodeWindows:RBNgui.EditPanel"
	 */
	private EditPanel editPanel;
	/**
	 * @uml.property  name="deleteAttribute"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JButton deleteAttribute = new JButton("Delete Attribute");

	/**
	 * @uml.property  name="tvmodel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private tupvalmodel tvmodel = new tupvalmodel();
	/**
	 * @uml.property  name="tupvaltable"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTable tupvaltable ;

	/**
	 * @uml.property  name="numattrmodel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private tupvalmodel numattrmodel = new tupvalmodel();
	/**
	 * @uml.property  name="numattrtable"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private JTable numattrtable;


	/**
	 * @uml.property  name="attributes" multiplicity="(0 -1)" dimension="1"
	 */
	private Vector[] attributes; //arity 1
	/**
	 * @uml.property  name="otherRels" multiplicity="(0 -1)" dimension="1"
	 */
	private Vector[] otherRels;  //arity 2 or bigger

	/**
	 * @uml.property  name="boolattributes" multiplicity="(0 -1)" dimension="1"
	 */
	private Vector[] boolattributes; //arity 1 bool
	/**
	 * @uml.property  name="boolotherRels" multiplicity="(0 -1)" dimension="1"
	 */
	private Vector[] boolotherRels;  //arity 2 or bigger bool

	/**
	 * @uml.property  name="numattributes" multiplicity="(0 -1)" dimension="1"
	 */
	private Vector[] numattributes; //arity 1 num
	/**
	 * @uml.property  name="numotherRels" multiplicity="(0 -1)" dimension="1"
	 */
	private Vector[] numotherRels;  //arity 2 or bigger num

	/**
	 * @uml.property  name="boolattributeRels"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.BoolRel"
	 */
	private Vector boolattributeRels;
	/**
	 * @uml.property  name="boolbinAndArityRels"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.BoolRel"
	 */
	private Vector boolbinAndArityRels;	

	/**
	 * @uml.property  name="boolattributeTuples"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="I"
	 */
	private Vector boolattributeTuples;
	/**
	 * @uml.property  name="boolbinAndArityTuples"
	 * @uml.associationEnd  
	 */
	private Vector boolbinAndArityTuples;

	/**
	 * @uml.property  name="numbinAndArityRels"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.NumRel"
	 */
	private Vector numbinAndArityRels;
	/**
	 * @uml.property  name="numbinAndArityTuples"
	 * @uml.associationEnd  
	 */
	private Vector numbinAndArityTuples;
	private Vector numbinAndArityValues;

	/**
	 * @uml.property  name="numvaluesandtuples" multiplicity="(0 -1)" dimension="1"
	 */
	private Vector[] numvaluesandtuples ;
	/**
	 * @uml.property  name="numvalues"
	 */
	private Vector numvalues;
	/**
	 * @uml.property  name="numtuples"
	 */
	private Vector numtuples;

	/**
	 * @uml.property  name="numattributesRels"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.NumRel"
	 */
	private Vector numattributesRels;
	/**
	 * @uml.property  name="numattributeTuples"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="[I"
	 */
	private Vector numattributeTuples;

	/**
	 * @uml.property  name="attrListItem"
	 */
	private int attrListItem = -1;
	/**
	 * @uml.property  name="relListItem"
	 */
	private int relListItem  = -1;
	/**
	 * @uml.property  name="index"
	 */
	private int index = -1;
	/**
	 * @uml.property  name="tableRowIndex"
	 */
	private int tableRowIndex = -1;

	/**
	 * @uml.property  name="boolattributesListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.BoolRel"
	 */
	private DefaultListModel boolattributesListModel;
	/**
	 * @uml.property  name="boolbinAndArityListModels"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="javax.swing.DefaultListModel"
	 */
	private Vector boolbinAndArityListModels = new Vector();
	/**
	 * @uml.property  name="boolbinAndArityLists"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="javax.swing.JList"
	 */
	private Vector boolbinAndArityLists      = new Vector();

	/**
	 * @uml.property  name="numattributesValueListModel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private DefaultListModel numattributesValueListModel;

	/**
	 * @uml.property  name="numattributesListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.NumRel"
	 */
	private DefaultListModel numattributesListModel;
	/**
	 * @uml.property  name="numbinAndArityListModels"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="javax.swing.DefaultListModel"
	 */
	private Vector numbinAndArityListModels = new Vector();
	/**
	 * @uml.property  name="numbinAndArityValueListModels"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="javax.swing.DefaultListModel"
	 */
	private Vector numbinAndArityValueListModels = new Vector();


	/**
	 * @uml.property  name="valueLists"
	 */
	private Vector valueLists      = new Vector();
	/**
	 * @uml.property  name="tupleLists"
	 */
	private Vector tupleLists      = new Vector();

	/**
	 * @uml.property  name="numbinAndArityValues"
	 */
//	private Vector<Double> numbinAndArityValues = new Vector<Double>();
	/**
	 * @uml.property  name="numattributesValues"
	 */
	private Vector<Double> numattributesValues = new Vector<Double>();

	/**
	 * @uml.property  name="previousv"
	 */
	private Double previousv;
	/*binAndArityRels i == binAndArityTuples i == binAndArityListModels i == binAndArityLists i
    == tab number i */
	/**
	 * @uml.property  name="rowdeleted"
	 */
	private boolean rowdeleted;

	private boolean deleteenabled;
	
	public NodeWindow(Bavaria b, EditPanel e, int ind){

		index     = ind;
		bavaria   = b;
		editPanel = e;

		deleteenabled = !(bavaria.getRelmode() == Bavaria.DISPLAYALLMODE || bavaria.getRelmode() == Bavaria.NORELMODE );
		if (!deleteenabled)
			deleteAttribute.setEnabled(false);
		
		boolattributes = bavaria.getAttrBoolRelsAndTuples(index);
		numattributes = bavaria.getAttrNumRelsAndTuples(index);

		boolotherRels = bavaria.getOtherBoolRelsAndTuples(index);
		numotherRels = bavaria.getOtherNumRelsAndTuples(index);

//		numbinAndArityValues = bavaria.numbinAndArityValues(index);
		numattributesValues = bavaria.numattributesValues(index);

		boolattributeRels 	  = boolattributes[0];
		boolattributeTuples   = boolattributes[1];

		numattributesRels = numattributes[0];
		numattributeTuples   = numattributes[1];

		boolbinAndArityRels   = boolotherRels[0];
		boolbinAndArityTuples	= boolotherRels[1];

		numbinAndArityRels   = numotherRels[0];
		numbinAndArityTuples	= numotherRels[1];
		numbinAndArityValues	= numotherRels[2];


		//renaming the node
		nodeNameField.setText(bavaria.nameAt(index));
		nodeNameField.addKeyListener( this );


		//create attributes list
		boolattributesListModel              = new DefaultListModel();
		attributesList			 = new JList();
	

		JScrollPane attributesScrollList = new JScrollPane();


		numattributesListModel = new DefaultListModel();
		numattributesValueListModel = new DefaultListModel();

		for(int i=0; i<boolattributeRels.size(); ++i){
			boolattributesListModel.addElement((BoolRel)boolattributeRels.elementAt(i));
		}
		for(int i=0; i<numattributesRels.size(); ++i){
			numattributesListModel.addElement((NumRel)numattributesRels.elementAt(i));
		}
		for(int i=0; i<numattributesValues.size(); ++i){
			numattributesValueListModel.addElement(numattributesValues.elementAt(i));
		}

		attributesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		attributesList.setModel(boolattributesListModel);
		attributesScrollList.getViewport().add(attributesList);
		attributesList.addMouseListener( this );

		//create tabs		
		JPanel booltab = createTab(boolattributesListModel);
		attrTabbedPane.addTab("Boolean", attributesList);
		JPanel numtab = createTab(numattributesListModel,numattributesValueListModel);

		numattrmodel = new tupvalmodel(numattributesListModel,numattributesValueListModel);
		numattrtable = new JTable(numattrmodel);
		numattrtable.getModel().addTableModelListener(this);

		

		numattrtable.addMouseListener(this);
		attrTabbedPane.addTab("Numeric", numattrtable);
		attrTabbedPane.addChangeListener( this );
		deleteAttribute.addActionListener( this );

		JPanel attributesPanel = new JPanel(new BorderLayout());
		attributesPanel.add(attributesLabel , BorderLayout.NORTH);
		attributesPanel.add(attrTabbedPane, BorderLayout.CENTER);
		attributesPanel.add(deleteAttribute, BorderLayout.SOUTH);
		//end attributes list

		//create tabs
		for(int i=0; i<boolbinAndArityRels.size(); ++i){
			BoolRel currentRel = (BoolRel)boolbinAndArityRels.elementAt(i);
			Vector tuples  = (Vector)boolbinAndArityTuples.elementAt(i);
			JPanel tab = createTab(currentRel, tuples, deleteenabled);
			tabbedPane.addTab(currentRel.name.name, tab);
		}
		for(int i=0; i<numbinAndArityRels.size(); ++i){
			NumRel currentRel = (NumRel)numbinAndArityRels.elementAt(i);
			Vector tuples  = (Vector)numbinAndArityTuples.elementAt(i);	
			Vector values = (Vector)numbinAndArityValues.elementAt(i);	
//			Todo: replace in the following numbinAndArityValues with a vector that contains these
//			values only for currentRel!
			JPanel tab = createTab(currentRel,tuples,values,deleteenabled);
			tabbedPane.addTab(currentRel.name.name, tab);
		}
		//end tabs


		tabbedPane.addChangeListener( this );

		JPanel nameFieldAndLabels = new JPanel(new BorderLayout(3, 3));
		nameFieldAndLabels.add(nodeNameLabel, BorderLayout.WEST);
		nameFieldAndLabels.add(nodeNameField, BorderLayout.CENTER);
		nameFieldAndLabels.add(nodeNameReserved, BorderLayout.SOUTH);

		JPanel relationPanel = new JPanel(new GridLayout(2, 1, 3, 3));
		relationPanel.add(attributesPanel);
		relationPanel.add(tabbedPane);

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout(5, 2));
		contentPane.add(nameFieldAndLabels, BorderLayout.NORTH);
		contentPane.add(relationPanel, BorderLayout.CENTER);

		/* Disable the delete buttons if Bavaria is in 
		 * DISPLAYALLMODE
		 */
		
	
			
		ImageIcon icon = new ImageIcon("small_logo.jpg");
		if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) //image ok
			this.setIconImage(icon.getImage());
		this.setTitle(bavaria.nameAt(index));
		this.setSize(300, 500);
		this.show();

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				editPanel.closeNodeWindow(NodeWindow.this);
				dispose();
			}
		});
	}
	public void tableChanged(TableModelEvent e) {

		Object source = e.getSource();
		if(source == numattrtable.getModel()){
			if(!rowdeleted){
				System.out.println(e);
				int row = e.getFirstRow();
				int column = e.getColumn();
				TableModel model = (TableModel)e.getSource();
				String columnName = model.getColumnName(column);
				Object data = model.getValueAt(row, column);
				Double v = Double.parseDouble((String)data);

				if(v != previousv){
					NumRel r = (NumRel)numattributesListModel.elementAt(row);
					int[] tuple = (int[])numattributeTuples.elementAt(row);
					try {
						bavaria.setData(r,tuple,v);
					} catch (RBNIllegalArgumentException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}		        

				}
			}
		}

	}
	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();
		if( source == deleteAttribute ){
			int i = attrTabbedPane.getSelectedIndex();
			if (i == 0){
				if(attrListItem != -1){
					BoolRel removed = (BoolRel)boolattributesListModel.remove(attrListItem);
					int[] tuple = (int[])boolattributeTuples.remove(attrListItem);
					bavaria.deleteTuple(removed, tuple);
					attrListItem = -1;
					editPanel.repaint();
				}
			}
			else {
				if(tableRowIndex != -1){
					NumRel removed = (NumRel)numattributesListModel.elementAt(tableRowIndex);
					rowdeleted = true;
					numattrmodel.remove(tableRowIndex);
					numattrmodel.fireTableStructureChanged();
					//numattrtable.requestFocus();				
					int[] tuple = (int[])numattributeTuples.remove(tableRowIndex);
					Double v = (Double)numattributesValues.remove(tableRowIndex);
					//bavaria.deleteTuple(removed, tuple, v);
					bavaria.deleteTuple(removed, tuple);
					tableRowIndex = -1;
					editPanel.repaint();
				}
			}
		}
	}

	public void keyPressed(KeyEvent e) {
		Object source = e.getSource();
		if( source == nodeNameField ){
			char c = e.getKeyChar();
			if(c == KeyEvent.VK_ENTER){
				if(bavaria.setName(nodeNameField.getText(), index) == 0){
					nodeNameReserved.setForeground(Color.black);
					nodeNameReserved.setText("Name already in use");
				}
				else {
					editPanel.repaint();
					editPanel.updateNodeName(index);
					setTitle(bavaria.nameAt(index));
					nodeNameReserved.setText(" ");
				}
			}
		}
	}
	public void keyReleased(KeyEvent e){
	}
	public void keyTyped(KeyEvent e){
		Object source = e.getSource();
	}

	public void stateChanged(ChangeEvent e){
		Object source = e.getSource();
		if( source == tabbedPane ){
			for(int i=0; i<boolbinAndArityLists.size(); ++i){
				JList temp = (JList)boolbinAndArityLists.elementAt(i);
				temp.clearSelection();
				tabbedPane.requestFocus();
			}
			relListItem = -1;
			//System.out.println(tabbedPane.getSelectedIndex());
		}
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
			attrListItem = attributesList.locationToIndex(e.getPoint());
			if(attrListItem == -1){
				attributesList.clearSelection();
				attributesLabel.requestFocus();
			}
		}	 
		else if(source == numattrtable){
			tableRowIndex = numattrtable.rowAtPoint(e.getPoint());
			previousv = (Double)numattrtable.getValueAt(tableRowIndex, 1);
			rowdeleted = false;
			if(tableRowIndex == -1){
				numattrtable.clearSelection();
				attrTabbedPane.requestFocus();
			}
		}
		else if(source == tupvaltable){
			relListItem = tupvaltable.rowAtPoint(e.getPoint());
			if(relListItem != -1){
				previousv = Double.parseDouble((String)tupvaltable.getValueAt(relListItem, 1));
			}
			rowdeleted = false;
			if(relListItem == -1){
				tupvaltable.clearSelection();
				tabbedPane.requestFocus();
			}
		}

	}
	public void mouseReleased(MouseEvent e){
	}

	//Returns index of the node
	/**
	 * @return
	 * @uml.property  name="index"
	 */
	public int getIndex(){
		return index;
	}


	//Changes index of the node
	public void changeIndex(){
		index = index-1;
	}


	//Disposes this window
	public void disposeNodeWindow(){
		editPanel.closeNodeWindow(this);
		this.dispose();
	}


	//Creates a tab for the relations with arity 2 or bigger
	public JPanel createTab(BoolRel re, final Vector tuples, boolean deleteenabled){
		final BoolRel r = re;
		final DefaultListModel boolbinAndArityListModel = new DefaultListModel();
		final JList boolbinAndArityList = new JList();
		JScrollPane boolbinAndArityScrollList = new JScrollPane();

		//creates a list items
		for(int j=0; j<tuples.size(); ++j){
			int[] temp = (int[])tuples.elementAt(j);
			String listItem = "(";
			for(int k=0; k<temp.length; ++k){
				if(k < temp.length-1)
					listItem = listItem + bavaria.nameAt(temp[k]) + ", ";
				else
					listItem = listItem + bavaria.nameAt(temp[k]);
			}
			listItem = listItem + ")";
			boolbinAndArityListModel.addElement(listItem);
		}

		boolbinAndArityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		boolbinAndArityList.setModel(boolbinAndArityListModel);
		boolbinAndArityScrollList.getViewport().add(boolbinAndArityList);

		boolbinAndArityListModels.addElement(boolbinAndArityListModel);
		boolbinAndArityLists.addElement(boolbinAndArityList);

		JButton deleteTuple = new JButton("Delete Tuple");
		if (!deleteenabled)
			deleteTuple.setEnabled(false);

		deleteTuple.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(relListItem != -1){
					int i = tabbedPane.getSelectedIndex();
					DefaultListModel temp = (DefaultListModel)boolbinAndArityListModels.elementAt(i);
					temp.remove(relListItem);

					Vector v = (Vector)boolbinAndArityTuples.elementAt(i);
					int[] tuple = (int[])v.remove(relListItem);
					editPanel.tupleDeletedFromNodeWindow(r, tuple);
					bavaria.deleteTuple(r, tuple);
					relListItem = -1;
					editPanel.repaint();
				}
			}
		});

		boolbinAndArityList.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				relListItem = boolbinAndArityList.locationToIndex(e.getPoint());
				if(relListItem == -1){
					boolbinAndArityList.clearSelection();
					tabbedPane.requestFocus();
				}
			}
		});

		JPanel relationTabPanel = new JPanel(new BorderLayout());
		relationTabPanel.add(boolbinAndArityScrollList, BorderLayout.CENTER);
		relationTabPanel.add(deleteTuple, BorderLayout.SOUTH);

		return relationTabPanel;
	}
	
	
	public JPanel createTab(NumRel re, final Vector tuples, final Vector values, boolean delenabled){

		final NumRel r = re;

		final DefaultListModel numbinAndArityListModel = new DefaultListModel();
		final DefaultListModel numbinAndArityValueListModel = new DefaultListModel();

		//creates a list items
		for(int j=0; j<tuples.size(); ++j){
			int[] temp = (int[])tuples.elementAt(j);
			String listItem = "(";
			for(int k=0; k<temp.length; ++k){
				if(k < temp.length-1)
					listItem = listItem + bavaria.nameAt(temp[k]) + ", ";
				else
					listItem = listItem + bavaria.nameAt(temp[k]);
			}
			listItem = listItem + ")";
			numbinAndArityListModel.addElement(listItem);
		}

		//creates a list of numerical values
		for(int j=0; j<values.size(); ++j){
			Double  val  = (Double)values.elementAt(j);
			String listItem = val.toString();
			numbinAndArityValueListModel.addElement(listItem);
		}

		numbinAndArityListModels.addElement(numbinAndArityListModel);
		numbinAndArityValueListModels.addElement(numbinAndArityValueListModel);

		tvmodel = new tupvalmodel(numbinAndArityListModel, numbinAndArityValueListModel);
		tupvaltable = new JTable(tvmodel);

		tupvaltable.getModel().addTableModelListener(new TableModelListener() {

			public void tableChanged(TableModelEvent e) {

				if(!rowdeleted){
					System.out.println(e);
					int row = e.getFirstRow();
					int column = e.getColumn();
					TableModel model = (TableModel)e.getSource();
					String data = (String)model.getValueAt(row, column);
					Double v = Double.parseDouble(data);

					if(v != previousv){
						int[] tuple = (int[])tuples.elementAt(row);

						try {
							bavaria.setData(r,tuple,v);
						} catch (RBNIllegalArgumentException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}		        

					}
				}

			}
		});

		JButton deleteTuple = new JButton("Delete Tuple");
		if (!delenabled)
			deleteTuple.setEnabled(false);

		deleteTuple.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(relListItem != -1){
					int i = tabbedPane.getSelectedIndex();
					//DefaultListModel temp = (DefaultListModel)numbinAndArityListModels.elementAt(i);
					//temp.remove(relListItem);

					Double value = Double.parseDouble((String)numbinAndArityValueListModel.elementAt(relListItem));
					//numbinAndArityListModel.remove(relListItem);
					//numbinAndArityValueListModel.remove(relListItem);
					rowdeleted = true;
					tvmodel.remove(relListItem);
					
					tvmodel.fireTableStructureChanged();
					tupvaltable.requestFocus();
					Vector v = (Vector)numbinAndArityTuples.elementAt(0);
					int[] tuple = (int[])v.remove(relListItem);
					//editPanel.tupleDeletedFromNodeWindow(r, tuple);
					//bavaria.deleteTuple(r, tuple, value);
					bavaria.deleteTuple(r, tuple);
					relListItem = -1;
					editPanel.repaint();
				}
			}
		});


		tupvaltable.addMouseListener(this);
		/*{
			public void mousePressed(MouseEvent e){
				relListItem = tupvaltable.rowAtPoint(e.getPoint());
				previousv = Double.parseDouble((String)tupvaltable.getValueAt(relListItem, 1));
				rowdeleted = false;
				if(relListItem == -1){
					tupvaltable.clearSelection();
					tabbedPane.requestFocus();
				}
			}
		});
		 */

		JPanel relationTabPanel = new JPanel(new BorderLayout());
		relationTabPanel.add(tupvaltable, BorderLayout.CENTER);

		//relationTabPanel.add(binAndArityScrollList, BorderLayout.CENTER);
		relationTabPanel.add(deleteTuple, BorderLayout.SOUTH);

		return relationTabPanel;
	}

	public JPanel createTab(DefaultListModel boolattributes){
		final DefaultListModel boolattributesListModel = boolattributes;
		final JList boolattributesList = new JList();
		JScrollPane boolattributesScrollList = new JScrollPane();

		boolattributesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		boolattributesList.setModel(boolattributesListModel);
		boolattributesScrollList.getViewport().add(boolattributesList);

		//boolattributesListModels.addElement(boolbinAndArityListModel);
		//boolattrributesLists.addElement(boolattributesList);
		JPanel attributeTabPanel = new JPanel(new BorderLayout());
		attributeTabPanel.add(boolattributesScrollList, BorderLayout.CENTER);
		return attributeTabPanel;
	}
	
	public JPanel createTab(DefaultListModel numattributes, DefaultListModel numattributesvalues){	
		numattrmodel = new tupvalmodel(numattributes,numattributesvalues);
		numattrtable = new JTable(numattrmodel);
		JPanel attributeTabPanel = new JPanel(new BorderLayout());
		attributeTabPanel.add(numattrtable, BorderLayout.CENTER);
		return attributeTabPanel;	
	}
	//Removes the tuple from the list

	public void tupleDeletedFromNodeWindow(BoolRel r, int[] tuple){
		int i = boolbinAndArityRels.indexOf(r);
		if(i != -1){
			Vector tuples = (Vector)boolbinAndArityTuples.elementAt(i);
			DefaultListModel temp = (DefaultListModel)boolbinAndArityListModels.elementAt(i);
			int index = tuples.indexOf(tuple);
			if(index != -1){
				temp.remove(index);
				tuples.remove(tuple);
			}
		}
	}
	public void tupleDeletedFromNodeWindow(NumRel r, int[] tuple){
		int i = numbinAndArityRels.indexOf(r);
		if(i != -1){
			Vector tuples = (Vector)numbinAndArityTuples.elementAt(i);
			DefaultListModel temp = (DefaultListModel)numbinAndArityListModels.elementAt(i);
			int index = tuples.indexOf(tuple);
			if(index != -1){
				temp.remove(index);
				tuples.remove(tuple);
			}
		}
	}


	//Updates the node's name in the lists (where arity >= 2)
	public void updateNodeName(int index){
		for(int i=0; i<boolbinAndArityTuples.size(); ++i){
			Vector tuples = (Vector)boolbinAndArityTuples.elementAt(i);
			DefaultListModel temp = (DefaultListModel)boolbinAndArityListModels.elementAt(i);
			for(int j=0; j<tuples.size(); ++j){
				int[] tuple = (int[])tuples.elementAt(j);
				for(int k=0; k<tuple.length; ++k){
					if(tuple[k] == index){
						//updates the list item
						String listItem = "(";
						for(int l=0; l<tuple.length; ++l){
							if(l < tuple.length-1)
								listItem = listItem + bavaria.nameAt(tuple[l]) + ", ";
							else
								listItem = listItem + bavaria.nameAt(tuple[l]);
						}
						listItem = listItem + ")";
						try{
							temp.set(j, listItem);
						}catch (Exception e){
						}
						break;
					}
				}
			}
		}
		for(int i=0; i<numbinAndArityTuples.size(); ++i){
			Vector tuples = (Vector)numbinAndArityTuples.elementAt(i);
			DefaultListModel temp = (DefaultListModel)numbinAndArityListModels.elementAt(i);
			for(int j=0; j<tuples.size(); ++j){
				int[] tuple = (int[])tuples.elementAt(j);
				for(int k=0; k<tuple.length; ++k){
					if(tuple[k] == index){
						//updates the list item
						String listItem = "(";
						for(int l=0; l<tuple.length; ++l){
							if(l < tuple.length-1)
								listItem = listItem + bavaria.nameAt(tuple[l]) + ", ";
							else
								listItem = listItem + bavaria.nameAt(tuple[l]);
						}
						listItem = listItem + ")";
						try{
							temp.set(j, listItem);
						}catch (Exception e){
						}
						break;
					}
				}
			}
		}
	}


	//Removes the tuples which include the deleted node
	public void nodeWindowNodeDeleted(int deletedNode){
		for(int i=0; i<boolbinAndArityTuples.size(); ++i){
			Vector tuples = (Vector)boolbinAndArityTuples.elementAt(i);
			DefaultListModel temp = (DefaultListModel)boolbinAndArityListModels.elementAt(i);
			for(int j=0; j<tuples.size(); ++j){
				int[] tuple = (int[])tuples.elementAt(j);
				for(int k=0; k<tuple.length; ++k){
					if(tuple[k] == deletedNode){
						tuples.removeElementAt(j);
						temp.removeElementAt(j);
						--j;
						break;
					}
				}
			}
		}
		for(int i=0; i<numbinAndArityTuples.size(); ++i){
			Vector tuples = (Vector)numbinAndArityTuples.elementAt(i);
			DefaultListModel temp = (DefaultListModel)numbinAndArityListModels.elementAt(i);
			for(int j=0; j<tuples.size(); ++j){
				int[] tuple = (int[])tuples.elementAt(j);
				for(int k=0; k<tuple.length; ++k){
					if(tuple[k] == deletedNode){
						tuples.removeElementAt(j);
						temp.removeElementAt(j);
						--j;
						break;
					}
				}
			}
		}
	}


	//Adds tuple to the open node window if it belongs to the tuple
	public void addTupleToNodeWindow(BoolRel r, int[] tuple){
		if(r.arity == 1){
			boolattributesListModel.addElement(r);
			boolattributeTuples.addElement(tuple);
		}
		else{
			for(int i=0; i<tuple.length; ++i){
				if(tuple[i] == index){
					int j = boolbinAndArityRels.indexOf(r);
					if(j != -1){
						DefaultListModel temp = (DefaultListModel)boolbinAndArityListModels.elementAt(j);
						String listItem = "(";
						for(int k=0; k<tuple.length; ++k){
							if(k < tuple.length-1)
								listItem = listItem + bavaria.nameAt(tuple[k]) + ", ";
							else
								listItem = listItem + bavaria.nameAt(tuple[k]);
						}
						listItem = listItem + ")";
						temp.addElement(listItem);
						Vector tuples = (Vector)boolbinAndArityTuples.elementAt(j);
						tuples.addElement(tuple);
					}
					break;
				}
			}
		}
	}
	public void addTupleToNodeWindow(NumRel r, int[] tuple){

		for(int i=0; i<tuple.length; ++i){
			if(tuple[i] == index){
				int j = numbinAndArityRels.indexOf(r);
				if(j != -1){
					DefaultListModel temp = (DefaultListModel)numbinAndArityListModels.elementAt(j);
					String listItem = "(";
					for(int k=0; k<tuple.length; ++k){
						if(k < tuple.length-1)
							listItem = listItem + bavaria.nameAt(tuple[k]) + ", ";
						else
							listItem = listItem + bavaria.nameAt(tuple[k]);
					}
					listItem = listItem + ")";
					temp.addElement(listItem);
					Vector tuples = (Vector)numbinAndArityTuples.elementAt(j);
					tuples.addElement(tuple);
				}
				break;

			}
		}
	}

	//Adds a new relation (tab) to the node window
	/*public void addNewRelToNodeWindow(Rel r){
		if(r.arity != 1){
			binAndArityRels.addElement(r);
			binAndArityTuples.addElement(new Vector());
			JPanel tab = createTab(r, new Vector());
			tabbedPane.addTab(r.name.name, tab);
		}
	}
	 */
	public void addNewRelToNodeWindow(BoolRel r){
		if(r.arity != 1){
			boolbinAndArityRels.addElement(r);
			boolbinAndArityTuples.addElement(new Vector());
			JPanel tab = createTab(r, new Vector(), deleteenabled);
			tabbedPane.addTab(r.name.name, tab);
		}
	}
	public void addNewRelToNodeWindow(NumRel r){
		if(r.arity != 1){
			numbinAndArityRels.addElement(r);
			numbinAndArityTuples.addElement(new Vector());
			JPanel tab = createTab(r, new Vector(),new Vector(), deleteenabled);
			tabbedPane.addTab(r.name.name, tab);
		}
	}


	//Removes the relation from the node window

	public void deleteRelFromNodeWindow(BoolRel r){
		if(r.arity == 1){
			int i = boolattributesListModel.indexOf(r);
			if(i != -1){ //node belongs to this relation
				boolattributesListModel.remove(i);
				boolattributeTuples.remove(i);
				attrListItem = -1;
			}
		}
		else {
			int i = boolbinAndArityRels.indexOf(r);
			if(i != -1){
				boolbinAndArityRels.remove(i);
				boolbinAndArityTuples.remove(i);
				boolbinAndArityListModels.remove(i);
				boolbinAndArityLists.remove(i);
				tabbedPane.remove(i);
				relListItem = -1;
			}
		}
	}
	public void deleteRelFromNodeWindow(NumRel r){

		int i = numbinAndArityRels.indexOf(r);
		if(i != -1){
			numbinAndArityRels.remove(i);
			numbinAndArityTuples.remove(i);
			numbinAndArityListModels.remove(i);
			//numbinAndArityLists.remove(i);
			tabbedPane.remove(i);
			relListItem = -1;

		}
	}


}