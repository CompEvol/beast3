package beastfx.app.beauti.theme;

import beastfx.app.beauti.ThemeProvider;
import javafx.application.Application;
import javafx.scene.Scene;

public class Default extends ThemeProvider {
	public Default() {}
	public String getThemeName() {return "Default";}
	public boolean loadMyStyleSheet(Scene scene) {
		Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
		return ThemeProvider.loadStyleSheet(scene, "/themes/default.css");
	}
}