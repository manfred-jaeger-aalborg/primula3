/*
 * RBN.java
 * 
 * Copyright (C) 2003 Max-Planck-Institut fuer Informatik,
 *                    Helsinki Institute for Information Technology
 *
 * contact:
 * jaeger@cs.auc.dk   www.cs.auc.dk/~jaeger/Primula.html
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

package RBNpackage;

import java.io.*;
import java.util.*;

import RBNio.*;
import RBNutilities.randomGenerators;
import RBNutilities.rbnutilities;
import RBNExceptions.*;





public class RBN extends java.lang.Object {


	private RBNElement[] elements; // the concatenation of macroelements and prelements.
	private RBNPreldef[] prelements;
	private RBNMacro[] macroelements;
	
	
	
	/* The current setting of values for all 
	 * parameters in the RBN
	 */
	private Hashtable<String,Double> paramvals;

	/** Creates new RBN */
	public RBN() {
	}

	public RBN(int nprels, int nmacros){
		prelements = new RBNPreldef[nprels];
		macroelements = new RBNMacro[nmacros];
		elements = new RBNElement[nprels+nmacros];
		
		paramvals = new Hashtable<String,Double>();
	}


	
	public RBN(File input_file){
		RBNReader3 rbnrdr =  new RBNReader3();
		
		RBN rbn = new RBN();
		try{
			rbn = rbnrdr.ReadRBNfromFile(input_file);
		}
		catch (RBNSyntaxException e){System.out.println(e);}
		catch (IOException e){System.out.println(e);};
		elements = rbn.elements;
		prelements = rbn.prelements;
		macroelements = rbn.macroelements;
		
		/* Initialize parameters with 0.5 values*/
		String[] allparams = this.parameters();
		
		paramvals = new Hashtable<String,Double>();
		for (int i=0;i<allparams.length;i++)
			paramvals.put(allparams[i],0.5);
	}


	/** @author keith cascio
    	@since 20060515 */
    	public RBN( RBN toCopy ){
    		if( toCopy.elements != null ) {
    			this.elements = (RBNElement[]) toCopy.elements.clone();
    			this.prelements = (RBNPreldef[]) toCopy.prelements.clone();
    			this.macroelements = (RBNMacro[]) toCopy.macroelements.clone();
    			
    		}
    		
    	}

    	public RBNElement[] elements(){
    		return elements;
    	}

    	public RBNPreldef[] prelements() {
    		return prelements;
    	}
    	
    	public RBNMacro[] macroelements() {
    		return macroelements;
    	}
    	
    	public String[] arguments_prels_At(int i){
    		return prelements[i].arguments();
    	}

    	public String[] arguments_element_At(int i){
    		return elements[i].arguments();
    	}
    	
    	public String[] arguments(Rel r) {
    		int ind = indexOf(r);
    		if (ind >= 0)
    			return arguments_prels_At(ind);
    		else
    			return null;
    	}
    	
    	public boolean multlinOnly(){
    		boolean result = true;
    		for (int i=0;i<elements.length;i++)
    			if (!elements[i].cpmod().multlinOnly())
    				result = false;
    		return result;
    	}

    	/** Returns the index of the relation r in elements;
    	 * Returns -1 if r not found in probRels
    	 */
    	private int indexOf(Rel r){
    		boolean found = false;
    		int ind = 0;
    		while (!found && ind<prelements.length){
    			if (prelements[ind].rel().equals(r))
    				found = true;
    			if (!found)
    				ind++;
    		}
    		if (found)
    			return ind;
    		else
    			return -1;
    	}

    	public int NumPFs()
    	{
    		return prelements.length;
    	}

    	public Rel[] Rels()
    	{
    		Rel[] result = new Rel[prelements.length];
    		for (int i=0;i<prelements.length;i++)
    			result[i]=prelements[i].rel();
    		return result;
    	}


    	public Rel relAt(int i)
    	{
    		return prelements[i].rel();
    	}

       	public CPModel modAt(int i)
    	{
    		return prelements[i].cpmod();
    	}

    	public CPModel cpmod_prelements_At(int i)
    	{
    		return prelements[i].cpmod();
    	}

    	public CPModel cpmod_elements_At(int i)
    	{
    		return elements[i].cpmod();
    	}
    	
    	/** Returns the model for relation r */
    	public CPModel probForm(Rel r){
    		int ind = indexOf(r);
    		if (ind >= 0)
    			return cpmod_prelements_At(ind);
    		else
    			return null;
    	}

    	public String NameAt(int i)
    	{
    		return elements[i].rel().name.name;
    	}

