/*
 * Author: tdanford
 * Date: Nov 19, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.input;

import java.util.Random;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;

public class FixedSegmentGenerator implements RandomSegmentGenerator {
	
	private Integer[] starts, lengths;
	private Double noiseVariance, levelVariance;
	private Double flatLevel, lineLevel, lineSlopeLevel;
	private Random rand;
	
	/**
	 * constructor
	 * @param sls Contains the starts and lengths of the segments<br>
	 * It has to be an even sized array.<br>
	 * I.e. for <tt>K</tt> segments, <tt>sls</tt> will have to look like:<br>
	 * <tt> start_1, length_1, start_2, length_2, ..., start_K, length_K</tt> which
	 * is of length <tt>2K</tt>
	 */
	public FixedSegmentGenerator(Integer... sls) { 
		if(sls.length%2 != 0) { throw new IllegalArgumentException(); }
		rand = new Random();
		int c = sls.length/2;
		starts = new Integer[c]; lengths = new Integer[c];
		for(int i = 0; i < sls.length; i+=2) { 
			starts[i/2] = sls[i];
			lengths[i/2] = sls[i+1];
		}
		
		noiseVariance = 1.0;
		levelVariance = 1.0;
		flatLevel = 10.0;
		lineLevel = 10.0;
		lineSlopeLevel = 1.0;
	}

	public Segment[][] generateSegments(int channels, int length) {
		
		Segment[][] array = new Segment[channels][starts.length];
		
		for(int i = 0; i < starts.length; i++) {
			
			int type = Segment.FLAT;
			int start = starts[i];
			int end = start+lengths[i]-1;
			Double[] params = generateParams(type);
			
			for(int k = 0; k < channels; k++) { 
				Segment s = new Segment(k, true, type, start, end, params);
				array[k][i] = s;
				}
			}
		
		return array;
	} 
	
	public Double[] generateParams(int type) {
		Double[] params = null;
		if(type == Segment.FLAT) { 
			params = new Double[2];
			params[0] = rand.nextGaussian() * levelVariance + flatLevel;
			params[1] = noiseVariance;
			
		} else if (type == Segment.LINE) {
			params = new Double[3];
			params[0] = rand.nextGaussian() * levelVariance + lineLevel;
			params[1] = lineSlopeLevel;
			params[2] = noiseVariance;
			
		} else { 
			throw new IllegalArgumentException();
		}
		return params;
	}
}