package edu.mit.csail.cgs.sigma.biogrid;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Mapper;

// 0: INTERACTOR_A    
// 1: INTERACTOR_B    
// 2: OFFICIAL_SYMBOL_A       
// 3: OFFICIAL_SYMBOL_B       
// 4: ALIASES_FOR_A   
// 5: ALIASES_FOR_B   
// 6: EXPERIMENTAL_SYSTEM     
// 7: SOURCE  
// 8: PUBMED_ID       
// 9: ORGANISM_A_ID   
// 10: ORGANISM_B_ID

public class BiogridEntry {

	private Interactor interA, interB;
	private String system, source, pubmed;
	
	public BiogridEntry(String line) { 
		String[] a = line.split("\t");
		interA = new Interactor(a[0], a[2], a[4], a[9]);
		interB = new Interactor(a[1], a[3], a[5], a[10]);
		system = a[6];
		source = a[7];
		pubmed = a[8];
	}
	
	public Interactor getA() { return interA; }
	public Interactor getB() { return interB; }
	public String getSystem() { return system; }
	public String getSource() { return source; }
	public String getPubmed() { return pubmed; }
	
	public static class BiogridMapper implements Mapper<String,BiogridEntry> {
		public BiogridEntry execute(String a) {
			return new BiogridEntry(a);
		}   
	}
}


