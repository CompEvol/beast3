package beastfx.app.inputeditor;



import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import beastfx.app.util.PartitionContextUtil;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.FilteredAlignment;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.spec.evolution.likelihood.GenericTreeLikelihood;
import beast.base.spec.evolution.sitemodel.SiteModel;
import beast.base.evolution.sitemodel.SiteModelInterface;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.MCMC;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.parser.PartitionContext;

// TODO: add warning if useAmbiguities=false and nr of patterns=1 (happens when all data is ambiguous)

public class AlignmentListInputEditor extends ListInputEditor {

	final static int NAME_COLUMN = 0;
	final static int FILE_COLUMN = 1;
	final static int TAXA_COLUMN = 2;
	final static int SITES_COLUMN = 3;
	final static int TYPE_COLUMN = 4;
	final static int SITEMODEL_COLUMN = 5;
	final static int CLOCKMODEL_COLUMN = 6;
	final static int TREE_COLUMN = 7;
	final static int USE_AMBIGUITIES_COLUMN = 8;
	
	final static int NR_OF_COLUMNS = 9;

    final static int STRUT_SIZE = 5;

	/**
	 * alignments that form a partition. These can be FilteredAlignments *
	 */
	protected List<Alignment> alignments;
	protected int partitionCount;
	protected GenericTreeLikelihood[] likelihoods;
	protected Object[][] tableData;
	protected ObservableList<Partition0> tableEntries;
	protected TableView<Partition0> table;
	protected TextField nameEditor;
	protected List<Button> linkButtons;
	protected List<Button> unlinkButtons;
	protected Button splitButton;
	
	boolean updateInProgress = false;

    /**
     * The button for deleting an alignment in the alignment list.
     */
    Button delButton;
    protected SmallButton replaceButton;

	public AlignmentListInputEditor(BeautiDoc doc) {
		super(doc);
	}

	public AlignmentListInputEditor() {
		super();
	}

	@Override
	public Class<?> type() {
		return List.class;
	}

	@Override
	public Class<?> baseType() {
		return Alignment.class;
	}

	@Override
	public Class<?>[] types() {
		Class<?>[] types = new Class[2];
		types[0] = List.class;
		types[1] = Alignment.class;
		return types;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
		this.itemNr = itemNr;
		if (input.get() instanceof List) {
			alignments = (List<Alignment>) input.get();
		} else {
			// we just have a single Alignment
			alignments = new ArrayList<>();
			alignments.add((Alignment) input.get());
		}
		linkButtons = new ArrayList<>();
		unlinkButtons = new ArrayList<>();
		partitionCount = alignments.size();

        // override BoxLayout in superclass
        // setLayout(new BorderLayout());
		BorderPane bpane = new BorderPane();
		pane = bpane;

        bpane.setTop(createLinkButtons());
        bpane.setCenter(createListBox());

        table.setOnDragOver(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        });
        
