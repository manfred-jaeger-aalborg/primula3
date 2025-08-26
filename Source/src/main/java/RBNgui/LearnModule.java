/*
* LearnModule.java 
* 
* Copyright (C) 2009 Aalborg University
*
* contact:
* jaeger@cs.aau.dk   http://www.cs.aau.dk/~jaeger/Primula.html
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

package RBNgui;

import RBNpackage.*;
import RBNinference.*;
import RBNExceptions.*;
import RBNio.*;
import RBNLearning.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;


public class LearnModule implements GradientGraphOptions
{
//	public static final int UseLik = 0;
//	public static final int UseLogLik = 1;
//	public static final int UseSquaredError = 2;
	
	/* Options for the top-level learning strategy in LearnThread:*/
	public static final int AscentBatch =0;
	/* Ref: D. P. Kingma and J. L. Ba: ADAM: A Method for Stochastic Optimization
	 * arXiv: 1412.6980v8
	 */
	public static final int AscentAdam =1;
	public static final int AscentStochHeur1 =2;
	public static final int AscentStochHeur2 =3;
	public static final int AscentBlock =4;
	public static final int AscentTwoPhase =5;
	
	/* Options for a single parameter learning/update call to GradientGraph: */
	public static final int AscentLBFGS =0;
	public static final int AscentAdagrad =1;
	public static final int AscentFletcherReeves =2;
	public static final int AscentDirectGradient =3;
	
	public static final String threadstrategy[] = {"Batch","Adam"};
	public static final String ggstrategy[] = {"LBFGS","Adagrad","FletcherReeves","Greedy"};

	private ParameterTableModel parammodel = new ParameterTableModel();

	private DefaultListModel numRelListModel = new DefaultListModel();

	/**
	 * @uml.property  name="myprimula"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="learnModule:RBNgui.Primula"
	 */
	Primula myprimula;
	PrimulaGUI myprimulaGUI;
	LearnModuleGUI learnModuleGUI;

	/**
	 * @uml.property  name="data"
	 * @uml.associationEnd  
	 */
	private RelData data;
	/**
	 * @uml.property  name="datafile"
	 */
//	private File datafile;

	/* Parameters for the data sampling process */
	/**
	 * @uml.property  name="samplesize"
	 */
	private int samplesize;
	/**
	 * @uml.property  name="percmiss"
	 */
	private double percmiss;
	/* Parameters for the learning process */
	/**
	 * @uml.property  name="numchains"
	 */
	private int numchains;
	/**
	 * @uml.property  name="restarts"
	 */
	private int restarts;
	
	/* Percentage by which to sub-sample false atoms */
	private int subsamples;
	
	/* Number of data mini-batches for stochastic gradient descent */
	private int numbatches;
	
	/* Type of datasplit RelData.SPLIT_BY_DOMAIN or RelData.SPLIT_ACROSS_DOMAINS */
	private int splitmode;
	
	/* Used in stochastic gradient descent */
	private double dampingfac;
	
	/* Number of parameter blocks for block gradient descent */
	private int numblocks;
	
	/**
	 * @uml.property  name="windowsize"
	 */
	private int windowsize;
	/**
	 * @uml.property  name="maxfails"
	 */
	private int maxfails;
	/**
	 * @uml.property  name="maxiterations"
	 */
	private int maxiterations;
	/**
	 * @uml.property  name="linedistancethresh"
	 */
	private double linedistancethresh;
	/**
	 * @uml.property  name="linelikelihoodthresh"
	 */
