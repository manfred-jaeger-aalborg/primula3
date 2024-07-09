/*
* GGLikelihoodNode.java 
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

package RBNLearning;


import RBNutilities.*;
import RBNExceptions.*;
import RBNgui.LearnModule;

import java.util.*;

public  class GGLikelihoodNode extends GGNode{



//	/** Encode the possible instantiation values for observed atoms */
//	private Integer trueint;
//	private Integer falseint;
//
//
//	/** Represents the instantiation values of the atoms corresponding to the top-level
//	 * probability formulas. instvals has the same size as children. If children.elementAt(i)
//	 * repesents an atom which is instantiated to true (false) in the data, then
//	 * instvals.elementAt(i)=this.trueint (this.falseint). If children.elementAt(i)
//	 * represents an atom which is not instantiated in the data, then 
//	 * instvals.elementAt(i) is the indicator node for this atom
//	 */
//	private Vector instvals;

	/*
	 * Constants for confusion matrix entries
	 */
	public static final int TP=0;
	public static final int FP=1;
	public static final int FN=2;
	public static final int TN=3;
	

	/** The current (likelihood) value represented 
	 * as a pair of doubles to be handled by class SmallDouble
	 */ 
	double[] small_likelihood;
	
	/*
	 * log -likelihood as a standard double for use with LearnModule.UseLogLik
	 */
	
	double log_likelihood;
	
	/**
	 * The current sum of negative squared errors
	 * (to be uniform with likelihood in that the
	 * objective is to maximize, not minimize)
	 */
	double ssqe;
	
	/**
	 * The current values of the confusion matrix [TP,FP,FN,TN]
	 */
	int[] confusion;

//	/** Likelihood values for all samples, at current parameter settings */
//	double[][] samplelikelihoods;
	
	/** Corresponds to values_for_samples at other GGNodes. Here implemented
	 * as small doubles
	 */
	private double[][] small_likelihoods_for_samples;
	
//	/** The sum of likelihoods for a current set of samples */
//	private Double[] likelihoodsum;

	/** Used instead of the standard gradient vector in GGNode class --
	 * for the likelihood node need gradient as small doubles!*/
	private double[][] smallgradient;

	/** The sum of gradients for a current set of samples. Array of small doubles */
	private double[][] gradientsum;
	


	/* Upper and lower bounds as small doubles on the value of this node given
	 * a current partial evaluation.
	 * Set to [-1,-1] if these bounds have not been evaluated
	 * for the current setting at the indicator nodes
	 */
	double[][] bounds;


	/* Maps parameters to the UGA nodes that
	 * depend on that parameter
	 */
	Hashtable<String,Vector<GGCPMNode>> ugasForParam;
	
	
	public GGLikelihoodNode(GradientGraphO gg){
		super(gg);
//		small_likelihood = new double[2];
//		likelihoodsum = new Double[2];
		confusion = new int[4];
		ssqe=0;
		bounds = new double[2][2];
		ugasForParam = new Hashtable<String,Vector<GGCPMNode>>();
	}

	public void initllgrads(int k){
		smallgradient = new double[k][2];
		gradientsum = new double[k][2];
	}
	
//	public void addToChildren(GGProbFormNode ggpfn, boolean tv){
//		children.add(ggpfn);
//		ggpfn.setIsuga(true);
//		if (tv)
//			instvals.add(trueint);
//		else 
//			instvals.add(falseint);
//	}

//	public void addToChildren(GGProbFormNode ggpfn, GGAtomNode ggin){
//		children.add(ggpfn);
//		ggpfn.setIsuga(true);
//		instvals.add(ggin);
//		ggpfn.setMyindicator(ggin);
//		ggin.setUGA(ggpfn);
//	}

