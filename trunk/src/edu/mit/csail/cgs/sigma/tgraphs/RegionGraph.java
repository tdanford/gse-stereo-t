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
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;

public class RegionGraph extends SimpleTaggedGraph {
	
	private Map<String,OverlappingRegionFinder> typedRegionNodeFinders;

	public RegionGraph() { 
		super();
		typedRegionNodeFinders = new TreeMap<String,OverlappingRegionFinder>();
	}
	
	public <T extends Region> void addRegionNodes(String type, Iterator<T> itr) {
		if(!typedRegionNodeFinders.containsKey(type)) { 
			typedRegionNodeFinders.put(type, new OverlappingRegionFinder());
		}
		OverlappingRegionFinder finder = typedRegionNodeFinders.get(type);
		
		while(itr.hasNext()) { 
			T region = itr.next();
			addNode(String.format("%s:%s", type, region.toString()), type);
			finder.addRegion(region);
		}
	}
	
	public void addOverlappingEdges(String nodeType1, String nodeType2) { 
		if(!typedRegionNodeFinders.containsKey(nodeType1)) {  
			throw new IllegalArgumentException(nodeType1);
		}
		if(!typedRegionNodeFinders.containsKey(nodeType2)) { 
			throw new IllegalArgumentException(nodeType2);
		}
		
		
	}
}
