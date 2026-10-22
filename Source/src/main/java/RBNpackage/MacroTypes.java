package RBNpackage;

import PyManager.TorchInputPf;
import RBNpackage.VarTermPackage.ArgTerm;

import java.util.*;

/**
 * Types of the macro arguments that are not declared in the macro head.
 *   @R0(t) = Rain(t)=R0                -> t gets the type of Rain's argument: Integer
 *   @C0(v,t) = ... Pollution(v,t)=C0   -> v: sub, t: Integer
 * An argument gets the type of the relation (or macro) argument where it is used in the macro body; a declared
 * type, e.g. @R0([Integer]t), is kept. A macro used as NODEFEAT of a GNN needs all its types: error otherwise.
 */
public class MacroTypes {

    /** Infers the missing types of all macros of rbn, then checks the macros used as NODEFEAT. */
    public static void infer(RBN rbn) {
        Map<RBNMacro, boolean[]> known = new HashMap<>();
        for (RBNMacro m : rbn.macroelements()) infer(m, known);

        for (RBNElement e : rbn.elements()) {
            if (!(e.cpmod() instanceof CatGnn gnn)) continue; // only CatGnn relations !
            for (String type : gnn.getTypedTorchPf().getTypedNames())
                for (TorchInputPf tip : gnn.getTypedTorchPf().getCombines(type))
                    for (CPModel feature : tip.getPfargsNode())
                        if (feature instanceof ProbFormMacroCall call) checkTyped(call.macro(), infer(call.macro(), known));
        }
    }

    /** known[i]: argument i of m has a type, declared or inferred. */
    private static boolean[] infer(RBNMacro m, Map<RBNMacro, boolean[]> known) {
        if (known.containsKey(m)) return known.get(m);
        Type[] types = m.types();
        ArgTerm[] params = m.arguments();
        boolean[] result = new boolean[params.length];
        for (int p = 0; p < params.length; p++) result[p] = !(types[p] instanceof TypeDomain);
        known.put(m, result); // a macro calling itself sees only the declared types

        // a GNN as macro body: nothing to infer from
        if (m.cpmod() == null || m.cpmod() instanceof CatGnn) return result;

        for (int p = 0; p < params.length; p++) {
            if (result[p]) continue;
            String param = params[p].argEval();
            List<CPModel> atoms = new ArrayList<>();
            collectAtoms(m.cpmod(), param, atoms);
            for (CPModel atom : atoms) {
                ArgTerm[] args;
                Type[] argTypes;
                boolean[] argTyped;
                if (atom instanceof ProbFormAtom pfa) {
                    args = pfa.getArguments();
                    argTypes = pfa.getRelation().getTypes();
                    argTyped = new boolean[args.length];
                    Arrays.fill(argTyped, argTypes != null);
                } else {
                    ProbFormMacroCall call = (ProbFormMacroCall) atom;
                    args = call.args();
                    argTypes = call.macro().types();
                    argTyped = infer(call.macro(), known);
                }
                for (int i = 0; i < args.length; i++) {
                    Set<String> vars = args[i].getVariables();
                    if (!argTyped[i] || vars == null || !vars.contains(param)) continue;
                    if (!result[p]) {
                        types[p] = argTypes[i];
                        result[p] = true;
                    } else if (!types[p].getName().equals(argTypes[i].getName()))
                        throw new IllegalArgumentException("Macro " + m.rel().name() + ": argument " + param
                                + " is used both as " + types[p].getName() + " and as " + argTypes[i].getName()
                                + "; declare its type in the macro head");
                }
            }
        }
        return result;
    }

    private static void checkTyped(RBNMacro m, boolean[] typed) {
        for (int p = 0; p < typed.length; p++)
            if (!typed[p]) {
                String param = m.arguments()[p].argEval();
                throw new IllegalArgumentException("Macro " + m.rel().name() + ": cannot infer the type of argument "
                        + param + "; declare it in the macro head, e.g. [Integer]" + param + " or [<node type>]" + param);
            }
    }

    /** Atoms and macro calls in pf where var is free (not inside a COMBINE that binds var). */
    private static void collectAtoms(CPModel pf, String var, List<CPModel> out) {
        if (pf instanceof ProbFormAtom || pf instanceof ProbFormMacroCall) out.add(pf);
        else if (pf instanceof ProbFormBoolAtom ba) out.add(ba.getPfatom());
        else if (pf instanceof ProbFormBoolAtomEquality ae) {
            for (Object a : ae.getArgs()) if (a instanceof CPModel m) collectAtoms(m, var, out);
        } else if (pf instanceof ProbFormBoolComposite bc) {
            for (int i = 0; i < bc.numComponents(); i++) collectAtoms(bc.componentAt(i), var, out);
        } else if (pf instanceof ProbFormConvComb cc) {
            collectAtoms(cc.f1(), var, out);
            collectAtoms(cc.f2(), var, out);
            collectAtoms(cc.f3(), var, out);
        } else if (pf instanceof ProbFormCombFunc cf) {
            for (ArgTerm q : cf.getQuantvars()) if (q.argEval().equals(var)) return;
            for (CPModel a : cf.getPfargs()) collectAtoms(a, var, out);
            collectAtoms(cf.getCconstr(), var, out);
        } else if (pf instanceof CatModelSoftMax sm) {
            for (CPModel a : sm.probforms) collectAtoms(a, var, out);
        }
        // constants, comparisons of terms (t<1, t=1), GNNs: no types
    }
}
