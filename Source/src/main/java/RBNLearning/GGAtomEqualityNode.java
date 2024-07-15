package RBNLearning;

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
    public Double[] evaluate(Integer sno) {
        if (is_evaluated) {
            if (this.values_for_samples==null)
                return value;
            else
                return this.values_for_samples[sno];
        }

        double[] final_res = new double[2];

        for (int i = 0; i < 2; i++) {
        	if (!Double.isNaN(evalOfPFs[i]))
        		final_res[i]=evalOfPFs[i];
        	else
        		final_res[i]=children.elementAt(i).evaluate(sno)[0]; // child is scalar
        }
        if (final_res[0] == final_res[1])
            value = new Double[]{1.0};
        else
            value = new Double[]{0.0};

		if (this.depends_on_sample) {
			values_for_samples[sno] = value;
			is_evaluated_for_samples[sno]=true;
		}
		else {
			is_evaluated = true;
		}
        return value;
    }

    @Override
    public double evaluateGrad(Integer sno, String param) throws RBNNaNException {
        return 0;
    }



    @Override
    public boolean isBoolean() {
        return false;
    }
}
