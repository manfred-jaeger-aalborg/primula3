package RBNinference;

import java.io.*;
import java.util.*;

import RBNExceptions.RBNNaNException;
import RBNLearning.*;
import RBNgui.*;
import RBNpackage.*;

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

    public boolean isSampling;
	public MapThread(InferenceModule infmodule,
			Primula mypr,
			GradientGraphO ggarg){
		myinfmodule = infmodule;
		myprimula = mypr;
		gg = ggarg;
		mapprobs = new MapVals(infmodule.getQueryatoms());
		if (infmodule.getInferenceModuleGUI() != null)
			mapprobs.addObserver(infmodule.getInferenceModuleGUI());

        this.gnnIntegration = this.checkGnnRel(this.myprimula.getRBN());
        this.isSampling = false;
	}

	public void run(){
        this.isSampling = true;
        if (this.gnnIntegration) {
            try {
                this.gnnPy = new GnnPy(myprimula, gg);
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
			if (myprimula.getPrimulaGUI() != null) {
				myLearnModule = myprimula.getPrimulaGUI().openLearnModule(true);
				myLearnModule.disableDataTab();
				myLearnModule.setParameters(gg.parameters());
				gg.setLearnModule(myLearnModule);
			}
		}

		gg.setNumIterGreedyMap(myinfmodule.getNumIterGreedyMap());

		Hashtable<Rel,int[]> newmapvals = new Hashtable<>();

		Map<String, double[][]> xDict = new HashMap<>();
		Map<String, ArrayList<ArrayList<Integer>>> edgeDict = new HashMap<>();
		
		int maxrestarts = myinfmodule.getMAPRestarts();
		int restarts =1;
		double oldll=Double.NEGATIVE_INFINITY;
		double newll=0;
		while (running && ((maxrestarts == -1) || (restarts <= maxrestarts))) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter("/Users/lz50rg/Dev/water-hawqs/results/txt_graph_" + Experiments.Water.RiverPollution.EXPNUM + "_" + restarts + ".txt", "UTF-8");
				newll = gg.mapInference(this);
				writer.println("Restart: " + restarts);
				writer.println("logll" + newll);
				if (!Double.isNaN(newll)) {
					if (newll > oldll) {
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
				} else
					System.out.println("MAP search aborted");

				mapprobs.setRestarts(restarts);
				mapprobs.notifyObservers();

				OneStrucData result = new OneStrucData();
				result.setParentRelStruc(myprimula.getRels());
				OneStrucData onsd = new OneStrucData(myprimula.getInstantiation());
				for (Rel key : bestMapVals.keySet()) {
					GroundAtomList gal = gg.mapatoms(key);
					for (int i = 0; i < gal.size(); i++) {
						writer.println(gal.atomAt(i).args()[0] + " : " + bestMapVals.get(key)[i]);
						result.add(gal.atomAt(i), bestMapVals.get(key)[i], "?");
					}
				}
				onsd.add(result);
				onsd.saveToRDEF(new File("/Users/lz50rg/Dev/water-hawqs/results/redef_graph_" + Experiments.Water.RiverPollution.EXPNUM + "_" + restarts + ".rdef"), myprimula.getRels());
				writer.close();
				restarts++;
			} catch (RBNNaNException e) {
				System.out.println(e);
				System.out.println("Restart aborted");
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		System.out.println("Best log-likelihood found: " + oldll);

		// path.pkl
//		String path = "/Users/lz50rg/Dev/football/res.pkl";
//		String path = "/Users/lz50rg/Dev/water-hawqs/map-results.pkl";
//		if (gnnPy != null) {
//			gnnPy.savePickleHetero(xDict, edgeDict, path);
////			gnnPy.savePickleGraph(xDict, edgeDict, path);
//		}

        if (this.gnnIntegration)
			this.gnnPy.closeInterpreter();

		this.gnnPy = null;
        this.isSampling = false;
    }

	public void setRunning(boolean r){
		this.running = r;
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
