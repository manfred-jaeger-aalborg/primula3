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

	//	double log_likelihood;

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
	private double[][] small_gradient;

	private double[][][] small_gradients_for_samples;

	//	/** The sum of gradients for a current set of samples. Array of small doubles */
	//	private double[][] gradientsum;



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
		small_gradient = new double[k][2];
		//		gradientsum = new double[k][2];
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
		return evaluate(sno,0, children,false,true,null);
	}

	/** Computes the likelihood 
	 * (ignoring those terms that are not dependent
	 * on unknown atoms or parameters)
	 * 
	 *  returns likelihood value as small double in the form [Double,Double]
	 *  
	 * See this.evaluateSmallGrad for batchelements parameter
	 * 
	 * 
	 * oldsmallls: array of dimension thisgg.windowsize*thisgg.numchains x 2 of small double values.
	 * oldsmallls[sno][] contains the small double representation of the likelihood factor contributed
	 * by the batchelements in the likelihood value for sample sno. Only needed when incremental=true
	 * and update=true.
	 * 
	 * In that case the 
	 * small_likelihood values are updated by multiplying the current values with
	 * the ratios of the previous factors given by oldsmallls and the
	 * new values. 
	 * 
	 * In the case incremental = true and update = false the local likelihood of batchelements is returned, and 
	 * no change to small_likelihoods_for_samples takes place
	 *
	 * smallSample: if it is not 0, the evaluation of the sampled value will be only on the dimension of smallSample for all the chains
	 * it should represent the size of the window for the prior "window-reduced" gibb sampling
	 */
	public double[] evaluate(Integer sno,
			int smallSample,
			Vector<GGCPMNode> batchelements,
			boolean incremental,
			boolean updatelik,
			double[][] oldsmallls)
	{
		if (this.small_likelihoods_for_samples==null && sno!=null)
			System.out.println("Calling GGLikelihoodNode.evaluate with sample number when no small_likelihoods_for_samples present");

		int idx =0; // index in small_likelihoods_for_samples that needs to be updated
		if (this.depends_on_sample && sno!=null)
			idx = sno;

		/*
		 * First the case where all samples have to be evaluated
		 */
		if (this.depends_on_sample && sno==null) {

			double[] small_likelihood_sum = new double[2];
			if (smallSample == 0) {
				for (int i = 0; i < thisgg.windowsize * thisgg.numchains; i++)
					small_likelihood_sum = SmallDouble.add(small_likelihood_sum, evaluate(i, 0, batchelements, incremental, updatelik, oldsmallls));
				small_likelihood_sum = SmallDouble.divide(small_likelihood_sum, thisgg.windowsize * thisgg.numchains);
			} else {
				int i = thisgg.getWindowIndex()-smallSample;
				if (i<0) i = thisgg.windowsize+i;
				int index=i;
				while (i < thisgg.windowsize*thisgg.numchains) {
					for (int c = 1; c <= thisgg.numchains; c++) {
						index = i;
						for (int j=0;j < smallSample; j++) {
							if (index == thisgg.windowsize*c) index = thisgg.windowsize*(c-1);
							small_likelihood_sum = SmallDouble.add(small_likelihood_sum, evaluate(index, smallSample, batchelements, incremental, updatelik, oldsmallls));
							index++;
						}
						i+=thisgg.windowsize;
					}
				}
				small_likelihood_sum = SmallDouble.divide(small_likelihood_sum, smallSample*thisgg.numchains);
			}

			if (updatelik) {
				if (incremental) {
					double[] oldlsum = new double[2];
					if (smallSample == 0) {
						for (int i = 0; i < thisgg.windowsize * thisgg.numchains; i++)
							oldlsum = SmallDouble.add(oldlsum, oldsmallls[i]);
						oldlsum = SmallDouble.divide(oldlsum, thisgg.windowsize * thisgg.numchains);
					} else {
						int i = thisgg.getWindowIndex()-smallSample;
						if (i<0) i = thisgg.windowsize+i;
						int index=i;
						while (i < thisgg.windowsize*thisgg.numchains) {
							for (int c = 1; c <= thisgg.numchains; c++) {
								index = i;
								for (int j=0;j < smallSample; j++) {
									if (index == thisgg.windowsize*c) index = thisgg.windowsize*(c-1);
									oldlsum = SmallDouble.add(oldlsum, oldsmallls[index]);
									index++;
								}
								i+=thisgg.windowsize;
							}
						}
						oldlsum = SmallDouble.divide(oldlsum, smallSample*thisgg.numchains);
					}
					double[] ratio = SmallDouble.divide(small_likelihood_sum, oldlsum);
					small_likelihood = SmallDouble.multiply(small_likelihood, ratio);
				}
				else
					small_likelihood=small_likelihood_sum.clone();
			}

			return small_likelihood;
		}

		/*
		 * The case where the requested likelihood is already computed:
		 */
		if (sno!= null && is_evaluated_val_for_samples[sno])
			return small_likelihoods_for_samples[sno];

		double[] local_small_likelihood = new double[] {1.0,0.0}; 

		// Main iteration over the children 	
		double[] childval;
		int childinst;
		double childlik;

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

			//			if (nextchild.getMyatom().equals("target(0)")) {
			//				System.out.println("target(): " + childlik);
			//			}

			if (!this.depends_on_sample || (this.depends_on_sample && sno!=null)) 
				local_small_likelihood=SmallDouble.multiply(local_small_likelihood, childlik);
		}


		if (updatelik && incremental) {
			double[] ratio = SmallDouble.divide(local_small_likelihood, oldsmallls[idx]);
			small_likelihoods_for_samples[idx] = SmallDouble.multiply(local_small_likelihood, ratio);
		}
		if (updatelik && !incremental && !this.depends_on_sample) {
			small_likelihoods_for_samples[0] = local_small_likelihood.clone();
			small_likelihood = local_small_likelihood.clone();
		}
		if (updatelik && !incremental && this.depends_on_sample) {
			small_likelihoods_for_samples[idx] = local_small_likelihood.clone();
		}
		is_evaluated_val_for_samples[idx] = true;

		return local_small_likelihood;
	}

	/** for compatibility with GGNode ... should not be used*/
	public double evaluateGrad(Integer sno) 
			throws RBNNaNException
	{
		System.out.println("Method evaluateGrad called in class GGLikelihoodNode -- trouble ahead!");
		return Double.NaN;
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

	//	public void evaluateGradients(Integer sno)
	//	throws RBNNaNException
	//	{
	//		for (String par: thisgg.parameters.keySet()){
	//			small_gradient[thisgg.parameters.get(par)]=evaluateSmallGrad(sno,par);
	//		}
	//	}

	//	private double evaluateLogGrad(int param){
	//		double result = 0;
	//		for (int i=0;i<children.size();i++){
	//			result = result + children.elementAt(i).evaluateGrad(param)/children.elementAt(i).value();
	//		}
	//		
	//		return result;
	//	}


	/*
	 * Computes the gradient of the likelihood (not log-likelihood!) in small doubles
	 * Returns array of dimensions #parameter x 2
	 */
	public double[][] evaluateSmallGrad(Integer sno)
			throws RBNNaNException
	{
		return evaluateSmallGrad(sno,children);    
	}

	/*
	 * Computes the gradient of the likelihood (not log-likelihood!) in small doubles
	 * Returns array of dimensions #parameter x 2
	 * batchelements contains the children that are 
	 * used (as a current data batch).
	 */
	private double[][] evaluateSmallGrad(Integer sno, Vector<GGCPMNode> batchelements)
			throws RBNNaNException
	{

		int idx =0; // index in small_gradients_for_samples that needs to be updated
		if (this.depends_on_sample && sno!=null)
			idx = sno;

		// Initialize relevant fields

		if (this.depends_on_sample && sno==null) {

			for (int i=0;i<thisgg.windowsize*thisgg.numchains;i++) 
				evaluateSmallGrad(i,batchelements);

			small_gradient=new double[thisgg.numberOfParameters()][2];
			for (int i=0;i<thisgg.windowsize*thisgg.numchains;i++) 
				SmallDouble.addArray(small_gradient, small_gradients_for_samples[i]);
			small_gradient = SmallDouble.divide(small_gradient, thisgg.windowsize*thisgg.numchains );		

			return small_gradient;
		}

		if (this.depends_on_sample && !is_evaluated_val_for_samples[sno]){
			this.evaluate(sno,0,batchelements,false,false,null); // TODO: incremental version of evaluateSmallGrad ?
		}
		if (!this.depends_on_sample && !is_evaluated_val_for_samples[0]){
			this.evaluate(sno,0,batchelements,false,false,null);
		}

		double[][] result = new double[thisgg.numberOfParameters()][2];

		double[] relevantlikelihood = small_likelihood.clone();


		int childinst;
		double childlik;
		double childpartderiv;
		//TreeMap<String,double[]> childgrad;
		double childgrad_at_value;

		if (relevantlikelihood[0]!=0 ){
//			Vector<double[]> childvals = new Vector<double[]>();
//			Vector<Gradient>childgrads = new Vector<Gradient>();
//			for (GGCPMNode child: batchelements) {
//				childvals.add(child.evaluate(sno));
//				childgrads.add(child.evaluateGradient(sno));
//			}

			for (GGCPMNode child: batchelements) {
				double[] childval = child.evaluate(sno);
				Gradient childgrad = child.evaluateGradient(sno);
				childinst = child.instval(sno);

				for(Gradient.IdxPD idpd: childgrad.as_idxpd_list()){

					if (!child.isBoolean()) {
						childlik = childval[childinst];
						childgrad_at_value=idpd.getPd()[childinst];

					} else {
						if (childinst==1) {
							childlik = childval[0];
							childgrad_at_value=idpd.getPd()[0];
						}
						else {
							childlik = 1- childval[0];
							childgrad_at_value=-idpd.getPd()[0];
						}
					}
					result[idpd.getIdx()]=SmallDouble.add(result[idpd.getIdx()],
							SmallDouble.multiply(SmallDouble.divide(relevantlikelihood,
											childlik),
									SmallDouble.asSmallDouble(childgrad_at_value)
							));
				}
//				childvals.add(child.evaluate(sno));
//				childgrads.add(child.evaluateGradient(sno));
			}
//			for (String param: thisgg.parameters.keySet()) {
//
//				double[] batch_small_partderiv = new double[2];
//
//				for (int i=0;i<childvals.size();i++){
//
//					childinst = batchelements.elementAt(i).instval(sno);
//					double[] childpartderiv = childgrads.elementAt(i).get_part_deriv(param);
//
//
//					if (!batchelements.elementAt(i).isBoolean()) {
//						childlik = childvals.elementAt(i)[childinst];
//						if (childpartderiv!=null)
//							childgrad_at_value=childpartderiv[childinst];
//						else
//							childgrad_at_value=0.0;
//					} else {
//						if (childinst==1) {
//							childlik = childvals.elementAt(i)[0];
//							if (childpartderiv!=null)
//								childgrad_at_value=childpartderiv[0];
//							else
//								childgrad_at_value=0.0;
//						}
//						else {
//							childlik = 1- childvals.elementAt(i)[0];
//
//							if (childpartderiv!=null)
//								childgrad_at_value=-childpartderiv[0];
//							else
//								childgrad_at_value=0.0;
//						}
//					}
//
//					batch_small_partderiv = SmallDouble.add(batch_small_partderiv,
//							SmallDouble.multiply(SmallDouble.divide(relevantlikelihood,
//									childlik),
//									SmallDouble.asSmallDouble(childgrad_at_value)
//									));
//				}
//				result[thisgg.parameters().get(param)] = batch_small_partderiv;
//			}
		}
		else {
			System.out.println("likelihood[0]=0 in evaluateSmallGrad");
		}


		small_gradients_for_samples[idx]=result;
		
		if (!depends_on_sample)
			small_gradient=small_gradients_for_samples[0];
		
		return result;	    
	}


	//	private int getInstVal(int i){
	//		return getInstVal(children.elementAt(i));
	//	}
	//
	private int getInstVal(int sno, GGCPMNode uga){
		Object ival = uga.getInstval();
		if (ival instanceof Integer)
			return (Integer)ival;
		if (ival instanceof GGAtomMaxNode)
			return ((GGAtomMaxNode)ival).getCurrentInst();
		if (ival instanceof GGAtomSumNode)
			return ((GGAtomSumNode)ival).getSampledVals(sno);
		System.out.println("Error: No applicable case in GGLikelihoodNode.getInstVal");
		return 0;
	}

	//	public double[] gradientsumAsDouble(){
	//		return SmallDouble.toStandardDoubleArray(gradientsum);
	//	}
	//	
	//	public double[] gradientsumAsDouble(int partial){
	//		double[] result = SmallDouble.toStandardDoubleArray(gradientsum);
	//		for (int i=0;i<result.length;i++)
	//			if (i!=partial)
	//				result[i]=0;
	//		return result;
	//	}


//	public double[] gradientAsDouble(){
//		return SmallDouble.toStandardDoubleArray(small_gradient);
//	}


	/* New version actually returning the gradient of the log-likelihood
	*
	 */
	public double[] gradientAsDouble(){
		return SmallDouble.toStandardDoubleArray(SmallDouble.divide(small_gradient,small_likelihood));
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
		result = SmallDouble.toStandardDoubleArray(small_gradient);
		for (int i=0;i<result.length;i++)
			if (i!=partial)
				result[i]=0;
		return result;
	}


	//	public void initSampleLikelihoods(int size){
	//		samplelikelihoods = new double[size][2];
	//	}

	public double loglikelihood(){
		return SmallDouble.log(small_likelihood);
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
		if (!this.depends_on_sample) {
			is_evaluated_val_for_samples[0] = false;
			//			log_likelihood = 0;
			small_likelihood[0]=0.0;
			small_likelihood[1]=0.0;
		}
		else if (sno==null)
			this.init_values_and_grad(true);
		else {
			is_evaluated_val_for_samples[sno] = false;
			small_likelihoods_for_samples[sno]=null;
		}
	}



	//	public void resetBounds(){
	//		bounds[0][0]=-1;
	//		bounds[0][1]=-1;
	//		bounds[1][0]=-1;
	//		bounds[1][1]=-1;
	//
	//	}



	//	public void showChildren(RelStruc A){
	//		System.out.println("***** children :" );
	//		for (int i=0;i<children.size();i++)
	//			System.out.println(children.elementAt(i).name(A));
	//	}


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

	//	public double objective(){
	//		switch (thisgg.objective()){
	//			case LearnModule.UseLik:
	//				return this.small_likelihood[0]; /* Careful: this is not good! */
	//			case LearnModule.UseLogLik:
	//				return SmallDouble.log(this.likelihood());
	//			case LearnModule.UseSquaredError:
	//				return this.getSSQE();
	//		}
	//		System.out.println("Reached unforseen case in GGLikelihoodNode.objective()!");
	//		return 0;
	//	}
	//	
	//	public double[] objectiveAsSmallDouble(){
	//		switch (thisgg.objective()){
	//			case LearnModule.UseLik:
	//				return this.small_likelihood;
	//			case LearnModule.UseLogLik:
	//				return SmallDouble.asSmallDouble(this.loglikelihood());
	//		}
	//		System.out.println("Reached unforseen case in GGLikelihoodNode.objective()!");
	//		return null;
	//	}

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



	/* Overrides default */
	public void init_values_and_grad(Boolean valuesonly) {
		super.init_values_and_grad(valuesonly);

		int dim;
		if (this.depends_on_sample)
			dim = thisgg.numchains*thisgg.windowsize;
		else
			dim =1;

		small_likelihoods_for_samples = new double[dim][];
		for (int i=0;i<small_likelihoods_for_samples.length;i++) {
			small_likelihoods_for_samples[i]=null;
		}
		if (!valuesonly) { // Also need gradients
			small_gradients_for_samples =  new double[dim][thisgg.numberOfParameters()][2];
		}
	}
}
