package PyManager;

import RBNpackage.BoolRel;
import RBNpackage.Rel;

import java.util.*;

public class TorchInputSpecs {

    Rel name;
    List<Rel> features;
    BoolRel edgeRelation;

    public TorchInputSpecs(Rel name, List<Rel> features, BoolRel edgeRelation) {
        this.name = name;
        this.features = features;
        this.edgeRelation = edgeRelation;
    }

    @Override
    public String toString() {
        return "NODE " + name.toString() + ":\n" +
                "  FEATURE: " + String.join(", ", features.toString()) + "\n" +
                "  EDGE: " + edgeRelation.toString();
    }

    public Rel getName() {
        return name;
    }

    public void setName(Rel name) {
        this.name = name;
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
