package beastfx.app.methodsection.implementation;



import java.util.ArrayList;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.inference.distribution.ParametricDistribution;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.MethodsTextFactory;
import beastfx.app.methodsection.Phrase;

public class ParametricDistributionMethodsText implements MethodsText {

	@Override
	public Class<?> type() {
		return ParametricDistribution.class;
	}
	
	@Override
	public List<Phrase> getModelDescription(Object o2, BEASTInterface parent, Input<?> input2, BeautiDoc doc) {
		ParametricDistribution o = (ParametricDistribution) o2;
		done.add(o);
		List<Phrase> b = new ArrayList<>();
		b.add(new Phrase(o, parent, input2, getName(o)));
		b.add(new Phrase(" distributed "));
		boolean isFirst = true;
		for (Input<?> input : o.listInputs()) {
			if (!BEASTObjectMethodsText.cfg.suppressBEASTObjects.contains(o.getClass().getName() + "." + input.getName())) {
				if (input.get() != null && input.get() instanceof BEASTObject) {
					if (isFirst) {
						isFirst = false;
						b.add(new Phrase("with "));
					} else {
						b.add(new Phrase(","));
					}
					b.add(new Phrase(input.get(), o, input," " + getInputName(input) + " "));
					List<Phrase> m = MethodsTextFactory.getModelDescription(input.get(), o, input, doc);
					b.addAll(m);
				}
			}
		}
 				
		b.addAll(describePriors(o, parent, input2, doc));
		
		return b;
	}
}
