/*
 * Author: tdanford
 * Date: May 6, 2009
 */
package edu.mit.csail.cgs.sigma.validation;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Generator;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.simulation.SimulatorGenerator;
import edu.mit.csail.cgs.sigma.expression.workflow.Workflow;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.assessment.TestSegmentationWorkflow;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.utils.models.Model;

public class SegmentationTestInstance implements TestInstance<SegmentationParameters> {

	private WorkflowProperties props;
	private String strain, exptName;
	private SimulatorGenerator lineGenerator;
	private SegmentComparison compare; 
	
	public SegmentationTestInstance(WorkflowProperties ps, String str, String expt, SimulatorGenerator ls, SegmentComparison comp) {
		props = ps;
		lineGenerator = ls;
		strain = str;
		compare = comp;
		exptName = expt;
	}

	public double evaluate(SegmentationParameters p) {
		Iterator<ProbeLine> probes = lineGenerator.execute();
		Workflow worker = new Workflow(props);
		Iterator<DataSegment> segs = worker.completeSegmentation(probes, strain, exptName, p);
		ArrayList<DataSegment> seglist = new ArrayList<DataSegment>();
		while(segs.hasNext()) { seglist.add(segs.next()); }
		//return compare.compare(lineGenerator.trueSegments(), seglist);
		return 0.0;
	} 
}
