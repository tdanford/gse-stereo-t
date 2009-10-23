package edu.mit.csail.cgs.sigma.metabolism;

import java.io.IOException;

import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.Parser;

/*
 * 0: ABBREVIATION    
 * 1: REACTION NAME   
 * 2: REACTION
 * 3: E.C. #  
 * 4: SUBSYSTEM       
 * 5: ORF     
 * 6: PROTEIN

Comments: 
 *Reactions that occur entirely within one compartment have a compartmental abbreviation at the beginning of the equation.
 Otherwise, metabolite location is noted immediately after its abbreviation.

Compartment Abbreviations
[c] : cytosol    [e] : extracellular    [g] : Golgi appratus    [m] : mitochondrion    [n] : nucleus    [r] : endoplasmi
c reticulum    [v] : vacoule    [x] : peroxisome"       
 */


public class MetabolicEntry {
	
	// Loads and prints the raw metabolic entry file.  I'm using this to test
	// whether the parsing and the LogicalORFTree stuff works correctly. -tim
	public static void main(String[] args) {
		MetabolismProperties props = new MetabolismProperties();
		Mapper<String,MetabolicEntry> decoder = new MetabolicEntry.MetabolicMapper();
		try {
			Parser<MetabolicEntry> parser = 
				new Parser<MetabolicEntry>(props.getNetworkFile(), decoder);
			while(parser.hasNext()) { 
				MetabolicEntry e = parser.next();
				String orfString = e.getProtein();
				System.out.println(String.format("GENES: '%s'", orfString));

				LogicalORFTree tree = new LogicalORFTree(orfString);
				tree.printTree(System.out);
				System.out.println();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String abbrev, reactionName, reaction, EC, subsystem, orf, protein;
	
	public MetabolicEntry(String line) { 
		String[] a = line.split("\t");
		abbrev = a[0];
		if(a.length <= 1) { System.err.println(line); }
		reactionName = a[1];
		reaction = a[2];
		EC = a[3];
		subsystem = a[4];
		orf = a.length > 5 ? a[5] : "";
		protein = a.length > 6 ? a[6] : "";
	}
	
	public String getAbbreviation() { return abbrev; }
	public String getReactionName() { return reactionName; }
	public String getReaction() { return reaction; }
	public String getEC() { return EC; }
	public String getSubsystem() { return subsystem; }
	public String getORF() { return orf; }
	public String getProtein() { return protein; }
	
	public int hashCode() { 
		int code = 17;
		code += abbrev.hashCode(); code *= 37;
		code += reaction.hashCode(); code *= 37;
		code += orf.hashCode(); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof MetabolicEntry)) { return false; }
		MetabolicEntry e = (MetabolicEntry)o;
		if(!abbrev.equals(e.abbrev)) { return false; }
		if(!reactionName.equals(e.reactionName)) { return false; }
		if(!reaction.equals(e.reaction)) { return false; }
		if(!orf.equals(e.orf)) { return false; }
		if(!protein.equals(e.protein)) { return false; }
		return true;
	}
	
	public String toString() { 
		return String.format("%s (%s)", reactionName, reaction);
	}
	
	public static class MetabolicMapper implements Mapper<String,MetabolicEntry> {
		public MetabolicEntry execute(String a) {
			return new MetabolicEntry(a);
		} 
	}
}
