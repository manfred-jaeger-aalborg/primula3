/*
 * InferenceModule.java
 * 
 * Copyright (C) 2005 Aalborg University
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


package RBNgui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

import RBNpackage.*;

import java.util.*;

import javax.swing.DefaultListModel;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNCyclicException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNInconsistentEvidenceException;
import RBNLearning.GradientGraph;
import RBNLearning.GradientGraphO;
import RBNLearning.RelData;
import RBNLearning.RelDataForOneInput;
import RBNinference.BayesConstructor;
import RBNinference.MapThread;
import RBNinference.PFNetwork;
import RBNinference.SampleThread;
import RBNutilities.rbnutilities;
import edu.ucla.belief.ace.Control;

public class InferenceModule implements GradientGraphOptions {

	InferenceModuleGUI inferenceModuleGUI;
	public static final int OPTION_SAMPLEORD_FORWARD = 0;
	public static final int OPTION_SAMPLEORD_RIPPLE = 1;
	public static final int OPTION_NOT_SAMPLE_ADAPTIVE = 0;
	public static final int OPTION_SAMPLE_ADAPTIVE = 1;

	/**
	 * @uml.property  name="relationsListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.Rel"
	 */
	DefaultListModel relationsListModel = new DefaultListModel();

	/**
	 * @uml.property  name="valuesListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.Rel"
	 */
	DefaultListModel valuesListModel     = new DefaultListModel();

	/**
	 * @uml.property  name="elementNamesListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	DefaultListModel elementNamesListModel = new DefaultListModel();

	/**
	 * @uml.property  name="instantiationsListModel"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	DefaultListModel instantiationsListModel = new DefaultListModel();

	//den nye queryatom tabel
	/**
	 * @uml.property  name="dataModel"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	Vector<QueryTableModel> queryModels  = new Vector<QueryTableModel>();;

	Vector<MAPTableModel> mapModels = new Vector<MAPTableModel>();

	protected Vector<MCMCTableModel> mcmcModels = new Vector<MCMCTableModel>();

	protected Vector<ACETableModel> aceModels = new Vector<ACETableModel>();

	/**
	 * @uml.property  name="myACEControl"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	Control                      myACEControl;

	private Vector<TestTableModel> testModels = new Vector<TestTableModel>();


	/**
	 * ... keith cascio
	 * @uml.property  name="first_bin"
	 */


//
//	private boolean first_bin = true;  //user has selected the first element
//	/**
//	 * @uml.property  name="first_arb"
//	 */
//	private boolean first_arb = true;
	/**
	 * @uml.property  name="firstbinarystar"
	 */
	private boolean firstbinarystar = false;
	/**
	 * the tuple of element identifiers (including wildcards) selected from the element names list
	 */
	protected int[] element_tuple = new int[1];
	/**
	 * @uml.property  name="aritynumber"
	 */
	private int aritynumber;

	/**
	 * @uml.property  name="myprimula"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="evidenceModule:RBNgui.Primula"
	 */
	Primula myprimula;

	/**
	 * @uml.property  name="instasosd"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	OneStrucData inst;
	/**
	 * @uml.property  name="queryatoms"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	Hashtable<Rel,GroundAtomList> queryatoms;

//	/**
//	 * Maps a string representation of a query atom to a two-part index:
//	 * first index is the index of the table for the relation (as an element of
//	 * queryatomsScrollists), the second
//	 * is the index for this tuple in that table
//	 */
//	private Hashtable<String,int[]> groundAtomIndex;

	public Hashtable<Rel, GroundAtomList> getQueryatoms() {
		return queryatoms;
	}

	/**
	 * Maps a relation (identified by its name) to the index of the query atoms
	 * for this relation in the queryatomsScrolllists
	 */
	Hashtable<String,Integer> relIndex = new Hashtable<String,Integer>();
	/**
	 * Vector of relations defining their order in queryatomsScrolllists:
	 *
	 * relArray[i]= r  <=> relIndex.get(r.name)==i
	 */
	Vector<Rel> relList;

//	public GroundAtomList getQueryatoms() {
//		return queryatoms;
//	}
//
//	public void setQueryatoms(GroundAtomList queryatoms) {
//		this.queryatoms = queryatoms;
//	}

	/* The gradient graph structure constructed in current inference
	 * process
	 */
	private GradientGraph currentGG;

	/**
	 * @uml.property  name="sampthr"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="RBNpackage.Atom"
	 */
	private SampleThread sampthr;
	
	private MapThread mapthr;
	/**
	 * @uml.property  name="sampling"
	 */
	protected boolean sampling;
	
	//private boolean maprestarts;
	/**
	 * @uml.property  name="pausemcmc"
	 */
	protected boolean pausemcmc = false;
	/**
	 * @uml.property  name="evi"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	//private InferenceModule evi = this;
	
	/**
	 * @uml.property  name="savefile"
	 */
	private File savefile;


	/**
	 * @uml.property  name="instantiations" multiplicity="(0 -1)" dimension="2"
	 */
