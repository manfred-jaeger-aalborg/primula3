/*
* Sampler.java 
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

//import java.util.*;
//import java.io.*;
import RBNpackage.*;
import RBNgui.*;
import RBNExceptions.*;
import RBNutilities.*;
import RBNinference.*;
import RBNio.*;
import mymath.*;
import myio.*;
import java.util.*;

public class Sampler{

	private Primula myPrimula;
	private PFNetwork pfnetw;

	public Sampler(){
		myPrimula = null;
		pfnetw = null;
	}

	
	
	
	public void makeSampleStruc(Primula pr){
		myPrimula = pr;
		BayesConstructor bcons = new BayesConstructor(myPrimula, new OneStrucData(), new GroundAtomList());
		try{
			pfnetw = bcons.constructPFNetwork(Primula.OPTION_NOT_EVIDENCE_CONDITIONED,
					Primula.OPTION_NOT_QUERY_SPECIFIC,
					Primula.OPTION_NOT_ELIMINATE_ISOLATED_ZERO_NODES);
			pfnetw.prepareForSampling(InferenceModule.OPTION_SAMPLEORD_FORWARD,
					InferenceModule.OPTION_NOT_SAMPLE_ADAPTIVE,
					new boolean[5],
					3, 
					new GroundAtomList(), 
					1,
					1,
					null);
		}
		catch(RBNCompatibilityException e){System.out.println(e);}
		catch(RBNIllegalArgumentException e){System.out.println(e);}
		catch(RBNCyclicException e){System.out.println(e);}
		catch(RBNInconsistentEvidenceException e){System.out.println(e);}
		catch(java.io.IOException e){System.out.println(e);};
	}


	/** Samples OneStrucData with a percentage of pcmissing values missing (completely at random) */

	public OneStrucData sampleOneStrucData(double pcmissing){

		OneStrucData result = new OneStrucData();
		double rand;

		try{
			if (pfnetw==null)
				throw new java.lang.NullPointerException("Initialize sampling structure before sampling!");

			pfnetw.sampleInst(0,false);

			for (int i = 0; i<pfnetw.allnodesSize(); i++){
	            /* add 		(BoolRel)pfnetw.relAt(i) to result if not already there */
				rand = Math.random();
				if (100*rand >= pcmissing){
					if (pfnetw.sampleValAt(i)==1){
						result.add((BoolRel)pfnetw.relAt(i), pfnetw.atomAt(i).args(),true,"?");
					}
					else
						result.add((BoolRel)pfnetw.relAt(i), pfnetw.atomAt(i).args(),false,"?");
				}
			}
		}
		catch(RBNCompatibilityException e){System.out.println(e);}
		catch(RBNInconsistentEvidenceException e){System.out.println(e);}

		return result;
	}
	/** Constructs a random graph (given by binary Relation 'edge') over
	 * 'size' nodes. Edges are independent with probability 'edgeprob'.
	 * When acyclic = true then an acyclic graph is produced.
	 * 
	 * Graph consists of compnum connected components. The nodes in component k
	 * are in the Relation comp_k.
	 * 
	 * Unary Relations 'node' containing all nodes, and 'root' containing all
	 * nodes without edge-predecessors are also created.
	 * 
	 */ 
	public SparseRelStruc makeRandomGraph(int size, 
			double edgeprob, boolean acyclic, int compnum){
		SparseRelStruc result = new SparseRelStruc(size*compnum);
		BoolRel nodeRel = new BoolRel("node",1);
		result.addRelation(nodeRel);
		BoolRel edgeRel = new BoolRel("edge",2);
		result.addRelation(edgeRel);
		BoolRel rootRel = new BoolRel("root",1);
		result.addRelation(rootRel);

		double coin;
		boolean isroot;
		for (int k=0; k< compnum; k++){
			BoolRel compRel = new BoolRel("comp_" + k,1);
			result.addRelation(compRel);
			for (int i=k*size;i<(k+1)*size;i++){
				int tup[] = {i};
				result.addTuple(compRel,tup);
				isroot = true;
				if (!acyclic){
					for (int j=k*size; j<(k+1)*size; j++){
						coin = Math.random();
						if (coin<edgeprob){
							int edgetup[]={j,i};
							result.addTuple(edgeRel, edgetup);
							isroot = false;
						}
					}
				}
				else
					for (int j=k*size;j<i;j++){
						coin = Math.random();
						if (coin<edgeprob){
							int edgetup[]={j,i};
							result.addTuple(edgeRel, edgetup);
							isroot = false;
						}
					}

				result.addTuple(nodeRel, tup);
				if (isroot){
					result.addTuple(rootRel, tup);
				}
			}
		}
		return result;	
	}

	/** Constructs a random graph (given by binary Relation 'edge') over
	 * 'size' nodes. Edges are independent with probability 'edgeprob'.
	 * Nodes are colored red,blue,green with probabilities redprob,blueprob,(1-redprob-blueprob)
	 */ 
	public SparseRelStruc makeRandomColoredGraph(int size, double edgeprob,double redprob, double blueprob){
		SparseRelStruc result = new SparseRelStruc(size);
		BoolRel edgeRel = new BoolRel("edge",2);
		BoolRel red = new BoolRel("red",1);
		BoolRel blue = new BoolRel("blue",1);
		BoolRel green = new BoolRel("green",1);
		result.addRelation(edgeRel);
		result.addRelation(red);
		result.addRelation(blue);
		result.addRelation(green);

		double coin;
		for (int i=0;i<size;i++){
			coin=Math.random();
			int tupcol[]={i};
			if (coin < redprob)
				result.addTuple(red,tupcol);
			else{
				if (coin < redprob+blueprob)
					result.addTuple(blue,tupcol);
				else
					result.addTuple(green,tupcol);
			}
			for (int j=0;j<size;j++){
				coin = Math.random();
				if (coin<edgeprob){
					int tup[]={i,j};
					result.addTuple(edgeRel, tup);
				}
			}
		}
		return result;	
	}


	/** Constructs a domain of size 'size' with objects of types 'types'.
	 * typeprob[i] is the probability that an object will be assigned type types[i].
	 * The elements of typeprobs should sum up to 1.
	 */ 
	public SparseRelStruc makeRandomTypedDomain(int size, String[] types, double[] typeprobs){
		SparseRelStruc result = new SparseRelStruc(size);
		BoolRel[] typeRels = new BoolRel[types.length];
		for (int i=0;i<types.length;i++){
			typeRels[i] = new BoolRel(types[i],1);
			result.addRelation(typeRels[i]);
		}

		double coin;

		for (int i=0;i<size;i++){
			boolean assigned = false;
			coin=Math.random();
			int tupcol[]={i};
			int typeindex = -1;
			double probsum = 0;
			while (!assigned){
				typeindex++;
				probsum = probsum + typeprobs[typeindex];
				if (coin <= probsum){
					result.addTuple(typeRels[typeindex],tupcol);
					assigned = true;
				}
			}
		}
		return result;	
	}

	/** Constructs a random pedigree-like structure: nodes have either two or zero
	 * parents
	 */
	public SparseRelStruc makeRandomPedigree(int size, double founderprop){
		SparseRelStruc result = new SparseRelStruc(size);
		BoolRel fathRel = new BoolRel("father",2);
		BoolRel mothRel = new BoolRel("mother",2);
		BoolRel foundRel = new BoolRel("founder",1);
		result.addRelation(fathRel);
		result.addRelation(mothRel);
		result.addRelation(foundRel);

		double coin;
		int firstpar;
		int secondpar;

		for (int i=0;i<2;i++){
			int tup[] = {i};
			result.addTuple(foundRel,tup);
		}
		for (int i=2;i<size;i++){
			coin = Math.random();
			if (coin<founderprop){ /* i is a new founder */ 
				int tup[] = {i};
				result.addTuple(foundRel,tup);
			}
			else{
				firstpar = (int)Math.floor(Math.random()*i);
				secondpar = firstpar;
				while (secondpar == firstpar){
					secondpar = (int)Math.floor(Math.random()*i);
				}
				int tupfp[] = {firstpar,i};
				int tupsp[] = {secondpar,i};
				result.addTuple(mothRel,tupfp);
				result.addTuple(fathRel,tupsp);
			}
		}

		return result;
	}

	/** 
	 * Constructs a random structure for investigating slotchain like
	 * dependencies: domain consists of 'numtypes' different types of objects;
	 * 'numoftype' objects of each type. Types are type_1,...,type_numtypes.
	 *  Between objects of type_i and type_i+1 there are 'numRels' different 
	 *  Relations 'Rel_i_k' k=0,...,numRels-1.
	 *  Each object of type i has exactly 'numsuccs' successors of type
	 *  i+1 in each Rel_i_k
	 *  
	 *  There are numcomp disjoint connected components of this structure
	 *  
	 * @param size
	 * @param numsuccs
	 * @param numtypes
	 * @return
	 */
	public SparseRelStruc makeSlotchainBase(int numoftype, int numRels, 
			int numsuccs, int numtypes, int numcomp){
		SparseRelStruc result = new SparseRelStruc(numoftype*numtypes*numcomp);
		BoolRel[] typeRels = new BoolRel[numtypes];
		for (int i=0;i<numtypes;i++){
			typeRels[i]=new BoolRel("type_" + i , 1 );
			result.addRelation(typeRels[i]);
		}

		BoolRel[] compRels = new BoolRel[numcomp];
		for (int k=0;k<numcomp;k++){
			compRels[k]= new BoolRel("comp_" + k, 1);
			result.addRelation(compRels[k]);
		}

		BoolRel[][] linkRels = new BoolRel[numtypes][numRels];
		for (int i=0;i<numtypes-1;i++){
			TypeRel[] types = new TypeRel[2];
			types[0] = new TypeRel(typeRels[i]);
			types[1] = new TypeRel(typeRels[i+1]);
			for (int k=0;k<numRels;k++){
				linkRels[i][k]=new BoolRel("Rel_" + i + "_" + k, 2 , types );
				result.addRelation(linkRels[i][k]);
			}
		}
		for (int k=0;k<numcomp;k++){
			int offset = k*numoftype*numtypes;

			for (int i=0;i<numtypes;i++){
				for (int h = offset; h<offset+numoftype; h++ ){
					int tup[] = {i*numoftype + h};
					result.addTuple(compRels[k],tup);
					result.addTuple(typeRels[i],tup);    				
				}
			}

			for (int i=0;i<numtypes-1;i++){
				for (int o=0;o<numoftype;o++){
					for (int r=0;r<numRels;r++){
						int[] succs = randomGenerators.multRandInt((i+1)*numoftype, 
								(i+2)*numoftype-1, numsuccs);
						for (int s=0;s<numsuccs;s++){
							int tup[] = {offset+o+i*numoftype,offset+succs[s]};
							result.addTuple(linkRels[i][r], tup );
						}
					}
				}
			}
		}


		return result;
	}

	/** 
	 * Similar to makeSlotchainBase, only number of successors is not constant numsucc, but
	 * sampled from Poisson distribution
	 * @param poissonparams the i'th component contains the parameter for the Poissondistribution
	 *        for the number of successors of type i+1 for objects of type i. Length of 
	 *        poissonparams must be numtypes-1;
	 * @param numtypes
	 * @return
	 */
	public SparseRelStruc makeStratifiedGraph(int numoftype, int numRels, 
			double[] poissonparams, int numtypes){
		SparseRelStruc result = new SparseRelStruc(numoftype*numtypes);
		BoolRel[] typeRels = new BoolRel[numtypes];
		for (int i=0;i<numtypes;i++){
			typeRels[i]=new BoolRel("type_" + i , 1 );
			result.addRelation(typeRels[i]);
		}

		BoolRel[][] linkRels = new BoolRel[numtypes][numRels];
		for (int i=0;i<numtypes-1;i++){
			TypeRel[] types = new TypeRel[2];
			types[0] = new TypeRel(typeRels[i]);
			types[1] = new TypeRel(typeRels[i+1]);
			for (int k=0;k<numRels;k++){
				linkRels[i][k]=new BoolRel("Rel_" + i + "_" + k, 2 , types );
				result.addRelation(linkRels[i][k]);
			}
		}

		for (int i=0;i<numtypes;i++){
			for (int h = 0; h<numoftype; h++ ){
				int tup[] = {i*numoftype + h};
				result.addTuple(typeRels[i],tup);    				
			}
		}

		for (int i=0;i<numtypes-1;i++){
			for (int o=0;o<numoftype;o++){
				for (int r=0;r<numRels;r++){
					int[] succs = randomGenerators.multRandInt((i+1)*numoftype, 
							(i+2)*numoftype-1, MyRandom.randomPoisson(poissonparams[i]));
					for (int s=0;s<succs.length;s++){
						int tup[] = {o+i*numoftype,succs[s]};
						result.addTuple(linkRels[i][r], tup );
					}
				}
			}
		}

		return result;
	}

	/** 
	 * Similar to makeSlotchainBase, only number of successors from type_i to type_i+1 is not constant numsucc, but
	 * randomly determined from the set numsuccs[i] according to the probabilities numsuccprobs[i]
	 * @param numtypes
	 * @return
	 */
	public SparseRelStruc makeStratifiedGraph(int numoftype, int numRels, 
			int[][] numsuccs, double[][] numsuccprobs, int numtypes){
		SparseRelStruc result = new SparseRelStruc(numoftype*numtypes);
		BoolRel[] typeRels = new BoolRel[numtypes];
		for (int i=0;i<numtypes;i++){
			typeRels[i]=new BoolRel("type_" + i , 1 );
			result.addRelation(typeRels[i]);
		}

		BoolRel[][] linkRels = new BoolRel[numtypes][numRels];
		for (int i=0;i<numtypes-1;i++){
			TypeRel[] types = new TypeRel[2];
			types[0] = new TypeRel(typeRels[i]);
			types[1] = new TypeRel(typeRels[i+1]);
			for (int k=0;k<numRels;k++){
				linkRels[i][k]=new BoolRel("Rel_" + i + "_" + k, 2 , types );
				result.addRelation(linkRels[i][k]);
			}
		}

		for (int i=0;i<numtypes;i++){
			for (int h = 0; h<numoftype; h++ ){
				int tup[] = {i*numoftype + h};
				result.addTuple(typeRels[i],tup);    				
			}
		}

		for (int i=0;i<numtypes-1;i++){
			for (int o=0;o<numoftype;o++){
				for (int r=0;r<numRels;r++){
					int[] succs = randomGenerators.multRandInt((i+1)*numoftype, 
							(i+2)*numoftype-1, numsuccs[i][MyRandom.randomMultinomial(numsuccprobs[i])]);
					for (int s=0;s<succs.length;s++){
						int tup[] = {o+i*numoftype,succs[s]};
						result.addTuple(linkRels[i][r], tup );
					}
				}
			}
		}

		return result;
	}

	public SparseRelStruc makeBagOfBagOfInstance(int numTopBagsTrain, int numTopBagsTest,  
				int numSubBags, int maxcomponents, int numInstances, int numFeatures){
		
		int mininstancepersb = 2;
		int maxinstancepersb = maxcomponents;
		int minsbpertb = 2;
		int maxsbpertb = maxcomponents;
		
		SparseRelStruc result = new SparseRelStruc();
		
		BoolRel tbRel = new BoolRel("TopBag",1);
		result.addRelation(tbRel);
		BoolRel sbRel = new BoolRel("SubBag",1);
		result.addRelation(sbRel);
		BoolRel instRel = new BoolRel("Instance",1);
		result.addRelation(instRel);
		TypeRel tbType = new TypeRel(tbRel);
		TypeRel sbType = new TypeRel(sbRel);
		TypeRel instType = new TypeRel(instRel);
		
		Type[] tbtypes = new Type[2];
		tbtypes[0] = tbType;
		tbtypes[1] = sbType;
		BoolRel tbrRel = new BoolRel("tb",2,tbtypes);
		result.addRelation(tbrRel);
		
		Type[] bbtypes = new Type[2];
		bbtypes[0] = sbType;
		bbtypes[1] = instType;
		BoolRel bbrRel = new BoolRel("bb",2,bbtypes);
		result.addRelation(bbrRel);
		
		Type[] posTBtypes = new Type[1];
		posTBtypes[0] = tbType;
		BoolRel posRel = new BoolRel("positiveTB",1,posTBtypes);
		result.addRelation(posRel,"?");
		
		Type[] posSBtypes = new Type[1];
		posSBtypes[0] = sbType;
		BoolRel posSBRel = new BoolRel("positiveSB",1,posSBtypes);
		result.addRelation(posSBRel);
	
		Type[] posInsttypes = new Type[1];
		posInsttypes[0] = instType;
		BoolRel posInstRel = new BoolRel("positiveInst",1,posInsttypes);
		result.addRelation(posInstRel);
	
		
		Type[] feattypes = new Type[1];
		feattypes[0] = instType;
		NumRel[] featRels = new NumRel[numFeatures];
		for (int i=0;i<numFeatures;i++){
			featRels[i]=new NumRel("f_" + i,1,feattypes);
			result.addRelation(featRels[i]);
		}
		
		String[] argasarr = new String[1];
		
		for (int tbi =0 ; tbi < numTopBagsTrain; tbi++){
			String nexttbname = "tb_train" + tbi;
			result.addNode(nexttbname);	
			argasarr[0]=nexttbname;
			result.addTuple(tbRel,argasarr,true);
		}
		for (int tbi =0 ; tbi < numTopBagsTest; tbi++){
			String nexttbname = "tb_test" + tbi;
			result.addNode(nexttbname);	
			argasarr[0]=nexttbname;
			result.addTuple(tbRel,argasarr,true);
		}
		for (int sbi =0 ; sbi < numSubBags; sbi++){
			String nexttbname = "sb_" + sbi;
			result.addNode(nexttbname);	
			argasarr[0]=nexttbname;
			result.addTuple(sbRel,argasarr,true);
		}
		
		// Sample the features and define class labels for the instances:
		double marginfac = 2;
		Vector<Integer> positiveInstanceInd = new Vector<Integer>();
		Vector<Integer> negativeInstanceInd = new Vector<Integer>();
		System.out.println("Threshold: " + marginfac*numFeatures/4);
		
		for (int ii =0 ; ii < numInstances; ii++){
			boolean thispositive = false;
			String nexttbname = "inst_" + ii;
			String argstrings[]={nexttbname};
			result.addNode(nexttbname);		
			double[] featval = new double[numFeatures];
			double multfac=marginfac*Math.random();
			double sum = 0;
			for (int f =0;f < numFeatures;f++){
				featval[f] = multfac*Math.random();
				sum = sum+featval[f];
			}
			System.out.print("Sum: " + sum);
			if (sum > marginfac*numFeatures/4){
				thispositive = true;
				System.out.println(" +");
			}
			else
				System.out.println(" -");
			for (int f =0;f < numFeatures;f++){
				String featrel = "f_" + f;		
				result.addTuple(result.getNumRel(featrel),argstrings,featval[f]);
			}
			if (thispositive){
				result.addTuple(posInstRel,argstrings,true);
				positiveInstanceInd.add(ii);
			}
			else
				negativeInstanceInd.add(ii);
		}
		
		// Now the bb relation:
		
		Vector<Integer> positiveSBInd = new Vector<Integer>();
		Vector<Integer> negativeSBInd = new Vector<Integer>();
		String[] argstrings = new String[2];
		String[] argsbstring = new String[1];
		for (int i=0;i<numSubBags;i++){ 
			argstrings[0]="sb_"+i;
			argsbstring[0]="sb_"+i;
			
			int instancesforsb = mininstancepersb + MyRandom.randomInteger(maxinstancepersb-mininstancepersb);
			// Selection probability for a positive instance so that probability that at least one 
			// neg. inst. is selected is 0.5
			double selneginstprob = 1-Math.pow(0.5,1.0/instancesforsb);
			boolean thissbpos = true;
			int selinstindx;
			for (int j=0;j<instancesforsb;j++){
				if (Math.random()>selneginstprob){
					selinstindx = positiveInstanceInd.elementAt(MyRandom.randomInteger(positiveInstanceInd.size()-1));
					argstrings[1]="inst_" + selinstindx;
					result.addTuple(result.getBoolRel("bb"),argstrings,true);	
				}
				else{
					selinstindx = negativeInstanceInd.elementAt(MyRandom.randomInteger(negativeInstanceInd.size()-1));
					argstrings[1]="inst_" + selinstindx;
					result.addTuple(result.getBoolRel("bb"),argstrings,true);
					thissbpos = false;
				}
			}
			if (thissbpos){
				positiveSBInd.add(i);
				result.addTuple(posSBRel,argsbstring,true);
			}
			else
				negativeSBInd.add(i);
		}

		// Now the tb relation:

		for (int i=0;i<numTopBagsTrain+numTopBagsTest;i++){ 
			if (i<numTopBagsTrain){
				argstrings[0]="tb_train"+i;
				argsbstring[0]="tb_train"+i;
			}
			else{
				argstrings[0]="tb_test"+(i-numTopBagsTrain);
				argsbstring[0]="tb_test"+(i-numTopBagsTrain);
			}

			int sbfortb = minsbpertb + MyRandom.randomInteger(maxsbpertb-minsbpertb);
			// Selection probability for a positive instance so that probability that at least one 
			// pos. inst. is selected is 0.5
			double selpossbprob = 1-Math.pow(0.5,1.0/sbfortb);
			boolean thistbpos = false;
			int selsbindx;
			for (int j=0;j<sbfortb;j++){
				if (Math.random()<selpossbprob){
					selsbindx = positiveSBInd.elementAt(MyRandom.randomInteger(positiveSBInd.size()-1));
					argstrings[1]="sb_" + selsbindx;
					result.addTuple(result.getBoolRel("tb"),argstrings,true);
					thistbpos = true;
				}
				else{
					selsbindx = negativeSBInd.elementAt(MyRandom.randomInteger(negativeSBInd.size()-1));
					argstrings[1]="sb_" + selsbindx;
					result.addTuple(result.getBoolRel("tb"),argstrings,true);
					
				}
			}
			if (thistbpos){
				result.addTuple(posRel,argsbstring,true);
			}
			else
				result.addTuple(posRel,argsbstring,false);
		}

		return result;	
	}
	/** Build a structure representing authors, papers and citations
	 * 
	 * numauthors: number of authors
	 * posauthors: probability of an author being a positive example (high h number)
	 * 
	 * @param numauthors 
	 * @return
	 */
