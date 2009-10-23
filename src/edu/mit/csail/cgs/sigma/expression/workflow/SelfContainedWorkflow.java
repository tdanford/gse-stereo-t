/*
 * Author: tdanford
 * Date: Apr 27, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.JSONOutputIterator;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;

public class SelfContainedWorkflow {
	
	public static void main(String[] args) {
		 if(args.length > 0) { 
			 for(int i = 0; i < args.length; i++) { 
				 process(args[i]);
			 }
		 } else { 
			 process("test_plus", "matalpha");
			 process("test_negative", "matalpha");
		 }
	}
	
	public static void process(String workflowName, String... expts) {
		WorkflowProperties props = new WorkflowProperties();
		
		String key = props.keyFromWorkflowName(workflowName);
		String strain = props.parseStrainFromKey(key);

		WorkflowIndexing index = props.getIndexing(key);
		if(expts==null || expts.length < 1) { 
			expts = index.getExpts(strain);
		}
		
		SegmentationParameters sparams = props.getDefaultSegmentationParameters();
		TranscriptionParameters tparams = props.getDefaultTranscriptionParameters();
		
		sparams.doHRMA = false;
		
		String filename = String.format("%s.data", workflowName);
		File input = new File(props.getDirectory(), filename);
		
		Workflow worker = new Workflow(props);
		
		File dsegOutput = new File(props.getDirectory(), 
				String.format("%s.datasegs", workflowName));
		
		try {
			Iterator<ProbeLine> probes = new WorkflowDataLoader(input);
			
			for(String expt : expts) {
				IteratorCacher<DataSegment> segs = 
					new IteratorCacher<DataSegment>(
							new JSONOutputIterator<DataSegment>(
									worker.completeSegmentation(probes, key, expt, sparams), 
									dsegOutput));

				File transcriptOutput = new File(props.getDirectory(), 
						String.format("%s_%s.transcripts", workflowName, expt));

				Iterator<TranscriptCall> calls = 
					new JSONOutputIterator<TranscriptCall>(
							worker.completeCalling(
									segs.iterator(), key, tparams, expt),
							transcriptOutput);
				int c = 0;
				while(calls.hasNext()) { 
					calls.next();
					c+=1;
				}
				
				System.out.println(String.format("Expt: %s -> %d calls", expt, c));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
