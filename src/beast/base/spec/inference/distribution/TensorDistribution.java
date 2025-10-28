package beast.base.spec.inference.distribution;


import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Domain;
import beast.base.spec.inference.parameter.BoolScalarParam;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Tensor;
import beast.base.spec.type.Vector;
import org.apache.commons.math3.exception.OutOfRangeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


@Description("The BEAST Distribution over a tensor.")
public abstract class TensorDistribution<D extends Domain<T>, T> extends Distribution {

    final public Input<? extends Tensor<D, T>> tensorInput = new Input<>("tensor",
            "point at which the density is calculated", Validate.REQUIRED);

    protected Tensor<D, T> tensor;

    @Override
    public void initAndValidate() {
        tensor = tensorInput.get();

        if (! tensor.isValid())
            throw new IllegalArgumentException("Tensor is not valid ! " + tensor);

        calculateLogP();
    }

    public double calcLogP(Tensor<D, T> tensor) {
        tensorInput.setValue(tensor, this);
        return calculateLogP();
    }

    @Override
    public double calculateLogP() {
        logP = 0;

        tensor = tensorInput.get();
        switch (tensor) {
            case Scalar<D, T> scalar -> {
                if (!scalar.isValid(scalar.get())) return Double.NEGATIVE_INFINITY;
                final T x = scalar.get();
                logP += logProb(x);
            }
            case Vector<D, T> vector -> {
                if (!vector.isValid())
                    return Double.NEGATIVE_INFINITY;
                for (int i = 0; i < vector.size(); i++) {
                    final T x = vector.get(i);
                    logP += logProb(x);
                }
            }
            default -> throw new IllegalStateException("Unexpected tensor type");
        }
        return logP;
    }

    public abstract double logProb(final T x);

    /**
     * @return  offset of distribution.
     */
    public abstract T getOffset();


    public abstract T[][] sample(int size);

    @Override
    public void sample(State state, Random random) {

        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled
        sampleConditions(state, random);

        // sample distribution parameters
        T[] newx;
        try {
            newx = sample(1)[0];

            switch (tensor) {
                case Scalar<D, T> scalar -> {
                    if (scalar instanceof Bounded b) {
                        while (!b.withinBounds((Comparable) newx[0])) {
                            newx = sample(1)[0];
                        }
                    }
                    if (scalar instanceof RealScalarParam rs)
                        rs.set((double) newx[0]);
                    else if (scalar instanceof IntScalarParam is)
                        is.set((int) newx[0]);
                    else if (scalar instanceof BoolScalarParam bs)
                        bs.set((boolean) newx[0]);

                    throw new RuntimeException("sample is not implemented yet for scalar that is not a RealScalarParam or IntScalarParam");
                }
                case Vector<D, T> vector -> {

                    //TODO

                    throw new UnsupportedOperationException("sample is not implemented yet for vector parameters");
                }
                default -> throw new IllegalStateException("Unexpected tensor type");
            }

        } catch (OutOfRangeException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to sample!");
        }
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<>();
        // dist.getID()
        conditions.add(getID());
        return conditions;
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<>();
        // TODO safe cast ?
        String id = ((BEASTInterface) tensor).getID();
        arguments.add(id);
        return arguments;
    }


}

