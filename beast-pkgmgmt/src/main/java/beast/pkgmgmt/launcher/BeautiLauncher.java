package beast.pkgmgmt.launcher;



import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Bootstrap launcher for BEAUti.
 **/
public class BeautiLauncher extends BeastLauncher {

	public static void main(String[] args) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		// Utils6.startSplashScreen();
		if (javaVersionCheck("BEAUti")) {
			// loadBEASTJars();
			BeastLauncher.testCudaStatusOnMac();
			String classpath = getPath(false, null);
			run(classpath, "beastfx.app.beauti.Beauti", args);
		}
        // Utils6.endSplashScreen();
	}
}
