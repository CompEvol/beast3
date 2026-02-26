package beastfx.app.util;

import java.io.File;

public class TreeFile extends File {

	private static final long serialVersionUID = 1L;

	public TreeFile(File parent, String child) {
		super(parent, child);
	}

	public TreeFile(String string) {
		super(string);
	}

}