//	public Double[] evaluate() {
//		super.evaluate();
//		double[] small_result=new double[2];
//		if (this.small_likelihoods_for_samples == null) {
//			return new Double[]{SmallDouble.toStandardDouble(this.small_likelihood)};
//		}
//		else {
//			switch (thisgg.objective()){
//			case LearnModule.UseLik:
//				small_result[0]=1.0;
//				for (int i=0; i<small_likelihoods_for_samples.length;i++)
//					small_result=SmallDouble.multiply(small_result, small_likelihoods_for_samples[i]);
//				break;
//			case LearnModule.UseLogLik:
//				small_result[0]=0.0;
//				for (int i=0; i<small_likelihoods_for_samples.length;i++)
//					small_result=SmallDouble.add(small_result, small_likelihoods_for_samples[i]);
//				break;
//			}
//		}
//			
//		return new Double[]{SmallDouble.toStandardDouble(small_result)};
//	}
//	
	public double[] evaluate(Integer sno) {
		return evaluate(sno,children);
	}
	
	/** Computes the (log-)likelihood and confusion matrix 
	 * (ignoring those terms that are not dependent
	 * on unknown atoms or parameters)
	 * 
	 * CONFUSION MATRIX currently disabled (needs revision for categorical variables)!
	 * 
	 * In case of LearnModule.UseLik: returns likelihood value as small double in the form [Double,Double]
	 * In case of LearnModule.UseLogLik: returns log-likelihood value as [Double]
	 * 
	 * See this.evaluateSmallGrad for batchelements parameter
	 */
	public double[] evaluate(Integer sno, Vector<GGCPMNode> batchelements){

		if (this.small_likelihoods_for_samples==null && sno!=null)
			System.out.println("Calling GGLikelihoodNode.evaluate with sample number when no small_likelihoods_for_samples present");

		/* calling evaluate(null,batchelements) in the case where there 
		 * are samples means that we need to evaluate for all samples and 
		 * sum the individual sample likelihoods. log-likelihoods not allowed
		 * in this case.
		 */
		if (this.depends_on_sample && sno==null) {
			switch (thisgg.objective()){
			case LearnModule.UseLogLik:
				System.out.println("Invalid option: log likelihood in the presence of incomplete data");
			case LearnModule.UseLik:
				small_likelihood=new double[] {0.0,0.0};
				for (int i=0;i<thisgg.windowsize*thisgg.numchains;i++)
					small_likelihood=SmallDouble.add(small_likelihood, evaluate(i,batchelements));
				return small_likelihood;
			}
		}

		// Initialize relevant fields
		if (!this.depends_on_sample) { 
			switch (thisgg.objective()){
			case LearnModule.UseLik:  // This should usually not be used
				small_likelihood=new double[2];
				small_likelihood[0]=1.0;
				break;
			case LearnModule.UseLogLik:
				log_likelihood=0;
			}
		}

		if (this.depends_on_sample) { // when we get here, then sno != null
			switch (thisgg.objective()){
			case LearnModule.UseLik: 
				small_likelihoods_for_samples[sno]=new double[] {1.0,0.0};
				break;
			case LearnModule.UseLogLik: // this should usually not be used
				log_likelihood=0;
			}
		}

		// Main iteration over the children 	
		Double[] childval;
		int childinst;
		double childlik;
		double[] s_lik = new double[] {1.0,0.0};
		
		for (GGCPMNode nextchild: batchelements){
			childval = nextchild.evaluate(sno);
			childinst = nextchild.instval(sno); 
			// we manage the value returned by the nextchild by checking if the node is boolean or not
			// if boolean we need to flip the value if instval() == 0 (all the nodes in GG return the probability of being true)
			if (!nextchild.isBoolean()) {
				childlik = childval[childinst];
			} else {
				if (childinst==1)
					childlik = childval[0];
				else
					childlik = 1- childval[0];
			}

			//			ival = nextchild.instval();
			//			switch (ival) {
			//				case 0:
			//					if (childlik<0.5)
			//						confusion[this.TN]++;
			//					else
			//						confusion[this.FP]++;
			//					ssqe=ssqe-Math.pow(childlik, 2);
			//					break;
			//				case 1:
			//					if (childlik<0.5)
			//						confusion[this.FN]++;
			//					else
			//						confusion[this.TP]++;
			//					ssqe=ssqe-Math.pow(1-childlik, 2);
			//					break;
			//			}

			if (this.depends_on_sample && sno!=null) {
				switch (thisgg.objective()){
				case LearnModule.UseLogLik:
					System.out.println("Invalid option: log likelihood in the presence of incomplete data");
				case LearnModule.UseLik:
					small_likelihoods_for_samples[sno]=SmallDouble.multiply(small_likelihoods_for_samples[sno], childlik);
				}
			}

			if (!this.depends_on_sample) {
				switch (thisgg.objective()){
				case LearnModule.UseLogLik:
					log_likelihood+=Math.log(childlik);
				case LearnModule.UseLik:
					SmallDouble.multiply(s_lik, childlik);
				}
			}

		}

		if (this.depends_on_sample && sno!=null) { 
			this.is_evaluated_for_samples[sno] = true;
			return small_likelihoods_for_samples[sno];
		}
		else {
			is_evaluated = true;
			switch (thisgg.objective()){
			case LearnModule.UseLogLik:
				small_likelihood = SmallDouble.asSmallDouble(log_likelihood);
				return new double[] {log_likelihood};
			case LearnModule.UseLik:
				return s_lik;
			}
		}
		System.out.println("Unhandled case in GGLikelihoodNode.evaluate");
		return new double[] {Double.NaN};
	}

	/** for compatibility with GGNode ....use with care */
	public double evaluateGrad(Integer sno, String param) 
	throws RBNNaNException
	{
		return evaluateSmallGrad(param)[0];
	}
	
