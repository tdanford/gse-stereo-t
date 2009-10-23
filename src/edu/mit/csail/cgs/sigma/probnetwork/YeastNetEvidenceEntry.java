package edu.mit.csail.cgs.sigma.probnetwork;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Mapper;

/*
 * Evidence code
  	
  Dataset
  0: CC	
  Co-citation of yeast genes
  1: CX	
  Co-expression among yeast genes (500 microarray datasets)
  2: GN	
  Gene neighbourhoods of bacterial and archaeal orthologs
  3: GT	
  Yeast genetic interactions (multiple datasets)
  4: LC	
  Literature curated yeast protein interactions
  5: MS	
  Protein complexes from affinity purification/mass spectrometry (multiple datasets)
  6: PG	
  Co-inheritance of bacterial orthologs of yeast genes
  7: RS	
  Rosetta Stone protein-based functional linkages
  8: TS	
  Protein interactions inferred from tertiary structures of complexes
  9: YH	
  High-throughput yeast 2-hybrid assays (multiple datasets)
 */
public class YeastNetEvidenceEntry {
	
	public static Map<String,Integer> codeMap;
	
	static { 
		codeMap = new LinkedHashMap<String,Integer>();
		codeMap.put("CC", 0);
		codeMap.put("CX", 1);
		codeMap.put("GN", 2);
		codeMap.put("GT", 3);
		codeMap.put("LC", 4);
		codeMap.put("MS", 5);
		codeMap.put("PG", 6);
		codeMap.put("RS", 7);
		codeMap.put("TS", 8);
		codeMap.put("YH", 9);
	}

	private String name1, name2;
	private Vector<Double> evidence;
	private double value;
	
	public YeastNetEvidenceEntry(String line) { 
		String[] a = line.split("\\s+");
		name1 = a[0];
		name2 = a[1];
		evidence = new Vector<Double>();
		for(int i = 0; i < 10; i++) { 
			String e = a[i+2];
			if(e.equals("NA")) { 
				evidence.add(null);
			} else { 
				evidence.add(Double.parseDouble(e));
			}
		}
		
		value = Double.parseDouble(a[12]);
	}
	
	public String getName1() { return name1; }
	public String getName2() { return name2; }
	public double getValue() { return value; }
	public Double getEvidence(int i) { return evidence.get(i); }
	public boolean hasEvidence(int i) { return evidence.get(i) != null; }
	
	public Double getEvidence(String k) { return evidence.get(codeMap.get(k)); }
	
	public boolean hasEvidence(String k) { 
		return codeMap.containsKey(k) && evidence.get(codeMap.get(k)) != null;
	}
	
	public int hashCode() { 
		int code = 17;
		code += name1.hashCode(); code *= 37;
		code += name2.hashCode(); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof YeastNetEvidenceEntry)) { return false; }
		YeastNetEvidenceEntry e = (YeastNetEvidenceEntry)o;
		return name1.equals(e.name1) && name2.equals(e.name2) && value==e.value;
	}
	
	public String toString() { 
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s\t%s", name1, name2));
		for(Double ev : evidence) { 
			if(ev == null) { 
				sb.append("\tNA");
			} else { 
				sb.append(String.format("\t%.4f", ev));
			}
		}
		sb.append(String.format("\t%.4f", value));
		return sb.toString();
	}
	
	public static class ParserMapper implements Mapper<String,YeastNetEvidenceEntry> { 
		public YeastNetEvidenceEntry execute(String a) { 
			return new YeastNetEvidenceEntry(a);
		}
	}
}
