open module beast.base {
    requires beast.pkgmgmt;
    requires java.xml;
    requires java.desktop;
    requires java.logging;

    // Apache Commons (proper modules from MR JARs)
    requires org.apache.commons.numbers.gamma;
    requires org.apache.commons.numbers.core;
    requires org.apache.commons.numbers.combinatorics;
    requires org.apache.commons.numbers.rootfinder;
    requires org.apache.commons.rng.api;
    requires org.apache.commons.rng.simple;
    requires org.apache.commons.rng.sampling;
    requires org.apache.commons.statistics.distribution;

    // Automatic modules
    requires commons.math3;
    requires org.antlr.antlr4.runtime;
    requires beagle;
    requires colt;

    // Export all packages
    exports beast.base;
    exports beast.base.core;
    exports beast.base.evolution;
    exports beast.base.evolution.alignment;
    exports beast.base.evolution.branchratemodel;
    exports beast.base.evolution.datatype;
    exports beast.base.evolution.distance;
    exports beast.base.evolution.likelihood;
    exports beast.base.evolution.operator;
    exports beast.base.evolution.operator.kernel;
    exports beast.base.evolution.sitemodel;
    exports beast.base.evolution.speciation;
    exports beast.base.evolution.substitutionmodel;
    exports beast.base.evolution.tree;
    exports beast.base.evolution.tree.coalescent;
    exports beast.base.evolution.tree.treeparser;
    exports beast.base.inference;
    exports beast.base.inference.distribution;
    exports beast.base.inference.operator;
    exports beast.base.inference.operator.kernel;
    exports beast.base.inference.parameter;
    exports beast.base.inference.util;
    exports beast.base.internal.json;
    exports beast.base.math;
    exports beast.base.math.matrixalgebra;
    exports beast.base.minimal;
    exports beast.base.parser;
    exports beast.base.spec;
    exports beast.base.spec.domain;
    exports beast.base.spec.evolution;
    exports beast.base.spec.evolution.alignment;
    exports beast.base.spec.evolution.branchratemodel;
    exports beast.base.spec.evolution.likelihood;
    exports beast.base.spec.evolution.operator;
    exports beast.base.spec.evolution.sitemodel;
    exports beast.base.spec.evolution.speciation;
    exports beast.base.spec.evolution.substitutionmodel;
    exports beast.base.spec.evolution.tree;
    exports beast.base.spec.evolution.tree.coalescent;
    exports beast.base.spec.inference.distribution;
    exports beast.base.spec.inference.operator;
    exports beast.base.spec.inference.operator.uniform;
    exports beast.base.spec.inference.parameter;
    exports beast.base.spec.inference.util;
    exports beast.base.spec.type;
    exports beast.base.util;

    // ServiceLoader service types
    uses beast.base.core.BEASTInterface;
    uses beast.base.evolution.datatype.DataType;
    uses beast.base.inference.ModelLogger;

    // Service providers from version.xml
    // NOTE: Abstract classes, classes without no-arg constructors, and
    // non-existent classes are omitted (JPMS requires concrete providers
    // with public no-arg constructors). These are still registered via
    // BEASTClassLoader.initServices() from version.xml at runtime.

    provides beast.base.evolution.datatype.DataType with
        beast.base.evolution.datatype.Aminoacid,
        beast.base.evolution.datatype.Nucleotide,
        beast.base.evolution.datatype.TwoStateCovarion,
        beast.base.evolution.datatype.Binary,
        beast.base.evolution.datatype.IntegerData,
        beast.base.evolution.datatype.StandardData,
        beast.base.evolution.datatype.UserDataType;

    provides beast.base.inference.ModelLogger with
        beast.base.inference.ModelLogger,
        beast.base.parser.XMLModelLogger;

