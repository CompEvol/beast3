package beastfx.app.beauti.theme;

import beastfx.app.beauti.ThemeProvider;
import javafx.scene.Scene;

public class Win7 extends ThemeProvider {
	public Win7() {}
	public String getThemeName() {return "Win7";}
	public boolean loadMyStyleSheet(Scene scene) {return ThemeProvider.loadStyleSheet(scene, "/themes/win7.css");}
}