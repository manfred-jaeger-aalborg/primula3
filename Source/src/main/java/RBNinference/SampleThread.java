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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SampleThread extends Thread{

	private final AtomicBoolean running = new AtomicBoolean(true);
	private final AtomicBoolean pause = new AtomicBoolean(false);
	private final ReentrantLock pauseLock = new ReentrantLock();
	private final Condition isNotPaused = pauseLock.newCondition();

	SampleProbs sprobs;
	private int num_subsamples;
	private PFNetwork pfn;
	boolean[] logmode;
	BufferedWriter logwriter;
	private volatile int numsamp = 0; // number of current sample
	private int subsind = 0; // index of current subsample


	private InferenceModuleGUI infmoduleGUI;
	private InferenceModule inferenceModule;
	public SampleThread(InferenceModule infmodule,
			PFNetwork pfn,
			HashMap<Rel,GroundAtomList> queryatoms,
//			int num_subsamples_param,
			boolean[] logmode_param,
			BufferedWriter logwriter_param){

		inferenceModule = infmodule;
		this.pfn = pfn;
		logmode = logmode_param;
		num_subsamples = pfn.getNum_subsamples_minmax();
		logwriter = logwriter_param;
		sprobs = new SampleProbs(queryatoms);
		if (infmodule.getInferenceModuleGUI() != null)
			sprobs.addObserver(infmodule.getInferenceModuleGUI());
	}

	public void run() {
		long lastUpdateTime = System.currentTimeMillis();

		try {
			while (running.get()) {
				checkPause();

				pfn.sampleInst(subsind, false);
				numsamp++;
				subsind = (subsind < num_subsamples - 1) ? subsind + 1 : 0;

				long currentTime = System.currentTimeMillis();
				if (currentTime - lastUpdateTime > 1000) {
					performUpdate();
					lastUpdateTime = currentTime;
				}
				Thread.yield();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			System.err.println("Sample Thread encountered error: " + e.getMessage());
		} finally {
			performUpdate();
		}
	}

	private void checkPause() throws InterruptedException {
		if (pause.get()) {
			pauseLock.lock();
			try {
				while (pause.get() && running.get()) {
					isNotPaused.await();
				}
			} finally {
				pauseLock.unlock();
			}
		}
	}

	private void performUpdate() {
		try {
			if (logwriter != null && (logmode[2] || logmode[3])) {
				logwriter.write(numsamp + " ");
			}
			pfn.setSampleProbs(sprobs, num_subsamples, logwriter);
			sprobs.setSize(numsamp);
			sprobs.setWeight(RBNutilities.SmallDouble.toStandardDouble(RBNutilities.SmallDouble.divide(pfn.allsampleweight(), numsamp)));
			sprobs.notifyObservers();
		} catch (Exception e) {
			System.err.println("Update failed: " + e.getMessage());
		}
	}

	public void setRunning(boolean isRunning) {
		this.running.set(isRunning);
		if (!isRunning) {
			this.interrupt();
			resumeThread();
		}
	}

	public void setPause(boolean isPaused) {
		this.pause.set(isPaused);
		if (!isPaused) {
			resumeThread();
		}
	}

	private void resumeThread() {
		pauseLock.lock();
		try {
			isNotPaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}

	public int getNumsamp() {
		return numsamp;
	}

	public SampleProbs getSprobs() {
		return sprobs;
	}
}
