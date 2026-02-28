package beastfx.app.inputeditor;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.FilteredAlignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;



public class TaxonSetInputEditor extends InputEditor.Base {
    List<Taxon> m_taxonset;
    List<Taxon> m_lineageset;
    Map<String, String> m_taxonMap;

    public class TaxonMap {
		String taxon;
    	String taxon2;

    	TaxonMap(String taxon, String taxon2) {
    		this.taxon = taxon;
    		this.taxon2 = taxon2;
    	}
    	
    	public String getTaxon() {
			return taxon;
		}
		public void setTaxon(String taxon) {
			this.taxon = taxon;
		}
		public String getTaxon2() {
			return taxon2;
		}
		public void setTaxon2(String taxon2) {
			this.taxon2 = taxon2;
		}
    }
    
    TableView<TaxonMap> m_table;
    ObservableList<TaxonMap> taxonMapping;
    // DefaultTableModel m_model = new DefaultTableModel();

    TextField filterEntry;
    String m_sFilter = ".*";
    int m_sortByColumn = 0;
    boolean m_bIsAscending = true;

	public TaxonSetInputEditor(BeautiDoc doc) {
		super(doc);
	}

    public TaxonSetInputEditor() {
		super();
	}

	@Override
    public Class<?> type() {
        return TaxonSet.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_input = input;
        m_beastObject = beastObject;
		this.itemNr = itemNr;
		pane = FXUtils.newVBox();
        TaxonSet taxonset = (TaxonSet) m_input.get();
        if (taxonset == null) {
            return;
        }
        // List<Taxon> taxonsets = new ArrayList<>();

        List<Taxon> taxa = taxonset.taxonsetInput.get();
//        for (Taxon taxon : taxa) {
//            taxonsets.add(taxon);
//        }
        pane.getChildren().add(getContent(taxa));
        if (taxa.size() == 1 && taxa.get(0).getID().equals("Beauti2DummyTaxonSet") || taxa.size() == 0) {
            taxa.clear();
            try {
                // species is first character of taxon
                guessTaxonSets("(.).*", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
//            taxonSetToModel();
//            modelToTaxonset();
        }
        for (Taxon t : m_taxonset) {
        	if (t instanceof TaxonSet taxonset2) {
	            for (Taxon taxon : ((TaxonSet) taxonset2).taxonsetInput.get()) {
	                m_lineageset.add(taxon);
	                m_taxonMap.put(taxon.getID(), taxonset2.getID());
	            }
        	} else {
                m_lineageset.add(t);
                m_taxonMap.put(t.getID(), taxonset.getID());
        	}
        }
        taxonSetToModel();
        modelToTaxonset();
        getChildren().add(pane);
    }

    private Pane getContent(List<Taxon> taxonset) {
        m_taxonset = taxonset;
        m_taxonMap = new HashMap<>();
        m_lineageset = new ArrayList<>();
        taxonMapping = FXCollections.observableArrayList();
//        for (Taxon taxonset2 : m_taxonset) {
//        	if (taxonset2 instanceof TaxonSet) {
//		        for (Taxon taxon : ((TaxonSet) taxonset2).taxonsetInput.get()) {
//		            m_lineageset.add(taxon);
//		            m_taxonMap.put(taxon.getID(), taxonset2.getID());
//		            taxonMapping.add(new TaxonMap(taxon.getID(), taxonset2.getID()));
//		        }
//        	}
//        }

        // set up table.
        // special features: background shading of rows
        // custom editor allowing only Date column to be edited.
        m_table = new TableView<>();        
        m_table.setPrefWidth(1024);
        m_table.setEditable(true);
        m_table.setItems(taxonMapping);

        TableColumn<TaxonMap, String> col1 = new TableColumn<>("Taxon");
        col1.setPrefWidth(500);
        col1.setEditable(false);
        col1.setCellValueFactory(
        	    new PropertyValueFactory<TaxonMap,String>("Taxon")
        	);
        m_table.getColumns().add(col1);
//        col1.getSortNode().setOnMouseClicked(e -> {
//            // The index of the column whose header was clicked
//			int vColIndex = 0;
//            if (vColIndex != m_sortByColumn) {
//                m_sortByColumn = vColIndex;
//                m_bIsAscending = true;
//            } else {
//                m_bIsAscending = !m_bIsAscending;
//            }
//            taxonSetToModel();
//        });

        TableColumn<TaxonMap, String> col2 = new TableColumn<>("Species/Population");
        col2.setPrefWidth(500);
        col2.setEditable(true);
        col2.setCellValueFactory(
        	    new PropertyValueFactory<TaxonMap,String>("Taxon2")
        	);
        col2.setCellFactory(TextFieldTableCell.forTableColumn());
//        col2.getSortNode().setOnMouseClicked(e -> {
//                    // The index of the column whose header was clicked
//        			int vColIndex = 1;
//                    if (vColIndex != m_sortByColumn) {
//                        m_sortByColumn = vColIndex;
//                        m_bIsAscending = true;
//                    } else {
//                        m_bIsAscending = !m_bIsAscending;
//                    }
//                    taxonSetToModel();
//            });
        m_table.getColumns().add(col2);
        m_table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
//        taxonSetToModel();

        col2.setOnEditCommit(
                new EventHandler<CellEditEvent<TaxonMap, String>>() {
					@Override
					public void handle(CellEditEvent<TaxonMap, String> event) {
						String newValue = event.getNewValue();
						TaxonMap taxonMap = event.getRowValue();
						taxonMap.setTaxon2(newValue);
						modelToTaxonset();
					}
				}                
            );

        
        
        
//        		new JTable(m_model) {
//
//            // method that induces table row shading
//            @Override
//            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
//                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
//                // even index, selected or not selected
//                if (isCellSelected(Index_row, Index_col)) {
//                    comp.setBackground(Color.gray);
//                } else if (Index_row % 2 == 0) {
//                    comp.setBackground(new Color(237, 243, 255));
//                } else {
//                    comp.setBackground(Color.white);
//                }
//                return comp;
//            }
//        };

//        m_table.setDefaultEditor(Object.class, new TableCellEditor() {
//            TextField m_textField = new TextField();
//            int m_iRow
//                    ,
//                    m_iCol;
//
//            @Override
//            public boolean stopCellEditing() {
//                m_table.removeEditor();
//                String text = m_textField.getText();
//                //Log.warning.println(text);
//                m_model.setValueAt(text, m_iRow, m_iCol);
//
//                // try {
//                // Double.parseDouble(text);
//                // } catch (Exception e) {
//                // return false;
//                // }
//                modelToTaxonset();
//                return true;
//            }
//            
//
//            @Override
//            public boolean isCellEditable(EventObject anEvent) {
//                return m_table.getSelectedColumn() == 1;
//            }
//
//            @Override
//            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowNr,
//                                                         int colNr) {
//                if (!isSelected) {
//                    return null;
//                }
//                m_iRow = rowNr;
//                m_iCol = colNr;
//                m_textField.setText((String) value);
//                return m_textField;
//            }
//
//            @Override
//            public boolean shouldSelectCell(EventObject anEvent) {
//                return false;
//            }
//
//            @Override
//            public void removeCellEditorListener(CellEditorListener l) {
//            }
//
//            @Override
//            public Object getCellEditorValue() {
//                return null;
//            }
//
//            @Override
//            public void cancelCellEditing() {
//            }
//
//            @Override
//            public void addCellEditorListener(CellEditorListener l) {
//            }
//
//        });

//        m_table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
//        m_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		int size = m_table.getFont().getSize();
//		m_table.setRowHeight(20 * size/13);
//        m_table.getColumnModel().getColumn(0).setPreferredWidth(250 * size/13);
//        m_table.getColumnModel().getColumn(1).setPreferredWidth(250 * size/13);

//        JTableHeader header = m_table.getTableHeader();
//        header.addMouseListener(new ColumnHeaderListener());

        //JScrollPane pane = new JScrollPane(m_table);
        //HBox tableBox = FXUtils.newHBox();
        //tableBox.getChildren().add(new Separator());
        //tableBox.getChildren().add(pane);
        //tableBox.getChildren().add(new Separator());

        VBox box = FXUtils.newVBox();
        box.getChildren().add(createFilterBox());
        box.getChildren().add(m_table);
        box.getChildren().add(createButtonBox());
        return box;
    }

    private Pane createButtonBox() {
        HBox buttonBox = FXUtils.newHBox();

        Button fillDownButton = new Button("Fill down");
        fillDownButton.setId("Fill down");
        fillDownButton.setTooltip(new Tooltip("replaces all taxons in selection with the one that is selected at the top"));
        fillDownButton.setOnAction(e -> {
                List<Integer> rows = m_table.getSelectionModel().getSelectedIndices();
                if (rows.size() < 2) {
                    return;
                }
                String taxon = taxonMapping.get(rows.get(0)).getTaxon2();
                for (int i = 1; i < rows.size(); i++) {
                    taxonMapping.get(rows.get(i)).setTaxon2(taxon);
                }
                modelToTaxonset();
            });

        Button guessButton = new Button("Guess");
        guessButton.setId("Guess");
        guessButton.setOnAction(e -> {
                guess();
            });

        //buttonBox.getChildren().add(new Separator());
        buttonBox.getChildren().add(fillDownButton);
        //buttonBox.getChildren().add(new Separator());
        buttonBox.getChildren().add(guessButton);
        //buttonBox.getChildren().add(new Separator());
        return buttonBox;
    }

//    public class ColumnHeaderListener extends MouseAdapter {
//        @Override
//		public void mouseClicked(MouseEvent evt) {
//            // The index of the column whose header was clicked
//            int vColIndex = m_table.getColumnModel().getColumnIndexAtX(evt.getX());
//            if (vColIndex == -1) {
//                return;
//            }
//            if (vColIndex != m_sortByColumn) {
//                m_sortByColumn = vColIndex;
//                m_bIsAscending = true;
//            } else {
//                m_bIsAscending = !m_bIsAscending;
//            }
//            taxonSetToModel();
//        }
//    }

    private void guess() {
        GuessPatternDialog dlg = new GuessPatternDialog(this, m_sPattern);
        switch(dlg.showDialog("Guess taxon sets")) {
        case canceled: return;
        case pattern: 
        String pattern = dlg.getPattern();
            try {
                guessTaxonSets(pattern, 0);
                m_sPattern = pattern;
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        case trait:
        	parseTrait(dlg.getTraitMap());
            break;
        }
        m_lineageset.clear();
        for (Taxon taxonset2 : m_taxonset) {
            for (Taxon taxon : ((TaxonSet) taxonset2).taxonsetInput.get()) {
                m_lineageset.add(taxon);
                m_taxonMap.put(taxon.getID(), taxonset2.getID());
            }
        }
        taxonSetToModel();
        modelToTaxonset();
    }

    /**
     * guesses taxon sets based on pattern in regExp based on the taxa in
     * m_rawData
     */
    public int guessTaxonSets(String regexp, int minSize) {
        m_taxonset.clear();
        HashMap<String, TaxonSet> map = new HashMap<>();
        Pattern m_pattern = Pattern.compile(regexp);
        Set<Taxon> taxa = new HashSet<>();
        Set<String> taxonIDs = new HashSet<>();
        for (Alignment alignment : getDoc().alignments) {
        	for (String id : alignment.getTaxaNames()) {
                if (!taxonIDs.contains(id)) {
                	Taxon taxon = getDoc().getTaxon(id);
	                taxa.add(taxon);
	                taxonIDs.add(id);
        		}
        	}
            for (Sequence sequence : alignment.sequenceInput.get()) {
                String id = sequence.taxonInput.get();
                if (!taxonIDs.contains(id)) {
                    Taxon taxon = getDoc().getTaxon(sequence.taxonInput.get());
                    // ensure sequence and taxon do not get same ID
                    if (sequence.getID().equals(sequence.taxonInput.get())) {
                        sequence.setID("_" + sequence.getID());
                    }
                    taxa.add(taxon);
                    taxonIDs.add(id);
                }
            }
        }

        List<String> unknowns = new ArrayList<>();
        for (Taxon taxon : taxa) {
            if (!(taxon instanceof TaxonSet)) {
                Matcher matcher = m_pattern.matcher(taxon.getID());
                String match;
                if (matcher.find()) {
                    match = matcher.group(1);
                } else {
                   	match = "UNKNOWN";
                   	unknowns.add(taxon.getID());
                }
                try {
                    if (map.containsKey(match)) {
                        TaxonSet set = map.get(match);
                        set.taxonsetInput.setValue(taxon, set);
                    } else {
                    	TaxonSet set = newTaxonSet(match);
                        set.taxonsetInput.setValue(taxon, set);
                        map.put(match, set);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (unknowns.size() > 0) {
        	showMisMatchMessage(unknowns);
        }
        // add taxon sets
        int ignored = 0;
        for (TaxonSet set : map.values()) {
            if (set.taxonsetInput.get().size() > minSize) {
                m_taxonset.add(set);
            } else {
                ignored += set.taxonsetInput.get().size();
            }
        }
        return ignored;
    }

    private TaxonSet newTaxonSet(String match) {
    	if (getDoc().taxaset.containsKey(match)) {
    		Taxon t = doc.taxaset.get(match);
    		if (t instanceof TaxonSet) {
    			TaxonSet set = (TaxonSet) t;
    			set.taxonsetInput.get().clear();
    			return set;
    		} else {
    			// TODO handle situation where taxon and set have same name (issue #135)
    		}
    	}
        TaxonSet set = new TaxonSet();
        set.setID(match);
		return set;
	}

	void parseTrait(Map<String,String> traitMap) {
        m_taxonset.clear();

        Set<Taxon> taxa = new HashSet<>();
        Set<String> taxonIDs = new HashSet<>();
        for (Alignment alignment : getDoc().alignments) {
        	if (alignment instanceof FilteredAlignment) {
        		alignment = ((FilteredAlignment)alignment).alignmentInput.get();
        	}
            for (String id : alignment.getTaxaNames()) {
                if (!taxonIDs.contains(id)) {
                    Taxon taxon = getDoc().getTaxon(id);
                    taxa.add(taxon);
                    taxonIDs.add(id);
                }
            }
        }

        HashMap<String, TaxonSet> map = new HashMap<>();
        List<String> unknowns = new ArrayList<>();
        for (Taxon taxon : taxa) {
            if (!(taxon instanceof TaxonSet)) {
                String match = traitMap.get(taxon.getID());
                if (match == null) {
                	match = "UNKNOWN";
                	unknowns.add(taxon.getID());
                }
                try {
                    if (map.containsKey(match)) {
                        TaxonSet set = map.get(match);
                        set.taxonsetInput.setValue(taxon, set);
                    } else {
                        TaxonSet set = newTaxonSet(match);
                        set.taxonsetInput.setValue(taxon, set);
                        map.put(match, set);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        // add taxon sets
        for (TaxonSet set : map.values()) {
             m_taxonset.add(set);
        }
        if (unknowns.size() > 0) {
        	showMisMatchMessage(unknowns);
        }
    }
    
    private void showMisMatchMessage(List<String> unknowns) {
    	Alert.showMessageDialog(
    	    this, 
    	    "Some taxa did not have a match and are set to UNKNOWN:\n" + unknowns.toString().replaceAll(",", "\n"), 
    	    "Warning", 
    	    Alert.INFORMATION_MESSAGE);
	}

	String m_sPattern = "^(.+)[-_\\. ](.*)$";


    private Pane createFilterBox() {
        HBox filterBox = FXUtils.newHBox();
        filterBox.getChildren().add(new Label("filter: "));
        // Dimension size = new Dimension(100,20);
        filterEntry = new TextField();
        filterEntry.setPrefColumnCount(20);
        // filterEntry.setMinSize(size);
        // filterEntry.setPrefSize(size);
        // filterEntry.setSize(size);
        filterEntry.setTooltip(new Tooltip("Enter regular expression to match taxa"));
//		int size = filterEntry.getFont().getSize();
//        filterEntry.setMaxSize(new Dimension(1024, 20 * size/13));
        filterBox.getChildren().add(filterEntry);
        //filterBox.getChildren().add(new Separator());
        filterBox.setOnKeyReleased(e->processFilter());
        
//        filterEntry.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void removeUpdate(DocumentEvent e) {
//                processFilter();
//            }
//
//            @Override
//            public void insertUpdate(DocumentEvent e) {
//                processFilter();
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent e) {
//                processFilter();
//            }
//
//        });
        return filterBox;
    }

    private void processFilter() {
        String filter = ".*" + filterEntry.getText() + ".*";
        try {
            // sanity check: make sure the filter is legit
            filter.matches(filter);
            m_sFilter = filter;
            taxonSetToModel();
            // m_table.repaint();
        } catch (PatternSyntaxException e) {
            // ignore
        }
    }

    /**
     * for convert taxon sets to table model *
     */
    private void taxonSetToModel() {
        TaxonSet taxonset = (TaxonSet) m_input.get();
        int i = 0;
        for (Taxon taxon : m_taxonset) {
            try {
            	if (taxon instanceof TaxonSet) {
            		for (Taxon t2 : ((TaxonSet)taxon).taxonsetInput.get()) {
                    	if (i < taxonMapping.size()) {
                    		taxonMapping.get(i).setTaxon(t2.getID());
                    		taxonMapping.get(i).setTaxon2(taxon.getID());
                    	} else {
                    		taxonMapping.add(new TaxonMap(t2.getID(), taxon.getID()));
                    	}
                        i++;
            			
            		}
            	}
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        m_table.refresh();
	    	
//        // clear table model
//        while (m_model.getRowCount() > 0) {
//            m_model.removeRow(0);
//        }
//
//        // fill table model with lineages matching the filter
//        for (String lineageID : m_taxonMap.keySet()) {
//            if (lineageID.matches(m_sFilter)) {
//                Object[] rowData = new Object[2];
//                rowData[0] = lineageID;
//                rowData[1] = m_taxonMap.get(lineageID);
//                m_model.addRow(rowData);
//            }
//        }
//
//        @SuppressWarnings("rawtypes")
//        Vector data = m_model.getDataVector();
//        Collections.sort(data, (Vector<?> v1, Vector<?> v2) -> {
//                String o1 = (String) v1.get(m_sortByColumn);
//                String o2 = (String) v2.get(m_sortByColumn);
//                if (o1.equals(o2)) {
//                    o1 = (String) v1.get(1 - m_sortByColumn);
//                    o2 = (String) v2.get(1 - m_sortByColumn);
//                }
//                if (m_bIsAscending) {
//                    return o1.compareTo(o2);
//                } else {
//                    return o2.compareTo(o1);
//                }
//            }
//
//        );
//        m_model.fireTableRowsInserted(0, m_model.getRowCount());
    }

    /**
     * for convert table model to taxon sets *
     */
    private void modelToTaxonset() {

    	Set<String> existingTaxa = new HashSet<>();
        for (Taxon taxon : m_taxonset) {
            existingTaxa.add(taxon.getID());
        }    	
    	
    	
        // update map
        for (int i = 0; i < taxonMapping.size(); i++) {
            String lineageID = taxonMapping.get(i).getTaxon();
            String taxonSetID = taxonMapping.get(i).getTaxon2();

            // new taxon set?
            if (!existingTaxa.contains(taxonSetID)) {
                // create new taxon set
                TaxonSet taxonset = newTaxonSet(taxonSetID);
                m_taxonset.add(taxonset);
            }
            m_taxonMap.put(lineageID, taxonSetID);
        }

        // clear old taxon sets
        for (Taxon taxon : m_taxonset) {
        	if (taxon instanceof TaxonSet set) {
	            set.taxonsetInput.get().clear();
	            doc.registerPlugin(set);
        	}
        }

        // group lineages with their taxon sets
        for (String lineageID : m_taxonMap.keySet()) {
            for (Taxon taxon : m_lineageset) {
                if (taxon.getID().equals(lineageID)) {
                    String taxonSet = m_taxonMap.get(lineageID);
                    for (Taxon taxon2 : m_taxonset) {
                    	if (taxon2 instanceof TaxonSet set) {
	                        if (set.getID().equals(taxonSet)) {
	                            try {
	                                set.taxonsetInput.setValue(taxon, set);
	                            } catch (Exception e) {
	                                e.printStackTrace();
	                            }
	                        }
                        }
                    }
                }
            }
        }

        // remove unused taxon sets
        for (int i = m_taxonset.size() - 1; i >= 0; i--) {
        	if (m_taxonset.get(i) instanceof TaxonSet set) {
	            if (set.taxonsetInput.get().size() == 0) {
	                doc.unregisterPlugin(m_taxonset.get(i));
	                m_taxonset.remove(i);
	            }
            }
        }

        taxonSetToModel();
//        TaxonSet taxonset = (TaxonSet) m_input.get();
//        taxonset.taxonsetInput.get().clear();
//        int i = 0;
//        for (Taxon taxon : m_taxonset) {
//            try {
//                taxonset.taxonsetInput.setValue(taxon, taxonset);
//                if (i > taxonMapping.size()) {
//                	taxonMapping.add(new TaxonMap(taxon.getID(), taxonset.getID()));
//                } else {
//	                taxonMapping.get(i).setTaxon(taxon.getID());
//	                taxonMapping.get(i).setTaxon2(taxonset.getID());
//                }
//                i++;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        m_table.refresh();

    }

}
