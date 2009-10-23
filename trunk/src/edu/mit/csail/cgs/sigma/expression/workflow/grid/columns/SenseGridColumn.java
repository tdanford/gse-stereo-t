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
import edu.mit.csail.cgs.sigma.*;

public class SenseGridColumn implements GridColumn {
	
	private String name;
	private String strain, expt;
	private WholeGenome genome;
    private OverlappingRegionKeyFinder<DataSegment> finder;
    private Integer[] channels;
	
	public SenseGridColumn(WorkflowProperties props, String key, String expt) throws IOException {
		name = String.format("sense:%s", expt);
		this.expt = expt;
        this.strain = props.parseStrainFromKey(key);
		genome = WholeGenome.loadWholeGenome(props, key);
        genome.loadIterators();
        finder = new OverlappingRegionKeyFinder<DataSegment>(genome.getTranscribedSegments(expt));
        channels = props.getIndexing(key).findChannels(strain, expt);
	}

	public boolean containsRegion(StrandedRegion region) {
		int start, end, predictionPoint;
		if(region.getStrand() == '+') { 
			start = Math.max(region.getStart(), region.getEnd()-100);
			end = region.getEnd();
            predictionPoint = end;
		} else { 
			start = region.getStart();
			end = Math.min(region.getStart(), region.getStart()+100);
            predictionPoint = start;
		}
		StrandedRegion landingZone = 
			new StrandedRegion(region.getGenome(), region.getChrom(), start, end, region.getStrand());
        RegionKey landingZoneKey = 
            new RegionKey(region.getChrom(), start, end, String.valueOf(region.getStrand()));
		//return genome.hasOverlapping(expt, landingZone);
        
        Collection<DataSegment> over = finder.findOverlapping(landingZoneKey);
        for(DataSegment s : over) { 
            if(s.predicted(channels, predictionPoint) >= 3.0) { 
                return true; 
            }
        }

        return false;
	}

	public String name() {
		return name;
	}
}
