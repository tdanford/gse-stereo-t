/*
 * Author: tdanford
 * Date: May 6, 2009
 */
package edu.mit.csail.cgs.sigma.expression.simulation;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Generator;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;

public class SimulatorGenerator implements Generator<ProbeLine> {
	
	private SimParameters params;
	private boolean strand;
	
	public SimulatorGenerator(SimParameters ps, boolean str) { 
		params = ps;
		strand = str;
	}

	public Iterator<ProbeLine> execute() {
		ProbeSimulator sim = 
			//new SimpleSimulator(params, strand);
			new LogAdditiveSimulator(params, strand);
		
		return sim.probes();
	}
	
	public Collection<Integer> trueBreakpoints(boolean str) { 
		return params.breakpoints(str);
	}
	
	public Collection<Integer> trueBreakpoints() { 
		return params.breakpoints(strand);
	}
	
	public Collection<TranscriptCall> trueTranscripts() { 
		return params.transcripts(strand);
	}
	
	public Collection<TrueSegment> trueSegments() {
		SimParameters.SimTranscript[] array = 
			strand ? params.plus : params.minus;
		String str = strand ? "+" : "-";
		
		ArrayList<TrueSegment> segs = new ArrayList<TrueSegment>();
		for(int i = 0; i < array.length; i++) { 
			SimParameters.SimTranscript trans = array[i];
			segs.add(new TrueSegment(params.chrom, str, trans.start, trans.end, trans.intensity));
		}
		return segs;
	}
	
	public static class TrueSegment extends RegionKey {
		
		public Double intensity;
		
		public TrueSegment(String c, String str, int s, int e, double its) { 
			super(c, s, e, str);
			intensity = its;
		}
	}
}
