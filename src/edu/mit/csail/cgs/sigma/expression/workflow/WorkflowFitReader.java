package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFit;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.ModelInputIterator;

public class WorkflowFitReader extends ModelInputIterator<TAFit> {
	
	public WorkflowFitReader() { 
		this(System.in);
	}
	
	public WorkflowFitReader(File f) throws IOException { 
		this(new FileInputStream(f));
	}

	public WorkflowFitReader(InputStream is) {
		super(new ModelInput.LineReader<TAFit>(TAFit.class, is));
	}
}
