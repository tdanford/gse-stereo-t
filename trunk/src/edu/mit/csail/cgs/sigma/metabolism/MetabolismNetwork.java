package edu.mit.csail.cgs.sigma.metabolism;

import java.util.*;
import java.util.logging.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.Parser;

public class MetabolismNetwork {
	
	private Logger logger;
	private MetabolismProperties props;
	
	private MetabolismAbbreviations abbrevs;
	private Vector<MetabolicEntry> entries;
	private Map<String,Rxn> reactions;
	private Map<String,LogicalORFTree> orfSets;
	private Set<String> locations;
	private Set<String> totalORFs;

	public MetabolismNetwork(MetabolismProperties ps) { 
		props = ps;
		logger = props.getLogger("MetabolismNetwork");
		entries = new Vector<MetabolicEntry>();
		locations = new TreeSet<String>();
		reactions = new TreeMap<String,Rxn>();
		orfSets = new TreeMap<String,LogicalORFTree>();
		totalORFs = new TreeSet<String>();
	}
	
	public Collection<String> getLocations() { return locations; }
	public Logger getLogger() { return logger; }
	public MetabolismAbbreviations getAbbrevations() { return abbrevs; }
	public Collection<String> getTotalORFs() { return totalORFs; }
	public LogicalORFTree getLogicalORFTree(String rxnAbbrev) { return orfSets.get(rxnAbbrev); }
	public Rxn getReaction(String rxnAbbrev) { return reactions.get(rxnAbbrev); }
	public Set<String> getReactionAbbreviations() { return reactions.keySet(); }
	
	public Map<String,Rxn> getReactions() {
		return reactions;
	}
	
	public Set<String> getInputReactions(Rxn r) { 
		TreeSet<String> rxns = new TreeSet<String>();
		for(String rn : reactions.keySet()) { 
			Rxn rxn = reactions.get(rn);
			if(rxn.connects(r)) { 
				rxns.add(rn);
			}
		}
		return rxns;
	}
	
	public Set<String> getOutputReactions(Rxn r) { 
		TreeSet<String> rxns = new TreeSet<String>();
		for(String rn : reactions.keySet()) { 
			Rxn rxn = reactions.get(rn);
			if(r.connects(rxn)) { 
				rxns.add(rn);
			}
		}
		return rxns;
	}
	
	public Map<String,Rxn> getReactions(String compartment) {
		TreeMap<String,Rxn> crxns = new TreeMap<String,Rxn>();
		for(String rn : reactions.keySet()) {
			Rxn r = reactions.get(rn);
			if(r.getLocation().equals(compartment)) { 
				crxns.put(rn, r);
			}
		}
		return crxns;
	}
	
	public void loadNetwork() throws IOException { 
		logger.log(Level.INFO, "Loading metabolism network...");
		
		File file = props.getNetworkFile();
		Mapper<String,MetabolicEntry> entryMapper = new MetabolicEntry.MetabolicMapper();
		Parser<MetabolicEntry> parser = new Parser<MetabolicEntry>(file, entryMapper);
		
		while(parser.hasNext()) { 
			MetabolicEntry entry = parser.next();
			entries.add(entry);
			
			Rxn rxn = new Rxn(props, entry.getReaction(), entry.getAbbreviation(), entry.getReactionName(), entry.getORF());
			reactions.put(entry.getAbbreviation(), rxn);
			
			locations.add(rxn.getLocation());
			
			LogicalORFTree lot = new LogicalORFTree(entry.getORF());
			ORFSet os = new ORFSet(entry.getORF());
			
			totalORFs.addAll(os.getORFs());
			orfSets.put(entry.getAbbreviation(), lot);
		}
		
		logger.log(Level.FINE, String.format("Loaded %d entries.", entries.size()));
		
		abbrevs = new MetabolismAbbreviations(props);
		abbrevs.loadAbbreviations();
		
		logger.log(Level.FINEST, "Loaded abbrevations.");
	}
}
