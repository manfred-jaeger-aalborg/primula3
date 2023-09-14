package RBNinference;

import java.util.*;

import RBNExceptions.RBNNaNException;
import RBNLearning.*;
import RBNutilities.*;
import RBNgui.InferenceModule;
import RBNgui.LearnModule;
import RBNgui.Primula;

public class MapThread extends GGThread {
	
	Primula myprimula;
	InferenceModule myinfmodule;
	LearnModule myLearnModule;
	GradientGraphO gg;
	MapVals mapprobs;
	boolean running;
	
	public MapThread(InferenceModule infmodule,
			Primula mypr,
			GradientGraphO ggarg){
		myinfmodule = infmodule;
		myprimula=mypr;
		gg = ggarg;
		mapprobs = new MapVals(gg.maxatoms().size());
		mapprobs.addObserver(infmodule);
	}
	
	public void run(){
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
		
		int[] lastmapvals=new int[gg.getMapVals().length];
		/* Make sure initial values are not equal to result of 
		 * first iteration: */
		for (int i=0;i<lastmapvals.length;i++)
			lastmapvals[i]=-1;
		int[] newmapvals;
		
		int maxrestarts = myinfmodule.getMAPRestarts();
		int restarts =1;
		double[] oldll=new double[2];
		double[] newll;

		while (running && ((maxrestarts == -1) || (restarts <= maxrestarts))){
			try {
				newll = gg.mapInference(this);	
				if (SmallDouble.compareSD(newll,oldll)==1){
					oldll=newll;
					newmapvals = gg.getMapVals();
					mapprobs.setMV(newmapvals);
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
			mapprobs.setRestarts(restarts);
			mapprobs.notifyObservers();
			restarts++;
		}
	}

	public void setRunning(boolean r){
		this.running = r;
	}

}
