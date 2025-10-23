package PyManager;

import RBNpackage.BoolRel;
import RBNpackage.Rel;

import java.util.*;

public class TorchInputSpecs {

    String type;
    List<Rel> nodeAttributes;
    BoolRel edgeRelation;
    List<Rel> edgeAttributes;

    public TorchInputSpecs(String type, List<Rel> nodeAttributes, BoolRel edgeRelation, List<Rel> edgeAttributes) {
        this.type = type;
        this.nodeAttributes = nodeAttributes;
        this.edgeRelation = edgeRelation;
        if (edgeAttributes == null)
            this.edgeAttributes = new ArrayList<>();
        else
            this.edgeAttributes = edgeAttributes;
    }

    @Override
    public String toString() {
        if (edgeAttributes != null || edgeAttributes.size() > 0) {
            return "NODE " + ":\n" +
                    "  FEATURE: " + String.join(", ", nodeAttributes.toString()) + "\n" +
                    "  EDGE: " + edgeRelation.toString() + "\n" +
                    "  EDGE ATTRIBUTES: " + String.join(", ", edgeAttributes.toString());
        }
        return "NODE " + ":\n" +
                "  FEATURE: " + String.join(", ", nodeAttributes.toString()) + "\n" +
                "  EDGE: " + edgeRelation.toString();
    }

    public List<Rel> getNodeAttributes() { return nodeAttributes; }

    public void setNodeAttributes(List<Rel> nodeAttributes) {
        this.nodeAttributes = nodeAttributes;
    }

    public BoolRel getEdgeRelation() {
        return edgeRelation;
    }

    public void setEdgeRelation(BoolRel edgeRelation) {
        this.edgeRelation = edgeRelation;
    }

    public List<Rel> getEdgeAttributes() { return edgeAttributes; }

    public void setEdgeAttributes(List<Rel> edgeAttributes) { this.edgeAttributes = edgeAttributes; }

    public String getType() {
        return type;
    }
}
