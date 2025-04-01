/*
 * Sampling.java
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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.JDialog;

import RBNio.*;

public class TypeSelectionPanel extends JOptionPane
{

	/**
	 * @uml.property  name="ctypenames"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	private Vector<String> ctypenames;
	
	public TypeSelectionPanel(Vector<String> ctns){
		ctypenames = ctns;

	}
	
	public  Vector<String> showInputDialog(){
		
		
		class mydialog extends JDialog implements ActionListener{
			private JScrollPane candidateTypeScrollList     = new JScrollPane();
			private JList candidateTypeList                 = new JList();
			private JButton okbutton = new JButton("OK");
			private DefaultListModel candidatetypesListModel = new DefaultListModel();
			private Vector<String> seltypes;
			
			private mydialog(Vector<String> ctns){
				setModal(true);
				candidateTypeList.setModel(candidatetypesListModel);
				candidateTypeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				candidateTypeScrollList.getViewport().add(candidateTypeList);

				okbutton.addActionListener(this);
				
				Container contentPane = this.getContentPane();
				contentPane.add(candidateTypeScrollList, BorderLayout.CENTER);
				contentPane.add(okbutton,BorderLayout.SOUTH);

				for (int i=0;i<ctns.size();i++){
					candidatetypesListModel.addElement(ctns.elementAt(i));
				}
				this.setTitle("Type Selection");
				this.setSize(300,400);
				this.setVisible(true);
			}

			public void actionPerformed(ActionEvent a){
				Object source = a.getSource();
				if (source == okbutton){
					int[] selindices = candidateTypeList.getSelectedIndices();
					seltypes = new Vector<String>();
					for (int i=0;i<selindices.length;i++)
						seltypes.add(ctypenames.elementAt(selindices[i]));
					dispose();
				}
			}

			Vector<String> getSelected(){
				return seltypes;
			}
		}
		
		return new mydialog(ctypenames).getSelected();
	}

}
