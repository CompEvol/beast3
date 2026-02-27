package beastfx.app.beauti;

import beastfx.app.util.Utils;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import javafx.scene.Scene;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** returns list of stylesheets **/
/** this is a service: if a package wants to add themes, it can do so by
 * implementing ThemeProvider and add an entry in the version.xml file of the package,
 * e.g. like so:

<service type="beastfx.app.beauti.ThemeProvider">
	<provider classname="mypackage.ThemeProvider"/>
</service>

where the class `mypackage.ThemeProvider` derives from ThemeProvider and implements getThemes()
 */
abstract public class ThemeProvider {

	public ThemeProvider() {};
	
	/** themeMap allows identifying themes by name **/
	private static Map<String, ThemeProvider> themeMap = null;
	
	public static ThemeProvider getThemeProvider(String name) {
		initThemeMap();
		if (themeMap.containsKey(name)) {
			return themeMap.get(name);
		}
		return null;
	}
	
	public static Map<String, ThemeProvider> getThemeMap() {
		initThemeMap();
		return themeMap;
	}

	// initiliase theme map if necessary
	private static void initThemeMap() {
		if (themeMap != null) {
			return;
		}
		themeMap = new HashMap<>();
		String [] x = PackageManager.listServices(ThemeProvider.class.getName()).toArray(new String[] {});
		Arrays.sort(x);
		for (String p: x) {
    		ThemeProvider provider;
			try {
				provider = (ThemeProvider) BEASTClassLoader.forName(p).newInstance();
				themeMap.put(provider.getThemeName(), provider);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
	}

	
	/**
	 * @return name of the theme, used as BEAUti menu item label
	 * 
	 * NB Return empty name if the style theme is not licensed and thus not available for the environment 
	 */
	abstract public String getThemeName();


	static public boolean clear = true;
	static public boolean loadStyleSheet(Scene scene, String themeFile) {
		try {
			if (clear) {
				scene.getStylesheets().clear();
				clear = false;
			}
			URL url = ThemeProvider.class.getResource(themeFile);
			String css = url.toExternalForm();
			scene.getStylesheets().add(css);
		} catch (Throwable e) {
			return false;
		}
		return true;
	}


	static public boolean loadStyleSheet(Scene scene) {
	    String theme= Utils.getBeautiProperty("theme");
	    ThemeProvider provider = getThemeProvider(theme);
	    if (provider != null) {
	    	return provider.loadMyStyleSheet(scene);
	    }
	    return false;
	}


	/**
	 * 
	 * @param scene to apply them to
	 * @return true if stylesheet was successfully loaded
	 */
	abstract public boolean loadMyStyleSheet(Scene scene);

	
	
	
}
