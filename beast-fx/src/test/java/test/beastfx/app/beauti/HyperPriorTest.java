package test.beastfx.app.beauti;


import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.spec.inference.distribution.Gamma;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.distribution.Normal;
import beastfx.app.beauti.BeautiTabPane;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.service.query.NodeQuery;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Checking "estimate" on a distribution's own parameter (e.g. LogNormal's M or S) is meant to
 * add a "hyper prior" for that parameter to the Priors panel (after confirming the "Hyper prior"
 * warning dialog). This used to silently do nothing in the spec framework, because the parameter's
 * auto-generated id ("RealScalarParam.N") never matched the legacy "RealParameter" prefix check
 * that triggers renaming + hyper prior creation. It was also possible for the newly created
 * hyper prior's distribution to be domain-incompatible with the target parameter (e.g. a Normal,
 * which is Real-domain, assigned to S, which is PositiveReal-domain).
 */
@ExtendWith(ApplicationExtension.class)
public class HyperPriorTest extends BeautiBase {

	@Start
	public void start(Stage stage) {
		System.setProperty("beast.is.junit.testing", "true");
		try {
			BeautiTabPane tabPane = BeautiTabPane.main2(new String[] {}, stage);
			this.doc = tabPane.doc;
			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void addHyperPriorForRealDomainParameter(FxRobot robot) throws Exception {
		switchBirthRatePriorToLogNormal(robot);

		int priorCountBefore = priors().size();

		// M (mean) is Real-domain: unbounded, so a Normal hyper prior is domain-compatible
		fireCheckBox(robot, "M.isEstimated");
		WaitForAsyncUtils.waitForFxEvents();
		robot.clickOn("Yes");
		WaitForAsyncUtils.waitForFxEvents();

		List<Distribution> priors = priors();
		assertThat(priors.size()).as("hyper prior for M should have been added to the Priors panel")
				.isEqualTo(priorCountBefore + 1);
		assertThat(lastPrior(priors)).as("hyper prior for a Real-domain parameter should be a Normal")
				.isInstanceOf(Normal.class);

		makeSureXMLParses();
	}

	@Test
	public void addHyperPriorForPositiveRealDomainParameter(FxRobot robot) throws Exception {
		switchBirthRatePriorToLogNormal(robot);

		int priorCountBefore = priors().size();

		// S (stdev) is PositiveReal-domain: a Normal hyper prior (Real-domain) would be
		// domain-incompatible and show up as a mismatched entry in its distribution dropdown
		fireCheckBox(robot, "S.isEstimated");
		WaitForAsyncUtils.waitForFxEvents();
		robot.clickOn("Yes");
		WaitForAsyncUtils.waitForFxEvents();

		List<Distribution> priors = priors();
		assertThat(priors.size()).as("hyper prior for S should have been added to the Priors panel")
				.isEqualTo(priorCountBefore + 1);
		assertThat(lastPrior(priors)).as("hyper prior for a PositiveReal-domain parameter should be domain-compatible (Gamma), not Normal")
				.isInstanceOf(Gamma.class);

		makeSureXMLParses();
	}

	/** switch the birth rate's prior from the default Gamma to LogNormal, which has its own
	 * M (Real-domain) and S (PositiveReal-domain) parameters to toggle "estimate" on */
	private void switchBirthRatePriorToLogNormal(FxRobot robot) throws Exception {
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		WaitForAsyncUtils.waitForFxEvents();

		selectTab(robot, "Priors");
		WaitForAsyncUtils.waitForFxEvents();

		selectDistribution(robot, "birthRate.t:anolis.distr", "Log Normal");
		WaitForAsyncUtils.waitForFxEvents();

		Distribution birthRatePrior = null;
		for (Distribution d : priors()) {
			if (d instanceof LogNormal) {
				birthRatePrior = d;
			}
		}
		assertThat(birthRatePrior).as("birth rate prior should now be a LogNormal").isNotNull();
	}

	/** fires the "estimate" checkbox with the given fx:id directly via the API, rather than a
	 * screen-coordinate click, and without robot.interact()'s wait-for-completion: toggling
	 * this checkbox pops a nested modal "Hyper prior" confirmation dialog, which needs a
	 * *separate* robot action (see the caller) to dismiss -- interact() would deadlock waiting
	 * for a Runnable that can't finish until that later action runs. */
	private void fireCheckBox(FxRobot robot, String id) {
		NodeQuery q = robot.lookup(target -> id.equals(target.getId()) && target.isVisible());
		CheckBox cb = (CheckBox) q.query();
		Platform.runLater(() -> {
			if (!cb.isSelected()) {
				cb.fire();
			}
		});
	}

	/** select an item (matched by its combo-box display text, e.g. "Log Normal") from a
	 * distribution ComboBox, driving the ComboBox API directly rather than the dropdown UI
	 * (avoids the ComboBox's popup window sticking open, which then swallows later clicks). */
	@SuppressWarnings("unchecked")
	private void selectDistribution(FxRobot robot, String comboBoxId, String displayText) {
		NodeQuery q = robot.lookup(target -> comboBoxId.equals(target.getId()) && target.isVisible());
		ComboBox<Object> comboBox = (ComboBox<Object>) q.query();
		robot.interact(() -> {
			for (Object item : comboBox.getItems()) {
				if (item.toString().equals(displayText)) {
					comboBox.getSelectionModel().select(item);
					return;
				}
			}
			throw new AssertionError("No item \"" + displayText + "\" in ComboBox #" + comboBoxId);
		});
	}

	@SuppressWarnings("unchecked")
	private List<Distribution> priors() {
		CompoundDistribution prior = (CompoundDistribution) doc.pluginmap.get("prior");
		return prior.pDistributions.get();
	}

	private Distribution lastPrior(List<Distribution> priors) {
		return priors.get(priors.size() - 1);
	}
}
