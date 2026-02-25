package RBNpackage.VarTermPackage;

import java.util.HashSet;
import java.util.Set;

public abstract class ArgTerm {

    public abstract String argEval();

    public abstract ArgTerm substitute(String var, ArgTerm arg);

    public abstract ArgTerm substitute(ArgTerm var, int arg);

    public abstract ArgTerm substitute(ArgTerm var, ArgTerm arg);

    public abstract boolean isGround();

    public abstract ArgTerm clone();

    public Set<String> getVariables() {
        return new HashSet<>();
    }

    public abstract Set<String> varsEqual(ArgTerm other);

    protected static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}