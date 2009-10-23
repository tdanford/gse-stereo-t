/*
 * Author: tdanford
 * Date: May 29, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.OverlappingRegionKeyFinder;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.sigma.tgraphs.GeneKey;

/**
 * Tries to figure out, for arbitrary regions, whether the region is 
 * differentially expressed or not.  
 * 
 * @author tdanford
 */
public class DifferentialExpression {
	
	private WorkflowProperties props;
	private String key, expt;
	private WorkflowIndexing indexing;
	private WholeGenome genome; 

	private OverlappingRegionKeyFinder<DataSegment> segs;
	
	public DifferentialExpression(WorkflowProperties props, String k, String e) throws IOException {
		this(props, k, e, WholeGenome.loadWholeGenome(props, k));
	}
	
	public DifferentialExpression(WorkflowProperties props, String k, String e, WholeGenome genome) throws IOException {
		key = k; expt = e;
		this.props = props;
		indexing = props.getIndexing(key);
		genome.loadIterators();
		
		segs = new OverlappingRegionKeyFinder<DataSegment>(
				genome.getTranscribedSegments(expt));
	}
	
	public boolean differentiallyExpressed(RegionKey key) { 
		Collection<DataSegment> over = segs.findOverlapping(key);
		for(DataSegment seg : over) { 
			if(seg.isDifferential(expt)) { 
				return true;
			}
		}
		return false;
	}

	public boolean strandedDifferentiallyExpressed(RegionKey key) { 
		Collection<DataSegment> over = segs.findStrandedOverlapping(key);
		for(DataSegment seg : over) { 
			if(seg.isDifferential(expt)) { 
				return true;
			}
		}
		return false;
	}
	
}

