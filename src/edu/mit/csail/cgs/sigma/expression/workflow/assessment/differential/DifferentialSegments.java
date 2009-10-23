/*
 * Author: tdanford
 * Date: Jan 22, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.utils.models.Model;

/**
 * Identifies segments which are "differentially expressed" between two channels, 
 * in a given segmentation.
 * 
 * @author tdanford
 */
public class DifferentialSegments {

	public static void main(String[] args) { 

		WorkflowProperties props = new WorkflowProperties();
		File inputFile = args.length > 0 ? new File(args[0]) : 
			new File(props.getDirectory(), "s288c_mata_1_plus.segments");

		try {
			DifferentialSegments dsegs = new DifferentialSegments(inputFile);
		
			Collection<DifferentialKey> keys = dsegs.differentialRegions(0, 1);
			System.out.println(DifferentialKey.header());
			for(DifferentialKey key : keys) { 
				System.out.println(key.toString());
			}
			
			System.out.println(String.format("# Regions: " + keys.size()));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<InputSegmentation> segmentations;
	
	/**
	 * The constructor reads in a segmentation file.  You can then ask (using the 
	 * differentialRegions() method) for the segments that are differentially expressed
	 * between any two channels in this dataset.
	 * 
	 * @param f
	 * @throws IOException
	 */
	public DifferentialSegments(File f) throws IOException { 
		
		segmentations = new ArrayList<InputSegmentation>();
		
		WorkflowSegmentationReader reader = new WorkflowSegmentationReader(f);
		while(reader.hasNext()) { 
			segmentations.add(reader.next());
		}
		
		// If you use any of the Workflow*Reader objects, please be sure
		// to close them when you're done.
		reader.close();  
	}
	
	/**
	 * Since the segmentations are disjoint, the total set of differentially expressed 
	 * segments is the union of the sets of differentially expressed segments in each 
	 * segmentation. 
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public SortedSet<DifferentialKey> differentialRegions(int c1, int c2) { 
		SortedSet<DifferentialKey> keys = new TreeSet<DifferentialKey>();
		for(InputSegmentation iseg : segmentations) { 
			keys.addAll(differentialRegions(iseg, c1, c2));
		}
		return keys;
	}
	
	/**
	 * Collects the differentially expressed segments (segment pairs with the same start and 
	 * end, but one member of the pair has the LINE type while the other does not) from a 
	 * given segmentation.  
	 * 
	 * @param seg
	 * @param c1
	 * @param c2
	 * @return
	 */
	private Collection<DifferentialKey> differentialRegions(InputSegmentation seg, int c1, int c2) { 
		ArrayList<DifferentialKey> keys = new ArrayList<DifferentialKey>();
		
		// Get the ground-truth data, so we can assemble the region identifiers when 
		// we've found the differentially expressed segments. 
		InputData data = seg.input;
		Integer[] locations = data.locations();
		String strand = data.strand();
		String chrom = data.chrom();

		// Most of the setup of this method is filtering and sorting the Segment objects, 
		// so that we *only* consider the segments from the two given channels (c1, and c2)
		// and so that we consider them in order, so that we're not comparing segments 
		// different parts of the input chunk.  
		SortedSet<Segment> c1Segs = new TreeSet<Segment>();
		SortedSet<Segment> c2Segs = new TreeSet<Segment>();
		
		for(Segment s : seg.segments) { 
			if(s.channel == c1) { 
				c1Segs.add(s);
			} else if (s.channel == c2) { 
				c2Segs.add(s);
			}
		}

		// These should now be in sorted order, given that we built them using the 
		// SortedSet objects.
		Segment[] c1array = c1Segs.toArray(new Segment[0]);
		Segment[] c2array = c2Segs.toArray(new Segment[0]);
		
		// sanity check. 
		if(c1array.length != c2array.length) { 
			throw new IllegalArgumentException(); 
		}
		
		for(int i = 0; i < c1array.length; i++) { 
			Segment s1 = c1array[i], s2 = c2array[i];
			
			// sanity check.  
			if(!s1.start.equals(s2.start) || !s1.end.equals(s2.end)) { 
				throw new IllegalArgumentException(String.format("%d,%d doesn't match %d,%d",
						s1.start, s1.end, s2.start, s2.end));
			}
			
			// There are three conditions here: 
			// (1) Neither of the segments is 'shared' 
			// (2) The segments don't have the same type
			// (3) At least one of them is a line.
			if(!s1.shared && !s2.shared) { 
				if(s1.segmentType.equals(Segment.LINE) ||
					s2.segmentType.equals(Segment.LINE)) {
					
					boolean differential = false;

					// Remember: for any segment 's', s.start and s.end are *indices* 
					// into the 'locations' array of the corresponding data chunk.  
					DifferentialKey key = new DifferentialKey(
							new RegionKey(chrom, 
									locations[s1.start], locations[s1.end], 
									strand), s1, s2);


					if(s1.segmentType.equals(Segment.FLAT)) { 
						differential = key.s2Expr() > key.s1Expr();
					} else if (s2.segmentType.equals(Segment.FLAT)) {
						differential = key.s1Expr() > key.s2Expr();
					} else { 
						differential = Math.abs(key.diffExpr()) >= 0.5;
					}

					if(differential) { 
						// If all conditions have been satisfied, then we build 
						// the region identifier and save it in the list to be returned.
						keys.add(key);
					}
				}
			}
		}
		
		return keys;
	}
}

class DifferentialKey extends Model implements Comparable<DifferentialKey> { 
	
	public RegionKey region;
	public Segment s1, s2;
	
	public DifferentialKey() {}
	
	public DifferentialKey(RegionKey key, Segment f1, Segment f2) { 
		this.region = key;
		s1 = f1; s2 = f2;
	}
	
	public Integer length() { return region.end - region.start + 1; }
	
	public Double s1Expr() { 
		if(s1.segmentType.equals(Segment.LINE)) { 
			return region.strand.equals("+") ? 
					s1.params[0] + s1.params[1] * length() : 
					s1.params[0];			
		} else { 
			return s1.params[0];
		}
	}
	
	public Double s2Expr() { 
		if(s2.segmentType.equals(Segment.LINE)) { 
			return region.strand.equals("+") ? 
					s2.params[0] + s2.params[1] * length() : 
					s2.params[0];			
		} else { 
			return s2.params[0];
		}
	}
	
	public Double diffExpr() { 
		double d1 = s1Expr();
		double d2 = s2Expr();
		return d1 - d2;
	}
	
	public static String header() { 
		return "DiffE\tFit\tChrom:Start-End\tStr\tUp\tDown";
	}
	
	public String toString() { 
		return String.format("%.2f\t%s:%s-%s\t%s\t%s\t%s",
				diffExpr(), region.chrom, region.start, region.end,
				region.strand, 
				s1.segmentType.equals(Segment.LINE) ? "L" : "F", 
				s2.segmentType.equals(Segment.LINE) ? "L" : "F");
	}

	public int compareTo(DifferentialKey k) { 
		Double d1 = diffExpr(), d2 = k.diffExpr();
		if(d1 < d2) { return 1; }
		if(d1 > d2) { return -1; }
		return region.compareTo(k.region);
	}	
}