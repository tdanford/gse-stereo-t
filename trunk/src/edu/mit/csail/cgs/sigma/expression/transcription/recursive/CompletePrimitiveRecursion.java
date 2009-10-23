/*
 * Author: tdanford
 * Date: Jun 12, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import java.util.*;
import java.io.*;

public class CompletePrimitiveRecursion {
	
	public static void main(String[] args) { 
		CompletePrimitiveRecursion rec = new CompletePrimitiveRecursion(7, 4, -1);
		System.out.println(String.format("# Primitives: %d", rec.size()));
	}

	private Map<CoverSize,PrimitiveRecursion> recmap;
	private int numFits, maxOverlaps;

	public CompletePrimitiveRecursion(int segs, int calls) { 
		this(segs, calls, -1);
	}

	public CompletePrimitiveRecursion(int segs, int calls, int max) { 
		recmap = new TreeMap<CoverSize,PrimitiveRecursion>();
		numFits = 0;
		maxOverlaps = max;
		
		for(int s = 1; s <= segs; s++) { 
			for(int c = 1; c <= calls; c++) { 
				CoverSize cs = new CoverSize(s, c);
				PrimitiveRecursion rec = new PrimitiveRecursion(s, c, maxOverlaps);
				recmap.put(cs, rec);
				int recsize = rec.size();
				int count = recsize * (segs-s+1);
				
				System.out.println(String.format("%d segs, %d calls : %d blocks (= %d fits)",
						s, c, recsize, count));
				numFits += count;
			}
		}
		
		System.out.println(String.format("# Fits: %d", numFits));
	}
	
	public Set<CoverSize> covers() { return recmap.keySet(); }
	
	public int size() { 
		int s = 0;
		for(CoverSize key : recmap.keySet()) { 
			s += recmap.get(key).size();
		}
		return s;
	}
}

class CoverSize implements Comparable<CoverSize> {
	
	private int segments, calls;
	
	public CoverSize(int s, int c) { 
		segments = s; calls = c;
	}
	public int segments() { return segments; }
	public int calls() { return calls; }
	
	public int hashCode() { 
		int code = 17;
		code += segments; code *= 37;
		code += calls; code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof CoverSize)) { 
			return false;
		}
		CoverSize s = (CoverSize)o;
		return s.segments == segments && s.calls == calls;
	}
	
	public int compareTo(CoverSize c) { 
		if(segments < c.segments) { return -1; }
		if(segments > c.segments) { return 1; }
		if(calls < c.calls) { return -1; }
		if(calls > c.calls) { return 1; }
		return 0;
	}
}
