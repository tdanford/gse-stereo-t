/*
 * Author: tdanford
 * Date: Apr 21, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.patterns;

import java.util.*;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.iterators.SingleIterator;

public class Edge extends AbstractGraphPattern implements EdgePattern {
	
	private String fromNode, toNode;
	private Set<String> types; 
	
	public Edge(String f, String t, String... ts) {
		super(new String[] { f, t }, new String[] {});
		fromNode = f; 
		toNode = t; 
		types = new TreeSet<String>();
		for(int i = 0; i < ts.length; i++) { 
			types.add(ts[i]);
		}
	}
	
	public Edge(Node n1, Node n2, String... ts) { 
		this(n1.nodeName(), n2.nodeName(), ts);
	}
	
	public Edge(Node n1, String n2, String... ts) { 
		this(n1.nodeName(), n2, ts);
	}
	
	public Edge(String n1, Node n2, String... ts) { 
		this(n1, n2.nodeName(), ts);
	}
	
	public String typeString() { 
		StringBuilder sb = new StringBuilder();
		for(String t : types) {
			if(sb.length() > 0) { sb.append(","); }
			sb.append(t);
		}
		return sb.toString();
	}
	
	public String toString() { 
		return String.format("%s --%s--> %s", fromNode, typeString(), toNode); 
	}
	
	public String fromNode() { return fromNode; }
	public String toNode() { return toNode; }
	public Collection<String> edgeTypes() { return types; }
	public boolean acceptsEdgeType(String t) { return types.contains(t); }

	public Iterator<GraphContext> match(TaggedGraph graph, GraphContext context) {

		String fromValue = context.lookup(fromNode);
		String toValue = context.lookup(toNode);

		if(fromValue == null && toValue == null) {
			throw new IllegalStateException(String.format("Pattern \"%s\" (fromNode: %s, toNode: %s) can't be matched in context %s", 
					toString(), String.valueOf(fromValue), String.valueOf(toValue), context.toString()));
			
		} else if (toValue == null) {
			Iterator<String> nodes = new ExpanderIterator<String,String>(
					new GraphTypeExpander(graph, fromValue, true), 
					types.iterator());
			return makeContexts(nodes, context, toNode);
			
		} else if (fromValue == null) {
			Iterator<String> nodes = new ExpanderIterator<String,String>(
					new GraphTypeExpander(graph, toValue, false), 
					types.iterator());
			return makeContexts(nodes, context, fromNode);
			
		} else {
			for(String type : types) { 
				if(graph.hasEdge(fromValue, toValue, type)) { 
					return new SingleIterator<GraphContext>(context);
				}
			} 
			
			return new EmptyIterator<GraphContext>();
		}
	}
	
	protected static class GraphTypeExpander implements Expander<String,String> {
		
		private TaggedGraph graph;
		private String node;
		private boolean direction;
		
		public GraphTypeExpander(TaggedGraph g, String n, boolean d) { 
			graph = g; node = n;
			direction = d;
		}

		public Iterator<String> execute(String type) {
			if(direction) { 
				return graph.forward(node, type);
			} else { 
				return graph.reverse(node, type);
			}
		} 
	}
}
