package beastfx.app.inputeditor;



import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.spec.evolution.likelihood.GenericTreeLikelihood;
import beast.base.spec.evolution.sitemodel.SiteModel;
import beast.base.evolution.sitemodel.SiteModelInterface;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.MCMC;
import beast.base.parser.PartitionContext;
import beastfx.app.beauti.ClonePartitionPanel;
import beastfx.app.inputeditor.BeautiPanelConfig.Partition;
import beastfx.app.inputeditor.InputEditor.ExpandOption;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * panel making up each of the tabs in Beauti *
 */
public class BeautiPanel extends Tab implements ChangeListener, BeautiDocProvider {

	static int partitionListPreferredWidth = 120;

    private SplitPane splitPane;

    /**
     * document that this panel applies to *
     */
    BeautiDoc doc;

    @Override
    public BeautiDoc getDoc() {
        return doc;
    }
    
    @Override
    public BeautiPanelConfig getConfig() {
    	return config;
    }
    
    @Override
    public int getPartitionIndex() {
    	return partitionIndex;    			
    }
    
    /**
     * configuration for this panel *
     */
    public BeautiPanelConfig config;

    /**
     * panel number *
     */
    int panelIndex;

    /**
     * partition currently on display *
     */
    public int partitionIndex = 0;

    /**
     * box containing the list of partitions, to make (in)visible on update *
     */
    SplitPane pane;
    /**
     * list of partitions in m_listBox *
     */
    public ListView<String> listOfPartitions;
    /**
     * model for m_listOfPartitions *
     */
    // DefaultListModel<String> listModel;
    public ObservableList<String> listModel;
    
    ScrollPane scroller;

    /**
     * component containing main input editor *
     */
    Node centralComponent = null;

    public BeautiPanel() {
    }

    
    private void addPane(Node pane, int location) {
    	if (this.pane.getItems().size() > location) {
        	this.pane.getItems().set(location, pane);
    	} else {
    		this.pane.getItems().add(pane);
    	}
    }
    
    private Button hmcButton;
    
    public void setHMCVisible(boolean isVisible) {
    	if (hmcButton != null) {
    		hmcButton.setVisible(isVisible);
    	}
    }
    
    
    public BeautiPanel(int panelIndex, String text, BeautiDoc doc, BeautiPanelConfig config) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.doc = doc;
        this.panelIndex = panelIndex;
        pane = new SplitPane();
        //setLayout(new BorderLayout());

        this.config = config;
        if (this.config.hasPartition() != Partition.none &&
                doc.getPartitions(config.hasPartitionsInput.get().toString()).size() > 1) {
        	splitPane = pane;
            //splitPane = new SplitPane();
            //pane.setCenter(splitPane);
        } else {
            splitPane = null;
        }

        // refreshPanel();
        addPartitionPanel(this.config.hasPartition(), panelIndex);

        //setContent(pane);
        //setOpaque(false);
        
        HBox box = new HBox();
        Label label = new Label(text);
        //label.setPadding(new Insets(5,0,0,3));
        box.getChildren().add(label);
        //box.setPadding(new Insets(0));
        box.setSpacing(4);
        