        table.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
        	if (db.hasFiles()) {
        		addItem(db.getFiles().toArray(new File[] {}));
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });
        
        // this should place the add/remove/split buttons at the bottom of the window.
        bpane.setBottom(createAddRemoveSplitButtons());
        
        //scroller.setMinSize(doc.beauti.frame.getWidth(), doc.beauti.frame.getHeight()-155);
        BeautiPanel.resizeList.clear();
        BeautiPanel.resizeList.add(table);
    	//table.setMinWidth(1020);
        getChildren().add(pane);
        updateStatus();
	}

    /**
     * Creates the link/unlink button component
     * @return a box containing three link/unlink button pairs.
     */
	private Pane createLinkButtons() {
        HBox box = FXUtils.newHBox();
		addLinkUnlinkPair(box, "Site Models");
        addLinkUnlinkPair(box, "Clock Models");
        addLinkUnlinkPair(box, "Trees");
		return box;
	}

    private Pane createAddRemoveSplitButtons() {
        HBox buttonBox = FXUtils.newHBox();

        addButton = new SmallButton("+", true, SmallButton.ButtonType.square);
        addButton.setId("+");
        addButton.setTooltip(new Tooltip("Add item to the list"));
        addButton.setOnAction(e -> addItem());
        buttonBox.getChildren().add(addButton);

        delButton = new SmallButton("-", true, SmallButton.ButtonType.square);
        delButton.setId("-");
        delButton.setTooltip(new Tooltip("Delete selected items from the list"));
        delButton.setOnAction(e -> {
            if (doc.hasLinkedAtLeastOnce) {
                Alert.showMessageDialog(null, "Cannot delete partition while parameters are linked");
                return;
            }
            delItem();
        });
        buttonBox.getChildren().add(delButton);

        replaceButton = new SmallButton("r", true, SmallButton.ButtonType.square);
        replaceButton.setId("r");
        replaceButton.setTooltip(new Tooltip("Replace alignment by one loaded from file"));
        replaceButton.setOnAction(e -> replaceItem());
        buttonBox.getChildren().add(replaceButton);

        
        splitButton = new Button("Split");
        splitButton.setId("Split");
        splitButton.setTooltip(new Tooltip("Split alignment into partitions, for example, codon positions"));
        splitButton.setOnAction(e -> splitItem());
        buttonBox.getChildren().add(splitButton);

        return buttonBox;
    }

	/**
     * This method just adds the two buttons (with add()) and does not add any glue or struts before or after.
     * @param box
     * @param label
     */
	private void addLinkUnlinkPair(HBox box, String label) {

        Button linkSModelButton = new Button("Link " + label);
		linkSModelButton.setId("Link " + label);
		linkSModelButton.setOnAction(e -> {
            Button button = (Button) e.getSource();
            link(columnLabelToNr(button.getText()));
    		initTableData();
        });
		box.getChildren().add(linkSModelButton);
		linkSModelButton.setDisable(getDoc().hasLinkedAtLeastOnce);
		Button unlinkSModelButton = new Button("Unlink " + label);
		unlinkSModelButton.setId("Unlink " + label);
		unlinkSModelButton.setOnAction(e -> {
            Button button = (Button) e.getSource();
            unlink(columnLabelToNr(button.getText()));
    		initTableData();
        });
		box.getChildren().add(unlinkSModelButton);
		unlinkSModelButton.setDisable(getDoc().hasLinkedAtLeastOnce);

		linkButtons.add(linkSModelButton);
		unlinkButtons.add(unlinkSModelButton);
	}

	private int columnLabelToNr(String column) {
		int columnNr;
		if (column.contains("Tree")) {
			columnNr = TREE_COLUMN;
		} else if (column.contains("Clock")) {
			columnNr = CLOCKMODEL_COLUMN;
		} else {
			columnNr = SITEMODEL_COLUMN;
		}
		return columnNr;
	}

	private void link(int columnNr) {
		List<Integer> selected = getTableRowSelection();
		// do the actual linking
		for (int i = 1; i < selected.size(); i++) {
			int rowNr = selected.get(i);
			link(columnNr, rowNr, selected.get(0));
		}
		for (int i : selected) {
			table.getSelectionModel().select(i);
		}
	}
	
	/** links partition in row "rowToLink" with partition in "rowToLinkWith" so that
	 * after linking there is only one partition for context "columnNr", namely that
	 * of "rowToLinkWith"
	 */
	private void link(int columnNr, int rowToLink, int rowToLinkWith) {
		Object old = tableData[rowToLink][columnNr];
		tableData[rowToLink][columnNr] = tableData[rowToLinkWith][columnNr];
		try {
			updateModel(columnNr, rowToLink);
		} catch (Exception ex) {
			Log.warning.println(ex.getMessage());
			// unlink if we could not link
			tableData[rowToLink][columnNr] = old;
			try {
				updateModel(columnNr, rowToLink);
			} catch (Exception ex2) {
				// ignore
			}
		}
		MRCAPriorInputEditor.customConnector(doc);
	}

	
	private void unlink(int columnNr) {
		List<Integer> selected = getTableRowSelection();
		for (int i = 1; i < selected.size(); i++) {
			int rowNr = selected.get(i);
			tableData[rowNr][columnNr] = getDoc().partitionNames.get(rowNr).partition;
			try {
				updateModel(columnNr, rowNr);
			} catch (Exception ex) {
				Log.err.println(ex.getMessage());
				ex.printStackTrace();
			}
		}
		for (int i : selected) {
			table.getSelectionModel().select(i);
		}
	}


    List<Integer> getTableRowSelection() {
    	List<Integer> selected = new ArrayList<>();
    	selected.addAll(table.getSelectionModel().getSelectedIndices());
        return selected;
	}

	/** set partition of type columnNr to partition model nr rowNr **/
	void updateModel(int columnNr, int rowNr) {
		Log.warning.println("updateModel: " + rowNr + " " + columnNr);// + " " +  
		for (int i = 0; i < partitionCount; i++) {
			Log.warning.println(i + " " + tableData[i][0] + " " + tableData[i][SITEMODEL_COLUMN] + " "
					+ tableData[i][CLOCKMODEL_COLUMN] + " " + tableData[i][TREE_COLUMN]);
		}

		getDoc();
		String partition = (String) tableData[rowNr][columnNr];

		// check if partition needs renaming
		String oldName = null;
		boolean isRenaming = false;
		try {
			switch (columnNr) {
			case SITEMODEL_COLUMN:
				if (!doc.pluginmap.containsKey("SiteModel.s:" + partition)) {
					String id = ((BEASTInterface)likelihoods[rowNr].siteModelInput.get()).getID();
					oldName = BeautiDoc.parsePartition(id);
					doc.renamePartition(BeautiDoc.SITEMODEL_PARTITION, oldName, partition);
					isRenaming = true;
				}
				break;
			case CLOCKMODEL_COLUMN: {
				String id = likelihoods[rowNr].branchRateModelInput.get().getID();
				String clockModelName = id.substring(0, id.indexOf('.')) + ".c:" + partition;
				if (!doc.pluginmap.containsKey(clockModelName)) {
					oldName = BeautiDoc.parsePartition(id);
					doc.renamePartition(BeautiDoc.CLOCKMODEL_PARTITION, oldName, partition);
					isRenaming = true;
				}
			}
				break;
			case TREE_COLUMN:
				if (!doc.pluginmap.containsKey("Tree.t:" + partition)) {
					String id = likelihoods[rowNr].treeInput.get().getID();
					oldName = BeautiDoc.parsePartition(id);
					doc.renamePartition(BeautiDoc.TREEMODEL_PARTITION, oldName, partition);
					isRenaming = true;
				}
				break;
			}
		} catch (Exception e) {
			Alert.showMessageDialog(this, "Cannot rename item: " + e.getMessage());
			setTableEntry(rowNr, columnNr, oldName);
			return;
		}
		if (isRenaming) {
			doc.determinePartitions();
			initTableData();
			setUpComboBoxes();
			return;
		}
		
		int partitionID = BeautiDoc.ALIGNMENT_PARTITION;
		switch (columnNr) {
		case SITEMODEL_COLUMN:
			partitionID = BeautiDoc.SITEMODEL_PARTITION;
			break;
		case CLOCKMODEL_COLUMN:
			partitionID = BeautiDoc.CLOCKMODEL_PARTITION;
			break;
		case TREE_COLUMN:
			partitionID = BeautiDoc.TREEMODEL_PARTITION;
			break;
		}
		int partitionNr = doc.getPartitionNr(partition, partitionID);
		GenericTreeLikelihood treeLikelihood = null;
		if (partitionNr >= 0) {
			// we ar linking
			treeLikelihood = likelihoods[partitionNr];
		}

		boolean needsRePartition = false;
		
		PartitionContext oldContext = PartitionContextUtil.newPartitionContext(this.likelihoods[rowNr]);

		switch (columnNr) {
		case SITEMODEL_COLUMN: {
			SiteModelInterface siteModel = null;
			if (treeLikelihood != null) { 
				siteModel = treeLikelihood.siteModelInput.get();
			} else {
				siteModel = (SiteModel) doc.pluginmap.get("SiteModel.s:" + partition);
				if (siteModel != likelihoods[rowNr].siteModelInput.get()) {
					PartitionContext context = getPartitionContext(rowNr);
					try {
					siteModel = (SiteModel.Base) BeautiDoc.deepCopyPlugin((BEASTInterface) likelihoods[rowNr].siteModelInput.get(),
							likelihoods[rowNr], (MCMC) doc.mcmc.get(), oldContext, context, doc, null);
					} catch (RuntimeException e) {
						Alert.showMessageDialog(this, "Could not clone site model: " + e.getMessage());
						return;
					}
				}
			}
			SiteModelInterface target = this.likelihoods[rowNr].siteModelInput.get();
			if (target instanceof SiteModel.Base && siteModel instanceof SiteModel.Base) {
				if (!((SiteModel.Base)target).substModelInput.canSetValue(((SiteModel.Base)siteModel).substModelInput.get(), (SiteModel.Base) target)) {
					throw new IllegalArgumentException("Cannot link site model: substitution models (" + 
							((SiteModel.Base)target).substModelInput.get().getClass().toString() + " and " +
							((SiteModel.Base)siteModel).substModelInput.get().getClass().toString() +
							") are incompatible");
				}
			} else {
				throw new IllegalArgumentException("Don't know how to link this site model");
			}
			needsRePartition = (this.likelihoods[rowNr].siteModelInput.get() != siteModel);
			this.likelihoods[rowNr].siteModelInput.setValue(siteModel, this.likelihoods[rowNr]);

			partition = ((BEASTInterface)likelihoods[rowNr].siteModelInput.get()).getID();
			partition = BeautiDoc.parsePartition(partition);
			getDoc().setCurrentPartition(BeautiDoc.SITEMODEL_PARTITION, rowNr, partition);
		}
			break;
		case CLOCKMODEL_COLUMN: {
			BranchRateModel clockModel = null;
			if (treeLikelihood != null) { 
				clockModel = treeLikelihood.branchRateModelInput.get();
			} else {
				clockModel = getDoc().getClockModel(partition);
				if (clockModel != likelihoods[rowNr].branchRateModelInput.get()) {
					PartitionContext context = getPartitionContext(rowNr);
					try {
						clockModel = (BranchRateModel) BeautiDoc.deepCopyPlugin(likelihoods[rowNr].branchRateModelInput.get(),
							likelihoods[rowNr], (MCMC) doc.mcmc.get(), oldContext, context, doc, null);
					} catch (RuntimeException e) {
						Alert.showMessageDialog(this, "Could not clone clock model: " + e.getMessage());
						return;
					}
				}
			}
			// make sure that *if* the clock model has a tree as input, it is
			// the same as
			// for the likelihood
			TreeInterface tree = null;
			for (Input<?> input : ((BEASTInterface) clockModel).listInputs()) {
				if (input.getName().equals("tree")) {
					tree = (TreeInterface) input.get();
				}

			}
			if (tree != null && tree != this.likelihoods[rowNr].treeInput.get()) {
				Alert.showMessageDialog(this, "Cannot link clock model with different trees");
				throw new IllegalArgumentException("Cannot link clock model with different trees");
			}

			needsRePartition = (this.likelihoods[rowNr].branchRateModelInput.get() != clockModel);
			this.likelihoods[rowNr].branchRateModelInput.setValue(clockModel, this.likelihoods[rowNr]);
			partition = likelihoods[rowNr].branchRateModelInput.get().getID();
			partition = BeautiDoc.parsePartition(partition);
			getDoc().setCurrentPartition(BeautiDoc.CLOCKMODEL_PARTITION, rowNr, partition);
		}
			break;
		case TREE_COLUMN: {
			TreeInterface tree = null;
			if (treeLikelihood != null) { 
				tree = treeLikelihood.treeInput.get();
			} else {
				tree = (TreeInterface) doc.pluginmap.get("Tree.t:" + partition);
				if (tree != likelihoods[rowNr].treeInput.get()) {
					PartitionContext context = getPartitionContext(rowNr);
					try {
						tree = (TreeInterface) BeautiDoc.deepCopyPlugin((BEASTInterface) likelihoods[rowNr].treeInput.get(), likelihoods[rowNr],
							(MCMC) doc.mcmc.get(), oldContext, context, doc, null);
					} catch (RuntimeException e) {
						Alert.showMessageDialog(this, "Could not clone tree model: " + e.getMessage());
						return;
					}
					
					State state = ((MCMC) doc.mcmc.get()).startStateInput.get();
					List<StateNode> stateNodes = new ArrayList<>();
					stateNodes.addAll(state.stateNodeInput.get());
					for (StateNode s : stateNodes) {
						if (s.getID().endsWith(".t:" + oldContext.tree) && !(s instanceof TreeInterface)) {
							try {
								@SuppressWarnings("unused")
								StateNode copy = (StateNode) BeautiDoc.deepCopyPlugin(s, likelihoods[rowNr], (MCMC) doc.mcmc.get(), oldContext, context, doc, null);
							} catch (RuntimeException e) {
								Alert.showMessageDialog(this, "Could not clone tree model: " + e.getMessage());
								return;
							}

						}
					}
				}
			}
			// sanity check: make sure taxon sets are compatible
			Taxon.assertSameTaxa(tree.getID(), tree.getTaxonset().getTaxaNames(),
					likelihoods[rowNr].dataInput.get().getID(), likelihoods[rowNr].dataInput.get().getTaxaNames());

			needsRePartition = (this.likelihoods[rowNr].treeInput.get() != tree);
			Log.warning.println("needsRePartition = " + needsRePartition);			
			if (needsRePartition) {
				TreeInterface oldTree = this.likelihoods[rowNr].treeInput.get();
				List<TreeInterface> tModels = new ArrayList<>();
				for (GenericTreeLikelihood likelihood : likelihoods) {
					if (likelihood.treeInput.get() == oldTree) {
						tModels.add(likelihood.treeInput.get());
					}
				}
				if (tModels.size() == 1) {
					// remove old tree from model
					((BEASTInterface)oldTree).setInputValue("estimate", false);
                	// use toArray to prevent ConcurrentModificationException
					for (Object beastObject : BEASTInterface.getOutputs(oldTree).toArray()) { //.toArray(new BEASTInterface[0])) {
						for (Input<?> input : ((BEASTInterface)beastObject).listInputs()) {
							try {
							if (input.get() == oldTree) {
								if (input.getRule() != Input.Validate.REQUIRED) {
									input.setValue(tree/*null*/, (BEASTInterface) beastObject);
								//} else {
									//input.setValue(tree, (BEASTInterface) beastObject);
								}
							} else if (input.get() instanceof List) {
								@SuppressWarnings("unchecked")
								List<TreeInterface> list = (List<TreeInterface>) input.get();
								if (list.contains(oldTree)) { // && input.getRule() != Validate.REQUIRED) {
									list.remove(oldTree);
									if (!list.contains(tree)) {
										list.add(tree);
									}
								}
							}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			likelihoods[rowNr].treeInput.setValue(tree, likelihoods[rowNr]);
			partition = likelihoods[rowNr].treeInput.get().getID();
			partition = BeautiDoc.parsePartition(partition);
			getDoc().setCurrentPartition(BeautiDoc.TREEMODEL_PARTITION, rowNr, partition);
		}
		}
		tableData[rowNr][columnNr] = partition;
		if (needsRePartition) {
			List<BeautiSubTemplate> templates = new ArrayList<>();
			templates.add(doc.beautiConfig.partitionTemplate.get());
			templates.addAll(doc.beautiConfig.subTemplates);
			// keep applying rules till model does not change
			doc.setUpActivePlugins();
			int n;
			do {
				n = doc.posteriorPredecessors.size();
				doc.applyBeautiRules(templates, false, oldContext);
				doc.setUpActivePlugins();
			} while (n != doc.posteriorPredecessors.size());
			doc.determinePartitions();
		}
		if (treeLikelihood == null) {
			initTableData();
			setUpComboBoxes();
		}
		
		updateStatus();
	}

	private void setTableEntry(int rowNr, int columnNr, Object o) {
		tableData[rowNr][columnNr] = o;
		if (o instanceof BEASTInterface) {
			switch (columnNr) {
			case NAME_COLUMN: tableEntries.get(rowNr).setName(((BEASTInterface)o).getID());break;
			case FILE_COLUMN: tableEntries.get(rowNr).setFile(((BEASTInterface)o).getID());break;
			case TAXA_COLUMN: tableEntries.get(rowNr).setTaxa(((BEASTInterface)o).getID());break;
			case SITES_COLUMN: tableEntries.get(rowNr).setSites(((BEASTInterface)o).getID());break;
			case TYPE_COLUMN: tableEntries.get(rowNr).setDataType(((BEASTInterface)o).getID());break;
			case SITEMODEL_COLUMN: tableEntries.get(rowNr).setSiteModel(((BEASTInterface)o).getID());break;
			case CLOCKMODEL_COLUMN: tableEntries.get(rowNr).setClockModel(((BEASTInterface)o).getID());break;
			case TREE_COLUMN: tableEntries.get(rowNr).setTree(((BEASTInterface)o).getID());break;
			// case USE_AMBIGUITIES_COLUMN: tableEntries.get(rowNr).setAmbiguities(((BEASTInterface)o).getID());break;
			}
		} else {
			switch (columnNr) {
			case NAME_COLUMN: tableEntries.get(rowNr).setName(o.toString());break;
			case FILE_COLUMN: tableEntries.get(rowNr).setFile(o.toString());break;
			case TAXA_COLUMN: tableEntries.get(rowNr).setTaxa(o.toString());break;
			case SITES_COLUMN: tableEntries.get(rowNr).setSites(o.toString());break;
			case TYPE_COLUMN: tableEntries.get(rowNr).setDataType(o.toString());break;
			case SITEMODEL_COLUMN: tableEntries.get(rowNr).setSiteModel(o.toString());break;
			case CLOCKMODEL_COLUMN: tableEntries.get(rowNr).setClockModel(o.toString());break;
			case TREE_COLUMN: tableEntries.get(rowNr).setTree(o.toString());break;
			// case USE_AMBIGUITIES_COLUMN: tableEntries.get(rowNr).setAmbiguities(o.toString());break;
			}
		}
	}

	private PartitionContext getPartitionContext(int rowNr) {
		PartitionContext context = new PartitionContext(
				tableData[rowNr][NAME_COLUMN].toString(),
				tableData[rowNr][SITEMODEL_COLUMN].toString(),
				tableData[rowNr][CLOCKMODEL_COLUMN].toString(),
				tableData[rowNr][TREE_COLUMN].toString());
		return context;
	}

	@Override
	protected void addInputLabel() {
	}

	void initTableData() {
		List<Integer> selected = getTableRowSelection();
		boolean b = updateInProgress;
		updateInProgress = true;
		this.likelihoods = new GenericTreeLikelihood[partitionCount];
		if (tableData == null) {
			tableData = new Object[partitionCount][NR_OF_COLUMNS];
		}
		CompoundDistribution likelihoods = (CompoundDistribution) doc.pluginmap.get("likelihood");

		List<Partition0> list = new ArrayList<>();
		for (int i = 0; i < partitionCount; i++) {
			Alignment data = alignments.get(i);
			// partition name
			//setTableEntry(i, NAME_COLUMN, data);
			tableData[i][NAME_COLUMN] = data;

			// alignment name
			if (data instanceof FilteredAlignment) {
				tableData[i][FILE_COLUMN] = ((FilteredAlignment) data).alignmentInput.get();
			} else {
				tableData[i][FILE_COLUMN] = data;
			}
			// # taxa
			tableData[i][TAXA_COLUMN] = data.getTaxonCount();
			// # sites
			tableData[i][SITES_COLUMN] = data.getSiteCount();
			// Data type
			tableData[i][TYPE_COLUMN] = data.getDataType();
			// site model
			GenericTreeLikelihood likelihood = (GenericTreeLikelihood) likelihoods.pDistributions.get().get(i);
			assert (likelihood != null);
			this.likelihoods[i] = likelihood;
			tableData[i][SITEMODEL_COLUMN] = getPartition(likelihood.siteModelInput);
			// clock model
			tableData[i][CLOCKMODEL_COLUMN] = getPartition(likelihood.branchRateModelInput);
			// tree
			tableData[i][TREE_COLUMN] = getPartition(likelihood.treeInput);
			// useAmbiguities
			tableData[i][USE_AMBIGUITIES_COLUMN] = null;
			try {
				if (hasUseAmbiguitiesInput(i)) {
					tableData[i][USE_AMBIGUITIES_COLUMN] = likelihood.getInputValue("useAmbiguities");
				}
			} catch (Exception e) {
				// ignore
			}
			list.add(new Partition0(likelihood, i));
		}
		tableEntries = FXCollections.observableArrayList(list);
		table.setItems(tableEntries);
		table.refresh();
		updateInProgress = b;

		for (int i : selected) {
			table.getSelectionModel().select(i);
		}
		
        try {
        	
//        	Parent root = getScene().getRoot();
//        	Bounds localRootBounds = root.getBoundsInLocal();
//        	Point2D	 localRootTopLeft = new Point2D(localRootBounds.getMinX(), localRootBounds.getMinY());
//        	Point2D screenRootTopLeft = root.localToScreen(localRootTopLeft);

            table.setMinSize(doc.beauti.frame.getWidth() - 12, doc.beauti.frame.getHeight() - 160);
        	// System.err.println("table.setMinSize(" + (doc.beauti.frame.getWidth()-12)+","+ (doc.beauti.frame.getHeight()-230) +")");

//        	if (!Double.isNaN(doc.beauti.frame.getWidth())) {
//        		table.setMinSize(doc.beauti.frame.getWidth() - 20, doc.beauti.frame.getHeight()-160);
//        	} else {
//            	table.setMinSize(1024 - 20, 768 - 138);
//        	}
        } catch (NullPointerException e) {
        	// ignore
        	// System.err.println("table.setMinSize(1024 - 20, 768 - 138)");
        	table.setMinSize(1024 - 20, 768 - 138);
        }

	}
		
	private boolean hasUseAmbiguitiesInput(int i) {
		try {
			for (Input<?> input : likelihoods[i].listInputs()) {
				if (input.getName().equals("useAmbiguities")) {
					return true;
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

	private String getPartition(Input<?> input) {
		BEASTInterface beastObject = (BEASTInterface) input.get();
		String id = beastObject.getID();
		String partition = BeautiDoc.parsePartition(id);
		return partition;
	}

	
	
	public class Partition0 {
		private SimpleStringProperty name, file, taxa, sites, dataType, siteModel, clockModel, tree;
		private SimpleBooleanProperty ambiguities;
		private int row;
		
		public Partition0(GenericTreeLikelihood likelihood, int row)  {
			this.row = row;
			Alignment data = likelihood.dataInput.get();
			String id = likelihood.getID();
			name = new SimpleStringProperty(id.indexOf('.') > 0 ? 
					id.substring(id.indexOf('.')+1) :
					id);
			if (data instanceof FilteredAlignment) {
				file = new SimpleStringProperty(((FilteredAlignment) data).alignmentInput.get().getID());
			} else {
				file = new SimpleStringProperty(data.getID());
			}
			// # taxa
			taxa = new SimpleStringProperty(data.getTaxonCount() + "");
			// # sites
			sites = new SimpleStringProperty(data.getSiteCount() + "");
			// Data type
			dataType = new SimpleStringProperty(data.getDataType().toString());
			// site model
			// this.likelihoods[i] = likelihood;
			siteModel = new SimpleStringProperty(getPartition(likelihood.siteModelInput));
			// clock model
			clockModel = new SimpleStringProperty(getPartition(likelihood.branchRateModelInput));
			// tree
			tree = new SimpleStringProperty(getPartition(likelihood.treeInput));
			// useAmbiguities
			ambiguities = new SimpleBooleanProperty();
			try {
				ambiguities.set((Boolean) likelihood.getInputValue("useAmbiguities"));
			} catch (Exception e) {
				// ignore
			}
			ambiguities.addListener((obs,  wasSelected,  isSelected) -> {
			    if (likelihood.getInputs().containsKey("useAmbiguities")) {
			        Input<?> input = likelihood.getInput("useAmbiguities");
			        input.setValue(isSelected, likelihood);
			    }
			});	
		}
		
		public int getRow() {return row;}
		public String getName() {
			return name.get();
		}

		public void setName(String name) {
			this.name.set(name);
		}

		public String getFile() {
			return file.get();
		}

		public void setFile(String file) {
			this.file.set(file);
		}

		public String getTaxa() {
			return taxa.get();
		}

		public void setTaxa(String taxa) {
			this.taxa.set(taxa);
		}

		public String getSites() {
			return sites.get();
		}

		public void setSites(String sites) {
			this.sites.set(sites);
		}

		public String getDataType() {
			return dataType.get();
		}

		public void setDataType(String dataType) {
			this.dataType.set(dataType);
		}

		public String getSiteModel() {
			return siteModel.get();
		}

		public void setSiteModel(String siteModel) {
			this.siteModel.set(siteModel);
		}

		public String getClockModel() {
			return clockModel.get();
		}

		public void setClockModel(String clockModel) {
			this.clockModel.set(clockModel);
		}

		public String getTree() {
			return tree.get();
		}

		public void setTree(String tree) {
			this.tree.set(tree);
		}

		public Boolean getAmbiguities() {
			return ambiguities.getValue();
		}

		public void setAmbiguities(Boolean ambiguities) {
			this.ambiguities.setValue(ambiguities);
		}
		
		public StringProperty siteModelProperty() {return siteModel;}
		public StringProperty clockModelProperty() {return clockModel;}
		public StringProperty treeProperty() {return tree;}
		public BooleanProperty ambiguitiesProperty() { return ambiguities; }

	}
	
	protected TableView<Partition0> createListBox() {
		String[] columnData = new String[] { "Name", "File", "Taxa", "Sites", "Data Type", "Site Model", "Clock Model",
				"Tree", "Ambiguities"};

		// set up table.
		// special features: background shading of rows
		// custom editor allowing only Date column to be edited.
		table = new TableView<Partition0>();
		table.setPlaceholder(new Label("No partitions loaded yet"));

		for (int i = 0; i < columnData.length; i++) {
			TableColumn<Partition0, String> col = new TableColumn<>(columnData[i]);
			String str = columnData[i].substring(0,1).toLowerCase() + columnData[i].substring(1);
			str = str.replaceAll(" ","");
			if (i != 5 && i!= 6 && i != 7)
				col.setCellValueFactory(new PropertyValueFactory<>(str));
			table.getColumns().add(col);
		}


        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().selectedItemProperty().addListener(
        	    (observable, oldValue, newValue) -> {
        	    	updateStatus();
        	    }
        		);
		table.setId("alignmenttable");

		// make columns 0, 5, 6 and 7 editable
		for (int i : new int[] {0, 5, 6, 7}) {
			TableColumn<Partition0, ?> tblColID = table.getColumns().get(i);
			((TableColumn)tblColID).setCellFactory(TextFieldTableCell.forTableColumn());
			tblColID.setOnEditCommit(e->{
				processPartitionName(e);
			});
		}

		setUpComboBoxes();
		
		// make column 9 use checkboxes
		if (table.getColumns().size() > 8) {
		TableColumn<Partition0, Boolean> ambiguitiesColumn = (TableColumn<Partition0, Boolean>) table.getColumns().get(8);
		ambiguitiesColumn.setCellValueFactory( f -> f.getValue().ambiguitiesProperty());
		ambiguitiesColumn.setCellFactory( tc -> new CheckBoxTableCell<>());
		

		}

		// show alignment viewer when double clicking a row
		table.setOnMouseClicked(e->{
			if (e.getClickCount() > 1) {
				try {
					int alignmemt = table.getSelectionModel().getSelectedIndex();//table.rowAtPoint(e.getPoint());
					Alignment alignment = alignments.get(alignmemt);
					int best = 0;
					BeautiAlignmentProvider provider = null;
					for (BeautiAlignmentProvider provider2 : doc.beautiConfig.alignmentProvider) {
						int match = provider2.matches(alignment);
						if (match > best) {
							best = match;
							provider = provider2;
						}
					}
					provider.editAlignment(alignment, doc);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				updateStatus();
			} else if (e.isMiddleButtonDown()) {
				int alignmemt = table.getSelectionModel().getSelectedIndex();// table.rowAtPoint(e.getPoint());
				Alignment alignment = alignments.get(alignmemt);
				ButtonType result = Alert.showConfirmDialog(null, "Do you want to replace alignment " + alignment.getID(), "Replace alignment?", Alert.YES_NO_OPTION);
				if (result == Alert.YES_OPTION) {
					replaceItem(alignment);
				}
			}			
		});
		
		initTableData();

		return table;
	} // createListBox
	

	private void processPartitionName(CellEditEvent<Partition0, ?> e) {
		String newId = (String) e.getNewValue();
		int row = e.getTablePosition().getRow();
		int col = e.getTablePosition().getColumn();
		processPartitionName(row, col, newId);
	}

	void setUpComboBoxes() {
		// set up comboboxes
		@SuppressWarnings("unchecked")
		Set<String>[] partitionNames = new HashSet[3];
		for (int i = 0; i < 3; i++) {
			partitionNames[i] = new HashSet<>();
		}
		if (partitionCount > 0 && likelihoods == null) {
			initTableData();
		}
		for (int i = 0; i < partitionCount; i++) {
			partitionNames[0].add(((BEASTInterface) likelihoods[i].siteModelInput.get()).getID());
			partitionNames[1].add(likelihoods[i].branchRateModelInput.get().getID());
			partitionNames[2].add(likelihoods[i].treeInput.get().getID());
		}
		String[][] partitionNameStrings = new String[3][];
		for (int i = 0; i < 3; i++) {
			partitionNameStrings[i] = partitionNames[i].toArray(new String[0]);
		}
		for (int j = 0; j < 3; j++) {
			for (int i = 0; i < partitionNameStrings[j].length; i++) {
				partitionNameStrings[j][i] = BeautiDoc.parsePartition(partitionNameStrings[j][i]);
			}
		}
		
        table.setEditable(true);
		
		String [] colName = {"Site Model", "Clock Model", "Tree"};
		int [] colPos = {SITEMODEL_COLUMN, CLOCKMODEL_COLUMN, TREE_COLUMN};
		
		for (int k = 0; k < 3; k++) {
			TableColumn<Partition0, StringProperty> column = new TableColumn<>(colName[k]);
	        switch (k) {
	        case 0:
	        	column.setCellValueFactory(i -> {
			        final StringProperty value = i.getValue().siteModelProperty();
			        return Bindings.createObjectBinding(() -> value);
			    });
			    column.setCellFactory(col -> {
			        final ComboBox<String> comboBox = new ComboBox<>();
			        comboBox.setMaxWidth(150);
			        TableCell<Partition0, StringProperty> c = new TableCell<Partition0, StringProperty>() {
			        	@Override
			        	protected void updateItem(StringProperty item, boolean empty) {
							super.updateItem(item, empty);
							setVisible(!empty);
							if (!empty) {
								comboBox.getSelectionModel().select(item.get());
								comboBox.setUserData(item);
						        comboBox.setOnKeyPressed(k->{
						        	if (k.getCode().equals(KeyCode.ENTER) || k.getCode().equals(KeyCode.TAB)) {
						        		comboActionListener(comboBox, item, SITEMODEL_COLUMN);
						        	}
						        });
							}
						}
			        };
			        comboBox.setOnAction(e -> {
			        	action(e, SITEMODEL_COLUMN);
			        });
			        comboBox.getItems().addAll(partitionNameStrings[0]);
			        comboBox.setEditable(true);
			        c.setId("siteModelCell");
			        c.setGraphic(comboBox);
			        return c;
			    });
			    
			    break;
	    	case 1: 
			    column.setCellValueFactory(i -> {
			        final StringProperty value = i.getValue().clockModelProperty();
			        return Bindings.createObjectBinding(() -> value);
			    });
			    column.setCellFactory(col -> {
			        final ComboBox<String> comboBox = new ComboBox<>();
			        comboBox.setMaxWidth(150);
			        TableCell<Partition0, StringProperty> c = new TableCell<Partition0, StringProperty>() {
			        	@Override
			        	protected void updateItem(StringProperty item, boolean empty) {
							super.updateItem(item, empty);
							setVisible(!empty);
							if (!empty) {
								comboBox.getSelectionModel().select(item.get());
						        comboBox.setUserData(item);
						        comboBox.setOnKeyPressed(k->{
						        	if (k.getCode().equals(KeyCode.ENTER) || k.getCode().equals(KeyCode.TAB)) {
						        		comboActionListener(comboBox, item, CLOCKMODEL_COLUMN);
						        	}
						        });
							}
						}
			        };
			        comboBox.setOnAction(e -> {
			        	action(e, CLOCKMODEL_COLUMN);
			        });
			        comboBox.getItems().addAll(partitionNameStrings[1]);
			        comboBox.setEditable(true);

			        c.setId("clockModelCell");
			        c.setGraphic(comboBox);
			        return c;
			    });
			    break;
	    	case 2: 
	        	
			    column.setCellValueFactory(i -> {
			        final StringProperty value = i.getValue().treeProperty();
			        return Bindings.createObjectBinding(() -> value);
			    });
			    column.setCellFactory(col -> {
			        final ComboBox<String> comboBox = new ComboBox<>();
			        comboBox.setMaxWidth(150);
			        TableCell<Partition0, StringProperty> c = new TableCell<Partition0, StringProperty>() {
			        	@Override
			        	protected void updateItem(StringProperty item, boolean empty) {
							super.updateItem(item, empty);
							setVisible(!empty);
							if (!empty) {
								comboBox.getSelectionModel().select(item.get());
						        comboBox.setUserData(item);
						        comboBox.setOnKeyPressed(k->{
						        	if (k.getCode().equals(KeyCode.ENTER) || k.getCode().equals(KeyCode.TAB)) {
						        		comboActionListener(comboBox, item, TREE_COLUMN);
						        	}
						        });
							}
						}
			        };
			        comboBox.setOnAction(e -> {
			        	action(e, TREE_COLUMN);
			        });
			        comboBox.getItems().addAll(partitionNameStrings[2]);
			        comboBox.setEditable(true);

			        c.setId("treeModelCell");
			        c.setGraphic(comboBox);
			        return c;
			    });
			}
		    
			
			table.getColumns().set(colPos[k], column);
		}
		
		
		
		
		
		
		
		

	}

	private void action(ActionEvent e, int column) {
    	ComboBox<String> comboBox2 = (ComboBox<String>) e.getTarget();
    	Node o = (Node) comboBox2.getParent().getParent();
    	
    	if (o == null) {
    		return;
    	}
    	TableRow<String> row = (TableRow) o;
    	Partition0 partition0 = tableEntries.get(row.getIndex());

    	String newValue = comboBox2.getValue();
    	if (tableData[partition0.getRow()][column].equals(newValue)) {
    		// nothing to do
    		return;
    	}
		tableData[partition0.getRow()][column] = newValue;
		switch (column) {
			case SITEMODEL_COLUMN: 
				partition0.siteModelProperty().setValue(newValue);
				break;
			case CLOCKMODEL_COLUMN: 
				partition0.clockModelProperty().setValue(newValue); 
				break;
			case TREE_COLUMN: 
				partition0.treeProperty().setValue(newValue);
				break;
		}
		
		for (int i = 0; i < partitionCount; i++) {
			try {
				updateModel(column, i);
			} catch (Exception ex) {
				Log.warning.println(ex.getMessage());
			}
		}	
	}

	synchronized private void comboActionListener(ComboBox comboBox /*ActionEvent e, */,StringProperty item, int column) {		
		Partition0 partition0 = null;
		for (Partition0 partition : tableEntries) {
			if (partition.siteModel == item ||
					partition.clockModelProperty() == item ||
					partition.tree == item) {
				partition0 = partition;
			}
		}
		
		String newValue = (String) comboBox.getEditor().getText();		
		String oldValue = (String) item.get();
		
		if (!oldValue.equals(newValue)) {
			tableData[partition0.getRow()][column] = newValue;
			switch (column) {
			case SITEMODEL_COLUMN: partition0.siteModelProperty().setValue(newValue); break;
			case CLOCKMODEL_COLUMN: partition0.clockModelProperty().setValue(newValue); break;
			case TREE_COLUMN: partition0.treeProperty().setValue(newValue); break;
			}
			
			for (int i = 0; i < partitionCount; i++) {
				try {
					updateModel(column, i);
				} catch (Exception ex) {
					Log.warning.println(ex.getMessage());
				}
			}	
		}
	}

	void processPartitionName(int row, int col, String newName) {
		Log.warning.println("processPartitionName");
		Log.warning.println(col + " " + row);
		String oldName = tableData[row][col].toString();
		if (!oldName.equals(newName) && newName.indexOf(".") >= 0) {
			// prevent full stops to be used in IDs
			newName = newName.replaceAll("\\.", "");
		}
		if (!oldName.equals(newName)) {
			try {
				int partitionID = -2;
				switch (col) {
				case NAME_COLUMN:
					partitionID = BeautiDoc.ALIGNMENT_PARTITION;
					tableEntries.get(row).setName(newName);
					break;
				case SITEMODEL_COLUMN:
					partitionID = BeautiDoc.SITEMODEL_PARTITION;
					tableEntries.get(row).setSiteModel(newName);
					break;
				case CLOCKMODEL_COLUMN:
					partitionID = BeautiDoc.CLOCKMODEL_PARTITION;
					tableEntries.get(row).setClockModel(newName);
					break;
				case TREE_COLUMN:
					partitionID = BeautiDoc.TREEMODEL_PARTITION;
					tableEntries.get(row).setTree(newName);
					break;
				default:
					throw new IllegalArgumentException("Cannot rename item in column");
				}
				getDoc().renamePartition(partitionID, oldName, newName);
				// table.setValueAt(newName, row, col);
				setUpComboBoxes();
			} catch (Exception e) {
				Alert.showMessageDialog(null, "Renaming failed: " + e.getMessage());
			}
		}
		// debugging code:
		//for (int i = 0; i < partitionCount; i++) {
		//	Log.warning.println(i + " " + tableData[i][0]);
		//}
	}


	@Override
	protected void addSingleItem(BEASTInterface beastObject) {
		initTableData();
		repaint();
	}

	@Override
	protected void addItem() {
		addItem(null);
	} // addItem

	private void addItem(File[] fileArray) {
		List<BEASTInterface> beastObjects = doc.beautiConfig.selectAlignments(doc, this, fileArray);

		updateInProgress = true;
		if (beastObjects != null) {
			for (BEASTInterface b : beastObjects) {
				GenericTreeLikelihood likelihood = null;
				for (BEASTInterface o : b.getOutputs()) {
					if (o instanceof GenericTreeLikelihood) {
						likelihood = (GenericTreeLikelihood) o;
						break;
					}
				}
				if (likelihood != null) {
					alignments.add(likelihood.dataInput.get());
				} else {
					// alignment maybe wrapped in a FilteredAlignment
					for (BEASTInterface o2 : b.getOutputs()) {
						if (o2 instanceof FilteredAlignment) {
							for (BEASTInterface o : ((FilteredAlignment) o2).getOutputs()) {
								if (o instanceof GenericTreeLikelihood) {
									likelihood = (GenericTreeLikelihood) o;
									alignments.add(likelihood.dataInput.get());
									break;
								}
							}
						}
					}
				}
			}
			partitionCount = alignments.size();
			tableData = null;
			initTableData();
	    	table.refresh();
			refreshPanel();
		}
		updateInProgress = false;
	}

	void delItem() {
		List<Integer> selected = getTableRowSelection();
		if (selected.size() == 0) {
			Alert.showMessageDialog(this, "Select partitions to delete, before hitting the delete button");
		}
		// do the actual deleting
		for (int i = selected.size() - 1; i >= 0; i--) {
			int rowNr = selected.get(i);
			
			// before deleting, unlink site model, clock model and tree
			
			// check whether any of the models are linked
			beast.base.spec.evolution.branchratemodel.Base clockModel = likelihoods[rowNr].branchRateModelInput.get();
			SiteModelInterface siteModel = likelihoods[rowNr].siteModelInput.get();
			TreeInterface tree = likelihoods[rowNr].treeInput.get();
			List<GenericTreeLikelihood> cModels = new ArrayList<>();
			List<GenericTreeLikelihood> models = new ArrayList<>();
			List<GenericTreeLikelihood> tModels = new ArrayList<>();
			for (GenericTreeLikelihood likelihood : likelihoods) {
				if (likelihood != likelihoods[rowNr]) {
				if (likelihood.branchRateModelInput.get() == clockModel) {
					cModels.add(likelihood);
				}
				if (likelihood.siteModelInput.get() == siteModel) {
					models.add(likelihood);
				}
				if (likelihood.treeInput.get() == tree) {
					tModels.add(likelihood);
				}
				}
			}
			
			try {
				if (cModels.size() > 0) {
					// clock model is linked, so we need to unlink
					if (doc.getPartitionNr(clockModel) != rowNr) {
						tableData[rowNr][CLOCKMODEL_COLUMN] = getDoc().partitionNames.get(rowNr).partition;
					} else {
						int freePartition = doc.getPartitionNr(cModels.get(0));
						tableData[rowNr][CLOCKMODEL_COLUMN] = getDoc().partitionNames.get(freePartition).partition;
					}
					updateModel(CLOCKMODEL_COLUMN, rowNr);
				}
				
				if (models.size() > 0) {
					// site model is linked, so we need to unlink
					if (doc.getPartitionNr((BEASTInterface) siteModel) != rowNr) {
						tableData[rowNr][SITEMODEL_COLUMN] = getDoc().partitionNames.get(rowNr).partition;
					} else {
						int freePartition = doc.getPartitionNr(models.get(0));
						tableData[rowNr][SITEMODEL_COLUMN] = getDoc().partitionNames.get(freePartition).partition;
					}
					updateModel(SITEMODEL_COLUMN, rowNr);
				}
				
				if (tModels.size() > 0) {
					// tree is linked, so we need to unlink
					if (doc.getPartitionNr((BEASTInterface) tree) != rowNr) {
						tableData[rowNr][TREE_COLUMN] = getDoc().partitionNames.get(rowNr).partition;
					} else {
						int freePartition = doc.getPartitionNr(tModels.get(0));
						tableData[rowNr][TREE_COLUMN] = getDoc().partitionNames.get(freePartition).partition;
					}
					updateModel(TREE_COLUMN, rowNr);
				}
				getDoc().delAlignmentWithSubnet(alignments.get(rowNr));
				alignments.remove(rowNr);
			    // remove deleted likelihood from likelihoods array
				GenericTreeLikelihood[] tmp = new GenericTreeLikelihood[likelihoods.length - 1];
				int k = 0;
				for (int j = 0; j < likelihoods.length; j++) {
					if (j != rowNr) {
						tmp[k] = likelihoods[j];
						k++;
					}
				}
				likelihoods = tmp;
				partitionCount--;
			} catch (Exception e) {
				Alert.showMessageDialog(null, "Deletion failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
		MRCAPriorInputEditor.customConnector(doc);
		refreshPanel();
	} // delItem

	@Override
    public void refreshPanel() {
		List<Integer> selected = table.getSelectionModel().getSelectedIndices();
		setUpComboBoxes();
		initTableData();
		for (int i : selected) {
			table.getSelectionModel().select(i);
		}
    }
	
	void replaceItem() {
		List<Integer> selected = getTableRowSelection();
		if (selected.size() != 1) {
			// don't know how to replace multiple alignments at the same time
			// should never get here (button is disabled)
			return;
		}
		Alignment alignment = alignments.get(selected.get(0));
		replaceItem(alignment);
	}
	
	private void replaceItem(Alignment alignment) {
		BeautiAlignmentProvider provider = new BeautiAlignmentProvider();
		List<BEASTInterface> list = provider.getAlignments(doc);
		List<Alignment> alignments = new ArrayList<>();
		for (BEASTInterface o : list) {
			if (o instanceof Alignment) {
				alignments.add((Alignment) o);
			}
		}
		Alignment replacement = null;
		if (alignments.size() > 1) {
			ComboBox<Alignment> jcb = new ComboBox<Alignment>();
			jcb.getItems().addAll(alignments.toArray(new Alignment[]{}));
			Alert.showMessageDialog( null, jcb, "Select a replacement alignment", Alert.QUESTION_MESSAGE);
			replacement = (Alignment) jcb.getValue();
		} else if (alignments.size() == 1) {
			replacement = alignments.get(0);
		}
		if (replacement != null) {
			if (!replacement.getDataType().getClass().getName().equals(alignment.getDataType().getClass().getName())) {
				Alert.showMessageDialog(null, "Data types do not match, so alignment cannot be replaced: " + 
						replacement.getID() + " " + replacement.getDataType().getClass().getName() + " != " + 
						alignment.getID() + " " + alignment.getDataType().getClass().getName());
				return;
			}
			// replace alignment
			Set<BEASTInterface> outputs = new LinkedHashSet<>();
			outputs.addAll(alignment.getOutputs());
			for (BEASTInterface o : outputs) {
				for (Input<?> input : o.listInputs()) {
					if (input.get() == alignment) {
						input.setValue(replacement, o);
						replacement.getOutputs().add(o);
					} else if (input.get() instanceof List) {
						@SuppressWarnings("rawtypes")
						List inputlist = (List) input.get();
						int i = inputlist.indexOf(alignment);
						if (i >= 0) {
							inputlist.set(i, replacement);
							replacement.getOutputs().add(o);
						}
					}
				}
			}
			int i = doc.alignments.indexOf(alignment);
			doc.alignments.set(i, replacement);
			refreshPanel();
		}
	} // replaceItem
	
	void splitItem() {
		List<Integer> selected = getTableRowSelection();
		if (selected.size() == 0) {
			Alert.showMessageDialog(this, "Select partitions to split, before hitting the split button");
			return;
		}
		String[] options = { "{1,2} + 3", "{1,2} + 3 frame 2", "{1,2} + 3 frame 3", "1 + 2 + 3", "1 + 2 + 3 frame 2", "1 + 2 + 3 frame 3"};

		String option = (String)Alert.showInputDialog(this, "Split selected alignments into partitions", "Option",
		                    Alert.WARNING_MESSAGE, null, options, "1 + 2 + 3");
		if (option == null) {
			return;
		}
		
		String[] filters = null;
		String[] ids = null;
		if (option.equals(options[0])) {
			filters = new String[] { "1::3,2::3", "3::3" };
			ids = new String[] { "_1,2", "_3" };
		} else if (option.equals(options[1])) {
			filters = new String[] { "1::3,3::3", "2::3" };
			ids = new String[] { "_1,2", "_3" };
		} else if (option.equals(options[2])) {
			filters = new String[] { "2::3,3::3", "1::3" };
			ids = new String[] { "_1,2", "_3" };
		} else if (option.equals(options[3])) {
			filters = new String[] { "1::3", "2::3", "3::3" };
			ids = new String[] { "_1", "_2", "_3" };
		} else if (option.equals(options[4])) {
			filters = new String[] { "2::3", "3::3", "1::3" };
			ids = new String[] { "_1", "_2", "_3" };
		} else if (option.equals(options[5])) {
			filters = new String[] { "3::3", "1::3", "2::3" };
			ids = new String[] { "_1", "_2", "_3" };
		} else {
			return;
		}

		for (int i = selected.size() - 1; i >= 0; i--) {
			int rowNr = selected.get(i);
			Alignment alignment = alignments.remove(rowNr);
			getDoc().delAlignmentWithSubnet(alignment);
			try {
				List<Alignment> newAlignments = new ArrayList<>();
				for (int j = 0; j < filters.length; j++) {
					FilteredAlignment f = new FilteredAlignment();
					f.initByName("data", alignment, "filter", filters[j], "dataType", alignment.dataTypeInput.get());
					f.setID(alignment.getID() + ids[j]);
					getDoc().addAlignmentWithSubnet(f, getDoc().beautiConfig.partitionTemplate.get());
					newAlignments.add(f);
				}
				alignments.addAll(newAlignments);
				partitionCount = alignments.size();
				tableData = null; 
				initTableData();
				if (newAlignments.size() == 2) {
					link(TREE_COLUMN, alignments.size() - 1, alignments.size() - 2);
				} else {
					link(TREE_COLUMN, alignments.size() - 2, alignments.size() - 3);
					tableData = null; 
					initTableData();			
					link(TREE_COLUMN, alignments.size() - 1, alignments.size() - 2);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		refreshPanel();
	} // splitItem

	/** enable/disable buttons, etc **/
	public void updateStatus() {
		boolean status = (alignments.size() > 1);
		if (alignments.size() >= 2 && getTableRowSelection().size() == 0) {
			status = false;
		}
		for (Button button : linkButtons) {
			button.setDisable(!status);
		}
		for (Button button : unlinkButtons) {
			button.setDisable(!status);
		}
		status = (getTableRowSelection().size() > 0);
		splitButton.setDisable(!status);
		delButton.setDisable(!status);
		replaceButton.setDisable(getTableRowSelection().size() != 1);
	}
	
} // class AlignmentListInputEditor

