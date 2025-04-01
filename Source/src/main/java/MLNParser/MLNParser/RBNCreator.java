/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package MLNParser.MLNParser;

import RBNpackage.*;
import myio.StringOps;
import MLNExceptions.NoSuchRelationException;
import MLNExceptions.BadArityException;
import MLNExceptions.DifferentTypesException;
import MLNParser.*;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Vector;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Alberto García Collado
 */
public class RBNCreator {
                
	private File mlnfile;
	
    private RBN network;
    private Vector<ProbForm> formulas;
    private Map<String, BoolRel> relations;// the relations are stored here not to be repeated
    private Vector<String> probabilisticRelations;
    
    /* artificialRelations and formulas are synchronized vectors: the ith ProbForm in 
     * formulas defines the ith artificial relation.
     */
    private Vector<Rel> artificialRelations;
    private Vector<Vector<ParsedRelation>> relationsForArtificial;// relations that are declared for checking the types of the freevars that are in each artificial relation.
    private int currentForm=0; // a pointer to the current vector used in relationsForArtificial
    
    private Map<String, DeclaredType> types;// the types are stored here not to be repeated
    
    private MLNParser parser;
//    private SecondMLNParser secondParser;
//    
    public RBNCreator(File mlnf) {
    	mlnfile = mlnf;
        formulas = new Vector<ProbForm>();
        relations = new TreeMap<String, BoolRel>();
        types = new TreeMap<String, DeclaredType>();
        
    }
    
    public void newRelTypesParser(File path){
        try {
            parser = new MLNParser(new java.io.FileInputStream(path));
            parser.setCreator(this);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }catch (FileNotFoundException e) {
            System.out.println("The path to the file "+path.toString()+" is not valid");
        }
    }
    
//    public void newWeightedFormulasParser(File path){
//        try {
//            secondParser = new SecondMLNParser(new java.io.FileInputStream(path));
//            secondParser.setCreator(this);
//        } catch (ParseException ex) {
//            ex.printStackTrace();
//        }catch (FileNotFoundException e) {
//            System.out.println("The path to the file "+path.toString()+" is not valid");
//        }
//    }
    public void checkTypesParser(){
        try {
            relationsForArtificial = new Vector<Vector<ParsedRelation>>();
            relationsForArtificial.add(new Vector<ParsedRelation>());
            parser.ReInit(new java.io.FileInputStream(mlnfile));
        }catch (FileNotFoundException e) {
            System.out.println("The path to the file "+mlnfile.toString()+" is not valid");
            e.printStackTrace();
        }
    }
    
