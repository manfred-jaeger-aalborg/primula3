package PyManager;

import RBNpackage.CatGnn;
import RBNpackage.CatRel;
import RBNpackage.Rel;

import java.util.ArrayList;
import java.util.List;


public class PyUtils {

    public record EvalEntry(
            Object evaluatedNode,   // GGCPMNode or CPModel (known value)
            List<int[]> argNodes,   // node index pairs / singles
            double evalValue,       // evaluated probability
            int[] tuple,            // substitution tuple
            int probFormIdx         // column position
    ) {}

    public static double[][] convertTo2D(double[] inputArray, int rows, int cols) {
        if (inputArray.length != rows * cols) {
            throw new IllegalStateException("The length of the input array does not match the provided dimensions.");
        }

        double[][] outputArray = new double[rows][cols];
        for (int i = 0; i < inputArray.length; i++) {
            int row = i / cols;
            int col = i % cols;
            outputArray[row][col] = (double) inputArray[i];
        }
        return outputArray;
    }

    public static double[] toDoubleArray(Object data) {
        if (data == null) return new double[0];

        if (data instanceof double[]) return (double[]) data;

        if (data instanceof float[]) {
            float[] f = (float[]) data;
            double[] d = new double[f.length];
            for (int i = 0; i < f.length; i++) d[i] = f[i];
            return d;
        }

        if (data instanceof int[]) {
            int[] a = (int[]) data;
            double[] d = new double[a.length];
            for (int i = 0; i < a.length; i++) d[i] = a[i];
            return d;
        }

        if (data instanceof long[]) {
            long[] a = (long[]) data;
            double[] d = new double[a.length];
            for (int i = 0; i < a.length; i++) d[i] = a[i];
            return d;
        }

        if (data instanceof Double[]) {
            Double[] arr = (Double[]) data;
            double[] d = new double[arr.length];
            for (int i = 0; i < arr.length; i++) d[i] = arr[i] == null ? Double.NaN : arr[i];
            return d;
        }

        if (data instanceof Float[]) {
            Float[] arr = (Float[]) data;
            double[] d = new double[arr.length];
            for (int i = 0; i < arr.length; i++) d[i] = arr[i] == null ? Double.NaN : arr[i];
            return d;
        }

        if (data instanceof Number[]) {
            Number[] arr = (Number[]) data;
            double[] d = new double[arr.length];
            for (int i = 0; i < arr.length; i++) d[i] = arr[i] == null ? Double.NaN : arr[i].doubleValue();
            return d;
        }

        throw new IllegalArgumentException("Unsupported data type: " + data.getClass());
    }
}
