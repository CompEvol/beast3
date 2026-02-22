package beastfx.app.inputeditor;


import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

import javax.swing.event.ListSelectionListener;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beastfx.app.util.FXUtils;


public class DoubleListInputEditor extends ListInputEditor {

	public DoubleListInputEditor(BeautiDoc doc) {
		super(doc);
	}

    public DoubleListInputEditor() {
		super();
	}

	@Override
    public Class<?> type() {
        return List.class;
    }

    /**
     * return type of the list *
     */
    @Override
	public Class<?> baseType() {
        return Double.class;
    }
    

    
    @SuppressWarnings("unchecked")
	@Override
    protected void initEntry() {
        if (m_input.get() != null) {
        	String str = "";
        	for (Double d : (List<Double>) m_input.get()) {
        		str += d + " ";
        	}
        	str = str.substring(0, str.length() - 1);
            m_entry.setText(str);
        }
    }
    
        /**
         * the input to be edited *
         */
        protected Input<?> m_input;

        /**
         * parent beastObject *
         */
        protected BEASTInterface m_beastObject;

        /**
         * text field used for primitive input editors *
         */
        protected TextField m_entry;
        
        protected int itemNr;

        @Override
		public TextField getEntry() {
            return m_entry;
        }

        Label m_inputLabel;
        protected static Dimension PREFERRED_SIZE = new Dimension(200, 25);
        protected static Dimension MAX_SIZE = new Dimension(1024, 25);

        /**
         * flag to indicate label, edit and validate buttons/labels should be added *
         */
        protected boolean m_bAddButtons = true;

        /**
         * label that shows up when validation fails *
         */
        protected SmallLabel m_validateLabel;

        /**
         * document that we are editing *
         */
        protected BeautiDoc doc;

        /**
         * list of objects that want to be notified of the validation state when it changes *
         */
        List<InputEditor> m_validateListeners;

        @Override
		public void addValidationListener(InputEditor validateListener) {
            if (m_validateListeners == null) {
                m_validateListeners = new ArrayList<>();
            }
            m_validateListeners.add(validateListener);
        }

        @Override
		public void notifyValidationListeners(ValidationStatus state) {
            if (m_validateListeners != null) {
                for (InputEditor listener : m_validateListeners) {
                    listener.startValidating(state);
                }
            }
        }

    	@Override
		protected BeautiDoc getDoc() {
            if (doc == null) {
                Parent c = this;
                while (c.getParent() != null) {
                    c = c.getParent();
                    if (c instanceof BeautiDocProvider) {
                        doc = ((BeautiDocProvider) c).getDoc();
                    }
                }
            }
            return doc;
        }


        @Override
		public Class<?>[] types() {
            Class<?>[] types = new Class<?>[1];
            types[0] = type();
            return types;
        }

        /**
         * construct an editor consisting of a label and input entry *
         */
        @Override
		public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
            m_bAddButtons = addButtons;
            m_input = input;
            m_beastObject = beastObject;
            this.itemNr= itemNr;
            
            pane = FXUtils.newHBox();
            addInputLabel();

            setUpEntry();

