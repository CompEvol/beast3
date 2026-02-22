package beastfx.app.inputeditor;





import java.io.File;

import javafx.scene.control.Button;

import beastfx.app.util.FXUtils;
import beastfx.app.util.OutFile;
import beastfx.app.util.Utils;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.ProgramStatus;

public class OutFileInputEditor extends InputEditor.Base {

	@Override
	public Class<?> type() {
		return OutFile.class;
	}

	public OutFileInputEditor(BeautiDoc doc) {
		super(doc);
	}

	public OutFileInputEditor() {
		super();
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		super.init(input, plugin, itemNr, bExpandOption, bAddButtons);
		if (input.get() == null) {
			m_entry.setText("[[none]]");
		} else {
			m_entry.setText(((File) m_input.get()).getName());
		}
		
		Button button = new Button("browse");
		button.setOnAction(e -> {				
				File defaultFile = FileInputEditor.getDefaultFile((File) m_input.get());
				File file = FXUtils.getSaveFile(m_input.getTipText(), defaultFile, "All files", Utils.isWindows() ? "*" : "");
				if (file != null) 
					file = new OutFile(file.getPath());
				try {
					m_entry.setText(file.getName());
					m_input.setValue(new OutFile(file.getPath()), m_beastObject);
					String path = file.getPath();
					ProgramStatus.setCurrentDir(path);
				} catch (Exception e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
		});
		pane.getChildren().add(button);
	}
	
}
