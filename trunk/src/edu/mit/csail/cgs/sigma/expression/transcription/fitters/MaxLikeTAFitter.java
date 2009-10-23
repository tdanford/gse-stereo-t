/*
 * Author: tdanford
 * Date: Jun 17, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.fitters;

import java.util.*;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.*;
import edu.mit.csail.cgs.sigma.expression.transcription.priors.*;

public class MaxLikeTAFitter implements TAFitter {
	
	private Integer channel;
	private TAPrior prior;
	
	public MaxLikeTAFitter(int ch) { 
		this(new NoPrior(), ch);
	}
	
	public MaxLikeTAFitter(TAPrior p, int ch) {
		channel = ch;
		prior = p;
	}

	public TAFit fitTranscripts(TranscriptArrangement arr) {
		Double[] levels = new Double[arr.calls.length+1];
		
		Likelihoods dgf = new Likelihoods(arr, channel);
		dgf.coordinateOptimize();
		Double err = dgf.error();
		Double ll = dgf.likelihood();
		
		double[] gammas = dgf.gammas();
		for(int i = 0; i < gammas.length; i++) { 
			levels[i] = gammas[i];
		}
		levels[gammas.length] = dgf.lambda();
		
		TAFit fit = new TAFit(arr, prior, levels, err, ll);
		fit.predicted = null;
		
		return fit;
	}
}
