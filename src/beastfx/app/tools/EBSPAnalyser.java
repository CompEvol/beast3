package beastfx.app.tools;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import beastfx.app.util.LogFile;
import beastfx.app.util.OutFile;
import beast.base.core.BEASTVersion2;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.Runnable;
import beast.base.spec.evolution.tree.coalescent.CompoundPopulationFunction;
import beast.base.spec.evolution.tree.coalescent.CompoundPopulationFunction.Type;
import beast.base.util.DiscreteStatistics;
import beast.base.util.HeapSort;

@Description("Application to convert output of a BEAST EBSP analysis into a table with population history estimates through time")
public class EBSPAnalyser extends Runnable {
	public Input<LogFile> inputFileInput = new Input<>("inputFile", "Input file produced by EBSP analysis. Must be specified.", new LogFile("not selected"));
	public Input<Type> fileTypeInput = new Input<>("fileType", "type of file, either linear or stepwise", Type.LINEAR, Type.values());
	public Input<Integer> burninPercentageInput = new Input<>("burnIn", "Percentage of cases to be disregarded as burn-in", 10);
	public Input<OutFile> outputFileInput = new Input<>("outputFile", "Output file where EBSPAnalyser results will be stored (stdout if not specified)", new OutFile("not selected"));
	
	
    String m_sFileOut;
    PrintStream m_out = System.out;
    CompoundPopulationFunction.Type m_type = Type.LINEAR;
    String m_sInputFile;
    int m_nBurninPercentage = 10;
    
	@Override
	public void initAndValidate() {
        BEASTVersion2 version = new BEASTVersion2();
        final String versionString = version.getVersionString();
        String aboutString = "EBSPAnalyser " + versionString + ", " + version.getDateString() + "\n" +
                "                        by\n" +
                "          Joseph Heled and Remco Bouckaert\n" +
                "Department of Computer Science, University of Auckland\n" +
                "                  jheled@gmail.com\n" +
                "               remco@cs.auckland.ac.nz\n" +
                "             Part of the BEAST 2 package\n" +
                "                http://www.beast2.org\n" +
                "\n";
        Log.warning(aboutString);
	}


	@Override
    public void run() throws IOException {
		m_sInputFile = inputFileInput.get().getAbsolutePath();		
		m_nBurninPercentage = burninPercentageInput.get();
		m_type = fileTypeInput.get();
		m_out = outputFileInput.get().getName().equals("not selected") ?
				System.out :
				new PrintStream(outputFileInput.get());
        parse(m_sInputFile, m_nBurninPercentage, m_type, m_out);
    }

    void parse(String fileName, int burnInPercentage, CompoundPopulationFunction.Type type, PrintStream out) throws IOException {
        logln("Processing " + fileName);
        BufferedReader fin = new BufferedReader(new FileReader(fileName));
        String str;
        int data = 0;
        // first, sweep through the log file to determine size of the log
        while (fin.ready()) {
            str = fin.readLine();
            // terrible hackish code, must improve later
            if( str.charAt(0) == '#' ) {
                int i = str.indexOf("spec=");
                if( i > 0 ) {
                   if( str.indexOf("type=\"stepwise\"") > 0 ) {
                      m_type = Type.STEPWISE;
                   }  else if( str.indexOf("type=\"linear\"") > 0 ) {
                      m_type = Type.LINEAR;
                   }
                }
            }
            if (str.indexOf('#') < 0 && str.matches(".*[0-9a-zA-Z].*")) {
                data++;
            }
        }
        final int burnIn = data * burnInPercentage / 100;
        logln(" skipping " + burnIn + " line\n\n");
        data = -burnIn - 1;
        fin.close();
        fin = new BufferedReader(new FileReader(fileName));

        // process log
        final List<List<Double>> times = new ArrayList<>();
        final List<List<Double>> popSizes = new ArrayList<>();
        double[] alltimes = null;
        while (fin.ready()) {
            str = fin.readLine();
            if (str.indexOf('#') < 0 && str.matches(".*[0-9a-zA-Z].*")) {
                if (++data > 0) {
                    final String[] strs = str.split("\t");
                    final List<Double> times2 = new ArrayList<>();
                    final List<Double> popSizes2 = new ArrayList<>();
                    if (alltimes == null) {
                        alltimes = new double[strs.length - 1];
                    }
                    for (int i = 1; i < strs.length; i++) {
                        final String[] strs2 = strs[i].split(":");
                        final Double time = Double.parseDouble(strs2[0]);
                        alltimes[i - 1] += time;
                        if (strs2.length > 1) {
                            times2.add(time);
                            popSizes2.add(Double.parseDouble(strs2[1]));
                        }
                    }
                    times.add(times2);
                    popSizes.add(popSizes2);

                }
            }
        }

        if (alltimes == null) {
            //burn-in too large?
            return;
        }

        // take average of coalescent times
        for (int i = 0; i < alltimes.length; i++) {
            alltimes[i] /= times.size();
        }

        // generate output
        out.println("time\tmean\tmedian\t95HPD lower\t95HPD upper");
        final double[] popSizeAtTimeT = new double[times.size()];
        int[] indices = new int[times.size()];

        for (final double time : alltimes) {
            for (int j = 0; j < popSizeAtTimeT.length; j++) {
                popSizeAtTimeT[j] = calcPopSize(type, times.get(j), popSizes.get(j), time);
            }

            HeapSort.sort(popSizeAtTimeT, indices);

            out.print(time + "\t");

            out.print(DiscreteStatistics.mean(popSizeAtTimeT) + "\t");
            out.print(DiscreteStatistics.median(popSizeAtTimeT) + "\t");

            double[] hpdInterval = DiscreteStatistics.HPDInterval(0.95, popSizeAtTimeT, indices);
            out.println(hpdInterval[0] + "\t" + hpdInterval[1]);
        }
    }

