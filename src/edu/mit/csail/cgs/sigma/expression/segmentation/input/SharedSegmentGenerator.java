/*
 * Author: tdanford
 * Date: Nov 19, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.input;

import java.util.Random;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;

public class SharedSegmentGenerator implements RandomSegmentGenerator {
	
	private int count;
	private double probShared;
	private Double noiseVariance, levelVariance;
	private Double flatLevel, lineLevel, lineSlopeLevel;
	private Random rand;
	
	public SharedSegmentGenerator(int c, double pshared) { 
		count = c;
		probShared = pshared;
		rand = new Random();

		noiseVariance = 1.0;
		levelVariance = 1.0;
		flatLevel = 10.0;
		lineLevel = 10.0;
		lineSlopeLevel = -0.6;
	}

	public Segment[][] generateSegments(int channels, int length) {
		Segment[][] array = new Segment[channels][count];
		
		int spacing = length/count;

		for(int i = 0; i < count; i++) {
			
			int type = Segment.LINE;
			int start = spacing*i + rand.nextInt(spacing);
			int len = Math.max(5, rand.nextInt(spacing));
			int end = Math.min(start+len-1, length-1);
			boolean shared = rand.nextDouble() <= probShared;
			
			int shift = 10;
			
			Double[] params = generateParams(type, end-start+1);
			
			for(int k = 0; k < channels; k++) {
				int diff = rand.nextInt(shift);
				int kstart = shared ? start : start - diff;
				if(kstart < 0) { kstart = 0; }
				if(kstart >= end) { kstart=end-1; }
				
				Segment s = new Segment(k, shared, type, kstart, end, params);
				array[k][i] = s;
			}
		}

		return array;
	} 
	
	public Double[] generateParams(int type, int len) {
		Double[] params = null;
		if(type == Segment.FLAT) { 
			params = new Double[2];
			params[0] = rand.nextGaussian() * levelVariance + flatLevel;
			params[1] = noiseVariance;
			
		} else if (type == Segment.LINE) {
			params = new Double[3];
			params[0] = rand.nextGaussian() * levelVariance + lineLevel;
			double slope = -((params[0]-1.0) / (double)len) * 0.5;
			params[1] = slope;
			params[2] = noiseVariance;
			
		} else { 
			throw new IllegalArgumentException();
		}
		return params;
	}	
}