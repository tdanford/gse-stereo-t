/*
 * Author: tdanford
 * Date: Oct 25, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos;

import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.utils.probability.NormalDistribution;

/**
 * A simple implementation of Dynamic Programming for Segmentation, with fitting segments that 
 * look a lot like Picard's "flat" segments.
 * 
 * Giorgos: I implemented this, both so that I could have some code with which to test my 
 * surrounding wrapper, visualization, and simulation system for segmentation -- but also 
 * in order to give you an example (if you needed it) of how the basic dynamic programming 
 * "might look".  This implementation is by *no* means optimized in a lot of ways that it should 
 * be -- it's way too slow and space-bloated to run on any "real" data, so it would need to be 
 * heavily optimized if we were using it for some real purpose.  It's also lacking all the major
 * features that we've talked about:
 *  
 * (1) The ability to fit multiple segment types
 * (2) The ability to simulataneously fit multiple channels of data.
 * 
 * @author tdanford
 * @date 10/25/2008
 */
public class SimpleSegmenter implements Segmenter {

	public static final Integer FIT = 0;
	public static final Double defaultProbSplit = 0.005;
	public static final Double defaultVarPenalty = 3.0;
	
	// This is (essentially) our prior probability on seeing any given segment split, and 
	// it functions as a penalty on the search for splits.  
	private Double probSplit;
	
	// This penalizes the "fit" of a segment, based on the variance of the fit itself.  
	private Double varPenalty;
	
	// This is the minimum length segment we can find, and should probably be >= 3.  
	private int minSegmentLength;
	
	public SimpleSegmenter() { 
		minSegmentLength = 3;
		probSplit = defaultProbSplit;
		varPenalty = defaultVarPenalty;
	}
	
	public SimpleSegmenter(int minSeg, double psplit) { 
		minSegmentLength = minSeg;
		probSplit = psplit;
		varPenalty = defaultVarPenalty;
	}

	/**
	 * The core implementation of the dynamic programming algorithm.
	 */
	public Collection<Segment> segment(InputData data) {
		Integer[] locations = data.locations();
		Double[][] values = data.values();
		
		assert values.length == 1;

		Integer[][] table = new Integer[locations.length][];
		Double[][][] params = new Double[locations.length][][];
		Double[][] scores = new Double[locations.length][];
		
		for(int i = 0; i < table.length; i++) { 
			// This is the length of the segments that we'll be considering at this 
			// iteration of the DP. 
			int segmentLength = i + 1;
			int numOffsets = locations.length-segmentLength+1;
			
			table[i] = new Integer[numOffsets];
			scores[i] = new Double[numOffsets];
			params[i] = new Double[numOffsets][];
			
			for(int j = 0; j < table[i].length; j++) {

				Double[] fitParams = fit(j, j+segmentLength, values);
				Double fitScore = scoreFit(j, j+segmentLength, fitParams, values);

				if(segmentLength <= minSegmentLength) { 
					
					// There's a minimum segment length -- if we're below it, we just fit the 
					// current segment, and move on.  There's no point in looking at possible 
					// splits below the minimum length. 

					table[i][j] = FIT;
					params[i][j] = fitParams;
					scores[i][j] = fitScore;
					
				} else {
					
					// Otherwise, we have to examine both possible cases of the dynamic 
					// programming -- either this segment is "fit", as is, or it should be 
					// split into the optimal explanation of two smaller segments.  (Because
					// we're working from smaller -> larger segments, those optimal fits will
					// already have been calculated).  
			
					Double bestScore = fitScore;
					int bestSplit = FIT;
					
					for(int k = j + minSegmentLength; k < j+segmentLength-minSegmentLength+1; k++) { 
						
						Double splitScore = scoreSplit(i, j, k, scores);
						
						if(splitScore > bestScore) { 
							bestSplit = k;
							bestScore = splitScore;
						}
					}
					
					table[i][j] = bestSplit;
					scores[i][j] = bestScore;
					params[i][j] = (bestSplit == FIT) ? fitParams : null;
				}
			}
		}
		
		return parseSegments(table, scores, params);
	}

	private Integer[] findLeft(int i, int j, int k) {
		assert k != FIT;
		//return new Integer[] { k-1, j };
		return new Integer[] { k-j-1, j };
	}
	
	private Integer[] findRight(int i, int j, int k) {
		assert k != FIT;
		//return new Integer[] { i-k, j+k };
		return new Integer[] { j+i-k, k };
	}