//	/** for compatibility with GGNode ....use with care */
//	public double evaluateGrad(int param, int[] batchelements)
//	throws RBNNaNException
//	{
//		evaluateSmallGrad(param,batchelements);
//		return SmallDouble.toStandardDouble(smallgradient[param]);
//	}

//	public void evaluateBounds(){
//		if (bounds[0][0]==-1){
//			//	    System.out.println("likelihoodnode.evaluateBounds");
//			/* Evaluate bounds at children: */
//			for (int i=0;i<children.size();i++)
//				children.elementAt(i).evaluateBounds();
//			double lowbound[] = {1,0};
//			double uppbound[] = {1,0};
//			for (int i=0;i<children.size();i++){
//				if (getInstVal(i)==1){
//					lowbound= SmallDouble.multiply(lowbound,children.elementAt(i).lowerBound());
//					uppbound= SmallDouble.multiply(uppbound,children.elementAt(i).upperBound());
//				}
//				else{
//					lowbound= SmallDouble.multiply(lowbound,(1-children.elementAt(i).upperBound()));
//					uppbound= SmallDouble.multiply(uppbound,(1-children.elementAt(i).lowerBound()));					}
//			}
//			bounds[0]=lowbound;
//			bounds[1]=uppbound;
//		}
//	}



	
//	public void evaluateGradients()
//	throws RBNNaNException
//	{
//		int[] idx = rbnutilities.indexArray(children.size());
//		evaluateGradients(idx);
//	}

//	public void evaluateGradients(int batchsize)
//	throws RBNNaNException
//	{
//		int[] idx = randomGenerators.multRandInt(0, children.size()-1, batchsize);
//		evaluateGradients(idx);
//	}
	
//	public void evaluateGradients(int[] batchelements)
//	throws RBNNaNException
//	{
//		for (int i=0;i<smallgradient.length;i++){
//			smallgradient[i]=evaluateSmallGrad(i,batchelements);
//		}
//	}

	public void evaluateGradients()
	throws RBNNaNException
	{
		for (String par: thisgg.parameters.keySet()){
			smallgradient[thisgg.parameters.get(par)]=evaluateSmallGrad(par);
		}
	}

//	private double evaluateLogGrad(int param){
//		double result = 0;
//		for (int i=0;i<children.size();i++){
//			result = result + children.elementAt(i).evaluateGrad(param)/children.elementAt(i).value();
//		}
//		
//		return result;
//	}
	
	
	
	private double[] evaluateSmallGrad(String param)
			throws RBNNaNException
	{
		return evaluateSmallGrad(param,children);    
	}

	
	/**
	 * Evaluates the partial derivative for parameter param as a small double.
	 * The array batchelements contains the indices of the children that are 
	 * used (as a current data batch).
	 * If the gradient with regard to the whole data is computed, then
	 * batchelements = [0,1,...,children.size()-1]
	 */
	private double[] evaluateSmallGrad(String param, Vector<GGCPMNode> batchelements)
	throws RBNNaNException
	{
		// TODO: complete check/revision
	
////		if (!isEvaluated){
//			this.evaluate(batchelements);
////		}
//		double smallgrad[] = {0,0};
//
//		System.out.println("debug: evaluateSmallGrad for parameter " + param);
//		
//		double[] relevantlikelihood = likelihood.clone();
//		GGProbFormNode child;
//		double ival;
//		
//		if (relevantlikelihood[0]!=0 || thisgg.objective()==LearnModule.UseSquaredError){
//			for (int i=0;i<batchelements.length;i++){
//				child = children.elementAt(batchelements[i]);
//				System.out.println("debug: evaluateSmallGrad for child " + batchelements[i]);
//				
//				if (child.dependsOn(param)){
//					System.out.println("debug: child depends on param ");
//					ival = getInstVal(batchelements[i]);
//					switch(thisgg.objective()){
//					case LearnModule.UseLik:
//						if (ival==1)
//							smallgrad = SmallDouble.add(smallgrad,
//									SmallDouble.multiply(SmallDouble.divide(relevantlikelihood,
//											child.value()),
//											child.evaluateGrad(param)
//											));
//						else smallgrad = SmallDouble.subtract(smallgrad,
//								SmallDouble.multiply(SmallDouble.divide(relevantlikelihood,
//										1-child.value()),
//										child.evaluateGrad(param)
//										));
//						break;
//					case LearnModule.UseLogLik:
//						if (ival ==1)
//							smallgrad = SmallDouble.add(smallgrad, 
//									SmallDouble.asSmallDouble(child.evaluateGrad(param)/child.value()));
//						else
//							smallgrad = SmallDouble.add(smallgrad, 
//									SmallDouble.asSmallDouble(-child.evaluateGrad(param)/(1-child.value())));
//						break;
//					case LearnModule.UseSquaredError:
//						/* We are computing the negative gradient of the squared error,
//						 * so that also in the squared error case we can always maximize
//						 * the objective function
//						 */
//						if (ival ==1)
//							smallgrad = SmallDouble.add(smallgrad, 
//									SmallDouble.asSmallDouble(child.evaluateGrad(param)*(1-child.value())));
//						else
//							smallgrad = SmallDouble.add(smallgrad, 
//									SmallDouble.asSmallDouble(-child.evaluateGrad(param)*child.value()));
//						break;
//					}
//				}
//			}
//		}
//		else {
//			System.out.println("likelihood[0]=0 in evaluateSmallGrad");
//		}
//		if (Double.isNaN(smallgrad[0]) || Double.isInfinite(smallgrad[0])){
//			System.out.println("Warning: gradient for " + thisgg.parameterAt(param) + "has value " + smallgrad[0] + "in GGLikelihoodNode.evaluateSmallGrad; Returning value 1000");
//			smallgrad[0]=1000;
//			smallgrad[1]=0;
//		}
//
		return null;	    
	}



	
