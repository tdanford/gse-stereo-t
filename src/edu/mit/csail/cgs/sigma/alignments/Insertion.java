package edu.mit.csail.cgs.sigma.alignments;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.utils.models.Model;


public class Insertion extends Model { 

	public String strain;
	public String chrom;
	public Integer start, end;
	
	public Insertion() {}
	
	public Insertion(String str, String chr, Integer loc, Integer width) { 
		strain = str;
		chrom = chr;
		start = loc;
		end = loc + width-1;
	}

	public boolean overlaps(Region query) {
		return query.getChrom().equals(chrom) && 
			((query.getStart() <= start && query.getEnd() >= start) || 
				(start <= query.getStart() && end >= query.getStart()));
	}
}
