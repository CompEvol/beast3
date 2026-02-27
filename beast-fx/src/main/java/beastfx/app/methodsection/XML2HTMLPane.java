package beastfx.app.methodsection;


import static javafx.concurrent.Worker.State.FAILED;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javafx.application.*;
import javafx.beans.value.*;
import javafx.concurrent.*;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.web.*;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;

import beast.base.core.BEASTInterface;
import beast.base.inference.MCMC;
import beast.base.parser.XMLParser;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.implementation.BEASTObjectMethodsText;



public class XML2HTMLPane extends JPanel {
	private static final long serialVersionUID = 1L;

	private final JFXPanel jfxPanel = new JFXPanel();
	private WebEngine engine;
	private final JPanel panel = new JPanel(new BorderLayout());
	double zoom = 1.0;

	
	BeautiDoc beautiDoc;
	String html;
	List<Phrase> m;
	XML2Text xml2textProducer;
	
	File tmpFile = null;

	
	public XML2HTMLPane(String [] args) throws Exception {
		this();
		
		beautiDoc = new BeautiDoc();
		File file = new File(args[0]);
		beautiDoc.setFileName(file.getAbsolutePath());
		beautiDoc.beautiConfig = new BeautiConfig();
		beautiDoc.beautiConfig.initAndValidate();		
		String xml = BeautiDoc.load(file);
		int i = xml.indexOf("beautitemplate=");
		if (i > 0) {
			i += 15;
			char c = xml.charAt(i);
			i++;
			int start = i;
			while (xml.charAt(i) != c) {
				i++;
			}
			String template = xml.substring(start, i);
			if (!template.endsWith("xml")) {
				template = template + ".xml";
			}
			beautiDoc.loadNewTemplate(template);
		} else {
			beautiDoc.loadNewTemplate("Standard.xml");
		}
		
		XMLParser parser = new XMLParser();
		MCMC mcmc = (MCMC) parser.parseFile(file);
		beautiDoc.mcmc.setValue(mcmc, beautiDoc);
		for (BEASTInterface o : InputFilter.getDocumentObjects(beautiDoc.mcmc.get())) {
			beautiDoc.registerPlugin(o);
		}
		beautiDoc.determinePartitions();
		BEASTObjectMethodsText.setBeautiCFG(beautiDoc.beautiConfig);
		
		MethodsText.initNameMap();
		initialise((MCMC) beautiDoc.mcmc.get(), true);		
	}
	
	final static String header = "<!DOCTYPE html>\n" +
			"<html>\n" +
			"<style>\n" +
			".reference {font-size:10pt;color:#aaa;}\n" + 
			"a{color:#555;text-decoration:none;background-color:#fafafa;}\n" + 
			".pe {color:#555;background-color:#fafafa;}\n" + 
			".para {color:#555;background-color:#fafafa;}\n" + 
			"select{color:#555;font-weight:normal;-webkit-appearance:none;background-color:#fafafa;border-width:5pt;}\n" + 
			"</style>\n" +
			"<body style='font: 12pt arial, sans-serif;'>\n";
	
	public void initialise(MCMC mcmc, boolean update) throws Exception {		
		xml2textProducer = new XML2Text(beautiDoc);
		xml2textProducer.initialise((MCMC) beautiDoc.mcmc.get());
		m = xml2textProducer.getPhrases();
		
		html = header + Phrase.toHTML(beautiDoc, m) + "</body>\n</html>";
		
        FileWriter outfile = new FileWriter("/tmp/index.html");
        outfile.write(html);
        outfile.close();
		
        if (update) {
        	updateState(html);
        } else {
        	load(html);
        }
	}

	
	public XML2HTMLPane() {
		setLayout(new BorderLayout());

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initComponents();
			}
		});

		JScrollPane scroller = new JScrollPane(panel);
		add(scroller, BorderLayout.CENTER);

