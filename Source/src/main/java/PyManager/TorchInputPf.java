package PyManager;
import RBNExceptions.RBNCompatibilityException;
import RBNLearning.Profiler;
import RBNgui.Primula;
import RBNpackage.*;
import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;
import RBNutilities.rbnutilities;

import java.util.*;

public class TorchInputPf {

    private CPModel pfargs[];
    private CPModel pfargsNode[];
    private CPModel pfargsEdge[];
    private CPModel pfargsEdgeAttr[];
    private ArgTerm quantvars[]; // TODO quantvars should be ArgTerm
    private ProbFormBool cconstr;
    // TODO ADD LAYER COMBINE

    public CPModel[] getPfargs() {
        return pfargs;
    }

    public ArgTerm[] getQuantvars() { return quantvars; }

    public ProbFormBool getCconstr() {
        return cconstr;
    }

    public TorchInputPf(CPModel[] pfa,
                        ArgTerm[] qvars,
                        ProbFormBool cc) throws IllegalArgumentException
    {
        pfargs = pfa;
        quantvars = qvars;
        cconstr = cc;
        pfargsNode = null;
        pfargsEdge = null;
        pfargsEdgeAttr = null;
    }

    public TorchInputPf(CPModel[] pfa,
                        CPModel[] pfaNode,
                        CPModel[] pfaEdge,
                        CPModel[] pfaEdgeAttr,
                        ArgTerm[] qvars,
                        ProbFormBool cc) throws IllegalArgumentException
    {
        pfargs = pfa;
        pfargsNode = pfaNode;
        pfargsEdge = pfaEdge;
        pfargsEdgeAttr = pfaEdgeAttr;
        quantvars = qvars;
        cconstr = cc;
    }

    public ArgTerm[] freevars()
    {
        ArgTerm result[]={};
        // first collect all the free variables from the pfargs formulas
        for (int i = 0 ; i<pfargs.length ; i++)
            result = rbnutilities.arraymerge(result,pfargs[i].freevars());
        // add the variables in the constraint:
        result = rbnutilities.arraymerge(result,cconstr.freevars());
        // subtract the variables in quantvars
        result = (ArgTerm[]) rbnutilities.arraysubstraction(result,quantvars);
        return result;
    }

    public boolean multlinOnly(){
        boolean result = true;
        for (int i=0;i<pfargs.length;i++)
            if (!pfargs[i].multlinOnly())
                result = false;
        return result;
    }

    public TorchInputPf substitute(String[] vars, int[] args)
    {
        TorchInputPf result;
        ProbFormBool subcconstr = null;
        /* Construct new substitution arguments by
         * eliminating the variables that appear in
         * quantvars and their associated
         * substitution values from vars and args
         */
        String[] subsvars;
        subsvars = rbnutilities.arraysubstraction(vars, rbnutilities.getVarsFromArgs(quantvars));
        int[] subsargs = rbnutilities.CorrArraySubstraction(subsvars,vars,args);

        // Perform substitution on pfargs
        CPModel[]  subpfargs = new CPModel[pfargs.length];
        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=pfargs[i].substitute(subsvars,subsargs);

        // just copy the results in order for the node, edges and edge attributes
        int idxShared = 0;
        CPModel[]  subpfargsNode = new CPModel[pfargsNode.length];
        for (int i = 0; i<pfargsNode.length; i++, idxShared++)
            subpfargsNode[i]=subpfargs[i];
        CPModel[]  subpfargsEdge = new CPModel[pfargsEdge.length];
        for (int i = 0; i<pfargsEdge.length; i++, idxShared++)
            subpfargsEdge[i]=subpfargs[idxShared];
        CPModel[]  subpfargsEdgeAttr = new CPModel[pfargsEdgeAttr.length];
        for (int i = 0; i<pfargsEdgeAttr.length; i++, idxShared++)
            subpfargsEdgeAttr[i]=subpfargs[idxShared];

        //Perform substitution on cconstr
        subcconstr = (ProbFormBool)cconstr.substitute(vars,args);

        result = new TorchInputPf(subpfargs, subpfargsNode, subpfargsEdge, subpfargsEdgeAttr, quantvars,subcconstr);
        return result;
    }

