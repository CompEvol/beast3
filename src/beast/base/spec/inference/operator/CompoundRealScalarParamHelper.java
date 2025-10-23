package beast.base.spec.inference.operator;


import beast.base.core.Description;
import beast.base.spec.domain.Domain;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealVector;

import java.util.List;


@Description("A temporary helper class to solve compound state nodes for operators, " +
        "but it cannot be used as input, before the framework is modified.")
public class CompoundRealScalarParamHelper<D extends Real> implements RealVector<D> {

    final List<RealScalarParam<D>> parameterList;

    public CompoundRealScalarParamHelper(final List<RealScalarParam<D>> parameterList) {
        this.parameterList = parameterList;

        if (parameterList == null || parameterList.isEmpty()) {
            throw new IllegalArgumentException("There is no parameter inputted into CompoundParameter !");
        }

        final Domain<Double> domain = parameterList.getFirst().getDomain();
        for (int i = 1; i < parameterList.size(); i++) {
            RealScalarParam<D> param = parameterList.get(i);
            if (param.getDomain() != domain)
                throw new IllegalArgumentException("Domain inside CompoundParameter does not match !\n" +
                        domain + " != " + param.getDomain() + " at index " + i);
        }
        // sanity check
        for (int i = 0; i < parameterList.size(); i++) {
            for (int j = i + 1; j < parameterList.size(); j++) {
                if (parameterList.get(i) == parameterList.get(j))
                    throw new RuntimeException("Duplicate scalar parameter (" + parameterList.get(j).getID() + ") found ! ");
            }
        }

    }

    public int size() {
        return parameterList.size();
    }

    @Override
    public List<Double> getElements() {
        return parameterList.stream()
                .map(RealScalarParam::get)
                .toList();
    }

    public RealScalarParam<D> getScalarParam(int i) {
        return parameterList.get(i);
    }

    @Override
    // Fast (no boxing)
    public double get(int i) {
        return getScalarParam(i).get();
    }

    @Override
    public D getDomain() {
        return parameterList.getFirst().getDomain();
    }

    public Double getLower(final int i) {
        return getScalarParam(i).getLower();
    }

    public Double getUpper(final int i) {
        return getScalarParam(i).getUpper();
    }

    // Fast (no boxing)
    public void set(final int i, final double value) {
        final RealScalarParam<D> param = getScalarParam(i);
        param.set(value);
    }

//    @Override
//    public boolean isValid(int i, final double value) {
//        return getScalarParam(i).isValid(value);
//    }

    @Override
    public boolean isValid() {
        for (int i = 0; i < size(); i++)
            if ( ! getScalarParam(i).isValid(get(i)))
                return false;
        return true;
    }
}