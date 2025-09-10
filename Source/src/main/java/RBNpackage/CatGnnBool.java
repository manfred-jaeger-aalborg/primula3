package RBNpackage;

import PyManager.GnnPy;
import PyManager.TorchInputRels;
import PyManager.TorchInputSpecs;
import PyManager.TorchModelWrapper;
import RBNExceptions.RBNCompatibilityException;
import RBNLearning.Profiler;
import RBNinference.PFNetworkNode;
import RBNutilities.rbnutilities;

import java.io.File;
import java.util.*;

public class CatGnnBool extends CatGnn implements RBNform {

    public CatGnnBool(String argument, String gnnId, int numLayers, ArrayList input_attr, ArrayList edge_attr, String gnn_inference, boolean oneHotEncoding) {
        super(argument, gnnId, numLayers, 1, input_attr, edge_attr, gnn_inference, oneHotEncoding);
    }

    public CatGnnBool(String configModelPath, Vector<String> freeVals, List<TorchInputSpecs> inputs, List<TorchInputRels> combinedClauses, boolean withGnnPy) {
        super(configModelPath, freeVals, 1, inputs, combinedClauses, withGnnPy);
    }

    public CatGnnBool(String argument, GnnPy gnnpy) {
        super(argument, gnnpy);
    }

    @Override
    public CPModel substitute(String[] vars, int[] args) {
        List<TorchInputRels> newgnnInputs = new ArrayList<>();
        for (TorchInputRels torchInput: gnnCombinedClauses) {
            TorchInputRels newnewInput = torchInput.substitute(vars, args);
            newgnnInputs.add(newnewInput);
        }

        CatGnnBool result = new CatGnnBool(this.configModelPath, this.freeVals, this.gnnInputs, this.gnnCombinedClauses, false);
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
        List<TorchInputRels> newgnnInputs = new ArrayList<>();
        for (TorchInputRels torchInput: gnnCombinedClauses) {
            TorchInputRels newnewInput = torchInput.substitute(vars, args);
            newgnnInputs.add(newnewInput);
        }

        CatGnnBool result = new CatGnnBool(this.configModelPath, this.freeVals, this.gnnInputs, this.gnnCombinedClauses, false);
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
    public int numvals() {
        return 1;
    }

    @Override
    public int evaluatesTo(RelStruc A, OneStrucData inst, boolean usesampleinst, Hashtable<String, GroundAtom> atomhasht) throws RBNCompatibilityException {
        return 0;
    }

    @Override
    public int evaluatesTo(RelStruc A) throws RBNCompatibilityException {
        return 0;
    }
}
