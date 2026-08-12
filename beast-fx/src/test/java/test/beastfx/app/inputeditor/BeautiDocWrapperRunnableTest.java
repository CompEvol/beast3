package test.beastfx.app.inputeditor;


import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.operator.TipDatesRandomWalker;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.parser.PartitionContext;
import beast.base.spec.domain.Real;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.base.spec.inference.operator.ScaleOperator;
import beast.base.spec.inference.parameter.RealScalarParam;
import beastfx.app.beauti.OperatorListInputEditor;
import beastfx.app.inputeditor.*;
import beastfx.app.inputeditor.InputEditor.ExpandOption;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Regression tests for issue #131 and PRs #132 / #133.
 *
 * BEAUti assumes the active analysis (BeautiDoc.mcmc) is always an MCMC, and used to hard-cast
 * it as such in many places. That assumption breaks for a wrapper Runnable such as
 * modelselection.inference.PathSampler, which does not extend MCMC but instead holds its own
 * MCMC via an input named "mcmc" (e.g. for path-sampling/stepping-stone model selection). Since
 * that package is not a dependency of this repo, {@link WrapperRunnable} below stands in for it:
 * same shape ({@link BeautiDoc#getMCMC()} and friends look for an input literally named "mcmc"
 * on a non-MCMC Runnable), without needing the real package on the test classpath.
 *
 * Each test below simulates one BEAUti interaction (applying a template connector, evaluating a
 * connector's guard condition, populating a panel, opening the Operators panel, toggling tip-date
 * sampling) while {@link WrapperRunnable} is the active runnable, and checks that BEAUti reaches
 * through to the wrapper's inner MCMC instead of throwing or silently doing nothing.
 */
@ExtendWith(ApplicationExtension.class)
public class BeautiDocWrapperRunnableTest {

	/** Minimal stand-in for modelselection.inference.PathSampler: a Runnable that is not
	 *  itself an MCMC, but wraps one via an input named "mcmc". */
	public static class WrapperRunnable extends beast.base.inference.Runnable {
		final public Input<MCMC> mcmcInput = new Input<>("mcmc", "wrapped MCMC", Input.Validate.REQUIRED);
		@Override public void initAndValidate() {}
		@Override public void run() throws Exception {}
	}


	@Start
	public void start(Stage stage) {
		// nothing to show, only the JavaFX toolkit is required
	}


	/** a minimal but otherwise ordinary MCMC: empty operator list, one loggable parameter. */
	private static MCMC newMCMC(String id) {
		CompoundDistribution posterior = new CompoundDistribution();
		posterior.setID("posterior." + id);
		posterior.initAndValidate();

		RealScalarParam<Real> param = new RealScalarParam<>(1.0, Real.INSTANCE);
		param.setID("param." + id);
		State state = new State();
		state.initByName("stateNode", param);
		state.setID("state." + id);

		beast.base.inference.Logger logger = new beast.base.inference.Logger();
		logger.setID("logger." + id);
		logger.initByName("log", param);

		beast.base.inference.OperatorSchedule schedule = new beast.base.inference.OperatorSchedule();
		schedule.setID("operatorschedule." + id);

		MCMC mcmc = new MCMC();
		mcmc.setID(id);
		mcmc.initByName("chainLength", 1000L, "state", state, "distribution", posterior,
				"operator", new ArrayList<Operator>(), "logger", logger, "operatorschedule", schedule);
		return mcmc;
	}

	private static WrapperRunnable newWrapper(String id, MCMC inner) {
		WrapperRunnable wrapper = new WrapperRunnable();
		wrapper.setID(id);
		wrapper.initByName("mcmc", inner);
		return wrapper;
	}

	/**
	 * Puts a wrapper Runnable in charge of doc, the way BEAUti would after the user selects a
	 * PathSampler-style template: doc.mcmc points at the wrapper, and both the wrapper and its
	 * inner MCMC are registered under doc so BeautiDoc can look them up by ID.
	 *
	 * @return the wrapper's inner MCMC, for the test to act on directly
	 */
	private static MCMC activateWrapperRunnable(BeautiDoc doc) {
		MCMC inner = newMCMC("inner");
		WrapperRunnable wrapper = newWrapper("pathSampler", inner);
		doc.mcmc.setValue(wrapper, doc);
		doc.registerPlugin(wrapper);
		doc.registerPlugin(inner);
		return inner;
	}

	/** a standalone Operator, suitable for connecting/disconnecting/toggling in a test --
	 *  its own construction is not what's under test. */
	private static ScaleOperator newTestOperator(String id) {
		RealScalarParam<Real> scaleParam = new RealScalarParam<>(2.0, Real.INSTANCE);
		scaleParam.setID("scaleParam." + id);
		ScaleOperator operator = new ScaleOperator();
		operator.setID(id);
		operator.initByName("parameter", scaleParam, "weight", 1.0, "scaleFactor", 0.7);
		return operator;
	}


	/**
	 * Scenario: BeautiDoc.getMCMC() is called with a wrapper Runnable, and separately with a
	 * plain MCMC. It should unwrap the wrapper to its inner MCMC, and return a plain MCMC as-is.
	 */
	@Test
	public void getMCMCUnwrapsWrapperRunnable() {
		MCMC inner = newMCMC("inner");
		WrapperRunnable wrapper = newWrapper("pathSampler", inner);

		assertSame(inner, BeautiDoc.getMCMC(wrapper),
				"getMCMC() should unwrap a wrapper Runnable to its inner MCMC");
		assertSame(inner, BeautiDoc.getMCMC(inner),
				"getMCMC() should return a plain MCMC as-is");
	}


	/**
	 * Scenario: a template rule such as {@code <connect targetID='mcmc' inputName='operator' .../>}
	 * is applied and then reversed against a wrapper Runnable -- this is what
	 * BeautiDoc.scrubAll() does every time a partition or model choice changes in BEAUti,
	 * calling connect() when the rule's condition holds and disconnect() when it doesn't.
	 * Both should act on the wrapper's inner MCMC, not the wrapper itself.
	 *
	 * Bug fixed by PR #133: only connect() redirected to the inner MCMC; disconnect() resolved
	 * the wrapper itself, found no "operator" input on it, and the resulting
	 * IllegalArgumentException was swallowed by scrubAll()'s catch-and-print. So an object
	 * connected via a wrapper could be added but never removed again.
	 */
	@Test
	public void connectThenDisconnectViaWrapperNetsToRemoved() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		runOnFXThread(() -> {
			// given a wrapper Runnable is active, and an operator to wire in
			BeautiDoc doc = new BeautiDoc();
			MCMC inner = activateWrapperRunnable(doc);
			Operator operator = newTestOperator("op1");
			doc.registerPlugin(operator);
			PartitionContext ctx = new PartitionContext("", "", "", "");
			BeautiConnector connector = new BeautiConnector("op1", "mcmc", "operator", null);

			// when the rule's condition holds, connect() should reach the inner MCMC
			doc.connect(connector, ctx);
			assertTrue(inner.operatorsInput.get().contains(operator),
					"connect() should add the operator to the wrapper's inner MCMC");

			// when the condition no longer holds, disconnect() should reach it too
			doc.disconnect(connector, ctx);
			assertFalse(inner.operatorsInput.get().contains(operator),
					"disconnect() should remove the operator from the inner MCMC again (issue #131/#133 regression)");
		}, error);
		if (error.get() != null) {
			throw new AssertionError(error.get());
		}
	}


	/**
	 * Scenario: a connector's {@code nooperator(op1)} guard condition -- used by templates to
	 * skip a rule once a given operator already exists -- is evaluated while a wrapper Runnable
	 * is active. It should read op1's presence from the wrapper's inner MCMC operator list, not
	 * the wrapper (which has no operator list of its own).
	 *
	 * Bug fixed by PR #132: isActivated() read {@code ((MCMC) doc.mcmc.get()).operatorsInput}
	 * directly, throwing ClassCastException for a wrapper Runnable before it could even check
	 * whether op1 was present.
	 */
	@Test
	public void connectorIsActivatedNoOperatorConditionUsesInnerMCMC() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		runOnFXThread(() -> {
			// given a wrapper Runnable is active, and an operator that is not yet connected
			BeautiDoc doc = new BeautiDoc();
			MCMC inner = activateWrapperRunnable(doc);
			Operator operator = newTestOperator("op1");
			doc.registerPlugin(operator);
			PartitionContext ctx = new PartitionContext("", "", "", "");
			List<BEASTInterface> empty = new ArrayList<>();
			BeautiConnector connector = new BeautiConnector("someSrc", "someTarget", "someInput", "nooperator(op1)");

			// while op1 is absent from the inner MCMC, "no operator" should hold
			assertTrue(connector.isActivated(ctx, empty, empty, doc),
					"nooperator(op1) should hold before op1 is in the wrapper's inner MCMC operator list");

			// once op1 is added to the inner MCMC, "no operator" should no longer hold
			inner.operatorsInput.get().add(operator);
			assertFalse(connector.isActivated(ctx, empty, empty, doc),
					"nooperator(op1) should no longer hold once op1 is in the inner MCMC's operator list (issue #131/#132 regression)");
		}, error);
		if (error.get() != null) {
			throw new AssertionError(error.get());
		}
	}


	/**
	 * Scenario: the Operators panel's configuration ({@code path="operator"}) is resolved while
	 * a wrapper Runnable is active -- this is what actually populates the Partitions, Priors,
	 * and Operators panels in BEAUti. It should walk down into the wrapper's inner MCMC operator
	 * list, rather than fail because the wrapper has no "operator" input of its own.
	 *
	 * Bug fixed by PR #132: resolveInput() called beastObject.getInput("operator") on the
	 * wrapper directly, which threw IllegalArgumentException -- caught by resolveInput()'s own
	 * catch-all and logged, leaving the panel silently empty instead of showing the operators.
	 */
	@Test
	public void panelConfigResolveInputFallsBackToInnerMCMC() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		runOnFXThread(() -> {
			// given a wrapper Runnable is active, with one operator already on its inner MCMC
			BeautiDoc doc = new BeautiDoc();
			MCMC inner = activateWrapperRunnable(doc);
			Operator operator = newTestOperator("op1");
			inner.operatorsInput.get().add(operator);

			// and a panel configured the same way the real Operators panel is: path="operator"
			BeautiPanelConfig panelConfig = new BeautiPanelConfig();
			panelConfig.initByName("panelname", "Operators", "tiptext", "operators", "path", "operator");

			// resolving the panel's input should find the inner MCMC's operators, not fail silently
			Input<?> resolved = panelConfig.resolveInput(doc, 0);
			assertNotNull(resolved,
					"resolveInput() should not silently fail for a wrapper Runnable (issue #131/#132 regression)");
			assertTrue(((List<?>) resolved.get()).contains(operator),
					"the Operators panel should resolve to the wrapper's inner MCMC operator list");
		}, error);
		if (error.get() != null) {
			throw new AssertionError(error.get());
		}
	}


	/**
	 * Scenario: the Operators panel is opened while a wrapper Runnable is active. Beneath the
	 * operator list, it also builds a sub-editor for the operator schedule; that sub-editor
	 * should be built against the wrapper's inner MCMC, not throw trying to read
	 * "operatorschedule" off the wrapper itself.
	 *
	 * Bug fixed by PR #133: OperatorListInputEditor read doc.mcmc.get() unchecked/uncast for the
	 * "operatorschedule" input, so it was missed by #131's (MCMC)-cast sweep, but failed the
	 * same way for a wrapper Runnable -- getInput("operatorschedule") throws
	 * IllegalArgumentException for an unknown input name.
	 */
	@Test
	public void operatorListInputEditorUsesInnerMCMCOperatorSchedule() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			// BEASTObjectInputEditor.init() (used for the operatorschedule sub-editor)
			// needs a loaded BeautiConfig regardless of addButtons
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));
			MCMC inner = activateWrapperRunnable(doc);

			// opening the Operators panel builds this editor against doc.mcmc.get() (the wrapper)
			OperatorListInputEditor editor = new OperatorListInputEditor(doc);
			editor.init(inner.operatorsInput, inner, -1, ExpandOption.FALSE, false);
		}, error);
		if (error.get() != null) {
			error.get().printStackTrace();
			fail("OperatorListInputEditor.init() should not throw for a wrapper Runnable: " + error.get());
		}
	}


	/**
	 * Scenario: an MRCA prior's "sample tip dates" checkbox (Priors panel) is switched on and
	 * then off while a wrapper Runnable is active. Turning it on should add a
	 * TipDatesRandomWalker operator to the wrapper's inner MCMC; turning it off should remove it
	 * again.
	 *
	 * Bug fixed by PR #133: MRCAPriorInputEditor read/wrote doc.mcmc.get()'s "operator" input
	 * unchecked/uncast, so like OperatorListInputEditor it was missed by #131's cast sweep but
	 * failed the same way for a wrapper Runnable.
	 */
	@Test
	public void tipDateSamplingToggleUsesInnerMCMCOperatorList() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		runOnFXThread(() -> {
			// given a wrapper Runnable is active
			BeautiDoc doc = new BeautiDoc();
			MCMC inner = activateWrapperRunnable(doc);

			// and an MRCA prior on a two-taxon tree, as the Priors panel would show
			Tree tree = new TreeParser("(A:1.0,B:1.0):0.0;", false);
			tree.setID("tree.t:test");
			Taxon a = new Taxon();
			a.setID("A");
			Taxon b = new Taxon();
			b.setID("B");
			TaxonSet taxonset = new TaxonSet();
			taxonset.initByName("taxon", a, "taxon", b);
			taxonset.setID("ab.taxonset");
			MRCAPrior prior = new MRCAPrior();
			prior.initByName("tree", tree, "taxonset", taxonset);
			prior.setID("ab.prior");
			doc.registerPlugin(tree);
			doc.registerPlugin(taxonset);
			doc.registerPlugin(prior);

			// the editor's m_beastObject is normally set by init(), which needs a full UI
			// build we don't want here -- reach around it directly instead
			MRCAPriorInputEditor editor = new MRCAPriorInputEditor(doc);
			setBeastObject(editor, prior);

			// when "sample tip dates" is switched on, the operator should land on the inner MCMC
			invokePrivate(MRCAPriorInputEditor.class, "enableTipSampling", editor);
			assertTrue(hasTipDatesRandomWalker(inner),
					"enableTipSampling() should add a TipDatesRandomWalker to the wrapper's inner MCMC");

			// when switched off again, it should come off the inner MCMC too
			invokeStaticPrivate(MRCAPriorInputEditor.class, "disableTipSampling",
					new Class<?>[] { BEASTInterface.class, BeautiDoc.class }, prior, doc);
			assertFalse(hasTipDatesRandomWalker(inner),
					"disableTipSampling() should remove the TipDatesRandomWalker from the inner MCMC again");
		}, error);
		if (error.get() != null) {
			error.get().printStackTrace();
			fail("tip-date sampling toggle should not throw for a wrapper Runnable: " + error.get());
		}
	}


	private static boolean hasTipDatesRandomWalker(MCMC mcmc) {
		for (Operator op : mcmc.operatorsInput.get()) {
			if (op instanceof TipDatesRandomWalker) {
				return true;
			}
		}
		return false;
	}


	/** m_beastObject has no public setter outside of the full init(...) UI build, which
	 *  needs a loaded BeautiConfig template; reach around it directly instead. */
	private static void setBeastObject(InputEditor.Base editor, BEASTInterface beastObject) throws Exception {
		Field field = InputEditor.Base.class.getDeclaredField("m_beastObject");
		field.setAccessible(true);
		field.set(editor, beastObject);
	}

	private static void invokePrivate(Class<?> clazz, String name, Object target) throws Exception {
		Method method = clazz.getDeclaredMethod(name);
		method.setAccessible(true);
		try {
			method.invoke(target);
		} catch (java.lang.reflect.InvocationTargetException e) {
			throw (e.getCause() instanceof Exception) ? (Exception) e.getCause() : e;
		}
	}

	private static void invokeStaticPrivate(Class<?> clazz, String name, Class<?>[] paramTypes, Object... args) throws Exception {
		Method method = clazz.getDeclaredMethod(name, paramTypes);
		method.setAccessible(true);
		try {
			method.invoke(null, args);
		} catch (java.lang.reflect.InvocationTargetException e) {
			throw (e.getCause() instanceof Exception) ? (Exception) e.getCause() : e;
		}
	}


	/** run task on the JavaFX application thread, recording any exception in error **/
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
