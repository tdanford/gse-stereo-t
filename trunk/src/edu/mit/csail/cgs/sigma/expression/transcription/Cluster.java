package edu.mit.csail.cgs.sigma.expression.transcription;

import java.io.PrintStream;
import java.util.*;

import Jama.Matrix;

import edu.mit.csail.cgs.sigma.Printable;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.ewok.verbs.Expander;

/**
 * Cluster of (neighboring) segments.  
 * 
 * A contiguous sequence of TRANSCRIBED segments.  
 * (Segment.segmentType == LINE)
 * 
 * @author tdanford
 *
 */
public class Cluster extends Model {

	public DataSegment[] segments;
	public Integer[] channels;
	
	public Double[][] values;
	public Integer[] locations;
	public Integer[] segmentOffsets;  // offsets into the values/locations arrays.
	
	public String toString() { 
		return String.format("Cluster: (%s:%d-%d) %d segments, %d channels",
				segments[0].chrom, segments[0].start, 
				segments[segments.length-1].end, 
				segments.length, channels.length);
	}
	
	public Cluster() {}
	
	public Cluster(Collection<DataSegment> ss, Integer... cs) {
		segments = ss.toArray(new DataSegment[0]);
		if(cs != null) { 
			channels = cs.clone();
		} else { 
			// If the channels are not given (== null), then we 
			// take the channels to be the first N channels which are present 
			// in all DataSegments of the cluster.
			int numchs = -1;
			for(DataSegment s : ss) { 
				numchs = (numchs == -1) ? s.dataValues.length : Math.min(numchs, s.dataValues.length);
			}
			channels = new Integer[numchs];
			for(int i = 0; i < channels.length; i++) { channels[i] = i; }
		}
		buildArrays();
	}

	public Collection<Cluster> subClusters(int ch) { 
		LinkedList<Cluster> subs = new LinkedList<Cluster>();
		Integer[] dips = fivePrimeDipSegments(ch).toArray(new Integer[0]);
		Arrays.sort(dips);
		if(dips.length == 0) { 
			subs.add(this); 
		} else { 
			int start = 0;
			for(int i = 0; i < dips.length; i++) { 
				if(dips[i] > 0) { 
					subs.add(subCluster(start, dips[i]));
				}
				start = dips[i];
			}
			subs.add(subCluster(start, segments.length));
		}

		return subs;
	}

	public Cluster subCluster(int s1, int s2) {
		if(s1 >= s2) { throw new IllegalArgumentException(String.format("%d,%d", s1, s2)); }
		LinkedList<DataSegment> segs = new LinkedList<DataSegment>();
		for(int i = s1; i < s2; i++) { segs.add(segments[i]); }
		return new Cluster(segs, channels);
	}
	
	public void update() { buildArrays(); }
	
	public String strand() { return segments[0].strand; }
	public String chrom() { return segments[0].chrom; }

	public Integer segmentStart(int i) {
		return segments[i].start;
	}
	public Integer segmentEnd(int i) {
		return segments[i].end;
	}

	public Integer start() {
		return segments[0].start;
	}
	
	public Integer end() {
		return segments[segments.length-1].end;
	}
	
	public Set<Integer> fivePrimeDipSegments(int ch) {
		double slopeThresh = 1.5;
		TreeSet<Integer> dips = new TreeSet<Integer>();
		for(int i = 0; i < segments.length; i++) { 
			if(segments[i].hasFivePrimeDip(ch)) {
				dips.add(i);
			} else { 
				double slope = segments[i].segmentParameters[ch][1];
				Double leftSlope = i > 0 ? segments[i-1].segmentParameters[ch][1] : slope;
				Double rightSlope = i < segments.length-1 ? segments[i+1].segmentParameters[ch][1] : slope;
				boolean sharpLeft = Math.abs(slope) > slopeThresh * Math.abs(leftSlope);
				boolean sharpRight= Math.abs(slope) > slopeThresh * Math.abs(rightSlope);
				if(sharpLeft || sharpRight) { 
					dips.add(i);
				}
			}
		}
		return dips;
	}
	
