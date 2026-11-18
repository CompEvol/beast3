package beastfx.app.inputeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.CompoundDistribution;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Exponential;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.Poisson;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.distribution.Uniform;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beastfx.app.inputeditor.ScalarDistributionInputEditor.PDPanel;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * The "log x" option of the density plot under a scalar prior (#293): when it is offered,
 * the tick labels, the range of the curve, and that ticking it redraws every plot on screen.
 */
@ExtendWith(ApplicationExtension.class)
public class LogScalePlotTest {

	@Start
	public void start(Stage stage) {
		// nothing to show, only the JavaFX toolkit is required
	}

	/** the editor caches templates in static fields, and the log scale choice is shared */
	@BeforeEach
	@AfterEach
	public void reset() throws Exception {
		for (String name : new String[] {"scalarTemplates", "templateInstances", "templateDomains"}) {
			Field field = ScalarDistributionInputEditor.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(null, null);
		}
		PDPanel.logScale = false;
	}

	@Test
	void formatPowerOfTen() {
		assertEquals("1", ScalarDistributionInputEditor.formatPowerOfTen(0));
		assertEquals("10", ScalarDistributionInputEditor.formatPowerOfTen(1));
		assertEquals("10000", ScalarDistributionInputEditor.formatPowerOfTen(4));
		assertEquals("0.1", ScalarDistributionInputEditor.formatPowerOfTen(-1));
		assertEquals("0.001", ScalarDistributionInputEditor.formatPowerOfTen(-3));
		assertEquals("1e5", ScalarDistributionInputEditor.formatPowerOfTen(5));
		assertEquals("1e-4", ScalarDistributionInputEditor.formatPowerOfTen(-4));
		// not a whole power of ten
		assertEquals("3.2", ScalarDistributionInputEditor.formatPowerOfTen(0.5));
	}

	/** offered for a continuous distribution on positive values only */
	@Test
	void logScaleOfferedForPositiveContinuousOnly() {
		assertTrue(offersLogScale(new LogNormal(null, new RealScalarParam<>(0.0, Real.INSTANCE),
				new RealScalarParam<>(8.0, PositiveReal.INSTANCE))), "LogNormal");
		assertTrue(offersLogScale(new Exponential(null, new RealScalarParam<>(1.0, PositiveReal.INSTANCE))), "Exponential");
		assertTrue(offersLogScale(new Uniform(null, new RealScalarParam<>(0.0, Real.INSTANCE),
				new RealScalarParam<>(1.0, Real.INSTANCE))), "Uniform(0, 1)");

		assertFalse(offersLogScale(new Normal(null, new RealScalarParam<>(0.0, Real.INSTANCE),
				new RealScalarParam<>(1.0, PositiveReal.INSTANCE))), "Normal reaches below 0");
		assertFalse(offersLogScale(new Uniform(null, new RealScalarParam<>(-1.0, Real.INSTANCE),
				new RealScalarParam<>(1.0, Real.INSTANCE))), "Uniform(-1, 1) reaches below 0");
		assertFalse(offersLogScale(new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE))),
				"Poisson is an integer distribution");
	}

	private static boolean offersLogScale(ScalarDistribution<?, ?> distr) {
		return ScalarDistributionInputEditor.canUseLogScale(distr,
				toDouble(distr.inverseCumulativeProbability(0.01)), toDouble(distr.inverseCumulativeProbability(0.99)));
	}

	private static double toDouble(Object o) {
		return ((Number) o).doubleValue();
	}

	/**
	 * Ticking "log x" under one prior redraws the other priors on screen as well, and the
	 * curve comes close to 0 at both ends instead of stopping in mid-air. The box is hidden
	 * under an integer prior.
	 */
	@Test
	void tickingLogScaleRedrawsAllPlots() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		Map<Object, PDPanel> panels = new HashMap<>();
		List<String> yLabels = new ArrayList<>();
		List<Boolean> ticked = new ArrayList<>();
		double[] endsOverPeak = new double[2];
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			RealScalarParam<NonNegativeReal> x = new RealScalarParam<>(1.0, NonNegativeReal.INSTANCE);
			x.setID("x");
			Exponential exponential = new Exponential(x, new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
			exponential.setID("XPrior");
			RealScalarParam<PositiveReal> y = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
			y.setID("y");
			LogNormal logNormal = new LogNormal(y, new RealScalarParam<>(0.0, Real.INSTANCE),
					new RealScalarParam<>(8.0, PositiveReal.INSTANCE));
			logNormal.setID("YPrior");
			IntScalarParam<NonNegativeInt> k = new IntScalarParam<>(2, NonNegativeInt.INSTANCE);
			k.setID("k");
			Poisson poisson = new Poisson(k, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE));
			poisson.setID("KPrior");
			CompoundDistribution prior = new CompoundDistribution();
			prior.initByName("distribution", exponential, "distribution", logNormal, "distribution", poisson);
			prior.setID("prior");
			for (BEASTInterface o : new BEASTInterface[] {x, exponential, y, logNormal, k, poisson, prior}) {
				doc.registerPlugin(o);
			}

			// expanded, as in the priors panel, so that each prior shows its plot
			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, prior, true, InputEditor.ExpandOption.TRUE,
					InputEditor.ButtonStatus.ALL, null, doc);
			new Scene(new VBox(editor.getComponent()));
			collectPanels(editor.getComponent(), panels);
			assertEquals(3, panels.size(), "one plot per prior");

			PDPanel exponentialPanel = panels.get(exponential);
			assertTrue(exponentialPanel.logScaleBox.isVisible(), "log x not offered for Exponential");
			assertTrue(panels.get(logNormal).logScaleBox.isVisible(), "log x not offered for LogNormal");
			assertFalse(panels.get(poisson).logScaleBox.isVisible(), "log x offered for Poisson");

			exponentialPanel.logScaleBox.setSelected(true);
			exponentialPanel.logScaleBox.getOnAction().handle(null);

			for (PDPanel panel : List.of(exponentialPanel, panels.get(logNormal))) {
				yLabels.add(panel.yAxis.getLabel());
				ticked.add(panel.logScaleBox.isSelected());
			}
			List<Data<Number, Number>> data = exponentialPanel.series.getData();
			double peak = data.stream().mapToDouble(d -> d.getYValue().doubleValue()).max().orElse(0);
			endsOverPeak[0] = data.get(0).getYValue().doubleValue() / peak;
			endsOverPeak[1] = data.get(data.size() - 1).getYValue().doubleValue() / peak;
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		assertEquals(List.of("p(log10 x)", "p(log10 x)"), yLabels, "plots not all redrawn on a log scale");
		assertEquals(List.of(true, true), ticked, "log x not ticked under every plot");
		// Exponential(1) at its 0.1% quantile: 0.0023 against a peak of 0.85 (0.027 from the 1% quantile)
		assertTrue(endsOverPeak[0] < 0.01, "curve starts at " + endsOverPeak[0] + " of its peak");
		assertTrue(endsOverPeak[1] < 0.05, "curve ends at " + endsOverPeak[1] + " of its peak");
	}

	/** the plot under each prior, by the prior */
	private static void collectPanels(Node node, Map<Object, PDPanel> panels) {
		if (node instanceof ScalarDistributionInputEditor editor && editor.graphPanel != null) {
			panels.put(editor.m_beastObject, editor.graphPanel);
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				collectPanels(child, panels);
			}
		}
	}

	private static void runOnFXThread(FXTask task, AtomicReference<Throwable> error) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				task.run();
			} catch (Throwable e) {
				error.set(e);
			} finally {
				latch.countDown();
			}
		});
		if (!latch.await(120, TimeUnit.SECONDS)) {
			throw new AssertionError("timed out waiting for JavaFX thread");
		}
	}

	private interface FXTask {
		void run() throws Exception;
	}
}
