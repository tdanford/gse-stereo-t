/*
 * Author: tdanford
 * Date: Nov 12, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.input;

import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;

public interface RandomSegmentGenerator {
	public Segment[][] generateSegments(int channels, int length);
}