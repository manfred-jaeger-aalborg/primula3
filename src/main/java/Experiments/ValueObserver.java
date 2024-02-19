package Experiments;

import RBNinference.MapVals;

import java.util.Observable;
import java.util.Observer;

public class ValueObserver implements Observer {

    private int[] mapVals;
    private String likelihood;

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof MapVals) {
            mapVals = ((MapVals) o).getMVs();
            likelihood = ((MapVals) o).getLLstring();
        }
    }

    public int[] getMapVals() {
        return mapVals;
    }

    public String getLikelihood() {
        return likelihood;
    }
}
