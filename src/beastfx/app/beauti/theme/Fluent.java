package beastfx.app.beauti.theme;

import beastfx.app.beauti.ThemeProvider;
import javafx.scene.Scene;

public class Fluent extends ThemeProvider {
	public Fluent() {}
	public String getThemeName() {return "Fluent";}
	public boolean loadMyStyleSheet(Scene scene) {return ThemeProvider.loadStyleSheet(scene, "/themes/fluent.css");}
}