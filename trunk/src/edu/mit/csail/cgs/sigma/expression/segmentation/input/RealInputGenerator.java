/*
 * Author: tdanford
 * Date: Nov 13, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.input;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.expression.models.ExprProbeModel;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.RegressionInputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.utils.Predicate;
import edu.mit.csail.cgs.utils.models.data.DataFrame;

/**
 * Loads data from the probe files.  
 * 
 * @author tdanford
 */
public class RealInputGenerator implements RandomInputGenerator {

	private Genome genome;
	private String chrom, strand; 
	private Integer start, end;
	private Double[][] data;
	private Integer[] locations;
	private DataFrame<ExprProbeModel> allData;
	
	public RealInputGenerator(StrandedRegion r, File file) throws IOException { 
		allData = new DataFrame<ExprProbeModel>(ExprProbeModel.class, file);
		genome = r.getGenome();
		chrom= r.getChrom();
		strand = String.valueOf(r.getStrand());
		start = r.getStart();
		end = r.getEnd();
	}
	
	private static void removeNulls(Map<Integer,Double[]> map) { 
		Set<Integer> nulls = new TreeSet<Integer>();
		for(Integer key : map.keySet()) { 
			if(hasNull(map.get(key))) { 
				nulls.add(key);
			}
		}
		System.out.println(String.format("%d - %d = %d locations", map.size(), nulls.size(), (map.size()-nulls.size())));
		for(Integer key : nulls) { 
			map.remove(key);
		}
	}
	
	private static boolean hasNull(Double[] d) { 
		for(int i = 0; i < d.length; i++) { 
			if(d[i] == null) { return true; }
		}
		return false;
	}
	
	private class ExprProbeFilter implements Predicate<ExprProbeModel> { 
		private StrandedRegion region;
		
		public ExprProbeFilter(StrandedRegion r) { 
			region = r;
		}

		public boolean accepts(ExprProbeModel v) {
			return region.getChrom().equals(v.chrom) && 
				region.getStrand() == v.strand.charAt(0) && 
				region.getStart() <= v.location && region.getEnd() >= v.location;
		}
	}

	public void generate(RandomSegmentGenerator segGen) {
		generate();
	}
	
	public void generate(String c, int s, int e, String st) { 
		chrom = c;
		start = s; end = e;
		strand = st;
		
		char strd = strand.charAt(0);
		
		DataFrame<ExprProbeModel> dataFrame = 
			allData.filter(
					new ExprProbeFilter(
							new StrandedRegion(genome, chrom, start, end, strd)));
		
		locations = new Integer[dataFrame.size()];
		data = new Double[1][locations.length];
		
		for(int i = 0; i < dataFrame.size(); i++) { 
			ExprProbeModel m = dataFrame.object(i);
			int location = m.location;
			Double value = m.intensity;
			Double bgvalue = m.background;
			
			locations[i] = location;
			data[0][i] = value;
			//data[1][i] = bgvalue;
		}
	}

	public void generate() {
		generate(chrom, start, end, strand);
	}
	
	public InputData inputData() { 
		return new RegressionInputData(chrom, strand, locations, data);
	}

	public Collection<Segment> segments() {
		return new ArrayList<Segment>();
	}

	public Collection<Segment> segments(int channel) {
		return new ArrayList<Segment>();
	}
}
