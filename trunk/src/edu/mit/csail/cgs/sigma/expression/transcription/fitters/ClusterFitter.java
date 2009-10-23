/*
 * Author: tdanford
 * Date: Jun 26, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.fitters;

import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.Endpts;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.PrimitiveIterator;

public interface ClusterFitter {
	public EndptsFit fitCluster(Cluster c);
}

