/*
 * Author: tdanford
 * Date: May 24, 2009
 */
package edu.mit.csail.cgs.sigma.tgraphs;

import java.util.Iterator;

import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;

public interface SigmaGraph extends TaggedGraph {

	public GeneKey lookupGene(String name);
	public DataSegment lookupSegment(String name);
	public TranscriptCall lookupTranscript(String name);
	
	public Iterator<GeneKey> findGenes(RegionKey query);
	public Iterator<DataSegment> findSegments(RegionKey query);
	public Iterator<TranscriptCall> findTranscripts(RegionKey query);
}
