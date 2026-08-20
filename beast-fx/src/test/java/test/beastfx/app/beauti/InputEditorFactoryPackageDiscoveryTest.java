package test.beastfx.app.beauti;

import beastfx.app.inputeditor.BEASTObjectInputEditor;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditorFactory;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import test.beastfx.app.beauti.fixtures.PackageProvidedInputEditor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for a bug in {@link InputEditorFactory#init()}: it could silently drop every
 * BEAUti InputEditor that a BEAST <i>package</i> provides (for example starbeast3's
 * {@code MRCAPriorInputEditorSB3}), so BEAUti would fall back to a generic built-in editor instead
 * of the package's own one.
 *
 * <h2>What went wrong</h2>
 * {@code init()} looks for InputEditors in two passes. The buggy version looked like this:
 *
 * <pre>{@code
 * Iterable<InputEditor> editors = ServiceLoader.load(InputEditor.class);
 * for (InputEditor editor : editors) {
 *     registerInputEditors(new String[]{editor.getClass().getName()});
 * }
 * if (isJUnitTest() || inputEditorMap.size() == 0) {          // <-- the bug
 *     Set<String> inputEditors = PackageManager.listServices("beastfx.app.inputeditor.InputEditor");
 *     registerInputEditors(inputEditors.toArray(new String[0]));
 * }
 * }</pre>
 *
 * <ol>
 *   <li><b>Pass 1</b> uses plain {@code ServiceLoader.load()}. This only finds InputEditors that
 *       live in beast.fx's own module -- the built-in "core" editors.</li>
 *   <li><b>Pass 2</b> uses {@code PackageManager.listServices()}, which reads every installed
 *       BEAST package's {@code version.xml}. This is the <i>only</i> way an external package's
 *       InputEditor is ever found, because a package is loaded into its own separate JPMS
 *       {@code ModuleLayer} at runtime, and plain {@code ServiceLoader.load()} cannot see into
 *       that layer.</li>
 * </ol>
 * The bug is the {@code if}: pass 2 only ran when pass 1 had found <i>nothing at all</i>
 * ({@code inputEditorMap.size() == 0}). In a real BEAUti run, pass 1 always finds several core
 * editors, so the map is never empty, so pass 2 never runs, so no package's InputEditors are ever
 * registered -- silently. The fix simply always runs pass 2, in addition to pass 1.
 *
 * <h2>How this test reproduces it</h2>
 * Rather than installing a real BEAST package (which needs a whole separate ModuleLayer), this
 * test reproduces the same two discovery mechanisms with one real beast.fx class and one small
 * test-only fixture:
 * <ul>
 *   <li>{@link BEASTObjectInputEditor} is a real, ordinary beast.fx core editor -- it is one of the
 *       classes genuinely listed in beast-fx's own {@code module-info.java} {@code provides}
 *       clause. It plays the role of "a core beast.fx editor" in this test. To make pass 1 find it
 *       even when the test runs on the plain classpath (see the note below), it is <i>also</i>
 *       listed in a test-only {@code META-INF/services/beastfx.app.inputeditor.InputEditor}
 *       resource -- the classic {@code ServiceLoader} registration file. Its only purpose here is
 *       to make pass 1 find <i>something</i>, so {@code inputEditorMap} is non-empty by the time
 *       pass 2 is considered.</li>
 *   <li>{@link PackageProvidedInputEditor} stands in for <b>an external package's editor</b> (the
 *       role {@code MRCAPriorInputEditorSB3} plays for starbeast3). It is listed only in a test
 *       {@code version.xml} {@code <service>} entry, so it is reachable <i>only</i> through pass
 *       2 ({@code PackageManager.listServices()}) and is invisible to {@code ServiceLoader.load()}.
 *       Whether this one gets registered is the actual thing this test checks.</li>
 * </ul>
 * <b>Maven CLI vs. an IDE (e.g. IntelliJ):</b> a {@code META-INF/services} file is only honoured by
 * {@code ServiceLoader} for classes on the plain classpath (the "unnamed module"). Maven Surefire
 * runs beast-fx's tests that way, so there it is the test's own {@code META-INF/services} entry
 * that makes pass 1 find {@link BEASTObjectInputEditor} -- beast-fx ships no such file of its own,
 * so without this fixture entry pass 1 would find nothing there. An IDE that runs tests as part of
 * the real {@code beast.fx} JPMS module (IntelliJ does, by default, because beast-fx has a
 * {@code module-info.java}) ignores that same test-only file -- a {@code META-INF/services} entry
 * inside a named module is not read by {@code ServiceLoader}, only a real {@code provides} clause
 * in {@code module-info.java} is. There, the test's {@code META-INF/services} entry is simply never
 * read, but {@link BEASTObjectInputEditor} is still found regardless, via its <i>genuine</i>
 * {@code provides} declaration -- the same one a real BEAUti run relies on. Either way, by the time
 * pass 2 runs, something from pass 1 is already in the map -- which is the only thing that actually
 * matters for reproducing the bug. That is why the first assertion below checks
 * "{@code inputEditorMap} is not empty" rather than checking for {@link BEASTObjectInputEditor}
 * specifically.
 *
 * <p><b>This split is not a {@code ServiceLoader} bug.</b> It is deliberate JPMS behaviour: a named
 * module's {@code module-info.java} is meant to be the single, compiler-checked source of what it
 * provides (two of the module system's core design goals, "reliable configuration" and "strong
 * encapsulation" -- see JEP 261), so once a class lives in a named module, {@code ServiceLoader}
 * intentionally stops honouring a {@code META-INF/services} file bundled inside that same module;
 * only {@code provides} counts there. {@code META-INF/services} remains fully supported for
 * providers on the plain class path (the unnamed module) -- beast-fx just doesn't ship one of its
 * own for {@code InputEditor} (it relies on {@code provides} instead), which is precisely why pass 1
 * finds beast-fx's real core editors when the test runs as part of the named {@code beast.fx}
 * module (an IDE) but finds none of them under a plain class-path run (Maven Surefire) -- hence why
 * this test needs its own {@code META-INF/services} entry to reproduce that "pass 1 found
 * something" precondition there. The real bug -- the one this test exists to catch -- is entirely
 * on the beast-fx side: {@code InputEditorFactory.init()} assumed a non-empty result from pass 1
 * meant "nothing more to look for", which is simply the wrong inference to draw from a mechanism
 * whose completeness depends on which mode the JVM happens to be running in.
 *
 * <h2>Why the registration call runs on the FX thread</h2>
 * The call to {@code new InputEditorFactory(doc)} below happens inside {@link Platform#runLater},
 * not directly in the {@code @Test} method. This is deliberate: {@code Utils6.isJUnitTest()}
 * checks the calling thread's stack trace for {@code org.junit.*} frames, and the pre-fix guard
 * had {@code || isJUnitTest()} specifically so that tests always ran pass 2, no matter what pass 1
 * found. Calling {@code new InputEditorFactory(doc)} straight from the {@code @Test} method would
 * trip that escape hatch and make this test pass even against the buggy code. Running it from the
 * FX Application Thread avoids that -- and matches real BEAUti, which always constructs
 * {@code InputEditorFactory} from that same thread.
 */
@ExtendWith(ApplicationExtension.class)
public class InputEditorFactoryPackageDiscoveryTest {

    @Start
    public void start(Stage stage) {
        // no UI needed; @Start just gets the JavaFX toolkit initialised
    }

    @Test
    public void packageProvidedEditorIsRegistered() throws Exception {
        AtomicReference<Map<Class<?>, String>> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // Run on the FX Application Thread, not the JUnit thread -- see "Why the registration call
        // runs on the FX thread" above. Calling this from the @Test method directly would make the
        // test pass even against the buggy code.
        Platform.runLater(() -> {
            try {
                BeautiDoc doc = new BeautiDoc();
                // The real, unmodified InputEditorFactory.init() runs here (called from the
                // constructor) and registers both BEASTObjectInputEditor and the
                // PackageProvidedInputEditor fixture from the test classpath.
                InputEditorFactory factory = new InputEditorFactory(doc);

                Field f = InputEditorFactory.class.getDeclaredField("inputEditorMap");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Class<?>, String> map = (Map<Class<?>, String>) f.get(factory);
                result.set(map);
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "InputEditorFactory init timed out on the FX thread");
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        Map<Class<?>, String> inputEditorMap = result.get();

        // Step 1 -- sanity check, not the actual regression test: confirm pass 1 (ServiceLoader)
        // found *something*, so inputEditorMap is non-empty going into pass 2. We deliberately do
        // NOT check for BEASTObjectInputEditor specifically here -- see "Maven CLI vs. an IDE"
        // above: under a plain classpath run (e.g. Maven Surefire) it's found via the test's own
        // META-INF/services entry; under a real JPMS module-path run (e.g. IntelliJ's default test
        // runner) it's found via its own genuine module-info "provides" clause instead -- either
        // way it's the same class, just discovered through a different mechanism. Checking for
        // "non-empty" rather than this specific class keeps the assertion valid in both cases. If
        // this assertion ever fails, the test setup is broken and step 2 below wouldn't be testing
        // anything meaningful.
        assertFalse(inputEditorMap.isEmpty(),
                "test setup problem: inputEditorMap was empty after pass 1 (ServiceLoader), so "
                + "this test isn't exercising the size()==0 guard the bug lived in -- fix the test "
                + "fixtures before trusting the assertion below");

        // Step 2 -- the actual regression check: was our "package editor" stand-in registered too?
        // It is only reachable via PackageManager.listServices() (pass 2). On the pre-fix code,
        // pass 2 never ran here because step 1 already proved pass 1 found something -- so this
        // assertion fails against the bug and passes once it's fixed.
        assertEquals(PackageProvidedInputEditor.class.getName(),
                inputEditorMap.get(PackageProvidedInputEditor.Target.class),
                "PackageProvidedInputEditor (registered only via version.xml) was not found -- "
                + "this is the bug: InputEditorFactory.init() only calls "
                + "PackageManager.listServices() when ServiceLoader found nothing, so it drops "
                + "every package-provided editor as soon as a core editor is found first");
    }
}
