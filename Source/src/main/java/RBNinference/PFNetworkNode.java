/*
 *  Class representing nodes in a BN representation used for sampling, with
 *  conditional probability distributions represented by probability formulas
 *  
 *  All nodes represent a ground atom.
 */


package RBNinference;
import java.util.*;
import RBNpackage.*;
import RBNExceptions.*;
import RBNutilities.*;
import RBNgui.*;
//import mymath.*;

public abstract class PFNetworkNode extends BNNode implements GroundAtomNodeInt{


//	public Vector<PFNetworkNode> parents;
//	public Vector<PFNetworkNode> children;

	protected GroundAtom myatom;
	protected int sampledepth; /* Defines a partial order for sampling: nodes with depth d must
	 * be sampled before nodes with depth d+1.
	 * Setting depends on chosen sampling order
	 */



	protected boolean upstreamofevidence; /* Set to true if this node is upstream of some
	 * instantiated node; used in adaptive sampling: nodes with
	 * upstreamofevidence == false are not sampled adaptively.
	 */

	protected int sampleinst;  /* The instantiation of this ground atom in a current sample */

	protected int[] sampleparentconfig; /* Parent configuration in current
	 * sample -- not used in all modes for sampling
	 */

	protected String sampleparentconfig_string; /* String representation (used as key in Hashtables)
	 * of sampleparentconfig
	 */

	protected double thissampleprob; /* The probability with which this node was set to its sampleinst value
	 * in the current sample
	 */

	protected double thisdistrprob; /* The conditional probability of the sampleinst value of this node
	 * given the sampleinst values of its parents according to the
	 * underlying distribution
	 */


	/** The sum of the likelihood weights of all samples in which this
	 * node was instantiated to a given value;
	 * 
	 * Represented as SmallDouble
	 **/
	protected double[][] valsampleweight; 

	/** The same but distributed over num_subsamples subsamples
	 * only used at querynodes
	 **/
	protected double[][][] valsampleweight_subsample = null;

    protected GnnPy gnnPy;

	public PFNetworkNode(GroundAtom at){
		super(at.asString());
		myatom = at;
		sampleinst = -1;
		thissampleprob = -1;
		thisdistrprob = -1;

		upstreamofevidence = false;

		Rel r = at.rel();
		if (r instanceof CatRel)
			this.numvalues=(int)r.numvals(); // overrides default value 2 already set in super constructor.
		valsampleweight = new double[this.numvalues][2];
	}


	public void addToSampleweight(double[] d,int subsind,int val){
		valsampleweight[val] = SmallDouble.add(valsampleweight[val], d);

		if (valsampleweight_subsample != null)
			valsampleweight_subsample[subsind][val]=SmallDouble.add(valsampleweight_subsample[subsind][val],d);
	}


	private void  addOtherParents(PFNetworkNode notthis,
			PFNetworkNode pfnn,
			Vector instnodes,
			boolean usesampleinst){

		for (int i=0;i< pfnn.parents.size();i++) {
			PFNetworkNode nextpfnn = (PFNetworkNode)pfnn.parents.elementAt(i);
			if (nextpfnn != notthis){
				if (!usesampleinst && nextpfnn.instantiated == -1 && !instnodes.contains(nextpfnn))
					instnodes.add(nextpfnn);
				if (usesampleinst && nextpfnn.sampleinstVal() == -1 && !instnodes.contains(nextpfnn))
					instnodes.add(nextpfnn);
			}
		}
	}




	public GroundAtom myatom(){
		return myatom;
	}

	//    /* vec is vector of PFNetworkNodes;
	//     * returns 1 if vec contains at least one node with a
	//     * sample-instantiation 1, else returns 0
	//     */
	//    private int containsTrue(Vector vec){
	//	int result = 0;
	//	int ind = 0;
	//	PFNetworkNode pfnn;
	//	while (result == 0 && ind<vec.size()){
	//	    pfnn = (PFNetworkNode)vec.elementAt(ind);
	//	    if (pfnn.sampleinstVal()==1)
	//		result = 1;
	//	    ind++;
	//	}
	//	return result;
	//    }


