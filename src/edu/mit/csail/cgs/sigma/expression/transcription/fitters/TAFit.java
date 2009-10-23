/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription.fitters;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

import edu.mit.csail.cgs.sigma.Printable;
import edu.mit.csail.cgs.sigma.expression.transcription.Call;
import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.sigma.expression.transcription.priors.TAPrior;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.models.Model;

/**
 * Gives the fitted parameters for a particular TranscriptArrangement object, 
 * including indications of error (for least-squares type models) and log-likelihood
 * (for probabilistic models). 
 * 
 * @author tdanford
 */
public class TAFit extends Model implements Printable, Comparable<TAFit> {

	public TranscriptArrangement arrangement;
	public Double[] params;
	public Double error, logPrior, logLikelihood;
	public Double[] predicted;
	
	public TAFit() {}
	
	public TAFit(TranscriptArrangement arr, TAPrior p, Double[] ps, Double err, Double ll) { 
		arrangement = arr;
		params = ps.clone();
		
		for(int i = 0; i < arrangement.calls.length; i++) { 
			arrangement.calls[i].value = params[i];
		}
		
		error = err;
		logPrior = p.prior(arrangement);
		logLikelihood = ll;
		predicted = null;
	}
	
	public Collection<TranscriptCall> calls() { 
		Cluster cluster = arrangement.cluster;
		ArrayList<TranscriptCall> calllist = new ArrayList<TranscriptCall>();
		for(int i = 0; i < arrangement.calls.length; i++) { 
			double ity = params[i];
			double fall = params[arrangement.calls.length];  // this is right?
			Call call = arrangement.calls[i];
			TranscriptCall tcall = new TranscriptCall(cluster.chrom(), cluster.strand(),
					cluster.segmentStart(call.start), 
					cluster.segmentEnd(call.end-1), 
					ity, fall);
			calllist.add(tcall);
		}
		return calllist;
	}
	
	public String toString() { 
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("[%s]", arrangement.toString()));
		for(int i = 0; i < params.length; i++) { 
			sb.append(String.format(" %.3f", params[i]));
		}
		sb.append(String.format(" (error: %.3f, lp: %.3f, ll: %.3f)", 
				error, logPrior, logLikelihood));
		return sb.toString();
	}
	
	public int compareTo(TAFit f) {
		Double s = getScore(), fs = f.getScore();
		if(s > fs) { return -1; }
		if(s < fs) { return 1; }
		
		int c = arrangement.calls.length, fc = f.arrangement.calls.length;
		if(c < fc) { return -1; }
		if(c > fc) { return 1; }
		return 0;
	}
	
	public Double getScore() { return logPrior + logLikelihood; }
	
	public void print(PrintStream ps) {
		ps.println(toString());
	}

	public int hashCode() { 
		return arrangement.hashCode();
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof TAFit)) { return false; }
		TAFit f = (TAFit)o;
		return arrangement.equals(f.arrangement);
	}

	public boolean isLegal() {
		for(int i = 0; i < params.length/2; i++) { 
			if(params[i] < 0.0) { 
				return false;
			}
		}
		return true;
		
		//return params[0] > 0.0;
	}
}
