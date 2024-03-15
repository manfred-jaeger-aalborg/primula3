
/*
 * PFNetwork.java 
 * 
 * Copyright (C) 2003 Max-Planck-Institut fuer Informatik,
 *                    Helsinki Institute for Information Technology
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

import java.util.*;
import RBNpackage.*;
import RBNExceptions.*;
import RBNutilities.*;
import RBNgui.*;
import java.io.*;

public class PFNetwork{


	private Primula myprimula;

	/** Vector of PFNetworkNode
	 *  Contains all nodes in the network
	 * (can consist of several connected components) 
	 */
	private Vector<PFNetworkNode> allnodes; 

	/** 
	 * Hashtable makes nodes accessible by their atom
	 * (needed when evaluating probability 
	 * formulas)
	 *
	 */
	private Hashtable<String,PFNetworkNode> atomhasht;

	private RelStruc A; // Current underlying RelStruc
	private OneStrucData inst;
	private Hashtable<Rel,PFNetworkNode[]> queryPFNnodes; // Contains the PFNnodes corresponding to 
	// to queryatoms (for each relation, in the same order as in InferenceModule.queryatoms)
	private Vector sampleord; // Vector of the uninstantiated nodes in the order in 
	// which they are to be sampled

	private int sampleordmode;
	private int adaptivemode;
	private boolean[] samplelogmode;

	private int num_subsamples_minmax;
	public int getNum_subsamples_minmax() {
		return num_subsamples_minmax;
	}

	public void setNum_subsamples_minmax(int num_subsamples_minmax) {
		this.num_subsamples_minmax = num_subsamples_minmax;
	}


	private int num_subsamples_adapt;

	private int numpar; // Max. number of parents for nodes with full CPT representation

	private double[] allsampleweight; // The sum of weights in importance sampling represented 
		// as a SmallDouble
	private double[][] allsampleweight_subsample;

	private long[] timers = new long[5];



	public PFNetwork(){
		allnodes = new Vector<PFNetworkNode>();
	}

	public PFNetwork(Primula pr, Vector an, RelStruc A, OneStrucData inst){
		myprimula = pr;
		allnodes = an;
		this.A = A;
		this.inst = inst;
	}

	public int allnodesSize(){
		return allnodes.size();
	}


	public double[] allsampleweight(){
		return allsampleweight;
	}

//	public String atomAt(int i){
//	return ((GroundAtomNodeInt)allnodes.elementAt(i)).myatom().asString();
//	}

