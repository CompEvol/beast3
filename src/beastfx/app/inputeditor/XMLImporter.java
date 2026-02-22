package beastfx.app.inputeditor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.evolution.alignment.Alignment;

@Description("BEAST XML file importer")
public class XMLImporter implements AlignmentImporter {

	@Override
	public String[] getFileExtensions() {
		return new String[]{"xml"};
	}

	@Override
	public List<BEASTInterface> loadFile(File file) {
		List<BEASTInterface> selectedBEASTObjects = new ArrayList<>();
		Alignment alignment = (Alignment)BeautiAlignmentProvider.getXMLData(file);
		selectedBEASTObjects.add(alignment);
		return selectedBEASTObjects;
	}
	
}

