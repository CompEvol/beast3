package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.Start;

import beastfx.app.beauti.Beauti;
import beastfx.app.beauti.BeautiTabPane;
import beastfx.app.inputeditor.BeautiDoc;
import javafx.stage.Stage;

public class BeautiCLITest extends BeautiBase {

	BeautiDoc doc;
	
	@Start
    public void start(Stage stage) {
    	try {
    		System.setProperty("beast.is.junit.testing", "true");
    		BeautiTabPane tabPane = BeautiTabPane.main2(new String[] {}, stage);
    		this.doc = tabPane.doc;
            stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	public static void setUpTestDir() {
		// make sure output goes to test directory
		File testDir = 	new File("./test");
		if (!testDir.exists()) {
			testDir.mkdir();
		}
		System.setProperty("file.name.prefix","test/");
	}

	{
		setUpTestDir();
	}
	
    String fileName = "test/tmp123x666.xml";
    String templateFile = "test/template123x666.xml";

    @Test
    // test that beauti can merge an alignment with a template and write out a file
    // this requires that the standard template can be read
    public void testStandarBatchMode() {
        BeautiDoc doc = new BeautiDoc();
        try {
            doc.processTemplate(BeautiBase.TEMPLATE_DIR + "/Standard.xml");
        } catch (Exception e) {
            assertEquals(true, false);
        }

        // ignore test if no X11 display available
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            File f = new File(fileName);
            if (f.exists()) {
                f.delete();
            }
            Beauti.main(("-template " + BeautiBase.TEMPLATE_DIR + "/Standard.xml -nex " + BeautiBase.NEXUS_DIR + "/dna.nex -out " + fileName + " -exitaction writexml").split(" "));
            f = new File(fileName);
            assertEquals(f.exists() && f.length() > 0, true);
        }
    }

    @Test
    // as testStandarBatchMode() but for the *Beast template
    public void testStarBeastBatchMode() {
    	BeautiDoc doc = new BeautiDoc();
        try {
            doc.processTemplate(BeautiBase.TEMPLATE_DIR + "/StarBeast.xml");
        } catch (Exception e) {
            assertEquals(true, false);
        }
        // ignore test if no X11 display available
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            File f = new File(fileName);
            if (f.exists()) {
                f.delete();
            }
            Beauti.main(("-template " + BeautiBase.TEMPLATE_DIR + "/StarBeast.xml -nex " + BeautiBase.NEXUS_DIR + "/26.nex -nex " + BeautiBase.NEXUS_DIR + "/29.nex -out " + fileName + " -exitaction writexml").split(" "));
            f = new File(fileName);
            assertEquals(f.exists() && f.length() > 0, true);
        }
    }


