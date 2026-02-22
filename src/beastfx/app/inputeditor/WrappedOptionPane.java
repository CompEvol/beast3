package beastfx.app.inputeditor;

import java.net.MalformedURLException;
import java.net.URL;

import beastfx.app.util.Utils;
import beastfx.app.beauti.ThemeProvider;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 * Alert but with text wrapping.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class WrappedOptionPane extends DialogPane {

//    @Override
//    public int getMaxCharactersPerLineCount() {
//        return 70;
//    }

    /**
     * Display a message dialog with long lines wrapped at word breaks so
     * that the text width is limited to 70 chars.
     *
     * @param message message to display
     */
    static public void showWrappedMessageDialog(Object message) {
        showWrappedMessageDialog(null, message, null);
    }

    /**
     * Display a message dialog with long lines wrapped at word breaks so
     * that the text width is limited to 70 chars.
     *
     * @param parentComponent parent component
     * @param message message to display
     */
    static public void showWrappedMessageDialog(Parent parentComponent, Object message) {
        showWrappedMessageDialog(parentComponent, message, null);
    }

    /**
     * Display a message dialog with long lines wrapped at word breaks so
     * that the text width is limited to 70 chars.
     *
     * @param parentComponent parent component
     * @param message      message to display
     * @param fontName     name of font used to display message
     */
    static public void showWrappedMessageDialog(Parent parentComponent, Object message, String fontName) {
        WrappedOptionPane pane = new WrappedOptionPane();
        Dialog<?> dialog = new Dialog<>();
        dialog.setDialogPane(pane);
        pane.setContentText(message.toString());
        Stage stage = (Stage) pane.getScene().getWindow();
        String str = WrappedOptionPane.class.getResource("/beastfx/app/inputeditor/icon/beast.png").toString();
        stage.getIcons().add(new Image(str));
        pane.setHeader(new ImageView(str));
        pane.setHeaderText("Information");

        pane.getButtonTypes().add(ButtonType.CLOSE);
        ThemeProvider.loadStyleSheet(pane.getScene());
        if (fontName != null) {
        	pane.setStyle("-fx-font-family:" + fontName + ";");
        }
        dialog.setResizable(true);
        dialog.showAndWait();

    }
}
