package beastfx.app.inputeditor;

import beast.base.core.BEASTInterface;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.VBox;

public abstract class ExpandActionListener implements EventHandler<ActionEvent> {
    VBox m_box;
    BEASTInterface m_beastObject;

    public ExpandActionListener(VBox box, BEASTInterface beastObject) {
        super();
        m_box = box;
        m_beastObject = beastObject;
    }
}
