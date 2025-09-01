package beast.base.type.domain;

/**
 * Domain defines constraints and validation.
 * The type parameter T is the Java type this domain works with.
 */
public interface Domain<T> {
	
	public enum DomainType {Bool, Int, NonNegativeInt, NonNegativeReal, PositiveInt, PositiveReal, Real, UnitInterval};
	
    /**
     * Check if a value is valid for this domain.
     */
    public boolean isValid(T value);
    
    /**
     * Return double representation of the value
     * of an instance of this domain
     */
//    public double getDoubleValue();
//    public int getIntValue();
//    public boolean getBoolValue();
    
    public T get();
    
    
    static public boolean isInt(double value) {
    	return isReal(value) && (value - (int) value == 0); 
    }

    static public boolean isNonNegativeInt(double value) {
    	return isInt(value) && value >= 0.0; 
    }
    
    static public boolean isPositiveInt(double value) {
    	return isInt(value) && value > 0.0; 
    }
    
    static public boolean isReal(double value) {
    	return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0.0; 
    }

    static public boolean isNonNegativeReal(double value) {
    	return isReal(value) && value >= 0.0; 
    }

    static public boolean isPositiveReal(double value) {
    	return isReal(value) && value > 0.0; 
    }

    static public boolean isUnitInterval(double value) {
    	return isReal(value) && value >= 0.0 && value <= 1.0; 
    }

    static public boolean isBool(double value) {
    	return isInt(value) && (value == 0.0 || value == 1.0); 
    }

}

