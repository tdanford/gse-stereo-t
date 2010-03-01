/*
 * Author: tdanford
 * Date: Apr 23, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.asynch;

import java.util.*;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;

/**
 * Does no processing -- instead, simply stores each GraphContext that 
 * passes through it, for later retrieval.  This is useful as the endpoint 
 * of a series of processors in the asynchronous processing framework.  
 * 
 * @author tdanford
 */
public class CachingProcessor implements Processor {
	
	private ArrayList<Processor> outputs;
	private LinkedList<GraphContext> pendingInput;
	private LinkedList<GraphContext> seen;
	
	public CachingProcessor() { 
		outputs = new ArrayList<Processor>();
		pendingInput = new LinkedList<GraphContext>();
		seen = new LinkedList<GraphContext>();
	}
	
	public Collection<GraphContext> getCachedResults() { 
		return seen; 
	}

	public void addInput(GraphContext c) {
		pendingInput.addLast(c);
		seen.addLast(c);
	}

	public void addOutputProcessor(Processor proc) {
		outputs.add(proc);
	}

	public boolean isReady() {
		return !pendingInput.isEmpty();
	}

	public void process(TaggedGraph graph) {
		GraphContext c = pendingInput.removeFirst();
		for(Processor proc : outputs) { 
			proc.addInput(c);
		}
	}
}
