package Experiments;

import RBNExceptions.RBNIllegalArgumentException;
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
    private int num_nodes;

    public PyTorchExport(SparseRelStruc sparseRelStruc, RBN rbn) {
        this.data = sparseRelStruc;
        this.gnnPy = new GnnPy();
        this.rbn = rbn;
        for (RBNPreldef prel: rbn.prelements()){
            if (prel.pform() instanceof ProbFormGnn) {
                this.pfgnn = (ProbFormGnn) prel.pform();
                break; // for now, we use the first we find
            }
        }

        for (Rel parent : this.pfgnn.parentRels()) {
            try {
                int[][] mat = sparseRelStruc.allTypedTuples(parent.getTypes());
                // maybe there could be attributes with different number, we keep the biggest
                if (parent.arity == 1 && mat.length >= this.num_nodes)
                    this.num_nodes = mat.length;
            } catch (RBNIllegalArgumentException e) {
                System.out.println(e);
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
    public String getX(int num_nodes) {
        return this.gnnPy.stringifyGnnFeatures(num_nodes, this.data, this.pfgnn.getGnnattr(), this.pfgnn.isOneHotEncoding());
    }

    public void writePythonDataOnFile(String path) {
        String x = getX(this.num_nodes);
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
