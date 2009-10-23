/*
 * Author: tdanford
 * Date: Dec 20, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription.priors;

import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.transcription.Call;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;

/**
 * @author tdanford
 */
public class TAPerProbePenalty implements TAPrior {
	
	private Double alpha, beta;
	private Double scalePenalty;
	
	public TAPerProbePenalty(double a, double b, double sc) {
		alpha = a;
		beta = b;
		scalePenalty = sc;
	}

	public Double prior(TranscriptArrangement arr) {
		double sum = 0.0;
		
		for(Call call : arr.calls) { 
			for(int segment = 0; segment < arr.cluster.segments.length; segment += 1) { 
				Integer overlaps = arr.overlappingCallIndices(segment).size();
				DataSegment s = arr.cluster.segments[segment];
				int count = s.dataLocations.length;
				sum += ((double)(count * overlaps) * alpha);
			}
			
			double value = call.value * call.value;
			sum += (scalePenalty * value);
		}
		
		sum += ((double)(arr.calls.length-1) * beta);
		
		return sum;
	}
}