//	public String atomAt(int i, RelStruc A){
//	return ((GroundAtomNodeInt)allnodes.elementAt(i)).myatom().asString(A);
//	}

	public GroundAtom atomAt(int i){
		return ((GroundAtomNodeInt)allnodes.elementAt(i)).myatom();
	}

	public int instValAt(int i){
		return ((BNNode)allnodes.elementAt(i)).instantiated;
	}



	/* Turns all ComplexPFNetworkNodes in allnodes that 
	 * have at most numpar parents into SimplePFNetworkNodes
	 */
	public void makeSimple(int numpar,OneStrucData inst, RelStruc A)
	throws RBNCompatibilityException
	{
		ComplexPFNetworkNode cpfn;
		for (int i=0;i<allnodes.size();i++){
			cpfn = (ComplexPFNetworkNode)allnodes.elementAt(i);
			if (cpfn.parents.size() <= numpar){
				SimplePFNetworkNode spfn = new SimplePFNetworkNode(cpfn,inst,A);
				allnodes.remove(i);
				allnodes.add(i,spfn);
				for (BNNode nextbnn: spfn.parents) 
					nextbnn.replaceInChildrenList(cpfn,spfn);
				for (BNNode nextbnn: spfn.children) 
					nextbnn.replaceInParentList(cpfn,spfn);
			}
		}
	}


	/* Sets the depth field of all nodes with upstreamofevidence = true to 
	 * the distance to the nearest instantiated node.
	 * Nodes with upstreamofevidence = false receive a sampledepth
	 * of depth + offset
	 *
	 * where offset = maximal sampledepth assigned to upstreamofevidence nodes -
	 *                minimal depth of non-upstreamofevidence nodes 
	 *                +1
	 *
	 * If no instantiated nodes exist in the connected
	 * component of a node, then this node retains the 
	 * original setting of its depth field.
	 *
	 * Similar to BayesConstructor.setDepths(Vector)
	 * (but here situtation is simpler, because all nodes 
	 * are contained in allnodes)
	 */ 
	private void setDepthToRipple(){
		// First some resetting ....
		PFNetworkNode pfnn;
		PFNetworkNode nextpfnn;
		Vector lastlayer = new Vector();
		Vector nextlayer = new Vector();
		int layerindex = 0;
		ListIterator li;
		int offset = 0;
		int minnonupstr = allnodes.size();
		for (int i=0;i<allnodes.size();i++){
			pfnn = (PFNetworkNode)allnodes.elementAt(i);
			pfnn.depthset = false;
			if (!pfnn.upstreamOfEvidence())
				minnonupstr = Math.min(minnonupstr,pfnn.depth());
			if (pfnn.instantiatedTo() != -1 && pfnn.upstreamOfEvidence()){
				nextlayer.add(pfnn);
				pfnn.setSampleDepth(layerindex);
				pfnn.depthset = true;
			}
		}

		while (nextlayer.size()>0){
			layerindex++;
			lastlayer = rbnutilities.clonevector(nextlayer);
			nextlayer = new Vector();
			for (int i=0;i<lastlayer.size();i++){
				pfnn = (PFNetworkNode)lastlayer.elementAt(i);
				li = pfnn.parents.listIterator();
				while (li.hasNext()){
					nextpfnn = (PFNetworkNode)li.next();
					if (!nextpfnn.depthset && pfnn.upstreamOfEvidence()){
						nextlayer.add(nextpfnn);
						nextpfnn.setSampleDepth(layerindex);
						nextpfnn.depthset = true;
					}
				}
				li = pfnn.children.listIterator();
				while (li.hasNext()){
					nextpfnn = (PFNetworkNode)li.next();
					if (!nextpfnn.depthset && pfnn.upstreamOfEvidence()){
						nextlayer.add(nextpfnn);
						nextpfnn.setSampleDepth(layerindex);
						nextpfnn.depthset = true;
					}
				}
			}
		}
		offset = layerindex - minnonupstr;
		for (int i=0;i<allnodes.size();i++){
			pfnn = (PFNetworkNode)allnodes.elementAt(i);
			if (!pfnn.upstreamOfEvidence()){
				pfnn.setSampleDepth(pfnn.depth+offset);
				pfnn.depthset = true;
			}
		}
	}


	private void setDepthToForward(){
		PFNetworkNode pfnn;
		for (int i=0;i<allnodes.size();i++){
			pfnn = (PFNetworkNode)allnodes.elementAt(i);
			pfnn.setSampleDepth(pfnn.depth());
			//System.out.println(pfnn.depth()+ "/" + pfnn.sampledepth());
		}
	}

	private void setUpstreamOfEvidence(){
		Vector pfnnstack = new Vector();
		PFNetworkNode pfnn;
		PFNetworkNode nextpfnn;
		ListIterator li;
		for (int i=0;i<allnodes.size();i++){
			pfnn = (PFNetworkNode)allnodes.elementAt(i);
			if (pfnn.instantiated != -1)
				pfnnstack.add(pfnn);
		}
		while (pfnnstack.size()>0){
			pfnn = (PFNetworkNode)pfnnstack.lastElement();
			pfnnstack.remove(pfnnstack.size()-1);
			pfnn.setUpstreamOfEvidence(true);
			li = pfnn.parents.listIterator();
			while (li.hasNext()){
				nextpfnn=(PFNetworkNode)li.next();
				if (nextpfnn.upstreamOfEvidence() == false){
					pfnnstack.add(nextpfnn); 
				}
			}
		}

	}

	public void makeSampleOrd(int sampleordmode){
		/* First make sure that the depth field of the nodes in all nodes
		 * represents the partial order according to which they are sampled
		 * Initially the depth field contains the genuine depth, which 
		 * corresponds to the partial order for forward sampling
		 */
		setUpstreamOfEvidence();
		switch (sampleordmode){
		case InferenceModule.OPTION_SAMPLEORD_FORWARD:
			setDepthToForward();
			break;
		case InferenceModule.OPTION_SAMPLEORD_RIPPLE:
			setDepthToRipple();
		}
		int maxlevel = 0;
		PFNetworkNode bnn;
		sampleord = new Vector();
		for (int i=0;i<allnodes.size();i++)
			maxlevel = Math.max(maxlevel,((PFNetworkNode)allnodes.elementAt(i)).sampledepth());
		for (int l=0;l<=maxlevel;l++){
			for (int i=0;i<allnodes.size();i++){
				bnn = (PFNetworkNode)allnodes.elementAt(i);
				if (bnn.sampledepth()==l)
					sampleord.add(bnn);
			}
		}
	}



	public void prepareForSampling(int sampleordmode, 
			int adaptivemode, 
			boolean[] samplelogmode,
			int numpar, 
			Hashtable<Rel,GroundAtomList> queryatoms, 
			int num_subsamples_minmax,
			int num_subsamples_adapt,
			BufferedWriter logwriter)
	throws RBNCompatibilityException,RBNInconsistentEvidenceException,IOException
	{
		this.sampleordmode = sampleordmode;
		this.adaptivemode = adaptivemode;
		this.num_subsamples_minmax=num_subsamples_minmax;
		this.num_subsamples_adapt=num_subsamples_adapt;

		this.samplelogmode = samplelogmode;
		allsampleweight = new double[2];
		allsampleweight_subsample = new double[num_subsamples_minmax][2];
		makeSimple(numpar,inst,A);
		sEval(A);
		atomhasht = new Hashtable<String,PFNetworkNode>(allnodes.size(),(float)1.0);
		for (int i=0;i<allnodes.size();i++)
			atomhasht.put(atomAt(i).asString(),allnodes.elementAt(i));

		/* Propagating of evidence instantiation:*/
		Vector instnodes = new Vector(); /* Vector of PFNetworkNodes.
		 * Serves as a stack of instantiated nodes
		 * whose instantiation information still needs 
		 * to be propagated to parents/children
		 */

		PFNetworkNode nextpfn;				 
		for (int i=0;i<allnodes.size();i++){
			nextpfn = (PFNetworkNode)allnodes.elementAt(i);
			if ( nextpfn.instantiated != -1){
				instnodes.add(allnodes.elementAt(i));
				nextpfn.sampleinstantiate(nextpfn.instantiated);
				nextpfn.setSampleProb(1.0);
			}
		}
		propagateDeterministic(instnodes,A,false);

		/* Create the sampling order */
		makeSampleOrd(sampleordmode);

		/* Initial log output */
		if (logwriter != null){
			logwriter.write("# Adaptive sampling: ");
			if (adaptivemode==InferenceModule.OPTION_SAMPLE_ADAPTIVE)
				logwriter.write("on" +'\n');
			else
				logwriter.write("off" +'\n');
			if (samplelogmode[4]){
				logwriter.write("# Nodes: " + allnodes.size() + '\n');
				int[] parentstats = new int[allnodes.size()]; // parentstats[i] should contain
				// the number of nodes with i
				// parents
				PFNetworkNode pfnn;
				for (int i=0;i<allnodes.size();i++){
					pfnn = (PFNetworkNode)allnodes.elementAt(i);
					parentstats[pfnn.parents.size()]++;		    
				}
				logwriter.write("# Node count by number of parents: "  + '\n');
				logwriter.write("# #Parents"  + '\t' + "#Nodes" + '\n');
				for (int i=0;i<allnodes.size();i++){
					if (parentstats[i] != 0)
						logwriter.write("# " + i + '\t' + parentstats[i] + '\n');
				}
				logwriter.write('\n');
				logwriter.flush();
			}
			if (samplelogmode[1]){ 
				logwriter.write("# Evidence: " + '\n');
				if (myprimula.evidencemode() == Primula.OPTION_EVIDENCE_CONDITIONED)
					logwriter.write(inst.printAsString(A,"# "));
				logwriter.write('\n');
				logwriter.flush();
			}
			if (samplelogmode[0]){
				logwriter.write("# Sample order:" + '\n');
				showSampleOrd(A,logwriter);
				logwriter.write('\n');
				logwriter.flush();
			}
//			if (samplelogmode[2] || samplelogmode[3]){
//				logwriter.write("# Iteration ");
//				String nextatom;
//				for (int i=0;i<queryatoms.allAtoms().size();i++){
//					nextatom = ((GroundAtom)queryatoms.atomAt(i)).asString(A);
//					if (samplelogmode[2])
//						logwriter.write(nextatom + " ");
//					if (samplelogmode[3])
//						logwriter.write(nextatom+"_P " + nextatom+"_Min " 
//								+ nextatom+"_Max " + nextatom+"_Var ");
//				}
//				logwriter.write('\n');
//				logwriter.flush();
//			}
		}
		for (int i=0;i<sampleord.size();i++){
			((PFNetworkNode)sampleord.elementAt(i)).initializeForSampling(sampleordmode,
					adaptivemode,
					queryatoms,
					num_subsamples_minmax,
					num_subsamples_adapt);
		}

		/* Create queryPFNnodes */
		queryPFNnodes = new Hashtable<Rel,PFNetworkNode[]>();
		String nextatomstring;
		for (Rel r: queryatoms.keySet()) {
			GroundAtomList gal = queryatoms.get(r);
			PFNetworkNode[] pfnn = new PFNetworkNode[gal.size()];
			queryPFNnodes.put(r,pfnn);
			for (int i=0;i<gal.size();i++) {
				nextatomstring  = ((GroundAtom)gal.atomAt(i)).asString();
				pfnn[i]=(PFNetworkNode)atomhasht.get(nextatomstring);
			}		
		}
	}

	/* Compute all instantiatiations that are deterministically
	 * implied by instasosd
	 * The instantiation instasosd must be consistent with the instantiation
	 * information at allnodes. instasosd can contain instantiation information
	 * on nodes not in allnodes
	 */
	public void propagateDeterministic(Vector instnodes, 
			RelStruc A, 
			boolean usesampleinst)
	throws RBNCompatibilityException,RBNInconsistentEvidenceException
	{
		PFNetworkNode nextpfn;

		/* Process until stack empty */

		while (instnodes.size() > 0){
			nextpfn = (PFNetworkNode)instnodes.lastElement();
			instnodes.remove(instnodes.size()-1);
			try{
				nextpfn.propagateDeterministic(A,inst,instnodes,usesampleinst,atomhasht);
			}
			catch (RBNBadSampleException e) {System.out.println(e);}
		}
	}


	public Rel relAt(int i){
		return ((GroundAtomNodeInt)allnodes.elementAt(i)).myatom().rel();
	}


	public int sampleValAt(int i){
		return ((PFNetworkNode)allnodes.elementAt(i)).sampleinstVal();
	}

	public double thisSampleProbAt(int i){
		return ((PFNetworkNode)allnodes.elementAt(i)).thissampleprob();
	}

	public double thisDistrProbAt(int i){
		return ((PFNetworkNode)allnodes.elementAt(i)).thisdistrprob();
	}

	public double[] sampleWeightAt(int i, int val){
		return ((PFNetworkNode)allnodes.elementAt(i)).valsampleweight[val];
	}

	public void showNodes(){
		for (int i=0;i<allnodes.size();i++){
			System.out.println(atomAt(i).asString() + "sampled: "+ sampleValAt(i) + " inst: " + instValAt(i) 
			+" sampleprob: " + thisSampleProbAt(i) + " distrprob: " + thisDistrProbAt(i) );
		}
	}

