/*
 * Author: tdanford
 * Date: May 16, 2009
 */
package edu.mit.csail.cgs.sigma.expression.simulation;

import java.util.Iterator;

import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;

public interface ProbeSimulator {

	public Iterator<ProbeLine> probes();

}