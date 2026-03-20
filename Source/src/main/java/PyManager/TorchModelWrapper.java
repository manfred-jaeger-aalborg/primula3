package PyManager;

import RBNpackage.ProbForm;
import jep.*;
import java.io.StringWriter;
import java.util.*;

public class TorchModelWrapper {
    private final String modelName;
    private final SharedInterpreter modelInterpreter;
    private final String modelClassName;

    private Object modelRef;

    private int lastFlattenHash = 0;
    private Map<String, float[]> cachedFlatXDict = null;
    private Map<String, int[]> cachedXShapeDict = null;
    private Map<String, int[]> cachedFlatEdgeDict = null;
    private Map<String, Integer> cachedEdgeCounts = null;

    // primitive single-type cache
    private float[] cachedFlatX = null;
    private int[] cachedXShape = null;
    private int[] cachedFlatEdge = null;
    private int cachedNumEdges = 0;
    private float[] cachedFlatEA = null;
    private int[] cachedEAShape = null;

    public TorchModelWrapper(String modelName, String modelClassName, SharedInterpreter interpreter) {
        this.modelName = modelName;
        this.modelInterpreter = interpreter;
        this.modelClassName = modelClassName;

        initializePythonEnvironment();
        cachePythonReferences();
    }

    private void initializePythonEnvironment() {
        try {
            modelInterpreter.exec("import torch");
            modelInterpreter.exec("from torch_geometric.data import HeteroData");
            modelInterpreter.exec("import sys");

            // Optimized Python functions utilizing .view() for zero-copy reshaping of flat arrays
            modelInterpreter.exec("""
                def _get_device(model):
                    param = next(model.parameters(), None)
                    return param.device if param is not None else torch.device("cpu")
                
                def forward_single_fast_(model, flat_x, x_shape, flat_edge, num_edges, with_gradients, flat_ea, ea_shape):
                    device = _get_device(model)
                    
                    # Reshape features and edges without copying memory
                    xi = torch.as_tensor(flat_x, dtype=torch.float32, device=device).view(x_shape[0], x_shape[1])
                    if num_edges > 0:
                        ei = torch.as_tensor(flat_edge, dtype=torch.long, device=device).view(2, num_edges)
                    else:
                        ei = torch.empty((2, 0), dtype=torch.long, device=device)
                    
                    ea = None
                    if flat_ea is not None:
                        ea = torch.as_tensor(flat_ea, dtype=torch.float32, device=device).view(ea_shape[0], ea_shape[1])

                    if with_gradients:
                        xi.requires_grad_(True)
                        if ea is not None:
                            ea.requires_grad_(True)
                        
                        out = model(xi, ei, ea) if ea is not None else model(xi, ei)
                        out.sum().backward()
                        
                        x_grad = xi.grad.detach().cpu().numpy() if xi.grad is not None else None
                        ea_grad = ea.grad.detach().cpu().numpy() if (ea is not None and ea.grad is not None) else None
                        return out.detach().cpu().numpy(), x_grad, ea_grad
                    else:
                        model.eval()
                        with torch.no_grad():
                            out = model(xi, ei, ea) if ea is not None else model(xi, ei)
                        return out.cpu().numpy(), None, None

                def forward_hetero_fast_(model, flat_x_dict, x_shapes_dict, flat_edge_dict, edge_cols_dict, edge_rels):
                    device = _get_device(model)
                    data_h = HeteroData()
                                      
                    for key, flat_x in flat_x_dict.items():
                        shape = x_shapes_dict[key]
                        data_h[key].x = torch.as_tensor(flat_x, dtype=torch.float32, device=device).view(shape[0], shape[1])
                                                              
                    for key, flat_edge in flat_edge_dict.items():                        
                        num_edges = edge_cols_dict[key]
                        edge_type = (edge_rels[key][0], key, edge_rels[key][1])  
                        if num_edges > 0:
                            data_h[edge_type].edge_index = torch.as_tensor(flat_edge, dtype=torch.long, device=device).view(2, num_edges)
                        else:
                            data_h[edge_type].edge_index = torch.empty((2, 0), dtype=torch.long, device=device)
                                                                   
                    model.eval()
                    with torch.no_grad():
                        out = model(data_h.x_dict, data_h.edge_index_dict)
                    return out.cpu().numpy()
                """);
        } catch (JepException e) {
            throw new RuntimeException("Failed to initialize Python environment", e);
        }
    }

    private void cachePythonReferences() {
        try {
            modelRef = modelInterpreter.getValue(modelName);
        } catch (JepException e) {
            throw new RuntimeException("Failed to cache Python references", e);
        }
    }

