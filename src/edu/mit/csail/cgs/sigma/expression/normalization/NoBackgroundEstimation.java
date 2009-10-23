/*
 * Author: tdanford
 * Date: May 26, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

import java.util.Iterator;

import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;

public class NoBackgroundEstimation implements BackgroundEstimation {
	public SignalModel estimateModel(Iterator ps, int channel) {
		//return new IdentityModel();
		return new ZeroModel();
	}
}
