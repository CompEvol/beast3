/*
* File BeastMCMC.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beastfx.app.beast;



import beast.base.core.BEASTVersion2;
import beast.base.core.Log;
import beast.base.core.ProgramStatus;
import beast.base.inference.Logger;
import beast.base.inference.Runnable;
import beast.base.parser.JSONParser;
import beast.base.parser.JSONParserException;
import beast.base.parser.XMLParser;
import beast.base.parser.XMLParserException;
import beast.base.util.FileUtils;
import beast.base.util.Randomizer;
import beast.pkgmgmt.PackageManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Main application for performing MCMC runs.
 * See getUsage() for command line options.
 */
public class BeastMCMC {
    final public static String VERSION = BEASTVersion2.INSTANCE.getVersionString();
    final public static String DEVELOPERS = "Beast 2 development team";
    final public static String COPYRIGHT = "Beast 2 development team " + BEASTVersion2.INSTANCE.getDateString();

    /**
     * random number seed used to initialise Randomizer *
     */
    long m_nSeed = 127;

    /**
     * MCMC object to execute *
     */
    Runnable m_runnable;

    /**
     * parse command line arguments, and load file if specified
     * @throws IOException 
     * @throws JSONException 
     * @throws JSONParserException 
     */
    public void parseArgs(String[] args) throws IOException, XMLParserException, JSONException {
        int i = 0;
        boolean resume = false;
        boolean useStrictVersions = false;
        boolean sampleFromPrior = false;
        boolean hasDF = false;
        String outFile = null;
        Map<String, String> parserDefinitions = new HashMap<>();

        File beastFile = null;

        try {
            while (i < args.length) {
                int old = i;
                if (i < args.length) {
                    if (args[i].equals("")) {
                        i += 1;
                    } else if (args[i].equals("-batch")) {
                        Logger.FILE_MODE = Logger.LogFileMode.only_new_or_exit;
                        i += 1;
                    } else if (args[i].equals("-resume")) {
                        resume = true;
                        Logger.FILE_MODE = Logger.LogFileMode.resume;
                        System.setProperty("beast.resume", "true");
                        System.setProperty("beast.debug", "false");
                        i += 1;
                    } else if (args[i].equals("-overwrite")) {
                        Logger.FILE_MODE = Logger.LogFileMode.overwrite;
                        i += 1;
                    } else if (args[i].equals("-seed")) {
                        if (args[i + 1].equals("random")) {
                            m_nSeed = Randomizer.getSeed();
                        } else {
                            m_nSeed = Long.parseLong(args[i + 1]);
                        }
                        i += 2;

                    } else if (args[i].equals("-threads")) {
                        ProgramStatus.m_nThreads = Integer.parseInt(args[i + 1]);
                        ProgramStatus.g_exec = Executors.newFixedThreadPool(ProgramStatus.m_nThreads);
                        i += 2;
                    } else if (args[i].equals("-prefix")) {
                        System.setProperty("file.name.prefix", args[i + 1].trim());
                        i += 2;
                    } else if (args[i].equals("-D")) {
                        String [] strs = args[i + 1].split("=",-1);
                        for (int eqIdx = 0; eqIdx<strs.length-1; eqIdx++) {
                            int lastCommaIdx = strs[eqIdx].lastIndexOf(",");

                            if (lastCommaIdx != -1 && eqIdx == 0)
                                throw new IllegalArgumentException("Argument to -D is not well-formed: expecting comma-separated name=value pairs");

                            String name = strs[eqIdx].substring(lastCommaIdx+1);

                            lastCommaIdx = strs[eqIdx+1].lastIndexOf(",");
                            String value;
                            if (eqIdx+1 == strs.length-1) {
                                value = strs[eqIdx+1];
                            } else {
                                if (lastCommaIdx == -1)
                                    throw new IllegalArgumentException("Argument to -D is not well-formed: expecting comma-separated name=value pairs");

                                value = strs[eqIdx+1].substring(0, lastCommaIdx);
                            }
                            parserDefinitions.put(name, value);
            			}
                        i += 2;
                    } else if (args[i].equals("-DF")) {
                        String argFile = args[i + 1];
                		String jsonString = FileUtils.load(argFile);
                		JSONObject jsonDictionary = new JSONObject(jsonString);
                		for (String key : jsonDictionary.keySet()) {
                			Log.warning("Found definition of " + key + " " + jsonDictionary.getString(key).length());
                            parserDefinitions.put(key, jsonDictionary.getString(key));
                		}
                		hasDF = true;
                        i += 2;     
                    } else if (args[i].equals("-DFout")) {
                    	outFile = args[i + 1];                    	 
                        i += 2;                    
                    } else if (args[i].equals("-strictversions")) {
                    	useStrictVersions = true;
                        i += 1;
                    } else if (args[i].equals("-sampleFromPrior")) {
                    	sampleFromPrior = true;
                        i += 1;
                    }
                    if (i == old) {
                        if (i == args.length - 1) {
                            beastFile = new File(args[i]);
                            i++;
                        } else {
                            throw new IllegalArgumentException("Wrong argument");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Error parsing command line arguments: " + Arrays.toString(args) + "\nArguments ignored\n\n" + getUsage());
        }

        if (hasDF && outFile == null && beastFile != null) {
        	outFile = beastFile.getAbsolutePath();
        	if (outFile.toLowerCase().endsWith(".xml")) {
        		outFile = outFile.substring(0, outFile.length() - 4);
        	}
    		outFile = outFile + ".out.xml";
        }        	
        
        if (beastFile == null) {
            // Not resuming so get starting options...
            return;
        }

        Log.warning.println("File: " + beastFile.getName() + " seed: " + m_nSeed + " threads: " + ProgramStatus.m_nThreads);
        if (resume) {
            Log.info.println("Resuming from file");
        }

        if (useStrictVersions) {
        	// grab "required" attribute from beast spec
            if (beastFile.getPath().toLowerCase().endsWith(".json")) {
            	throw new IllegalArgumentException("The -strictversions flag is not implemented for JSON files yet (only XML files are supported).");
            } else {
                BufferedReader fin = new BufferedReader(new FileReader(beastFile));
                StringBuffer buf = new StringBuffer();
                String str = null;
                int lineCount = 0;
                while (fin.ready() && lineCount < 100) {
                    str = fin.readLine();
                    buf.append(str);
                    buf.append(' ');
                }
                fin.close();
                str = buf.toString();
                int start = str.indexOf("required=");
                if (start < 0) {
                	throw new IllegalArgumentException("Could not find a 'required' attribute in the XML. Add the required attribute, or run without the -strictversions flag");
                }
                char c = str.charAt(start + 9);
                start += 10;
                int end = str.indexOf(c, start);
                String packages = str.substring(start, end);
                PackageManager.loadExternalJars(packages);
            }
        } else {
            PackageManager.loadExternalJars();
        }
        

        // parse xml
        Randomizer.setSeed(m_nSeed);
        if (beastFile.getPath().toLowerCase().endsWith(".json")) {
            m_runnable = new JSONParser(parserDefinitions).parseFile(beastFile, sampleFromPrior);
        } else {        	
        	try {
				m_runnable = new XMLParser(parserDefinitions, outFile, hasDF).parseFile(beastFile, sampleFromPrior);
			} catch (SAXException | ParserConfigurationException e) {
				throw new IllegalArgumentException(e);
			}
        }
        m_runnable.setStateFile(beastFile.getName() + ".state", resume);
    } // parseArgs


    public static String getUsage() {
        return "Usage: BeastMCMC [options] <Beast.xml>\n" +
                "where <Beast.xml> the name of a file specifying a Beast run\n" +
                "and the following options are allowed:\n" +
                "-resume : read state that was stored at the end of the last run from file and append log file\n" +
                "-overwrite : overwrite existing log files (if any). By default, existing files will not be overwritten.\n" +
                "-seed [<int>|random] : sets random number seed (default 127), or picks a random seed\n" +
                "-threads <int> : sets number of threads (default 1)\n" +
                "-prefix <name> : use name as prefix for all log files\n" +
                "-beastlib <path> : Colon separated list of directories. All jar files in the path are loaded. (default 'beastlib')";
    } // getUsage


    public void run() throws Exception {
    	ProgramStatus.g_exec = Executors.newFixedThreadPool(ProgramStatus.m_nThreads);
        m_runnable.run();
        ProgramStatus.g_exec.shutdown();
        ProgramStatus.g_exec.shutdownNow();
    } // run


    public void validate() {
    	try {
			m_runnable.validate();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    }

    public static void main(String[] args) {
        try {
            System.setProperty("beast.debug", "true");
            BeastMCMC app = new BeastMCMC();
            app.parseArgs(args);
            app.run();
        } catch (XMLParserException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(BeastMCMC.getUsage());
        }
        if (System.getProperty("beast.useWindow") == null) {
            // this indicates no window is open
            System.exit(0);
        }
    } // main

} // BeastMCMC
