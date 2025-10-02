package beast.base.spec.inference.operator;

import beast.base.core.Description;

import java.text.DecimalFormat;


@Description("Operator with a flexible kernel distribution")
public class OptimizeUtils {

    public static final double Target_Acceptance_Probability = 0.3;

    public static String getPerformanceSuggestion(final int nAccepted, final int nRejected,
                                                  final double targetProb, final double delta) {
        final double prob = nAccepted / (nAccepted + nRejected + 0.0);
//        final double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        final double newDelta = delta * ratio;

        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else if (prob > 0.40) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else return "";
    }

}
