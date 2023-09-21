/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package MLNParser.DBParser;

import RBNpackage.*;


import MLNParser.*;

import RBNExceptions.RBNIllegalArgumentException;
import RBNExceptions.RBNCompatibilityException;

import RBNpackage.Type;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.File;
import java.util.Map;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;


/**
 *
 * @author Alberto García Collado
 */
public class RSTCreator {

    private SparseRelStruc relStruc;
    private OneStrucData instance;
    private DBParser parser;
    private Map<String, DeclaredType> types;
    private Map<String, BoolRel> relations;
    private Vector<String> probabilisticRelations;
    private Vector<String> predefinedRelations;
//    private Vector<BoolRel> artificialRelations;
//    private Vector<ProbForm> formulas;
    private boolean ow = false;

    public RSTCreator() {
    	instance = new OneStrucData();
    	predefinedRelations = new Vector<String>();
    }

    private boolean checkTypes(ParsedRelation relation) {
        Rel rel = relations.get(relation.getRelationName());
        Type[] relTypes = rel.getTypes();
        Vector<String> arguments = relation.getArguments();
        int i = 0;
        for (String argument : arguments) {
            if (!types.get(relTypes[i++].getName()).isConstantInType(argument)) {
                return false;
            }
        }
        return true;
    }
    
    public void newRelation(ParsedRelation relation, boolean state) {    
        //adds the constants declared in the type
        int i=0;
        Type[] typesOfRelation = relations.get(relation.getRelationName()).getTypes();
        for (String arg:relation.getArguments()){            
            //DeclaredType newDeclaredType =types.get(relation.getRelationName());
            DeclaredType typeOfTheRelation = types.get(typesOfRelation[i].getName());
            if(!typeOfTheRelation.isConstantInType(arg)){
                typeOfTheRelation.insertConstant(arg);
                relStruc.addNode(arg);
                
                String [] constantArray = new String[1];
                constantArray[0]=arg;
                relStruc.addTuple(typeOfTheRelation.getType().getRel(),constantArray);
            }
            i++;
        }
        
        if (checkTypes(relation)) {
            if (ow) {
                instance.add((BoolRel)relations.get(relation.getRelationName()),
                        relStruc.getIndexes(relation.getArgumentsArray()), state, "?");
            } else {
                if(!predefinedRelations.contains(relation.getRelationName()))
                    predefinedRelations.add(relation.getRelationName());
  
                if(state)
                relStruc.addTuple(relations.get(relation.getRelationName()),
                        relation.getArgumentsArray());
            }
        } else {
            System.out.println("the arguments are not with the correct type");
        }

    }

    public void newOWParser(File path) {
        try {
            if (instance == null) {
                instance = new OneStrucData();
            }
            ow = true;
            if (parser == null) {
                parser = new DBParser(new FileInputStream(path));
                parser.addCreator(this);
            } else 
            {
                parser.ReInit(new FileInputStream(path));
            }
        } catch (FileNotFoundException e) {
            System.out.println("The path to the file " + path + " is not valid");
        } catch (ParseException e) {
            System.out.println("Parse Exception");
            e.printStackTrace();
        }
    }
    
    public void newCWParser(File path) {
//        relStruc = new SparseRelStruc();
//        this.addTypeRel();
        ow = false;
        try {
            if (parser == null) {
                parser = new DBParser(new FileInputStream(path));
                parser.addCreator(this);
            } else {
                parser.ReInit(new FileInputStream(path));
            }

        } catch (FileNotFoundException e) {
            System.out.println("The path to the file " + path + " is not valid");
        } catch (ParseException e) {
            System.out.println("Parse Exception");
            e.printStackTrace();
        }
    }
    
    
    
