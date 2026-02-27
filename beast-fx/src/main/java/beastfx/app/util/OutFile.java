package beastfx.app.util;

import java.io.File;

public class OutFile extends File {
	private static final long serialVersionUID = 1L;
	public final static String NO_FILE = "[[none]]";

	public OutFile(File parent, String child) {
		super(parent, child);
	}

	public OutFile(String string) {
		super(string);
	}
	
	public static boolean isSpecified(File file) {
		if (file == null) {
			return false;
		}
		if (file.getName().equals(NO_FILE)) {
			return false;
		}		
		return true;
	}
}
