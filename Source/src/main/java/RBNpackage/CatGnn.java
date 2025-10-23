package RBNpackage;

import PyManager.*;
import RBNExceptions.RBNCompatibilityException;
import RBNLearning.Gradient_Array;
import RBNLearning.Gradient_TreeMap;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;

import java.io.File;
import java.util.*;

public class CatGnn extends CPModel {
    // the order of attributes need to be respected! this order will be used for the gnn encoding
    private ArrayList<ArrayList<Rel>> input_attr;
    private ArrayList edge_attr;
    String argument;

    // true if we use one-hot encoding for the features representation
    private boolean oneHotEncoding;
    private GnnPy gnnPy;
    // each gnn will have an id that identify the model
    private String gnnId;
    String configModelPath;
    List<TorchInputSpecs> gnnInputs;
//    List<List<TorchInputRels>> gnnCombinedClauses;
//    List<List<TorchInputRels>> gnnGroundCombinedClauses;
//    List<TorchInputRels> gnnCombinedClausesFlatted;
//    List<TorchInputRels> gnnGroundCombinedClausesFlatted;

    TypedTorchPf typedTorchPf;
    TypedTorchPf groundTypedTorchPf;

    // if is set to true, means that the GNN is for categorical output, if false is boolean
    private boolean categorical;
    private int numvals;
    private static boolean isInitialized = false;
    // this variable is used to set the inference for node or graph classification. Keyword: "node" or "graph"
    private String gnn_inference;
    private int numLayers;
    private TorchModelWrapper torchModel;
    Vector<String> freeVals;
    Vector<String> outTypes;

    public CatGnn(String argument, String gnnId, int numLayers, int numvals, ArrayList input_attr, ArrayList edge_attr, String gnn_inference, boolean oneHotEncoding) {
        this.argument = argument;
        this.gnnId = gnnId;
        this.categorical = true;
        this.numvals = numvals;
        this.numLayers = numLayers;
        this.input_attr = input_attr;
        this.edge_attr = edge_attr;
        this.oneHotEncoding = oneHotEncoding;
        this.gnn_inference = gnn_inference;
        isInitialized = false;
    }

    public CatGnn(String configModelPath, Vector<String> freeVals, int numVals, List<TorchInputSpecs> inputs, TypedTorchPf typedTorchPf, Vector<String> outTypes, boolean withGnnPy) {
        File f = new File(configModelPath);
        // get a file name without extension
        int lastIndexOfDot = f.getName().lastIndexOf('.');
        if (lastIndexOfDot == -1)
            this.gnnId = f.getName(); // No extension found
        else
            this.gnnId = f.getName().substring(0, lastIndexOfDot);

        this.argument = "";
        this.freeVals = freeVals;
        if (!freeVals.isEmpty())
            this.argument = freeVals.get(0);

        this.categorical = true;
        this.configModelPath = f.getParent();
        this.numvals = numVals;
        this.gnnInputs = inputs;
        this.typedTorchPf = typedTorchPf;
        this.outTypes = outTypes;

        this.oneHotEncoding = true; // this for now it is always true, later we need to add this to the RBN specification

        if (withGnnPy)
            this.gnnPy = new GnnPy(this, f.getParent());

        isInitialized = false;
    }

