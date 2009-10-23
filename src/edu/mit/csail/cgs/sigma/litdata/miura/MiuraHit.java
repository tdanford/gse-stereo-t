package edu.mit.csail.cgs.sigma.litdata.miura;

import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;

public class MiuraHit extends RegionKey {
	
	public String estName;
	
	public MiuraHit() {}
	
	public MiuraHit(String key, String chrom, Integer start, Integer end, String strand) {
		super(chrom, start, end, strand);
		estName = key;
	}
}