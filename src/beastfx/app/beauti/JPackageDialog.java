package beastfx.app.beauti;









import beast.base.core.Description;
import beast.pkgmgmt.Package;
import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.PackageVersion;
import beastfx.app.inputeditor.BEASTObjectDialog;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Window;

import static beast.pkgmgmt.PackageManager.*;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * dialog for managing Package.
 * List, install and uninstall Package
 *
 * @author  Remco Bouckaert
 * @author  Walter Xie
 */
@Description("BEAUti package manager")
public class JPackageDialog extends DialogPane {
    // JScrollPane scrollPane;
    Label jLabel;
    HBox buttonBox;
    // JFrame frame;
    TableView<Package0> dataTable = null;
    private List<Package> packageList = null;
    private ObservableList<Package0> packages;

    public class Package0 {
    	Package pkg;
    	public Package0(Package pkg) {
    		this.pkg = pkg;
    	}
    	
    	public String getName() {
    		return pkg.getName();
    	}
    	
    	public String getInstalledVersion() {
    		if (pkg.getInstalledVersion() != null) {
    			return pkg.getInstalledVersion().getVersion();
    		}
    		return "";
    	}
    	
    	public String getLatestVersion() {
    		if (pkg.getLatestVersion() != null) {
    			return pkg.getLatestVersion().getVersion();
    		}
    		return "";
    	}
    	
        public String getDependencies() {
            return pkg.getDependenciesString();
        }
        
        public String getProjectURL() {
        	if (pkg.getProjectURL() != null)
                return pkg.getProjectURL().toString();
        	return "";
        }
        
        public String getDescription() {
            return pkg.getDescription();
        }
    }
    
    boolean useLatestVersion = true;

    TreeMap<String, Package> packageMap = new TreeMap<>((s1,s2)->{
    	return comparePackageNames(s1, s2);
    });


    boolean isRunning;
    Thread t;
    BorderPane pane;
    
    public JPackageDialog() {
        jLabel = new Label("List of available packages for BEAST v" + beastVersion.getMajorVersion() + ".*");
        jLabel.setMinWidth(400);
        pane = new BorderPane();
        pane.setTop(jLabel);

        dataTable = createTable();
        // update packages using a 30 second time out
        isRunning = true;
        t = new Thread() {
        	@Override
			public void run() {
                resetPackages();
        		isRunning = false;
        	}
        };
        t.start();
    	Thread t2 = new Thread() {
    		@Override
			public void run() {
    			try {
    				// wait 30 seconds
					sleep(30000);
	    			if (isRunning) {
	    				t.interrupt();
	    				Alert.showMessageDialog(null, "Download of file " +
	    						PackageManager.PACKAGES_XML + " timed out.\n" +
	    								"Perhaps this is due to lack of internet access\n" +
	    								"or some security settings not allowing internet access."
	    						);
	    			}
				} catch (InterruptedException e) {
				}
    		}
    	};
    	t2.start();
        
        try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        
        pane.setCenter(dataTable);

        buttonBox = createButtonBox();
        pane.setBottom(buttonBox);

        setPrefSize(dataTable.getPrefWidth() + 0, dataTable.getPrefHeight() + buttonBox.getPrefHeight() + 60);
        
        dlgPane = this;
        setContent(pane);
    }


