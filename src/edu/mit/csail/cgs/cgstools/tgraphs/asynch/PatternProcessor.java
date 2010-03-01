/*
 * Author: tdanford
 * Date: Apr 22, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.asynch;

import java.util.*;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.cgstools.tgraphs.asynch.*;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.*;

public class PatternProcessor implements Processor {
	
	private LinkedList<GraphContext> input;
	private ArrayList<Processor> output;
	private GraphPattern pattern;
	
	public PatternProcessor(GraphPattern p) { 
		pattern = p;
		input = new LinkedList<GraphContext>();
		output = new ArrayList<Processor>();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.graphs.Processor#isReady()
	 */
	public boolean isReady() { 
		return !input.isEmpty(); 
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.graphs.Processor#process(edu.mit.csail.cgs.sigma.expression.graphs.TaggedGraph)
	 */
	public void process(TaggedGraph graph) {
		GraphContext c = input.removeFirst();
		//System.out.println(String.format("%s >>> %s", c.toString(), pattern.toString()));
		Iterator<GraphContext> results = pattern.match(graph, c);
		while(results.hasNext()) { 
			GraphContext r = results.next();
			//System.out.println(String.format("\t== %s", r.toString()));
			for(Processor proc : output) { 
				proc.addInput(r);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.graphs.Processor#addInput(edu.mit.csail.cgs.sigma.expression.graphs.GraphContext)
	 */
	public void addInput(GraphContext c) { 
		input.addLast(c);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.graphs.Processor#addOutputProcessor(edu.mit.csail.cgs.sigma.expression.graphs.PatternProcessor)
	 */
	public void addOutputProcessor(Processor proc) { 
		output.add(proc);
	}
}
