package beastfx.app.util;

import java.io.File;

public class XMLFile extends File {

	private static final long serialVersionUID = 1L;

	public XMLFile(File parent, String child) {
		super(parent, child);
	}

	public XMLFile(String pathname) {
		super(pathname);
	}

}