    @Override
    public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias) {
        StringBuilder sb = new StringBuilder();
        sb.append("GNN("+this.argument+")=");
        sb.append("[");
        sb.append(gnnPy.getTorchModel().toString()+", ");
        if (gnnInputs != null && !gnnInputs.isEmpty()) {
            for (TorchInputSpecs pair : gnnInputs)
                sb.append(pair.toString()).append(", ");
            sb.setLength(sb.length() - 2);
        } else
            sb.append("null");
        sb.append("]");
        return sb.toString();
    }

    @Override
    public CPModel conditionEvidence(RelStruc A, OneStrucData inst) throws RBNCompatibilityException {
        // RAF: This is not required for gnn since it does not have direct dependencies
        System.out.println("conditionEvidence code");
        return null;
    }

    @Override
    public boolean dependsOn(String variable, RelStruc A, OneStrucData data) throws RBNCompatibilityException {
        System.out.println("dependsOn code");
        return false;
    }

    @Override
    public Object[] evaluate(RelStruc A,
                             OneStrucData inst,
                             String[] vars,
                             int[] tuple,
                             int gradindx,
                             boolean useCurrentCvals,
                             boolean useCurrentPvals,
                             Hashtable<Rel, GroundAtomList> mapatoms,
                             boolean useCurrentMvals,
                             Hashtable<String, Object[]> evaluated,
                             Hashtable<String, Integer> params,
                             int returntype,
                             boolean valonly,
                             Profiler profiler)
            throws RBNCompatibilityException {

        // CHECK IF THIS DOES NOT BREAK INFERENCE WITH MAP or MCMC

        for (TorchInputPf inps: groundTypedTorchPf.getCombines()) {
            Object[] res = inps.evaluate(A, inst, vars, tuple, gradindx, useCurrentCvals, useCurrentPvals, mapatoms, useCurrentMvals, evaluated, params, returntype, valonly, profiler);
            // if res[0] contains NaN return res
            if (res[0] instanceof Double) {
                if (Double.isNaN((Double) res[0])) {
                    return res;
                }
            }

        }

        CatGnn subCatGnn = null;
        if (this instanceof CatGnnBool)
            subCatGnn = (CatGnnBool)this.substitute(vars, tuple);
        else
            subCatGnn = (CatGnn)this.substitute(vars, tuple);

        Object[] res = gnnPy.evaluate_gnnHetero(A, inst, subCatGnn, valonly);

        if (subCatGnn instanceof CatGnnBool) {
            double[] trueProb = (double[]) res[0];
            res[0] = trueProb[0];
        }
        if (!(subCatGnn instanceof CatGnnBool) && this.numvals() == 1) {
            double[] trueProb = (double[]) res[0];
            double[] resultArray =  new double[] {1-trueProb[0],trueProb[0]};
            res[0] = resultArray;
        }

        if (!valonly) {
            res[1] = null;
            if (returntype == ProbForm.RETURN_ARRAY)
                res[1] = new Gradient_Array(params);
            else
                res[1] = new Gradient_TreeMap(params);
        }

        return res;
    }

    @Override
    public double[] evalSample(RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst, Hashtable<String,double[]> evaluated, long[] timers) throws RBNCompatibilityException {
        if (!isInitialized) {
            this.gnnPy.initGnnData(this, A, inst);
            isInitialized = true;
        }
        double[] resGnn = gnnPy.evalSample_gnn(this, atomhasht);
        if (this.numvals() == 1) {
            double[] resultArray =  new double[] {1-resGnn[0],resGnn[0]};
            return resultArray;
        }
        return resGnn;
    }

    @Override
    public String[] freevars() {
        System.out.println("freevars code");
//        return rbnutilities.NonIntOnly(new String[]{this.argument}); // convert
        for (TorchInputPf inps: groundTypedTorchPf.getCombines()) {
            String[] res = inps.freevars();
            if (res.length > 0)
                return res;
        }
        return new String[0];
    }

    @Override
    public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone) throws RBNCompatibilityException {
        Vector result = new Vector();
        for (TorchInputPf inps: groundTypedTorchPf.getCombines()) {

            CPModel nextprobform;

            int[][] subslist = A.allTrue(inps.getCconstr(), inps.getQuantvars());

            for (int i = 0; i < inps.getPfargs().length; i++) {
                for (int j = 0; j < subslist.length; j++) {
                    nextprobform = inps.getPfargs()[i].substitute(inps.getQuantvars(), subslist[j]);
                    result = rbnutilities.combineAtomVecs(result, nextprobform.makeParentVec(A, inst, macrosdone));
                }
            }
        }
        return result;
    }

    @Override
    public int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable<String, GroundAtom> atomhasht) throws RBNCompatibilityException {
        return 0;
    }

    @Override
    public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
        return 0;
    }

    @Override
    public boolean multlinOnly() {
        System.out.println("multlinOnly code");
        return true;
    }

    @Override
    public String[] parameters() {
//        System.out.println("parameters code");
        return new String[0];
    }

    /**
     * Returns a ProbForm in which the dependence on A
     * is already pre-evaluated (substitution lists in
     * combination functions, and values of ProbFormSFormula)
     */
    @Override
    public CPModel sEval(RelStruc A) throws RBNCompatibilityException {
//        System.out.println("sEval code");
        return this;
    }

    @Override
    public CPModel substitute(String[] vars, int[] args) {
        TypedTorchPf newpf = this.typedTorchPf.substitute(vars, args);

        CatGnn result;
        if (this instanceof CatGnnBool)
            result = new CatGnnBool(this.configModelPath, this.freeVals, this.gnnInputs, newpf, this.outTypes, false);
        else
            result = new CatGnn(this.configModelPath, this.freeVals, this.numvals, this.gnnInputs, newpf, this.outTypes, false);

        result.groundTypedTorchPf = newpf;
        result.setGnnPy(this.getGnnPy());

        if (vars.length == 0)
            result.argument = Arrays.toString(new String[0]);
        else
            result.argument = rbnutilities.array_substitute(vars, new String[]{argument}, args)[0];

        if (this.alias != null)
            result.setAlias(this.alias.substitute(vars, args));

        return result;
    }

    @Override
    public CPModel substitute(String[] vars, String[] args) {
        TypedTorchPf newpf = this.typedTorchPf.substitute(vars, args);

        CatGnn result;
        if (this instanceof CatGnnBool)
            result = new CatGnnBool(this.configModelPath, this.freeVals, this.gnnInputs, newpf, this.outTypes, false);
        else
            result = new CatGnn(this.configModelPath, this.freeVals, this.numvals, this.gnnInputs, newpf, this.outTypes, false);

        result.groundTypedTorchPf = newpf;
        result.setGnnPy(this.getGnnPy());

        if (vars.length == 0)
            result.argument = Arrays.toString(new String[0]);
        else
            result.argument = rbnutilities.array_substitute(vars, new String[]{argument}, args)[0];

        if (this.alias != null)
            result.setAlias(this.alias.substitute(vars, args));

        return result;
    }

    @Override
    public void setCvals(String paramname, double val) {
        System.out.println("setCvals code");
    }

    @Override
    public TreeSet<Rel> parentRels() {
//        System.out.println("parentRels code 1");
        TreeSet<Rel> result = new TreeSet<Rel>();
        for (TorchInputPf inps: groundTypedTorchPf.getCombines()) {
            result.addAll(inps.parentRels());
        }
        return result;
    }

    @Override
    public TreeSet<Rel> parentRels(TreeSet<String> processed) {
        System.out.println("parentRels code 2");
        TreeSet<Rel> result = new TreeSet<Rel>();
        assert !processed.isEmpty(); // when it is used?
        for (TorchInputPf inps: groundTypedTorchPf.getCombines()) {
            result.addAll(inps.parentRels());
        }
        return result;
    }

    public TypedTorchPf getGroundTypedTorchPf() {
        return groundTypedTorchPf;
    }

    public TypedTorchPf getTypedTorchPf() {
        return typedTorchPf;
    }

    @Override
    public int numvals() {
        return numvals;
    }

    public GnnPy getGnnPy() {
        return gnnPy;
    }

    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }

    public List<TorchInputSpecs> getGnnInputs() {
        return this.gnnInputs;
    }

    public String getArgument() {
        return argument;
    }

    public String getGnnId() {
        return gnnId;
    }

    public boolean isOneHotEncoding() {
        return oneHotEncoding;
    }

    public boolean isBoolean() { return !categorical; }

    public Vector<String> getOutTypes() { return outTypes; }
}
