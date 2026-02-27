package beastfx.app.beauti.theme;

import beastfx.app.beauti.ThemeProvider;
import javafx.scene.Scene;

public class Dark extends ThemeProvider {
	public Dark() {}
	public String getThemeName() {return "Dark";}
	public boolean loadMyStyleSheet(Scene scene) {return ThemeProvider.loadStyleSheet(scene, "/themes/dark.css");}
}