/*
 * Author: tdanford
 * Date: Apr 21, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.patterns;

import java.util.Iterator;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.utils.iterators.SingleIterator;

public class ContextFilterPattern extends AbstractGraphPattern {

	private Filter<GraphContext,GraphContext> filter;
	
	public ContextFilterPattern(Filter<GraphContext,GraphContext> f, String... ns) { 
		super(ns, new String[] {});
		filter = f; 
	}

	public Iterator<GraphContext> match(TaggedGraph graph, GraphContext context) {
		return new FilterIterator<GraphContext,GraphContext>(filter, new SingleIterator<GraphContext>(context));
	}
	
	public String toString() { 
		return String.format("filter(%s)", AbstractGraphPattern.namesToString(dependencies()));
	}
}
