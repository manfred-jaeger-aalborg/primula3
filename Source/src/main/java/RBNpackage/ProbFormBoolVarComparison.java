package RBNpackage;

import java.util.*;

import RBNExceptions.RBNCompatibilityException;
import RBNinference.PFNetworkNode;
import RBNpackage.VarTermPackage.ArgTerm;
import RBNpackage.VarTermPackage.VarTerm;
import RBNutilities.rbnutilities;
import RBNLearning.*;

public class ProbFormBoolVarComparison extends ProbFormBool {

    private ArgTerm term1, term2;
    final private String LT_OP = "<";
    final private String GT_OP = ">";
    private String op; // ">" or "<"

    public ProbFormBoolVarComparison(ArgTerm t1, ArgTerm t2, String op, boolean s) {
        term1 = t1;
        term2 = t2;
        this.op = op;
        sign = s;
    }

    public ProbFormBoolVarComparison(ArgTerm t1, ArgTerm t2, String op) {
        term1 = t1;
        term2 = t2;
        this.op = op;
        sign = true;
    }

    @Override
    public int evaluatesTo(RelStruc A, OneStrucData inst,
            boolean usesampleinst, HashMap<String, GroundAtom> atomhasht)
            throws RBNCompatibilityException {
        return evaluatesTo(A);
    }

    @Override
    public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
        if (!isGround())
            return -1;
        int v1 = Integer.parseInt(term1.argEval());
        int v2 = Integer.parseInt(term2.argEval());
        boolean tv;
        if (op.equals(LT_OP))
            tv = v1 < v2;
        else if (op.equals(GT_OP)) {
            tv = v1 > v2;
        } else
            throw new IllegalArgumentException("Invalid comparison operator: " + op);

