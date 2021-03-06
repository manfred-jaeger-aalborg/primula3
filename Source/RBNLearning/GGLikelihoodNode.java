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
	double[] likelihood;
	
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
	
	/** The sum of likelihoods for a current set of samples */
	private double[] likelihoodsum;

	/** Used instead of the standard gradient vector in GGNode class --
	 * for the likelihood node need gradient as small doubles!*/
	private double[][] smallgradient;

	/** The sum of gradients for a current set of samples. Array of small doubles */
	private double[][] gradientsum;
	

	/** True if the current likelihood values represent the the correct
	 * values for the current settings of parameter and instantiation
	 * values. isEvaluated=false corresponds to value=null for other
	 * GGNode's
	 */
	private boolean isEvaluated;

	/* Upper and lower bounds as small doubles on the value of this node given
	 * a current partial evaluation.
	 * Set to [-1,-1] if these bounds have not been evaluated
	 * for the current setting at the indicator nodes
	 */
	double[][] bounds;


	public GGLikelihoodNode(GradientGraphO gg){
		super(gg);
//		trueint = new Integer(1);
//		falseint = new Integer(0);
		//instvals = new Vector();
		likelihood = new double[2];
		likelihoodsum = new double[2];
		confusion = new int[4];
		ssqe=0;
		isEvaluated = false;
		bounds = new double[2][2];
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

	public double evaluate() {
		int[] idx = rbnutilities.indexArray(children.size());
		return evaluate(idx);
	}
	
	/** Computes the (log-)likelihood and confusion matrix 
	 * (ignoring those terms that are not dependent
	 * on unknown atoms or parameters)
	 * 
	 * Returns the SmallDouble likelihood value as double. 
	 * Risk of underflow! This return value should not be used --
	 * only here for compatibility of evaluate() function at other
	 * GGNodes.
	 * 
	 * See this.evaluateSmallGrad for batchelements parameter
	 */
	public double evaluate(int[] batchelements){


		switch (thisgg.objective()){
		case GradientGraph.UseLik:
			likelihood[0]=1.0;
			break;
		case GradientGraph.UseLogLik:
			likelihood[0]=0.0;
		}
		likelihood[1]=0.0;
		for (int i=0;i<4;i++)
			confusion[i]=0;
		ssqe=0;
		
		double childlik;
		GGProbFormNode nextchild;
		int ival;
		
		for (int i=0;i<batchelements.length;i++){
			nextchild=children.elementAt(batchelements[i]);
			childlik = nextchild.evaluate();
			ival = nextchild.instval();
			switch (ival) {
				case 0:
					if (childlik<0.5)
						confusion[this.TN]++;
					else
						confusion[this.FP]++;
					ssqe=ssqe-Math.pow(childlik, 2);
					break;
				case 1:
					if (childlik<0.5)
						confusion[this.FN]++;
					else
						confusion[this.TP]++;
					ssqe=ssqe-Math.pow(1-childlik, 2);
					break;
			}
				
			
			if (ival==0)
				childlik = 1-childlik;
			
			switch (thisgg.objective()){
			case GradientGraph.UseLik:
				likelihood = SmallDouble.multiply(likelihood,childlik);
				break;
			case GradientGraph.UseLogLik:
				likelihood = SmallDouble.add(likelihood,SmallDouble.asSmallDouble(Math.log(childlik)));
			}
		}
		isEvaluated = true;
		return SmallDouble.toStandardDouble(likelihood);
	}

	/** for compatibility with GGNode ....use with care */
	public double evaluateGrad(int param) 
	throws RBNNaNException
	{
		int[] idx = rbnutilities.indexArray(children.size());
		return evaluateGrad(param,idx);
	}
	
	/** for compatibility with GGNode ....use with care */
	public double evaluateGrad(int param, int[] batchelements)
	throws RBNNaNException
	{
		evaluateSmallGrad(param,batchelements);
		return SmallDouble.toStandardDouble(smallgradient[param]);
	}

	public void evaluateBounds(){
		if (bounds[0][0]==-1){
			//	    System.out.println("likelihoodnode.evaluateBounds");
			/* Evaluate bounds at children: */
			for (int i=0;i<children.size();i++)
				children.elementAt(i).evaluateBounds();
			double lowbound[] = {1,0};
			double uppbound[] = {1,0};
			for (int i=0;i<children.size();i++){
				if (getInstVal(i)==1){
					lowbound= SmallDouble.multiply(lowbound,children.elementAt(i).lowerBound());
					uppbound= SmallDouble.multiply(uppbound,children.elementAt(i).upperBound());
				}
				else{
					lowbound= SmallDouble.multiply(lowbound,(1-children.elementAt(i).upperBound()));
					uppbound= SmallDouble.multiply(uppbound,(1-children.elementAt(i).lowerBound()));					}
			}
			bounds[0]=lowbound;
			bounds[1]=uppbound;
		}
	}



	
	public void evaluateGradients()
	throws RBNNaNException
	{
		int[] idx = rbnutilities.indexArray(children.size());
		evaluateGradients(idx);
	}

	public void evaluateGradients(int batchsize)
	throws RBNNaNException
	{
		int[] idx = randomGenerators.multRandInt(0, children.size()-1, batchsize);
		evaluateGradients(idx);
	}
	
	public void evaluateGradients(int[] batchelements)
	throws RBNNaNException
	{
		for (int i=0;i<smallgradient.length;i++){
			smallgradient[i]=evaluateSmallGrad(i,batchelements);
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
	
	/**
	 * Evaluates the partial derivative for parameter param as a small double.
	 * The array batchelements contains the indices of the children that are 
	 * used (as a current data batch).
	 * If the gradient with regard to the whole data is computed, then
	 * batchelements = [0,1,...,children.size()-1]
	 */
	private double[] evaluateSmallGrad(int param, int[] batchelements)
	throws RBNNaNException
	{
//		if (!isEvaluated){
			this.evaluate(batchelements);
//		}
		double smallgrad[] = {0,0};


		double[] relevantlikelihood = likelihood.clone();
		GGProbFormNode child;
		double ival;
		
		if (relevantlikelihood[0]!=0 || thisgg.objective()==GradientGraph.UseSquaredError){
			for (int i=0;i<batchelements.length;i++){
				child = children.elementAt(batchelements[i]);
				if (child.dependsOn(param)){
					ival = getInstVal(batchelements[i]);
					switch(thisgg.objective()){
					case GradientGraph.UseLik:
						if (ival==1)
							smallgrad = SmallDouble.add(smallgrad,
									SmallDouble.multiply(SmallDouble.divide(relevantlikelihood,
											child.value()),
											child.evaluateGrad(param)
											));
						else smallgrad = SmallDouble.subtract(smallgrad,
								SmallDouble.multiply(SmallDouble.divide(relevantlikelihood,
										1-child.value()),
										child.evaluateGrad(param)
										));
						break;
					case GradientGraph.UseLogLik:
						if (ival ==1)
							smallgrad = SmallDouble.add(smallgrad, 
									SmallDouble.asSmallDouble(child.evaluateGrad(param)/child.value()));
						else
							smallgrad = SmallDouble.add(smallgrad, 
									SmallDouble.asSmallDouble(-child.evaluateGrad(param)/(1-child.value())));
						break;
					case GradientGraph.UseSquaredError:
						/* We are computing the negative gradient of the squared error,
						 * so that also in the squared error case we can always maximize
						 * the objective function
						 */
						if (ival ==1)
							smallgrad = SmallDouble.add(smallgrad, 
									SmallDouble.asSmallDouble(-child.evaluateGrad(param)*(1-child.value())));
						else
							smallgrad = SmallDouble.add(smallgrad, 
									SmallDouble.asSmallDouble(-child.evaluateGrad(param)*child.value()));
						break;
					}
				}
			}
		}
		else {
			System.out.println("likelihood[0]=0 in evaluateSmallGrad");
		}
		if (Double.isNaN(smallgrad[0]) || Double.isInfinite(smallgrad[0])){
			System.out.println("Warning: gradient for " + thisgg.parameterAt(param) + "has value " + smallgrad[0] + "in GGLikelihoodNode.evaluateSmallGrad; Returning value 1000");
			smallgrad[0]=1000;
			smallgrad[1]=0;
		}

		return smallgrad;	    
	}


	private int getInstVal(int i){
		Object ival = children.elementAt(i).getInstval();
		if (ival instanceof Integer)
			return (Integer)ival;
		else{
			if (((GGAtomNode)ival).getCurrentInst()==-1)
				System.out.println("Illegal instantiation value!");
			return ((GGAtomNode)ival).getCurrentInst();
		}
	}

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
	 * @param zeros
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
		case GradientGraph.UseLik:
			return SmallDouble.log(likelihood);
		case GradientGraph.UseLogLik:
			if (likelihood[1]!=0)
				System.out.println("Warning: overflow in log-likelihood value");
			return likelihood[0];
		default: return Double.NaN;
		}
	}
	
	public double[] likelihood(){
		return likelihood.clone();
	}

	public double[] likelihoodsum(){
		return likelihoodsum.clone();
	}


	public double[] lowerBound(){
		return bounds[0].clone();
	}

	public double[] upperBound(){
		return bounds[1].clone();
	}

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

	public void resetValue(){
		value = null;
		likelihood[0]=0;
		likelihood[1]=0;
		isEvaluated = false;
	}

	public void resetLikelihoodSum(){
		likelihoodsum[0] = 0;
		likelihoodsum[1] = 0;
	}

	public void resetGradientSum(){
		for (int i=0;i<gradientsum.length;i++){
			gradientsum[i][0]=0.0;
			gradientsum[i][1]=0.0;
		}
	}

	public void resetBounds(){
		bounds[0][0]=-1;
		bounds[0][1]=-1;
		bounds[1][0]=-1;
		bounds[1][1]=-1;

	}

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

	/** updates the likelihoodsum field by adding value
	 */
	public void updateLikelihoodSum(){
		//System.out.print("add " + StringOps.arrayToString(likelihoodsum) + " " + StringOps.arrayToString(likelihood));
		likelihoodsum=SmallDouble.add(likelihoodsum,likelihood);
		//System.out.println(" is " + StringOps.arrayToString(likelihoodsum));
	}

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
			case GradientGraph.UseLik:
				return this.likelihood[0]; /* Careful: this is not good! */
			case GradientGraph.UseLogLik:
				return this.loglikelihood();
			case GradientGraph.UseSquaredError:
				return this.getSSQE();
		}
		System.out.println("Reached unforseen case in GGLikelihoodNode.objective()!");
		return 0;
	}
	
	public double[] objectiveAsSmallDouble(){
		switch (thisgg.objective()){
			case GradientGraph.UseLik:
				return this.likelihood.clone();
			case GradientGraph.UseLogLik:
				return SmallDouble.asSmallDouble(this.loglikelihood());
			case GradientGraph.UseSquaredError:
				return SmallDouble.asSmallDouble(this.getSSQE());
		}
		System.out.println("Reached unforseen case in GGLikelihoodNode.objective()!");
		return null;
	}
	
}
