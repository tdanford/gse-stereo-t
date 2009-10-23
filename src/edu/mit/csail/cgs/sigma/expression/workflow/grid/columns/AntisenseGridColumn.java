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

public class AntisenseGridColumn implements GridColumn {
	
	private String name;
	private String expt;
	private WholeGenome genome;
	
	public AntisenseGridColumn(WorkflowProperties props, String key, String expt) throws IOException {
		name = String.format("antisense:%s", expt);
		this.expt = expt; 
		genome = WholeGenome.loadWholeGenome(props, key);
	}
	
	public AntisenseGridColumn(WholeGenome g, String e) { 
		expt = e; 
		name = String.format("antisense:%s", expt);
		genome = g;		
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
		StrandedRegion landingZone = 
			new StrandedRegion(region.getGenome(), region.getChrom(), start, end, strand);
		return genome.hasOverlapping(expt, landingZone);
	}

	public String name() {
		return name;
	}
}
