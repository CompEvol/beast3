package phylospec.base.types;

import org.phylospec.primitives.Primitive;
import org.phylospec.primitives.Real;
import org.phylospec.types.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RealVectorImpl<P extends Real> implements Vector<P> {

    private final List<Double> elements;

    /**
     * Constructs a Vector from a list of elements.
     *
     * @param elements the elements for this vector
     * @throws IllegalArgumentException if elements is null or contains invalid elements
     */
    public RealVectorImpl(List<Double> elements) {
        if (elements == null) {
            throw new IllegalArgumentException("Vector elements cannot be null");
        }
        this.elements = new ArrayList<>(elements);
        if (!isValid()) {
            throw new IllegalArgumentException("Vector contains invalid elements");
        }
    }

    /**
     * Constructs a Vector from an array of elements.
     *
     * @param elements the elements for this vector
     * @throws IllegalArgumentException if any element is invalid
     */
    @SafeVarargs
    public RealVectorImpl(Double... elements) {
        this(Arrays.asList(elements));
    }

    @Override
    public List<Double> getElements() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public double get(int i) {
        return elements.get(i);
    }


    @Override
    public Double get(int... idx) {
        if (idx[0] != 1) throw new IllegalArgumentException();
        return elements.get(idx[0]);
    }

    @Override
    public Real primitiveType() {
        return Real.INSTANCE;
    }
}
