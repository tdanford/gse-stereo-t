package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.ModelInputIterator;

public class WorkflowTranscriptReader extends ModelInputIterator<TranscriptCall> {
	
	public WorkflowTranscriptReader() { 
		this(System.in);
	}
	
	public WorkflowTranscriptReader(File f) throws IOException { 
		this(new FileInputStream(f));
	}

	public WorkflowTranscriptReader(InputStream is) {
		super(new ModelInput.LineReader<TranscriptCall>(TranscriptCall.class, is));
	}
}
