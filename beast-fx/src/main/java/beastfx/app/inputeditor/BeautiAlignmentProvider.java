package beastfx.app.inputeditor;





import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import beastfx.app.util.Utils;
import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.ProgramStatus;
import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.base.parser.XMLParser;
import beast.pkgmgmt.BEASTClassLoader;


@Description("Class for creating new alignments to be edited by AlignmentListInputEditor")
public class BeautiAlignmentProvider extends BEASTObject {
	/** map extension to importer class names **/
	static List<AlignmentImporter> importers = null;

	private void initImporters() {
		importers = new ArrayList<>();		

        // build up list of data types
        Set<String> importerClasses = Utils.loadService(AlignmentImporter.class);        
        for (String _class: importerClasses) {
        	try {
        		if (!_class.startsWith(this.getClass().getName())) {
					AlignmentImporter importer = (AlignmentImporter) BEASTClassLoader.forName(_class).newInstance();
					importers.add(importer);
        		}
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
	}

	final public Input<BeautiSubTemplate> template = new Input<>("template", "template to be used after creating a new alignment. ", Validate.REQUIRED);
	
	@Override
	public void initAndValidate() {
	}
	
	/** 
	 * return amount to which the provided matches an alignment 
	 * The provider with the highest match will be used to edit the alignment 
	 * */
	public int matches(Alignment alignment) {
		return 1;
	}
	
	/** 
	 * return new alignment, return null if not successful 
	 * **/
	public List<BEASTInterface> getAlignments(BeautiDoc doc) {
		if (importers == null) {
			initImporters();
		}
		Set<String> extensions = new HashSet<>();
		for (AlignmentImporter importer : importers) {
			for (String extension : importer.getFileExtensions()) {
				extensions.add(extension);
			}
		}
        File [] files = FXUtils.getLoadFiles("Load Alignment File",
                new File(ProgramStatus.g_sDir), "Alignment files");//extensions.toArray(new String[]{}));
        if (files != null && files.length > 0) {
        	ProgramStatus.setCurrentDir(files[0].getPath().substring(0,
        			files[0].getPath().lastIndexOf(File.separator)));
            return getAlignments(doc, files);
        }
		return null;
	}

    /**
     * return new alignment given files
     * @param doc
     * @param files
     * @return
     */
    public List<BEASTInterface> getAlignments(BeautiDoc doc, File[] files) {
		if (files == null) {
			// merge "+ button" and "drag drop" function
			return getAlignments(doc);
		}
		if (importers == null) {
			initImporters();
		}
        List<BEASTInterface> selectedBEASTObjects = new ArrayList<>();
        List<MRCAPrior> calibrations = new ArrayList<>();
        for (File file : files) {
			// create list of importers that can handle the file
			List<AlignmentImporter> availableImporters = new ArrayList<>();
			for (AlignmentImporter importer : importers) {
				if (importer.canHandleFile(file)) {
					availableImporters.add(importer);
				}
			}
			if (availableImporters.size() == 0 && file.getPath().toLowerCase().endsWith(".txt")) {
				// remove .txt extension and try again
				String path = file.getPath();
				path = path.substring(0, path.length() - 4);
				File file2 = new File(path);
				for (AlignmentImporter importer : importers) {
					if (importer.canHandleFile(file2)) {
						availableImporters.add(importer);
					}
				}
			}
			
			if (availableImporters.size() > 0) {
				AlignmentImporter importer = availableImporters.get(0);
				if (availableImporters.size() > 1) {
					// let user choose an importer
					List<String> descriptions = new ArrayList<>();
					for (AlignmentImporter i : availableImporters) {
						descriptions.add(i.getDescription());
					}
					String option = (String)Alert.showInputDialog(null, "Which importer is appropriate", "Option",
		                    Alert.WARNING_MESSAGE, null, descriptions.toArray(), descriptions.get(0));
					if (option == null) {
						return selectedBEASTObjects;
					}
					int i = descriptions.indexOf(option);
					importer = availableImporters.get(i);
				}
				
				// get a fresh instance
				//try {
				//	importer = importer.getClass().newInstance();
				//} catch (InstantiationException | IllegalAccessException e) {
				//	// TODO Auto-generated catch block
				//	e.printStackTrace();
				//}
				List<BEASTInterface> list = importer.loadFile(file);
				for (BEASTInterface o : list) {
					if (o.getID() != null && o.getID().contains(":")) {
						o.setID(o.getID().replaceAll(":", "-"));
					}
				}
				selectedBEASTObjects.addAll(list);
			} else {
                Alert.showMessageDialog(null,
                        "Unsupported sequence file.",
                        "Error", Alert.ERROR_MESSAGE);
			}
			
        }
        addAlignments(doc, selectedBEASTObjects);
        if (calibrations != null) {
        	selectedBEASTObjects.addAll(calibrations);
        }
        // doc.addMRCAPriors(calibrations);
        return selectedBEASTObjects;
    }
    
    /** this allows subclasses of BeautiAlignmentProvider to be called with pre-defined arguments
     * for example from a scripting environment (see CompactAnalysis in BEASTLabs). The subclass
     * can choose to suppress GUI components.
     * Typical usage is for importing alignments using a standard template. 
     */
    public List<BEASTInterface> getAlignments(BeautiDoc doc, File[] files, String [] args) {
    	List<BEASTInterface> selectedBEASTObjects = getAlignments(doc, files);
    	return selectedBEASTObjects;
    }
        
    protected void addAlignments(BeautiDoc doc, List<BEASTInterface> selectedBEASTObjects) {
        for (BEASTInterface beastObject : selectedBEASTObjects) {
        	if (beastObject instanceof Alignment) {
	        	// ensure ID of alignment is unique
	        	int k = 0;
	        	String id = beastObject.getID();
        		boolean found = true;
	        	while (doc.pluginmap.containsKey(id) && found) {
	        		found = false;
	        		for (Alignment data : doc.alignments) {
	        			if (data.getID().equals(beastObject.getID())) {
	        				found = true;
	        				break;
	        			}
	        		}
	        		if (found) {
		        		k++;
		        		id = beastObject.getID() + k;	        			
	        		} else {
	        			BEASTInterface oldData = doc.pluginmap.get(beastObject.getID());
	        			replaceItem(doc, oldData, beastObject);
	        		}
	        	}
	        	beastObject.setID(id);
	        	sortByTaxonName(((Alignment) beastObject).sequenceInput.get());
	        	if (getStartTemplate() != null) {
	        		doc.addAlignmentWithSubnet((Alignment) beastObject, getStartTemplate());
	        	}
        	}
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private void replaceItem(BeautiDoc doc, BEASTInterface oldData, BEASTInterface newData) {
        doc.pluginmap.remove(newData.getID());
        Set<BEASTInterface> outputs = new LinkedHashSet<>();
        outputs.addAll(oldData.getOutputs()); 
        for (BEASTInterface o : outputs) {
        	for ( Input i : o.listInputs()) {
        		if (i.get() == oldData) {
        			i.setValue(newData, o);
        		} else if (i.get() != null && i.get() instanceof List) {
        			List list = (List) i.get();
        			int index = list.indexOf(oldData);
        			if (index >= 0) {
        				list.set(index, newData);
        				newData.getOutputs().add(o);
        			}
        		}
        	}
        }		
	}

	/** provide GUI for manipulating the alignment **/
	public void editAlignment(Alignment alignment, BeautiDoc doc) {
		try {
			AlignmentViewer.showInDialog(alignment);
		} catch (Exception e) {
			Alert.showMessageDialog(null, "Something went wrong viewing the alignment: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/** check validity of the alignment, 
	 * return null if there are no problens, 
	 * return message string if something is fishy **/
	String validateAlignment() {
		return null;
	}
	
	/** return template to apply to this new alignment.
	 * By default, the partition template of the current beauti template is returned **/
	protected BeautiSubTemplate getStartTemplate() {
		return template.get();
	}

    static protected void sortByTaxonName(List<Sequence> seqs) {
        Collections.sort(seqs, (Sequence o1, Sequence o2) -> {
                return o1.taxonInput.get().compareTo(o2.taxonInput.get());
            }
        );
    }

	static public BEASTInterface getXMLData(File file) {
		String xml = "";
		try {
			// parse as BEAST 2 xml fragment
			XMLParser parser = new XMLParser();
			BufferedReader fin = new BufferedReader(new FileReader(file));
			while (fin.ready()) {
				xml += fin.readLine() + "\n";
			}
			fin.close();
			BEASTInterface runnable = parser.parseBareFragment(xml, false);
			BEASTInterface alignment = getAlignment(runnable);
            alignment.initAndValidate();
            return alignment;
		} catch (Exception ex) {
			// attempt to parse as BEAST 1 xml
			try {
				String ID = file.getName();
				ID = ID.substring(0, ID.lastIndexOf('.')).replaceAll("\\..*", "");
				BEASTInterface alignment = parseBeast1XML(ID, xml);
				if (alignment != null) {
					alignment.setID(file.getName().substring(0, file.getName().length() - 4).replaceAll("\\..*", ""));
				}
				return alignment;
			} catch (Exception ex2) {
				ex.printStackTrace();
				Alert.showMessageDialog(null, "Loading of " + file.getName() + " failed: " + ex.getMessage()
						+ "\n" + ex2.getMessage());
			}
			return null;
		}
	}
	

	private static BEASTInterface parseBeast1XML(String ID, String xml) throws SAXException, IOException, ParserConfigurationException  {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		doc.normalize();

		NodeList alignments = doc.getElementsByTagName("alignment");
		Alignment alignment = new Alignment();
		alignment.dataTypeInput.setValue("nucleotide", alignment);

		// parse first alignment
		org.w3c.dom.Node node = alignments.item(0);

		String dataTypeName = node.getAttributes().getNamedItem("dataType").getNodeValue();
		int totalCount = 4;
		if (dataTypeName == null) {
			alignment.dataTypeInput.setValue("integer", alignment);
		} else if (dataTypeName.toLowerCase().equals("dna") || dataTypeName.toLowerCase().equals("nucleotide")) {
			alignment.dataTypeInput.setValue("nucleotide", alignment);
			totalCount = 4;
		} else if (dataTypeName.toLowerCase().equals("aminoacid") || dataTypeName.toLowerCase().equals("protein")) {
			alignment.dataTypeInput.setValue("aminoacid", alignment);
			totalCount = 20;
		} else {
			alignment.dataTypeInput.setValue("integer", alignment);
		}

		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			org.w3c.dom.Node child = children.item(i);
			if (child.getNodeName().equals("sequence")) {
				Sequence sequence = new Sequence();
				// find the taxon
				String taxon = "";
				NodeList sequenceChildren = child.getChildNodes();
				for (int j = 0; j < sequenceChildren.getLength(); j++) {
					org.w3c.dom.Node child2 = sequenceChildren.item(j);
					if (child2.getNodeName().equals("taxon")) {
						taxon = child2.getAttributes().getNamedItem("idref").getNodeValue();
					}
				}
				String data = child.getTextContent();
				sequence.initByName("totalcount", totalCount, "taxon", taxon, "value", data);
				sequence.setID("seq_" + taxon);
				alignment.sequenceInput.setValue(sequence, alignment);

			}
		}
		alignment.setID(ID);
		alignment.initAndValidate();
		return alignment;
	} // parseBeast1XML


	static BEASTInterface getAlignment(BEASTInterface beastObject) throws IllegalArgumentException, IllegalAccessException {
		if (beastObject instanceof Alignment) {
			return beastObject;
		}
		for (BEASTInterface beastObject2 : beastObject.listActiveBEASTObjects()) {
			beastObject2 = getAlignment(beastObject2);
			if (beastObject2 != null) {
				return beastObject2;
			}
		}
		return null;
	}




}
