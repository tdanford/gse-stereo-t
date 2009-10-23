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
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.SharingMultiChannelSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.FlatFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.LineFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.AllOrNothingSharingFactory;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharingFactory;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.models.Timer;
import edu.mit.csail.cgs.utils.models.Timing;

public class WorkflowSegmentation implements Iterator<InputSegmentation> {
	
	private Integer minimum;
	private Segmenter segmenter;
	private Iterator<FileInputData> inputItr;
	private Timer timer;

	public WorkflowSegmentation(int min, Timer t, 
			Segmenter seger, Iterator<FileInputData> ii) {
		
		minimum = min;
		segmenter = seger;
		inputItr = ii;
		timer = t;
	}
	
	public void setTimer(Timer t) { timer = t; }
	
	public void remove() { throw new UnsupportedOperationException(); }
	
	public boolean hasNext() { 
		return inputItr.hasNext();
	}
	
	public InputSegmentation next() { 
		FileInputData data = inputItr.next();
		InputSegmentation segmentation = null;
		System.out.println(String.format("Segmenting: %s:%d-%d:%s (%d probes)",
				data.chrom(), data.locations[0], data.locations[data.length()-1], data.strand(), data.length()));
		long millis = System.currentTimeMillis();
		
		if(data.flags().contains("REPEAT-TRANSCRIPT")) { 
			segmentation = new InputSegmentation(data, true);
		} else if(data.length() >= minimum) { 
			// Generate "real" segments...
			segmentation = new InputSegmentation(data, segmenter.segment(data));
		} else { 
			// fills in a default "flat" segment.
			segmentation = new InputSegmentation(data, false);
		}
		
		long duration = System.currentTimeMillis() - millis;
		double seconds = (double)duration / 1000.0;
		double pps = (double)seconds / (double)Math.max(1, data.length());
		System.out.println(String.format("\t%.3fs (%.5fs/probe)", seconds, pps));
		
		Timing t = new Timing(data.length(), seconds);
		if(timer != null) { timer.addTiming(t); }
		
		return segmentation;
	}
	
}
