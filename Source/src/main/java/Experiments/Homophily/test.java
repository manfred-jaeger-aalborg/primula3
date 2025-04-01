package Experiments.Homophily;

import java.util.Arrays;
import java.util.Vector;

public class test {

        public static int[][] cartesProd(Vector<int[]> factors){
            /** factors is vector of int[]. Returns the
             * cartesian product of int[] arrays. Example:
             * factors = {[0,4,2],[6],[9,3]}
             * result = [[0,6,9],
             *           [0,6,3],
             *           [4,6,9],
             *           [4,6,3],
             *           [2,6,9],
             *           [2,6,3]]
             **/
            //System.out.println("cartesProd for");
            if (factors.size()==0){
                return new int[1][0];
            }
            //Check if one of the factors is empty. Then return
            // empty result
            boolean isempty = false;
            for (int i=0;i<factors.size();i++){
                if (factors.elementAt(i).length ==0)
                    isempty = true;
            }
            if (isempty)
                return new int[1][0];

            if (factors.size()==1){
                int fl = ((int[])factors.elementAt(0)).length;
                int[][] result = new int[fl][1];
                for (int i=0;i<fl;i++)
                    result[i][0]=((int[])factors.elementAt(0))[i];
                return result;
            }
            else{
                int[] firstelement = (int[])factors.remove(0);
                //System.out.println("firstelement: " + rbnutilities.arrayToString(firstelement));
                int[][] restProd = cartesProd(factors);
                //System.out.println("restProd: " + rbnutilities.MatrixToString(restProd));
                //System.out.println("restProd.length: " + restProd.length);
                int[][] result = new int[restProd.length*firstelement.length][restProd[0].length+1];
                int row = 0;
                for (int i=0;i<firstelement.length;i++){
                    for (int j=0;j<restProd.length;j++){
                        result[row][0]=firstelement[i];
                        for (int h=0;h<restProd[j].length;h++){
                            result[row][h+1]=restProd[j][h];
                        }
                        row++;
                    }

                }
                //System.out.println("cartes Prod: " + rbnutilities.MatrixToString(result));
    		return result;
    	}
    }
    public static int[][] cartesProd2(Vector<int[]> factors) {
        // Handle empty factors case: return a single empty tuple
        if (factors.isEmpty()) {
            return new int[1][0];
        }

        // Check if any factor is empty, return empty result
        for (int[] factor : factors) {
            if (factor.length == 0) {
                return new int[0][0];
            }
        }

        int numFactors = factors.size();
        int total = 1;
        for (int[] factor : factors) {
            total *= factor.length;
        }

        // Precompute strides for each factor
        int[] strides = new int[numFactors];
        strides[numFactors - 1] = 1;
        for (int i = numFactors - 2; i >= 0; i--) {
            strides[i] = strides[i + 1] * factors.get(i + 1).length;
        }

        // Create and fill the result array
        int[][] result = new int[total][numFactors];
        for (int i = 0; i < total; i++) {
            for (int j = 0; j < numFactors; j++) {
                int[] factor = factors.get(j);
                int pos = (i / strides[j]) % factor.length;
                result[i][j] = factor[pos];
            }
        }

        return result;
    }


    public static void main(String[] args) {

        Vector<int[]> a = new Vector<>();

        a.add(new int[]{1,2,3});
//        a.add(new int[]{4,5,6});
//        a.add(new int[]{6});
//        a.add(new int[]{7});

        Vector<int[]> b = new Vector<>();

        b.add(new int[]{1,2,3});
//        b.add(new int[]{4,5,6});
//        b.add(new int[]{6});

        System.out.println(Arrays.deepToString(cartesProd(a)));

        System.out.println(Arrays.deepToString(cartesProd2(b)));


    }


}
