/*
 * Author: tdanford
 * Date: Oct 29, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.fitters;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;

public interface SegmentFitter {
	
	public int type();  
	public int numParams();
	
	public Double[] fit(int j1, int j2, InputData data, Integer[] channels);
	public Double score(int j1, int j2, Double[] params, InputData data, Integer[] channels);
}

