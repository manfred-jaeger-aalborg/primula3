package RBNpackage;
import java.util.*;


import org.dom4j.Element;

import RBNutilities.rbnutilities;

public class CatRel extends Rel {

	private static final long serialVersionUID = 1L;
	
	private String[] values;
	
	private HashMap<String,Integer> stringToIndx;
	private HashMap<Integer,String> indxToString;
	

	public CatRel() {
		super();
		valtype = Rel.CATEGORICAL;
	}
	
	public CatRel(String n, int a){
		super(n,a);
		valtype = Rel.CATEGORICAL;
	}
	
	public CatRel(String n, int a, String[] vals){
		this(n,a);
		this.values=vals;
		this.stringToIndx = new HashMap<String,Integer>();
		this.indxToString = new HashMap<Integer,String>();
		for (int i=0;i<vals.length;i++) {
			stringToIndx.put(vals[i], i);
			indxToString.put(i, vals[i]);
		}
	}
	
	public CatRel(String n, int a, Type[] types){
		super(n,a,types);
		valtype = Rel.CATEGORICAL;
	
	}
	
	public CatRel(String n, int a, Vector<Type> types){
		super(n,a,types);
		valtype = Rel.CATEGORICAL;
	}
	
	public CatRel(Rel r){
		super(r.name(),r.arity,r.getTypes());
		valtype = Rel.CATEGORICAL;
		inout = r.getInout();
	}
	
	public String get_String_val(Integer i) {
		return this.indxToString.get(i);
	}

	public Integer get_Int_val(String s) {
		return this.stringToIndx.get(s);
	}
	
	public int numVals() {
		return values.length;
	}
	
	/** Adds to root an element containing the 
  	 * header information for this CatRel
  	 * @param root
  	 */
  	public void addRelHeader(Element root, String def, String inputoutput){
 
  		Element relel = root.addElement("Rel");
  		relel.addAttribute("name", name.name);
  		relel.addAttribute("arity", Integer.toString(arity));
 		relel.addAttribute("argtypes", getTypesAsString());
 		relel.addAttribute("valtype", "categorical");	
 		relel.addAttribute("values", rbnutilities.arrayToString(values));
 		relel.addAttribute("default", def);
 		relel.addAttribute("type", inputoutput);
  		if (arity==1 || arity == 2)
  			relel.addAttribute("color", "(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() +")");;
  	}

}