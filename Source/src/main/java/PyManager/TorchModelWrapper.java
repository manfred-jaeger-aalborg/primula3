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
    private Map<String, String[]> cachedEdgeRels = null;

    private Object[] cachedHomoResult = new Object[2];
    private Object[] cachedHeteroResult = new Object[1];
    private int lastHeteroHash = 0;

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

            modelInterpreter.exec("""
                def _get_device(model):
                    param = next(model.parameters(), None)
                    return param.device if param is not None else torch.device("cpu")
                
                def forward_single_fast_(model, flat_x, x_shape, flat_edge, num_edges, with_gradients, flat_ea, ea_shape, target_row=None):
                    device = _get_device(model)
                    model.eval()
                    xi = torch.as_tensor(flat_x, dtype=torch.float32, device=device).view(x_shape[0], x_shape[1])
                    if num_edges > 0:
                        ei = torch.as_tensor(flat_edge, dtype=torch.long, device=device).view(2, num_edges)
                    else:
                        ei = torch.empty((2, 0), dtype=torch.long, device=device)
                    
                    ea = None
                    if flat_ea is not None:
                        ea = torch.as_tensor(flat_ea, dtype=torch.float32, device=device).view(ea_shape[0], ea_shape[1])

                    if with_gradients:
                        # gradients of the target node's output only: x_grads[c, k, f] = d out[target_row, c] / d x[k, f]
                        # (and the same for edge attributes). One backward pass per class.
                        if target_row is None or target_row < 0:
                            raise ValueError("forward_single_fast_: gradients need the target row")
                        with torch.enable_grad():
                            xg = xi.detach().clone().requires_grad_(True)
                            eag = ea.detach().clone().requires_grad_(True) if ea is not None else None
                            out = model(xg, ei, eag) if eag is not None else model(xg, ei)
                            row = out[target_row] if out.dim() == 2 else out[target_row:target_row + 1]
                            inputs = [xg] if eag is None else [xg, eag]
                            x_grads, ea_grads = [], []
                            for c in range(row.shape[0]):
                                g = torch.autograd.grad(row[c], inputs, retain_graph=True, allow_unused=True)
                                x_grads.append(g[0] if g[0] is not None else torch.zeros_like(xg))
                                if eag is not None:
                                    ea_grads.append(g[1] if g[1] is not None else torch.zeros_like(eag))
                        x_grads = torch.stack(x_grads).cpu().numpy()
                        ea_grads = torch.stack(ea_grads).cpu().numpy() if eag is not None else None
                        return out.detach().cpu().numpy(), x_grads, ea_grads
                    else:
                        with torch.no_grad():
                            out = model(xi, ei, ea) if ea is not None else model(xi, ei)
                        return out.cpu().numpy(), None, None

                _hetero_device = None
                _hetero_x_tensors = {}
                _hetero_ei_tensors = {}

                def _init_hetero_device(model):
                    global _hetero_device
                    if _hetero_device is None:
                        _hetero_device = _get_device(model)
                        model.eval()

                def forward_hetero_fast_(model, flat_x_dict, x_shapes_dict, flat_edge_dict, edge_cols_dict, edge_rels):
                    global _hetero_device, _hetero_x_tensors, _hetero_ei_tensors

                    _init_hetero_device(model)
                    device = _hetero_device

                    x_dict = {}
                    for key, flat_x in flat_x_dict.items():
                        rows, cols = x_shapes_dict[key]
                        cached = _hetero_x_tensors.get(key)
                        if cached is not None and cached.shape == (rows, cols):
                            cached.copy_(torch.as_tensor(flat_x, dtype=torch.float32).view(rows, cols))
                            x_dict[key] = cached
                        else:
                            t = torch.as_tensor(flat_x, dtype=torch.float32, device=device).view(rows, cols)
                            _hetero_x_tensors[key] = t
                            x_dict[key] = t
                 
                    edge_index_dict = {}
                    for key, flat_edge in flat_edge_dict.items():
                        num_edges = edge_cols_dict[key]
                        src_type, dst_type = edge_rels[key]
                        edge_type = (src_type, key, dst_type)

                        if num_edges > 0:
                            cached = _hetero_ei_tensors.get(key)
                            if cached is not None and cached.shape == (2, num_edges):
                                cached.copy_(torch.as_tensor(flat_edge, dtype=torch.long).view(2, num_edges))
                                edge_index_dict[edge_type] = cached
                            else:
                                t = torch.as_tensor(flat_edge, dtype=torch.long, device=device).view(2, num_edges)
                                _hetero_ei_tensors[key] = t
                                edge_index_dict[edge_type] = t
                        else:
                            edge_index_dict[edge_type] = torch.empty((2, 0), dtype=torch.long, device=device)

                    with torch.no_grad():
                        out = model(x_dict, edge_index_dict)
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

    private static int hashMatrixDict(Map<String, double[][]> dict) {
        int h = 0;
        for (double[][] m : dict.values()) {
            h = 31 * h + Arrays.deepHashCode(m);
        }
        return h;
    }

    private static int hashEdgeDict(Map<String, ArrayList<ArrayList<Integer>>> dict) {
        int h = 0;
        for (ArrayList<ArrayList<Integer>> edges : dict.values()) {
            if (edges == null || edges.isEmpty()) continue;
            h = 31 * h + edges.hashCode();
        }
        return h;
    }

    public synchronized Object[] forward(Map<String, double[][]> xDict,
                                         Map<String, ArrayList<ArrayList<Integer>>> edgeDict,
                                         Map<String, double[][]> edge_attr,
                                         List<TorchInputSpecs> gnnInputs,
                                         boolean withgradients, Integer targetRow) {

        // targetRow: row of x whose output the gradients are taken of (only used when withgradients)
        int curHash = Objects.hash(hashMatrixDict(xDict), hashEdgeDict(edgeDict), hashMatrixDict(edge_attr), targetRow);
        boolean reuseFlat = (curHash == lastFlattenHash);

        try {
            if (xDict.size() == 1) {
                String nodeType = xDict.keySet().iterator().next();
                double[][] xData = xDict.get(nodeType);
                float[] flatX;
                int[] xShape;
                int[] flatEdge;
                int numEdges;
                float[] flatEA = null;
                int[] eaShape = null;

                // reuse only if the cached result has what this call needs
                if (reuseFlat && cachedHomoResult[0] != null && (!withgradients || cachedHomoResult[1] != null)) {
                    return cachedHomoResult;
                }

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

                lastFlattenHash = curHash;

                Object pythonResult = modelInterpreter.invoke(
                        "forward_single_fast_",
                        modelRef, flatX, xShape, flatEdge, numEdges, withgradients, flatEA, eaShape, targetRow
                );

                List<Object> pyTuple = (List<Object>) pythonResult;
                NDArray outArray = (NDArray) pyTuple.get(0);
                cachedHomoResult[0] = convertNDArrayTo2D(outArray);
                correctBinaryOutput(cachedHomoResult[0], outArray.getDimensions());

                // gradients: [class][row][col], see forward_single_fast_
                cachedHomoResult[1] = null;  // never keep gradients of an older input
                if (withgradients) {
                    Map<String, double[][][]> gradsDict = new HashMap<>();
                    if (pyTuple.get(1) != null) gradsDict.put("x", convertNDArrayTo3D((NDArray) pyTuple.get(1)));
                    if (pyTuple.get(2) != null) gradsDict.put("ea", convertNDArrayTo3D((NDArray) pyTuple.get(2)));
                    cachedHomoResult[1] = gradsDict;
                }

                return cachedHomoResult;

            } else {
                if (!edge_attr.isEmpty()) throw new RuntimeException("Edge attributes not implemented for hetero GNNs");
                if (withgradients)       throw new RuntimeException("Gradients not implemented for hetero GNNs");

                if (curHash == lastHeteroHash && cachedHeteroResult[0] != null) {
                    return cachedHeteroResult;
                }

                // Build / reuse flattened arrays
                Object[] flatXData;
                Object[] flatEdgeData;
                if (reuseFlat && cachedFlatXDict != null) {
                    flatXData  = new Object[]{cachedFlatXDict, cachedXShapeDict};
                    flatEdgeData = new Object[]{cachedFlatEdgeDict, cachedEdgeCounts};
                } else {
                    flatXData    = flattenXDict(xDict);
                    flatEdgeData = flattenEdgeDict(edgeDict);
                    cachedFlatXDict   = (Map<String, float[]>)   flatXData[0];
                    cachedXShapeDict  = (Map<String, int[]>)      flatXData[1];
                    cachedFlatEdgeDict= (Map<String, int[]>)      flatEdgeData[0];
                    cachedEdgeCounts  = (Map<String, Integer>)    flatEdgeData[1];
                    lastFlattenHash   = curHash;
                }

                // Edge relations never change: build once and reuse
                if (cachedEdgeRels == null) {
                    cachedEdgeRels = buildEdgeRelations(gnnInputs);
                }

                Object pythonResult = modelInterpreter.invoke(
                        "forward_hetero_fast_",
                        modelRef,
                        flatXData[0], flatXData[1],
                        flatEdgeData[0], flatEdgeData[1],
                        cachedEdgeRels
                );

                NDArray outArray = (NDArray) pythonResult;
                cachedHeteroResult[0] = convertNDArrayTo2D(outArray);
                lastHeteroHash = curHash;

                return cachedHeteroResult;
            }

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
            for (int c = 0; c < cols; c++) flat[idx++] = (float) row[c];
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
        final Map<String, float[]> flatX  = new HashMap<>(xDict.size());
        final Map<String, int[]>   shapes = new HashMap<>(xDict.size());
        for (Map.Entry<String, double[][]> e : xDict.entrySet()) {
            final double[][] m = e.getValue();
            final int rows = m.length, cols = rows == 0 ? 0 : m[0].length;
            flatX.put(e.getKey(), flattenMatrix(m));
            shapes.put(e.getKey(), new int[]{rows, cols});
        }
        return new Object[]{flatX, shapes};
    }

    private static Object[] flattenEdgeDict(Map<String, ArrayList<ArrayList<Integer>>> edgeDict) {
        final Map<String, int[]>    flatEdges = new HashMap<>(edgeDict.size());
        final Map<String, Integer>  counts    = new HashMap<>(edgeDict.size());
        for (Map.Entry<String, ArrayList<ArrayList<Integer>>> e : edgeDict.entrySet()) {
            final ArrayList<ArrayList<Integer>> edges = e.getValue();
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
            flatEdges.put(e.getKey(), flat);
            counts.put(e.getKey(), num);
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

    private double[][][] convertNDArrayTo3D(NDArray array) {
        double[] flat = PyUtils.toDoubleArray(array.getData());
        int[] dims = array.getDimensions();
        if (dims.length != 3) throw new RuntimeException("Expected 3-D gradients, got shape " + Arrays.toString(dims));
        double[][][] out = new double[dims[0]][][];
        int block = dims[1] * dims[2];
        for (int c = 0; c < dims[0]; c++)
            out[c] = PyUtils.convertTo2D(Arrays.copyOfRange(flat, c * block, (c + 1) * block), dims[1], dims[2]);
        return out;
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