//	private double linelikelihoodthresh;
	private int likelihoodwindow;
	private double llikhoodthresh;
	/**
	 * @uml.property  name="gradientdistancethresh"
	 */
	private double gradientdistancethresh;
	/**
	 * @uml.property  name="paramratiothresh"
	 */
	private double paramratiothresh;
	/**
	 * @uml.property  name="omitrounds"
	 */
	private int omitrounds;
	/**
	 * @uml.property  name="verbose"
	 */
	private boolean useggs;
	
	private boolean usememoize;
	
	private boolean learnverbose;
	private boolean ggrandominit;
	private boolean gg2phase;
	
	private boolean numrelsfromfile;
	private String[][] numrelblocks;
	
	protected int ggascentstrategy;
	protected int threadascentstrategy;
	protected int lbfgsmemory;
	
	double adagradfade;
	double adagradepsilon;
	double adam_beta1;
	double adam_beta2;
	double adam_epsilon;
	double adam_alpha;
	
	private int type_of_gradient; // one of ProbForm.RETURN_ARRAY or  ProbForm.RETURN_SPARSE
	private boolean aca;
	LearnThread lthread;

	//private Vector<String> selectednumrels;
	
	public LearnModule(Primula mypr){

		myprimula = mypr;
		data = myprimula.getReldata();
		threadascentstrategy = AscentAdam;
		ggascentstrategy = AscentLBFGS;
		
		lbfgsmemory = 10;

		samplesize = 1;
		restarts = 1; /*-1 is for open-ended restarts */
		subsamples = 100;
		numblocks = 1;
		numbatches =50;
		splitmode = RelData.SPLIT_ACROSS_DOMAINS;
		dampingfac =0.99;
		numchains = 10;
//		windowsize = 20;
		maxfails = 5;
		maxiterations = 200;
		linedistancethresh = 0.0001;
//		linelikelihoodthresh = 0.001;
		likelihoodwindow = 5;
		llikhoodthresh = 0.00005;
		gradientdistancethresh = 0.001;
		paramratiothresh = 0.0;
		omitrounds = 3;
		percmiss = 0.0;
		learnverbose = true;
		gg2phase = false;
		ggrandominit = true;
		numrelsfromfile = false;
		aca = false;
		readNumRels();
		useggs=false;
		usememoize=false;
		
		//selectednumrels = new Vector<String>();
		type_of_gradient=ProbForm.RETURN_SPARSE;
		
		adagradfade = 0.5;
		adagradepsilon = 1.0E-10;
		adam_beta1=0.9;
		adam_beta2=0.999;
		adam_epsilon = 1.0E-8;
		adam_alpha = 0.01;
	}

	public void setLearnSampleSize(Integer lss){
		numchains = lss;
	}

	public void setWindowSize(Integer gr){
		windowsize = gr;
	}

	public void setMaxIterations(Integer mi){
		maxiterations = mi;
	}
	
	public void setMaxFails(Integer mf){
		maxfails = mf;
	}

	public void setLineDistThresh(double d){
		linedistancethresh = d;
	}

	public void setLLikThresh(double d){
		llikhoodthresh = d;
	}

	public void setLikelihoodWindow(int d){
		likelihoodwindow = d;
	}

	public void setGradDistThresh(double d){
		gradientdistancethresh = d;
	}

	public int getMaxIterations(){
		return maxiterations;
	}
	
	public int getMaxFails(){
		return maxfails;
	}

	public double getLineDistThresh(){
		return linedistancethresh;
	}