//	private int [][] instantiations;


	/* in adaptive sampling and for the query nodes the samples are assigned in a cyclic
	 * fashion to num_subsamples_adapt, resp. num_subsamples_minmax.
	 * For adaptive sampling: Variance of sampleweights in
	 * the different subsamples is used to determine
	 * the weight with which the current estimated probabilities
	 * are used for the sampling probabilities
	 * For querynodes: variance (and max/min values) in the
	 * different subsamples is displayed to provide some error estimate
	 *
	 */
	/**
	 * @uml.property  name="adaptivemode"
	 */
	private int adaptivemode;
	/**
	 * @uml.property  name="sampleordmode"
	 */
	private int sampleordmode;
	/**
	 * @uml.property  name="cptparents"
	 */
	private int cptparents = 3; // Max. number of parents for nodes with standard cpt
	/**
	 * @uml.property  name="num_subsamples_minmax"
	 */
	private int num_subsamples_minmax = 10;
	/**
	 * @uml.property  name="num_subsamples_adapt"
	 */
	private int num_subsamples_adapt = 10;

	
	/* Options for MAP inference */
	
	private int windowsize;
	private int numchains;
	private int numrestarts;
	private boolean ggverbose;
	private int maxfails;
	
	/**
	 * @uml.property  name="samplelogmode" multiplicity="(0 -1)" dimension="1"
	 */
	private boolean[] samplelogmode = new boolean[5];
	/* True components of samplelogmode determine what is to be logged:
	 * [0]: Sampling order
	 * [1]: Current Evidence
	 * [2]: Compact Trace
	 * [3]: Full Trace (only one of [2] or [3] can be true)
	 * [4]: Network statistics
	 */

	/**
	 * @uml.property  name="logwriter"
	 */
	protected BufferedWriter logwriter = null;
	/**
	 * @uml.property  name="logfilename"
	 */
	protected String logfilename = "";
	private Observer valueObserver;
	private String modelPath;
	private String scriptPath;
	private String scriptName;
	private String pythonHome;
	List<MapThread> threads;
	int num_threads;
	// set the Map Seach Algorithm used during map inference
	protected int mapSearchAlg;
	private int batchSearchSize;
	private int sampleSizeScoring;
	// the number of iteration for the greedy search algorithm
	private int numIterGreedyMap;
	private int lookaheadSearch;

	/**
	 * @uml.property  name="settingssamplingwindowopen"
	 */
	boolean settingssamplingwindowopen = false;
	boolean settingsmapwindowopen = false;


	public InferenceModule( Primula myprimula_param ){

		myprimula = myprimula_param;
		inferenceModuleGUI = null;
		sampling = false;
//		maprestarts = false;
		inst = myprimula.instasosd;

		ggverbose=true;
		sampleordmode = OPTION_SAMPLEORD_FORWARD;
		adaptivemode = OPTION_NOT_SAMPLE_ADAPTIVE;
		for (int i=0;i<samplelogmode.length;i++)
			samplelogmode[i]=false;

		numchains = 0;
		windowsize = 2;
		numrestarts = 1;
		batchSearchSize = 1;
		sampleSizeScoring = 0;
		lookaheadSearch = 3;

		readElementNames();
		readRBNRelations();

		updateInstantiationList();

		queryatoms=myprimula.queryatoms.asHashTable();
		relIndex = new Hashtable<String,Integer>();
		relList = new Vector<Rel>();
		int idx =0;

		for (Rel r: queryatoms.keySet()) {
			relIndex.put(r.name(), (Integer)idx);
			relList.add(r);
			queryModels.add(new QueryTableModel());
			idx++;
		}

		this.numIterGreedyMap = 1;
		this.mapSearchAlg = 2;
	}

	public SampleThread startSampleThread(){
		sampling = true;
		PFNetwork pfn = null;
		if (!noLog()){
			if (logfilename != "")
				logwriter = myio.FileIO.openOutputFile(logfilename);
			else logwriter = new BufferedWriter(new OutputStreamWriter(System.out));

		}

		try{
			BayesConstructor constructor = null;
			constructor = new BayesConstructor(myprimula.rbn,myprimula.rels,inst,myprimula.queryatoms,myprimula);
			constructor.setMyprimulaGUI(myprimula.myprimulaGUI);
			pfn = constructor.constructPFNetwork(myprimula.evidencemode,
					Primula.OPTION_QUERY_SPECIFIC,
					myprimula.isolatedzeronodesmode);
			pfn.prepareForSampling(sampleordmode,
					adaptivemode,
					samplelogmode,
					cptparents,
					this.queryatoms,
					num_subsamples_minmax,
					num_subsamples_adapt,
					logwriter);
		}
		catch(RBNCompatibilityException ex){System.out.println(ex.toString());}
		catch(RBNIllegalArgumentException ex){System.out.println(ex.toString());}
		catch(RBNCyclicException ex){System.out.println(ex.toString());}
		catch (RBNInconsistentEvidenceException ex){System.out.println("Inconsistent Evidence");}
		catch (IOException ex){System.out.println(ex.toString());}

		sampthr = new SampleThread(this,
				pfn,
				this.queryatoms,
				samplelogmode,
				logwriter);

		sampthr.start();
		return sampthr;
	}

    public void stopSampling() {
        sampling = false;
        sampthr.setRunning(false);
    }

    public void setQueryAtoms(Hashtable<Rel,GroundAtomList> atomsList) {
        this.queryatoms = atomsList;
    }

    public void stopSampleThread(){
        sampling = false;
        sampthr.setRunning(false);
        if (!noLog()){
            try{
                logwriter.flush();
                if (logfilename != "")
                    logwriter.close();
            }
            catch (java.io.IOException ex){System.err.println(ex);};
        }
        pausemcmc = false;
    }

    public SampleThread getSampthr() {
        return sampthr;
    }

    public GradientGraph startMapThread(){
		GradientGraph gg = null;
		try{
//			maprestarts = true;
			RelData evidence = new RelData(myprimula.getRels(),myprimula.getInstantiation());
			int mode;

			Hashtable<String,Integer> parameters = new Hashtable<>();
			if (inferenceModuleGUI != null)
				parameters = myprimula.makeParameterIndexGUI();
			else {
				parameters = myprimula.makeParameterIndex();
			}

//			String[] rbnparams = myprimula.getRBN().parameters();
//			Hashtable<String,Integer> rbnparamidx = new Hashtable<String,Integer>();
//			for (int i=0;i<rbnparams.length;i++)
//				rbnparamidx.put(rbnparams[i], i);

			if (parameters.size() > 0)
				mode = GradientGraphO.LEARNANDMAPMODE;
			else
				mode = GradientGraphO.MAPMODE;

			gg = new GradientGraphO(myprimula,
					evidence,
					parameters,
					myprimula.makeMinMaxBounds(),
					this,
					queryatoms,
					mode,
					true);
			((GradientGraphO) gg).setNumChains(numchains);
			((GradientGraphO) gg).setWindowSize(windowsize);
			((GradientGraphO) gg).setMapSearchAlg(mapSearchAlg);
			((GradientGraphO) gg).setBatchSearchSize(batchSearchSize);
			((GradientGraphO) gg).setSampleSizeScoring(sampleSizeScoring);
			((GradientGraphO) gg).setLookaheadSearch(lookaheadSearch);
			((GradientGraphO) gg).load_gnn_settings(myprimula.getLoadGnnSet());
			mapthr = new MapThread(this, myprimula, (GradientGraphO) gg);
			mapthr.start();

		}
		catch (RBNCompatibilityException ex){System.out.println(ex.toString());}
		return gg;
	}

	private boolean checkGnnRel(RBN rbn) {
		for(int i=0; i<rbn.prelements().length; i++) {
			if (rbn.cpmod_prelements_At(i) instanceof CatGnn)
				return true;
		}
		return false;
	}

	public void stopMapThread() {
		mapthr.setRunning(false);
	}

	//reads the element names from the relstruc
	void readElementNames(){
		if(myprimula.rels instanceof SparseRelStruc){
			SparseRelStruc sparserst = (SparseRelStruc)myprimula.rels;
			Vector elementNames = sparserst.getNames();
			for(int i=0; i<elementNames.size(); ++i){
				elementNamesListModel.addElement((String)elementNames.elementAt(i));
			}
			Vector<BoolRel> attributeNames = sparserst.getBoolAttributes();
			for(int j =0; j<attributeNames.size();j++){
				elementNamesListModel.addElement("["+attributeNames.elementAt(j)+"*]");
			}
		}
//		if(myprimula.rels instanceof OrdStruc){
//			OrdStruc ordStruc = (OrdStruc)myprimula.rels;
//			for(int i=0; i<ordStruc.dom; ++i){
//				elementNamesListModel.addElement(ordStruc.nameAt(i));
//			}
//		}
		elementNamesListModel.addElement("*");
	}

	public void newAdaptiveMode(int admode){
		adaptivemode = admode;
	}

	public void newSampleordMode(int sordmode){
		sampleordmode = sordmode;
	}

	//reads the relation names from the rbn-file
	public void readRBNRelations(){
		if(myprimula.rbn != null){
			Rel[] rels = myprimula.rbn.Rels();
			for(int i=0; i<rels.length; ++i){
					relationsListModel.addElement(rels[i]);
			}
		}
	}

	/* Computes all tuples of domain elements that match
	 * the sequence of elementNamesListModel entries at 
	 * the indices given by tuple (either single domain 
	 * element indices, or *-expressions)
	 */
	protected int[][] allMatchingTuples(int[] tuple){
		Vector<int[]> elementsForCoordinate = new Vector<int[]>();
		int[] nextComponent;
		String stringAtTupleIndex;
		for(int i=0; i<tuple.length; i++){
			stringAtTupleIndex = (String)elementNamesListModel.elementAt(tuple[i]);
			if(stringAtTupleIndex.equals("*")){
				nextComponent = new int[myprimula.getRels().domSize()];
				for(int j=0; j<nextComponent.length; j++)
					nextComponent[j]=j;
			}
			else if(stringAtTupleIndex.startsWith("[")){
				String attrname = stringAtTupleIndex.substring(1,stringAtTupleIndex.length()-2);
				Vector<int[]> elementsOfAttr = myprimula.getRels().allTrue(myprimula.sig.getRelByName(attrname));
				/* Turn vector of int[1] into int[]:*/
				nextComponent = rbnutilities.intArrVecToArr(elementsOfAttr);
			}
			else{ /* tuple[i] is the domain element with index i */
				nextComponent = new int[1];
				nextComponent[0]=tuple[i];
			}
			elementsForCoordinate.add(nextComponent);
		}
		return rbnutilities.cartesProd(elementsForCoordinate);
	}

	private int[][] allMatchingTuples(String[] strtuple){
		Vector<int[]> elementsForCoordinate = new Vector<int[]>();
		int[] nextComponent;
		String nextstr;
		for(int i=0; i<element_tuple.length; i++){
			nextstr = strtuple[i];
			if(nextstr.equals("*")){
				nextComponent = new int[myprimula.getRels().domSize()];
				for(int j=0; j<nextComponent.length; j++)
					nextComponent[j]=j;
			}
			else if(nextstr.startsWith("[")){
				String attrname = nextstr.substring(1,nextstr.length()-2);
				Vector<int[]> elementsOfAttr = myprimula.getRels().allTrue(myprimula.sig.getRelByName(attrname));
				/* Turn vector of int[1] into int[]:*/
				nextComponent = rbnutilities.intArrVecToArr(elementsOfAttr);
			}
			else{ /* tuple[i] is the name of a domain element*/
				nextComponent = new int[1];
				nextComponent[0]=element_tuple[i];
			}
			elementsForCoordinate.add(nextComponent);
		}
		return rbnutilities.cartesProd(elementsForCoordinate);
	}

	public void updateInstantiationList(){
		//		selectedInstAtom = null;
		instantiationsListModel.clear();
		inst = myprimula.instasosd;
		Vector instAtoms = inst.allInstAtoms();
		for(int i=0; i<instAtoms.size(); ++i){
			InstAtom temp = (InstAtom)instAtoms.elementAt(i);
			int[] nodes = temp.args;
			String names = "(";
			if (temp.rel.arity > 0) {
				for (int j = 0; j < nodes.length; ++j) {
					if (j + 1 < nodes.length) {
						names = names + elementNamesListModel.elementAt(nodes[j]) + ", ";
					} else {  //last item
						names = names + elementNamesListModel.elementAt(nodes[j]);
					}
				}
			}
			names = names + ")";
			String listItem = (String)(temp.rel.name.name)  + names + " = " + temp.val_string();
			instantiationsListModel.addElement(listItem);
		}
//		instantiationsPanel.updateUI();
		if( myACEControl != null ) myACEControl.primulaEvidenceChanged();//keith cascio 20061010
	}

	// this function is to add query atom without the GUI
	public void addQueryAtoms(Rel rel, GroundAtomList gal) {
		Integer idx = relIndex.get(rel.toString());
		if (idx != null) {
			queryatoms.get(rel).add(gal);
		}
		else {
			idx=relIndex.size();
			relIndex.put(rel.name(), (Integer)idx);
			relList.add(rel);
			queryatoms.put(rel, gal);
			queryModels.add(new QueryTableModel());
		}
		queryModels.elementAt(idx).addQuery(gal);
		myprimula.queryatoms.add(gal);
//		this.buildQueryatomsTables(queryModels);
	}

	//updates the query atoms list
