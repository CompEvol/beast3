package test.beast.evolution.likelihood;

import beast.base.evolution.likelihood.TreeLikelihood;
/**
 * @deprecated replaced by {@link beast.base.spec.evolution.likelihood.BeagleTreeLikelihoodTest}
 */
@Deprecated
public class BeagleTreeLikelihoodTest extends TreeLikelihoodTest {

	@Override
	protected TreeLikelihood newTreeLikelihood() {
    	System.err.println("==============================================================================================================");
    	System.err.println("==============================================================================================================");
    	System.err.println("== Make sure to have BEAGLE installed and set the -Djava.library.path=/path/to/beagle option for this test. ==");
    	System.err.println("==============================================================================================================");
    	System.err.println("==============================================================================================================");
    	System.setProperty("java.only", "false");
        return new TreeLikelihood();
	}
}
