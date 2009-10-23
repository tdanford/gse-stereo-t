/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.ClusterFitter;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.EndptsFit;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.EndptsFitter;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFit;
import edu.mit.csail.cgs.sigma.expression.transcription.identifiers.TranscriptIdentifier;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.models.Timer;
import edu.mit.csail.cgs.utils.models.Timing;

public class WorkflowTranscriptCaller extends ExpanderIterator<Cluster,TranscriptCall> {

	public WorkflowTranscriptCaller(TranscriptIdentifier tc, Timer t, Iterator<Cluster> clusters) {
		super(new TranscriptCaller(tc, t), clusters);
	}

	public WorkflowTranscriptCaller(ClusterFitter tc, Timer t, Iterator<Cluster> clusters) {
		super(new MaxLikelihoodTranscriptCaller(tc, t), clusters);
	}
}	

class TranscriptCaller implements Expander<Cluster,TranscriptCall> {

	private TranscriptFitter fitter;
	
	public TranscriptCaller(TranscriptIdentifier tc, Timer t) { 
		fitter = new TranscriptFitter(tc, t);
	}
	
	public Iterator<TranscriptCall> execute(Cluster cluster) { 
		TAFit fit = fitter.execute(cluster);
		if(fit == null) { 
			System.out.println(String.format(
					"Cluster %s\n \t -> returned no TAFit.", cluster.toString()));
			return new EmptyIterator<TranscriptCall>(); 
		}
		Collection<TranscriptCall> calls = fit.calls();
		return calls.iterator();
	}
}

class MaxLikelihoodTranscriptCaller implements Expander<Cluster,TranscriptCall> {

	private MaxLikelihoodTranscriptFitter fitter;
	
	public MaxLikelihoodTranscriptCaller(ClusterFitter tc, Timer t) { 
		fitter = new MaxLikelihoodTranscriptFitter(tc, t);
	}
	
	public Iterator<TranscriptCall> execute(Cluster cluster) { 
		System.out.println(String.format("MaxLikelihoodTranscriptCaller:\n\t%s", cluster.toString()));
		EndptsFit fit = fitter.execute(cluster);
		if(fit == null) { 
			System.out.println(String.format(
					"Cluster %s\n \t -> returned no TAFit.", cluster.toString()));
			return new EmptyIterator<TranscriptCall>(); 
		}
		Collection<TranscriptCall> calls = fit.calls();
		return calls.iterator();
	}
}

class TranscriptFitter implements Mapper<Cluster,TAFit> {

	private TranscriptIdentifier caller;
	private Timer timer;
	
	public TranscriptFitter(TranscriptIdentifier tc, Timer t) { 
		caller = tc;
		timer = t;
	}
	
	public void setTimer(Timer t) { timer = t; }
	
	public TAFit execute(Cluster cluster) { 
		System.out.println(String.format("Cluster: %s:%s:%d-%d (%d) segs",
				cluster.chrom(), cluster.strand(), cluster.start(), cluster.end(), 
				cluster.segments.length));
		
		long timeStart = System.currentTimeMillis();
		int size = cluster.segments.length;
		
		TAFit fit = caller.identify(cluster);
		
		long timeEnd = System.currentTimeMillis();
		Timing timing = new Timing(size, (double)(timeEnd-timeStart)/1000.0);
		
		if(timer != null) { 
			timer.addTiming(timing);
			System.out.println("\t=" + timing.toString());
		}
		
		return fit;
	}
}

class MaxLikelihoodTranscriptFitter implements Mapper<Cluster,EndptsFit> {

	private ClusterFitter caller;
	private Timer timer;
	
	public MaxLikelihoodTranscriptFitter(ClusterFitter tc, Timer t) { 
		caller = tc;
		timer = t;
	}
	
	public void setTimer(Timer t) { timer = t; }
	
	public EndptsFit execute(Cluster cluster) { 
		System.out.println(String.format("Cluster: %s:%s:%d-%d (%d) segs",
				cluster.chrom(), cluster.strand(), cluster.start(), cluster.end(), 
				cluster.segments.length));
		
		long timeStart = System.currentTimeMillis();
		int size = cluster.segments.length;
		
		EndptsFit fit = caller.fitCluster(cluster);
		
		long timeEnd = System.currentTimeMillis();
		Timing timing = new Timing(size, (double)(timeEnd-timeStart)/1000.0);
		
		if(timer != null) { 
			timer.addTiming(timing);
			System.out.println("\t=" + timing.toString());
		}
		
		return fit;
	}
}
