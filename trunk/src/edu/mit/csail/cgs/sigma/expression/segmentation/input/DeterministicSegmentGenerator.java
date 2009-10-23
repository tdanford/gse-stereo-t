/*
 * Author: tdanford
 * Date: Jan 7, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.input;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;

public class DeterministicSegmentGenerator implements RandomSegmentGenerator {
	
	private Integer[] offsets, lengths;
	private Double[] intensities;
	
	public DeterministicSegmentGenerator(Integer... args) { 
		if(args.length % 3 != 0) { throw new IllegalArgumentException(); }
		offsets = new Integer[args.length/3];
		lengths = new Integer[args.length/3];
		intensities = new Double[args.length/3];
		for(int i = 0; i < args.length; i+=3) { 
			int j = i / 2;
			offsets[j] = args[i];
			lengths[j] = args[i+1];
			intensities[j] = (double)args[i+2];
		}
	}

	public Segment[][] generateSegments(int channels, int length) {
		Segment[][] array = new Segment[channels][offsets.length];
		for(int i = 0; i < channels; i++) { 
			for(int j = 0; j < offsets.length; j++) { 
				Double[] params = new Double[3];
				params[0] = 1.0 + intensities[j];
				double slope = -(5.0 / (double)lengths[j])/3.0;
				params[1] = slope;
				params[2] = 1.0; // noiseVariance
				
				array[i][j] = new Segment(
						i, true, Segment.LINE, offsets[j], offsets[j]+lengths[j]-1,
						params);
			}
		}
		return array;
	}

}
