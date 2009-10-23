/*
 * Author: tdanford
 * Date: Mar 11, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.util.*;
import java.io.*;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowDataSegmentReader;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;

public class DataSegmentExpander implements Expander<Region,DataSegment> {

	private OverlappingRegionFinder<DataRegion> regions;
	private Region lastRegion; 
	private ArrayList<DataSegment> lastSegments;
	
	public DataSegmentExpander(WorkflowProperties props, String strain, String key) throws IOException { 
		this(props.getSigmaProperties().getGenome(strain), 
			new File(props.getDirectory(), String.format("%s.datasegs", key)));
	}
	
	public DataSegmentExpander(Genome genome, File f) throws IOException {
		this(genome, new WorkflowDataSegmentReader(f));
	}
	
	public DataSegmentExpander(Genome genome, Iterator<DataSegment> segs) { 
		regions = new OverlappingRegionFinder<DataRegion>(
				new MapperIterator<DataSegment,DataRegion>(new DataRegionMapper(genome), 
						segs));
		lastRegion = null;
		lastSegments = null;
	}
	
	public Iterator<DataSegment> execute(Region r) { 
		if(lastRegion == null || !lastRegion.equals(r)) { 
			lastRegion = r;
			lastSegments = new ArrayList<DataSegment>();
			Collection<DataRegion> rs = regions.findOverlapping(r);
			for(DataRegion dr : rs) { 
				lastSegments.add(dr.segment);
			}
		}
		
		return lastSegments.iterator();
	}
}

class DataRegionMapper implements Mapper<DataSegment,DataRegion> {
	
	private Genome genome;
	
	public DataRegionMapper(Genome g) { genome  = g; }
	
	public DataRegion execute(DataSegment s) { 
		return new DataRegion(genome, s); 
	}
}

class DataRegion extends Region {
	
	public DataSegment segment;
	
	public DataRegion(Genome g, DataSegment ds) { 
		super(g, ds.chrom, ds.start, ds.end);
		segment = ds;
	}
}