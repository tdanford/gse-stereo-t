/*
 * Author: tdanford
 * Date: Mar 23, 2009
 */
package edu.mit.csail.cgs.sigma.motifs;

import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixHit;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.utils.models.Model;

public class MotifHit extends RegionKey {
	
	public Double score;
	public String motif;

	public MotifHit() {}
	
	public MotifHit(String c, Integer st, Integer ed, String str, Double wt, String mtf) { 
		super(c, st, ed, str);
		score = wt;
		motif = mtf;
	}
	
	public MotifHit(WeightMatrixHit h) { 
		super(h.getChrom(), h.getStart(), h.getEnd(), String.valueOf(h.getStrand()));
		score = h.getScore();
		motif = h.getMatrix().name;
	}
	
	public int hashCode() { 
		int code = super.hashCode(); 
		code += motif.hashCode(); 
		code *= 37;
		return code;
	}
	
	public String toString() { 
		return String.format("%s@%s", motif, super.toString());
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof MotifHit)) { return false; }
		MotifHit h = (MotifHit)o;
		if(!super.equals(h)) { return false; }
		return motif.equals(h.motif);
	}
}
