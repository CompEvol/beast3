package beastfx.app.inputeditor;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import beastfx.app.beauti.PriorInputEditor;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.pkgmgmt.BEASTClassLoader;

public class ListInputEditor extends InputEditor.Base {

    static public Image DOWN_ICON;
    static public Image RIGHT_ICON;

    {
        try {
        	DOWN_ICON = FXUtils.getIcon(BEASTObjectDialog.ICONPATH + "down.png").getImage();
        	RIGHT_ICON = FXUtils.getIcon(BEASTObjectDialog.ICONPATH + "right.png").getImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected ButtonStatus m_buttonStatus = ButtonStatus.ALL;

    /**
     * buttons for manipulating the list of inputs *
     */
    protected SmallButton addButton;
    protected List<TextField> m_entries;
    protected List<SmallButton> delButtonList;
    protected List<SmallButton> m_editButton;
    protected List<SmallLabel> m_validateLabels;
    protected VBox m_listBox;
    protected ExpandOption m_bExpandOption;

    // the box containing any buttons
    protected HBox buttonBox;

    static protected Set<String> g_collapsedIDs = new HashSet<>();
    static Set<String> g_initiallyCollapsedIDs = new HashSet<>();

    public abstract class ActionListenerObject implements EventHandler<ActionEvent> {
        public Object m_o;

        public ActionListenerObject(Object o) {
            super();
            m_o = o;
        }
    }

    //public ListInputEditor() {}
    public ListInputEditor(BeautiDoc doc) {
        super(doc);
        m_entries = new ArrayList<>();
        delButtonList = new ArrayList<>();
        m_editButton = new ArrayList<>();
        m_validateLabels = new ArrayList<>();
        m_bExpandOption = ExpandOption.FALSE;
        // setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public ListInputEditor() {
		super();
	}

	@Override
    public Class<?> type() {
        return ArrayList.class;
    }

    /**
     * return type of the list *
     */
    public Class<?> baseType() {
        return BEASTInterface.class;
    }

    /**
     * construct an editor consisting of
     * o a label
     * o a button for selecting another plug-in
     * o a set of buttons for adding, deleting, editing items in the list
     */
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_bExpandOption = isExpandOption;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr = -1;
        if (pane == null) { 
        	pane = new BorderPane();
        }
        addInputLabel();
        if (m_inputLabel != null) {
            //m_inputLabel.setMaxSize(m_inputLabel.getSize().width, 1000);
            //m_inputLabel.setAlignmentY(1.0f);
            //m_inputLabel.setVerticalAlignment(SwingConstants.TOP);
            //m_inputLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }

        m_listBox = FXUtils.newVBox();
        // list of inputs
        for (Object o : (List<?>) input.get()) {
            if (o instanceof BEASTInterface) {
                BEASTInterface beastObject2 = (BEASTInterface) o;
                addSingleItem(beastObject2);
            }
        }

        ((BorderPane)pane).setTop(m_listBox);

        buttonBox = FXUtils.newHBox();
        if (m_buttonStatus == ButtonStatus.ALL || m_buttonStatus == ButtonStatus.ADD_ONLY) {
            addButton = new SmallButton("+", true);
            addButton.setId("+");
            addButton.setTooltip(new Tooltip("Add item to the list"));
            addButton.setOnAction(e -> addItem());
            buttonBox.getChildren().add(addButton);
            if (!doc.isExpertMode()) {
                // if nothing can be added, make add button invisible
                List<String> tabuList = new ArrayList<>();
                for (int i = 0; i < m_entries.size(); i++) {
                    tabuList.add(m_entries.get(i).getText());
                }
                List<String> beastObjectNames = doc.getInputEditorFactory().getAvailablePlugins(m_input, m_beastObject, tabuList, doc);
                if (beastObjectNames.size() == 0) {
                    addButton.setVisible(false);
                }
            }
        }

        // add validation label at the end of a list
        m_validateLabel = new SmallLabel("x", "red");
        if (m_bAddButtons) {
            buttonBox.getChildren().add(m_validateLabel);
            m_validateLabel.setVisible(true);
            validateInput();
        }
        //buttonBox.getChildren().add(new Separator());
        m_listBox.getChildren().add(buttonBox);

        updateState();
        
        getChildren().add(pane);
//        // RRB: is there a better way to ensure lists are not spaced out across all available space?
//    	JFrame frame = doc.getFrame();
//    	if (frame != null) {
//    		m_listBox.add(Box.createVerticalStrut(frame.getHeight() - 150));
//    	}

    } // init

    protected void addSingleItem(BEASTInterface beastObject) {
        Pane itemBox0 = FXUtils.newVBox();
        Pane itemBox = FXUtils.newHBox();
        
        SmallButton editButton = new SmallButton("e", true, SmallButton.ButtonType.square);
        editButton.setId(beastObject.getID() + ".editButton");
        if (m_bExpandOption == ExpandOption.FALSE || m_bExpandOption == ExpandOption.IF_ONE_ITEM && ((List<?>) m_input.get()).size() > 1) {
            editButton.setTooltip(new Tooltip("Edit item in the list"));
			editButton.setOnAction(new ActionListenerObject(beastObject) {
				@Override
				public void handle(ActionEvent event) {
                    m_o = editItem(m_o);
				}
			});
        } else {
            editButton.setTooltip(new Tooltip("Expand/collapse item in the list"));
            editButton.setButtonType(SmallButton.ButtonType.toolbar);
        }
        m_editButton.add(editButton);
        itemBox.getChildren().add(editButton);

        InputEditor editor = addPluginItem(itemBox, beastObject);

        SmallLabel validateLabel = new SmallLabel("x", "red");
        itemBox.getChildren().add(validateLabel);
        validateLabel.setVisible(true);
        m_validateLabels.add(validateLabel);
        
//        m_listBox.getChildren().add(itemBox);
//        if (m_validateLabel == null) {
//        	rowCount++;
//            // m_listBox.add(itemBox, 0, rowCount);
//            m_listBox.getChildren().add(itemBox);
//        } else {
//        	rowCount++;
//        	Node c = m_listBox.getChildren().get(m_listBox.getChildren().size() - 1);
//            m_listBox.getChildren().remove(c);
//            //m_listBox.add(itemBox, 0, rowCount);
//            m_listBox.getChildren().add(itemBox);
//            m_listBox.getChildren().add(c);
//        }

        itemBox0.getChildren().add(itemBox);
        if (m_bExpandOption == ExpandOption.TRUE || m_bExpandOption == ExpandOption.TRUE_START_COLLAPSED ||
                (m_bExpandOption == ExpandOption.IF_ONE_ITEM && ((List<?>) m_input.get()).size() == 1)) {
        	VBox expandBox = createExpandBox(beastObject, editor, editButton);
        	
            editButton.setOnAction(new ExpandActionListener(expandBox, beastObject) {
                @Override
    			public void handle(ActionEvent e) {
                    SmallButton editButton = (SmallButton) e.getSource();
                    expandBox.setVisible(!expandBox.isVisible());
                    if (expandBox.isVisible()) {
                        try {
                        	editButton.setImg(DOWN_ICON);
                        }catch (Exception e2) {
    						// ignore
    					}
                        expandBox.setPrefHeight(USE_COMPUTED_SIZE);
                        expandBox.setMinHeight(m_box.getPrefHeight());
                        expandBox.setVisible(true);
                        expandBox.setManaged(true);
                        g_collapsedIDs.remove(m_beastObject.getID());
                    } else {
                    	try {
                    		editButton.setImg(RIGHT_ICON);
                        }catch (Exception e2) {
    						// ignore
    					}
                    	expandBox.setPrefHeight(0);
                    	expandBox.setMinHeight(0);
                    	expandBox.setVisible(false);
                    	expandBox.setManaged(false);
                        g_collapsedIDs.add(m_beastObject.getID());
                    }
                }
            });
            try {
    	        if (expandBox.isVisible()) {
    	            editButton.setImg(DOWN_ICON);
    	        } else {
    	            editButton.setImg(RIGHT_ICON);
    	        }
            } catch (Exception e) {
    			// TODO: handle exception
    		}
            itemBox0.getChildren().add(expandBox);
        } else {
            if (BEASTObjectPanel.countInputs(beastObject, doc) == 0) {
System.err.println("BEASTObjectPanel.countInputs(beastObject, doc) = 0");
                editButton.setVisible(false);
            }
        }
         
        m_listBox.getChildren().add(itemBox0);

        // FXUtils.createHMCButton(itemBox, beastObject, m_input);

    } // addSingleItem

    private VBox createExpandBox(BEASTInterface beastObject, InputEditor editor, SmallButton editButton) {
        VBox expandBox = FXUtils.newVBox();
        boolean editButtonIsVisible = updateExpandBox(doc, expandBox, beastObject, editor);

        if (editButtonIsVisible) {
        	m_listBox.getChildren().add(expandBox);
        } else {
            editButton.setVisible(false);
        }
        return expandBox;
	}

    public static boolean updateExpandBox(BeautiDoc doc, VBox expandBox, BEASTInterface beastObject, InputEditor editor) {
        //box.add(itemBox);
    	boolean editButtonIsVisible = true;

    	expandBox.getChildren().clear();
        List<InputEditor> editors = doc.getInputEditorFactory().addInputs(expandBox, beastObject, editor, null, doc);
        
        boolean addExpansionBox = editors.size() >= 1;
        //System.err.print(expandBox.getComponentCount());
        if (editors.size() == 1 && editors.get(0) instanceof BEASTObjectInputEditor) {
        	BEASTObjectInputEditor boie = (BEASTObjectInputEditor) editors.get(0);
        	if (boie.m_expansionBox != null && boie.m_expansionBox.getChildren().size() > 1) {
        		addExpansionBox = true;
        	}
        }
        
        if (addExpansionBox) {
                // only go here if it is worth showing expanded box
                //expandBox.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.gray));
                //itemBox = box;
                //VBox box2 = FXUtils.newVBox();
                //box2.getChildren().add(itemBox);
//TODO: find out what this line does:  itemBox.getChildren().add(0, editButton);
                //box2.getChildren().add(expandBox);
//    		//itemBox.remove(editButton);
//    		editButton.setVisible(false);
//    	} else {
                //itemBox = box2;
                // m_listBox.add(expandBox, 0, rowCount);
            if (g_collapsedIDs.contains(beastObject.getID())) {
        		expandBox.setPrefHeight(0);
            	expandBox.setMinHeight(0);
        		expandBox.setVisible(false);
        		expandBox.setManaged(false);
        	} else {
            	expandBox.setPrefHeight(USE_COMPUTED_SIZE);
            	expandBox.setMinHeight(expandBox.getPrefHeight());
        		expandBox.setVisible(true);
        		expandBox.setManaged(true);
        	}
            
            if (editor instanceof HasExpandBox heb) {
            	heb.setExpandBox(expandBox);
            }
        } else {
        	editButtonIsVisible = false;
        }
        String id = beastObject.getID();
        expandBox.setVisible(!g_collapsedIDs.contains(id));
        return editButtonIsVisible;
    }

	/**
     * add components to box that are specific for the beastObject.
     * By default, this just inserts a label with the beastObject ID
     *
     * @param itemBox box to add components to
     * @param beastObject  beastObject to add
     */
    protected InputEditor addPluginItem(Pane itemBox, BEASTInterface beastObject) {
        String name = beastObject.getID();
        if (name == null || name.length() == 0) {
            name = beastObject.getClass().getName();
            name = name.substring(name.lastIndexOf('.') + 1);
        }
        Label label = new Label(name);

        //itemBox.getChildren().add(new Separator());
        itemBox.getChildren().add(label);
        //itemBox.getChildren().add(new Separator());
        return this;
    }

    class IDDocumentListener implements DocumentListener {
        BEASTInterface m_beastObject;
        TextField m_entry;

        IDDocumentListener(BEASTInterface beastObject, TextField entry) {
            m_beastObject = beastObject;
            m_entry = entry;
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            processEntry();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            processEntry();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            processEntry();
        }

        void processEntry() {
            String oldID = m_beastObject.getID();
            m_beastObject.setID(m_entry.getText());
            BEASTObjectPanel.renamePluginID(m_beastObject, oldID, m_beastObject.getID(), doc);
            validateAllEditors();
            m_entry.requestFocus();
        }
    }

    protected void addItem() {
        List<String> tabuList = new ArrayList<>();
        for (int i = 0; i < m_entries.size(); i++) {
            tabuList.add(m_entries.get(i).getText());
        }
        List<BEASTInterface> beastObjects = pluginSelector(m_input, m_beastObject, tabuList);
        if (beastObjects != null) {
            for (BEASTInterface beastObject : beastObjects) {
                try {
                	setValue(beastObject);
                    //m_input.setValue(beastObject, m_beastObject);
                } catch (Exception ex) {
                    Log.err.println(ex.getClass().getName() + " " + ex.getMessage());
                }
                addSingleItem(beastObject);
                getDoc().addPlugin(beastObject);
            }
            validateInput();
            updateState();
            repaint();
        }
    } // addItem

    protected Object editItem(Object o) {
        int i = ((List<?>) m_input.get()).indexOf(o);
        BEASTInterface beastObject = (BEASTInterface) ((List<?>) m_input.get()).get(i);
        BEASTObjectDialog dlg = new BEASTObjectDialog(beastObject, m_input.getType(), doc);
        if (dlg.showDialog()) {
            //m_labels.get(i).setText(dlg.m_panel.m_beastObject.getID());
        	if (m_entries.size() > i) {
        		m_entries.get(i).setText(dlg.m_panel.m_beastObject.getID());
        	}
            //o = dlg.m_panel.m_beastObject;
            dlg.accept((BEASTInterface) o, doc);
            refreshPanel();
        }
        //BEASTObjectPanel.m_position.x -= 20;
        //BEASTObjectPanel.m_position.y -= 20;
        validateAllEditors();
        updateState();
        //doLayout();
        return o;
    } // editItem

    protected void deleteItem(Object o) {
        int i = ((List<?>) m_input.get()).indexOf(o);
        m_listBox.getChildren().remove(i);
        ((List<?>) m_input.get()).remove(i);
        safeRemove(m_entries, i);
        safeRemove(delButtonList, i);
        safeRemove(m_editButton, i);
        safeRemove(m_validateLabels, i);
        validateInput();
        updateState();
        //doLayout();
        repaint();
    } // deleteItem

    private void safeRemove(List<?> list, int i) {
        if (list.size() > i) {
            list.remove(i);
        }
    }

    /**
     * Select existing plug-in, or create a new one.
     * Suppress existing plug-ins with IDs from the taboo list.
     * Return null if nothing is selected.
     */
    protected List<BEASTInterface> pluginSelector(Input<?> input, BEASTInterface parent, List<String> tabooList) {
        List<BEASTInterface> selectedPlugins = new ArrayList<>();
        List<String> beastObjectNames = doc.getInputEditorFactory().getAvailablePlugins(input, parent, tabooList, doc);
        /* select a beastObject **/
        String className = null;
        if (beastObjectNames.size() == 1) {
            // if there is only one candidate, select that one
            className = beastObjectNames.get(0);
        } else if (beastObjectNames.size() == 0) {
            // no candidate => we cannot be in expert mode
            // create a new BEASTObject
            doc.setExpertMode(true);
            beastObjectNames = doc.getInputEditorFactory().getAvailablePlugins(input, parent, tabooList, doc);
            doc.setExpertMode(false);
            className = beastObjectNames.get(0);
        } else {
            // otherwise, pop up a list box
            className = (String) Alert.showInputDialog(null,
                    "Select a constant", "select",
                    Alert.PLAIN_MESSAGE, null,
                    beastObjectNames.toArray(new String[0]),
                    null);
            if (className == null) {
                return null;
            }
        }
        if (!className.startsWith("new ")) {
            /* return existing beastObject */
            selectedPlugins.add(doc.pluginmap.get(className));
            return selectedPlugins;
        }
        /* create new beastObject */
        try {
            BEASTInterface beastObject = (BEASTInterface) BEASTClassLoader.forName(className.substring(4)).newInstance();
            BEASTObjectPanel.addPluginToMap(beastObject, doc);
            selectedPlugins.add(beastObject);
            return selectedPlugins;
        } catch (Exception ex) {
            Alert.showMessageDialog(null, "Could not select beastObject: " +
                    ex.getClass().getName() + " " +
                    ex.getMessage()
            );
            return null;
        }
    } // pluginSelector

    protected void updateState() {
        for (int i = 0; i < ((List<?>) m_input.get()).size(); i++) {
            try {
                BEASTInterface beastObject = (BEASTInterface) ((List<?>) m_input.get()).get(i);
                beastObject.validateInputs();
                m_validateLabels.get(i).setVisible(false);
            } catch (IndexOutOfBoundsException e) {
            	// happens when m_validateLabels is not large enough, so there is nothing to show 
            } catch (Exception e) {
            	// something went wrong, so show label if available
                if (m_validateLabels.size() > i) {
                    m_validateLabels.get(i).setTooltip(new Tooltip(e.getMessage()));
                    m_validateLabels.get(i).setVisible(true);
                }
            }
        }
        validateInput();
        // this triggers properly re-layouting after an edit action
        setVisible(false);
        setVisible(true);
    } // updateState

    @Override
    public void startValidating(ValidationStatus state) {
        updateState();
    }

    public void setButtonStatus(ButtonStatus buttonStatus) {
        m_buttonStatus = buttonStatus;
    }

} // class ListPluginInputEditor
