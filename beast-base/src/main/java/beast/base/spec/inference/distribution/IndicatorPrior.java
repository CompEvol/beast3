package beast.base.spec.inference.distribution;

/**
 * A prior on a vector of indicators (booleans) that is the whole prior on them: it gives
 * every configuration of the indicators its probability, e.g. {@link CountPrior}.
 * <p>
 * Operators on such indicators need no Hastings term of their own to make every placement
 * of the same number of indicators equally probable, as {@code BitFlipOperator} with
 * {@code uniform="true"} adds for a prior on the number of indicators only (a count
 * distribution on {@code IntSum(indicators)}). With an indicator prior that term would be
 * applied twice (#231), so {@code BitFlipOperator} defaults to {@code uniform="false"} for
 * indicators that have one.
 */
public interface IndicatorPrior {
}
