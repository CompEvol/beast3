package beast.base.type;

import beast.base.type.domain.Domain;

/**
 * Tensor type - ordered value or values.
 * 
 * @param <D> the type of element(s)
 * 
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public interface Tensor<D extends Domain<?>> {

	public enum TensorType {Scalar, Vector, Matrix, Tensor, Simplex, SquareMatrix};
	
    int rank();

    int[] shape();

    Object get(int... idx);
    
    double getDoubleValue(int... idx);

    default long size(){
        long s=1;
        for(int d:shape())
            s*=d;
        return s;
    }

    /**
     * Validate that this instance satisfies the type constraints.
     *
     * @return true if this instance is valid according to its type constraints, false otherwise
     */
    boolean isValid();
}