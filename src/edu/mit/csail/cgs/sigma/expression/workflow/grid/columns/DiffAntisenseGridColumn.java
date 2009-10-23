/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.grid.columns;

import java.io.IOException;
import java.util.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.sigma.expression.workflow.WholeGenome;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.grid.GridColumn;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;

public class DiffAntisenseGridColumn implements GridColumn {
	
	private String name;
	private String expt;
	private ArrayList<RegionKey> diffRegions;
	
	public DiffAntisenseGridColumn(WholeGenome g, String e) throws IOException { 
		expt = e; 
		name = String.format("diffantisense:%s", expt);
		diffRegions = new ArrayList<RegionKey>();
		
		g.loadIterators();
		Iterator<DataSegment> segs = new SerialIterator<DataSegment>(
				g.getWatsonSegments(), g.getCrickSegments());
		while(segs.hasNext()) { 
			DataSegment seg = segs.next();
			if(seg.isDifferential(expt)) {
				RegionKey key = new RegionKey(seg.chrom, seg.start, seg.end, seg.strand);
				diffRegions.add(key);
			}
		}
	}

	public boolean containsRegion(StrandedRegion region) {
		int start, end;
		char strand;
		if(region.getStrand() == '-') { 
			start = Math.max(region.getStart(), region.getEnd()-100);
			end = region.getEnd();
			strand = '+';
		} else { 
			start = region.getStart();
			end = Math.min(region.getStart(), region.getStart()+100);
			strand = '-';
		}
		RegionKey landingZone =
			new RegionKey(region.getChrom(), start, end, String.valueOf(strand));
		return hasOverlapping(landingZone);
	}
	
	public boolean hasOverlapping(RegionKey query) {
		for(RegionKey k : diffRegions) { 
			if(k.overlaps(query)) { 
				return true;
			}
		}
		return false;
	}

	public String name() {
		return name;
	}
}
