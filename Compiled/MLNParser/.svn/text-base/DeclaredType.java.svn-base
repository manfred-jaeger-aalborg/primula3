/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package MLNParser;

import RBNpackage.TypeRel;
import java.util.Vector;
/**
 *
 * @author Alberto García Collado
 */
public class DeclaredType {
    /**this class is made to have a relation between the string with the name of 
     * the type, and its TypeRel and the constants that are asociated to it.
     */
    private TypeRel type;
    private Vector<String> constants;
    
    
    public DeclaredType(TypeRel typeR){
        type=typeR;
        constants=new Vector<String>();
    }
    public TypeRel getType() {
        return type;
    }

    public Vector<String> getConstants() {
        return constants;
    }
    
    public void insertConstant(String constant){
        constants.add(constant);
    }
    public boolean isConstantInType(String constant){
        return constants.contains(constant);
    }
    
}
