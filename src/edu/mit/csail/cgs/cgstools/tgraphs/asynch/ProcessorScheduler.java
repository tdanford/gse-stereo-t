/*
 * Author: tdanford
 * Date: Apr 22, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.asynch;

import java.util.*;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.GraphQuery;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.cgstools.tgraphs.asynch.*;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.*;

public class ProcessorScheduler {

	public ArrayList<Processor> procs; 
	public CachingProcessor terminus;
	
	public ProcessorScheduler(GraphQuery query) { 
		this(query.queryPath());
	}
	
	public ProcessorScheduler(GraphPattern[] patts) {
		
		Set<Integer> paths = new TreeSet<Integer>();
		
		procs = new ArrayList<Processor>();
		terminus = new CachingProcessor();

		for(int i = 0; i < patts.length; i++) { 
			procs.add(new PatternProcessor(patts[i]));
			if(i > 0) { 
				procs.get(i-1).addOutputProcessor(procs.get(i));
			}
			
			if(patts[i] instanceof Path) { 
				paths.add(i);
			}
		}
		
		procs.get(procs.size()-1).addOutputProcessor(terminus);
		procs.add(terminus);
		
		for(int pidx : paths) { 
			procs.get(pidx).addOutputProcessor(procs.get(pidx));
		}
		
		procs.get(0).addInput(new GraphContext());
	}
	
	public Collection<GraphContext> getResults() { 
		return terminus.getCachedResults();
	}
	
	public boolean isFinished() { 
		for(Processor p : procs) { 
			if(p.isReady()) { 
				return false;
			}
		}
		return true;
	}
	
	public boolean process(TaggedGraph graph) { 
		for(Processor p : procs) { 
			if(p.isReady()) { 
				p.process(graph);
			}
		}
		return !isFinished();
	}
	
	public void run(TaggedGraph graph) { 
		while(process(graph)) {}
	}
}
