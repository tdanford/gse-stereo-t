package edu.mit.csail.cgs.sigma.giorgos.examples;

import edu.mit.csail.cgs.sigma.expression.segmentation.RegressionInputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.giorgos.PicardFlatSegmenter;

import edu.mit.csail.cgs.utils.probability.NormalDistributionGio;

public class FlatPicardSegmenterTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Integer[] locs = {1,2,3,4,5,6,7};
		Double[][] vals = {{2.0, 3.0, 3.0, 4.0, 5.0, 2.0, 1.0}};
		
		int n = vals[0].length;
		
		for(int i = 0; i < n; i++)
		{
			for(int j = 0; j < n-i; j++)
			{
				double sum = 0.0;
				for(int k = j; k < j + i +1; k++)
				{
					double val = vals[0][k];
					sum += val;
				}
				sum /= i+1;
				double mean = sum;
				
				sum = 0.0;
				for(int k = j; k < j + i +1; k++)
				{
					double val = vals[0][k];
					sum += (val-mean)*(val-mean);
				}
				sum /= i+1;
				double var = sum;
				
				sum = 0.0;
				for(int k = j; k < j + i +1; k++)
				{
					double val = vals[0][k];
					sum += NormalDistributionGio.calcLogProbability(val, mean, var);
				}
				double totalLogProb = sum;
				
				int foo = 3;
			}
		}
		
		
		
		InputData inputData = new RegressionInputData("1", "+", locs, vals);
		
		Integer[] k_arr = {2, 5, 3};
		PicardFlatSegmenter segmenter = new PicardFlatSegmenter(k_arr, 0.0); 
		segmenter.setPenaltyModel("lavielle", 0.5);
		segmenter.segment(inputData);

	}

}
