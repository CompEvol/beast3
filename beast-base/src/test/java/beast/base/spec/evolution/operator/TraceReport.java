package beast.base.spec.evolution.operator;

import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.inference.Logger;
import beast.base.spec.inference.util.ESS;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Tensor;
import beast.base.spec.type.TensorUtils;
import beast.base.spec.type.Vector;
import org.apache.commons.math4.legacy.stat.StatUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static beast.base.spec.type.TensorUtils.toDouble;

/**
 * Modified logger which analyses a sequence of tree states generated
 * by an MCMC run.
 */
public class TraceReport extends Logger {

    public Input<Integer> burninInput = new Input<Integer>("burnin",
            "Number of samples to skip (burn in)", Input.Validate.REQUIRED);

    public Input<Boolean> silentInput = new Input<Boolean>("silent",
            "Don't display final report.", false);

    BEASTObject paramToTrack;

    int m_nEvery = 1;
    int burnin;
    boolean silent = false;

    List<Double> values;
    List<double[]> values2;

    @Override
    public void initAndValidate() {
//            System.out.println("\nSeed : " + Randomizer.getSeed() + "\n");

        List<BEASTObject> loggers = loggersInput.get();
        final int nLoggers = loggers.size();
        if (nLoggers == 0) {
            throw new IllegalArgumentException("Logger with nothing to log specified");
        }

        if (everyInput.get() != null)
            m_nEvery = everyInput.get();

        burnin = burninInput.get();

        if (silentInput.get() != null)
            silent = silentInput.get();

        paramToTrack = loggers.get(0);
        values = new ArrayList<>();
        values2 = new ArrayList<>();
    }

    @Override
    public void init() { }

    @Override
    public void log(long nSample) {

        if ((nSample % m_nEvery > 0) || nSample<burnin)
            return;

        if (paramToTrack instanceof Tensor<?,?> tensor) {
            values.add(TensorUtils.valuesToDoubleArray(tensor)[0]);
        } else
            throw new IllegalArgumentException("Require Tensor, but got " + paramToTrack);

        // add all loggable in logger, flat the vector elements and concat with other scalars
        List<Double> allValues = new ArrayList<>();
        for (BEASTObject beastObject : loggersInput.get()) {
            if (beastObject instanceof Scalar scalar) {
                allValues.add(toDouble(scalar.get()));
            } else if (tensor instanceof Vector vector) {
                for (int i = 0; i < vector.size(); i++) {
                    allValues.add(toDouble(vector.get(i)));
                }
            } else
                throw new UnsupportedOperationException("Only support Vector or Scalar ! But get " + tensor.getClass());
        }
        values2.add(allValues.stream().mapToDouble(Double::doubleValue).toArray());
    }

    @Override
    public void close() {

        if (!silent) {
            System.out.println("\n----- Tree trace analysis -----------------------");
            double[] v = new double[values.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = values.get(i);
            }
            double m = StatUtils.mean(v);
            double s = StatUtils.variance(v);
            double ess = ESS.calcESS(values);
            System.out.println("Mean: " + m + " variance: " + s + " ESS: " + ess);
            System.out.println("-------------------------------------------------");
            System.out.println();

            try {
                PrintStream log = new PrintStream(new File("/tmp/bactrian.log"));
                log.print("Sample\t");
                int n = values2.get(0).length;
                for (int j = 0; j < n; j++) {
                    log.print("param" + (j+1) + "\t");
                }
                log.println();
                for (int i = 0; i < v.length; i++) {
                    log.print(i + "\t");
                    for (int j = 0; j < n; j++) {
                        log.print(values2.get(i)[j] + "\t");
                    }
                    log.println();
                }
                log.close();
                System.out.println("trace log written to /tmp/bactrian.log");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Obtain completed analysis.
     *
     * @return trace.
     */
    public List<Double> getAnalysis() {
        return values;
    }

    public List<double[]> getAnalysis2() {
        return values2;
    }

}
