
package MLNParser;


import MLNParser.DBParser.*;
import MLNParser.MLNParser.*;

import RBNpackage.OneStrucData;
import RBNpackage.RBN;
import RBNpackage.SparseRelStruc;
import java.io.File;
import myio.StringOps;
/**
 *
 * @author Alberto García Collado
 */
public class MLNParserFacade {
    
    private RBNCreator rbnCreator;
    private RSTCreator rstCreator;
    private RBN network;
    private OneStrucData instant;
    private SparseRelStruc relStruc;
    
    public MLNParserFacade(){
        
    }
    public void ReadMLN(File mln, File cwdb, File owdb){        
        rbnCreator = new RBNCreator(mln);
        rbnCreator.newRelTypesParser(mln);
        rbnCreator.readRelationAndTypes();
        
        rstCreator = new RSTCreator();
        rstCreator.setDeclaredTypes(rbnCreator.getDeclaredTypes());
        rstCreator.setRelations(rbnCreator.getRelations());
        
        rstCreator.initRelStruc();
        
        //read the close world
        if (cwdb != null){
        	rstCreator.newCWParser(cwdb);
        	rstCreator.readDB();
 
        }
        
       	rstCreator.setProbabilisticRelations();
    	relStruc=rstCreator.getRelStruc();
    	
        //read the open world
        if (owdb != null){
        	rstCreator.newOWParser(owdb);
        	rstCreator.readDB();
        }
        
        rbnCreator.setProbabilisticRelations(rstCreator.getProbabilisticRelations());
        rbnCreator.setTypes(rstCreator.getTypes());
        rbnCreator.readWeightedFormulas();

        rbnCreator.checkTypesParser();
        network = rbnCreator.readCheckTypes();
        rstCreator.addInstantiationOfArtificial(network);
        
    	instant=rstCreator.getInstantiation();
        
    }
    public RBN getRBN(){
        return network;
    }

    public SparseRelStruc getRelStruc() {
        return relStruc;
    }

    public OneStrucData getInstantiation() {
        return instant;
    }
    
   /* public static void main(String[]args){
        MLNParserFacade facade = new MLNParserFacade();
        facade.ReadMLN();
        System.exit(0);
    }*/

}
