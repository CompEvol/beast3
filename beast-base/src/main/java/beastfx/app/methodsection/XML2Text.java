package beastfx.app.methodsection;







import java.io.File;
import java.io.PrintStream;
import java.util.*;

import beastfx.app.util.OutFile;
import beastfx.app.util.XMLFile;
import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeDistribution;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import beast.base.parser.ClassToPackageMap;
import beast.base.parser.XMLParser;
import beast.base.spec.evolution.likelihood.GenericTreeLikelihood;
import beast.base.spec.inference.operator.DeltaExchangeOperator;
import beast.base.spec.type.Tensor;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.methodsection.Phrase.PhraseType;
import beastfx.app.methodsection.implementation.BEASTObjectMethodsText;
import beastfx.app.tools.Application;


@Description("Convert MCMC analysis in XML file to a methods section")
public class XML2Text extends beast.base.inference.Runnable {
	public Input<XMLFile> xmlInput = new Input<>("xml",
			"file name of BEAST XML file containing the model for which to create a methods text file for",
			new XMLFile("examples/normalTest-1XXX.xml"), Validate.REQUIRED);
	public Input<CitationPhrase.mode> citationModeInput = new Input<>("citationMode", "method for displaying citations, default none, "
			+ "should be one of " + Arrays.toString(CitationPhrase.mode.values()), CitationPhrase.mode.text, CitationPhrase.mode.values());
	public Input<OutFile> outputInput = new Input<>("output", "where to save the file", new OutFile("methods.txt"));

	private BeautiDoc beautiDoc;
	private String text;	
	private List<Phrase> m;
	
	
	public XML2Text() {}
	
	public XML2Text(BeautiDoc beautiDoc) {
		this.beautiDoc = beautiDoc;
	}
	
	@Override
	public void initAndValidate() {
	}
	
	@Override
	public void run() throws Exception {
		CitationPhrase.CitationMode = citationModeInput.get();

		beautiDoc = new BeautiDoc();
		File file = xmlInput.get();
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
		initialise((MCMC) beautiDoc.mcmc.get());
		
		
		if (outputInput.get() != null) {
			PrintStream out = new PrintStream(outputInput.get());
			out.print(text);
			out.close();
			System.exit(0);
		}
	}
	
		
	public String initialise(MCMC mcmc) throws Exception {
        CompoundDistribution posterior = (CompoundDistribution) mcmc.posteriorInput.get();
        
		m = new ArrayList<>();
		addAnalysisIdentifier();
		
		addPartitionSection();
				
		addSiteModelDescription(posterior);
        
		addClockModelDescription(posterior);

		addTreePrior(posterior);

        addFixMeanMutationRatesOperator(mcmc);

        addOtherInformation(posterior);
        
        addReferenceSection();
        
        cleanText(Phrase.toString(m));
        
		Log.warning(text);
		Log.warning("Done!");
		return text;
	}
	
	private void addReferenceSection() {
		m.add(new SectionPhrase("References"));
		int i = m.size();
		m.add(new Phrase("\nThis analysis is for BEAST "));
		m.add(CitationPhrase.createCitationPhrase("10.1371/journal.pcbi.1003537"));

		// XMLProducer producer = new XMLProducer();
		beautiDoc.scrubAll(false, false);
		String [] packages = ClassToPackageMap.getPackagesAndVersions(beautiDoc.mcmc.get()).toArray(new String[]{});
		if (packages.length > 0) {
			m.add(new Phrase(" using "));
			for (String pkg : packages) {
				m.add(new Phrase(pkg));
				m.add(new Phrase(", "));
			}
			m.remove(m.size() - 1);
			if (packages.length > 1) {
				m.get(m.size() - 2).text = ", and ";				
			}
		}
		
		
		m.add(new Phrase(".\n\n"));

		if (CitationPhrase.citations.size() > 0) {
			completePhrase("\nReferences:\n");
			for (CitationPhrase citation : CitationPhrase.citations.values()) {
				try {
					String reference = citation.toReference();
					completePhrase(reference + "\n\n");
				} catch (Exception e) {
					completePhrase("Unknown reference " + e.getMessage() + " \n\n");
				}
			}
		}
		while (i < m.size()) {
			m.get(i++).setType(PhraseType.reference);
		}
	}


