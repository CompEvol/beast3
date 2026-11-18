package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.parser.PartitionContext;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.BeautiSubTemplate;
import javafx.stage.Stage;

/**
 * The objects BEAUti builds from its subtemplates give no tensor input a value whose domain
 * does not fit the declared one (#226), e.g. a NonNegativeReal proportionInvariant for the
 * site model's UnitInterval input, which would warn for every analysis built in BEAUti.
 * Subtemplates that cannot be built on their own, outside an analysis, are skipped.
 */
@ExtendWith(ApplicationExtension.class)
public class SubTemplateDomainTest {

    @Start
    public void start(Stage stage) {
        // nothing to show, only the JavaFX toolkit is required
    }

    @Test
    public void subTemplatesFitDeclaredDomains() throws Exception {
        BeautiDoc doc = new BeautiDoc();
        doc.beautiConfig = new BeautiConfig();
        doc.beautiConfig.initAndValidate();
        doc.newAnalysis();

        PartitionContext context = new PartitionContext("dna");
        Set<String> built = new TreeSet<>();
        Set<String> mismatches = new TreeSet<>();
        for (BeautiSubTemplate template : new ArrayList<>(doc.beautiConfig.subTemplates)) {
            BEASTInterface o;
            try {
                o = template.createSubNet(context, true);
            } catch (Throwable e) {
                continue;
            }
            if (o == null) {
                continue;
            }
            built.add(template.getID());
            Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            collect(o, seen);
            for (Object x : seen) {
                BEASTInterface b = (BEASTInterface) x;
                for (Input<?> input : b.listInputs()) {
                    String mismatch = input.domainMismatch(b);
                    if (mismatch != null) {
                        mismatches.add(template.getID() + ": " + mismatch);
                    }
                }
            }
        }
        assertTrue(built.contains("Gamma Site Model"), "Gamma Site Model not built; built " + built);
        if (!mismatches.isEmpty()) {
            fail("Subtemplates give inputs a value of the wrong domain:\n  " + String.join("\n  ", mismatches));
        }
    }

    private static void collect(Object o, Set<Object> seen) {
        if (o instanceof Collection<?> c) {
            for (Object x : c) collect(x, seen);
        } else if (o instanceof Map<?, ?> m) {
            for (Object x : m.values()) collect(x, seen);
        } else if (o instanceof BEASTInterface b && seen.add(b)) {
            for (Input<?> input : b.listInputs()) collect(input.get(), seen);
        }
    }
}
