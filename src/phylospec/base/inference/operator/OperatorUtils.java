package phylospec.base.inference.operator;

import beast.base.util.Randomizer;
import phylospec.base.inference.ScalarParam;

public class OperatorUtils {


    public static Double getRandomDouble(double value, Double lower, Double upper,
                                         boolean useGaussian, final double windowSize) {
        double newValue = value;
        if (useGaussian) {
            newValue += Randomizer.nextGaussian() * windowSize;
        } else {
            newValue += Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }

        if (newValue < lower || newValue > upper) {
            return null;
        }
        if (newValue == value) {
            // this saves calculating the posterior
            return null;
        }
        return newValue;
    }


}