	private void cleanText(String b) {
		text = b;
		// clean up
		text = text.replaceAll("  ", " ");
		text = text.replaceAll("\n\n", "\n");
		text = text.replaceAll("\\s([\\.,\\)\\}\\]])", "$1");
		text = text.replaceAll("([\\[\\{\\(])\\s", "$1");
		for (char c : new char[]{'a','e','i','o','u'}) {
			text = text.replaceAll(" a " + c, " an " + c);					
		}				
	}


	/** any priors other than parameter and tree priors? **/
	private void addOtherInformation(CompoundDistribution posterior) {
		List<Phrase> m = new ArrayList<>();

        List<List<Phrase>> others = new ArrayList<>();
        for (Distribution distr : posterior.pDistributions.get()) {
            if (!distr.getID().equals("likelihood")) {
                for (Distribution prior : ((CompoundDistribution) distr).pDistributions.get()) {
                	if (!(prior instanceof beast.base.inference.distribution.Prior || prior instanceof TreeDistribution)) {
                    	m = MethodsTextFactory.getModelDescription(prior, null, null, beautiDoc);
                    	if (m.size() > 0) {
//                    		others.add(Phrase.toSimpleString(m));
                    		others.add(m);
                    	}
                	}
                }
            }
        }
    	if (others.size() > 0) {
    		completePhrase("\nOther information:\n");
    		int n = getLongestPostfix(others);

			for (List<Phrase> other : others) {
				for (int i = 0; i < n; i++) {
					this.m.add(other.get(i));
				}
//				completePhrase(other.substring(0, other.length() - longestPostfix.length()).trim());
        		addDot();
			}
        	if (n != others.get(0).size()) {
        		String haveAreStr = others.get(0).get(n).text.trim().startsWith("is") ? "are": "have";
        		this.m.add(new Phrase("All " + haveAreStr + " "));
        		for (int i = n; i < others.get(0).size(); i++) {
        			this.m.add(others.get(0).get(i));
        		}
        		addDot();
        		//completePhrase("All have " + longestPostfix.substring(1));
        	}
    	}
	}


	private void addTreePrior(CompoundDistribution posterior) {
		m.add(new SectionPhrase("Tree"));
		List<Phrase> m = new ArrayList<>();

        // tree priors        
        Set<TreeInterface> trees = new LinkedHashSet<>();
        // List<GenericTreeLikelihood> likelihoods = new ArrayList<>();
        for (Distribution distr : posterior.pDistributions.get()) {
            if (distr.getID().equals("likelihood")) {
                for (Distribution likelihood : ((CompoundDistribution) distr).pDistributions.get()) {
                    if (likelihood instanceof GenericTreeLikelihood) {
                        GenericTreeLikelihood treeLikelihood = (GenericTreeLikelihood) likelihood;
                    	trees.add(treeLikelihood.treeInput.get());
                    	// likelihoods.add(treeLikelihood);
                    }
                }
            }
        }
        
        if (trees.size() == 1) {
        	TreeInterface tree = (TreeInterface) trees.toArray()[0];
        	m = MethodsTextFactory.getModelDescription(tree, null, null, beautiDoc);
        	m.add(0, new Phrase("\nThere is a single tree with ", PhraseType.tipdates));
        	TraitSet traitSet = ((Tree) tree).getDateTrait();
        	if (traitSet != null) {
        		String direction = traitSet.getDateType().equals(TraitSet.DATE_BACKWARD_TRAIT) ? 
        				"ages not dates" : "dates not ages";
         		m.add(1, new Phrase(" dated tips (in " + direction + ") ", PhraseType.tipdates));
        	}
            addPhrases(m);
        } else if (beautiDoc.pluginmap.containsKey("Tree.t:Species")) {
        	BEASTInterface speciesTree = (BEASTInterface) beautiDoc.pluginmap.get("Tree.t:Species");
        	m = MethodsTextFactory.getModelDescription(speciesTree, null, null, beautiDoc);
        	if (speciesTree instanceof Tree) {
	        	TraitSet traitSet = ((Tree) speciesTree).getDateTrait();
	        	if (traitSet != null) {
	        		String direction = traitSet.getDateType().equals(TraitSet.DATE_BACKWARD_TRAIT) ? 
	        				"ages not dates" : "dates not ages";
	        		m.add(1, new Phrase("Tree " + speciesTree.getID() +" has dated tips (in " + direction + ").", PhraseType.tipdates));
	        	}
        	}
        	m.add(0, new Phrase("\nTree prior: ", PhraseType.tipdates));
            addPhrases(m);        	
        } else {
	        for (TreeInterface tree : trees) {
	        	m = MethodsTextFactory.getModelDescription(tree, null, null, beautiDoc);
	        	TraitSet traitSet = ((Tree) tree).getDateTrait();
	        	if (traitSet != null) {
	        		String direction = traitSet.getDateType().equals(TraitSet.DATE_BACKWARD_TRAIT) ? 
	        				"ages not dates" : "dates not ages";
	        		m.add(1, new Phrase("Tree " + tree.getID() +" has dated tips (in " + direction + ").", PhraseType.tipdates));
	        	}
	        	m.add(0, new Phrase("\nTree prior: ", PhraseType.tipdates));
	            addPhrases(m);
	        }
        }
        
        addDot();
 		
	}


