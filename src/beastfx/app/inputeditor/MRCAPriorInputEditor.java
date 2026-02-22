package beastfx.app.inputeditor;




import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.operator.TipDatesRandomWalker;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.base.spec.inference.distribution.Gamma;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.evolution.tree.Tree;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.parser.PartitionContext;


public class MRCAPriorInputEditor extends InputEditor.Base implements HasExpandBox {

	public MRCAPriorInputEditor(BeautiDoc doc) {
		super(doc);
	}

	public MRCAPriorInputEditor() {
		super();
	}

	@Override
	public Class<?> type() {
		return MRCAPrior.class;
	}

	@Override
	public void init(Input<?> input, BEASTInterface beastObject, final int listItemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr= listItemNr;
		
        pane = FXUtils.newVBox();
        pane.setPadding(new Insets(0, 5, 5, 0));
        HBox itemBox = FXUtils.newHBox();
        itemBox.setPadding(new Insets(0, 5, 5, 5));

        MRCAPrior prior = (MRCAPrior) beastObject;
        String text = prior.getID();

        Button taxonButton = new Button(text);
        taxonButton.setMinSize(Base.PREFERRED_SIZE.getWidth(), Base.PREFERRED_SIZE.getHeight());
        taxonButton.setPrefSize(Base.PREFERRED_SIZE.getWidth(), Base.PREFERRED_SIZE.getHeight());
        itemBox.getChildren().add(taxonButton);
        taxonButton.setOnAction(e -> {
                List<?> list = (List<?>) m_input.get();
                MRCAPrior prior2 = (MRCAPrior) list.get(itemNr);
                try {
                    TaxonSet taxonset = prior2.taxonsetInput.get();
                    List<Taxon> originalTaxa = new ArrayList<>();
                    originalTaxa.addAll(taxonset.taxonsetInput.get());
                    Set<Taxon> candidates = getTaxonCandidates(prior2);
                    TaxonSetDialog dlg = new TaxonSetDialog(taxonset, candidates, doc);
                    if (dlg.showDialog()) {
        	            if (dlg.taxonSet.taxonsetInput.get().size() == 0) {
        	            	Alert.showMessageDialog(doc.getFrame().getScene().getRoot(), "At least one taxon should be included in the taxon set",
        	            			"Error specifying taxon set", Alert.ERROR_MESSAGE);
        	            	taxonset.taxonsetInput.get().addAll(originalTaxa);
        	            	return;
        	            }

                        prior2.taxonsetInput.setValue(dlg.taxonSet, prior2);
                        int i = 1;
                        String id = dlg.taxonSet.getID();
                        while (doc.pluginmap.containsKey(dlg.taxonSet.getID()) && doc.pluginmap.get(dlg.taxonSet.getID()) != dlg.taxonSet) {
                        	dlg.taxonSet.setID(id + i);
                        	i++;
                        }
                        BEASTObjectPanel.addPluginToMap(dlg.taxonSet, doc);
                        prior2.setID(dlg.taxonSet.getID() + ".prior");

                    }
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                refreshPanel();
            });


        if (prior.distInput.getType() == null) {
            try {
                // this sets up the type
                prior.distInput.setValue(new Gamma(null,
                		new RealScalarParam<>(0.01, PositiveReal.INSTANCE), // shape 
                		new RealScalarParam<>(0.01, PositiveReal.INSTANCE)) // rate
                		, prior);
                // this removes the distribution
                prior.distInput.setValue(null, prior);
            } catch (Exception e) {
                // TODO: handle exception
            }

        }

        List<BeautiSubTemplate> availableBEASTObjects = doc.getInputEditorFactory().getAvailableTemplates(prior.distInput, prior, null, doc);
        ComboBox<BeautiSubTemplate> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(availableBEASTObjects.toArray(new BeautiSubTemplate[]{}));
        comboBox.setId(text+".distr");

        if (prior.distInput.get() != null) {
            String id = prior.distInput.get().getID();
            //id = BeautiDoc.parsePartition(id);
            id = id.substring(0, id.indexOf('.'));
            for (BeautiSubTemplate template : availableBEASTObjects) {
                if (template.classInput.get() != null && template.shortClassName.equals(id)) {
                    comboBox.setValue(template);
                }
            }
        } else {
            comboBox.setValue(BeautiConfig.NULL_TEMPLATE);
        }
        comboBox.setOnAction(e -> {
                @SuppressWarnings("unchecked")
				ComboBox<BeautiSubTemplate> comboBox0 = (ComboBox<BeautiSubTemplate>) e.getSource();
                BeautiSubTemplate template = (BeautiSubTemplate) comboBox0.getValue();
                List<?> list = (List<?>) m_input.get();
                MRCAPrior prior2 = (MRCAPrior) list.get(itemNr);

                try {
                    template.createSubNet(new PartitionContext(""), prior2, prior2.distInput, true);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                hardSync();
                refreshPanel();
                scrubPrior();
        });
        itemBox.getChildren().add(comboBox);
        
        itemBox.getChildren().add(FXUtils.createHMCButton(FXUtils.getHMCBase() + "Priors/MRCAPrior/"));

        CheckBox isMonophyleticdBox = new CheckBox(doc.beautiConfig.getInputLabel(prior, prior.isMonophyleticInput.getName()));
        isMonophyleticdBox.setId(text+".isMonophyletic");
        isMonophyleticdBox.setSelected(prior.isMonophyleticInput.get());
        isMonophyleticdBox.setTooltip(new Tooltip(prior.isMonophyleticInput.getTipText()));
        isMonophyleticdBox.setOnAction(e->{
            try {
                prior.isMonophyleticInput.setValue(((CheckBox) e.getSource()).isSelected(), prior);
                refreshPanel();
            } catch (Exception ex) {
            	Log.warning.println("PriorListInputEditor " + ex.getMessage());
            }
        });
        // }new MRCAPriorActionListener(prior));
        itemBox.getChildren().add(isMonophyleticdBox);

        Button deleteButton = new SmallButton("-", true);
        deleteButton.setTooltip(new Tooltip("Delete this calibration"));
        deleteButton.setOnAction(e-> {
				Log.warning.println("Trying to delete a calibration");
				List<?> list = (List<?>) m_input.get();
				MRCAPrior prior0 = (MRCAPrior) list.get(itemNr);
				doc.disconnect(prior0, "prior", "distribution");
				doc.disconnect(prior0, "tracelog", "log");
				if (prior0.onlyUseTipsInput.get()) {
					disableTipSampling(m_beastObject, doc);
				}
				doc.unregisterPlugin(prior0);
				list.remove(listItemNr);
				hardSync();
				refreshPanel();
        });
        // itemBox.getChildren().add(new Separator());
        itemBox.getChildren().add(deleteButton);

        pane.getChildren().add(itemBox);
        getChildren().add(pane);
	}
	
	private void scrubPrior() {
		// may be necessary to remove duplicates of MRCAPriors from the prior
		// due to something going wrong when syncing
		// TODO: create more elegant solution than cleaning up afterwards like so:
		Object o = doc.pluginmap.get("prior");
		if (o != null && o instanceof CompoundDistribution) {
			CompoundDistribution distr = (CompoundDistribution) o;
			Set<Distribution> ds = new HashSet<>();
			for (int i = distr.pDistributions.get().size()-1; i>=0; i--) {
				Distribution d = distr.pDistributions.get().get(i);
				if (ds.contains(d)) {
					distr.pDistributions.get().remove(i);
				}
				ds.add(d);
			}
		}
	}

	public static void customConnector(BeautiDoc doc) {
		Object o0 = doc.pluginmap.get("prior");
		if (o0 != null && o0 instanceof CompoundDistribution) {
			CompoundDistribution p =  (CompoundDistribution) o0;
			for (Distribution p0 : p.pDistributions.get()) {
				if (p0 instanceof MRCAPrior) {
					MRCAPrior prior = (MRCAPrior) p0;
			        if (prior.treeInput.get() != null) {
			        	boolean isInState = false;
			        	for (BEASTInterface o : prior.treeInput.get().getOutputs()) {
			        		if (o instanceof State) {
			        			isInState = true;
			        			break;
			        		}
			        	}
			        	if (!isInState) {
			        		doc.disconnect(prior, "prior", "distribution");
			        		doc.disconnect(prior, "tracelog", "log");
			        		if (prior.onlyUseTipsInput.get()) {
			        			disableTipSampling(prior, doc);
			        		}
			        		doc.unregisterPlugin(prior);
			        		return;
			        	}
					}
				}
			}
		}

	}
	
	protected Set<Taxon> getTaxonCandidates(MRCAPrior prior) {
        Set<Taxon> candidates = new HashSet<>();
        Tree tree = prior.treeInput.get();
        String [] taxa = null;
        if (tree.m_taxonset.get() != null) {
        	try {
            	TaxonSet set = tree.m_taxonset.get();
        		set.initAndValidate();
            	taxa = set.asStringList().toArray(new String[0]);
        	} catch (Exception e) {
            	taxa = prior.treeInput.get().getTaxaNames();
			}
        } else {
        	taxa = prior.treeInput.get().getTaxaNames();
        }
        
        for (String taxon : taxa) {
            candidates.add(doc.getTaxon(taxon));
        }
        return candidates;
    }

    
    InputEditor tipsonlyEditor;
    
    public InputEditor createTipsonlyEditor() throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        BooleanInputEditor e = new BooleanInputEditor (doc) {

			@Override
        	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption,
        			boolean addButtons) {
        		super.init(input, beastObject, itemNr, isExpandOption, addButtons);
        		for (Node o1 : getChildren()) {
        			if (o1 instanceof HBox) {
	            		for (Node o : ((HBox)o1).getChildren()) {
		        			if (o instanceof CheckBox) {
		        				((CheckBox)o).setOnAction(e -> {
				                	CheckBox src = (CheckBox) e.getSource();
				                    MRCAPrior prior = (MRCAPrior) m_beastObject;
				                    prior.onlyUseTipsInput.setValue(src.isSelected(), prior);
				                	if (src.isSelected()) {
				                		enableTipSampling();
				                	} else {
				                		disableTipSampling(m_beastObject, doc);
				                	}
		        				});
		        			}
		    			}
		    		}
		    	}
			}
        };

        MRCAPrior prior = (MRCAPrior) m_beastObject;
        Input<?> input = prior.onlyUseTipsInput;
        e.init(input, prior, -1, ExpandOption.FALSE, false);
        return e;
    }

