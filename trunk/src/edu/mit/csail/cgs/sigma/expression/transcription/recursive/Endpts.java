/*
 * Author: tdanford
 * Date: Jun 12, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import java.util.Collection;
import java.util.LinkedList;

public class Endpts implements Comparable<Endpts> {
	
	public int start, end;
	
	public Endpts(int s, int e) { 
		if(s < 0 || s >= e) { 
			throw new IllegalArgumentException(String.format("%d,%d", s, e));
		}
		start = s; 
		end = e;
	}
	
	public boolean coversBreak(int b) { 
		return start < b && end > b;
	}
	
	public boolean isAfter(Endpts[] before) { 
		for(int i = 0; i < before.length; i++) { 
			if(compareTo(before[i]) <= 0) { 
				return false;
			}
		}
		return true;
	}
	
	public boolean isDuplicate(Collection<Endpts> epts) { 
		for(Endpts e : epts) { 
			if(e.equals(this)) { 
				return true;
			}
		}
		return false;
	}
	
	public boolean isContiguous(Endpts e) { 
		return end == e.start || start == e.end;
	}
	
	public Endpts add(Endpts e) { 
		if(!isContiguous(e)) { throw new IllegalArgumentException(e.toString()); }
		return new Endpts(Math.min(e.start, start), Math.max(end, e.end));
	}
	
	public int hashCode() { 
		int code = 17;
		code += start; code *= 37;
		code += end; code *= 37;
		return code;
	}
	
	public String toString() { return String.format("[%d,%d)", start, end); }
	
	public boolean equals(Object o) { 
		if(!(o instanceof Endpts)) { 
			return false;
		}
		Endpts e = (Endpts)o;
		return e.start==start && e.end==end;
	}
	
	public int compareTo(Endpts e) { 
		if(start < e.start) { return -1; }
		if(start > e.start) { return 1; }
		if(end < e.end) { return -1; }
		if(end > e.end) { return 1; }
		return 0;
	}

	public int length() {
		return end-start;
	}

	public boolean isOverlapping(Endpts e2) {
		return (start <= e2.start && end > e2.start) ||
			(e2.start <= start && e2.end > start);
	}
}
