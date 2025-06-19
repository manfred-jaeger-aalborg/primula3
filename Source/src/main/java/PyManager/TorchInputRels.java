package PyManager;
import RBNExceptions.RBNCompatibilityException;
import RBNLearning.Profiler;
import RBNgui.Primula;
import RBNpackage.*;
import RBNutilities.rbnutilities;

import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Vector;

public class TorchInputRels {

    private ProbForm pfargs[];
    private String quantvars[];
    private ProbFormBool cconstr;

    public ProbForm[] getPfargs() {
        return pfargs;
    }

    public String[] getQuantvars() { return quantvars; }

    public ProbFormBool getCconstr() {
        return cconstr;
    }

    public TorchInputRels(ProbForm[] pfa, String[] qvars, ProbFormBool cc) throws IllegalArgumentException
    {
        pfargs = pfa;
        quantvars = qvars;
        cconstr = cc;
    }

    public  String[] freevars()
    {
        String result[]={};
        // first collect all the free variables from the pfargs formulas
        for (int i = 0 ; i<pfargs.length ; i++)
            result = rbnutilities.arraymerge(result,pfargs[i].freevars());
        // add the variables in the constraint:
        result = rbnutilities.arraymerge(result,cconstr.freevars());
        // subtract the variables in quantvars
        result = rbnutilities.arraysubstraction(result,quantvars);
        return result;
    }

    public boolean multlinOnly(){
        boolean result = true;
        for (int i=0;i<pfargs.length;i++)
            if (!pfargs[i].multlinOnly())
                result = false;
        return result;
    }

    public TorchInputRels substitute(String[] vars, int[] args)
    {
        TorchInputRels result;
        ProbFormBool subcconstr = null;
        /* Construct new substitution arguments by
         * eliminating the variables that appear in
         * quantvars and their associated
         * substitution values from vars and args
         */
        String[] subsvars;
        subsvars = rbnutilities.arraysubstraction(vars,quantvars);
        int[] subsargs = rbnutilities.CorrArraySubstraction(subsvars,vars,args);


        // Perform substitution on pfargs
        ProbForm[]  subpfargs = new ProbForm[pfargs.length];
        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=pfargs[i].substitute(subsvars,subsargs);
        //Perform substitution on cconstr

        subcconstr = (ProbFormBool)cconstr.substitute(vars,args);

        result = new TorchInputRels(subpfargs,quantvars,subcconstr);

        return result;
    }

    public TreeSet<Rel> parentRels(){
        TreeSet<Rel> result = new TreeSet<Rel>();
        for (int i=0;i<pfargs.length;i++)
            result.addAll(pfargs[i].parentRels());
        return result;
    }

    public TreeSet<Rel> parentRels(TreeSet<String> processed){
        String mykey = this.makeKey(null,null,true);
        if (processed.contains(mykey))
            return new TreeSet<Rel>();
        else {
            processed.add(mykey);
            TreeSet<Rel> result = new TreeSet<Rel>();
            for (int i=0;i<pfargs.length;i++)
                result.addAll(pfargs[i].parentRels(processed));
            return result;
        }
    }

    // same code taken from ProbFormCombFunc
    public TorchInputRels substitute(String[] vars, String[] args)
    {
        TorchInputRels result;
        ProbForm[]  subpfargs = new ProbForm[pfargs.length];
        ProbFormBool subcconstr = null;

        // Rename all the variables bound
        // by combination function
        String[] freev = freevars();
        String[] reserved = new String[vars.length+args.length+freev.length];
        for (int i = 0;i<vars.length;i++)
            reserved[i]=vars[i];
        for (int i = 0;i<args.length;i++)
            reserved[vars.length+i]=args[i];
        for (int i = 0;i<freev.length;i++)
            reserved[vars.length+args.length+i]=freev[i];

        String[] newquantvars = rbnutilities.NewVariables(reserved,quantvars.length);

        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=pfargs[i].substitute(quantvars,newquantvars);

        subcconstr = (ProbFormBool)cconstr.substitute(quantvars,newquantvars);

        // Now perform the original substitution
        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=subpfargs[i].substitute(vars,args);

        subcconstr = (ProbFormBool)subcconstr.substitute(vars,args);
        result = new TorchInputRels(subpfargs,newquantvars,subcconstr);

        return result;
    }

