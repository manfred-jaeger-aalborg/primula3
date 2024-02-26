package Experiments;

import RBNpackage.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.Vector;

public class PyTorchExport {

    private SparseRelStruc data;
    private GnnPy gnnPy;
    private RBN rbn;
    private ProbFormGnn pfgnn;
    private int num_features;

    public PyTorchExport(SparseRelStruc sparseRelStruc, RBN rbn, int num_features) {
        this.data = sparseRelStruc;
        this.gnnPy = new GnnPy();
        this.num_features = num_features;
        this.rbn = rbn;
        for (RBNPreldef prel: rbn.prelements()){
            if (prel.pform() instanceof ProbFormGnn) {
                this.pfgnn = (ProbFormGnn) prel.pform();
                break; // for now, we use the first we find
            }
        }
    }

    public String getEdges() {
        Vector<BoolRel> boolrel = this.data.getBoolBinaryRelations();
        String edge_index = "";
        for (BoolRel element : boolrel) {
            if (this.data.getmydata().findInBoolRel(element).allTrue().isEmpty()) {
                edge_index = "";
                break;
            } else {
                if (Objects.equals(element.name(), this.pfgnn.getEdge_name())) {
                    if (Objects.equals(this.pfgnn.getEdge_direction(), "ABBA"))
                        edge_index = this.gnnPy.stringifyGnnEdgesABBA(this.data, element);
                    if (Objects.equals(this.pfgnn.getEdge_direction(), "AB"))
                        edge_index = this.gnnPy.stringifyGnnEdgesAB(this.data, element);
                    if (Objects.equals(this.pfgnn.getEdge_direction(), "BA"))
                        edge_index = this.gnnPy.stringifyGnnEdgesBA(this.data, element);
                    break;
                }
            }
        }
        return edge_index;
    }
    public String getX(int num_features) {
        return this.gnnPy.stringifyGnnFeatures(num_features, this.data, this.pfgnn.getGnnattr(), this.pfgnn.isOneHotEncoding());
    }

    public void writePythonDataOnFile(String path) {
        String x = getX(this.num_features);
        String edge_index = this.getEdges();
        try {
            File myObj = new File(path);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            }

            try (FileWriter myWriter = new FileWriter(path)) {
                myWriter.write(x+'\n');
                myWriter.write(edge_index);
                System.out.println("Written on: " + path);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