//	private void addAtoms(Rel rel, int[] tuple){
//		SparseRelStruc rstnew = new SparseRelStruc();
//		rstnew = (SparseRelStruc)myprimula.rels;
//
//		int[] temp = new int[tuple.length];
//		int pos = 0;
//		int length = tuple.length;
//		for(int x=0; x<tuple.length; x++){
//			temp[x] = tuple[x];
//		}
//		for(int i=0; i<length; i++){
//			if(elementNamesListModel.elementAt(tuple[i]).equals("*")){
//				Vector v = rstnew.getNames();
//				for(int j=0; j<v.size(); j++){
//					temp[pos] = j;
//					addAtoms(rel, temp);
//				}
//			}
//			else if(((String)elementNamesListModel.elementAt(tuple[i])).startsWith("[")){
//				Vector<BoolRel> attributeNames = rstnew.getBoolAttributes();
//				BoolRel nextattr;
//				for(int j =0; j<attributeNames.size();j++){
//					nextattr = attributeNames.elementAt(j);
//					if(((String)elementNamesListModel.elementAt(tuple[i])).equals("["+ nextattr +"*]")){
//						Vector<int[]> tuples = rstnew.allTrue(nextattr);
//						for(int k =0; k<tuples.size(); k++){
//							int[] temp2 = tuples.elementAt(k);
//							temp[pos] = temp2[0];
//							addAtoms(rel, temp);
//						}
//					}
//				}
//			}
//			else{
//				if(pos == length-1){
//					queryatoms.add(rel, temp);
//				}
//			}
//			pos++;
//		}
//		updateQueryatomsList(queryModel);
//		infoMessage.setText(rel.name.name+" ("+addedTuples+") added");
//		temp = null;
//	}

