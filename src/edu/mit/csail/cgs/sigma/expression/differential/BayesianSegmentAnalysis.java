/*
 * Author: tdanford
 * Date: Mar 19, 2009
 */
package edu.mit.csail.cgs.sigma.expression.differential;

import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;

public class BayesianSegmentAnalysis {

	public DataSegment[] segments;
	public TotalModel total;
	
	public BayesianSegmentAnalysis(DataSegment[] s) { 
		segments = s.clone();
		total = new TotalModel(segments, segments[0].channels);
	}
	
	public TotalModel sample() {
		
		
		return total;
	}
}
