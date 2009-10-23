/*
 * Author: tdanford
 * Date: Nov 16, 2008
 */
package edu.mit.csail.cgs.sigma.validation;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.Interval;
import edu.mit.csail.cgs.utils.NonOverlappingIntervalSet;

/**
 * SegmentComparison defines an interface for testing the accuracy of Segment <i>calls</i> against
 * <i>true</i> Segment annotations (either hand-annotations on real data, or artificially generated
 * Segments which were used to create the synthetic data).  
 * 
 * SegmentComparison provides a means of returning an arbitrary (real-valued) measure of accuracy 
 * for a particular set of Segment calls.  We can then optimize this measure over a grid of possible
 * Segmentation parameters.
 * 
 * @author tdanford
 *
 */
public interface SegmentComparison {
	public Double compare(Collection<Integer> correct, Collection<DataSegment> learned);
}

