package RBNpackage;

import PyManager.GnnPy;
import PyManager.TorchInputRels;
import PyManager.TorchInputSpecs;
import PyManager.TorchModelWrapper;
import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;

import java.io.File;
import java.util.*;

public class CatGnn extends CPModel {
    // the order of attributes need to be respected! this order will be used for the gnn encoding
    private ArrayList<ArrayList<Rel>> input_attr;
    private ArrayList edge_attr;
    private String argument;

    // true if we use one-hot encoding for the features representation
    private boolean oneHotEncoding;
    private GnnPy gnnPy;
    // each gnn will have an id that identify the model
    private String gnnId;
    String configModelPath;
    List<TorchInputSpecs> gnnInputs;
    List<TorchInputRels> gnnCombinedClauses;
    List<TorchInputRels> gnnGroundCombinedClauses;

    // if is set to true, means that the GNN is for categorical output, if false is boolean
    private boolean categorical;
    private int numvals;
    private static boolean isInitialized = false;
    // this variable is used to set the inference for node or graph classification. Keyword: "node" or "graph"
    private String gnn_inference;
    private int numLayers;
    private TorchModelWrapper torchModel;
    private Vector<String> freeVals;

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
    public CatGnn(String configModelPath, Vector<String> freeVals, int numVals, List<TorchInputSpecs> inputs, List<TorchInputRels> combinedClauses, boolean withGnnPy) {
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
        this.gnnCombinedClauses = combinedClauses;

        this.oneHotEncoding = true; // this for now it is always true, later we need to add this to the RBN specification

        if (withGnnPy)
            this.gnnPy = new GnnPy(this, f.getParent());

        isInitialized = false;
    }
    public CatGnn(String argument, GnnPy gnnpy) {
        this.argument = argument;
        this.gnnPy = gnnpy;
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
    public ProbForm conditionEvidence(RelStruc A, OneStrucData inst) throws RBNCompatibilityException {
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

        // exhaustive check if all the elements have a value before evaluate the gnn
        for (Rel parent: parentRels()) {
            try {
                int[][] alltuple = A.allTypedTuples(parent.getTypes());
                for (int[] tuple2: alltuple) {
                    int val = inst.truthValueOf(parent, tuple2);
                    // if a tuple still does not have a value, return the NaN array
                    if (val==-1) {
                        Object[] result = new Object[2];
                        result[0] = new double[this.numvals()];
                        Arrays.fill((double[]) result[0], Double.NaN);
                        return result;
                    }
                }
            } catch (RBNIllegalArgumentException e) {
                throw new RuntimeException(e);
            }
        }

        Object[] res = gnnPy.evaluate_gnnHetero(A, inst, this, valonly);
        if (this.numvals() == 1) {
            double[] trueProb = (double[]) res[0];
            double[] resultArray =  new double[] {1-trueProb[0],trueProb[0]};
            res[0] = resultArray;
        }
        return res;
    }

    @Override
    public double[] evalSample(RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst, Hashtable<String,double[]> evaluated, long[] timers) throws RBNCompatibilityException {
        if (!isInitialized) {
            this.gnnPy.initGnnData(this, A, inst);
            isInitialized = true;
        }
        double[] resGnn = gnnPy.evalSample_gnn(this, A, atomhasht, inst);
        if (this.numvals() == 1) {
            double[] resultArray =  new double[] {1-resGnn[0],resGnn[0]};
            return resultArray;
        }
        return resGnn;
    }

    @Override
    public String[] freevars() {
        System.out.println("freevars code");
        return rbnutilities.NonIntOnly(new String[]{this.argument}); // convert
    }

    @Override
    public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone) throws RBNCompatibilityException {
        Vector result = new Vector();
        for (TorchInputRels inps: gnnGroundCombinedClauses) {

            ProbForm nextprobform;

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
    public boolean multlinOnly() {
        System.out.println("multlinOnly code");
        return true;
    }

    @Override
    public String[] parameters() {
        System.out.println("parameters code");
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
        List<TorchInputRels> newgnnInputs = new ArrayList<>();
        for (TorchInputRels torchInput: gnnCombinedClauses) {
            TorchInputRels newnewInput = torchInput.substitute(vars, args);
            newgnnInputs.add(newnewInput);
        }

        CatGnn result = new CatGnn(this.configModelPath, this.freeVals, this.numvals, this.gnnInputs, this.gnnCombinedClauses, false);
        result.gnnGroundCombinedClauses = newgnnInputs;
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
        System.out.println("substitute code 2");
        return null;
    }

    @Override
    public void setCvals(String paramname, double val) {
        System.out.println("setCvals code");
    }

    @Override
    public TreeSet<Rel> parentRels() {
//        System.out.println("parentRels code 1");
        TreeSet<Rel> result = new TreeSet<Rel>();
        for (TorchInputRels inps: gnnCombinedClauses) {
            result.addAll(inps.parentRels());
        }
        return result;
    }

    @Override
    public TreeSet<Rel> parentRels(TreeSet<String> processed) {
        System.out.println("parentRels code 2");
        TreeSet<Rel> result = new TreeSet<Rel>();
        assert !processed.isEmpty(); // when it is used?
        for (TorchInputRels inps: gnnCombinedClauses) {
            result.addAll(inps.parentRels());
        }
        return result;
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

    public List<TorchInputRels> getGnnGroundCombinedClauses() {
        return gnnGroundCombinedClauses;
    }
}
