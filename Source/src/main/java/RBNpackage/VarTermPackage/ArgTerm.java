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
        if (s == null || s.isEmpty()) return false;

        int i = 0;
        int len = s.length();

        if (s.charAt(0) == '-') {
            if (len == 1) return false;
            i = 1;
        }

        for (; i < len; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }

        return true;
    }
}