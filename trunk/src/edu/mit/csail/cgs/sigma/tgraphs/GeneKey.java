/*
 * Author: tdanford
 * Date: May 24, 2009
 */
package edu.mit.csail.cgs.sigma.tgraphs;

import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;

public class GeneKey extends RegionKey {
	
	private static int threePrimeWidth = 100;
	
	public String id, name;
	
	public GeneKey() {}
	
	public GeneKey(edu.mit.csail.cgs.datasets.species.Gene g) { 
		super(g.getChrom(), g.getStart(), g.getEnd(), String.valueOf(g.getStrand())); 
		id = g.getID();
		name = g.getName();
	}
	
	public GeneKey(String c, Integer st, Integer ed, String str, String id) {
		super(c, st, ed, str);
		this.id = name = id;
	}
	
	public String toString() { 
		return String.format("gene:%s", id);
	}
	
	public RegionKey threePrimeZone() {
		int width = threePrimeWidth;
		if(strand.equals("+")) { 
			return new RegionKey(chrom, end-width, end, strand);
		} else { 
			return new RegionKey(chrom, start, start+width, strand);
		}
	}

}