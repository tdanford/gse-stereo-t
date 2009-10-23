package edu.mit.csail.cgs.sigma.giorgos.examples.segmentation;

import java.util.Collection;
import java.util.ArrayList;

import edu.mit.csail.cgs.sigma.expression.segmentation.input.SimpleInputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.SharedSegmentGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.FixedSegmentGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.RandomSegmentGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.RandomInputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.SimpleInputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.SegmentViz;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.SegmentVizFrame;
import edu.mit.csail.cgs.sigma.giorgos.examples.segmentation.Tiler;

public class TestSegmentGenerator implements Runnable 
{
	private int length = 4; 

	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		// TODO Auto-generated method stub
		
		/*
		int start = 51;	int end = 100; 	int num = 3;
		
		Tiler regTiler = new RegularTiler();
		Integer[] regTiles = regTiler.tile(start, end, num);
		
		Tiler randTiler = new RandomTiler();
		Integer[] randTiles = randTiler.tile(start, end, num);
		*/

		TestSegmentGenerator obj = new TestSegmentGenerator();
		System.out.println(obj.toHexString());
		obj.run();
		

	}
	
	public void run()
	{
		int count = 3;
		double pshared = 0.3;
		RandomSegmentGenerator fsg = new SharedSegmentGenerator(count, pshared);
		//Segment[][] sharedSegs = fsg.generateSegments(2, 100);		
		
		int channels= 2;
		RandomInputGenerator rig = new SimpleInputGenerator(fsg, 100, channels, "chr01", "+");
		rig.generate();
		InputData inputData = rig.inputData();
		Integer[] locations = inputData.locations();
		Double[][] values = inputData.values();
		Collection<Segment> fixedSegsCol = rig.segments();
						
		int start = Math.max(0, locations[0]-1);
		int end = locations[locations.length-1]+1;
		
		SegmentViz[] viz = new SegmentViz[channels];
		for(int i = 0; i < viz.length; i++)
			viz[i] = new SegmentViz();
		
		for(int i = 0; i < viz.length; i++)
		{
			viz[i].setData(locations, values[0]);
	        viz[i].setBounds("chr01", "+", start, end);
			viz[i].setSegments(rig.segments(i));
			
		}
		
		SegmentVizFrame frame = new SegmentVizFrame(this, null, viz);
	}

	
	
	public void run2()
	{
		RandomSegmentGenerator fsg = new FixedSegmentGenerator(3, 17, 17, 25, 37, 30);
		/*
		Segment[][] fixedSegsInChannels = fsg.generateSegments(1, 1);
		Segment[] fixedSegs = fixedSegsInChannels[0];
		Collection<Segment> fixedSegsCol = new ArrayList<Segment>();
		for(Segment fixedSeg:fixedSegs)
			fixedSegsCol.add(fixedSeg);
		*/
		
		RandomInputGenerator rig = new SimpleInputGenerator(fsg, 100, 1, "chr01", "+");
		rig.generate();
		InputData inputData = rig.inputData();
		Integer[] locations = inputData.locations();
		Double[][] values = inputData.values();
		Collection<Segment> fixedSegsCol = rig.segments();
						
		SegmentViz viz = new SegmentViz();
		viz.setData(locations, values[0]);
		
		int start = Math.max(0, locations[0]-1);
		int end = locations[locations.length-1]+1;
        viz.setBounds("chr01", "+", start, end);
		viz.setSegments(fixedSegsCol);
		SegmentVizFrame frame = new SegmentVizFrame(this, null, viz);
	}
	
	public String toHexString() {
		int fs = length/4;
		StringBuilder sb = new StringBuilder();
		if(true) {
			for(int i = 0; i < fs; i++) { 
				sb.append("F");
			}
			int extra = length%4;
			int value = 1;
			for(int i = 0, base = 8; i < extra; i++, base /= 2) { 
				value += base; 
			}
			
			if(value < 10) { 
				sb.append(String.valueOf(value)); 
			} else { 
				int c = (char)('A' + (value-10));
				sb.append(c);
			}
			
		} else { 
			for(int i = 0; i <= fs; i++) { 
				sb.append("0");
			}
		}
		return sb.toString();
	}

}// end of TestSegmentGenerator class
