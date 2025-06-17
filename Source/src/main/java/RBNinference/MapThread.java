package RBNinference;

import java.io.*;
import java.util.*;

import PyManager.GnnPy;
import PyManager.JepManager;
import RBNExceptions.RBNNaNException;
import RBNLearning.*;
import RBNgui.*;
import RBNpackage.*;
import jep.SharedInterpreter;

public class MapThread extends GGThread {
	
	Primula myprimula;
	InferenceModule myinfmodule;
	LearnModule myLearnModule;
	GradientGraphO gg;
	MapVals mapprobs;
	boolean running;
    private GnnPy gnnPy;
//    private final boolean gnnIntegration;
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

//        this.gnnIntegration = this.checkGnnRel(this.myprimula.getRBN());
        this.isSampling = false;
	}

	public void run(){
        this.isSampling = true;
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

		Hashtable<Rel,int[]> newmapvals = new Hashtable<>();
		
		int maxrestarts = myinfmodule.getMAPRestarts();
		int restarts =1;
		double oldll=Double.NEGATIVE_INFINITY;
		double newll=0;
		OneStrucData onsd = new OneStrucData(myprimula.getInstantiation());

		while (running && ((maxrestarts == -1) || (restarts <= maxrestarts))) {
			try {
				newll = gg.mapInference(this);
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

						mapprobs.setRestarts(restarts);
						mapprobs.notifyObservers();
						OneStrucData result = new OneStrucData();
						result.setParentRelStruc(myprimula.getRels());

						for (Rel key : bestMapVals.keySet()) {
							GroundAtomList gal = gg.mapatoms(key);
							for (int i = 0; i < gal.size(); i++) {
								result.add(gal.atomAt(i), bestMapVals.get(key)[i], "?");
							}
						}

						onsd.add(result);
						restarts++;
					}
				} else
					System.out.println("MAP search aborted");
			} catch (RBNNaNException e) {
				throw new RuntimeException(e);
			}
		}

		System.out.println("Best log-likelihood found: " + oldll);

		// save res as pickle
		String path = "/Users/lz50rg/Dev/football/res.pkl";
		// createInputs(onsd, path);

        this.isSampling = false;
	}

	public void setRunning(boolean r){
		this.running = r;
	}

	public void createInputs(OneStrucData a, String path) {
		OneStrucData data = new OneStrucData(myprimula.getRels().getmydata().copy()); // only one copy per time
		SparseRelStruc sampledRel = new SparseRelStruc(myprimula.getRels().getNames(), data, myprimula.getRels().getCoords(), myprimula.getRels().signature());
		sampledRel.getmydata().add(a.copy());

		for(int i=0; i<myprimula.getRBN().prelements().length; i++) {
			if (myprimula.getRBN().cpmod_prelements_At(i) instanceof CatGnn) {
				CatGnn cpm = (CatGnn) myprimula.getRBN().cpmod_prelements_At(i);
				Map<Rel, int[][]> nodesDict = GnnPy.constructNodesDict(cpm, myprimula.getRels());
				Map<Integer, Integer> nodeMap = GnnPy.constructNodesDictMap(cpm, myprimula.getRels());

				Vector<BoolRel> boolRels = sampledRel.getBoolBinaryRelations();
				Map<String, double[][]> x_dict = GnnPy.inputAttrToDict(cpm, nodeMap, nodesDict, sampledRel);
				Map<String, ArrayList<ArrayList<Integer>>> edge_dict = GnnPy.edgesToDict(boolRels, sampledRel, nodeMap);

				savePickleGraph(x_dict, edge_dict, path);
			}
		}

	}
	public void savePickleHetero(Map<String, double[][]> xDict, Map<String, ArrayList<ArrayList<Integer>>> edgeDict, String path) {
		SharedInterpreter interpreter = JepManager.getInterpreter(true);
		interpreter.set("java_map_x", xDict);
		interpreter.set("java_map_edge", edgeDict);

		interpreter.exec(
				"import pickle\n" +
						"data_h = HeteroData()\n" +

						"for key, value in java_map_x.items():\n" +
						"    data_h[key].x = torch.as_tensor(value, dtype=torch.float32)\n" +

						"for key, value in java_map_edge.items():\n" +
						"    n_key = key.split('_to_')\n" + // here the key must have the form type_to_type
						"    if len(value) > 0:\n" +
						"        data_h[n_key[0], 'to', n_key[1]].edge_index = torch.as_tensor(value, dtype=torch.long)\n" +
						"    else:\n" +
						"        data_h[n_key[0], 'to', n_key[1]].edge_index = torch.empty((2, 0), dtype=torch.long)\n" +

						"with open('" + path + "', 'wb') as f:\n" +
						"    pickle.dump(data_h, f)\n"
		);
		System.out.println("Pickle written in: " + path);
	}

	public void savePickleGraph(Map<String, double[][]> xDict, Map<String, ArrayList<ArrayList<Integer>>> edgeDict, String path) {
		SharedInterpreter interpreter = JepManager.getInterpreter(true);
		interpreter.set("java_map_x", xDict);
		interpreter.set("java_map_edge", edgeDict);

		interpreter.exec(
				"import pickle\n" +
					"import torch\n" +
					"from torch_geometric.data import Data\n" +
					"data = Data()\n" +
					"if len(java_map_x) > 0:\n" +
					"    key = list(java_map_x.keys())[0]\n" +
					"    data.x = torch.as_tensor(java_map_x[key], dtype=torch.float32)\n" +

					"if len(java_map_edge) > 0:\n" +
					"    key = list(java_map_edge.keys())[0]\n" +
					"    value = java_map_edge[key]\n" +
					"    if value and len(value) > 0 and len(value[0]) > 0:\n" +
					"        data.edge_index = torch.as_tensor(value, dtype=torch.long)\n" +
					"    else:\n" +
					"        data.edge_index = torch.empty((2, 0), dtype=torch.long)\n" +
					"\n" +
					"with open('" + path + "', 'wb') as f:\n" +
					"    pickle.dump(data, f)\n"
		);
		System.out.println("Pickle written in: " + path);
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
