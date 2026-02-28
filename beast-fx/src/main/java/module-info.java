open module beast.fx {
    requires beast.base;
    requires beast.pkgmgmt;
    requires java.xml;
    requires java.desktop;
    requires java.logging;

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires javafx.graphics;
    requires javafx.base;

    // JDK modules
    requires jdk.jsobject;

    // Automatic modules
    requires beagle;
    requires colt;

    // Export all beastfx packages
    exports beastfx.app.beast;
    exports beastfx.app.beastfx;
    exports beastfx.app.beauti;
    exports beastfx.app.beauti.theme;
    exports beastfx.app.draw;
    exports beastfx.app.inputeditor;
    exports beastfx.app.inputeditor.spec;
    exports beastfx.app.methodsection;
    exports beastfx.app.methodsection.implementation;
    exports beastfx.app.methodsection.implementation.spec;
    exports beastfx.app.methodsection.objecteditor;
    exports beastfx.app.seqgen;
    exports beastfx.app.tools;
    exports beastfx.app.treeannotator;
    exports beastfx.app.treeannotator.services;
    exports beastfx.app.util;

    // ServiceLoader service types
    uses beast.base.core.BEASTInterface;
    uses beast.base.evolution.datatype.DataType;
    uses beast.base.inference.ModelLogger;
    uses beastfx.app.beauti.ThemeProvider;
    uses beastfx.app.inputeditor.InputEditor;
    uses beastfx.app.inputeditor.AlignmentImporter;
    uses beastfx.app.beauti.PriorProvider;
    uses beastfx.app.treeannotator.services.TopologySettingService;
    uses beastfx.app.treeannotator.services.NodeHeightSettingService;

    // Service providers

    provides beastfx.app.beauti.ThemeProvider with
        beastfx.app.beauti.theme.Default,
        beastfx.app.beauti.theme.Dark,
        beastfx.app.beauti.theme.Bootstrap,
        beastfx.app.beauti.theme.Fluent,
        beastfx.app.beauti.theme.Win7,
        beastfx.app.beauti.theme.Metro;

    provides beastfx.app.inputeditor.InputEditor with
        beastfx.app.inputeditor.BEASTObjectInputEditor,
        beastfx.app.inputeditor.BooleanInputEditor,
        beastfx.app.inputeditor.ConstantInputEditor,
        beastfx.app.inputeditor.DoubleInputEditor,
        beastfx.app.inputeditor.DoubleListInputEditor,
        beastfx.app.inputeditor.EnumInputEditor,
        beastfx.app.inputeditor.FileInputEditor,
        beastfx.app.inputeditor.FileListInputEditor,
        beastfx.app.inputeditor.IntegerInputEditor,
        beastfx.app.inputeditor.IntegerListInputEditor,
        beastfx.app.inputeditor.ListInputEditor,
        beastfx.app.inputeditor.LongInputEditor,
        beastfx.app.inputeditor.OutFileInputEditor,
        beastfx.app.inputeditor.OutFileListInputEditor,
        beastfx.app.inputeditor.ParameterInputEditor,
        beastfx.app.inputeditor.ParametricDistributionInputEditor,
        beastfx.app.inputeditor.SiteModelInputEditor,
        beastfx.app.inputeditor.StringInputEditor,
        beastfx.app.inputeditor.TreeFileInputEditor,
        beastfx.app.inputeditor.TreeFileListInputEditor,
        beastfx.app.inputeditor.XMLFileInputEditor,
        beastfx.app.inputeditor.XMLFileListInputEditor,
        beastfx.app.beauti.PriorInputEditor,
        beastfx.app.beauti.PriorListInputEditor,
        beastfx.app.inputeditor.AlignmentListInputEditor,
        beastfx.app.inputeditor.LogFileInputEditor,
        beastfx.app.inputeditor.LogFileListInputEditor,
        beastfx.app.inputeditor.LoggerListInputEditor,
        beastfx.app.inputeditor.MRCAPriorInputEditor,
        beastfx.app.inputeditor.TaxonSetInputEditor,
        beastfx.app.inputeditor.TaxonSetListInputEditor,
        beastfx.app.inputeditor.TipDatesInputEditor,
        beastfx.app.beauti.ClockModelListInputEditor,
        beastfx.app.beauti.ConstantPopulationInputEditor,
        beastfx.app.beauti.ExponentialPopulationInputEditor,
        beastfx.app.beauti.ScaledPopulationInputEditor,
        beastfx.app.beauti.GeneTreeForSpeciesTreeDistributionInputEditor,
        beastfx.app.beauti.OperatorListInputEditor,
        beastfx.app.beauti.SpeciesTreePriorInputEditor,
        beastfx.app.beauti.StateNodeInitialiserListInputEditor,
        beastfx.app.beauti.StateNodeListInputEditor,
        beastfx.app.beauti.TreeDistributionInputEditor,
        beastfx.app.inputeditor.spec.SiteModelInputEditor,
        beastfx.app.inputeditor.spec.ScalarInputEditor,
        beastfx.app.inputeditor.ScalarDistributionInputEditor,
        beastfx.app.inputeditor.TensorDistributionInputEditor,
        beastfx.app.inputeditor.IIDInputEditor;

    provides beastfx.app.inputeditor.AlignmentImporter with
        beastfx.app.inputeditor.NexusImporter,
        beastfx.app.inputeditor.FastaImporter,
        beastfx.app.inputeditor.XMLImporter;

    provides beastfx.app.beauti.PriorProvider with
        beastfx.app.beauti.MRCAPriorProvider;

    provides beastfx.app.treeannotator.services.TopologySettingService with
        beastfx.app.treeannotator.services.MCCTopologyService,
        beastfx.app.treeannotator.services.MaxSumCladeCrediblityTopologyService,
        beastfx.app.treeannotator.services.UserTargetTreeTopologyService;

    provides beastfx.app.treeannotator.services.NodeHeightSettingService with
        beastfx.app.treeannotator.services.CommonAncestorNodeHeigtService,
        beastfx.app.treeannotator.services.KeepHeightsNodeHeightsService,
        beastfx.app.treeannotator.services.MeanNodeHeightService,
        beastfx.app.treeannotator.services.MedianNodeHeightService;

    provides beast.base.core.BEASTInterface with
        beastfx.app.beauti.Fragment,
        beastfx.app.draw.BEASTObjectSet,
        beastfx.app.inputeditor.BeautiAlignmentProvider,
        beastfx.app.inputeditor.BeautiConfig,
        beastfx.app.inputeditor.BeautiConnector,
        beastfx.app.inputeditor.BeautiDoc,
        beastfx.app.inputeditor.BeautiPanelConfig,
        beastfx.app.inputeditor.BeautiSubTemplate,
        beastfx.app.seqgen.MergeDataWith,
        beastfx.app.seqgen.SequenceSimulator,
        beastfx.app.seqgen.SimulatedAlignment,
        beastfx.app.tools.ClassEnumerator,
        beastfx.app.tools.JarHealthChecker,
        beastfx.app.tools.PackageHealthChecker;
}
