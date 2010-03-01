/*
 * Author: tdanford
 * Date: Apr 21, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.graphs.GraphModel;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.models.Model;

public class DefaultTaggedGraph extends Model implements TaggedGraph {
	
	private String[] tags;
	private GraphModel[] graphs;
	private Map<String,Integer> graphIndices; 
	
	public DefaultTaggedGraph() { 
		graphIndices = new TreeMap<String,Integer>();
		tags = new String[] {};
		graphs = new GraphModel[] {};
	}
	
	public DefaultTaggedGraph(DefaultTaggedGraph g1, DefaultTaggedGraph g2) { 
		graphIndices = new TreeMap<String,Integer>();
		
		for(int i = 0; i < g1.tags.length; i++) { 
			if(g2.hasEdgeTag(g1.tags[i])) { 
				throw new IllegalArgumentException(String.format("Duplicate tag: %s", 
						g1.tags[i]));
			}
		}
		
		tags = ArrayUtils.concat(g1.tags, g2.tags);
		graphs = ArrayUtils.concat(g1.graphs, g2.graphs);
		
		rebuildIndices();
	}
	
	public void updateModel() { 
		rebuildIndices();
	}
	
	public boolean hasEdgeTag(String tag) { 
		for(int i = 0; i < tags.length; i++) { 
			if(tags[i].equals(tag)) { return true; }
		}
		return false;
	}
	
	private void rebuildIndices() { 
		graphIndices.clear();
		for(int i = 0; i < tags.length; i++) { 
			graphIndices.put(tags[i], i);
		}
	}
	
	public void addTaggedGraph(String t, String[] edges) { 
		addTaggedGraph(t, new GraphModel(edges));
	}
	
	public void addTaggedGraph(String t, GraphModel m) { 
		tags = ArrayUtils.append(tags, t);
		graphs = ArrayUtils.append(graphs, m);
		graphIndices.put(t, graphs.length-1);
	}

	public Iterator<String> allNodes() {
		TreeSet<String> ns = new TreeSet<String>();
		for(String tag : graphIndices.keySet()) { 
			String[] narray = graphs[graphIndices.get(tag)].nodes;
			for(int i = 0; i < narray.length; i++) { 
				ns.add(narray[i]);
			}
		}
		return ns.iterator();
	}

	public String[] edgeTags() {
		return graphIndices.keySet().toArray(new String[0]); 
	}

	public Iterator<String> forward(String node, String edgeTag) {
		return graphIndices.containsKey(edgeTag) ? 
				graphs[graphIndices.get(edgeTag)].forward(node).iterator() :
				new EmptyIterator<String>();
	}

	public boolean hasEdge(String n1, String n2, String tag) {
		return graphIndices.containsKey(tag) ? 
				graphs[graphIndices.get(tag)].hasEdge(n1, n2) : 
					false;
	}

	public boolean hasNode(String n) {
		for(String t : graphIndices.keySet()) { 
			if(graphs[graphIndices.get(t)].hasNode(n)) { 
				return true; 
			}
		} 
		return false;
	}

	public boolean hasNode(String n, String type) {
		return type.equals("default") ? hasNode(n) : false;
	}

	public String[] nodeTypes() {
		return new String[] { "default" };
	}

	public Iterator<String> reverse(String node, String edgeTag) {
		return graphIndices.containsKey(edgeTag) ? 
				graphs[graphIndices.get(edgeTag)].reverse(node).iterator() :
				new EmptyIterator<String>();
	}

	public Iterator<String> typedNodes(String type) {
		if(type.equals("default")) { 
			return allNodes();
		} else { 
			return new EmptyIterator<String>();
		}
	}
}

