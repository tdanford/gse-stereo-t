/*
 * Author: tdanford
 * Date: Apr 22, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.asynch;

import java.util.Iterator;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.GraphQuery;
import edu.mit.csail.cgs.cgstools.tgraphs.QueryEvaluator;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;

public class AsynchQueryEvaluator implements QueryEvaluator {
	
	private TaggedGraph graph;
	
	public AsynchQueryEvaluator(TaggedGraph g) { 
		graph = g;
	}

	public Iterator<GraphContext> eval(GraphQuery query) {
		ProcessorScheduler scheduler = new ProcessorScheduler(query);
		scheduler.run(graph);
		return scheduler.getResults().iterator();
	}

}