    public String makeKey(String[] vars, int[] args, Boolean nosub){
        if (nosub) {
            return this.asString(Primula.CLASSICSYNTAX, 0, null, false, true);
        }
        else return this.substitute(vars,args).asString(Primula.CLASSICSYNTAX, 0, null, false, true);
    }

    public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue,boolean usealias)
    {
        String result="";
        String tabstring = "";
        for (int i=0;i<depth;i++)
            tabstring = tabstring +" ";

        result = "COMBINE " ;
        if (pfargs.length >= 1)
            result = result  + pfargs[0].asString(syntax, depth+8 ,A,paramsAsValue,usealias);
        for (int i = 1; i<pfargs.length; i++)
        {
            result = result + "," +'\n' + tabstring + "        " + pfargs[i].asString(syntax, depth+8 ,A,  paramsAsValue,usealias) ;
        }
        result = result + '\n' + tabstring + "FORALL " +  rbnutilities.arrayToString(quantvars);
        result = result + '\n' + tabstring + "WHERE " + cconstr.asString(Primula.CHERRYSYNTAX,0,A, paramsAsValue,usealias) ;

        return result;
    }


    /** Returns the set of all tuples in A that satisfy the CConstr of this formula
     * after the substituion vars/tuple has been performed
     */
    public int[][] tuplesSatisfyingCConstr(RelStruc A,  String[] vars, int[] tuple)
            throws RBNCompatibilityException {
        ProbFormBool subscc = (ProbFormBool)this.cconstr.substitute(vars,tuple);
        return  A.allTrue(subscc,quantvars);
    }

    /**
     * This function will just return the input features for the model.
     * It is the same code (not all) from ProbFormCombFunc, but here we do not have any comb for torch
     */
    public Object[] evaluate(RelStruc A,
                             OneStrucData inst,
                             String[] vars,
                             int[] tuple,
                             boolean useCurrentCvals,
                             boolean useCurrentPvals,
                             Hashtable<Rel,GroundAtomList> mapatoms,
                             boolean useCurrentMvals,
                             Hashtable<String,Object[]> evaluated,
                             Hashtable<String,Integer> params,
                             int returntype,
                             boolean valonly,
                             Profiler profiler)
            throws RBNCompatibilityException {

        String key = "";

        if (evaluated != null) {
            key = this.makeKey(vars, tuple, false);
            Object[] d = evaluated.get(key);
            if (d != null) {
                return d;
            }
        }

        TorchInputRels subspfcf = (TorchInputRels) this.substitute(vars, tuple);

        int[][] subslist = tuplesSatisfyingCConstr(A, vars, tuple);

        /* Initialize array of arguments for combination function */
        Vector<Object[]> combargs = new Vector<Object[]>();

        /* Evaluate the probability formulas in pfargs and
         * enter results into combargs
         */
        int nextindex;
        double[] nextvalue;

        for (int i = 0; i < subspfcf.pfargs.length; i++) {
            for (int j = 0; j < subslist.length; j++) {
                combargs.add(subspfcf.pfargs[i].evaluate(A,
                        inst,
                        quantvars,
                        subslist[j],
                        useCurrentCvals,
                        useCurrentPvals,
                        mapatoms,
                        useCurrentMvals,
                        evaluated,
                        params,
                        returntype,
                        valonly,
                        profiler));
            }
        }

        Object[] result = new Object[2];

        double[] vals = new double[combargs.size()];
        int i = 0;
        // if some values have NaN, return all NaN
        boolean hasNaN = false;
        for (Object[] d : combargs) {
            vals[i] = (Double) d[0];
            if (Double.isNaN(vals[i])) {
                hasNaN = true;
            }
            i++;
        }
        if (hasNaN) {
            double[] nanArray = new double[vals.length];
            for (int j = 0; j < nanArray.length; j++) {
                nanArray[j] = Double.NaN;
            }
            result[0] = nanArray;
        } else {
            result[0] = vals;
        }
        if (evaluated != null && !hasNaN) {
            evaluated.put(key, result);
        }
        return result;
    }

    public int numPFargs(){
        return pfargs.length;
    }

    public ProbForm probformAt(int i){
        return pfargs[i];
    }

    public void setPfargs(ProbForm[] pfargs) {
        this.pfargs = pfargs;
    }

    public void setQuantvars(String[] quantvars) {
        this.quantvars = quantvars;
    }

    public void setCconstr(ProbFormBool cconstr) {
        this.cconstr = cconstr;
    }
}