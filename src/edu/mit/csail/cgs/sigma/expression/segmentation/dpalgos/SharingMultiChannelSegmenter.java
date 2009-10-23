/*
 * Author: tdanford
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos;

import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.SegmentFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharing;
import edu.mit.csail.cgs.utils.probability.NormalDistribution;

/**
 * 
 * @author tdanford
 * @date Feb 14, 2009
 */
public class SharingMultiChannelSegmenter implements Segmenter {
	
	public static final Integer FIT = 0;
	
	// This is (essentially) our prior probability on seeing any given segment split, and 
	// it functions as a penalty on the search for splits.  
	private Double probSplit;
	
	// This is the minimum length segment we can find, and should probably be >= 3.  
	private int minSegmentLength;

	private Map<Integer,SegmentFitter> fitters;
	private ParameterSharing[] sharing;
	
	public SharingMultiChannelSegmenter(SegmentationParameters params, 
			Collection<ParameterSharing> shares, 
			SegmentFitter... sfs) {
		
		minSegmentLength = params.minSegmentLength;
		probSplit = params.probSplit;
		
		sharing = shares.toArray(new ParameterSharing[0]);
		//if(sharing.length != 2) { throw new IllegalArgumentException(); }
		
		fitters = new HashMap<Integer,SegmentFitter>();
		for(int i = 0; i < sfs.length; i++) { 
			fitters.put(sfs[i].type(), sfs[i]);
		}
		
		if(fitters.size() < 1) { 
			throw new IllegalArgumentException("No segment fitters were given."); 
		}
	}
	
	/**
	 * The core implementation of the dynamic programming algorithm.
	 */
	public Collection<Segment> segment(InputData inputData) {
		
		Integer[] locations = inputData.locations();

		Integer[][] table = new Integer[locations.length][];
		BestFit[][] params = new BestFit[locations.length][];
		Double[][] scores = new Double[locations.length][];
		
		for(int i = 0; i < table.length; i++) { 
			
			// This is the length of the segments that we'll be considering at this 
			// iteration of the DP. 
			int segmentLength = i + 1;
			int numOffsets = locations.length-segmentLength+1;
			
			table[i] = new Integer[numOffsets];
			scores[i] = new Double[numOffsets];
			params[i] = new BestFit[numOffsets];
			
			for(int j = 0; j < table[i].length; j++) {
				
				BestFit bf = findBestFit(j, j+segmentLength, inputData);

				Double fitScore = bf.score;
				Integer fitType = FIT;

				if(segmentLength <= minSegmentLength) { 
					
					// There's a minimum segment length -- if we're below it, we just fit the 
					// current segment, and move on.  There's no point in looking at possible 
					// splits below the minimum length. 

					table[i][j] = fitType;
					params[i][j] = bf;
					scores[i][j] = fitScore;
					
				} else {
					
					// Otherwise, we have to examine both possible cases of the dynamic 
					// programming -- either this segment is "fit", as is, or it should be 
					// split into the optimal explanation of two smaller segments.  (Because
					// we're working from smaller -> larger segments, those optimal fits will
					// already have been calculated).  
			
					Double bestScore = fitScore;
					int bestSplit = fitType;
					
					for(int k = j + minSegmentLength; 
							k < j+segmentLength-minSegmentLength+1; 
							k++) { 
						
						Double splitScore = scoreSplit(i, j, k, scores);
						
						if(splitScore > bestScore) { 
							bestSplit = k;
							bestScore = splitScore;
						}
					}
					
					table[i][j] = bestSplit;
					scores[i][j] = bestScore;
					params[i][j] = (bestSplit == FIT) ? bf : null;
				}
			}
		}
		
		return parseSegments(inputData.channels(), table, scores, params);
	}
	
