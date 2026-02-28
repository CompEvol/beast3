package beast.base.spec.inference.parameter;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Int;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A compound parameter that aggregates multiple {@link IntScalarParam} instances
 * and exposes them as a single {@link IntVectorParam}.
 * Each element delegates to its underlying scalar parameter for storage and state management.
 *
 * @param <D> the integer domain type shared by all constituent scalars
 */
@Description("Parameter consisting of 2 or more IntScalarParam but behaving like a single IntVectorParam")
public class CompoundIntScalarParam<D extends Int> extends IntVectorParam<D> {

//TODO do we still need this ?

    final public Input<List<IntScalarParam<D>>> parameterListInput = new Input<>(
            "parameter", "parameters making up the compound parameter",
            new ArrayList<>(), Validate.REQUIRED);

    // immutable
    List<IntScalarParam<D>> parameters = List.of();

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

    /** {@inheritDoc} */
    @Override
    public List<Integer> getElements() {
        return parameters.stream()
                .map(IntScalarParam::get)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return parameters.size();
    }

    /** {@inheritDoc} */
    @Override
	public int get(int i) {
		return parameters.get(i).get();
	}

    public double get() {
        return parameters.getFirst().get();
    }

    public int[] getValues() {
        return parameters.stream()
                .mapToInt(IntScalarParam::get) // convert to IntStream
                .toArray();
    }

    @Override
    public Integer getLower() {
        return parameters.getFirst().getLower();
    }

    @Override
    public Integer getUpper() {
        return parameters.getFirst().getUpper();
    }

    public Integer getLower(int i) {
        return parameters.get(i).getLower();
    }

    public Integer getUpper(int i) {
        return parameters.get(i).getUpper();
    }

    public boolean isValid(int i, Integer value) {
        return parameters.get(i).isValid(value);
    }

    //*** setters ***

    public void set(final Integer value) {
        this.set(0, value);
    }

	public void set(int i, final Integer value) {
        parameters.get(i).set(value);
	}

    @Override
    public void assignFromWithoutID(StateNode other) {
        if (other instanceof CompoundIntScalarParam otherCompound) {
            CompoundIntScalarParam other2 = otherCompound;
            int k = 0;
            for (IntScalarParam p : parameters) {
                int v = other2.get(k++);
                IntScalarParam r = new IntScalarParam(v, p.getDomain());
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
        for (IntScalarParam p : parameters) {
            if (p.somethingIsDirty()) {
                hasStartedEditing = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public void setEverythingDirty(boolean isDirty) {
        setSomethingIsDirty(isDirty);
        for (IntScalarParam p : parameters) {
            p.setEverythingDirty(isDirty);
        }
    }

    @Override
    public void init(PrintStream out) {
        for (IntScalarParam p : parameters)
            p.init(out);
    }

    @Override
    public void log(long sample, PrintStream out) {
        for (IntScalarParam p : parameters)
            p.log(sample, out);
    }

    @Override
    public void close(PrintStream out) {
        for (IntScalarParam p : parameters)
            p.close(out);
    }

}
