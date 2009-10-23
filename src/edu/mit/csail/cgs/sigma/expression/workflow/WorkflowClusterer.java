package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.utils.iterators.BacktrackingIterator;

public class WorkflowClusterer implements Iterator<Cluster> {
	
	private BacktrackingIterator<DataSegment> segItr;
	private Integer[] channels;
	private Cluster pending;
	
	public WorkflowClusterer(Iterator<DataSegment> si) { 
		this(null, si);
	}
	
	public WorkflowClusterer(Integer[] chs, Iterator<DataSegment> si) {
		channels = chs != null ? chs.clone() : null;
		segItr = new BacktrackingIterator<DataSegment>(si);
		findNext();
	}
	
	private void findNext() { 
		DataSegment s = null;
		pending = null;
		
		while(segItr.hasNext() && (s = segItr.next()) != null && !isInCluster(s)) { 
			s = null; 
		}
		
		if(s != null) {
			ArrayList<DataSegment> csegs = new ArrayList<DataSegment>();
			csegs.add(s);

			while(segItr.hasNext() && (s = segItr.next()) != null && isInCluster(s)) { 
				csegs.add(s);
			}
			
			pending = new Cluster(csegs, channels);
		}
	}
	
	public boolean isInCluster(DataSegment s) { 
		return s.hasConsistentType(Segment.LINE, channels);
	}
	
	public boolean hasNext() { 
		return pending != null;
	}
	
	public Cluster next() { 
		Cluster c = pending;
		findNext();
		return c;
	}
	
	public void remove() { 
		throw new UnsupportedOperationException();
	}
	
}
