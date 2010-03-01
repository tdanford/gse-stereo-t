/*
 * Author: tdanford
 * Date: Apr 21, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.patterns;

import java.util.Iterator;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.Predicate;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.iterators.SingleIterator;

public class ContextPredicatePattern extends AbstractGraphPattern {
	
	private Predicate<Pair<TaggedGraph,GraphContext>> pred;
	
	public ContextPredicatePattern(
			Predicate<Pair<TaggedGraph,GraphContext>> p, String... ns) { 
		super(ns, new String[] {});
		pred = p;
	}

	public Iterator<GraphContext> match(TaggedGraph graph, GraphContext context) {
		return pred.accepts(new Pair<TaggedGraph,GraphContext>(graph, context)) ? 
			new SingleIterator<GraphContext>(context) : 
			new EmptyIterator<GraphContext>();
	}
}