//	public void updateQueryatomsList(QueryTableModel qtm){
//		selectedQueryAtom = null;
//		qtm.reset();
//		Vector queries = queryatoms.allAtoms();
//		for(int i=0; i<queries.size(); ++i){
//			GroundAtom temp = (GroundAtom)queries.elementAt(i);
//			int nodes[] = temp.args;
//			Rel rel = temp.rel;
//			String names = ""+rel.name.name + "(";
//			for(int j=0; j<nodes.length; ++j){
//				if(j+1 < nodes.length){
//					names = names + elementNamesListModel.elementAt(nodes[j]) + ",";
//				}
//				else { //last item
//					names = names + elementNamesListModel.elementAt(nodes[j]);
//				}
//			}
//			names = names + ")";
//			String listItem = names;
//			qtm.addQuery(listItem);
//		}
//		querytable.updateUI();
//
//		if( myACEControl != null ) myACEControl.primulaQueryChanged();//keith cascio 20060620
//	}

	/**
	 * Before calling this method this.queryatoms and this.relIndex must contain current
	 * and consistent data
	 */
//	private void rebuildQueryAtomsPanel() {
//		queryatomsPanel.removeAll();
//		queryatomsScrolllists=new Vector<JScrollPane>();
//		querytables = new Vector<JTable>();
//		// First construct the appropriate number of gui elements:
//		for (int i =0;i<relIndex.size();i++) {
//			JScrollPane nextjsp = new JScrollPane();
//			JTable nextjt = new JTable();
//			queryatomsScrolllists.add(nextjsp);
//			queryatomsPanel.add(nextjsp);
//			nextjsp.getViewport().add(nextjt);
//		}
//		// Now connect to the data:
//		for (String r: relIndex.keySet()) {
//			int idx = relIndex.get(r);
//			querytables.elementAt(idx).setModel(queryModels.elementAt(idx));
//		}
//
//		queryatomsPanel.updateUI();
//	}


