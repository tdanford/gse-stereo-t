/*
 * Author: tdanford
 * Date: Oct 8, 2008
 */
package edu.mit.csail.cgs.sigma.viz.chroms.information;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.viz.chroms.ChromPainter;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;

public class GeneChromPainter implements ChromPainter {
	
	private Region chrom;
	private int[] bins;
	private ArrayList<Region> features;
	private Character strand;
	
	public GeneChromPainter(Region c, Expander<Region,? extends Region> fexp) {
		this(c, fexp, null);
	}
	
	public GeneChromPainter(Region c, Expander<Region,? extends Region> fexp, Character str) { 
		Iterator<? extends Region> ftrs = fexp.execute(c);
		chrom = c;
		bins = null;
		features = new ArrayList<Region>();
		this.strand = str;
		
		while(ftrs.hasNext()) { 
			Region r = ftrs.next();
			if(strand==null || !(r instanceof StrandedRegion) || ((StrandedRegion)r).getStrand()==strand) { 
				features.add(r);
			}
		}
	}
	
	public Character strand() { return strand; }

	public Collection<Region> features() { return features; }

	public int length() {
		return chrom.getWidth()-1;
	}

	public void recalculate(int units) {
		if(bins == null || bins.length != units) { 
			bins = new int[units];
			int len = length();
			
			for(Region f : features) { 
				double f1 = (double)f.getStart() / (double)len;
				double f2 = (double)f.getEnd() / (double)len;
				int u1 = (int)Math.floor(f1 * (double)units);
				int u2 = Math.min(units-1, Math.max(u1+1, (int)Math.ceil(f2 * (double)units)));
				
				for(int i = u1; i < u2; i++) { 
					bins[i] = 1;
				}
			}
			
			System.out.println(String.format("#features=%d", features.size()));
		}
	}

	public int size() {
		return bins == null ? 0 : bins.length;
	}

	public boolean value(int unit) {
		return bins == null ? false : bins[unit] == 1;
	}

}
