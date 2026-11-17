package beast.base.spec.inference.distribution;

import beast.base.spec.Bounded;
import beast.base.spec.domain.DeclaresDomain;

/**
 * A distribution that wraps another one and changes its support, as Offset and Bounded
 * (Truncated) do. Tools such as BEAUti use it to tell which inner distributions fit a
 * parameter, without building the wrapper for each candidate: e.g. inside an Offset of 1 on a
 * PositiveInt parameter, Poisson fits, but inside an Offset of 0 it does not.
 */
public interface WrapsDistribution extends DeclaresDomain {

    /** @return the wrapped distribution */
    ScalarDistribution<?, ?> getInnerDistribution();

    /**
     * Where the support of this wrapper would reach outside a domain if {@code inner} took the
     * place of its distribution, with the current offset or truncation bounds.
     *
     * @param inner  a distribution of the same kind (integer or real) as the wrapped one
     * @param domain e.g. the domain of the parameter
     * @return null when the support lies within the domain, otherwise where it does not, e.g.
     *         "below 1" (see {@link beast.base.spec.type.BoundUtils#supportOutsideDomain})
     */
    String supportOutsideDomainWith(ScalarDistribution<?, ?> inner, Bounded<?> domain);
}
