package test.beastfx.integration;


import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.parser.JSONParser;
import beast.base.util.Randomizer;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * check whether all example files parse *
 */
public class ExampleJSONParsingTest  {
	{
		ExampleXmlParsingTest.setUpTestDir();
	}
	
    @Test
    public void test_ThatXmlExamplesParse() {
        String dir = System.getProperty("user.dir") + "examples";
        if (!new File(dir).exists()) {
        	dir = System.getProperty("user.dir") + "/../beast2/examples";
        }
    	test_ThatJSONExamplesParse(dir);
    }
    
    public void test_ThatJSONExamplesParse(String dir) {
        try {
            Randomizer.setSeed(127);
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            System.out.println("Test JSON Examples in " + dir);
            File exampleDir = new File(dir);
            String[] exampleFiles = exampleDir.list(new FilenameFilter() {
                @Override
				public boolean accept(File dir, String name) {
                    return name.endsWith(".json");
                }
            });

            List<String> failedFiles = new ArrayList<String>();
            for (String fileName : exampleFiles) {
                System.out.println("Processing " + fileName);
                JSONParser parser = new JSONParser();
                try {
                    parser.parseFile(new File(dir + "/" + fileName));
                } catch (Exception e) {
                	e.printStackTrace()
                	;
                    System.out.println("ExampleJSONParsing::Failed for " + fileName
                            + ": " + e.getMessage());
                    failedFiles.add(fileName);
                }
                System.out.println("Done " + fileName);
            }
            if (failedFiles.size() > 0) {
                System.out.println("\ntest_ThatJSONExamplesParse::Failed for : " + failedFiles.toString());
            } else {
                System.out.println("\ntest_ThatJSONExamplesParse::Success");
            }
            assertTrue(failedFiles.size() == 0, failedFiles.toString());
        } catch (Exception e) {
            System.out.println("exception thrown ");
            System.out.println(e.getMessage());
        }
    } // test_JSONExamples

    @Test
    public void test_ThatJSONExamplesRun() {
        String dir = System.getProperty("user.dir") + "/examples";
        test_ThatJSONExamplesRun(dir);
    }
    
    public void test_ThatJSONExamplesRun(String dir) {
        try {
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            System.out.println("Test that JSON Examples run in " + dir);
            File exampleDir = new File(dir);
            String[] exampleFiles = exampleDir.list(new FilenameFilter() {
                @Override
				public boolean accept(File dir, String name) {
                    return name.endsWith(".json");
                }
            });

            List<String> failedFiles = new ArrayList<String>();
            int seed = 127;
            for (String fileName : exampleFiles) {
                Randomizer.setSeed(seed);
                seed += 10; // need more than one to prevent trouble with multiMCMC logs
                System.out.println("Processing " + fileName);
                JSONParser parser = new JSONParser();
                try {
                    beast.base.inference.Runnable runable = parser.parseFile(new File(dir + "/" + fileName));
                    if (runable instanceof MCMC) {
                        MCMC mcmc = (MCMC) runable;
                        mcmc.setInputValue("preBurnin", 0);
                        mcmc.setInputValue("chainLength", 1000l);
                        mcmc.run();
                    }
                } catch (Exception e) {
                    System.out.println("ExampleJSONParsing::Failed for " + fileName
                            + ": " + e.getMessage());
                    failedFiles.add(fileName);
                }
                System.out.println("Done " + fileName);
            }
            if (failedFiles.size() > 0) {
                System.out.println("\ntest_ThatJSONExamplesRun::Failed for : " + failedFiles.toString());
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


    public static void main(String args[]) {
    	// junit5 equivalent
    	//JUnitCore junit = new JUnitCore();
    	//junit.addListener(new TextListener(System.out));
    	//junit.run(test.beast.integration.ExampleJSONParsingTest.class);
    	
    	// junit4 deprecated
        // org.junit.runner.JUnitCore.main("test.beast.integration.ExampleJSONParsingTest");
    }

} // ExampleJSONParsingTest
