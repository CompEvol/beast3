package beastfx.app.beauti;


import java.lang.reflect.InvocationTargetException;

import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.util.FXUtils;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.spec.evolution.tree.coalescent.CompoundPopulationFunction;
import beast.base.spec.evolution.tree.coalescent.ScaledPopulationFunction;

public class ScaledPopulationInputEditor extends InputEditor.Base {

	public ScaledPopulationInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
	public ScaledPopulationInputEditor() {
		super();
	}

	@Override
	public Class<?> type() {
		return ScaledPopulationFunction.class;
	}
	
	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption,
			boolean addButtons) {
		ScaledPopulationFunction population = (ScaledPopulationFunction) input.get();
		try {
			pane = FXUtils.newVBox();

			CompoundPopulationFunction f = (CompoundPopulationFunction) population.popParameterInput.get();
			InputEditor editor = doc.inputEditorFactory.createInputEditor(f.popSizeParameterInput, f, doc);
			pane.getChildren().add(editor.getComponent());

			editor = doc.inputEditorFactory.createInputEditor(f.demographicTypeInput, f, doc);
			pane.getChildren().add(editor.getComponent());

			editor = doc.inputEditorFactory.createInputEditor(f.useMiddleInput, f, doc);
			pane.getChildren().add(editor.getComponent());
			
			InputEditor editor2 = doc.inputEditorFactory.createInputEditor(population.scaleFactorInput, population, doc);
			pane.getChildren().add(editor2.getComponent());

			getChildren().add(pane);
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
