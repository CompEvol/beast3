package beastfx.app.methodsection.implementation;


import java.util.ArrayList;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.inference.StateNode;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.MethodsTextFactory;
import beastfx.app.methodsection.Phrase;

public class SubstModelMethodsText implements MethodsText {

	@Override
	public Class<?> type() {
		return SubstitutionModel.Base.class;
	}

	@Override
	public List<Phrase> getModelDescription(Object o2, BEASTInterface parent, Input<?> input2, BeautiDoc doc) {
		List<Phrase> b = new ArrayList<>();		
		SubstitutionModel.Base o = (SubstitutionModel.Base) o2;
		boolean hasWith = false;

		done.add(o2);
		for (Input<?> input : o.listInputs()) {
			if (!BEASTObjectMethodsText.cfg.suppressBEASTObjects.contains(o.getClass().getName() + "." + input.getName())) {
				if (input.get() != null && input.get() instanceof StateNode) { // && ((StateNode)input.get()).isEstimatedInput.get()) {
					if (!hasWith) {
						b.add(new Phrase(" with "));
						hasWith = true;
					} else {
						b.add(new Phrase(" and "));
					}
					b.add(new Phrase(input.get(), o, input, getInputName(input) + " "));
					done.add(input.get());
					if (((StateNode)input.get()).isEstimatedInput.get()) {
						List<Phrase> m = describePriors((StateNode) input.get(), o, input, doc);
						b.addAll(m);
					} else {
						List<Phrase> m =  MethodsTextFactory.getModelDescription(input.get(), o, input, doc);
						b.addAll(m);
					}
				}
			}
		}
		if (hasWith) {
			b.add(new Phrase("\nand "));
		}
		
		if (o.frequenciesInput.get() != null) {
			List<Phrase> m = MethodsTextFactory.getModelDescription(o.frequenciesInput.get(), o, o.frequenciesInput, doc);
			String text = o.frequenciesInput.get().getID();
			if (text.toLowerCase().indexOf("freqs") > 0) {
				text = text.substring(0, text.toLowerCase().indexOf("freqs"));
			}
			if (m.size() > 0) {
				m.get(0).setText(text);
			} else {
				m.add(new Phrase(text));
			}
			b.addAll(m);
			b.add(new Phrase(" frequencies"));
		}
		return b;
	}

}
