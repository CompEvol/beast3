package beastfx.app.methodsection.implementation;


import java.util.ArrayList;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.distribution.Prior;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.MethodsTextFactory;
import beastfx.app.methodsection.Phrase;

public class PriorMethodsText implements MethodsText {

	@Override
	public Class<?> type() {
		return Prior.class;
	}

	@Override
	public List<Phrase> getModelDescription(Object o, BEASTInterface parent, Input<?> input2, BeautiDoc doc) {
		Prior p = (Prior) o;
		List<Phrase> b = new ArrayList<>();
		List<Phrase> m = MethodsTextFactory.getModelDescription(p.distInput.get(), p, p.distInput, doc);
		b.addAll(m);
		return b;
	}

}
