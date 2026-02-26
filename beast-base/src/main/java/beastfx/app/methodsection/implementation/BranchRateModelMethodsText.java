package beastfx.app.methodsection.implementation;


import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.StateNode;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.Phrase;

import java.util.*;

public class BranchRateModelMethodsText implements MethodsText {

	@Override
	public Class<?> type() {		
		return BranchRateModel.Base.class;
	}

	@Override
	public List<Phrase> getModelDescription(Object o, BEASTInterface parent, Input<?> input2, BeautiDoc doc) {
		List<Phrase> b = new ArrayList<>();		
		BranchRateModel.Base brm = (BranchRateModel.Base) o;

		
		b.add(new Phrase(o, parent, input2, getName(o) + " "));
		boolean hasWith = false;

		done.add(brm);
		for (Input<?> input : ((BEASTInterface)brm).listInputs()) {
			if (!BEASTObjectMethodsText.cfg.suppressBEASTObjects.contains(o.getClass().getName() + "." + input.getName())) {
				if (input.get() != null && 
						input.get() instanceof StateNode && 
						(!(input.get() instanceof TreeInterface)) && 
						((StateNode)input.get()).isEstimatedInput.get()) {
					if (!hasWith) {
						b.add(new Phrase("with "));
						hasWith = true;
					}
					b.add(new Phrase(input.get(), (BEASTInterface) brm, input, getInputName(input) + " "));
					done.add(input.get());
					List<Phrase> m = describePriors((StateNode) input.get(), parent, input2, doc);
					b.addAll(m);
				}
			}
		}
		return b;
	}

}
