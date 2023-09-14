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


    protected GroundAtom myatom;
    protected int sampledepth; /* Defines a partial order for sampling: nodes with depth d must
				* be sampled before nodes with depth d+1.
				* Setting depends on chosen sampling order
				*/



    protected boolean upstreamofevidence; /* Set to true if this node is upstream of some
					   * instantiated node
					   */

    protected int sampleinst;  /* The instantiation of this ground atom in a current sample */

    protected int sampleparentconfig; /* Index of parent configuration in current
				       * sample -- not used in all modes for sampling
				       */

    protected double thissampleprob; /* The probability with which this node was set to its sampleinst value
				      * in the current sample
				      */

    protected double thisdistrprob; /* The conditional probability of the sampleinst value of this node
				     * given the sampleinst values of its parents according to the
				     * underlying distribution
				     */


    /** The sum of the likelihood weights of all samples in which this
	* node was instantiated true;
	* 
	* Represented as SmallDouble
	**/
    protected double[] truesampleweight; 

    /** The same but distributed over num_subsamples subsamples
     * only used at querynodes
     **/
    protected double[][] truesampleweight_subsample = null; 


    public PFNetworkNode(GroundAtom at){
	super(at.asString());
	myatom = at;
	sampleinst = -1;
	thissampleprob = -1;
	thisdistrprob = -1;
	truesampleweight = new double[2];
	upstreamofevidence = false;
    }

    public void addToTruesampleweight(double[] d,int subsind){
	truesampleweight = SmallDouble.add(truesampleweight, d);
	if (truesampleweight_subsample != null)
	    truesampleweight_subsample[subsind]=SmallDouble.add(truesampleweight_subsample[subsind],d);
    }


    private void  addOtherParents(PFNetworkNode notthis,
				  PFNetworkNode pfnn,
				  Vector instnodes,
				  boolean usesampleinst){
	ListIterator li = pfnn.parents.listIterator();
	PFNetworkNode nextpfnn;
	while (li.hasNext()){
	    nextpfnn = (PFNetworkNode)li.next();
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

    /* vec is vector of PFNetworkNodes;
     * returns 1 if vec contains at least one node with a
     * sample-instantiation 1, else returns 0
     */
    private int containsTrue(Vector vec){
	int result = 0;
	int ind = 0;
	PFNetworkNode pfnn;
	while (result == 0 && ind<vec.size()){
	    pfnn = (PFNetworkNode)vec.elementAt(ind);
	    if (pfnn.sampleinstVal()==1)
		result = 1;
	    ind++;
	}
	return result;
    }


    /* Return 0 resp. 1 if the probability of this node is 0 resp. 1
     * when evaluated over A and w.r.t. to given partial instantiation instasosd
     * Return -1 if neither is the case
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
    		GroundAtomList queryatoms,
    		int num_subsamples_minmax,
    		int num_subsamples_adapt)
    {
    	truesampleweight = new double[2];
    	if (queryatoms.contains(myatom)){
    		truesampleweight_subsample = new double[num_subsamples_minmax][2];
    	}
    }


    /* Checks whether instantiating this node with truthval (0 or 1)
     * is locally consistent with already existing instantiations.
     * If usesampleinst=false then this is relative to evidence instantiation
     * given by instantiated fields and instasosd argument, otherwise
     * relative to sampleinst fields
     */
    public boolean isLocallyConsistent(RelStruc A,
				       OneStrucData inst,
				       boolean usesampleinst,
				       Hashtable atomhasht,
				       int truthval)
    throws RBNCompatibilityException
    {
	// System.out.print("isloccons for " + this.myatom().asString(A) + " " + truthval);
	boolean result = true;
	int value;
	boolean isinstantiated = false; /* Set to true if this node is already instantiated */

	/* First see whether truthval is consistent with already
	 * existing instantiation information at this node
	 */
	int existtruthval = -1;
	if (!usesampleinst)
	    existtruthval = this.instantiated;
	else
	    existtruthval = this.sampleinstVal();
	if (existtruthval != -1)
	    isinstantiated = true;
	if (isinstantiated && existtruthval != truthval)
	    result = false;

	/* Now see whether evaluation of conditional probability
	 * distribution at this node forces a particular value
	 * (local consistency with partial parent configuration)
	 */
	if (result){
	    value = this.evaluatesTo(A,inst,usesampleinst,atomhasht);
	    if ((truthval == 1 && value == 0) || (truthval == 0 && value == 1))
		result =  false;
	 }
	 /* Now see whether instantiating this node to truthval is consistent
	  * with existing child instantiations
	  */
	if (result){
	    int nextbnint;
	    if (!isinstantiated){
		if (!usesampleinst){
		    this.instantiated = truthval;
		    inst.add(this.myatom(),truthval,"?");
		}
		else
		    this.sampleinstantiate(truthval);
	    }
	    ListIterator li = children.listIterator();
	    PFNetworkNode nextbn;
	    while (li.hasNext()){
		nextbn = (PFNetworkNode)li.next();
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
	    boolean trueisconsistent = this.isLocallyConsistent(A,inst,usesampleinst,atomhasht,1);
	    boolean falseisconsistent = this.isLocallyConsistent(A,inst,usesampleinst,atomhasht,0);
	    boolean forcedinst = false;
	    if (!trueisconsistent && !falseisconsistent)
		throwExceptions(usesampleinst);
	    else{
		if (!trueisconsistent){
		    forcedinst = true;
		    if (!usesampleinst){
			this.instantiated = 0;
			inst.add(this.myatom(),false,"?");
		    }
		    //System.out.println("add " + this.myatom().asString(A) + "=false");
		    this.sampleinstantiate(0); // also required when !usesampleinst !
		    thissampleprob = 1;
		}
		if (!falseisconsistent){
		    forcedinst = true;
		    if (!usesampleinst){
			this.instantiated = 1;
			inst.add(this.myatom(),true,"?");
		    }
		    //System.out.println("add " + this.myatom().asString(A) + "=true");
		    this.sampleinstantiate(1);
		    thissampleprob = 1;
		}
		if (forcedinst){
		    ListIterator li;
		    PFNetworkNode nextpfnn;
		    li = this.parents.listIterator();
		    while (li.hasNext()){
			nextpfnn = (PFNetworkNode)li.next();
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
		    li = this.children.listIterator();
		    while (li.hasNext()){
			nextpfnn = (PFNetworkNode)li.next();
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
    		Hashtable atomhasht,
    		OneStrucData inst,
    		int sampleordmode,
    		int adaptivemode,
    		long[] timers,
    		boolean verbose)
    throws RBNCompatibilityException,RBNInconsistentEvidenceException,RBNBadSampleException
    {
    	switch (sampleordmode){
    	case InferenceModule.OPTION_SAMPLEORD_FORWARD:
    		sampleForward(A,atomhasht, inst, adaptivemode,timers);
    		break;
    	case InferenceModule.OPTION_SAMPLEORD_RIPPLE:
    		sampleRipple(A,atomhasht,inst,adaptivemode,timers,verbose);
    	}
    }


    /* Not all arguments are needed for implementation of this abstract method in all subclasses! */
    public abstract void sampleForward(RelStruc A,Hashtable atomhasht,OneStrucData inst,int adaptivemode,long[] timers)
	throws RBNCompatibilityException;

    public  void sampleRipple(RelStruc A,
			      Hashtable atomhasht,
			      OneStrucData inst,
			      int adaptivemode,
			      long[] timers,
			      boolean verbose)
	throws RBNCompatibilityException,RBNInconsistentEvidenceException,RBNBadSampleException
    {
	long inittime;
	//System.out.print("Sample Ripple for " + this.myatom().asString(A));
	if (sampleinst == -1){ // Node can already be instantiated due to deterministic propagation
	    if (!upstreamofevidence)
		sampleForward(A,atomhasht,inst,adaptivemode,timers);
	    else{
		inittime = System.currentTimeMillis();
		boolean trueiscons = isLocallyConsistent(A,inst,true,atomhasht,1);
		boolean falseiscons = isLocallyConsistent(A,inst,true,atomhasht,0);
		timers[2]=timers[2]+ System.currentTimeMillis() - inittime;

		if (!trueiscons && !falseiscons){
		    //System.out.println(" (0) ");
		    throw new RBNBadSampleException();
		}
		if (trueiscons && !falseiscons){
		    //System.out.println(" (1) ");
		    sampleinst = 1;
		    thissampleprob = 1;
		}
		if (!trueiscons && falseiscons){
		    //System.out.println(" (2) ");
		    sampleinst = 0;
		    thissampleprob = 1;
		}
		if (trueiscons && falseiscons){
		    // Set value with prob. 1/2 (no conditional distribution used!)
		    //System.out.println(" (3) ");
		    double rand = Math.random();
		    if (rand > 0.5)
			sampleinst = 1;
		    else
			sampleinst = 0;
		    thissampleprob = 0.5;
		}
		if (verbose)
		    System.out.println("sample value for " + this.myatom().asString(A) + ": "
				       + sampleinst + " " + trueiscons + "/" + falseiscons);

		//propagateDeterministic(A,instasosd,new Vector(),true,atomhasht);

	    }
	} // sampleinst == -1
	else {
	    //System.out.println(" isinstantiated ");
	    //thissampleprob = 1;
	}
   }


    /* Sets the thisdistrprob field according to current sample
     * instantiation
     */
    public abstract void setDistrProb(RelStruc A, Hashtable atomhasht,OneStrucData inst,long[] timers)
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


    public double[] truesampleweight(){
	return truesampleweight;
    }


    public double[] truesampleweight_subsample(int i){
	return truesampleweight_subsample[i];
    }




    public boolean upstreamOfEvidence(){
	return upstreamofevidence;
    }


}
