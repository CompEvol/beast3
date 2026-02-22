package beastfx.app.methodsection;

import java.lang.annotation.Annotation;
import java.util.*;

import beast.base.core.*;

public class CitationPhrase extends Phrase {
	static public Map<String,CitationPhrase> citations = new LinkedHashMap<>();
	
	String DOI;
	Citation citation;
	int counter;
	
	public enum mode {none,bibtex,markdown,text,pandocmd}
	
	public static mode CitationMode = mode.text;

	static public CitationPhrase createCitationPhrase(Object source) {
		if (source instanceof BEASTInterface) {
			BEASTInterface bi = (BEASTInterface) source;
			List<Citation> c = bi.getCitationList();
			if (c != null && c.size() > 0) {				
				if (citations.containsKey(c.get(0).DOI())) {
					return citations.get(c.get(0).DOI());
				} else {
					CitationPhrase phrase = new CitationPhrase(source);
					citations.put(c.get(0).DOI(), phrase);
					return phrase;
				}
			}
		}
		
		List<Citation> c = getCitationList(source);
		if (c != null && c.size() > 0) {				
			if (citations.containsKey(c.get(0).DOI())) {
				return citations.get(c.get(0).DOI());
			} else {
				CitationPhrase phrase = new CitationPhrase(source);
				citations.put(c.get(0).DOI(), phrase);
				return phrase;
			}
		}

		CitationPhrase phrase = new CitationPhrase(source);
		return phrase;
	}
	
	
    static private List<Citation> getCitationList(Object o) {
        final Annotation[] classAnnotations = o.getClass().getAnnotations();
        List<Citation> citations = new ArrayList<>();
        for (final Annotation annotation : classAnnotations) {
            if (annotation instanceof Citation) {
            	citations.add((Citation) annotation);
            }
            if (annotation instanceof Citation.Citations) {
            	for (Citation citation : ((Citation.Citations) annotation).value()) {
            		citations.add(citation);
            	}
            }
        }
       	return citations;
    }

	
	static public CitationPhrase createCitationPhrase(String DOI) {
		if (citations.containsKey(DOI)) {
			return citations.get(DOI);
		}		
		return new CitationPhrase(DOI);		
	}	
	
	private CitationPhrase(Object source) {
		super(source, "");
		
		if (source instanceof BEASTInterface) {
			BEASTInterface bi = (BEASTInterface) source;
			List<Citation> c = bi.getCitationList();
			if (c != null && c.size() > 0) {
				citation = c.get(0);
				DOI = citation.DOI();
				counter = citations.size() + 1;
				if (DOI != null) {
					String text = toString();
					if (!text.equals(" (null)")) {
						setText(text);
					} else {
						setText(citation.value());
					}
				} else {
					setText(citation.value());
				}
			}
		}
		citations.put(DOI,this);
		counter = citations.size();
	}

	private CitationPhrase(String DOI) {
		super(DOI);
		this.DOI = DOI;		
		citations.put(DOI,this);
		counter = citations.size();
		setText(toString());
	}
	
	@Override
	public String toString() {
		switch (CitationMode) {
		case none:
			return "[" + counter + "]";
		case bibtex:
			return bibtexRef();
		case text:
			return textRef();
		case markdown:
			return markdownRef();
		case pandocmd:
			return "^[" + counter + "](http://doi.org/" + DOI + ")^";
		}
		return "";
	}

	private String markdownRef() {
		try {
			String citation = DOI2Citation.resolve(DOI);
			return bibtex2markdownRef(citation);
		} catch (Exception e) {
			e.printStackTrace();
			return " ([@" + DOI + "])";
		}
	}

	public static String bibtex2markdownRef(String citation) {
		String ref = bibtex2textRef(citation);
		ref = ref.replaceAll("[\\(\\) ]","");
		return " ([@" + ref + "])";
	}


	private String textRef() {
		try {
			String citation = DOI2Citation.resolve(DOI);
			return " (" + bibtex2textRef(citation) +")";
		} catch (Exception e) {
			e.printStackTrace();
			return " (" + DOI + ")";
		}
	}

	/** format text reference from bibtex citation **/
	public static String bibtex2textRef(String citation) {
		if (citation == null) {
			return null;
		}
		String [] strs = citation.split("\n");
		String author = "";
		String year = "";
		for (String str : strs) {
			if (str.trim().startsWith("author=")) {
				author = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
				String [] authors = author.split("and");
				if (authors.length == 1) {
					author = getSurname(author);
				} else if (authors.length == 2) {
					author = getSurname(authors[0]) + " and " + getSurname(authors[1]);
				} else {
					author = getSurname(authors[0]) + " et al.";
				}
			}
			if (str.trim().startsWith("year=")) {
				year = str.substring(str.indexOf("year=") + 6, str.indexOf('}'));
			}
		}
		return author + " (" + year + ")";
	}


	private static String getSurname(String author) {
		if (author.indexOf(',') > 0) {
			author = author.substring(0, author.indexOf(','));
		}
		return author;
	}


	private String bibtexRef() {
		try {
			String citation = DOI2Citation.resolve(DOI);
			String cite = citation.substring(citation.indexOf('{') + 1, citation.indexOf(','));
			return "\\cite{" + cite + "}";
		} catch (Exception e) {
			return "\\cite{" + DOI + "}";
		}
	}

	public String toReference() throws Exception {
		switch (CitationMode) {
		case none:
			return counter + ": " + DOI2Citation.resolve(DOI, "apa");
		case bibtex:
			return DOI2Citation.resolve(DOI);
		case text:
			return DOI2Citation.resolve(DOI, "apa");
		case markdown:
			String ref = markdownRef();
			ref = ref.substring(2, ref.length() - 1);
			return ref + ": " + DOI2Citation.resolve(DOI, "apa");
		case pandocmd:
			return "\n" + counter + ": "+ DOI2Citation.resolve(DOI, "apa");
		}
		return "";		
	}
}
