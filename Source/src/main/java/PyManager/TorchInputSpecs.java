package PyManager;

import RBNpackage.BoolRel;
import RBNpackage.Rel;

import java.util.*;

public class TorchInputSpecs {

    List<Rel> features;
    BoolRel edgeRelation;

    public TorchInputSpecs(List<Rel> features, BoolRel edgeRelation) {
        this.features = features;
        this.edgeRelation = edgeRelation;
    }

    @Override
    public String toString() {
        return "NODE " + ":\n" +
                "  FEATURE: " + String.join(", ", features.toString()) + "\n" +
                "  EDGE: " + edgeRelation.toString();
    }

    public List<Rel> getFeatures() {
        return features;
    }

    public void setFeatures(List<Rel> features) {
        this.features = features;
    }

    public BoolRel getEdgeRelation() {
        return edgeRelation;
    }

    public void setEdgeRelation(BoolRel edgeRelation) {
        this.edgeRelation = edgeRelation;
    }
}
