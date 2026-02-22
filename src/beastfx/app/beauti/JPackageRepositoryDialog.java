/*
 * Copyright (C) 2014 Tim Vaughan <tgvaughan@gmail.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package beastfx.app.beauti;



import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import beast.pkgmgmt.PackageManager;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class JPackageRepositoryDialog extends DialogPane {
    public static class URL0 {
    	URL url;
    	public URL0(URL url) {
    		this.url = url;
    	}
    	
    	public String getURL() {
    		return url.toString();
    	}
    	public void setURL(String URL) {
    		// ignore
    	}
    }

    private ObservableList<URL0> urls0;

    
	public JPackageRepositoryDialog(final Pane frame) {
		Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("BEAST 2 Package Repository Manager");
        // Get current list of URLs:
        List<URL> urls;
        try {
            urls = PackageManager.getRepositoryURLs();
        } catch (MalformedURLException exception) {
            urls = new ArrayList<>();
            try {
                urls.add(new URL(PackageManager.PACKAGES_XML));
            } catch (MalformedURLException e) {
                // Hard-coded URL is broken. Should never happen!
                e.printStackTrace();
            }
        }

        List<URL0> urls0 = new ArrayList<>();
        for (URL url : urls) {
        	urls0.add(new URL0(url));
        }
        this.urls0 = FXCollections.observableArrayList(urls0);
        
        // Assemble table
        VBox pane = FXUtils.newVBox();
        
        final TableView<URL0> repoTable = new TableView<>();
        pane.getChildren().add(repoTable);
        
		TableColumn<URL0, String> col1 = new TableColumn<>("Package repository URL");
		col1.setPrefWidth(400);
		col1.setCellValueFactory(new PropertyValueFactory<>("URL"));
		repoTable.getColumns().add(col1);

		repoTable.setItems(this.urls0);
        
        // Add buttons
        HBox box = FXUtils.newHBox();
        
        // ADD URL
        Button addURLButton = new Button("Add URL");
        addURLButton.setOnAction(e -> {
            Optional<String> newURLString = Alert.showInputDialog(frame,
                    "Enter package repository URL",
                    "Add repository URL",Alert.PLAIN_MESSAGE, "http://");

            if (newURLString.isEmpty())
                return; // User canceled

            URL newURL;
            try {
                newURL = new URL(newURLString.get());
            } catch (MalformedURLException exception) {
                Alert.showMessageDialog(frame, "Invalid URL.");
                return;
            }

            for (URL0 url : this.urls0) {
	            if (url.getURL().equals(newURL.getPath())) {
	                Alert.showMessageDialog(frame, "Repository already exists!");
	                return;
	            }
            }

            try {
                if (newURL.getHost() == null)
                    return;

                InputStream is = newURL.openStream();
                is.close();

            } catch (IOException ex) {
                Alert.showMessageDialog(frame, "Could not access URL.");
                return;
            }

            // Add to table:
            this.urls0.add(new URL0(newURL));
            save();
        });
        box.getChildren().add(addURLButton);
        
        // DELETE URL
        Button deleteURLButton = new Button("Delete selected URL");
        deleteURLButton.setOnAction(e -> {
            if (Alert.showConfirmDialog(dlg.getDialogPane(), "Really delete this repository?", "Delete?", Alert.YES_NO_OPTION) ==Alert.YES_OPTION) {
                this.urls0.remove(repoTable.getSelectionModel().getSelectedIndex());
                save();
            }
        });
        deleteURLButton.setDisable(true);
        box.getChildren().add(deleteURLButton);
        
        // Action listeners to disable/enable delete button
        repoTable.getSelectionModel().selectedIndexProperty().
    	addListener(e -> deleteURLButton.setDisable(repoTable.getSelectionModel().getSelectedIndex() == 0));
                
        pane.getChildren().add(box);
        pane.setPrefWidth(400);
        pane.setPrefHeight(400);
        
        dlg.getDialogPane().setContent(pane);
        dlg.getDialogPane().getButtonTypes().add(Alert.CLOSED_OPTION);
        dlg.setResizable(true);
    	ThemeProvider.loadStyleSheet(pane.getScene());
        dlg.showAndWait();

    }

	private void save() {
		List<URL> urls = new ArrayList<>();
		for (URL0 url0 : this.urls0) {
			urls.add(url0.url);
		}
		PackageManager.saveRepositoryURLs(urls);
	}

}
