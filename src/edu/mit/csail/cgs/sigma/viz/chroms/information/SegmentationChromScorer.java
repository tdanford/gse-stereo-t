/**
 * 
 */
package edu.mit.csail.cgs.sigma.viz.chroms.information;

import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.viz.chroms.ChromScorer;

/**
 * @author tdanford
 *
 */
public class SegmentationChromScorer implements ChromScorer {
	
	private int[] counts; // array of counts.
	private int maxCount; // the maximum count in the array
	private int bpWidth;  // width of a bucket, in base-pairs.
	
    // genomic information: 
	private Genome genome;
	private String chrom;
	private int chromLength;
	private char chromStrand;
	private int channel;

	// the segments on this genome.
	private OverlappingRegionFinder<DataSegmentRegion> finder;

	public SegmentationChromScorer(StrandedRegion chr, int ch, Iterator<DataSegment> segs) { 
		genome = chr.getGenome();
		chrom = chr.getChrom();
		chromLength = chr.getEnd();
		chromStrand = chr.getStrand();
		channel = ch;
		
		counts = null;
		maxCount = 1;
		bpWidth = 0;
		
		finder = new OverlappingRegionFinder<DataSegmentRegion>(
				new FilterIterator<DataSegment,DataSegmentRegion>(
						new DataSegmentRegionMapper(genome),
						segs));
		
		System.out.println(String.format("Loaded %d regions.", finder.size()));
	}
	
	public void recalculate(int units) {
		if(counts != null && units == counts.length) { return; }
		
		bpWidth = (int)Math.ceil((double)chromLength / (double)units);
		counts = new int[units];
		maxCount = 1;
		
		System.out.println(String.format("Recalculating (%d units)...", units));
		
		for(int start = 0, i = 0; start < chromLength && i < units; start += bpWidth, i++) { 
			int end = start + bpWidth - 1;
			Collection<DataSegmentRegion> regs = 
				finder.findOverlapping(new Region(genome, chrom, start, end));
			//System.out.println(String.format("\t#%d: %d regions", i, regs.size()));

			int c = 0;

			for(DataSegmentRegion r : regs) { 
				if(r.segment.segmentTypes[channel].equals(Segment.LINE)) { 
					
					int s = Math.max(start, r.getStart());
					int e = Math.min(end, r.getEnd());
					int w = e-s+1;
					c += w;
				}
			}
			
			counts[i] = c;
			//System.out.println(String.format("\tBin %d: %d", i, counts[i]));
			
			maxCount = Math.max(maxCount, counts[i]);
		}
		System.out.println(String.format("Max Count: %d", maxCount));
	}

	public Double max() {
		return 1.0;
	}

	public Character strand() {
		return chromStrand;
	}

	public Double value(int unit) {
		return (double)counts[unit] / (double)Math.max(1, bpWidth);
	}

	public Double zero() {
		return 0.0;
	}

	public int length() {
		return chromLength;
	}

	public int size() {
		return counts.length;
	}
	
	private class DataSegmentRegionMapper implements Mapper<DataSegment,DataSegmentRegion> { 
		private Genome genome;
		public DataSegmentRegionMapper(Genome g) { 
			genome = g;
		}
		
		public DataSegmentRegion execute(DataSegment s) { 
			return new DataSegmentRegion(genome, s);
		}
	}
	
	private class DataSegmentRegion extends Region { 
		public DataSegment segment;
		public DataSegmentRegion(Genome g, DataSegment s) { 
			super(g, s.chrom, s.start, s.end);
			segment = s;
		}
	}
}

