package beastfx.app.inputeditor;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.parser.PartitionContext;
import beast.base.spec.Bounded;
import beast.base.spec.inference.distribution.TensorDistribution;
import beast.base.spec.type.Tensor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;

import java.util.List;
import java.util.function.Function;

/**
 * Code shared by the prior input editors ({@link ScalarDistributionInputEditor},
 * {@link IIDInputEditor} and {@link TensorDistributionInputEditor}), so that
 * the three editors replace a prior, label their distribution combo box and
 * report an out-of-range initial value in the same way.
 */
final class PriorEditorUtil {

	private PriorEditorUtil() {
	}

	/** style used to mark the "initial = ..." button when the value is outside the prior's range **/
	static final String INVALID_STYLE = "-fx-text-fill: red;";

	/**
	 * Replace the prior at position itemNr of list by a new prior created from template,
	 * and attach the parameter of the old prior to the new one.
	 *
	 * @return the new prior
	 */
	static TensorDistribution<?,?> replacePrior(BeautiDoc doc, BeautiSubTemplate template,
			PartitionContext context, List<Distribution> list, int itemNr) {
		TensorDistribution<?,?> oldPrior = (TensorDistribution<?,?>) list.get(itemNr);
		String id = oldPrior.getID();
		BEASTInterface param = (BEASTInterface) oldPrior.paramInput.get();

		// Detach the old prior from the parameter. The effective bounds of a parameter are
		// computed from the priors in its outputs (see RealScalar.getLower), so a replaced
		// prior left there would keep restricting the parameter until the panel is rebuilt.
		if (param != null) {
			param.getOutputs().remove(oldPrior);
		}

		// createSubNet needs an input to put the new prior in; a dummy one is used, since
		// the prior goes in the list below rather than in an input of oldPrior
		Input<TensorDistribution<?,?>> dummyInput = new Input<>("param", "dummy input");
		dummyInput.setType(TensorDistribution.class);
		TensorDistribution<?,?> newPrior = (TensorDistribution<?,?>) template.createSubNet(context, oldPrior, dummyInput, true);

		// Attach the parameter. setValue also adds newPrior to the parameter's outputs.
		// initAndValidate() is not called again. The parameter bounds do not need it: they read
		// paramInput (TensorDistribution.getArguments) and call refresh(). An initial value
		// outside the new prior's range is reported by the editor (checkInitialValue below).
		// The field `param` that initAndValidate() caches for the MCMC hot path (calculateLogP,
		// dimension) stays unset, which is safe because BEAUti never evaluates a prior's
		// density, and the model is parsed again from XML for a run.
		newPrior.paramInput.setValue(param, newPrior);

		list.set(itemNr, newPrior);
		newPrior.setID(id);
		doc.pluginmap.remove(id);
		doc.registerPlugin(newPrior);
		return newPrior;
	}

	/**
	 * Button cell of the distribution combo box. The text is produced by a function
	 * given by the editor, and refresh() re-renders the cell, so the label rule is
	 * defined once and used both for drawing and for refreshing.
	 */
	static class DistrButtonCell extends ListCell<BeautiSubTemplate> {
		private final Function<BeautiSubTemplate, String> label;

		DistrButtonCell(Function<BeautiSubTemplate, String> label) {
			this.label = label;
		}

		@Override
		protected void updateItem(BeautiSubTemplate item, boolean empty) {
			super.updateItem(item, empty);
			setText(empty || item == null ? null : label.apply(item));
		}

		/** redraw the label, e.g. after a parameter of the distribution was edited **/
		void refresh() {
			updateItem(getItem(), isEmpty());
		}
	}

	/** re-render the button cell of a combo box created with a {@link DistrButtonCell} **/
	static void refreshLabel(ComboBox<BeautiSubTemplate> comboBox) {
		if (comboBox != null && comboBox.getButtonCell() instanceof DistrButtonCell cell) {
			cell.refresh();
		}
	}

	/**
	 * @return a message if the initial value of the parameter of prior is outside the
	 * range allowed by its domain and the prior, or null if it is valid or there is no parameter.
	 */
	static String invalidInitialValueMessage(TensorDistribution<?,?> prior) {
		if (prior == null || !(prior.paramInput.get() instanceof Tensor<?,?> param)) {
			return null;
		}
		try {
			// isValid() checks the value against the effective bounds, which include the bounds
			// of the prior (via TensorDistribution.getArguments)
			if (param.isValid()) {
				return null;
			}
		} catch (RuntimeException e) {
			return "Initial value of " + ((BEASTInterface) param).getID() + " could not be checked: " + e.getMessage();
		}
		String range = param instanceof Bounded<?> b ? " " + b.boundsToString() : "";
		return "Initial value " + param.get() + " of " + ((BEASTInterface) param).getID()
				+ " is outside the range" + range + " allowed by its prior "
				+ prior.getClass().getSimpleName() + ". Change the initial value or the prior.";
	}

	/**
	 * Show an out-of-range initial value in the editor: the validation label (if the editor
	 * has one) turns red, the "initial = ..." button turns red with the message as tooltip,
	 * and validation listeners are told the input is invalid.
	 * <p>
	 * Called from validateInput(), which runs again whenever the panel is rebuilt or a value
	 * changes, so the error stays visible until the user fixes it, and then goes away.
	 *
	 * @return true if the initial value is valid
	 */
	static boolean checkInitialValue(InputEditor.Base editor, TensorDistribution<?,?> prior,
			Button initialValueButton, String initialValueTooltip) {
		String message = invalidInitialValueMessage(prior);
		if (initialValueButton != null) {
			initialValueButton.setStyle(message == null ? "" : INVALID_STYLE);
			initialValueButton.setTooltip(new Tooltip(message == null ? initialValueTooltip : message));
		}
		if (message == null) {
			return true;
		}
		if (editor.m_validateLabel != null) {
			editor.m_validateLabel.setTooltip(new Tooltip(message));
			editor.m_validateLabel.setColor("red");
			editor.m_validateLabel.setVisible(true);
		}
		editor.notifyValidationListeners(InputEditor.ValidationStatus.IS_INVALID);
		return false;
	}
}