//    	public String[] ArgsAt(int i)
//    	{
//    		return elements[i].arguments;
//    	}

    	/** Returns the argument tuple for the ProbForm for r */
    	public String[] args(Rel r){
    		int ind = indexOf(r);
    		if (ind >= 0)
    			return arguments_prels_At(ind);
    		else
    			return null;
    	}

    	public void insertPRel(RBNPreldef prd,int i) {
    		prelements[i]=prd;
    		elements[macroelements.length+i]=prd;
    	}
    	
    	public void insertMacro(RBNMacro m, int i) {
    		macroelements[i]=m;
    		elements[i]=m;
    	}
    	
//    	public void insertRel(BoolRel r, int i)
//    	{
//    		elements[i].set_rel(r);
//    	}
////    	public void insertRel(NumRel r, int i)
////    	{
////    		elements[i].prel=r;
////    	}
//    	public void insertArguments(String[] ags, int i)
//    	{
//    		elements[i].set_args(ags);
//    	}
//
//    	public void insertProbForm(ProbForm pf, int i)
//    	{
//    		elements[i].set_pform(pf);
//    	}

    	/** Returns all the parameters contained in probability formulas
    	 * in the RBN. Two occurrences of parameters with the same name 
    	 * are included only once.
    	 * @return
    	 */
    	public String[] parameters(){
    		String[] result = new String[0];
    		for (int i=0;i<elements.length;i++){
    			result = rbnutilities.arraymerge(result,elements[i].cpmod().parameters());
    		}
    		return result;
    	}

    	public void saveToFile(File rbnfile, int syntax, boolean paramsAsValues){
    		try{
    			FileWriter filwrt = new FileWriter(rbnfile);
    			for (int i=0;i<elements.length;i++){
    				filwrt.write(NameAt(i));
    				filwrt.write("(");
    				String[] args = arguments_element_At(i);
    				Type[] types = typesAt(i);
    				for (int j=0;j<args.length;j++){
    					if (!(types[j] instanceof TypeDomain))
    						filwrt.write("[" + types[j].getName() + "]");
    					filwrt.write(args[j]);
    					if (j<args.length-1)
    						filwrt.write(",");
    				}
    				filwrt.write(")");
    				filwrt.write("=" + '\n');
    				filwrt.write(cpmod_elements_At(i).asString(syntax,0,null,paramsAsValues,false)+ ";" + '\n' + '\n');

    			}
    			filwrt.close();
    		}
    		catch (Exception e) {
    			System.err.println(e);	 
    		}
    	}

    	/** Sets all occurrences of parameters appearing in params 
    	 * to their corresponding value in values. params and values
    	 * must be arrays of the same length
    	 * @param params
    	 * @param values
    	 */
    	public void setParameters(String[] params,  double[] values){
    		for (int i=0;i<params.length;i++) {
    			paramvals.put(params[i],values[i]);
    			for (int j=0; j<elements.length; j++){
    				elements[j].cpmod().setCvals(params[i], values[i]);
    			}
    		}
    	}
    	
    	public void setParameter(String par,  double val){
    		paramvals.put(par,val);
			for (int j=0; j<elements.length; j++){
				elements[j].cpmod().setCvals(par, val);
			}
    	}
    	
    	public void setParametersInFormulas() {
    		for(Enumeration<String>  e = paramvals.keys();e.hasMoreElements();)
    		{
    			String key = e.nextElement();
    			for (int j=0; j<elements.length; j++)
    				elements[j].cpmod().setCvals(key, paramvals.get(key));
    		}
    	}
    	
    	private Type[] typesAt(int i){
    		return elements[i].types();
    	}
    	
    	/* Set the values of all parameters in the rbn to 
    	 * random values
    	 */
    	public void setRandomParameterVals(){
    		Hashtable<String, Double> newht = new Hashtable<String,Double>();

    		for(Enumeration<String>  e = paramvals.keys();e.hasMoreElements();)
    		{
    			String key = e.nextElement();
    			if (key.charAt(0)=='#')
    				newht.put(key, Math.random());
    				
    			else 
        			newht.put(key, 2*Math.random()-1); // Initialize in range [-1,1]
    				//newht.put(key, 0.1);
    		}
    		paramvals= newht;
    		setParametersInFormulas();
    	}
    	
    	public void setRandomParameterVal(String parname){
    		if (parname.charAt(0)=='#')
    			paramvals.put(parname, Math.random());
    		else 
    			//paramvals.put(parname, 2*Math.random()-1); // random between -1 and 1
    			paramvals.put(parname, 0.1);
    	}
    	
    	public double getParameterValue(String parname){
    		return paramvals.get(parname);
    	}
    	
    	/**
    	 * Checks and sets relation properties in this 
    	 * rbn according to the signature s:
    	 * 
    	 * - checks that relations for which this rbn contains a defining
    	 * element are declared as probabilistic in s (else prints a warning)
    	 * 
    	 * - sets the relation attribute in all atoms that appear in probforms
    	 * in this rbn to the relations with the corresponding name in s 
    	 * 
    	 * @param s
    	 */
    	public void updateSig(Signature s){
    		RBNElement el;
    		Rel headrel;
    		Rel relinsig;
    		for (int i=0; i<prelements.length; i++){
    			el=prelements[i];
    			headrel = el.rel();
    			relinsig = s.getRelByName(headrel.name());
    			if (relinsig == null)
    				System.out.println("Warning: did not find relation " + headrel.name() + " used in RBN in the signature declaration");
    			if (relinsig.getInout()!=Rel.PROBABILISTIC)
    				System.out.println("Warning: relation " + headrel.name() + " not probabilistic according to signature");
    			if (!(relinsig instanceof BoolRel))
    				System.out.println("Warning: relation " + headrel.name() + " not Boolean according to signature");
    			else
    				el.setRel((BoolRel)relinsig);
    			el.cpmod().updateSig(s);
    		}
    		for (int i=0; i<macroelements.length; i++){
    			macroelements[i].cpmod().updateSig(s);
    		}
    	}
    	
    	/**
    	 * Returns all the probabilistic ancestor relations of r according to the RBN
    	 * 
    	 * @param r
    	 * @return
    	 */
    	public TreeSet<Rel> ancestorRels(Rel r){
    		TreeSet<Rel> result = new TreeSet<Rel>();
    		Stack<Rel>toprocess = new Stack<Rel>();
    		TreeSet<String> processedpfs = new TreeSet<String>();
    		toprocess.push(r);
    		while (!toprocess.isEmpty()) {
    			Rel nr = toprocess.pop();
    			if (!result.contains(nr)) {
    				result.add(nr);
    				for (Rel pr: this.probForm(nr).parentRels(processedpfs)) 
    					toprocess.push(pr);
    			}
    		}
    		return result;
    		
    	}
    	
    	public void set_elements(RBNElement[] els) {
    		elements = els;
    	}
    	
       	public void set_macroelements(RBNMacro[] ms) {
    		macroelements = ms;
    	}
       	
       	public void set_prelements(RBNPreldef[] prs) {
    		prelements = prs;
    	}
}

