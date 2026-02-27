package beastfx.app.inputeditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import javafx.scene.control.ComboBox;
import beastfx.app.util.Alert;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.datatype.DataType;
import beast.base.parser.NexusParser;

@Description("Fasta file importer")
public class FastaImporter implements AlignmentImporter {
	enum dtype { userdefined, aminoacid, nucleotide};
	dtype datatype = dtype.userdefined;

	public FastaImporter() {
		super();
		datatype = dtype.userdefined;
	}
	
	@Override
	public String[] getFileExtensions() {
		return new String[]{"fa","fas","fst","fasta","fna","ffn","faa","frn"};
	}

	@Override
	public List<BEASTInterface> loadFile(File file) {
		List<BEASTInterface> selectedBEASTObjects = new ArrayList<>();
	    	try {
	    		// grab alignment data
	        	Map<String, StringBuilder> seqMap = new HashMap<>();
	        	List<String> taxa = new ArrayList<>();
	        	String currentTaxon = null;
				BufferedReader fin = new BufferedReader(new FileReader(file));
		        String missing = "?";
		        String gap = "-";
		        int totalCount = 4;
		        String datatype = "nucleotide";
		        // According to http://en.wikipedia.org/wiki/FASTA_format lists file formats and their data content
				// .fna = nucleic acid
				// .ffn = nucleotide coding regions
				// .frn = non-coding RNA
				// .ffa = amino acid
		        String filename = file.getName();
		        if (filename.toLowerCase().endsWith(".txt")) {
		        	filename = filename.substring(0, filename.length() - 4);
		        }
	    		boolean mayBeAminoacid = !(filename.toLowerCase().endsWith(".fna") || filename.toLowerCase().endsWith(".ffn") || filename.toLowerCase().endsWith(".frn"));
	    		
				while (fin.ready()) {
					String line = fin.readLine();
					if (line.startsWith(";")) {
						// it is a comment, ignore
					} else 	if (line.startsWith(">")) {
						// it is a taxon
						currentTaxon = line.substring(1).trim();
						// only up to first space
						currentTaxon = currentTaxon.replaceAll("\\s.*$", "");
					} else {
						// it is a data line
						if (currentTaxon == null) {
							fin.close();
							throw new RuntimeException("Expected taxon defined on first line");
						}
						if (seqMap.containsKey(currentTaxon)) {
							StringBuilder sb = seqMap.get(currentTaxon);
							sb.append(line);
						} else {
							StringBuilder sb = new StringBuilder();
							seqMap.put(currentTaxon, sb);
							sb.append(line);
							taxa.add(currentTaxon);
						}
					}
				}
				fin.close();
				
				int charCount = -1;
				Alignment alignment = new Alignment();
				HashMap<String, Integer> guessDataTypeMap = new HashMap<>();
		        for (final String taxon : taxa) {
		            final StringBuilder bsData = seqMap.get(taxon);
		            String data = bsData.toString();
		            data = data.replaceAll("\\s", "");
		            seqMap.put(taxon, new StringBuilder(data));

		            if (charCount < 0) {charCount = data.length();}
		            if (data.length() != charCount) {
		                throw new IllegalArgumentException("Expected sequence of length " + charCount + " instead of " + data.length() + " for taxon " + taxon);
		            }
		            // map to standard missing and gap chars
		            data = data.replace(missing.charAt(0), DataType.MISSING_CHAR);
		            data = data.replace(gap.charAt(0), DataType.GAP_CHAR);

					String guessType = guessSequenceType(data);
					if (guessDataTypeMap.containsKey(guessType)) {
						guessDataTypeMap.put(guessType, guessDataTypeMap.get(guessType) + 1);
					} else {
						guessDataTypeMap.put(guessType, 1);
					}

		            if (mayBeAminoacid && datatype.equals("nucleotide") && 
		            		guessType != null && guessType.equals("aminoacid")) {
		            	datatype = "aminoacid";
		            	totalCount = 20;
		            	for (Sequence seq : alignment.sequenceInput.get()) {
		            		seq.totalCountInput.setValue(totalCount, seq);
		            	}
		            }
		            
		            final Sequence sequence = new Sequence();
		            data = data.replaceAll("[Xx]", "?");
		            sequence.init(totalCount, taxon, data);
		            sequence.setID(NexusParser.generateSequenceID(taxon));
		            alignment.sequenceInput.setValue(sequence, alignment);
		        }
		        String ID = file.getName();
		        ID = ID.substring(0, ID.lastIndexOf('.')).replaceAll("\\..*", "");
		        alignment.setID(ID);

				// get most likely guess for data type
				int maxCount = 0;
				String guessTypeMax = "nucleotide";
				Set<String> dataTypeKeys = guessDataTypeMap.keySet();
				for (String k: dataTypeKeys) {
					if (guessDataTypeMap.get(k) > maxCount) {
						maxCount = guessDataTypeMap.get(k);
						guessTypeMax = k;
					}
				}
				
				String currentProvider = "";
		        if (mayBeAminoacid) {
		        	switch (this.datatype) {
			        	case userdefined: 
			        		// make user choose 
							TreeMap<String, DataType> allTypes = Alignment.getTypes();
							// guessing data type
							String[] providers;
							if (guessTypeMax != null && (guessTypeMax.equals("nucleotide") || guessTypeMax.equals("aminoacid"))) {
								// data types excluding numeric types
								Set<String> providerSet = new HashSet<>();
								for (String typeName: allTypes.keySet()) {
									try {
										DataType.Base type = (DataType.Base) allTypes.get(typeName);
										if (isNumericType(type) == false) {
											providerSet.add(typeName);
										}
									} catch	(ClassCastException e) {}
								}
								providers = addAllNucleotideAminoAcidToSet(providerSet);
								System.out.println("Guessing type: " + guessTypeMax);
								if (guessTypeMax.equals("nucleotide")) {
									currentProvider = "nucleotide";
								} else {
									currentProvider = "aminoacid";
								}
							} else if (guessTypeMax != null && guessTypeMax.equals("numerictype")) {
								// data types excluding non numeric types
								Set<String> providerSet = new HashSet<>();
								for (String typeName: allTypes.keySet()) {
									try {
										DataType.Base type = (DataType.Base) allTypes.get(typeName);
										if (isNumericType(type)) {
											// add numeric data type
											providerSet.add(typeName);
										}
									} catch (ClassCastException e) {}
								}
								providers = new String[providerSet.size()];
								providers = providerSet.toArray(providers);
								currentProvider = providers[0];
							} else {
								// all data types
								providers = addAllNucleotideAminoAcidToSet(allTypes.keySet());	;
								currentProvider = providers[0];
							}

				        	String selectedType = (String) Alert.showInputDialog(null, "Choose the datatype of alignment " + alignment.getID(),
				                    "Add partition",
				                    Alert.QUESTION_MESSAGE, null, providers,
				                    currentProvider);
				        	
				        	switch (selectedType) {
					        	case "aminoacid": datatype = "aminoacid"; totalCount = 20; break;
					        	case "nucleotide": datatype = "nucleotide"; totalCount = 4; break;
					        	case "all are aminoacid": datatype = "aminoacid"; this.datatype = dtype.aminoacid; totalCount = 20; break;
					        	case "all are nucleotide": datatype = "nucleotide"; this.datatype = dtype.nucleotide; totalCount = 4; break;
								default:
									// catch all for other data types
									datatype = selectedType;
									totalCount = allTypes.get(selectedType).getStateCount();
									break;
				        	}
				        	break;
			        	case aminoacid:
			        		datatype = "aminoacid";
			        		totalCount = 20;
			        		break;
			        	case nucleotide:
			        		datatype = "nucleotide";
			        		totalCount = 4;
		        	}
	            	for (Sequence seq : alignment.sequenceInput.get()) {
	            		seq.totalCountInput.setValue(totalCount, seq);
	            	}
		        }
				alignment.dataTypeInput.setValue(datatype, alignment);
		        alignment.initAndValidate();
		        selectedBEASTObjects.add(alignment);
	    	} catch (Exception e) {
				e.printStackTrace();
				Alert.showMessageDialog(null, "Loading of " + file.getName() + " failed: " + e.getMessage());
	    	}
		return selectedBEASTObjects;
	}

