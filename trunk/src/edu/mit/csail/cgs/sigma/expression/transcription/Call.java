package edu.mit.csail.cgs.sigma.expression.transcription;

import java.io.PrintStream;

import edu.mit.csail.cgs.sigma.Printable;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.models.Model;

/**
 * Will hold the indexes of the segments (relative to the cluster) within a transcript.
 * 
 * @author tdanford
 */
public class Call extends Model implements Comparable<Call>, Printable {

	public Integer start, end;  // [start, end)
	public Double value;
	
	public Call() {}
	
	public Call(int s, int e) { 
		start = s; 
		end = e;
		value = null;
	}
	
	public Call(int s, int e, double v) { 
		start = s; 
		end = e;
		value = v;;
	}
	
	public Call(Call c) { 
		start = c.start; 
		end = c.end;
		value = c.value;
	}
	
	public boolean overlaps(Call c) {
		return (start <= c.start && end > c.start) ||
			(c.start <= start && c.end > start);
	}
	
	public void print(PrintStream ps) {
		ps.println(toString());
	}

	public String toString() { 
		if(value == null) { 
			return String.format("[%d:%d)", start, end);
		} else { 
			return String.format("[%d:%d) (%.2f)", start, end, value);			
		}
	}
	
	public int hashCode() { 
		int code = 17;
		code += start; code *= 37;
		code += end; code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof Call)) { return false; }
		Call c = (Call)o;
		return c.start.equals(start) && c.end.equals(end);
	}
	
	/**
	 * When either the <tt>start</tt> or <tt>end</tt> of <tt>TranscriptCall c</tt> is downstream
	 * (namely, towards the 3' end) of the
	 *  <tt>start</tt> or <tt>end</tt> of current <tt>TranscriptCall</tt>, respectively <tt>-1</tt> is returned.<br>
	 *  Otherwise, <tt>1</tt>.
	 * @param c
	 * @return
	 */
	public int compareTo(Call c) {
		int cstart = c.start, cend = c.end;
		if(start < cstart) { return -1; }
		if(start > cstart) { return 1; }
		if(end < cend) { return -1; }
		if(end > cend) { return 1; }
		return 0;
	}

}
