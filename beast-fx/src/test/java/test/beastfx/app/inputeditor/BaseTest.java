package test.beastfx.app.inputeditor;


import beastfx.app.inputeditor.AlignmentViewer;
import beastfx.app.inputeditor.BEASTObjectInputEditor;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.inputeditor.InputEditor.ExpandOption;
import beastfx.app.inputeditor.TaxonSetDialog;
import beastfx.app.util.Alert;

import java.io.File;
import java.util.*;

import beastfx.app.beauti.JPackageDialog;
import beastfx.app.beauti.JPackageRepositoryDialog;
import beastfx.app.beauti.S11InitialSelection;
import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.speciation.YuleModel;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.Distribution;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.ParametricDistribution;

import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class BaseTest extends javafx.application.Application {
	public enum e {value1,value2,value3};
	static private int testNum = 5;

	public class InputClass2 extends BEASTObject {
		final public Input<InputClass> InputClassInput = new Input<>("InputClass", "Input class", new InputClass());
		@Override
		public void initAndValidate() {}
	}
	public class InputClass extends BEASTObject {
		/*		
		final public Input<e> eInput = new Input<>("enum", "integer valued input for test class", e.value2, e.values());
		final public Input<Integer> intInput = new Input<>("int", "integer valued input for test class", 3);
 		final public Input<Boolean> boolInput = new Input<>("bool", "boolean valued input for test class", true);
		final public Input<Double> doubleInput = new Input<>("double", "double valued input for test class", 3.12);
		final public Input<Long> longInput = new Input<>("long", "long valued input for test class", 123L);
		final public Input<String> strInput = new Input<>("string", "string valued input for test class", "string");
		final public Input<File> fileInput = new Input<>("file", "file valued input for test class");
		final public Input<List<File>> filesInput = new Input<>("files", "file valued input for test class", new ArrayList<>());
		final public Input<Tree> treeInput = new Input<>("tree", "tree valued input for test class");
		final public Input<List<Alignment>> alignmentListInput = new Input<>("data", "alignment list valued input for test class", new ArrayList<>());
		final public Input<List<Distribution>> distributionInput = new Input<>("distribution", "distribution valued input for test class", new ArrayList<>());
*/		
		final public Input<ParametricDistribution> paramDistrInput = new Input<>("paramDistr", "parametric distribution valued input for test class");
		
		
		@Override
		public void initAndValidate() {
			//System.out.println("Value of int = " + intInput.get());
		}
		
	}
	
    static public Tree getTree(Alignment data) throws Exception {
        TreeParser tree = new TreeParser();
        tree.initByName("taxa", data,
                "newick", "((((human:0.024003,(chimp:0.010772,bonobo:0.010772):0.013231):0.012035,gorilla:0.036038):0.033087000000000005,orangutan:0.069125):0.030456999999999998,siamang:0.099582);",
                "IsLabelledNewick", true);
        
        //Tree tree2 = new Tree(tree.getRoot());
        tree.setID("Tree.t:tree");
        return tree;
    }

	
	
    static public Alignment getAlignment() throws Exception {
        Sequence human = new Sequence("human", "AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGGAGCTTAAACCCCCTTATTTCTACTAGGACTATGAGAATCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAATACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTG-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGACCAATGGGACTTAAACCCACAAACACTTAGTTAACAGCTAAGCACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCGGAGCTTGGTAAAAAGAGGCCTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGGCCTCCATGACTTTTTCAAAAGGTATTAGAAAAACCATTTCATAACTTTGTCAAAGTTAAATTATAGGCT-AAATCCTATATATCTTA-CACTGTAAAGCTAACTTAGCATTAACCTTTTAAGTTAAAGATTAAGAGAACCAACACCTCTTTACAGTGA");
        Sequence chimp = new Sequence("chimp", "AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTCAAATCCCCTTATTTCTACTAGGACTATAAGAATCGAACTCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTAAGCACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCTCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AACCCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCGACACCTCTTTACAGTGA");
        Sequence bonobo = new Sequence("bonobo", "AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTTAAATCCCCTTATTTCTACTAGGACTATGAGAGTCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAACACAGACCAAGAGCCTTCAAAGCTCTCAGTAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAGC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTTGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCCCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCAACACCTCTTTACAGTGA");
        Sequence gorilla = new Sequence("gorilla", "AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGAGGTTTAAACCCCCTTATTTCTACTAGGACTATGAGAATTGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTGTCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTCACATCCTTCCCGTACTAAGAAATTTAGGTTAAACATAGACCAAGAGCCTTCAAAGCCCTTAGTAAGTTA-CAACACTTAATTTCTGTAAGGACTGCAAAACCCTACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATCAATGGGACTCAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAGTCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAT-TCACCTCGGAGCTTGGTAAAAAGAGGCCCAGCCTCTGTCTTTAGATTTACAGTCCAATGCCTTA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGACCTTCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAGGTTAAATTACGGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCGTTAACCTTTTAAGTTAAAGATTAAGAGTATCGGCACCTCTTTGCAGTGA");
        Sequence orangutan = new Sequence("orangutan", "AGAAATATGTCTGACAAAAGAGTTACTTTGATAGAGTAAAAAATAGAGGTCTAAATCCCCTTATTTCTACTAGGACTATGGGAATTGAACCCACCCCTGAGAATCCAAAATTCTCCGTGCCACCCATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTA--CACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTCA-CAGCACTTAATTTCTGTAAGGACTGCAAAACCCCACTTTGCATCAACTGAGCGCAAATCAGCCACTTTAATTAAGCTAAGCCCTCCTAGACCGATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAGTCAAT-TGGCTTCAGTCCAAAGCCCCGGCAGGCCTTAAAGCTGCTCCTTCGAATTTGCAATTCAACATGACAA-TCACCTCAGGGCTTGGTAAAAAGAGGTCTGACCCCTGTTCTTAGATTTACAGCCTAATGCCTTAACTCGGCCATTTTACCGCAAAAAAGGAAGGAATCGAACCTCCTAAAGCTGGTTTCAAGCCAACCCCATAACCCCCATGACTTTTTCAAAAGGTACTAGAAAAACCATTTCGTAACTTTGTCAAAGTTAAATTACAGGTC-AGACCCTGTGTATCTTA-CATTGCAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGACTAAGAGAACCAGCCTCTCTTTGCAATGA");
        Sequence siamang = new Sequence("siamang", "AGAAATACGTCTGACGAAAGAGTTACTTTGATAGAGTAAATAACAGGGGTTTAAATCCCCTTATTTCTACTAGAACCATAGGAGTCGAACCCATCCTTGAGAATCCAAAACTCTCCGTGCCACCCGTCGCACCCTGTTCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCATACTAAGAAATTTAGGTTAAACACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTAACAAAACTTAATTTCTGCAAGGGCTGCAAAACCCTACTTTGCATCAACCGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATCGATGGGACTTAAACCCATAAAAATTTAGTTAACAGCTAAACACCCTAAACAACCTGGCTTCAATCTAAAGCCCCGGCAGA-GTTGAAGCTGCTTCTTTGAACTTGCAATTCAACGTGAAAAATCACTTCGGAGCTTGGCAAAAAGAGGTTTCACCTCTGTCCTTAGATTTACAGTCTAATGCTTTA-CTCAGCCACTTTACCACAAAAAAGGAAGGAATCGAACCCTCTAAAACCGGTTTCAAGCCAGCCCCATAACCTTTATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATCACAGGTCCAAACCCCGTATATCTTATCACTGTAGAGCTAGACCAGCATTAACCTTTTAAGTTAAAGACTAAGAGAACTACCGCCTCTTTACAGTGA");

        Alignment data = new Alignment();
        data.initByName("sequence", human, "sequence", chimp, "sequence", bonobo, "sequence", gorilla, "sequence", orangutan, "sequence", siamang,
                "dataType", "nucleotide"
        );
        return data;
    }
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		// create BeautiDoc and beauti configuration
		BeautiDoc doc = new BeautiDoc();
		doc.beautiConfig = new BeautiConfig();
		doc.beautiConfig.initAndValidate();
		doc.newAnalysis();
		Alignment data = getAlignment();
		Tree tree = getTree(data);
	    
		switch (testNum) {
		case 0:{
			// autocomplete combobox test
			VBox root = new VBox();
	        // the combo box (add/modify items if you like to)
	        ComboBox<Object> comboBox = new ComboBox<>();
	        comboBox.getItems().addAll(new Object[] {"Ester", "Jordi", "Jordina", "Jorge", "Sergi"});
	        // has to be editable
	        comboBox.setEditable(true);
	        // add autocompletion
	        new S11InitialSelection(comboBox);
	        Dialog dlg = new Dialog();
	        dlg.getDialogPane().setContent(comboBox);
	        dlg.getDialogPane().getButtonTypes().add(Alert.CLOSED_OPTION);
	        dlg.showAndWait();
			}
	        break;
		case 1:
	    	// JPackageDialog test
	    	Dialog dlg1 = JPackageDialog.asDialog(null);
	        dlg1.showAndWait();
			System.exit(0);
		case 2:
	    	// JPackageRepositoryDialog test
			JPackageRepositoryDialog dlg0 = new JPackageRepositoryDialog(null);
			System.exit(0);
		case 3:
	    	// TaxonSetDialog test
			Set<Taxon> candidates = new HashSet<>();
			Taxon t1 = new Taxon("human");
			Taxon t2 = new Taxon("chimp");
			Taxon t3 = new Taxon("bonobo");
			candidates.add(t1);
			candidates.add(t2);
			candidates.add(t3);
			for (int i = 0; i < 50; i++) {
				Taxon t = new Taxon("taxon" + i);
				candidates.add(t);
			}
			TaxonSet taxonSet = new TaxonSet();
			taxonSet.taxonsetInput.get().add(t2);
			
			TaxonSetDialog dlg = new TaxonSetDialog(taxonSet, candidates, doc);
			if (dlg.showDialog()) {
				System.out.println(taxonSet.toString());
			}
			System.out.println(taxonSet.toString());
			break;
			
		case 4:
			// AlignmentViewer test
			AlignmentViewer.showInDialog(data);
			break;
		case 5:
			// basic input editor test
			VBox box = new VBox();
			InputClass2 beastObject = new InputClass2();
			beastObject.setID("inputClass2");
			InputClass oi = beastObject.InputClassInput.get(); 
			oi.setID("inputClass");
			LogNormalDistributionModel distr = new LogNormalDistributionModel();
			distr.setID("LogNormalDistributionModel.0");
			distr.initByName("M","1","S","0.15");
			setInputValue(oi, "paramDistr", distr);
			setInputValue(oi, "tree", tree);
			YuleModel yule = new YuleModel();
			yule.setID("Tree.t:tree");
			yule.initByName("tree", tree, "birthDiffRate", "1.0");
			if (oi.getInputs().containsKey("distribution")) {
				oi.getInput("distribution").setValue(yule, oi);
			}

			
			InputEditor e = new BEASTObjectInputEditor(doc);
			e.init(beastObject.InputClassInput, beastObject, -1, ExpandOption.TRUE, true);
			box.getChildren().add((Pane) e);
			
			for (Input<?> input : beastObject.listInputs()) {
				Object o = doc.getInputEditorFactory().createInputEditor(input, beastObject, doc);
				Pane node = (Pane) o;
				box.getChildren().add(node);
			}
	
			ScrollPane root = new ScrollPane();
	        root.setContent(box);
	 
	        // Set the Style-properties of the VBox
	        root.setStyle("-fx-padding: 10;" +
	                "-fx-border-style: solid inside;" +
	                "-fx-border-width: 2;" +
	                "-fx-border-insets: 5;" +
	                "-fx-border-radius: 5;" +
	                "-fx-border-color: blue;");				    
	        
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.show();
			break;
		case 6:
			
		}


		primaryStage.setOnCloseRequest((event) -> {
		    System.exit(0);
		});
	}       

	private void setInputValue(InputClass oi, String inputName, Object value) {
		if (!oi.getInputs().containsKey(inputName)) {
			return;
		}
		Input<?> input = oi.getInput(inputName);
		input.setValue(value, oi);
	}



	public static void main(String[] args) {
		if (args.length > 0) {
			testNum = Integer.parseInt(args[0]);
		}
		launch();
	    
	}
	
}
