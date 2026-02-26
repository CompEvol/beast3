package beast.base.inference;

import beast.base.core.Description;

@Description("For StateNodes that can be scaled by a scale/up-down operator")
public interface Scalable {

    /**
     * Scale StateNode with amount scale and
     *
     * @param scale scaling factor
     * @return the number of degrees of freedom used in this operation. This number varies
     *         for the different types of StateNodes. For example, for real
     *         valued n-dimensional parameters, it is n, for a tree it is the
     *         number of internal nodes being scaled.
     * @throws IllegalArgumentException when StateNode become not valid, e.g. has
     *                   values outside bounds or negative branch lengths.
     */
    abstract public int scale(double scale);

    /**
     * only scale the i-th element of the StateNode
     * @param i
     * @param scale
     */
    abstract public void scaleOne(int i, double scale);

	default double scaleAll(double scale) {
		try {
			int d = scale(scale);		
			return d * Math.log(scale);
		} catch (IllegalArgumentException e) {
			return Double.NEGATIVE_INFINITY;
		}
	}
	
}
