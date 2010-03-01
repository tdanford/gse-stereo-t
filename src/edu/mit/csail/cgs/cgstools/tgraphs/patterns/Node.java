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

public class Node extends AbstractGraphPattern implements NodePattern {
	
	private String nodeName;
	private Set<String> types;

	public Node(String n, String... ts) {
		super(new String[] {}, new String[] { n });
		nodeName = n;
		types = new TreeSet<String>();
		for(int i = 0; i < ts.length; i++) { 
			types.add(ts[i]);
		}
	}

	public String nodeName() { return nodeName; }
	public Collection<String> nodeTypes() { return types; } 
	
	public boolean acceptsNodeType(String t) { return types.contains(t); }
	
	public String typeString() { 
		StringBuilder sb = new StringBuilder();
		for(String t : types) {
			if(sb.length() > 0) { sb.append(","); }
			sb.append(t);
		}
		return sb.toString();
	}
	
	public String toString() { return String.format("%s::%s", nodeName, typeString()); }

	public Iterator<GraphContext> match(TaggedGraph graph, GraphContext context) {
		String nodeValue = context.lookup(nodeName);
		
		if(nodeValue == null) { 
			Iterator<String> nodes = new ExpanderIterator<String,String>(
					new GraphTypeExpander(graph), types.iterator());
			return makeContexts(nodes, context, nodeName);
		} else {
			for(String type : types) { 
				if(graph.hasNode(nodeValue, type)) { 
					return new SingleIterator<GraphContext>(context);
				}
			}
			return new EmptyIterator<GraphContext>();
		}
	}
	
	protected static class GraphTypeExpander implements Expander<String,String> {
		
		private TaggedGraph graph;
		
		public GraphTypeExpander(TaggedGraph g) { 
			graph = g; 
		}

		public Iterator<String> execute(String type) {
			return graph.typedNodes(type);
		} 
	}
}