        if ((tv && sign) || (!tv && !sign))
            return 1;
        else
            return 0;
    }

    @Override
    public String asString(int syntax, int depth, RelStruc A, boolean paramsAsValue, boolean usealias) {
        if (usealias && this.getAlias() != null)
            return this.getAlias();
        return "[" + term1.toString() + op + term2.toString() + "]";
    }

    public String asString() {
        return "[" + term1.toString() + op + term2.toString() + "]";
    }

    @Override
    public CPModel conditionEvidence(RelStruc A, OneStrucData inst)
            throws RBNCompatibilityException {
        return this;
    }

    public Object[] evaluate(RelStruc A,
            OneStrucData inst,
            ArgTerm[] vars,
            int[] tuple,
            int gradindx,
            boolean useCurrentCvals,
            boolean useCurrentPvals,
            HashMap<Rel, GroundAtomList> mapatoms,
            boolean useCurrentMvals,
            HashMap<String, Object[]> evaluated,
            HashMap<String, Integer> params,
            int returntype,
            boolean valonly,
            Profiler profiler) {
        Object[] result = new Object[2];
        if (!valonly) {
            if (returntype == ProbForm.RETURN_SPARSE)
                result[1] = new Gradient_TreeMap(params);
            else result[1] = new Gradient_Array(params);
        }

        ProbFormBoolVarComparison thissubstituted = (ProbFormBoolVarComparison) this.substitute(vars, tuple);
        if (!thissubstituted.isGround())
            throw new IllegalArgumentException("Attempt to evaluate non-ground comparison");
        int v1 = Integer.parseInt(thissubstituted.term1.argEval());
        int v2 = Integer.parseInt(thissubstituted.term2.argEval());
        boolean tv;
        if (op.equals(LT_OP))
            tv = v1 < v2;
        else if (op.equals(GT_OP)) {
            tv = v1 > v2;
        } else
            throw new IllegalArgumentException("Invalid comparison operator: " + op);
        result[0] = tv ? 1.0 : 0.0;
        return result;
    }

    @Override
    public double[] evalSample(RelStruc A,
            HashMap<String, PFNetworkNode> atomhasht,
            OneStrucData inst,
            HashMap<String, double[]> evaluated,
            long[] timers)
            throws RBNCompatibilityException {

        String key = null;
        if (evaluated != null) {
            key = this.makeKey(A);
            double[] d = evaluated.get(key);
            if (d != null) {
                return d;
            }
        }
        double[] result = new double[] {evaluate(A, null)};
        if (evaluated != null) {
            evaluated.put(key, result);
        }
        return result;
    }

    @Override
    public VarTerm[] freevars() {
        ArgTerm[] bothterms = {term1, term2};
        return rbnutilities.NonIntOnly(bothterms);
    }

    @Override
    public Vector<GroundAtom> makeParentVec(RelStruc A, OneStrucData inst, TreeSet<String> macrosdone)
            throws RBNCompatibilityException {
        return new Vector<GroundAtom>();
    }

    @Override
    public CPModel sEval(RelStruc A) throws RBNCompatibilityException {
        double value = evaluate(A, null);
        if (value == 1)
            return new ProbFormBoolConstant(false);
        else
            return new ProbFormBoolConstant(true);
    }

    @Override
    public CPModel substitute(String[] vars, int[] args) {
        if (vars.length != args.length)
            System.out.println("ProbFormBoolComparison.substitute: vars: " + rbnutilities.arrayToString(vars) + "   args: " + rbnutilities.arrayToString(args));
        ArgTerm termx = term1;
        ArgTerm sterm1 = term1;
        ArgTerm sterm2 = term2;
        for (int j = 0; j < vars.length; j++) {
            if (termx.equals(vars[j])) sterm1 = new VarTerm(args[j]);
        }
        termx = term2;
        for (int j = 0; j < vars.length; j++) {
            if (termx.equals(vars[j])) sterm2 = new VarTerm(args[j]);
        }
        ProbFormBoolVarComparison result = new ProbFormBoolVarComparison(sterm1, sterm2, op, sign);
        if (this.alias != null)
            result.setAlias((ProbFormAtom) this.alias.substitute(vars, args));
        return result;
    }

    @Override
    public CPModel substitute(String[] vars, String[] args) {
        if (vars.length != args.length)
            System.out.println("ProbFormBoolComparison.substitute: vars: " + rbnutilities.arrayToString(vars) + "   args: " + rbnutilities.arrayToString(args));
        ArgTerm termx = term1;
        ArgTerm sterm1 = term1;
        ArgTerm sterm2 = term2;
        for (int j = 0; j < vars.length; j++) {
            if (termx.equals(vars[j])) sterm1 = new VarTerm(args[j]);
        }
        termx = term2;
        for (int j = 0; j < vars.length; j++) {
            if (termx.equals(vars[j])) sterm2 = new VarTerm(args[j]);
        }
        ProbFormBoolVarComparison result = new ProbFormBoolVarComparison(sterm1, sterm2, op, sign);
        if (this.alias != null)
            result.setAlias((ProbFormAtom) this.alias.substitute(vars, args));
        return result;
    }

    @Override
    public CPModel substitute(String[] vars, ArgTerm[] args) {
        if (vars.length != args.length)
            System.out.println("ProbFormBoolComparison.substitute: vars: " + rbnutilities.arrayToString(vars) + "   args: " + rbnutilities.arrayToString(args));
        ArgTerm termx = term1;
        ArgTerm sterm1 = term1;
        ArgTerm sterm2 = term2;
        for (int j = 0; j < vars.length; j++) {
            for (String vs : termx.getVariables()) {
                if (vs.equals(vars[j]))
                    sterm1 = sterm1.substitute(vars[j], args[j]);
            }
        }
        termx = term2;
        for (int j = 0; j < vars.length; j++) {
            for (String vs : termx.getVariables()) {
                if (vs.equals(vars[j]))
                    sterm2 = sterm2.substitute(vars[j], args[j]);
            }
        }
        ProbFormBoolVarComparison result = new ProbFormBoolVarComparison(sterm1, sterm2, op, sign);
        if (this.alias != null)
            result.setAlias((ProbFormAtom) this.alias.substitute(vars, args));
        return result;
    }

    @Override
    public CPModel substitute(ArgTerm[] vars, ArgTerm[] args) {
        if (vars.length != args.length)
            System.out.println("ProbFormBoolComparison.substitute: vars: " + rbnutilities.arrayToString(vars) + "   args: " + rbnutilities.arrayToString(args));
        ArgTerm termx = term1;
        ArgTerm sterm1 = term1;
        ArgTerm sterm2 = term2;
        for (int j = 0; j < vars.length; j++) {
            Set<String> sameVars = termx.varsEqual(vars[j]);
            if (sameVars.size() > 0)
                sterm1 = sterm1.substitute(vars[j], args[j]);
        }
        termx = term2;
        for (int j = 0; j < vars.length; j++) {
            Set<String> sameVars = termx.varsEqual(vars[j]);
            if (sameVars.size() > 0)
                sterm2 = sterm2.substitute(vars[j], args[j]);
        }
        ProbFormBoolVarComparison result = new ProbFormBoolVarComparison(sterm1, sterm2, op, sign);
        if (this.alias != null)
            result.setAlias((ProbFormAtom) this.alias.substitute(vars, args));
        return result;
    }

    @Override
    public CPModel substitute(ArgTerm[] vars, int[] args) {
        if (vars.length != args.length)
            System.out.println("ProbFormBoolComparison.substitute: vars: " + rbnutilities.arrayToString(vars) + "   args: " + rbnutilities.arrayToString(args));
        ArgTerm termx = term1;
        ArgTerm sterm1 = term1;
        ArgTerm sterm2 = term2;
        for (int j = 0; j < vars.length; j++) {
            Set<String> sameVars = termx.varsEqual(vars[j]);
            if (sameVars.size() > 0)
                sterm1 = sterm1.substitute(vars[j], args[j]);
        }
        termx = term2;
        for (int j = 0; j < vars.length; j++) {
            Set<String> sameVars = termx.varsEqual(vars[j]);
            if (sameVars.size() > 0)
                sterm2 = sterm2.substitute(vars[j], args[j]);
        }
        ProbFormBoolVarComparison result = new ProbFormBoolVarComparison(sterm1, sterm2, op, sign);
        if (this.alias != null)
            result.setAlias((ProbFormAtom) this.alias.substitute(vars, args));
        return result;
    }

    public ArgTerm term1() {
        return term1;
    }

    public ArgTerm term2() {
        return term2;
    }

    public String op() {
        return op;
    }

    private boolean isGround() {
        return term1.isGround() && term2.isGround();
    }

    public CPModel toStandardPF(boolean recursive) {
        return this;
    }

    public ArgTerm[] terms() {
        ArgTerm[] result = new ArgTerm[2];
        result[0] = term1;
        result[1] = term2;
        return result;
    }

    public ProbFormBoolVarComparison clone() {
        return new ProbFormBoolVarComparison(term1, term2, op, sign);
    }

    public void updateSig(Signature s) {
    }

    public TreeSet<Rel> parentRels() {
        return new TreeSet<Rel>();
    }

    public TreeSet<Rel> parentRels(TreeSet<String> processed) {
        return new TreeSet<Rel>();
    }

    @Override
    public int numvals() {
        return 2;
    }
}
