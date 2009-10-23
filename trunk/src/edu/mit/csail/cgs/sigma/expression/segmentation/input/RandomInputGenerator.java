/*
 * Author: tdanford
 * Date: Oct 27, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.input;

import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.utils.probability.NormalDistribution;

public interface RandomInputGenerator extends InputGenerator {

	/**
	 * Call generate() in order to have this random input generator create 
	 * a new input set.  This method must be called *before* any call to 
	 * locations, data, or segments methods below.
	 */
	public void generate();
	
	/**
	 * The "right" answers.
	 * @return The set of segments which were used by the generator to create
	 * the underlying data.  
	 */
	public Collection<Segment> segments();
		
	/**
	 * The "right" answers for a particular channel.
	 * @param channel a channel index.  
	 * @return The set of segments which were used by the generator to create
	 * the underlying data in the given channel.
	 */
	public Collection<Segment> segments(int channel);
}