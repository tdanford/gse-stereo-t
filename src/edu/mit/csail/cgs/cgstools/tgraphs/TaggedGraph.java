/*
 * Author: tdanford
 * Date: Apr 19, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;

public interface TaggedGraph { 
	
	public String[] edgeTags();
	public String[] nodeTypes();
	
	public Iterator<String> allNodes();
	public Iterator<String> typedNodes(String type);
	
	public Iterator<String> forward(String node, String edgeTag);
	public Iterator<String> reverse(String node, String edgeTag);
	
	public boolean hasNode(String n); 
	public boolean hasNode(String n, String type); 
	public boolean hasEdge(String n1, String n2, String tag);
}

