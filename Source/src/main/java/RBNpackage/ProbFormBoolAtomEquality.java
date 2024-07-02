package RBNpackage;

import RBNExceptions.RBNCompatibilityException;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;
import jdk.incubator.vector.VectorOperators;

import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Vector;

public class ProbFormBoolAtomEquality extends ProbFormBool {


	// arg1,arg2 are either ProbFormAtoms or Integers (i.e., indices of possible values of categorical relations)
    private Object arg1, arg2;

    public ProbFormBoolAtomEquality(Object a1, Object a2, boolean s){
        arg1 = a1;
        arg2 = a2;
        sign = s;
    }
    public ProbFormBoolAtomEquality(Object a1, Object a2, boolean s, Signature sig){
        if (a1 instanceof String) {
        	// Turn the string representation of a value into its integer index
        	Rel r = ((ProbFormAtom)a2).getRelation();
        	a1=r.get_Int_val((String)a1);
        }
        if (a2 instanceof String) {
        	// Turn the string representation of a value into its integer index
        	Rel r = ((ProbFormAtom)a1).getRelation();
        	a2=r.get_Int_val((String)a2);
        }
        arg1 = a1;
        arg2 = a2;
        sign = s;
    }

    @Override
    public int evaluatesTo(RelStruc A, OneStrucData inst,
                           boolean usesampleinst, Hashtable<String, GroundAtom> atomhasht)
            throws RBNCompatibilityException {
        return evaluatesTo(A);
    }

    @Override
    public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
        int a1 = -1, a2 = -1;

        if (arg1 instanceof ProbFormAtom)
            a1 = ((ProbFormAtom) arg1).evaluatesTo(A);
        else if (arg1 instanceof Integer) {
            a1 = (int) arg1;
        }
        if (arg2 instanceof ProbFormAtom)
            a2 = ((ProbFormAtom) arg2).evaluatesTo(A);
        else if (arg1 instanceof Integer) {
            a2 = (int) arg2;
        }

        if (a1 == -1 || a2 == -1)
            return -1;

        if (a1 == a2)
            return 1;
        else
            return 0;
    }

    // @Override