    private TableView<Package0> createTable() {
        dataTable = new TableView<>();
        dataTable.setPlaceholder(new TextArea("No package found yet.\n"
        		+ "The package manager needs access to " + PACKAGES_XML + "\n"
        				+ "or " + PACKAGES_XML_BACKUP + ".\n"
        		+ "If no packages appear here shortly, check your internet connection.\n"
        		+ "If the connection is OK, check if you can access\n"
        		+ "     " + PACKAGES_XML.substring(0,PACKAGES_XML.indexOf("master")) +"\n"
        		+ "or\n"
        		+ "     " + PACKAGES_XML_BACKUP.substring(0,PACKAGES_XML_BACKUP.indexOf("raw")) +"\n"
        		+ "by opening the above link in a browser."));

        final int linkColumn = 4;

        
		String[] columnNames = {"Name", "Installed", "Latest", "Dependencies", "Link", "Detail"};
		int[] columnWidth = {200, 60, 50, 200, 40, 1200};
		String[] names = {"Name", "InstalledVersion", "LatestVersion", "Dependencies", "ProjectURL", "Description"};

		for (int i = 0; i < columnNames.length; i++) {
			TableColumn<Package0, String> col1 = new TableColumn<>(columnNames[i]);
			col1.setPrefWidth(columnWidth[i]);
			if (i != linkColumn) {
				col1.setCellValueFactory(new PropertyValueFactory<Package0, String>(names[i]));
			} else {
				col1.setCellFactory(e->{
					TableCell cell = new TableCell();
					ImageView imageview = FXUtils.getIcon(BEASTObjectDialog.ICONPATH + "link.png");
					cell.setGraphic(imageview);
				    return cell;
				});
			}
			dataTable.getColumns().add(col1);
		}

		dataTable.setPrefWidth(200+50+50+200+30+400);
		dataTable.setPrefHeight(600);
		dataTable.setMinSize(930, 600);
		dataTable.setPadding(new Insets(7));
        
//        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        
        
        
		dataTable.setItems(packages);
        
        dataTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        dataTable.setOnMouseClicked(e -> {
                if (dataTable.getSelectionModel().getSelectedCells().get(0).getColumn() == linkColumn) {
                    URL url = getSelectedPackage(dataTable.getSelectionModel().getSelectedIndex()).getProjectURL();
                    if (url != null) {
                        try {
                            Desktop.getDesktop().browse(url.toURI());
                        } catch (IOException | URISyntaxException e1) {
                            e1.printStackTrace();
                        }
                    }

                } else {
                    if (e.getClickCount() == 2) {
                        Package selPackage = getSelectedPackage(dataTable.getSelectionModel().getSelectedIndex());
                        showDetail(selPackage);
                    }
                }
        });

        dataTable.setOnMouseMoved(e -> {
        		TablePosition<Package, String> tc = dataTable.getFocusModel().getFocusedCell();
                int row = tc.getRow();
                int col = tc.getColumn();

                Cursor currentCursorType = dataTable.getCursor();

                if (col != linkColumn) {
                    if (currentCursorType == Cursor.HAND)
                        dataTable.setCursor(Cursor.DEFAULT);

                    return;
                }

                Package thisPkg = getSelectedPackage(row);

                if (thisPkg.getProjectURL() == null) {
                    if (currentCursorType == Cursor.HAND)
                        dataTable.setCursor(Cursor.DEFAULT);

                    return;
                }

                dataTable.setCursor(Cursor.HAND);
        });

		return dataTable;
    }


