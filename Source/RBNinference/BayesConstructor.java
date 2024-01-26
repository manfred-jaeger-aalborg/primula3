/*
 * BayesConstructor.java
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
import java.io.*;
import javax.swing.JOptionPane;

import RBNpackage.*;
import RBNgui.*;
import RBNExceptions.*;
import RBNutilities.*;



public class BayesConstructor extends java.lang.Object {


	private Vector<BNNode> complexnodes;
	private Hashtable<String,BNNode> groundatomhasht;
	private Hashtable<String,BNNode> simplenodehasht;
//	private boolean[] isolatedzeroind;
	private BayesNetInt bni;
	private int numnodes = 0;
//	private int reccalls = 0;

	private static int NODEWIDTH = 1;
	private static final int COMPLNODE  = 0;
	private static final int PFNNODE  = 1;

	private RBN rbnarg;
	private RelStruc strucarg;
	private File bnoutputfile;
	private OneStrucData instarg;
	private GroundAtomList queryatoms;
	private String myAlternateName;
	private Primula myprimula;

//	private Rel[] relations;
//	private int domsize;



	/** Creates new BayesConstructor */
	public BayesConstructor() {
	}

	public BayesConstructor(RBN r, RelStruc rs, OneStrucData in, 
			GroundAtomList qats, File bnout) {
		rbnarg = r;
		strucarg = rs;
		bnoutputfile = bnout;
		instarg = in;
		queryatoms = qats;
	}

	public BayesConstructor(RBN r, RelStruc rs, OneStrucData in, 
			GroundAtomList qats, File bnout, Primula primula) {
		this(r,rs,in,qats,bnout);
		this.myprimula = primula;
	}
	
	public BayesConstructor(RBN r, RelStruc rs, OneStrucData in, GroundAtomList qats) {
		this( r, rs, in, qats, (File)null );
	}

	public BayesConstructor(RBN r, RelStruc rs, OneStrucData in, 
			GroundAtomList qats, Primula primula) {
		this( r, rs, in, qats, (File)null );
		this.myprimula = primula;
	}

	public BayesConstructor( Primula primula, OneStrucData in, GroundAtomList qats) {
		this( primula.getRBN(), primula.getRels(), in, qats, (File)null );
		this.myprimula = primula;
	}

	/**
       @author Keith Cascio
       @since 040504
	 */
	public BayesConstructor( RBN r, RelStruc rs, OneStrucData in, GroundAtomList qats, String name, Primula primula )
	{
		this( r, rs, in, qats, (File)null );
		this.myAlternateName = name;
		this.myprimula = primula;
	}

	public BayesConstructor( Primula primula , OneStrucData in, GroundAtomList qats, String name)
	{
		this(primula.getRBN(),primula.getRels(), in, qats, (File)null );
		this.myAlternateName = name;
		this.myprimula = primula;
	}



	private void buildInitialGAHT(int querymode, int evidencemode, int nodetype ,int isolatedzeronodesmode)
			throws RBNCompatibilityException,RBNIllegalArgumentException{
		if (querymode == Primula.OPTION_NOT_QUERY_SPECIFIC)
			buildInitialGAHT(evidencemode, nodetype , isolatedzeronodesmode);
		else { // Primula.OPTION_QUERY_SPECIFIC
			groundatomhasht = new Hashtable<String,BNNode>();
			Stack<BNNode> toprocess = new Stack<BNNode>();
			
			/* A set containing all the relations that are represented by atoms in 
			 * groundatomhasht after query atoms and their parents have been added.
			 * 
			 * Used to avoid adding evidence atoms that do not have any predecessors in
			 * groundatomhasht.
			 */
			TreeSet<Rel> relset = new TreeSet<Rel>();
			
			
			/* Start by adding all queryatoms to groundatomhashtable, and also put them on stack of nodes whose parents still 
			 * have to be added
			 */
			for (GroundAtom ga: queryatoms.allAtoms()) {
				BNNode newestnode;
				CPModel cpm = myprimula.getRBN().cpmodel(ga.rel());
				if (!relset.contains(ga.rel())) {
					relset.add(ga.rel());
					relset.addAll(myprimula.getRBN().ancestorRels(ga.rel()));
				}
				
				CPModel groundcpm = cpm.substitute(myprimula.getRBN().args(ga.rel()), ga.args());
				String argumentnames = rbnutilities.namestring(ga.args(),strucarg);
				switch (nodetype){
				case COMPLNODE:
					newestnode = new ComplexBNGroundAtomNode(
							ga.rel(),
							argumentnames,
							ga.args(),
							groundcpm);
					break;
				case PFNNODE:
					newestnode = new ComplexPFNetworkNode(
							ga.rel(),
							argumentnames,
							ga.args(),
							groundcpm);
					break;
				default:
					newestnode = new ComplexBNGroundAtomNode();
					throw new RuntimeException("Illegal case encountered");
				}
				if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED)
					newestnode.instantiate(instarg.truthValueOf(ga.rel(),ga.args()));
				
				groundatomhasht.put(ga.asString(), newestnode);
				toprocess.push(newestnode);
			} // for (GroundAtom ga: queryatoms.allAtoms()) 
			
			/* Now do the same for evidence atoms */
			if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED) {
				BNNode newestnode;
				for (BoolRel br:  instarg.getBoolRels()){
					/* Only proceed if br has ancestors included in relset
					 * Otherwise evidence atoms for br will not have common
					 * ancestors with query atoms
					 * 
					 * The first condition tests whether br might be a relation that 
					 * is declared in the instantiation structure, but for which the
					 * RBN does not actually contain a probability formula
					 */
					if (myprimula.getRBN().cpmodel(br) != null &&  ! emptyIntersect(rbnarg.ancestorRels(br),relset)) {
						CPModel cpm = myprimula.getRBN().cpmodel(br);
						for (int[] arg : instarg.allInstantiated(br)) {
							CPModel groundcpm = cpm.substitute(myprimula.getRBN().args(br), arg);
							GroundAtom ga = new GroundAtom(br,arg);
							String argumentnames = rbnutilities.namestring(ga.args(),strucarg);
							switch (nodetype){
							case COMPLNODE:
								newestnode = new ComplexBNGroundAtomNode(
										ga.rel(),
										argumentnames,
										ga.args(),
										groundcpm);
								break;
							case PFNNODE:
								newestnode = new ComplexPFNetworkNode(
										ga.rel(),
										argumentnames,
										ga.args(),
										groundcpm);
								break;
							default:
								newestnode = new ComplexBNGroundAtomNode();
								throw new RuntimeException("Illegal case encountered");
							}
							newestnode.instantiate(instarg.truthValueOf(ga.rel(),ga.args()));

							groundatomhasht.put(ga.asString(), newestnode);
							toprocess.push(newestnode);
						}
					} // if (! emptyIntersect(rbnarg.ancestorRels(br),relset)) 
				} // for (BoolRel br:  instarg.getBoolRels())
			} // if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED) 
			
			/* Now add ancestors of nodes in groundatomhasht.
			 * 
			 * Maintain invariant that if a node is in groundatomhasht, then 
			 * either it is also in the toprocess stack, or all its parents have
			 * already been added to the toprocess stack
			 * 
			 */
			while (!toprocess.empty()) {
				BNNode bn = toprocess.pop();
				CPModel pf = null;
				GroundAtom ga = null;
				if (bn instanceof ComplexBNGroundAtomNode) {
					ComplexBNGroundAtomNode cbn = (ComplexBNGroundAtomNode)bn;
					pf = cbn.cpmodel();
					ga = cbn.myatom();
				}
				else if (bn instanceof ComplexPFNetworkNode) {
					pf = ((ComplexPFNetworkNode)bn).cpmodel();
					ga = ((ComplexPFNetworkNode)bn).myatom();
				}
				else
					throw new RBNCompatibilityException("Did not get expected complex node type");
				/* Set instantiation */
				if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED)
				{
					bn.instantiate(instarg.truthValueOf(ga));
				}
				/* add bn to groundatomhasht */
				groundatomhasht.put(ga.asString(), bn);
				/* add parents to stack */
				
				//need to perform substitution or pass substitution argument here!
				
	
				
				Vector<GroundAtom> parents = null;
				TreeSet<String> macrosdone = new TreeSet<String>();
				if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED){
					parents=pf.makeParentVec(strucarg,instarg,macrosdone);
				}
				else 
					parents=pf.makeParentVec(strucarg, new OneStrucData(), macrosdone);
				
				for (GroundAtom pga: parents) {
					BNNode newestnode;
					if (groundatomhasht.get(pga.asString())==null) {
						CPModel ppf = myprimula.getRBN().cpmodel(pga.rel());
						String[] args = myprimula.getRBN().arguments(pga.rel());
						CPModel ppf_sub = ppf.substitute(args, pga.args);
						String argumentnames = rbnutilities.namestring(pga.args(),strucarg);
						switch (nodetype){
						case COMPLNODE:
							newestnode = new ComplexBNGroundAtomNode(
									pga.rel(),
									argumentnames,
									pga.args(),
									ppf_sub);
							break;
						case PFNNODE:
							newestnode = new ComplexPFNetworkNode(
									pga.rel(),
									argumentnames,
									pga.args(),
									ppf_sub);
							break;
						default:
							newestnode = new ComplexBNGroundAtomNode();
							throw new RuntimeException("Illegal case encountered");
						}
						if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED)
							newestnode.instantiate(instarg.truthValueOf(ga.rel(),ga.args()));
						toprocess.push(newestnode);
					} // if (groundatomhasht.get(pga.asString())==null) 
						
				} // for (GroundAtom pga: pf.makeParentVec(strucarg))
			} // while (!toprocess.empty())
		}// end Primula.OPTION_QUERY_SPECIFIC
	}
	
	/* builds an initial groundatomhasht for all possible ground atoms*/
	private void buildInitialGAHT(int evidencemode, int nodetype ,int isolatedzeronodesmode)
	throws RBNCompatibilityException,RBNIllegalArgumentException
	{
		groundatomhasht = new Hashtable<String,BNNode>();

		/* Generate all ground atoms
		 */

		Rel thisrel;
		String[] thisvars;
		int[] thistuple;
		GroundAtom thisatom;
		CPModel thispf;
		CPModel groundpf;

		for (int i = 0;i<rbnarg.NumPFs();i++)
		{
			thisrel = rbnarg.relAt(i);
			thisvars = rbnarg.arguments_prels_At(i);
			thispf = rbnarg.cpmod_prelements_At(i);
			
			int[][] allargs=strucarg.allArgTuples(thisrel);

			for (int j =0;j<allargs.length;j++){
				thistuple = allargs[j];
				thisatom = new GroundAtom(thisrel,thistuple);
				/* Determine the domainelement names corresponding to
				 * thistuple
				 */ 
				String argumentnames = rbnutilities.namestring(thistuple,strucarg);
				groundpf = thispf.substitute(thisvars,thistuple);


				BNNode newestnode;
				switch (nodetype){
				case COMPLNODE:
					newestnode = new ComplexBNGroundAtomNode(thisrel,
							argumentnames,
							thistuple,
							groundpf);
					break;
				case PFNNODE:
					newestnode = new ComplexPFNetworkNode(thisrel,
							argumentnames,
							thistuple,
							groundpf);
					break;
				default:
					newestnode = new ComplexBNGroundAtomNode();
					throw new RuntimeException("Illegal case encountered");
				}
				if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED)
				{
					newestnode.instantiate(instarg.truthValueOf(thisrel,thistuple));
				}
				groundatomhasht.put(thisatom.asString(), newestnode);
				// 		}
				numnodes++;
				++myProgress;//keith cascio 20060515
			}
		}
	}


	/* Connects the nodes in groundatomhasht
	 * according to parentvecs generated by
	 * makeParentvecs
	 *
	 * It is assumed that initially all parent and
	 * children linkelLists are empty!
	 *
	 * Only to be used when all nodes are in
	 * groundataomhasht, i.e. no auxiliary
	 * nodes from decomposition are present
	 * 
	 * Method requires that groundatomhasht and parentvecs
	 * are synchronized in the sense that the i'th element of 
	 * parentvecs is the parentvector of the i'th element in
	 * the enumeration groundatomhasht.elements
	 */
	private void connectParents(Vector<Vector<GroundAtom>> parentvecs){
		Vector<GroundAtom> parvec = null;
		BNNode nextpar;
		BNNode newgatn;
		int nodeindex = 0;
		for (Enumeration<BNNode> e=groundatomhasht.elements();e.hasMoreElements();){
			newgatn = e.nextElement();
			parvec = (Vector<GroundAtom>)parentvecs.elementAt(nodeindex);
			for (int j=0;j<parvec.size();j++){
				nextpar = groundatomhasht.get(((GroundAtom)parvec.elementAt(j)).asString());
				newgatn.addToParents(nextpar);
				nextpar.addToChildren(newgatn);
			}
			nodeindex++;
		}
	}


	private Vector<BNNode> exportnodes(int isolatedzeronodesmode){
		Vector<BNNode> result = new Vector<BNNode>();
		for (Enumeration<BNNode> e=groundatomhasht.elements();e.hasMoreElements();) {
			BNNode nextn = e.nextElement();
			switch (isolatedzeronodesmode) {
				case Primula.OPTION_NOT_ELIMINATE_ISOLATED_ZERO_NODES:{
					result.add(nextn);
					break;
				}
				case Primula.OPTION_ELIMINATE_ISOLATED_ZERO_NODES:{
					if (!((SimpleBNNode)nextn).isIsolatedZeroNode())
						result.add(nextn);
					break;
				}
			}
		}
		return result;
	}


	

	public PFNetwork constructPFNetwork(int evidencemode,
			int querymode,
			int isolatedzeronodesmode)
	throws RBNCompatibilityException,RBNCyclicException,RBNIllegalArgumentException
	{

		myprimula.showMessage("construct PF network...");
		/* The following lines all operate on
		 * groundatomhasht
		 */
		buildInitialGAHT(querymode, evidencemode, PFNNODE , isolatedzeronodesmode);
		
		Vector parentvecs = makeParentvecs(evidencemode);
		connectParents(parentvecs);


		Vector exportnodes = null; // Todo exportnodes(evidencemode, querymode, isolatedzeronodesmode);
		/* the PFNetwork constructor requires a vector containing all nodes;
		 * therefore have to add all predecessors of nodes in exportnodes to
		 * exportnodes
		 */
		
		Vector<BNNode> nodestack;
		Vector<BNNode> allnodes = new Vector<BNNode>();

		for (Enumeration<BNNode> en= groundatomhasht.elements(); en.hasMoreElements();){
			BNNode bnn = en.nextElement();
			if (!bnn.visited[0]){
				nodestack = bnn.buildNodeStack();
				for (int j=0;j<nodestack.size();j++){
					BNNode bnn2=(BNNode)nodestack.elementAt(j);
					bnn2.visited[0]=true;
					allnodes.add(bnn2);
				}
			}
		}
		for (int i=0;i<allnodes.size();i++)
			((BNNode)allnodes.elementAt(i)).visited[0]=false;
		setDepths(allnodes);
		myprimula.appendMessage("done");

		return new PFNetwork(myprimula,allnodes,strucarg,instarg);

	}

	public boolean constructCPTNetwork(int evidencemode,
			int querymode,
			int decomposemode,
			int isolatedzeronodesmode,
			int layoutmode,
			int bnsystem)
	throws RBNCompatibilityException,RBNCyclicException,RBNIllegalArgumentException
	{
		try{
			if( (decomposemode != Primula.OPTION_NOT_DECOMPOSE) && (!rbnarg.multlinOnly()) ){
				String msg           = "Decompose mode is on, but the current model is not decomposable because it contains a non-multilinear combination function.";
				int    options       = -1;
				String captionIgnore = null;
				if( myprimula == null ){
					options          = JOptionPane.OK_CANCEL_OPTION;
					captionIgnore    = "OK";
				}
				else{
					options          = JOptionPane.YES_NO_CANCEL_OPTION;
					captionIgnore    = "No";
					msg             += "\nClick \"Yes\" to change the Primula decompose mode to \"none\" and continue construction without decomposition.";
				}
				msg                 += "\nClick \""+captionIgnore+"\" to ignore the Primula decompose mode and continue construction without decomposition.\nClick \"Cancel\" to abort construction.";

				int result = JOptionPane.showConfirmDialog( myprimula, msg, "Model Not Decomposable", options );
				if( result == JOptionPane.CANCEL_OPTION ) return false;
				else{
					decomposemode = Primula.OPTION_NOT_DECOMPOSE;
					if( (myprimula != null) && (result == JOptionPane.YES_OPTION) ) myprimula.setDecomposeMode( Primula.OPTION_NOT_DECOMPOSE );
				}
			}

			/** keith cascio 20060515 ... */
			myProgress    = 0;
			myProgressMax = 1;

			/* A quick fix... see whether it makes any difference! */
			int numatoms = 100;
			//System.out.println( "numatoms " + numatoms );
			/** for buildInitialGAHT() */
			myProgressMax += numatoms;
			/** for makeBNetwork() */
			if( decomposemode == Primula.OPTION_NOT_DECOMPOSE ) myProgressMax += numatoms;
			/** for makeDecBNetwork() */
			else myProgressMax += numatoms;
			/** for exportnodes() */
			myProgressMax += numatoms;
			if( querymode == Primula.OPTION_QUERY_SPECIFIC ) myProgressMax += numatoms;
			/** for remainder */
			int estNumExportNodes = -1;
			if( querymode == Primula.OPTION_NOT_QUERY_SPECIFIC ) estNumExportNodes = numatoms;
			else{
				estNumExportNodes = queryatoms.size();
				if( evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED ) estNumExportNodes += instarg.size();
			}
			myProgressMax += estNumExportNodes;
			//System.out.println( "estNumExportNodes " + estNumExportNodes );
			int factor = ( layoutmode == Primula.OPTION_LAYOUT ) ? 6 : 3;
			//System.out.println( "factor " + factor );
			int estRemainder = (estNumExportNodes * factor);
			//System.out.println( "estRemainder " + estRemainder );
			myProgressMax += estRemainder;
			/** ... keith cascio */

			myprimula.showMessage("construct CPT network...");
			
			
			buildInitialGAHT(querymode, evidencemode, COMPLNODE,isolatedzeronodesmode );
			

		
			/** keith cascio 20060515 ... */

			/** ... keith cascio */

			/* build a standard Bayesian network over the nodes in groundatomhasht:
			 * in case of a decompose option, additional (auxiliary)  nodes will be introduced
			 * that are not in groundatomhasht, but are accessible by backward chaining from
			 * nodes in groundatomhasht.
			 */
			switch (decomposemode){
			case Primula.OPTION_DECOMPOSE:{
				makeDecBNetwork(evidencemode,decomposemode);
				break;}
			case Primula.OPTION_NOT_DECOMPOSE:{
				makeBNetwork(evidencemode);
				break;
			}
			case Primula.OPTION_DECOMPOSE_DETERMINISTIC:{
				makeDecBNetwork(evidencemode,decomposemode);
				break;
			}
			}

			
			/* Now all nodes in groundatomhasht (and all other nodes in network)
			 * are SimpleBNNodes
			 */
			Vector<BNNode> exportnodes = exportnodes(isolatedzeronodesmode);
  
			
			/*Reduce exportnodes to list that contains only one representative
			 *node from each connected component of the network
			 */
			int position = 0;
			while (position < exportnodes.size()) {
				removeReachable((SimpleBNNode)exportnodes.elementAt(position),exportnodes,true);
				position++;
				++myProgress;//keith cascio 20060515
			}

			//revise the estimate
			myProgressMax -= estRemainder;
			myProgressMax += (exportnodes.size() * factor);

			SimpleBNNode nextconvcomp;

			//Remove redundant auxiliary nodes. An auxiliary node is redundant
			//(i.e. it is not needed to reduce the connectivity of the network)
			//if it has at most one parent
			boolean red;
			for (int i=0;i<exportnodes.size();i++){
				nextconvcomp = (SimpleBNNode)exportnodes.elementAt(i);
				red = true;
				while (red){
					red = marginalizeOutRedundant(nextconvcomp,decomposemode);
					//nextconvcomp.resetVisited(-1);
				}
				++myProgress;//keith cascio 20060515
			}

			

//			// Merge equivalent deterministic nodes
//			Temporarily disabled (needs update for categorical)
//			boolean merge;
//			for (int i=0;i<exportnodes.size();i++){
//				nextconvcomp = (SimpleBNNode)exportnodes.elementAt(i);
//				merge = true;
//				while (merge)
//					merge = mergeEquivalentDetNodes(nextconvcomp);
//				++myProgress;//keith cascio 20060515
//			}

			
			
			if (layoutmode == Primula.OPTION_LAYOUT){
				Primula.appendMessage("layout...");
				// 	    // Set depths of nodes, includes acyclicity check
				int[] maxlevels = setDepths(exportnodes);

				int sum = 0;
				for( int depth : maxlevels ) sum += depth;
				int costOfBalancing = sum * 5;

				//revise the estimate
				myProgressMax += costOfBalancing;


				/*Set heights*/
				Vector nodestack = new Vector();
				SimpleBNNode topnode;
				for (int i = 0; i<exportnodes.size(); i++){
					nextconvcomp = (SimpleBNNode)exportnodes.elementAt(i);
					nodestack.add(nextconvcomp);
					while (!nodestack.isEmpty()){
						topnode = (SimpleBNNode)nodestack.elementAt(nodestack.size()-1);
						nodestack.removeElementAt(nodestack.size()-1);
						setHeight(topnode, nodestack);
					}
					nextconvcomp.resetVisitedUpDownstream(-1);
					++myProgress;//keith cascio 20060515
				}

				/* Set levels of nodes */
				for (int i = 0; i<exportnodes.size(); i++){
					nextconvcomp = (SimpleBNNode)exportnodes.elementAt(i);
					setLevel(nextconvcomp,maxlevels[i]);
					nextconvcomp.resetVisitedUpDownstream(0);
					++myProgress;//keith cascio 20060515
				}

				/* Set xcoordinates */
				for (int i = 0; i<exportnodes.size(); i++){
					nextconvcomp = (SimpleBNNode)exportnodes.elementAt(i);
					balanceLevels(nextconvcomp,maxlevels[i]);
					nextconvcomp.resetVisitedUpDownstream(0);
					++myProgress;//keith cascio 20060515
				}

			} // end OPTION_LAYOUT

			
			
			Primula.appendMessage("export...");

			switch (bnsystem){
			case Primula.OPTION_JAVABAYES: bni = new BayesNetIntBIF(bnoutputfile);break;
			case Primula.OPTION_HUGIN: bni = new BayesNetIntHuginNet(bnoutputfile);break;
			case Primula.OPTION_NETICA: bni = new BayesNetIntNeticaDnet(bnoutputfile);break;
			case Primula.OPTION_SAMIAM: bni = new BayesNetIntSamIam( myprimula, myAlternateName );break;
			}

			int xoffset = 0;

			for (int i = 0;i<exportnodes.size();i++){
				nextconvcomp = (SimpleBNNode)exportnodes.elementAt(i);
				switch (isolatedzeronodesmode){
				case Primula.OPTION_NOT_ELIMINATE_ISOLATED_ZERO_NODES:
					exportBNNode(nextconvcomp,evidencemode,xoffset);
					xoffset = xoffset + computeWidth(nextconvcomp);
					break;
				case Primula.OPTION_ELIMINATE_ISOLATED_ZERO_NODES:
					if (!nextconvcomp.isIsolatedZeroNode()){
						exportBNNode(nextconvcomp,evidencemode,xoffset);
						xoffset = xoffset + computeWidth(nextconvcomp);
						break;
					}
				}
				++myProgress;//keith cascio 20060515
			}

			bni.open();
			Primula.appendMessage("done");
		}catch( InterruptedException interruptedexception ){
			Primula.appendMessage( "interrupted" );
			return false;
		}

		return true;
	}

	/** @author keith cascio
    	@since 20060515 */
	public int getProgress(){
		return myProgress;
	}

	/** @author keith cascio
    	@since 20060515 */
	public int getProgressMax(){
		return myProgressMax;
	}

	/** @author keith cascio
    	@since 20060515 */
	private int myProgress = 0;
	/** @author keith cascio
    	@since 20060515 */
	private int myProgressMax = 10;


	/* Compute a CPT representation of conditional distribution defined
	 * by pform. parentatoms contains the atoms on which pform effectively
	 * depends given inst. The computed cpt is w.r.t. the parent order defined
	 * by parentatoms
	 * 
	 * the cpt has dimensions (#parent configurations) x (#values of relation) 
	 * 
	 * The probability values are listed in the order defined by their integer
	 * indices (Boolean case: first false, then true)
	 * 
	 * Also returns a hashmap <Integer,int[]> that maps indices of parent configurations
	 * to the tuple of parent values at that index. 
	 */
	public static Object[] makeCPT(CPModel cpmodel,RelStruc A,OneStrucData inst,Vector<GroundAtom> parentatoms)
	throws RBNCompatibilityException
	{
		boolean iscatmodel = (cpmodel instanceof CatModelSoftMax);
		boolean isboolmodel = !iscatmodel;
		
		int[] numparvals = new int[parentatoms.size()];
		
		int numparconfigs =1;
		for (int i=0;i<parentatoms.size();i++) {
			numparvals[i]=(int)parentatoms.elementAt(i).rel.numvals();
			numparconfigs*=numparvals[i];
		}
		
		int numvals = cpmodel.numvals();
		double[][] cpt = new double[numparconfigs][numvals];
		HashMap<Integer,int[]> indxToTuple = new HashMap<Integer,int[]>();
		
		int[]  oldinst;
		int[]  newinst;
		int[]  diffinst;
		int diff;

		/*TODO : avoid costly copying of inst.
		 * 
		 */
		OneStrucData copyinst = inst.copy();
		/* Additional bit-vector representations of instantiations: */
		oldinst = new int[parentatoms.size()];
		newinst = new int[parentatoms.size()];
		diffinst = new int[parentatoms.size()];


		/* Initialize instasosd as the instantiation that
		 * assigns false to all atoms in parentatoms
		 */
		for (int j=0;j<parentatoms.size();j++){
			newinst[j]=0;
			copyinst.add((GroundAtom)parentatoms.elementAt(j),0,"?");
		}

		/* Iterate over all instantiations */
		for (int h=0;h<cpt.length;h++){
			indxToTuple.put(h, newinst);
			if (isboolmodel) {
			double trueval =(double)cpmodel.evaluate(A,
					copyinst,
					new String[0], 
					new int[0], 
					0,
					true, 
					true,
					new GroundAtomList(),
					false,
					new Hashtable<String,Object[]>(),
					null,
					ProbForm.RETURN_ARRAY,
					true,
					null)[0];
			cpt[h]=new double[] {1-trueval,trueval};
			}
			if (iscatmodel) {
				cpt[h] =(double[])cpmodel.evaluate(A,
						copyinst,
						new String[0], 
						new int[0], 
						0,
						true, 
						true,
						new GroundAtomList(),
						false,
						new Hashtable<String,Object[]>(),
						null,
						ProbForm.RETURN_ARRAY,
						true,
						null)[0];
				}
			
			/* the last two arguments here are just dummy arguments,
			 * because pform is ground
			 *
			 * Now create the next instantiation
			 */
			if (h<cpt.length-1){
				for (int k=0;k<newinst.length;k++)
					oldinst[k]=newinst[k];
				rbnutilities.incrementCatVector(numparvals,newinst);
				for (int k=0;k<newinst.length;k++){
					if (oldinst[k]!=newinst[k])
						copyinst.add((GroundAtom)parentatoms.elementAt(k),newinst[k],"?");
				}
			}
		}
		return new Object[] {cpt,indxToTuple};
	}

	private void makeBNetwork(int evidencemode)
	throws RBNCompatibilityException
	{

		OneStrucData inst=null;

		Vector<Vector<GroundAtom>> parentvecs = new Vector<Vector<GroundAtom>> (); // Vector of Vector
		Vector<GroundAtom> parvec = null;
		ComplexBNGroundAtomNode currentnode;
		CPModel cpmodel;
		double[][] cpt;
		SimpleBNGroundAtomNode newgatn;


		parentvecs = makeParentvecs(evidencemode);
		int nodeindex = 0;
		/* Now construct the cpts
		 * for all nodes and replace
		 * ComplexBNGroundAtomNodes in groundatomhasht with
		 * SimpleBNGroundAtomNodes
		 */
		
		
		
		switch (evidencemode){
		case Primula.OPTION_NOT_EVIDENCE_CONDITIONED:{
			inst = new OneStrucData();
			break;
		}
		case Primula.OPTION_EVIDENCE_CONDITIONED:{
			inst = instarg.copy();
			break;
		}
		}
		
		for (Enumeration<BNNode> e=groundatomhasht.elements();e.hasMoreElements();){
			System.out.print(".");
			currentnode = (ComplexBNGroundAtomNode)e.nextElement();
			parvec = (Vector<GroundAtom>)parentvecs.elementAt(nodeindex);
			nodeindex++;
			cpmodel = currentnode.cpmodel;
			GroundAtom atom = currentnode.myatom();
			String name = currentnode.name;

		
			/* turn complexnode into simplenode
			 */
			cpt = (double[][])makeCPT(cpmodel,strucarg,inst,parvec)[0];
			
			newgatn = new SimpleBNGroundAtomNode(atom,
					name,
					cpt,
					new Vector(),
					new Vector());

			if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED){
				newgatn.instantiate(currentnode.instantiated);
			}
			/* add simplified node to groundatomhasht*/
			groundatomhasht.put(atom.asString(), newgatn);

			++myProgress;//keith cascio 20060515
		}// (Enumeration<BNNode> e=groundatomhasht.elements();e.hasMoreElements();)

		
		/* Second pass over groundatomhasht:
		 * set parents and children
		 */
		connectParents(parentvecs);
		
	}



	private void makeDecBNetwork(int evidencemode, int decomposemode)
	throws RBNCompatibilityException {

		/* Constructs a standard Bayesian network by
		 * decomposing the probability formulas of the
		 * nodes stored in groundatomhasht and connecting
		 * them
		 *
		 * After completion, the pointers in groundatomhasht
		 * point to SimpleBNGroundAtomNodes
		 */

		complexnodes = new Vector();
		for (Enumeration<BNNode> e=groundatomhasht.elements();e.hasMoreElements();)
			complexnodes.add(e.nextElement());

		while (complexnodes.size() > 0)
		{
			ComplexBNNode nextnode = (ComplexBNNode)complexnodes.lastElement();
			complexnodes.removeElementAt(complexnodes.size()-1);

			/* The following methods can add new elements
			 * to  complexnodes!
			 */
			if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED){
				nextnode.cpmodel = (ProbForm)nextnode.cpmodel.conditionEvidence(strucarg,instarg);
			}

			if (nextnode.cpmodel instanceof ProbFormConstant)
				processConstNode(nextnode,evidencemode);
			if (nextnode.cpmodel instanceof ProbFormAtom)
				processAtomNode((ComplexBNGroundAtomNode)nextnode,evidencemode);
			if (nextnode.cpmodel instanceof ProbFormConvComb)
				processConvCombNode(nextnode,evidencemode,decomposemode);
			if (nextnode.cpmodel instanceof ProbFormCombFunc)
				processCombFuncNode(nextnode,evidencemode,decomposemode);
			if (nextnode.cpmodel instanceof  ProbFormBoolEquality 	)
				processProbFormBoolEquality(nextnode,evidencemode);
			if (nextnode.cpmodel instanceof ProbFormBool){ // and not atomic!
				nextnode.setCPModel(((ProbFormBool)nextnode.cpmodel).toStandardPF(true));
				complexnodes.add(nextnode);
			}

			++myProgress;//keith cascio 20060515
		}
	}


	/* determines for all ComplexBNodes in groundatomhasht
	 * a vector containing all Atoms on which the
	 * evaluation of the probform depends (depending
	 * on evidencemode)
	 * All these vectors are collected in vector
	 * parentvecs
	 * The order of the parentvectors in parentvecs
	 * is determined by the order in which groundatomhasht.elements()
	 * returns the ground atoms!
	 */
	private Vector<Vector<GroundAtom>> makeParentvecs(int evidencemode)
	throws RBNCompatibilityException
	{
		Vector<Vector<GroundAtom>> parentvecs = new Vector<Vector<GroundAtom>>(); // Vector of Vector of ground Atoms
		Vector<GroundAtom> parvec = null;
		BNNode newgatn;
		TreeSet<String> macrosdone = new TreeSet<String>();
		for (Enumeration<BNNode> e=groundatomhasht.elements();e.hasMoreElements();){
			switch (evidencemode){
			case Primula.OPTION_NOT_EVIDENCE_CONDITIONED:{
				newgatn = e.nextElement();
				parvec = newgatn.cpmodel().makeParentVec(strucarg, new OneStrucData() ,macrosdone);
				break;
			}
			case Primula.OPTION_EVIDENCE_CONDITIONED:{
				newgatn = e.nextElement();
				parvec = newgatn.cpmodel().makeParentVec(strucarg,instarg,macrosdone);
				break;
			}
			}
			parentvecs.add(parvec);
		}
		return parentvecs;
	}



	private void substituteNode(BNNode oldnode,BNNode newnode)
	// replaces pointers to oldnode in hashtable and
	// parent lists of the children of oldnode with
	// pointers to newnode
	{
		BNNode nextchild,nextparent;

		ListIterator li = oldnode.children.listIterator();
		while (li.hasNext()){
			nextchild = (BNNode)li.next();
			nextchild.replaceInParentList(oldnode,newnode);
		}
		li = oldnode.parents.listIterator();
		while (li.hasNext()){
			nextparent = (BNNode)li.next();
			nextparent.replaceInChildrenList(oldnode,newnode);
		}
		if (newnode instanceof SimpleBNGroundAtomNode){
			groundatomhasht.put(((GroundAtomNodeInt)newnode).myatom().asString(),newnode);
		}
	}

	private void processConstNode(ComplexBNNode node, int evidencemode){
		// Node must be transformed from ComplexBNNode to SimpleBNNode
		// and new SimpleBNNode substituted in parents-lists of all
		// children of node
		int typeofnode;
		if (node instanceof ComplexBNGroundAtomNode) typeofnode = 0;
		else typeofnode = 1;
		SimpleBNNode newnode;
		double[][] cpt = new double[2][1];
		cpt[0][0] = 1-((ProbFormConstant)node.cpmodel).cval;
		cpt[0][1] = ((ProbFormConstant)node.cpmodel).cval;

		switch (typeofnode)
		{
		case 0:
			newnode = new SimpleBNGroundAtomNode(((ComplexBNGroundAtomNode)node).myatom(),
					((ComplexBNGroundAtomNode)node).name,
					cpt,
					node.parents,
					node.children);
			if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED)
			{
				newnode.instantiate(node.instantiated);
			}
			break;
		case 1:
			newnode = new SimpleBNNode(node.name,cpt,node.parents,node.children);
			break;
		default:
			newnode = new SimpleBNNode();
		}
		// Now substitution in parents lists of children of node
		substituteNode(node,newnode);
	}

	private void processAtomNode(ComplexBNGroundAtomNode node, int evidencemode){
		// ComplexBNNodes on the stack whose Probability Formulas are Atoms
		// can only come from the original ground atoms put on the stack
		// and therefore are ComplexBNGroundAtomNodes
		//
		// Node must be transformed from ComplexBNNode to SimpleBNNode
		// and new SimpleBNNode substituted in parents-lists of all
		// children of node
		SimpleBNGroundAtomNode newnode = new SimpleBNGroundAtomNode(node.myatom(),node.name);
		if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED)
		{
			newnode.instantiate(node.instantiated);
		}

		Rel parrel = ((ProbFormAtom)node.cpmodel).getRelation();
		int[] parargs = rbnutilities.stringArrayToIntArray(((ProbFormAtom)node.cpmodel).getArguments());
		GroundAtom paratom = new GroundAtom(parrel,parargs);
		BNNode par = groundatomhasht.get(paratom.hashCode());
		newnode.parents.add(par);
		par.children.add(newnode);
		/*
		 * TODO: unchecked fix for categorical version
		 */
		double newcpt[][] = {{0,1}};
		newnode.cptentries = newcpt;
		newnode.children = node.children;

		substituteNode(node,newnode);
	}

	private void processConvCombNode(ComplexBNNode node, int evidencemode, int decomposemode)
	throws RBNCompatibilityException
	{
		int typeofnode;
		if (node instanceof ComplexBNGroundAtomNode) typeofnode = 0;
		else typeofnode = 1;
		SimpleBNNode newnode;
		switch (typeofnode)
		{
		case 0:
			newnode = new SimpleBNGroundAtomNode(((ComplexBNGroundAtomNode)node).myatom(),((ComplexBNGroundAtomNode)node).name);
			if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED)
			{
				newnode.instantiate(node.instantiated);
			}
			break;
		case 1:
			newnode = new SimpleBNNode(node.name);
			break;
		default:
			newnode = new SimpleBNNode();
		}

		// Arrays storing the probforms and their types and
		// values (if constants)
		ProbForm[] pf = new ProbForm[3];
		int[] type = new int[3];
		double value[] = {0,0,0};
		BNNode[] parnodes = new BNNode[3];
		//parnodes should contain 3 nodes
		for (int i=0;i<3;i++)
			processComponentOfConvComp(i,node,newnode,pf,type,value,parnodes,decomposemode);

		Vector<BNNode> parents = new Vector<BNNode>();
		int numparents = 0;
		for (int i = 0; i<3; i++)
			if (type[i] != 0 )
			{
				parents.add(parnodes[i]);
				numparents++;
			}
		newnode.parents =parents;
		newnode.children = node.children;

		double[][] cpt = new double[2][(int)Math.pow(2,numparents)];
		switch (type[0]){
		case 0:
			switch (type[1]){
			case 0:
				switch (type[2]){
				case 0: // type 0 0 0
					cpt[0][1] = value[0]*value[1]+(1-value[0])*value[2];
					break;
				default: // type 0 0 *
					cpt[0][1]  = value[0]*value[1];
					cpt[1][1]  = value[0]*value[1]+1-value[0];
				}
				break;
			default :
				switch (type[2]){
				case 0: // type 0 * 0
					cpt[0][1]  = (1-value[0])*value[2];
					cpt[1][1]  = value[0]+(1-value[0])*value[2];
					break;
				default: // type 0 * *
					cpt[0][1]  = 0;
					cpt[1][1]  = 1-value[0];
					cpt[2][1]  = value[0];
					cpt[3][1]  = 1;
				}
				break;
			}
			break;
		default :
			switch (type[1]){
			case 0:
				switch (type[2]){
				case 0: // type * 0 0
					cpt[0][1]  = value[2];
					cpt[1][1]  = value[1];
					break;
				default: // type * 0 *
					cpt[0][1]  = 0;
					cpt[1][1]  = 1;
					cpt[2][1]  = value[1];
					cpt[3][1]  = value[1];
				}
				break;
			default :
				switch (type[2]){
				case 0: // type * * 0
					cpt[0][1]  = value[2];
					cpt[1][1]  = value[2];
					cpt[2][1]  = 0;
					cpt[3][1]  = 1;
					break;
				default: // type * * *
					cpt[0][1]  = 0;
					cpt[1][1]  = 1;
					cpt[2][1]  = 0;
					cpt[3][1]  = 1;
					cpt[4][1]  = 0;
					cpt[5][1]  = 0;
					cpt[6][1]  = 1;
					cpt[7][1]  = 1;
				}
				break;
			}
			break;
		}

		for (int i=0;i<cpt.length;i++)
			cpt[i][0]=1-cpt[i][1];
		newnode.cptentries = cpt;
		numnodes = numnodes + 3; // This is only an upper bound on the
		// actual number of nodes
		substituteNode(node,newnode);

	}

	private void processComponentOfConvComp(int i,
			ComplexBNNode oldnode,
			BNNode newnode,
			ProbForm[] pf,
			int[] type,
			double[] vals,
			BNNode[] pnodes,
			int decomposemode)
	throws RBNCompatibilityException
	{

		switch (i)
		{
		case 0:
			pf[i] = ((ProbFormConvComb)oldnode.cpmodel).f1();
			break;
		case 1:
			pf[i] = ((ProbFormConvComb)oldnode.cpmodel).f2();
			break;
		case 2:
			pf[i] = ((ProbFormConvComb)oldnode.cpmodel).f3();
			break;
		}



		// Simplify convex combination by replacing
		// f2 or f3 factors with constants if f1=0 or =1.
		if (i==1 && type[0]==0 && vals[0]==0)
			pf[i]= new ProbFormConstant(1); // the value 1 doesn't matter
		if (i==2 && type[0]==0 && vals[0]==1)
			pf[i]= new ProbFormConstant(1);

		//               constants             : type 0 (including predefined atoms)
		//               probabilistic atoms   : type 1
		//               others                : type 2
		if (pf[i] instanceof ProbFormConstant)
			type[i] = 0;
		if (pf[i] instanceof ProbFormAtom && ((ProbFormAtom)pf[i]).getRelation().ispredefined())
			type[i] = 0;
		if (pf[i] instanceof ProbFormAtom && ((ProbFormAtom)pf[i]).getRelation().isprobabilistic())
			type[i] = 1;
		if (pf[i] instanceof ProbFormConvComb)
			type[i] = 2;
		if (pf[i] instanceof ProbFormCombFunc)
			type[i] = 2;
		if (pf[i] instanceof ProbFormBool && ! (pf[i] instanceof ProbFormAtom)){
			int val = pf[i].evaluatesTo(strucarg);
			if (val != -1){
				type[i] = 0;
				pf[i] = new ProbFormConstant(val);
			}
			else 
				type[i] = 2;  
		}
		switch (type[i])
		{
		case 0:
			vals[i] = ((ProbFormConstant)pf[i]).cval;
			break;
		case 1:
			// if i>0 and the same ground atom has already occurred as
			// pf[j] (j<i), then treat it as a constant
			if (i==1 &&
					(pf[0] instanceof ProbFormAtom) &&
					((ProbFormAtom)pf[0]).equals((ProbFormAtom)pf[1]))
			{
				type[1] = 0;
				vals[1] = 1;
				break;
			}
			if (i==2 &&
					(pf[0] instanceof ProbFormAtom) &&
					((ProbFormAtom)pf[0]).equals((ProbFormAtom)pf[2]))
			{
				type[2] = 0;
				vals[2] = 0;
				break;
			}
			if (i==2 &&
					(pf[1] instanceof ProbFormAtom) &&
					((ProbFormAtom)pf[1]).equals((ProbFormAtom)pf[2]))
			{
				type[0] = 0;
				type[2] = 0;
				vals[0] = 1;
				vals[2] = 0;
				break;
			}
			// Now the usual case:

			GroundAtom at = new GroundAtom(((ProbFormAtom)pf[i]).getRelation(),
					rbnutilities.stringArrayToIntArray(((ProbFormAtom)pf[i]).getArguments()));
			pnodes[i] = groundatomhasht.get(at.hashCode());
			pnodes[i].children.add(newnode);
			break;
		case 2:
			pnodes[i] = new ComplexBNNode(oldnode.name + ".CC" + i,pf[i]);
			Vector<BNNode> children = new Vector<BNNode>();
			children.add(newnode);
			pnodes[i].children = children;
			complexnodes.add(pnodes[i]);
			++myProgressMax;//keith cascio 20060515
			break;
		default :
			;
		}
	}


	private void processProbFormBoolEquality(ComplexBNNode node, int evidencemode)
	throws RBNCompatibilityException, RBNRuntimeException
	{
	
		// Like processConstNode with truth value
		// of atomic formula instead of cval.
		int typeofnode;
		if (node instanceof ComplexBNGroundAtomNode) typeofnode = 0;
		else typeofnode = 1;
		SimpleBNNode newnode;
		double[][] cpt = new double[1][2];
		int val = ((ProbFormBoolEquality)node.cpmodel()).evaluatesTo(strucarg);
		if (val == -1)
			throw new RBNRuntimeException("Equality with undefined value in BayesConstructor");
		cpt[0][1] = val;
		cpt[0][0]=1-val;
		
		switch (typeofnode)
		{
		case 0:
			newnode = new SimpleBNGroundAtomNode(((ComplexBNGroundAtomNode)node).myatom(),
					((ComplexBNGroundAtomNode)node).name,
					cpt,
					node.parents,
					node.children);
			if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED)
			{
				newnode.instantiate(node.instantiated);
			}
			break;
		case 1:
			newnode = new SimpleBNNode(node.name,cpt,node.parents,node.children);
			break;
		default:
			newnode = new SimpleBNNode();
		}
		// Now substitution in parents lists of children of node
		substituteNode(node,newnode);
	}
	
