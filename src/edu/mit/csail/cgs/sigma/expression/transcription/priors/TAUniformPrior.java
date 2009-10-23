/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription.priors;

import java.util.*;

import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;

/**
 * By default, for testing purposes, this is a "uniform," un-normalized prior which 
 * weights every TranscriptArrangement object equally.  
 * 
 * @author tdanford
 */
public class TAUniformPrior implements TAPrior {
	
	public TAUniformPrior() { 
	}

	public Double prior(TranscriptArrangement arr) {
		return 1.0;
	}
}
