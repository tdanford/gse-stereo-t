package edu.mit.csail.cgs.sigma.expression.segmentation;

import java.util.Collection;

public interface Segmenter {
	
	/**
	 * Given a description of the data in a region, finds the set of segments which best fits that data.
	 * 
	 * @param locations An array of base-pair coordinates, for each probe in the channels.  
	 * 
	 * @param values An array of channel values -- values[0] is the array for channel 0, values[1] for channel 1, and so forth.
	 * values[i].length==locations.length is guaranteed to be true, for all i.
	 * 
	 * @return A collection of Segment objects, which represent the "calls" of this segmenter.
	 */
	public Collection<Segment> segment(InputData data);
}