//    public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
//        if (!isGround())
//            return -1;
//        boolean tv = (Integer.parseInt(arg1) == Integer.parseInt(arg2));
//        if ((tv && sign)||(!tv && !sign))
//            return 1;
//        else
//            return 0;
//    }

    @Override
    public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue,boolean usealias) {
        if (usealias && this.getAlias() != null)
            return this.getAlias();
        return "[" + arg1 + "=" + arg2 + "]";
    }

    @Override
    public ProbForm conditionEvidence(RelStruc A, OneStrucData inst)
            throws RBNCompatibilityException {
        Object newArg1 = new Object(), newArg2 = new Object();

        if (arg1 instanceof ProbFormAtom) {
            newArg1 = ((ProbFormAtom) arg1).conditionEvidence(A, inst);
        } else if (arg1 instanceof Integer) {
            newArg1 = arg1;
        }

        if (arg2 instanceof ProbFormAtom) {
            newArg2 = ((ProbFormAtom) arg2).conditionEvidence(A, inst);
        } else if (arg2 instanceof Integer) {
            newArg2 = arg2;
        }

        return new ProbFormBoolAtomEquality(newArg1, newArg2, this.sign);
    }

    public Object[] evaluate(RelStruc A,
                             OneStrucData inst,
                             String[] vars,
                             int[] tuple,
                             boolean useCurrentCvals,
                             // String[] numrelparameters,
                             boolean useCurrentPvals,
                             Hashtable<Rel,GroundAtomList> mapatoms,
                             boolean useCurrentMvals,
                             Hashtable<String,Object[]> evaluated,
                             Hashtable<String,Integer> params,
                             int returntype,
                             boolean valonly,
                             Profiler profiler)
    {
//		if (!valonly)
//			System.out.println("Warning: trying to evaluate gradient for Boolean ProbForm" + this.makeKey(A));
        Object[] result = new Object[2];

        if (returntype == ProbForm.RETURN_SPARSE)
            result[1] = new Hashtable<String,Double>();
        else result[1] = new double[0];

        RBNpackage.ProbFormBoolAtomEquality thissubstituted = (RBNpackage.ProbFormBoolAtomEquality)this.substitute(vars,tuple);
        if (!thissubstituted.isGround())
            throw new IllegalArgumentException("Attempt to evaluate non-ground equality");

        double a1=0,a2=0;
        if (arg1 instanceof ProbFormAtom) {
        	a1= (double) ((ProbFormAtom) thissubstituted.arg1).evaluate(A, inst, vars, 
            		tuple, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, 
            		params, returntype, valonly, profiler)[0];
        	if (Double.isNaN(a1)) {
        		result[0]=Double.NaN;
        		return result;
        	}
        } else if (arg1 instanceof Integer) {
            a1 = (Integer)arg1;
        }

        if (arg2 instanceof ProbFormAtom) {
        	a2= (double) ((ProbFormAtom) thissubstituted.arg2).evaluate(A, inst, vars, 
            		tuple, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, 
            		params, returntype, valonly, profiler)[0];
        	if (Double.isNaN(a2)) {
        		result[0]=Double.NaN;
        		return result;
        	}
        } else if (arg2 instanceof Integer) {
            a2 = (Integer)arg2;
        }

 
        if (a1 == a2) result[0] = 1.0;
        else result[0]=0.0;
        return result;
    }


    @Override
    public Double evalSample(RelStruc A,
                             Hashtable<String, PFNetworkNode> atomhasht,
                             OneStrucData inst,
                             Hashtable<String,Double> evaluated,
                             long[] timers)
            throws RBNCompatibilityException {

        String key = null;

        if (evaluated != null) {
            key = this.makeKey(A);
            Double d = evaluated.get(key);
            if (d!=null) {
                return d;
            }
        }

        Integer a1 = null;
        Integer a2 = null;
        if (arg1 instanceof ProbFormAtom) {
            a1 = (Integer) ((ProbFormAtom) arg1).evalSample(A, atomhasht, inst, evaluated, timers).intValue();
        } else if (arg1 instanceof Integer) {
            a1 = (Integer) arg1;
        }

        if (arg2 instanceof ProbFormAtom) {
            a2 = (Integer) ((ProbFormAtom) arg2).evalSample(A, atomhasht, inst, evaluated, timers).intValue();
        } else if (arg2 instanceof Integer) {
            a2 = (Integer) arg2;
        }

        if (a1.equals(a2))
            return 1.0;
        else
            return 0.0;
    }

    @Override
    public String[] freevars() {
        String[] a1 = null;
        String[] a2 = null;
        if (arg1 instanceof ProbFormAtom)
            a1 = ((ProbFormAtom) arg1).freevars();
        if (arg2 instanceof ProbFormAtom) {
            a2 = ((ProbFormAtom) arg2).freevars();
        }
        return rbnutilities.arraymerge(a1, a2);
    }

