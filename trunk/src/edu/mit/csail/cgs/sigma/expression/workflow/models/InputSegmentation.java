/*
 * Author: tdanford
 * Date: Jan 6, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.models;

import java.util.*;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;

import edu.mit.csail.cgs.sigma.Printable;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.FlatFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.LineFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.SegmentFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharing;
import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.utils.json.JSONObject;
import edu.mit.csail.cgs.utils.models.Model;

public class InputSegmentation extends Model implements Printable, Comparable<InputSegmentation> {
	
	private static SegmentFitter defaultFlatFitter = new FlatFitter(); 
	private static SegmentFitter defaultLineFitter = new LineFitter(); 

	public FileInputData input;
	public Segment[] segments;
	
	public InputSegmentation() { 
	}
	
	public InputSegmentation(JSONObject obj) { 
		super(obj);
	}
	
	public InputSegmentation(FileInputData inp, boolean transcript) {
		input = inp;
		segments = new Segment[inp.channels()];
		int start = 0, end = input.length()-1;
		
		SegmentFitter fitter = transcript ? defaultLineFitter : defaultFlatFitter;
		int segType = transcript ? Segment.LINE : Segment.FLAT;
				
		Integer[] chs = new Integer[inp.channels()];
		for(int i = 0; i < chs.length; i++) { 
			chs[i] = i;
		}
		Double[] params = fitter.fit(start, end, input, chs);
		
		for(int i = 0; i < input.channels(); i++) { 
			segments[i] = new Segment(i, true, segType, start, end, params);
		}
	}
	
	public InputSegmentation(FileInputData d, Collection<Segment> segs) { 
		this(d, false);
		segments = segs.toArray(new Segment[0]);
		Arrays.sort(segments);
	}
	
	public Segment lastSegment() { 
		return segments.length > 0 ? segments[segments.length-1] : null;
	}
	
	public Segment[] array() { 
		return segments;
	}
	
	public void print(PrintStream ps) { 
		input.print(ps);
		for(Segment s : segments) { 
			ps.println(s.toString());
		}
		ps.println("----------------------------");
	}
	
	public int hashCode() { return input.hashCode(); }
	
	public boolean equals(Object o) { 
		if(!(o instanceof InputSegmentation)) { return false; }
		InputSegmentation s = (InputSegmentation)o;
		return input.equals(s.input);
	}
	
	public int compareTo(InputSegmentation is) { 
		return input.compareTo(is.input);
	}
}