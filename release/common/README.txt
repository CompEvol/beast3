                    BEAST v2.8.0 2026
                 BEAST development team 2011-2026

Last updated: March 2026

Contents:
1) INTRODUCTION
2) INSTALLING BEAST
3) CONVERTING SEQUENCES
4) RUNNING BEAST
5) ANALYZING RESULTS
6) NATIVE LIBRARIES
7) PACKAGES
8) SUPPORT & LINKS
9) ACKNOWLEDGMENTS

___________________________________________________________________________
1) INTRODUCTION

BEAST (Bayesian evolutionary analysis sampling trees) is a package for
evolutionary inference from molecular sequences.

BEAST uses a complex and powerful input format (specified in XML) to
describe the evolutionary model. This has advantages in terms of
flexibility in that the developers of BEAST do not have to try and predict
every analysis that researchers may wish to perform and explicitly provide
an option for doing it. However, this flexibility means it is possible to
construct models that don't perform well under the Markov chain Monte Carlo
(MCMC) inference framework used. We cannot test every possible model that
can be used in BEAST. There are two solutions to this: Firstly, we supply
a range of recipes for commonly performed analyses that we know should work
in BEAST and provide example input files for these (although, the actual
data can also produce unexpected behaviour). Secondly, we provide advice and
tools for the diagnosis of problems and suggestions on how to fix them:

<http://beast2.org/>

BEAST is not a black-box into which you can put your data and expect an
easily interpretable answer. It requires careful inspection of the output
to check that it has performed correctly and usually will need tweaking,
adjustment and a number of runs to get a valid answer. Sorry.
___________________________________________________________________________
2) INSTALLING BEAST

The BEAST installation package comes with a bundled Java runtime (JRE), so
no separate Java installation is required.

The package contains the following:

Application             Description
BEAST.app               Main BEAST application (with bundled JRE)
BEAUti.app              Model setup GUI
TreeAnnotator.app       Summarise tree posteriors
LogCombiner.app         Combine log/tree files from multiple runs
AppLauncher.app         Launch BEAST tools and package apps
bin/                    Command-line launcher scripts
examples/               Sample NEXUS and XML files

On macOS, drag the folder to your Applications directory. The wrapper
applications (BEAUti, TreeAnnotator, etc.) share the JRE and JARs
bundled inside BEAST.app, so they must remain in the same folder.
___________________________________________________________________________
3) CONVERTING SEQUENCES

A program called "BEAUti" will import data in NEXUS format, allow you to
select various models and options and generate an XML file ready for use in
BEAST.

To run BEAUti simply double-click BEAUti.app (macOS) or run from the
command line:

    bin/beauti

___________________________________________________________________________
4) RUNNING BEAST

To run BEAST simply double-click BEAST.app. You will be asked to select a
BEAST XML input file.

Alternatively, open a terminal and type:

    bin/beast input.xml

Where "input.xml" is the name of a BEAST XML format file. This file can
either be created from scratch using a text editor or be created by the
BEAUti program from a NEXUS format file.

For documentation on creating and tuning the input files look at the
documentation and tutorials on-line at:

Help -      <http://beast2.org/>
FAQ -       <http://beast2.org/faq/>
Tutorials - <http://beast2.org/tutorials/>

BEAST arguments:
    -window Provide a console window
    -options Display an options dialog
    -working Change working directory to input file's directory
    -seed Specify a random number generator seed
    -prefix Specify a prefix for all output log filenames
    -statefile Specify the filename for storing/restoring the state
    -overwrite Allow overwriting of log files
    -resume Allow appending of log files
    -validate Parse the XML, but do not run -- useful for debugging XML
    -errors Specify maximum number of numerical errors before stopping
    -threads The number of computational threads to use (default 1), -1 for number of cores
    -java Use Java only, no native implementations
    -noerr Suppress all output to standard error
    -loglevel error,warning,info,debug,trace
    -instances divide site patterns amongst number of threads (use with -threads option)
    -beagle Use beagle library if available
    -beagle_info BEAGLE: show information on available resources
    -beagle_order BEAGLE: set order of resource use
    -beagle_CPU BEAGLE: use CPU instance
    -beagle_GPU BEAGLE: use GPU instance if available
    -beagle_SSE BEAGLE: use SSE extensions if available
    -beagle_single BEAGLE: use single precision if available
    -beagle_double BEAGLE: use double precision if available
    -beagle_scaling BEAGLE: specify scaling scheme to use
    -help Print this information and stop
    -version Print version and stop
    -strictversions Use only package versions as specified in the 'required' attribute
    -D attribute-value pairs to be replaced in the XML, e.g., -D "arg1=10,arg2=20"
    -DF as -D, but attribute-value pairs defined in file in JSON format
    -DFout BEAST XML file written when -DF option is used
    -sampleFromPrior samples from prior for MCMC analysis
    -version_file Provide a version file containing a list of services to explicitly allow
    -packagedir Set user package directory instead of using the default

For example:

     bin/beast -seed 123456 -overwrite input.xml

___________________________________________________________________________
5) ANALYZING RESULTS

We have produced a powerful graphical program for analysing MCMC log files
(it can also analyse output from MrBayes and other MCMCs). This is called
'Tracer' and is available from the Tracer web site:

<http://tree.bio.ed.ac.uk/software/tracer>

Additionally, two programs are distributed as part of the BEAST package:
LogCombiner & TreeAnnotator. LogCombiner can combine log or tree files from
multiple runs of BEAST into a single combined results file (after removing
appropriate burn-ins). TreeAnnotator can summarize a sample of trees from
BEAST using a single target tree, annotating it with posterior
probabilities, HPD node heights and rates. This tree can then be viewed in
FigTree:

<http://tree.bio.ed.ac.uk/software/figtree>

___________________________________________________________________________
6) NATIVE LIBRARIES

We recommend that you install the BEAGLE library. BEAST attempts to use
BEAGLE by default and this can speed up running BEAST considerably. The
BEAGLE library needs to be installed separately from BEAST, and can be
obtained from:

<https://github.com/beagle-dev/beagle-lib>

___________________________________________________________________________
7) PACKAGES

BEAST supports external packages that add models and functionality.
Packages can be managed through BEAUti (File > Manage Packages) or from
the command line:

    bin/packagemanager -list
    bin/packagemanager -add <package-name>
    bin/packagemanager -maven <groupId>:<artifactId>:<version>

Packages are installed to:
~/Library/Application Support/BEAST/2.8  on macOS
~/.beast/2.8                             on Linux
%USERPROFILE%\BEAST\2.8                  on Windows

___________________________________________________________________________
8) SUPPORT & LINKS

BEAST is an extremely complex program and as such will inevitably have
bugs. Please email us to discuss any problems:

<r.bouckaert@auckland.ac.nz>
<alexei@cs.auckland.ac.nz>
<a.rambaut@ed.ac.uk>
<msuchard@ucla.edu>

The BEAST users' mailing-list
<https://groups.google.com/forum/#!forum/beast-users>

The website for BEAST is here:

<http://beast2.org/>

Source code distributed under the GNU Lesser General Public License:

<https://github.com/CompEvol/beast3>

___________________________________________________________________________
9) ACKNOWLEDGMENTS

Thanks to all who have contributed code, reported bugs, or assisted with
the creation and testing of BEAST.
