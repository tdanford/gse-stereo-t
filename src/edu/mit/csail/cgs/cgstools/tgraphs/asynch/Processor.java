/*
 * Author: tdanford
 * Date: Apr 22, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.asynch;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;

/**
 * An asynchronous processing module in the query framework.  
 * 
 * @author tdanford
 */
public interface Processor {

	public boolean isReady();
	public void process(TaggedGraph graph);
	public void addInput(GraphContext c);
	public void addOutputProcessor(Processor proc);
}