//	private int getInstVal(int i){
//		return getInstVal(children.elementAt(i));
//	}
//
//	private int getInstVal(GGCPMNode uga){
//		Object ival = uga.getInstval();
//		if (ival instanceof Integer)
//			return (Integer)ival;
//		else{
//			if (((GGAtomNode)ival).getCurrentInst()==-1)
//				System.out.println("Illegal instantiation value!");
//			return ((GGAtomNode)ival).getCurrentInst();
//		}
//	}
	
	public double[] gradientsumAsDouble(){
		return SmallDouble.toStandardDoubleArray(gradientsum);
	}
	
	public double[] gradientsumAsDouble(int partial){
		double[] result = SmallDouble.toStandardDoubleArray(gradientsum);
		for (int i=0;i<result.length;i++)
			if (i!=partial)
				result[i]=0;
		return result;
	}


	public double[] gradientAsDouble(){
			return SmallDouble.toStandardDoubleArray(smallgradient);
	}


	
	/** Returns the gradient (scaled to fit double precision) 
	 * with all components where zeros[i]=1 set to 0
	 * 
	 * Corresponds to taking partial derivatives, ignoring
	 * parameters with index i.
	 * 
	 * @param partial
	 * @return
	 */ 
	public double[] gradientAsDouble(int partial){
		double result[];
			result = SmallDouble.toStandardDoubleArray(smallgradient);
		for (int i=0;i<result.length;i++)
			if (i!=partial)
				result[i]=0;
		return result;
	}

	
//	public void initSampleLikelihoods(int size){
//		samplelikelihoods = new double[size][2];
//	}

	public double loglikelihood(){
		switch (thisgg.objective()){
		case LearnModule.UseLik:
			return SmallDouble.log(small_likelihood);
		case LearnModule.UseLogLik:
			if (small_likelihood[1]!=0)
				System.out.println("Warning: overflow in log-likelihood value");
			return small_likelihood[0];
		default: return Double.NaN;
		}
	}
	
	public double loglikelihood(Integer sno){
			return SmallDouble.log(small_likelihoods_for_samples[sno]);
	}
	
	public double[] likelihood(){
		return small_likelihood.clone();
	}

//	public double[] likelihoodsum(){
//		return likelihoodsum.clone();
//	}


//	public double[] lowerBound(){
//		return bounds[0].clone();
//	}
//
//	public double[] upperBound(){
//		return bounds[1].clone();
//	}

	/** The name of this node. The name identifies the function represented
	 * by a node. 
	 */
	public String name(){
		return "Likelihood";
	}