//	public double getLineLikThresh(){
//		return llikhoodthresh;
//	}

	public double getLikelihoodWindow(){
		return likelihoodwindow;
	}
	
	public double getLLikThresh(){
		return llikhoodthresh;
	}
	
	public double getGradDistThresh(){
		return gradientdistancethresh;
	}

	public int getWindowSize(){
		return likelihoodwindow;
	}

	public void setRestarts(Integer rs){
		restarts = rs;
	}

	public void setSubsamples(Integer rs){
		subsamples = rs;
	}

	public void setNumblocks(Integer nb){
		numblocks = nb;
	}

	public void setNumbatches(Integer nb){
		numbatches = nb;
	}

	public void setSplitmode(Integer sm){
		splitmode=sm;
	}

	public void setDampingFac(Double df){
		dampingfac = df;
	}

	public int getRestarts(){
		return restarts;
	}
	
	public int getSubsamples(){
		return subsamples;
	}
	
	public int getNumBatches(){
		return numbatches;
	}
	
	public int getSplitmode(){
		return splitmode;
	}

	public double getDampingFac(){
		return dampingfac;
	}
	
	public int getNumblocks(){
		return numblocks;
	}
	
	public boolean gguse2phase(){
		return gg2phase;
	}

	public int getNumChains(){
		return numchains;
	}

	public int getType_of_gradient() {
		return type_of_gradient;
	}
	
	public void setType_of_gradient(int t) {
		type_of_gradient =t;
	}

	/**
	 * @param v
	 * @uml.property  name="verbose"
	 */
	public void setVerbose(boolean v){
		learnverbose = v;
	}
	
	
	public void set2phase(boolean v){
		gg2phase = v;
	}
	
	public void setRandomInit(boolean v){
		ggrandominit = v;
	}
	
	public boolean learnverbose(){
		return learnverbose;
	}
	
	public boolean ggrandominit(){
		return ggrandominit;
	}
	
	/**
	 * @param v
	 * @uml.property  name="aca"
	 */
	public void setAca(boolean v){
		aca = v;
	}
	
	public void setUseGGs(boolean v){
		useggs = v;
	}
	
	public void setUseMemoize(boolean v){
		usememoize = v;
	}
	
	public boolean getUseGGs() {
		return useggs;
	}
	
	public boolean getUseMemoize() {
		return usememoize;
	}
	
	public boolean aca(){
		return aca;
	}

	/**
	 * @return
	 * @uml.property  name="paramratiothresh"
	 */
	public double getParamratiothresh(){
		return paramratiothresh;
	}
	
	public int omitRounds(){
		return omitrounds;
	}
	
	public void getRelDataFromPrimula(){
		data=myprimula.getReldata();
	}

	public void setParameters(Hashtable<String,Integer> params){
		parammodel.setParameters(params);
		if (this.learnModuleGUI!=null)
			this.learnModuleGUI.getParametertable().updateUI();
	}
	
	public void setParameterValues(double[] pvals, double ll){
		parammodel.setParameterEstimates(pvals, ll);
		if (this.learnModuleGUI!=null)
			this.learnModuleGUI.getParametertable().updateUI();
	}
	
	public void setParametersPrimula() {
		String[] pars = parammodel.getParameters();
		double[] vals = parammodel.getEstimates();
		/* Need to cut off last element, which is the likelihood: */
		myprimula.setParameters(Arrays.copyOfRange(pars,0,pars.length-1),
				         		Arrays.copyOfRange(vals,0,vals.length-1));
		myprimula.updateBavaria();
	}
	
	private void readNumRels(){
		if(myprimula.rels instanceof SparseRelStruc){
			SparseRelStruc sparserst = (SparseRelStruc)myprimula.rels;
			Vector<NumRel> nextrels;
			nextrels = sparserst.getNumGlobals();
			for (int i=0;i<nextrels.size();i++)
				numRelListModel.addElement(nextrels.elementAt(i).name());

			nextrels = sparserst.getNumAttributes();
			for (int i=0;i<nextrels.size();i++)
				numRelListModel.addElement(nextrels.elementAt(i).name());
			
			nextrels = sparserst.getNumBinaryRelations();
			for (int i=0;i<nextrels.size();i++)
				numRelListModel.addElement(nextrels.elementAt(i).name());
			
			nextrels = sparserst.getNumArbitraryRelations();
			for (int i=0;i<nextrels.size();i++)
				numRelListModel.addElement(nextrels.elementAt(i).name());
			}
			
	}

	
	/* Transforms the data as follows:
	 * For all (Boolean) probabilistic relations with default value 'false'
	 * Default value is turned into '?', and pc% of the atoms that had
	 * 'false' values according to the default are explicitly added as 
	 * 'false' 
	 */
	private void negativeSampleData(int pc){
		data.subSampleData(pc);
	}
	
	public int lbfgsmemory(){
		return lbfgsmemory;
	}
	
	public int ggascentstrategy(){
		return ggascentstrategy;
	}
	
	public int threadascentstrategy(){
		return threadascentstrategy;
	}
	
	
	public double adam_beta1(){
		return adam_beta1;
	}
	
	public double adam_beta2(){
		return adam_beta2;
	}
	
	public double adam_epsilon(){
		return adam_epsilon;
	}
	
	public double adam_alpha(){
		return adam_alpha;
	}
	
	public double adagradfade(){
		return adagradfade;
	}
	
	public double adagradepsilon(){
		return adagradepsilon;
	}
	
	public void setThreadStrategy(int s){
		threadascentstrategy = s;
	}
	
	public int getThreadStrategy(){
		return threadascentstrategy;
	}
	
	public GGThread getLearnThread() {
		return lthread;
	}
	
	public void setGGStrategy(int s){
		ggascentstrategy = s;
	}
	
	public int getGGStrategy(){
		return ggascentstrategy;
	}
	
	public double getAdamBeta1(){
		return adam_beta1;
	}
	
	public void setAdamBeta1(double b){
		adam_beta1 = b;
	}
	
	public double getAdamBeta2(){
		return adam_beta2;
	}
	
	public void setAdamBeta2(double b){
		adam_beta2 = b;
	}
	public double getAdamEpsilon(){
		return adam_epsilon;
	}
	
	public void setAdamEpsilon(double b){
		adam_epsilon = b;
	}
	public double getAdamAlpha(){
		return adam_alpha;
	}
	
	public void setAdamAlpha(double b){
		adam_alpha = b;
	}
	
	public boolean ascentIsStochastic(){
		return (threadascentstrategy == AscentAdam || threadascentstrategy == AscentStochHeur1);
	}

	public void startLearning() {
		RelData beforesplitdata;
		RelData[] learndata = null;
		if (this.getSubsamples() < 100) {
			beforesplitdata = data.subSampleData(this.getSubsamples());
		} else {
			beforesplitdata = data;
		}
		if (ascentIsStochastic()) {
			try {
				learndata = beforesplitdata.randomSplit(this.getNumBatches(), this.getSplitmode());
			} catch (RBNRuntimeException e) {
				System.out.println(e);
			}
		} else {
			learndata = new RelData[1];
			learndata[0] = beforesplitdata;
		}

		lthread = new LearnThread(myprimula, data, learndata, parammodel, this);

		if (this.getLearnModuleGUI() != null) {
			lthread.setNumrestartsfield(this.getLearnModuleGUI().getTextnumrestarts());
			lthread.setParametertable(this.getLearnModuleGUI().getParametertable());
		}

		lthread.start();
	}
	
	public boolean ggverbose() {
		return learnverbose;
	}

	public int getSamplesize() {
		return samplesize;
	}

	public void setSamplesize(int samplesize) {
		this.samplesize = samplesize;
	}

	public double getPercmiss() {
		return percmiss;
	}

	public void setPercmiss(double percmiss) {
		this.percmiss = percmiss;
	}

	public void setMyprimulaGUI(PrimulaGUI myprimulaGUI) { this.myprimulaGUI = myprimulaGUI; }

	public void setLearnModuleGUI(LearnModuleGUI learnModuleGUI) { this.learnModuleGUI = learnModuleGUI; }

	public LearnModuleGUI getLearnModuleGUI() {
		return learnModuleGUI;
	}

	public RelData getData() {
		return data;
	}

	public void setData(RelData data) {
		this.data = data;
	}

	public void setNumrelblocks(String[][] numrelblocks) {
		this.numrelblocks = numrelblocks;
	}

	public String[][] getNumrelblocks() {
		return numrelblocks;
	}

	public void setNumrelsfromfile(boolean numrelsfromfile) {
		this.numrelsfromfile = numrelsfromfile;
	}

	public ParameterTableModel getParammodel() {
		return parammodel;
	}

	public DefaultListModel getNumRelListModel() {
		return numRelListModel;
	}

	public boolean isNumrelsfromfile() {
		return numrelsfromfile;
	}


}
