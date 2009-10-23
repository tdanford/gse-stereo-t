/*
 * Author: tdanford
 * Date: May 1, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.fitters;

import java.util.*;

import edu.mit.csail.cgs.utils.*;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.sigma.expression.rma.SingleExptHDot;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.sigma.expression.transcription.priors.TAPrior;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;

/**
 * Full probabilistic treatement for Transcript Fitting.
 *
 * @author tdanford
 */
public class SamplingTAFitter implements TAFitter {

	private TAPrior prior;
	private int samples, channel;
	
	public SamplingTAFitter(WorkflowProperties ps, TAPrior p, int ch) {
		channel = ch;
		prior = p;
		samples = ps.getNumTranscriptionSamples();
	}
	
	public TAFit fitTranscripts(TranscriptArrangement arr) {
		
		SingleExptHDot ht = new SingleExptHDot(arr, channel);
		
		/*
		System.out.print(String.format("\tSampling (%d calls, %d probes) x %d:",
				arr.calls.length, arr.cluster.locations.length, 
				samples)); 
		System.out.flush();
		*/
		
		for(int i = 0; i < samples; i++) { 
			ht.sample();
			//System.out.print(String.format(" %d", i)); System.out.flush();
		}
		//System.out.println();
		TAFit fit = ht.createFit(prior);
		
		double var = 0.0;
		
		int length = ht.y.length;
		Double[] predArray = ht.predictions();
		
		for(int i = 0; i < length; i++) { 
			double diff = ht.y[i] - predArray[i];
			var += diff*diff;
		}
		
		var /= (double)length;
		var = Math.sqrt(var);
		
		fit.predicted = predArray;
		return fit;
	}

}
