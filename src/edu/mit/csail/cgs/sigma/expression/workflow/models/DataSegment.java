/*
 * Author: tdanford
 * Date: Feb 27, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.models;

import java.util.*;

import edu.mit.csail.cgs.cgstools.slicer.StudentsTTest;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential.DifferentialSpec;

public class DataSegment extends RegionKey {
	
	public Integer channels;
	public Integer[] segmentTypes; 
	public Double[][] segmentParameters;
	public Boolean[] segmentSharing;
	
	public Integer[] dataLocations;
	public Double[][] dataValues;
	
	public Double[] probeAlphas;
	public Double[] differential;
	
	public DataSegment() {}
	
	public DataSegment(InputSegmentation segs, int break1, int break2) {
		super(segs.input.chrom(), 
				segs.input.locations[break1],  
				segs.input.locations[break2],
				segs.input.strand());

		FileInputData data = segs.input;
		
		channels = data.channels();
		segmentTypes = new Integer[channels];
		segmentParameters = new Double[channels][];
		segmentSharing = new Boolean[channels];
		
		dataLocations = subsetLocs(data.locations(), break1, break2);
		dataValues = subsetValues(data.values(), break1, break2);
		
		for(Segment s : segs.segments) { 
			if(s.start.equals(break1) && s.end.equals(break2)) { 
				segmentTypes[s.channel] = s.segmentType;
				segmentParameters[s.channel] = s.params.clone();
				segmentSharing[s.channel] = s.shared;
			}
		}
		
		probeAlphas = null;
		differential = null;
	}
	
	public boolean isContinuous(DataSegment s, Integer... channels) { 
		if(!chrom.equals(s.chrom)) { return false; }
		if(!strand.equals(s.strand)) { return false; }
		
		if(s.lastLocation() < firstLocation()) {  
			return s.isContinuous(this, channels);
			
		} else { 
			/*
			int maxK = 5;
			Double[] residuals = new Double[Math.min(maxK, s.dataLocations.length)];
			for(int i = 0; i < residuals.length; i++) { 
				residuals[i] = predicted(channels, s.dataLocations[i]) - 
						s.meanValue(i, channels);
			}
			Double t = StudentsTTest.calculateT(residuals, 0.0);
			StudentsTTest tester = new StudentsTTest(residuals.length-1);
			Double pvalue = tester.pvalue(t);
			return pvalue > 0.1 && pvalue < 0.9;
			*/
			
			Double p1 = predicted(channels, s.start);
			Double p2 = s.predicted(channels, s.start);
			return Math.abs(p1-p2) <= 0.5;
		}
	}
	
	public Double meanValue(int i, Integer[] chs) {
		double sum = 0.0;
		int c = 0;
		for(Integer ch : chs) { 
			Double v = dataValues[ch][i];
			if(v != null) { 
				sum += v; c += 1;
			}
		}
		return sum / (double)Math.max(1, c);
	}
	
	public boolean hasFivePrimeDip(int ch) { 
		if(dataLocations.length <= 2) { return false; }
		if(segmentTypes[ch].equals(Segment.LINE) || dataLocations.length < 4) { 
			
			if(strand.equals("+")) { 
				double e0 = dataValues[ch][0] - predicted(ch, dataLocations[0]); 
				double e1 = dataValues[ch][1] - predicted(ch, dataLocations[1]);

				for(int i = 2; i < dataLocations.length; i++) { 
					double e = dataValues[ch][i] - predicted(ch, dataLocations[i]);
					if(e < e0 || e < e1) { 
						return false;
					}
				}

			} else {
				int i0 = dataLocations.length-1;
				int i1 = i0 - 1;
				double e0 = dataValues[ch][i0] - predicted(ch, dataLocations[i0]); 
				double e1 = dataValues[ch][i1] - predicted(ch, dataLocations[i1]);

				for(int i = 0; i < i1; i++) { 
					double e = dataValues[ch][i] - predicted(ch, dataLocations[i]);
					if(e < e0 || e < e1) { 
						return false;
					}
				}
			}
			
			return true;
		} else { 
			return false;
		}
	}
	
	public double meanDifference(int ch1, int ch2) {
		double sum = 0.0;
		for(int i = 0; i < dataLocations.length ;i++) { 
			double diff = dataValues[ch1][i] - dataValues[ch2][i];
			sum += diff;
		}
		return sum / (double)dataLocations.length;
	}
	
	public Double predicted(Integer[] channels, int location) {
		if(channels.length == 1) { return predicted(channels[0], location); }
		double sum = 0.0;
		int c = 0;
		for(Integer ch : channels) { 
			sum += predicted(ch, location);
			c += 1;
		}
		return sum / (double)Math.max(1, c);
	}
	
	public Double predicted(int channel, int location) { 
		if(segmentTypes[channel].equals(Segment.FLAT)) { 
			return segmentParameters[channel][0];
		} else { 
			double expr = segmentParameters[channel][0], 
				slope = segmentParameters[channel][1];
			int diff = location-start;
			return expr + slope * diff;
		}
	}
	
	public Segment getSegment(int channel) {
		Double[] params = segmentParameters[channel];
		return new Segment(
				channel, segmentSharing[channel], segmentTypes[channel],
				dataLocations[0], dataLocations[dataLocations.length-1], 
				params);
	}
	
	public boolean isMissingChannel() { 
		for(int i = 0; i < segmentTypes.length; i++) { 
			if(segmentTypes[i] == null) { 
				return true;
			}
		} 
		return false;
	}
	
	public static Integer[] subsetLocs(Integer[] ls, int b1, int b2) { 
		Integer[] nls = new Integer[b2-b1];
		for(int i = b1; i < b2; i++) { 
			nls[i-b1] = ls[i];
		}
		return nls;
	}

	public static Double[][] subsetValues(Double[][] ls, int b1, int b2) { 
		Double[][] nls = new Double[ls.length][b2-b1];
		for(int i = b1; i < b2; i++) { 
			for(int j = 0; j < ls.length; j++) { 
				nls[j][i-b1] = ls[j][i];
			}
		}
		return nls;
	}

	public boolean hasType(Integer type, Integer... channels) {
		if(channels == null || channels.length == 0) { 
			for(int i = 0; i < segmentTypes.length; i++) { 
				if(segmentTypes[i].equals(type)) { 
					return true;
				}
			}			
			return false;
		} else { 
			for(int i = 0; i < channels.length; i++) { 
				if(segmentTypes[channels[i]].equals(type)) { 
					return true;
				}
			}
			return false;
		}
	}

	public Integer firstLocation() {
		return dataLocations[0];
	}
	
	public Integer lastLocation() { 
		return dataLocations[dataLocations.length-1];
	}

	public boolean hasConsistentType(Integer type, Integer[] channels) {
		if(channels == null) { return hasConsistentType(type); }
		for(int i = 0; i < channels.length; i++) { 
			if(!segmentTypes[channels[i]].equals(type)) { 
				return false;
			}
		}
		return true;
	}

	public boolean hasConsistentType(Integer type) {
		for(int i = 0; i < segmentTypes.length; i++) { 
			if(!segmentTypes[i].equals(type)) { 
				return false;
			}
		}
		return true;
	}

	/**
	 * The 'differential' array contains samples from a probabilistic model over this DataSegment -- 
	 * each sample is the difference between the average of one or more foreground channels, and one 
	 * or more background channels.  (Needed: generalize this, so that we can store samples from 
	 * multiple channel-set-pairs simultaneously.)
	 * 
	 * isDifferential() right now ignores its one argument, 'key', which is meant to correspond to an 
	 * experiment ('matalpha', etc.)
	 * 
	 * @param key
	 * @return
	 */
	public boolean isDifferential(String key) { 
		return isDifferential(key, 0.01);
	}
	
	public boolean isAnyDifferential() { 
		return isAnyDifferential(0.01);
	}
	
	public boolean isDifferential(String key, double confidenceThreshold) {
		if(confidenceThreshold < 0.0 || confidenceThreshold > 1.0) { 
			throw new IllegalArgumentException(String.format("%.4f", confidenceThreshold));
		}
		double f = getDifferentialConfidence(key, 0.0);
		return f <= confidenceThreshold || f >= (1.0 - confidenceThreshold);
	}
	
	public boolean isAnyDifferential(double confidenceThreshold) { 
		double f = getDifferentialConfidence(null, 0.0);
		return f <= confidenceThreshold || f >= (1.0 - confidenceThreshold);
	}
	
	public double getDifferentialConfidence(String key, double levelThreshold) {
		int c = 0;
		for(int i = 0; differential != null && i < differential.length; i++) {
			if(levelThreshold >= 0.0) { 
				c += (differential[i] >= levelThreshold) ? 1 : 0;
			} else { 
				c += (differential[i] <= levelThreshold) ? 1 : 0;
			}
		}
		double f = (double)c / (double)(differential != null ? differential.length : 1);
		return f;
	}
	
	public double getExpectedDifferential(String key) {
		double c = 0;
		for(int i = 0; differential != null && i < differential.length; i++) { 
			c += differential[i];
		}
		c /= differential != null ? (double)Math.max(1, differential.length) : 1.0;
		return c;
	}
}
