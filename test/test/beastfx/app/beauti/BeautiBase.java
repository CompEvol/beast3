package test.beastfx.app.beauti;


import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Function;
import beast.base.inference.*;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.Parameter;
import beast.base.parser.XMLParser;
import beast.pkgmgmt.PackageManager;
import beastfx.app.beauti.BeautiTabPane;
import beastfx.app.inputeditor.AlignmentListInputEditor;
import beastfx.app.inputeditor.AlignmentListInputEditor.Partition0;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.service.query.NodeQuery;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;



/**
 * Basic test methods for Beauti  
 * 
 */
@ExtendWith(ApplicationExtension.class)
public class BeautiBase extends ApplicationExtension {
	final static String TEMPLATE_DIR = BeautiConfig.TEMPLATE_DIR;
	protected final static String NEXUS_DIR = "../beast2/examples/nexus/";

	// If skipAssertions = true, the BEAUti status will not be checked
	// Can be handy for debugging TestFX unit tests
	final static private boolean skipAssertions = false;

	protected BeautiDoc doc;

	public BeautiBase() {
		// make sure BEAST.base and BEAST.app are installed
		System.setProperty("beast.is.junit.testing", "true");
		try {
			Set<String> packages = listInstalledPackages();
			if (!packages.contains("BEAST.base")) {
				// install minimal BEAST.base package
				installBEASTBase();
			}
			if (!packages.contains("BEAST.app")) {
				// install minimal BEAST.base package
				installBEASTApp();
			}
		} catch (IOException e) {
			e.printStackTrace();
        	System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
        	return;
        }
	}

	private Set<String> listInstalledPackages() {
		String userDir = PackageManager.getPackageUserDir();
		File dirFile = new File(userDir);
		Set<String> packages = new HashSet<>();
		if (dirFile.exists() && dirFile.isDirectory()) {
			for (String dir : dirFile.list()) {
				File versionXML = new File(userDir + "/" + dir + "/version.xml");
				if (!versionXML.exists())
					continue;

				try {
	                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	                Document doc = factory.newDocumentBuilder().parse(versionXML);
	                doc.normalize();
	                // get name and version of package
	                Element packageElement = doc.getDocumentElement();
	                String packageName = packageElement.getAttribute("name");
	                packages.add(packageName);
				} catch (Exception e) {
					e.printStackTrace();
                }
			}
		}

		return packages;
	}

