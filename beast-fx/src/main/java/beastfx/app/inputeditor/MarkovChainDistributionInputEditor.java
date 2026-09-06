package beastfx.app.inputeditor;


import beast.base.spec.inference.distribution.MarkovChainDistribution;
import javafx.scene.control.ComboBox;

/**
 * Dedicated editor for a MarkovChainDistribution row in the Priors panel (e.g. Bayesian
 * Skyline's "Markov chained prior on population sizes"). Unlike IID or Dirichlet,
 * MarkovChainDistribution is not meant to be freely swapped for another prior via the
 * generic "distr" combo box: it requires specific wiring (jeffreys/initialMean) that a
 * domain-only compatibility check can't validate, and it has no registered subtemplate
 * of its own to switch back to. So this editor never offers a picker at all, and (being
 * registered for MarkovChainDistribution.class specifically, not TensorDistribution.class)
 * it never appears as a candidate in any other row's picker either.
 */
public class MarkovChainDistributionInputEditor extends TensorDistributionInputEditor {

	public MarkovChainDistributionInputEditor() {
		super();
	}
	public MarkovChainDistributionInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> type() {
		return MarkovChainDistribution.class;
	}

	@Override
	protected ComboBox<BeautiSubTemplate> createComboBox() {
		return null;
	}
}
