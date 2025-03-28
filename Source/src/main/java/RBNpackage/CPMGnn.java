package RBNpackage;

import java.util.ArrayList;
import java.util.TreeSet;

public interface CPMGnn {
    // this class will be used as a solution to overcome Java not supporting multiple inheritance

    GnnPy getGnnPy();
    void setGnnPy(GnnPy gnnPy);

    void setEdge_name(String edge_name);

    String getEdge_name();

    Rel[] getGnnattr();

    String getArgument();

    String getIdGnn();

    boolean isOneHotEncoding();

    String getGnn_inference();

    boolean isBoolean();

    TreeSet<Rel> parentRels();

    ArrayList<ArrayList<Rel>> getInput_attr();

    ArrayList<Rel> getEdge_attr();

    int getNumLayers();
}
