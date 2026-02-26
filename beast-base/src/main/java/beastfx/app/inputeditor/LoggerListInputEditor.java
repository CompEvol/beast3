package beastfx.app.inputeditor;


import java.util.List;

import javafx.geometry.Dimension2D;
import javafx.scene.control.TextField;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.Logger;



public class LoggerListInputEditor extends ListInputEditor {

	public LoggerListInputEditor(BeautiDoc doc) {
		super(doc);
	}

    public LoggerListInputEditor() {
		super();
	}

	@Override
    public Class<?> type() {
        return List.class;
    }

    @Override
    public Class<?> baseType() {
        return Logger.class;
    }
    

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
    	super.init(input, beastObject, itemNr, isExpandOption, addButtons);
    }
    
    @Override
    protected void addSingleItem(BEASTInterface beastObject) {
    	currentLogger = (Logger) beastObject;
    	super.addSingleItem(beastObject);
    }
    
    public InputEditor createFileNameEditor() {
        final Input<?> input = currentLogger.fileNameInput;
        StringInputEditor fileNameEditor = new StringInputEditor(doc);
        fileNameEditor.init(input, currentLogger, -1, ExpandOption.FALSE, true);

        // ensure file name entry has larger size than the standard size
        TextField fileNameEntry = fileNameEditor.getEntry();
        Dimension2D size = new Dimension2D(400, fileNameEntry.getPrefHeight());
        fileNameEntry.setMinSize(size.getWidth(), size.getHeight());
        fileNameEntry.setPrefSize(size.getWidth(), size.getHeight());
        return fileNameEditor;
    }
    
    Logger currentLogger;
}
