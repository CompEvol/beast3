package beast.base.spec.parameter;

import beast.base.spec.Bounded;
import beast.base.spec.inference.StateNode;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Tensor;
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


}
