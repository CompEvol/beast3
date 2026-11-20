package beast.pkgmgmt.plan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Which packages to plan for. {@link #ALL}: every installed package, as usual. Strict
 * ({@code beast -strictversions}): the package versions an analysis was made with, from the
 * {@code required} attribute BEAUti writes on the {@code <beast>} element, e.g.
 * {@code required="BEAST.base v2.7.6:SA v2.1.1"}. Then only those packages are loaded, each at
 * exactly that version, with what they depend on; for the core the pinned version is preferred.
 *
 * @param pinned package name to version; empty for {@link #ALL}
 * @param strict whether only the pinned packages (and their dependencies) are loaded
 */
public record Selection(Map<String, Version> pinned, boolean strict) {

    public static final Selection ALL = new Selection(Map.of(), false);

    /** strict, from a required attribute: "Name vVersion" entries separated by ':' */
    public static Selection fromRequired(String required) {
        Map<String, Version> pinned = new LinkedHashMap<>();
        if (required != null) {
            for (String entry : required.split(":")) {
                String s = entry.trim();
                int space = s.lastIndexOf(' ');
                if (space > 0) {
                    String version = s.substring(space + 1).trim();
                    if (version.startsWith("v") || version.startsWith("V")) {
                        version = version.substring(1);
                    }
                    pinned.put(s.substring(0, space).trim(), Version.of(version));
                }
            }
        }
        return new Selection(Collections.unmodifiableMap(pinned), true);
    }

    /**
     * Strict, from an XML analysis: the {@code required} attribute of its root element.
     *
     * @throws IllegalArgumentException if the analysis has no required attribute
     */
    public static Selection fromAnalysis(Path xml) throws IOException {
        try (InputStream in = Files.newInputStream(xml)) {
            XMLInputFactory f = XMLInputFactory.newInstance();
            f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader r = f.createXMLStreamReader(in);
            while (r.hasNext()) {
                if (r.next() == XMLStreamConstants.START_ELEMENT) {
                    String required = r.getAttributeValue(null, "required");
                    if (required == null) {
                        throw new IllegalArgumentException(xml.getFileName() + " has no 'required' attribute on <"
                                + r.getLocalName() + ">, so it does not say which package versions it was made with."
                                + " Run it without -strictversions.");
                    }
                    return fromRequired(required);
                }
            }
            throw new IllegalArgumentException(xml.getFileName() + " has no root element");
        } catch (XMLStreamException e) {
            throw new IOException(xml.getFileName() + " is not readable as XML: " + e.getMessage(), e);
        }
    }
}
