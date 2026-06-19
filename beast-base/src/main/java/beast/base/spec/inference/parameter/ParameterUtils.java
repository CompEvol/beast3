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

/**
 * Utility methods for serializing and deserializing parameter values to/from XML.
 * Used by parameter {@link beast.base.inference.StateNode#toString()} and
 * {@link beast.base.inference.StateNode#fromXML(org.w3c.dom.Node)} implementations.
 */
public class ParameterUtils {

    /**
     * This method is used by {@link StateNode#fromXML(Node)} to restore a parameter from its
     * serialized state-file string, and must stay consistent with {@link #paramToString(StateNode)}.
     * <p>
     * In BEAST3, bounds are derived from the parameter's domain and are never written to the
     * state file. The expected format is therefore always bound-free:
     * <ul>
     *   <li>scalar: {@code kappa: 29}</li>
     *   <li>vector: {@code freqs{4}: 0.25 0.25 0.25 0.25}</li>
     *   <li>boolean scalar: {@code isEstimated: true}</li>
     *   <li>boolean vector: {@code isSelected{2}: true false}</li>
     * </ul>
     * A state file entry that still contains explicit bounds (BEAST2 legacy format such as
     * {@code kappa{[0.0,Infinity]}: 29}) is rejected with {@link IllegalArgumentException}.
     *
     * @param node   XML node whose text content is the serialized parameter string
     * @param param  the target {@link StateNode} to restore
     * @throws IllegalArgumentException if the string contains legacy explicit bounds
     * @throws RuntimeException         if the string format is unrecognised
     */
    public static void parseParameter(final Node node, StateNode param) {

        final NamedNodeMap atts = node.getAttributes();
        param.setID(atts.getNamedItem("id").getNodeValue());
        final String str = node.getTextContent();

        // Explicit bounds in state files are a BEAST2 legacy format.
        // In BEAST3, bounds are derived from the domain (see BoundedParam removal).
        // Fail fast so the user knows to restart rather than resume from such a file.
        Pattern boundedPattern = Pattern.compile("^.*" +
                "\\{" + "(?:(\\d+|\\[\\d+,\\s*\\d+\\]),\\s*)?" +
                "[\\[\\(](.*),(.*)[\\]\\)]" + "\\}" +
                ":\\s*(.*)\\s*$");
        if (boundedPattern.matcher(str).matches()) {
            throw new IllegalArgumentException(
                    "XML file entry '" + str + "' contains explicit bounds, which are not " +
                    "supported in BEAST3. Bounds are now derived from the parameter domain; " +
                    "values can be constrained further using a prior distribution.");
        }

        // All BEAST3 parameter types serialize without explicit bounds.
        // The non-greedy prefix (.*?) ensures the optional {shape} group is captured
        // for vector types (e.g. "freqs{4}: 0.25 ..."), and the non-greedy suffix (.*?)
        // lets the trailing \s* absorb any whitespace the vector loop appends.
        // Format: id{shape}: value(s)  — {shape} is absent for scalars.
        Pattern noboundPattern = Pattern.compile("^.*?" +
                "(?:\\{(\\d+|\\[\\d+,\\s*\\d+\\])\\})?" +
                ":\\s*(.*?)\\s*$");
        Matcher matcher = noboundPattern.matcher(str);

        if (matcher.matches()) {
            final String shape = matcher.group(1);         // null for scalars
            final String valuesAsString = matcher.group(2);
            final String[] valuesStr = valuesAsString.split("\\s+");

            if (param instanceof RealScalarParam<?> realScalarParam) {
                realScalarParam.fromXML(shape, valuesStr);
            } else if (param instanceof IntScalarParam<?> intScalarParam) {
                intScalarParam.fromXML(shape, valuesStr);
            } else if (param instanceof RealVectorParam<?> realVectorParam) {
                realVectorParam.fromXML(shape, valuesStr);
            } else if (param instanceof IntVectorParam<?> intVectorParam) {
                intVectorParam.fromXML(shape, valuesStr);
            } else if (param instanceof BoolScalarParam boolScalar) {
                boolScalar.fromXML(valuesStr[0]);
            } else if (param instanceof BoolVectorParam boolVector) {
                boolVector.fromXML(valuesStr);
            } else
                throw new RuntimeException("Unknown parameter type : " + param.getClass().getName());
        } else {
            throw new RuntimeException("String could not be parsed to parameter : " + str);
        }
    }

    /**
     * Serializes a parameter to a bound-free string for state-file persistence.
     * Must stay consistent with {@link #parseParameter(Node, StateNode)}.
     * <p>
     * Format:
     * <ul>
     *   <li>scalar: {@code kappa: 29}</li>
     *   <li>vector: {@code freqs{4}: 0.25 0.25 0.25 0.25}</li>
     *   <li>boolean scalar: {@code isEstimated: true}</li>
     *   <li>boolean vector: {@code isSelected{2}: true false}</li>
     * </ul>
     * Bounds are not written; they are derived from the domain at runtime.
     *
     * @param param a parameter
     * @return the serialized string
     */
    public static String paramToString(StateNode param) {
        String str = param.getID();
        if (param instanceof Tensor<?,?> tensor) {
            str += "{";
            String shapeStr = TypeUtils.shapeToString(tensor);
            // Empty for scalars; size for vectors; [r,c] for matrices.
            // Scalars drop the braces entirely; vectors and matrices keep them
            // so parseParameter can validate the element count on restore.
            if (!shapeStr.isEmpty())
                str += shapeStr;
            if (str.endsWith("{"))
                str = str.substring(0, str.length() - 1); // scalar: remove empty braces
            else
                str += "}";
        }
        str += ": ";
        if (param instanceof Scalar scalar)
            str += scalar.get();
        else if (param instanceof Vector vector) {
            List elements = vector.getElements();
            for (Object element : elements)
                str += element.toString() + " ";
        }

        return str;
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
