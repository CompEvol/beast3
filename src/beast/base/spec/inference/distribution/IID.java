package beast.base.spec.inference.distribution;

import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.spec.domain.Domain;
import beast.base.spec.type.Vector;

import java.util.List;
import java.util.Random;

public class IID<D extends Domain<T>, T> extends Distribution {

//    final public Input<List<Scalar>> scalarsInput = new Input<>("scalar",
//            "point at which the iid is calculated", Input.Validate.XOR);
    final public Input<Vector<D, T>> vectorInput = new Input<>("vector",
            "point at which the iid is calculated", Input.Validate.REQUIRED);

    final public Input<TensorDistribution<D, T>> distInput = new Input<>("distr",
            "tensor distribution used for iid, e.g. normal, beta, gamma.", Input.Validate.REQUIRED);


//    protected List<Scalar> scalarList;
    protected Vector vector;

    protected TensorDistribution dist;

    @Override
    public void initAndValidate() {
        dist = distInput.get();

//        scalarList = scalarsInput.get();
        vector = vectorInput.get();
        // Either or
//        if ( !(scalarList == null || vector == null) )
//            throw new IllegalStateException("Provide either a list of scalars or one vector !");



    }

    @Override
    public void sample(State state, Random random) {
        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled
        sampleConditions(state, random);

        // sample distribution parameters
        Object[] newx = dist.sample(1)[0];

//        scalarList = scalarsInput.get();
//        if (scalarList != null) {
//            for(Scalar scalar : scalarList) {
//
//                if (scalar instanceof Bounded b) {
//                    while (!b.withinBounds((Comparable) newx[0])) {
//                        newx = dist.sample(1)[0];
//                    }
//                }
//                if (scalar instanceof RealScalarParam rs)
//                    rs.set((double) newx[0]);
//                else if (scalar instanceof IntScalarParam is)
//                    is.set((int) newx[0]);
//                else if (scalar instanceof BoolScalarParam bs)
//                    bs.set((boolean) newx[0]);
//
//                throw new RuntimeException("sample is not implemented yet for scalar that is not a RealScalarParam or IntScalarParam");
//            }
//
//        } else {
            vector = vectorInput.get();

//TODO

            throw new UnsupportedOperationException("sample is not implemented yet for vector parameters");

//        }


    }

    @Override
    public List<String> getArguments() {
        return List.of();
    }

    @Override
    public List<String> getConditions() {
        return List.of();
    }

}
