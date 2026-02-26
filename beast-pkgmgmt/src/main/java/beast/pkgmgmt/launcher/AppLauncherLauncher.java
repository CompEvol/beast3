package beast.pkgmgmt.launcher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class AppLauncherLauncher extends BeastLauncher {
	/** Bootstrap launcher for AppLauncher. **/
	public static void main(String[] args) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		if (javaVersionCheck("AppLauncher")) {
			String classpath = getPath(false, null);
			run(classpath, "beastfx.app.tools.AppLauncher", args);
		}
	}

}