//	private void generateQueryatoms(){
//		LinkedList relstruct = new LinkedList();
//		queryatoms.reset();
//		LinkedList queryatoms = queryModel.getQuery();
//		for(int i=0; i<queryatoms.size(); i++){
//			String atom = ""+queryatoms.get(i);
//			//System.out.println("in generateQueryAtoms: " + atom);
//			String rel = atom.substring(0, atom.indexOf("("));
//			//rel = rel.substring(0, atom.indexOf(" "));
//			LinkedList elementNames = new LinkedList();
//			int comma = atom.indexOf("(")+1;
//			for(int j = atom.indexOf("("); j<atom.length(); j++){
//				String temp =""+ atom.charAt(j);
//				if(temp.equals(",")){
//					String element = atom.substring(comma, j);
//					elementNames.add(element);
//					comma = j+2;
//				}
//			}
//			String element = atom.substring(comma, atom.indexOf(")"));
//			elementNames.add(element);
//			int[] tuple = new int[elementNames.size()];
//			Rel relnew = null;
//			if(elementNames.size() == 1){
//				for(int m=0;m<attributesListModel.size();m++){
//					if(attributesListModel.get(m).toString().equals(rel)){
//						relnew = (Rel)attributesListModel.get(m);
//					}
//				}
//			}
//			else if(elementNames.size() == 2){
//				for(int m=0;m<binaryListModel.size();m++){
//					if(binaryListModel.get(m).toString().equals(rel)){
//						relnew = (Rel)binaryListModel.get(m);
//						//System.out.println("binaryListModel: "+binaryListModel.get(m).toString());
//					}
//				}
//			}
//			else {
//				for(int m=0;m<arbitraryListModel.size();m++){
//					if(((Rel)arbitraryListModel.get(m)).printname().equals(rel)){
//						relnew = (Rel)arbitraryListModel.get(m);
//					}
//				}
//			}
//			int [] args = new int [elementNames.size()];
//			for(int n=0; n<elementNames.size(); n++){
//				for(int o=0; o<elementNamesListModel.size(); o++){
//					if(elementNamesListModel.get(o).equals(elementNames.get(n))){
//						args[n] = o;
//					}
//				}
//			}
//			TempAtoms temp = new TempAtoms(relnew, args);
//			relstruct.add(temp);
//		}
//		for(int t=0; t<relstruct.size(); t++){
//			TempAtoms temp = (TempAtoms)relstruct.get(t);
//			addAtoms(temp.getRel(), temp.getArgs());
//		}
//	}

