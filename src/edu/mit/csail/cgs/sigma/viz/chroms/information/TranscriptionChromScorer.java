/*
 * Author: tdanford
 * Date: Feb 20, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms.information;

import java.util.*;

import edu.mit.csail.cgs.datasets.chippet.RunningOverlapSum;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.viz.chroms.ChromScorer;

public class TranscriptionChromScorer implements ChromScorer {
	
	private int length;
	private Genome genome;
	private String chrom;
	private Character strand;
	private Double[] fractions;
	private OverlappingRegionFinder<Region> overlappedRegions;
	
	public TranscriptionChromScorer(Genome g, String c, char str, Iterator<DataSegment> itr) {
		genome = g;
		chrom = c;
		length = g.getChromLength(chrom);
		RunningOverlapSum overlapSum = new RunningOverlapSum(g, chrom);
		strand = str;
		
		while(itr.hasNext()) { 
			DataSegment region = itr.next();
			overlapSum.addInterval(region.start, region.end);
		}
		
		overlappedRegions = new OverlappingRegionFinder<Region>(overlapSum.collectRegions(1));
		fractions = new Double[] { 0.0 };
	}

	public void recalculate(int units) {
		if(units < 1) { 
			throw new IllegalArgumentException(String.format("illegal unit: %d", units)); 
		}
		
		if(units != fractions.length) { 
			fractions = new Double[units];
			int width = (int)Math.floor((double)length / (double)units);
			
			for(int i = 0; i < units; i++) { 
				int start = i * width;
				int end = start + width - 1;
				Collection<Region> overs = 
					overlappedRegions.findOverlapping(new Region(genome, chrom, start, end));
				
				int overlapSum = 0;
				
				for(Region over : overs) { 
					int ostart = Math.max(start, over.getStart());
					int oend = Math.min(end, over.getEnd());
					int owidth = oend-ostart+1;
					
					overlapSum += owidth;
				}
				
				fractions[i] = (double)overlapSum / (double)width;
			}
		}
	}

	public Double max() {
		return 1.0;
	}

	public Character strand() {
		return strand;
	}

	public Double value(int unit) {
		if(unit < 0 || unit >= fractions.length) { 
			throw new IllegalArgumentException(String.format(
					"%d not in [0, %d)", unit, fractions.length));
		}
		return fractions[unit];
	}

	public Double zero() {
		return 0.0;
	}

	public int length() {
		return length;
	}

	public int size() {
		return fractions.length;
	}

}