    // same code taken from ProbFormCombFunc
    public TorchInputPf substitute(String[] vars, String[] args)
    {
        TorchInputPf result;
        CPModel[]  subpfargs = new CPModel[pfargs.length];
        ProbFormBool subcconstr = null;

        // Rename all the variables bound
        // by combination function
        ArgTerm[] freev = freevars();
        Vector<String> reservedvec = new Vector<>();
        for (int i = 0;i<vars.length;i++)
            reservedvec.add(vars[i]);
        for (int i = 0;i<args.length;i++)
            reservedvec.add(args[i]);
        for (int i = 0;i<freev.length;i++) {
            for (String vs : freev[i].getVariables())
                reservedvec.add(vs);
        }
        String[] reserved = reservedvec.toArray(new String[0]);

        String[] newquantvars = rbnutilities.NewVariables(reserved, rbnutilities.getVarsFromArgs(quantvars).length);
        ArgTerm[] newquantvarsAsArgTerm = new ArgTerm[newquantvars.length];
        for (int i = 0; i<newquantvars.length; i++)
            newquantvarsAsArgTerm[i] = new VarTerm(newquantvars[i]);

        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=pfargs[i].substitute(rbnutilities.getVarsFromArgs(quantvars),newquantvars);

        // just copy the results in order for the node, edges and edge attributes
        int idxShared = 0;
        CPModel[]  subpfargsNode = new CPModel[pfargsNode.length];
        for (int i = 0; i<pfargsNode.length; i++, idxShared++)
            subpfargsNode[i]=subpfargs[i];
        CPModel[]  subpfargsEdge = new CPModel[pfargsEdge.length];
        for (int i = 0; i<pfargsEdge.length; i++, idxShared++)
            subpfargsEdge[i]=subpfargs[idxShared];
        CPModel[]  subpfargsEdgeAttr = new CPModel[pfargsEdgeAttr.length];
        for (int i = 0; i<pfargsEdgeAttr.length; i++, idxShared++)
            subpfargsEdgeAttr[i]=subpfargs[idxShared];

        subcconstr = (ProbFormBool)cconstr.substitute(quantvars, newquantvarsAsArgTerm);

        // Now perform the original substitution
        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=subpfargs[i].substitute(vars,args);

        subcconstr = (ProbFormBool)subcconstr.substitute(vars,args);
        return new TorchInputPf(subpfargs, subpfargsNode, subpfargsEdge, subpfargsEdgeAttr, newquantvarsAsArgTerm, subcconstr);
    }

    public TorchInputPf substitute(String[] vars, ArgTerm[] args)
    {
        TorchInputPf result;
        CPModel[]  subpfargs = new CPModel[pfargs.length];
        ProbFormBool subcconstr = null;

        ArgTerm[] freev = freevars();
        Vector<String> reservedvec = new Vector<>();
        for (int i = 0;i<vars.length;i++)
            reservedvec.add(vars[i]);
        for (int i = 0;i<args.length;i++) {
            for (String vs : args[i].getVariables())
                reservedvec.add(vs);
        }
        for (int i = 0;i<freev.length;i++) {
            for (String vs : freev[i].getVariables())
                reservedvec.add(vs);
        }
        String[] reserved = reservedvec.toArray(new String[0]);

        String[] newquantvars = rbnutilities.NewVariables(reserved, rbnutilities.getVarsFromArgs(quantvars).length);
        ArgTerm[] newquantvarsAsArgTerm = new ArgTerm[newquantvars.length];
        for (int i = 0; i<newquantvars.length; i++)
            newquantvarsAsArgTerm[i] = new VarTerm(newquantvars[i]);

        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=pfargs[i].substitute(rbnutilities.getVarsFromArgs(quantvars),newquantvars);

        subcconstr = (ProbFormBool)cconstr.substitute(quantvars, newquantvarsAsArgTerm);

        // Now perform the original substitution
        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=subpfargs[i].substitute(vars,args);

        // just copy the results in order for the node, edges and edge attributes
        int idxShared = 0;
        CPModel[]  subpfargsNode = new CPModel[pfargsNode.length];
        for (int i = 0; i<pfargsNode.length; i++, idxShared++)
            subpfargsNode[i]=subpfargs[i];
        CPModel[]  subpfargsEdge = new CPModel[pfargsEdge.length];
        for (int i = 0; i<pfargsEdge.length; i++, idxShared++)
            subpfargsEdge[i]=subpfargs[idxShared];
        CPModel[]  subpfargsEdgeAttr = new CPModel[pfargsEdgeAttr.length];
        for (int i = 0; i<pfargsEdgeAttr.length; i++, idxShared++)
            subpfargsEdgeAttr[i]=subpfargs[idxShared];

        //Perform substitution on cconstr
        subcconstr = (ProbFormBool)subcconstr.substitute(vars,args);

        return new TorchInputPf(subpfargs, subpfargsNode, subpfargsEdge, subpfargsEdgeAttr, newquantvarsAsArgTerm, subcconstr);
    }