        hmcButton = FXUtils.createHMCButton(doc.getTemplateName(), config.nameInput.get());
        if (hmcButton != null) {
        	hmcButton.setTooltip(new Tooltip("Click to help me choose"));
        	hmcButton.setVisible(false);
        	box.getChildren().add(hmcButton);
        }
        setGraphic(box);
        
    } // c'tor

    private void addPartitionPanel(Partition hasPartition, int panelIndex) {
        VBox box = FXUtils.newVBox();
        if (splitPane != null && hasPartition != Partition.none) {
            box.getChildren().add(createList());
        } else {
            return;
        }

        addPane(box, 0);
        // splitPane.getItems().add(box);
        if (listOfPartitions != null) {
            listOfPartitions.getSelectionModel().select(partitionIndex);
        }
        splitPane.setDividerPositions(0.2,0.8);
    }
    
    /**
     * Create a list of partitions and return as a JComponent;
     * @return
     */
    private Node createList() {
    	BorderPane partitionComponent = new BorderPane();
        // partitionComponent.setLayout(new BorderLayout());
        Label partitionLabel = new Label("Partition");
        partitionLabel.setAlignment(Pos.CENTER);
        partitionComponent.setTop(partitionLabel);//, BorderLayout.NORTH);
        listModel = FXCollections.observableArrayList();
        listOfPartitions = new ListView<>();//listModel);
        listOfPartitions.setId("listOfPartitions");
        listOfPartitions.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listOfPartitions.setItems(listModel);

        Dimension2D size = new Dimension2D(partitionListPreferredWidth, 300);
        //listOfPartitions.setFixedCellWidth(120);
//    	m_listOfPartitions.setSize(size);
        //listOfPartitions.setPrefSize(size);
    	listOfPartitions.setMinSize(size.getWidth(), size.getHeight());
//    	m_listOfPartitions.setBounds(0, 0, 100, 100);

        listOfPartitions.getSelectionModel().selectedItemProperty().addListener(this);
        partitionComponent.setCenter(listOfPartitions);
        updateList();
        return partitionComponent;

//        // AJD: This is unnecessary and not appropriate for Mac OS X look and feel
//        //listOfPartitions.setBorder(new BevelBorder(BevelBorder.RAISED));
//
//        ScrollPane listPane = new ScrollPane();
//        listPane.setContent(listOfPartitions);
//        partitionComponent.setCenter(listPane);
//        // AJD: This is unnecessary and not appropriate for Mac OS X look and feel
//        //partitionComponent.setBorder(new EtchedBorder());
//        return partitionComponent;
    }

    public void updateList() {
    	List<Integer> selected = null;
        if (listModel == null) {
            return;
        } else {
        	selected = new ArrayList<>();
        	selected.addAll(listOfPartitions.getSelectionModel().getSelectedIndices());	
        }
        listModel.clear();
        if (listModel.size() > 0) {
            // this is a weird bit of code, since listModel.clear should ensure that size()==0, but it doesn't
            return;
        }
        String type = config.hasPartitionsInput.get().toString();
        for (BEASTInterface partition : doc.getPartitions(type)) {
        	if (type.equals("SiteModel")) {
        		partition = (BEASTInterface) ((GenericTreeLikelihood) partition).siteModelInput.get();
        	} else if (type.equals("ClockModel")) {
        		partition = ((GenericTreeLikelihood) partition).branchRateModelInput.get();
        	} else if (type.equals("Tree")) {
        		partition = (BEASTInterface) ((GenericTreeLikelihood) partition).treeInput.get();
        	}
            String partitionID = partition.getID();
            partitionID = partitionID.substring(partitionID.lastIndexOf('.') + 1);
            if (partitionID.length() > 1 && partitionID.charAt(1) == ':') {
            	partitionID = partitionID.substring(2);
            }
            listModel.add(partitionID);
        }
        if (partitionIndex >= 0 && listModel.size() > 0) {
        	if (selected != null && selected.size() > 0) {
        		for (int index : selected) {
        			listOfPartitions.getSelectionModel().select(index);
        		}
        	} else {
        		listOfPartitions.getSelectionModel().select(partitionIndex);
        	}
        }
    }

    

    // AR remove globals (doesn't seem to be used anywhere)...
//	static BeautiPanel g_currentPanel = null;

    public void refreshPanel() throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (doc.alignments.size() == 0) {
            refreshInputPanel();
            setContent();
            return;
        }
        doc.scrubAll(true, false);

        // toggle splitpane
        if (splitPane == null && config.hasPartition() != Partition.none &&
                doc.getPartitions(config.hasPartitionsInput.get().toString()).size() > 1) {
            //splitPane = new SplitPane();//JSplitPane.HORIZONTAL_SPLIT);
            //pane = splitPane;
        	splitPane = pane;
            addPartitionPanel(config.hasPartition(), panelIndex);
        }
        if (splitPane != null && (config.hasPartition() == Partition.none ||
                doc.getPartitions(config.hasPartitionsInput.get().toString()).size() <= 1)) {
        	//((BorderPane)getContent()).setCenter(null);
        	//pane.getItems().remove(0);
            splitPane = null;
        }
        // setContent(pane);

        refreshInputPanel();
        setContent();

