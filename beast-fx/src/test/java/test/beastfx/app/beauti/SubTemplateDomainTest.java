package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
 * Every subtemplate is built for an imported partition, so tree priors, clocks and tree
 * initialisers, which refer to the partition's tree, are checked too; one that does not build
 * fails the test rather than dropping out of the check.
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
        // a partition with its tree: tree priors, clocks and tree initialisers refer to them
        doc.importNexus(new File(BeautiBase.NEXUS_DIR + "anolis.nex"));

        PartitionContext context = new PartitionContext("anolis");
        Set<String> built = new TreeSet<>();
        Map<String, String> notBuilt = new TreeMap<>();
        Set<String> mismatches = new TreeSet<>();
        for (BeautiSubTemplate template : new ArrayList<>(doc.beautiConfig.subTemplates)) {
            BEASTInterface o;
            try {
                o = template.createSubNet(context, true);
            } catch (Throwable e) {
                notBuilt.put(template.getID(), e.toString());
                continue;
            }
            if (o == null) {
                notBuilt.put(template.getID(), "nothing built");
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
        // a subtemplate that stops building would drop out of the check unnoticed
        assertTrue(notBuilt.isEmpty(), "subtemplates not built, so not checked: " + notBuilt);
        assertTrue(built.size() > 50, "only " + built.size() + " subtemplates: " + built);
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
