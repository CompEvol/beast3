package beastfx.app.beauti;

import java.lang.reflect.InvocationTargetException;

import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.util.FXUtils;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.spec.evolution.tree.coalescent.ConstantPopulation;

public class ConstantPopulationInputEditor extends InputEditor.Base {

	public ConstantPopulationInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
	public ConstantPopulationInputEditor() {
		super();
	}

	@Override
	public Class<?> type() {
		return ConstantPopulation.class;
	}
	
	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption,
			boolean addButtons) {
		ConstantPopulation population = (ConstantPopulation) input.get();
		try {
			InputEditor editor = doc.inputEditorFactory.createInputEditor(population.popSizeParameter, population, doc);
			pane = FXUtils.newHBox();
			pane.getChildren().add(editor.getComponent());
			getChildren().add(pane);
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
