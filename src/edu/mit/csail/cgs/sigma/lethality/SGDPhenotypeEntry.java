package edu.mit.csail.cgs.sigma.lethality;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Mapper;

/*
 * ftp://genome-ftp.stanford.edu/pub/yeast/data_download/literature_curation/README
 * 
phenotypes.tab       Contains phenotype data, the majority of which is the data 
                     from the systematic deletion project. These data also include 
                     results from the Genetic Footprinting study;
                     additional data from the footprinting study can be found at:
        

ftp://ftp.yestgenome.org/pub/yeast/data_download/systematic_results/genetic_footprinting/

Columns are:            Contents:

1) ORF (mandatory)      - Systematic name of the ORF                        
2) Gene (optional)      - Gene name, if one exists                          
3) SGDID (mandatory)            - the SGDID, unique database identifier, for the ORF
4) Phenotype_type (mandatory)   - classification of the phenotype
5) Phenotype (mandatory)    - the phenotype
6) Description (optional)   - a description of the type of experiment, if available
7) Reference (optional)     - the unique PubMed identifer (PMID:) or
                              the unique SGD identifier (SGD_ref:) for a reference
 */

/**
 * @author tdanford
 * 
 */
public class SGDPhenotypeEntry {

	private String orf, gene, sgdID;
	private String phenotypeType, phenotype, description, reference;

	public SGDPhenotypeEntry(String line) {
	    try { 
	        String[] a = line.split("\t");
	        int i = 0;
	        orf = a[i++]; 
	        gene = a[i++]; 
	        sgdID = a[i++];

	        phenotypeType = a[i++];
            
            if(phenotypeType.equals("Free text")) { 
                phenotype = "";
                description = i < a.length ? a[i++] : "";
                reference = null;
                
            } else { 
            
                phenotype = a[i++];
                description = i < a.length ? a[i++] : null;
                reference = i < a.length ? a[i++] : null;
            }
            
	    } catch(ArrayIndexOutOfBoundsException e) { 
	        System.err.println(String.format("Couldn't parse line \"%s\"", line));
	    }
	}
	
	public String getORF() { return orf; }
	public String getGene() { return gene; }
	public String getSGDID() { return sgdID; }
	public String getPhenotypeType() { return phenotypeType; }
	public String getPhenotype() { return phenotype; }
	public String getDescription() { return description; }
    public String getReference() { return reference; }
	
	public int hashCode() { 
		int code = 17;
		code += orf.hashCode(); code *= 37;
		code += phenotypeType.hashCode(); code *= 37;
		code += phenotype.hashCode(); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof SGDPhenotypeEntry)) { return false; }
		SGDPhenotypeEntry e = (SGDPhenotypeEntry)o;
		return e.orf.equals(orf) && phenotypeType.equals(e.phenotypeType) && 
			phenotype.equals(e.phenotype);
	}
	
	public String toString() { 
		return String.format("%s (%s) : %s --> %s", orf, gene, phenotypeType, phenotype);
	}
	
	public static class SGDPhenotypeMapper implements Mapper<String,SGDPhenotypeEntry> {
		public SGDPhenotypeEntry execute(String a) {
			return new SGDPhenotypeEntry(a);
		} 
	}
}