	/* Returns j if the probability of state j is 1 
	 * when evaluated over A and w.r.t. to given partial instantiation (evidence) inst
	 * 
	 * if usesampleinst then also sampled instantiation values will be used (apart from the
	 * 'static' values in inst).
	 * 
	 * Return -1 if the state of this PFNetworkNode is not determined in the given context.
	 */
	public abstract int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable atomhasht)
			throws RBNCompatibilityException;


	public void initializeForNextSample(){
		if (instantiated == -1){
			sampleinst=-1;
			thissampleprob = -1;
		}
		thisdistrprob = -1;
	}

	public  void initializeForSampling(int sampleordmode,
			int adaptivemode,
			Hashtable<Rel,GroundAtomList> queryatoms,
			int num_subsamples_minmax,
			int num_subsamples_adapt)
	{
		valsampleweight = new double[this.numvalues][2];
		GroundAtomList gal = queryatoms.get(myatom.rel());
		if (gal != null && gal.contains(myatom)){
			valsampleweight_subsample = new double[num_subsamples_minmax][this.numvalues][2];
		}
	}


	/* Checks whether instantiating this node with val 
	 * is locally consistent with already existing instantiations.
	 * If usesampleinst=false then this is relative to evidence instantiation
	 * given by instantiated fields and instasosd argument, otherwise
	 * relative to sampleinst fields
	 */
	public boolean isLocallyConsistent(RelStruc A,
			OneStrucData inst,
			boolean usesampleinst,
			Hashtable atomhasht,
			int val)
					throws RBNCompatibilityException
	{
		// System.out.print("isloccons for " + this.myatom().asString(A) + " " + truthval);
		boolean result = true;
		int value;
		boolean isinstantiated = false; /* Set to true if this node is already instantiated */

		/* First see whether truthval is consistent with already
		 * existing instantiation information at this node
		 */
		int existsval = -1;
		if (!usesampleinst)
			existsval = this.instantiated;
		else
			existsval = this.sampleinstVal();
		if (existsval != -1)
			isinstantiated = true;
		if (isinstantiated && existsval != val)
			result = false;

		/* Now see whether evaluation of conditional probability
		 * distribution at this node forces a particular value
		 * (local consistency with partial parent configuration)
		 */
		if (result){
			value = this.evaluatesTo(A,inst,usesampleinst,atomhasht);
			if (value != val)
				result =  false;
		}
		/* Now see whether instantiating this node to val is consistent
		 * with existing child instantiations
		 */
		if (result){
			int nextbnint;
			if (!isinstantiated){
				if (!usesampleinst){
					this.instantiated = val;
					inst.add(this.myatom(),val,"?");
				}
				else
					this.sampleinstantiate(val);
			}
			for  (int i=0;i< children.size();i++) {
				PFNetworkNode nextbn = (PFNetworkNode)children.elementAt(i);
				if (!usesampleinst)
					nextbnint = nextbn.instantiated;
				else
					nextbnint = nextbn.sampleinstVal();
				if (nextbnint != -1){
					value = nextbn.evaluatesTo(A,inst,usesampleinst,atomhasht);
					//System.out.print(" " +  nextbn.myatom().asString(A) + ": " + value + " ");
					if (value != -1 && value != nextbnint)
						result = false;
				}
			}

			/* Undo trial instantiation */
			if (!isinstantiated){
				if (!usesampleinst){
					this.instantiated = -1;
					inst.delete(this.myatom());
				}
				else
					this.sampleinstantiate(-1);
			}
		}
		//System.out.println(" ... " + result);
		return result;
	}



	/* Check whether parents or children force a particular instantiation
	 * of this node. If yes, set instantiation value of this node and
	 * add all non-instantiated parents and children, as well as
	 * non-instantiated parents of instantiated children to instnodes
	 */
	public void propagateDeterministic(RelStruc A,
			OneStrucData inst,
			Vector instnodes,
			boolean usesampleinst,
			Hashtable atomhasht)
					throws RBNCompatibilityException,RBNInconsistentEvidenceException,RBNBadSampleException
	{
		//System.out.println("propdet");
		if ((!usesampleinst && this.instantiated == -1) ||  // This condition only not satisfied in initial  call
				(usesampleinst && this.sampleinstVal() == -1)){  // to propagateDeterministic
			boolean[] valconsistent = new boolean[this.numvalues];
			boolean oneconsistent = true;
			for (int val =0;val<this.numvalues;val++) {
				valconsistent[val]=this.isLocallyConsistent(A,inst,usesampleinst,atomhasht,val);
				oneconsistent = (oneconsistent && valconsistent[val]);
			}

			boolean forcedinst = false;
			if (!oneconsistent)
				throwExceptions(usesampleinst);
			else{
				for (int val=0;val<this.numvalues;val++) {
					if (!valconsistent[val]) {
						forcedinst = true;
						if (!usesampleinst){
							this.instantiated = val;
							inst.add(this.myatom(),val,"?");
						}
						//System.out.println("add " + this.myatom().asString(A) + "=false");
						this.sampleinstantiate(val); // also required when !usesampleinst !
						thissampleprob = 1;
					}
				}


				if (forcedinst){
					for  (int i=0;i< this.parents.size();i++){
						PFNetworkNode nextpfnn = (PFNetworkNode)parents.elementAt(i);
						if (!usesampleinst){
							if (nextpfnn.instantiated == -1){
								instnodes.add(nextpfnn);
								//  System.out.println("add to instnodes: " + nextpfnn.myatom().asString(A));
							}
						}
						else
							if (nextpfnn.sampleinstVal() == -1)
								instnodes.add(nextpfnn);
					}
					for  (int i=0;i< this.parents.size();i++){
						PFNetworkNode nextpfnn = (PFNetworkNode)this.parents.elementAt(i);
						if (!usesampleinst){
							if (nextpfnn.instantiated == -1 && !instnodes.contains(nextpfnn)){
								instnodes.add(nextpfnn);
								// System.out.println("add to instnodes: " + nextpfnn.myatom().asString(A));
							}
						}
						else
							if (nextpfnn.sampleinstVal() == -1 && !instnodes.contains(nextpfnn))
								instnodes.add(nextpfnn);
						addOtherParents(this,nextpfnn,instnodes,usesampleinst);
					}
				}
			}
		}
	}




	public int sampledepth(){
		return sampledepth;
	}

	public int sampleinstVal(){
		return sampleinst;
	}

	public void sampleinstantiate(int val){
		sampleinst = val;
	}



	/* Sample an instantiation for this node.
	 *
	 * Probability formulas are evaluated over
	 * RelStruc A
	 *
	 * ComplexBNGroundAtomNode's for
	 * ground atoms on which the evaluation of
	 * probform may depend are accessible via
	 * their asString() name from atomhasht.
	 * If these are not instantiated already,
	 * require recursive sampling
	 *
	 * Returns the sampleweight-factor contributed by this
	 * node: if node is instantiated to false, then
	 * return 1-prob, where prob is the probability for
	 * this node being true, if it was resampled according
	 * to current parent configuration. If node is instantiated
	 * to true, then return prob. If node is not instantiated
	 * return 1.0.
	 */
	public void sample(RelStruc A,
			Hashtable<String,PFNetworkNode> atomhasht,
			OneStrucData inst,
			int sampleordmode,
			int adaptivemode,
			Hashtable<String,Double> evaluated,
			long[] timers,
			boolean verbose)
					throws RBNCompatibilityException,RBNInconsistentEvidenceException,RBNBadSampleException
	{
		switch (sampleordmode){
		case InferenceModule.OPTION_SAMPLEORD_FORWARD:
			sampleForward(A,atomhasht, inst, adaptivemode,evaluated,timers);
			break;
		case InferenceModule.OPTION_SAMPLEORD_RIPPLE:
			sampleRipple(A,atomhasht,inst,adaptivemode,evaluated,timers,verbose);
		}
	}


	/* Not all arguments are needed for implementation of this abstract method in all subclasses! */
	public abstract void sampleForward(RelStruc A,
			Hashtable<String,PFNetworkNode> atomhasht,
			OneStrucData inst,
			int adaptivemode,
			Hashtable<String,Double> evaluated,
			long[] timers)
					throws RBNCompatibilityException;

	public  void sampleRipple(RelStruc A,
			Hashtable<String,PFNetworkNode> atomhasht,
			OneStrucData inst,
			int adaptivemode,
			Hashtable<String,Double> evaluated,
			long[] timers,
			boolean verbose)
					throws RBNCompatibilityException,RBNInconsistentEvidenceException,RBNBadSampleException
	{
		long inittime;
		//System.out.print("Sample Ripple for " + this.myatom().asString(A));
		if (sampleinst == -1){ // Node can already be instantiated due to deterministic propagation
			if (!upstreamofevidence)
				sampleForward(A,atomhasht,inst,adaptivemode,evaluated,timers);
			else{
				inittime = System.currentTimeMillis();
				boolean[] valconsistent = new boolean[this.numvalues];
				boolean oneconsistent = true;
				int onlyconsistent = -2; // index of the only consistent value, if one such exists (-1 for multiple consistent values)
				for (int val =0;val<this.numvalues;val++) {
					valconsistent[val]=this.isLocallyConsistent(A,inst,true,atomhasht,val);
					oneconsistent = (oneconsistent && valconsistent[val]);
					if (onlyconsistent == -2 && valconsistent[val])
						onlyconsistent = val; // the first consistent value
					if (onlyconsistent >=0 && valconsistent[val])
						onlyconsistent = -1; // once and for all!
				}

				timers[2]=timers[2]+ System.currentTimeMillis() - inittime;

				if (!oneconsistent){
					//System.out.println(" (0) ");
					throw new RBNBadSampleException();
				}
				if (onlyconsistent != -1){ // the case onlyconsistent == -2 is already covered by !oneconsistent
					//System.out.println(" (1) ");
					sampleinst = onlyconsistent;
					thissampleprob = 1;
				}

				else {
					// Set value with uniform probability (no conditional distribution used!)
					//System.out.println(" (3) ");
					double rand = Math.random();
					sampleinst = (int)rand*this.numvalues;
					thissampleprob = 1/this.numvalues;
				}

			}
		} // sampleinst == -1
	}


	/* Sets the thisdistrprob field according to current sample
	 * instantiation
	 */
	public abstract void setDistrProb(RelStruc A, 
			Hashtable<String,PFNetworkNode> atomhasht,
			OneStrucData inst,
			Hashtable<String,Double> evaluated,
			long[] timers)
					throws RBNCompatibilityException;

	public void setSampleProb(double p){
		thissampleprob = p;
	}

	public void setSampleDepth(int d){
		sampledepth = d;
	}

	public void setUpstreamOfEvidence(boolean b){
		upstreamofevidence = b;
	}

	public double thissampleprob(){
		return thissampleprob;
	}

	public double thisdistrprob(){
		return thisdistrprob;
	}


	private void throwExceptions(boolean usesampleinst)
			throws RBNInconsistentEvidenceException,RBNBadSampleException
	{
		if (!usesampleinst)
			throw new RBNInconsistentEvidenceException();
		else
			throw new RBNBadSampleException();
	}


	public double[] sampleweight(int val){
		return valsampleweight[val];
	}


	public boolean upstreamOfEvidence(){
		return upstreamofevidence;
	}

    public GnnPy getGnnPy() {
        return gnnPy;
    }

    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }

//	public Vector<PFNetworkNode> buildNodeStack()
//	/* Returns Vector of nodes that are in the connected component
//	   of this PFNetworkNode
//	 */
//	{
//		Vector<PFNetworkNode> nodestack = new Vector<PFNetworkNode>();
//		PFNetworkNode nextnode;
//		if (!visited[4]){
//			nodestack.add(this);
//			this.visited[4]=true;
//			ListIterator<PFNetworkNode> li = parents.listIterator();
//			while (li.hasNext()){
//				nextnode = (PFNetworkNode)li.next();
//				nextnode.buildNodeStack(nodestack);
//			}
//			li = children.listIterator();
//			while (li.hasNext()){
//				((PFNetworkNode)li.next()).buildNodeStack(nodestack);
//			}
//		}
//		resetVisitedUpDownstream(4);
//		return nodestack;
//	}
//
//	private void buildNodeStack(Vector<PFNetworkNode> nodestack)
//	{
//		if (!visited[4]){
//			nodestack.add(this);
//			visited[4]=true;
//			for (PFNetworkNode pfn: parents)
//				pfn.buildNodeStack(nodestack);
//			for (PFNetworkNode pfn: children)
//				pfn.buildNodeStack(nodestack);
//			
//		}
//	}

}
