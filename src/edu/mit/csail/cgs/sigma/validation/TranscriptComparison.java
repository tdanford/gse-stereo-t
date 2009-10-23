/*
 * Author: tdanford
 * Date: May 6, 2009
 */
package edu.mit.csail.cgs.sigma.validation;

import java.util.Collection;

import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;

public interface TranscriptComparison {

	public Double compare(
			Collection<TranscriptCall> correct,
			Collection<TranscriptCall> learned);
}
