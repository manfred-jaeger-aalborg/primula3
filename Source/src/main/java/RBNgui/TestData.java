package RBNgui;

import java.util.*;

class TestData extends Observable{
  /**
 * @uml.property  name="tal" multiplicity="(0 -1)" dimension="1"
 */
private int[] tal;
	
 public TestData(){ }

	public void setData(int[] tal){
		this.tal = tal;
		setChanged();
	}
	
	public int[] getData(){
		return tal;
	}
}
