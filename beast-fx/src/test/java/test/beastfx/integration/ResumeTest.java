package test.beastfx.integration;

import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.parser.XMLParser;
import beast.base.util.Randomizer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

/**
 * check that a chain can be resumed after termination *
 */
public class ResumeTest  {

    final static String XML_FILE = "testHKY.xml";
    // Classpath path to the example, copied from beast-base test resources
    // by Maven's generate-test-resources phase (see beast-fx/pom.xml).
    final static String XML_CLASSPATH = "beast.base/examples/" + XML_FILE;

	{
		ExampleXmlParsingTest.setUpTestDir();
	}


    @Test
    public void test_ThatXmlExampleResumes() throws Exception {
        Randomizer.setSeed(127);
        Logger.FILE_MODE = Logger.LogFileMode.overwrite;
        // Resolve via the test classpath so this works on any machine or CI runner
        // without relying on user.dir or navigating the source tree.
        URL resource = ResumeTest.class.getClassLoader().getResource(XML_CLASSPATH);
        if (resource == null)
            throw new RuntimeException(XML_CLASSPATH + " not found on test classpath. " +
                    "Run 'mvn generate-test-resources -pl beast-fx' to copy example files.");
        String fileName = new File(resource.toURI()).getAbsolutePath();

        System.out.println("Processing " + fileName);
        XMLParser parser = new XMLParser();
        beast.base.inference.Runnable runable = parser.parseFile(new File(fileName));
        runable.setStateFile("tmp.state", false);
        if (runable instanceof MCMC) {
            MCMC mcmc = (MCMC) runable;
            mcmc.setInputValue("preBurnin", 0);
            mcmc.setInputValue("chainLength", 1000l);
            mcmc.run();
        }
        System.out.println("Done " + fileName);

        System.out.println("Resuming " + fileName);
        Logger.FILE_MODE = Logger.LogFileMode.resume;
        parser = new XMLParser();
        runable = parser.parseFile(new File(fileName));
        runable.setStateFile("tmp.state", true);
        if (runable instanceof MCMC) {
            MCMC mcmc = (MCMC) runable;
            mcmc.setInputValue("preBurnin", 0);
            mcmc.setInputValue("chainLength", 1000l);
            mcmc.run();
        }
        System.out.println("Done " + fileName);
    } // test_ThatXmlExampleResumes

} // class ResumeTest
