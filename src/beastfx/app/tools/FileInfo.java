package beastfx.app.tools;

import java.io.File;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class FileInfo {
    
	public SimpleStringProperty name;
	public SimpleIntegerProperty burnin;

	FileInfo(File file) {
		name = new SimpleStringProperty(file.getPath());
		burnin = new SimpleIntegerProperty(10);
	}
	
	public String getFile() {
		return name.get();
	}

	public void setFile(String fname) {
		name.set(fname);
	}
	
	public void setBurnin(Integer burnin) {
		this.burnin.set(burnin);
	}
	
	public Integer getBurnin() {
		return burnin.get();
	}
	
	public SimpleStringProperty getNameProperty() {
		return name;
	}
	
	public SimpleIntegerProperty getBIntegerProperty() {
		return burnin;
	}

}