//		g_currentPanel = this;
    }
    
    private void setContent() {
        if (pane.getItems().size() > 1 /*partitionComponent != null*/ && 
        		config.getType() != null &&
        		this.config.hasPartition() != Partition.none) {
            // partitionComponent.setVisible(doc.getPartitions(config.getType()).size() > 1);
            if (doc.getPartitions(config.getType()).size() > 1) {
            	pane.setDividerPositions(0.2, 0.8);
            	// make splitpane the content of the tab
            	setContent(pane);
            } else {
            	pane.setDividerPositions(0, 1);
            	// make last item the content of the tab
            	setContent(pane.getItems().get(pane.getItems().size()-1));
            }
        } else {
        	pane.setDividerPositions(0, 1);
        	// make last item the content of the tab
        	setContent(pane.getItems().get(pane.getItems().size()-1));
        }
	}

	void refreshInputPanel(BEASTInterface beastObject, Input<?> input, boolean addButtons, InputEditor.ExpandOption forceExpansion) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (centralComponent != null && pane.getItems().size() > 1) {// ((BorderPane)getContent()) != null) {
        	// pane.getItems().remove(1);
        	// ((BorderPane)getContent()).setCenter(null);
        }
        if (input != null && input.get() != null && input.getType() != null) {
            InputEditor.ButtonStatus bs = config.buttonStatusInput.get();
            InputEditor inputEditor = doc.getInputEditorFactory().createInputEditor(input, beastObject, addButtons, forceExpansion, bs, null, doc);

            BorderPane p = new BorderPane();
            if (isToClone()) {
                ClonePartitionPanel clonePartitionPanel = new ClonePartitionPanel(this);
                p.setTop(clonePartitionPanel);//, BorderLayout.NORTH);
            } else {
                p.setCenter(inputEditor.getComponent());//, BorderLayout.CENTER);
            }

//            Rectangle bounds = new Rectangle(0,0);
//            if (scroller != null) {
//            	// get lastPaintPosition from viewport
//            	// HACK access it through its string representation
//	            JViewport v = scroller.getViewport();
//	            String vs = v.toString();
//	            int i = vs.indexOf("lastPaintPosition=java.awt.Point[x=");
//	            if (i > -1) {
//	            	i = vs.indexOf("y=", i);
//	            	vs = vs.substring(i+2, vs.indexOf("]", i));
//	            	i = Integer.parseInt(vs);
//	            } else {
//	            	i = 0;
//	            }
//	            bounds.y = -i;
//            }
//            scroller = new JScrollPane(p);
//            scroller.getViewport().scrollRectToVisible(bounds);
//            centralComponent = scroller;
            centralComponent = p;
        } else {
            centralComponent = new Label("No input editors.");
        }
        
        ScrollPane scroller = new ScrollPane();
        scroller.setContent(centralComponent);
        centralComponent = scroller;
        
        if (splitPane != null) {
            //BorderPane panel = new BorderPane();
            //panel.setTop(centralComponent);
            addPane(centralComponent, 1);
        } else {
            addPane(centralComponent, 1);
        }
    }

    void refreshInputPanel() throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        doc.currentInputEditors.clear();
        InputEditor.Base.g_nLabelWidth = config.labelWidthInput.get();
        BEASTInterface beastObject = config;
        final Input<?> input = config.resolveInput(doc, partitionIndex);

        boolean addButtons = config.addButtons();
        ExpandOption forceExpansion = config.forceExpansion();
        refreshInputPanel(beastObject, input, addButtons, forceExpansion);
    }

    /** 
     * Clones partition identified by sourceID to targetID and type (Site/Clock/Tree model)
     * as stored in config.
     * @param sourceID
     * @param targetID
     */
    public void cloneFrom(String sourceID, String targetID) {
    	if (sourceID.equals(targetID)) {
    		return;
    	}

    	String type = config.hasPartitionsInput.get().toString();
    	java.util.List<BEASTInterface> list = doc.getPartitions(type);
    	int source = -1, target = -1;
        for (int i = 0; i < list.size(); i++) {
        	BEASTInterface partition = list.get(i);
        	if (type.equals("SiteModel")) {
        		partition = (BEASTInterface) ((GenericTreeLikelihood) partition).siteModelInput.get();
        	} else if (type.equals("ClockModel")) {
        		partition = ((GenericTreeLikelihood) partition).branchRateModelInput.get();
        	} else if (type.equals("Tree")) {
        		partition = (BEASTInterface) ((GenericTreeLikelihood) partition).treeInput.get();
        	}
            String partitionID = partition.getID();
            partitionID = partitionID.substring(partitionID.lastIndexOf('.') + 1);
            if (partitionID.length() > 1 && partitionID.charAt(1) == ':') {
            	partitionID = partitionID.substring(2);
            }
            if (partitionID.equals(sourceID)) {
            	source = i;
            }
            if (partitionID.equals(targetID)) {
            	target = i;
            }
        } 
    	if (target == -1) {
    		throw new RuntimeException("Programmer error: sourceID and targetID should be in list");
    	}
    	
		CompoundDistribution likelihoods = (CompoundDistribution) doc.pluginmap.get("likelihood");
		
		GenericTreeLikelihood likelihoodSource = (GenericTreeLikelihood) likelihoods.pDistributions.get().get(source);
		GenericTreeLikelihood likelihood = (GenericTreeLikelihood) likelihoods.pDistributions.get().get(target);
		PartitionContext oldContext = doc.getContextFor(likelihoodSource);
		PartitionContext newContext = doc.getContextFor(likelihood);
		// this ensures the config.sync does not set any input value
		config._input.setValue(null, config);

    	if (type.equals("SiteModel")) {		
			SiteModelInterface siteModelSource = likelihoodSource.siteModelInput.get();
			SiteModelInterface  siteModel = null;
			try {
				siteModel = (SiteModel.Base) BeautiDoc.deepCopyPlugin((BEASTInterface) siteModelSource,
					likelihood, (MCMC) doc.mcmc.get(), oldContext, newContext, doc, null);
			} catch (RuntimeException e) {
				Alert.showMessageDialog(this.getContent() instanceof Pane ? ((Pane)this.getContent()) : null, 
						"Could not clone " + sourceID + " to " + targetID + " " + e.getMessage());
				return;
			}
			likelihood.siteModelInput.setValue(siteModel, likelihood);
			return;
    	} else if (type.equals("ClockModel")) {
    		BranchRateModel clockModelSource = likelihoodSource.branchRateModelInput.get();
    		BranchRateModel clockModel = null;
			try {
				clockModel = (BranchRateModel) BeautiDoc.deepCopyPlugin((BEASTInterface) clockModelSource,
						likelihood, (MCMC) doc.mcmc.get(), oldContext, newContext, doc, null);
			} catch (Exception e) {
				Alert.showMessageDialog(this.getContent() instanceof Pane ? ((Pane)this.getContent()) : null, 
						"Could not clone " + sourceID + " to " + targetID + " " + e.getMessage());
				return;
			}
			// make sure that *if* the clock model has a tree as input, it is
			// the same as for the likelihood
			TreeInterface tree = null;
			try {
				for (Input<?> input : ((BEASTInterface) clockModel).listInputs()) {
					if (input.getName().equals("tree")) {
						tree = (TreeInterface) input.get();
					}

				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (tree != null && tree != likelihood.treeInput.get()) {
				//likelihood.treeInput.setValue(tree, likelihood);
				Alert.showMessageDialog(null, "Cannot clone clock model with different trees");
				return;
			}

			likelihood.branchRateModelInput.setValue(clockModel, likelihood);
			return;
    	} else if (type.equals("Tree")) {
			TreeInterface tree = null;
			TreeInterface treeSource = likelihoodSource.treeInput.get();
			try {
			tree = (TreeInterface) BeautiDoc.deepCopyPlugin((BEASTInterface) treeSource, likelihood,
							(MCMC) doc.mcmc.get(), oldContext, newContext, doc, null);
				} catch (Exception e) {
					Alert.showMessageDialog(((Pane)this.getContent()), "Could not clone " + sourceID + " to " + targetID + " " + e.getMessage());
					return;
			}
			// sanity check: make sure taxon sets are compatible
            Taxon.assertSameTaxa(tree.getID(), tree.getTaxonset().getTaxaNames(),
                    likelihood.dataInput.get().getID(), likelihood.dataInput.get().getTaxaNames());

			likelihood.treeInput.setValue(tree, likelihood);
			return;

    	} else {
    		throw new RuntimeException("Programmer error calling cloneFrom: Should only clone Site/Clock/Tree model");
    	}
    } // cloneFrom

    private boolean isToClone() {
        return listOfPartitions != null && listOfPartitions.getSelectionModel().getSelectedIndices().size() > 1;
    }

//    public static boolean soundIsPlaying = false;
//
//    public static synchronized void playSound(final String url) {
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    synchronized (this) {
//                        if (soundIsPlaying) {
//                            return;
//                        }
//                        soundIsPlaying = true;
//                    }
//                    Clip clip = AudioSystem.getClip();
//                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(getClass().getResourceAsStream("/beastfx.app.beauti/" + url));
//                    clip.open(inputStream);
//                    clip.start();
//                    Thread.sleep(500);
//                    synchronized (this) {
//                        soundIsPlaying = false;
//                    }
//                } catch (Exception e) {
//                    soundIsPlaying = false;
//                    System.err.println(e.getMessage());
//                }
//            }
//        }).start();
//    }

//    @Override
//    public void valueChanged(ListSelectionEvent e) {
   	@Override
   	public void changed(ObservableValue observable, Object oldValue, Object newValue) {
        //System.err.print("BeautiPanel::valueChanged " + m_iPartition + " => ");
        if (observable != null) {
            config.sync(partitionIndex);
            if (listOfPartitions != null) {
                partitionIndex = Math.max(0, listOfPartitions.getSelectionModel().getSelectedIndex());
            }
        }
//        BeautiPanel.playSound("woosh.wav");
        //System.err.println(m_iPartition);
        try {
            refreshPanel();

//            centralComponent.repaint();
//            repaint();

            // hack to ensure m_centralComponent is repainted RRB: is there a better way???
//            if (Frame.getFrames().length == 0) {
//                // happens at startup
//                return;
//            }
//            Frame frame = Frame.getFrames()[Frame.getFrames().length - 1];
//            frame.setSize(frame.getSize());
            //Frame frame = frames[frames.length - 1];
//			Dimension size = frames[frames.length-1].getSize();
//			frames[frames.length-1].setSize(size);

//			m_centralComponent.repaint();
//			m_centralComponent.requestFocus();
            centralComponent.requestFocus();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

   	
   	public static List<Region> resizeList = new ArrayList<>();
   	
   	public void setWidth(Number oldVal, Number newVal) {
   		//pane.setPrefWidth(newVal.doubleValue());
		for (Region region : resizeList) {
			double w = region.getMinWidth();
			w += newVal.doubleValue() - oldVal.doubleValue();
			if (!Double.isNaN(w) && w > 0) {
				region.setMinWidth(w);
				System.err.println("new width: " + w + " " + region.getId() + " " + region.getClass().getName());
			}
		}
	}

   	public void setHeight(Number oldVal, Number newVal) {
   		//pane.setPrefWidth(newVal.doubleValue());
		for (Region region : resizeList) {
			double w = region.getMinHeight();
			w += newVal.doubleValue() - oldVal.doubleValue();
			if (!Double.isNaN(w) && w > 0) {
				region.setMinHeight(w);
				System.err.println("new width: " + w + " " + region.getId() + " " + region.getClass().getName());
			}
		}
	}

    
} // class BeautiPanel
