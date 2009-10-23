/*
 * Author: tdanford
 * Date: May 1, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.models;

import edu.mit.csail.cgs.datasets.general.Region;

public class TranscriptCall extends RegionKey {

	public Double[] intensities;
	public Double falloff;
	
	public TranscriptCall() {}
	
	public TranscriptCall(String c, String s, Integer st, Integer ed, Double intsty, Double fall) { 
		super(c, st, ed, s);
		intensities = new Double[] { intsty };
		falloff = fall;
	}
	
	public TranscriptCall(String c, String s, Integer st, Integer ed, Double[] intsty, Double fall) { 
		super(c, st, ed, s);
		intensities = intsty.clone();
		falloff = fall;
	}
	
	public String toString() { 
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < intensities.length; i++) { 
			sb.append(String.format("%.2f ", intensities[i]));
		}
		return String.format("%s -> %s(%.5f)", super.toString(), sb.toString(), falloff);
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof TranscriptCall)) { return false; }
		TranscriptCall c = (TranscriptCall)o;
		if(intensities.length != c.intensities.length) { return false; }
		for(int i = 0; i < intensities.length; i++) { 
			double ieps = Math.abs(c.intensities[i] - intensities[i]);
			if(ieps >= 0.001) { 
				return false;
			}
		}
		double feps = Math.abs(c.falloff - falloff);
		return super.equals(c) && feps <= 1.0e-6; 
	}
	
	public int hashCode() {  
		return super.hashCode();
	}

	public boolean overlapsStrandInvariant(Region region) {
		return chrom.equals(region.getChrom()) && 
			(start <= region.getStart() && end >= region.getStart()) || 
			(region.getStart() <= start && region.getEnd() >= start);
	}

	public int length() {
		return end-start+1;
	}
}
