package PyManager;

public class PyUtils {

    public static double[][] convertTo2D(float[] inputArray, int rows, int cols) {
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
}
