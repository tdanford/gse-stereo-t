/*
 * Author: tdanford
 * Date: Apr 17, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.patterns;

import java.util.*;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.GraphQuery;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.ewok.verbs.MapperIterator;
import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.Predicate;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.iterators.SingleIterator;
import edu.mit.csail.cgs.utils.models.Model;

public interface GraphPattern {
	
	/**
	 * The core method of a GraphPattern -- takes in a GraphContext, and a graph, and
	 * returns a (possibly empty) stream of GraphContexts, which may be identical to 
	 * the first context, or modified from it, or extended from it.  
	 * 
	 * @param graph
	 * @param context
	 * @return
	 */
	public Iterator<GraphContext> match(TaggedGraph graph, GraphContext context);
	
	// 
	// These two methods actually have slightly different semantics. 
	//	
	// dependencies() returns an array of names that the pattern ultimately *requires* 
	// to be present.  In other words, they are 'necessary' for the pattern to hold.  
	//
	// names(), on the other hand, introduces a set of nodes for which this pattern is 
	// 'sufficient' -- that is, they are nodes which this pattern can introduce if 
	// not present; alternately, if values of these variables are present, this pattern
	// may further restrict the set of GraphContexts which pass through it based on the 
	// values of those variables.
	// 
	
	public String[] dependencies();
	public String[] names();
}
