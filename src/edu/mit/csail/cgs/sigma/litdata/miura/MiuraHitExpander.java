package edu.mit.csail.cgs.sigma.litdata.miura;

import java.io.IOException;
import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;

public class MiuraHitExpander implements Expander<Region,MiuraHit> {
	
	private LinkedList<MiuraHit> hits;
	
	public MiuraHitExpander(Iterator<MiuraHit> hs) { 
		hits = new LinkedList<MiuraHit>();
		while(hs.hasNext()) { hits.add(hs.next()); }
	}
	
	public MiuraHitExpander(WorkflowProperties ps) throws IOException { 
		this(new MiuraHits(ps));
	}

	public Iterator<MiuraHit> execute(Region a) {
		return new FilterIterator<MiuraHit,MiuraHit>(new OverlapFilter(a), hits.iterator());
	}

	public static class OverlapFilter implements Filter<MiuraHit,MiuraHit> { 
		private Region region;
		public OverlapFilter(Region r) { region = r; }
		
		public MiuraHit execute(MiuraHit h) { 
			int hs = h.start, he = h.end;
			return (hs <= region.getStart() && he >= region.getStart()) || 
				(region.getStart() <= hs && region.getEnd() >= hs) ? h : null;
		}
	}
	
}