    provides beast.base.core.BEASTInterface with
        beast.base.core.Function.Constant,
        beast.base.evolution.RateStatistic,
        beast.base.evolution.Sum,
        beast.base.evolution.TreeWithMetaDataLogger,
        beast.base.evolution.alignment.Alignment,
        beast.base.evolution.alignment.FilteredAlignment,
        beast.base.evolution.alignment.Sequence,
        beast.base.evolution.alignment.Taxon,
        beast.base.evolution.alignment.TaxonSet,
        beast.base.evolution.branchratemodel.RandomLocalClockModel,
        beast.base.evolution.branchratemodel.StrictClockModel,
        beast.base.evolution.branchratemodel.UCRelaxedClockModel,
        beast.base.evolution.datatype.Aminoacid,
        beast.base.evolution.datatype.Binary,
        beast.base.evolution.datatype.IntegerData,
        beast.base.evolution.datatype.Nucleotide,
        beast.base.evolution.datatype.StandardData,
        beast.base.evolution.datatype.TwoStateCovarion,
        beast.base.evolution.datatype.UserDataType,
        beast.base.evolution.distance.F84Distance,
        beast.base.evolution.distance.HammingDistance,
        beast.base.evolution.distance.JukesCantorDistance,
        beast.base.evolution.distance.SMMDistance,
        beast.base.evolution.likelihood.BeagleTreeLikelihood,
        beast.base.evolution.likelihood.GenericTreeLikelihood,
        beast.base.evolution.likelihood.ThreadedTreeLikelihood,
        beast.base.evolution.likelihood.TreeLikelihood,
        beast.base.evolution.operator.AdaptableOperatorSampler,
        beast.base.evolution.operator.EpochFlexOperator,
        beast.base.evolution.operator.Exchange,
        beast.base.evolution.operator.NodeReheight,
        beast.base.evolution.operator.ScaleOperator,
        beast.base.evolution.operator.SubtreeSlide,
        beast.base.evolution.operator.TipDatesRandomWalker,
        beast.base.evolution.operator.TipDatesScaler,
        beast.base.evolution.operator.TreeStretchOperator,
        beast.base.evolution.operator.Uniform,
        beast.base.evolution.operator.WilsonBalding,
        beast.base.inference.operator.kernel.KernelDistribution.Bactrian,
        beast.base.evolution.operator.kernel.AdaptableVarianceMultivariateNormalOperator,
        beast.base.evolution.operator.kernel.BactrianNodeOperator,
        beast.base.evolution.operator.kernel.BactrianOperatorSchedule,
        beast.base.evolution.operator.kernel.BactrianScaleOperator,
        beast.base.evolution.operator.kernel.BactrianSubtreeSlide,
        beast.base.evolution.operator.kernel.BactrianTipDatesRandomWalker,
        beast.base.inference.operator.kernel.Transform.FisherZTransform,
        beast.base.inference.operator.kernel.Transform.LogConstrainedSumTransform,
        beast.base.inference.operator.kernel.Transform.LogTransform,
        beast.base.inference.operator.kernel.Transform.LogitTransform,
        beast.base.inference.operator.kernel.Transform.NegateTransform,
        beast.base.inference.operator.kernel.Transform.NoTransform,
        beast.base.inference.operator.kernel.Transform.NoTransformMultivariable,
        beast.base.inference.operator.kernel.Transform.PowerTransform,
        beast.base.evolution.sitemodel.SiteModel,
        beast.base.evolution.speciation.BirthDeathGernhard08Model,
        beast.base.evolution.speciation.CalibratedBirthDeathModel,
        beast.base.evolution.speciation.CalibratedYuleInitialTree,
        beast.base.evolution.speciation.CalibratedYuleModel,
        beast.base.evolution.speciation.CalibrationPoint,
        beast.base.evolution.speciation.GeneTreeForSpeciesTreeDistribution,
        beast.base.evolution.speciation.RandomGeneTree,
        beast.base.evolution.speciation.SpeciesTreeLogger,
        beast.base.evolution.speciation.SpeciesTreePopFunction,
        beast.base.evolution.speciation.SpeciesTreePrior,
        beast.base.evolution.speciation.StarBeastStartState,
        beast.base.evolution.speciation.TreeTopFinder,
        beast.base.evolution.speciation.YuleModel,
        beast.base.evolution.substitutionmodel.BinaryCovarion,
        beast.base.evolution.substitutionmodel.Blosum62,
        beast.base.evolution.substitutionmodel.CPREV,
        beast.base.evolution.substitutionmodel.Dayhoff,
        beast.base.evolution.substitutionmodel.Frequencies,
        beast.base.evolution.substitutionmodel.GTR,
        beast.base.evolution.substitutionmodel.GeneralSubstitutionModel,
        beast.base.evolution.substitutionmodel.ComplexSubstitutionModel,
        beast.base.evolution.substitutionmodel.HKY,
        beast.base.evolution.substitutionmodel.JTT,
        beast.base.evolution.substitutionmodel.JukesCantor,
        beast.base.evolution.substitutionmodel.MTREV,
        beast.base.evolution.substitutionmodel.MutationDeathModel,
        beast.base.evolution.substitutionmodel.SYM,
        beast.base.evolution.substitutionmodel.TIM,
        beast.base.evolution.substitutionmodel.TN93,
        beast.base.evolution.substitutionmodel.TVM,
        beast.base.evolution.substitutionmodel.WAG,
        beast.base.evolution.tree.ClusterTree,
        beast.base.evolution.tree.MRCAPrior,
        beast.base.evolution.tree.Node,
        beast.base.evolution.tree.TraitSet,
        beast.base.evolution.tree.Tree,
        beast.base.evolution.tree.TreeIntervals,
        beast.base.evolution.tree.TreeParser,
        beast.base.evolution.tree.TreeStatLogger,
        beast.base.evolution.tree.coalescent.BayesianSkyline,
        beast.base.evolution.tree.coalescent.Coalescent,
        beast.base.evolution.tree.coalescent.CompoundPopulationFunction,
        beast.base.evolution.tree.coalescent.ConstantPopulation,
        beast.base.evolution.tree.coalescent.ExponentialGrowth,
        beast.base.evolution.tree.coalescent.RandomTree,
        beast.base.evolution.tree.coalescent.SampleOffValues,
        beast.base.evolution.tree.coalescent.ScaledPopulationFunction,
        beast.base.inference.CompoundDistribution,
        beast.base.inference.DirectSimulator,
        beast.base.inference.Logger,
        beast.base.inference.MCMC,
        beast.base.inference.OperatorSchedule,
        beast.base.inference.State,
        beast.base.inference.distribution.Beta,
        beast.base.inference.distribution.ChiSquare,
        beast.base.inference.distribution.Dirichlet,
        beast.base.inference.distribution.Exponential,
        beast.base.inference.distribution.Gamma,
        beast.base.inference.distribution.InverseGamma,
        beast.base.inference.distribution.LaplaceDistribution,
        beast.base.inference.distribution.LogNormalDistributionModel,
        beast.base.inference.distribution.MarkovChainDistribution,
        beast.base.inference.distribution.Normal,
        beast.base.inference.distribution.OneOnX,
        beast.base.inference.distribution.Poisson,
        beast.base.inference.distribution.Prior,
        beast.base.inference.distribution.Uniform,
        beast.base.inference.operator.BitFlipOperator,
        beast.base.inference.operator.DeltaExchangeOperator,
        beast.base.inference.operator.IntRandomWalkOperator,
        beast.base.inference.operator.JointOperator,
        beast.base.inference.operator.RealRandomWalkOperator,
        beast.base.inference.operator.SwapOperator,
        beast.base.inference.operator.UniformOperator,
        beast.base.inference.operator.UpDownOperator,
        beast.base.inference.operator.kernel.BactrianDeltaExchangeOperator,
        beast.base.inference.operator.kernel.BactrianIntervalOperator,
        beast.base.inference.operator.kernel.BactrianRandomWalkOperator,
        beast.base.inference.operator.kernel.BactrianUpDownOperator,
        beast.base.inference.parameter.BooleanParameter,
        beast.base.inference.parameter.CompoundRealParameter,
        beast.base.inference.parameter.CompoundValuable,
        beast.base.inference.parameter.IntegerParameter,
        beast.base.inference.parameter.RealParameter,
        beast.base.inference.util.ESS,
        beast.base.inference.util.RPNcalculator,
        beast.base.spec.inference.parameter.BoolScalarParam,
        beast.base.spec.inference.parameter.BoolVectorParam,
        beast.base.spec.inference.parameter.IntScalarParam,
        beast.base.spec.inference.parameter.IntVectorParam,
        beast.base.spec.inference.parameter.RealScalarParam,
        beast.base.spec.inference.parameter.RealVectorParam,
        beast.base.spec.inference.parameter.SimplexParam,
        beast.base.spec.inference.parameter.IntSimplexParam,
        beast.base.spec.inference.util.AsRealScalar,
        beast.base.spec.inference.util.AsIntScalar,
        beast.base.spec.inference.util.ESS,
        beast.base.spec.inference.util.RPNcalculator,
        beast.base.spec.inference.operator.BitFlipOperator,
        beast.base.spec.inference.operator.DeltaExchangeOperator,
        beast.base.spec.inference.operator.IntRandomWalkOperator,
        beast.base.spec.inference.operator.RealRandomWalkOperator,
        beast.base.spec.inference.operator.SampleOffValues,
        beast.base.spec.inference.operator.ScaleOperator,
        beast.base.spec.inference.operator.SwapOperator,
        beast.base.spec.inference.operator.uniform.IntervalOperator,
        beast.base.spec.inference.operator.uniform.IntUniformOperator,
        beast.base.spec.inference.distribution.Bernoulli,
        beast.base.spec.inference.distribution.Beta,
        beast.base.spec.inference.distribution.Cauchy,
        beast.base.spec.inference.distribution.ChiSquare,
        beast.base.spec.inference.distribution.Dirichlet,
        beast.base.spec.inference.distribution.Exponential,
        beast.base.spec.inference.distribution.Gamma,
        beast.base.spec.inference.distribution.GammaMean,
        beast.base.spec.inference.distribution.IntUniform,
        beast.base.spec.inference.distribution.InverseGamma,
        beast.base.spec.inference.distribution.Laplace,
        beast.base.spec.inference.distribution.LogNormal,
        beast.base.spec.inference.distribution.MarkovChainDistribution,
        beast.base.spec.inference.distribution.Normal,
        beast.base.spec.inference.distribution.Poisson,
        beast.base.spec.inference.distribution.Uniform,
        beast.base.spec.inference.distribution.IID,
        beast.base.spec.inference.distribution.OffsetReal,
        beast.base.spec.inference.distribution.OffsetInt,
        beast.base.spec.inference.distribution.TruncatedReal,
        beast.base.spec.inference.distribution.TruncatedInt,
        beast.base.spec.FunctionOfTensor,
        beast.base.spec.evolution.Sum,
        beast.base.spec.evolution.IntSum,
        beast.base.spec.evolution.TreeWithMetaDataLogger,
        beast.base.spec.evolution.tree.ClusterTree,
        beast.base.spec.evolution.tree.MRCAPrior,
        beast.base.spec.evolution.tree.coalescent.BayesianSkyline,
        beast.base.spec.evolution.tree.coalescent.CompoundPopulationFunction,
        beast.base.spec.evolution.tree.coalescent.ConstantPopulation,
        beast.base.spec.evolution.tree.coalescent.ExponentialGrowth,
        beast.base.spec.evolution.tree.coalescent.RandomTree,
        beast.base.spec.evolution.tree.coalescent.ScaledPopulationFunction,
        beast.base.spec.evolution.likelihood.BeagleTreeLikelihood,
        beast.base.spec.evolution.likelihood.GenericTreeLikelihood,
        beast.base.spec.evolution.likelihood.ThreadedTreeLikelihood,
        beast.base.spec.evolution.likelihood.TreeLikelihood,
        beast.base.spec.evolution.speciation.BirthDeathGernhard08Model,
        beast.base.spec.evolution.speciation.CalibratedBirthDeathModel,
        beast.base.spec.evolution.speciation.CalibratedYuleInitialTree,
        beast.base.spec.evolution.speciation.CalibratedYuleModel,
        beast.base.spec.evolution.speciation.CalibrationPoint,
        beast.base.spec.evolution.speciation.YuleModel,
        beast.base.spec.evolution.speciation.GeneTreeForSpeciesTreeDistribution,
        beast.base.spec.evolution.speciation.SpeciesTreePrior,
        beast.base.spec.evolution.speciation.RandomGeneTree,
        beast.base.spec.evolution.alignment.FilteredAlignment,
        beast.base.spec.evolution.branchratemodel.RandomLocalClockModel,
        beast.base.spec.evolution.branchratemodel.StrictClockModel,
        beast.base.spec.evolution.branchratemodel.UCRelaxedClockModel,
        beast.base.spec.evolution.sitemodel.SiteModel,
        beast.base.spec.evolution.substitutionmodel.BinaryCovarion,
        beast.base.spec.evolution.substitutionmodel.ComplexSubstitutionModel,
        beast.base.spec.evolution.substitutionmodel.Frequencies,
        beast.base.spec.evolution.substitutionmodel.GeneralSubstitutionModel,
        beast.base.spec.evolution.substitutionmodel.GTR,
        beast.base.spec.evolution.substitutionmodel.HKY,
        beast.base.spec.evolution.substitutionmodel.JTT,
        beast.base.spec.evolution.substitutionmodel.JukesCantor,
        beast.base.spec.evolution.substitutionmodel.MutationDeathModel,
        beast.base.spec.evolution.substitutionmodel.SYM,
        beast.base.spec.evolution.substitutionmodel.TIM,
        beast.base.spec.evolution.substitutionmodel.TN93,
        beast.base.spec.evolution.substitutionmodel.TVM,
        beast.base.spec.evolution.substitutionmodel.Blosum62,
        beast.base.spec.evolution.substitutionmodel.CPREV,
        beast.base.spec.evolution.substitutionmodel.MTREV,
        beast.base.spec.evolution.substitutionmodel.Dayhoff,
        beast.base.spec.evolution.substitutionmodel.WAG,
        beast.base.spec.evolution.operator.ScaleTreeOperator,
        beast.base.spec.evolution.operator.AdaptableOperatorSampler,
        beast.base.spec.evolution.operator.AdaptableVarianceMultivariateNormalOperator,
        beast.base.spec.evolution.operator.UpDownOperator,
        beast.base.spec.inference.operator.Transform.FisherZTransform,
        beast.base.spec.inference.operator.Transform.LogConstrainedSumTransform,
        beast.base.spec.inference.operator.Transform.LogTransform,
        beast.base.spec.inference.operator.Transform.LogitTransform,
        beast.base.spec.inference.operator.Transform.NegateTransform,
        beast.base.spec.inference.operator.Transform.NoTransform,
        beast.base.spec.inference.operator.Transform.NoTransformMultivariable,
        beast.base.spec.inference.operator.Transform.PowerTransform;
}
