package test.beastfx.app.beauti;


import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import beastfx.app.beauti.BeautiTabPane;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;


/**
 * An MRCA prior is added without a distribution, which used to make BEAUti pop up a
 * "Could not add entry for distr" message, both when the prior was added and every
 * time the priors panel was shown again (issue #128).
 */
@ExtendWith(ApplicationExtension.class)
public class BeautiMRCAPriorTest extends BeautiBase {

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
	public void addMRCAPriorWithoutDistribution(FxRobot robot) throws Exception {
		// a single partition, so that adding the prior does not ask for a tree first
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		WaitForAsyncUtils.waitForFxEvents();

		selectTab(robot, "Priors");
		scrollToBottom(robot);
		clickOnButtonWithText(robot, "+ Add Prior");

		robot.clickOn("#idEntry").write("twoAnolis");
		robot.clickOn("#listOfTaxonCandidates").clickOn("Anolis_ahli");
		clickOnButtonWithText(robot, ">>");
		robot.clickOn("OK");

		// the prior has no distribution at this point, which is what used to fail
		assertNoMessageShowing(robot, "after adding an MRCA prior");

		// the priors panel is rebuilt every time it is shown
		selectTab(robot, "MCMC");
		selectTab(robot, "Priors");
		assertNoMessageShowing(robot, "after returning to the priors panel");

		// and picking a distribution should still work
		clickOnNodesWithID(robot, "twoAnolis.prior.distr");
		robot.clickOn("Normal");
		WaitForAsyncUtils.waitForFxEvents();
		assertNoMessageShowing(robot, "after selecting a distribution");
	}


	/** fail if BEAUti is showing a message dialog **/
	private void assertNoMessageShowing(FxRobot robot, String when) {
		for (Window window : robot.listWindows()) {
			if (window.isShowing() && window.getScene() != null) {
				DialogPane pane = findDialogPane(window.getScene().getRoot());
				if (pane != null) {
					fail("unexpected message " + when + ": " + pane.getContentText());
				}
			}
		}
	}


	/** first dialog pane in the node tree rooted at node, or null if there is none **/
	private DialogPane findDialogPane(Node node) {
		if (node instanceof DialogPane dialogPane) {
			return dialogPane;
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				DialogPane dialogPane = findDialogPane(child);
				if (dialogPane != null) {
					return dialogPane;
				}
			}
		}
		return null;
	}
}
