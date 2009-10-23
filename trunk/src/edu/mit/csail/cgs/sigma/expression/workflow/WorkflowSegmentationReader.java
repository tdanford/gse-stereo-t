/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.RegressionInputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.MultiChannelSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.FlatFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.LineFitter;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.ModelInputIterator;

public class WorkflowSegmentationReader extends ModelInputIterator<InputSegmentation> {
	
	public WorkflowSegmentationReader() { 
		this(System.in);
	}
	
	public WorkflowSegmentationReader(File f) throws IOException { 
		this(new FileInputStream(f));
	}

	public WorkflowSegmentationReader(InputStream is) {
		super(new ModelInput.LineReader<InputSegmentation>(InputSegmentation.class, is));
	}
}
