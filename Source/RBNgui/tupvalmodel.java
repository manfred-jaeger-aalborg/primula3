package RBNgui;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.DefaultListModel;

public class tupvalmodel extends AbstractTableModel implements TableModelListener {
	/**
	 * @uml.property  name="column1"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	DefaultListModel column1;
	/**
	 * @uml.property  name="column2"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	DefaultListModel column2;


	public tupvalmodel(){
		column1 = new DefaultListModel();
		column2 = new DefaultListModel();
		
	}
	public tupvalmodel(DefaultListModel c1, DefaultListModel c2){
		column1 = c1;
		column2 = c2;		
	}
	public int getColumnCount(){
		return 2;
	}
	public int getRowCount(){
		return column1.size();
	}
	public Object getValueAt(int rowIndex,int columnIndex){

		if(rowIndex >= 0 && rowIndex <= getRowCount()){
			switch(columnIndex){
			case 0: return column1.elementAt(rowIndex);
			case 1: return column2.elementAt(rowIndex);
			default: break;
			}
		}
		return null;

	}

	public void setValueAt(Object avalue, int rowIndex,int columnIndex){

		if(rowIndex >= 0 && rowIndex <= getRowCount()){
			switch(columnIndex){
			case 0: break;
			case 1:  column2.setElementAt(avalue, rowIndex) ;
			default: break;
			}
		}
		this.fireTableCellUpdated(rowIndex, columnIndex);
	}
	public boolean isCellEditable(int rowIndex,int columnIndex){
		if(rowIndex >= 0 && rowIndex <= getRowCount()){
			switch(columnIndex){
			case 0: return false;
			case 1:  return true;
			default: break;
			}
		}
		return false;
	}
	public void remove(int rowIndex){
		if(rowIndex >= 0 && rowIndex <= getRowCount()){
			column1.remove(rowIndex);
			column2.remove(rowIndex);
			}
		this.fireTableDataChanged();

	}
	@Override
	public void tableChanged(TableModelEvent e) {
		// TODO Auto-generated method stub
		
	}

}
