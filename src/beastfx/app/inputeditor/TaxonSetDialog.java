package beastfx.app.inputeditor;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import beastfx.app.beauti.ThemeProvider;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import javax.swing.UIManager;

import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;



public class TaxonSetDialog extends DialogPane {
    public boolean isOK = false;
    public TaxonSet taxonSet;
    String id;
    List<Taxon> _candidates;

    TextField idEntry;

    TextField filterEntry;

    ListView<Taxon> listOfTaxonCandidates;
    ListView<Taxon> listOfTaxonSet;


    VBox box;
    BeautiDoc doc;
    
    public TaxonSetDialog(TaxonSet taxonSet, Set<Taxon> candidates, BeautiDoc doc) {
        // initialize state
        this.taxonSet = taxonSet;
        this.doc = doc;
        id = taxonSet.getID();
        // create components
        box = FXUtils.newVBox();
        box.getChildren().add(createIDBox());
        box.getChildren().add(createFilterBox());
        box.getChildren().add(createTaxonSelector());
        //box.getChildren().add(new Separator());
        //box.add(createCancelOKButtons());

        // initialise lists
        List<Taxon> taxonset = taxonSet.taxonsetInput.get();
        Comparator<Taxon> comparator = (o1, o2) -> o1.getID().compareTo(o2.getID());
        Collections.sort(taxonset, comparator);
        _candidates = new ArrayList<>();
        _candidates.addAll(candidates);
        Collections.sort(_candidates, comparator);

        listOfTaxonCandidates.getItems().addAll(_candidates);
        listOfTaxonCandidates.getItems().removeAll(taxonset);
        listOfTaxonSet.getItems().addAll(taxonset);

        getChildren().add(box);
        int size = UIManager.getFont("Label.font").getSize();
        setPrefSize(400 * size / 13, 600 * size / 13);
        //setModal(true);
    } // c'tor
    
    public boolean showDialog() {
    	Dialog<ButtonType> dialog = new Dialog<>();
    	dialog.setDialogPane(this);
    	dialog.setResizable(true);
    	
    	getButtonTypes().addAll(Alert.OK_CANCEL_OPTION);
    	dialog.setTitle("Taxon set editor");    	

    	ThemeProvider.loadStyleSheet(this.getScene());
    	Optional<ButtonType> result = dialog.showAndWait();
    	
        isOK = result.get() == Alert.OK_OPTION;
        if (isOK) {
            taxonSet.setID(id);
            List<Taxon> taxa = taxonSet.taxonsetInput.get();
            while (taxa.size() > 0) {
                taxa.remove(0);
            }
            for (int i = 0; i < listOfTaxonSet.getItems().size(); i++) {
                taxa.add(listOfTaxonSet.getItems().get(i));
            }
            isOK = true;
        }
        return isOK;
    }

    

    private Pane createFilterBox() {
        HBox box = FXUtils.newHBox();
        Label label = new Label("Filter:");
        box.getChildren().add(label);
        label.setMinSize(100,20);
        label.setPrefSize(100,20);
        filterEntry = new TextField();
        filterEntry.setPrefColumnCount(50);
        filterEntry.setMinSize(100,20);
        filterEntry.setPrefSize(100,20);
        //filterEntry.setSize(size);
        filterEntry.setTooltip(new Tooltip("Enter regular expression to match taxa"));
        //int fontsize = filterEntry.getFont().getSize();
        //filterEntry.setMaxSize(new Dimension(1024 * fontsize / 13, 50 * fontsize / 13));
        box.getChildren().add(filterEntry);
        //box.getChildren().add(new Separator());
        filterEntry.setOnKeyReleased(e -> processEntry());
        return box;
    }

    private void processEntry() {
        String filter = ".*" + filterEntry.getText() + ".*";

        listOfTaxonCandidates.getItems().clear();
        for (Taxon taxon : _candidates) {
            if (taxon.getID().matches(filter)) {
            	listOfTaxonCandidates.getItems().add(taxon);
            }
        }
        for (int i = 0; i < listOfTaxonSet.getItems().size(); i++) {
        	listOfTaxonCandidates.getItems().remove(listOfTaxonSet.getItems().get(i));
        }
    }

