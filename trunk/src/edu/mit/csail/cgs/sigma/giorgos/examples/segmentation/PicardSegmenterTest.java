package edu.mit.csail.cgs.sigma.giorgos.examples.segmentation;

import java.util.Collection;

import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.RegressionInputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.SegmentViz;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.SegmentVizFrame;
import edu.mit.csail.cgs.sigma.giorgos.PicardFlatSegmenter;


public class PicardSegmenterTest implements Runnable{

	InputData inputData;
	Integer[] locations;
	Double[][] values;
	Collection<Segment> fixedRealSegs;
	int start, end;
	Collection<Segment> predPicardSegs;
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PicardSegmenterTest obj = new PicardSegmenterTest();
		obj.run();
		obj.runFrame();
	}
	
	public void run()
	{
		Integer[] locs = {
				1,13,20,28,35,45,53,61,69,77,85,93,101,109,117,125,133,141,149,157,165,173,181,189,197,205,213,220,229,237,244,254,261,269,277,285,293,301,309,317,325,333,341,349,357,365,373,381,389,396,405,413,421,428,435,444,453,461,469,477,485,493,501,509,517,525,532,539,546,553,561,573,581,589,597,605,613,621,629,636,644,652,659,667,677,685,693,701,709,717,725,733,741,748,757,765,775,789,797,805,813,821,829,837,845,853,861,869,877,885,893,901,909,917,925,933,941,949,957,964,973,981,989,1005	
		};
		
		Double[][] vals = {{0.160000000000000,0.0900000000000000,0.290000000000000,0.0,-0.0200000000000000,0.970000000000000,-0.520000000000000,-0.700000000000000,-0.700000000000000,-1.19000000000000,-0.890000000000000,-0.800000000000000,-0.580000000000000,-0.0600000000000000,-0.0200000000000000,-0.700000000000000,-0.420000000000000,-0.140000000000000,-0.140000000000000,-0.680000000000000,0.120000000000000,0.0200000000000000,-0.230000000000000,-0.310000000000000,-0.800000000000000,-0.440000000000000,0.320000000000000,0.310000000000000,-0.410000000000000,0.0100000000000000,-0.580000000000000,0.330000000000000,-0.720000000000000,-0.730000000000000,-0.520000000000000,-0.950000000000000,-0.690000000000000,-1.01000000000000,-0.660000000000000,-0.570000000000000,-0.730000000000000,-0.610000000000000,-0.790000000000000,-0.440000000000000,-0.650000000000000,0.0400000000000000,-0.350000000000000,-0.290000000000000,-0.380000000000000,0.540000000000000,-1.04000000000000,-1.15000000000000,-0.650000000000000,-0.730000000000000,-0.970000000000000,-0.770000000000000,0.0100000000000000,-0.820000000000000,-1.0,-0.830000000000000,-0.370000000000000,-0.800000000000000,-0.880000000000000,-0.640000000000000,-0.540000000000000,0.180000000000000,-0.670000000000000,-0.660000000000000,-0.530000000000000,-0.630000000000000,-0.0100000000000000,-0.260000000000000,-0.660000000000000,-1.13000000000000,-0.450000000000000,-0.460000000000000,0.0900000000000000,-0.600000000000000,-0.780000000000000,-0.640000000000000,-0.510000000000000,-0.0600000000000000,-0.950000000000000,-1.31000000000000,-0.710000000000000,-1.12000000000000,-1.08000000000000,1.07000000000000,-0.850000000000000,-0.790000000000000,-0.570000000000000,-0.300000000000000,0.480000000000000,0.0100000000000000,-0.800000000000000,-0.770000000000000,-0.320000000000000,-1.11000000000000,-1.04000000000000,-1.06000000000000,-1.06000000000000,-0.970000000000000,-0.940000000000000,-1.05000000000000,-1.03000000000000,-1.32000000000000,-1.10000000000000,-0.850000000000000,-0.910000000000000,-1.27000000000000,-0.700000000000000,-0.690000000000000,-0.740000000000000,-1.13000000000000,-0.690000000000000,-1.05000000000000,-1.21000000000000,-1.21000000000000,-0.970000000000000,-1.10000000000000,-1.01000000000000,-1.04000000000000,-0.200000000000000,-0.970000000000000}};
		
		inputData = new RegressionInputData("7", "+", locs, vals);
		locations = inputData.locations();
		values = inputData.values();
		start = Math.max(0, locations[0]-1);
		end = locations[locations.length-1]+1;
	
		Integer[] k = {2, 4, 5, 6, 7, 9, 10, 15};
		PicardFlatSegmenter picardFlatSegmenter = new PicardFlatSegmenter(k);
		//picardFlatSegmenter.setPenaltyModel("lavielle", 0.49);
		picardFlatSegmenter.setPenaltyModel("bic", 0.49);
		long startPicard = System.currentTimeMillis();
		predPicardSegs = picardFlatSegmenter.segment(inputData);
		long endPicard = System.currentTimeMillis();

    	double periodPicard= (double)(endPicard - startPicard)/1000.0;
		
		System.out.println("\n");
		System.out.println("PicardFlatSegmenter took " + periodPicard + " seconds");
	}
	
	public void runFrame()
	{
		int numViz = 1;
		SegmentViz[] viz = new SegmentViz[numViz];
		for(int i = 0; i < viz.length; i++)
			viz[i] = new SegmentViz();

		for(int i = 0; i < viz.length; i++)
		{
			viz[i].setData(locations, values[0]);
	        viz[i].setBounds("chr01", "+", start, end);	
		}
		
		viz[0].setFitted(new FilterIterator<Segment,Segment>(new ChannelFilter(0), predPicardSegs.iterator()));
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


}//end of PicardSegmenterTest class