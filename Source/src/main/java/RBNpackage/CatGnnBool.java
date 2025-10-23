package RBNpackage;

import PyManager.*;
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

    public CatGnnBool(String configModelPath, Vector<String> freeVals, List<TorchInputSpecs> inputs, TypedTorchPf typedTorchPf, Vector<String> outTypes, boolean withGnnPy) {
        super(configModelPath, freeVals, 1, inputs, typedTorchPf, outTypes, withGnnPy);
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