    Pane createIDBox() {
        HBox box = FXUtils.newHBox();
        Label label = new Label("Taxon set label:");
        box.getChildren().add(label);        
        label.setMinSize(100,20);
        idEntry = new TextField();
        idEntry.setMinSize(100,20);
        idEntry.setId("idEntry");
        idEntry.setText(id);
        box.getChildren().add(idEntry);
        idEntry.setOnKeyReleased(e->{id = idEntry.getText();});
        return box;
    }
    
    Pane createTaxonSelector() {
        HBox box = FXUtils.newHBox();

        // list of taxa to select from
        listOfTaxonCandidates = new ListView<>();
        listOfTaxonCandidates.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listOfTaxonCandidates.setId("listOfTaxonCandidates");
        listOfTaxonCandidates.setMinSize(200,300);
        listOfTaxonCandidates.setPrefSize(200,300);
        listOfTaxonCandidates.setCellFactory(new Callback<ListView<Taxon>, ListCell<Taxon>>() {
			@Override
			public ListCell<Taxon> call(ListView<Taxon> param) {
				return new ListCell<Taxon>() {
					@Override
					public void updateItem(Taxon taxon, boolean empty) {
						super.updateItem(taxon, empty);
		                if (empty || taxon == null) {
		                    setText(null);
		                } else {
		                    setText(taxon.getID());
		                }
					}
				};
			}
		});

        listOfTaxonSet = new ListView<>();
        listOfTaxonSet.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listOfTaxonSet.setId("listOfTaxonSet");
        listOfTaxonSet.setMinSize(200,300);
        listOfTaxonSet.setPrefSize(200,300);
        listOfTaxonSet.setCellFactory(new Callback<ListView<Taxon>, ListCell<Taxon>>() {
			@Override
			public ListCell<Taxon> call(ListView<Taxon> param) {
				return new ListCell<Taxon>() {
					@Override
					public void updateItem(Taxon taxon, boolean empty) {
						super.updateItem(taxon, empty);
		                if (empty || taxon == null) {
		                    setText(null);
		                } else {
		                    setText(taxon.getID());
		                }
					}
				};
			}
		});

//        ScrollPane scroller = new ScrollPane();
//        scroller.setContent(listOfTaxonCandidates);
//        box.getChildren().add(scroller);
        box.getChildren().add(listOfTaxonCandidates);

        // add buttons to select/deselect taxa
        VBox buttonBox = FXUtils.newVBox();
        //buttonBox.getChildren().add(new Separator());
        Button selectButton = new Button(">>");
        selectButton.setId(">>");
        selectButton.setMinSize(50,30);
        selectButton.setPrefSize(50,30);
        selectButton.setOnAction(e -> {
                List<Integer> selected = listOfTaxonCandidates.getSelectionModel().getSelectedIndices();
                for (int i : selected) {
                    listOfTaxonSet.getItems().add(listOfTaxonCandidates.getItems().get(i));
                }
                for (int i = 0; i < listOfTaxonSet.getItems().size(); i++) {
                	listOfTaxonCandidates.getItems().remove(listOfTaxonSet.getItems().get(i));
                }
            });
        buttonBox.getChildren().add(selectButton);
        Button deselectButton = new Button("<<");
        deselectButton.setMinSize(50,30);
        deselectButton.setPrefSize(50,30);
        deselectButton.setId("<<");
        deselectButton.setOnAction(e -> {
        		List<Integer> selected = listOfTaxonSet.getSelectionModel().getSelectedIndices();
                for (int i : selected) {
                	listOfTaxonCandidates.getItems().add(listOfTaxonSet.getItems().get(i));
                }
                for (int i = 0; i < listOfTaxonCandidates.getItems().size(); i++) {
                	listOfTaxonSet.getItems().remove(listOfTaxonCandidates.getItems().get(i));
                }
            });
        buttonBox.getChildren().add(deselectButton);
        //buttonBox.getChildren().add(new Separator());
        box.getChildren().add(buttonBox);

//        ScrollPane scroller2 = new ScrollPane();
//        scroller2.setContent(listOfTaxonSet);
//        box.getChildren().add(scroller2);
        box.getChildren().add(listOfTaxonSet);
        return box;
    } // createTaxonSelector



} // class TaxonSetDialog