	private void addSiteModelDescription(CompoundDistribution posterior) {
		m.add(new SectionPhrase("Site model"));
        // collect model descriptions of all partitions
        List<String> partitionIDs = new ArrayList<>();
        List<String> smPartitionIDs = new ArrayList<>();
        List<List<Phrase>> siteModels = new ArrayList<>();
        
        for (Distribution distr : posterior.pDistributions.get()) {
            if (distr.getID().equals("likelihood")) {
                for (Distribution likelihood : ((CompoundDistribution) distr).pDistributions.get()) {
                    if (likelihood instanceof GenericTreeLikelihood) {
                        GenericTreeLikelihood treeLikelihood = (GenericTreeLikelihood) likelihood;
                    	partitionIDs.add(treeLikelihood.dataInput.get().getID());
                    	
                    	BEASTInterface siteModel = (BEASTInterface) treeLikelihood.siteModelInput.get();
                		List<Phrase> sm = MethodsTextFactory.getModelDescription(siteModel, treeLikelihood, treeLikelihood.siteModelInput, beautiDoc);
                		// sm.get(0).setInput(treeLikelihood, treeLikelihood.siteModelInput);
                		// sm.add(new Phrase("\n"));
                		siteModels.add(sm);
                		smPartitionIDs.add(BeautiDoc.parsePartition(siteModel.getID()));
                    }
                }
            }
        }
        
        // amalgamate partitions
        amalgamate(siteModels, partitionIDs, smPartitionIDs);
        addDot();
	}


	private void addClockModelDescription(CompoundDistribution posterior) {
		m.add(new SectionPhrase("Clock model"));
        // collect model descriptions of all partitions
        List<String> partitionIDs = new ArrayList<>();
        List<String> cmPartitionIDs = new ArrayList<>();
        List<List<Phrase>> clockModels = new ArrayList<>();
        
        for (Distribution distr : posterior.pDistributions.get()) {
            if (distr.getID().equals("likelihood")) {
                for (Distribution likelihood : ((CompoundDistribution) distr).pDistributions.get()) {
                    if (likelihood instanceof GenericTreeLikelihood) {
                        GenericTreeLikelihood treeLikelihood = (GenericTreeLikelihood) likelihood;
                    	partitionIDs.add(treeLikelihood.dataInput.get().getID());
                		
                		BEASTInterface clockModel = treeLikelihood.branchRateModelInput.get();
                		List<Phrase> cm = MethodsTextFactory.getModelDescription(clockModel, treeLikelihood, treeLikelihood.branchRateModelInput, beautiDoc);
                		// cm.get(0).setInput(treeLikelihood, treeLikelihood.branchRateModelInput);
                		// cm.add(new Phrase("\n"));
                		clockModels.add(cm);
                		cmPartitionIDs.add(BeautiDoc.parsePartition(clockModel.getID()));
                    }
                }
            }
        }
        
        // amalgamate partitions
        amalgamate(clockModels, partitionIDs, cmPartitionIDs);
        addDot();
	}

	private void addAnalysisIdentifier() {
		List<Phrase> m = new ArrayList<>();
		
		BeautiSubTemplateMethodsText.initialise();
		for (String analysisIdentifier : BeautiSubTemplateMethodsText.analysisIdentifiers) {
			if (beautiDoc.pluginmap.containsKey(analysisIdentifier)) {
	        	BEASTInterface speciesTree = (BEASTInterface) beautiDoc.pluginmap.get(analysisIdentifier);
	        	m = MethodsTextFactory.getModelDescription(speciesTree, null, null, beautiDoc);
	            addPhrases(m);
				addDot();
				m.clear();
			}
		}
	}



