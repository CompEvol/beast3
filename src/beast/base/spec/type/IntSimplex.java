package beast.base.spec.type;

import beast.base.spec.domain.NonNegativeInt;

public interface IntSimplex<D extends NonNegativeInt> extends IntVector<D> {

    int expectedSum();

    default double sum() {
        int s = 0;
        for(int i = 0; i < size(); i++) {
            s += get(i);
        }
        return s;
    }

    default boolean isValid() {
        IntVector.super.isValid();

        double s = sum();
        return !(Math.abs(s - expectedSum()) > 1e-10);
    }

}
