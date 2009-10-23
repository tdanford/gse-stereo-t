package edu.mit.csail.cgs.sigma.lethality;

import java.util.regex.*;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.utils.parsing.CSVFileParser;

/*
 * 0: ID,
 * 1: "ORF",
 * 2: "GENE",
 * 3: "SET",
 * 4: "ARRAY",
 * 5: "?1278b VIABILITY",
 * 6: "PHENOTYPIC SEGREGATION",
 * 7: "NOTES",
 * 8: "BIOLOGICAL PROCESS",
 * 9: "MOLECULAR FUNCTION"
*/

public class LethalityEntry {
	
	public static CSVFileParser csv = new CSVFileParser();
	public static Pattern quoted = Pattern.compile("\"(.*)\"");
	
	public static String stripQuotes(String str) { 
		Matcher m = quoted.matcher(str);
		if(m.matches()) { 
			return m.group(1);
		} else { 
			return str;
		}
	}
	
	public static String clean(String str) { 
		str = stripQuotes(str);
		if(str.equals("#N/A")) { 
			return "";
		} else { 
			return str;
		}
	}

	private String id, orf, gene, set, array, viability, 
	segregation, notes, process, function;
	
	public LethalityEntry(String line) { 
		String[] a = csv.parseLine(line);
		id = clean(a[0]);
		orf = clean(a[1]);
		gene = clean(a[2]);
		set = clean(a[3]);
		array = clean(a[4]);
		viability = clean(a[5]);
		segregation = clean(a[6]);
		notes = clean(a[7]);
		process = clean(a[8]);
		function = clean(a[9]);
	}
	
	public String getID() { return id; }
	public String getORF() { return orf; }
	public String getGene() { return gene; }
	public String getSet() { return set; }
	public String getArray() { return array; }
	public String getViability() { return viability; }
	public String getSegregation() { return segregation; }
	public String getNotes() { return notes; }
	public String getProcess() { return process; }
	public String getFunction() { return function; }
	
	public int hashCode() { 
		int code =17;
		code += id.hashCode(); code *= 37;
		code += orf.hashCode(); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof LethalityEntry)) { return false; }
		LethalityEntry e = (LethalityEntry)o;
		return id.equals(e.id) && orf.equals(e.orf);
	}
	
	public String toString() { 
		return String.format("%s: %s (%s)", id, orf, gene);
	}
	
	public static class LethalityMapper implements Mapper<String,LethalityEntry> {
		public LethalityEntry execute(String a) {
			return new LethalityEntry(a);
		}  
	}
}
