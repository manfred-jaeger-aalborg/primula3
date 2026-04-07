package RBNpackage.VarTermPackage;

import java.util.HashMap;
import java.util.Map;

public class LinearForm {
    private final Map<String, Integer> coefficients;
    private final int constant;

    public LinearForm(Map<String, Integer> coefficients, int constant) {
        this.coefficients = new HashMap<>(coefficients);
        this.constant = constant;
    }

    public LinearForm add(LinearForm other) {
        Map<String, Integer> newCoeffs = new HashMap<>(this.coefficients);
        other.coefficients.forEach((k, v) -> newCoeffs.merge(k, v, Integer::sum));
        return new LinearForm(newCoeffs, this.constant + other.constant);
    }

    public LinearForm subtract(LinearForm other) {
        Map<String, Integer> newCoeffs = new HashMap<>(this.coefficients);
        other.coefficients.forEach((k, v) -> newCoeffs.merge(k, -v, Integer::sum));
        return new LinearForm(newCoeffs, this.constant - other.constant);
    }

    public Map<String, Integer> getCoefficients() { return coefficients; }
    public int getConstant() { return constant; }
}