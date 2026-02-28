package beastfx.app.methodsection.implementation;

import java.util.*;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.parameter.Parameter;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.Phrase;

public class ParameterMethodsText implements MethodsText {

	@Override
	public Class<?> type() {
		return Parameter.Base.class;
	}
	
	@Override
	public List<Phrase> getModelDescription(Object o, BEASTInterface parent, Input<?> input2, BeautiDoc doc) {
		Parameter.Base<?> p = (Parameter.Base<?>) o;
		List<Phrase> b = new ArrayList<>();
		if (p.isEstimatedInput.get()) {
			List<Phrase> m = describePriors(p, parent, input2, doc);
			if (m.size() > 0) {
				b.addAll(m);
			} else {
				// it was not estimated after all, just a constant
				b.add(new Phrase(p.valuesInput.get(), p, p.valuesInput, p.getValue().toString()));
			}
		} else {
			b.add(new Phrase(p.valuesInput.get(), p, p.valuesInput, p.getValue().toString()));
		}
		return b;
	}
}
