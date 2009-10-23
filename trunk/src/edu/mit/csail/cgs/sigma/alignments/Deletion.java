package edu.mit.csail.cgs.sigma.alignments;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.utils.models.Model;

public class Deletion extends Model { 

	public String strain;
	public String chrom;
	public Integer location;
	public Integer size;
	
	public Deletion() {}
	
	public Deletion(String str, String chr, Integer loc, Integer s) { 
		strain = str;
		chrom = chr;
		location = loc;
		size = s;
	}
	
	public boolean overlaps(Region query) {
		return query.getChrom().equals(chrom) && (query.getStart() <= location && query.getEnd() >= location); 
	}

}
