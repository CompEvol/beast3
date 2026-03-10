/**
 * Trace file reading and summary statistics for MCMC log output.
 *
 * <p>This package provides lightweight, standalone utilities for parsing BEAST
 * log files and computing trace-level statistics (mean, ESS, HPD intervals,
 * autocorrelation time, etc.).  The classes were originally located in the
 * test tree ({@code test.beast.beast2vs1.trace}) but are general-purpose
 * tools used by downstream BEAST packages, so they have been promoted to
 * main sources.</p>
 *
 * <h2>Future work</h2>
 * <p>{@code beastfx.app.tools.LogAnalyser} duplicates much of the
 * functionality here (log parsing, ESS, HPD, etc.) but is coupled to the
 * {@code beast-fx} module via a Swing file-chooser fallback.  A future
 * refactor should reconcile the two implementations: extract the pure
 * computation from {@code beastfx.app.tools.LogAnalyser} into this package
 * and have the {@code beast-fx} class delegate to it, so that all
 * trace-analysis logic lives in {@code beast-base} with no GUI
 * dependencies.</p>
 */
package beast.base.trace;
