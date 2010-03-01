/*
 * Author: tdanford
 * Date: Apr 21, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.patterns;

import java.util.Iterator;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.ewok.verbs.Expander;

public class GraphPatternExpander implements Expander<GraphContext,GraphContext> {
	
	private TaggedGraph graph;
	private GraphPattern pattern;
	
	public GraphPatternExpander(TaggedGraph g, GraphPattern p) { 
		graph = g; 
		pattern = p; 
	}
	
	public Iterator<GraphContext> execute(GraphContext c) { 
		return pattern.match(graph, c);
	}
}