    public TorchInputPf substitute(ArgTerm[] vars, int[] args) {
        ArgTerm[] subsvars = rbnutilities.arraysubstraction(vars, rbnutilities.getVarsFromArgs(quantvars));
        int[] subsargs = rbnutilities.CorrArraySubstraction(rbnutilities.getVarsFromArgs(subsvars), rbnutilities.getVarsFromArgs(vars), args);

        // Perform substitution on pfargs
        CPModel[]  subpfargs = new CPModel[pfargs.length];
        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=pfargs[i].substitute(subsvars,subsargs);
        //Perform substitution on cconstr

        // just copy the results in order for the node, edges and edge attributes
        int idxShared = 0;
        CPModel[]  subpfargsNode = new CPModel[pfargsNode.length];
        for (int i = 0; i<pfargsNode.length; i++, idxShared++)
            subpfargsNode[i]=subpfargs[i];
        CPModel[]  subpfargsEdge = new CPModel[pfargsEdge.length];
        for (int i = 0; i<pfargsEdge.length; i++, idxShared++)
            subpfargsEdge[i]=subpfargs[idxShared];
        CPModel[]  subpfargsEdgeAttr = new CPModel[pfargsEdgeAttr.length];
        for (int i = 0; i<pfargsEdgeAttr.length; i++, idxShared++)
            subpfargsEdgeAttr[i]=subpfargs[idxShared];

        ProbFormBool subcconstr = (ProbFormBool)cconstr.substitute(vars,args);
        return new TorchInputPf(subpfargs, subpfargsNode, subpfargsEdge, subpfargsEdgeAttr, quantvars, subcconstr);
    }