    private double calcPopSize(CompoundPopulationFunction.Type type, List<Double> xs, List<Double> ys, double d) {
        // TODO completely untested
        // assume linear
        //assert typeName.equals("Linear");

        final int n = xs.size();
        final double xn = xs.get(n - 1);
        if (d >= xn) {
            return ys.get(n - 1);
        }
        assert d >= xs.get(0);

        int i = 1;
        while (d >= xs.get(i)) {
            ++i;
        }
        // d < xs.get(i)

        double x0 = xs.get(i-1);
        double x1 = xs.get(i);
        double y0 = ys.get(i-1);
        double y1 = ys.get(i);
        assert x0 <= d && d <= x1 : "" + x0 + "," + x1 + "," + d;
        switch (type) {
            case LINEAR:
                final double p = (d * (y1 - y0) + (y0 * x1 - y1 * x0)) / (x1 - x0);
                assert p > 0;
                return p;
            case STEPWISE:
                assert y1 > 0;
                return y1;
        }
        return 0;
    }


    protected void log(String s) {
    	Log.warning.print(s);
    }

    protected void logln(String s) {
    	Log.warning.println(s);
    }

//
//    public class EBSPAnalyserDialog {
//        private final JFrame frame;
//
//        private final OptionsPanel optionPanel;
//
//        private final JTextField inputFileNameText = new JTextField("not selected", 16);
//        private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{"linear", "stepwise"});
//
//        final WholeNumberField burninText = new WholeNumberField(0, Long.MAX_VALUE);
//        private final JTextField outputFileNameText = new JTextField("not selected", 16);
//
//        private File outputFile = null;
//        private File inputFile = null;
//
//        public EBSPAnalyserDialog(final JFrame frame, String titleString, Icon icon) {
//            this.frame = frame;
//
//            optionPanel = new OptionsPanel(12, 12);
//
//            final JLabel titleText = new JLabel(titleString);
//            titleText.setIcon(icon);
//            optionPanel.addSpanningComponent(titleText);
//            Font font = UIManager.getFont("Label.font");
//            titleText.setFont(new Font("sans-serif", font.getStyle(), font.getSize()));
//
//            JPanel panel = new JPanel(new BorderLayout());
//            panel.setOpaque(false);
//
//            JButton button = new JButton("Choose Input File...");
//            button.addActionListener(ae -> {
//                    File file = Utils.getLoadFile("Select input file...");
//                    if (file == null) {
//                        // the dialog was cancelled...
//                        return;
//                    }
//
//                    inputFile = file;
//                    inputFileNameText.setText(inputFile.getName());
//
//                });
//            inputFileNameText.setEditable(false);
//
//            JButton button2 = new JButton("Choose Output File...");
//            button2.addActionListener(ae -> {
//                    File file = Utils.getSaveFile("Select output file...");
//                    if (file == null) {
//                        // the dialog was cancelled...
//                        return;
//                    }
//
//                    outputFile = file;
//                    outputFileNameText.setText(outputFile.getName());
//
//                });
//            outputFileNameText.setEditable(false);
//
//            JPanel panel1 = new JPanel(new BorderLayout(0, 0));
//            panel1.add(inputFileNameText, BorderLayout.CENTER);
//            panel1.add(button, BorderLayout.EAST);
//            optionPanel.addComponentWithLabel("Input File: ", panel1);
//
//            optionPanel.addComponentWithLabel("File type: ", typeCombo);
//
//            burninText.setColumns(12);
//            burninText.setValue(10);
//            optionPanel.addComponentWithLabel("Burn in percentage: ", burninText);
//
//            optionPanel.addSpanningComponent(panel);
//
//            JPanel panel3 = new JPanel(new BorderLayout(0, 0));
//            panel3.add(outputFileNameText, BorderLayout.CENTER);
//            panel3.add(button2, BorderLayout.EAST);
//            optionPanel.addComponentWithLabel("Output File: ", panel3);
//        }
//
//        public boolean showDialog(String title) {
//
//            JOptionPane optionPane = new JOptionPane(optionPanel,
//                    JOptionPane.PLAIN_MESSAGE,
//                    JOptionPane.OK_CANCEL_OPTION,
//                    null,
//                    new String[]{"Run", "Quit"},
//                    null);
//            optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));
//
//            final JDialog dialog = optionPane.createDialog(frame, title);
//            dialog.pack();
//
//            dialog.setVisible(true);
//
//            return optionPane.getValue().equals("Run");
//        }
//
//        public String getOutputFileName() {
//            if (outputFile == null) return null;
//            return outputFile.getPath();
//        }
//
//        public String[] getArgs() {
//            java.util.List<String> args = new ArrayList<>();
//            if (inputFile != null) {
//                args.add("-i");
//                args.add(inputFile.getPath());
//            }
//            args.add("-burnin");
//            args.add(burninText.getText());
//            args.add("-type");
//            args.add(typeCombo.getSelectedItem().toString());
//            if (outputFile != null) {
//                args.add("-o");
//                args.add(outputFile.getPath());
//            }
//            return args.toArray(new String[0]);
//        }
//
//    }


    public static void main(String[] args) {
        BEASTVersion2 version = new BEASTVersion2();
        final String versionString = version.getVersionString();
        String title = "EBSP Analyser " + versionString;

        try {
    		new Application(new EBSPAnalyser(), title, 600, 300, args);
    		if (args.length == 0) {
    			System.out.println("Finished - Quit program to exit.");
    		}
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