//	private void processSFormulaNode(ComplexBNNode node, int evidencemode)
//	throws RBNCompatibilityException
//	{
//		// Like processConstNode with truth value
//		// of SFormula instead of cval.
//		int typeofnode;
//		if (node instanceof ComplexBNGroundAtomNode) typeofnode = 0;
//		else typeofnode = 1;
//		SimpleBNNode newnode;
//		double[] cpt = new double[1];
//		cpt[0] = ((ProbFormSFormula)node.probform).evaluateBool(strucarg);
//		switch (typeofnode)
//		{
//		case 0:
//			newnode = new SimpleBNGroundAtomNode(((ComplexBNGroundAtomNode)node).myatom(),
//					((ComplexBNGroundAtomNode)node).name,
//					cpt,
//					node.parents,
//					node.children);
//			if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED)
//			{
//				newnode.instantiate(node.instantiated);
//			}
//			break;
//		case 1:
//			newnode = new SimpleBNNode(node.name,cpt,node.parents,node.children);
//			break;
//		default:
//			newnode = new SimpleBNNode();
//		}
//		// Now substitution in parents lists of children of node
//		substituteNode(node,newnode);
//	}

	private void markUpstream(BNNode bnn, int k)
	// Sets visited[k] to true for all nodes that are reachable
	// by backward chaining from bnn.
	{
		ListIterator li = bnn.parents.listIterator();
		BNNode nextbnn;
		bnn.visited[k] = true;
		while (li.hasNext()){
			nextbnn = (BNNode)li.next();
			if (nextbnn.visited[k] == false)
				markUpstream(nextbnn, k);
		}
	}


	private void processCombFuncNode( ComplexBNNode node, int evidencemode, int decomposemode ){
		CombFunc   cfunc       = ((ProbFormCombFunc)node.cpmodel).getMycomb();
		if( !(cfunc instanceof MultLinCombFunc) ) throw new IllegalArgumentException( "Cannot proceed contructing a bayesian network at node \"" + node.name + "\".\nDecompose mode is on, but the current model is not decomposable because it contains the non-multilinear combination function \"" + cfunc.name + "\".\nPlease set decompose mode to \"none\" (in Primula console menu Options:Construction Mode:Decompose Mode:none)." );

		int typeofnode;
		if (node instanceof ComplexBNGroundAtomNode) typeofnode = 0;
		else typeofnode = 1;
		SimpleBNNode newnode;
		switch (typeofnode)
		{
		case 0:
			newnode = new SimpleBNGroundAtomNode(((ComplexBNGroundAtomNode)node).myatom(),((ComplexBNGroundAtomNode)node).name);
			if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED)
			{
				newnode.instantiate(node.instantiated);
			}
			break;
		case 1:
			newnode = new SimpleBNNode(node.name);
			break;
		default:
			newnode = new SimpleBNNode();
		}

		substituteNode(node,newnode);
		newnode.children       = node.children;

		LinkedList decompnodes = new LinkedList();
		ProbForm nextpf;
		ComplexBNNode newbnnode;
		int ind;


		ProbForm[] argpfs      = ((ProbFormCombFunc)node.cpmodel).getPfargs();
		ProbFormBool    ccon        = ((ProbFormCombFunc)node.cpmodel).getCconstr();
		String[]   qvars       = ((ProbFormCombFunc)node.cpmodel).getQuantvars();
		int[][]    argtuples   = new int[0][0];
		try{
			argtuples = strucarg.allTrue(ccon,qvars);
		}
		catch( RBNCompatibilityException e ){myprimula.showMessage(e.toString());};
		for (int i=0;i<argpfs.length;i++){
			for (int j=0;j<argtuples.length;j++){
				nextpf = argpfs[i].substitute(qvars,argtuples[j]);
				if (nextpf instanceof ProbFormAtom){
					GroundAtom at = new GroundAtom(((ProbFormAtom)nextpf).getRelation(),
							rbnutilities.stringArrayToIntArray(((ProbFormAtom)nextpf).getArguments()));
					BNNode nextdecompnode = (BNNode) groundatomhasht.get(at.hashCode());
					if (nextdecompnode == null)
						System.out.println("Trying to retrieve non-existent node " + at.asString(strucarg) 
								+ " while processing " + node.name + " -- add type constraints to combination function selector?");
					decompnodes.add(nextdecompnode);
				}
				else {
					newbnnode = new ComplexBNNode(node.name + ".pf" + i + "(" + rbnutilities.arrayToString(argtuples[j]) + ")",nextpf);
					numnodes++;
					decompnodes.add(newbnnode);
					complexnodes.add(newbnnode);
					/** keith cascio 20060515 ...
		    	This line, if uncommented,
		    	would result in an incorrect value for myProgressMax.
		    	Why i don't know.
					 */
					++myProgressMax;
					/** ... keith cascio */
				};
			}
		}
		((MultLinCombFunc)cfunc).insertCompNetwork(decompnodes,newnode,decomposemode);
		numnodes = numnodes + 2*decompnodes.size();
		// Upper bound for new nodes introduced by insertCompNetwork
		// when cfund is noisy-or or mean. Has to be revised if
		// combination functions are added that require larger
		// computation networks.

	}

	private void removeReachable(SimpleBNNode bnn,Vector exportnodes,boolean firstcall){
		// removes all nodes reachable from bnn from exportnodes
		bnn.visited[0] = true;
		boolean success;
		if (!firstcall) {
			success = exportnodes.remove(bnn);
		}
		ListIterator li = bnn.parents.listIterator();
		SimpleBNNode nextnode;
		Object object;
		while (li.hasNext()){
			object = li.next();
			nextnode = (SimpleBNNode)object;
			if (!nextnode.visited[0]) removeReachable(nextnode,exportnodes,false);
		}
		li = bnn.children.listIterator();
		while (li.hasNext()){
			nextnode = (SimpleBNNode)li.next();
			if (!nextnode.visited[0]) removeReachable(nextnode,exportnodes,false);
		}
		if (firstcall) bnn.resetVisited(0);
	}

	/** Marginalize out of network(component) given by bnn all
	 *  auxiliary nodes that have at most one parent.
	 *  Returns true if at least one node was eliminated
	 */
	private boolean marginalizeOutRedundant(SimpleBNNode bnn,int decomposemode)
	throws RBNIllegalArgumentException{
		boolean result = false;
		SimpleBNNode currentnode;
		LinkedList childrenlist;
		ListIterator li;
		Vector nodestack = bnn.buildNodeStack();
		for (int i=0; i<nodestack.size(); i++){
			currentnode = (SimpleBNNode)nodestack.elementAt(i);
			if(!(currentnode instanceof SimpleBNGroundAtomNode) && currentnode.parents.size() <=1){
				switch (decomposemode){
				case Primula.OPTION_DECOMPOSE:
					childrenlist = new LinkedList(currentnode.children);
					li = childrenlist.listIterator();
					while (li.hasNext())
						marginalizeOut((SimpleBNNode)li.next(),currentnode);
					result = true;
					break;
//				case Primula.OPTION_DECOMPOSE_DETERMINISTIC:
//					//if (currentnode.parents.size()>0){
//					if (isDeterministic(currentnode)){
//						childrenlist = new LinkedList(currentnode.children);
//						li = childrenlist.listIterator();
//						while (li.hasNext())
//							marginalizeOut((SimpleBNNode)li.next(),currentnode);
//						result = true;
//					}
				}// end switch decomposemode
			}
		}
		return result;
	}

