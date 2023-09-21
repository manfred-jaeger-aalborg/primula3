package RBNgui;


/* Interface defining some necessary parameters for the construction and
 * use of a Gradient Graph. 
 * 
 * Interface is implemented by LearnModule and InferenceModule.
 * 
 * A gradient graph is always constructed either through a LearnModule
 * or an InferenceModule. Many of the parameters are only meaningful
 * in the context of either learning or inference. The implementing classes 
 * for which a certain parameter is not relevant then just has to return
 * a default value.
 */
public interface GradientGraphOptions {
	
	public abstract int getNumChains();
	
	public abstract int getWindowSize();
	
	public abstract boolean aca();

	public abstract int getMaxFails();
	
	public abstract boolean learnverbose();
	
	public abstract boolean ggrandominit();
	
	public abstract boolean gguse2phase();
	
	public abstract int threadascentstrategy();
	
	public abstract int ggascentstrategy();
	
	public abstract int lbfgsmemory();
	
	public abstract int getMaxIterations();
	
	public abstract double getLLikThresh();
	
	public abstract double getLineDistThresh();
	
	public abstract double adagradepsilon();
	
	public abstract double adagradfade();
}
