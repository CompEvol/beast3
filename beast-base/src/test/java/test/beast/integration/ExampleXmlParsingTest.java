package test.beast.integration;


import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.minimal.BeastMain;
import beast.base.parser.XMLParser;
import beast.base.util.Randomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * check whether all example files parse *
 */
// Logger.FILE_MODE and file.name.prefix are JVM-wide globals; serialize all classes
// that mutate them so parallel test runs don't clobber each other's prefix mid-run.
@ResourceLock(value = "beast.logger.globals", mode = ResourceAccessMode.READ_WRITE)
public class ExampleXmlParsingTest  {
    @BeforeEach
    void setUp() { XMLPathUtil.setUpOutputDir(); }

    @Test
    public void test_ThatXmlExamplesParse() {
    	test_ThatXmlExamplesParse(XMLPathUtil.resolveExamplesDir());
    }
    
    public void test_ThatXmlExamplesParse(String dir) {
        try {
            Randomizer.setSeedReproducible(127);
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            System.out.println("Test XML Examples in " + dir);
            File exampleDir = new File(dir);
            assertTrue(exampleDir.exists(), "Example directory does not exist: " + dir);
            String[] exampleFiles = exampleDir.list(new FilenameFilter() {
                @Override
				public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });

            List<String> failedFiles = new ArrayList<String>();
            for (String fileName : exampleFiles) {
                System.out.println("Processing " + fileName);
                XMLParser parser = new XMLParser();
                try {
                    parser.parseFile(new File(dir + "/" + fileName));
                } catch (Exception e) {
                	e.printStackTrace()
                	;
                    System.out.println("ExampleXmlParsing::Failed for " + fileName
                            + ": " + e.getMessage());
                    failedFiles.add(fileName);
                }
                System.out.println("Done " + fileName);
            }
            if (failedFiles.size() > 0) {
                System.out.println("\ntest_ThatXmlExamplesParse::Failed for : " + failedFiles.toString());
            } else {
                System.out.println("\ntest_ThatXmlExamplesParse::Success");
            }
            assertTrue(failedFiles.size() == 0, failedFiles.toString());
        } catch (Exception e) {
            System.out.println("exception thrown ");
            System.out.println(e.getMessage());
        }
    } // test_XmlExamples

    @Test
    public void test_ThatXmlExamplesRun() {
        test_ThatXmlExamplesRun(XMLPathUtil.resolveExamplesDir());
    }
    
    public void test_ThatXmlExamplesRun(String dir) {
        try {
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            System.out.println("Test that XML Examples run in " + dir);
            File exampleDir = new File(dir);
            assertTrue(exampleDir.exists(), "Example directory does not exist: " + dir);
            String[] exampleFiles = exampleDir.list(new FilenameFilter() {
                @Override
				public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });

            List<String> failedFiles = new ArrayList<String>();
            int seed = 127;
            for (String fileName : exampleFiles) {
                Randomizer.setSeedReproducible(seed);
                seed += 10; // need more than one to prevent trouble with multiMCMC logs
                System.out.println("Processing " + fileName);
                XMLParser parser = new XMLParser();
                try {
                    beast.base.inference.Runnable runable = parser.parseFile(new File(dir + "/" + fileName));
                    if (runable instanceof MCMC) {
                        MCMC mcmc = (MCMC) runable;
                        mcmc.setInputValue("preBurnin", 0);
                        mcmc.setInputValue("chainLength", 1000l);
                        mcmc.run();
                    }
                } catch (Exception e) {
                    System.out.println("ExampleXmlParsing::Failed for " + fileName
                            + ": " + e.getMessage());
                    failedFiles.add(fileName);
                }
                System.out.println("Done " + fileName);
            }
            if (failedFiles.size() > 0) {
                System.out.println("\ntest_ThatXmlExamplesRun::Failed for : " + failedFiles.toString());
            } else {
                System.out.println("SUCCESS!!!");
            }
            assertTrue(failedFiles.size() == 0, failedFiles.toString());
        } catch (Exception e) {
            System.out.println("exception thrown ");
            System.out.println(e.getMessage());
            ;
        }
    } // test_ThatXmlExamplesRun

