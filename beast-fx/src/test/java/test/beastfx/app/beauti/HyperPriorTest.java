package test.beastfx.app.beauti;


import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.distribution.LogUniform;
import beastfx.app.beauti.BeautiTabPane;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Checking "estimate" on a LogNormal prior's own parameter (M or S) in the Priors panel is meant
 * to pop up a "Hyper prior" confirmation dialog, and, once confirmed, add a hyper prior (a
 * LogUniform distribution) for that parameter as a new row in the Priors panel. This used to
 * silently do nothing in the spec framework, because the parameter's auto-generated id
 * ("RealScalarParam.N") never matched the legacy "RealParameter" prefix check that triggered
 * renaming + hyper prior creation, and the legacy hyper prior object was also incompatible with
 * spec-framework parameters -- so no row was ever added.
 * <p>
 * The same "estimate" checkbox in the Site Model and Clock Model panels must never pop up the
 * hyper prior dialog at all, since those parameters (e.g. mutationRate, clock.rate) are not a
 * distribution's own hyperparameter -- see spec.ScalarInputEditor#isHyperparameter.
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

	/**
	 * Both M and S are checked in a single test method (rather than one @Test each) because
	 * every "estimate" checkbox in this window shares its plain fx:id across BEAUti windows
	 * (e.g. "M.isEstimated" is not partition-qualified), and this test harness does not close a
	 * previous test method's window before the next one opens (see FixedMeanRateTest's comment
	 * on cascading windows) -- a second, separate @Test method here would risk resolving its
	 * checkbox lookup against the first method's now-stale window instead of its own.
	 */
	@Test
	public void addHyperPriorForLogNormalMAndS(FxRobot robot) throws Exception {
		switchBirthRatePriorToLogNormal(robot);

		// M is Real-domain, S is PositiveReal-domain: both get a LogUniform hyper prior
		int priorCountBefore = priors().size();
		clickOnNodesWithID(robot, "M.isEstimated");
		WaitForAsyncUtils.waitForFxEvents();
		robot.clickOn("Yes");
		WaitForAsyncUtils.waitForFxEvents();

		List<Distribution> priors = priors();
		assertThat(priors.size()).as("a hyper prior for M should have been added to the Priors panel")
				.isEqualTo(priorCountBefore + 1);
		LogUniform hyperPriorForM = (LogUniform) lastPrior(priors);
		assertThat(hyperPriorForM.paramInput.get()).as("the hyper prior must apply to M itself")
				.isSameAs(findLogNormal(priors).MParameterInput.get());

		priorCountBefore = priors().size();
		clickOnNodesWithID(robot, "S.isEstimated");
		WaitForAsyncUtils.waitForFxEvents();
		robot.clickOn("Yes");
		WaitForAsyncUtils.waitForFxEvents();

		priors = priors();
		assertThat(priors.size()).as("a hyper prior for S should have been added to the Priors panel")
				.isEqualTo(priorCountBefore + 1);
		LogUniform hyperPriorForS = (LogUniform) lastPrior(priors);
		assertThat(hyperPriorForS.paramInput.get()).as("the hyper prior must apply to S itself")
				.isSameAs(findLogNormal(priors).SParameterInput.get());

		makeSureXMLParses();
	}

	@Test
	public void siteModelEstimateNeverOffersHyperPrior(FxRobot robot) throws Exception {
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		WaitForAsyncUtils.waitForFxEvents();

		selectTab(robot, "Site Model");
		WaitForAsyncUtils.waitForFxEvents();
		int priorCountBefore = priors().size();

		// if this popped up the "Hyper prior" dialog, nothing here would dismiss it, and
		// the assertion below would see a stale (unsynced) prior list
		clickOnNodesWithID(robot, "mutationRate.isEstimated");
		WaitForAsyncUtils.waitForFxEvents();

		assertThat(priors().size()).as("estimating a Site Model parameter must never add a hyper prior")
				.isEqualTo(priorCountBefore);
	}

	@Test
	public void clockModelEstimateNeverOffersHyperPrior(FxRobot robot) throws Exception {
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		WaitForAsyncUtils.waitForFxEvents();

		selectTab(robot, "Clock Model");
		WaitForAsyncUtils.waitForFxEvents();
		int priorCountBefore = priors().size();

		clickOnNodesWithID(robot, "clock.rate.isEstimated");
		WaitForAsyncUtils.waitForFxEvents();

		assertThat(priors().size()).as("estimating a Clock Model parameter must never add a hyper prior")
				.isEqualTo(priorCountBefore);
	}

	/** switch the birth rate's prior from the default Gamma to LogNormal, then expand its row so
	 * its own M (Real-domain) and S (PositiveReal-domain) parameters -- collapsed by default,
	 * like every prior row -- render their "estimate" checkboxes. LogNormal itself is
	 * PositiveReal-domain, matching birthRate -- Normal (Real-domain) is not a domain-compatible
	 * option for birthRate, so this is not just an arbitrary choice. */
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

		clickOnNodesWithID(robot, birthRatePrior.getID() + ".editButton");
		WaitForAsyncUtils.waitForFxEvents();
	}

	/** select an item (matched by its combo-box display text, e.g. "Log Normal") from a
	 * distribution ComboBox, driving the ComboBox API directly rather than the dropdown UI
	 * (avoids the ComboBox's popup window sticking open, which would then swallow later clicks). */
	@SuppressWarnings("unchecked")
	private void selectDistribution(FxRobot robot, String comboBoxId, String displayText) {
		NodeQuery q = robot.lookup(target -> comboBoxId.equals(target.getId()) && target.isVisible());
		Set<javafx.scene.Node> nodes = q.queryAll();
		assertThat(nodes).as("no visible ComboBox with id " + comboBoxId).isNotEmpty();
		ComboBox<Object> comboBox = (ComboBox<Object>) nodes.iterator().next();
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

	private List<Distribution> priors() {
		CompoundDistribution prior = (CompoundDistribution) doc.pluginmap.get("prior");
		return prior.pDistributions.get();
	}

	private Distribution lastPrior(List<Distribution> priors) {
		return priors.get(priors.size() - 1);
	}

	private LogNormal findLogNormal(List<Distribution> priors) {
		for (Distribution d : priors) {
			if (d instanceof LogNormal ln) {
				return ln;
			}
		}
		throw new AssertionError("No LogNormal found among the priors");
	}
}
