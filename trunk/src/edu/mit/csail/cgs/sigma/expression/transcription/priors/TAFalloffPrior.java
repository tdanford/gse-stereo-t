/*
 * Author: tdanford
 * Date: Dec 20, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription.priors;

import java.util.*;

import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;

/**
 * @author tdanford
 */
public class TAFalloffPrior implements TAPrior {
	
	private Double alpha;
	
	public TAFalloffPrior(double a) {
		alpha = a;
		if(alpha >= 1.0) { throw new IllegalArgumentException(String.valueOf(a)); }
	}

	public Double prior(TranscriptArrangement arr) {
		int K = arr.calls.length;
		double p = 1.0;
		for(int i = 0; i < K-1; i++) { 
			p *= (1.0 - alpha);
		}
		p *= alpha;
		return p;
	}
}
