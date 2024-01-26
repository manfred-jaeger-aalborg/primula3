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

import RBNpackage.*;
import RBNgui.*;
import RBNExceptions.*;
import RBNutilities.SmallDouble;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;

public class SampleThread extends Thread{

	private boolean running;
	SampleProbs sprobs;
	private boolean pause;
	private int queryAtomSize;
	private int num_subsamples;
	double[] test;
	double tal = 0;
	private PFNetwork pfn;
	boolean[] logmode;
	BufferedWriter logwriter;
	private int numsamp = 0; // number of current sample
	private int subsind = 0; // index of current subsample

	long time;
	long newtime;

	// FOR GNN INTERACTION the jep library needs to be on the same thread
	// the jep object then will be shared across all the probforms that need it
	// probably only one jep object can be created at time --> close it when it is not needed anymore
	private GnnPy gnnPy;
	private boolean gnnIntegration;
	public SampleThread(Observer infmodule, 
			PFNetwork pfn, 
			GroundAtomList queryatoms, 
			int num_subsamples_param,
			boolean[] logmode_param,
			BufferedWriter logwriter_param){

		running = true;
		this.queryAtomSize = queryatoms.allAtoms().size();
		this.pfn = pfn;
		logmode = logmode_param;
		num_subsamples = num_subsamples_param;
		logwriter = logwriter_param;
		sprobs = new SampleProbs(queryAtomSize);
		sprobs.addObserver(infmodule);
		pause = false;
		test = new double[queryAtomSize];
		this.gnnIntegration = true;
	}

	public void run()
	{
		// if we use the python-java interface we create the object
		// this variable needs to be defined apriori
		// the jep object needs to be in the same thread
		if (this.gnnIntegration) {
			String modelPath = "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python/primula-gnn";
			String scriptPath = "/Users/lz50rg/Dev/GNN-RBN-workspace/GNN-RBN-reasoning/python";
			String scriptName = "inference_test";

			String jepPath = "/Users/lz50rg/miniconda3/envs/torch/lib/python3.10/site-packages/jep/libjep.jnilib";
			String pythonHome = "/Users/lz50rg/miniconda3/envs/torch/bin/python";
			this.gnnPy = new GnnPy(modelPath, scriptPath, scriptName, jepPath, pythonHome);
			pfn.setGnnPy(this.gnnPy);
		}
		else {
			this.gnnPy = null;
		}

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
		this.closeGnnIntepreter();
	}

	public void closeGnnIntepreter() {
		this.gnnPy.closeInterpreter();
	}
	public void setRunning(boolean running){
		this.running = running;
	}

	public void setPause(boolean pause){
		this.pause = pause;
	}

	public SampleProbs getSprobs() {
		return sprobs;
	}
}
