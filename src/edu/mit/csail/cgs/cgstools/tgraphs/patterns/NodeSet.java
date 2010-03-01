/*
 * Author: tdanford
 * Date: Apr 21, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.patterns;

import java.util.*;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.iterators.SingleIterator;

public class NodeSet extends AbstractGraphPattern implements NodePattern {
	
	private String nodeName;
	private Set<String> nodeNames;

	public NodeSet(String n, String... ts) {
		super(new String[] {}, new String[] { n });
		nodeName = n;
		nodeNames = new TreeSet<String>();
		for(int i = 0; i < ts.length; i++) { 
			nodeNames.add(ts[i]);
		}
	}

	public String nodeName() { return nodeName; }
	public Collection<String> targetNames() { return nodeNames; } 
	
	public String nameString() { 
		StringBuilder sb = new StringBuilder();
		for(String t : nodeNames) {
			if(sb.length() > 0) { sb.append(","); }
			sb.append(t);
		}
		return sb.toString();
	}
	
	public String toString() { return String.format("%s in {%s}", nodeName, nameString()); }

	public Iterator<GraphContext> match(TaggedGraph graph, GraphContext context) {
		String nodeValue = context.lookup(nodeName);
		
		if(nodeValue == null) { 
			Iterator<String> nodes = new FilterIterator<String,String>(
					new GraphNameFilter(graph), nodeNames.iterator());
			return makeContexts(nodes, context, nodeName);
		} else if(nodeNames.contains(nodeValue)) { 
			return new SingleIterator<GraphContext>(context);
		} else { 
			return new EmptyIterator<GraphContext>();
		}
	}
	
	protected static class GraphNameFilter implements Filter<String,String> {
		
		private TaggedGraph graph;
		
		public GraphNameFilter(TaggedGraph g) { 
			graph = g; 
		}

		public String execute(String name) {
			return graph.hasNode(name) ? name : null;
		} 
	}
}