//	@Override
//	public Vector<GroundAtom> makeParentVec(RelStruc A)
//			throws RBNCompatibilityException {
//		return new Vector<GroundAtom>();
//	}

    @Override
    public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
            throws RBNCompatibilityException {

        Vector<GroundAtom> result = new Vector<GroundAtom>();
        GroundAtom par1=null,par2=null;
        
        if (arg1 instanceof ProbFormAtom) {
        	par1=((ProbFormAtom)arg1).atom();
            result.add(par1);
        }
        if (arg2 instanceof ProbFormAtom) {
        	par2 = ((ProbFormAtom)arg2).atom();
        	if (par1==null || (par1 != null && !par1.equals(par2)))
        		result.add(par2);
        }
        return result;
    }

    @Override
    public ProbForm sEval(RelStruc A) throws RBNCompatibilityException {

        Object a1=null, a2=null;
        if (arg1 instanceof ProbFormAtom) {
            a1 = ((ProbFormAtom) arg1).sEval(A);
            if (a1 instanceof ProbFormConstant)
                a1 = (Integer) (int) ((ProbFormConstant) a1).getCval();
        } else if (arg1 instanceof Integer) {
            a1 = arg1;
        }

        if (arg2 instanceof ProbFormAtom) {
            a2 = ((ProbFormAtom) arg2).sEval(A);
            if (a2 instanceof ProbFormConstant)
                a2 = (Integer) (int) ((ProbFormConstant) a2).getCval();
        } else if (arg2 instanceof Integer) {
            a2 = arg2;
        }

        return new ProbFormBoolAtomEquality(a1, a2, this.sign);
    }

    @Override
    public ProbForm substitute(String[] vars, int[] args) {
        if (vars.length != args.length)
            System.out.println("ProbFormBoolAtomEquality.substitute: vars: " + rbnutilities.arrayToString(vars) + "   args: " + rbnutilities.arrayToString(args));

        Object a1=null, a2=null;
        if (arg1 instanceof ProbFormAtom)
            a1 = ((ProbFormAtom) arg1).substitute(vars, args);
        else if (arg1 instanceof Integer) {
            a1 = arg1;
        }
        if (arg2 instanceof ProbFormAtom)
            a2 = ((ProbFormAtom) arg2).substitute(vars, args);
        else if (arg2 instanceof Integer) {
            a2 = arg2;
        }

        RBNpackage.ProbFormBoolAtomEquality result = new RBNpackage.ProbFormBoolAtomEquality(a1, a2, sign);
        if (this.alias != null)
            result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
        return result;
    }

    @Override
    public ProbForm substitute(String[] vars, String[] args) {
        if (vars.length != args.length)
            System.out.println("ProbFormBoolAtomEquality.substitute: vars: " + rbnutilities.arrayToString(vars) + "   args: " + rbnutilities.arrayToString(args));

        Object a1=null, a2=null;
        if (arg1 instanceof ProbFormAtom)
            a1 = ((ProbFormAtom) arg1).substitute(vars, args);
        else if (arg1 instanceof Integer) {
            a1 = arg1;
        }
        if (arg2 instanceof ProbFormAtom)
            a2 = ((ProbFormAtom) arg2).substitute(vars, args);
        else if (arg2 instanceof Integer) {
            a2 = arg2;
        }

        RBNpackage.ProbFormBoolAtomEquality result = new RBNpackage.ProbFormBoolAtomEquality(a1, a2, sign);
        if (this.alias != null)
            result.setAlias((ProbFormAtom)this.alias.substitute(vars, args));
        return result;

    }

    public Object arg1(){
        return arg1;
    }

    public Object arg2(){
        return arg2;
    }

    private boolean isGroundComponent(Object o) {
    	return (o instanceof ProbFormAtom && ((ProbFormAtom)o).isGround()  || o instanceof Integer);
    }
    
    private boolean isGround(){
    	return (isGroundComponent(arg1) && isGroundComponent(arg2));
    }

    public ProbForm toStandardPF(boolean recursive){
        return this;
    }

    public Object[] args(){
        Object[] result = new Object[2];
        result[0]= arg1;
        result[1]= arg2;
        return result;
    }

    public RBNpackage.ProbFormBoolAtomEquality clone(){
        return new RBNpackage.ProbFormBoolAtomEquality(arg1, arg2, sign);
    }

    public void updateSig(Signature s){
    }

    public TreeSet<Rel> parentRels(){
        return new TreeSet<Rel>();
    }

    public TreeSet<Rel> parentRels(TreeSet<String> processed){
        return new TreeSet<Rel>();
    }
}