    /**
     * The BEAST 2 examples in {@code legacy/} are not converted: they must still parse in BEAST 2.8
     * (BEAST 3), which keeps the old, partly deprecated, classes for them. Parses every XML and JSON
     * file directly in {@code legacy/}. Subfolders are not checked: some hold BEAST 1 XML
     * ({@code benchmark/1}), files that need a removed class ({@code benchmark/II}) or command-line
     * arguments ({@code parameterised}). The legacy examples that need beast-fx classes are in
     * {@code beast.fx/examples/legacy}, checked by beast-fx's {@code ExampleXmlParsingTest}.
     */
    @Test
    public void test_ThatLegacyXmlExamplesParse() {
        File dir = new File(XMLPathUtil.resolveExamplesDir(), "legacy");
        assertTrue(dir.isDirectory(), "Legacy example directory does not exist: " + dir);
        String[] files = dir.list((d, name) -> name.endsWith(".xml") || name.endsWith(".json"));
        assertTrue(files != null && files.length > 0, "No legacy examples found in " + dir);
        java.util.Arrays.sort(files);

        Logger.FILE_MODE = Logger.LogFileMode.overwrite;
        List<String> failedFiles = new ArrayList<>();
        for (String fileName : files) {
            Randomizer.setSeedReproducible(127);
            File file = new File(dir, fileName);
            System.out.println("Processing legacy " + fileName);
            try {
                if (fileName.endsWith(".json")) {
                    new beast.base.parser.JSONParser().parseFile(file);
                } else {
                    new XMLParser().parseFile(file);
                }
            } catch (Exception e) {
                System.out.println("ExampleXmlParsing::Failed for legacy " + fileName + ": " + e.getMessage());
                failedFiles.add(fileName);
            }
        }
        assertTrue(failedFiles.isEmpty(), "Legacy BEAST 2 examples that no longer parse: " + failedFiles);
    } // test_ThatLegacyXmlExamplesParse

    
    protected static class ExitException extends SecurityException 
    {
        public final int status;
        public ExitException(int status) 
        {
            super("There is no escape!");
            this.status = status;
        }
    }
    
    // Suppress warning for removal of SecurityManager
    // there does not seem to be a viable alternative
    // for blocking System.exit() calls yet
    @SuppressWarnings({ "removal", "deprecation" })
    @DisabledForJreRange(min = JRE.JAVA_18, disabledReason = "SecurityManager removed in Java 18+")
	@Test
    public void test_ThatParameterisedXmlExamplesRuns() throws IOException {
        String dir = XMLPathUtil.resolveExamplesDir() + "/parameterised";
        Logger.FILE_MODE = Logger.LogFileMode.overwrite;
        System.out.println("Test that parameterised XML example runs in " + dir + "/RSV2.xml");
        Randomizer.setSeedReproducible(127);
        
        // prevent System.exit() having an effect
		final SecurityManager securityManager = new SecurityManager() {
            @Override
            public void checkPermission( Permission permission ) {
              if( "exitVM".equals( permission.getName() ) ) {
                // throw new RuntimeException("Exit called") ;
            	  System.err.println("Exit called");
              }
            }
            @Override
            public void checkExit(int status) 
            {
            	throw new ExitException(status);
            }
        };
		SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager( securityManager ) ;
          
        try {
        BeastMain.main(new String[]{
        		"-D", "chainLength=1000",
        		"-DF", dir + "/RSV2.json",
        		"-DFout", "/tmp/RSV2.out.xml",
        		dir + "/RSV2.xml"});
        } catch (ExitException e) {
        	if (e.status != 0) {
        		e.printStackTrace();
        		throw new RuntimeException("Exitted with status = " + e.status);
        	}
        }
        
        // reinstate System.exit() behaviour
        System.setSecurityManager(sm) ;

        if (!new File("/tmp/RSV2.out.xml").exists()) {
    		throw new RuntimeException("Could not find file /tmp/RSV2.out.xml");
        }
                
    } // test_ThatParameterisedXmlExamplesRuns
  
    
    

    public static void main(String args[]) {
    	// see ExampleJSONParsingTest.main for comments
        // org.junit.runner.JUnitCore.main("test.beast.integration.ExampleXmlParsingTest");
    }


} // ExampleXmlParsingTest
