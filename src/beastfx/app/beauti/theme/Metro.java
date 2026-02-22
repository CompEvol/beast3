package beastfx.app.beauti.theme;

import beastfx.app.beauti.ThemeProvider;
import javafx.scene.Scene;
//import jfxtras.styles.jmetro.JMetro;
//import jfxtras.styles.jmetro.Style;

public class Metro extends ThemeProvider {
	public Metro() {}
	public String getThemeName() {return "Metro";}
	public boolean loadMyStyleSheet(Scene scene) {
//		JMetro jMetro = new JMetro(Style.LIGHT); 
//		jMetro.setScene(scene);
//		return true;
		return ThemeProvider.loadStyleSheet(scene, "/themes/metro.css");
	}
}