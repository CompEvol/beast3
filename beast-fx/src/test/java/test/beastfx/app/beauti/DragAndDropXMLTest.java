package test.beastfx.app.beauti;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import beast.base.inference.MCMC;
import beastfx.app.beauti.BeautiTabPane;
import beastfx.app.inputeditor.BeautiDoc;
import javafx.application.Platform;
import javafx.stage.Stage;


/**
 * Dropping a BEAST XML file on BEAUti used to import its alignment only, dropping the
 * model and MCMC settings on the floor (issue #141). It should load the complete analysis,
 * like File/Load does.
 */
@ExtendWith(ApplicationExtension.class)
public class DragAndDropXMLTest extends BeautiBase {

	final static long CHAIN_LENGTH = 12345678;

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
	public void droppedXMLLoadsModelAndMCMCSettings(FxRobot robot) throws Exception {
		// set up an analysis that differs from what the standard template produces:
		// HKY instead of JC69, and a non-default chain length
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		WaitForAsyncUtils.waitForFxEvents();

		selectTab(robot, "Site Model");
		robot.clickOn("#substModelComboBox").clickOn("HKY");
		WaitForAsyncUtils.waitForFxEvents();

		onFxThread(() -> {
			MCMC mcmc = doc.getMCMC();
			mcmc.chainLengthInput.setValue(CHAIN_LENGTH, mcmc);
		});
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "freqParameter.s:anolis");

		// tests run with the test output directory as working directory
		File xml = new File("dragAndDropTest.xml");
		xml.delete();
		onFxThread(() -> {
			try {
				doc.save(xml);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		assertThat(xml.exists()).as("saved analysis " + xml.getPath()).isTrue();
		assertThat(BeautiDoc.isBeautiAnalysisFile(xml))
			.as("saved analysis is recognised as a BEAUti analysis").isTrue();

		// start from scratch, as if BEAUti was just opened
		onFxThread(() -> doc.newAnalysis());
		assertThat(doc.alignments).as("analysis is empty before the drop").isEmpty();

		// drop the file: this is what the drag and drop handlers end up calling
		BeautiTabPane pane = robot.lookup("#BeautiTabPane").queryAs(BeautiTabPane.class);
		onFxThread(() -> pane.loadDroppedAnalyses(List.of(xml)));
		// loading itself is posted to the FX thread by loadDroppedAnalyses
		WaitForAsyncUtils.waitForFxEvents();

		// the alignment is back ...
		assertThat(doc.alignments.size()).as("number of alignments after the drop").isEqualTo(1);
		assertThat(doc.alignments.get(0).getID()).isEqualTo("anolis");
		// ... and so are the model and the MCMC settings
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "freqParameter.s:anolis");
		assertThat((long) doc.getMCMC().chainLengthInput.get())
			.as("chain length after the drop").isEqualTo(CHAIN_LENGTH);
	}


	/** run on the JavaFX thread and wait for it, and for anything it schedules, to finish **/
	private void onFxThread(Runnable runnable) {
		Platform.runLater(runnable);
		WaitForAsyncUtils.waitForFxEvents();
	}
}
