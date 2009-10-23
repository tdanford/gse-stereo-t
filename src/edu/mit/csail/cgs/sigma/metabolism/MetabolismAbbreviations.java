package edu.mit.csail.cgs.sigma.metabolism;

import java.util.*;
import java.util.logging.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.Parser;

public class MetabolismAbbreviations {
	
	public static void main(String[] args) { 
		MetabolismProperties props = new MetabolismProperties();
		MetabolismAbbreviations abbrevs = new MetabolismAbbreviations(props);
		try {
			abbrevs.loadAbbreviations();
		} catch (IOException e) {
			abbrevs.getLogger().log(Level.SEVERE, e.getMessage());
		}
	}
	
	private MetabolismProperties props;
	private Logger logger;
	
	private Map<String,Vector<MetabolicAbbreviation>> abbrevs;
	
	public MetabolismAbbreviations(MetabolismProperties ps) { 
		props = ps;
		logger = props.getLogger("MetabolismAbbreviations");
		
		abbrevs = new TreeMap<String,Vector<MetabolicAbbreviation>>();
	}
	
	public Logger getLogger() { return logger; }
	
	public void loadAbbreviations() throws IOException {
		logger.log(Level.INFO, "Loading metabolism abbreviations...");
		
		File file = props.getAbbreviationFile();
		Mapper<String,MetabolicAbbreviation> mapper = new AbbreviationMapper();
		int header = props.getAbbreviationHeaderLines();
		Parser<MetabolicAbbreviation> parser = new Parser<MetabolicAbbreviation>(file, mapper, header);
		
		int i = 0;
		while(parser.hasNext()) { 
			MetabolicAbbreviation abbrev = parser.next();
			addAbbreviation(abbrev);
			i++;
		}
		
		logger.log(Level.FINE, String.format("Loaded %d abbreviation entries.", i));
	}
	
	private void addAbbreviation(MetabolicAbbreviation a) { 
		if(!abbrevs.containsKey(a.abbreviation)) { 
			abbrevs.put(a.abbreviation, new Vector<MetabolicAbbreviation>());
		} else { 
			for(MetabolicAbbreviation abb : abbrevs.get(a.abbreviation)) {
				if(!abb.metaMatches(a)) { 
					String msg = String.format("META-DATA MISMATCH (%s) vs. (%s)", 
							abb.toString(), a.toString());
					logger.log(Level.WARNING, msg);
					return;
				}
			}
		}
		abbrevs.get(a.abbreviation).add(a);
	}
	
	public boolean containsAbbreviation(String abb) { 
		return abbrevs.containsKey(abb);
	}
	
	public String getName(String abb) { 
		return abbrevs.get(abb).get(0).name;
	}
	
	public Set<String> getCompartments(String abb) { 
		TreeSet<String> cmts = new TreeSet<String>();
		for(MetabolicAbbreviation abbr : abbrevs.get(abb)) { 
			cmts.add(abbr.compartment);
		}
		return cmts;
	}
	
	public String getFormula(String abb) { 
		return abbrevs.get(abb).get(0).formula;
	}

	private static class AbbreviationMapper implements Mapper<String,MetabolicAbbreviation> {
		public MetabolicAbbreviation execute(String a) {
			return new MetabolicAbbreviation(a);
		} 
	}
	
	private static class MetabolicAbbreviation {
		
		public String abbreviation;
		public String name;
		public String compartment;
		public String formula;
		public int charge;
		
		public MetabolicAbbreviation(String line) { 
			String[] a = line.split("\\t+");
			if(a.length < 4 || a.length > 6) { 
				throw new IllegalArgumentException(line);
			}
			
			int i = a.length==5 ? 0 : 1;
			
			abbreviation = a[i++];
			name = a[i++];
			compartment = a[i++];
			formula = a[i++];
			charge = Integer.parseInt(a[i++]);
		}
		
		public boolean metaMatches(MetabolicAbbreviation a) { 
			if(!name.equals(a.name)) { return false; }
			if(!formula.equals(a.formula)) { return false; }
			if(charge != a.charge) { return false; }
			return true;
		}
		
		public int hashCode() { 
			int code = 17;
			code += abbreviation.hashCode(); code *= 37;
			code += name.hashCode(); code *= 37;
			code += compartment.hashCode(); code *= 37;
			code += formula.hashCode(); code *= 37;
			code += charge; code *= 37;
			return code;
		}
		
		public String toString() { 
			return String.format("%s (%s) %s : %s %d", abbreviation, name, compartment, formula, charge);
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof MetabolicAbbreviation)) { return false; }
			MetabolicAbbreviation a = (MetabolicAbbreviation)o;
			if(!abbreviation.equals(a.abbreviation)) { return false; }
			if(!name.equals(a.name)) { return false; }
			if(!compartment.equals(a.compartment)) { return false; }
			if(!formula.equals(a.formula)) { return false; }
			if(charge != a.charge) { return false; }
			return true;
		}
	}
}

