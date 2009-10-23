
package edu.mit.csail.cgs.sigma.giorgos.examples.segmentation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.RegressionInputData;

import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.MultiTypeSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.FlatFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.LineFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.SegmentFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharing;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.SegmentViz;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.SegmentVizFrame;

public class ExamineMultipleLineFittersExample {
	
	private Random rand;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Double[][] values = {{0.01, 0.002, 0.03,   
		/* y = x-2 */         0.8944, 2.1415, 2.9195, 4.0529, 5.0219, 5.9078, 6.7829, 7.9941, 8.8989, 10.0614, 11.0508, 12.1692,    
			                  0.001, 0.005, 0.01,
		/* y = (1/2)x-5 */    3.9881, 4.5051, 5.0221, 5.5392,
			                  0.02, 0.001, 0.03,
		/* y = -0.3x + 10 */  2.0992, 1.8258, 1.7082, 1.2869, 1.0390,
		                      0.01, 0.02
			                 }};
		
		Integer[] locations = new Integer[values[0].length];
		for(int i = 0; i < locations.length; i++)
			locations[i] = new Integer(i);
		
		InputData inputData = new RegressionInputData("chr01", "+", locations, values);
		
		int j1 = 3; int j2 = 14;
		Segment realSeg = new Segment(j1, j2, 2.3);
		Collection<Segment> realSegs = new ArrayList<Segment>();
		realSegs.add(realSeg);
		j1 = 18; j2 = j1+4-1;
		realSeg = new Segment(j1, j2, 2.3);
		realSegs.add(realSeg);
		j1 = 25; j2 = j1+5-1;
		realSeg = new Segment(j1, j2, 2.3);
		realSegs.add(realSeg);
		
		// Do manual segmentation
		Collection<Segment> predSegs = new ArrayList<Segment>();
		SegmentFitter lineFitter = new LineFitter(Math.log(.1), 2.0); 
		for(Segment seg:realSegs){
			int start = seg.start;
			int end   = seg.end;
			ParameterSharing sharing = new ParameterSharing(inputData.channels(), true);
			Double[] params = lineFitter.fit(start, end+1, inputData, sharing.getChannels(0));
			predSegs.add(new Segment(0, false, -1, start, end, params));
		}
		
		// Let MultiSegmentFitter do the segmentation
		double probSplit = .05;
		double probLine = .01;
		double probFlat = 1 -probSplit - probLine;
		int minSegmentLength = 2;
		Segmenter segFitter = new MultiTypeSegmenter(minSegmentLength,
				                                     probSplit,
				                                     new FlatFitter(Math.log(probFlat), 2.0),
				                                     new LineFitter(Math.log(probLine), 2.0));
		Collection<Segment> predSegs1 = segFitter.segment(inputData);
		
		
		// ManualFitter
		System.out.println("ManualFitter:" + "\n" +
				            "-----------");
		for(Segment predSeg:predSegs)
			System.out.println(predSeg.toString());

		// MultiSegmentFitter
		System.out.println("MultiSegmentFitter:" + "\n" +
				            "------------------");
		for(Segment predSeg:predSegs1)
			System.out.println(predSeg.toString());
		
		SegmentViz viz = new SegmentViz();
		viz.setData(locations, values[0]);
		viz.setSegments(realSegs);
		viz.setFitted(new FilterIterator<Segment, Segment>(
		                  new ChannelFilter(0),
		                  predSegs.iterator())
		              );
		//SegmentVizFrame vizFrame = new SegmentVizFrame(viz);
		
		SegmentViz viz1 = new SegmentViz();
		viz1.setData(locations, values[0]);
		viz1.setSegments(realSegs);
		viz1.setFitted(new FilterIterator<Segment, Segment>(
				           new ChannelFilter(0),
				           predSegs1.iterator())
		               );
			
		//SegmentVizFrame vizFrame1 = new SegmentVizFrame(viz1);
		
		/*
		 * If you want to load the abs Visualization, aka the one that takes into account the actual
		 * parameters of a region and not the ones that occur after shifting the region towards the 
		 * origin, type the following two commands 
		 * 
		 * SegmentAbsViz viz = new SegmentAbsViz();
		 * ...
		 * SegmentAbsVizFrame vizFrame = new SegmentAbsVizFrame(viz);
		 * 
		 * instead of:
		 * 
		 * SegmentViz viz = new SegmentViz();
		 * ...
		 * SegmentVizFrame vizFrame = new SegmentVizFrame(viz);
		 * 
		 */
		
		
		
	}//end of main method
	
	
private static class ChannelFilter implements Filter<Segment,Segment> {
		
		public ChannelFilter(int chan) {
			channel = chan;
		}
		private int channel;
		
		public Segment execute(Segment a) {
			return a.channel == channel ? a : null;
		}
	}//end of ChannelFilter class

	
}// end of ExamineLineFitterExample class

