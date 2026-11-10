package beastfx.app.inputeditor;




import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.MCMC;
import beast.base.parser.XMLProducer;
import beast.pkgmgmt.BEASTClassLoader;
import beastfx.app.beauti.ThemeProvider;
import beastfx.app.util.Alert;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for editing BEASTObjects.
 * <p/>
 * This dynamically creates a dialog consisting of
 * InputEditors associated with the inputs of a BEASTObject.
 * *
 */

public class BEASTObjectDialog extends Dialog {

    private boolean m_bOK = false;

    public BEASTObjectPanel m_panel;

    BeautiDoc doc;

    /** the object being edited (the panel edits a copy of it), null if not known **/
    private BEASTInterface original;
    
    public BEASTObjectDialog(BEASTObjectPanel panel, BeautiDoc doc) {
        init(panel);
        this.doc = doc;
    }

    public BEASTObjectDialog(BEASTInterface beastObject, Class<? extends BEASTInterface> aClass, List<BEASTInterface> beastObjects, BeautiDoc doc) {
        this(new BEASTObjectPanel(beastObject, aClass, beastObjects, doc), doc);
        this.original = beastObject;
    }

    public BEASTObjectDialog(BEASTInterface beastObject, Class<?> type, BeautiDoc doc) {
        this(new BEASTObjectPanel(beastObject, type, doc), doc);
        this.original = beastObject;
    }

    final public static String ICONPATH = "/beastfx/app/inputeditor/icon/";
    