    public void readRelationAndTypes() {
        try {
        	parser.setParse(1);
            parser.ReadMarkovLogicNetwork();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }
    
    public RBN readWeightedFormulas(){
        try {
//            secondParser.setArtificialRels(false);
//            secondParser.ReadMarkovLogicNetwork();
        	parser.ReInit(new java.io.FileInputStream(mlnfile));
        	parser.setParse(2);
        	parser.ReadMarkovLogicNetwork();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        catch (FileNotFoundException e) {
            System.out.println("The path to the file "+mlnfile.toString()+" is not valid");
            e.printStackTrace();
        }
        return network;
    }
    public RBN readCheckTypes(){
        try {
//            secondParser.setArtificialRels(true);
//            secondParser.ReadMarkovLogicNetwork();
        	parser.ReInit(new java.io.FileInputStream(mlnfile));
        	parser.setParse(3);
        	parser.ReadMarkovLogicNetwork();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        catch (FileNotFoundException e) {
            System.out.println("The path to the file "+mlnfile.toString()+" is not valid");
            e.printStackTrace();
        }
        return network;
                
    }
    
    public ProbForm createNot(ProbForm formula) {
        // !!a is translated to ((a:0,1):0,1)
        return new ProbFormConvComb(formula, new ProbFormConstant(0), new ProbFormConstant(1));
    }

    public ProbForm createAnd(ProbForm firstPart, ProbForm secondPart) {
        // a ^ b ^ c is translated to (c:(b:a,0),0)
        return new ProbFormConvComb(secondPart, firstPart, new ProbFormConstant(0));
    }

    public ProbForm createOr(ProbForm firstPart, ProbForm secondPart) {
        // a v b v c is translated to ((a:1,b):1,c)
        return new ProbFormConvComb(firstPart, new ProbFormConstant(1), secondPart);
    }

    public ProbForm createImplies(ProbForm firstPart, ProbForm secondPart) {
        //a => b => c is translated to ((a:b,1):c,1)
        return new ProbFormConvComb(firstPart, secondPart, new ProbFormConstant(1));
    }

    public ProbForm createIfOnlyIf(ProbForm firstPart, ProbForm secondPart) {
        // a <=> b <=> c is translated to ((a:b,(b:0,1)):c,(c:0,1))
        return new ProbFormConvComb(firstPart, secondPart,
                new ProbFormConvComb(secondPart, new ProbFormConstant(0), new ProbFormConstant(1)));
    }

    public ProbForm createExistential(ProbForm formula, Vector<String> quantargs) {
        // Exists x f(x,y) is translated to n-or{f(x,y)|x:}
    	ProbForm[] formargs = new ProbForm[1];
    	formargs[0]=formula;
        return new ProbFormCombFunc(new CombFuncNOr(), 
        		formargs, 
        		StringOps.stringVectorToArray(quantargs) , 
        		new ProbFormBoolConstant(true));
    }

    public ProbForm createUniversal(ProbForm formula, Vector<String> quantargs) {
        // Forall x f(x,y) is translated to (n-or{(f(x,y):0,1)|x:}:0,1)
    	ProbForm[] formargs = new ProbForm[1];
    	formargs[0]=new ProbFormConvComb(formula,new ProbFormConstant(0.0),new ProbFormConstant(1.0));
        return new ProbFormConvComb(
        		new ProbFormCombFunc(new CombFuncNOr(), 
        		formargs, 
        		StringOps.stringVectorToArray(quantargs) , 
        		new ProbFormBoolConstant(true)),
        		new ProbFormConstant(0.0),
        		new ProbFormConstant(1.0));
    }

    public void createRelation(ParsedRelation relation) {
        Type[] typesOfRelation = new Type[relation.getArguments().size()];
        int i = 0;
        for (String argument : relation.getArguments()) {
            typesOfRelation[i++]=newTypeDeclared(argument);
        }
        BoolRel rel = new BoolRel(relation.getRelationName(), relation.getArguments().size(), typesOfRelation);
        relations.put(relation.getRelationName(), rel);
    }
    public Type newTypeDeclared(String type){
        //this.getType(type);
        /**
         * Checks if the type is in the types Map, and returns it 
         * (adds it to the map if it isn't)
         */
        if(types.containsKey(type))        
            return types.get(type).getType();
        else{
            DeclaredType newType = new DeclaredType(new TypeRel(new BoolRel(type,1)));
            types.put(type, newType);
            return newType.getType();
        }
    }
    
    public void insertTypeConstant(String type,String constant){
        types.get(type).insertConstant(constant);
    }
    

    public ProbForm createFormulaRelation(ParsedRelation relation)
            throws NoSuchRelationException, BadArityException {
        Rel rel = relations.get(relation.getRelationName());
        if (rel == null) {
            throw new NoSuchRelationException();
        }
        if (rel.arity != relation.getArgumentsArray().length) {
            throw new BadArityException();
        }
//        if (!probabilisticRelations.contains(relation.getRelationName()))
//             return new ProbFormSFormula(new CConstrAtom(relations.get(relation.getRelationName()),
//                                                        relation.getArgumentsArray()));
//       return new ProbFormIndicator(rel, relation.getArgumentsArray());
        if (probabilisticRelations.contains(relation.getRelationName()))
        		rel.setInout(Rel.PROBABILISTIC);
        return new ProbFormAtom(rel, relation.getArgumentsArray());
        
    }
    
    public ProbForm createFormulaEquality(String var1, String var2){
    	return new ProbFormBoolEquality(var1,var2,true);
    }
    
    /*private Vector<Rel> getAllRels(Vector<Rel> vectorRelations, String parameterName,ProbForm formula){
    if (ProbFormIndicator.class.isInstance(formula)){
    String[] parameters = formula.parameters();
    for(int i=0; i<parameters.length;i++){
    if(parameters[i].equals(parameterName))
    vectorRelations.add(formula.);
    }
    }
    return null;
    }*/

    public ProbForm createWeightFormula(ProbForm formula, String tokenImage) {
        double weight = 0;
        if (tokenImage.equals("")) {
            formula = new ProbFormConvComb(formula, new ProbFormConstant(1), new ProbFormConstant(0));
        } else {
            try {
                weight = Double.valueOf(tokenImage);
            } catch (NumberFormatException e) {
                System.out.println("The weight " + tokenImage + " has not the correct format");
                e.printStackTrace();
            }
            if (weight >= 0) {
                formula = new ProbFormConvComb(formula, new ProbFormConstant(1), new ProbFormConstant(1 / Math.exp(weight)));
            } else {
                formula = new ProbFormConvComb(formula, new ProbFormConstant(Math.exp(weight)), new ProbFormConstant(1));
            }
        }
        return formula;
    }
    
    public void newFormulaForArtificial()
    {
        this.relationsForArtificial.add(new Vector<ParsedRelation>());
        this.currentForm++;
    }
    
    public void saveRelationForArtificial(ParsedRelation relation){
        relationsForArtificial.get(currentForm).add(relation);
    }
    
    public Type[] checkFreeVars(ProbForm formula){
        try{
            String[] freevars = formula.freevars();
            Type[] freetypes = new Type[freevars.length];
            Type[] typesOfRelation;
            for(int i=0;i<freevars.length;i++){
                boolean found=false;
                for(ParsedRelation rel:relationsForArtificial.get(currentForm)){
                    typesOfRelation = relations.get(rel.getRelationName()).getTypes();
                    for(int numArg=0; numArg < rel.getArgumentsArray().length; numArg++)
                        //check if the freevar is named in the relation
                        if(freevars[i].equals(rel.getArgumentsArray()[numArg])){
                            if(!found){
                                found=true;
                                freetypes[i]=typesOfRelation[numArg];
                            }else{
                                if(freetypes[i]!=typesOfRelation[numArg])
                                    throw new DifferentTypesException(freetypes[i],relations.get(rel.getRelationName()),freevars[i]);
                            }
                        }
                }
            }            
            return freetypes;
        }catch(DifferentTypesException e){
        System.out.println("The relation "+ e.getRelation().name + " has " + 
                e.getRelation().getTypes().toString() + " types declared and " + 
                e.getBadType() + "does not belong for " + e.getVariable());
        e.printStackTrace();
        }
        return null;
    }
    /*private Type searchTypeOfVar(String variable,ProbForm formula,Type typeFound){
        
        try{
            if (formula instanceof ProbFormConvComb){
                ProbFormConvComb newFormula = (ProbFormConvComb)formula;
                Type type1,type2,type3;
                type1 = searchTypeOfVar(variable, newFormula.f1(),typeFound);
                type2 = searchTypeOfVar(variable, newFormula.f2(),typeFound);
                type3 = searchTypeOfVar(variable, newFormula.f3(),typeFound);
                if (! (type1.equals(type2) && type2.equals(type3)))
                    System.out.println("types are different in "+newFormula.asString());
                else return type1;
            }
            else if (formula instanceof ProbFormConstant){
                return null;
            }
            else if (formula instanceof ProbFormIndicator){

                ProbFormIndicator newFormula = (ProbFormIndicator) formula;
                for (int i=0; i<newFormula.arguments.length; i++)
                    if(newFormula.arguments[i].equals(variable)){
                        if(typeFound==null){              
                            typeFound=newFormula.relation.getTypes()[i];
                            return typeFound;
                        }
                        else
                           if(typeFound.equals(newFormula.relation.getTypes()[i]))
                               return typeFound;
                           else
                               throw new DifferentTypesException (typeFound,newFormula.relation,variable);  
                    }
            
            }else if (formula instanceof ProbFormSFormula){
                ProbFormSFormula newFormula = (ProbFormSFormula) formula;
                /*for (int i=0; i<newFormula.parameters(); i++){
                    newFormula.
                    if(newFormula.sEval(A))
                        return newFormula.relation.getTypes()[i];
                }
                newFormula.
            }   
            return null;
        }catch(DifferentTypesException e){
            System.out.println("The relation "+ e.getRelation().name + " has " + 
                    e.getRelation().getTypes().toString() + " types declared and " + 
                    e.getBadType() + "does not belong for " + e.getVariable());
            e.printStackTrace();
        }

    }*/
    public void fileFinished() {
        network = new RBN(formulas.size() + probabilisticRelations.size(),0);
        artificialRelations = new Vector<Rel>();
        
        /*create the artificial relations
         */ 
        this.currentForm=0;
        for (ProbForm formula : formulas) {
            Type[] freetypes = this.checkFreeVars(formula);
            BoolRel relation = new BoolRel("MLNRel" + currentForm,formula.freevars().length,freetypes);
            
            RBNPreldef pdef=new RBNPreldef(relation,formula.freevars(),formula);
            network.insertPRel(pdef, currentForm);
            
            artificialRelations.add(relation);
            currentForm++;
        }
        
        /*insert the probabilistic relations into the RBN with a probability of 0.5
         */ 
        Vector<BoolRel> rels = new Vector(relations.values());
        for (BoolRel rel: rels){
            if(probabilisticRelations.contains(rel.printname())){
                String[] args = new String[rel.getArity()];
                for (int i=0;i<args.length;i++)
                	args[i]="x" + i;
                
                RBNPreldef pdef=new RBNPreldef(rel,args,new ProbFormConstant(0.5));
                network.insertPRel(pdef, currentForm);
            
                currentForm++;
            }
        }
    }

    public void addNewRelationFormula(ProbForm formula) {
        formulas.add(formula);
    }
    public Map<String,DeclaredType> getDeclaredTypes(){
        return this.types;
    }
    public Map<String,BoolRel> getRelations(){
        return this.relations;
    }

    public Vector<Rel> getArtificialRelations() {
        return artificialRelations;
    }

    public void setProbabilisticRelations(Vector<String> probabilisticRelations) {
        this.probabilisticRelations = probabilisticRelations;
    }


    public void setTypes(Map<String, DeclaredType> types) {
        this.types = types;
    }
    
    public Vector<ProbForm> getFormulas(){
    	return formulas;
    }
}