	private void installBEASTBase() throws IOException {
		String dir = PackageManager.getPackageUserDir();
		dir += "/BEAST.base";
		new File(dir).mkdirs();
		Files.copy(Paths.get("../beast2/version.xml"), 
				Paths.get(dir+"/version.xml"),
				StandardCopyOption.REPLACE_EXISTING);
		dir += "/lib";
		new File(dir).mkdirs();
		if (!new File("../beast2/build/dist/BEAST.base.jar").exists()) {
			new File("../beast2/build/dist/").mkdirs();
			createJar("../beast2/build/", dir + "/BEAST.base.jar", "../beast2/build/".length());
		} else {
			Files.copy(Paths.get("../beast2/build/dist/BEAST.base.jar"), 
				Paths.get(dir+"/BEAST.base.jar"),
				StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void installBEASTApp() throws IOException {
		String dir = PackageManager.getPackageUserDir();
		dir += "/BEAST.app";
		new File(dir).mkdirs();
		Files.copy(Paths.get("../BeastFX/version.xml"), 
				Paths.get(dir+"/version.xml"),
				StandardCopyOption.REPLACE_EXISTING);
		dir += "/lib";
		new File(dir).mkdirs();
		if (!new File("../BeastFX/build/dist/BEAST.app.jar").exists()) {
			new File("../BeastFX/build/dist/").mkdirs();
			createJar("../BeastFX/build/", dir + "/BEAST.app.jar", "../BeastFX/build/".length());
		} else {
			Files.copy(Paths.get("../BeastFX/build/dist/BEAST.app.jar"),
				Paths.get(dir+"/BEAST.app.jar"),	
				StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public void createJar(String inputDirectory, String outputFile, int root) throws IOException {
	    Manifest manifest = new Manifest();
	    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
	    JarOutputStream target = new JarOutputStream(new FileOutputStream(outputFile), manifest);
	    add(new File(inputDirectory), target, root);
	    target.close();
	}

	private void add(File source, JarOutputStream target, int root) throws IOException {
	    String name = source.getPath();
	    if (name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip")) {
	    	return;
	    }
	    if (source.isDirectory()) {
	        if (!name.endsWith("/")) {
	            name += "/";
	        }
	        String entryName = name.substring(root);
	        JarEntry entry = new JarEntry(entryName);
	        entry.setTime(source.lastModified());
	        target.putNextEntry(entry);
	        target.closeEntry();
	        for (File nestedFile : source.listFiles()) {
	            add(nestedFile, target, root);
	        }
	    } else {
	        String entryName = name.substring(root);
	        JarEntry entry = new JarEntry(entryName);
	        entry.setTime(source.lastModified());
	        target.putNextEntry(entry);
	        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source))) {
	            byte[] buffer = new byte[1024];
	            while (true) {
	                int count = in.read(buffer);
	                if (count == -1)
	                    break;
	                target.write(buffer, 0, count);
	            }
	            target.closeEntry();
	        }
	    }
	}
	
	
	
	String priorsAsString() {
		CompoundDistribution prior = (CompoundDistribution) doc.pluginmap.get("prior");
		List<Distribution> priors = prior.pDistributions.get();
		return "assertPriorsEqual" + pluginListAsString(priors);
	}
	
	String stateAsString() {
		State state = (State) doc.pluginmap.get("state");
		List<StateNode> stateNodes = state.stateNodeInput.get();
		return "assertStateEquals" + pluginListAsString(stateNodes);
	}

	String operatorsAsString() {
		MCMC mcmc = (MCMC) doc.mcmc.get();
		List<Operator> operators = mcmc.operatorsInput.get();
		return "assertOperatorsEqual" + pluginListAsString(operators);
	}
	
	String traceLogAsString() {
		Logger logger = (Logger) doc.pluginmap.get("tracelog");
		List<BEASTObject> logs = logger.loggersInput.get();
		return "assertTraceLogEqual" + pluginListAsString(logs);
	}

	
	private String pluginListAsString(List<?> list) {
		if (list.size() == 0) {
			return "";
		}
		StringBuffer bf = new StringBuffer();
		for (Object o : list) {
			BEASTObject beastObject = (BEASTObject) o;
			bf.append('"');
			bf.append(beastObject.getID());
			bf.append("\", ");
		}
		String str = bf.toString();
		return "(" + str.substring(0, str.length()-2) + ");";
	}

	void assertPriorsEqual(String... ids) {
		if (skipAssertions) return;
		System.err.println("assertPriorsEqual");
		CompoundDistribution prior = (CompoundDistribution) doc.pluginmap.get("prior");
		List<Distribution> priors = prior.pDistributions.get();
		for (String id : ids) {
			boolean found = false;
			for (BEASTObject node : priors) {
				if (node.getID().equals(id)) {
					found = true;
				}
			}
			assertThat(found).as("Could not find beastObject with ID " + id).isEqualTo(true);
		}
		List<String> extras = new ArrayList<>();
		for (BEASTObject node : priors) {
			boolean found = false;
			for (String id : ids) {
				if (node.getID().equals(id)) {
					found = true;
				}
			}
			if (!found) {
				extras.add(node.getID());
			}
		}
		if (extras.size() != 0) {
			System.err.println("Extra ids found: " + Arrays.toString(extras.toArray(new String[]{})));
		}
		assertThat(ids.length).as("list of beastObjects do not match").isEqualTo(priors.size());;
	}

	private void asserListsEqual(List<?> list, String[] ids) {
		// check all ids are in list
		for (String id : ids) {
			boolean found = false;
			for (Object o: list) {
				BEASTObject node = (BEASTObject) o;
				if (node.getID().equals(id)) {
					found = true;
					break;
				}
			}
			printBeautiState();
			assertThat(found).as("Could not find beastObject with ID " + id).isEqualTo(true);
		}
		// check all items in list have a unique ie
		Set<String> idsInList = new HashSet<String>();
		Set<String> duplicates = new HashSet<String>();
		for (Object o : list) {
			String id = ((BEASTObject) o).getID();
			if (idsInList.contains(id)) {
				duplicates.add(id);
			} else {
				idsInList.add(id);
			}
		}
		assertThat(duplicates.size()).as("Duplicate ids found: " + Arrays.toString(duplicates.toArray())).isEqualTo(0);
		
		if (list.size() != ids.length) {
			// list.size > ids.length, otherwise it would have been picked up above
			List<String> extraIDs = new ArrayList<String>(); 
			for (Object o : list) {
				String id = ((BEASTObject) o).getID();
				boolean found = false;
				for (String id2 : ids) {
					if (id2.equals(id)) {
						found = true;
						break;
					}
				}
				if (!found) {
					extraIDs.add(id);
				}
			}
			assertThat(ids.length).as("list of beastObjects do not match: found extra items " + Arrays.toString(extraIDs.toArray())).isEqualTo(list.size());
		}
	}

	void assertStateEquals(String... ids) {
		if (skipAssertions) return;
		System.err.println("assertStateEquals");
		State state = (State) doc.pluginmap.get("state");
		List<StateNode> stateNodes = state.stateNodeInput.get();
		asserListsEqual(stateNodes, ids);
	}

	void assertOperatorsEqual(String... ids) {
		if (skipAssertions) return;
		System.err.println("assertOperatorsEqual");
		MCMC mcmc = (MCMC) doc.mcmc.get();
		List<Operator> operators = mcmc.operatorsInput.get();
		asserListsEqual(operators, ids);
	}

	void assertTraceLogEqual(String... ids) {
		if (skipAssertions) return;
		System.err.println("assertTraceLogEqual");
		Logger logger = (Logger) doc.pluginmap.get("tracelog");
		List<BEASTObject> logs = logger.loggersInput.get();
		asserListsEqual(logs, ids);
	}

	void assertArrayEquals(Object [] o, String array) {
		String str = array.substring(1, array.length() - 1);
		String [] strs = str.split(", ");
		for (int i = 0; i < o.length && i < strs.length; i++) {
			assertThat(strs[i]).as("expected array value " + strs[i] + " instead of " + o[i].toString()).isEqualTo(o[i].toString());
		}
		assertThat(o.length).as("arrays do not match: different lengths").isEqualTo(strs.length);
	}
	
	void assertParameterCountInPriorIs(int i) {
		// count nr of parameters in Prior objects in prior
		// including those for prior distributions (Normal, etc)
		// useful to make sure they do (or do not) get linked
		Set<Function> parameters = new LinkedHashSet<>();
		CompoundDistribution prior = (CompoundDistribution) doc.pluginmap.get("prior");
		for (Distribution p : prior.pDistributions.get()) {
			if (p instanceof Prior) {
				Prior p2 = (Prior) p;
				parameters.add(p2.m_x.get());
				for (BEASTInterface o : p2.distInput.get().listActiveBEASTObjects()) {
					if (o instanceof Parameter) {
						parameters.add((Parameter<?>) o);
					}
				}
			}
		}
		System.err.println("Number of parameters in prior = " + parameters.size());
		if (parameters.size() != i) {
			printBeautiState();
		}
		if (i >= 0) {
			assertThat(parameters.size()).as("Expected " + i + " parameters in prior").isEqualTo(i);
		}
	}
	
	public void printBeautiState() {
		// Thread.sleep(500);
		// String s = stateAsString();
        doc.scrubAll(true, false);
		// f.selectTab("MCMC");
		System.err.println(stateAsString());
		System.err.println(operatorsAsString());
		System.err.println(priorsAsString());
		System.err.println(traceLogAsString());
	}

	void printTableContents(FxRobot robot) {
		TableView<Partition0> table = robot.lookup(".table-view").queryAs(TableView.class);
		String [][] contents = tableContents(table);
		for (int i = 0; i < contents.length; i++) {
			System.err.print("\"" + Arrays.toString(contents[i]));
			if (i < contents.length - 1) {
				System.err.print("*\" +");
			} else {
				System.err.print("\"");
			}
			System.err.println();
		}
	}
	void checkTableContents(FxRobot robot, String str) {
		TableView<Partition0> table = robot.lookup(".table-view").queryAs(TableView.class);
		String [][] contents = tableContents(table);
		String [] strs = str.split("\\*");
		assertThat(contents.length).as("tables do not match: different #rows").isEqualTo(strs.length);
		for (int i = 0; i < contents.length; i++) {
			assertArrayEquals(contents[i], strs[i]);
		}
	}

	public void warning(String str) {
		System.err.println("\n\n=====================================================\n");
		System.err.println(str);
		System.err.println("\n=====================================================\n\n");
	}
	
	public void makeSureXMLParses() {
		warning("Make sure that XML that BEAUti produces parses");
		File XMLFile = new File(org.assertj.core.util.Files.temporaryFolder() + "/x.xml");
		if (XMLFile.exists()) {
			XMLFile.delete();
		}
		
		saveFile(""+org.assertj.core.util.Files.temporaryFolder(), "x.xml");

//		JFileChooserFixture fileChooser = findFileChooser().using(robot());
//		fileChooser.setCurrentDirectory(org.assertj.core.util.Files.temporaryFolder());
//		fileChooser.selectFile(new File("x.xml")).approve();
		
		XMLParser parser = new XMLParser();
		XMLFile = new File(org.assertj.core.util.Files.temporaryFolder() + "/x.xml");
		try {
			parser.parseFile(XMLFile);
		} catch (Exception e) {
			e.printStackTrace();
			assertThat(0).as("Parser exception: " + e.getMessage()).isEqualTo(1);
		}
	}

	protected void saveFile(String dir, String file) {
//		if (!Utils.isMac()) {
//			beautiFrame.menuItemWithPath("File", "Save As").click();
//			JFileChooserFixture fileChooser = findFileChooser().using(robot());
//			fileChooser.setCurrentDirectory(new File(dir));
//			fileChooser.selectFile(new File(file)).approve();
//		} else {
			File _file = new File(dir + "/" + file);
			warning("Writing to file " + dir + "/" + file);
			//Platform.runLater(()-> {
		        	try {
		        		doc.save(_file);
		        	} catch (Exception e) {
						e.printStackTrace();
					}
		    //});
			
//		}	
	}

	// for handling file open events on Mac
//	FileDialog fileDlg = null;
//	String _dir;
//	File _file;

public void importAlignment(String dir, File ... files) {
//		if (!Utils.isMac()) {
//			beautiFrame.menuItemWithPath("File", "Import Alignment").click();
//			JFileChooserFixture fileChooser = findFileChooser().using(robot());
//			fileChooser.setCurrentDirectory(new File(dir));
//			fileChooser.selectFiles(files).approve();
//			// close down any popup message
//			robot().pressKey(KeyEvent.VK_ESCAPE);
//		} else {
System.err.println("Trying to load " + dir + " " + files[0]);
			String _dir;
			_dir = dir;
			for (File file : files) {
				File _file = new File(dir + "/" + file.getName());
				if (!_file.exists()) {
					throw new IllegalArgumentException("File " + _file.getName() + " does not exist, so cannot be imported");
				}
				Platform.runLater(() -> {
		        	try {
		        		doc.importNexus(_file);
		    			doc.beauti.refreshPanel();
		        	} catch (Exception e) {
						e.printStackTrace();
					}
			    });
			}
//		}			
	}
	
	
	protected String[] tabTitles(BeautiTabPane pane) {
		String [] titles = new String[pane.getTabs().size()];
		int i = 0;
		for (Tab tab : pane.getTabs()) {
			ObservableList<Node> nodes = ((Pane) tab.getGraphic()).getChildren(); 
			Node n = nodes.get(0);
			titles[i++] = ((Label)n).getText();
		}
		return titles;
	}

	
	protected void screenshot(String filename) {
		Platform.runLater(() -> {
			WritableImage snapshot = doc.beauti.snapshot(new SnapshotParameters(), null);
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", new File(filename));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	protected String[][] tableContents(TableView<Partition0> table) {
		String [][] contents = new String[table.getItems().size()][table.getColumns().size()];
		for (int i = 0; i < contents.length; i++) {
			Partition0 partition = table.getItems().get(i);
			for (int j = 0; j < contents[0].length; j++) {
			    TableColumn col = table.getColumns().get(j);
			    Object o = col.getCellObservableValue(partition).getValue();
			    if (o instanceof SimpleStringProperty) {
			    		contents[i][j] = ((SimpleStringProperty) o).get();
			    } else {
			    		contents[i][j] = o.toString();
			    }
			}
		}
		return contents;
	}



	/** robustly select rows in partition table -- don't give up after first attempt **/
	protected void selectPartitions(FxRobot robot, int ... rows) {
		robot.clickOn("Partitions");

		TableView<Partition0> table = robot.lookup(".table-view").queryAs(TableView.class);
		
		for (int attempt = 0; attempt < 5; attempt++) {
			robot.bounds(table); //table.requestFocus();
	        table.getSelectionModel().clearSelection();
	        table.getSelectionModel().selectIndices(rows[0], rows);
	        table.getFocusModel().focus(rows[0]);
			
			if (table.getSelectionModel().getSelectedIndices().size() == rows.length) {
				Parent p = table;
				while (p != null & !(p instanceof AlignmentListInputEditor)) {
					p = p.getParent();
				}
				((AlignmentListInputEditor)p).updateStatus();
				return;
			}
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				
			}
		}
	}
	
	protected void selectTab(FxRobot robot, String tab) {
		tab = tab.replaceAll(" ", "");
		robot.clickOn("#" + tab);
	}
	
	protected int getPartitionCount(FxRobot robot) {
		robot.clickOn("Partitions");

		TableView<Partition0> table = robot.lookup(".table-view").queryAs(TableView.class);
		int rowCount = table.getItems().size();
		return rowCount;
	}

	protected FxRobot clickOnButtonWithText(FxRobot robot, String buttonText) {
		Node node = (Node) robot.lookup(target -> {
			if (target instanceof Button) {
				return buttonText.equals(((Button)target).getText());
			}
		    return false;
		}).queryAs(Node.class);
		robot.clickOn(node);
		return robot;
	}

	protected FxRobot clickOnNodesWithID(FxRobot robot, String id) {
		NodeQuery q = robot.lookup(target -> {
			// if (target.getId() != null) System.err.println(target.getId());
			return id.equals(target.getId()) && target.isVisible();
		});
		Set<Node> nodes = q.queryAll();
		if (nodes.size() > 0) {
			robot.clickOn((Node)(nodes.toArray()[0]));
		}
//		for (Node node : nodes) {
//			robot.clickOn(node);
//		}
		return robot;
	}

    protected void setPartitionTableCell(FxRobot robot, int col, String string) {
		TableView<Partition0> table = robot.lookup(".table-view").queryAs(TableView.class);
		switch (col) {
		case 5:robot.clickOn("#siteModelCell");break;
		case 6:robot.clickOn("#clockModelCell");break;
		case 7:robot.clickOn("#treeModelCell");break;
		}
		robot.eraseText(10);
		robot.write(string + "\n");
		robot.press(KeyCode.ENTER);
	}


    protected void scrollToBottom(FxRobot robot) {
        ScrollPane scrollPane = robot.lookup(".scroll-pane").queryAs(ScrollPane.class);
		robot.interact(()-> {
			scrollPane.setVvalue(scrollPane.getVmax());
		});
	}

	protected void scrollToTop(FxRobot robot) {
        ScrollPane scrollPane = robot.lookup(".scroll-pane").queryAs(ScrollPane.class);
		robot.interact(()-> {
			scrollPane.setVvalue(scrollPane.getVmin());
		});		
	}

    protected void setCheckBox(FxRobot robot, String id) {
		NodeQuery q = robot.lookup(target -> {
			if (target.getId()!=null) System.err.println(target.getId());
			return id.equals(target.getId()) && target.isVisible();
		});
		Set<Node> nodes = q.queryAll();
    	for (Node n : nodes) {
    		CheckBox cb = (CheckBox) n;
    		if (!cb.isSelected()) {
    			robot.clickOn(cb);
    		}
    	}		
	}

	protected void selectFromCombobox(FxRobot robot, String id, String str) {
		NodeQuery q = robot.lookup(target -> {
			if (target.getId()!=null) System.err.println(target.getId());
			if (target instanceof ComboBox) {
				ComboBox cb = (ComboBox)target;
				if (cb.getValue() == null || 
					 cb.getValue().toString().equals(id)) {
					return cb.isVisible();
				}
			}
			return id.equals(target.getId()) && target.isVisible();
		});
		Set<Node> nodes = q.queryAll();

        robot.interact(()-> {
        	for (Node n : nodes) {
        		ComboBox cb = (ComboBox)n;
        		List<Object> items = cb.getItems();
        		for (int i = 0; i < items.size(); i++) {
        			if (items.get(i).toString().equals(str)) {
        				cb.getSelectionModel().select(i);
        			}
        		}
        	}
        });
	}
}