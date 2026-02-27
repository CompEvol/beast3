package beastfx.app.methodsection.implementation;

import java.util.*;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.tree.MRCAPrior;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.MethodsTextFactory;
import beastfx.app.methodsection.Phrase;

public class MRCAPriorMethodsText implements MethodsText {

	@Override
	public Class<?> type() {
		return MRCAPrior.class;
	}

	@Override
	public List<Phrase> getModelDescription(Object o2, BEASTInterface parent, Input<?> input2, BeautiDoc doc) {		
		MRCAPrior o = (MRCAPrior) o2;
		List<Phrase> b = new ArrayList<>();
		if (o.taxonsetInput.get().getTaxonCount() == 0) {
			o.taxonsetInput.get().initAndValidate();
		}
		b.add(new Phrase(o, " MRCA prior " + o.taxonsetInput.get().getID() + " (" + o.taxonsetInput.get().getTaxonCount() + " taxa)"));
		boolean isUsed = false;
		if (o.isMonophyleticInput.get()) {
			b.add(new Phrase(" is monophyletic "));
			isUsed = true;
		}
		if (o.onlyUseTipsInput.get()) {
			b.add(new Phrase(" on tips only "));
		}
		if (o.distInput.get() != null) {
				List<Phrase> m = MethodsTextFactory.getModelDescription(o.distInput.get(), o, o.distInput, doc);
				if (!isUsed) {
					b.add(new Phrase(" being "));
				} else {
					b.add(new Phrase(" and "));
				}
				b.addAll(m);
		} else {
			if (!o.isMonophyleticInput.get() &&! o.onlyUseTipsInput.get()) {
				b.add(new Phrase(" is added for logging only"));
			}
		}
		return b;
	}

}
