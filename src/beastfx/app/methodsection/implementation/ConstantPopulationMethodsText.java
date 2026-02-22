package beastfx.app.methodsection.implementation;

import java.util.*;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.evolution.tree.coalescent.ConstantPopulation;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.MethodsTextFactory;
import beastfx.app.methodsection.Phrase;

public class ConstantPopulationMethodsText implements MethodsText {
	
	@Override
	public Class<?> type() {
		return ConstantPopulation.class;
	}
	
	@Override
	public List<Phrase> getModelDescription(Object o2, BEASTInterface parent, Input<?> input2, BeautiDoc doc) {
		if (done.contains(o2)) {
			return new ArrayList<>();
		}
		ConstantPopulation o = (ConstantPopulation) o2;
		done.add(o);
		List<Phrase> b = new ArrayList<>();
		b.add(new Phrase(o, parent, input2, getName(o) + " where "));
		for (Input<?> input : o.listInputs()) {
			if (input.get() != null && input.get() instanceof BEASTObject) {
				List<Phrase> m = MethodsTextFactory.getModelDescription(input.get(), o, input, doc);
				if (m.size() > 0) {
					b.add(new Phrase(input.get(), o, input, " " + getInputName(input) + " is "));
					b.addAll(m);
				}
			}
		}
 				
		b.addAll(describePriors(o, parent, input2, doc));
		
		return b;
	}

}
