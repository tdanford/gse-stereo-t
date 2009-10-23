/*
 * Author: tdanford
 * Date: May 13, 2009
 */
package edu.mit.csail.cgs.sigma.tgraphs;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.utils.graphs.DirectedGraph;
import edu.mit.csail.cgs.utils.graphs.UndirectedGraph;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;

public class SimpleTaggedGraph implements TaggedGraph {
	
	public static String typeGraph = "node-types";
	
	private Set<String> nodes;
	private Set<String> nodeTypes;
	private Map<String,DirectedGraph> edges;
	
	public SimpleTaggedGraph() { 
		nodes = new LinkedHashSet<String>();
		nodeTypes = new TreeSet<String>();
		edges = new TreeMap<String,DirectedGraph>();
		edges.put(typeGraph, new DirectedGraph());
	}
	
	public void addNode(String name, String type) { 
		if(!nodes.contains(name)) { 
			nodes.add(name);
			edges.get(typeGraph).addVertex(name);
			
			if(!nodeTypes.contains(type)) { 
				nodeTypes.add(type);
				edges.get(typeGraph).addVertex(name);
			}
			
			edges.get(typeGraph).addEdge(name, type);
		}
	}
	
	public void addEdge(String n1, String n2, String edgeTag) { 
		if(!nodes.contains(n1)) { 
			throw new IllegalArgumentException(n1);
		}
		if(!nodes.contains(n2)) { 
			throw new IllegalArgumentException(n2);
		}
		
		if(!edges.containsKey(edgeTag)) { 
			edges.put(edgeTag, new DirectedGraph());
		}
		
		DirectedGraph eg = edges.get(edgeTag);
		if(!eg.containsVertex(n1)) { eg.addVertex(n1); }
		if(!eg.containsVertex(n2)) { eg.addVertex(n2); }
		
		eg.addEdge(n1, n2);
	}

	public Iterator<String> allNodes() {
		return nodes.iterator();
	}

	public String[] edgeTags() {
		return edges.keySet().toArray(new String[0]);
	}

	public Iterator<String> forward(String node, String edgeTag) {
		if(edges.containsKey(edgeTag)) { 
			return edges.get(edgeTag).getNeighbors(node).iterator();
		} else { 
			return new EmptyIterator<String>();
		}
	}

	public boolean hasEdge(String n1, String n2, String tag) {
		if(edges.containsKey(tag)) { 
			return edges.get(tag).containsEdge(n1, n2);
		} else { 
			return false;
		}
	}

	public boolean hasNode(String n) {
		return nodes.contains(n);
	}

	public boolean hasNode(String n, String type) {
		return hasNode(n) && nodeTypes.contains(type) &&
			edges.get(typeGraph).containsEdge(n, type);
	}

	public String[] nodeTypes() {
		return nodeTypes.toArray(new String[0]);
	}

	public Iterator<String> reverse(String node, String edgeTag) {
		if(edges.containsKey(edgeTag)) { 
			return edges.get(edgeTag).getParents(node).iterator();
		} else { 
			return new EmptyIterator<String>();
		}
	}

	public Iterator<String> typedNodes(String type) {
		return edges.get(typeGraph).getParents(type).iterator();
	}
}
