package test.beastfx.integration;



import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import beastfx.app.beast.BeastMain;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.parser.XMLParser;
import beast.base.util.Randomizer;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * check whether all example files parse *
 */
public class ExampleXmlParsingTest  {
	public static void setUpTestDir() {
		// make sure output goes to test directory
		File testDir = 	new File("./test");
		if (!testDir.exists()) {
			testDir.mkdir();
		}
		System.setProperty("file.name.prefix","test/");
	}
	
	{
		setUpTestDir();
	}

    @Test
    public void test_ThatXmlExamplesParse() {
        String dir;
        dir = System.getProperty("user.dir") + "/../beast2/examples/benchmark/II";
    	test_ThatXmlExamplesParse(dir);
        dir = System.getProperty("user.dir") + "/../beast2/examples/";
    	test_ThatXmlExamplesParse(dir);
        dir = System.getProperty("user.dir") + "/../beast2/examples/beast2vs1";
    	test_ThatXmlExamplesParse(dir);
    }
    
    public void test_ThatXmlExamplesParse(String dir) {
        try {
            Randomizer.setSeed(127);
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            System.out.println("Test XML Examples in " + dir);
            File exampleDir = new File(dir);
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
        String dir = System.getProperty("user.dir") + "/examples";
        if (!new File(dir).exists()) {
        	dir = System.getProperty("user.dir") + "/../beast2/examples";
        }
        test_ThatXmlExamplesRun(dir);
    }
    
    public void test_ThatXmlExamplesRun(String dir) {
        try {
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            System.out.println("Test that XML Examples run in " + dir);
            File exampleDir = new File(dir);
            String[] exampleFiles = exampleDir.list(new FilenameFilter() {
                @Override
				public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });

            List<String> failedFiles = new ArrayList<String>();
            int seed = 127;
            for (String fileName : exampleFiles) {
                Randomizer.setSeed(seed);
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
	@Test
    public void test_ThatParameterisedXmlExamplesRuns() throws IOException {
        String dir = System.getProperty("user.dir") + "/examples/parameterised";
        Logger.FILE_MODE = Logger.LogFileMode.overwrite;
        System.out.println("Test that parameterised XML example runs in " + dir + "/RSV2.xml");
        Randomizer.setSeed(127);
        
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
