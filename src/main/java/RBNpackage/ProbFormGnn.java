package RBNpackage;

import RBNExceptions.RBNCompatibilityException;
import RBNExceptions.RBNIllegalArgumentException;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;
import RBNutilities.IntArrayComparator;
import RBNutilities.rbnutilities;
import jep.python.PyObject;

import java.util.*;

/**
 * alpha(a) = COMPUTE WITH
 *      <gnn>
 * FOR INPUTS
 *      <list of attributes>
 * FORALL b
 *      WITH edge(a, b) <> WITH edge(b, a) <> WITH edge(a, b) || WITH edge(b, a)
 */
public class ProbFormGnn extends ProbForm {
    // the order of attributes need to be respected! this order will be used for the gnn encoding
    private Rel gnnattr[];
    private String argument; //x

    // the name of the edge relation in the RBN definition (usually "edge")
    private String edge_name;
    private String edge_direction;
    private GnnPy gnnPy;

    public ProbFormGnn(String argument, Rel[] attr) {
        this.setEdge_name("edge");
        this.setEdge_direction("ABBA");

        this.argument = argument;
        this.gnnattr = attr;
    }

    public ProbFormGnn(String argument, GnnPy gnnpy) {
        this.setEdge_name("edge");
        this.setEdge_direction("ABBA");

        this.argument = argument;
        this.gnnPy = gnnpy;
    }

    @Override
    public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias) {
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
                             // not currently used those
                             boolean useCurrentCvals,
                             boolean useCurrentPvals,
                             GroundAtomList mapatoms,
                             boolean useCurrentMvals,
                             Hashtable<String, Object[]> evaluated,
                             Hashtable<String, Integer> params,
                             int returntype,
                             // -----------------------
                             boolean valonly,
                             Profiler profiler)
            throws RBNCompatibilityException {

        Double[] result = new Double[2];

        /**
         * Reminder: look BayesConstructor makeCPT for evaluate function
         */

        return result;
    }

    @Override
    public double evalSample(RelStruc A, Hashtable<String, PFNetworkNode> atomhasht, OneStrucData inst, long[] timers) throws RBNCompatibilityException {
//            System.out.println("evalSample code");
        OneStrucData onsd = new OneStrucData(A.getmydata().copy());
        // hardcoded solution where the sampled parent is blue
        // we need to sample the entire structure before sending to python
        int num_features = 0;
        SparseRelStruc sampledRel = new SparseRelStruc(A.getNames(), onsd, A.getCoords(), A.signature());
        sampledRel.getmydata().add(inst.copy());
        for (Rel parent : this.parentRels()) {
                try {
                    int[][] mat = A.allTypedTuples(parent.getTypes());
                    // maybe there could be attributes with different number, we keep the biggest
                    if (parent.arity == 1 && mat.length >= num_features)
                        num_features = mat.length;

                    for (int i = 0; i < mat.length; i++) {
                        if (sampledRel.truthValueOf(parent, mat[i]) == -1) {
                            GroundAtom myatom = new GroundAtom(parent, mat[i]);
                            String myatomname = myatom.asString();
                            PFNetworkNode gan = (PFNetworkNode) atomhasht.get(myatomname);
                            if (gan != null) {
                                double result = (double) gan.sampleinstVal();
                                boolean sampledVal = false;
                                if (result == 1)
                                    sampledVal = true;
                                sampledRel.getmydata().findInBoolRel(parent).add(mat[i], sampledVal);
                            }
                        }
                    }

                } catch (RBNIllegalArgumentException e) {
                    throw new RuntimeException(e);
                }
        }

        // take only the "edge" relation
        // this can be later changed to a more general approach
        // find the boolean relations that should represent edges
        Vector<BoolRel> boolrel = sampledRel.getBoolBinaryRelations();
        String edge_index = "";
        for (BoolRel element : boolrel) {
            if (Objects.equals(element.name(), this.edge_name)) {
                if (Objects.equals(this.edge_direction, "ABBA"))
                    edge_index =  this.gnnPy.stringifyGnnEdgesABBA(sampledRel, element);
                if (Objects.equals(this.edge_direction, "AB"))
                    edge_index =  this.gnnPy.stringifyGnnEdgesAB(sampledRel, element);
                if (Objects.equals(this.edge_direction, "BA"))
                    edge_index =  this.gnnPy.stringifyGnnEdgesBA(sampledRel, element);
                break;
            }
        }

        String x = this.gnnPy.stringifyGnnFeatures(num_features, sampledRel, this.gnnattr);
        return this.gnnPy.inferModelNodeDouble(Integer.parseInt(this.argument), x, edge_index, "");
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

    @Override
    public Vector<GroundAtom> makeParentVec(RelStruc A) throws RBNCompatibilityException {
        System.out.println("makeParentVec code 1");
        return makeParentVec(A, new OneStrucData(), null);
    }

    @Override
    public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone) throws RBNCompatibilityException {
        System.out.println("makeParentVec code 2");
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
        return false;
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
        System.out.println("sEval code");
        return this;
    }

    // we cannot substitute this in smaller prob formula -> return the same object
    @Override
    public ProbForm substitute(String[] vars, int[] args) {
        System.out.println("substitute code 1");
        ProbFormGnn result = new ProbFormGnn(this.argument, this.gnnattr);

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
    public void updateSig(Signature s) {
        System.out.println("updateSig code");
    }

    @Override
    public void setCvals(String paramname, double val) {
        System.out.println("setCvals code");
    }

    @Override
    public TreeSet<Rel> parentRels() {
//        System.out.println("parentRels code 1");
        return new TreeSet<Rel>(Arrays.asList(this.gnnattr));
    }

    @Override
    public TreeSet<Rel> parentRels(TreeSet<String> processed) {
        System.out.println("parentRels code 2");
        TreeSet<Rel> result = new TreeSet<Rel>();
        // this checks if processed makes sense here
        assert !processed.isEmpty();
        return new TreeSet<Rel>(Arrays.asList(this.gnnattr));
    }

    public void setGnnPy(GnnPy gnnPy) {
        this.gnnPy = gnnPy;
    }

    public void setEdge_name(String edge_name) {
        this.edge_name = edge_name;
    }

    public void setEdge_direction(String edge_direction) {
        this.edge_direction = edge_direction;
    }
}