//	public void showNodes(RelStruc A){
//	double nodeprob;
//	SimplePFNetworkNode spfn;
//	for (int i=0;i<allnodes.size();i++){
//	nodeprob = trueSampleWeightAt(i)/allsampleweight;
//	String showstring = atomAt(i,A);
//	if (allnodes.elementAt(i) instanceof SimplePFNetworkNode){
//	spfn = (SimplePFNetworkNode)allnodes.elementAt(i);
//	showstring = showstring + " simple ";
//	showstring = showstring +  " " + spfn.showAllTrueWeights() + " " + spfn.showNumTrue();
//	showstring = showstring +  " " + spfn.showAllFalseWeights() + " " + spfn.showNumFalse() + " " ;
//	//showstring = showstring + StringOps.arrayToString(((SimplePFNetworkNode)allnodes.elementAt(i)).mycpt());
//	}
//	else 
//	showstring = showstring + " complex ";
//	showstring = showstring + instValAt(i) + " "+ nodeprob;
//	System.out.println(showstring);
//	}
//	}


//	public void showAllnodes(RelStruc A){
//	ListIterator li;
//	for (int i=0;i<allnodes.size();i++){
//	PFNetworkNode pfnn = (PFNetworkNode)allnodes.elementAt(i);
//	System.out.print(pfnn.myatom().asString(A) + " " 
//	+ pfnn.sampledepth() + "/" + pfnn.depth()+ " " 
//	+ pfnn.upstreamOfEvidence()+ " "
//	+ pfnn.instantiated + "/"
//	+ pfnn.sampleinstVal()+ "/"
//	+ pfnn.thissampleprob()+ " "
//	+  " children: " );
//	// 	    li = pfnn.children.listIterator();
//	// 	    while (li.hasNext()){
//	// 		System.out.print(((PFNetworkNode)li.next()).myatom().asString(A) + " " );
//	// 	    }
//	System.out.println();
//	}
//	}

	public void showSampleOrd(RelStruc A, BufferedWriter lwr)
	throws IOException
	{
		ListIterator li;
		for (int i=0;i<sampleord.size();i++){
			PFNetworkNode pfnn = (PFNetworkNode)sampleord.elementAt(i);
			String outstring = "# ";
			outstring = outstring + pfnn.myatom().asString(A) + " ";
			if (pfnn instanceof SimplePFNetworkNode)
				outstring = outstring + "[s] ";
			else
				outstring = outstring + "[c] ";
			//outstring = outstring + pfnn.sampledepth() + "/" + pfnn.depth()+ " " ;
			//outstring = outstring + pfnn.upstreamOfEvidence()+ " ";
			//outstring = outstring + pfnn.instantiated + "/";
			if (pfnn.instantiated == -1)
				outstring = outstring + " no ev";
			if (pfnn.instantiated == 0)
				outstring = outstring + " ev: false";
			if (pfnn.instantiated == 1)
				outstring = outstring + " ev: true";
			//outstring = outstring + pfnn.sampleinstVal()+ "/";
			//outstring = outstring + pfnn.thissampleprob()+ " ";

			lwr.write(outstring + '\n');
		}
	}


	/** Sample one instantiation; set 
	 * instantiated field of the nodes
	 * accordingly
	 * <p>
	 * If instasosd is nonempty, then do importance sampling
	 * of the conditional distribution given instasosd, and
	 * return the weight of the sample
	 * <p>
	 * Instantiated nodes that become isolated prob zero nodes
	 * will not contribute to the weight -- this is 
	 * no problem, because their weight is constant for 
	 * all samples
	 * <p>
	 * subsind is the index of the subsample for this instantiation
	 */
	public double[] sampleInst(int subsind, boolean verbose)    
	throws RBNCompatibilityException,RBNInconsistentEvidenceException
	{
		//double sampleprob = 1; 
		//double distrprob = 1;
		double importance[] = {1.0,0.0};
		//double dp = 0;
		PFNetworkNode nextpfnn;
		boolean badsample = false;
		long inittime;
		Vector instnodes;
		//PFNetworkNode topnode;
		/* 
		 * reset sampleinst and thissampleweight fields
		 * at all nodes
		 */
		for (int i=0;i<allnodes.size();i++){
			((PFNetworkNode)allnodes.elementAt(i)).initializeForNextSample();
		}
		
		Hashtable<String,Double> evaluated = new Hashtable<String,Double>();
		//Hashtable<String,Double> evaluated = null;
		
		for (int i=0;i<sampleord.size() && !badsample;i++){
			nextpfnn = (PFNetworkNode)sampleord.elementAt(i);
			
			inittime = System.currentTimeMillis();
			try {
				nextpfnn.sample(A,atomhasht,inst,sampleordmode,adaptivemode,evaluated,timers,verbose);
			}
			catch (RBNBadSampleException e){
				badsample = true;
				System.out.print("BS");
			}
			timers[3]= timers[3]+System.currentTimeMillis() - inittime;
			if (sampleordmode ==  InferenceModule.OPTION_SAMPLEORD_RIPPLE && nextpfnn.upstreamOfEvidence()){
				instnodes = new Vector();
				instnodes.add(nextpfnn);
				inittime = System.currentTimeMillis();
				propagateDeterministic(instnodes,A,true);
				timers[1]=timers[1] + System.currentTimeMillis() - inittime;
			}
		}

		/* Compute importance
		 */
		inittime = System.currentTimeMillis();
		if (!badsample){

			/* When sampleordmode = ripple then first need to set 
			 * thisdistrprob fields at all nodes
			 */

			for (int i=0;i<sampleord.size();i++){
				nextpfnn = (PFNetworkNode)sampleord.elementAt(i);
				if (sampleordmode == InferenceModule.OPTION_SAMPLEORD_RIPPLE)
					nextpfnn.setDistrProb(A,atomhasht,inst,evaluated,timers);
				if (verbose){
					System.out.println(nextpfnn.myatom().asString(A) + ": dp: " + nextpfnn.thisdistrprob() 
							+ " sp: " + nextpfnn.thissampleprob());
				}
				importance = SmallDouble.divide(SmallDouble.multiply(importance,nextpfnn.thisdistrprob()),nextpfnn.thissampleprob());
				//System.out.println(importance);
			}
		}
		else {
			importance[0] = 0;
			importance[1] = 0;
		}
		if (verbose)
			System.out.print(" i: " + importance);
		timers[4]=timers[4]+ System.currentTimeMillis() - inittime;

		/*
		 * Update truesampleweight and  allsampleweights at all nodes
		 */
		if (!badsample || adaptivemode == InferenceModule.OPTION_SAMPLE_ADAPTIVE){
			for (int i=0;i<allnodes.size();i++){
				nextpfnn = (PFNetworkNode)allnodes.elementAt(i);
				nextpfnn.addToSampleweight(importance,subsind,nextpfnn.sampleinstVal());
				if (adaptivemode == InferenceModule.OPTION_SAMPLE_ADAPTIVE && nextpfnn instanceof SimplePFNetworkNode){
					((SimplePFNetworkNode)nextpfnn).updateconditionalsampleweights();
				}
			}
		} 

		//		/* Update subsample statistics at querynodes
//		*/
//		for (int i=0;i<queryPFNnodes.length;i++){
//		nextpfnn = (SimplePFNetworkNode)queryPFNnodes[i];
//		}

//		System.out.print(".");
//		System.out.flush();
		allsampleweight = SmallDouble.add(allsampleweight,importance);
		allsampleweight_subsample[subsind] = SmallDouble.add(allsampleweight_subsample[subsind],importance);
		return importance;
	}





	public void setSampleProbs(SampleProbs sps,
			int num_subsamples, 
			BufferedWriter logwriter)
	throws IOException
	{
//		for (int j=0;j<num_subsamples;j++){
//			System.out.print(StringOps.arrayToString(queryPFNnodes[0].truesampleweight_subsample(j),"(",")")+'\t');
//		}
//		System.out.println();
//		for (int j=0;j<num_subsamples;j++){
//			System.out.print(StringOps.arrayToString(
//					SmallDouble.subtract(allsampleweight_subsample[j],queryPFNnodes[0].truesampleweight_subsample(j)),"(",")")+'\t');
//		}
//		System.out.println();
//		System.out.println("************");
		
		double[] nextprob;
		double[] min,max,var,nextprob_subsample;
		for (Rel r: queryPFNnodes.keySet()) {
			PFNetworkNode[] pfnns = queryPFNnodes.get(r);
			for (int i=0;i<pfnns.length;i++){
				int numvals = pfnns[i].getNumvalues();
				nextprob = SmallDouble.toStandardDoubleArray(
						SmallDouble.divide(pfnns[i].valsampleweight,
								allsampleweight));
				if (logwriter != null && (samplelogmode[2] || samplelogmode[3]))
					logwriter.write(nextprob + " ");
				sps.setProb(r,nextprob,i);
				var = new double[numvals];

				for (int k=0;k<num_subsamples;k++){
					nextprob_subsample = SmallDouble.toStandardDoubleArray(
							SmallDouble.divide(pfnns[i].valsampleweight_subsample[k],
									allsampleweight_subsample[k]));
					var = rbnutilities.arrayAdd(var ,
							rbnutilities.arrayCompPow(rbnutilities.arraySubtract(nextprob,nextprob_subsample),2.0));
				}
				var = rbnutilities.arrayScalMult(var,1/(double)num_subsamples);
				sps.setVar(r,var,i);
			}
			if (logwriter != null && (samplelogmode[2] || samplelogmode[3])){
				logwriter.write('\n');
				logwriter.flush();
			}
		}
	}


//	public void logSampleProbs(BufferedWriter logwriter)
//	throws java.io.IOException
//	{
//	double nextprob;
//	for (int i=0;i<queryPFNnodes.length;i++){
//	nextprob = queryPFNnodes[i].truesampleweight()/allsampleweight;
//	logwriter.write(nextprob + " ");

//	}
//	logwriter.write('\n');
//	}





	private void sEval(RelStruc A)
	throws RBNCompatibilityException
	{
		for (int i=0;i<allnodes.size();i++)
			if (allnodes.elementAt(i) instanceof ComplexPFNetworkNode)
				((ComplexPFNetworkNode)allnodes.elementAt(i)).sEval(A);
	}

}
