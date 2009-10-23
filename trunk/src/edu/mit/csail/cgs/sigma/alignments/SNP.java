package edu.mit.csail.cgs.sigma.alignments;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.utils.models.Model;

public class SNP extends Model {

	public String strain;
	public String chrom;
	public Integer location;
	
	public SNP() {}
	
	public SNP(String str, String chr, Integer loc) { 
		strain = str;
		chrom = chr;
		location = loc;
	}
	
	public boolean overlaps(Region query) {
		return query.getChrom().equals(chrom) && (query.getStart() <= location && query.getEnd() >= location); 
	}
}
