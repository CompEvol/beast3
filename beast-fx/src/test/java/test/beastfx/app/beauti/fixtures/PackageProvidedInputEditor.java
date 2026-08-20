package test.beastfx.app.beauti.fixtures;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;

/**
 * Test fixture standing in for an InputEditor contributed by an external BEAST <b>package</b> --
 * the same role starbeast3's {@code MRCAPriorInputEditorSB3} plays for real.
 *
 * <p>It is listed only in the test resource {@code src/test/resources/version.xml}, under a
 * {@code <service type="beastfx.app.inputeditor.InputEditor">} entry -- the same way a real BEAST
 * package declares its editors. It deliberately has <b>no</b> {@code META-INF/services} entry, so
 * plain {@code ServiceLoader.load(InputEditor.class)} can never find it; the only way to discover
 * it is via {@code PackageManager.listServices()}, which reads {@code version.xml}.
 *
 * <p>On a real deployment, a package like starbeast3 is loaded into its own JPMS plugin
 * {@code ModuleLayer} at runtime, which is exactly why plain {@code ServiceLoader} can never see
 * into it. This fixture reproduces that same "invisible to ServiceLoader, visible only via
 * {@code PackageManager.listServices()}" situation without needing a real package or ModuleLayer.
 *
 * <p>See {@link test.beastfx.app.beauti.InputEditorFactoryPackageDiscoveryTest} for how this is
 * used -- whether this class gets registered is the actual thing that test checks.
 */
public class PackageProvidedInputEditor extends InputEditor.Base {

    /** stand-in for a BEASTObject type this fixture editor "handles" */
    public static class Target {}

    public PackageProvidedInputEditor() {
        super();
    }

    public PackageProvidedInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return Target.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                      ExpandOption isExpandOption, boolean addButtons) {
        // not exercised by this test
    }
}