//	/** Merges all equivalent deterministic auxiliary nodes in
//	 * network component given by bnn
//	 * returns true if at least one merge operation has taken place
//	 * 
//	 * Currently disabled for maintainability!
//	 */
//	private boolean mergeEquivalentDetNodes(SimpleBNNode bnn){
//		boolean result = false;
//		SimpleBNNode currentnode;
//		SimpleBNNode child1;
//		SimpleBNNode child2;
//		SimpleBNNode nextgrandchild;
//		SimpleBNNode nextcousin;
//		Vector nodestack = bnn.buildNodeStack();
//		ListIterator li;
//		boolean removed = true;
//		for (int i=0; i<nodestack.size(); i++){
//			currentnode = (SimpleBNNode)nodestack.elementAt(i);
//
//			LinkedList newchildren = new LinkedList(currentnode.children);
//			int j = 0;
//			while (j<currentnode.children.size()-1){
//				child1 = (SimpleBNNode)currentnode.children.get(j);
//				int h = j+1;
//				while (h<currentnode.children.size()){
//					child2 = (SimpleBNNode)currentnode.children.get(h);
//					if (!(child1 instanceof SimpleBNGroundAtomNode) && !(child2 instanceof SimpleBNGroundAtomNode) ){
//						if (child1.isDetEquivalent(child2)){
//							result = true;
//							//System.out.println("merging  " + child1.name + "  and  " + child2.name);
//							currentnode.children.remove(child2);
//							li = child2.children.listIterator();
//							while (li.hasNext()){
//								nextgrandchild = (SimpleBNNode)li.next();
//								if (!nextgrandchild.parents.contains(child1)){
//									nextgrandchild.replaceInParentList(child2,child1);
//									child1.addToChildren(nextgrandchild);
//									child2.children = new LinkedList();// child2 then passed over when considered
//									// as next element in nodestack
//								}
//								else{
//									int child1ind = nextgrandchild.parents.indexOf(child1);
//									int child2ind = nextgrandchild.parents.indexOf(child2);
//
//									double[] newcpt = eliminateDuplicateDependency(nextgrandchild.cptentries,
//											nextgrandchild.parents.size(),
//											child1ind,child2ind);
//									nextgrandchild.parents.remove(child2);
//									child2.children = new LinkedList();
//									nextgrandchild.cptentries = newcpt;
//								}
//							}
//							li = child2.parents.listIterator();
//							while (li.hasNext()){
//								nextcousin = (SimpleBNNode)li.next();
//								if (nextcousin != currentnode){
//									removed=true;
//									while (removed){
//										removed = nextcousin.children.remove(child2);
//									}
//								}
//							}
//						} // end  if (child1.isDetEquivalent(child2))
//					} // end if (!(child1 instanceof SimpleBNGroundAtomNode) && ...
//					h++;
//				}// end while h
//				j++;
//			} // end while j
//			//currentnode.children = newchildren;
//
//		}
//		return result;
//	}

	private boolean isDeterministic(SimpleBNNode node){
		if (!node.isIsboolean()) {
			System.out.println("BayesConstructor.isDeterministic called for non Boolean node");
			return false;
		}
		boolean result = true;
		for (int i=0;i<node.cptentries.length;i++){
			if (node.cptentries[i][0]!=0.0 &&  node.cptentries[i][0]!=1.0) result = false;
		}
		return result;
	}


	private void marginalizeOut(SimpleBNNode child, SimpleBNNode parent)
	throws RBNIllegalArgumentException
	// parent is a node with at most one parent. child is a node with
	// parent among its parents. Dependency of
	// child on parent is eliminated. child becomes dependent on (single)
	// parent of parent (if exists)
	{
		if (!child.isIsboolean() || !parent.isIsboolean())
			throw new RBNIllegalArgumentException("Non-boolean nodes in marginalizeOut!");
		int cptsize = child.cptentries.length;
		int numparents = child.parents.size();
		int parentpos = child.parents.indexOf(parent);
		int grandparentpos;
		int corrindex;
		double p1,p2;
		SimpleBNNode grandparent;
		if (parent.parents.size()==0){
			p1 = parent.cptentries[0][0]; // marginal probability of parent being true
			child.parents.remove(parent);
			parent.children.remove(child);
			double[][] newcpt = new double[2][cptsize/2];
			int newcptpos = 0;
			for (int i=0; i<cptsize; i++){
				if (rbnutilities.indexToTuple(i,numparents,2)[parentpos]==0){
					corrindex=rbnutilities.computeCorrespondingIndex(numparents,parentpos,i);
					newcpt[newcptpos][0]=(1-p1)*child.cptentries[i][0]+p1*child.cptentries[corrindex][0];
					newcptpos++;
				}
			}
			child.cptentries = newcpt;
		}
		if (parent.parents.size()==1){
			grandparent = (SimpleBNNode)parent.parents.get(0);
			if (!(child.parents.contains(grandparent))){
				// grandparent --> parent --> child
				// CPTs:
				// grandparent | parent = true
				// ---------------------------
				//     false   | p1
				//     true    | p2
				//
				// other parents parent  | child = true
				// ------------------------------------
				//      x       false    |    q1
				//      x       true     |    q2
				//         ...           |    ...
				// becomes
				//
				// other parents  grandparent | child = true
				// -----------------------------------------
				//       x          false     |    q1(1-p1)+q2p1
				//       x          true      |    q1(1-p2)+q2p2
				//            ...             |     ...

				p1=parent.cptentries[0][1];
				p2=parent.cptentries[1][1];
				child.parents.remove(parent);
				parent.children.remove(child);

				child.parents.add(parentpos,grandparent);
				grandparent.children.remove(parent);
				parent.parents.remove(grandparent);
				grandparent.children.add(child);
				double[][] newcpt = new double[2][cptsize];
				for (int i=0; i<cptsize; i++){
					if (rbnutilities.indexToTuple(i,numparents,2)[parentpos]==0){
						corrindex=rbnutilities.computeCorrespondingIndex(numparents,parentpos,i);
						newcpt[i][1]=child.cptentries[i][1]*(1-p1)+child.cptentries[corrindex][1]*p1;
					}
					if (rbnutilities.indexToTuple(i,numparents,2)[parentpos]==1){
						corrindex=rbnutilities.computeCorrespondingIndex(numparents,parentpos,i);
						newcpt[i][1]=child.cptentries[corrindex][1]*(1-p2)+child.cptentries[i][1]*p2;
					}
				}
				child.cptentries = newcpt;
			}
			else {
				// grandparent --> parent --> child
				//       |                     ^
				//       ----------------------|
				// CPTs:
				// grandparent | parent = true
				// ---------------------------
				//     false   | p1
				//     true    | p2
				//
				// other parents grandparent parent  | child = true
				// ------------------------------------------------
				//      x         false      false   |    q1
				//      x         false      true    |    q2
				//      x         true       false   |    q3
				//      x         true       true    |    q4
				//         ...                       |    ...
				// becomes
				//
				// other parents  grandparent | child = true
				// -----------------------------------------
				//       x          false     |    q1(1-p1)+q2p1
				//       x          true      |    q3(1-p2)+q4p2
				//            ...             |     ...

				grandparentpos = child.parents.indexOf(grandparent);
				p1=parent.cptentries[0][1];
				p2=parent.cptentries[1][1];
				child.parents.remove(parent);
				parent.children.remove(child);
				grandparent.children.remove(parent);
				parent.parents.remove(grandparent);

				double[][] newcpt = new double[2][cptsize/2];
				int newcptpos = 0;
				for (int i=0; i<cptsize; i++){
					if (rbnutilities.indexToTuple(i,numparents,2)[parentpos]==0){
						corrindex=rbnutilities.computeCorrespondingIndex(numparents,parentpos,i);
						if (rbnutilities.indexToTuple(i,numparents,2)[grandparentpos]==0)
							newcpt[newcptpos][1]=(1-p1)*child.cptentries[i][1]+p1*child.cptentries[corrindex][1];
						else
							newcpt[newcptpos][1]=(1-p2)*child.cptentries[i][1]+p2*child.cptentries[corrindex][1];
						newcptpos++;
					}
				}
				child.cptentries = newcpt;
			} // end else

		}
	}


	private double[] eliminateDuplicateDependency(double[] oldcpt, int numpars, int ind1, int ind2){
		// Eliminates a dependency of a cpt on parent with index ind2
		// given that parent with index ind1 is equivalent, i.e. with
		// probability 1 ind1 and ind2 always have the same value.
		int[] inst;
		int nextentry = 0;
		double[] result = new double[oldcpt.length/2];
		for (int i=0;i<oldcpt.length;i++){
			inst = rbnutilities.indexToTuple(i,numpars,2);
			if (inst[ind1]==inst[ind2]){
				result[nextentry]=oldcpt[i];
				nextentry++;
			}
		}
		return result;
	}

	/* somenodes is a vector of BNNodes with parent and children vectors
	 * set. somenodes need not contain all nodes connected to the nodes
	 * in somenodes.
	 *
	 * Sets the depth value for each node connected to a node in somenodes, where depth
	 * is the length of the longest path from some root.
	 *
	 * Includes acyclicity check
	 *
	 * Returns an array containing the maximal depths of  nodes in
	 * the various connected components (sorted according to the first
	 * appearance of a representative of a connected component in somenodes)
	 */
	private int[] setDepths(Vector somenodes)
	throws RBNCyclicException
	{
		int numconncomp=0;
		BNNode nextbnn;
		BNNode topnode;
		Vector nodestack;
		int[] maxdepths = new int[somenodes.size()];
		/* first initialize the depths of nodes in somenodes to -1 */
		for (int i=0;i<somenodes.size();i++)
			((BNNode)somenodes.elementAt(i)).setDepth(-1);
		for (int i=0;i<somenodes.size();i++){
			nextbnn = (BNNode)somenodes.elementAt(i);
			if (nextbnn.depth()==-1){
				numconncomp++;
				nodestack = new Vector();
				nextbnn.resetVisited(-1);
				nodestack.add(nextbnn);
				while (!nodestack.isEmpty()){
					topnode = (BNNode)nodestack.elementAt(nodestack.size()-1);
					nodestack.removeElementAt(nodestack.size()-1);
					maxdepths[numconncomp-1]=Math.max(maxdepths[numconncomp-1],setDepth(topnode, nodestack));
				}
				nextbnn.resetVisited(-1);
			}
		}
		int[] result = new int[numconncomp];
		for (int i = 0;i<numconncomp;i++)
			result[i]=maxdepths[i];
		return result;
	}

	private int setDepth(BNNode bnn, Vector nodestack)
	throws RBNCyclicException
	{
		// determines depth of bnn by recursively determining depth
		// of all parents of bnn. During upstream recursion all
		// children of nodes are put on nodestack.
		// visited[0] becomes true for a node when procedure
		// setDepth(bnn,nodestack) is called.
		// visited[1] is set to true when node is put on the
		// nodestack.
		// Returns depth assigned to argument bnn
		if (bnn.visited[0] && !bnn.depthset) throw new RBNCyclicException("Network is cyclic!");
		// 		System.out.print("setDepth for " + ((PFNetworkNode)bnn).myatom().asString(strucarg) + " " );
		bnn.visited[0] = true;
		ListIterator li;
		if (!bnn.depthset){
			if (bnn.parents.size()==0) bnn.setDepth(0);
			else{
				BNNode nextpar;
				li = bnn.parents.listIterator();
				while (li.hasNext())
				{
					nextpar = (BNNode)li.next();
					setDepth(nextpar,nodestack);
					if (nextpar.depth() >= bnn.depth())
						bnn.setDepth(nextpar.depth() + 1);
				}
			}
			bnn.depthset = true;
			li = bnn.children.listIterator();
			BNNode nextchild;
			while (li.hasNext()){
				nextchild = (BNNode)li.next();
				if (!nextchild.visited[1]){
					nodestack.add(nextchild);
					nextchild.visited[1] = true;
				}
			}
		}
		// 		System.out.println(bnn.depth());
		return bnn.depth();
	}

	private void setHeight(SimpleBNNode bnn, Vector nodestack)
	throws RBNCyclicException
	{
		if (!bnn.visited[0]){
			bnn.visited[0] = true;
			bnn.visited[1] = true;
			ListIterator li;
			if (bnn.children.size()==0) bnn.height =0;
			else{
				SimpleBNNode nextchil;
				li = bnn.children.listIterator();
				while (li.hasNext())
				{
					nextchil = (SimpleBNNode)li.next();
					setDepth(nextchil,nodestack);
					bnn.height = Math.max(bnn.height,nextchil.height+1);
					// experimental:
					//bnn.level = bnn.height;
				}
			}
			li = bnn.parents.listIterator();
			SimpleBNNode nextpar;
			while (li.hasNext()){
				nextpar = (SimpleBNNode)li.next();
				if (!nextpar.visited[1]){
					nodestack.add(nextpar);
					nextpar.visited[1] = true;
				}
			}
		}
	}

	private void setLevel(SimpleBNNode bnn,int maxlevel){
		// Assign all nodes in the connected component
		// of bnn to a level.



		SimpleBNNode topnode;
		int pardepth;
		int childheight;
		int topnodelevel;
		ListIterator li;
		// First put all nodes on the nodestack
		Vector nodestack = bnn.buildNodeStack();
		// Initialize levels to depth
		for (int i=0; i<nodestack.size(); i++)
			((SimpleBNNode)nodestack.elementAt(i)).level =  ((SimpleBNNode)nodestack.elementAt(i)).depth();
		while (!nodestack.isEmpty()){
			topnode = (SimpleBNNode)nodestack.elementAt(nodestack.size()-1);
			nodestack.removeElementAt(nodestack.size()-1);
			pardepth = 0;
			li = topnode.parents.listIterator();
			while (li.hasNext()){
				pardepth = Math.max(pardepth,((SimpleBNNode)li.next()).level);
			}
			childheight = maxlevel;
			li = topnode.children.listIterator();
			while (li.hasNext()){
				childheight = Math.min(childheight,((SimpleBNNode)li.next()).level);
			}
			topnodelevel = (pardepth+childheight)/2;
			if(topnodelevel > topnode.level){
				topnode.level = topnodelevel;
				li = topnode.parents.listIterator();
				while (li.hasNext())
					nodestack.add((SimpleBNNode)li.next());
				li = topnode.children.listIterator();
				while (li.hasNext())
					nodestack.add((SimpleBNNode)li.next());
			}

		}
	}

	/** @author keith cascio
	@since 20060515 */
	public static class WeightComparator implements Comparator{
		private WeightComparator(){}

		public int compare( Object o1, Object o2 ){
			double difference = ((BNNode)o2).weight - ((BNNode)o1).weight;
			if( difference > 0 ) return -1;
			else if( difference == 0 ) return 0;
			else return 1;
		}

		public static final WeightComparator INSTANCE = new WeightComparator();
	}

	private void balanceLevels( SimpleBNNode bnn, int maxlevel ) throws InterruptedException {
		//System.out.println( "BayesConstructor.balanceLevels( "+maxlevel+" )" );

		if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();

		int maxlevelPlusOne = maxlevel + 1;
		// First collect nodes of each level in a vector
		ArrayList[] levels = new ArrayList[ maxlevelPlusOne ];
		for( int i=0; i<levels.length; i++ ) levels[i] = new ArrayList();

		addToLevel(bnn,levels);
		bnn.resetVisitedUpDownstream(0);
		// Insert dummy nodes
		ListIterator li;
		BNNode currentnode;
		BNNode currentchild;
		DummyBNNode currentdummy;
		DummyBNNode nextdummy;
		Vector<BNNode> newchildren;
		int dist;
		int pos;
		for( int i=0; i<levels.length; i++ ){
			for (int j=0;j<levels[i].size();j++){
				currentnode = (BNNode)levels[i].get(j);//elementAt(j);
				if (currentnode instanceof SimpleBNNode){
					li = currentnode.children.listIterator();
					newchildren = new Vector<BNNode>();
					while (li.hasNext()){
						currentchild = (BNNode)li.next();
						dist = currentchild.level-currentnode.level;
						if (dist > 1){
							//currentnode.children.remove(currentchild);
							currentdummy = new DummyBNNode(currentnode.level + 1);
							newchildren.add(currentdummy);
							currentdummy.parents.add(currentnode);
							levels[currentdummy.level].add(currentdummy);
							for (int h =0;h<dist-2;h++){
								nextdummy = new DummyBNNode(currentnode.level+2+h);
								currentdummy.children.add(nextdummy);
								nextdummy.parents.add(currentdummy);
								levels[nextdummy.level].add(nextdummy);
								currentdummy = nextdummy;
							}
							currentdummy.children.add(currentchild);
							pos = currentchild.parents.indexOf(currentnode);
							currentchild.parents.remove(pos);
							currentchild.parents.add(pos,currentdummy);
						}
						else newchildren.add(currentchild);
					}
					currentnode.children = newchildren;
				}
			}
			++myProgress;//keith cascio 20060515
		}
		if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();

		// Initialize posnumbers and weights
		for( int i=0; i<levels.length; i++ ){
			setPosNumber(   levels[i] );
		}
		if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();
		for( int i=1; i<levels.length; i++ ){
			computeWeights( levels[i] );
		}

		// Now iterate the weighting and sorting...
//	boolean terminate = false;
//	boolean levelsorted;
//	int count = 0;
//	while (!terminate && count<5){
//	    if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();
//	    ////System.out.println("Iteration: "+ count);
//	    terminate = true;
//	    for (int i=0;i<levels.length;i++){
//		computeWeights(levels[i]);
//		//levelsorted = !sortOnWeights(levels[i],0,levels[i].size()-1);
//		Collections.sort( levels[i], WeightComparator.INSTANCE );
//		levelsorted = true;
//		terminate = terminate && levelsorted;
//		setPosNumber(levels[i]);
//		++myProgress;//keith cascio 20060515
//	    }
//	    count++;
//	    if( !terminate ){
//	    	//System.out.println( "sorting loop cycling around " + count );
//	    	myProgressMax += levels.length;
//	    }
//	}////System.out.println("Iterations in balance levels: " + count);

		ArrayList currentLevel;
		Boolean changed = true;
		int itcount = 0;
		while (changed && itcount<100){
			for( int i=0; i < levels.length; i++ ){
				if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();
				computeWeights(   currentLevel = levels[i] );
//				for (ListIterator it = currentLevel.listIterator(); it.hasNext();){
//					System.out.print(((BNNode)it.next()).name + " ");
//				}
//				System.out.println();
				Collections.sort( currentLevel, WeightComparator.INSTANCE );
				changed = setPosNumber(     currentLevel );
				itcount++;
				++myProgress;//keith cascio 20060515
			}
		}
		
		// Remove the dummies
		// Compute for each level the "levelwidth" given
		// by posnumber*weight of the last element
		// compute the maximal levelwidth
		double[] levelwidth = new double[levels.length];
		double maxlevelwidth = 0;
		BNNode parent,child;
		for( int i=0; i<levels.length; i++ ){
			for( ListIterator it = (currentLevel = levels[i]).listIterator(); it.hasNext(); ){
				currentnode = (BNNode) it.next();

				if (currentnode instanceof DummyBNNode){
					parent = (BNNode)currentnode.parents.getFirst();
					child = (BNNode)currentnode.children.getFirst();
					parent.children.remove(currentnode);
					pos = child.parents.indexOf(currentnode);
					child.parents.remove(pos);
					parent.children.add(child);
					child.parents.add(pos,parent);
					it.remove();
				}
				else {
					//ind++;
					levelwidth[i]=currentnode.posnumber * currentnode.weight;
				}
			}
			maxlevelwidth = Math.max(maxlevelwidth,levelwidth[i]);
			++myProgress;//keith cascio 20060515
		}

		if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();

		// Assign the xccords
		// Initial assignment:
		double doublexcoord;
		for( int i=0; i<levels.length; i++ ){
			for( Iterator it = (currentLevel = levels[i]).iterator(); it.hasNext(); ){
				currentnode = (SimpleBNNode) it.next();//levels[i].elementAt(j);
//				doublexcoord = (maxlevelwidth-levelwidth[i])/2+(currentnode.weight + currentnode.posnumber);
				doublexcoord = 0.2*((maxlevelwidth-levelwidth[i])/2+(currentnode.weight * currentnode.posnumber));
				currentnode.xcoord = (int)(100*doublexcoord);
			}
			++myProgress;//keith cascio 20060515
		}

		
//		// Balance nodes without changing their order:
//		boolean  unbalanced  = true;
//		boolean  nodeshifted = false;
//		Iterator iterator    = null;
//		BNNode   nodePrevious, nodeCurrent, nodeNext;
//
//		// DEBUG VARIABLES ...
//		int      count        = 0;
//		/*
//	boolean  flagShifted  = false;
//	int      total        = 0;
//	int      countShifted = 0;
//	int      delta        = 0;
//	int      deltaAbs     = 0;
//	int      accumulated  = 0;
//	Map<BNNode,int[]> mapShifts = new HashMap<BNNode,int[]>( 4096 );
//	for( int i=0; i<levels.length; i++ ){
//	    for( Iterator it = levels[i].iterator(); it.hasNext(); ){
//		mapShifts.put( (BNNode) it.next(), new int[512] );
//	    }
//	}*/
//		
//		// ... DEBUG VARIABLES
//
//		for( count = 0; unbalanced && (count < 150); count++ )
//		{
//			//showNetwork(levels);
//			nodeshifted  = false;
//			//total        = 0;
//			//countShifted = 0;
//			//accumulated  = 0;
//
//			for( int i=0; i<levels.length; i++ ){
//				if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();
//
//				if( levels[i].isEmpty() ) continue;
//
//				iterator     = levels[i].iterator();
//				nodePrevious = null;
//				nodeCurrent  = null;
//				nodeNext     = (BNNode) iterator.next();
//
//				//total++;
//
//				while( iterator.hasNext() ){
//					//total++;
//					nodePrevious = nodeCurrent;
//					nodeCurrent  = nodeNext;
//					nodeNext     = (BNNode) iterator.next();
//
//
//					//delta = balanceDelta( nodePrevious, nodeCurrent, nodeNext, mapShifts, count );
//					//accumulated += (deltaAbs = Math.abs( delta ));
//					//nodeshifted |= (flagShifted = (deltaAbs > 0));
//					//if( flagShifted ) countShifted++;
//					nodeshifted |= (balanceDelta( nodePrevious, nodeCurrent, nodeNext, null, 0 ) > 0);
//				}
//				if( nodeNext != null ){
//					//delta = balanceDelta( nodeCurrent, nodeNext, null, mapShifts, count );
//					//accumulated += (deltaAbs = Math.abs( delta ));
//					//nodeshifted |= (flagShifted = (deltaAbs > 0));
//					//if( flagShifted ) countShifted++;
//					nodeshifted |= (balanceDelta( nodeCurrent, nodeNext, (BNNode)null, null, 0 ) > 0);
//				}
//
//				++myProgress;//keith cascio 20060515
//			}
//
//			//System.out.println( "iteration " + count + ", " + countShifted + "/" + total + " shifted by " + accumulated );
//
//			//unbalanced = !nodeshifted;
//			unbalanced = nodeshifted;
//		}
//		//System.out.println( "ultimate balance loop iterated " + count + " times" );
//		//detectThrash( mapShifts );
//		 
		
	}




	/** @author keith cascio
    	@since 20060516 */
	/*private void detectThrash( Map<BNNode,int[]> mapShifts ){
	int[] shiftHistory;
	int currentDelta;
	int delta;
	boolean currentDirection;
	boolean direction;
	int countThrashes = 0;
	int twists = 0;
	int[] arrayTwists = new int[4];
    	for( BNNode node : mapShifts.keySet() ){
	    shiftHistory     = mapShifts.get( node );
	    currentDirection = ((currentDelta = shiftHistory[0]) >= 0);
	    twists = 0;
	    for( int i = 1; i<shiftHistory.length; i++ ){
	        direction = ((delta = shiftHistory[i]) >= 0);
	        if( (currentDelta != 0) && (delta != 0) && (currentDirection != direction) ) arrayTwists[twists++] = i;
	        currentDelta = delta;
	        currentDirection = direction;
	        if( twists > 3 ){
		    ++countThrashes;
		    //System.out.println( "thrash "+countThrashes+" @ " + Arrays.toString( arrayTwists ) );
		    break;
	        }
	    }
    	}
    }*/

	/** @author keith cascio
    	@since 20060516 */
	/*private boolean balance( BNNode nodePrevious, BNNode currentnode, BNNode nodeNext )
    {
    	boolean nodeshifted = false;
	int     center      = (int) familyXcenter( currentnode );
	int     newxcoord   =  0;

	if( center > currentnode.xcoord ){
	    if( nodeNext == null ) newxcoord = center;
	    else newxcoord  = Math.min( center, nodeNext.xcoord-1 );
	}
	if( center < currentnode.xcoord ){
	    if( nodePrevious == null ) newxcoord = center;
	    else newxcoord  = Math.max( center, nodePrevious.xcoord+1 );
	}
	if( newxcoord != currentnode.xcoord ){
	    currentnode.xcoord = newxcoord;
	    nodeshifted = true;
	}
	return nodeshifted;
    }*/

	/** @author keith cascio
    	@since 20060516 */
	private int balanceDelta( BNNode nodePrevious, BNNode currentnode, BNNode nodeNext, Map<BNNode,int[]> mapShifts, int count )
	{
		int     delta = 0;
		int     center      = (int) currentnode.familyXcenter();
		int     newxcoord   =  0;

		if( center > currentnode.xcoord ){
			if( nodeNext == null ) newxcoord = center;
			else newxcoord  = Math.min( center, nodeNext.xcoord-1 );
		}
		if( center < currentnode.xcoord ){
			if( nodePrevious == null ) newxcoord = center;
			else newxcoord  = Math.max( center, nodePrevious.xcoord+1 );
		}
		//delta = Math.abs( newxcoord - currentnode.xcoord );
		delta = newxcoord - currentnode.xcoord;

		/*if( (delta != 0) && (count > 0) ){
	    int lastDelta = mapShifts.get( currentnode )[count-1];
	    if( lastDelta != 0 ){
		boolean lastDirection = lastDelta >= 0;
		boolean direction     =     delta >= 0;
		if( direction != lastDirection ){
		    //newxcoord = (newxcoord - currentnode.xcoord) / 2;
		    //delta = newxcoord - currentnode.xcoord;
		    newxcoord = currentnode.xcoord;
		    delta = 0;
		}
	    }
	}

	mapShifts.get( currentnode )[count] = delta;*/
		currentnode.xcoord = newxcoord;

		return delta;
	}

	/*private void showNetwork(Vector[] levels){
	// for debugging only
	for (int i=0;i<levels.length;i++){
	    System.out.println("Level " + i + " Size " + levels[i].size());
	    for (int j=0;j<levels[i].size();j++){
		System.out.println(((BNNode)levels[i].elementAt(j)).xcoord);
	    }
	}
    }*/

	/*private void showNodes(SimpleBNNode bnn)
	// for debugging only
    {
	Vector nodestack = bnn.buildNodeStack();
	SimpleBNNode topnode;
	ListIterator li;
	for (int i=0; i<nodestack.size(); i++){
	    topnode=(SimpleBNNode)nodestack.elementAt(i);
	    System.out.println(topnode.name);
	    System.out.println("  parents: ");
	    li = topnode.parents.listIterator();
	    while (li.hasNext()) System.out.println("      " + ((SimpleBNNode)li.next()).name);
	    System.out.println("  children: ");
	    li = topnode.children.listIterator();
	    while (li.hasNext()) System.out.println("      " + ((SimpleBNNode)li.next()).name);
	    //System.out.print("  visited: ");
	    //for (int j=0;j<5;j++) System.out.print(topnode.visited[j]);
	    System.out.println();
	}
    }*/

	/*private void displayWeights(Vector vec){
	for (int j=0;j<vec.size();j++){
	    System.out.print(((BNNode)vec.elementAt(j)).weight + "  ");
	}
	System.out.println();
    }*/

	/** Subroutine of balanceLevels */
	private void addToLevel(SimpleBNNode bnn,ArrayList[] levels){
		if (!bnn.visited[0]){
			bnn.visited[0]=true;
			levels[bnn.level].add(bnn);
			ListIterator li = bnn.children.listIterator();
			while (li.hasNext()){
				addToLevel((SimpleBNNode)li.next(),levels);
			}
			li = bnn.parents.listIterator();
			while (li.hasNext()){
				addToLevel((SimpleBNNode)li.next(),levels);
			}
		}
	}

	/** Subroutine of balanceLevels 
	 *  Returns true if one posnumber has changed */
	private Boolean setPosNumber( ArrayList vec ){
		int size = vec.size();
		Boolean change = false;
		for( int j=0; j<size; j++ ){
			BNNode nextbnn = (BNNode)vec.get(j);
			if (nextbnn.posnumber != j)
				change = true;
			nextbnn.posnumber = j;
		}
		return change;
	}

	/** Subroutine of balanceLevels */
	private void computeWeights( ArrayList vec ){
		int famsize;
		BNNode currentnode;
		ListIterator li;
		int size = vec.size();
		for( int j=0; j<size; j++ ){
			currentnode = (BNNode)vec.get(j);//elementAt(j);
			currentnode.weight=0.0;
			li = currentnode.parents.listIterator();
			while (li.hasNext()){
				currentnode.weight = currentnode.weight + ((BNNode)li.next()).posnumber;
			}
			li = currentnode.children.listIterator();
			while (li.hasNext()){
				currentnode.weight = currentnode.weight + ((BNNode)li.next()).posnumber;
			}
			famsize=currentnode.parents.size()+currentnode.children.size();
			if (famsize>0){
				currentnode.weight=currentnode.weight/famsize;}
			else currentnode.weight=0;
		}
	}

	/** Subroutine of balanceLevels */
	/*private boolean sortOnWeights(Vector vec, int low, int high){
    	System.out.println( "    sortOnWeights( "+low+"..."+high+" )" );
	//System.out.print("sort for  "  );
	//displayWeights(vec);
	//System.out.println("["+low + "," + high + "]");
	// vec: a vector of BNNode
	// method sorts the elements with indices between low and high
	// according to the
	// weight fields of the BNNodes.
	// Returns true if input wasn't already sorted
	double pivot;
	int currhigh=high;
	int currlow=low;
	BNNode highnode,lownode;
	int pivotind;
	boolean result = false;
	boolean subres1,subres2;

	if (currhigh > currlow){
	    pivotind=(high+low)/2;
	    pivot = ((BNNode)vec.elementAt(pivotind)).weight;
	    while(currhigh > currlow){
		//System.out.println("entering with currhigh: " + currhigh + " currlow: "+ currlow);
		while (((BNNode)vec.elementAt(currhigh)).weight > pivot && currhigh > low) currhigh--;
		while (((BNNode)vec.elementAt(currlow)).weight < pivot && currlow < high) currlow++;
		if (currhigh > currlow){
		    highnode = ((BNNode)vec.elementAt(currhigh));
		    lownode = ((BNNode)vec.elementAt(currlow));
		    vec.remove(currlow);
		    vec.add(currlow,highnode);
		    vec.remove(currhigh);
		    vec.add(currhigh,lownode);
		    currlow++;
		    currhigh--;
		    if (highnode.weight != lownode.weight) result = true;
		}
	    }
	    if (currhigh == currlow) {
		subres1 = sortOnWeights(vec,low,currlow);
		subres2 = sortOnWeights(vec,currhigh+1,high);
		result = (result || subres1 || subres2 );
	    }
	    if (currhigh == currlow -1){
		subres1 = sortOnWeights(vec,low,currhigh);
		subres2 = sortOnWeights(vec,currlow,high);
		result = (result || subres1 || subres2);
	    }

	}
	return result;
    }*/

	private int computeWidth(SimpleBNNode bnn){
		// Computes width of connected component of node bnn
		int[] minmax = new int[2];
		minmax[0] = bnn.xcoord;
		minmax[1] = bnn.xcoord;
		bnn.visited[0] = true;
		ListIterator li = bnn.parents.listIterator();
		SimpleBNNode nextnode;
		while (li.hasNext()){
			nextnode = (SimpleBNNode)li.next();
			if (!nextnode.visited[0]) computeWidth(nextnode,minmax);}

		li = bnn.children.listIterator();
		while (li.hasNext()){
			nextnode = (SimpleBNNode)li.next();
			if (!nextnode.visited[0]) computeWidth(nextnode,minmax);
		}
		bnn.resetVisitedUpDownstream(0);
		int width = minmax[1]-minmax[0];
		//System.out.println("**** Computed width  " + width);
		return minmax[1]-minmax[0]+NODEWIDTH;
	}

	private void computeWidth(SimpleBNNode bnn,int[] minmax){

		minmax[0] = Math.min(minmax[0],bnn.xcoord);
		minmax[1] = Math.max(minmax[1],bnn.xcoord);
		bnn.visited[0] = true;
		ListIterator li = bnn.parents.listIterator();
		SimpleBNNode nextnode;
		while (li.hasNext()){
			nextnode = (SimpleBNNode)li.next();
			if (!nextnode.visited[0]) computeWidth(nextnode,minmax);
		}
		li = bnn.children.listIterator();
		while (li.hasNext()) {
			nextnode = (SimpleBNNode)li.next();
			if (!nextnode.visited[0]) computeWidth(nextnode,minmax);
		}
	}

	// 	private boolean isolatedZeroNode(SimpleBNNode node){
	// 	    boolean result = true;
	// 	    if (node.parents.size() != 0) result = false;
	// 	    if (node.children.size() != 0) result = false;
	// 	    if (node.cptentries[0]!=0) result = false;
	// 	    return result;
	// 	}

	private void exportBNNode(SimpleBNNode node,int evidencemode, int xoffset){
		Vector nodestack = new Vector();
		SimpleBNNode topnode;
		nodestack.add(node);
		while (!nodestack.isEmpty()){
			topnode = (SimpleBNNode)nodestack.elementAt(nodestack.size()-1);
			nodestack.removeElementAt(nodestack.size()-1);
			exportBNNode(topnode,evidencemode,xoffset,nodestack);
		}
	}

	private void exportBNNode(SimpleBNNode node,int evidencemode, int xoffset, Vector nodestack)
	throws RuntimeException
	{

		SimpleBNNode nextexport;
		if (!node.exported){
			node.exported = true;
			ListIterator li = node.parents.listIterator();
			while (li.hasNext())
			{
				nextexport = (SimpleBNNode)li.next();
				exportBNNode(nextexport,evidencemode,xoffset,nodestack);
			}
			if (evidencemode == Primula.OPTION_EVIDENCE_CONDITIONED && node instanceof SimpleBNGroundAtomNode)
			{
				int tval = ((SimpleBNGroundAtomNode)node).instantiated;
				bni.addNode(node,xoffset,tval);
			}
			else bni.addNode(node,xoffset,-1);
			li = node.children.listIterator();
			while (li.hasNext())
			{
				nextexport = (SimpleBNNode)li.next();
				if (!nextexport.exported) nodestack.add(nextexport);
			}
		}
	}

	/** Utility method for debugging **/
	private void showGAHT(){
		BNNode nextbnn;
		for (Enumeration<BNNode> e=groundatomhasht.elements();e.hasMoreElements();){
			nextbnn = e.nextElement();
			System.out.println(((GroundAtomNodeInt)nextbnn).myatom().asString());
		}
		System.out.println();
	}
	
	private Boolean emptyIntersect(TreeSet<Rel> tsr1, TreeSet<Rel> tsr2) {
		Boolean result = true;
		
		for (Iterator<Rel> it= tsr1.iterator();it.hasNext();) {
			Rel r = it.next();
			if (tsr2.contains(r))
				result = false;
		}
		
		return result;
	}

	

}
