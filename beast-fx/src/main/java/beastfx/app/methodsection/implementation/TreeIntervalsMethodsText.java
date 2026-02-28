package beastfx.app.methodsection.implementation;

import java.util.*;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.tree.TreeIntervals;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.Phrase;


public class TreeIntervalsMethodsText implements MethodsText {

	@Override
	public Class<?> type() {
		return TreeIntervals.class;
	}

	@Override
	public List<Phrase> getModelDescription(Object o, BEASTInterface parent, Input<?> input2, BeautiDoc doc) {
		return new ArrayList<>();
	}

}
