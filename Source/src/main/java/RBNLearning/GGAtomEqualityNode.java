package RBNLearning;

import java.util.*;
import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNNaNException;
import RBNpackage.*;

import java.util.Hashtable;
import java.util.Vector;

public class GGAtomEqualityNode extends GGCPMNode{

    double[] evalOfPFs;
    
    public GGAtomEqualityNode(GradientGraphO gg,
                              ProbForm pf,
                              Hashtable<String,GGCPMNode> allnodes,
                              RelStruc A,
                              OneStrucData I,
                              int inputcaseno,
                              int observcaseno,
                              Hashtable<String,Integer> parameters,
                              boolean useCurrentPvals,
                              Hashtable<Rel, GroundAtomList> mapatoms,
                              Hashtable<String,Object[]>  evaluated) throws RBNCompatibilityException {
        super(gg,pf,A,I);

        evalOfPFs = new double[2];
        
        for (int i=0;i<2;i++) {
        	Object o = ((ProbFormBoolAtomEquality) pf).args()[i];
        	if (o instanceof Integer)
        		evalOfPFs[i]=Double.valueOf((Integer)o);
        	else { // o is ProbFormAtom
        		evalOfPFs[i]=(double)((ProbFormAtom)o).evaluate(A, 
    					I , 
    					new String[0], 
    					new int[0] , 
    					false,
    					useCurrentPvals,
    					mapatoms,
    					false,
    					evaluated,
    					parameters,
    					ProbForm.RETURN_ARRAY,
    					true,
    					null)[0];
        		if (Double.isNaN(evalOfPFs[i])) {
        			GGCPMNode constructedchild = GGCPMNode.constructGGPFN(gg,
        					(ProbFormAtom) o,
        					allnodes,
        					A,
        					I,
        					inputcaseno,observcaseno,
        					parameters,
        					false,
        					false,
        					"",
        					mapatoms,
        					evaluated);
        			children.add(constructedchild);
        			constructedchild.addToParents(this);
        		}
        		else
        			children.add(null);

        	}
        }
    }

    @Override
    public double[] evaluate(Integer sno) {
        
		if (this.depends_on_sample && sno==null) {
			for (int i=0;i<thisgg.numchains*thisgg.windowsize;i++)
				this.evaluate(i);
			return null;
		}			
		if (this.depends_on_sample && is_evaluated_for_samples[sno]) 
				return this.values_for_samples[sno];
		if (!this.depends_on_sample && is_evaluated_for_samples[0])
			return this.values_for_samples[0];
		
        double[] both_sides = new double[2];
        double[] value = new double[1];

        for (int i = 0; i < 2; i++) {
        	if (!Double.isNaN(evalOfPFs[i]))
        		both_sides[i]=evalOfPFs[i];
        	else
        		both_sides[i]=children.elementAt(i).evaluate(sno)[0]; // child is scalar
        }
        if (both_sides[0] == both_sides[1])
        	value[0] = 1.0;
        else
        	value[0] = 0.0;

        if (this.depends_on_sample) {
			values_for_samples[sno] = value;
			is_evaluated_for_samples[sno]=true;
		}
		else {
			values_for_samples[0] = value;
			is_evaluated_for_samples[0]=true;
		}

        return value;
    }

    @Override
    public TreeMap<String,double[]> evaluateGradient(Integer sno) throws RBNNaNException {
        return new TreeMap<String,double[]>();
    }



    @Override
    public boolean isBoolean() {
        return false;
    }
}
