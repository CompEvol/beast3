package test.beastfx.app.beauti;



import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beastfx.app.beauti.BeautiTabPane;
import beastfx.app.inputeditor.AlignmentListInputEditor.Partition0;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.stage.Stage;



@ExtendWith(ApplicationExtension.class)
public class BeautiSimpleTest extends BeautiBase {

    @Start
    public void start(Stage stage) {
    	try {
    		BeautiTabPane tabPane = BeautiTabPane.main2(new String[] {}, stage);
    		this.doc = tabPane.doc;
            stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	
    	
	@Test
	void simpleTest(FxRobot robot) throws Exception {

		importAlignment(NEXUS_DIR, new File("anolis.nex"));

		// load anolis.nex
		BeautiTabPane pane = robot.lookup("#BeautiTabPane").queryAs(BeautiTabPane.class);
		assertThat(pane.isVisible());
		String[] titles = tabTitles(pane);

		assertArrayEquals(titles,"[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
		System.err.println(Arrays.toString(titles));
		robot.clickOn("#Partitions");
		Tab f = pane.getSelectionModel().getSelectedItem();
		TableView<Partition0> table = robot.lookup(".table-view").queryAs(TableView.class);
		String[][] tc = tableContents(table);
		System.err.println(Arrays.toString(tc[0]));
		assertEquals(Arrays.toString(tc[0]),"[anolis, anolis, 29, 1456, nucleotide, anolis, anolis, anolis, false]");
		assertThat(f).isNotNull();
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "YuleModelBICEPSEpochTop.t:anolis", "YuleModelBICEPSEpochAll.t:anolis", "YuleModelBICEPSTreeFlex.t:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis");

		(new File("/tmp/simpleTest1.png")).delete();
		(new File("/tmp/simpleTest2.png")).delete();
		(new File("/tmp/simpleTest3.png")).delete();
		(new File("/tmp/simpleTest4.png")).delete();

		screenshot("/tmp/simpleTest1.png");
		

		// Set the site model to HKY (estimated)
		robot.clickOn("#SiteModel");
		robot.clickOn("#substModelComboBox").clickOn("HKY");

		printBeautiState();
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "freqParameter.s:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "YuleModelBICEPSEpochTop.t:anolis", "YuleModelBICEPSEpochAll.t:anolis", "YuleModelBICEPSTreeFlex.t:anolis", "KappaScaler.s:anolis", "FrequenciesExchanger.s:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis", "KappaPrior.s:anolis", "FrequenciesPrior.s:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "freqParameter.s:anolis");

		// Set the site model to HKY (G4) (estimated)
		robot.clickOn("#SiteModel");
		robot.clickOn("#gammaCategoryCount");
		robot.write("4");
		
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "gammaShape.s:anolis", "freqParameter.s:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "YuleModelBICEPSEpochTop.t:anolis", "YuleModelBICEPSEpochAll.t:anolis", "YuleModelBICEPSTreeFlex.t:anolis", "KappaScaler.s:anolis", "gammaShapeScaler.s:anolis", "FrequenciesExchanger.s:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis", "KappaPrior.s:anolis", "GammaShapePrior.s:anolis", "FrequenciesPrior.s:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "kappa.s:anolis", "gammaShape.s:anolis", "freqParameter.s:anolis");

		screenshot("/tmp/simpleTest2.png");
		
		// rename tree from 'anolis' to 'tree'
		robot.clickOn("#Partitions");
		// table = robot.lookup(".table-view").queryAs(TableView.class);
		robot.clickOn("#treeModelCell");
		robot.eraseText(10);
		robot.write("tree\n");

		printBeautiState();
		assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:anolis", "gammaShape.s:anolis", "freqParameter.s:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "KappaScaler.s:anolis", "gammaShapeScaler.s:anolis", "FrequenciesExchanger.s:anolis");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "GammaShapePrior.s:anolis", "KappaPrior.s:anolis", "FrequenciesPrior.s:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "kappa.s:anolis", "gammaShape.s:anolis", "freqParameter.s:anolis");

		screenshot("/tmp/simpleTest3.png");
		robot.clickOn("#Priors");
		screenshot("/tmp/simpleTest4.png");
		
		makeSureXMLParses();
	}

}
