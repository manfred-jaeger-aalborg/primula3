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
public class CatGnn extends CPModel implements CPMGnn {
    // the order of attributes need to be respected! this order will be used for the gnn encoding
    private Rel gnnattr[];
    private String argument;

    // for graph classification: each probformgnn will represent a class for the classifier
    // e.g. MUTAG class has 2 classes: mutagenic (0) and non-mutagenic (1)
    // for binary output represent the index of the True vale in the final logits/probabilities of the GNNs
    private int classId;
    // true if we use one-hot encoding for the features representation
    private boolean oneHotEncoding;

    // the name of the edge relation in the RBN definition (usually "edge")
    private String edge_name;
    // AB, BA, ABBA (edge direction in the adjacency matrix)
    private String edge_direction;
    private GnnPy gnnPy;
    // each gnn will have an id that identify the model
    private String idGnn;

    // if is set to true, means that the GNN is for categorical output, if false is boolean
    private boolean categorical;

    // this variable is used to set the inference for node or graph classification. Keyword: "node" or "graph"
    private String gnn_inference;
    public CatGnn(String argument, String idGnn, Boolean categorical, Rel[] attr, String edge_name, String edge_direction, String gnn_inference, boolean oneHotEncoding) {
        this.setEdge_name(edge_name);
        this.setEdge_direction(edge_direction);

        this.argument = argument;
        this.idGnn = idGnn;
        this.categorical = categorical;
        this.gnnattr = attr;
        this.classId = -1;
        this.oneHotEncoding = oneHotEncoding;
        this.gnn_inference = gnn_inference;
    }

    /**
     *
     * @param argument argument
     * @param idGnn id used in the load_gnn.py to load the GNN weights
     * @param categorical boolean flag, True if the GNN is used for multiple classes
     * @param attr array of Rel for the GNN attributes
     * @param edge_name the name of the edge relation in the formula
     * @param edge_direction how the adjacency matrix is composed (AB, BA, ABBA)
     * @param gnn_inference "node" if the GNN is used for node classification, "graph" for graph classification
     * @param oneHotEncoding true if the representation of the features are in a one-hot encoding representation
     * @param classId index of the True value for which the CPMGnn will evaluate the probability
     */
    public CatGnn(String argument, String idGnn, Boolean categorical, Rel[] attr, String edge_name, String edge_direction, String gnn_inference, boolean oneHotEncoding, int classId) {
        this.setEdge_name(edge_name);
        this.setEdge_direction(edge_direction);

        this.argument = argument;
        this.idGnn = idGnn;
        this.categorical = categorical;
        this.gnnattr = attr;
        this.classId = classId;
        this.gnn_inference = gnn_inference;
        this.oneHotEncoding = oneHotEncoding;
    }

    public CatGnn(String argument, GnnPy gnnpy) {
        this.argument = argument;
        this.gnnPy = gnnpy;
    }

    @Override
    public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias) {
        if (this.classId != -1)
            return "[ gnn("+this.argument+")-" + this.classId + " ]";
        return "gnn("+this.argument+")";
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

        return gnnPy.evaluate_gnn(A, inst, this, valonly);
    }

    @Override
    public Double evalSample(RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst, Hashtable<String,Double> evaluated, long[] timers) throws RBNCompatibilityException {
        return gnnPy.evalSample_gnn(this, A, atomhasht, inst);
    }

    @Override
    public String[] freevars() {
        System.out.println("freevars code");
        return rbnutilities.NonIntOnly(new String[]{this.argument}); // convert
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
    public CPModel sEval(RelStruc A) throws RBNCompatibilityException {
        System.out.println("sEval code");
        return this;
    }

    // we cannot substitute this in smaller prob formula -> return the same object
    @Override
    public CPModel substitute(String[] vars, int[] args) {
//        System.out.println("substitute code 1");
        CatGnn result;
        if (this.classId == -1)
            result = new CatGnn(this.argument, this.idGnn, this.categorical, this.gnnattr, this.edge_name, this.edge_direction, this.gnn_inference, this.oneHotEncoding);
        else
            result = new CatGnn("-1", this.idGnn, this.categorical, this.gnnattr, this.edge_name, this.edge_direction, this.gnn_inference, this.oneHotEncoding, this.classId);

        if (vars.length == 0)
            result.argument = Arrays.toString(new String[0]);
        else
            result.argument = rbnutilities.array_substitute(new String[]{argument}, vars, args)[0];

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
    public int numvals() {
        return 0; // TODO what numvals should return?
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
    public boolean isBoolean() { return !categorical; }
}