	private BestFit findBestFit(int j1, int j2, InputData data) {
		BestFit bestFit = null;
		Double bestScore = null;

		BestFit fit0 = new BestFit(j1, j2, data, sharing[0], false);
		Double score0 = fit0.score + sharing[0].getWeight();

		if(sharing.length > 1) { 
			BestFit fit1 = new BestFit(j1, j2, data, sharing[1], true);
			Double score1 = fit1.score + sharing[1].getWeight();

			if(score1 >= score0) { 
				return fit1;
			} else { 
				return fit0;
			}
		} else { 
			return fit0; 
		}
	}
	
	public Double sizeScore(int len) { 
		return 0.0;
	}

	private Integer[] findLeft(int i, int j, int k) {
		assert k != FIT;
		return new Integer[] { k-j-1, j };
	}
	
	private Integer[] findRight(int i, int j, int k) {
		assert k != FIT;
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
	private Collection<Segment> parseSegments(int channels, Integer[][] table, Double[][] scores, BestFit[][] params) {
		TreeSet<Segment> segs = new TreeSet<Segment>();
		
		// The "right" solution would be a recursive descent of the table, starting from the 
		// last (top) entry.  But because these tables can be rather larger, and because Java
		// doesn't implement proper tail recursion, we run the risk of exceeding the maximum 
		// stack depth when we recurse on teh height of the table.  So we implement an iterative
		// version instead...
		
		LinkedList<Integer[]> fitted = new LinkedList<Integer[]>();
		LinkedList<Integer[]> tosearch = new LinkedList<Integer[]>();
		tosearch.add(new Integer[] { table.length-1, 0 });

		assert table.length > 0;
		
		while(!tosearch.isEmpty()) { 
			Integer[] p = tosearch.removeFirst();
			
			assert p[0] >= 0;
			assert p[1] >= 0;

			if(table[p[0]][p[1]] <= FIT) {
				fitted.addLast(p);
			} else { 
				int i = p[0];
				int j = p[1];
				int k = table[i][j];

				Integer[] left = findLeft(i, j, k);
				Integer[] right = findRight(i, j, k);
				
				assert left[0] >= 0; 
				assert left[1] >= 0;
				assert right[0] >= 0; 
				assert right[1] >= 0;

				tosearch.add(left); 
				tosearch.add(right);
			}
		}

		for(Integer[] fit : fitted) { 
			int i = fit[0], j = fit[1];
			//int type = table[i][j];
			int length = i+1;
			
			for(int k = 0; k < channels; k++) {
				int pk = params[i][j].shared ? 0 : k;

				assert i >= 0; 
				assert i < params.length;
				assert j >= 0; 
				assert j < params[i].length;
				assert pk >= 0; 
				assert pk < params[i][j].types.length;
				
				Integer type = params[i][j].types[pk];
				
				assert type != null;
				
				Segment seg = 
					new Segment(k, params[i][j].shared, type, j, j+length-1, 
							params[i][j].params[pk]);
				//System.out.println(String.format("-> %s", seg.toString()));
				segs.add(seg);
			}
		}
		
		return segs;
	}
	
	private class BestFit { 
		
		public boolean shared;
		public Integer[] types;
		public Double[][] params;
		public Double score;
		
		public BestFit(int j1, int j2, 
				InputData data, ParameterSharing sharing, boolean shar) {
			score = 0.0;

			int numChannels = data.channels();
			
			types = new Integer[numChannels];
			params = new Double[numChannels][];
			shared = shar;

			for(int p = 0; p < sharing.getNumPatterns(); p++) { 
				Integer[] channels = sharing.getChannels(p);
				
				Double bestScore = null;
				Integer bestType = null;
				Double[] bestParams = null;

				for(Integer type : fitters.keySet()) { 
					SegmentFitter fitter = fitters.get(type);
					Double[] params = fitter.fit(j1, j2, data, channels);
					Double score = fitter.score(j1, j2, params, data, channels);

					if(bestScore == null || score > bestScore) { 
						bestType = type;
						bestScore = score;
						bestParams = params;
					}
				}

				score += bestScore;
				for(int ch : channels) {
					types[ch] = bestType;
					params[ch] = bestParams;					
				}
			}
			
		}
	}
}


