/*
 * Author: tdanford
 * Date: Apr 17, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs;

import java.io.PrintStream;
import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;
import edu.mit.csail.cgs.utils.iterators.SingleIterator;
import edu.mit.csail.cgs.cgstools.tgraphs.asynch.*;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.*;

public class SerialQueryEvaluator implements QueryEvaluator {

	private TaggedGraph graph; 
	private boolean debugging;
	
	public SerialQueryEvaluator(TaggedGraph g) { 
		graph = g;
		debugging = false;
	}
	
	public static void printQueryPath(GraphPattern[] path, PrintStream ps) { 
		for(int i = 0; i < path.length; i++) { 
			ps.println(String.format("%d: %s", i, path[i].toString()));
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.graphs.QueryEvaluator#eval(edu.mit.csail.cgs.sigma.expression.graphs.GraphQuery)
	 */
	public Iterator<GraphContext> eval(GraphQuery query) { 
		GraphPattern[] patts = query.queryPath();
		LinkedList<GraphPattern> plist = new LinkedList<GraphPattern>();
		for(GraphPattern p : patts) { plist.add(p); }
		return eval(plist);
	}
	
	private Iterator<GraphContext> eval(List<GraphPattern> ps) { 
		if(ps.isEmpty()) {
			if(debugging) { 
				System.out.println(String.format("Creating the root context..."));
			}
			return new SingleIterator<GraphContext>(new GraphContext());
		} else { 
			return eval(ps.get(ps.size()-1), eval(ps.subList(0, ps.size()-1)));
		}
	}
	
	private Iterator<GraphContext> eval(GraphPattern p, Iterator<GraphContext> citr) {
		if(debugging) { 
			System.out.println(String.format("Evaluating: \"%s\"", p.toString()));
			LinkedList<GraphContext> inner = new LinkedList<GraphContext>();
			int i = 0;
			while(citr.hasNext()) { 
				GraphContext c = citr.next();
				System.out.println(String.format("\t%d: %s", i++, c.toString()));
				inner.add(c);
			}
			System.out.println();
			citr = inner.iterator();
		}
		
		return new ExpanderIterator<GraphContext,GraphContext>(
				new PatternExpander(p), citr);
	}

	private class PatternExpander implements Expander<GraphContext,GraphContext> {
		
		private GraphPattern pattern;
		
		private PatternExpander(GraphPattern p){ 
			pattern = p; 
		}

		public Iterator<GraphContext> execute(GraphContext root) {
			return pattern.match(graph, root);
		} 
	}
	
}