	private int getLongestPostfix(List<List<Phrase>> others) {
		if (others.size() <= 1) {
			return others.get(0).size();
		}
		int postfix = others.get(0).size();
		int k = others.get(0).size() - 1;
		while (k > 0) {			
			String str = others.get(0).get(k).text;
			for (int i = 1; i < others.size(); i++) {
				List<Phrase> other = others.get(i);
				int k2 = other.size() - (others.get(0).size() - k);
				if (k2 <= 0 || !other.get(k2).text.equals(str)) {
					return postfix;
				}
			}
			postfix--;
			k--;
		}
		return postfix;
	}

	private void addPartitionSection() {
		m.add(new SectionPhrase("Data\n"));
		List<Phrase> m = new ArrayList<>();

		List<BEASTInterface> parts = beautiDoc.getPartitions("Partitions");
		StringBuilder b = new StringBuilder();
		if (parts.size() == 1) {
			b.append("There is one alignment with ");
			Alignment data = (Alignment) parts.get(0);
			b.append(data.getTaxonCount());
			b.append(" taxa, and ");
			b.append(data.getSiteCount());
			b.append(" characters.");
		} else {
			List<String> strs = new ArrayList<>();
			for (BEASTInterface o : parts) {
				Alignment data = (Alignment) o;
				strs.add(data.getID());
			}	
			b.append("There are " + parts.size() + " partitions (" + printParitions(strs) + ") with ");
			strs.clear();
			for (BEASTInterface o : parts) {
				Alignment data = (Alignment) o;
				strs.add(data.getSiteCount() + "");
			}	
			b.append(printParitions(strs, -1));
			b.append(" sites respectively.");
		}
		b.append("\n");
		m.add(new PartitionPhrase(b.toString()));
        addPhrases(m);
		
	}


	private void amalgamate(List<List<Phrase>> models, List<String> partitionIDs, 
			List<String> xPartitionIDs) {
		List<Phrase> m = new ArrayList<>();

		for (int i = 0; i < partitionIDs.size(); i++) {
        	if (models.get(i) != null) {
                List<String> currentPartitionIDs = new ArrayList<>();
                currentPartitionIDs.add(partitionIDs.get(i));
                String model = Phrase.toSimpleString(models.get(i));

                List<List<Phrase>> selected = new ArrayList<>();
                selected.add(models.get(i));
                // String modelID = xPartitionIDs.get(i);
                for (int j = i + 1; j < partitionIDs.size(); j++) {
                	String modelj = Phrase.toSimpleString(models.get(j));
                	if (modelj.equals(model)) {
                	//if (xPartitionIDs.get(j).equals(modelID)) {
                        selected.add(models.get(j));
                		models.set(j, null);
                		currentPartitionIDs.add(partitionIDs.get(j));
                	}
                }
                
                // update MethodsText.partitionGroupMap
                if (selected.size() > 0) {
	                for (int k = 0; k < selected.get(0).size(); k++) {
	                	Object source = models.get(i).get(k).source;
	                	if (source != null) {
		                	Set<Phrase> set = MethodsText.partitionGroupMap.get(source);
		                	if (set != null) {
			                	for (int j = 0; j < selected.size(); j++) {
			                		Phrase phrase = selected.get(j).get(k);
			                		set.add(phrase);
			                		MethodsText.partitionGroupMap.put(phrase.source, set);	                		
			                	}
		                	}
		                	
	                	}
	                }
                }
                
                // translate to text                
                boolean shared = isShared(selected);
                // model = Phrase.toString(selected.toArray(new List[]{}));
            	m.clear();
                if (currentPartitionIDs.size() == partitionIDs.size()) {
                	StringBuilder b2 = new StringBuilder();
                	if (currentPartitionIDs.size() == 1) {
                		b2.append("\nThe partition ");
                	} else if (currentPartitionIDs.size() == 2) {
                		b2.append("\nBoth partitions ");
                	} else {
                		b2.append("\nAll partitions ");
                	}
            		if (selected.size() > 1) {
            			if (shared) {
                			b2.append(" share a ");
                		} else {
                			b2.append(" individually have a ");
                		}
                	} else {
                		b2.append(" has a ");
                	}
                	m.add(new PartitionPhrase(b2.toString()));
                	b2.append(model);
                	//b.append(b2.toString());
                } else if (currentPartitionIDs.size() > 1) {
                	StringBuilder b2 = new StringBuilder();
                	b2.append("\nPartitions ");
                	b2.append(printParitions(currentPartitionIDs));
            		if (selected.size() > 1) {
            			if (shared) {
                			b2.append(" share a ");
                		} else {
                			b2.append(" individually have a ");
                		}
                	} else {
                		b2.append(" has a ");
                	}
                	m.add(new PartitionPhrase(b2.toString()));
                	b2.append(model);
                	//b.append(b2.toString());

                } else {
                	m.add(new PartitionPhrase("\nPartition " + currentPartitionIDs.get(0) + " has a "));
                	//b.append("\nPartitions " + currentPartitionIDs.get(0) + " has a " + model);                	
                }
                
                if (model.trim().length() > 0) {
                	addPhrases(m);
                }

                addPhraseSet(selected);
        	}
        }
     }


