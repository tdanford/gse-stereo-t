package edu.mit.csail.cgs.sigma.giorgos.examples.segmentation;

import java.util.Collection;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.RegressionInputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.SimpleDynSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.SimpleSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.utils.probability.Gaussian;

public class TestSimpleDynSegmenter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Integer[] locations = new Integer[] {0, 1, 2, 3, 4, 5};
		Double[][] values = new Double[][] {{1.0, 3.0, 5.0, 3.0, 8.0, 4.0}};
		
		//compareTotalLogLikelihoods(values);
		
		InputData inputData = new RegressionInputData("chr01", "+", locations, values);
		
		Segmenter dynSegmenter = new SimpleDynSegmenter();
		long startDyn = System.currentTimeMillis();
		Collection<Segment> dynSegs = dynSegmenter.segment(inputData);
		long endDyn = System.currentTimeMillis();
		
		Segmenter segmenter = new SimpleSegmenter();
		long start = System.currentTimeMillis();
		Collection<Segment> segs = segmenter.segment(inputData);
		long end = System.currentTimeMillis();
		
		System.out.println("SimpleDynSegmenter");
		for(Segment seg:dynSegs)
			System.out.print(seg.toString());
		
		System.out.println("\n\nSimpleSegmenter");
		for(Segment seg:segs)
			System.out.print(seg.toString());
		
		
		double periodDyn= (double)(endDyn - startDyn)/1000.0;
		double period   = (double)(end - start)/1000.0;
		
		System.out.println("\n");
		System.out.println("SimpleDynSegmenter took " + periodDyn + " seconds");
		System.out.println("SimpleSegmenter took " + period + " seconds");

	}//end of main method
	
	public static void compareTotalLogLikelihoods(Double[][] values)
	{
		Double[] vals = values[0];
		Double prevMean = 0.0; 
		Double prevVar = 0.0;
		for(int i = 0; i < vals.length; i++)
		{
			int n = i +1;
			double sum = 0;
			for(int j = 0; j <= i; j++)
			{
				sum += vals[j];
			}
			sum /= n;
			double mean = sum;
			
			sum = 0;
			for(int j = 0; j <= i; j++)
			{
				Double diff = vals[j] - mean; 
				sum += diff*diff;
			}
			sum /= n;
			double var = sum;
				
			double totalScore = 0.0;
			for(int j = 0; j <= i; j++)
			{
				totalScore += Gaussian.calcLogProbability(vals[j], mean, var);;
			}
			
			double totalScore1 = 0.0;
			Gaussian nd = new Gaussian(mean, var);
			
			for(int j = 0; j <= i; j++)
			{
				totalScore1 += nd.calcLogProbability(vals[j]);
			}
			
			double value = vals[i];
			Double totalScoreDyn = Gaussian.dynCalcTotalLogProbability(value, n, mean, var, prevMean, prevVar);
			
			prevMean = mean;
			prevVar = var;
			
			System.out.println("regular total score: " + totalScore);
			System.out.println("object  total score: " + totalScore1);
			System.out.println("Dynamic total score: " + totalScoreDyn);
		}

		
	}//end of compareTotalLogLikelihoods method

}//end of TestSimpleDynSegmenter method
