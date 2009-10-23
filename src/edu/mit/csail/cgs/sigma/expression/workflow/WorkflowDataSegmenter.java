/*
 * Author: tdanford
 * Date: Mar 10, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential.DifferentialSpec;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;

public class WorkflowDataSegmenter extends ExpanderIterator<InputSegmentation,DataSegment> {

	public WorkflowDataSegmenter(WorkflowProperties ps, String strain, 
			Iterator<InputSegmentation> seger) { 
		super(new SegmentationDataExpander(ps, strain), seger);
	}
}

class SegmentationDataExpander implements Expander<InputSegmentation,DataSegment> {
	
	public SegmentationDataExpander(WorkflowProperties ps, String strain) {
	}

	public Iterator<DataSegment> execute(InputSegmentation a) {
		ArrayList<DataSegment> regs = new ArrayList<DataSegment>();
		Integer[] bps = breakpoints(a);
		for(int i = 0; i < bps.length-1; i++) {
			DataSegment seg = new DataSegment(a, bps[i], bps[i+1]);
			if(seg.dataLocations.length > 0 && !seg.isMissingChannel()) {
				//seg.calculateDifferential(spec);
				regs.add(seg);
			}
		}
		System.out.println(String.format("-> %d data-segments", regs.size()));
		return regs.iterator();
	}

	public Integer[] breakpoints(InputSegmentation a) { 
		Set<Integer> bps = new TreeSet<Integer>();
		for(Segment s : a.segments) { 
			bps.add(s.start);
			bps.add(s.end);
		}
		return bps.toArray(new Integer[0]);
	}
}

