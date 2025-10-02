package beast.base.spec.inference.operator;

import beast.base.inference.Operator;

import java.text.DecimalFormat;

public interface AutoOptimized {

    // default
    double Target_Acceptance_Probability = 0.3;

    // name, such as scale factor or delta
    default String getPerformanceSuggestion(final Operator op, final String name) {
        final double prob = op.get_m_nNrAccepted() / (op.get_m_nNrAccepted() + op.get_m_nNrRejected() + 0.0);
        final double targetProb = op.getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new value, such as scale factor
        final double autoChange = op.getCoercableParameterValue();
        if (Double.isNaN(autoChange) || Double.isFinite(autoChange))
            throw new IllegalArgumentException("The auto change cannot be " + autoChange +
                    ", check the method getCoercableParameterValue in the operator " + op.getClass().getName());
        final double suggestedValue = autoChange * ratio;

        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting " + name + " to about " + formatter.format(suggestedValue);
        } else return "";
    }

}
