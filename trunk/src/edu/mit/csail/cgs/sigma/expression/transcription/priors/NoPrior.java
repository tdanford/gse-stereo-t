/*
 * Author: tdanford
 * Date: Jun 17, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.priors;

import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;

public class NoPrior implements TAPrior {

	public Double prior(TranscriptArrangement arr) {
		return 0.0;
	}

}
