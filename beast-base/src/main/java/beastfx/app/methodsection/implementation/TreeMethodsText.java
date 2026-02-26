package beastfx.app.methodsection.implementation;



import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.evolution.tree.Tree;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.MethodsTextFactory;
import beastfx.app.methodsection.Phrase;


public class TreeMethodsText implements MethodsText {

	@Override
	public Class<?> type() {
		return Tree.class;
	}

	@Override
	public List<Phrase> getModelDescription(Object o, BEASTInterface parent, Input<?> input2, BeautiDoc doc) {
		List<Phrase> m = describePriors((BEASTObject) o, parent, input2, doc);
		if (m != null && m.size() > 0) {
			return m;
		}
		
		// collect outputs and outputs of outputs
		Set<BEASTInterface> outputs = new LinkedHashSet<>();
		outputs.addAll(((BEASTInterface)o).getOutputs());
		for (BEASTInterface output : ((BEASTInterface)o).getOutputs()) {
			outputs.addAll(output.getOutputs());			
		}
		// check if any of them is in the prior
		List<Phrase> b = new ArrayList<>();
		for (Object output : outputs) {
			if (output instanceof Distribution) {
				Distribution distr = (Distribution) output;
				// is it in the prior?
				for (Object output2 : distr.getOutputs()) {
					if (output2 instanceof CompoundDistribution && ((CompoundDistribution) output2).getID().equals("prior")) {
						b.add(new Phrase(" using "));
						m = MethodsTextFactory.getModelDescription(distr, (CompoundDistribution) output2, ((CompoundDistribution) output2).pDistributions, doc);
						b.addAll(m);
						done.remove(distr);
					}
				}
			}
		}
		return b;
	}

}