//		try {
////			//updateState(new URL("file://" + "/Users/remco/workspace/beasy/src/methods/index.html"));
//			updateState(new URL("file://" + "/tmp/index.html"));
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}

	} // c'tor

	private void initComponents() {
		createScene();

		JPanel statusBar = new JPanel(new BorderLayout(5, 0));
		statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

		panel.add(jfxPanel, BorderLayout.CENTER);
		panel.add(statusBar, BorderLayout.SOUTH);

		jfxPanel.setPreferredSize(new Dimension(400, 400));
		setPreferredSize(new Dimension(400, 400));
	}

	private void createScene() {

		Platform.runLater(new Runnable() {
			@Override
			public void run() {

				WebView view = new WebView();
				engine = view.getEngine();

				engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
					@Override
					public void changed(ObservableValue ov, State oldState, State newState) {
						if (newState == Worker.State.SUCCEEDED) {
							// note next classes are from org.w3c.dom domain
							EventListener listener = new EventListener() {
								@Override
								public void handleEvent(org.w3c.dom.events.Event evt) {
									String href = ((Element) evt.getTarget()).getAttribute("href");
									System.out.println("link:" + href);
									// goToLink(href);
								}
							};

							org.w3c.dom.Document doc = engine.getDocument();
							// Element el = doc.getElementById("a");
							NodeList lista = doc.getElementsByTagName("a");
							for (int i = 0; i < lista.getLength(); i++) {
								((org.w3c.dom.events.EventTarget) lista.item(i)).addEventListener("click", listener, false);
							}
						}
					}
				});
				engine.titleProperty().addListener(new ChangeListener<String>() {
					@Override
					public void changed(ObservableValue<? extends String> observable, String oldValue,
							final String newValue) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								// System.out.println(newValue);
							}
						});
					}
				});

				engine.setOnStatusChanged(new EventHandler<WebEvent<String>>() {
					@Override
					public void handle(final WebEvent<String> event) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								System.out.println("status changed:");
								System.out.println(event.getData());
//								ModelEditor me = new ModelEditor();
//								if (me.handleCmd(event.getData(), beautiDoc, panel)) {
//									beautiDoc.determinePartitions();
//									beautiDoc.scrubAll(false, false);
//									CitationPhrase.citations.clear();
//
//									MethodsText.clear();
//									try {
//										initialise((MCMC) beautiDoc.mcmc.get());
//									} catch (Exception e) {
//										// TODO Auto-generated catch block
//										e.printStackTrace();
//									}
//								}
							}
						});
					}
				});

				engine.locationProperty().addListener(new ChangeListener<String>() {
					@Override
					public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue) {
//						SwingUtilities.invokeLater(new Runnable() {
//							@Override
//							public void run() {
								System.out.println("changed:");
								System.out.println(newValue);
//								ModelEditor me = new ModelEditor(false);
//								if (me.handleCmd(newValue, beautiDoc, panel)) {
//									beautiDoc.determinePartitions();
//									beautiDoc.scrubAll(false, false);
//									CitationPhrase.citations.clear();
//
//									MethodsText.clear();
//									try {
//										initialise((MCMC) beautiDoc.mcmc.get(), false);
//										load(html);
//									} catch (Exception e) {
//										// TODO Auto-generated catch block
//										e.printStackTrace();
//									}
//								}
							}
//						});
//					}
				});

				engine.getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {

					@Override
					public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
						if (engine.getLoadWorker().getState() == FAILED) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									JOptionPane.showMessageDialog(panel,
											(value != null) ? engine.getLocation() + "\n" + value.getMessage()
													: engine.getLocation() + "\nUnexpected error.",
											"Loading error...", JOptionPane.ERROR_MESSAGE);
								}
							});
						}
					}
				});

				jfxPanel.setScene(new Scene(view));
				jfxPanel.addKeyListener(new KeyListener() {

					@Override
					public void keyTyped(java.awt.event.KeyEvent e) {
					}

					@Override
					public void keyReleased(java.awt.event.KeyEvent e) {
						System.out.println(e);
						if (e.getKeyChar() == '-') {
							zoomOut();
						}
						if (e.getKeyChar() == '+') {
							zoomIn();
						}

					}

					@Override
					public void keyPressed(java.awt.event.KeyEvent e) {
					}
				});

			}
		});
	}
	
	/**
	 * change html text and enable/disable buttons (where appropriate) *
	 */
	void updateState(Object page) {
		if (page instanceof String) {

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					load((String) page);
					//engine.loadContent((String) page);
				}

			});
			zoom(zoom);
		} else if (page instanceof URL) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					String tmp = null;
					try {
						tmp =  new URL("" + page).toExternalForm();
					} catch (MalformedURLException e) {
						try {
							tmp =  new URL("http://" + page).toExternalForm();
						} catch (MalformedURLException e2) {
							return;
						}
					}

					engine.load(tmp);
				}
			});
			zoom(zoom);
		}
	} // updateState

	public void load(String page) {
		try {
			if (tmpFile == null) {
				tmpFile = File.createTempFile("index", ".html");
				System.err.println(tmpFile.getPath());
			}
			FileWriter outfile = new FileWriter(tmpFile);
			outfile.write(html);
			outfile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// the following does not appear to be
		// able the handle select.onchanged events
		// but loading from file does -- not sure why
//		engine.loadContent((String)page, "text/html");
//		engine.setJavaScriptEnabled(true);
		engine.load("file://" + tmpFile);
	}

	
	private void zoomIn() {
		zoom(zoom * 1.1);
	}

	private void zoomOut() {
		zoom(zoom / 1.1);

	}

	void zoom(double _zoom) {
		this.zoom = _zoom;

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				engine.executeScript("document.body.style.zoom=" + zoom);
			}
		});
	}
	
	public String getText(CitationPhrase.mode mode) throws Exception {		
		CitationPhrase.CitationMode = mode;
		xml2textProducer = new XML2Text(beautiDoc);
		xml2textProducer.initialise((MCMC) beautiDoc.mcmc.get());
		m = xml2textProducer.getPhrases();
		return Phrase.toString(m);
	}

	public static void main(String[] args) throws Exception {
		JFrame frame = new JFrame();
		frame.setSize(700, 500);
		XML2HTMLPane textPane = new XML2HTMLPane(args);
		frame.add(textPane);
//		frame.add(new XML2HTMLPane());
        JButton copyButton = new JButton("Copy text to clipboard");
        frame.add(copyButton, BorderLayout.SOUTH);
        copyButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = JOptionPane.showOptionDialog(frame, "Choose format", "Copy text",
						JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, 
						CitationPhrase.mode.values(), CitationPhrase.CitationMode);
				CitationPhrase.mode mode = CitationPhrase.mode.values()[i];
				StringSelection stringSelection;
				try {
					stringSelection = new StringSelection(textPane.getText(mode));
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(stringSelection, null);				
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}
