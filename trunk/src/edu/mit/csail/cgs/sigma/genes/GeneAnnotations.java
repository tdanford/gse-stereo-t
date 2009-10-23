package edu.mit.csail.cgs.sigma.genes;

import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import java.io.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.Parser;

public class GeneAnnotations {

	private GeneAnnotationProperties props;
	private Logger logger;
	
	private Map<String,GeneOrthologyEntry> orthologyEntries;
	
	public GeneAnnotations(GeneAnnotationProperties ps) { 
		props = ps;
		logger = props.getLogger("GeneAnnotations");
		orthologyEntries = new TreeMap<String,GeneOrthologyEntry>();
	}
	
	public boolean isOrthologous(String g) { return orthologyEntries.containsKey(g); }
	public Set<String> getGeneList() { return orthologyEntries.keySet(); }
	
	public Set<String> getGeneList(GeneOrthologyEntry.Flag f) { 
		TreeSet<String> geneNames = new TreeSet<String>();
		for(String gene : orthologyEntries.keySet()) {
			if(f != null) { 
				if(orthologyEntries.get(gene).getFlags().contains(f)) { 
					geneNames.add(gene);
				} 
			} else { 
				if(orthologyEntries.get(gene).getFlags().size() == 0) { 
					geneNames.add(gene);
				}
			}
		}
		return geneNames;
	}
    
    public Set<String> getMatchedGenes() { 
        Set<String> geneNames = new TreeSet<String>();
        geneNames.addAll(getGeneList(GeneOrthologyEntry.Flag.PERFECT));
        geneNames.addAll(getGeneList(null));
        return geneNames;
    }
    
    public Set<String> getMissingGenes() { 
    	return getGeneList(GeneOrthologyEntry.Flag.MISSING);
    }
	
	public void loadData() { 
		File orthologFile = props.getOrthologousGenesFile();
		Mapper<String,GeneOrthologyEntry> mapper = new GeneOrthologyEntry.ParsingMapper();
		try {
			Parser<GeneOrthologyEntry> orthParser = new Parser<GeneOrthologyEntry>(orthologFile, mapper);
			while(orthParser.hasNext()) { 
				GeneOrthologyEntry e = orthParser.next();
				orthologyEntries.put(e.getGeneName(), e);
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, String.format("loadData() IOException: %s", e.getMessage()));
		}
	}
}
