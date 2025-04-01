package RBNinference;

public class GGThread extends Thread {
	
	private boolean isstopped = false;
	
	public void setStopped(){
		isstopped = true;
	}
	
	public boolean isstopped(){
		return isstopped;
	}

}