    /**
     * Compute a simple hash for a matrix dictionary (sum of each matrix hash).
     */
    private static int hashMatrixDict(Map<String, double[][]> dict) {
        int h = 0;
        for (double[][] m : dict.values()) {
            int rows = m.length;
            int cols = (rows == 0) ? 0 : m[0].length;
            h = 31 * h + rows;
            h = 31 * h + cols;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    h = 31 * h + Double.hashCode(m[i][j]);
                }
            }
        }
        return h;
    }

    private static int hashEdgeDict(Map<String, ArrayList<ArrayList<Integer>>> dict) {
        int h = 0;
        for (ArrayList<ArrayList<Integer>> edges : dict.values()) {
            if (edges == null || edges.isEmpty()) continue;
            ArrayList<Integer> src = edges.get(0);
            ArrayList<Integer> dst = edges.get(1);
            int num = src.size();
            h = 31 * h + num;
            for (int v : src) h = 31 * h + v;
            for (int v : dst) h = 31 * h + v;
        }
        return h;
    }

    public Object[] forward(Map<String, double[][]> xDict,
                            Map<String, ArrayList<ArrayList<Integer>>> edgeDict,
                            Map<String, double[][]> edge_attr,
                            List<TorchInputSpecs> gnnInputs,
                            boolean withgradients) {
        Object[] result = new Object[2];

        // compute a hash of the three inputs so we can reuse flattened arrays
        int curHash = Objects.hash(hashMatrixDict(xDict), hashEdgeDict(edgeDict), hashMatrixDict(edge_attr));
        boolean reuseFlat = (curHash == lastFlattenHash);

        try {
            if (xDict.size() == 1) {
                // GRAPH FLATTENING
                String nodeType = xDict.keySet().iterator().next();
                double[][] xData = xDict.get(nodeType);
                float[] flatX;
                int[] xShape;
                int[] flatEdge;
                int numEdges;
                float[] flatEA = null;
                int[] eaShape = null;

                if (reuseFlat) {
                    flatX = cachedFlatX;
                    xShape = cachedXShape;
                    flatEdge = cachedFlatEdge;
                    numEdges = cachedNumEdges;
                    flatEA = cachedFlatEA;
                    eaShape = cachedEAShape;
                } else {
                    flatX = flattenMatrix(xData);
                    xShape = new int[]{xData.length, xData.length > 0 ? xData[0].length : 0};

                    String edgeType = edgeDict.isEmpty() ? null : edgeDict.keySet().iterator().next();
                    ArrayList<ArrayList<Integer>> edges = edgeType != null ? edgeDict.get(edgeType) : null;
                    numEdges = (edges != null && !edges.isEmpty()) ? edges.get(0).size() : 0;
                    flatEdge = flattenEdges(edges);

                    if (!edge_attr.isEmpty()) {
                        String eaKey = edge_attr.keySet().iterator().next();
                        double[][] eaData = edge_attr.get(eaKey);
                        flatEA = flattenMatrix(eaData);
                        eaShape = new int[]{eaData.length, eaData.length > 0 ? eaData[0].length : 0};
                    }

                    // store for reuse
                    cachedFlatX = flatX;
                    cachedXShape = xShape;
                    cachedFlatEdge = flatEdge;
                    cachedNumEdges = numEdges;
                    cachedFlatEA = flatEA;
                    cachedEAShape = eaShape;
                    lastFlattenHash = curHash;
                }

                // perform the forward call using flattened arrays (either reused or just computed)
                long startTime = System.nanoTime();
                Object pythonResult = modelInterpreter.invoke(
                        "forward_single_fast_",
                        modelRef, flatX, xShape, flatEdge, numEdges, withgradients, flatEA, eaShape
                );
                long endTime = System.nanoTime();
                System.out.println("forward took " + ((endTime - startTime) / 1_000_000.0) + " milliseconds");

                List<Object> pyTuple = (List<Object>) pythonResult;
                NDArray outArray = (NDArray) pyTuple.get(0);
                result[0] = convertNDArrayTo2D(outArray);
                correctBinaryOutput(result[0], outArray.getDimensions());

                if (withgradients) {
                    Map<String, double[][]> gradsDict = new HashMap<>();
                    if (pyTuple.get(1) != null) gradsDict.put("x", convertNDArrayTo2D((NDArray) pyTuple.get(1)));
                    if (pyTuple.get(2) != null) gradsDict.put("ea", convertNDArrayTo2D((NDArray) pyTuple.get(2)));
                    result[1] = gradsDict.isEmpty() ? null : gradsDict;
                }

            } else {
                // HETEROGENEOUS GRAPH FLATTENING
                if (!edge_attr.isEmpty()) throw new RuntimeException("Edge attributes not implemented for hetero GNNs");
                if (withgradients) throw new RuntimeException("Gradients not implemented for hetero GNNs");

                Object[] flatXData;
                Object[] flatEdgeData;
                if (reuseFlat && cachedFlatXDict != null) {
                    flatXData = new Object[]{cachedFlatXDict, cachedXShapeDict};
                    flatEdgeData = new Object[]{cachedFlatEdgeDict, cachedEdgeCounts};
                } else {
                    flatXData = flattenXDict(xDict);
                    flatEdgeData = flattenEdgeDict(edgeDict);
                    // store
                    cachedFlatXDict = (Map<String,float[]>) flatXData[0];
                    cachedXShapeDict = (Map<String,int[]>) flatXData[1];
                    cachedFlatEdgeDict = (Map<String,int[]>) flatEdgeData[0];
                    cachedEdgeCounts = (Map<String,Integer>) flatEdgeData[1];
                    lastFlattenHash = curHash;
                }
                Map<String, String[]> edgeRels = buildEdgeRelations(gnnInputs);

                long startTime = System.nanoTime();
                Object pythonResult = modelInterpreter.invoke(
                        "forward_hetero_fast_",
                        modelRef, flatXData[0], flatXData[1], flatEdgeData[0], flatEdgeData[1], edgeRels
                );
                long endTime = System.nanoTime();
//                System.out.println("Operation took " + ((endTime - startTime) / 1_000_000.0) + " milliseconds");

                NDArray outArray = (NDArray) pythonResult;
                result[0] = convertNDArrayTo2D(outArray);
            }
            return result;

        } catch (JepException e) {
            System.err.println("Failed forward pass: " + e);
            return null;
        }
    }

    private static float[] flattenMatrix(double[][] matrix) {
        final int rows = matrix.length;
        if (rows == 0) return new float[0];

        final int cols = matrix[0].length;
        final float[] flat = new float[rows * cols];

        int idx = 0;
        for (int r = 0; r < rows; r++) {
            final double[] row = matrix[r];
            for (int c = 0; c < cols; c++) {
                flat[idx++] = (float) row[c];
            }
        }

        return flat;
    }

    private static int[] flattenEdges(ArrayList<ArrayList<Integer>> edges) {
        if (edges == null || edges.isEmpty()) return new int[0];

        final ArrayList<Integer> src = edges.get(0);
        final int numEdges = src.size();
        if (numEdges == 0) return new int[0];

        final ArrayList<Integer> dst = edges.get(1);
        final int[] flat = new int[2 * numEdges];

        for (int i = 0; i < numEdges; i++) {
            flat[i] = src.get(i);
            flat[numEdges + i] = dst.get(i);
        }

        return flat;
    }

    private static Object[] flattenXDict(Map<String, double[][]> xDict) {
        final int size = xDict.size();
        final Map<String, float[]> flatX = new HashMap<>(size);
        final Map<String, int[]> shapes = new HashMap<>(size);

        for (Map.Entry<String, double[][]> entry : xDict.entrySet()) {
            final String key = entry.getKey();
            final double[][] m = entry.getValue();

            final int rows = m.length;
            final int cols = (rows == 0) ? 0 : m[0].length;

            flatX.put(key, flattenMatrix(m));
            shapes.put(key, new int[]{rows, cols});
        }

        return new Object[]{flatX, shapes};
    }

    private static Object[] flattenEdgeDict(Map<String, ArrayList<ArrayList<Integer>>> edgeDict) {
        final int size = edgeDict.size();
        final Map<String, int[]> flatEdges = new HashMap<>(size);
        final Map<String, Integer> counts = new HashMap<>(size);

        for (Map.Entry<String, ArrayList<ArrayList<Integer>>> entry : edgeDict.entrySet()) {
            final String key = entry.getKey();
            final ArrayList<ArrayList<Integer>> edges = entry.getValue();

            int num = 0;
            int[] flat = new int[0];

            if (edges != null && !edges.isEmpty()) {
                final ArrayList<Integer> src = edges.get(0);
                num = src.size();
                if (num > 0) {
                    final ArrayList<Integer> dst = edges.get(1);
                    flat = new int[2 * num];
                    for (int i = 0; i < num; i++) {
                        flat[i] = src.get(i);
                        flat[num + i] = dst.get(i);
                    }
                }
            }

            flatEdges.put(key, flat);
            counts.put(key, num);
        }

        return new Object[]{flatEdges, counts};
    }

    private double[][] convertNDArrayTo2D(NDArray array) {
        Object raw = array.getData();
        double[] flatData = PyUtils.toDoubleArray(raw);
        int[] dims = array.getDimensions();
        if (dims.length == 2) return PyUtils.convertTo2D(flatData, dims[0], dims[1]);
        if (dims.length == 1) return new double[][]{flatData};
        throw new RuntimeException("Invalid output shape: " + Arrays.toString(dims));
    }

    private void correctBinaryOutput(Object outputObj, int[] dims) {
        if (dims.length == 2 && dims[0] == 1 && dims[1] == 2) {
            double[][] output = (double[][]) outputObj;
            if (output[0][0] == 1.0 || output[0][0] == 0.0) output[0][0] = 1.0 - output[0][1];
            if (output[0][1] == 1.0 || output[0][1] == 0.0) output[0][1] = 1.0 - output[0][0];
        }
    }

    private Map<String, String[]> buildEdgeRelations(List<TorchInputSpecs> gnnInputs) {
        Map<String, String[]> edgeRels = new HashMap<>();
        for (TorchInputSpecs input : gnnInputs) {
            edgeRels.put(input.getEdgeRelation().name(),
                    new String[]{input.getEdgeRelation().getTypes()[0].getName(), input.getEdgeRelation().getTypes()[1].getName()});
        }
        return edgeRels;
    }

    public SharedInterpreter getModelInterpreter() { return modelInterpreter; }

    @Override
    public String toString() {
        return "TorchModelWrapper{" +
                "pyModel=" + modelName +
                ", class=" + modelClassName +
                ", interpreter=" + modelInterpreter +
                '}';
    }
}