//	private double[][] computeQueryBatch(){
//		/* Computes the probability of each query atom in all
//		 * data cases contained in myprimula.rdata
//		 *
//		 * Assumes that all probabilities can be computed by just
//		 * evaluating the probability formula, i.e., no dependence on
//		 * unobserved atoms.
//		 *
//		 * Returns a queryatoms.length x 7 double matrix, containing for each
//		 * query atom:
//		 *
//		 * count of true positives
//		 * count of false positives
//		 * count of false negatives
//		 * count of true negatives
//		 * count of atoms for which probability was not computed, because of dependence on unobserved atom
//		 * count of atoms for which a truth value was not given in the data case
//		 * average log-likelihood
//		 */
//
//		double[][] result = new double[queryatoms.size()][7];
//
//		RelData rdata = myprimula.getReldata();
//
//		if (rdata.size() > 1){
//			System.out.println("Warning: data available for more than one input domain. Will evaluate queries only "
//					+ "for first input domain");
//		}
//		RelDataForOneInput rdoi = rdata.caseAt(0);
//		RelStruc A = rdoi.inputDomain();
//		OneStrucData osd;
//
//		GroundAtom gat;
//		CPModel pf;
//		String[] varargs;
//		int[] intargs;
//		double prob=0;
//		int tv;
//		RBN rbn = myprimula.rbn;
//		Boolean predpos=false;
//		for (int i=0;i<rdoi.numberOfObservations();i++){
//			osd = rdoi.oneStrucDataAt(i);
//			for (int j=0;j<queryatoms.size();j++){
//				gat=queryatoms.atomAt(j);
//				pf = rbn.cpmodel(gat.rel());
//				varargs = rbn.args(gat.rel());
//				intargs = gat.args();
//				try{
//					prob = (double)pf.evaluate(A,
//							osd,
//							varargs,
//							intargs,
//							0,
//							true,
//							true,
//							null,
//							false,
//							null,
//							null,
//							ProbForm.RETURN_ARRAY,
//							true,
//							null)[0];
//				}
//				catch (RBNCompatibilityException ex){System.out.println(ex);}
//				if (prob!=Double.NaN)
//					predpos = (prob>0.5);
//				else {
//					result[j][4]++; // no prediction
//					break;
//				}
//				result[j][6]+= prob;
//				// Now get the actual truth value
//				tv=osd.truthValueOf(gat);
//				switch (tv){
//				case 1:
//					if (predpos)
//						result[j][0]++; // true positive
//					else
//						result[j][2]++; // false negative
//					break;
//				case 0:
//					if (predpos)
//						result[j][1]++; // false positive
//					else
//						result[j][3]++; // true negative
//					break;
//				case -1:
//					result[j][5]++; // no ground truth
//				}
//			} //for (int j=0;j<queryatoms.size();j++)
//		} // (int i=0;i<rdoi.numberOfObservations();i++)
//
//		/* Normalize the likelihood */
//		double numevaluated;
//		for (int j=0;j<queryatoms.size();j++){
//			numevaluated = result[j][0]+result[j][1]+result[j][2]+result[j][3];
//			result[j][6]=result[j][6]/numevaluated;
//		}
//
//		// Temporary:
//		double TP=0;
//		double FP=0;
//		double FN=0;
//		double TN=0;
//
//		System.out.println("[TP,FP,FN,TN,Pred. not evaluated,Ground truth unknown, P(positive)]");
//		for (int i=0;i< result.length;i++){
//			System.out.println(queryatoms.atomAt(i).asString(A)+'\t'+StringOps.arrayToString(result[i], "[", "]"));
//			TP=TP+result[i][0];
//			FP=FP+result[i][1];
//			FN=FN+result[i][2];
//			TN=TN+result[i][3];
//		}
//
//		double acc= (TP+TN)/(TP+TN+FP+FN);
//
//		System.out.println("TP: " + TP +" FP: " + FP + " FN: " + FN + " TN: "+ TN);
//		System.out.println("Accuracy: " + acc );
//		return result;
//	}
//
//	private void evaluateAccuracy(){
//		/* Computes the accuracy of the current rbn 
//		 * for all probabilistic relations w.r.t. the 
//		 * probabilistic relations contained in myprimula.getRelData()
//		 * 
//		 * Assumes that all probabilities can be computed by just
//		 * evaluating the probability formula, i.e., no dependence on 
//		 * unobserved atoms. 
//		 * 
//		 * Returns for each relation a double matrix of length 5, containing:
//		 * 
//		 * count of true positives
//		 * count of false positives
//		 * count of false negatives
//		 * count of true negatives
//		 * count of atoms for which probability was not computed, because of dependence on unobserved atom
//		 */
//		
//		
//		Signature sig = myprimula.getSignature();
//		RelData rdata = myprimula.getReldata();
//		RBN rbn = myprimula.rbn;
//		
//		for (Rel r : sig.getProbRels()) {
//			System.out.println("Evaluate relation " + r.name() + " for " + rdata.cases().size() + " input domains");
//			ProbForm pf = rbn.probForm(r);
//			String[] varargs = rbn.args(r);
//			
//			double[] result = new double[7];
//
//			for (RelDataForOneInput rdoi: rdata.cases()) {
//				System.out.print(".");
//				RelStruc A = rdoi.inputDomain();
//				for (OneStrucData osd: rdoi.allOneStrucData()) {
//					double prob=Double.NaN;
//					/* First the true cases: */
//					Vector<int[]> at = osd.allTrue(r);
//
//					for (int[] intargs : at){
//							
//							boolean predpos;
//							
//							try{
//								prob = pf.evaluate(A, 
//										osd, 
//										varargs, 
//										intargs, 
//										true, 
//										new String[0], 
//										true, 
//										null, 
//										false,
//										null);
//							}
//							catch (RBNCompatibilityException ex){System.out.println(ex);}
//							if (prob!=Double.NaN)
//								if (prob > 0.5) // true positive
//									result[0]++;
//								else //false negative
//									result[2]++;
//							else {
//								result[4]++; // no prediction
//								break;
//							}
//					}
//					
//					/* Now the false cases (near duplicate code): */
//					at = osd.allFalse((BoolRel)r);
//
//					for (int[] intargs : at){
//							boolean predpos;
//
//							try{
//								prob = pf.evaluate(A, 
//										osd, 
//										varargs, 
//										intargs, 
//										true, 
//										new String[0], 
//										true, 
//										null, 
//										false,
//										null);
//							}
//							catch (RBNCompatibilityException ex){System.out.println(ex);}
//							if (prob!=Double.NaN)
//								if (prob < 0.5) // true negative
//									result[3]++;
//								else //false positive
//									result[1]++;
//							else {
//								result[4]++; // no prediction
//								break;
//							}
//					}		
//	
//	
//
//				
//	
//				} // for (OneStrucData osd: rdoi.allOneStrucData())
//			} // for (RelDataForOneInput rdoi: rdata.cases())
//			double acc= (result[0]+result[3])/(result[0]+result[1]+result[2]+result[3]);
//
//			System.out.println("TP: " + result[0]+" FP: " + result[1] + " FN: " + result[2] + " TN: "+ result[3]);
//			System.out.println("Accuracy: " + acc );
//		} // for (Rel r : : sig.getProbRels()) 
//
//
//	}
//	

	protected void evaluateAccuracy(){
		/* Computes the accuracy of the current rbn
		 * for all probabilistic relations w.r.t. the
		 * probabilistic relations contained in myprimula.getRelData()
		 *
		 * Assumes that all probabilities can be computed by just
		 * evaluating the probability formula, i.e., no dependence on
		 * unobserved atoms.
		 *
		 * Prints for each relation:
		 *
		 * count of correct predictions
		 * count of incorrect predictions
		 */


		Signature sig = myprimula.getSignature();
		RelData rdata = myprimula.getReldata();
		RBN rbn = myprimula.rbn;

		for (Rel r : sig.getProbRels()) {
			System.out.println("Evaluate relation " + r.name() + " for " + rdata.cases().size() + " input domains");
			CPModel cpm = rbn.cpmodel(r);
			String[] varargs = rbn.args(r);

			double[] result = new double[2];

			for (RelDataForOneInput rdoi: rdata.cases()) {
				RelStruc A = rdoi.inputDomain();
				for (OneStrucData osd: rdoi.allOneStrucData()) {

					Hashtable<String,Object[]> evaluated = new Hashtable<String,Object[]>();

					if (r instanceof BoolRel) {
						double prob =0;
						Vector<int[]> at = osd.allTrue(r);
						for (int[] intargs : at){
							try{
								prob = ((Double)cpm.evaluate(A,
										osd,
										varargs,
										intargs,
										0,
										true,
										true,
										null,
										false,
										evaluated,
										null,
										ProbForm.RETURN_ARRAY,
										true,
										null)[0]);
							}
							catch (RBNCompatibilityException ex){System.out.println(ex);}
							if (prob < 0.5) // false prediction
								result[1]++;
							else //true prediction
								result[0]++;
						}

						/* Now the false cases (near duplicate code): */
						at = osd.allFalse((BoolRel)r);
						for (int[] intargs : at){
							boolean predpos;

							try{
								prob = ((Double)cpm.evaluate(A,
										osd,
										varargs,
										intargs,
										0,
										true,
										true,
										null,
										false,
										evaluated,
										null,
										ProbForm.RETURN_ARRAY,
										true,
										null)[0]);
							}
							catch (RBNCompatibilityException ex){System.out.println(ex);}
							if (prob < 0.5) // true negative
								result[0]++;
							else //false positive
								result[1]++;
						}

					} //if (r instanceof BoolRel)
					if (r instanceof CatRel) {
						double[] values = null;
						Vector<int[]> at = osd.allInstantiated(r);
						for (int[] intargs : at){
							try{
								values = ((double[])cpm.evaluate(A,
										osd,
										varargs,
										intargs,
										0,
										true,
										true,
										null,
										false,
										evaluated,
										null,
										ProbForm.RETURN_ARRAY,
										true,
										null)[0]);
							}
							catch (RBNCompatibilityException ex){System.out.println(ex);}
							int trueval = (int)osd.valueOf(r, intargs);
							if (rbnutilities.argmax(values)==trueval)
								result[0]++;
							else
								result[1]++;
						}
					}//if (r instanceof CatRel)
				} // for (OneStrucData osd: rdoi.allOneStrucData())
			} // for (RelDataForOneInput rdoi: rdata.cases())
			System.out.println();
			double acc = result[0]/(result[0]+result[1]);
			System.out.println("True: " + result[0]+" False: " + result[1] + " Accuracy: " + acc);
			System.out.println( );
		} // for (Rel r : : sig.getProbRels())


	}
	

	/**
	 * @return
	 * @uml.property  name="logfilename"
	 */
	public String getLogfilename(){
		return logfilename;
	}
	/**
	 * @param logfilename
	 * @uml.property  name="logfilename"
	 */
	public void setLogfilename(String logfilename){
		this.logfilename = logfilename;
	}

	public int getSampleOrdMode(){
		return sampleordmode;
	}

	public void setSampleOrdMode(int sampleordmode){
		this.sampleordmode = sampleordmode;
	}

	public void setLearnSampleSize(Integer lss){
		numchains = lss;
	}
	public void setWindowSize(Integer gr){
		windowsize = gr;
	}
	public void setNumChains(int ch){ numchains = ch; }

	public void setVerbose(boolean v){
		ggverbose = v;
	}
	
	public Boolean getVerbose(){
		return ggverbose;
	}
	
	public void setMaxFails(Integer mf){
		maxfails = mf;
	}
	
	public void setNumRestarts(Integer mf){
		numrestarts = mf;
	}
	
	public int getAdaptiveMode(){
		return adaptivemode;
	}

	public void setAdaptiveMode(int adaptivemode){
		this.adaptivemode = adaptivemode;
	}

	public void setSettingsOpen(boolean b){
		settingssamplingwindowopen = b;
	}

	public boolean[] getSampleLogMode(){
		return samplelogmode;
	}

	public boolean getSampleLogMode(int i){
		return samplelogmode[i];
	}

	public void setCPTParents(int np){
		this.cptparents = np;
	}

	public int getCPTParents(){
		return cptparents;
	}

	public void setNumSubsamples_minmax(int nss){
		this.num_subsamples_minmax = nss;
	}

	public int getNumSubsamples_minmax(){
		return num_subsamples_minmax;
	}

	public void setNumSubsamples_adapt(int nss){
		this.num_subsamples_adapt = nss;
	}

	public int getNumSubsamples_adapt(){
		return num_subsamples_adapt;
	}

	/* aca = true only interesting for learning.
	 * Here this function is just required for implementing
	 * GradientGraphOptions
	 */
	public boolean aca(){
		return false;
	}
	
	public int getNumChains(){
		return numchains;
	}
	
	public int getWindowSize(){
		return windowsize;
	}
	
	public int getMaxFails(){
		return maxfails;
	}
	
	public int getMAPRestarts(){
		return numrestarts;
	}
	
	public boolean ggverbose(){
		return ggverbose;
	}
	
	public boolean ggrandominit(){
		return true;
	}
	
	
	/* Following some functions that are only relevant for learning.
	 * Here  just required for implementing
	 * GradientGraphOptions
	 */
	public boolean gguse2phase(){
		return false;
	}


	public int threadascentstrategy(){
		return 0;
	}

	public  int ggascentstrategy(){
		return 0;
	}

	public int lbfgsmemory(){
		return 0;
	}

	public int getMaxIterations(){
		return 0;
	}

	public  double getLLikThresh(){
		return 0.05; //TODO: integrate this into the MAP options window
	}

	public double getLineDistThresh(){
		return 0;
	}

	public double adagradepsilon(){
		return 0;
	}

	public  double adagradfade(){
		return 0;
	}

	@Override
	public int getType_of_gradient() {
		return 0;
	}

	public boolean learnverbose(){
		return false;
	}

	protected boolean noLog(){
		boolean result = true;
		for (int i=0;i<samplelogmode.length;i++){
			if (samplelogmode[i])
				result = false;
		}
		return result;
	}

	public void setSampleLogMode(int i, boolean b){
		this.samplelogmode[i] = b;
	}


	//     public void setDummyDouble(double dummydouble ){
		// 	this.dummydouble = dummydouble;
	//     }

	//     public double getDummyDouble(){
	// 	return dummydouble;
	//     }


