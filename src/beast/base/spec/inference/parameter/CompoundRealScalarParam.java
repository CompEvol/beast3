package beast.base.spec.inference.parameter;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealVector;
import org.w3c.dom.Node;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@Description("Parmeter consisting of 2 or more RealScalarParam but behaving like a single RealVectorParam")
// partial implementation
public class CompoundRealScalarParam<D extends Real> extends StateNode implements RealVector<D> {

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
        throw new UnsupportedOperationException();
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
	public Double get(int i) {
		return parameters.get(i).get();
	}

    public double[] getValues() {
        return parameters.stream()
                .mapToDouble(RealScalarParam::get) // convert to DoubleStream
                .toArray();
    }

    @Override
    public Double getLower() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Double getUpper() {
        throw new UnsupportedOperationException();
    }

    //*** setters ***

    public void set(final Double value) {
        this.set(0, value);
    }

	public void set(int i, final Double value) {
        parameters.get(i).set(value);
	}

//	public void setLower(Double lower) {
//		for (RealScalarParam p : parameters) {
//			p.setLower(lower);
//		}
//	}
//
//	public void setUpper(Double upper) {
//		for (RealScalarParam p : parameters) {
//			p.setUpper(upper);
//		}
//	}
//
//	public void setBounds(Double lower, Double upper) {
//		for (RealScalarParam p : parameters) {
//			p.setBounds(lower, upper);
//		}
//	}

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
        throw new UnsupportedOperationException();
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
