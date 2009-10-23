/*
 * Author: tdanford
 * Date: May 26, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

import java.util.Iterator;

import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;

public interface BackgroundEstimation<M extends SignalModel> {
	public M estimateModel(Iterator<ProbeLine> ps, int channel); 
}
