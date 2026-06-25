package beastfx.app.inputeditor.spec;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import beast.base.spec.inference.distribution.TensorDistribution;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.Vector;
import beastfx.app.inputeditor.BEASTObjectInputEditor;
import beastfx.app.inputeditor.BEASTObjectPanel;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.util.FXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * InputEditor for spec vector parameters: RealVectorParam, IntVectorParam,
 * BoolVectorParam (and their subclasses, e.g. SimplexParam).
 *
 * Displays all element values in a single space-separated text field, an
 * isEstimated checkbox, and an edit button for accessing dimension / domain /
 * key settings via BEASTObjectDialog.
 *
 * Handles IntVector and BoolVector via the same text-field pattern, which is
 * consistent with how ParameterInputEditor handles the legacy Parameter.Base.
 */
public class VectorInputEditor extends BEASTObjectInputEditor {

    public CheckBox m_isEstimatedBox;

    public VectorInputEditor() {
        super();
    }

    public VectorInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return Vector.class;
    }

    /**
     * Register all three concrete vector-param base classes.
     * Subclasses (e.g. SimplexParam extends RealVectorParam) are found
     * automatically through InputEditorFactory's superclass walk.
     */
    @Override
    public Class<?>[] types() {
        return new Class<?>[] {
            RealVectorParam.class,
            IntVectorParam.class,
            BoolVectorParam.class,
        };
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {
        if ("param".equals(input.getName()) && beastObject instanceof TensorDistribution) {
            // TensorDistributionInputEditor shows paramInput via the range button; suppress here
            pane = FXUtils.newHBox();
            setVisible(false);
            setManaged(false);
            return;
        }
        super.init(input, beastObject, itemNr, isExpandOption, addButtons);
        m_beastObject = beastObject;
        pane.setPadding(new Insets(5));
    }

    // --- value helpers ---------------------------------------------------

    private Object resolveParam() {
        if (itemNr < 0) {
            return m_input.get();
        }
        return ((List<?>) m_input.get()).get(itemNr);
    }

    /** Space-separated string of all element values. */
    private String valuesToString(Object param) {
        StringBuilder sb = new StringBuilder();
        if (param instanceof RealVectorParam<?> rvp) {
            for (double v : rvp.getValues()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(v);
            }
        } else if (param instanceof IntVectorParam<?> ivp) {
            for (int v : ivp.getValues()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(v);
            }
        } else if (param instanceof BoolVectorParam bvp) {
            for (boolean v : bvp.getValues()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(v);
            }
        }
        return sb.toString();
    }

    // --- InputEditor.Base overrides -------------------------------------

    @Override
    protected void setUpEntry() {
        super.setUpEntry();
        // wider field: vectors can have many elements
        m_entry.setPrefWidth(300);
        m_entry.setMaxWidth(500);
    }

    @Override
    protected void initEntry() {
        Object param = resolveParam();
        if (param == null) return;
        m_entry.setText(valuesToString(param));
    }

    @Override
    protected void processEntry() {
        try {
            Object param = resolveParam();
            String text = m_entry.getText().trim();

            if (param instanceof RealVectorParam<?> rvp) {
                String oldValue = valuesToString(rvp);
                int oldDim = rvp.size();
                rvp.valuesInput.setValue(text, rvp);
                rvp.initAndValidate();
                if (rvp.size() != oldDim) {
                    rvp.setDimension(oldDim);
                    rvp.valuesInput.setValue(oldValue, rvp);
                    rvp.initAndValidate();
                    throw new IllegalArgumentException("Entry caused change in dimension");
                }
            } else if (param instanceof IntVectorParam<?> ivp) {
                String oldValue = valuesToString(ivp);
                int oldDim = ivp.size();
                ivp.valuesInput.setValue(text, ivp);
                ivp.initAndValidate();
                if (ivp.size() != oldDim) {
                    ivp.setDimension(oldDim);
                    ivp.valuesInput.setValue(oldValue, ivp);
                    ivp.initAndValidate();
                    throw new IllegalArgumentException("Entry caused change in dimension");
                }
            } else if (param instanceof BoolVectorParam bvp) {
                bvp.valuesInput.setValue(text, bvp);
                bvp.initAndValidate();
            }

            validateInput();
        } catch (Exception ex) {
            if (m_validateLabel != null) {
                m_validateLabel.setVisible(true);
                m_validateLabel.setTooltip(new Tooltip(
                    "Parsing error: " + ex.getMessage() +
                    ". Value was left at " + resolveParam() + "."));
                m_validateLabel.setColor("orange");
            }
            repaint();
        }
    }

    /**
     * Replaces the generic BEASTObject combobox with a text field showing
     * all element values, an isEstimated checkbox, and an edit button.
     */
    @Override
    protected void addComboBox(Pane box, Input<?> input, BEASTInterface beastObject) {
        Object parameter = (itemNr >= 0)
            ? ((List<?>) input.get()).get(itemNr)
            : input.get();

        if (parameter == null) {
            super.addComboBox(box, input, beastObject);
            return;
        }

        HBox paramBox = FXUtils.newHBox();

        setUpEntry();
        paramBox.getChildren().add(m_entry);
        FXUtils.createHMCButton(paramBox, m_beastObject, m_input);

        if (m_bAddButtons && BEASTObjectPanel.countInputs(parameter, doc) > 0) {
            paramBox.getChildren().add(createEditButton(input));
        }

        if (parameter instanceof StateNode sn) {
            m_isEstimatedBox = new CheckBox(
                doc.beautiConfig.getInputLabel(
                    (BEASTInterface) parameter, sn.isEstimatedInput.getName()));
            m_isEstimatedBox.setId(input.getName() + ".isEstimated");
            m_isEstimatedBox.setMaxWidth(Double.POSITIVE_INFINITY);
            box.setMaxWidth(Double.POSITIVE_INFINITY);
            m_isEstimatedBox.setSelected(sn.isEstimatedInput.get());
            m_isEstimatedBox.setTooltip(
                new Tooltip("Estimate value of this parameter in the MCMC chain"));
            m_isEstimatedBox.setVisible(doc.isExpertMode());

            for (Object output : ((BEASTInterface) parameter).getOutputs()) {
                if (output instanceof Operator) {
                    m_isEstimatedBox.setVisible(true);
                    break;
                }
            }

            m_isEstimatedBox.setOnAction(e -> {
                try {
                    sn.isEstimatedInput.setValue(m_isEstimatedBox.isSelected(), sn);
                    hardSync();
                    refreshPanel();
                } catch (Exception ex) {
                    // ignore
                }
            });

            paramBox.getChildren().add(m_isEstimatedBox);
        }

        box.getChildren().add(paramBox);
    }

    @Override
    protected void addValidationLabel() {
        super.addValidationLabel();
        // hide the edit button when the isEstimated checkbox is hidden
        if (m_editBEASTObjectButton != null && m_isEstimatedBox != null) {
            m_editBEASTObjectButton.setVisible(m_isEstimatedBox.isVisible());
        }
    }

    @Override
    protected void refresh() {
        Object param = resolveParam();
        if (param != null && m_entry != null) {
            m_entry.setText(valuesToString(param));
        }
        if (m_isEstimatedBox != null && param instanceof StateNode sn) {
            m_isEstimatedBox.setSelected(sn.isEstimatedInput.get());
        }
        repaint();
    }
}
