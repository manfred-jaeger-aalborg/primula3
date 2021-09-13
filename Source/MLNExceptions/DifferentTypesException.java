/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package MLNExceptions;

import RBNpackage.Rel;
import RBNpackage.Type;

/**
 *
 * @author Alberto García Collado
 */
public class DifferentTypesException extends Exception {
    Type badType;
    Rel relation;
    String variable;
    /**
     * Creates a new instance of <code>DifferentTypesException</code> without detail message.
     */
    public DifferentTypesException() {
    }
    public DifferentTypesException(Type badType, Rel relation,String variable){
        this.relation = relation;
        this.badType = badType;
        this.variable=variable;
    }
    /**
     * Constructs an instance of <code>DifferentTypesException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public DifferentTypesException(String msg) {
        super(msg);
    }

    public Type getBadType() {
        return badType;
    }

    public Rel getRelation() {
        return relation;
    }

    public String getVariable() {
        return variable;
    }
    
}
