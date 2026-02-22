package beastfx.app.beauti.theme;

import beastfx.app.beauti.ThemeProvider;
import javafx.scene.Scene;

public class Bootstrap extends ThemeProvider {
	public Bootstrap() {}
	public String getThemeName() {return "Bootstrap";}
	public boolean loadMyStyleSheet(Scene scene) {return ThemeProvider.loadStyleSheet(scene, "/themes/bootstrap.css");}
}