	/**
	 * Pulls out the "locations" (probe coordinates) from the segments
	 * of this method.  
	 * 
	 * The channel parameter is given, for completeness, but ignored
	 * in this implementation (since the probe locations don't vary
	 * between channels).  
	 * 
	 * This is provided as a utility function for downstream analysis 
	 * methods (such as transcript analysis methods).  
	 * 
	 * @param channel
	 * @return
	 */
	public Integer[] channelLocations() { 
		int n = 0;
		for(int i = 0; i < segments.length; i++) { 
			n += segments[i].dataLocations.length;
		}
		
		Integer[] vs = new Integer[n];
		for(int i = 0, k = 0; i < segments.length; i++) { 
			for(int j = 0; j < segments[i].dataLocations.length; j++) { 
				vs[k++] = segments[i].dataLocations[j];
			}
		}
		
		return vs;		
	}

	public int[] channeliLocations() { 
		int n = 0;
		for(int i = 0; i < segments.length; i++) { 
			n += segments[i].dataLocations.length;
		}
		
		int[] vs = new int[n];
		for(int i = 0, k = 0; i < segments.length; i++) { 
			for(int j = 0; j < segments[i].dataLocations.length; j++) { 
				vs[k++] = segments[i].dataLocations[j];
			}
		}
		
		return vs;		
	}

	/**
	 * Pulls out the "values" (probe intensities) wihtin a single channel
	 * of this cluster (not necessarily one of the channels that the cluster
	 * is across).
	 * 
	 * This is provided as a utility function for downstream analysis 
	 * methods (such as transcript analysis methods).  
	 * 
	 * @param channel
	 * @return
	 */
	public Double[] channelValues(int channel) {
		int n = 0;
		for(int i = 0; i < segments.length; i++) { 
			n += segments[i].dataLocations.length;
		}
		
		Double[] vs = new Double[n];
		for(int i = 0, k = 0; i < segments.length; i++) { 
			for(int j = 0; j < segments[i].dataLocations.length; j++) { 
				vs[k++] = segments[i].dataValues[channel][j];
			}
		}
		
		return vs;
	}
	
	public int channelSegmentOffset(int s) {
		int offset = 0;
		for(int i = 0; i < s && i < segments.length; i++) { 
			offset += segments[i].dataLocations.length;
		}
		return offset;
	}

	public double[] channeldValues(int channel) {
		int n = 0;
		for(int i = 0; i < segments.length; i++) { 
			n += segments[i].dataLocations.length;
		}
		
		double[] vs = new double[n];
		for(int i = 0, k = 0; i < segments.length; i++) { 
			for(int j = 0; j < segments[i].dataLocations.length; j++) { 
				vs[k++] = segments[i].dataValues[channel][j];
			}
		}
		
		return vs;
	}

	private void buildArrays() {
		int len = getNumDatapoints();
		segmentOffsets = new Integer[segments.length];
		values = new Double[channels.length][len];
		locations = new Integer[len];

		int k = 0;
		for(int i = 0; i < segments.length; i++){
			
			segmentOffsets[i] = k;
			
			for(int j = 0; j < segments[i].dataLocations.length; j++) {
				locations[k] = segments[i].dataLocations[j];
				for(int c = 0; c < channels.length; c++) { 
					values[c][k] = segments[i].dataValues[c][j];
				}
				k++;
			}
		}
	}

	private int getNumDatapoints() { 
		int num = 0;
		for(int i = 0; i < segments.length; i++){
			for(int j = 0; j < segments[i].dataLocations.length; j++) { 
				for(Integer ch : channels) {
					if(segments[i].dataValues[ch][j] != null) { 
						num += 1;
					}
				}
			}
		}
		return num;
	}

	public Integer findIndexSegment(int i) {
		int k = 0; 
		while(k < segmentOffsets.length && segmentOffsets[k] < i) { 
			k++;
		}
		return k;
	}
	
	/**
	 * 
	 * @return the breakpoints of this Cluster
	 */
	public Integer[] breakpoints() { 
		Integer[] bps = new Integer[segments.length+1];
		for(int i = 0; i < segments.length; i++) { 
			bps[i] = segments[i].start;
		}
		bps[bps.length-1] = segments[segments.length-1].end-1;
		return bps;
	}

	public static class SubClusterExpander implements Expander<Cluster,Cluster> {
		private int channel;
		public SubClusterExpander(int ch) { channel = ch; }
		public Iterator<Cluster> execute(Cluster c) { 
			return c.subClusters(channel).iterator(); 
		}
	}
}
