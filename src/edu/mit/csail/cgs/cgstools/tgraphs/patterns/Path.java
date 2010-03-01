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
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.ewok.verbs.MapperIterator;

public class Path extends AbstractGraphPattern implements EdgePattern {
	
	private String pathKey; 
	private String fromNode, toNode;
	private Set<String> types; 
	private boolean direction;
	
	public Path(String key, String f, String t, String... ts) {
		this(key, f, t, true, ts);
	}
	
	public Path(String key, String f, String t, boolean d, String... ts) {
		super(new String[] { f }, new String[] { t });
		fromNode = f; 
		toNode = t; 
		direction = d;
		types = new TreeSet<String>();
		for(int i = 0; i < ts.length; i++) { 
			types.add(ts[i]);
		}
		pathKey = String.format("PATH*%s", key);
	}
	
	public Path(String key, Node n1, Node n2, String... ts) { 
		this(key, n1.nodeName(), n2.nodeName(), ts);
	}
	
	public Path(String key, Node n1, Node n2, boolean dir, String... ts) { 
		this(key, n1.nodeName(), n2.nodeName(), dir, ts);
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
		if(direction) { 
			return String.format("%s ~~%s~~> %s", fromNode, typeString(), toNode);
		} else { 			
			return String.format("%s <~~%s~~ %s", fromNode, typeString(), toNode);
		}
	}
	
	public boolean direction() { return direction; }
	public String fromNode() { return fromNode; }
	public String toNode() { return toNode; }
	public Collection<String> edgeTypes() { return types; }
	public boolean acceptsEdgeType(String t) { return types.contains(t); }

	private static Set<String> parsePathString(String s) { 
		Set<String> p = new TreeSet<String>();
		if(s != null) { 
			String[] a = s.split("|");
			for(int i = 0; i < a.length; i++) { 
				p.add(a[i]); 
			}
		}
		return p;
	}
	
	public static String createPathString(Set<String> p) { 
		StringBuilder sb = new StringBuilder();
		for(String v : p) { 
			if(sb.length() > 0) { sb.append("|"); }
			sb.append(v);
		}
		return sb.toString();
	}
	
	public Iterator<GraphContext> match(TaggedGraph graph, GraphContext context) {
		
		String fromValue = context.lookup(fromNode);
		String toValue = context.lookup(toNode);
		
		Iterator<GraphContext> results = null;

		if(fromValue == null) { 
			throw new IllegalStateException(
					String.format("Can't extend a null path: %s", pathKey));
			
		} else if (toValue == null) { 
			results = new FilterIterator<GraphContext,GraphContext>(
					new PathExtendingFilter(), 
					pathStart(graph, context, toNode, fromValue));
			
		} else { 
			results = new FilterIterator<GraphContext,GraphContext>(
					new PathExtendingFilter(), 
					pathExtend(graph, context, toNode, toValue));
		}
		
		return results;
	}

	private Iterator<GraphContext> pathStart(
			TaggedGraph graph, GraphContext context, String name, String value) {

		Iterator<String> nodes = new ExpanderIterator<String,String>(
				new GraphTypeExpander(graph, value, direction), 
				types.iterator());
		
		return makeContexts(nodes, context, name);
	}
	
	private Iterator<GraphContext> pathExtend(
			TaggedGraph graph, GraphContext context, String name, String value) {

		Iterator<String> nodes = new ExpanderIterator<String,String>(
				new GraphTypeExpander(graph, value, direction), 
				types.iterator());
		
		return new MapperIterator<String,GraphContext>(
				new ContextModifier(name, context), nodes);
	}
	
	protected static class ContextModifier implements Mapper<String,GraphContext> {
		
		private String name;
		private GraphContext context;
		
		public ContextModifier(String n, GraphContext c) { 
			name = n;
			context = c;
		}
		
		public GraphContext execute(String value) { 
			GraphContext cc = GraphContext.replace(name, value, context);
			//System.out.println(String.format("%s + %s=%s => %s", context.toString(), name, value, cc.toString()));
			return cc;
		}
	}
	
	private class PathExtendingFilter implements Filter<GraphContext,GraphContext> {
		
		public PathExtendingFilter() { 
		}

		public GraphContext execute(GraphContext a) {
			String oldpv = a.lookup(pathKey);
			String terminalNode = a.lookup(toNode());
			String newpv = oldpv != null ? 
					String.format("%s|%s", oldpv, terminalNode) : 
					String.format("%s|%s", a.lookup(fromNode()), terminalNode);
			
			if(oldpv == null) { 
				GraphContext aa = new GraphContext(pathKey, newpv, a);
				//System.out.println(String.format("\t%s ~> %s", a.toString(), aa.toString()));
				return aa;
				
			} else {
				Set<String> prevPath = parsePathString(oldpv);
				if(prevPath.contains(terminalNode)) { 
					//System.out.println(String.format("\t%s ~> ***", a.toString()));
					return null; 
				} else { 
					GraphContext aa = GraphContext.replace(pathKey, newpv, a);
					//System.out.println(String.format("\t%s ~> %s", a.toString(), aa.toString()));
					return aa;
				}
			}
		} 
		
	}

	private static class GraphTypeExpander implements Expander<String,String> {
		
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
