/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription.fitters;

import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.Endpts;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.Likelihoods;

public interface EndptsFitter {
	public EndptsFit fitTranscripts(Endpts[] epts, Double bestScore);
}