	/**
	 * Scores a potential fit on the region [j1, j2) of the data. This implementation is just 
	 * "fitting averages" to segments, so the params field will be two numbers (mean and variance)
	 * and the "fit" will just be the log-likelihood of the model on the data in this segment.    
	 * 
	 * @param j1
	 * @param j2
	 * @param params
	 * @param values
	 * @return
	 */
	private Double scoreFit(int j1, int j2, Double[] params, Double[][] values) {
		int channel = 0;
		NormalDistribution nd = new NormalDistribution(params[0], params[1]);
		Double sum = Math.log(1.0-probSplit);
		for(int j = j1; j < j2; j++) { 
			double value = values[channel][j];
			double logp = nd.calcLogProbability(value);
			sum += logp;
		}
		
		Double score = sum - varPenalty * params[1];
		
		return score;
	}
	
	/**
	 * Calculates the score of a split entry -- table[i][j] = k.  
	 * Right now, we assume that this is additive, but (in general) this isn't right, 
	 * and we need to add some penalty to the score for splitting.  (Note: a penalty
	 * is basically equivalent to a "prior on splitting.")  
	 * 
	 * @param i The length-1 of the segment to be scored.
	 * @param j The offset of the segment to be scored.
	 * @param k The location of the split (in 'absolute' coordinates)
	 * @param scores The table of pre-computed scores.
	 * @return The score of the split k at this position in the table.  
	 */
	private Double scoreSplit(int i, int j, int k, Double[][] scores) { 
		Integer[] left = findLeft(i, j, k);
		Integer[] right = findRight(i, j, k);
		
		Double score_left = scores[left[0]][left[1]];
		Double score_right = scores[right[0]][right[1]];
		Double penalty = Math.log(probSplit);
		
		return score_left + score_right + penalty;
	}
	
	/**
	 * Calculates an array of parameters, to fit the data in the index range [j1, j2) 
	 * 
	 * @param j1 The starting index of the fit (inclusive).
	 * @param j2 The ending index of the fit (exclusive).
	 * @param values  The array of measured (experimental) values.
	 * @return  An array of parameters for a fit segment in this location.  
	 */
	private Double[] fit(int j1, int j2, Double[][] values) {
		int channel = 0;
		
		// The parameters of the fit are going to be the mean and variance 
		// of the values in this segment.  
		
		Double sum = 0.0;
		for(int i = j1; i < j2; i++) { 
			sum += values[channel][i];
		}
		Double mean = sum / (double)Math.max(1, j2-j1);
		sum = 0.0;
		
		for(int i = j1; i < j2; i++) { 
			double diff = (values[channel][i] - mean);
			sum += diff*diff;
		}
		Double var = sum / (double)Math.max(1, j2-j1);
		
		return new Double[] { mean, var };
	}
	
	/**
	 * Takes the final, assembled table (and the corresponding table of parameters), and 
	 * walks backward from the top level, extracting all the separate fit segments.  
	 * 
	 * @param table  The table of parameters, built in segment() as part of the DP. 
	 * @param params  The table of fit-parameters, also built in segment()
	 * @return A collection of Segment objects represent the core "fit" segments in the table.
	 */
	private Collection<Segment> parseSegments(Integer[][] table, Double[][] scores, Double[][][] params) { 
		TreeSet<Segment> segs = new TreeSet<Segment>();
		
		// The "right" solution would be a recursive descent of the table, starting from the 
		// last (top) entry.  But because these tables can be rather larger, and because Java
		// doesn't implement proper tail recursion, we run the risk of exceeding the maximum 
		// stack depth when we recurse on teh height of the table.  So we implement an iterative
		// version instead...
		
		LinkedList<Integer[]> fitted = new LinkedList<Integer[]>();
		LinkedList<Integer[]> tosearch = new LinkedList<Integer[]>();
		tosearch.add(new Integer[] { table.length-1, 0 });
		
		while(!tosearch.isEmpty()) { 
			Integer[] p = tosearch.removeFirst();
			
			if(table[p[0]][p[1]] == FIT) {
				fitted.addLast(p);
			} else { 
				int i = p[0];
				int j = p[1];
				int k = table[i][j];

				Integer[] left = findLeft(i, j, k);
				Integer[] right = findRight(i, j, k);

				tosearch.add(left); 
				tosearch.add(right);
			}
		}
		
		for(Integer[] fit : fitted) { 
			int i = fit[0], j = fit[1];
			int length = i+1;
			Segment seg = new Segment(j, j+length-1, params[i][j]);
			segs.add(seg);
		}
		
		return segs;
	}
}

