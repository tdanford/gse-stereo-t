/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription.fitters;

import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;

public interface TAFitter {
	public TAFit fitTranscripts(TranscriptArrangement arr);
}