    private void addTypeRel(){        
        //add typeRel to relStruc
        Vector<DeclaredType> decTypes = new Vector(types.values());
        for(DeclaredType decType:decTypes){
            BoolRel relation = decType.getType().getRel();
            relStruc.addRelation(relation);
            for (String constant:decType.getConstants()){
                relStruc.addNode(constant);
                
                String [] constantArray = new String[1];
                constantArray[0]=constant;                
                relStruc.addTuple(relation,constantArray);
            }                
        }        
    }
    
//    public void AddArtificialRelations() {
//        /*look for all the types that are in the relations and 
//         * get from the allTypedTuples the relations that are true 
//         * those types.
//         */
//        try{
////            for (Rel artRelation:artificialRelations){
////                //Type[] typ = artRelation.getTypes();
////                int[][] tuples =relStruc.allTypedTuples(artRelation.getTypes());
////                instance.add(artRelation, tuples, true, "?");    
////
////            }
//        	Rel nextartrel;
//        	ProbForm nextpf;
//        	for (int i=0;i<artificialRelations.size();i++){
//        		nextartrel = artificialRelations.elementAt(i);
//        		nextpf = formulas.elementAt(i);
//        		int[][] tuples =relStruc.allTypedTuples(nextartrel.getTypes());
//        		/* Note: the following assumes that  the order of the variables 
//        		 * in the argument of the 
//        		 */
//        		for (int k=0;k<tuples.length;k++){
//        			if (nextpf.substitute(nextpf.freevars(),tuples[k]).evaluatesTo(relStruc)==-1){
//        				
//        			}
//        		}
//        	}
//        } catch (RBNIllegalArgumentException e) {
//            System.out.println("Illegal types are trying to be added to the rel Struc");
//            e.printStackTrace();
//        }
//          catch (RBNCompatibilityException e) {
//                System.out.println("Illegal types are trying to be added to the rel Struc");
//                e.printStackTrace();
//          }
//        }
        
    public void addInstantiationOfArtificial(RBN network) {

    	BoolRel nextrel;
    	int[][] alltuples;
    	String nextrelname;
    	try{
    		for (int i=0;i<network.NumPFs();i++){
    			nextrel = network.relAt(i);
    			nextrelname = nextrel.printname();
    			if (nextrelname.length()>=6 && nextrelname.substring(0,6).equals("MLNRel")){
    				alltuples = relStruc.allTypedTuples(nextrel.getTypes());
    				for (int k=0;k<alltuples.length;k++){
    					instance.add(nextrel, alltuples[k], true, "?");   
    				}
    			}
    		}
    	}
    	catch (RBNIllegalArgumentException e) {
    		System.out.println("Illegal types are trying to be added to the rel Struc");
    		e.printStackTrace();
    	}
    }




    public void readDB() {
        try {
            parser.ReadDBFile();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }

    public OneStrucData getInstantiation() {
        return instance;
    }

    public SparseRelStruc getRelStruc() {
        return relStruc;
    }

    public Vector<String> getProbabilisticRelations() {
        return probabilisticRelations;
    }

    public Map<String, DeclaredType> getTypes() {
        return types;
    }

    public void setDeclaredTypes(Map<String, DeclaredType> tc) {
        types = tc;
        
    }

    public void setRelations(Map<String, BoolRel> rel) {
        relations = rel;
    }

//    public void setProbForms(Vector<ProbForm> pfs){
//    	formulas = pfs;
//    }
//    
//    public void setArtificialRelations(Vector<BoolRel> artificialRelations) {
//        this.artificialRelations = artificialRelations;
//    }
    
    /* probabilistic relations are all relations that were not determined
    * to be predefined by an occurrence in a domain .db
     */
    public void setProbabilisticRelations(){
    	probabilisticRelations = new Vector<String>();
    	Rel nextrel;
    	for (Iterator<BoolRel> it = relations.values().iterator(); it.hasNext();){
    		nextrel = it.next();
            if(!predefinedRelations.contains(nextrel.printname()))
              probabilisticRelations.add(nextrel.printname());
    	}
    }
    
    public void initRelStruc(){
        relStruc = new SparseRelStruc();
        this.addTypeRel();
    }
    
}
