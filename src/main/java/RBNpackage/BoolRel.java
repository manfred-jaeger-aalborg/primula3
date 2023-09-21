package RBNpackage;
import java.util.Vector;

import org.dom4j.Element;

public class BoolRel extends Rel {

	private static final long serialVersionUID = 1L;

	public BoolRel() {
		super();
		valtype = Rel.BOOLEAN;
	}
	
	public BoolRel(String n, int a){
		super(n,a);
		valtype = Rel.BOOLEAN;
	}
	
	
	public BoolRel(String n, int a, Type[] types){
		super(n,a,types);
		valtype = Rel.BOOLEAN;
	
	}
	
	public BoolRel(String n, int a, Vector<Type> types){
		super(n,a,types);
		valtype = Rel.BOOLEAN;
	}
	
	public BoolRel(Rel r){
		super(r.name(),r.arity,r.getTypes());
		valtype = Rel.BOOLEAN;
		inout = r.getInout();
	}

	/** Adds to root an element containing the 
  	 * header information for this BoolRel
  	 * @param root
  	 */
  	public void addRelHeader(Element root, String def, String inputoutput){
 
  		Element relel = root.addElement("Rel");
  		relel.addAttribute("name", name.name);
  		relel.addAttribute("arity", Integer.toString(arity));
 		relel.addAttribute("argtypes", getTypesAsString());
 		relel.addAttribute("valtype", "boolean");	
 		relel.addAttribute("default", def);
 		relel.addAttribute("type", inputoutput);
  		if (arity==1 || arity == 2)
  			relel.addAttribute("color", "(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() +")");;
  	}
//  	public BoolRel relToBoolRel(Rel r){
//		BoolRel result = new BoolRel(r.name.name,r.arity, r.getTypes() );
//		return result;
//	}
}