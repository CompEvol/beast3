package beast.pkgmgmt;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for issue #94: a package depending on BEAST.app failed to
 * resolve because BEAST.app was never registered as an installed package.
 * addInstalledPackages() must register both core packages (BEAST.base and
 * BEAST.app) at the running version so the dependency resolver knows them.
 */
public class CorePackagesInstalledTest {

    @Test
    public void corePackagesAreRegisteredAsInstalled() {
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addInstalledPackages(packageMap);

        for (String name : new String[] {PackageManager.BEAST_BASE_PACKAGE_NAME,
                                         PackageManager.BEAST_APP_PACKAGE_NAME}) {
            assertTrue(packageMap.containsKey(name),
                    name + " should be present in the package map");
            assertTrue(packageMap.get(name).isInstalled(),
                    name + " should be marked as installed");
        }
    }
}
