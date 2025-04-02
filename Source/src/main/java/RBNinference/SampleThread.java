/*
 * SampleThread.java
 * 
 * Copyright (C) 2005 Aalborg University
 *
 * contact:
 * jaeger@cs.auc.dk    www.cs.auc.dk/~jaeger/Primula.html
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


package RBNinference;

import PyManager.GnnPy;
import RBNpackage.*;
import RBNgui.*;
import RBNExceptions.*;
import RBNutilities.SmallDouble;

import java.util.*;
import java.io.*;

public class SampleThread extends Thread{

	private volatile boolean running;
	SampleProbs sprobs;
	private boolean pause;
//	private int queryAtomSize;
	private int num_subsamples;
//	double[] test;
	double tal = 0;
	private PFNetwork pfn;
	boolean[] logmode;
	BufferedWriter logwriter;
	private volatile int numsamp = 0; // number of current sample
	private int subsind = 0; // index of current subsample

	long time;
	long newtime;

	// FOR GNN INTERACTION the jep library needs to be on the same thread
	// the jep object then will be shared across all the probforms that need it
	// probably only one jep object can be created at time --> close it when it is not needed anymore
	private GnnPy gnnPy;
	private boolean gnnIntegration;
	private String modelPath;
	private String scriptPath;
	private String scriptName;
	private String pythonHome;
	private InferenceModuleGUI infmoduleGUI;
	private InferenceModule inferenceModule;
	public SampleThread(InferenceModule infmodule,
			PFNetwork pfn,
			Hashtable<Rel,GroundAtomList> queryatoms,
//			int num_subsamples_param,
			boolean[] logmode_param,
			BufferedWriter logwriter_param){

		running = true;
		inferenceModule = infmodule;
//		this.queryAtomSize = queryatoms.allAtoms().size();
		this.pfn = pfn;
		logmode = logmode_param;
		num_subsamples = pfn.getNum_subsamples_minmax();
		logwriter = logwriter_param;
		sprobs = new SampleProbs(queryatoms);
		if (infmodule.getInferenceModuleGUI() != null)
			sprobs.addObserver(infmodule.getInferenceModuleGUI());
		pause = false;
//		test = new double[queryAtomSize];
//        this.gnnIntegration = this.pfn.checkGnnRel();
	}

	public void run()
	{
		// if we use the python-java interface we create the object
		// this variable needs to be defined apriori
		// the jep object needs to be in the same thread
//		if (this.gnnIntegration) {
//			try {
//				this.gnnPy = new GnnPy(scriptPath, scriptName, pythonHome);
//				pfn.setGnnPy(this.gnnPy);
//
//				this.gnnPy = new GnnPy(inferenceModule.getPrimula());
//				pfn.setGnnPy(this.gnnPy);
//				gnnPy.load_gnn_set(inferenceModule.getPrimula().getLoadGnnSet());
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//		}
//		else {
//			this.gnnPy = null;
//		}

		time = System.currentTimeMillis();
		while(running){
			try{
				while(pause){
					Thread.sleep((int)(Math.random()*3));	
				}				
				Thread.sleep((int)(Math.random()*3));				
			}
			catch(InterruptedException e){
				System.err.println(e.toString());
			}

			try{
				pfn.sampleInst(subsind,false);
			}
			catch (RBNCompatibilityException e){System.out.println(e);}
			catch (RBNInconsistentEvidenceException e){System.out.println(e);}
			
			numsamp++;
			if (subsind < num_subsamples-1) subsind++;
			else subsind = 0;

			newtime = System.currentTimeMillis();
			if(newtime - time > 2000 || running == false){
				time = newtime;
				try{
					if (logwriter != null && (logmode[2] || logmode[3]))
						logwriter.write(numsamp + " ");
					pfn.setSampleProbs(sprobs,num_subsamples,logwriter);
				}
				catch (java.io.IOException ex){System.out.println(ex);};
				sprobs.setSize(numsamp);
				sprobs.setWeight(SmallDouble.toStandardDouble(
						SmallDouble.divide(pfn.allsampleweight(),numsamp)));
				sprobs.notifyObservers();
			}

		}
		// the interpreter needs to be closed from the same thread
//		if (this.gnnIntegration)
//			this.closeGnnIntepreter();
		this.gnnPy = null;
	}

	public int getNumsamp() {
		return numsamp;
	}

//	public void closeGnnIntepreter() {
//		this.gnnPy.closeInterpreter();
//	}
	public void setRunning(boolean running){
		this.running = running;
	}

	public void setPause(boolean pause){
		this.pause = pause;
	}

	public SampleProbs getSprobs() {
		return sprobs;
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

	public void setInfmoduleGUI(InferenceModuleGUI infmoduleGUI) { this.infmoduleGUI = infmoduleGUI; }
}
