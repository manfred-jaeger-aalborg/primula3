package RBNpackage.VarTermPackage;

import java.util.HashSet;
import java.util.Set;

public class IntPlus extends IntOp {

    public IntPlus(ArgTerm left, ArgTerm right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String argEval() {
        // if is grounded it will return the expression evaluated, otherwise return the non-grounded string
        if (left.isGround() && right.isGround()) return Integer.toString(Integer.parseInt(left.argEval()) + Integer.parseInt(right.argEval()));
        return toString();
    }

    @Override
    public ArgTerm substitute(String var, ArgTerm replacement) {
        return new IntPlus(left.substitute(var, replacement), right.substitute(var, replacement));
    }

    @Override
    public ArgTerm substitute(ArgTerm var, int arg) {
        return new IntPlus(left.substitute(var, arg), right.substitute(var, arg));
    }

    @Override
    public ArgTerm substitute(ArgTerm var, ArgTerm replacement) {
        return new IntPlus(left.substitute(var, replacement), right.substitute(var, replacement));
    }

    @Override
    public boolean isGround() {
        return left.isGround() && right.isGround();
    }

    @Override
    public ArgTerm clone() {
        return new IntPlus(left.clone(), right.clone());
    }

    public String toString() {
        return "(" + left + "+" + right + ")";
    }

    @Override
    public Set<String> getVariables() {
        Set<String> vars = new HashSet<>();
        vars.addAll(left.getVariables());
        vars.addAll(right.getVariables());
        return vars;
    }

    @Override
    public Set<String> varsEqual(ArgTerm other) {
        Set<String> vars = new HashSet<>();
        vars.addAll(left.varsEqual(other));
        vars.addAll(right.varsEqual(other));
        return vars;
    }

    @Override
    public Set<int[]> getAllTrue(int tmin, int tmax) {
        return Set.of();
    }
}
