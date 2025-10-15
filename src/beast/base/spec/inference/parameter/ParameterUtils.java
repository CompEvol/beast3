package beast.base.spec.inference.parameter;

import beast.base.inference.StateNode;
import beast.base.spec.type.Scalar;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterUtils {

    public static void parseScalarParameter(final Node node, Scalar scalar) {
        if (scalar instanceof StateNode stateNode) {
            final NamedNodeMap atts = node.getAttributes();
            stateNode.setID(atts.getNamedItem("id").getNodeValue());
            final String str = node.getTextContent();
            // need to sync with toString
//        Pattern pattern = Pattern.compile(".*[\\[(](.*),(.*)[\\])]: (.*) ");
            // (?: ... )? makes the whole bracketed section optional.
            Pattern pattern = Pattern.compile(".*(?:[\\[(](.*),(.*)[\\])] )?: (.*) ");
            Matcher matcher = pattern.matcher(str);
            if (matcher.matches()) {
//            final String dimension = matcher.group(1);
                final String lower = matcher.group(1);
                final String upper = matcher.group(2);
                final String valuesAsString = matcher.group(3);
//            final String[] values = valuesAsString.split(" ");
//            minorDimension = 0;

//                scalarParam.fromXML(valuesAsString, lower, upper);


            } else {
                throw new RuntimeException("String could not be parsed to parameter : " + str);
            }
        } else
            throw new IllegalArgumentException("Parameter should be a StateNode ! ");
    }

    public static String scalarParamToString(Scalar scalar) {
        final StringBuilder buf = new StringBuilder();
//        buf.append(scalarParam.getID());
//        // bounds are optional
//        if (scalarParam instanceof Bounded bounded)
//            buf.append(bounded.boundsToString());
//        buf.append(": ").append(scalarParam.get()).append(" ");
        return buf.toString();
    }




//    public static <D extends Real>Bounded<Double> initBounds(
//            Input<Double> lowerInput, Input<Double> upperInput,
//            D domain, double currentLower, double currentUpper) {
//        double lower = (lowerInput.get() != null) ? lowerInput.get() : currentLower;
//        double upper = (upperInput.get() != null) ? upperInput.get() : currentUpper;
//
//        lower = Math.max(lower, domain.getLower());
//        upper = Math.min(upper, domain.getUpper());
//
//        return new Bounded.BoundedReal(lower, upper);
//    }
//
//    public static <D extends Int>Bounded<Integer> initBounds(
//            Input<Integer> lowerInput, Input<Integer> upperInput,
//            D domain, int currentLower, int currentUpper) {
//        int lower = (lowerInput.get() != null) ? lowerInput.get() : currentLower;
//        int upper = (upperInput.get() != null) ? upperInput.get() : currentUpper;
//
//        lower = Math.max(lower, domain.getLower());
//        upper = Math.min(upper, domain.getUpper());
//
//        return new Bounded.BoundedInt(lower, upper);
//    }

}
