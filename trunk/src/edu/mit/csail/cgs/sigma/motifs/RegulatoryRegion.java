/*
 * Author: tdanford
 * Date: Mar 1, 2009
 */
package edu.mit.csail.cgs.sigma.motifs;

import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;

public class RegulatoryRegion extends RegionKey {
	
	public RegulatoryRegion() { 
		super();
	}

	public RegulatoryRegion(String c, Integer s, Integer e, String st) { 
		super(c, s, e, st);
	}
	
	public RegulatoryRegion(RegionKey k) { 
		super(k);
	}
	
	
}
