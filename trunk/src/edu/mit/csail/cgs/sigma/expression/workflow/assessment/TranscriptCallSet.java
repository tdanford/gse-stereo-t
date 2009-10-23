package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.models.*;

public class TranscriptCallSet {
	
	public static void main(String[] args) { 
		String key = args.length > 0 ? args[0] : "s288c";
		WorkflowProperties props = new WorkflowProperties();
		
		try {
			TranscriptCallSet calls = new TranscriptCallSet(props, key);
			OverlappingCalls overlap = new OverlappingCalls(calls);
			int count = overlap.countOverlaps();
			System.out.println(String.format("# Overlapping Regions: %d", count));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String key;
	private WorkflowProperties props;
	public TranscriptCall[] calls;

	public TranscriptCallSet(WorkflowProperties ps, String k) throws IOException { 
		props = ps;
		key = k;
		
		File plusCalls = new File(props.getDirectory(), String.format("%s_plus.transcripts", key));
		File minusCalls = new File(props.getDirectory(), String.format("%s_negative.transcripts", key));
		
		ArrayList<TranscriptCall> calllist = new ArrayList<TranscriptCall>();
		
		WorkflowTranscriptReader loader = new WorkflowTranscriptReader(plusCalls);
		while(loader.hasNext()) { 
			TranscriptCall call = loader.next();
			calllist.add(call);
		}
		loader.close();
		
		loader = new WorkflowTranscriptReader(minusCalls);
		while(loader.hasNext()) { 
			TranscriptCall call = loader.next();
			calllist.add(call);
		}
		loader.close();
		
		calls = calllist.toArray(new TranscriptCall[0]);
		Arrays.sort(calls);
		System.out.println(String.format("Loaded %d calls.", calls.length));
	}

	public Collection<TranscriptCall> getWatsonTranscripts() {
		ArrayList<TranscriptCall> cs = new ArrayList<TranscriptCall>();
		for(int i = 0; i < calls.length; i++) { 
			if(calls[i].strand.equals("+")) { 
				cs.add(calls[i]);
			}
		}
		return cs;
	}

	public Collection<TranscriptCall> getCrickTranscripts() {
		ArrayList<TranscriptCall> cs = new ArrayList<TranscriptCall>();
		for(int i = 0; i < calls.length; i++) { 
			if(calls[i].strand.equals("-")) { 
				cs.add(calls[i]);
			}
		}
		return cs;
	}
}

class OverlappingCalls {
	
	private TranscriptCallSet set;
	
	public OverlappingCalls(TranscriptCallSet cs) { 
		set = cs;
	}
	
	public int countOverlaps() { 
		int count = 0;
		for(int i = 0; i < set.calls.length; i++) {
			int start = -1, end = -1;
			for(int j = i + 1; j < set.calls.length && set.calls[j].strandInvariantOverlaps(set.calls[i]); j++) {
				if(set.calls[i].strand.equals(set.calls[j].strand)) { 
					start = Math.max(set.calls[i].start, set.calls[j].start);
					end = Math.min(set.calls[i].end, set.calls[j].end);
				}
			}
			
			if(end > start) { 
				System.out.println(String.format("%s:%d-%d", set.calls[i].chrom, start, end));
				for(int j = i; j < set.calls.length && set.calls[j].strandInvariantOverlaps(set.calls[i]); j++) {
					if(set.calls[i].strand.equals(set.calls[j].strand)) {
						TranscriptCall c = set.calls[j];
						System.out.println(String.format("\t%s:%d-%d:%s", c.chrom, c.start, c.end, c.strand));
					}
				}
				
				count += 1;
			}
		}
		return count;
	}
}
