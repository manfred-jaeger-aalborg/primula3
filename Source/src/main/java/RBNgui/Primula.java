/*
 * Primula.java
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



import javax.swing.*;

import RBNutilities.rbnutilities;
import myio.StringOps;

import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import MLNParser.MLNParserFacade;
import RBNpackage.*;
import RBNio.*;
import RBNinference.*;
import RBNExceptions.*;
import RBNLearning.*;
import edu.ucla.belief.ui.primula.*;
import edu.ucla.belief.ace.PrimulaSystemSnapshot;

public class Primula {

	PrimulaGUI myprimulaGUI;
	private Map<String,Object> load_gnn_set;
	public static final int OPTION_NOT_EVIDENCE_CONDITIONED = 0;
	public static final int OPTION_EVIDENCE_CONDITIONED = 1;
	public static final int OPTION_NOT_QUERY_SPECIFIC = 0;
	public static final int OPTION_QUERY_SPECIFIC = 1;
	public static final int OPTION_DECOMPOSE = 0;
	public static final int OPTION_DECOMPOSE_DETERMINISTIC = 1;
	public static final int OPTION_NOT_DECOMPOSE = 2;
	public static final int OPTION_NOT_ELIMINATE_ISOLATED_ZERO_NODES = 0;
	public static final int OPTION_ELIMINATE_ISOLATED_ZERO_NODES = 1;
	public static final int OPTION_NO_LAYOUT = 0;
	public static final int OPTION_LAYOUT = 1;
	public static final int OPTION_JAVABAYES = 0;
	public static final int OPTION_HUGIN = 1;
	public static final int OPTION_NETICA = 2;
	public static final int OPTION_SAMIAM = 3;

	public static final int CLASSICSYNTAX = 0;
	public static final int CHERRYSYNTAX = 1;

	/**
	 * @uml.property  name="querymode"
	 */
	protected int querymode = OPTION_NOT_QUERY_SPECIFIC ;
	/**
	 * @uml.property  name="evidencemode"
	 */
	protected int evidencemode = OPTION_EVIDENCE_CONDITIONED;
	/**
	 * @uml.property  name="decomposemode"
	 */
	protected int decomposemode = OPTION_NOT_DECOMPOSE;
	/**
	 * @uml.property  name="isolatedzeronodesmode"
	 */
	protected int isolatedzeronodesmode = OPTION_ELIMINATE_ISOLATED_ZERO_NODES;
	/**
	 * @uml.property  name="layoutmode"
	 */
	protected int layoutmode = OPTION_LAYOUT;
	/**
	 * @uml.property  name="bnsystem"
	 */
	protected int bnsystem = OPTION_SAMIAM;
	/**
	 * @uml.property  name="rbnsyntax"
	 */
	protected int rbnsyntax = CLASSICSYNTAX;

	/** @author keith cascio
	 @since  20061105 */
	public static final String  STR_OPTION_DEBUG = "debug";
	public static       boolean FLAG_DEBUG       = false;

	/**
	 * @uml.property  name="srsfile"
	 */
	protected File srsfile;

	protected File rdeffile;
	/**
	 * @uml.property  name="rbnfile"
	 */
	protected File rbnfile;
	/**
	 * @uml.property  name="bnoutfile"
	 */
	protected File bnoutfile;
	/**
	 * @uml.property  name="evidenceModule"
	 * @uml.associationEnd  inverse="myprimula:RBNgui.InferenceModule"
	 */
	protected InferenceModule evidenceModule;
	// +Learn
	/**
	 * @uml.property  name="learnModule"
	 * @uml.associationEnd  inverse="myprimula:RBNgui.LearnModule"
	 */
	protected LearnModule learnModule;

	/**
	 * @uml.property  name="rels"
	 * @uml.associationEnd
	 */
	protected GNNSettings gnnSettings;
	protected RelStruc rels;

	protected Signature sig;
	/**
	 * @uml.property  name="rbn"
	 * @uml.associationEnd
	 */
	protected RBN rbn;
	/**
	 * @uml.property  name="instasosd"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	protected OneStrucData instasosd = new OneStrucData();


	/* All data defining input domain, instantiation, and learning cases.
	 * First input domain specified in rdata is equal to rels,
	 * and first observed data case for first input domain is equal to
	 * instasosd
	 */
	RelData rdata;
	
	String[] rbnparameters;
	
	/* Numerical relations to be learned, divided into blocks
	 * Some relations may also be given by enumeration of their ground
	 * atoms
	 * 
	 */
	String[][] parameternumrels;
	
	/* All parameters to be optimized (RBN params and ground numerical relation atoms)
	 * mapped to an integer index by this hashtable */
	Hashtable<String,Integer> parameters;
	
	/*
	 *  2d array that contains for all parameters the relevant maximum and minimum bounds
	 *  First index is the index of a parameter according to the parameters hashtable
	 */
	double[][] minmaxbounds;
	
	/**
	 * @uml.property  name="queryatoms"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	protected GroundAtomList queryatoms = new GroundAtomList();

	/** @author keith cascio
	 @since 20060728 */
	public RBN getRBN(){
		return Primula.this.rbn;
	}

	public void setRBN(RBN newrbn){
		rbn = newrbn;
	}

	/**
	 * @author  keith cascio
	 * @since  20060728
	 * @uml.property  name="rels"
	 */
	public RelStruc getRels(){
		return Primula.this.rels;
	}

	public RelData getReldata(){
		return rdata;
	}

	public Signature getSignature(){
		return sig;
	}

	/** @author keith cascio
	 @since 20060515 */
	public PrimulaSystemSnapshot snapshot(){
		if( (this.rbn == null) || (this.rels == null) ) return null;

		PrimulaSystemSnapshot ret = new PrimulaSystemSnapshot(
				this,
				this.rbn,
				this.rels,
				this.instasosd,
				this.queryatoms,
				this.srsfile,
				this.rbnfile,
				this.bnoutfile,
				this.querymode,
				this.evidencemode,
				this.decomposemode,
				this.isolatedzeronodesmode,
				this.layoutmode,
				this.bnsystem,
				this.getPreferences().getACESettings()
		);
		return ret;
	}

	/** @author keith cascio
	 @since  20061201 */
	public void setDecomposeMode( int mode ){
		Primula.this.decomposemode = mode;
	}

	/**
	 * @uml.property  name="mySamiamManager"
	 * @uml.associationEnd
	 */
	protected SamiamManager mySamiamManager;
	/**
	 * @uml.property  name="myFlagSystemExitEnabled"
	 */
	boolean myFlagSystemExitEnabled = true;
	//	public static final String STR_FILENAME_LOGO = "src/main/java/Icons/small_logo.jpg";
	public static final String STR_FILENAME_LOGO = "small_logo.jpg";
	/**
	 * @uml.property  name="myPreferences"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	protected Preferences myPreferences;

	static boolean isLrnModuleOpen = false;
	static boolean isBavariaOpen  = false;
	static boolean isEvModuleOpen = false;

	/**
	 * @uml.property  name="bavaria"
	 * @uml.associationEnd  inverse="mainWindow:RBNgui.Bavaria"
	 */
	protected Bavaria bavaria;

	/**
	 @author Keith Cascio
	 @since 042104
	 */
	public ImageIcon getIcon( String fileName ){
		ClassLoader myLoader = this.getClass().getClassLoader();
		java.net.URL urlImage = myLoader.getResource( fileName );
		if( urlImage == null ){
			System.err.println( "Warning: loader.getResource(\""+fileName+"\") failed." );
			return new ImageIcon( fileName );
		}
		else return new ImageIcon( urlImage );
	}

	/**
	 @author Keith Cascio
	 @since 040504
	 */
	public static String pluckNameFromPath( String path )
	{
		int index0 = path.lastIndexOf( File.separator );
		if( index0 < (int)0 ) index0 = (int)0;
		else ++index0;
		int index1 = path.lastIndexOf( "." );
		if( index1 < (int)0 ) index1 = path.length();
		return path.substring( index0, index1 );
	}

	/**
	 @author Keith Cascio
	 @since 050404
	 */
	public Preferences getPreferences(){
		return myPreferences;
	}

	private String scriptPath;
	private String scriptName;
	private String pythonHome;

	public Primula()
	{
		load_gnn_set = new HashMap<>();
	}

	//creates a new ordered structure
	public void newOrdStruc(int dom){
		rels = new SparseRelStruc(dom);
//		strucEdited = false;
		srsfile = null;
//		if(isEvModuleOpen)
//			evidenceModule.newElementNames();
//		else{
//			instasosd.clear();
//			queryatoms.reset();
//		}
//		rstsrc.setText("Ordered Structure size "+dom);
	}

	//loads the sparserel structure from file