    // test that a dataset can be merged with a simple template
    String template = "<beast version='2.0'  namespace='beast.base.evolution.alignment:beast.base.core:beast.base.evolution.tree.coalescent:beast.base.util:beast.base.evolution.operator:beast.base.evolution.sitemodel:beast.base.evolution.substitutionmodel:beast.base.evolution.likelihood'>\n" +
    		"<data id='data' dataType='nucleotide'>\n" +
    		"    <sequence taxon='human'>\n" +
    		"        AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGGAGCTTAAACCCCCTTATTTCTACTAGGACTATGAGAATCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAATACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTG-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGACCAATGGGACTTAAACCCACAAACACTTAGTTAACAGCTAAGCACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCGGAGCTTGGTAAAAAGAGGCCTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGGCCTCCATGACTTTTTCAAAAGGTATTAGAAAAACCATTTCATAACTTTGTCAAAGTTAAATTATAGGCT-AAATCCTATATATCTTA-CACTGTAAAGCTAACTTAGCATTAACCTTTTAAGTTAAAGATTAAGAGAACCAACACCTCTTTACAGTGA\n" +
    		"    </sequence>\n" +
    		"   <sequence taxon='chimp'>\n" +
    		"        AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTCAAATCCCCTTATTTCTACTAGGACTATAAGAATCGAACTCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTAAGCACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCTCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AACCCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCGACACCTCTTTACAGTGA\n" +
    		"   </sequence>\n" +
    		"</data>\n" +
    		"    <input spec='HKY' id='hky'>\n" +
    		"        <kappa idref='hky.kappa'/>\n" +
    		"        <frequencies id='freqs' spec='Frequencies'>\n" +
    		"            <data idref='data'/>\n" +
    		"        </frequencies>\n" +
    		"    </input>\n" +
    		"    <input spec='SiteModel' id='siteModel' gammaCategoryCount='1'>\n" +
    		"        <substModel idref='hky'/>\n" +
    		"    </input>\n" +
    		"    <input spec='TreeLikelihood' id='treeLikelihood'>\n" +
    		"        <data idref='data'/>\n" +
    		"        <tree idref='tree'/>\n" +
    		"        <siteModel idref='siteModel'/>\n" +
    		"    </input>\n" +
    		"    <parameter id='hky.kappa' value='1.0' lower='0.0'/>\n" +
    		"    <tree spec='beast.base.evolution.tree.coalescent.RandomTree' id='tree' taxa='@data'>\n" +
    		"        <populationModel spec='ConstantPopulation'>\n" +
    		"		<popSize spec='beast.base.inference.parameter.RealParameter' value='1'/>\n" +
    		"	</populationModel>\n" +
    		"    </tree>\n" +
    		"    <run spec='beast.base.inference.MCMC' id='mcmc' chainLength='10000000'>\n" +
    		"	<distribution spec='beast.base.inference.CompoundDistribution' id='posterior'>\n" +
    		"        	<distribution id='likelihood' idref='treeLikelihood'/>\n" +
    		"	</distribution>\n" +
    		"        <operator id='kappaScaler' spec='ScaleOperator' scaleFactor='0.5' weight='1' parameter='@hky.kappa'/>\n" +
    		"        <operator id='treeScaler' spec='ScaleOperator' scaleFactor='0.5' weight='1' tree='@tree'/>\n" +
    		"        <operator spec='Uniform' weight='10' tree='@tree'/>\n" +
    		"        <operator spec='SubtreeSlide' weight='5' gaussian='true' size='1.0' tree='@tree'/>\n" +
    		"        <operator id='narrow' spec='Exchange' isNarrow='true' weight='1' tree='@tree'/>\n" +
    		"        <operator id='wide' spec='Exchange' isNarrow='false' weight='1' tree='@tree'/>\n" +
    		"        <operator spec='WilsonBalding' weight='1' tree='@tree'/>\n" +
    		"        <logger logEvery='10000' fileName='test.$(seed).log'>\n" +
    		"	        <model idref='likelihood'/>\n" +
    		"            <log idref='likelihood'/>\n" +
    		"            <log idref='hky.kappa'/>\n" +
    		"            <log spec='beast.base.evolution.tree.TreeHeightLogger' tree='@tree'/>\n" +
    		"        </logger>\n" +
    		"        <logger logEvery='10000' fileName='test.$(seed).trees'>\n" +
    		"            <log idref='tree'/>\n" +
    		"        </logger>\n" +
    		"        <logger logEvery='10000'>\n" +
    		"	        <model idref='likelihood'/>\n" +
    		"            <log idref='likelihood'/>\n" +
    		"    	    <ESS spec='beast.base.inference.util.ESS' name='log' arg='@likelihood'/>\n" +
    		"            <log idref='hky.kappa'/>\n" +
    		"    	    <ESS spec='beast.base.inference.util.ESS' name='log' arg='@hky.kappa'/>\n" +
    		"        </logger>\n" +
    		"    </run>\n" +
    		"</beast>";
    @Test
    public void testCustomBatchMode() {
    	BeautiDoc doc = new BeautiDoc();
        try {
        	PrintStream out = new PrintStream(templateFile);
        	out.print(template);
        	out.close();
            doc.processTemplate(templateFile);
        } catch (Exception e) {
            assertEquals(true, false);
        }
        // ignore test if no X11 display available
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            File f = new File(fileName);
            if (f.exists()) {
                f.delete();
            }
            Beauti.main(("-template " + templateFile + " -nex " + BeautiBase.NEXUS_DIR + "/anolis.nex -out " + fileName + " -exitaction writexml").split(" "));
            f = new File(fileName);
            assertEquals(f.exists() && f.length() > 0, true);
        }
    }

}