	private String[] addAllNucleotideAminoAcidToSet(Set<String> keySet) {
		String[] dataNames = new String[keySet.size() + 2];
		dataNames[0] = "aminoacid";
		dataNames[1] = "nucleotide";
		dataNames[2] = "all are aminoacid";
		dataNames[3] = "all are nucleotide";
		int i = 4;
		for (String key: keySet) {
			if (!key.equals("nucleotide") && !key.equals("aminoacid")) {
				dataNames[i] = key;
				i++;
			}
		}
		return dataNames;
	}

	private boolean isNumericType(DataType.Base type) {
		int stateCount = type.getStateCount();
		if (stateCount == -1) {
			// infinite state count such as IntegerData
			return true;
		}
		boolean allNumeric = true;
		String codeMap = type.getCodeMap();
		if (codeMap != null) {
			for (int z = 0; z < codeMap.length(); z++) {
				char c = codeMap.charAt(z);
				String numericString = "0123456789.-";
				if (c != DataType.GAP_CHAR && c != DataType.MISSING_CHAR && numericString.indexOf(c) == -1) {
					allNumeric = false;
				}
			}
			if (allNumeric) {
				// type has all numeric character map
				return true;
			} else {
				return false;
			}
		} else {
			// type has no code map (may not be numeric)
			return false;
		}
	}

