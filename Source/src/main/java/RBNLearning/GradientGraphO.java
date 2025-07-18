/*
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

import java.util.*;
import java.util.concurrent.TimeUnit;

import PyManager.GnnPy;
import RBNpackage.*;
import RBNgui.*;
import RBNExceptions.*;
import RBNutilities.*;
import RBNinference.*;
import myio.StringOps;

/** Main class for RBN parameter learning. The Gradient Graph is a representation of the
 * likelihood function given data consisting of pairs of relational input domains (objects of
 * type RelStruc), and observed values of the probabilistic relations (given as objects of type
 * Instantiation). Each pair may contain a different input domain, or there may be multiple
 * observations of the probabilistic relations for one input domain.
 *
 * Nodes in the gradient graph correspond to ground probability formulas obtained from
 * recursively evaluating the probability formulas corresponding to the ground atoms in the
 * Instantiations.  Identical ground (sub-) formulas obtained from the evaluation of different
 * instantiated ground atoms are included only once in the GradientGraphO. For this purpose a
 * hashtable allNodes for the nodes is maintained. The keys for the nodes are constructed as
 * strings consisting of a concatenation of the index of the data case with the string representation
 * of the ground probability formula.
 *
 * Example: the probabilistic relation r(x,y) is defined by F(x,y) = (s(x,y):t(y),0.6).
 * In both the first and the second data pair the ground atom r(4,7) is observed to be true.
 * Then two nodes will be constructed, one with key 1.(s(4,7):t(7),0.6), and one with key
 * 2.(s(4,7):t(7),0.6). Since the sub-formulas s(4,7) and t(7) may evaluate differently
 * in the two data pairs, these formulas have to be distinguished. If, for example, s(4,7)
 * is observed to be true in the first data pair, and false in the second, then a further
 * nodes with key 1.t(7) will be constructed, but no node 2.t(7).
 *
 *
 *
 * @author jaeger
 *
 */
public class GradientGraphO extends GradientGraph{


	private Hashtable<String,GGCPMNode> allNodes;

	/* Maximum identifier value currently assigned to a node;
	 *
	 */
	private int maxid;


	GGLikelihoodNode llnode;
	Vector<GGAtomSumNode> sumindicators; /* All the indicators for atoms to be summed over */
	Hashtable<Rel,Vector<GGAtomMaxNode>> maxindicators; /* for given rel, contains the GGAtomMaxNodes in the same order as
	defined by the GroundAtomList mapatoms.get(rel) */


	GGConstantNode[] paramNodes; /* All the constant (i.e. parameter) nodes */

	private GnnPy gnnPy;
	private boolean gnnIntegration;
	private int mapSearchAlg;
	private int nIterGreedy;

	private PrimulaGUI primulaGUI;
	public int num_iter;
	public double cooling_fact;
	private int batchSearchSize;
	private int sampleSizeScoring;
	private int lookaheadSearch;

