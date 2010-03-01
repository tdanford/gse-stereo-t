/*
 * Author: tdanford
 * Date: Apr 21, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.patterns;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.GraphQuery;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.utils.ArrayUtils;

public class Compose implements GraphPattern {
	
	private String[] deps, out;
	private GraphPattern pfirst, psecond;
	
	public Compose(GraphPattern s, GraphPattern f) { 
		psecond = s; 
		pfirst = f;
		
		if(GraphQuery.patternConflicts(pfirst, psecond)) { 
			throw new IllegalArgumentException(String.format("Graph patterns conflict."));
		}
		
		if(GraphQuery.patternDepends(pfirst, psecond)) { 
			throw new IllegalArgumentException(String.format("First pattern can't depend on second pattern in ComposePatterns"));
		}
		
		Set<String> d = new TreeSet<String>();
		for(String out : pfirst.dependencies()) {
			d.add(out);
		}
		
		for(String out : psecond.dependencies()) {
			if(GraphQuery.stringArrayFind(pfirst.names(), out) == -1) { 
				d.add(out);
			}
		}
		
		out = ArrayUtils.cat(pfirst.names(), psecond.names());
		deps = d.toArray(new String[0]);
	}
	
	public String toString() { 
		return String.format("(and %s %s)", pfirst.toString(), psecond.toString());
	}

	public String[] dependencies() {
		return deps; 
	}

	public Iterator<GraphContext> match(TaggedGraph graph, GraphContext context) {
		return new ExpanderIterator<GraphContext,GraphContext>(new GraphPatternExpander(graph, psecond), pfirst.match(graph, context));
	}

	public String[] names() {
		return out;
	} 
	
}