/*
 * Author: tdanford
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.SegmentFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharing;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.*;
import edu.mit.csail.cgs.utils.probability.NormalDistribution;

/**
 * @author tdanford
 */
public class SteinmetzSegmenter implements Segmenter {
	
	public static void main(String[] args) { 
		SteinmetzProperties props = new SteinmetzProperties();
		WorkflowProperties wprops = new WorkflowProperties();
		
		SteinmetzPackager packager = new SteinmetzPackager(props);
		
		SegmentationParameters segProps = wprops.getDefaultSegmentationParameters();
		SteinmetzSegmenter segmenter = new SteinmetzSegmenter(segProps);
		
		WorkflowSegmentation wseg = new WorkflowSegmentation(
				3, null, segmenter, packager.data()
				);
		
		WorkflowDataSegmenter wdseg = new WorkflowDataSegmenter(
				wprops, "s288c", wseg
				);
		
		File output = new File(wprops.getDirectory(), "steinmetz.datasegs");
		try { 
			PrintStream ps = new PrintStream(new FileOutputStream(output));
			int count = 0;
			while(wdseg.hasNext()) { 
				DataSegment dseg = wdseg.next();
				ps.println(dseg.asJSON().toString());
				System.out.println(String.format("Segmented: %s:%d-%d:%s", dseg.chrom, dseg.start, dseg.end, dseg.strand));
				count += 1;
			}
			ps.close();
			System.out.println(String.format("Output: %d segments", count));
		} catch(IOException e) { 
			e.printStackTrace();
		}
	}
	
	public static final Integer FIT = 0;
	
	private SegmentationParameters pms;
	private ProgressiveFitter fitter;
	
	public SteinmetzSegmenter(SegmentationParameters p) {
		pms = p;
		//fitter = new SteinmetzFitter();
		fitter = new ProgressiveFitter();
	}
	
	/**
	 * The core implementation of the dynamic programming algorithm.
	 */
	public Collection<Segment> segment(InputData inputData) {
		
		Integer[] locations = inputData.locations();

		Integer[][] table = new Integer[locations.length][];
		BestFit[][] params = new BestFit[locations.length][];
		Double[][] scores = new Double[locations.length][];
		
		fitter.setData(inputData);
		
		for(int i = 0; i < table.length; i++) { 
			
			// This is the length of the segments that we'll be considering at this 
			// iteration of the DP. 
			int segmentLength = i + 1;
			int numOffsets = locations.length-segmentLength+1;
			
			fitter.setLength(segmentLength);
			
			//System.out.println(String.format("Segment Length: %d", segmentLength));
			//System.out.println(String.format("\t# Elements: %d", numOffsets));
			
			table[i] = new Integer[numOffsets];
			scores[i] = new Double[numOffsets];
			params[i] = new BestFit[numOffsets];
			
			for(int j = 0; j < table[i].length; j++) {
				
				BestFit bf = findBestFit(j, j+segmentLength, inputData);

				Double fitScore = bf.score;
				Integer fitType = FIT;

				if(segmentLength <= pms.minSegmentLength) { 
					
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
					
					for(int k = j + pms.minSegmentLength; 
							k < j+segmentLength-pms.minSegmentLength+1; 
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

				/*
				if(j > 0) { 
					if(j % 1000 == 0) { 
						System.out.print("."); System.out.flush(); 
					}
					
					if(j % 10000 == 0) { 
						System.out.print(String.format("(%dk)", j/1000));
						System.out.flush();
					}
				}
				*/
			}
			
			//System.out.println();
		}
		
		return parseSegments(table, scores, params);
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
		Double penalty = Math.log(pms.probSplit);
		
		return score_left + score_right + penalty;
	}
	
	public BestFit findBestFit(int j1, int j2, InputData data) { 
		BestFit f = new BestFit(j1, j2, data);
		return f;
	}
	
	/**
	 * Takes the final, assembled table (and the corresponding table of parameters), and 
	 * walks backward from the top level, extracting all the separate fit segments.  
	 * 
	 * @param table  The table of parameters, built in segment() as part of the DP. 
	 * @param params  The table of fit-parameters, also built in segment()
	 * @return A collection of Segment objects represent the core "fit" segments in the table.
	 */
	private Collection<Segment> parseSegments(Integer[][] table, Double[][] scores, BestFit[][] params) {
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
			int length = i+1;
			
			Segment seg = 
				new Segment(0, false, Segment.LINE, j, j+length-1, 
						params[i][j].params);
			segs.add(seg);
		}

		return segs;
	}
	
	private class BestFit { 
		
		public Double[] params;
		public Double score;
		
		public BestFit(int j1, int j2, InputData data) {
			params = fitter.fit(j1, j2, data, null);
			score = fitter.score(j1, j2, params, data, null);
		}
	}
}

class ProgressiveFitter implements SegmentFitter {
	
	private int currentLength;
	private InputData data;
	private double[] means;
	private double[] vars;
	
	public ProgressiveFitter() { 
	}
	
	public void setData(InputData d) { 
		data = d;
		currentLength = -1;
		means = vars = null;
	}
	
	public void setLength(int len) { 
		if(currentLength != len) {
			
			System.out.print(String.format("%d ", len)); System.out.flush();
			
			currentLength = len;
			Double[][] vals = data.values();
			means = new double[vals[0].length-currentLength+1];
			vars = new double[vals[0].length-currentLength+1];
			
			//System.out.println(String.format("\tCalculating Means (%d)", vals[0].length));
			double sum = 0.0;
			for(int i = 0; i < vals[0].length; i++) { 
				if(i >= currentLength) { 
					sum -= vals[0][i-currentLength];
				}

				sum += vals[0][i];

				if(i >= currentLength-1) { 
					means[i-currentLength+1] = sum / (double)currentLength;
				}
				
				/*
				if(i > 0) { 
					if(i % 10000 == 0) { 
						System.out.print("."); System.out.flush();
					}
					if(i % 100000 == 0) { 
						System.out.print(String.format("(%dk)", i/1000));
						System.out.flush();
					}
				}
				*/
			}
			//System.out.println();

			//System.out.println(String.format("\tCalculating Variances (%d)", vals[0].length));
			LinkedList<Double[]> partialVars = new LinkedList<Double[]>();
			for(int i = 0; i < vals[0].length; i++) {
				double v = vals[0][i];

				if(i >= currentLength) {  
					partialVars.removeFirst();
				}
				
				partialVars.addLast(new Double[] { 0.0 });
				
				int j = partialVars.size()-1;
				for(Double[] p : partialVars) { 
					int idx = i - j;
					if(idx >= 0 && idx < means.length) { 
						double m = means[i-j];
						double diff = v-m;
						p[0] += (diff*diff);
					}
					j--;
				}
				
				if(i >= currentLength-1) { 
					Double[] first = partialVars.getFirst();
					vars[i-currentLength+1] = first[0] / (double)currentLength;
				}

				/*
				if(i > 0) { 
					if(i % 10000 == 0) { 
						System.out.print("."); System.out.flush();
					}
					if(i % 100000 == 0) { 
						System.out.print(String.format("(%dk)", i/1000));
						System.out.flush();
					}
				}
				*/
			}
			//System.out.println();
		}
	}

	public Double[] fit(int j1, int j2, InputData data, Integer[] channels) {
		return new Double[] { means[j1], vars[j1] };
	}

	public int numParams() {
		return 2;
	}

	public Double score(int j1, int j2, Double[] params, InputData data, Integer[] channels) {
		Double mean = params[0], var = params[1];
		return Math.log(var);
	}

	public int type() {
		return Segment.LINE;
	}
}