    public TorchInputPf substitute(ArgTerm[] vars, ArgTerm[] args)
    {
        TorchInputPf result;
        CPModel[]  subpfargs = new CPModel[pfargs.length];
        ProbFormBool subcconstr = null;

        ArgTerm[] freev = freevars();
        ArgTerm[] reserved = new ArgTerm[vars.length+args.length+freev.length];
        for (int i = 0;i<vars.length;i++)
            reserved[i]=vars[i];
        for (int i = 0;i<args.length;i++)
            reserved[vars.length+i]=args[i];
        for (int i = 0;i<freev.length;i++)
            reserved[vars.length+args.length+i]= freev[i];

        ArgTerm[] newquantvars = rbnutilities.NewVariables(reserved, rbnutilities.getVarsFromArgs(quantvars).length);

        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=pfargs[i].substitute(rbnutilities.getVarsFromArgs(quantvars), newquantvars);

        subcconstr = (ProbFormBool)cconstr.substitute(quantvars, newquantvars);

        // Now perform the original substitution
        for (int i = 0; i<pfargs.length; i++)
            subpfargs[i]=subpfargs[i].substitute(vars,args);

        int idxShared = 0;
        CPModel[]  subpfargsNode = new CPModel[pfargsNode.length];
        for (int i = 0; i<pfargsNode.length; i++, idxShared++)
            subpfargsNode[i]=subpfargs[i];
        CPModel[]  subpfargsEdge = new CPModel[pfargsEdge.length];
        for (int i = 0; i<pfargsEdge.length; i++, idxShared++)
            subpfargsEdge[i]=subpfargs[idxShared];
        CPModel[]  subpfargsEdgeAttr = new CPModel[pfargsEdgeAttr.length];
        for (int i = 0; i<pfargsEdgeAttr.length; i++, idxShared++)
            subpfargsEdgeAttr[i]=subpfargs[idxShared];

        subcconstr = (ProbFormBool)subcconstr.substitute(vars,args);
        return new TorchInputPf(subpfargs, subpfargsNode, subpfargsEdge, subpfargsEdgeAttr, newquantvars, subcconstr);
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

//    public String makeKey(ArgTerm[] vars, int[] args, Boolean nosub){
//        if (nosub) {
//            return this.asString(Primula.CLASSICSYNTAX, 0, null, false, true);
//        }
//        else return this.substitute(vars,args).asString(Primula.CLASSICSYNTAX, 0, null, false, true);
//    }

    public String makeKey(ArgTerm[] vars, int[] args, Boolean nosub) {
        int hash;

        if (nosub) {
            hash = computeHash(this);
        } else {
            TorchInputPf sub = this.substitute(vars, args);
            hash = computeHash(sub);
        }

        return Integer.toHexString(hash);
    }

    private int computeHash(TorchInputPf obj) {
        int result = 1;

        result = 31 * result + Arrays.hashCode(obj.pfargs);
        result = 31 * result + Arrays.hashCode(obj.quantvars);
        result = 31 * result + Objects.hashCode(obj.cconstr);

        result = 31 * result + Arrays.hashCode(obj.pfargsNode);
        result = 31 * result + Arrays.hashCode(obj.pfargsEdge);
        result = 31 * result + Arrays.hashCode(obj.pfargsEdgeAttr);

        return result;
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
    public int[][] tuplesSatisfyingCConstr(RelStruc A, ArgTerm[] vars, int[] tuple)
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
                             ArgTerm[] vars,
                             int[] tuple,
                             int gradindx,
                             boolean useCurrentCvals,
                             boolean useCurrentPvals,
                             HashMap<Rel,GroundAtomList> mapatoms,
                             boolean useCurrentMvals,
                             HashMap<String,Object[]> evaluated,
                             HashMap<String,Integer> params,
                             int returntype,
                             boolean valonly,
                             Profiler profiler)
            throws RBNCompatibilityException {

        String key = "";

//        if (evaluated != null) {
//            key = this.makeKey(vars, tuple, false);
//            Object[] d = evaluated.get(key);
//            if (d != null) {
//                return d;
//            }
//        }

        TorchInputPf subspfcf = this.substitute(vars, tuple);

//        TorchInputPf subspfcf2 = this.substitute(new String[]{"v1"}, tuple);
//        System.out.println(subspfcf2);


        int[][] subslist = tuplesSatisfyingCConstr(A, vars, tuple);

        /* Initialize array of arguments for combination function */
        Vector<Object[]> combargs = new Vector<Object[]>();

        /* Evaluate the probability formulas in pfargs and
         * enter results into combargs
         */

        for (int i = 0; i < subspfcf.pfargs.length; i++) {
            for (int j = 0; j < subslist.length; j++) {
                combargs.add(subspfcf.pfargs[i].evaluate(A,
                        inst,
                        quantvars,
                        subslist[j],
                        gradindx,
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
        result[1] = null;

        double[] vals = new double[combargs.size()];
        int i = 0;
        // if some values have NaN, return all NaN
        boolean hasNaN = false;
        for (Object[] d : combargs) {
            vals[i] = (Double) d[0];
            if (Double.isNaN(vals[i])) {
                hasNaN = true;
                break;
            }
            i++;
        }
        if (hasNaN) {
//            double[] nanArray = new double[vals.length];
//            for (int j = 0; j < nanArray.length; j++) {
//                nanArray[j] = Double.NaN;
//            }
            result[0] = Double.NaN;
        } else {
            result[0] = vals;
        }
        if (evaluated != null && !hasNaN) {
            evaluated.put(key, result);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TorchInputRels{");
        sb.append("pfargs=").append(Arrays.toString(pfargs));
        sb.append(", pfargsNode=").append(Arrays.toString(pfargsNode));
        sb.append(", pfargsEdge=").append(Arrays.toString(pfargsEdge));
        sb.append(", pfargsEdgeAttr=").append(Arrays.toString(pfargsEdgeAttr));
        sb.append(", quantvars=").append(Arrays.toString(quantvars));
        sb.append(", cconstr=").append(cconstr == null ? "null" : cconstr.toString());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(pfargs);
        result = 31 * result + Arrays.hashCode(pfargsNode);
        result = 31 * result + Arrays.hashCode(pfargsEdge);
        result = 31 * result + Arrays.hashCode(pfargsEdgeAttr);
        result = 31 * result + Arrays.hashCode(quantvars);
        result = 31 * result + Objects.hashCode(cconstr);
        return result;
    }

    public int numPFargs(){
        return pfargs.length;
    }

    public CPModel probformAt(int i){
        return pfargs[i];
    }

    public void setPfargs(CPModel[] pfargs) {
        this.pfargs = pfargs;
    }

    public void setQuantvars(ArgTerm[] quantvars) {
        this.quantvars = quantvars;
    }

    public void setCconstr(ProbFormBool cconstr) {
        this.cconstr = cconstr;
    }

    public CPModel[] getPfargsNode() {
        return pfargsNode;
    }

    public CPModel[] getPfargsEdge() {
        return pfargsEdge;
    }

    public CPModel[] getPfargsEdgeAttr() {
        return pfargsEdgeAttr;
    }

    public CPModel getPfargsNodeAt(int i) {
        return pfargsNode[i];
    }

    public CPModel getPfargsEdgeAt(int i) {
        return pfargsEdge[i];
    }

    public CPModel getPfargsEdgeAttrAt(int i) {
        return pfargsEdgeAttr[i];
    }
}