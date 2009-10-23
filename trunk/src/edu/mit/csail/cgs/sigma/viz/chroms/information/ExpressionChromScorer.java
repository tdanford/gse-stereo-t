/*
 * Author: tdanford
 * Date: Oct 8, 2008
 */
package edu.mit.csail.cgs.sigma.viz.chroms.information;

import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.expression.models.*;
import edu.mit.csail.cgs.sigma.viz.chroms.ChromScorer;

public class ExpressionChromScorer implements ChromScorer {
	
	private ArrayList<ExpressionProbe> probes;
	private Region chrom;
	private Double[] bins;
	private double max;
	private Character strand;
	
	public ExpressionChromScorer(Region c, Expander<Region,ExpressionProbe> prober, Character str) { 
		probes = new ArrayList<ExpressionProbe>();
		chrom = c;
		bins = null;
		max = 1.0;
		strand = str;
		
		Iterator<ExpressionProbe> itr = prober.execute(chrom);
		while(itr.hasNext()) { 
			ExpressionProbe p = itr.next();
			if(strand == null || p.getStrand() == strand) { 
				probes.add(p);
			}
		}
	}
	
	public Character strand() { return strand; }
	
	public void filterAll(Collection<Collection<Region>> regarray) { 
		Set<Region> total = new HashSet<Region>();
		for(Collection<Region> regs : regarray) { 
			total.addAll(regs);
		}
		filter(total);
	}
	
	public void filter(Collection<Region> regs) { 
		OverlappingRegionFinder finder = new OverlappingRegionFinder(regs);
		Iterator<ExpressionProbe> itr = probes.iterator();
		while(itr.hasNext()) { 
			ExpressionProbe p = itr.next();
			if(finder.findOverlapping(p).isEmpty()) { 
				itr.remove();
			}
		}
	}

	public int length() {
		return chrom.getWidth();
	}

	public Double max() {
		return max;
	}
	
	public Double zero() { return null; }

	public void recalculate(int units) {
		if(bins == null || bins.length != units) { 
			bins = new Double[units];
			int[] counts = new int[units];
			max = 0.0;
			int len = length();
			
			for(ExpressionProbe p : probes) { 
				double f = (double)p.getLocation() / (double)len;
				int u = Math.min(units-1, (int)Math.round(f * (double)units));
				double value = p.meanlog();
				
				if(!Double.isNaN(value) && !Double.isInfinite(value)) { 
					counts[u] += 1;
					bins[u] = (counts[u] > 1 ? bins[u] + value : value);
				}
			}
			
			int total = 0;
			for(int i = 0; i < units; i++) { 
				if(counts[i] > 0) { 
					bins[i] /= (double)counts[i];
					max = Math.max(bins[i], max);
					total += counts[i];
				} else { 
					bins[i] = null;
				}
			}
			
			System.out.println(String.format("ExpressionChromScorer: binned=%d max=%.3f", total, max));
		}
	}

	public int size() {
		return bins != null ? bins.length : 0;
	}

	public Double value(int unit) {
		return bins != null ? bins[unit] : null;
	}

}
