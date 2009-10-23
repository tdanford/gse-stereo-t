package edu.mit.csail.cgs.sigma.giorgos.examples.segmentation;

import java.util.Collection;

import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.RegressionInputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.FixedSegmentGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.RandomInputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.RandomSegmentGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.SimpleInputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.SegmentViz;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.SegmentVizFrame;
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.SimpleSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.SimpleDynSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.giorgos.PicardFlatSegmenter;

public class CompareSimpleSegmenters implements Runnable{
	
	RandomSegmentGenerator fsg; 
	RandomInputGenerator rig;
	InputData inputData;
	Integer[] locations;
	Double[][] values;
	Collection<Segment> fixedRealSegs;
	String chrom, strand;
	int start, end;
	Collection<Segment> predSimpleSegs;
	Collection<Segment> predSimpleDynSegs;
	Collection<Segment> predPicardSegs;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CompareSimpleSegmenters obj = new CompareSimpleSegmenters();
		obj.run();
		//InputData inputData = new RegressionInputData(obj.locations, obj.values);
		
		obj.runFrame();
	}//end of main method
	
	public void run()
	{
		
		chrom = "1"; strand = "+";
		fsg = new FixedSegmentGenerator(3, 17, 17, 25, 37, 30, 100, 150, 200, 300);		
		rig = new SimpleInputGenerator(fsg, 500, 1, chrom, strand);
		
		//fsg = new FixedSegmentGenerator(3, 17, 17, 25, 37, 30);		
		//rig = new SimpleInputGenerator(fsg, 100, 1);
		
		
		rig.generate();
		inputData = rig.inputData();
		
		locations = inputData.locations();
		values = inputData.values();
		fixedRealSegs = rig.segments();
		start = Math.max(0, locations[0]-1);
		end = locations[locations.length-1]+1;
		
		// Compare Segmenters
		Segmenter simpleSegmenter = new SimpleSegmenter();
		long startSimple = System.currentTimeMillis();
		predSimpleSegs = simpleSegmenter.segment(inputData);
		long endSimple = System.currentTimeMillis();
		
		Segmenter simpleDynSegmenter = new SimpleDynSegmenter();
		long startDyn = System.currentTimeMillis();
		predSimpleDynSegs = simpleDynSegmenter.segment(inputData);
		long endDyn = System.currentTimeMillis();
		
		Integer[] k = {2, 4, 5, 6, 7, 9, 10, 15};
		PicardFlatSegmenter picardFlatSegmenter = new PicardFlatSegmenter(k);
		picardFlatSegmenter.setPenaltyModel("lavielle", 0.49);
		long startPicard = System.currentTimeMillis();
		predPicardSegs = picardFlatSegmenter.segment(inputData);
		long endPicard = System.currentTimeMillis();


		double period   = (double)(endSimple - startSimple)/1000.0;
		double periodDyn= (double)(endDyn - startDyn)/1000.0;
		double periodPicard= (double)(endPicard - startPicard)/1000.0;
		
		System.out.println("\n");
		System.out.println("SimpleDynSegmenter took " + periodDyn + " seconds");
		System.out.println("SimpleSegmenter took " + period + " seconds");
		System.out.println("PicardFlatSegmenter took " + periodPicard + " seconds");

		
	}
	
	public void runFrame()
	{
		int numViz = 3;
		SegmentViz[] viz = new SegmentViz[numViz];
		for(int i = 0; i < viz.length; i++)
			viz[i] = new SegmentViz();

		for(int i = 0; i < viz.length; i++)
		{
			viz[i].setData(locations, values[0]);
	        viz[i].setBounds(chrom, strand, start, end);
			viz[i].setSegments(fixedRealSegs);
			
		}
		
		viz[0].setFitted(new FilterIterator<Segment,Segment>(new ChannelFilter(0), predSimpleSegs.iterator()));
		viz[1].setFitted(new FilterIterator<Segment,Segment>(new ChannelFilter(0), predSimpleDynSegs.iterator()));
		viz[2].setFitted(new FilterIterator<Segment,Segment>(new ChannelFilter(0), predPicardSegs.iterator()));
		
		SegmentVizFrame frame = new SegmentVizFrame(this, null, viz);
		
	}
	
	private static class ChannelFilter implements Filter<Segment,Segment> {

		public ChannelFilter(int chan) { 
			channel = chan;
		}
		private int channel;

		public Segment execute(Segment a) {
			return a.channel == channel ? a : null;
		} 
	}

}//end of CompareSimpleSegmenters class
