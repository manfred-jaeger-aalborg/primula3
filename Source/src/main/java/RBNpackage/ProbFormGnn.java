package RBNpackage;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;

import java.util.*;

/**
 * alpha(a) = COMPUTE WITH
 *      <gnn> {path/of/the/weights}
 * FOR INPUTS
 *      <list of attributes>
 * FORALL b
 *      WITH edge(a, b) <> WITH edge(b, a) <> WITH edge(a, b) || WITH edge(b, a)
 */
public class ProbFormGnn extends ProbForm implements CPMGnn {
    // the order of attributes need to be respected! this order will be used for the gnn encoding
    private Rel gnnattr[];
    private String argument;

    // true if we use one-hot encoding for the features representation
    private boolean oneHotEncoding;

    // the name of the edge relation in the RBN definition (usually "edge")
    private String edge_name;
    private String edge_direction;
    private GnnPy gnnPy;
    // each gnn will have an id that identify the model
    private String idGnn;

    // this variable is used to set the inference for node or graph classification. Keyword: "node" or "graph"
    private String gnn_inference;
    public ProbFormGnn(String argument, String idGnn, Rel[] attr, String edge_name, String edge_direction, boolean oneHotEncoding) {
        this.setEdge_name(edge_name);
        this.setEdge_direction(edge_direction);

        this.argument = argument;
        this.idGnn = idGnn;
        this.gnnattr = attr;
        this.oneHotEncoding = oneHotEncoding;
    }

    public ProbFormGnn(String argument, String idGnn, Rel[] attr, String edge_name, String edge_direction, String gnn_inference, boolean oneHotEncoding) {
        this.setEdge_name(edge_name);
        this.setEdge_direction(edge_direction);

        this.argument = argument;
        this.idGnn = idGnn;
        this.gnnattr = attr;
        this.gnn_inference = gnn_inference;
        this.oneHotEncoding = oneHotEncoding;
    }

    public ProbFormGnn(String argument, GnnPy gnnpy) {
        this.argument = argument;
        this.gnnPy = gnnpy;
    }

    @Override
    public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias) {
        return "gnn("+this.argument+")";
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

        Double[][] res = (Double [][]) gnnPy.evaluate_gnn(A, inst, this, valonly);
        return res[0];
    }

    @Override
    public double[] evalSample(RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst, Hashtable<String,double[]> evaluated, long[] timers) throws RBNCompatibilityException {
        return gnnPy.evalSample_gnn(this, A, atomhasht, inst);
    }

    @Override
    public int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable<String, GroundAtom> atomhasht) throws RBNCompatibilityException {
        System.out.println("evaluatesTo code 1");
        return 0;
    }

    @Override
    public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
        System.out.println("evaluatesTo code 2");
        return 0;
    }

    @Override
    public String[] freevars() {
        System.out.println("freevars code");
        return rbnutilities.NonIntOnly(new String[]{this.argument}); // convert
    }

    public void setFreevars(String freevar) {
        this.argument = freevar;
    }

    public Vector<GroundAtom> makeParentVec(RelStruc A) throws RBNCompatibilityException {
        System.out.println("makeParentVec code 1");
        return makeParentVec(A, new OneStrucData(), null);
    }

    @Override
    public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone) throws RBNCompatibilityException {
//        System.out.println("makeParentVec code 2");
        // use parentRels to obtain the parents and see thorugh the arguments which is one that is not ground and add to result
        Vector<GroundAtom> result = new Vector<GroundAtom>();
        for (Rel parent : this.parentRels()) {
            // return all tuples within a certain type
            try {
                int[][] mat = A.allTypedTuples(parent.getTypes());
                for (int i = 0; i < mat.length; i++) {
                    if (inst.truthValueOf(parent, mat[i]) == -1) {
                        result.add(new GroundAtom(parent, mat[i]));
                    }
                }
            } catch (RBNIllegalArgumentException e) {
                throw new RuntimeException(e);
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
    public ProbForm sEval(RelStruc A) throws RBNCompatibilityException {
//        System.out.println("sEval code");
        return this;
    }

    @Override
    public ProbForm conditionEvidence(RelStruc A, OneStrucData inst) throws RBNCompatibilityException {
        return null;
    }

    // we cannot substitute this in smaller prob formula -> return the same object
    @Override
    public ProbForm substitute(String[] vars, int[] args) {
//        System.out.println("substitute code 1");
        ProbFormGnn result = new ProbFormGnn("-1", this.idGnn, this.gnnattr, this.edge_name, this.edge_direction, this.gnn_inference, this.oneHotEncoding);

        if (vars.length == 0)
            result.argument = Arrays.toString(new String[0]);
        else
            result.argument = rbnutilities.array_substitute(new String[]{argument}, vars, args)[0];

        if (this.alias != null)
            result.setAlias(this.alias.substitute(vars, args));

        return result;
    }

    @Override
    public ProbForm substitute(String[] vars, String[] args) {
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
        TreeSet<Rel> parent = new TreeSet<>();
        for (Rel rel: this.getGnnattr())
            if (rel.isprobabilistic())
                parent.add(rel);
        return parent;
    }

    @Override
    public TreeSet<Rel> parentRels(TreeSet<String> processed) {
        System.out.println("parentRels code 2");
        TreeSet<Rel> result = new TreeSet<Rel>();
        // this checks if processed makes sense here
        assert !processed.isEmpty();
        TreeSet<Rel> parent = new TreeSet<>();
        for (Rel rel: this.getGnnattr())
            if (rel.isprobabilistic())
                parent.add(rel);
        return parent;
    }

    @Override
    public GnnPy getGnnPy() {
        return gnnPy;
    }
    @Override
    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }

    @Override
    public void setEdge_name(String edge_name) {
        this.edge_name = edge_name;
    }

    @Override
    public void setEdge_direction(String edge_direction) {
        this.edge_direction = edge_direction;
    }

    @Override
    public String getEdge_name() {
        return edge_name;
    }

    @Override
    public String getEdge_direction() {
        return edge_direction;
    }

    @Override
    public Rel[] getGnnattr() {
        return gnnattr;
    }

    @Override
    public String getArgument() {
        return argument;
    }

    @Override
    public String getIdGnn() {
        return idGnn;
    }

    @Override
    public boolean isOneHotEncoding() {
        return oneHotEncoding;
    }

    @Override
    public String getGnn_inference() {
        return gnn_inference;
    }

    @Override
    public boolean isBoolean() { return true; }
}