    public boolean showDialog() {
        Image image = new Image(this.getClass().getResource("icon/beast.png").toString(), 100, 100, true, true);
        
		Dialog<ButtonType> dlg = new Dialog<>();
		DialogPane pane = dlg.getDialogPane();
		
		pane.setContent(new ScrollPane(m_panel));
		pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		dlg.setTitle(m_panel.m_beastObject.getClass().getName());
		Stage stage = (Stage) pane.getScene().getWindow();
		pane.setGraphic(new ImageView(image));
		pane.setPrefSize(InputEditor.Base.PREFERRED_SIZE.getWidth() * 9, 
				InputEditor.Base.PREFERRED_SIZE.getHeight() * 15);
		
		dlg.setResizable(true);
    	ThemeProvider.loadStyleSheet(pane.getScene());
		// Keep the dialog open while the values do not fit the dimension, instead of letting
		// the parameter silently repeat or extend them (see checkValueAndDimension)
		pane.lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, e -> {
			String message = checkValueAndDimension(m_panel.m_beastObject, original);
			if (message != null) {
				e.consume();
				Alert.showMessageDialog(null, message, "Value does not match dimension", AlertType.WARNING);
			}
		});
		ButtonType result = dlg.showAndWait().get();
		m_bOK = (result != ButtonType.CANCEL);
        return m_bOK;
    }
    
    /**
     * Check that the number of values of a vector parameter fits its dimension.
     * <p>
     * A vector parameter fills its dimension by repeating its values, and uses the number of
     * values as dimension when there are more values than the dimension. This is useful in XML
     * (value="1.0" dimension="4"), but in the dialog, where both are shown, changing one
     * silently changes the other: dimension 3 with values "0.1 0.2" becomes [0.1, 0.2, 0.1],
     * while the Value field still shows "0.1 0.2". So only allow one value (used for every
     * element) or exactly one value per element.
     * <p>
     * Vector parameters are recognised by their "value" (a list) and "dimension" inputs, so
     * this works for any of them.
     * <p>
     * Only edits made in the dialog are checked: if neither Value nor Dimension differs from
     * original, OK is allowed. A parameter can already be mismatched before the dialog opens,
     * because setDimension() (used by models that size a parameter in code) updates the
     * dimension but not the value, and refusing OK then would block the user for something
     * they did not change.
     *
     * @param beastObject the copy being edited in the dialog
     * @param original the object the dialog was opened for, or null to always check
     * @return a message saying what to change, or null if the values fit the dimension,
     * neither was changed, or beastObject is not a vector parameter
     */
    public static String checkValueAndDimension(BEASTInterface beastObject, BEASTInterface original) {
    	if (original != null && Objects.equals(valueAndDimension(beastObject), valueAndDimension(original))) {
    		return null;
    	}
    	return checkValueAndDimension(beastObject);
    }

    /** the value list and dimension of a vector parameter, or null if beastObject is not one **/
    private static List<Object> valueAndDimension(BEASTInterface beastObject) {
    	Input<?> valueInput = beastObject.getInputs().get("value");
    	Input<?> dimensionInput = beastObject.getInputs().get("dimension");
    	if (valueInput == null || dimensionInput == null || !(valueInput.get() instanceof List<?> values)) {
    		return null;
    	}
    	return List.of(new ArrayList<>(values), String.valueOf(dimensionInput.get()));
    }

    /**
     * As {@link #checkValueAndDimension(BEASTInterface, BEASTInterface)}, but always checks.
     */
    public static String checkValueAndDimension(BEASTInterface beastObject) {
    	Input<?> valueInput = beastObject.getInputs().get("value");
    	Input<?> dimensionInput = beastObject.getInputs().get("dimension");
    	if (valueInput == null || dimensionInput == null
    			|| !(valueInput.get() instanceof List<?> values) || !(dimensionInput.get() instanceof Integer dimension)) {
    		return null;
    	}
    	if (values.size() == 1 || values.size() == dimension) {
    		return null;
    	}
    	return "Value has " + values.size() + " numbers, but Dimension is " + dimension + ".\n"
    			+ "Enter " + dimension + " numbers, or 1 number to use for all " + dimension
    			+ " elements, or change Dimension to " + values.size() + ".";
    }

    /* to be called when OK is pressed **/
    public void accept(BEASTInterface beastObject, BeautiDoc doc) {
        try {
            for (Input<?> input : m_panel.m_beastObject.listInputs()) {
            	if (input.get() != null && (input.get() instanceof List)) {
                    // setInpuValue (below) on lists does not lead to expected result
            		// it appends values to the list instead, so we have to clear it first
                    List<?> list = (List<?>)beastObject.getInput(input.getName()).get();
                    list.clear();
            	}
            	beastObject.setInputValue(input.getName(), input.get());
            }
            beastObject.setID(m_panel.m_beastObject.getID());
            if (doc != null) {
            	doc.addPlugin(beastObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void init(BEASTObjectPanel panel) {
        this.m_panel = panel;

        initModality(Modality.APPLICATION_MODAL);

        getDialogPane().getChildren().add(panel);

        setTitle(panel.m_beastObject.getID() + " Editor");

    } // c'tor

    public boolean getOK(BeautiDoc doc) {
        if (m_bOK) {
            String oldID = m_panel.m_beastObject.getID();
            BEASTObjectPanel.g_plugins.remove(oldID);
            m_panel.m_beastObject.setID(m_panel.m_identry.getText());
            BEASTObjectPanel.registerPlugin(m_panel.m_beastObject.getID(), m_panel.m_beastObject, doc);
        }
        return m_bOK;
    }

    /**
     * rudimentary test *
     */
    public static void main(String[] args) {
        BEASTObjectDialog dlg = null;
        try {
            if (args.length == 0) {
                dlg = new BEASTObjectDialog(new BEASTObjectPanel(new MCMC(), Runnable.class, null), null);
            } else if (args[0].equals("-x")) {
                StringBuilder text = new StringBuilder();
                String NL = System.getProperty("line.separator");
                Scanner scanner = new Scanner(new File(args[1]));
                try {
                    while (scanner.hasNextLine()) {
                        text.append(scanner.nextLine() + NL);
                    }
                } finally {
                    scanner.close();
                }
                BEASTInterface beastObject = new beast.base.parser.XMLParser().parseBareFragment(text.toString(), false);
                dlg = new BEASTObjectDialog(new BEASTObjectPanel(beastObject, beastObject.getClass(), null), null);
            } else if (args.length == 1) {
                dlg = new BEASTObjectDialog(new BEASTObjectPanel((BEASTInterface) BEASTClassLoader.forName(args[0]).newInstance(), BEASTClassLoader.forName(args[0]), null), null);
            } else if (args.length == 2) {
                dlg = new BEASTObjectDialog(new BEASTObjectPanel((BEASTInterface) BEASTClassLoader.forName(args[0]).newInstance(), BEASTClassLoader.forName(args[1]), null), null);
            } else {
                throw new IllegalArgumentException("Incorrect number of arguments");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Usage: " + BEASTObjectDialog.class.getName() + " [-x file ] [class [type]]\n" +
                    "where [class] (optional, default MCMC) is a BEASTObject to edit\n" +
                    "and [type] (optional only if class is specified, default Runnable) the type of the BEASTObject.\n" +
                    "for example\n" +
                    "");
            System.exit(1);
        }
        dlg.setOnCloseRequest(e->{System.exit(0);});
        if (dlg.showDialog()) {
            BEASTInterface beastObject = dlg.m_panel.m_beastObject;
            String xml = new XMLProducer().modelToXML(beastObject);
            System.out.println(xml);
        }
    } // main
} // class PluginDialog

