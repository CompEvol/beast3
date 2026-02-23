package beast.base.spec.inference.parameter;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Real;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@Description("Parameter consisting of 2 or more RealScalarParam but behaving like a single RealVectorParam")
public class CompoundRealScalarParam<D extends Real> extends RealVectorParam<D> {

//TODO Compound RealScalarParam should be enough, but new Compound could be added if required

    final public Input<List<RealScalarParam<D>>> parameterListInput = new Input<>(
            "parameter", "parameters making up the compound parameter",
            new ArrayList<>(), Validate.REQUIRED);

    // immutable
    List<RealScalarParam<D>> parameters = List.of();

	public void initAndValidate() {

		if (isEstimatedInput.get() != true)
			throw new IllegalArgumentException("estimate input should not be specified");

        // an unmodifiable List
		parameters = List.copyOf(parameterListInput.get());
	}

    @Override
    public D getDomain() {
        // TODO or throw Exception ?
        return parameters.getFirst().getDomain();
    }

    @Override
    public List<Double> getElements() {
        return parameters.stream()
                .map(RealScalarParam::get)
                .toList();
    }

    @Override
    public int size() {
        return parameters.size();
    }

    @Override
	public double get(int i) {
		return parameters.get(i).get();
	}

    public double get() {
        return parameters.getFirst().get();
    }

    public double[] getValues() {
        return parameters.stream()
                .mapToDouble(RealScalarParam::get) // convert to DoubleStream
                .toArray();
    }

    @Override
    public Double getLower() {
        return parameters.getFirst().getLower();
    }

    @Override
    public Double getUpper() {
        return parameters.getFirst().getUpper();
    }

    public Double getLower(int i) {
        return parameters.get(i).getLower();
    }

    public Double getUpper(int i) {
        return parameters.get(i).getUpper();
    }

    public boolean isValid(int i, Double value) {
        return parameters.get(i).isValid(value);
    }

    //*** setters ***

    public void set(final Double value) {
        this.set(0, value);
    }

	public void set(int i, final Double value) {
        parameters.get(i).set(value);
	}


    @Override
    public void assignFromWithoutID(StateNode other) {
        if (other instanceof CompoundRealScalarParam otherCompound) {
            CompoundRealScalarParam other2 = otherCompound;
            int k = 0;
            for (RealScalarParam p : parameters) {
                double v = other2.get(k++);
                RealScalarParam r = new RealScalarParam(v, p.getDomain());
                p.assignFrom(r);
            }
        }
    }

    @Override
    public StateNode getCurrent() {
        return this;
    }


    @Override
    protected void store() {
        // do nothing
    }

    @Override
    public void restore() {
        // do nothing
        hasStartedEditing = false;
    }

    @Override
    protected boolean requiresRecalculation() {
        for (RealScalarParam p : parameters) {
            if (p.somethingIsDirty()) {
                hasStartedEditing = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public int scale(double scale) {
        int sum = 0;
        for (RealScalarParam p : parameters) {
            sum += p.scale(scale);
        }
        return sum;
//        throw new UnsupportedOperationException();
    }

    @Override
    public void setEverythingDirty(boolean isDirty) {
        setSomethingIsDirty(isDirty);
        for (RealScalarParam p : parameters) {
            p.setEverythingDirty(isDirty);
        }
    }

    @Override
    public void init(PrintStream out) {
        for (RealScalarParam p : parameters)
            p.init(out);
    }

    @Override
    public void log(long sample, PrintStream out) {
        for (RealScalarParam p : parameters)
            p.log(sample, out);
    }

    @Override
    public void close(PrintStream out) {
        for (RealScalarParam p : parameters)
            p.close(out);
    }


}