//	public String name(RelStruc A){
//		return name();
//	}

	public int numChildren(){
		return children.size();
	}

	public void resetValue(Integer sno){
		is_evaluated = false;
		if (this.small_likelihoods_for_samples==null) {
			value = null;
			log_likelihood = 0;
			small_likelihood[0]=0.0;
			small_likelihood[1]=0.0;
		}
		else if (sno==null)
			this.init_values_for_samples();
		else
			small_likelihoods_for_samples[sno]=null;

	}

//	public void resetLikelihoodSum(){
//		likelihoodsum[0] = 0;
//		likelihoodsum[1] = 0;
//	}

	public void resetGradientSum(){
		for (int i=0;i<gradientsum.length;i++){
			gradientsum[i][0]=0.0;
			gradientsum[i][1]=0.0;
		}
	}

//	public void resetBounds(){
//		bounds[0][0]=-1;
//		bounds[0][1]=-1;
//		bounds[1][0]=-1;
//		bounds[1][1]=-1;
//
//	}

//	/** Sets the current likelihood value as the likelihood value for the i'th sample */
//	public void setSampleLikelihood(int i){
//		samplelikelihoods[i][0]=likelihood[0];
//		samplelikelihoods[i][1]=likelihood[1];
//	}

//	public double[][] getSampleLikelihoods(){
//		return samplelikelihoods;
//	}

//	public double[] getSampleLikelihood(int sno){
//		return samplelikelihoods[sno];
//	}


//	public void showChildren(RelStruc A){
//		System.out.println("***** children :" );
//		for (int i=0;i<children.size();i++)
//			System.out.println(children.elementAt(i).name(A));
//	}


	public void updateGradSum(){
		for (int i=0; i<gradientsum.length; i++){
			gradientsum[i]=SmallDouble.add(gradientsum[i],smallgradient[i]);
		}
	}
	
	public double[][] getSmallgradient(){
		return smallgradient;
	}
	
	public int[] getConfusion() {
		return confusion;
	}
	
	public double getAccuracy() {
		int truecount = confusion[this.TP]+confusion[this.TN];
		int allcount = confusion[this.TP]+confusion[this.TN] + confusion[this.FP]+confusion[this.FN];
		return (double)truecount/(double)allcount;
	}
	
	public double getSSQE() {
		return ssqe;
	}
	
	public double objective(){
		switch (thisgg.objective()){
			case LearnModule.UseLik:
				return this.small_likelihood[0]; /* Careful: this is not good! */
			case LearnModule.UseLogLik:
				return this.loglikelihood();
			case LearnModule.UseSquaredError:
				return this.getSSQE();
		}
		System.out.println("Reached unforseen case in GGLikelihoodNode.objective()!");
		return 0;
	}
	
	public double[] objectiveAsSmallDouble(){
		switch (thisgg.objective()){
		// Make no distinction between UseLik and UseLogLik here. UseLik should not be a separate objective
		// Only in the internal computation do we need to distinguish the need to compute at the likelihood 
		// level when dealing with incomplete data
			case LearnModule.UseLik:
				return SmallDouble.asSmallDouble(this.loglikelihood());
			case LearnModule.UseLogLik:
				return SmallDouble.asSmallDouble(this.loglikelihood());
			case LearnModule.UseSquaredError:
				return SmallDouble.asSmallDouble(this.getSSQE());
		}
		System.out.println("Reached unforseen case in GGLikelihoodNode.objective()!");
		return null;
	}
	
	public void addUgaForParam(String par, GGCPMNode uga) {
		Vector<GGCPMNode> u = ugasForParam.get(par); 
		if (u == null) {
			Vector<GGCPMNode> newvec = new Vector<GGCPMNode>();
			newvec.add(uga);
			ugasForParam.put(par,newvec);
		}
		else
			u.add(uga);
		
	}
	
//	public void set_value_for_sample(int sno) {
//		for (GGCPMNode uga: this.children)
//			uga.set_value_for_sample(sno);
//	}
	
	/*
	 * Default implementation of method required for GGNode. Should not be used. 
	 * The outputs of the GGLikelihoodNode differ in dimensions and are retrieved 
	 * by the functions likelihood(), getSmallGradient() etc.
	 */
	public int outDim() {
		return 1;
	}
	
	/* Overrides default */
	public void init_values_for_samples() {
		small_likelihoods_for_samples = new double[thisgg.numchains*thisgg.windowsize][];
		is_evaluated_for_samples = new Boolean[thisgg.numchains*thisgg.windowsize];
		for (int i=0;i<small_likelihoods_for_samples.length;i++) {
			small_likelihoods_for_samples[i]=null;
			is_evaluated_for_samples[i]=false;
		}
	}
}
