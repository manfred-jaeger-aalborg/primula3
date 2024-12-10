package RBNinference;

import java.util.*;

import RBNExceptions.RBNNaNException;
import RBNLearning.*;
import RBNpackage.*;
import RBNgui.InferenceModule;
import RBNgui.LearnModule;
import RBNgui.Primula;

import java.io.IOException;

public class MapThread extends GGThread {
	
	Primula myprimula;
	InferenceModule myinfmodule;
	LearnModule myLearnModule;
	GradientGraphO gg;
	MapVals mapprobs;
	boolean running;
    private GnnPy gnnPy;
    private final boolean gnnIntegration;
	private Hashtable<Rel,int[]> bestMapVals;
	private double[] bestLikelihood;
    private String modelPath;
    private String scriptPath;
    private String scriptName;
    private String pythonHome;

    public boolean isSampling;
	public MapThread(
            Observer valueObserver,
            InferenceModule infmodule,
			Primula mypr,
			GradientGraphO ggarg){
		myinfmodule = infmodule;
		myprimula = mypr;
		gg = ggarg;
		mapprobs = new MapVals(infmodule.getQueryatoms());
		mapprobs.addObserver(infmodule);

        this.gnnIntegration = this.checkGnnRel(this.myprimula.getRBN());
        this.isSampling = false;
	}

	public void run(){
        this.isSampling = true;
        if (this.gnnIntegration) {
            try {
                this.gnnPy = new GnnPy(this.scriptPath, this.scriptName, this.pythonHome);
                gg.setGnnPy(this.gnnPy);
				gg.load_gnn_settings(myprimula.getLoadGnnSet());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else
            this.gnnPy = null;

		running = true;
		
		/* Open a LearnModule to monitor parameter values, if
		 * there are any free parameters in the model
		 */
		
		if (gg.parameters().size() > 0){
			myLearnModule = myprimula.openLearnModule(true);			
			myLearnModule.disableDataTab();
			myLearnModule.setParameters(gg.parameters());
			gg.setLearnModule(myLearnModule);
		}

		gg.setNumIterGreedyMap(myinfmodule.getNumIterGreedyMap());

		Hashtable<Rel,int[]> newmapvals = new Hashtable<>();

		Map<String, double[][]> xDict = new HashMap<>();
		Map<String, int[][]> edgeDict = new HashMap<>();
		
		int maxrestarts = myinfmodule.getMAPRestarts();
		int restarts =1;
		double oldll=Double.NEGATIVE_INFINITY;
		double newll=0;

		while (running && ((maxrestarts == -1) || (restarts <= maxrestarts))){
			try {
                System.out.println("Current restart: " + restarts);
				newll = gg.mapInference(this);
				if (!Double.isNaN(newll)) {
					if (newll>oldll) {
						oldll = newll;
						newmapvals = gg.getMapVals();
						bestMapVals = newmapvals;
						bestLikelihood = new double[]{newll};
						mapprobs.setMVs(newmapvals);
						mapprobs.setLL(String.valueOf(oldll));
						if (gg.parameters().size() > 0)
							myLearnModule.setParameterValues(gg.getParameters());
						if (gnnPy != null) {
							xDict = gnnPy.getCurrentXdict();
							edgeDict = gnnPy.getCurrentEdgeDict();
						}
					}
				} else {
					System.out.println("MAP search aborted");
				}
			}
			catch (RBNNaNException e) 
			{
				System.out.println(e);
				System.out.println("Restart aborted");
			}
			mapprobs.setRestarts(restarts);
			mapprobs.notifyObservers();
			restarts++;
		}

		System.out.println("Best log-likelihood found: " + oldll);

//		if (gnnPy != null) {
//			gnnPy.savePickleHetero(xDict, edgeDict);
//		}

        if (this.gnnIntegration)
			this.gnnPy.closeInterpreter();

        this.isSampling = false;
    }

	public void setRunning(boolean r){
		this.running = r;
	}

	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}

	public void setScriptPath(String scriptPath) {
		this.scriptPath = scriptPath;
	}

	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
	}

	public void setPythonHome(String pythonHome) {
		this.pythonHome = pythonHome;
	}

	public boolean isGnnIntegration() {
		return gnnIntegration;
	}

	private boolean checkGnnRel(RBN rbn) {
		for(int i=0; i<rbn.prelements().length; i++) {
			if (rbn.cpmod_prelements_At(i) instanceof CPMGnn)
				return true;
		}
		return false;
	}

	public void setGg(GradientGraphO gg) {
		this.gg = gg;
	}

	public GnnPy getGnnPy() {
		return gnnPy;
	}

	public MapVals getMapprobs() {
		return mapprobs;
	}

	public boolean getRunning() {
		return this.running;
	}

	public Hashtable<Rel, int[]> getBestMapVals() {
		return bestMapVals;
	}

	public double[] getBestLikelihood() {
		return bestLikelihood;
	}
}
