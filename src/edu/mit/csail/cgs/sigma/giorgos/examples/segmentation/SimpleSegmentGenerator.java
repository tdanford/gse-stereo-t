package edu.mit.csail.cgs.sigma.giorgos.examples.segmentation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import edu.mit.csail.cgs.sigma.expression.segmentation.*;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.RandomInputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.RandomSegmentGenerator;

public class SimpleSegmentGenerator implements RandomInputGenerator
{
	
	private Integer numProbes, numSegments;
	
	public Integer[] locs;
	public Double[] values;
	
	public Integer[][] segments;
	public Double[] segmentValues;
	
	public Double bgScale, bgMean;
	public Double exprScale, exprMean;
	public Double noiseScale;
	
	private Random rand;
	
	public SimpleSegmentGenerator(int len, int segs) {
		numProbes = len; 
		numSegments = segs;
		
		locs = new Integer[len];
		values = new Double[len];
		
		for(int i = 0; i < len; i++) { 
			//locs[i] = (i+1) * 25;
			locs[i] = i;
		}
		
		rand = new Random();
		bgScale = 0.5; bgMean = 1.0;
		exprScale = 1.0; exprMean = 10.0;
		noiseScale = 1.0;
	}
	
	private double sampleBackground() { 
		return Math.abs((rand.nextGaussian() * bgScale) + bgMean);
	}

	private double sampleExpr() { 
		return Math.abs((rand.nextGaussian() * exprScale) + exprMean);
	}

	private double sampleNoise() { 
		return (rand.nextGaussian() * noiseScale);
	}
	
	public Collection<Segment> segments() { 
		ArrayList<Segment> segs = new ArrayList<Segment>();
		for(int i = 0; i < segments.length; i++) {
			Integer[] s = segments[i];
			segs.add(new Segment(s[0], s[1], segmentValues[i]));
		}
		return segs;
	}
	
	public void generate(Integer... startOffsets)
	{	
		for(int i = 0; i < locs.length; i++) { 
			values[i] = sampleBackground();
		}
		
		segments = new Integer[numSegments][];
		segmentValues = new Double[numSegments];
		
		int partSize = locs.length/numSegments;
		int length = Math.max(1, partSize*2/3);
		
		for(int i = 0; i < numSegments; i++)
		{
			int startOffset;
			if( i < startOffsets.length)
				startOffset = startOffsets[i];
			else
				startOffset = rand.nextInt(partSize-1);
			
			int start = partSize*i + startOffset;
			int end   = Math.min(start + length-1, locs.length-1);
			segments[i] = new Integer[] {start, end};
			
			double expr = sampleExpr();
			segmentValues[i] = expr;
			
			for(int j = start; j <= end; j++)
				values[j] += expr;
		}
		
		for(int i = 0; i < locs.length; i++) { 
			values[i] = Math.max(0.0, values[i] + sampleNoise());
		}
		
	}//end of generate(Integer... startOffsets) method
	
	public void generate() {
		for(int i = 0; i < locs.length; i++) { 
			values[i] = sampleBackground();
		}
		
		segments = new Integer[numSegments][];
		segmentValues = new Double[numSegments];
		
		int partSize = locs.length/numSegments;
		int length = Math.max(1, partSize*2/3);
		
		for(int i = 0; i < numSegments; i++) { 
			int start = partSize*i + rand.nextInt(partSize-1);
			int end = Math.min(start+length-1, locs.length-1);
			segments[i] = new Integer[] { start, end };
			
			double expr = sampleExpr();
			segmentValues[i] = expr;
			
			for(int j = start; j <= end; j++) { 
				values[j] += expr;
			}
		}
		
		for(int i = 0; i < locs.length; i++) { 
			values[i] = Math.max(0.0, values[i] + sampleNoise());
		}
	}
	
	public InputData inputData() { 
		return new RegressionInputData("1", "+", locations(), data());
	}

	public Double[][] data() {
		return new Double[][] { values };
	}

	public Integer[] locations() {
		return locs;
	} 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		System.out.println(Math.log(Math.E));
		
		RandomInputGenerator rig = new SimpleSegmentGenerator(100, 3);

		rig.generate();
		InputData inputData = rig.inputData();
		Integer[] locs = inputData.locations();		
		Double[][] data = inputData.values();
		
		Collection<Segment> segmentsColl = rig.segments();
		
		int count = 0;
		for(Segment segment:segmentsColl)
			System.out.println(++count + ") " + segment.toString());
		
		
		Segment[] segments = segmentsColl.toArray(new Segment[segmentsColl.size()]);
		Segment firstSegment = segments[0];
		Segment copySegment = new Segment(firstSegment.start, firstSegment.end, firstSegment.params); 
	    int value = copySegment.compareTo(firstSegment);
				
		
		
		int foo = 3;
		
		

	}

	/*
	 * Giorgos -- these two new methods are now part of RandomInputGenerator.  I've added 
	 * default implementations to your class here, so that the sigma module will compile.  
	 * This shouldn't affect anything you've written on your own so far.
	 * - Tim
	 */
	
	public void generate(RandomSegmentGenerator segGen) {
		generate();
	}

	public Collection<Segment> segments(int channel) {
		return segments();
	}

	public void generate(String chrom, int start, int end, String strand) {
		generate();
	}

}//end of SimpleSegmentGenerator class
