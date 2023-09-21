/*
 * RDEFReader.java 
 * 
 * Copyright (C) 2009 Aalborg University
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


package RBNio;

import java.awt.Color;
import java.util.*;
import java.io.File;

import RBNpackage.*;
import RBNutilities.*;
import RBNExceptions.*;
import RBNLearning.*;
import RBNgui.Primula;

import myio.*;
import mymath.*;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.SAXReader;


public class RDEFReader {

	public static final int READONESTRUC =0;
	public static final int READRELDATA =1;
	
	private Primula myprimula;
	
	private static class stringKeyPairComparator implements Comparator{
		
		/* The object arguments are presumed of the type
		 * [String,[Integer,Object]]
		 * 
		 *  Comparison is based on Integer component.
		 *  
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object ob1, Object ob2){
			
			int i1 = (Integer) ((Object[])((Object[])ob1)[1])[0];
			int i2 = (Integer) ((Object[])((Object[])ob2)[1])[0];
			
			if (i1 == i2)
				return 0;
			if (i1 > i2)
				return 1;
			else 
				return -1;
			
		}
	}
	
	public RDEFReader(Primula pr){
		myprimula = pr;
	}
	
	private static Rel findRel(Vector<Rel> vrel, String name){
		for (Iterator<Rel> i = vrel.iterator(); i.hasNext();){
			Rel nr = i.next();
			if (nr.name().equals(name))
				return nr;
		}
		return null;
	}
	
/** RelData primarily stores information on observed 
 * probabilistic relations. Specification of underlying 
 * input structures can be given either as an argument A, or
 * can be found in the rdef file. Only one of these should be
 * true. Therefore, if the rdef is found to contain specifications
 * of input structures (declarations of predefined relations, domain
 * specifications), then A must be null.
 * 
 * 
 * @param rdef
 * @param A
 * @return
 * @throws RBNIllegalArgumentException
 */
	public RelData readRDEF(String rdef, RelStruc A)
	throws RBNIllegalArgumentException
	{
		
		RelData reldata = new RelData();
		
		Signature sig = new Signature();
		
		Vector<OneBoolRelData> boolpredrels = new Vector<OneBoolRelData>();
		Vector<OneBoolRelData> boolprobrels = new Vector<OneBoolRelData>();
		
		Vector<OneNumRelData> numpredrels = new Vector<OneNumRelData>();
		Vector<OneNumRelData> numprobrels = new Vector<OneNumRelData>();
		
		//Vector<Rel> allrels = new Vector<Rel>();
		
		try{
			SAXReader reader = new SAXReader();

			File rdeffile = new File(rdef);
			Document doc = reader.read(rdeffile);
			Element root = doc.getRootElement();

			
			/* Read the relation declarations */
			Element reldecs = root.element("Relations");
			String rname;
			int rarity;
			String dv;
			String type;
			String argtypes;
			String minval;
			String maxval;
			String colstr;
			Color col;
			
			Rel r = new Rel();
			
			String valtype;
			
			for ( Iterator i = reldecs.elementIterator("Rel"); i.hasNext(); ) {
				Element reldec = (Element) i.next();
				rname = reldec.attributeValue("name");
				rarity = Integer.parseInt(reldec.attributeValue("arity"));
				dv = reldec.attributeValue("default");
				minval = reldec.attributeValue("min");
				maxval = reldec.attributeValue("max");
				type = reldec.attributeValue("type");
				argtypes = reldec.attributeValue("argtypes");
				valtype = reldec.attributeValue("valtype");
				
//				int[] colrgb = StringOps.stringToIntArray(reldec.attributeValue("color"));
//				System.out.println(rbnutilities.arrayToString(colrgb));
//				col = new Color(colrgb[0],colrgb[1],colrgb[2]);
				
				colstr = reldec.attributeValue("color");
				if (colstr != null){
					int[] colrgb = StringOps.stringToIntArray(colstr);
					col = new Color(colrgb[0],colrgb[1],colrgb[2]);
				}
				else
					col = Color.black;
				
				
				if(valtype.equals("boolean")){
					r = new BoolRel(rname,rarity,typeStringToArray(argtypes,rarity));
					r.setColor(col);
				}
				else{
					double min;
					double max;
					if (minval != null){
						min = Double.parseDouble(minval);
					}
					else min = Double.NEGATIVE_INFINITY;
					if (maxval != null){
						max = Double.parseDouble(maxval);
					}
					else max = Double.POSITIVE_INFINITY;
					
					r = new NumRel(rname,rarity,typeStringToArray(argtypes,rarity),min,max);
					r.setColor(col);
				}
				
				
				if (type.equals("predefined")){
					r.setInout(Rel.PREDEFINED);
					
					if (A != null)
						throw new RBNioException("Multiple specifications of input domains");
					else{
						if(valtype.equals("boolean")){
							boolpredrels.add(new OneBoolRelData((BoolRel)r,dv));	
						}
						else{
							numpredrels.add(new OneNumRelData((NumRel)r,Double.parseDouble(dv)));
						}

					}

				}
				if (type.equals("probabilistic")){
					r.setInout(Rel.PROBABILISTIC);
					if(valtype.equals("boolean")){
						boolprobrels.add(new OneBoolRelData((BoolRel)r,dv));	
					}
					else{
						numprobrels.add(new OneNumRelData((NumRel)r,Double.parseDouble(dv)));
					}
				}
				
				sig.addRel(r);
			}

			
			myprimula.setSignature(sig);
			Element datael = root.element("Data");
			for ( Iterator i = datael.elementIterator("DataForInputDomain"); i.hasNext(); ) {
				reldata.add(parseDataForOneInput((Element)i.next(),boolpredrels,boolprobrels,numpredrels,numprobrels,A,sig));
			}
			
		}
		catch (Exception e) {
			System.err.println(e);
		}
		return reldata;
	}
	
	private OneStrucData parseOneDataElement(
			Vector<OneBoolRelData> boolinitrels, 
			Vector<OneNumRelData> numinitrels,
			Element datael, 
			Hashtable<String,Object[]> namehasht,
			boolean havedomdec,
			boolean haverelstruc)
	throws RBNIllegalArgumentException
	{
		OneStrucData result = new OneStrucData();
		/* Initialize the result by a copy of initrels */
		for (int i=0;i<boolinitrels.size();i++)
			result.add(new OneBoolRelData(boolinitrels.elementAt(i).rel(),boolinitrels.elementAt(i).dv()));

		for (int i=0;i<numinitrels.size();i++)
			result.add(new OneNumRelData(numinitrels.elementAt(i).rel(),numinitrels.elementAt(i).dv()));

		int relarity = 0;
		
		for ( Iterator i = datael.elementIterator("d"); i.hasNext();) {
			Element nextdat = (Element) i.next();
			
			Rel currentrel = result.find(nextdat.attributeValue("rel")).rel();
			
			
			
			String argstr = nextdat.attributeValue("args");
			
					
		
			
			/*  Two possibilities: 
			 * 
			 *  1. argstr is of the form (o2,o43)(o33,0437)...(o4,o74) being 
			 *     an enumeration of ground tuples represented by their names
			 *     
			 *  2. argst is a pattern [type3,type2] of type relations. The pattern
			 *     represents all tuples in the Cartesian product of the types.
			 *     Especially needed for numeric relations.
			 * 
			 */
			int[][] intargs = null;
			
			if (argstr.length()>0) {
				if (argstr.startsWith("[")){
					String[] types = myio.StringOps.stringToStringArray(argstr);
					if (types.length != currentrel.getArity())
						System.out.println("Warning: number of components in " + argstr + "does not match arity of " + currentrel.name());
					Vector<int[]> typeints = new Vector<int[]>();
					for (int j=0;j<types.length;j++){
						Rel nexttyperel = result.find(types[j]).rel();
						if (!(nexttyperel instanceof BoolRel)||nexttyperel.getArity() != 1 )
							System.out.println("Warning: " + argstr + " contains invalid relation " + types[j]);
						Vector<int[]> nexttype = result.allTrue(nexttyperel);
						/* convert from Vector<int[]> to int[]
						 * The components of the vector are arrays of length 1
						 */
						int[] nexttypeasarr = new int[nexttype.size()];
						for (int k=0;k<nexttype.size();k++)
							nexttypeasarr[k] = nexttype.elementAt(k)[0];
						typeints.add(nexttypeasarr);
					}
					intargs = rbnutilities.cartesProd(typeints);
				}
				else{ // The o2,o43)(o33,0437)...(o4,o74) case
					String[][] argarr = myio.StringOps.stringToStringMatrix(argstr);

					relarity = currentrel.getArity();

					if (relarity == 0 ){
						intargs = new int[1][1];
						intargs[0][0]=0;
					}
					else{
						if (argarr.length>0)
							intargs = new int[argarr.length][argarr[0].length];
						else intargs = new int[0][];
						for (int tupno =0; tupno<argarr.length; tupno++){
							for (int k = 0;k<argarr[tupno].length;k++){
								Integer intval = (Integer)(namehasht.get(argarr[tupno][k])[0]);
								if (intval == null){
									/* If the rdef contains a domain declaration, or a RelStruc argument is 
									 * given, then namehasht must contain all names encountered in the data
									 */
									if (havedomdec)
										throw new RBNioException("Data contains undeclared object " + argarr[k]);
									if (haverelstruc)
										throw new RBNioException("Data contains object " + argarr[k] + " not existing in RelStruc");

									Integer nextind = new Integer(namehasht.size());
									Object[] elementinfo = new Object[2];
									elementinfo[0]=nextind;
									elementinfo[1]=null;
									namehasht.put(argarr[tupno][k],elementinfo);
									intargs[tupno][k]=nextind;
								}
								else
									intargs[tupno][k]=intval;
							}
						}
					}
				}
			}
			else {
				intargs=new int[0][];
			}

			
			
			String truthval = nextdat.attributeValue("val");
			
//			if(isBool(truthval)){
//				boolean tv = isBoolTrue(truthval);
//				for (int tupno =0; tupno<intargs.length; tupno++){
//					result.setData(currentrel,intargs[tupno],tv);
//				}
//			}
//			else {
//				for (int tupno =0; tupno<intargs.length; tupno++){
//					result.setData(new NumRel(nextdat.attributeValue("rel"),relarity),intargs[tupno],Double.parseDouble(truthval));
//				}
//			}
			
			if(currentrel instanceof BoolRel){
				for (int tupno =0; tupno<intargs.length; tupno++){
					result.setData((BoolRel)currentrel,intargs[tupno],isBoolTrue(truthval));
//					if (tupno % 100 ==0)
//						System.out.println(tupno);
				}
			}
			
			else {
				for (int tupno =0; tupno<intargs.length; tupno++){
					result.setData((NumRel)currentrel,intargs[tupno],Double.parseDouble(truthval));
				}
			}

			
		}
		String wstring = datael.attributeValue("weight");
		if (wstring!= null) {
			result.setWeight(Double.parseDouble(wstring));
		};
		return result;
	}

	private RelDataForOneInput parseDataForOneInput(Element el,
			Vector<OneBoolRelData> boolpredrels,
			Vector<OneBoolRelData> boolprobrels,
			Vector<OneNumRelData> numpredrels,
			Vector<OneNumRelData> numprobrels,
			RelStruc A,
			Signature sig){


		RelDataForOneInput result = new RelDataForOneInput();

		/* In a DataForInputDomain element of the rdef file objects are denoted by
		 * their name. For the internal representation in  OneRelData
		 * this has to be transformed into integer indices 0..n-1.
		 * 
		 * There are three possibilities:
		 * 
		 * - the DataForInputDomain element contains a "Domain" element, in which case the indices
		 * are taken from there
		 * 
		 * - there is no "Domain" element, but A != null. It then must be 
		 * the case that the names in the data are contained in A.elementnames.
		 * The correspondence between names and indices is given by
		 * A.elementnames.
		 * 
		 * - otherwise, objects are assigned indices in the order in which 
		 * they appear in the data portion.
		 * 
		 * In both cases a hashtable is constructed for mapping names (strings)
		 * to indices.
		 *  
		 * 
		 */


		Hashtable<String,Object[]> namehasht = new Hashtable<String,Object[]>();

		boolean havedomdec = false;
		boolean haverelstruc = (A != null);
		Object[] elementinfo;

		Element domel = el.element("Domain");
		try{
			if (domel != null){
				if (haverelstruc)
					throw new RBNioException("Multiple specifications of input domains");
				else{
					havedomdec=true;
					List<Element> objelements = domel.elements("obj");
					List<Element> namelist = domel.elements("simpledomain");
					if (objelements.size() > 0 && namelist.size() >0)
						System.out.println("Conflicting domain declarations using both <obj> and <names> elements");
					if (namelist.size() >1)
						System.out.println("Multiple declarations of <names> elements");
					if (objelements.size() > 0 ){
						for ( Iterator i = objelements.iterator(); i.hasNext();) {
							Element nextobj = (Element) i.next();
							elementinfo = new Object[2];
							elementinfo[0]=Integer.parseInt(nextobj.attributeValue("ind"));
							if (nextobj.attributeValue("coords")!=null)
								elementinfo[1]=StringOps.stringToIntegerVector(nextobj.attributeValue("coords")).asArray();
							else elementinfo[1]=null;
							namehasht.put(nextobj.attributeValue("name"),elementinfo);
						}
					}
					if (namelist.size() > 0 ){
						String allnames = namelist.get(0).attributeValue("allnames");
						String[] namearray = StringOps.stringToStringArray(allnames);
						for (int i=0;i<namearray.length;i++){
							elementinfo = new Object[2];
							elementinfo[0]=new Integer(i);
							elementinfo[1]=null;
							if(namehasht.get(namearray[i])!=null)
								System.out.println("Warning: duplicate name " + namearray[i] + " in allnames declaration");
							namehasht.put(namearray[i],elementinfo);	
						}
					}

				}
			}

			if (!havedomdec && haverelstruc){
				Vector<String> elementnames = A.getNames();
				Vector<int[]> coords = A.getCoords();

				for (int i=0;i<elementnames.size();i++){
					elementinfo = new Object[2];
					elementinfo[0]=(Integer)i;
					elementinfo[1]=coords.elementAt(i);
					namehasht.put(elementnames.elementAt(i),elementinfo);
				}
			}



			RelStruc inputrs;
			if (haverelstruc)
				inputrs = A;
			else{
				Element predreldata = el.element("PredefinedRels");
				OneStrucData inputdata = parseOneDataElement(boolpredrels, 
						numpredrels,
						predreldata,
						namehasht,
						havedomdec,
						haverelstruc);
				/* Construct the vector of elementnames and coordinates. This is 
				 * the vector of keys in namehasht sorted according to 
				 * their integer values. Note that the correspondence 
				 * between elementnames and indices 0,...,n-1 that is 
				 * usually assumed in a RelStr may not be enforced in
				 * the rdef file specification of the input structure.
				 */
				Vector<Object> stringinfopairs = new Vector<Object>();
				for (Iterator it = namehasht.entrySet().iterator(); it.hasNext();){
					Object[] nextpair = new Object[2];
					Map.Entry<String,Object[]> me = (Map.Entry<String,Object[]>)it.next();
					nextpair[0]=me.getKey();
					nextpair[1]=me.getValue();
					stringinfopairs.add(nextpair);
				}
				Object[] stringinfopairsarr = stringinfopairs.toArray();
				Arrays.sort(stringinfopairsarr, new stringKeyPairComparator());
				Vector<String> elementnames = new Vector<String>();
				Vector<int[]> coordinates = new Vector<int[]>();
				int[] nextcoords;
				for (int i=0;i<stringinfopairsarr.length;i++){
					elementnames.add((String)((Object[])stringinfopairsarr[i])[0]);
					nextcoords = (int[])((Object[])((Object[])stringinfopairsarr[i])[1])[1];
					if (nextcoords != null)
						coordinates.add(nextcoords);
					else 
						coordinates.add(new int[2]);
				}
				inputrs = new SparseRelStruc(elementnames,inputdata,coordinates,sig);
			}
			
			result.setA(inputrs);
			
			/* Now start reading the data */

			OneStrucData nextonestruc;

			for (Iterator<Element> i = el.elementIterator("ProbabilisticRelsCase");i.hasNext();){
				Element nextdatael = (Element)i.next();
				nextonestruc =  parseOneDataElement(boolprobrels,
						numprobrels,
						nextdatael,
						namehasht,
						havedomdec,
						haverelstruc);
				nextonestruc.setParentRelStruc(inputrs);
				result.addCase(nextonestruc);
			}

		}
		catch (RBNIllegalArgumentException e){System.out.println(e);}
		return result;
	}

	private Type[] typeStringToArray(String ts, int arity){
		Type[] result = new Type[arity];
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
	
	private Boolean isBoolTrue(String tv){
		return (tv.equals("true") || tv.equals("True") );
	}
	
	private Boolean isBool(String tv){
		return (tv.equals("true") || tv.equals("false") || tv.equals("True") || tv.equals("False"));
	}
	
}
