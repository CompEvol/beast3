package beast.base.spec.domain;

/**
 * A distribution that declares the domain of the values it puts probability on, rather
 * than naming it in its class's type argument, e.g. a wrapper generic in its domain such
 * as {@code OffsetInt<D extends Int>}, whose class only says {@code Int}. Tools such as
 * BEAUti use it to find the domain of such a distribution.
 */
public interface DeclaresDomain {

    /**
     * @return the domain declared for the values this distribution puts probability on,
     * or the widest domain of its kind (e.g. Int or Real) when none is declared
     */
    Domain<?> getParameterDomain();
}