	// https://stackoverflow.com/questions/4573123/java-updating-text-in-the-command-line-without-a-new-line
	// for now the function will not be integrated (merging conflicts)
	private static void printProgress(long startTime, long total, long current) {
		long eta = current == 0 ? 0 :
			(total - current) * (System.currentTimeMillis() - startTime) / current;

		String etaHms = current == 0 ? "N/A" :
			String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
					TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
					TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));

		StringBuilder string = new StringBuilder(140);
		int percent = (int) (current * 100 / total);
		string
		.append('\r')
		.append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
		.append(String.format(" %d%% [", percent))
		.append(String.join("", Collections.nCopies(percent, "=")))
		.append('>')
		.append(String.join("", Collections.nCopies(100 - percent, " ")))
		.append(']')
		.append(String.join("", Collections.nCopies((int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
		.append(String.format(" %d/%d, ETA: %s", current, total, etaHms));

		System.out.print(string);
	}

	public GradientGraphO(Primula mypr,
			RelData data,
			Hashtable<String,Integer> params,
			double[][] mmbds,
			GradientGraphOptions go,
			Hashtable<Rel,GroundAtomList> mapats,
			int m,
			Boolean showInfoInPrimula)
					throws RBNCompatibilityException
	{
		super(mypr,data,params,go,mapats,m,showInfoInPrimula);

		num_iter = 0;
		cooling_fact = 0.0;

		RBN rbn = myPrimula.getRBN();

		this.minmaxbounds=mmbds;

		//parameters = myPrimula.getParamNumRels();
		allNodes = new Hashtable<String,GGCPMNode>();


		sumindicators = new Vector<GGAtomSumNode>();
		//maxindicators = new Vector<GGAtomMaxNode>();
		maxindicators = new Hashtable<Rel,Vector<GGAtomMaxNode>> ();

		int inputcaseno;
		int observcaseno;
		RelDataForOneInput rdoi;
		RelStruc A;
		OneStrucData osd;
		CPModel nextcpm;
		int[] nexttup;



		//		/* Determine how many of the parameters are rbn parameters
		//		 * NOTE: it is required that in parameters all rbn parameters come
		//		 * first, followed by numeric relations parameters. Otherwise the subsequent
		//		 * usage made of maxrbnparam would be false */
		//		maxrbnparam = -1;
		//		for (int i=0;i<parameters.size();i++)
		//			if (myPrimula.isRBNParameter(parameters[i]))
		//				maxrbnparam ++;

		llnode = new GGLikelihoodNode(this);
		/* Create all the ground probability formulas for the atoms in data
		 *
		 */

		Rel nextrel;
		Vector<int[]> inrel;
		String[] vars; /* The argument list for nextpf */
		CPModel groundnextcpm;
		String atomstring;
		GGCPMNode fnode;
		boolean dependsonmissing = false;

		/* First get (approximate) count of all upper ground atom nodes that
		 * need to be constructed (to support progress report)
		 *
		 * Includes count for atoms that will actually not be included, because they
		 * do not depend on parameters.
		 */
		int ugacounter = 0;
		for ( inputcaseno=0; inputcaseno<data.size(); inputcaseno++){
			rdoi = data.caseAt(inputcaseno);
			for (observcaseno=0; observcaseno<rdoi.numObservations(); observcaseno++){
				osd = rdoi.oneStrucDataAt(observcaseno);
				for (int i=0; i<rbn.NumPFs(); i++){
					nextrel = rbn.relAt(i);
					ugacounter = ugacounter + osd.allInstantiated(nextrel).size();
				}
			}
		}

		if (mode == MAPMODE || mode == LEARNANDMAPMODE)
			ugacounter = ugacounter + mapatoms.size();

		int processedcounter = 0;
		int currentpercentage = 0;
		if (showInfoInPrimula)
			if (myPrimula.getPrimulaGUI() != null)
				myPrimula.getPrimulaGUI().appendMessageThis("0%");



		/* Start by constructing upper ground atom nodes for the map-query atoms
		 *
		 */
		if (myggoptions.ggverbose())
			System.out.println("Building GG:");
		if (mode == MAPMODE || mode == LEARNANDMAPMODE){
			int[] naargs;
			GroundAtom nextatom;
			GGAtomMaxNode ggmn;

			if (myggoptions.ggverbose())
				System.out.println("\t-constructing uga nodes for map-query atom...");
			long startTime = System.currentTimeMillis();
			for (Rel narel: mapatoms.keySet()) {
				for (int qano=0; qano<mapatoms.get(narel).size(); qano++){
					nextatom = mapatoms.get(narel).atomAt(qano);
					narel = nextatom.rel();
					naargs = nextatom.args();
					nextcpm = rbn.cpmodel(narel);
					vars = rbn.args(narel);
					groundnextcpm = nextcpm.substitute(vars,naargs);

					/* When MAP inference is performed, then there only is a single
					 * input structure, and a single observed case for the input
					 * (= evidence). Therefore, here always inputcaseno=observcaseno=0.
					 *
					 * In principle, one can also have mode != LEARNMODE, and still
					 * inputcaseno > 0 or observcaseno > 0. In that case, the query
					 * atoms would be interpreted as relating to the first data case
					 * given by inputcaseno=observcaseno=0.
					 */
					rdoi = data.caseAt(0);
					A = rdoi.inputDomain();
					osd = rdoi.oneStrucDataAt(0);



					fnode = GGCPMNode.constructGGPFN(this,
							groundnextcpm,
							allNodes,
							A,
							osd,
							0,
							0,
							parameters,
							false,
							true,
							nextatom.asString(),
							mapatoms,
							null); /* TODO: optimization with a non-null Hashtable here */

					fnode.setMyatom(nextatom.asString());

					/* Need to find/construct a GGAtomMaxNode for this query atom
					 * If no other probform in the graph depends on this query atom, then this node
					 * will not be connected to the rest of the graph (except by its membership in
					 * llnode.instvals). It is then only needed to
					 * store the current instantiation for the query atom.
					 */


					ProbFormAtom atomAsPf = new ProbFormAtom(narel,naargs);
					ggmn = (GGAtomMaxNode)findInAllnodes(atomAsPf,0,0,A);
					if (ggmn == null){
						ggmn = new GGAtomMaxNode(this,atomAsPf,A,osd,0,0);
						allNodes.put(makeKey(atomAsPf,0,0,A),ggmn);
					}
					//llnode.addToChildren(fnode,ggmn);
					llnode.addToChildren(fnode);
					fnode.setIsuga(true);
					fnode.setMyindicator(ggmn);
					fnode.setInstvalToIndicator();
					ggmn.setUGA(fnode);
					processedcounter++;
					if (showInfoInPrimula && (10*processedcounter)/ugacounter > currentpercentage){
						if (myPrimula.getPrimulaGUI() != null)
							myPrimula.getPrimulaGUI().appendMessageThis("X");
						currentpercentage++;
					}
				}
				if (myggoptions.ggverbose())
					System.out.println("\t-uga map-query atoms constructed in: " + (System.currentTimeMillis()-((double)startTime))/1000.0 + " sec.");
			} //for (Rel narel: mapatoms.keySet())
		} // if (mode == MAPMODE || mode == LEARNANDMAPMODE)

		//this.showMaxNodes();

		/* Now construct the nodes for the data/evidence atoms
		 *
		 */
		if (myggoptions.ggverbose())
			System.out.println("\t-constructing nodes for data/evidence atom...");
		int numNodes = 0;
		long startTime = System.currentTimeMillis();
		for (inputcaseno=0; inputcaseno<data.size(); inputcaseno++){

			rdoi = data.caseAt(inputcaseno);
			A = rdoi.inputDomain();
			for (observcaseno=0; observcaseno<rdoi.numObservations(); observcaseno++){
				osd = rdoi.oneStrucDataAt(observcaseno);

				Hashtable<String,Object[]>  evaluated = new Hashtable<String,Object[]>();
				//System.out.println("\t\tnum rbn pfs: " + rbn.NumPFs());
				for (int i=0; i<rbn.NumPFs(); i++){
					nextcpm = rbn.cpmod_prelements_At(i);
					vars = rbn.arguments_prels_At(i);
					nextrel = rbn.relAt(i);

					long startTimeProg = System.currentTimeMillis();

					inrel=osd.allInstantiated(nextrel);
//					if (debugPrint) {
//						System.out.println("next rel: " + nextrel.name());
//						System.out.println();}
					for (int k=0;k<inrel.size();k++){
						nexttup = (int[])inrel.elementAt(k);
						int instvalue = (int)osd.valueOf(nextrel,nexttup); // A bit complicated; should directly get
						// from osd instantiated and their values.
//						if (vars.length == 0)
//							vars = new String[]{""};
						groundnextcpm = nextcpm.substitute(vars,nexttup);
						atomstring = nextrel.name()+StringOps.arrayToString((int[])inrel.elementAt(k),"(",")");
						//							System.out.print("\r\t\t\tcurrent atom: " + atomstring);
//						if (debugPrint)
//							printProgress(startTimeProg, inrel.size(), k+1); // we keep deatciate for now

						/* check whether this atom has already been included as an upper ground atom node because
						 * it is a map atom
						 */
						// TODO: check correct handling of mapatoms that are also instantiated by the evidence!
						if (mapatoms == null || mapatoms.get(nextrel)==null || !mapatoms.get(nextrel).contains(nextrel,nexttup)){

//							if (groundnextcpm instanceof CPMGnn && ((CPMGnn) groundnextcpm).getGnnPy() == null)
//								((CPMGnn) groundnextcpm).setGnnPy(tempGNN);

							Object pfeval = groundnextcpm.evaluate(A,
									osd,
									new String[0],
									new int[0],
									0, // irrelevant because valonly=true
									false,
									false,
									mapatoms,
									false,
									evaluated,
									parameters,
									ProbForm.RETURN_ARRAY,
									true, //valonly
									null)[0];

							if (myggoptions.aca()){
								dependsonmissing = groundnextcpm.dependsOn("unknown_atom",A,osd);
							}

							boolean undefined = ((pfeval instanceof Double && Double.isNaN((Double)pfeval))||
									pfeval instanceof double[] && Double.isNaN(((double[])pfeval)[0]));

							//							System.out.println("undefined = " + undefined);

							if (undefined && !(myggoptions.aca() && dependsonmissing)){
								/* if pfeval != Double.NaN, then this groundnextpf has a constant value
								 * independent of parameter settings or instantiation of unknown
								 * atoms. For a correct numeric value of the likelihood this value
								 * would need to be considered, but for maximizing the likelihood
								 * it is irrelevant
								 */
								fnode = GGCPMNode.constructGGPFN(this,
										groundnextcpm,
										allNodes,
										A,
										osd,
										inputcaseno,
										observcaseno,
										parameters,
										false, //use current Pvals
										true,  // is uga
										atomstring,
										mapatoms,
										evaluated);
								llnode.addToChildren(fnode);
								fnode.setInstval(instvalue);

								fnode.setMyatom(atomstring);
								fnode.setIsuga(true);
								numNodes++;
							}
							else if (!undefined){
								double likfactor =0;
								if (pfeval instanceof Double) {
									switch (instvalue) {
									case 0:
										likfactor = 1-(Double)pfeval;
										break;
									case 1:
										likfactor = (Double)pfeval;
									}
								}
								if (pfeval instanceof double[]) {
									likfactor = ((double[])pfeval)[instvalue];
								}

								loglikconstant = loglikconstant + Math.log(likfactor);
							}
							processedcounter++;
							if (showInfoInPrimula && (10*processedcounter)/ugacounter > currentpercentage){
								if (myPrimula.getPrimulaGUI() != null)
									myPrimula.getPrimulaGUI().appendMessageThis("X");
								currentpercentage++;
							}
						} /* if (!mapatoms.contains(nextrel,nexttup)) */
					} /* for (int k=0;k<inrel.size();k++) */
											System.gc();
					//System.out.println();
				} /* for int i; i<rbn.NumPFs()*/
			} /* int j=0; j<rdoi.numObservations(); */

		}
		if (myggoptions.ggverbose())
			System.out.println("\t-finished constructing " + numNodes + " nodes in " + (System.currentTimeMillis()-((double)startTime))/1000.0 + " sec.");

		if (showInfoInPrimula)
			if (myPrimula.getPrimulaGUI() != null)
				myPrimula.getPrimulaGUI().appendMessageThis("100%");

		/* Construct ProbFormNodes for all SumIndicator nodes.
		 */
		GroundAtom at;
		int[] nextarg;

		for (GGAtomSumNode nextggin: sumindicators) {
			at = nextggin.myatom();
			nextarg = at.args();
			inputcaseno = nextggin.inputcaseno();
			observcaseno = nextggin.observcaseno();
			nextcpm = rbn.cpmodel(at.rel());
			vars = rbn.args(at.rel());
			groundnextcpm = nextcpm.substitute(vars,nextarg);
			/** Note that (arbitrarily) the truthval of the constructed
			 * node is set to true. This initial setting must always be overridden
			 * by some sample value for this node
			 */
			fnode =  GGCPMNode.constructGGPFN(this,
					groundnextcpm,
					allNodes,
					data.elementAt(inputcaseno).inputDomain(),
					new OneStrucData(data.elementAt(inputcaseno).oneStrucDataAt(observcaseno)),
					inputcaseno,
					observcaseno,
					parameters,
					false,
					true,
					at.asString(),
					mapatoms,
					null); /* TODO: optimization with a non-null Hashtable here */
			llnode.addToChildren(fnode);
			fnode.setIsuga(true);
			fnode.setMyatom(at.asString());
			fnode.setMyindicator(nextggin);
			fnode.setInstvalToIndicator();
			nextggin.setUGA(fnode);
			//			System.out.println("*** setting uga for ind node " + nextggin.getMyatom() +
			//					" as " + fnode.getMyatom());
		}


		/**
		 * Set the depends_on_sample fields
		 */
		if (sumindicators.size() > 0){
			numchains = myggoptions.getNumChains();
			if (numchains == 0) {
				System.out.println("Set num chains to 2");
				numchains = 2;
			}

			windowsize = myggoptions.getWindowSize();

			for (GGAtomSumNode nextggin: sumindicators) {
				nextggin.setDepends_on_sample(true);
				for (GGNode anc: nextggin.ancestors()) {
					anc.setDepends_on_sample(true);
				}
			}
			llnode.setDepends_on_sample(true);
			for (GGNode sumuga: sumindicators)
				sumuga.setDepends_on_sample(true);
		}

		/**
		 * Initialize the sample value arrays
		 */
		for (GGNode ggn: this.allNodes.values()) {
			ggn.init_values_and_grad(mode==MAPMODE);
		}
		llnode.init_values_and_grad(mode==MAPMODE);

		/* Construct vector ParamNodes, and array parameters containing the names of
		 * the parameters in the same order
		 */

		paramNodes = new GGConstantNode[parameters.size()];
		GGConstantNode nextcn;

		//		/* The index of a current parameter in the final parameters array,
		//		 * which is obtained from the current one by deleting parameters for
		//		 * which there does not exist parameter nodes in the graph (parameters
		//		 * on which the likelihood of the data does not depend).
		//		 */
		//		int newindex = 0;
		//		boolean[] deletethese = new boolean[parameters.length];
		//		for (int i=0;i<deletethese.length;i++)
		//			deletethese[i]=false;

		TreeSet<GGNode> ancs;

		for (String par : parameters.keySet()){
			int pidx = parameters.get(par);

			nextcn = (GGConstantNode)allNodes.get(par);
			/* Set the dependsOn array entries */
			if (nextcn == null){
				/* This happens when the data does not depend on parameter[i]*/
				if (myggoptions.ggverbose())
					System.out.println("Warning: no parameter node for " + par);
				/* construct a 'dummy' GGConstantNode that is not connected to the Gradient Graph
				 * and which stores the current parameter value according to the current RBN,
				 * respectively RelStruc A
				 */
				GGConstantNode constparam = new GGConstantNode(this,
						new ProbFormConstant(par),
						null,
						null);

				double pval;

				if (myPrimula.isRBNParameter(par)){
					pval=rbn.getParameterValue(par);
				}
				else{
					/* The data only has one case (RelDataForOneInput).
					 * (otherwise LearnThread would have thrown RBNRuntimeException)
					 */
					A = data.caseAt(0).inputDomain();
					pval=A.getNumAtomValue(par);
				}
				constparam.setCurrentParamVal(pval);
				paramNodes[pidx]=constparam;
			}
			else{
				paramNodes[pidx]=nextcn;
				nextcn.setDependsOn(par);
				ancs = nextcn.ancestors();
				for (GGNode nextggn: ancs){
					nextggn.setDependsOn(par);
					if (nextggn instanceof GGCPMNode &&  ((GGCPMNode)nextggn).isuga()) {
						llnode.addUgaForParam(par, (GGCPMNode)nextggn);
					}
				}
			}
		}



		llnode.initllgrads(parameters.size());

		/* build maxindicators_for_r
		 */

		//Vector<GGAtomMaxNode> newmaxind = new Vector<GGAtomMaxNode>();
		if (mode == MAPMODE || mode == LEARNANDMAPMODE) {
			for (Rel r: mapatoms.keySet()) {
				Vector<GGAtomMaxNode> rnodes = new Vector<GGAtomMaxNode>();

				for (GroundAtom gat: mapatoms.get(r).allAtoms())
					rnodes.add(findInMaxindicators(gat)); // Inefficient, but only done once!
				maxindicators.put(r, rnodes);
			}
			//maxindicators=newmaxind;

			for (Rel r: maxindicators.keySet()) {
				for (GGAtomMaxNode maxn: maxindicators.get(r) ) {
					maxn.setAllugas();
				}
			}
		}

		/* Set the references between Upper Ground Atom nodes and Indicator nodes *
		 *
		 */

		GGAtomSumNode nextisumn;
		for (Iterator<GGAtomSumNode> it = sumindicators.iterator(); it.hasNext();){
			nextisumn = it.next();
			nextisumn.setAllugas();
		}

		if (showInfoInPrimula){
			if (myPrimula.getPrimulaGUI() != null) {
				myPrimula.getPrimulaGUI().showMessageThis("#Ground atoms:" + llnode.childrenSize());
				myPrimula.getPrimulaGUI().showMessageThis("#Sum atoms:" + sumindicators.size());
				myPrimula.getPrimulaGUI().showMessageThis("#Max atoms:" + this.numberOfMaxIndicators());
				myPrimula.getPrimulaGUI().showMessageThis("#Internal nodes:" + allNodes.size());
				//myPrimula.showMessageThis("#Links:" + numLinks());
				myPrimula.getPrimulaGUI().showMessageThis("");
			}
		}
		//showAllNodes(6,null);

//		if (tempGNN != null) {
//			tempGNN.closeInterpreter();
//			this.gnnPy = null;
//		}
		//		System.out.println("Calls to constructGGPFN:" + profiler.constructGGPFNcalls);
		//		System.out.println("Found nodes:" + profiler.foundnodes);
		//		System.out.println("Time 1:" + profiler.time1);
		//		System.out.println("Time 2:" + profiler.time2);
		//		System.out.println("Count 1:" + profiler.count1);
		if (myggoptions.ggverbose()) {
			System.out.println("#Ground atoms:" + llnode.childrenSize());
			System.out.println("#Sum atoms:" + sumindicators.size());
			System.out.println("#Max atoms:" + maxindicators.size());
			System.out.println("#Internal nodes:" + allNodes.size());
		}
	}


	protected void addToSumIndicators(GGAtomSumNode ggin){
		sumindicators.add(ggin);
	}

	protected void addToMaxIndicators(GGAtomMaxNode ggin){
		Rel r = ggin.myatom().rel();
		Vector<GGAtomMaxNode> v = maxindicators.get(r);
		if (v!=null)
			v.add(ggin);
		else {
			v=new Vector<GGAtomMaxNode>();
			v.add(ggin);
			maxindicators.put(r, v);
		}
	}


	public double[] currentLikelihood(){
		return llnode.likelihood();
	}

	public double currentLogLikelihood(){
		return SmallDouble.log(llnode.likelihood());
	}

	public double[] currentGradient(){
		return llnode.gradientAsDouble();
	}

	public double[] getConfusionDouble() {
		double[] result = new double[4];
		int[] intconf = llnode.getConfusion();
		for (int i=0;i<4;i++)
			result[i]=(double)intconf[i];
		return result;
	}

	public double getAccuracy() {
		double[] conf = this.getConfusionDouble();
		return (conf[0] + conf[3])/(conf[0]+conf[1]+conf[2]+conf[3]);
	}

	public double[] currentParameters(){
		double[] result = new double[paramNodes.length];
		for (int i=0;i<paramNodes.length;i++){
			if (paramNodes[i]!= null)
				result[i]=paramNodes[i].getCurrentParamVal();
			else result[i] = 0.5;
		}
		return result;
	}


	/** Computes the  likelihood and  partial derivatives
	 * defined by the values in the value and gradient field of the the GGNodes
	 *
	 */
	public void evaluateLikelihoodAndPartDerivs(boolean likelihoodonly)
			throws RBNNaNException{
		llnode.evaluate(null);
		if (!likelihoodonly)
			llnode.evaluateSmallGrad(null);
	}




	//	public void evaluateBounds(){
	//		llnode.evaluateBounds();
	//	}


	/* Resets to null the value fields in nodes in this GradientGraphO.
	 * If valueonly=false, then also the gradients are reset
	 *
	 */
	public void resetValues(Integer sno, boolean valueonly){
		llnode.resetValue(sno);
		if (!valueonly)
			llnode.resetGradient(sno);
		Enumeration<GGCPMNode> e = allNodes.elements();
		GGNode ggn;
		while (e.hasMoreElements()){
			ggn = (GGNode)e.nextElement();
			ggn.resetValue(sno);
			if (!valueonly)
				ggn.resetGradient(sno);
		}
	}

	public void resetValues(TreeSet<GGNode> ancestors, Integer sno, boolean valueonly){
		llnode.resetValue(sno);
		if (!valueonly)
			llnode.resetGradient(sno);

		for (GGNode ancestor: ancestors) {
			if (ancestor instanceof GGCPMNode) {
				ancestor.resetValue(sno);
				if (!valueonly)
					ancestor.resetGradient(sno);
			}
		}
	}

	//	/** Resets to [-1,-1] the bounds in all nodes */
	//	public void resetBounds(){
	//		llnode.resetBounds();
	//		Enumeration<GGCPMNode> e = allNodes.elements();
	//		GGCPMNode ggn;
	//		while (e.hasMoreElements()){
	//			ggn = (GGCPMNode)e.nextElement();
	//			ggn.resetBounds();
	//		}
	//	}
	//


	/* Tries to randomly generate numchains instantiations of the
	 * indicator variables with nonzero probability given the
	 * current parameter values. Returns true if successful.
	 */
	public boolean initIndicators(Thread mythread){
		boolean abort = false;
		boolean abortforsum = false;
		boolean success = false;
		boolean successforsum = false;

		int failcount=0;
		int failcountforsum=0;
		int maxfailcount = 100; //This should be defined in the settings
		int maxfailcountforsum = myggoptions.getMaxFails()*numchains;

		/* Find initial instantiations with nonzero probability */

		while (!success && !abort){
//			for (GGAtomMaxNode mxnode: maxind_as_ts()) {
//				mxnode.setCurrentInst(0);
//			}

			/* First instantiate the Max nodes */
			for (GGAtomMaxNode mxnode: maxind_as_list()) {
				mxnode.setRandomInst();
			}
			/* Now find initial values for the k Markov chains */
			for (int k=0;k<numchains && !abortforsum;k++){
				successforsum = false;
				while (!successforsum && !abortforsum){
					resetValues(k*windowsize,true);

				for (int i=0;i<sumindicators.size();i++){
					sumindicators.elementAt(i).setRandomSampleVal(k*windowsize);
				}

					llnode.evaluate(k*windowsize);
					double lik = llnode.loglikelihood(k*windowsize);
					if (lik!=Double.NEGATIVE_INFINITY)
						successforsum=true;
					else{
						failcountforsum++;
						if (failcountforsum > maxfailcountforsum)
							abortforsum = true;
					}
				}
			}
			if (abortforsum){
				failcount++;
				if (failcount > maxfailcount)
					abort = true;
			} else if (numchains != 0) success = true;

			if (numchains == 0 && (mode==MAPMODE || mode==LEARNANDMAPMODE)) {
				llnode.evaluate(null);
				if (llnode.likelihood()[0] != 0)
					success = true;
				else{
					failcount++;
					if (failcount > maxfailcount)
						abort = true;
				}
			}
		}

	// Only relevant when numchains>0. At this point, the sampledVals at GGAtomSumNodes look like this:
	// [0,null,null,1,null,null] (here:  numchains=2, windowsize=3) Setting windowindex=1 means that
	// in the next round of Gibbs sampling the first 'null' values of each chain will be overwritten
	if (windowsize>1)
		windowindex =1;
	else
		windowindex =0;

	/* Perform windowsize-1 many steps of Gibbs sampling */
	if (!abort){
		for (int j=1;j<windowsize;j++){
			gibbsSample(mythread);
			if (myggoptions.ggverbose())
				System.out.print(",");
		}
		llnode.evaluate(null);
	}
	return !abort;
}

	/** Performs one round of Gibbs sampling.
	 * Each variable is resampled once.
	 *
	 * windowindex is the index of the oldest among the this.windowsize samples
	 * that are being stored. In the GGAtomSumNode.sampledVals
	 * arrays the values windowindex+0,...,windowindex+numchains-1 are
	 * overwritten
	 */
	public void gibbsSample(Thread mythread){
		Double[] oldsamplelik;
		Double[] newsamplelik;
		double[][] sd_scores; // 'scores' of candidate evaluates as small doubles
		double[] sampleprobs;
		int sampledval;
		//		double likratio;
		//		double coin;
		/* the index of the most recent sample */
		int recentindex;
		if (windowindex != 0) // this implies windowsize >1
			recentindex = windowindex -1;
		else {
			if (windowsize > 1)
				recentindex = windowsize - 1;
			else
				recentindex = windowindex; // both equal 0
		}


		GGAtomSumNode ggin;
		for (int k=0;k<numchains && (mythread == null || mythread.isAlive()) ;k++){
			int sno=k*windowsize+windowindex; // index for re-sampling
			// First reset the values for the current sample number at all nodes:
			resetValues(sno,true);
			// Initialize for the sumindicators the new sample value with the previous one;
			// This enables a uniform access to the current sample value of all sumindicators,
			// regardless whether they already have been re-sampled in this round of Gibbs sampling
			// or still have the value from the previous round
			for (GGAtomSumNode gast:sumindicators) {
				gast.setSampleVal(sno, (int)gast.evaluate(k*windowsize+recentindex)[0]);
			}

			for (GGAtomSumNode gast:sumindicators){
				// When re-sampling the first sumindicator, the previous likelihood is found at the the previous
				// sample index; Afterwards, the last reference likelihood is already found at k*windowsize+windowindex

				int nvals = (int)gast.myatom.rel().numvals();
				sd_scores=new double[nvals][2];

				for (int v=0;v<nvals;v++) {
					gast.setSampleVal(sno, v);
					gast.reEvaluateUpstream(sno);
					sd_scores[v]=llnode.evaluate(sno,0, gast.allugas, false, false, null);
				}
				sampleprobs=SmallDouble.toProbabilityArray(sd_scores);

				sampledval = rbnutilities.sampledValue(sampleprobs);
				gast.setSampleVal(sno, sampledval);
				gast.reEvaluateUpstream(sno);
			}
		}
		windowindex++;
		if (windowindex == windowsize)
			windowindex = 0;
	}

	/**
	 * gibbSample but only on the atoms that are affected by a specific atom
	 */
	public void gibbsSample(Thread mythread, TreeSet<GGNode> ancestors){
		double[][] sd_scores; // 'scores' of candidate evaluates as small doubles
		double[] sampleprobs;
		int sampledval;
		//		double likratio;
		//		double coin;
		/* the index of the most recent sample */
		int recentindex;
		if (windowindex != 0) // this implies windowsize >1
			recentindex = windowindex -1;
		else {
			if (windowsize > 1)
				recentindex = windowsize - 1;
			else
				recentindex = windowindex; // both equal 0
		}

		TreeSet<GGAtomSumNode> sumNodes = new TreeSet<>();
		for (GGNode ancestor: ancestors) {
			if (ancestor instanceof GGCPMNode)
				for (GGAtomSumNode sumnode: ((GGCPMNode)ancestor).getSumIndicators()) sumNodes.add(sumnode);
		}

		GGAtomSumNode ggin;
		for (int k=0;k<numchains && (mythread == null || mythread.isAlive()) ;k++){
			int sno=k*windowsize+windowindex; // index for re-sampling
			// First reset the values for the current sample number at all nodes:
			resetValues(ancestors, sno,true);
			// Initialize for the sumindicators the new sample value with the previous one;
			// This enables a uniform access to the current sample value of all sumindicators,
			// regardless whether they already have been re-sampled in this round of Gibbs sampling
			// or still have the value from the previous round

			for (GGAtomSumNode gast: sumNodes) {
				gast.setSampleVal(sno, (int)gast.evaluate(k*windowsize+recentindex)[0]);
			}

			for (GGAtomSumNode gast: sumNodes){
				// When re-sampling the first sumindicator, the previous likelihood is found at the the previous
				// sample index; Afterwards, the last reference likelihood is already found at k*windowsize+windowindex

				int nvals = (int)gast.myatom.rel().numvals();
				sd_scores=new double[nvals][2];

				for (int v=0;v<nvals;v++) {
					gast.setSampleVal(sno, v);
					gast.reEvaluateUpstream(sno);
					sd_scores[v]=llnode.evaluate(sno,0, gast.allugas, false, false, null);
				}
				sampleprobs=SmallDouble.toProbabilityArray(sd_scores);

				sampledval = rbnutilities.sampledValue(sampleprobs);
				gast.setSampleVal(sno, sampledval);
				gast.reEvaluateUpstream(sno);
			}
		}
		windowindex++;
		if (windowindex == windowsize)
			windowindex = 0;

	}

//	/* Similar to gibbsSample, but operates on the maxindicators, and
//	 * greedily  toggles truth values if it leads to an improvement in
//	 * likelihood
//	 */
//	public void mapStep(){
//		double[] oldlik;
//		double[] newlik;
//
//		GGAtomMaxNode ggin;
//
//			for (int i=0;i<maxindicators.size();i++){
//				evaluateLikelihoodAndPartDerivs(true);
//				oldlik=llnode.likelihoodsum();
//
//				ggin = (GGAtomMaxNode)maxindicators.elementAt(i);
//
//				ggin.toggleCurrentInst();
//
//				evaluateLikelihoodAndPartDerivs(true);
//				newlik=llnode.likelihoodsum();
//
//				if (SmallDouble.compareSD(newlik, oldlik) == -1){
//					ggin.toggleCurrentInst();
//				}
//
//			}
//
//	}

	public double greedySearch(GGThread mythread, TreeSet<GGAtomMaxNode> flipcandidates, int maxIterations, int tabuListSize, int neighborhoodSize) {
		List<GGAtomMaxNode> candidateList = new ArrayList<>(flipcandidates);
		List<GGAtomMaxNode> tabuList = new ArrayList<>();
//		showGraphInfo(10, null);
		Random random = new Random();
		double current_temp = 0.0;
		double cooling_rate = 0.999;
		for (int iter = 0; iter < maxIterations; iter++) {
			System.out.println("Iteration: " + iter);

			GGAtomMaxNode bestNode = null;
			double bestLocalScore = Double.NEGATIVE_INFINITY;

			// select a random neighborhood of nodes for evaluation (instead of evaluating all)
			List<GGAtomMaxNode> neighborhood = new ArrayList<>();

			// randomly select neighborhoodSize nodes that are not in the tabu list
			while (neighborhood.size() < neighborhoodSize) {
				GGAtomMaxNode node = candidateList.get(random.nextInt(candidateList.size()));
				if (!tabuList.contains(node) && !neighborhood.contains(node))
					neighborhood.add(node);
				else
					System.out.println("already selected!");
			}
			System.out.println("selected: " + neighborhood.get(0).getMyatom());

			// evaluate the score for nodes in the selected neighborhood
			bestLocalScore = Double.NEGATIVE_INFINITY;
			for (GGAtomMaxNode node : neighborhood) {
				node.setScore(mythread, 0);
				double score = node.getScore();
				if (score > bestLocalScore) {
					bestLocalScore = score;
					bestNode = node;
				}
			}

			if (bestNode != null && bestLocalScore > 0) {
				System.out.println("Flipping node: " + bestNode.getMyatom() + " from " + bestNode.getCurrentInst() + " to " + bestNode.getHighvalue() + " with score: " + bestLocalScore);
				bestNode.setCurrentInst(bestNode.getHighvalue());
				bestNode.reEvaluateUpstream(null);
				tabuList.add(bestNode);
			}
			else if (current_temp > 0 && bestNode != null && bestLocalScore <= 0) {
				double prob_accept = Math.exp(bestLocalScore / current_temp);
				if (Math.random() < prob_accept) {
					System.out.println("Annealing step, flipping node: " + bestNode.getMyatom() + " with score: " + bestLocalScore);
					bestNode.setCurrentInst(bestNode.getHighvalue());
					bestNode.reEvaluateUpstream(null);
					tabuList.add(bestNode);
				}
			} else {
				System.out.println("No improvement found in this iteration, atom at " + bestNode.getCurrentInst());
			}

			if (tabuList.size() > tabuListSize)
				tabuList.remove(0);  // maintain the size of the tabu list

			for (int j = 0; j < windowsize; j++) {
				gibbsSample(mythread);
			}
			if (myggoptions.ggverbose()) {
				System.out.println("New sampled values:");
				showSumAtomsVals();
			}
			current_temp *= cooling_rate;
			num_iter++;
		}

//		System.out.println("Final likelihood: " + currentLikelihood()[0] + " " + currentLikelihood()[1]);
		return currentLogLikelihood();
	}

	public double greedySearch2(GGThread mythread, TreeSet<GGAtomMaxNode> flipcandidates, int maxIterations, int tabuListSize, int neighborhoodSize) {
		List<GGAtomMaxNode> candidateList = new ArrayList<>(flipcandidates);

		for (int j = 0; j < windowsize; j++)
			gibbsSample(mythread);

		for (int iter = 0; iter < candidateList.size(); iter++) {
			if (myggoptions.ggverbose())
				System.out.println("Iteration: " + iter);
			GGAtomMaxNode node = candidateList.get(iter);
			node.setScore(mythread, 0);
			double score = node.getScore();

			if (node != null && score > 0) {
				if (myggoptions.ggverbose())
					System.out.println("Flipping node: " + node.getMyatom() + " from " + node.getCurrentInst() + " to " + node.getHighvalue() + " with score: " + score);
				node.setCurrentInst(node.getHighvalue());
				node.reEvaluateUpstream(null);
			} else if (myggoptions.ggverbose())
				System.out.println("No improvement found in this iteration.");

			for (int j = 0; j < windowsize; j++)
				gibbsSample(mythread);

			if (myggoptions.ggverbose()) {
				System.out.println("New sampled values:");
				showSumAtomsVals();
			}
		}
		return currentLogLikelihood();
	}

	public double mapSearch(GGThread mythread,TreeSet<GGAtomMaxNode> flipcandidates) throws RBNNaNException {
		PriorityQueue<GGAtomMaxNode> scored_atoms = new PriorityQueue<GGAtomMaxNode>(new GGAtomMaxNode_Comparator()); // NB. with priority queue only the best is guarantee to be on the top

		for (GGAtomMaxNode mxnode: flipcandidates) {
			mxnode.setScore(mythread, 0);
			scored_atoms.add(mxnode);
		}

		if (myggoptions.ggverbose()) {
			System.out.println("Flip scores");
			showMaxAtomFlipScoresBestK(scored_atoms, 10);
	//		showMaxAtomFlipScores(scored_atoms);
		}

		int num_flipped = 0;
		GGAtomMaxNode flipnext;
		while (true) {
			flipnext = scored_atoms.poll();
			if (flipnext != null) {
				if (flipnext.getScore() <= 0) {
					if (num_flipped == 0 && myggoptions.ggverbose())
						System.out.println("Ineffective search, no atoms flipped!");
					else if (num_flipped > 0 && myggoptions.ggverbose())
						System.out.println("Flipped " + num_flipped + " atoms");
					break;
				}
				if (myggoptions.ggverbose())
					System.out.println("Flipping: " + flipnext.getMyatom() + " to " + flipnext.getHighvalue());

				flipnext.setCurrentInst(flipnext.getHighvalue());
				flipnext.reEvaluateUpstream(null);
				num_flipped++;

				/** sample **/
				for (int j=0;j<windowsize;j++){
					gibbsSample(mythread);
				}
				if (windowsize > 0 && myggoptions.ggverbose()) {
					System.out.println("New sampled values:");
					showSumAtomsVals();
				}
				/*
				 * Collect all the GGAtomMaxNodes whose score has to be re-calculated:
				 * flipnext, and all MaxNodes who have a shared uga with flipnext
				 */
				TreeSet<GGAtomMaxNode> update_us = new TreeSet<GGAtomMaxNode>();
				update_us.add(flipnext);
				for (GGCPMNode uga : flipnext.getAllugas()) {
					for (GGAtomMaxNode mx : uga.getMaxIndicators()) {
						update_us.add(mx);
					}
				}
				for (GGAtomMaxNode mx : update_us) {
					scored_atoms.remove(mx);
					mx.setScore(mythread, 0);
					scored_atoms.add(mx);
				}
				if (myggoptions.ggverbose()) {
					System.out.println("New flip scores");
	//				showMaxAtomFlipScores(scored_atoms);
						showMaxAtomFlipScoresBestK(scored_atoms, 10);
				}
			} else {
				System.out.println("flipnext null, return");
				break;
			}
	//		evaluateLikelihoodAndPartDerivs(true);
	//		printProgressBar(num_flipped, scored_atoms.size(), currentLogLikelihood());
		}

		if (myggoptions.ggverbose()) {
			System.out.println("Map search result");
			for (Rel r : maxindicators.keySet()) {
				for (GGAtomMaxNode nextgimn : maxindicators.get(r))
					System.out.println(nextgimn.getMyatom() + ": " + nextgimn.getCurrentInst());
			}
			llnode.evaluate(null);
			System.out.println("Log-Likelihood: " + currentLogLikelihood());
			System.out.println("-----------------");
		}

		return currentLogLikelihood();
	}

	public double[] scoreBatch(TreeSet<GGAtomMaxNode> batch) {
		Vector<GGCPMNode> batchUGAS = new Vector<>();
		for (GGAtomMaxNode node: batch) batchUGAS.addAll(node.getAllugas());

		double oldll = SmallDouble.log(llnode.evaluate(null,0,batchUGAS,true,false,null));
		double newll,fs;
		double highscore = Double.NEGATIVE_INFINITY;
		int highvalue = 0;
		// Remember the current value
		Map<GGAtomMaxNode, Integer> originalValues = new HashMap<>();
		for (GGAtomMaxNode node: batch) originalValues.put(node, node.getCurrentInst());

		for (int v=0; v<(int)batch.first().myatom().rel().numvals(); v++) { // all the batch elements must be the same type
			TreeSet<GGCPMNode> batchParents = new TreeSet<>();
			for (GGAtomMaxNode node: batch) {
				node.setCurrentInst(v);
				batchParents.addAll(node.parents());
			}

			if (sumindicators.size() > 0)
				for (int j=0; j<windowsize; j++) gibbsSample(null, null);

			for (GGAtomMaxNode node: batch) node.reEvaluateUpstream(null);
			newll = SmallDouble.log(llnode.evaluate(null,0,batchUGAS,true,false,null));
			fs=newll-oldll;
			if (fs>highscore) {
				highscore = fs;
				highvalue = v;
			}
		}
		// Reset to original configuration
		for (GGAtomMaxNode node: batch) node.setCurrentInst(originalValues.get(node));
		// Return best score, with corresponded value
		return new double[]{highscore, highvalue};
	}

	public void flipBatch(TreeSet<GGAtomMaxNode> batch, int value) {
		for (GGAtomMaxNode node: batch) node.setCurrentInst(value);
	}

	public void reEvaluateBatch(TreeSet<GGAtomMaxNode> batch) {
		for (GGAtomMaxNode node: batch) node.reEvaluateUpstream(null);
	}

	public double mapSearchBatch(GGThread mythread, TreeSet<GGAtomMaxNode> flipcandidates) {
		int[] batchsizes = new int[]{40,20,20};
		Vector<GGAtomMaxNode> flipvector = new Vector<>(flipcandidates);
		for (int ij = 0; ij < batchsizes.length; ij++) {
			Collections.shuffle(flipvector);

			// all batches are identified by a number
			HashMap<Integer, TreeSet<GGAtomMaxNode>> allBatch = new HashMap<>();
			// divide the flipcandidated in groups that shares the same ugas
			TreeSet<Vector<GGAtomMaxNode>> sameUgas = new TreeSet<>(new Comparator<Vector<GGAtomMaxNode>>() {
				@Override
				public int compare(Vector<GGAtomMaxNode> v1, Vector<GGAtomMaxNode> v2) {
					int cmp = Integer.compare(v1.size(), v2.size());
					if (cmp != 0) return cmp;

					// If sizes are equal, compare element by element.
					for (int i = 0; i < v1.size(); i++) {
						String s1 = v1.get(i).getMyatom();
						String s2 = v2.get(i).getMyatom();
						cmp = s1.compareTo(s2);
						if (cmp != 0) return cmp;
					}
					return 0;
				}
			});
			for (GGAtomMaxNode node : flipvector) {
				Vector<GGCPMNode> allUgas = node.getAllugas();
				Collections.shuffle(allUgas);
				for (GGCPMNode uga : allUgas) {
					if (uga != node.getMyUga()) {
						sameUgas.add(uga.getMaxIndicators());
					}
				}
			}

			int batchSize = batchsizes[ij];
			int batchidx = 0;
			for (Vector<GGAtomMaxNode> same : sameUgas) {
				int totalAtoms = same.size();
				for (int i = 0; i < totalAtoms; i += batchSize) {
					TreeSet<GGAtomMaxNode> batch = new TreeSet<>();
					for (int j = i; j < Math.min(i + batchSize, totalAtoms); j++) {
						batch.add(same.get(j));
					}
					allBatch.put(batchidx, batch);
					batchidx++;
				}
			}

			// the score associated to each batch
			Map<Integer, double[]> batchScores = new HashMap<>();
			double[] flipval = new double[2];
			for (Integer batchId : allBatch.keySet()) {
				TreeSet<GGAtomMaxNode> batch = allBatch.get(batchId);
				flipval = scoreBatch(batch);
				batchScores.put(batchId, flipval);
			}

			// Create a list from the entries of batchScores
			List<Map.Entry<Integer, double[]>> sortedEntries = new ArrayList<>(batchScores.entrySet());
			// Sort the list in descending order based on the score, which is at index 0 of the double array
			Collections.sort(sortedEntries, new Comparator<Map.Entry<Integer, double[]>>() {
				@Override
				public int compare(Map.Entry<Integer, double[]> e1, Map.Entry<Integer, double[]> e2) {
					// Compare e2 to e1 to get descending order
					return Double.compare(e2.getValue()[0], e1.getValue()[0]);
				}
			});

			for (Map.Entry<Integer, double[]> entry : sortedEntries) {
				if (entry.getValue()[0] > 0) {
					System.out.println("Flipping batch id: " + entry.getKey() + ", Score: " + entry.getValue()[0] + ", value: " + entry.getValue()[1]);
					flipBatch(allBatch.get(entry.getKey()), (int) entry.getValue()[1]);
					reEvaluateBatch(allBatch.get(entry.getKey()));
				}
			}
			for (int j = 0; j < windowsize; j++)
				gibbsSample(mythread);
			if (windowsize > 0 && myggoptions.ggverbose()) {
				System.out.println("New sampled values:");
				showSumAtomsVals();
			}
		}

		llnode.evaluate(null,0,null,false,true,null);
		return currentLogLikelihood();
	}

	public double mapSearchSampling(GGThread mythread, ArrayList<GGAtomMaxNode> flipcandidates) throws RBNNaNException {
		// Use an ArrayList to hold scored atoms
		List<GGAtomMaxNode> scored_atoms = new ArrayList<>();
		List<GGAtomMaxNode> rescoreList = new ArrayList<>();
		List<GGAtomMaxNode> topAtoms = new ArrayList<>();

		System.out.println("Scoring flipcandidates...");
		for (GGAtomMaxNode mxnode : flipcandidates) {
			mxnode.setScore(mythread, sampleSizeScoring);
			scored_atoms.add(mxnode);
		}
		Collections.sort(scored_atoms, new GGAtomMaxNode_Comparator());

		if (myggoptions.ggverbose()) {
			System.out.println("Flip scores");
			showMaxAtomFlipScoresBestK(scored_atoms, 10);
		}

		double prevLikelihood = 0; // SmallDouble.log(llnode.evaluate(null, null,true,false,null));
		double perc_inc = 1.0;
		double improvementThreshold = (prevLikelihood / 100.0) * perc_inc; // not used for nows

		int initialSize = flipcandidates.size();
		int num_flipped = 0;
		num_iter = 0;
		GGAtomMaxNode flipnext;
		int rescoreWhen = 10;
		int counterRescore = 0;
		int max_iter = 2500;
		int max_noimprovements = 5;
		boolean terminate = false;
		boolean rescore_early = false; // rescore after fewer than rescoreWhen many flips, because there are not enough atoms with positive score in scored_atoms
		double currentlikelihood;
		double bestlikelihood = Double.NEGATIVE_INFINITY;
		int noimprovements = 0;

		Hashtable<Rel,int[]> bestMapVals = new Hashtable<>();

		Iterator<GGAtomMaxNode> it = scored_atoms.iterator();while (it.hasNext() && num_flipped < max_iter && !terminate) {
			if (num_iter < 2)
				rescoreWhen = flipcandidates.size()/2;
			else if (num_iter >= 2 && num_iter <=4)
				rescoreWhen = (int) Math.ceil(flipcandidates.size()*0.3);
			else
				rescoreWhen = (int) Math.ceil(flipcandidates.size()*0.1);;

			// Always remove the first element (best candidate)
			flipnext = it.next();

			if (flipnext.getScore()>0) {
				topAtoms.add(flipnext);

				if (myggoptions.ggverbose()) {
					System.out.println("Flipping: " + flipnext.getMyatom() + " to " + flipnext.getHighvalue() + " with score " + flipnext.getScore());
					System.out.println("num flipped: " + num_flipped);
				}

				flipnext.setCurrentInst(flipnext.getHighvalue());
				rescoreList.add(flipnext);

				num_flipped++;
				counterRescore++;
			}
			else rescore_early = true;

			if (rescore_early || counterRescore == rescoreWhen) {
				num_iter++;
				// Recompute scores for the best atoms
				if (myggoptions.ggverbose())
					System.out.println("Computing score for flipped atoms ...");

				for (GGAtomMaxNode atom : rescoreList)
					atom.reEvaluateUpstream(null);
				rescoreList = new ArrayList<>();

				if (numchains > 0) {
					for (int j = 0; j < windowsize; j++)
						gibbsSample(mythread);
					if (myggoptions.ggverbose()) {
						System.out.println("New sampled values:");
						showSumAtomsVals();
					}
				}
				evaluateLikelihoodAndPartDerivs(true);
				currentlikelihood = currentLogLikelihood();

				if (currentlikelihood < bestlikelihood){
					noimprovements++;
				} else {
					bestlikelihood = currentlikelihood;
					bestMapVals = this.getMapVals();
					noimprovements = 0;
				}
				if (noimprovements==max_noimprovements) {
					System.out.println("Stopping for no improvement");
					terminate = true;
				}
				System.out.println("Current Likelihood: " + currentlikelihood +  " noimprovements: " + noimprovements);
				if (!terminate) {
					// fin all the nodes that are influenced by the fipped atoms
					HashSet<GGAtomMaxNode> update_us = new HashSet<>();
					for (GGAtomMaxNode node : topAtoms) {
						update_us.add(node);
						for (GGCPMNode uga : node.getAllugas()) {
							for (GGAtomMaxNode mx : uga.getMaxIndicators())
								if (!update_us.contains(mx))
									update_us.add(mx);
						}
					}
					System.out.println("re-scoring " + update_us.size() + " atoms");

					// Update scores
					for (int i = 0; i < scored_atoms.size(); i++) {
						if (update_us.contains(scored_atoms.get(i)))
							scored_atoms.get(i).setScore(mythread, sampleSizeScoring);
					}
					Collections.sort(scored_atoms, new GGAtomMaxNode_Comparator());
					if (scored_atoms.get(0).getScore() <= 0)
						terminate = true;

					topAtoms = new ArrayList<>();
					it = scored_atoms.iterator();
					counterRescore = 0;

					if (myggoptions.ggverbose()) {
						System.out.println("New flip scores");
						showMaxAtomFlipScoresBestK(scored_atoms, 10);
					}
				}
			}
		}

		// set to the best map vals
		if (!bestMapVals.isEmpty()) {
			System.out.println("set to best likelihood: " + bestlikelihood);
			for (Rel r : mapatoms.keySet()) {
				int[] rvals = bestMapVals.get(r);
				int idx = 0;
				for (GGAtomMaxNode mnode : maxindicators.get(r)) {
					mnode.setCurrentInst(rvals[idx]);
					idx++;
				}
			}
		}

		System.out.println("number flipped: " + num_flipped);
		return currentLogLikelihood();
	}

	private double[] getUgasValues(Vector<GGCPMNode> ugas) {
		double[] values = new double[ugas.size()];
		Arrays.fill(values, 0.0);
		for (int i = 0; i < values.length; i++) {
			// if we are sampling: the old values are an average using all #windowsize*numchains values
			if (ugas.get(i).getSumIndicators().size()>0) {
				for (int j = 0; j < windowsize*numchains; j++) {
					int childinst = ugas.get(i).instval(j);
					double[] childval = ugas.get(i).values_for_samples[0];
					if (!ugas.get(i).isBoolean()) {
						values[i] += childval[childinst];
					} else {
						if (childinst == 1)
							values[i] += childval[0];
						else
							values[i] += 1 - childval[0];
					}
				}
				for (int j = 0; j < values.length; j++)
					values[i] /= windowsize*numchains;
			} else {
				int childinst = ugas.get(i).instval(null);
				double[] childval = ugas.get(i).values_for_samples[0];
				if (!ugas.get(i).isBoolean()) {
					values[i] = childval[childinst];
				} else {
					if (childinst == 1)
						values[i] = childval[0];
					else
						values[i] = 1 - childval[0];
				}
			}
			if (myggoptions.ggverbose())
				System.out.print(ugas.get(i).getMyatom() + " ");
		}
		return values;
	}

	public double mapSearchRecursiveWrap(GGThread mythread, Vector<GGAtomMaxNode> flipcandidates, int maxDepth) {
		return mapSearchRecursive(mythread, new TreeSet<GGAtomMaxNode>(new GGAtomMaxNode_Comparator()), flipcandidates, 1, 0, maxDepth);
	}

	public double mapSearchRecursive(GGThread mythread, TreeSet<GGAtomMaxNode> alreadyflipped, Vector<GGAtomMaxNode> flipcandidates, double currentllratio, int depth, int maxDepth) {
		if (depth == maxDepth) {
			if (myggoptions.ggverbose())
				System.out.println("Max depth reached, return");
			return currentllratio;
		}

		String depthS = "";
		for (int i = 0; i < depth; i++) {
			depthS += "\t";
		}
		if (myggoptions.ggverbose())
			System.out.println(depthS + "current depth " + depth);
		PriorityQueue<GGAtomMaxNode> scored_atoms = new PriorityQueue<GGAtomMaxNode>(new GGAtomMaxNode_Comparator()); // NB. with priority queue only the best is guarantee to be on the top

		for (GGAtomMaxNode mxnode: flipcandidates) {
			mxnode.setScore(mythread,sampleSizeScoring);
			scored_atoms.add(mxnode);
		}

//		showMaxAtomFlipScores(scored_atoms);

		GGAtomMaxNode flipnext = null;
		for (Iterator<GGAtomMaxNode> it = scored_atoms.iterator(); (it.hasNext() && flipnext==null);){
			GGAtomMaxNode nextimn = it.next();
			if (!alreadyflipped.contains(nextimn))
				flipnext=nextimn;
		}

		if (flipnext == null){
			if (myggoptions.ggverbose()) {
				System.out.println(depthS + "could not find new candidate for flipping");
				System.out.println(depthS + "1 returning " + currentllratio);
			}
			return currentllratio;
		}

		Vector<GGCPMNode> ugas = flipnext.getAllugas();
		double oldll = SmallDouble.log(llnode.evaluate(null,0, ugas,true,false,null));
		double[] oldvalues = getUgasValues(ugas);
		if (myggoptions.ggverbose()) {
			System.out.println(depthS + "Old UGAS");
			System.out.print(depthS);
			System.out.println();
			System.out.println(depthS + Arrays.toString(oldvalues));
			System.out.println(depthS + "Flipping: " + flipnext.getMyatom() + " to " + flipnext.getHighvalue());
		}

		int oldValue = flipnext.getCurrentInst();
		flipnext.setCurrentInst(flipnext.getHighvalue());
		flipnext.reEvaluateUpstream(null);

		double newll = SmallDouble.log(llnode.evaluate(null,0, ugas,true,false,null));
		double[] newvalues = getUgasValues(ugas);
		currentllratio = currentllratio*oldll/newll;

		int worstUgasCount=0;
		for (int i = 0; i < newvalues.length; i++) {
			if (newvalues[i]<oldvalues[i])
				worstUgasCount++;
		}

		if (myggoptions.ggverbose()) {
			System.out.println(depthS + "New UGAS");
			System.out.print(depthS);
			System.out.println();
			System.out.println(depthS + Arrays.toString(newvalues));
			System.out.println(depthS + "Worst UGAS: " + worstUgasCount + "/" + newvalues.length);
		}

		for (int j=0;j<windowsize;j++){
			gibbsSample(mythread);
		}
		if (numchains > 0 && myggoptions.ggverbose()) {
			if (myggoptions.ggverbose())
				System.out.println(depthS + "New sampled values:");
			showSumAtomsVals();
		}
		/* If we have obtained an improvement in likelihood, then we terminate
		 * here. Otherwise we determine the next indicator to flip.
		 */
		if (currentllratio > 1 && worstUgasCount==0) {
			if (myggoptions.ggverbose())
				System.out.println(depthS + "2 returning " + currentllratio);
			return currentllratio;
		}

		/* Find the uga that had the worst change in likelihood */
		alreadyflipped.add(flipnext);
		int minind = 0;
		double minratio = newvalues[0]/oldvalues[0];
		for (int i=0;i<newvalues.length;i++){
			if (newvalues[i]/oldvalues[i]<minratio){
				minratio = newvalues[i]/oldvalues[i];
				minind = i;
			}
		}
		GGCPMNode minuga = ugas.elementAt(minind);
		double recsearch = mapSearchRecursive(mythread, alreadyflipped, minuga.getMaxIndicators(), currentllratio,depth+1, maxDepth);
		if (recsearch < 1) {
			if (myggoptions.ggverbose())
				System.out.println(depthS + "flipping back " + flipnext.getMyatom() + " to " + oldValue);
			flipnext.setCurrentInst(oldValue);
		}

		if (myggoptions.ggverbose())
			System.out.println(depthS + "3 returning " + recsearch);
		return recsearch;
	}

	class MyCount {
		int count;
	}

	public static void printProgressBar(int current, int total, double curll) {
		int progressBarLength = 40;
		double progressPercentage = (double) current / total;
		int filledBars = (int) (progressBarLength * progressPercentage);

		StringBuilder progressBar = new StringBuilder("[");
		for (int i = 0; i < filledBars; i++) {
			progressBar.append("#");
		}
		for (int i = filledBars; i < progressBarLength; i++) {
			progressBar.append("-");
		}
		progressBar.append("]");

		String percentageText = String.format("%.2f%%", progressPercentage * 100);
		System.out.print("\rProgress: " + progressBar.toString() + " " + percentageText + " (" + current + "/" + total + ") - current log-ll: " + curll);
	}

	public double mapInference(GGThread mythread) throws RBNNaNException{
		boolean terminate = false;
		double score = 0;
		double oldll = 0;
		int itcount = 0;
		Boolean gotinit = initIndicators(mythread);
		if (!gotinit) {
			System.out.println("No successful initialization of max/sum indicators");
			return Double.NaN;
		}

		if (myggoptions.ggverbose()) {
			System.out.println("Initial max values:");
			showMaxAtomsVals();
			if (this.numchains>0) {
				System.out.println("Initial sampled values:");
				showSumAtomsVals();
			}
		}

		MyCount myCount = new MyCount();
		evaluateLikelihoodAndPartDerivs(true);
		double curll = currentLogLikelihood();
		System.out.println("initial log-likelihood= " + curll);

		while (!terminate){
			if (myggoptions.ggverbose())
				System.out.println("starting from the top ..." + itcount);
			itcount++;
			evaluateLikelihoodAndPartDerivs(true);
			oldll=currentLogLikelihood();

			if (myggoptions.ggverbose())
				System.out.println("log-likelihood= " + oldll);

			if (mapSearchAlg == 0) {
				System.out.println("MAP search 0..");
				score = mapSearch(mythread, maxind_as_ts());
			}
			else if (mapSearchAlg == 1)
				score = greedySearch(mythread, maxind_as_ts(), nIterGreedy, 1, 1);
			else if (mapSearchAlg == 2) {
				Vector flip = maxind_as_vec();
				score = mapSearchRecursiveWrap(mythread, flip, this.lookaheadSearch);
				evaluateLikelihoodAndPartDerivs(true);
				if (score <= 1)
					terminate = true;
			} else if (mapSearchAlg == 3) {
				score = mapSearchSampling(mythread, maxind_as_list());
				terminate = true;
			}

			if (myggoptions.ggverbose() && mapSearchAlg != 2)
				System.out.println("log-likelihood improvement " + oldll/score);

			if (mapSearchAlg != 2 && oldll/score <= 1+myggoptions.getLLikThresh()) {
				terminate = true;
			}
			if (!terminate) {
				if (mode == LEARNMODE) {
					if (myggoptions.ggverbose())
						System.out.print("Learning parameters ...");
					learnParameters(mythread, GradientGraph.FullLearn, false);
					if (myggoptions.ggverbose())
						System.out.println("... done");
				}
			}
		}

		System.out.println();
		evaluateLikelihoodAndPartDerivs(true);
		curll = currentLogLikelihood();
		System.out.println("final log-likelihood= " + curll);

//		for (GGCPMNode nextchild: this.llnode.children) {
//			if (nextchild.getMyatom().equals("constr(0)")) {
//				for (int i = 0; i < numchains; i++) {
//					for (int j = 0; j < windowsize; j++) {
//						System.out.print(nextchild.values_for_samples[i+j][0] + "\t");
//					}
//					System.out.println();
//				}
//			}
//		}

		return llnode.loglikelihood();
	}

//	/** Sets the truthval fields in the ProbFormNodes corresponding
//	 * to unobserved atoms to the truthvalues in the sno's sample
//	 *
//	 * If sno=null do nothing!
//	 */
//	public void setTruthVals(Integer sno){
//		if (sno != null)
//			for (int i=0;i<sumindicators.size();i++){
//				sumindicators.elementAt(i).setCurrentInst(sno);
//			}
//	}


//public void showLikelihoodNode(RelStruc A){
//	System.out.println("Likelihood" + llnode.value());
//}


	public void showMaxAtomsVals() {
		for (Rel r: maxindicators.keySet()) {
			for (GGAtomMaxNode nextgimn: maxindicators.get(r))
				System.out.println(nextgimn.getMyatom() + ": " + nextgimn.getCurrentInst());
		}
	}

	public void showMaxAtomFlipScores(TreeSet<GGAtomMaxNode> scored_atoms) {
		Iterator<GGAtomMaxNode> it = scored_atoms.iterator();
		while (it.hasNext()) {
			GGAtomMaxNode el = it.next();
			System.out.println(el.getMyatom() + ": " + el.getScore());
		}
	}

	public void showMaxAtomFlipScores(PriorityQueue<GGAtomMaxNode> scored_atoms) {
		Iterator<GGAtomMaxNode> it = scored_atoms.iterator();
		while (it.hasNext()) {
			GGAtomMaxNode el = it.next();
			System.out.println(el.getMyatom() + ": " + el.getScore());
		}
	}

	public void showMaxAtomFlipScoresBestK(PriorityQueue<GGAtomMaxNode> scored_atoms, int k) {
		Iterator<GGAtomMaxNode> it = scored_atoms.iterator();
		System.out.println("Best " + k + " atoms:");
		while (it.hasNext()) {
			GGAtomMaxNode el = it.next();
			System.out.println(el.getMyatom() + ": " + el.getScore());
			k--;
			if (k == 0)
				break;
		}
	}

	public void showMaxAtomFlipScoresBestK(List<GGAtomMaxNode> scored_atoms, int k) {
		Iterator<GGAtomMaxNode> it = scored_atoms.iterator();
		System.out.println("Best " + k + " atoms:");
		while (it.hasNext()) {
			GGAtomMaxNode el = it.next();
			System.out.println(el.getMyatom() + ": from " + el.getCurrentInst() + " to " + el.getHighvalue() + " with score " + el.getScore());
			k--;
			if (k == 0)
				break;
		}
	}

	public void showMaxAtomFlipScoresBestK(TreeSet<GGAtomMaxNode> scored_atoms, int k) {
		Iterator<GGAtomMaxNode> it = scored_atoms.iterator();
		System.out.println("Best " + k + " atoms:");
		while (it.hasNext()) {
			GGAtomMaxNode el = it.next();
			System.out.println(el.getMyatom() + ": from " + el.getCurrentInst() + " to " + el.getHighvalue() + " with score " + el.getScore());
			k--;
			if (k == 0)
				break;
		}
	}

	public void showSumAtomsVals(){
		showSumAtomsVals(0);
	}

	public void showSumAtomsVals(int indent) {
		for (GGAtomSumNode sn: sumindicators) {
	//		for (int i = 0; i < indent; i++) {
	//			System.out.print("\t");
	//		}
			int[] sampledvals = sn.getSampledVals();
	//		System.out.println(sn.getMyatom() + ": " + rbnutilities.arrayToString(sampledvals));
			Map<Integer, Integer> map = new HashMap<>();
			int num_vals = (int)sn.myatom.rel.numvals();
			for (int i = 0; i < num_vals; i++) map.put(i, 0);

			int nonzerocount = 0;
			for (int i = 0; i < sampledvals.length; i++) {
				if (sampledvals[i] != 0)
					nonzerocount++;
				map.put(sampledvals[i], map.get(sampledvals[i])+1);
			}
			for (int i = 0; i < indent+1; i++) {
				System.out.print("\t");
			}
//			System.out.println(sn.getMyatom() + ": #nonzeros: " + nonzerocount + "/" + sampledvals.length + ": " + (double)nonzerocount/sampledvals.length);
			double[] prob = new double[num_vals];
			for (int i = 0; i < num_vals; i++)
				prob[i] = (double)map.get(i)/sampledvals.length;
			System.out.println(sn.getMyatom() + ":\t" + Arrays.toString(prob));
		}
	}

	public void showAllNodes(int verbose,RelStruc A){
		if (verbose >0){
			System.out.println("**** Node " + llnode.name());
				System.out.println("**** Log likelihood: " + llnode.loglikelihood());
			System.out.println();
		}
		if (verbose >5){
			GGNode nextggn;
			for (Enumeration<String> e = allNodes.keys();e.hasMoreElements();){
				String nextkey = e.nextElement();
				nextggn = (GGNode)allNodes.get(nextkey);
				System.out.println("**** Node " + "   " + nextggn.identifier()+ "   "  +nextggn.getClass().getName() + '\n' + nextkey + "   " );
				if (nextggn instanceof GGCPMNode) {
					System.out.print("Parents: ");
					((GGCPMNode)nextggn).printParents();
				}
				System.out.println();
				System.out.print("Children: ");
				nextggn.printChildren();

				if 	(nextggn instanceof GGAtomNode)
					((GGAtomNode)nextggn).printAllUgas();
				System.out.println();
				//				}
			}
		}
	}

	public void showAllNodes2(RelStruc A) {
		GGNode nextggn;
		for (Enumeration<String> e = allNodes.keys();e.hasMoreElements();){
			String nextkey = e.nextElement();
			nextggn = (GGNode)allNodes.get(nextkey);
			System.out.println("**** Node" + " " + nextggn.identifier()+ "   "  +nextggn.getClass().getName() + '\n' + nextkey);

			if 	(nextggn instanceof GGAtomNode)
				((GGAtomNode)nextggn).printAllUgas();
			System.out.println();
			//				}
		}
	}




/** Searches for likelihood-optimizing parameters, starting at
 * currenttheta
 *
 * Returns array of length n+4, where n is the number of parameter nodes
 * in the Gradient Graph.
 *
 * The result array contains:
 *
 * [0..n-1]: the current parameter values at the end of thetasearch
 *
 * [n]: the log-likelihood value of the current parameters
 *
 * [n+1]: the log-likelihood value divided by the number of
 * children of the likelihood node. This gives a 'per observed atom'
 * likelihood value that is more useful than the overall likelihood.
 *
 * [n+2]: the log-likelihood of the whole data = log-likelihood+this.likelihoodconst
 *
 */
protected double[] thetasearch(double[] currenttheta,
		GGThread mythread,
		int fullorincremental,
		boolean verbose)
				throws RBNNaNException{
	double[] gradient;
	double[] direction = null;
	double[] oldgradient = null;
	double[] oldthetas = currenttheta;
	double[] olddirection = null;
	double newlikelihood =0;


	boolean terminate = false;


	double[] result = new double[currenttheta.length+3];


	/* Initialize ascent strategy specific variables:*/
	switch(myggoptions.ggascentstrategy()){
	case LearnModule.AscentAdagrad:
		gradmemory = new double[parameters.size()];
		break;
	case LearnModule.AscentLBFGS:
		thetadiffhistory = new double[myggoptions.lbfgsmemory()][parameters.size()];
		graddiffhistory = new double[myggoptions.lbfgsmemory()][parameters.size()];
		rhos = new double[myggoptions.lbfgsmemory()];
		break;
	case LearnModule.AscentFletcherReeves:
		olddirection = new double[parameters.size()];
	}



	int llwindow = myggoptions.getWindowSize();
	int itindx = 0;
	double gain; /* Likelihood gain */
	double initialll = Double.NEGATIVE_INFINITY; /* The likelihood at the beginning */
	double[] llgains = new double[llwindow];
	for (int i=0;i<llgains.length;i++)
		llgains[i]=Double.POSITIVE_INFINITY;






	int itcounter =0;
	int maxiterations = myggoptions.getMaxIterations();
	int lbfgs_iterationcount =0;

	evaluateLikelihoodAndPartDerivs(true);
	double llikhood =  llnode.loglikelihood();
	double newllikhood;


	//System.out.println("Initial likelihood: " + llikhood);

	if(Double.isInfinite(llikhood)){
		result[currenttheta.length+3]=Double.NEGATIVE_INFINITY;
		System.out.println("Zero likelihood at initial parameters");
		return result;
	}



	long timestart = System.currentTimeMillis();

	if (verbose)
		System.out.println("Iterationcounter" + '\t' +  "Time" + '\t' + "gradient*direction" + '\t' +    "stepsize"  + '\t' +    "log-likelihood"  );

	while (!terminate && !mythread.isstopped()){

		/* compute the direction of the gradient
		 * The variable gradient here will not contain
		 * the actual gradient, but a normalized version
		 *
		 * */

		evaluateLikelihoodAndPartDerivs(false);



		gradient = llnode.gradientAsDouble();


		/* If the gradient at llnode is not representable as a standard double vector
		 * (usually only when useloglik == false) then gradient now is a scaled version of the actual
		 * gradient. When useloglik == true, then this will usually not cause any loss
		 * in precision.
		 */


		/* Now get the direction for the linesearch: */

		switch (myggoptions.ggascentstrategy()){
		case LearnModule.AscentAdagrad:

			direction = getDirectionAscentAdagrad(gradient);
			break;
		case LearnModule.AscentDirectGradient:
			direction=gradient;
			//direction = rbnutilities.normalizeDoubleArray(gradient);
			break;
		case LearnModule.AscentLBFGS:
			if (lbfgs_iterationcount > 0){
				int idx = (lbfgs_iterationcount -1) % myggoptions.lbfgsmemory();
				graddiffhistory[idx] = rbnutilities.arraySubtract(gradient, oldgradient);
				rhos[idx] = 1/rbnutilities.arrayDotProduct(graddiffhistory[idx], thetadiffhistory[idx]);
				//System.out.println("# rho: " + rhos[idx]);
			}

			direction = getDirectionLBFGS(gradient,lbfgs_iterationcount);

			break;
		case LearnModule.AscentFletcherReeves:
			direction = getDirectionFletcherReeves(gradient,oldgradient,olddirection);
		}

		double gtimesd = rbnutilities.arrayDotProduct(rbnutilities.normalizeDoubleArray(gradient),
				rbnutilities.normalizeDoubleArray(direction));



		/****************************************
		 * call linesearch
		 ****************************************/
		if (myggoptions.ggverbose() )
			System.out.print("linesearch: ");


		currenttheta = linesearch(currenttheta,
				constrainedDirection(currenttheta,direction),
				mythread);


		// If there was no progress on this linesearch, then this may
		// have been caused by the lbfgs direction being too orthogonal to
		// the gradient.
		// We "reset the lbfgs history" by setting the effective iteration
		// counter to 0. Then in the next iteration will re-start the
		// lbfgs process, and follow the gradient as the ascent direction
		if (myggoptions.ggascentstrategy()==LearnModule.AscentLBFGS) {
			if (Arrays.equals(currenttheta,oldthetas))
				lbfgs_iterationcount = 0;
			else{
				thetadiffhistory[lbfgs_iterationcount % myggoptions.lbfgsmemory() ] = rbnutilities.arraySubtract(currenttheta, oldthetas);
				lbfgs_iterationcount++;
			}
		}


		setParameters(currenttheta);
		if (fullorincremental == GradientGraph.OneLineSearch)
			terminate = true;

		evaluateLikelihoodAndPartDerivs(true);

		if (verbose) {
			double stepsize = rbnutilities.euclidDist(oldthetas, currenttheta);
			long tick = System.currentTimeMillis()-timestart;
			System.out.print(""+ itcounter + '\t' + " " + tick +  '\t'+ gtimesd + '\t'
					+ stepsize +'\t'
					+ llnode.loglikelihood());
		}



		/****************************************
		 * Gibbs sample
		 ****************************************/
		if (!terminate){
			newlikelihood = llnode.loglikelihood();


			if (myggoptions.ggverbose() && numchains > 0)
				System.out.print("<sampling ... ");

			gibbsSample(mythread);

			if (myggoptions.ggverbose()  && numchains > 0)
				System.out.println("done>");

		}

		itcounter++;
		itindx = (itindx+1) % llwindow;
		newllikhood = llnode.loglikelihood();
		gain = (newllikhood-llikhood)/llnode.numChildren();
		llgains[itindx] = gain;
		terminate = ( rbnutilities.arrayAverage(llgains)<myggoptions.getLLikThresh());
		terminate = terminate || (itcounter>=maxiterations);
		llikhood = newllikhood;


		oldthetas = currenttheta;
		oldgradient = gradient;
		olddirection = direction;

		if (verbose)
			System.out.println();
	} /* end main while loop */


	for (int i=0;i<currenttheta.length;i++){
		result[i]=currenttheta[i];
		if (Double.isNaN(currenttheta[i]))
			System.out.println("learned NaN");
	}
	result[currenttheta.length]=newlikelihood;
	result[currenttheta.length+1]=newlikelihood/llnode.numChildren();
	result[currenttheta.length+2] = llnode.loglikelihood();

	long timespent = System.currentTimeMillis()-timestart;

	if (verbose){
		System.out.println();
		System.out.println("# Iterations: " + itcounter);
		System.out.println("# Time: " + timespent);
		System.out.println();
	}
	return result;
}



public double[] learnParameters(GGThread mythread, int fullorincremental, boolean verbose)
		throws RBNNaNException
{
	if (myggoptions.ggverbose())
		System.out.println("** start learnParameters ** ");
	/* Returns:
	 * resultArray[0:paramNodes.length-1] : the parameter values learned in the
	 *                                      order given by paramNodes
	 * resultArray[paramNodes.length]: the log-likelihood value for the parameters
	 *
	 * resultArray[paramNodes.length+1]: log-likelihood/number of childrent of likelihood node
	 *
	 * resultArray[paramNodes.length+2]: the log-likelihood of the full data (cf. thetasearch)
	 */
	double[] resultArray = new double[paramNodes.length+3];
	double[] lastthetas = null;

	/* First find an
	 * initial sample, such that at least a proportion of
	 * sampleSuccessRate samples were not aborted
	 */

	boolean success = false;
	if (myggoptions.ggverbose())
		System.out.print("< Initialize Markov Chains ... ");

	if (mode==LEARNMODE && numchains>0){
		while (!success && !mythread.isstopped()){
			if (initIndicators(mythread))
				success = true;
			else {
				if (myPrimula.getPrimulaGUI() != null)
					myPrimula.getPrimulaGUI().showMessageThis("Failed to sample missing values");
			}
		}
	}


	if (myggoptions.ggverbose())
		System.out.println("done >");

	double[] currenttheta = currentParameters();
	double[] firstthetas = currenttheta.clone();


	/************************************************
	 *
	 *  call thetasearch
	 *
	 ************************************************/
	if (paramNodes.length>0){
		lastthetas =thetasearch(currenttheta,mythread,fullorincremental,verbose);

		for (int k=0;k<lastthetas.length;k++)
			resultArray[k]=lastthetas[k];
	}
	else{ /* Only compute likelihood for the given parameters;
	 * the first two components of the resultArray are not used */
		llnode.evaluate(null);
		resultArray[0]=llnode.loglikelihood();
		resultArray[1]=resultArray[0]/llnode.numChildren();
		resultArray[2]=resultArray[1]+this.loglikconstant;
	}

	return resultArray;
}




/** Performs a linesearch for parameter settings optimizing
 * log-likelihood starting from oldthetas in the direction
 * gradient
 *
 * returns new parameter settings
 *

 */
protected double[] linesearch(double[] oldthetas,
		double[] gradient,
		GGThread mythread)
				throws RBNNaNException{

	if (iszero(gradient))
		return oldthetas;

	//		/* check whether the gradient is of the form (0,...,0,1,0,...0), i.e.,
	//		 * optimzation is w.r.t. to the single parameter at the '1' index
	//		 */
	//		int partderiv = isPartDeriv(gradient);
	//		GGConstantNode currentparamnode = null;
	//TODO replace the earlier 'partderiv' case with a more general 'lazy evaluation' strategy
	// where values are updated only for selected ugas affected by changes in selected parameters


	//		if (partderiv != -1){
	//			currentparamnode = paramNodes.elementAt(partderiv);
	//			currentparamnode.setAncestors();
	//		}

	double[] leftbound=oldthetas.clone();
	double[] rightbound = new double[oldthetas.length];
	double[] middle1 = new double[oldthetas.length];
	double[] middle2 = new double[oldthetas.length];


	double[] leftvalue;
	double[] rightvalue = new double[2];
	double[] lastrightvalue;
	double[] middlevalue1 = new double[2];
	double[] middlevalue2 = new double[2];

	/* First find the point where the line oldthetas+lambda*gradient intersects
	 * the boundary of the parameter space, if the parameter space is bounded
	 * in the direction of gradient
	 */
	double lambda = getAlphaBound(gradient, oldthetas);

	/* In case no bound is encountered in the direction of gradient,
	 * determine a lambda so that the likelihood at leftbound + lambda*gradient
	 * is less than the likelihood at leftbound
	 */

	/* First get the value at oldthetas (leftbound) */
	setParameters(oldthetas);
	evaluateLikelihoodAndPartDerivs(true);
	leftvalue = llnode.likelihood();

	if (lambda == Double.POSITIVE_INFINITY){
		if (myggoptions.ggverbose())
			System.out.print("lambdasearch: ");
		lambda = 1;
		boolean terminate =false;
		lastrightvalue = new double[2];
		double[] lastrightbound = leftbound;

		//System.out.print("left: " + StringOps.arrayToString(leftbound,"[","]") + "   " + StringOps.arrayToString(leftvalue,"(",")"));
		while (!terminate && lambda != Double.POSITIVE_INFINITY){
			//System.out.print("l: " + lambda + " ");
			rightbound = rbnutilities.arrayAdd(leftbound, rbnutilities.arrayScalMult(gradient,lambda));
			//System.out.print(">");

			//				if (currentparamnode == null){
			setParameters(rightbound);
			evaluateLikelihoodAndPartDerivs(true);
			//				else{
			//					currentparamnode.setCurrentParamVal(rightbound[partderiv]);
			//					currentparamnode.reEvaluateUpstream(partderiv);
			//				}
			rightvalue = llnode.likelihood();
			//System.out.println("right: " + StringOps.arrayToString(rightbound,"[","]") + "   " + StringOps.arrayToString(rightvalue,"(",")"));
			if (Double.isInfinite(rightvalue[0]) ||
					Double.isNaN(rightvalue[0]) ||
					likelihoodGreater(leftvalue,rightvalue) ||
					! likelihoodGreater(rightvalue,lastrightvalue))
				terminate = true;
			else{
				lambda = 2*lambda;
				lastrightvalue = rightvalue;
				lastrightbound = rightbound.clone();
			}
		}
		if (Double.isInfinite(rightvalue[0]) || Double.isNaN(rightvalue[0]) ) { // went too far in the last step; have to find a point
																				// between lastrightbound and rightbound
			terminate = false;
			double[] middle;
			double[] middlevalue;
			while (!terminate) {
				middle = midpoint(lastrightbound,rightbound,0.5);
				setParameters(middle);
				evaluateLikelihoodAndPartDerivs(true);
				middlevalue = llnode.likelihood();
				if (Double.isInfinite(rightvalue[0]) || Double.isNaN(rightvalue[0]) )
					rightbound=middle;
				if (likelihoodGreater(leftvalue,middlevalue)) {
					rightbound = middle;
					terminate = true;
				}
				if (likelihoodGreater(middlevalue,leftvalue)) {
					lastrightbound = middle;
				}
			}
		}
	}
	else{ // not lambda == Double.POSITIVE_INFINITY, i.e., defines boundary of feasible region
		rightbound = rbnutilities.arrayAdd(leftbound, rbnutilities.arrayScalMult(gradient,lambda));
	}


	if (lambda == Double.POSITIVE_INFINITY){
		System.out.println("# Warning:  linesearch failed. Leftval: " + rbnutilities.arrayToString(leftvalue) +
				" Rightval: " + rbnutilities.arrayToString(rightvalue));
		return leftbound;
	}




	/** The following can easily result in rightvalue = NaN or -infinity:
	 * setParameters(rightbound);
	 * evaluateLikelihoodAndPartDerivs(true);
	 */

	/** Pro forma initialization of rightvalue */
	rightvalue = llnode.likelihood();
	boolean terminate = false;
	boolean moveleftprobe = true;
	boolean moverightprobe = true;
	//boolean leftboundhasmoved = false;

	//System.out.print("|");

	while (!terminate && !mythread.isstopped()) {


		if (moveleftprobe){
			middle1 = midpoint(leftbound,rightbound,0.6);

			//				if (currentparamnode == null){
			setParameters(middle1);
			evaluateLikelihoodAndPartDerivs(true);
			//				else{
			//					currentparamnode.setCurrentParamVal(middle1[partderiv]);
			//					currentparamnode.reEvaluateUpstream(partderiv);
			//				}
			middlevalue1=llnode.likelihood();
			//				System.out.print(" m1: " + middlevalue1[0]);
			//				if (Double.isNaN(middlevalue1[0]) || Double.isInfinite(middlevalue1[0])){
			//					System.out.println();
			//					System.out.println("# Warning: middlevalue1 undefined");
			//				}

		}

		if (moverightprobe){
			middle2 = midpoint(leftbound,rightbound,0.4);

			//				if (currentparamnode == null){
			setParameters(middle2);
			evaluateLikelihoodAndPartDerivs(true);
			//				else{
			//					currentparamnode.setCurrentParamVal(middle2[partderiv]);
			//					currentparamnode.reEvaluateUpstream(partderiv);
			//				}
			middlevalue2=llnode.likelihood();
			//				if (Double.isNaN(middlevalue2[0]) || Double.isInfinite(middlevalue2[0])){
			//					System.out.println();
			//					System.out.println("# Warning: middlevalue2 undefined");
			//				}
		}


		if (likelihoodGreaterEqual(middlevalue1,middlevalue2) || likelihoodGreaterEqual(leftvalue,middlevalue2)){
			rightbound = middle2.clone();
			rightvalue = middlevalue2.clone();
			middle2=middle1;
			middlevalue2=middlevalue1;
			moveleftprobe=true;
			moverightprobe=false;
		}
		else{
			leftbound = middle1.clone();
			//leftboundhasmoved = true;
			leftvalue = middlevalue1.clone();
			middle1=middle2;
			middlevalue1=middlevalue2;
			moverightprobe = true;
			moveleftprobe = false;
		}

		//			if (moverightprobe)
		//				System.out.print(">");
		//			else
		//				System.out.print("<");

		double leftasdouble = SmallDouble.toStandardDouble(leftvalue);

		/* also terminate if the updated rightvalue is better than the original leftvalue; */
		//			if ( fullorincremental == GradientGraph.StochUpdate &&  likelihoodGreaterEqual(rightvalue,leftvalue) )
		//				terminate=true;

		//			lratio = SmallDouble.toStandardDouble(SmallDouble.divide(leftvalue,rightvalue));
		//			System.out.println("Dist: " + mymath.MyMathOps.euclDist(rightbound,leftbound) +
		//					       "  Lik: " + lratio);

		double bounddist = mymath.MyMathOps.euclDist(rightbound,leftbound)/(rbnutilities.euclidNorm(rightbound)*rbnutilities.euclidNorm(leftbound));


		if ( bounddist < myggoptions.getLineDistThresh())
			terminate = true;


	}
	if (myggoptions.ggverbose())
		System.out.println();

	//		System.out.println();

	//		if (currentparamnode != null)
	//			currentparamnode.deleteAncestors();
	//
	//

	if (likelihoodGreaterEqual(leftvalue,rightvalue)){
		//			if (!leftboundhasmoved){
		//				System.out.println();
		//				System.out.println("# Warning: linesearch returns initial point");
		//			}
		return leftbound;
	}
	else
		return rightbound;
}


protected double[] linesearch_wolfe(double[] oldthetas,
		double[] direction,
		GGThread mythread)
				throws RBNNaNException
{
	double abound = getAlphaBound(direction,oldthetas);
	double alpha = Math.min(0.1, abound);
	double beta1 = 0.01;
	double beta2 = 0.5;
	Boolean wolfecond = false;
	double[] alphathetas = new double[0];
	double oldval;
	double newval;
	double[] oldgrad;
	double[] newgrad;
	double olddot;
	double newdot;

	setParameters(oldthetas);
	evaluateLikelihoodAndPartDerivs(false);

	oldval = llnode.loglikelihood();
	oldgrad = llnode.gradientAsDouble();

	while (!mythread.isstopped() && !wolfecond){

		alphathetas = rbnutilities.arrayAdd(oldthetas, rbnutilities.arrayScalMult(direction, alpha));
		setParameters(alphathetas);
		evaluateLikelihoodAndPartDerivs(false);
		newval = llnode.loglikelihood();
		newgrad = llnode.gradientAsDouble();


		wolfecond = wolfe_cond(oldthetas,alphathetas,oldgrad,newgrad,newval,oldval,direction,beta1,beta2,alpha);

		alpha = 0.5*alpha;
		System.out.println("alpha: " + alpha + " Lik.: " + newval);
	}

	return alphathetas;
}

/* Determine the maximal value alpha, such that
 * oldthetas + alpha*direction is within the
 * feasible parameter region
 */
protected double getAlphaBound(double[] direction, double[] oldthetas){
	double alpha = Double.POSITIVE_INFINITY;
	for (int i=0;i<minmaxbounds.length;i++){
		if (direction[i]<0 && minmaxbounds[i][0]!=Double.NEGATIVE_INFINITY)
			alpha = Math.min(alpha, (minmaxbounds[i][0]-oldthetas[i])/direction[i]);
		if (direction[i]>0 && minmaxbounds[i][1]!=Double.POSITIVE_INFINITY)
			alpha = Math.min(alpha,(minmaxbounds[i][1]-oldthetas[i])/direction[i]);
	}
	return alpha;
}


protected Boolean wolfe_cond(double[] oldth, double[] newth,
		double[] oldgr, double[] newgr,
		double newval, double oldval,
		double[] dir,
		double beta1, double beta2, double alpha){
	boolean result = true;
	double olddot = rbnutilities.arrayDotProduct(oldgr, dir);
	double newdot = rbnutilities.arrayDotProduct(newgr, dir);

	if (newval < oldval + beta1*alpha*olddot)
		result = false;
	if (newdot > beta2*olddot)
		result = false;
	return result;
}

/** Sets the parameter values in the nodes to thetas. thetas[i] will be the
 * value of the parameter in the i'th position in this.paramNodes
 */
protected void setParameters(double[] thetas){
	if (thetas.length != paramNodes.length)
		System.out.println("Size mismatch in GradientGraphO.setParameters!");
	for (int i=0;i<thetas.length;i++)
		if (paramNodes[i]!=null) {
			paramNodes[i].setCurrentParamVal(thetas[i]);
			paramNodes[i].resetUpstream(null);
		}
}

public double[] getParameters(){
	double[] result = new double[paramNodes.length];
	for (int i=0;i<paramNodes.length;i++)
		result[i]=paramNodes[i].getCurrentParamVal();
	return result;
}

public double[] getGradient()
		throws RBNNaNException{
	return llnode.gradientAsDouble();
}


//	public double[] getGradientBatch(int batchsize)
//			throws RBNNaNException
//	{
//		llnode.evaluateGradients(batchsize);
//		return llnode.gradientAsDouble();
//	}

protected void setParametersRandom(){
	for (int i=0;i<paramNodes.length;i++)
		if (paramNodes[i]!=null)
			paramNodes[i].setCurrentParamVal(randomGenerators.getRandom(minmaxbounds[i][0],minmaxbounds[i][1],1.0));
}

public void setParametersFromAandRBN(){
	GGConstantNode nextnode ;
	//		for (Iterator<GGConstantNode> it = paramNodes.iterator();it.hasNext();){
	for (int i =0;i<paramNodes.length;i++) {
		String paramname;
		nextnode=paramNodes[i];
		if (nextnode!=null){
			paramname = nextnode.paramname();
			if (myPrimula.isRBNParameter(paramname))
				nextnode.setCurrentParamVal(myPrimula.getRBN().getParameterValue(paramname));
			else /* numrel atom */
				nextnode.setCurrentParamVal(myPrimula.getRels().getNumAtomValue(paramname));
		}
	}
}

private void setParametersUniform(){
	for (int i=0;i<paramNodes.length;i++)
		if (paramNodes[i]!=null)
			paramNodes[i].setCurrentParamVal(0.5);
}




public String showGraphInfo(int ggverbose, RelStruc A){
	String result = "";
	result = result + "% Number of indicator nodes: " + sumindicators.size() +'\n';
	result = result + "% Number of upper ground atom nodes: " + llnode.childrenSize() +'\n';

	// 	result = result + "Parameter nodes: ";
	// 	for (int i=0;i<paramNodes.length;i++)
	// 	    System.out.print(paramNodes[i].name()+ "  ");
	// 	result = result + ();
	// 	result = result + ("Parameter values: " + rbnutilities.arrayToString(currentParameters()));

	// 	result = result + ();
	result = result + "% Total number of nodes: " + allNodes.size() +'\n';
	result = result + "% Total number of links: " + numberOfEdges() +'\n';

	showAllNodes(ggverbose,A);
	return result;
}



//	private void unsetSumIndicators(){
//		for (int i=0;i<sumindicators.size();i++)
//			sumindicators.elementAt(i).unset();
//	}



/** Returns the number of nodes in the graph */
public int numberOfNodes(){
	return allNodes.size()+1;
}

/** Returns the number of indicator nodes in the graph */
public int numberOfIndicators(){
	return sumindicators.size();
}

public int numberOfMaxIndicators() {
	int result = 0;
	for (Rel r: maxindicators.keySet()) {
		result+=maxindicators.get(r).size();
	}
	return result;
}

/** Returns the number of links in the graph */
public int numberOfEdges(){
	int result = llnode.childrenSize();
	Enumeration e = allNodes.elements();

	while (e.hasMoreElements())
		result = result + ((GGNode)e.nextElement()).childrenSize();

	return result;
}



///** Computes the objective function value given the current parameter setting
// * by Gibbs sampling. It is assumed that initIndicators() has been
// * successfully executed.
// *
// * Also returns the confusion matrix values TP,FP,FN,TN
// * (only useful when data is complete, and no Gibbs sampling
// * involved)
// *
// * Returns double array with result[0]=per node likelihood,
// * result[1]=objective function value, result[2]=TP,
// * result[3]=FP, result[4]=FN, result[5]=TN
// *
// * @return
// */
//public double[] computeObjectiveandConfusion(GGThread mythread)
//		throws RBNNaNException{
//	double[] result = new double[6];
//	double nodelik;
//
//	for (int i = 0; i<windowsize; i++)
//		gibbsSample(mythread);
//
//	evaluateLikelihoodAndPartDerivs(true);
//
//	if (llnode.numChildren()>0)
//		nodelik=currentLogLikelihood()/llnode.numChildren();
//	else
//		nodelik = 0.0;
//
//	result[0] = nodelik;
//	result [1] =  llnode.objective()+this.objectiveconstant;
//
//	int[] conf = llnode.getConfusion();
//	for (int i=0;i<4;i++)
//		conf[i] += confusionconst[i];
//	for (int i=0;i<4;i++)
//		result[2+i]=conf[i];
//
//	return result;
//
//}



public GGCPMNode findInAllnodes(String key){
	return allNodes.get(key);
}

public GGCPMNode findInAllnodes(CPModel pf, int inputcaseno, int observcaseno, RelStruc A ){

	if (pf == null) System.out.println("pf is null");
	return allNodes.get(makeKey(pf,inputcaseno,observcaseno,A));
}

public GGAtomMaxNode findInMaxindicators(GroundAtom at){
	Rel r = at.rel();
	Vector<GGAtomMaxNode> mxnodes = maxindicators.get(r);
	if (mxnodes != null)
		for (GGAtomMaxNode mxn: mxnodes)
			if (mxn.myatom().equals(at))
				return mxn;
	return null;
}


public Hashtable<Rel,int[]> getMapVals(){

	Hashtable<Rel,int[]> result = new Hashtable<Rel,int[]>();

	for (Rel r: mapatoms.keySet()) {
		int[] rvals = new int[mapatoms.get(r).size()];
		int idx=0;
		for (GGAtomMaxNode mnode: maxindicators.get(r)) {
			rvals[idx]=mnode.getCurrentInst();
			idx++;
		}
		result.put(r, rvals);

	}

	return result;
}

//	/* Computes the likelihood contribution of the upper ground atom nodes contained in ugas,
//	 * and writes into singlells the factors of the individual nodes (return value is product
//	 * of singlells values)
//	 *
//	 */
//	public static double computePartialLikelihood(Vector<GGCPMNode> ugas, double[] singlells){
//		GGCPMNode nextggpfn;
//		Object nextival;
//		double val;
//
//		//System.out.println("computePartialLikelihood: ");
//		for (int i=0;i<ugas.size();i++){
//			nextggpfn=ugas.elementAt(i);
//			val=nextggpfn.value();
//			nextival=nextggpfn.getInstval();
//			//System.out.print(nextggpfn.getMyatom() + ":" );
//			if (nextival instanceof Integer){
//				if ((Integer)nextival==1)
//					singlells[i]=val;
//				else
//					singlells[i]=1-val;
//			}
//			if (nextival instanceof GGAtomNode){
//				if (((GGAtomNode)nextival).getCurrentInst()==1)
//					singlells[i]=val;
//				else
//					singlells[i]=1-val;
//			}
//			//System.out.print(singlells[i] + " ");
//		}
//		//System.out.println();
//		double result =1;
//		for (int k=0;k<singlells.length;k++)
//			result = result*singlells[k];
//		return result;
//	}

/* Computes the likelihood contribution of the upper ground atom nodes contained in ugas)
 *
 */
//	public static double computePartialLogLikelihood(Vector<GGCPMNode> ugas){
//
//		double result=0;
//
//		for (GGCPMNode next_uga: ugas)
//			if (next_uga.isBoolean()) {
//				if (next_uga.instval() == 1)
//					result+= Math.log(next_uga.value()[0]);
//				else
//					result+= Math.log(1-next_uga.value()[0]);
//			}
//			else
//				result += Math.log(next_uga.value()[next_uga.instval()]);
//		return result;
//	}

public int getNextId(){
	maxid++;
	return maxid;
}

private double[] getDirectionAscentAdagrad(double[] gradient){
	double[] result = new double[gradient.length];
	/* Manipulate gradient */
	for (int i=0;i<result.length;i++){
		result[i]=gradient[i]/Math.sqrt(gradmemory[i]+myggoptions.adagradepsilon());
	}
	/* update gradmemory */
	for (int i=0;i<gradmemory.length;i++){
		gradmemory[i]=myggoptions.adagradfade()*gradmemory[i]+(1-myggoptions.adagradfade())*Math.pow(gradient[i],2);
	}
	return result;
}




private double[] getDirectionLBFGS(double[] gradient,int itercount){

	double[] result = gradient.clone();

	int bound = Math.min(itercount, myggoptions.lbfgsmemory() );
	double[] alphas = new double[bound];
	double[] betas = new double[bound];
	int offset = (itercount<=myggoptions.lbfgsmemory())? 0: itercount-myggoptions.lbfgsmemory();

	for (int i = bound-1; i>=0; i--){
		int j = (i+offset) % myggoptions.lbfgsmemory();
		alphas[i]=rhos[j]*rbnutilities.arrayDotProduct(thetadiffhistory[j], result);
		result = rbnutilities.arraySubtract(result,rbnutilities.arrayScalMult(graddiffhistory[j], alphas[i]));
	}

	/* Now should come the multiplication with the initial inverse pseudo-Hessian H_0.
	 * Taking H_0 to be the identity matrix, nothing to do at this point!
	 */

	for (int i = 0; i< bound; i++){
		int j = (i+offset) % myggoptions.lbfgsmemory();
		betas[i]=rhos[j]*rbnutilities.arrayDotProduct(graddiffhistory[j],result);
		result = rbnutilities.arrayAdd(result, rbnutilities.arrayScalMult(thetadiffhistory[j], alphas[i]-betas[i]));
	}
	return result;
}

private double[] getDirectionFletcherReeves(double[] gradient, double[] oldgradient, double[] olddirection){
	double[] result = gradient.clone();
	if (oldgradient != null){
		double gg = rbnutilities.arrayDotProduct(gradient, gradient);
		double ogog = rbnutilities.arrayDotProduct(oldgradient, oldgradient);
		result = rbnutilities.arrayAdd(result ,rbnutilities.arrayScalMult(olddirection, gg/ogog));
	}
	return result;
}

private int numLinks() {
	int result =0;
	for (GGCPMNode pfn:allNodes.values()) {
		result += pfn.childrenSize();
	}
	return result;
}

private TreeSet<GGAtomMaxNode> maxind_as_ts(){
	TreeSet<GGAtomMaxNode> result = new TreeSet<GGAtomMaxNode>();
	for (Rel r: maxindicators.keySet()) {
		for (GGAtomMaxNode mn: maxindicators.get(r))
			if (mn.getmapInstVal()==-1)
				result.add(mn);
	}
	return result;
}

	private Vector<GGAtomMaxNode> maxind_as_vec() {
		Vector<GGAtomMaxNode> result = new Vector<>();
		for (Rel r : maxindicators.keySet()) {
			for (GGAtomMaxNode mn : maxindicators.get(r)) {
				if (mn.getmapInstVal() == -1) {
					result.add(mn);
				}
			}
		}
		return result;
	}

	private ArrayList<GGAtomMaxNode> maxind_as_list() {
		ArrayList<GGAtomMaxNode> result = new ArrayList<>();
		for (Rel r : maxindicators.keySet()) {
			for (GGAtomMaxNode mn : maxindicators.get(r)) {
				if (mn.getmapInstVal() == -1) {
					result.add(mn);
				}
			}
		}
		return result;
	}

public Hashtable<Rel, Vector<GGAtomMaxNode>> getMaxindicators() {
	return maxindicators;
}

	public void initGnnPy(RBN rbn) {
		for(int i=0; i<rbn.prelements().length; i++) {
			if (rbn.cpmod_prelements_At(i) instanceof CatGnn)
				((CatGnn) rbn.cpmod_prelements_At(i)).getGnnPy().initData();
		}
	}

public void setGnnPyToNodes() {
	for (GGNode node: this.llnode.children){
		if (node instanceof GGGnnNode)
			((GGGnnNode) node).setGnnPy(this.gnnPy);
	}
}

public void setGnnPy(GnnPy gnnPy) {
	this.gnnPy = gnnPy;
	setGnnPyToNodes();
}

	public void setMapSearchAlg(int mapSearchAlg) {
		this.mapSearchAlg = mapSearchAlg;
	}

	public void setNumIterGreedyMap(int setNumIterGreedyMap) {
		this.nIterGreedy = setNumIterGreedyMap;
	}

	public void load_gnn_settings(Map<String, Object> sett){
		if (this.gnnPy != null) {
			gnnPy.load_gnn_set(sett);
		}
	}

	public void setPrimulaGUI(PrimulaGUI primulaGUI) { this.primulaGUI = primulaGUI; }

	public Vector<GGCPMNode> getllchildred() {
		return llnode.children;
	}

	public void setNumChains(int numChains) { numchains = numChains; }
	public void setWindowSize(int ws) { windowsize = ws; }

	public int getWindowIndex() { return windowindex; }

	public int getBatchSearchSize() {
		return batchSearchSize;
	}

	public void setBatchSearchSize(int batchSearchSize) {
		this.batchSearchSize = batchSearchSize;
	}

	public int getSampleSizeScoring() {
		return sampleSizeScoring;
	}

	public void setSampleSizeScoring(int sampleSizeScoring) {
		this.sampleSizeScoring = sampleSizeScoring;
	}

	public int getLookaheadSearch() {
		return lookaheadSearch;
	}

	public void setLookaheadSearch(int lookaheadSearch) {
		this.lookaheadSearch = lookaheadSearch;
	}
}
