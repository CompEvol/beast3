package beastfx.app.util;

import java.io.File;

public class LogFile extends File {

	private static final long serialVersionUID = 1L;

	public LogFile(File parent, String child) {
		super(parent, child);
	}

	public LogFile(String pathname) {
		super(pathname);
	}

}
