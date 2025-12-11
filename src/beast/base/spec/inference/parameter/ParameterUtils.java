package beast.base.spec.inference.parameter;

import beast.base.inference.StateNode;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Tensor;
import beast.base.spec.type.TypeUtils;
import beast.base.spec.type.Vector;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterUtils {

    /**
     * This method is used by {@link StateNode#fromXML(Node)} for a {@link Scalar},
     * and has to be consistent with the method {@link StateNode#toString()} for creating XML.
     * <p>
     * For example, <code>kappa{[0.0,Infinity]}: 29</code>,
     * or <code>freqs{4, [0.0,1.0]}: 0.25 0.25 0.25 0.25</code>.
     * If no bounds, then <code>isEstimated: true</code>, or <code>isSelected{2}: true false</code>.
     *
     * @param node     XML node
     * @param param    a parameter which is also a {@link StateNode}
     */
    public static void parseParameter(final Node node, StateNode param) {

        final NamedNodeMap atts = node.getAttributes();
        // set ID from XML
        param.setID(atts.getNamedItem("id").getNodeValue());
        final String str = node.getTextContent();

        // need to sync with toString
        Pattern boundedPattern = Pattern.compile("^.*" + // id
                // shape is optional, empty for scalar, or size of vector, or [3, 4] for matrix
                "\\{" + "(?:(\\d+|\\[\\d+,\\s*\\d+\\]),\\s*)?" +
                "[\\[\\(](.*),(.*)[\\]\\)]" + "\\}" + // bounds
                ":\\s*(.*)\\s*$"); // value(s)
        Matcher matcher1 = boundedPattern.matcher(str);

        Pattern noboundPattern = Pattern.compile("^.*" + // id
                // shape is optional, empty for scalar, or size of vector, or [3, 4] for matrix
                "(?:\\{(\\d+|\\[\\d+,\\s*\\d+\\])\\})?" +
                ":\\s*(.*)\\s*$"); // value(s)
        Matcher matcher2 = noboundPattern.matcher(str);

        // scalar and vector use shape for validation, but it is compulsory for matrix.

        if (matcher1.matches()) { // with bounds
            // id is already assigned
            final String shape = matcher1.group(1);
            final String lower = matcher1.group(2);
            final String upper = matcher1.group(3);
            final String valuesAsString = matcher1.group(4);

            final String[] valuesStr = valuesAsString.split(" ");
            if (param instanceof RealScalarParam<?> realScalarParam) {
                realScalarParam.fromXML(shape, valuesStr);
            } else if (param instanceof BoundedParam<?> boundedParam) { //TODO
                boundedParam.fromXML(lower, upper, shape, valuesStr);
            } else
                throw new RuntimeException("Unknown parameter type : " + param.getClass().getName());

        } else if (matcher2.matches()) { // without bounds

            final String shape = matcher2.group(1); // null for scalar
            final String valuesAsString = matcher2.group(2);
            final String[] valuesStr = valuesAsString.split(" ");

            if (param instanceof BoolScalarParam boolScalar) {
                boolScalar.fromXML(valuesStr[0]);
            } else if (param instanceof BoolVectorParam boolVector) {
                boolVector.fromXML(valuesStr);
//TODO            } else if (param instanceof BoolMatrixParam boolMatrix) {
            } else
                throw new RuntimeException("Unknown parameter type : " + param.getClass().getName());

        } else {
            throw new RuntimeException("String could not be parsed to parameter : " + str);
        }
    }

    /**
     * This method is used by {@link StateNode#toString()}.
     * @see #parseParameter(Node, StateNode)
     * @param param   a parameter
     * @return  kappa{[0.0,Infinity]}: 29  or
     *          freqs{4, [0.0,1.0]}: 0.25 0.25 0.25 0.25
     */
    public static String paramToString(StateNode param) {
        String str = param.getID();
        if (param instanceof Tensor<?,?> tensor) {
            str += "{";
            String shapeStr = TypeUtils.shapeToString(tensor);
            // empty for scalar, or size of vector, or [3, 4] for matrix.
            // scalar and vector use shape for validation, but it is compulsory for matrix.
            if (!shapeStr.isEmpty())
                str += shapeStr + ", ";
            if (param instanceof BoundedParam<?> boundedParam)
                str += boundedParam.boundsToString();
            // check if nothing inside { }
            if (str.endsWith("{"))
                str = str.substring(0, str.length() - 1);
            else
                str += "}"; // close {
        }
        str += ": ";
        if  (param instanceof Scalar scalar)
            str += scalar.get();
        else if  (param instanceof Vector vector) {
            List elements = vector.getElements();
            for (Object element : elements)
                str += element.toString() + " ";
        }

        return str; //+ " ";
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
