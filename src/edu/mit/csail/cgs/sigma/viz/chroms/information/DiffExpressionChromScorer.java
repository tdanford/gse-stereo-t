/*
 * Author: tdanford
 * Date: Oct 8, 2008
 */
package edu.mit.csail.cgs.sigma.viz.chroms.information;

import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.expression.models.*;
import edu.mit.csail.cgs.sigma.viz.chroms.ChromScorer;

/**
 * Scores the chromosome by taking the mean difference-of-logs across the given 
 * region.  This should correspond, roughly, with our differential expression values.   
 * 
 * @author tdanford
 */
public class DiffExpressionChromScorer implements ChromScorer {
	
	private ArrayList<ExpressionTwoChannelProbe> probes;
	private Region chrom;
	private Double[] bins;
	private double min, max;
	private Character strand;

	public DiffExpressionChromScorer(Region c, 
			Expander<Region,ExpressionTwoChannelProbe> prober) { 
		this(c, prober, null);
	}

	public DiffExpressionChromScorer(Region c, 
			Expander<Region,ExpressionTwoChannelProbe> prober, 
			Character str) { 

		probes = new ArrayList<ExpressionTwoChannelProbe>();
		chrom = c;
		bins = null;
		max = 0.1;
		min = -0.1;
		strand = str;

		Iterator<ExpressionTwoChannelProbe> itr = prober.execute(chrom);
		while(itr.hasNext()) { 
			ExpressionTwoChannelProbe p = itr.next();
			if(strand == null || p.getStrand() == strand) { 
				probes.add(p);
			}
		}
	}

	public Character strand() { return strand; }
	
	public void filter(Collection<Region> regs) { 
		OverlappingRegionFinder finder = new OverlappingRegionFinder(regs);
		Iterator<ExpressionTwoChannelProbe> itr = probes.iterator();
		while(itr.hasNext()) { 
			ExpressionTwoChannelProbe p = itr.next();
			Collection<Region> overlaps = finder.findOverlapping(p);
			boolean hasOverlap = hasOverlapInStrand(p, overlaps);
			if(!hasOverlap) { 
				itr.remove();
			}
		}
	}
	
	private boolean hasOverlapInStrand(ExpressionTwoChannelProbe sp, Collection<Region> regs) {
		if(strand == null) { 
			return !regs.isEmpty();
		}
		
		for(Region r : regs) { 
			if(r instanceof StrandedRegion) { 
				StrandedRegion sr = (StrandedRegion)r;
				if(sr.getStrand() == strand) { 
					return true;
				}
			} else { 
				return true;
			}
		}
		
		return false;
	}

	public void filterAll(Collection<Collection<Region>> regarray) { 
		Set<Region> total = new HashSet<Region>();
		for(Collection<Region> regs : regarray) { 
			total.addAll(regs);
		}
		filter(total);
	}

	public int length() {
		return chrom.getWidth();
	}

	public Double max() {
		return max-min;
	}
	
	public Double zero() { 
		if(min < 0.0) { 
			return -min; 
		} else { 
			return null;
		}
	}
	
	private boolean isValid(Double v) { 
		return v != null && !Double.isInfinite(v) && !Double.isNaN(v);
	}

	public void recalculate(int units) {
		if(bins == null || bins.length != units) { 
			bins = new Double[units];
			int[] counts = new int[units];
			max = 0.0;
			min = 0.0;
			int len = length();
			
			for(ExpressionTwoChannelProbe p : probes) { 
				double f = (double)p.getLocation() / (double)len;
				int u = Math.min(units-1, (int)Math.round(f * (double)units));
	
				Double v1 = p.meanlog(true);
				Double v2 = p.meanlog(false);

				if(isValid(v1) && isValid(v2)) { 
					double diff = v1-v2;
					counts[u] += 1;
					bins[u] = (counts[u] > 1 ? bins[u] + diff : diff);
				}
			}
			
			int total = 0;
			for(int i = 0; i < units; i++) { 
				if(counts[i] > 0) { 
					bins[i] /= (double)counts[i];
					max = Math.max(bins[i], max);
					min = Math.min(bins[i], min);
					total += counts[i];
				} else { 
					bins[i] = null;
				}
			}
			
			System.out.println(String.format("DiffExpressionChromScorer: binned=%d max=%.3f", total, max));
		}
	}

	public int size() {
		return bins != null ? bins.length : 0;
	}

	public Double value(int unit) {
		Double val = bins != null ? bins[unit] : null;
		return val != null ? val-min : null;
	}
}
