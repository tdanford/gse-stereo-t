/*
 * Author: tdanford
 * Date: May 6, 2009
 */
package edu.mit.csail.cgs.sigma.validation;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Generator;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.simulation.SimulatorGenerator;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.workflow.Workflow;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.assessment.TestSegmentationWorkflow;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.models.Model;

public class TranscriptTestInstance implements TestInstance<TranscriptionParameters> {

	private WorkflowProperties props;
	private String strain;
	private SimulatorGenerator lineGenerator;
	private SegmentationParameters sparams;
	private String expt;
	private TranscriptComparison compare; 
	
	public TranscriptTestInstance(WorkflowProperties ps, String str, SimulatorGenerator ls, SegmentationParameters sp, String e, Integer ch, TranscriptComparison comp) {
		props = ps;
		lineGenerator = ls;
		strain = str;
		compare = comp;
		sparams = sp;
		expt = e;
	}

	public double evaluate(TranscriptionParameters p) {
		Iterator<ProbeLine> probes = lineGenerator.execute();
		Workflow worker = new Workflow(props);
		Iterator<TranscriptCall> calls = worker.completeCalling(probes, strain, sparams, p, expt);
		ArrayList<TranscriptCall> calllist = new ArrayList<TranscriptCall>();
		while(calls.hasNext()) { calllist.add(calls.next()); }
		return compare.compare(lineGenerator.trueTranscripts(), calllist);
	} 
}
