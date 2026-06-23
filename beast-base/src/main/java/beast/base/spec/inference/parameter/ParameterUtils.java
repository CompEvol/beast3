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
     * The XML node's {@code id} attribute is set on {@code param} directly; the node's text
     * content is the full {@link #paramToString(StateNode)} output, which still begins with the
     * parameter ID.  In BEAST 3, bounds are derived from the domain at runtime and are never
     * written to the state file.  Expected formats:
     * <ul>
     *   <li>scalar: {@code kappa: 29}</li>
     *   <li>vector: {@code freqs{4}: 0.25 0.25 0.25 0.25}</li>
     *   <li>boolean scalar: {@code isEstimated: true}</li>
     *   <li>boolean vector: {@code isSelected{2}: true false}</li>
     * </ul>
     * A state file entry in the BEAST 2 format — where explicit bounds are embedded as
     * {@code kappa [0.0 Infinity] (0.0,Infinity): 29 } — is rejected with
     * {@link IllegalArgumentException}.
     *
     * @param node   XML node whose text content is the serialized parameter string
     * @param param  the target {@link StateNode} to restore
     * @throws IllegalArgumentException if the string matches the BEAST 2 bounded parameter format
     * @throws RuntimeException         if the string format is unrecognised
     */
    public static void parseParameter(final Node node, StateNode param) {

        final NamedNodeMap atts = node.getAttributes();
        final String id = atts.getNamedItem("id").getNodeValue();
        param.setID(id);
        final String str = node.getTextContent();

        // beast2 cases: 1. hky.frequencies[4 1] (-Infinity,Infinity): 0.2 0.2 0.2 0.4
        //               2. hky.kappa[1 1] (0.0,Infinity): 5.0
        Pattern b2pattern1 = Pattern.compile("^.*\\[(.*) (.*)\\].*\\((.*),(.*)\\):\\s*(.*)\\s*$");
        Pattern b2pattern2 = Pattern.compile(".*\\[(.*)\\].*\\((.*),(.*)\\):\\s*(.*)\\s*$");
        if (b2pattern1.matcher(str).matches() || b2pattern2.matcher(str).matches()) {
            throw new IllegalArgumentException("XML file entry '" + str +
                    " is BEAST 2 version, please use BEAST 3 !");
        }

        // str is the full paramToString() output; the ID prefix is NOT pre-stripped.
        // Examples: "hky.kappa: 2.5"  or  "freqParameter.s:primate{4}: 0.25 0.25 0.25 0.25"
        //
        // Segment 1 — ^.*?
        //   Non-greedy wildcard that skips the parameter ID.  Non-greedy is required
        //   because the ID may itself contain colons (e.g. "freqParameter.s:primate"),
        //   so greedy .* would overshoot past the shape token {N}.
        //
        // Segment 2 — (?:\{(\d+|\[\d+,\s*\d+\])\})?
        //   The whole segment is optional (?:...)?  — absent for scalar parameters.
        //   \{  \}       literal braces that wrap the shape token
        //   (...)        capturing group(1): the shape token itself, two alternatives:
        //     \d+          vector: one or more digits, e.g. "4"  → matches {4}
        //     |
        //     \[\d+,\s*\d+\]  matrix: literal "[", digits (rows), comma, optional
        //                     whitespace \s*, digits (cols), literal "]"
        //                     e.g. "[2,3]" or "[2, 3]"  → matches {[2,3]}
        //   group(1) is null when the segment is absent (scalar).
        //
        // Segment 3 — :(?=[^:]*$)\s*(.*?)\s*$
        //   :            literal colon — the key-value separator
        //   (?=[^:]*$)   lookahead: [^:]* matches zero or more non-colon characters,
        //                anchored to $ (end of string).  This asserts that no further
        //                colon exists after this one, so we always match the LAST colon
        //                even when the ID contains colons.
        //   \s*          skips optional whitespace between the colon and the value
        //   (.*?)        capturing group(2): the value string (non-greedy, so the
        //                trailing \s* below can absorb whitespace rather than group(2))
        //   \s*$         absorbs trailing whitespace (e.g. the space paramToString()
        //                appends after each vector element) without including it in group(2)
        Pattern noboundPattern = Pattern.compile("^.*?" +
                "(?:\\{(\\d+|\\[\\d+,\\s*\\d+\\])\\})?" +
                ":(?=[^:]*$)\\s*(.*?)\\s*$");
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
