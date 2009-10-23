/*
 * Author: tdanford
 * Date: Feb 24, 2009
 */
package edu.mit.csail.cgs.sigma.genes;

import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;

public class GeneAnnotation extends RegionKey {
	
	public String id;

	public GeneAnnotation() { 
		super();
	}
	
	public GeneAnnotation(String c, Integer st, Integer ed, String str, String id) { 
		super(c, st, ed, str);
		this.id = id;
	}
}
