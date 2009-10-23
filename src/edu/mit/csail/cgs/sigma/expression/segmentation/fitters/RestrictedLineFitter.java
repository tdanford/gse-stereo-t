/*
 * Author: tdanford
 * Date: May 1, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.fitters;

import java.util.*;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;

/**
 * Full probabilistic treatment of segment fitting.
 *   
 * @author tdanford
 *
 */
public class RestrictedLineFitter implements SegmentFitter {
	
	public RestrictedLineFitter() {
	}

	public Double[] fit(int j1, int j2, InputData data, Integer[] channels) {
		return null;
	}

	public int numParams() {
		return 4;
	}

	public Double score(int j1, int j2, Double[] params, InputData data,
			Integer[] channels) {
		return null;
	}

	public int type() {
		return Segment.LINE;
	}
}
