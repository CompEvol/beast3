package test.beast.evolution.substmodel;

import org.junit.jupiter.api.Test;

import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.evolution.substitutionmodel.BinaryCovarion;
import beast.base.spec.evolution.substitutionmodel.Frequencies;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
import beast.base.spec.type.RealVector;
import beast.base.spec.type.Simplex;
import beast.base.util.Randomizer;
import cern.colt.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinaryCovarionModelTest  {
    final static double EPSILON = 1e-6;
	
    /**      
	 * test that equilibrium frequencies are
	 * [ p0 * f0, p1, * f0, p0 * f1, p1, * f1 ]
	 */
	@Test
	public void testEquilibriumFrequencies() {
		Randomizer.setSeed(127);
		doWithTufflySteel();

		for (int i = 0; i < 10; i++) {
			doWithEqualHFreqs("BEAST");
			doWithEqualHFreqs("REVERSIBLE");
		}
		doWithUnEqualHFreqs("REVERSIBLE");
	}
	
	private void doWithTufflySteel() {
	        Frequencies dummyFreqs = new Frequencies();
	        Simplex frequencies = new SimplexParam(new double[]{0.25,0.25,0.25,0.25});
	        dummyFreqs.initByName("frequencies", frequencies, "estimate", false);
	        BinaryCovarion substModel;

	        double d = 0.05+Randomizer.nextDouble()*0.9;
	        Simplex vfrequencies = new SimplexParam(new double[]{d, 1.0 - d});
	        RealVector<UnitInterval> switchrate = new RealVectorParam<>(new double[]{Randomizer.nextDouble(), Randomizer.nextDouble()}, UnitInterval.INSTANCE);
	        
	        substModel = new BinaryCovarion();
	        substModel.initByName("frequencies", dummyFreqs, 
	        		"vfrequencies", vfrequencies, /* [p0, p1] */
	        		"alpha", new RealScalarParam<PositiveReal>(0.01, PositiveReal.INSTANCE),
	        		"switchRate", switchrate,
	        		//"eigenSystem", "beast.evolution.substitutionmodel.RobustEigenSystem",
	        		"mode", "TUFFLEYSTEEL");
	        
	        double [] matrix = new double[16];
	        substModel.getTransitionProbabilities(null, 1000, 0, 1.0, matrix);
        	double h0 = switchrate.get(1) / (switchrate.get(0) + switchrate.get(1));
        	double h1 = switchrate.get(0) / (switchrate.get(0) + switchrate.get(1));
	        double [] baseFreqs = new double[] {
	    	        vfrequencies.get(0) * h0,
	    	        vfrequencies.get(1) * h0,
	    	        vfrequencies.get(0) * h1,
	    	        vfrequencies.get(1) * h1
	        };
	        System.err.println("Expected: " + Arrays.toString(baseFreqs));
	        System.err.println("Calculat: " + Arrays.toString(matrix));
	        for (int j = 0; j < 4; j++) {
	        	assertEquals(baseFreqs[j], matrix[j], 1e-3);
	        }
		}

	private void doWithEqualHFreqs(String mode) {
        Frequencies dummyFreqs = new Frequencies();
        Simplex frequencies = new SimplexParam(new double[]{0.25,0.25,0.25,0.25});
        dummyFreqs.initByName("frequencies", frequencies, "estimate", false);
        BinaryCovarion substModel;

        Simplex hfrequencies = new SimplexParam(new double[]{0.5, 0.5});
        double d = Randomizer.nextDouble();
        Simplex vfrequencies = new SimplexParam(new double[]{d, 1.0 - d});
        
        substModel = new BinaryCovarion();
        substModel.initByName("frequencies", dummyFreqs, 
        		"hfrequencies", hfrequencies, /* [f0, f1] */
        		"vfrequencies", vfrequencies, /* [p0, p1] */
        		"alpha", new RealScalarParam<PositiveReal>(0.01, PositiveReal.INSTANCE),
        		"switchRate", new RealVectorParam<PositiveReal>(new double[] {0.1}, PositiveReal.INSTANCE) {},
        		"mode", mode);
        
        double [] matrix = new double[16];
        substModel.getTransitionProbabilities(null, 100, 0, 1.0, matrix);
        double EPSILON = 1e-10;
        assertEquals(vfrequencies.get(0) * hfrequencies.get(0), matrix[0], EPSILON);
        assertEquals(vfrequencies.get(1) * hfrequencies.get(0), matrix[1], EPSILON);
        assertEquals(vfrequencies.get(0) * hfrequencies.get(1), matrix[2], EPSILON);
        assertEquals(vfrequencies.get(1) * hfrequencies.get(1), matrix[3], EPSILON);
	}

	private void doWithUnEqualHFreqs(String mode) {
        Frequencies dummyFreqs = new Frequencies();
        Simplex frequencies = new SimplexParam(new double[]{0.25,0.25,0.25,0.25});
        dummyFreqs.initByName("frequencies", frequencies, "estimate", false);
        BinaryCovarion substModel;

        double d = 0.05 + Randomizer.nextDouble()*0.9;
        Simplex hfrequencies = new SimplexParam(new double[]{d, 1.0 - d});
        d = 0.05 + Randomizer.nextDouble()*0.9;
        Simplex vfrequencies = new SimplexParam(new double[]{d, 1.0 - d});
        
        substModel = new BinaryCovarion();
        substModel.initByName("frequencies", dummyFreqs, 
        		"hfrequencies", hfrequencies, /* [f0, f1] */
        		"vfrequencies", vfrequencies, /* [p0, p1] */
        		"alpha", new RealScalarParam<PositiveReal>(0.01, PositiveReal.INSTANCE),
        		"switchRate", new RealVectorParam<PositiveReal>(new double[] {0.1}, PositiveReal.INSTANCE) {},
        		"eigenSystem", "beast.base.evolution.substitutionmodel.ColtEigenSystem",
        		"mode", mode);
        
        double [] matrix = new double[16];
        substModel.getTransitionProbabilities(null, 1000, 0, 1.0, matrix);
        double [] baseFreqs = new double[] {
	        (vfrequencies.get(0) * hfrequencies.get(0)),
	        (vfrequencies.get(1) * hfrequencies.get(0)),
	        (vfrequencies.get(0) * hfrequencies.get(1)),
	        (vfrequencies.get(1) * hfrequencies.get(1))
        };
        System.err.println("Expected: " + Arrays.toString(baseFreqs));
        System.err.println("Calculat: " + Arrays.toString(matrix));
        for (int j = 0; j < 4; j++) {
        	assertEquals(baseFreqs[j], matrix[j], 1e-3);
        }
	}
}
