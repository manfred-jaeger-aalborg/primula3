package RBNinference;

import java.util.*;

import RBNExceptions.RBNNaNException;
import RBNLearning.*;
import RBNutilities.*;
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
    private String modelPath;
    private String scriptPath;
    private String scriptName;
    private String pythonHome;

    public boolean isSampling;
	public MapThread(
            Observer infmoduleObs,
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            this.gnnPy = null;
        }

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
		
		//int[] lastmapvals=new int[gg.getMapVals().length];
		/* Make sure initial values are not equal to result of
		 * first iteration: */
		//for (int i=0;i<lastmapvals.length;i++)
		//	lastmapvals[i]=-1;
		Hashtable<Rel,int[]> newmapvals;
		
		int maxrestarts = myinfmodule.getMAPRestarts();
		int restarts =1;
		double[] oldll=new double[2];
		double[] newll;

        /**
         * SAVE LIKELIHOOD ON FILE
         *
         * File filell = new File("/Users/lz50rg/Desktop/ll3.txt");
         * 		FileWriter fileWriter = null;
         * 		try {
         * 			fileWriter = new FileWriter(filell, true);
         *                } catch (IOException e) {
         * 			throw new RuntimeException(e);
         *        }
         * 		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
         */

		while (running && ((maxrestarts == -1) || (restarts <= maxrestarts))){
			try {
                System.out.println("Current restart: " + restarts);
				newll = gg.mapInference(this);
				if (SmallDouble.compareSD(newll,oldll)==1){
					oldll=newll;
					newmapvals = gg.getMapVals();
					mapprobs.setMVs(newmapvals);
					mapprobs.setLL(SmallDouble.asString(oldll));
					if (gg.parameters().size() > 0)
						myLearnModule.setParameterValues(gg.getParameters());
				}
			}
			catch (RBNNaNException e) 
			{
				System.out.println(e);
				System.out.println("Restart aborted");
			}
            /**
             * catch (IOException e) {
             * 		throw new RuntimeException(e);
             * }
             */
			mapprobs.setRestarts(restarts);
			mapprobs.notifyObservers();
			restarts++;
		}

		System.out.println("Best likelihood found: " + SmallDouble.toStandardDouble(oldll));
		System.out.println("Best combination found: ");
		for (Rel r: gg.getMaxindicators().keySet()) {
			for (GGAtomMaxNode nextgimn: gg.getMaxindicators().get(r))
				System.out.println(nextgimn.getMyatom() + ": " + nextgimn.getCurrentInst());
		}

        if (this.gnnIntegration)
            this.gnnPy.closeInterpreter();
        this.isSampling = false;

        /**
         try {
         bufferedWriter.close();
         fileWriter.close();
         } catch (IOException e) {
         throw new RuntimeException(e);
         }*/

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
}