            pane.getChildren().add(m_entry);
            // pane.getChildren().add(new Separator());
            addValidationLabel();
            getChildren().add(pane);
        } // init

        @Override
		protected void setUpEntry() {
            m_entry = new TextField();
            m_entry.setId(m_input.getName());
//            int size = m_entry.getFont().getSize();
//            PREFERRED_SIZE = new Dimension(200, 25 * size / 13);
//            m_entry.setMinSize(PREFERRED_SIZE.getWidth(), PREFERRED_SIZE.getHeight());
//            m_entry.setPrefSize(PREFERRED_SIZE.getWidth(), PREFERRED_SIZE.getHeight());
//            // m_entry.setSize(PREFERRED_SIZE.getWidth(), PREFERRED_SIZE.getHeight());
            initEntry();
            m_entry.setTooltip(new Tooltip(m_input.getTipText()));
//            m_entry.setMaxSize(1024, 25 * size / 13);
            m_entry.setOnKeyReleased(e-> processEntry());

//            m_entry.getDocument().addDocumentListener(new DocumentListener() {
//                @Override
//                public void removeUpdate(DocumentEvent e) {
//                    processEntry();
//                }
//
//                @Override
//                public void insertUpdate(DocumentEvent e) {
//                    processEntry();
//                }
//
//                @Override
//                public void changedUpdate(DocumentEvent e) {
//                    processEntry();
//                }
//            });
        }

        @Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
    	protected void setValue(Object o) {
        	if (itemNr < 0) {
        		m_input.setValue(o, m_beastObject);
        	} else {
        		// set value of an item in a list
        		List list = (List) m_input.get();
        		Object other = list.get(itemNr);
        		if (other != o) {
        			if (other instanceof BEASTInterface) {
        				BEASTInterface.getOutputs(other).remove(m_beastObject);
        			}
        			list.set(itemNr, o);
        			if (o instanceof BEASTInterface) {
        				BEASTInterface.getOutputs(o).add(m_beastObject);
        			}
        		}
        	}
        }
        
        @Override
		protected void processEntry() {
            try {
            	setValue(m_entry.getText());
                validateInput();
                m_entry.requestFocus();
            } catch (Exception ex) {
//    			Alert.showMessageDialog(null, "Error while setting " + m_input.getName() + ": " + ex.getMessage() +
//    					" Leaving value at " + m_input.get());
//    			m_entry.setText(m_input.get() + "");
                if (m_validateLabel != null) {
                    m_validateLabel.setVisible(true);
                    m_validateLabel.setTooltip(new Tooltip("Parsing error: " + ex.getMessage() + ". Value was left at " + m_input.get() + "."));
                    m_validateLabel.setColor("orange");
                }
                repaint();
            }
        }

        @Override
		protected void addInputLabel() {
            if (m_bAddButtons) {
                String name = formatName(m_input.getName());
                addInputLabel(name, m_input.getTipText());
            }
        }

        @Override
		protected String formatName(String name) {
    	    if (doc.beautiConfig.inputLabelMap.containsKey(m_beastObject.getClass().getName() + "." + name)) {
    	        name = doc.beautiConfig.inputLabelMap.get(m_beastObject.getClass().getName() + "." + name);
    	    } else {
    	        name = name.replaceAll("([a-z])([A-Z])", "$1 $2");
    	        name = name.substring(0, 1).toUpperCase() + name.substring(1);
    	    }
    	    return name;
        }

        @Override
		protected void addInputLabel(String label, String tipText) {
            if (m_bAddButtons) {
                m_inputLabel = new Label(label);
                m_inputLabel.setTooltip(new Tooltip(tipText));
//                m_inputLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
//                //Dimension size = new Dimension(g_nLabelWidth, 20);
//                int fontsize = m_inputLabel.getFont().getSize();
//                Dimension size = new Dimension(200, 20 * fontsize / 13);
//                m_inputLabel.setMaxSize(size.getWidth(),size.getHeight());
//                m_inputLabel.setMinSize(size.getWidth(),size.getHeight());
//                m_inputLabel.setPrefSize(size.getWidth(),size.getHeight());
//                m_inputLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

                // RRB: temporary
                //m_inputLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
                pane.getChildren().add(m_inputLabel);
            }
        }

        @Override
		protected void addValidationLabel() {
            if (m_bAddButtons) {
                m_validateLabel = new SmallLabel("x", "red");
                pane.getChildren().add(m_validateLabel);
                m_validateLabel.setVisible(true);
                validateInput();
            }
        }

        /* check the input is valid, continue checking recursively */
        @Override
		protected void validateAllEditors() {
            for (InputEditor editor : doc.currentInputEditors) {
                editor.validateInput();
            }
        }

        @Override
        public void validateInput() {
            try {
                m_input.validate();
                if (m_entry != null && !m_input.canSetValue(m_entry.getText(), m_beastObject)) {
                    throw new IllegalArgumentException("invalid value");
                }
                // recurse
                try {
                    validateRecursively(m_input, new HashSet<>());
                } catch (Exception e) {
                    notifyValidationListeners(ValidationStatus.HAS_INVALIDMEMBERS);
                    if (m_validateLabel != null) {
                        m_validateLabel.setVisible(true);
                        m_validateLabel.setTooltip(new Tooltip("Recursive error in " + e.getMessage()));
                        m_validateLabel.setColor("orange");
                    }
                    repaint();
                    return;
                }
                if (m_validateLabel != null) {
                    m_validateLabel.setVisible(false);
                }
                notifyValidationListeners(ValidationStatus.IS_VALID);
            } catch (Exception e) {
            	Log.warning.println("Validation message: " + e.getMessage());
                if (m_validateLabel != null) {
                    m_validateLabel.setTooltip(new Tooltip(e.getMessage()));
                    m_validateLabel.setColor("red");
                    m_validateLabel.setVisible(true);
                }
                notifyValidationListeners(ValidationStatus.IS_INVALID);
            }
            repaint();
        }

        /* Recurse in any of the input beastObjects
          * and validate its inputs */
        @Override
		void validateRecursively(Input<?> input, Set<Input<?>> done) {
            if (done.contains(input)) {
                // this prevent cycles to lock up validation
                return;
            } else {
                done.add(input);
            }
            if (input.get() != null) {
                if (input.get() instanceof BEASTInterface) {
                    BEASTInterface beastObject = ((BEASTInterface) input.get());
                    for (Input<?> input2 : beastObject.listInputs()) {
                        try {
                            input2.validate();
                        } catch (Exception e) {
                            throw new IllegalArgumentException(((BEASTInterface) input.get()).getID() + "</p><p> " + e.getMessage());
                        }
                        validateRecursively(input2, done);
                    }
                }
                if (input.get() instanceof List<?>) {
                    for (Object o : (List<?>) input.get()) {
                        if (o != null && o instanceof BEASTInterface) {
                            BEASTInterface beastObject = (BEASTInterface) o;
                            for (Input<?> input2 : beastObject.listInputs()) {
                                try {
                                    input2.validate();
                                } catch (Exception e) {
                                    throw new IllegalArgumentException(((BEASTInterface) o).getID() + " " + e.getMessage());
                                }
                                validateRecursively(input2, done);
                            }
                        }
                    }
                }
            }
        } // validateRecursively

        @Override
        public void startValidating(ValidationStatus state) {
            validateInput();
        }


        @Override
		public void refreshPanel() {
            Parent c = this;
            while (c.getParent() != null) {
                c = c.getParent();
                if (c instanceof ListSelectionListener) {
                    ((ListSelectionListener) c).valueChanged(null);
                }
            }
        }

        /**
         * synchronise values in panel with current network *
         */
        @Override
		protected void sync() {
        	Parent c = this;
            while (c.getParent() != null) {
                c = c.getParent();
                if (c instanceof BeautiDocProvider) {
                	BeautiDocProvider panel = (BeautiDocProvider) c;
                    BeautiPanelConfig cfgPanel = panel.getConfig();
                    cfgPanel.sync(panel.getPartitionIndex());
                }
            }
        }

        // we should leave it to the component to set its own border
//        @Override
//		@Deprecated
//        public void setBorder(Border border) {
//    		super.setBorder(border);
//        }

        @Override
        public void setDoc(BeautiDoc doc) {
        	this.doc = doc;
        }

        // what is this method for? We should leave repainting to the standard mechanism
//        @Override
//		@Deprecated
//    	public void repaint() {
//    	this.repaint(0);
//    		super.repaint();
//    	}

    	@Override
		public Parent getComponent() {
    		return this;
    	}

}
