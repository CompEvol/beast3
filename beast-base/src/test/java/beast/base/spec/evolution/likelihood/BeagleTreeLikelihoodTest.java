package beast.base.spec.evolution.likelihood;


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