//	public SparseRelStruc makeCitationGraph(int numauthors,
//			double posauthors,
//			double meanpapers,
//			double meancites)
//	throws RBNCompatibilityException,RBNSyntaxException
//	{
//		SparseRelStruc result = new SparseRelStruc(numauthors);
//		Rel authorRel = new Rel("Author",1);
//		result.addRelation(authorRel);
//		TypeRel authortype  = new TypeRel(authorRel);
//		Rel paperRel = new Rel("Paper",1);
//		result.addRelation(paperRel);
//		TypeRel papertype = new TypeRel(paperRel);
//		TypeRel tRelargs1[] = {authortype};
//		Rel posauthor = new Rel("Positive",1,tRelargs1);
//		result.addRelation(posauthor);
//		TypeRel tRelargs2[] = {authortype,papertype};
//		Rel author2paper = new Rel("Author2Paper",2,tRelargs2);
//		result.addRelation(author2paper);
//		TypeRel tRelargs3[] = {papertype,papertype};
//		Rel cites = new Rel("Cites",2,tRelargs3);
//		double rand;
//		
//		/* Randomly assigning authors to be positive */
//		for (int i=0;i<numauthors;i++){
//			int addtuple[] = {i};
//			result.addTuple(authorRel,addtuple);
//			rand = Math.random();
//			if (rand < posauthors){
//				result.addTuple(posauthor,addtuple);
//			}
//		}
//		/* For each author generate papers according to Poisson
//		 * distribution with mean meanpapers
//		 */
//		int numpapers;
//		for (int i=0;i<numauthors;i++){
//			numpapers = MyRandom.randomPoisson(meanpapers);
//			for (int h=0;h<numpapers;h++){
//				result.addNode();
//				int addtuple1[] = {result.domSize()-1};
//				result.addTuple(paperRel,addtuple1);
//				/* result.domSize()-1 is the index of a new object */
//				int addtuple2[] = {i, result.domSize()-1};
//				result.addTuple(author2paper,addtuple2);
//			}
//		}
//		
//		RBNReaderOLD cconstrparser = new RBNReaderOLD();
//		
//
//		/* Create citations of papers. Number of citing papers: Poisson
//		 * with mean meancites*numpapers(author). Distribution over papers by one author:
//		 * uniform for 
//		 */
//		
//		String queryargs1[] = {"x"};
//		int[][] papersofauthor;
//		int totalcitesforauthor;
//		
//		for (int i=0;i<numauthors;i++){
//			/* Get the papers of nextauth */
//			CConstr query = cconstrparser.stringToCConstr("Author2Paper("+Integer.toString(i)+",x)");
//			papersofauthor = result.allTrue(query,queryargs1);
//			totalcitesforauthor = MyRandom.randomPoisson(papersofauthor.length * meancites);
//			int intargs[] = {i};
//			int ispositive = result.truthValueOf(posauthor,intargs);
//			for (int h=0;h<totalcitesforauthor;h++){
//				/* Generate a new citing paper, and make it reference a random
//				 * paper of author i 
//				 */
//				result.addNode();
//				int addtuple3[] = {result.domSize()-1};
//				result.addTuple(paperRel,addtuple3);
//				/* Determine the index within papersofauthor of the paper that this
//				 * paper cites
//				 */
//				int citethis=0;
//				switch (ispositive){
//				case 0: citethis = 0;
//				break;
//				case 1: citethis = MyRandom.randomInteger(papersofauthor.length-1);
//				break;
//				case -1: 
//					System.out.println("Undefined truth value!");
//				}
//				/* Create the citation link */
//				int indexofcitedpaper = papersofauthor[citethis][0];
//				int addtuple4[] = {result.domSize()-1,indexofcitedpaper};
//				result.addTuple(cites,addtuple4);
//			}
//
//		}
//		
//		
//
//		return result;
//	}
}