//	public OneStrucData getMapValuesAsInst(){
//		LinkedList<String> mapvals = mapModel.getMapValues();
//		LinkedList<String> queryats = mapModel.getQuery();
//		OneStrucData result = new OneStrucData();
//		result.setParentRelStruc(myprimula.getRels());
//
//		Iterator<String> itq = queryats.iterator();
//
//		for (Iterator<String> itmap = mapvals.iterator(); itmap.hasNext();) {
//			System.out.println(itq.next() + " " + itmap.next());
//		}
////		for (int i=0;i< mapatoms.size();i++){
////			result.add(mapatoms.atomAt(i),instvals[i],"?");
////		}
//		return result;
//	}

//	private void setMAPTable() {
//		for (int i=0;i<querytables.size();i++) {
//			JTable qt = querytables.elementAt(i);
//			qt.setModel(mapModels.elementAt(i));
//			qt.setShowHorizontalLines(false);
//			qt.setPreferredScrollableViewportSize(new Dimension(146, 100));
//			//table header values
//			qt.getColumnModel().getColumn(0).setHeaderValue("Query Atoms");
//			qt.getColumnModel().getColumn(1).setHeaderValue("MAP");
//			qt.getColumnModel().getColumn(0).setPreferredWidth(150);
//		}
//	}

