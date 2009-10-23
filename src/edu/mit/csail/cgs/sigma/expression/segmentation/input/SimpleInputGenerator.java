/*
 * Author: tdanford
 * Date: Nov 19, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.RegressionInputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;

public class SimpleInputGenerator implements RandomInputGenerator {
	
	private RandomSegmentGenerator segmentGenerator;
	private Tiler tiler;
	
	private Integer numProbes, channels, length;

	public String chrom, strand;
	public Integer[] locs;
	public Double[][] values;
	public Segment[][] segments;
	
	public Double bgScale, bgMean, noiseScale;

	private Random rand;
	
	public SimpleInputGenerator(RandomSegmentGenerator sgen, int len, int chan, String chr, String str) {
		//tiler = new RandomTiler();
		tiler = new RegularTiler();
		
		segmentGenerator = sgen;
		chrom = chr; strand = str;
		numProbes = len; 
		channels = chan;
		length = len;
		
		rand = new Random();
		bgScale = 2.0; bgMean = 1.0;
		noiseScale = 0.75;
	}
	
	private double sampleBackground() { 
		return Math.abs((rand.nextGaussian() * bgScale) + bgMean);
	}

	private double sampleNoise() { 
		return (rand.nextGaussian() * noiseScale);
	}
	
	public Collection<Segment> segments(int channel) { 
		ArrayList<Segment> segs = new ArrayList<Segment>();
		for(int i = 0; i < segments[channel].length; i++) {
			segs.add(segments[channel][i]);
		}
		return segs;		
	}
	
	public Collection<Segment> segments() {
		return segments(0);
	}
	
	public void generate(String c, int start, int end, String str) { 
		generate();
	}
	
	public void generate() {
		
		locs = tiler.tile(1, length*2, length);
		values = new Double[channels][length];
		
		for(int k = 0; k < channels; k++) { 
			for(int i = 0; i < length; i++) { 
				values[k][i] = sampleBackground();
			}
		}
		
		segments = segmentGenerator.generateSegments(channels, length);
		
		for(int k = 0; k < channels; k++) { 
			for(int i = 0; i < segments[k].length; i++) {
				Segment s = segments[k][i];

				for(int j = s.start; j <= s.end; j++) { 
					if(s.segmentType == Segment.FLAT) { 
						values[k][j] += s.params[0];
					} else if (s.segmentType == Segment.LINE) {
						int offset = locs[s.end] - locs[j];
						double mean = s.params[0] + s.params[1] * offset;
						values[k][j] += mean;
					}
				}
			}
		}
		
		for(int k = 0; k < channels; k++) { 
			for(int i = 0; i < length; i++) {
				values[k][i] = Math.max(0.1, values[k][i] + sampleNoise());
			}
		}
	}
	
	public InputData inputData() { return new RegressionInputData(chrom, strand, locs, values); }
}