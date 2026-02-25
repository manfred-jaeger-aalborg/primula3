package RBNpackage.VarTermPackage;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class VarTerm extends ArgTerm {
    private final String name;

    public VarTerm(String name) {
        if (name == null) throw new IllegalArgumentException("Variable name cannot be null");
//        if (name.contains("+") || name.contains("-") || name.contains("=") || name.contains("(") || name.contains(")")) throw new IllegalArgumentException("Variable name cannot contain expressions +, -, =, (, or )");
        if (!name.matches("^[a-zA-Z0-9]+$")) throw new IllegalArgumentException("Variable name must be alphanumeric: " + name);

        this.name = name;
    }

    public VarTerm(int name) {
        this.name = String.valueOf(name);
    }

    public String getName() { return name; }

    @Override
    public ArgTerm substitute(String var, ArgTerm arg) {
        if (name.equals(var)) {
            return arg;
        }
        return this;
    }

    @Override
    public ArgTerm substitute(ArgTerm var, int arg) {
        if (var.equals(this)) {
            return new VarTerm(arg);
        }
        return this;
    }

    @Override
    public ArgTerm substitute(ArgTerm var, ArgTerm arg) {
        if (var.equals(this)) {
            return arg;
        }
        return this;
    }


    @Override
    public Set<String> getVariables() {
        Set<String> vars = new HashSet<>();
        if (!isGround()) {
            vars.add(name);
        }
        return vars;
    }

    @Override
    public Set<String> varsEqual(ArgTerm other) {
        Set<String> vars = new HashSet<>();
        for (String var : getVariables()) {
            for (String otherVar : other.getVariables()) {
                if (var.equals(otherVar)) vars.add(var);
            }
        }
        return vars;
    }

    @Override
    public String argEval() {
        return name;
    }

    @Override
    public boolean isGround() {
        return isInt(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (o instanceof ArgTerm) {
            ArgTerm argTerm = (ArgTerm) o;
            return Objects.equals(this.name, argTerm.argEval());
        }

        if (o instanceof Integer) {
            return Objects.equals(this.name, String.valueOf(o));
        }

        if (o instanceof String) {
            return Objects.equals(this.name, o);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public VarTerm clone() {
        return new VarTerm(name);
    }
}
