package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.ModelInputIterator;

public class WorkflowClusterReader extends ModelInputIterator<Cluster> {
	
	public WorkflowClusterReader() { 
		this(System.in);
	}
	
	public WorkflowClusterReader(File f) throws IOException { 
		this(new FileInputStream(f));
	}

	public WorkflowClusterReader(InputStream is) {
		super(new ModelInput.LineReader<Cluster>(Cluster.class, is));
	}
}
