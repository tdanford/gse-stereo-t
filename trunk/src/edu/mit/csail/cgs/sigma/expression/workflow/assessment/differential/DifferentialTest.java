package edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential;

import edu.mit.csail.cgs.cgstools.slicer.StudentsTTest;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.models.Model;

/**
 * A data structure which holds a logical specification for when we call a 
 * String tag "differential" or not, over a specific region.
 *
 */
public abstract class DifferentialTest extends Model {
	
	public DifferentialTest() {}
	public abstract boolean isDifferent(DataSegment seg);
}