    private void resetPackages() {
        packageMap.clear();
        try {
            addAvailablePackages(packageMap);
            addInstalledPackages(packageMap);

            // Create list of packages excluding beast2
            packageList = new ArrayList<>();
            List<Package0> list = new ArrayList<>();
            for (Package pkg : packageMap.values()) {
                if (!pkg.getName().equals("beast2")) {
                    packageList.add(pkg);
                    list.add(new Package0(pkg));
                } else {
                	list.add(0, new Package0(pkg));
                }
            }
            packages = FXCollections.observableArrayList(list);
            if (dataTable != null) {
            	dataTable.setItems(packages);
            	dataTable.refresh();
            }
        } catch (PackageManager.PackageListRetrievalException e) {
        	StringBuilder msgBuilder = new StringBuilder(e.getMessage() + "\n");
            if (e.getCause() instanceof IOException)
                msgBuilder.append(NO_CONNECTION_MESSAGE.replaceAll("\\.", ".\n"));

        	try {
        		Platform.runLater(() -> Alert.showMessageDialog(null, msgBuilder.toString()));
        	} catch (Exception e0) {
        		e0.printStackTrace();
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Package getSelectedPackage(int selectedRow) {
        if (packageList.size() <= selectedRow)
            throw new IllegalArgumentException("Incorrect row " + selectedRow +
                    " is selected from package list, size = " + packageMap.size());
        return packageList.get(selectedRow);
    }

    private void showDetail(Package aPackage) {
        //custom title, no icon
        String msg = "Installed version: " + (aPackage.isInstalled() ? aPackage.getInstalledVersion() : "NA") + "\n";
        msg += "Latest version: " + (aPackage.isAvailable() ? aPackage.getLatestVersion() : "NA") + "\n";
        msg += "" + aPackage.getDescription() +"\n";

        Alert.showMessageDialog(null,
                msg,
                aPackage.getName(),
                Alert.PLAIN_MESSAGE);
    }

    private HBox createButtonBox() {
        HBox box = FXUtils.newHBox();
        final CheckBox latestVersionCheckBox = new CheckBox("Latest");
        latestVersionCheckBox.setTooltip(new Tooltip("If selected, only the latest version is installed when hitting the Install/Upgrade button. "
        		+ "Otherwise, you can select from a list of available versions."));
        box.getChildren().add(latestVersionCheckBox);
        latestVersionCheckBox.setOnAction(e -> {
        	CheckBox checkBox = (CheckBox) e.getSource();
        	useLatestVersion = checkBox.isSelected();
        });
        latestVersionCheckBox.setSelected(useLatestVersion);
        Button installButton = new Button("Install/Upgrade");
        installButton.setOnAction(e -> {
            // first get rid of existing packages
        	List<Integer> selectedRows = new ArrayList<>();
        	for (int i : dataTable.getSelectionModel().getSelectedIndices()) {
        		selectedRows.add(i);
        	}
            String installedPackageNames = "";

            setCursor(Cursor.WAIT);

            Map<Package, PackageVersion> packagesToInstall = new HashMap<>();
            PackageManager.useArchive(!useLatestVersion);
            for (int selRow : selectedRows) {
                Package selPackage = getSelectedPackage(selRow);
                if (selPackage != null) {
                	if (useLatestVersion) {
                		packagesToInstall.put(selPackage, selPackage.getLatestVersion());
                	} else {
                		PackageVersion version = (PackageVersion) Alert.showInputDialog( null, "Select Version for " + selPackage.getName(), 
                				"Select version", 
                				Alert.QUESTION_MESSAGE, null, 
                				selPackage.getAvailableVersions().toArray(), selPackage.getAvailableVersions().toArray()[0]);
                		if (version == null) {
                			return;
                		}
                		packagesToInstall.put(selPackage, version);
                	}
                }
            }

            try {
            	try {
            		populatePackagesToInstall(packageMap, packagesToInstall);
            	} catch (IllegalArgumentException ex) {
                    setCursor(Cursor.DEFAULT);
            		Alert.showMessageDialog(null, "Something went wrong (" + ex.getMessage() + ") "
            				+ "\n\nPossibly the package is installed by hand and "
            				+ "there is no package repository containing the package. Such packages "
            				+ "cannot be upgraded.\n\nSet up the appropriate package repository to "
            				+ "upgrade the package."
            				);
            		return;
            	}
                populatePackagesToInstall(packageMap, packagesToInstall);

                prepareForInstall(packagesToInstall, false, null);

                if (getToDeleteListFile().exists()) {
                    Alert.showMessageDialog(box,
                            "Upgrading packages on your machine requires BEAUti " +
                                    "to restart. Shutting down now.");
                    System.exit(0);
                }

                installPackages(packagesToInstall, false, null);

                // Refresh classes:
                loadExternalJars();

                installedPackageNames = String.join(",",
                        packagesToInstall.keySet().stream()
                                .map(Package::toString)
                                .collect(Collectors.toList()));

                setCursor(Cursor.DEFAULT);

            } catch (DependencyResolutionException | IOException ex) {
                Alert.showMessageDialog(null, "Install failed because: " + ex.getMessage());
                setCursor(Cursor.DEFAULT);
            }

            resetPackages();
            if (selectedRows.size() > 0)
            	dataTable.getSelectionModel().select(selectedRows.get(0));

            if (installedPackageNames.length()>0)
                Alert.showMessageDialog(null, "Package(s) "
                        + installedPackageNames + " installed. "
                        + "Note that any changes to the BEAUti "
                        + "interface will\n not appear until a "
                        + "new document is created or BEAUti is "
                        + "restarted.");
        });
        box.getChildren().add(installButton);

        Button uninstallButton = new Button("Uninstall");
        uninstallButton.setOnAction(e -> {
            StringBuilder removedPackageNames = new StringBuilder();
            List<Integer> selectedRows = new ArrayList<>();
            for (int i : dataTable.getSelectionModel().getSelectedIndices()) {
            	selectedRows.add(i);
            }

            for (int selRow : selectedRows) {
                Package selPackage = getSelectedPackage(selRow);
                if (selPackage != null) {
                    try {
                        if (selPackage.isInstalled()) {
                            setCursor(Cursor.WAIT);
                            List<String> deps = getInstalledDependencyNames(selPackage, packageMap);

                            if (deps.isEmpty()) {
                                String result = uninstallPackage(selPackage, selPackage.getInstalledVersion(), false, null);

                                if (result != null) {
                                    if (removedPackageNames.length() > 0)
                                        removedPackageNames.append(", ");
                                    removedPackageNames.append("'")
                                            .append(selPackage.getName())
                                            .append(" v")
                                            .append(selPackage.getInstalledVersion())
                                            .append("'");
                                }
                            } else {
                                throw new DependencyResolutionException("package " + selPackage
                                        + " is used by the following packages: "
                                + String.join(", ", deps) + "\n"
                                + "Remove those packages first.");
                            }

                            setCursor(Cursor.DEFAULT);
                        }

                        resetPackages();
                        if (selectedRows.size() > 0)
                        	dataTable.getSelectionModel().select(selectedRows.get(0));
                    } catch (IOException | DependencyResolutionException ex) {
                        Alert.showMessageDialog(null, "Uninstall failed because: " + ex.getMessage());
                        setCursor(Cursor.DEFAULT);
                    }
                }
            }

            if (getToDeleteListFile().exists()) {
                Alert.showMessageDialog(dataTable,
                        "Removing packages on your machine requires BEAUti " +
                                "to restart. Shutting down now.");
                System.exit(0);
            }

            if (removedPackageNames.length()>0)
                Alert.showMessageDialog(null, "Package(s) "
                        + removedPackageNames.toString() + " removed. "
                        + "Note that any changes to the BEAUti "
                        + "interface will\n not appear until a "
                        + "new document is created or BEAUti is "
                        + "restarted.");
        });
        box.getChildren().add(uninstallButton);

        Button packageRepoButton = new Button("Package repositories");
        packageRepoButton.setOnAction(e -> {
                JPackageRepositoryDialog dlg = new JPackageRepositoryDialog(dlgPane);
                dlg.setVisible(true);
                resetPackages();
            });
        box.getChildren().add(packageRepoButton);


        Button button = new Button("?");
        button.setTooltip(new Tooltip(getPackageUserDir() + " " + getPackageSystemDir()));
        button.setOnAction(e -> {
                Alert.showMessageDialog(dataTable, "By default, packages are installed in \n\n" + getPackageUserDir() +
                        "\n\nand are available only to you.\n" +
                        "\nPackages can also be moved manually to \n\n" + getPackageSystemDir() +
                        "\n\nwhich makes them available to all users\n"
                        + "on your system.", "Repository directory", null);
            });
        box.getChildren().add(button);
        
        closeButton = new Button("Close");
        closeButton.setOnAction(e -> {
        	// set a result (any object but 'null' will do) to be able to close the dialog
        	if (packageMap != null && packageMap.size() > 0) {
        		dlg.setResult((Package)packageMap.values().toArray()[0]);
        	}
        	dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        	dlg.close();
        });
        closeButton.setOnKeyReleased(e-> {
        	if (e.getCode().equals(KeyCode.ESCAPE)) {
        		dlg.setResult((Package)packageMap.values().toArray()[0]);
        		dlg.close();
        	}
        });
        Region spacer = new Region();
        spacer.setMinWidth(50);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        box.getChildren().add(spacer);
        box.getChildren().add(closeButton);
        
        for (Node n : box.getChildren()) {
        	if (n instanceof Control) {
        		((Control)n).setMinWidth(140);
        		((Control)n).setPadding(new Insets(5));
        	}
        }        
        return box;
    }

	private static Dialog<Package> dlg;
    private static Button closeButton;
    
	public static Dialog<Package> asDialog(Parent pane) {
		dlg = new Dialog<>();
		dlg.setDialogPane(new JPackageDialog());
		Window    window = dlg.getDialogPane().getScene().getWindow();
		window.setOnCloseRequest(event -> window.hide());
		
		dlg.setTitle("BEAST 2 Package Manager");
		dlg.setResizable(true);
		
		Window stage = null;
		
		if (pane == null) {
			stage = dlg.getDialogPane().getScene().getWindow();
		} else {
			stage = pane.getScene().getWindow();
		}
		
        dlg.setX(stage.getX() + stage.getWidth() / 2 - dlg.getWidth() / 2);
        dlg.setY(stage.getY() + stage.getHeight() / 2 - dlg.getHeight() / 2);
        
        if (pane != null) {
        	pane.setCursor(Cursor.DEFAULT);
        }

        closeButton.setCancelButton(true);
        closeButton.requestFocus();
        return dlg;
	}


	DialogPane dlgPane = null;

}