	// has FixMeanMutationRatesOperator? If so, say so.
	private void addFixMeanMutationRatesOperator(MCMC mcmc) {
		List<Phrase> m = new ArrayList<>();
		StringBuilder b = new StringBuilder();
        for (Operator op : mcmc.operatorsInput.get()) {
        	if (op.getID().equals("FixMeanMutationRatesOperator")) {
                List<String> partitionIDs = new ArrayList<>();
                for (Tensor<?,?> s : ((DeltaExchangeOperator)op).parameterInput.get()) {
                	partitionIDs.add(BeautiDoc.parsePartition(((BEASTInterface)s).getID()));
                }
        		b.append("Relative substitution rates among ");
        		if (partitionIDs.size() != beautiDoc.alignments.size()) {
        			b.append("partitions ");
        		}
                b.append(printParitions(partitionIDs, beautiDoc.alignments.size()));
        		b.append("are estimated");
        		m.clear();
        		m.add(new Phrase(b.toString()));
                addPhrases(m);
                addDot();
        	}
        }
	}


	private void addPhrases(List<Phrase> m) {
		this.m.addAll(m);		
	}

	private void addPhraseSet(List<List<Phrase>> selected) {
		addPhrases(selected.get(0));
	}

	private void addDot() {
		m.add(new Phrase(".\n\n"));
	}

	private void completePhrase(String str) {
		m.add(new Phrase(str));
	}


	private boolean isShared(List<List<Phrase>> selected) {
		if (selected.size() == 1) {
			return true;
		}
			
		Set<BEASTInterface> stateNodes = new LinkedHashSet<>();
		for (Phrase phrase : selected.get(0)) {
			if (phrase.source instanceof BEASTInterface) {
				stateNodes.add((BEASTInterface)phrase.source);
			}
		}
		
		for (int i = 1; i < selected.size(); i++) {
			Set<BEASTInterface> otherNodes = new LinkedHashSet<>();
			for (Phrase phrase : selected.get(i)) {
				if (phrase.source instanceof BEASTInterface) {
					otherNodes.add((BEASTInterface)phrase.source);
					
				}
			}
			if (!stateNodes.containsAll(otherNodes) || !otherNodes.containsAll(stateNodes)) {
				return false;
			}
		}
		
		return true;
	}


	private String printParitions(List<String> partitionIDs, int totalPartitionCount) {
		StringBuilder b = new StringBuilder();
		if (partitionIDs.size() == totalPartitionCount) {
			if (partitionIDs.size() == 2) {
				b.append("both partitions ");
			} else {
				b.append("all partitions ");
			}
		} else {
	    	for (int j = 0; j < partitionIDs.size() - 1; j++) {
	    		b.append(partitionIDs.get(j));
	    		if (j < partitionIDs.size() - 2) {
	    			b.append(", ");
	    		} else {
	    			b.append(" and ");
	    		}
	    	}
	    	if (partitionIDs.size() > 0) {
	    		b.append(partitionIDs.get(partitionIDs.size() - 1) + " ");
	    	}
		}
		return b.toString();
	}


	private String printParitions(List<String> partitionIDs) {
		return printParitions(partitionIDs, -1);	
	}

	public List<Phrase> getPhrases() {
		List<Phrase> copy = new ArrayList<>();
		copy.addAll(m);
		return copy;
	}

	
	public static void main(String[] args) throws Exception {
		if (System.getProperty("beasy.style") != null) {
			String style = System.getProperty("beasy.style");
			CitationPhrase.CitationMode = CitationPhrase.mode.valueOf(style);
		}
		new Application(new XML2Text(), "XML 2 methods section", args);
	}

}
