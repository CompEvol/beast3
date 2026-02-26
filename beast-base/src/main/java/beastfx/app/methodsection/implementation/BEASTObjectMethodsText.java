package beastfx.app.methodsection.implementation;

import java.util.*;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.MethodsTextFactory;
import beastfx.app.methodsection.Phrase;

public class BEASTObjectMethodsText implements MethodsText {
	
	
	@Override
	public Class<?> type() {
		return BEASTObject.class;
	}
	
	@Override
	public List<Phrase> getModelDescription(Object o2, BEASTInterface parent, Input<?> input2, BeautiDoc doc) {
		if (done.contains(o2)) {
			return new ArrayList<>();
		}
		BEASTObject o = (BEASTObject) o2;
		done.add(o);
				
		List<Phrase> b = new ArrayList<>();
		b.add(new Phrase(o, parent, input2, getName(o) + " with "));
		for (Input<?> input : o.listInputs()) {
			if (input.get() != null && input.get() instanceof BEASTObject) {
				if (!cfg.suppressBEASTObjects.contains(o.getClass().getName() + "." + input.getName())) {
					List<Phrase> m = MethodsTextFactory.getModelDescription(input.get(), o, input, doc);
					if (m.size() > 0) {
						b.add(new Phrase(input.get(), o, input, " " + getInputName(input) + " is "));
						b.addAll(m);
					}
				}
			}
		}
 				
		b.addAll(describePriors(o, parent, input2, doc));
		
		return b;
	}


	static BeautiConfig cfg;
	public static void setBeautiCFG(BeautiConfig beautiConfig) {
		cfg = beautiConfig;		
	}
}
