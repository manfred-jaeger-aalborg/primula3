package RBNpackage;
import java.util.Vector;

import org.dom4j.Element;

public class NumRel extends Rel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private double minval = Double.NEGATIVE_INFINITY;
	private double maxval = Double.POSITIVE_INFINITY;
	
	public NumRel() {
		super();
	}
	
	public NumRel(String n, int a){
		super(n,a);
		valtype = Rel.NUMERIC;
	}
		
	public NumRel(String n, int a, Type[] types){
		super(n,a,types);
		valtype = Rel.NUMERIC;
	
	}
	
	public NumRel(String n, int a, Vector<Type> types){
		super(n,a,types);
		valtype = Rel.NUMERIC;
	}
	
	public NumRel(String n, int a, double min, double max){
		super(n,a);
		valtype = Rel.NUMERIC;
		minval = min;
		maxval = max;
	}
		
	public NumRel(String n, int a, Type[] types, double min, double max){
		super(n,a,types);
		valtype = Rel.NUMERIC;
		minval = min;
		maxval = max;
	
	}
	
	public NumRel(String n, int a, Vector<Type> types, double min, double max){
		super(n,a,types);
		valtype = Rel.NUMERIC;
		minval = min;
		maxval = max;
	}
	
	public NumRel(Rel r){
		super(r.name(),r.arity,r.getTypes());
		valtype = Rel.NUMERIC;
		inout = r.getInout();
		if (r instanceof NumRel){
			minval = ((NumRel) r).minval();
			maxval = ((NumRel) r).maxval();
		}
		else{
			System.out.println("Warning: setting default min/max values in construction of NumRel" +  r.name());
			minval=0.0;
			maxval=1.0;
		}
	}
	
	/** Adds to root an element containing the 
  	 * header information for this NumRel
  	 * @param root
  	 */
  	public void addRelHeader(Element root, String def, String inputoutput){
 
  		Element relel = root.addElement("Rel");
  		relel.addAttribute("name", name.name);
  		relel.addAttribute("arity", Integer.toString(arity));
 		relel.addAttribute("argtypes", getTypesAsString());
 		relel.addAttribute("valtype", "numeric");	
 		relel.addAttribute("default", def);
 		relel.addAttribute("type", inputoutput);
  		if (arity==1 || arity == 2)
  			relel.addAttribute("color", "(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() +")");
  	}
  	public NumRel relToNumRel(Rel r){
		NumRel result = new NumRel(r.name.name,r.arity, r.getTypes() );
		return result;
	}
  	
  	public double minval(){
  		return minval;
  	}
  	
 	public double maxval(){
  		return maxval;
  	}
  	
}