//	public void loadSparseRelFile(File srsfile) throws RBNIllegalArgumentException {
//		RDEFReader rdefreader = new RDEFReader(this);
//		rdata = rdefreader.readRDEF(srsfile.getPath(),null);
//		rels = rdata.caseAt(0).inputDomain();
//		getInstFromReldata();
//	}

	//loads the sparserel structure from file
	public void loadSparseRelFile(File srsfile){
		try{
			Rel.resetTheColorCounters();
			RDEFReader rdefreader = new RDEFReader(this);
			rdata = rdefreader.readRDEF(srsfile.getPath(),null);
			rels = rdata.caseAt(0).inputDomain();
//				rels = new SparseRelStruc(rdata.caseAt(0).inputDomain());
			getInstFromReldata();
		}catch (Exception ex){
			rels = null;
			srsfile = null;
			if (getPrimulaGUI() != null) {
				myprimulaGUI.rstsrc.setText("");
				myprimulaGUI.showMessage(ex.toString());
			}
		}
		if (getPrimulaGUI() != null)
			getPrimulaGUI().getDatasrcfilename().setText(srsfile.getName());
	}



	//loads the rbn file
	public void loadRBNFunction(File input_file){
		if(instasosd.isEmpty() && queryatoms.isEmpty()){
			rbn = new RBN(input_file, this.sig);
			rbnfile = input_file;
			if (getPrimulaGUI() != null)
				getPrimulaGUI().getRbnfilename().setText(rbnfile.getName());
			Rel.resetTheColorCounters();
			if(isEvModuleOpen)
				evidenceModule.getInferenceModuleGUI().updateRBNRelations();
		}
		else{
			try{
				rbn = new RBN(input_file, this.sig);
				rbnfile = input_file;
				if (getPrimulaGUI() != null)
					getPrimulaGUI().getRbnfilename().setText(rbnfile.getName());
			}catch (Exception ex){
				rbn = null;
				rbnfile = null;
				if (getPrimulaGUI() != null) {
					getPrimulaGUI().getRbnfilename().setText("");
					getPrimulaGUI().showMessage(ex.toString());
				}
			}
			if(isEvModuleOpen)
				evidenceModule.getInferenceModuleGUI().updateRBNRelations();
		}


//		rbn = new RBN(input_file, this.sig);
//		rbnfile = input_file;
		instasosd.init(rbn);
		/* extract rbn parameters */
		this.rbnparameters = rbn.parameters();
	}

	public void setRbnparameters(String[] rbnparameters) {
		this.rbnparameters = rbnparameters;
	}

	public void setRelData(RelData rd){
		rdata = rd;
		rels = rdata.caseAt(0).inputDomain();
	}

	public void setSignature(Signature s){
		sig=s;
	}

	//sets the current input file
	public void setInputFile(File inputFile){
		if(inputFile == null)
			srsfile = null;
		else
			srsfile = inputFile;
	}

	/** @author keith cascio
	 @since 20061023 */
	public OneStrucData getInstantiation(){
		return this.instasosd;
	}

	/** Returns this Instantiation as a SparseRelStruc **/
	public SparseRelStruc getInstantiationAsSRS(){
		return new SparseRelStruc(rels.getNames(),instasosd.copy(),rels.getCoords(),sig);
	}

	public String[][] getParamNumRels(){
		if (isLrnModuleOpen)
			return learnModule.getSelectedNumRels();
		else
			return new String[0][0];
	}

	/** @author keith cascio
	 @since 20061023 */
	public boolean instContainsAll( OneStrucData old ){
		if( instasosd == null ) return (old == null) || old.isEmpty();
		else               return instasosd.containsAll( old );
	}

	//returns true if the instantiation is empty (used by Bavaria)
	public boolean isInstEmpty(){
		return instasosd.isEmpty();
	}

	//returns true if the atomlist is empty (used by Bavaria)
	public boolean isQueryatomsEmpty(){
		return queryatoms.isEmpty();
	}

	public int evidencemode(){
		return evidencemode;
	}

	/** Opens Bavaria with the current rels */
	//	public Bavaria openBavaria(){
	//		return new Bavaria(new SparseRelStruc(), Primula.this, strucEdited);
	//	}

	public void getInstFromReldata(){
		instasosd = rdata.caseAt(0).oneStrucDataAt(0);
		if (instasosd==null){
			instasosd = new OneStrucData();
			instasosd.setParentRelStruc(rels);
		}
	}

	public void updateInstantiationInEM(){
		if(isEvModuleOpen)
			evidenceModule.updateInstantiationList();
	}

	public void updateBavaria(){
		if (isBavariaOpen)
			bavaria.update();
	}

	/* Initializes the RelData with rs as input domain
	 * and empty instantiations
	 */
	public void initRelData(RelStruc rs){
		instasosd = new OneStrucData();
		rdata = new RelData(rs, instasosd);
	}

	/* paramnames contains string representations of rbn parameters, and
	 * numerical atoms. paramvalues is an array of corresponding length.
	 * Method sets all rbn parameters to their given values in the rbn, and
	 * all the numerical atoms to their values in rels
	 */
	public void setParameters(String[] paramnames, double[] paramvalues){

		if (paramnames.length != paramvalues.length)
			System.out.println("Warning: un-matched arguments in Primula.setParameters");
		// first separate the RBN model parameters from
		// the numerical relation parameters
		Vector<String> rbnparams = new Vector<String>();
		Vector<String> nrelparams = new Vector<String>();
		Vector<Double> rbnvalues = new Vector<Double>();
		Vector<Double> nrelvalues = new Vector<Double>();


		for (int i=0;i<paramnames.length;i++)
			if (isRBNParameter(paramnames[i])){
				rbnparams.add(paramnames[i]);
				rbnvalues.add(paramvalues[i]);
			}
			else{
				nrelparams.add(paramnames[i]);
				nrelvalues.add(paramvalues[i]);
			}

		// Setting the RBN parameters:
		rbn.setParameters(StringOps.stringVectorToArray(rbnparams),StringOps.doubleVectorToArray(rbnvalues));

		// Setting the numerical relations:
		rels.addTuples(nrelparams,nrelvalues);
	}

	public void setParameters(Hashtable<String,Integer> paramidx,double[]paramvalues) {
		for (String par: paramidx.keySet()) {
			if (isRBNParameter(par))
				rbn.setParameter(par, paramvalues[paramidx.get(par)]);
			else
				rels.addTuple(par,paramvalues[paramidx.get(par)] );
		}
	}

	public double[] getParameterVals(String[] paramnames) {
		double[] result = new double[paramnames.length];
		for (int i=0;i<result.length;i++) {
			if (isRBNParameter(paramnames[i]))
				result[i]=rbn.getParameterValue(paramnames[i]);
			else
				result[i]=rels.getNumAtomValue(paramnames[i]);
		}
		return result;
	}

	public double[] getParameterVals(Hashtable<String,Integer> paramidx) {
		double[] result = new double[paramidx.size()];
		for (String par: paramidx.keySet()) {
			if (isRBNParameter(par))
				result[paramidx.get(par)]=rbn.getParameterValue(par);
			else
				result[paramidx.get(par)]=rels.getNumAtomValue(par);
		}
		return result;
	}

	public Boolean isRBNParameter(String str){
		if (str.charAt(0)=='#' || str.charAt(0)=='$')
			return true;
		else
			return false;
	}
	
	public Hashtable<String,Integer> makeParameterIndex(){
		this.parameters = new Hashtable<String,Integer>();
		int pidx = 0; // the index of the next parameter added to parameters
		for (int i=0;i<rbnparameters.length;i++) {
			parameters.put(rbnparameters[i], pidx);
			pidx++;
		}
		String[][] parameternumrels = this.getParamNumRels();
		
		/* Cannot handle learning numerical input relations for
		 * data with multiple input domains: check this and throw
		 * exception
		 */
		if (parameternumrels.length > 0 && (parameternumrels[0].length > 0 && rdata.size()>1))
			throw new RBNRuntimeException("Cannot handle learning numerical relations with multiple input domains");
		
		/* Construct the parameters corresponding to ground numrel atoms */
		RelDataForOneInput rdoi = rdata.caseAt(0);
		RelStruc A = rdoi.inputDomain();
		String nextp;
		for (int i=0;i<parameternumrels.length;i++){
			for (int j=0;j<parameternumrels[i].length;j++){
				nextp = parameternumrels[i][j];
				// The following a bit crude: distinguish relation names from ground atoms
				// just by occurrence of "("
				if (!nextp.contains("(")){
					Vector<String[]> alltuples = A.allTrue(nextp,A);
					for (String[] nexttup: alltuples) {
						parameters.put(parameternumrels[i][j]+StringOps.arrayToString(nexttup,"(",")"),pidx);
						pidx++;
					}
				}
				else{
					parameters.put(nextp,pidx);
					pidx++;
				}

			}
		}
		return this.parameters;
	}
	
	public double[][] makeMinMaxBounds(){
		/*
		 *  Create a 2d array that contains for all parameters the relevant maximum and minimum bounds
		 *  First index is the index of a parameter according to the parameters hashtable
		 */
		this.minmaxbounds = new double[parameters.size()][2];
		int pidx;

		for (String par: parameters.keySet()){
			pidx = parameters.get(par);
			if (this.isRBNParameter(par)) {
				if (par.charAt(0)=='#') {
					minmaxbounds[pidx][0] = 0.001;
					minmaxbounds[pidx][1] = 0.999;
				}
				else {
					minmaxbounds[pidx][0] = Double.NEGATIVE_INFINITY;
					minmaxbounds[pidx][1] = Double.POSITIVE_INFINITY;
				}
			}
			else {
				NumRel nextnr = this.getRels().getNumRel(GroundAtom.relnameFromString(par));
				minmaxbounds[pidx][0] = nextnr.minval();
				minmaxbounds[pidx][1] = nextnr.maxval();
			}
		}
		return this.minmaxbounds;
	}

	//sets the state of the Bavaria window
	public static void setIsBavariaOpen(boolean b){
		isBavariaOpen = b;
	}

	//sets the state of the evidence module window
	public static void setIsEvModuleOpen(boolean b){
		isEvModuleOpen = b;
	}

	//sets the state of the evidence module window
	public static void setIsLearnModuleOpen(boolean b){
		isLrnModuleOpen = b;
	}

	public void setScriptPath(String scriptPath) {
		this.scriptPath = scriptPath;
	}

	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
	}

	public void setPythonHome(String pythonHome) {
		this.pythonHome = pythonHome;
	}

	public String getScriptPath() {
		return this.scriptPath;
	}

	public String getScriptName() {
		return this.scriptName;
	}

	public String getPythonHome() {
		return this.pythonHome;
	}

	// add in order to use the RBN without the file
	public void setRbn(RBN rbn) {
		this.rbn = rbn;
	}

	public void setLoadGnnSet(Map<String,Object> sett) {
		this.load_gnn_set = sett;
	}

	public Map<String,Object> getLoadGnnSet() { return this.load_gnn_set; }

	public LearnModule openLearnModule(boolean visible){
		if(!isLrnModuleOpen){
			learnModule = new LearnModule(this, visible);
			isLrnModuleOpen = true;
		}
		return learnModule;
	}

	public InferenceModule createInferenceModule() {
		evidenceModule = new InferenceModule(this);
		return evidenceModule;
	}
	public void setPrimulaGUI(PrimulaGUI myprimulaGUI) { this.myprimulaGUI = myprimulaGUI; }

	public PrimulaGUI getPrimulaGUI() { return myprimulaGUI; }

	private void loadDefaults() throws RBNIllegalArgumentException {

		String rbninputfilestring = "/home/jaeger/B/Primula/Examples/GraphNN-develop/alpha1-[4].rbn";	
		String rstinputfilestring = "/home/jaeger/B/Primula/Examples/GraphNN-develop/GNNdata/p1/train-random-erdos-5000-40-50.rdef";

		srsfile = new File(rstinputfilestring);
		rbnfile = new File(rbninputfilestring);

		loadSparseRelFile(srsfile);
		loadRBNFunction(rbnfile);
	}

	private static RBNpackage.Type[] typeStringToArray(String ts, int arity){
		RBNpackage.Type[] result = new RBNpackage.Type[arity];
		String nexttype;
		int nextcomma;
		for (int i=0;i<arity;i++)
		{
			nextcomma = ts.indexOf(",");
			if (nextcomma != -1){
				nexttype = ts.substring(0,nextcomma);
				ts = ts.substring(nextcomma+1);
			}
			else{
				nexttype = ts;
				ts = "";
			}
			if (nexttype.equals("Domain"))
				result[i]=new TypeDomain();
			else
				result[i]=new TypeRel(nexttype);
		}
		return result;
	}
	private static  String[] valStringToArray(String vs) {
		return rbnutilities.stringToArray(vs,",");
	}

	private void setGNNPath() {
		setPythonHome("/Users/lz50rg/miniconda3/envs/torch/bin/python");
		setScriptPath("/Users/lz50rg/Dev/primula-workspace/primula3/Source/python/");
		setScriptName("load_gnn");
	}
}
