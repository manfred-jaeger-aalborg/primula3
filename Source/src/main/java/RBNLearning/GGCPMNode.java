/*
 * GGProbFormNode.java 
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

import RBNpackage.*;
import RBNgui.*;
import RBNExceptions.*;
import RBNutilities.*;

public abstract class GGCPMNode extends GGNode{

	TreeSet<GGCPMNode> parents;
	TreeSet<GGNode> ancestors; // Includes the likelihood node


	//String probformasstring;

	//	/* Upper and lower bounds on the value of this node given
	//	 * a current partial evaluation.
	//	 * Set to [-1,-1] if these bounds have not been evaluated
	//	 * for the current setting at the indicator nodes
	//	 */
	//	double[] bounds;

	/* Set to true if this ProbFormNode is an Upper Ground Atom node (i.e., direct child of the
	 * likelihood node.
	 */
	private boolean isuga;


	/* The atom for which this is the upper ground atom node;
	 * Empty string, if this is not an upper ground atom node.
	 */
	private String myatom;


	/* If this is an upper ground atom node: instval represents
	 * the current instantiation value of this atom:
	 * if this node repesents an atom which is instantiated to true (false) in the data, then
	 * instval is a fixed Integer. If this node
	 * represents an atom which is not instantiated in the data, then 
	 * instval is the indicator node for this atom
	 * 
	 * If this is not an uga node, then instval=null;
	 */
	private Object instval;

	/* If this GGProbFormNode is the Upper Ground Atom Node of 
	 * an un-instantiated atom, then myindicator is the corresponding 
	 * indicator node. Otherwise set to null.
	 */
	private GGAtomNode myindicator;

	/* If this is an Upper Ground Atom Node: the set of all IndicatorMaxNodes on whose value 
	 * this node depends. Otherwise null.
	 */
	private Vector<GGAtomMaxNode> mymaxindicators;

	/* If this is an Upper Ground Atom Node: the set of all IndicatorSumNodes on whose value 
	 * this node depends. Otherwise null.
	 */
	private Vector<GGAtomSumNode> mysumindicators;



	public GGCPMNode(GradientGraphO gg,
			CPModel cpm,
			RelStruc A,
			OneStrucData I)
					throws RBNCompatibilityException
	{
		super(gg);
		//probformasstring = pf.asString(Primula.CLASSICSYNTAX,0,null);

		//		formula = pf;
		////		truthval = tv;
		//		bounds = new double[2];
		//		bounds[0]=-1;
		//		bounds[1]=-1;
		parents = new TreeSet<GGCPMNode>();
		ancestors = null;
		myindicator = null;
		myatom ="";
		instval = null;
		mymaxindicators = new Vector<GGAtomMaxNode>();
		mysumindicators = new Vector<GGAtomSumNode>();	
		isuga = false;
		outDim = cpm.numvals();
		//		dependsOnParam = new boolean[gg.numberOfParameters()];
		//		for (int i=0; i< dependsOnParam.length; i++)
		//			dependsOnParam[i]=false;
	}

	//	/** dependsOnParam[i] is true if the probform of this node depends on 
	//	 * the i'th parameter, as given by the order defined by gg
	//	 */
	//	protected boolean[] dependsOnParam;

	public static GGCPMNode constructGGPFN(GradientGraphO gg,
			CPModel cpm, 
			Hashtable<String,GGCPMNode> allnodes, 
			RelStruc A, 
			OneStrucData I,
			int inputcaseno,
			int observcaseno,
			Hashtable<String,Integer> parameters,
			boolean useCurrentPvals,
			boolean isuga,
			String uganame,
			Hashtable<Rel,GroundAtomList> mapatoms,
			Hashtable<String,Object[]>  evaluated )
					throws RuntimeException,RBNCompatibilityException
	{

		//System.out.println("construct ggpfn for " + pf.asString(1, 0, A, false, false));

		/* If this is not the construction of an upper ground atom node (which 
		 * has to be inserted, no matter whether an equivalent node already exists), 
		/* first try to find the GGProbFormNode in allnodes: */

		GGCPMNode ggn = null;

		/* Perform expansion for macro calls. 
		 * 
		 */
		if (cpm instanceof ProbFormMacroCall) {
			cpm = ((ProbFormMacroCall)cpm).pform().substitute(((ProbFormMacroCall)cpm).macro().arguments(), ((ProbFormMacroCall)cpm).args());
		}

		/* Must transform ProbFormBoolAtom with negative sign so that it does not
		 * get identified with an existing AtomNode (loosing the sign info)
		 */
		if ((cpm instanceof ProbFormBoolAtom) && ((ProbFormBoolAtom)cpm).sign()==false){
			((ProbFormBoolAtom)cpm).toggleSign();
			cpm = new ProbFormConvComb((ProbForm)cpm,new ProbFormConstant(0), new ProbFormConstant(1));
		}

		if (!isuga) {
			ggn = gg.findInAllnodes(cpm, inputcaseno, observcaseno, A);
		}
		if (ggn != null){
			return ggn;
		}
		else{

			GGCPMNode result = null;
			if (cpm instanceof ProbFormConstant)
				if (isuga){
					// if this is an upper ground atom node, cannot just construct a constant node that would
					// then not be identified as an rbn parameter, because it receives an uga key.
					// Therefore, turn the pf into auxiliary conv. comb. formula (1:pf,0) which will then be 
					// handled by subsequent case.
					cpm = new ProbFormConvComb(new ProbFormConstant(1.0),(ProbForm)cpm,new ProbFormConstant(0.0));
				}
				else {
					result =  new GGConstantNode(gg,(ProbForm)cpm,A,I);
					String pname = ((ProbFormConstant)cpm).getParamName();
					if (pname !="") {
						((GGConstantNode)result).setCurrentParamVal(gg.myPrimula.getRBN().getParameterValue(pname));
					}
				}
			if ( (cpm instanceof ProbFormAtom && ((ProbFormAtom)cpm).getRelation().isprobabilistic() )
					|| 
					(cpm instanceof ProbFormBoolAtom) && ((ProbFormBoolAtom)cpm).getRelation().isprobabilistic())
			{

				if (gg.mapatoms != null && 
						gg.mapatoms(((ProbFormAtom)cpm).getRelation()) != null && 
						gg.mapatoms(((ProbFormAtom)cpm).getRelation()).contains(((ProbFormAtom)cpm).atom()))
					result =  new GGAtomMaxNode(gg,(ProbForm)cpm,A,I,inputcaseno,observcaseno);
				else
					result =  new GGAtomSumNode(gg,(ProbForm)cpm,A,I,inputcaseno,observcaseno);
			}
			if ((cpm instanceof ProbFormAtom && ((ProbFormAtom)cpm).getRelation().ispredefined() )
					|| 
					(cpm instanceof ProbFormBoolAtom) && ((ProbFormBoolAtom)cpm).getRelation().ispredefined()){
				ProbFormConstant pfconst = new ProbFormConstant(cpm.asString(Primula.CLASSICSYNTAX,0,A,false,false));
				ggn = gg.findInAllnodes(pfconst, inputcaseno, observcaseno, A);
				if (ggn != null){
					return ggn;
				}
				else{
					double pfvalue =0 ;
					if (cpm instanceof ProbFormAtom){
						pfvalue = A.valueOf( ((ProbFormAtom)cpm).getRelation(), 
								rbnutilities.stringArrayToIntArray(((ProbFormAtom)cpm).getArguments()));
					}
					if (cpm instanceof ProbFormBoolAtom){
						pfvalue = A.truthValueOf( ((ProbFormAtom)cpm).getRelation(), 
								rbnutilities.stringArrayToIntArray(((ProbFormAtom)cpm).getArguments()));
						if (!((ProbFormBool)cpm).sign())
							pfvalue = Math.abs(1-pfvalue);
					}
					cpm=pfconst;
					result = new GGConstantNode(gg,(ProbForm)cpm,A,I);
					((GGConstantNode)result).setCurrentParamVal(pfvalue);;
				}

			}
			if (cpm instanceof ProbFormConvComb)
				result =  new GGConvCombNode(gg,(ProbForm)cpm,allnodes,A,I,inputcaseno,observcaseno,parameters,
						useCurrentPvals,mapatoms,evaluated);
			if (cpm instanceof CatModelSoftMax)
				result =  new GGSoftMaxNode(gg,(CatModelSoftMax)cpm,allnodes,A,I,inputcaseno,observcaseno,parameters,
						useCurrentPvals,mapatoms,evaluated);
			if (cpm instanceof ProbFormCombFunc)
				result =  new GGCombFuncNode(gg,(ProbForm)cpm,allnodes,A,I,inputcaseno,observcaseno,parameters,
						useCurrentPvals,mapatoms,evaluated);
			if (cpm instanceof ProbFormBoolComposite){
				ProbForm pfstandard = ((ProbFormBoolComposite) cpm).toStandardPF(false);
				if (pfstandard instanceof ProbFormCombFunc)
					result =  new GGCombFuncNode(gg,pfstandard,allnodes,A,I,inputcaseno,observcaseno,parameters,
							useCurrentPvals,mapatoms,evaluated);
				if (pfstandard instanceof ProbFormConvComb)
					result =  new GGConvCombNode(gg,pfstandard,allnodes,A,I,inputcaseno,observcaseno,parameters,
							useCurrentPvals,mapatoms,evaluated);
			}
			if (cpm instanceof ProbFormBoolConstant){
				result = new GGConstantNode(gg,(ProbForm)cpm,A,I);
				((GGConstantNode)result).setCurrentParamVal(((ProbFormBoolConstant)cpm).value());
			}
			if (cpm instanceof ProbFormBoolEquality){
				result = new GGConstantNode(gg,(ProbForm)cpm,A,I);
				((GGConstantNode)result).setCurrentParamVal(((ProbFormBoolEquality)cpm).evaluate(A,I));
			}
			if (cpm instanceof ProbFormBoolAtomEquality) {
				result = new GGAtomEqualityNode(gg,(ProbForm)cpm,allnodes,A,I,inputcaseno,observcaseno,parameters,
						useCurrentPvals,mapatoms,evaluated);

			}

			if (cpm instanceof ProbFormGnn || cpm instanceof CatGnn || cpm instanceof CatGnnHetero ) {
				result = new GGGnnNode(gg,cpm,allnodes,A,I,inputcaseno,observcaseno,parameters,useCurrentPvals,mapatoms,evaluated);
			}
//			if (cpm instanceof CatGnn) {
//				result = new GGCatGnnNode(gg,cpm,A,I);
//			}
			String key = gg.makeKey(cpm, inputcaseno, observcaseno, A);

			if (isuga) 
				key = uganame + "_" + key;
			allnodes.put(key,result);

			return result;
		}

	}


	//	public double lowerBound(){
	//		return bounds[0];
	//	}
	//
	//	public double upperBound(){
	//		return bounds[1];
	//	}
	//
	//	public void resetBounds(){
	//		bounds[0]=-1;
	//		bounds[1]=-1;
	//	}


	/** The name of this node. The name identifies the function represented
	 * by a node. 
	 */
	//	public String name(){
	//		if (!isuga)
	//			return probformasstring;
	//		else
	//			return "uga_" + myatom +":" + probformasstring;
	//	}



	/*
	 * See GGNode.evaluate for the use of sno
	 * 
	 * Gradient returned as TreeMap keyed by the parameter names for which the gradient is != 0
	 * 
	 * Dimension of array value for a given parameter is equal to this.outDim()
	 * 
	 *  
	 */
	public abstract TreeMap<String,double[]> evaluateGradient(Integer sno) throws RBNNaNException;

	public void setMyindicator(GGAtomNode mind){
		myindicator = mind;
	}

	//	public boolean dependsOn(int param){
	//		return dependsOnParam[param];
	//	}
	//
	//	public void setDependsOn(int param){
	//		dependsOnParam[param] = true;
	//	}
	//	



	public void setIsuga(boolean tv){
		isuga = tv;
	}

	public void setMyatom(String atm){
		myatom = atm;
	}

	public void setInstval(Object iv){
		instval = iv;
	}

	public Object getInstval(){
		return instval;
	}

	public int instval(Integer sno){
		if (instval == null){
			if (!isuga)
				System.out.println("Trying to call instval() for node that is not upper ground atom node");
			else
				System.out.println("instval() called while instval field is null");
		}
		int result;
		if (instval instanceof GGAtomNode)
			result = (int)((GGAtomNode)instval).evaluate(sno)[0];
		else 
			result = (Integer)instval;
		return result;
	}

	public void setInstvalToIndicator(){
		instval = myindicator;
	}

	public String getMyatom(){
		return myatom;
	}

	public boolean isuga(){
		return isuga;
	}

	public void addToMaxIndicators(GGAtomMaxNode addthis){
		mymaxindicators.add(addthis);
	}

	public void addToSumIndicators(GGAtomSumNode addthis){
		mysumindicators.add(addthis);
	}

	public Vector<GGAtomMaxNode> getMaxIndicators(){
		return mymaxindicators;
	}

	public void printMyMaxIndicators(){
		GGAtomMaxNode nextggmax = null;
		for (Iterator<GGAtomMaxNode> it = mymaxindicators.iterator(); it.hasNext();){
			nextggmax = it.next();
			System.out.print(nextggmax.myatom().asString()+"    ");
		}
		System.out.println();
	}
	public void printMySumIndicators(){
		GGAtomSumNode nextggsum = null;
		for (Iterator<GGAtomSumNode> it = mysumindicators.iterator(); it.hasNext();){
			nextggsum = it.next();
			System.out.print(nextggsum.myatom().asString()+"    ");
		}
		System.out.println();
	}

	public void printMyIndicators(){
		if (myindicator != null)
			System.out.println("My Indicator: " + myindicator.myatom().asString());
		System.out.print("Max: ");
		printMyMaxIndicators();
		System.out.print("Sum: ");
		printMySumIndicators();
	}


	//	public void set_value_for_sample(int sno) {
	//		values_for_samples[sno]=value;
	//	}
	//	


	public abstract boolean isBoolean();
	public TreeSet<GGCPMNode> parents(){
		return parents;
	}

	public void addToParents(GGCPMNode ggn){
		parents.add(ggn);
	}
	
	/** Returns the set of all ancestors of this node
	 * in the Graph
	 * @return
	 */
	public TreeSet<GGNode> ancestors(){
		TreeSet<GGNode> result = new TreeSet<GGNode>();
		for (GGCPMNode nextggn:parents){
			result.add((GGCPMNode)nextggn);
			nextggn.collectAncestors(result);
		}
		result.add(thisgg.llnode);
		return result;
	}

	public void setAncestors(){
		ancestors = ancestors();
	}

	public void deleteAncestors(){
		ancestors = null;
	}

	private void collectAncestors(TreeSet<GGNode> ancests){
		for (GGCPMNode nextggn: parents){
			if (!ancests.contains(nextggn)){
				ancests.add(nextggn);
				nextggn.collectAncestors(ancests);
			}
		}
	}

	public void resetUpstream(Integer sno) {
		if (ancestors == null) 
			ancestors = ancestors();

		for (GGNode anc: ancestors)
			anc.resetValue(sno);
	}

	/** Re-evaluates all ancestor nodes of this node. 
	 * Used to propagate value changes when the value of this
	 * node has been changed. Mostly applied when this is 
	 * a GGAtomNode, and the value of this
	 * indicator has been changed in Gibbs sampling or MAP inference
	 */
	public void reEvaluateUpstream(Integer sno){

		resetUpstream(sno);

		for (GGNode anc: ancestors)
			anc.evaluate(sno);
	}

	public void printParents(){
		for (Iterator<GGCPMNode> e=parents.iterator() ; e.hasNext();)
			System.out.print(e.next().identifier() + " ");
	}
}
