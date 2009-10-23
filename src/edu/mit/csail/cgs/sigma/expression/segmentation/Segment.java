package edu.mit.csail.cgs.sigma.expression.segmentation;

import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.models.Model;

/**
 * Segment is one of the core classes of this package -- Segments are returned by the 
 * Segmenter object, and represent pieces of the data which are 
 * 
 * (a) fitted with a consistent model (either within or between channels), and 
 * (b) can be stitched together into transcripts.
 * 
 * @author tdanford
 *
 */
public class Segment extends Model implements Comparable<Segment> {
	
	public static final Integer FLAT = 0;
	public static final Integer LINE = -1;

	public Integer segmentType;  // For use, when we fit multiple segment types.
	public Integer channel;      // For use when we are fitting multiple channels simultaneously. 
	public Double[] params;      // an arbitrary array of parameters associated with this segment.
	public Integer start, end;   // The coordinate indices of this segment 
	                             // (so the segment extends from locations[start] to locations[end], inclusive).
	public Boolean shared;		 // A flag to indicate whether the segment is "shared" across
                                 // multiple channels/experiments. 
	
	public Segment(Integer s, Integer e, Double... ps) { 
		channel = 0;
		segmentType = FLAT;
		start = s;
		end = e;
		params = ps.clone();
		shared = false;
	}
	
	public Segment(Segment s) { 
		channel = s.channel;
		segmentType = s.segmentType;
		params = s.params.clone();
		start = s.start; end = s.end;
		shared = s.shared;
	}
	
	public Segment(Segment s, int offset) { 
		this(s);
		start += offset;
		end += offset;
	}
	
	public Segment() { 
	}
	
	public Segment(Integer chan, boolean sh, Integer type, Integer s, Integer e, Double... ps) {
		shared = sh;
		channel = chan;
		segmentType = type;
		start = s;
		end = e;
		params = ps.clone();
	}
	
	public String toString() { 
		StringBuilder ps = new StringBuilder();
		String name = shared ? "Shared-Segment" : "Segment";
		ps.append(String.format("%s type %d, ch %d (%d-%d) [", 
				name, segmentType, channel, start, end));
		for(int i = 0; i < params.length; i++) { 
			ps.append(i > 0 ? " " : "");
			ps.append(String.format("%.3f", params[i]));
		}
		ps.append("]");
		return ps.toString();
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof Segment)) { return false; }
		Segment s = (Segment)o;
		return s.shared == shared && 
			s.segmentType == segmentType && 
			s.channel == channel && 
			s.start == start && 
			s.end == end;
	}
	
	public int hashCode() { 
		int code = 17;
		code += shared ? 1 : 0; code *= 37;
		code += segmentType; code *= 37;
		code += channel; code *= 37;
		code += start; code *= 37;
		code += end; code *= 37;
		return code;
	}
	
	public int compareTo(Segment s) { 
		if(start < s.start) { return -1; }
		if(start > s.start) { return 1; }
		if(end < s.end) { return -1; }
		if(end > s.end) { return 1; }
		if(channel < s.channel) { return -1; }
		if(channel > s.channel) { return 1; }
		for(int i = 0; i < Math.min(params.length, s.params.length); i++) { 
			if(params[i] != s.params[i]) { 
				return params[i].compareTo(s.params[i]);
			}
		}
		return 0;
	}
}
