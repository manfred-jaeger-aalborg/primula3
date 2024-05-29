package Experiments;

import RBNinference.MapVals;
import RBNinference.SampleProbs;

import java.util.Observable;
import java.util.Observer;

public class ValueObserver implements Observer {

    private int[] mapVals;
    private double[] probs;
    private double[] minprobs;
    private double[] maxprobs;
    private double[] variance;
    private double samplesize;
    private String likelihood;

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof MapVals) {
            mapVals = ((MapVals) o).getMVs();
            likelihood = ((MapVals) o).getLLstring();
        }
        if (o instanceof SampleProbs) {
            probs = ((SampleProbs) o).getProbs();
            minprobs = ((SampleProbs) o).getMinProbs();
            maxprobs = ((SampleProbs) o).getMaxProbs();
            variance = ((SampleProbs) o).getVar();
            samplesize = ((SampleProbs)o).getSize();
        }
    }

    public double[] getProbs() {
        return probs;
    }

    public double[] getMinprobs() {
        return minprobs;
    }

    public double[] getMaxprobs() {
        return maxprobs;
    }

    public double[] getVariance() {
        return variance;
    }

    public double getSamplesize() {
        return samplesize;
    }

    public int[] getMapVals() {
        return mapVals;
    }

    public String getLikelihood() {
        return likelihood;
    }
}