//	public QueryTableModel getDataModel() {
//		return dataModel;
//	}

	public MapThread getMapthr() {
		return mapthr;
	}

	public SampleThread getSamThr() {
		return sampthr;
	}

	public List<MapThread> getThreads() {
		return threads;
	}

	public void setNum_threads(int num_threads) {
		this.num_threads = num_threads;
	}

	public void setValueObserver(Observer o) {
		this.valueObserver = o;
	}

	// function for toggle value to true
	// val is the index in the categorical values we want to toggle
	public void toggleAtom(Rel rel, int val) {

		int[] el_tup = new int[1];
		el_tup = new int[rel.getArity()];
		int[][] instantiations = allMatchingTuples(el_tup);

		inst.add(rel, instantiations, val,"?");
		updateInstantiationList();
	}

	public void setBoolInstArbitrary(BoolRel rel, Boolean truthValue) {
		inst.add(new GroundAtom(rel,new int[0]),truthValue,"?");
		updateInstantiationList();
	}

	public void setMapSearchAlg(int mapSearchAlg) {
		this.mapSearchAlg = mapSearchAlg;
	}

	public int getBatchSearchSize() { return this.batchSearchSize; }
	public void setBatchSearchSize(int batchSearchSize) { this.batchSearchSize = batchSearchSize; }

	public int getSampleSizeScoring() { return sampleSizeScoring; }

	public void setSampleSizeScoring(int sampleSizeScoring) { this.sampleSizeScoring = sampleSizeScoring; }

	public int getNumIterGreedyMap() {
		return numIterGreedyMap;
	}

	public int getLookaheadSearch() {
		return lookaheadSearch;
	}

	public void setLookaheadSearch(int lookaheadSearch) {
		this.lookaheadSearch = lookaheadSearch;
	}

	public void setNumIterGreedyMap(int numIterGreedyMap) {
		this.numIterGreedyMap = numIterGreedyMap;
	}

	public InferenceModuleGUI getInferenceModuleGUI() {
		return inferenceModuleGUI;
	}

	public void setInferenceModuleGUI(InferenceModuleGUI inferenceModuleGUI) {
		this.inferenceModuleGUI = inferenceModuleGUI;
	}

	public void addQueryAtom(Rel rel, GroundAtomList atstoadd, Integer idx) {
		idx=relIndex.size();
		relIndex.put(rel.name(), (Integer)idx);
		relList.add(rel);
		queryatoms.put(rel, atstoadd);
		queryModels.add(new QueryTableModel());
		queryModels.elementAt(idx).addQuery(atstoadd);
		myprimula.queryatoms.add(atstoadd);
	}
	public void deleteQueryAtoms() {
		queryModels=new Vector<QueryTableModel>();
		queryatoms = new Hashtable<Rel, GroundAtomList>();
		relList = new Vector<Rel>();
		relIndex = new Hashtable<String,Integer>();
	}

	public Primula getPrimula() {
		return myprimula;
	}
}
