/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package MLNParser;

import java.util.Vector;

/**
 *
 * @author Alberto García Collado
 */
public class ParsedRelation {
    private String relationName;
    private Vector<String> arguments;
    
    public ParsedRelation(){
        arguments = new Vector();
    }
    public ParsedRelation(String relationName) {
        arguments = new Vector();
        this.relationName = relationName;
    }
    
    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public Vector<String> getArguments() {
        return arguments;
    }
    public String[] getArgumentsArray(){
        String[]a = {};
        return arguments.toArray(a);
    }
    public String getRelationName() {
        return relationName;
    }
    

    public void addArgument(String argument){
        arguments.add(argument);
    }
    
    

}
