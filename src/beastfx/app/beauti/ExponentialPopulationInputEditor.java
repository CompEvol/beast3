package beastfx.app.beauti;


import java.lang.reflect.InvocationTargetException;

import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.util.FXUtils;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.spec.evolution.tree.coalescent.ExponentialGrowth;

public class ExponentialPopulationInputEditor extends InputEditor.Base {

	public ExponentialPopulationInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
	public ExponentialPopulationInputEditor() {
		super();
	}

	@Override
	public Class<?> type() {
		return ExponentialGrowth.class;
	}
	
	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption,
			boolean addButtons) {
		ExponentialGrowth population = (ExponentialGrowth) input.get();
		try {
			InputEditor editor = doc.inputEditorFactory.createInputEditor(population.popSizeParameterInput, population, doc);
			pane = FXUtils.newVBox();
			pane.getChildren().add(editor.getComponent());

			InputEditor editor2 = doc.inputEditorFactory.createInputEditor(population.growthRateParameterInput, population, doc);
			pane.getChildren().add(editor2.getComponent());
			getChildren().add(pane);
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
