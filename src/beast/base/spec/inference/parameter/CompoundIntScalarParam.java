package beast.base.spec.inference.parameter;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Int;
import beast.base.spec.type.IntVector;
import org.w3c.dom.Node;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@Description("Parameter consisting of 2 or more IntScalarParam but behaving like a single IntVectorParam")
// partial implementation
public class CompoundIntScalarParam<D extends Int> extends StateNode implements IntVector<D> {

//TODO Compound IntScalarParam should be enough, but new Compound could be added if required

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
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Integer> getElements() {
        return parameters.stream()
                .map(IntScalarParam::get)
                .toList();
    }

    @Override
    public int size() {
        return parameters.size();
    }

    @Override
	public Integer get(int i) {
		return parameters.get(i).get();
	}

    public int[] getValues() {
        return parameters.stream()
                .mapToInt(IntScalarParam::get) // convert to IntStream
                .toArray();
    }

    @Override
    public Integer getLower() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer getUpper() {
        throw new UnsupportedOperationException();
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

//	public void setLower(Integer lower) {
//		for (IntScalarParam p : parameters) {
//			p.setLower(lower);
//		}
//	}
//
//	public void setUpper(Integer upper) {
//		for (IntScalarParam p : parameters) {
//			p.setUpper(upper);
//		}
//	}
//
//	public void setBounds(Integer lower, Integer upper) {
//		for (IntScalarParam p : parameters) {
//			p.setBounds(lower, upper);
//		}
//	}

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
    public int scale(double scale) {
        throw new UnsupportedOperationException();
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


//TODO in dev


    @Override
    public void assignFromFragile(StateNode other) {

    }

    @Override
    public void fromXML(Node node) {

    }

    @Override
    public StateNode copy() {
        return null;
    }

    @Override
    public void assignTo(StateNode other) {

    }

    @Override
    public void assignFrom(StateNode other) {

    }

}