    // add TipDatesRandomWalker (if not present) and add to list of operators
    private void enableTipSampling() {
    	// First, create/find the operator
    	TipDatesRandomWalker operator = null;
    	MRCAPrior prior = (MRCAPrior) m_beastObject;
    	TaxonSet taxonset = prior.taxonsetInput.get();
    	taxonset.initAndValidate();
    	
    	// see if an old operator still hangs around -- happens when toggling the TipsOnly checkbox a few times
    	for (BEASTInterface o : taxonset.getOutputs()) {
    		if (o instanceof TipDatesRandomWalker) {
    			operator = (TipDatesRandomWalker) o;
    		}
    	}
    	
    	if (operator == null) {
    		operator = new TipDatesRandomWalker();
    		operator.initByName("tree", prior.treeInput.get(), "taxonset", taxonset, "windowSize", 1.0, "weight", 1.0);
    	}
   		operator.setID("tipDatesSampler." + taxonset.getID());
   	    	
    	doc.mcmc.get().setInputValue("operator", operator);
	}

    // remove TipDatesRandomWalker from list of operators
	private static void disableTipSampling(BEASTInterface m_beastObject, BeautiDoc doc) {
    	// First, find the operator
    	TipDatesRandomWalker operator = null;
    	MRCAPrior prior = (MRCAPrior) m_beastObject;
    	TaxonSet taxonset = prior.taxonsetInput.get();
    	
    	// We cannot rely on the operator ID created in enableTipSampling()
    	// since the taxoneset name may have changed.
    	// However, if there is an TipDatesRandomWalker with taxonset as input, we want to remove it.
    	for (BEASTInterface o : taxonset.getOutputs()) {
    		if (o instanceof TipDatesRandomWalker) {
    			operator = (TipDatesRandomWalker) o;
    		}
    	}
    	
    	if (operator == null) {
    		// should never happen
    		return;
    	}
    	
    	// remove from list of operators
    	Object o = doc.mcmc.get().getInput("operator");
    	if (o instanceof Input<?>) {
    		Input<List<Operator>> operatorInput = (Input<List<Operator>>) o;
    		List<Operator> operators = operatorInput.get();
    		operators.remove(operator);
    	}
	}

    VBox expandBox = null;
    
    @Override
	public void setExpandBox(VBox expandBox) {
		this.expandBox = expandBox;
	}
	
	@Override
	public void refreshPanel() {
		if (expandBox != null) {
			ListInputEditor.updateExpandBox(doc, expandBox, m_beastObject, this);
		}
		super.refreshPanel();
	}	

}
