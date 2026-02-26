package beastfx.app.inputeditor;





import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.Tree;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.text.DateFormat;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TipDatesInputEditor extends BEASTObjectInputEditor {

    private static String DATE_FORMAT_HELP_MESSAGE =
            "If the radio button on the second line is selected, this format string " +
                    "will be used to convert the entries in the second column of the table " +
                    "into heights (ages in years before the most recent sample) in the " +
                    "second column.\n" +
                    "\n" +
                    "The format string may be any combination of the following special characters " +
                    "and other delimiting characters such as '-' or '/'.\n" +
                    "\n" +
                    "  Symbol  Meaning                     Examples\n" +
                    "  ------  -------                     --------\n" +
                    "   G       era                         AD; Anno Domini; A\n" +
                    "   u       year                        2004; 04\n" +
                    "   y       year-of-era                 2004; 04\n" +
                    "   D       day-of-year                 189\n" +
                    "   M/L     month-of-year               7; 07; Jul; July; J\n" +
                    "   d       day-of-month                10\n" +
                    "\n" +
                    "   Q/q     quarter-of-year             3; 03; Q3; 3rd quarter\n" +
                    "   Y       week-based-year             1996; 96\n" +
                    "   w       week-of-week-based-year     27\n" +
                    "   W       week-of-month               4\n" +
                    "   E       day-of-week                 Tue; Tuesday; T\n" +
                    "   e/c     localized day-of-week       2; 02; Tue; Tuesday; T\n" +
                    "   F       week-of-month               3\n" +
                    "\n" +
                    "(The table above is an extract from the documentation for the DateTimeFormatter " +
                    "class which is used by BEAST to parse dates.)\n" +
                    "\n" +
                    "Symbols in the above table representing numeric quantities (e.g. day-of-month " +
                    "and month-of-year) may be repeated to allow for zero-padding.  For instance, " +
                    "the format string \"d/m/y\" will match dates such as \"1/3/2004\" while \"dd/mm/y\" " +
                    "will match \"01/03/2004\".  In addition, use \"yy\" to match the short form of the year.\n" +
                    "\n" +
                    "Be aware that in all cases it is necessary that the format chosen be able to pinpoint a " +
                    "single day in the calendar. For instance, \"d/m\" is not a valid format string as it " +
                    "doesn't allow days to be uniquely identified.";


    public TipDatesInputEditor() {
    	super();
    }    
    public TipDatesInputEditor(BeautiDoc doc) {
        super(doc);
    }

    DateFormat dateFormat = DateFormat.getDateInstance();

    @Override
    public Class<?> type() {
        return Tree.class;
    }
    Tree tree;
    TraitSet traitSet;
    ComboBox<TraitSet.Units> unitsComboBox;
    ComboBox<String> relativeToComboBox;
    List<String> taxa;
    // Object[][] tableData;
    boolean[] recordValid;
    
    public class TipDate {
    	String taxon;
    	String date;
    	Double age;

    	TipDate(String taxon, String date, Double age) {
    		this.taxon = taxon;
    		this.date = date;
    		this.age = age;
    	}

    	public String getTaxon() {
			return taxon;
		}
		public void setTaxon(String taxon) {
			this.taxon = taxon;
		}
		public String getDate() {
			return date;
		}
		public void setDate(String date) {
			this.date = date;
		}
		public Double getAge() {
			return age;
		}
		public void setAge(Double age) {
			this.age = age;
		}
    } // class TipDate

    ObservableList<TipDate> tipDateEntries;
    TableView<TipDate> table;
    String m_sPattern = ".*(\\d\\d\\d\\d).*";
    ScrollPane scrollPane;
    List<Taxon> taxonsets;

    RadioButton numericRadioButton, formattedDateRadioButton;
    ToggleGroup radioButtonGroup;
    ComboBox<String> dateFormatComboBox;
    CheckBox useTipDates;
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        pane = FXUtils.newVBox();
        this.itemNr = itemNr;
        if (itemNr >= 0) {
            tree = (Tree) ((List<?>) input.get()).get(itemNr);
        } else {
            tree = (Tree) input.get();
        }
        if (tree != null) {
            try {
                m_input = ((BEASTInterface) tree).getInput("trait");
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            m_beastObject = tree;
            traitSet = tree.getDateTrait();

            VBox box = FXUtils.newVBox();

            useTipDates = new CheckBox("Use tip dates");
            useTipDates.setId("useTipDates");
            useTipDates.setSelected(traitSet != null);
            useTipDates.setOnAction(e -> {
            	CheckBox checkBox = (CheckBox) e.getSource();
                    try {
                        if (checkBox.isSelected()) {
                            if (traitSet == null) {
                                traitSet = new TraitSet();
                                traitSet.initByName("traitname", "date",
                                        "taxa", tree.getTaxonset(),
                                        "value", "");
                                traitSet.setID("dateTrait.t:" + BeautiDoc.parsePartition(tree.getID()));
                            }
                            tree.setDateTrait(traitSet);
                        } else {
                            tree.setDateTrait(null);
                        }

                        refreshPanel();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                });
            HBox box2 = FXUtils.newHBox();
            box2.getChildren().add(useTipDates);
            // box2.getChildren().add(new Separator());
            box.getChildren().add(box2);

            if (traitSet != null) {
                box.getChildren().add(createButtonBox());
                box.getChildren().add(createListBox());
            }
            pane.getChildren().add(box);
        }
        getChildren().add(pane);
    } // init

    @Override
    public void refreshPanel() {
    	VBox box = (VBox) pane.getChildren().get(0);
        if (useTipDates.isSelected()) {
        	if (box.getChildren().size() == 1) {
        		box.getChildren().add(createButtonBox());
        		box.getChildren().add(createListBox());
        	}
        } else {
        	while (box.getChildren().size() > 1) {
        		box.getChildren().remove(box.getChildren().size()-1);
        	}
        }
        convertTraitToTableData();
    } 
    
    private Node createListBox() {
        taxa = traitSet.taxaInput.get().asStringList();
        List<TipDate> list = new ArrayList<>();
        for (String taxon : taxa) {
        	list.add(new TipDate(taxon, "0", 0.0));
        }
        tipDateEntries = FXCollections.observableArrayList(list);
        
        // String[] columnData = new String[]{"Name", "Date (raw value)", "Height"};
        // tableData = new Object[taxa.size()][3];
        
        table = new TableView<>();
        table.setPrefWidth(800);
        table.setMinSize(doc.beauti.frame.getWidth()-12, doc.beauti.frame.getHeight()-230);
        BeautiPanel.resizeList.clear();
        BeautiPanel.resizeList.add(table);

        table.setEditable(true);

        TableColumn<TipDate,String> col1 = new TableColumn<>("Taxon");
        col1.setPrefWidth(500);
        col1.setEditable(false);
        col1.setCellValueFactory(
        	    new PropertyValueFactory<TipDate,String>("Taxon")
        	);
        table.getColumns().add(col1);
        
        TableColumn<TipDate,String> col2 = new TableColumn<>("Date (raw value)");
        col2.setPrefWidth(150);
        col2.setEditable(true);
        col2.setCellValueFactory(
        	    new PropertyValueFactory<TipDate,String>("Date")
        	);
        table.getColumns().add(col2);
        
        col2.setCellFactory(TextFieldTableCell.forTableColumn());
        col2.setOnEditCommit(
                new EventHandler<CellEditEvent<TipDate, String>>() {
					@Override
					public void handle(CellEditEvent<TipDate, String> event) {
						String newValue = event.getNewValue();
						TipDate tipDate = event.getRowValue();
						tipDate.setDate(newValue);
						convertTableDataToTrait();
						convertTraitToTableData();
					}
				}
                
//                <CellEditEvent<TipDate<CellEditEvent<TipDate, String>>() {
//                    public void handle(CellEditEvent<GasRatioMeasureBean, String> measure) {
//                        ((GasRatioMeasureBean) measure.getTableView().getItems().get(
//                        measure.getTablePosition().getRow())
//                        ).setMeasure(measure.getNewValue());
//                    }
//                }
            );
        
        TableColumn<TipDate,Double> col3 = new TableColumn<>("Age/Height");
        col3.setPrefWidth(150);
        col3.setEditable(false);
        col3.setCellValueFactory(
        	    new PropertyValueFactory<TipDate,Double>("Age")
        	);
        table.getColumns().add(col3);
        
        table.setItems(tipDateEntries);

        
        recordValid = new boolean[taxa.size()];
        convertTraitToTableData();
        
        // set up table.
        // special features: background shading of rows
        // custom editor allowing only Date column to be edited.
//        table = new JTable(tableData, columnData) {
//
//            // method that induces table row shading
//            @Override
//            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
//                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
//                //even index, selected or not selected
//                comp.setForeground(Color.black);
//                if (isCellSelected(Index_row, Index_col)) {
//                    comp.setBackground(Color.lightGray);
//                } else if (!recordValid[Index_row]) {
//                    comp.setForeground(Color.WHITE);
//                    comp.setBackground(Color.RED);
//                } else if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col)) {
//                    comp.setBackground(new Color(237, 243, 255));
//                } else {
//                    comp.setBackground(Color.white);
//                }
//                return comp;
//            }
//        };

//        // set up editor that makes sure only doubles are accepted as entry
//        // and only the Date column is editable.
//        table.setDefaultEditor(Object.class, new TableCellEditor() {
//            TextField m_textField = new TextField();
//            int m_iRow,
//                    m_iCol;
//
//            @Override
//            public boolean stopCellEditing() {
//                table.removeEditor();
//                String text = m_textField.getText();
//
//                tableData[m_iRow][m_iCol] = text;
//                convertTableDataToTrait();
//                convertTraitToTableData();
//                return true;
//            }
//
//            @Override
//            public boolean isCellEditable(EventObject anEvent) {
//                return table.getSelectedColumn() == 1;
//            }
//
//            @Override
//            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowNr, int colNr) {
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
//        });
//        // int fontsize = table.getFont().getSize();
        // table.setRowHeight(24 * fontsize / 13);
//        scrollPane = new ScrollPane();
//        scrollPane.setContent(table);
//
//        return scrollPane;
        return table;
    } // createListBox

    private void clearTable(boolean heightsOnly) {
         for (int i = 0; i < tipDateEntries.size(); i++) {
        	 TipDate entry = tipDateEntries.get(i);
        	 entry.setTaxon(taxa.get(i));
            if (!heightsOnly)
                entry.setDate("0");
            entry.setAge(0.0);
         }
         table.refresh();
    }

    /* synchronise table with data from traitSet BEASTObject */
    private void convertTraitToTableData() {
        clearTable(false);

        String[] traits = traitSet.traitsInput.get().split(",");
        for (String trait : traits) {
            trait = trait.replaceAll("\\s+", " ");
            String[] strs = trait.split("=");
            if (strs.length != 2) {
                break;
            }

            String taxonID = normalize(strs[0]);
            int taxonIndex = taxa.indexOf(taxonID);

            if (taxonIndex >= 0) {
                tipDateEntries.get(taxonIndex).setDate(normalize(strs[1]));
                tipDateEntries.get(taxonIndex).setTaxon(taxonID);
            } else {
            	Log.warning.println("WARNING: File contains taxon " + taxonID + " that cannot be found in alignment");
            }
        }

        boolean numericParseError = false;
        boolean dateParseError = false;
        double [] convertedValues = new double[taxa.size()];

        for (int i=0; i< tipDateEntries.size(); i++) {
            recordValid[i] = true;

            try {
                convertedValues[i] = traitSet.convertValueToDouble((String) tipDateEntries.get(i).getDate());
            } catch (DateTimeParseException ex) {
                dateParseError = true;
                recordValid[i] = false;
            } catch (IllegalArgumentException ex) {
                numericParseError = true;
                recordValid[i] = false;
            }
        }

        if (dateParseError) {
            Alert.showMessageDialog(this,
                    "Error interpreting one or more trait values as a formatted date or numeric value.\n\n" +
                            "Problem taxa will be highlighted in table.",
                    "Date parsing error",
                    Alert.ERROR_MESSAGE);
            clearTable(true);
        } else if (numericParseError) {
            Alert.showMessageDialog(this,
                    "Error interpreting one or more trait values as a numeric value.\n\n" +
                            "Problem taxa will be highlighted in table.",
                    "Date parsing error",
                    Alert.ERROR_MESSAGE);
            clearTable(true);
        } else {
            if (traitSet.traitNameInput.get().equals(TraitSet.DATE_BACKWARD_TRAIT)) {
                Double minDate = Double.MAX_VALUE;
                for (int i = 0; i < tipDateEntries.size(); i++) {
                    minDate = Math.min(minDate, convertedValues[i]);
                }
                for (int i = 0; i < tipDateEntries.size(); i++) {
                	tipDateEntries.get(i).setAge(convertedValues[i] - minDate);
                }
            } else {
                Double maxDate = 0.0;
                for (int i = 0; i < tipDateEntries.size(); i++) {
                    maxDate = Math.max(maxDate, convertedValues[i]);
                }
                for (int i = 0; i < tipDateEntries.size(); i++) {
                	tipDateEntries.get(i).setAge(maxDate - convertedValues[i]);
                }
            }
        }

        table.refresh();
//        if (table != null) {
//            for (int i = 0; i < tipDateEntries.size(); i++) {
//                table.setValueAt(tipDateEntries.get(i).getDate(), i, 1);
//                table.setValueAt(tipDateEntries.get(i).getAge(), i, 2);
//            }
//        }
    } // convertTraitToTableData

    private String normalize(String str) {
        if (str.charAt(0) == ' ') {
            str = str.substring(1);
        }
        if (str.endsWith(" ")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * synchronise traitSet BEAST object with table data
     */
    private void convertTableDataToTrait() {
        String trait = "";
        for (int i = 0; i < tipDateEntries.size(); i++) {
            trait += taxa.get(i) + "=" + tipDateEntries.get(i).getDate();
            if (i < tipDateEntries.size() - 1) {
                trait += ",\n";
            }
        }
        try {
            traitSet.traitsInput.setValue(trait, traitSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * create box with comboboxes for selectin units and trait name *
     */
    private HBox createButtonBox() {
        HBox buttonBox = FXUtils.newHBox();

        Label label = new Label("Dates specified: ");
        // label.setAlignment(Pos.TOP_CENTER);
        label.setMaxSize(label.getPrefWidth(), label.getPrefHeight());
        buttonBox.getChildren().add(label);

        VBox formatBox = new VBox();
        formatBox.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
        HBox formatBoxFirstLine = new HBox();
        formatBoxFirstLine.setSpacing(10);
        formatBoxFirstLine.setPadding(new Insets(3));
        HBox formatBoxSecondLine = new HBox();
        formatBoxSecondLine.setSpacing(10);
        formatBoxSecondLine.setPadding(new Insets(10,3,3,3));

        radioButtonGroup = new ToggleGroup();
        numericRadioButton = new RadioButton("numerically as");
        numericRadioButton.setTooltip(new Tooltip("Interpret values as numerical times."));

        if (traitSet.dateTimeFormatInput.get() == null)
            numericRadioButton.setSelected(true);

        numericRadioButton.setOnAction(e -> {
            traitSet.dateTimeFormatInput.setValue(null, traitSet);
            unitsComboBox.setDisable(false);
            relativeToComboBox.setDisable(false);
            dateFormatComboBox.setDisable(true);
            refreshPanel();
        });

        numericRadioButton.setToggleGroup(radioButtonGroup);
        formatBoxFirstLine.getChildren().add(numericRadioButton);

        unitsComboBox = new ComboBox<>();
        unitsComboBox.getItems().addAll(TraitSet.Units.values());
        unitsComboBox.setValue(traitSet.unitsInput.get());
        unitsComboBox.setOnAction(e -> {
                String selected = unitsComboBox.getValue().toString();
                try {
                    traitSet.unitsInput.setValue(selected, traitSet);
                    //System.err.println("Traitset is now: " + m_traitSet.m_sUnits.get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        Dimension2D d = new Dimension2D(unitsComboBox.getPrefWidth(), unitsComboBox.getPrefHeight());
        unitsComboBox.setMaxSize(d.getWidth(), d.getHeight());
        unitsComboBox.setPrefSize(d.getWidth(), d.getHeight());
        unitsComboBox.setDisable(!numericRadioButton.isSelected());
        formatBoxFirstLine.getChildren().add(unitsComboBox);

        relativeToComboBox = new ComboBox<>();
        relativeToComboBox.getItems().addAll(new String[]{"Since some time in the past", "Before the present"});
        relativeToComboBox.setTooltip(new Tooltip("Whether dates go forward or backward"));
        if (traitSet.traitNameInput.get().equals(TraitSet.DATE_BACKWARD_TRAIT)) {
            relativeToComboBox.getSelectionModel().select(1);
        } else {
            relativeToComboBox.getSelectionModel().select(0);
        }
        relativeToComboBox.setOnAction(e -> {
                String selected = TraitSet.DATE_BACKWARD_TRAIT;
                if (relativeToComboBox.getSelectionModel().getSelectedIndex() == 0) {
                    selected = TraitSet.DATE_FORWARD_TRAIT;
                }
                try {
                    traitSet.traitNameInput.setValue(selected, traitSet);
                    Log.warning.println("Relative position is now: " + traitSet.traitNameInput.get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                convertTraitToTableData();
            });
        relativeToComboBox.setMaxSize(relativeToComboBox.getPrefWidth(), relativeToComboBox.getPrefHeight());
        relativeToComboBox.setDisable(!numericRadioButton.isSelected());
        formatBoxFirstLine.getChildren().add(relativeToComboBox);
        //formatBoxFirstLine.getChildren().add(new Separator());
        formatBox.getChildren().add(formatBoxFirstLine);

        formattedDateRadioButton = new RadioButton("as dates with format");
        formattedDateRadioButton.setTooltip(new Tooltip("Interpret values as dates with the given format."));

        if (traitSet.dateTimeFormatInput.get() != null)
            formattedDateRadioButton.setSelected(true);

        formattedDateRadioButton.setOnAction(e -> {
            traitSet.dateTimeFormatInput.setValue(dateFormatComboBox.getValue(), traitSet);
            unitsComboBox.setDisable(true);
            relativeToComboBox.setDisable(true);
            dateFormatComboBox.setDisable(false);
            refreshPanel();
        });
        formattedDateRadioButton.setToggleGroup(radioButtonGroup);
        formatBoxSecondLine.getChildren().add(formattedDateRadioButton);

        String[] dateFormatExamples = {
                "dd/M/yyyy",
                "M/dd/yyyy",
                "yyyy/M/dd",
                "dd-M-yyyy",
                "M-dd-yyyy",
                "yyyy-M-dd"};

        dateFormatComboBox = new ComboBox<>();
        dateFormatComboBox.getItems().addAll(dateFormatExamples);
        dateFormatComboBox.setTooltip(new Tooltip("Set format used to parse date values"));
        dateFormatComboBox.setEditable(true);
        if (traitSet.dateTimeFormatInput.get() != null)
            dateFormatComboBox.setValue(traitSet.dateTimeFormatInput.get());
        else
            dateFormatComboBox.setValue(dateFormatExamples[0]);
        dateFormatComboBox.setMaxSize(dateFormatComboBox.getPrefWidth(), dateFormatComboBox.getPrefHeight());
        dateFormatComboBox.setDisable(!formattedDateRadioButton.isSelected());
        dateFormatComboBox.setOnAction(e -> {
            traitSet.dateTimeFormatInput.setValue(dateFormatComboBox.getValue(), traitSet);
            refreshPanel();
        });
        formatBoxSecondLine.getChildren().add(dateFormatComboBox);

        Button dateFormatHelpButton = new Button("?");
        dateFormatHelpButton.setOnAction(e ->
                WrappedOptionPane.showWrappedMessageDialog(this,
                        DATE_FORMAT_HELP_MESSAGE, "Menlo"));
        formatBoxSecondLine.getChildren().add(dateFormatHelpButton);

        //formatBoxSecondLine.getChildren().add(new Separator());

        formatBox.getChildren().add(formatBoxSecondLine);
        formatBox.setAlignment(Pos.TOP_CENTER);
        buttonBox.getChildren().add(formatBox);

        //buttonBox.getChildren().add(new Separator());

        Button guessButton = new Button("Auto-configure");
        //guessButton.setPadding(new Insets(0,10,0,0));
        // guessButton.setAlignment(Pos.TOP_CENTER);
        guessButton.setTooltip(new Tooltip("Automatically configure dates based on taxon names"));
        guessButton.setId("Guess");
        guessButton.setOnAction(e -> {
                GuessPatternDialog dlg = new GuessPatternDialog(null, m_sPattern);
                dlg.allowAddingValues();
                StringBuilder traitBuilder = new StringBuilder();
                switch (dlg.showDialog("Guess dates")) {
                    case canceled:
                        return;

                    case trait:
                        Map<String,String> traitMap = dlg.getTraitMap();
                        for (String taxon : taxa) {
                            if (!traitMap.containsKey(taxon))
                                continue;

                            if (traitBuilder.length()>0) {
                                traitBuilder.append(",");
                            }

                            traitBuilder.append(taxon).append("=")
                                    .append(traitMap.get(taxon));
                        }
                        break;

                    case pattern:
                        for (String taxon : taxa) {
                            String match = dlg.match(taxon);
                            if (match == null || match.isEmpty()) {
                                return;
                            }
                            if (traitBuilder.length() > 0) {
                                traitBuilder.append(",");
                            }
                            traitBuilder.append(taxon).append("=").append(match);
                        }
                        break;
                }
                try {
                    traitSet.traitsInput.setValue(traitBuilder.toString(), traitSet);
                } catch (Exception ex) {
                    // TODO: handle exception
                }
                refreshPanel();
            });
        buttonBox.getChildren().add(guessButton);


        Button clearButton = new Button("Clear");
        clearButton.setAlignment(Pos.TOP_CENTER);
        clearButton.setTooltip(new Tooltip("Set all dates to zero"));
        clearButton.setOnAction(e -> {
                try {
                    traitSet.traitsInput.setValue("", traitSet);
                } catch (Exception ex) {
                    // TODO: handle exception
                }
                refreshPanel();
            });
        buttonBox.getChildren().add(clearButton);

        return buttonBox;
    } // createButtonBox
}
