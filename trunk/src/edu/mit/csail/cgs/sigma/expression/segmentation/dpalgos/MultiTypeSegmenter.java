/*
 * Author: tdanford
 * Date: Oct 29, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos;

import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.SegmentFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharing;
import edu.mit.csail.cgs.utils.probability.NormalDistribution;

/**
 * 
 * @author tdanford
 * @date 10/29/2008
 */
public class MultiTypeSegmenter implements Segmenter {

	public static final Integer FIT = 0;
	
	public static final Double defaultProbSplit = 0.005;
	
	// This is (essentially) our prior probability on seeing any given segment split, and 
	// it functions as a penalty on the search for splits.  
	private Double probSplit;
	
	// This is the minimum length segment we can find, and should probably be >= 3.  
	private int minSegmentLength;

	private Map<Integer,SegmentFitter> fitters;
	
	public MultiTypeSegmenter(SegmentFitter... sfs) { 
		minSegmentLength = 3;
		probSplit = defaultProbSplit;
		fitters = new HashMap<Integer,SegmentFitter>();
		for(int i = 0; i < sfs.length; i++) { 
			fitters.put(sfs[i].type(), sfs[i]);
		}
		
		if(fitters.size() < 1) { 
			throw new IllegalArgumentException("No segment fitters were given."); 
		}
	}
	
	public MultiTypeSegmenter(int minSeg, double psplit, SegmentFitter... sfs) {
		this(sfs);
		minSegmentLength = minSeg;
		probSplit = psplit;
	}

	/**
	 * The core implementation of the dynamic programming algorithm.
	 */
	public Collection<Segment> segment(InputData data) {
		
		Integer[] locations = data.locations();
		Double[][] values = data.values();

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
				
				BestFit bf = findBestFit(j, j+segmentLength, data);

				Double[] fitParams = bf.params;
				Double fitScore = bf.score;
				Integer fitType = bf.type;

				if(segmentLength <= minSegmentLength) { 
					
					// There's a minimum segment length -- if we're below it, we just fit the 
					// current segment, and move on.  There's no point in looking at possible 
					// splits below the minimum length. 

					table[i][j] = fitType;
					params[i][j] = fitParams;
					scores[i][j] = fitScore;
					
				} else {
					
					// Otherwise, we have to examine both possible cases of the dynamic 
					// programming -- either this segment is "fit", as is, or it should be 
					// split into the optimal explanation of two smaller segments.  (Because
					// we're working from smaller -> larger segments, those optimal fits will
					// already have been calculated).  
			
					Double bestScore = fitScore;
					int bestSplit = fitType;
					
					for(int k = j + minSegmentLength; k < j+segmentLength-minSegmentLength+1; k++) { 
						
						Double splitScore = scoreSplit(i, j, k, scores);
						
						if(splitScore > bestScore) { 
							bestSplit = k;
							bestScore = splitScore;
						}
					}
					
					table[i][j] = bestSplit;
					scores[i][j] = bestScore;
					params[i][j] = (bestSplit <= FIT) ? fitParams : null;
				}
			}
		}
		
		return parseSegments(table, scores, params);
	}
	
	private BestFit findBestFit(int j1, int j2, InputData data) { 
		Double bestScore = null;
		Integer bestType = null;
		Double[] bestParams = null;
		
		ParameterSharing sharing = new ParameterSharing(data.channels(), true);
		
		for(Integer type : fitters.keySet()) { 
			SegmentFitter fitter = fitters.get(type);
			Double[] params = fitter.fit(j1, j2, data, sharing.getChannels(0));
			Double score = fitter.score(j1, j2, params, data, sharing.getChannels(0));
			
			if(bestScore == null || score > bestScore) { 
				bestType = type;
				bestScore = score;
				bestParams = params;
			}
		}
		
		return new BestFit(bestType, bestScore, bestParams);
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
	
	private Double scoreSplit(int i, int j, int k, Double[][] scores) { 
		Integer[] left = findLeft(i, j, k);
		Integer[] right = findRight(i, j, k);
		
		Double score_left = scores[left[0]][left[1]];
		Double score_right = scores[right[0]][right[1]];
		Double penalty = Math.log(probSplit);
		
		return score_left + score_right + penalty;
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
			
			if(table[p[0]][p[1]] <= FIT) {
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
			int type = table[i][j];
			int length = i+1;
			Segment seg = new Segment(0, false, type, j, j+length-1, params[i][j]);
			segs.add(seg);
		}
		
		return segs;
	}
	
	private class BestFit { 

		public Integer type;
		public Double score;
		public Double[] params; 
		
		public BestFit(Integer t, Double s, Double[] p) { 
			type = t;
			score = s;
			params = p;
		}
	}
}


