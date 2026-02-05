# Guide for migration BEAST v2.7 packages to this version

## 0. Setting up a beast3 + BeastFX3 project

* git clone git@github.com:CompEvol/beast3.git
  git clone git@github.com:CompEvol/BeastFX3.git
* update JDK to v25. Make sure to include Java FX (the JDK+FX version from https://www.azul.com/downloads/?package=jdk#zulu)
## 1. Make sure your package compiles

* include jar files
  * beagle.jar
  * colt.jar
  * antlr-runtime-4.13.2.ja <= new version
  * commons.jar <= contains commons-math3-3.6.1 + other commons jars
  
Some changes to keep in mind:
* StateNode is not a Function any more -- plan is to use Typed version to produce values, but making your class implement Function works as intermediate step
* StateNode does not have a `scale` method any more -- implement the `Scalable` interface if you want your StateNode to scale
* `beast.base.inference.Evaluator` removed -- impacts MCMC derived classes
* `beast.base.evolution.alignment.AscertainedAlignment` removed -- use standard Alignment instead

## 2. Migrate package to use strongly typed classes

Your IDE should show where you are using deprecated classes (e.g. shown as line through in Eclipse). Identify where this happens by navigating your code.

Wherever classes are deprecated, there will be an alternative in the `beast.base.spec` hierarchy.
Links can be found in deprecated classes, for example for RealParameter there is a comment stating

``` * @deprecated use {@link RealScalarParam} or {@link RealVectorParam} ```

For your package
* consider using a `<yourpackage>.spec` package if you want to be backward compatible
* option mostly for packages that have other packages depending on them
* for `stand alone` packages, just replace the Parameter and Function Inputs with typed versions


## 3a. Update example XMLs

* Replace all `XXXParameters` with appropriate types `XXXParams`
* Replace all `spec` attributes with the appropriate classes (mostly insert '.spec')
* Replace operator sets for Trees with new operator sets

Old set:
```
<operator id="$(m)BICEPSEpochTop.t:$(n)" spec="beast.base.evolution.operator.EpochFlexOperator" tree="@Tree.t:$(n)" weight="2.0" scaleFactor="0.1"/>
<operator id="$(m)BICEPSEpochAll.t:$(n)" spec="beast.base.evolution.operator.EpochFlexOperator" tree="@Tree.t:$(n)" weight="2.0" scaleFactor="0.1" fromOldestTipOnly="false"/>      
<operator id="$(m)BICEPSTreeFlex.t:$(n)" spec="beast.base.evolution.operator.TreeStretchOperator" scaleFactor="0.01" tree="@Tree.t:$(n)" weight="2.0"/>
<operator id='$(m)TreeRootScaler.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianScaleOperator' scaleFactor="0.1" weight="3" tree="@Tree.t:$(n)" rootOnly='true'/>
<operator id='$(m)UniformOperator.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianNodeOperator' weight="30" tree="@Tree.t:$(n)"/>
<operator id='$(m)SubtreeSlide.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianSubtreeSlide' weight="15" size="1.0" tree="@Tree.t:$(n)"/>
<operator id='$(m)Narrow.t:$(n)' spec='Exchange' isNarrow='true' weight="15" tree="@Tree.t:$(n)"/>
<operator id='$(m)Wide.t:$(n)' spec='Exchange' isNarrow='false' weight="3" tree="@Tree.t:$(n)"/>
<operator id='$(m)WilsonBalding.t:$(n)' spec='WilsonBalding' weight="3" tree="@Tree.t:$(n)"/>
```

New set
```
<operator id="$(m)TreeScaler.t:$(n)" spec="beast.base.spec.evolution.operator.UpDownOperator" up="@Tree.t:$(n)" weight="4.0" scaleFactor="0.1"/>
<operator id='$(m)TreeRootScaler.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianScaleOperator' scaleFactor="0.1" weight="3" tree="@Tree.t:$(n)" rootOnly='true'/>
<operator id='$(m)UniformOperator.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianNodeOperator' weight="30" tree="@Tree.t:$(n)"/>
<operator id='$(m)SubtreeSlide.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianSubtreeSlide' weight="15" size="1.0" tree="@Tree.t:$(n)"/>
<operator id='$(m)Narrow.t:$(n)' spec='Exchange' isNarrow='true' weight="15" tree="@Tree.t:$(n)"/>
<operator id='$(m)Wide.t:$(n)' spec='Exchange' isNarrow='false' weight="3" tree="@Tree.t:$(n)"/>
<operator id='$(m)WilsonBalding.t:$(n)' spec='WilsonBalding' weight="3" tree="@Tree.t:$(n)"/>
```

## 3b. Update BEAUti templates & input editors

* Replace all `XXXParameters` with appropriate types `XXXParams`
* Replace all `spec` attributes with the appropriate classes (mostly insert '.spec')
* Replace operator sets for Trees with new operator sets (see above)
* Update `InputEditor` implementations to deal with typed inputs
* Some BEAUti InputEditors have changed signatures