	/** Ported from jebl2
     * Guess type of sequence from contents.
     * @param seq the sequence
     * @return SequenceType.NUCLEOTIDE or SequenceType.AMINO_ACID, if sequence is believed to be of that type.
     *         If the sequence contains characters that are valid for neither of these two sequence
     *         types, then this method returns null.
     */
    public String guessSequenceType(final String seq) {

        int canonicalNucStates = 0;
        int undeterminedStates = 0;
        // true length, excluding any gaps
        int sequenceLength = seq.length();
        final int seqLen = sequenceLength;

        boolean onlyValidNucleotides = true;
        boolean onlyValidAminoAcids = true;
		boolean onlyValidNumeric = true;

        // do not use toCharArray: it allocates an array size of sequence
        for(int k = 0; (k < seqLen) && (onlyValidNucleotides || onlyValidAminoAcids || onlyValidNumeric); ++k) {
            final char c = seq.charAt(k);
            final boolean isNucState = ("ACGTUXNacgtuxn?_-".indexOf(c) > -1);
            final boolean isAminoState = ("ACDEFGHIKLMNPQRSTVWYXacdefghiklmnpqrstvwyx?_-".indexOf(c) > -1);
			final boolean isNumericState = ("0123456789?_-.".indexOf(c) > -1);

            onlyValidNucleotides &= isNucState;
            onlyValidAminoAcids &= isAminoState;
			onlyValidNumeric &= isNumericState;

            if (onlyValidNucleotides) {
                assert(isNucState);
                if (("ACGTacgt".indexOf(c) > -1)) {
                    ++canonicalNucStates;
                } else {
                    if (("?_-".indexOf(c) > -1)) {
                        --sequenceLength;
                    } else if( ("UXNuxn".indexOf(c) > -1)) {
                        ++undeterminedStates;
                    }
                }
            } else if (onlyValidAminoAcids && "?_-".indexOf(c) > -1) {
				--sequenceLength;
			} else if (onlyValidNumeric && "?_-".indexOf(c) > -1) {
				--sequenceLength;
			}
        }

        String result = "aminoacid";
        if (onlyValidNucleotides) {  // only nucleotide states
            // All sites are nucleotides (actual or ambigoues). If longer than 100 sites, declare it a nuc
            if( sequenceLength >= 100 ) {
                result = "nucleotide";
            } else {
                // if short, ask for 70% of ACGT or N
                final double threshold = 0.7;
                final int nucStates = canonicalNucStates + undeterminedStates;
                // note: This implicitely assumes that every valid nucleotide
                // symbol is also a valid amino acid. This is true since we
                // added support for the 21st amino acid, U (Selenocysteine)
                // in AminoAcids.java.
                result = nucStates >= sequenceLength * threshold ? "nucleotide" : "aminoacid";
            }
        } else if (onlyValidAminoAcids) {
            result = "aminoacid";
        } else if (onlyValidNumeric && canonicalNucStates == 0) {
			result = "numerictype";
		} else {
			result = null;
		}
        return result;
    }

	
}