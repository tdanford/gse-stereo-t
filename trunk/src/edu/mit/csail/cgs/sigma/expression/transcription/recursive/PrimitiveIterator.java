/*
 * Author: tdanford
 * Date: Jun 18, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;

public class PrimitiveIterator implements Iterator<Endpts[]> {
	
	public static void main(String[] args) {
		
		String strand = "+";
		LinkedList<Integer> dips = new LinkedList<Integer>();
		int segs = 4;
		int calls = 3;
		dips.add(3);
		PrimitiveIterator itr = new PrimitiveIterator(segs, calls, 2, null, null);
		Filter<Endpts[],Endpts[]> filter = new BreakpointFilter(dips, strand, segs);

		int c = 0;
		while(itr.hasNext()) { 
			String str = itr.nextString();
			Endpts[] epts = itr.next();
			if(filter != null && filter.execute(epts) != null) { 
				System.out.println(str);
				System.out.println();
				c += 1;
			}
		}
		System.out.println(String.format("# Configs: %d", c));
	}
	
	private int segs;
	private int calls;
	
	private boolean debugPrint; 
	private boolean requireComplete, requirePrimitive, requireNonRedundant;
	private int minOverlap, maxOverlap;
	
	private int[] segments, breaks;
	private Map<Integer,Integer> breakMax;
	
	private LinkedList<Iterator<Endpts>> itrs;
	private LinkedList<Endpts> treePath;

	public PrimitiveIterator(int s, int c) { 
		this(s, c, -1, null, null); 
	}
	
	public PrimitiveIterator(int s, int c, int maxOver, Collection<Integer> dips, String strand) {
		segs = s;
		calls = c;
		
		debugPrint = false;
		requireComplete = true;
		requirePrimitive = false;
		requireNonRedundant = true;
		minOverlap = 0;
		maxOverlap = maxOver;
		breakMax = new TreeMap<Integer,Integer>();
		
		segments = new int[segs];
		breaks = new int[segs+1];
		
		for(int i = 0; i < segments.length; i++) {
			segments[i] = 0;
			if(i < breaks.length) { breaks[i] = 0; }
		}
		
		itrs = new LinkedList<Iterator<Endpts>>();
		treePath = new LinkedList<Endpts>();
		if(dips != null && strand != null) { 
			setDips(dips, strand);
		}
		initialize();		
	}

	public PrimitiveIterator(ClusterData d, int c) {
		this(d.segments(), c);
	}
	
	private void setMinOverlap(int m) { 
		if(m < 1 && m != -1) { throw new IllegalArgumentException(String.valueOf(m)); }
		minOverlap = m;
	}
	
	private void setMaxOverlap(int m) { 
		if(m < 1 && m != -1) { throw new IllegalArgumentException(String.valueOf(m)); }
		maxOverlap = m;
	}

	private void setDips(Collection<Integer> dips, String strand) {
		breakMax.clear();
		boolean str = strand.equals("+");
		for(Integer dipseg : dips) { 
			if(str) {
				if(dipseg > 0) { 
					breakMax.put(dipseg, 0);
				}
			} else { 
				if(dipseg < segs-1) { 
					breakMax.put(dipseg+1, 0);
				}
			}
		}
	}

	public boolean isLegalOverlap() { 
		for(int i = 0; i < segments.length; i++) { 
			if(minOverlap != -1 && segments[i] < minOverlap) { 
				return false;
			}
			if(maxOverlap != -1 && segments[i] > maxOverlap) { 
				return false;
			}
		}
		return true;
	}
	
	public boolean isNonRedundant() { 
		Endpts last = null;
		for(Endpts e : treePath) { 
			if(last != null && last.equals(e)) { 
				return false;
			}
			last = e;
		}
		return true;
	}
	
	public boolean isPrimitive() { 
		for(int i = 1; i < segments.length; i++) { 
			if(breaks[i] == 0) { 
				return false;
			}
		}
		return true;
	}
	
	public boolean isComplete() { 
		for(int i = 0; i < segments.length; i++) { 
			if(segments[i] == 0) { 
				return false;
			}
		}
		return true;
	}
	
	public boolean isLegalBreaks() { 
		for(Integer breakpt : breakMax.keySet()) { 
			if(breaks[breakpt] > breakMax.get(breakpt)) { 
				return false;
			}
		}
		return true;
	}
	
	private void addEndpts(Endpts e) {

		if(debugPrint) { 
			for(int i = 0; i < treePath.size(); i++) { 
				System.out.print("  ");
			}
			System.out.println(String.format("+[%d, %d] (%d:%d)", e.start, e.end, treePath.size(), itrs.size()));
		}
		
		treePath.addLast(e);
		for(int i = e.start; i < e.end; i++) { 
			segments[i] += 1;
			if(i > e.start) { 
				breaks[i] += 1;
			}
		}
	}
	
	private void removeEndpts() { 
		Endpts e = treePath.getLast();

		if(debugPrint) { 
			for(int i = 0; i < treePath.size()-1; i++) { 
				System.out.print("  ");
			}
			System.out.println(String.format("-[%d, %d] (%d:%d)", e.start, e.end, treePath.size(), itrs.size()));
		}
		
		treePath.removeLast();

		for(int i = e.start; i < e.end; i++) { 
			segments[i] -= 1;
			if(i > e.start) { 
				breaks[i] -= 1;
			}
		}
	}
	
	private void initialize() {
		if(debugPrint) { System.out.println("initialize()"); } 
		itrs.add(new NextIterator());
		addEndpts(new Endpts(0, 1));
		findNext();
		if(debugPrint) { System.out.println("initialize() end"); } 
	}

	public boolean hasNext() {
		return treePath.size() == calls;
	}
	
	private boolean isValid() { 
		return treePath.size() == calls &&
			(!requireComplete || isComplete()) && 
			(!requireNonRedundant || isNonRedundant()) && 
			isLegalOverlap() &&
			//isLegalBreaks() && 
			(!requirePrimitive || isPrimitive());
	}
	
	/**
	 * findNext() is the outermost loop to the tree-search -- when called, the 
	 * treePath() is filled with a complete path, but and that path must be replaced
	 * by a new path (for which isValid() will return true).  
	 *
	 */
	private void findNext() {
		if(debugPrint) { System.out.println("findNext()"); } 

		boolean builtNext = true;
		while((builtNext = buildNext()) && !isValid()) { 
			// do nothing.
			builtNext = true;
		}

		if(!builtNext) { 
			treePath.clear();
		}

		if(hasNext() && !isValid()) { 
			throw new IllegalStateException("How did we get here??");
		}
		
		if(debugPrint) { 
			System.out.println(String.format("findNext() end, isValid=%b",
					isValid()));
		}
	}
	
	private boolean buildNext() {
		if(debugPrint) { System.out.println("buildNext()"); } 
		clearTrailingEmpty();
		if(!itrs.isEmpty()) { 
			removeEndpts();
			addEndpts(itrs.getLast().next());
			
			// Redundancy (unlike completeness or primitiveness) is the one
			// property that we can check for "as we go" -- if a sub-path is 
			// redundant, then all paths containing that sub-path will be redundant 
			// as well.  Therefore, if we *need* non-redundancy, and we see it 
			// early, we can break off our search.  
			if(requireNonRedundant && !isNonRedundant()) { 
				return true;
			}
			
			fillPath();
			
			if(debugPrint) { System.out.println("buildNext()=true"); } 
			return true;
		} else {
			if(debugPrint) { System.out.println("buildNext()=false"); } 
			return false;
		}
	}
	
	private void fillPath() { 
		if(debugPrint) { System.out.println("fillPath()"); } 
		if(itrs.size() < calls) { 
			Endpts laste = treePath.getLast();
			Iterator<Endpts> eitr = new NextIterator(laste);
			if(eitr.hasNext()) { 
				itrs.add(eitr);
				addEndpts(eitr.next());
				fillPath();
			}
		}
		if(debugPrint) { System.out.println("fillPath() end"); } 
	}
	
	private int clearTrailingEmpty() { 
		if(debugPrint) { System.out.println("clearTrailingEmpty()"); } 
		int r = 0;
		while(!itrs.isEmpty() && !itrs.getLast().hasNext()) { 
			removeEndpts();
			itrs.removeLast();
			r += 1;
		}
		if(debugPrint) { System.out.println(String.format("clearTrailingEmpty() = %d", r)); } 
		return r;
	}
	
	public Endpts[] next() {
		if(!isValid()) { 
			System.err.println(nextString());
			throw new IllegalStateException(); 
		}
		Endpts[] e = treePath.toArray(new Endpts[treePath.size()]);
		findNext();
		return e;
	}
	
	public String nextString() { 
		StringBuilder sb = new StringBuilder();
		sb.append(" ");
		for(int i = 0; i < segments.length; i++) { 
			String breakValue = breakMax.containsKey(i) ? "v" : " ";
			sb.append(String.format("|%s",breakValue));
		}
		sb.append("\n");

		for(int i = 0; i < segments.length; i++) { 
			sb.append(String.format(" %d", segments[i]));
		}
		sb.append(" \n");

		//sb.append(" ");
		for(int i = 0; i < breaks.length; i++) { 
			sb.append(String.format("%d ", breaks[i]));
		}
		sb.append("\n");

		if(breakMax.size() > 0) { 
			sb.append(" ");
			for(int i = 0; i < breaks.length; i++) { 
				String v = breakMax.containsKey(i) ? String.valueOf(breakMax.get(i)) : " ";
				sb.append(String.format(" %s", v));
			}
			sb.append("\n");
		}

		/*
		sb.append("Break Maxima:");
		for(Integer key : breakMax.keySet()) { 
			Integer limit = breakMax.get(key);
			sb.append(String.format(" %d:%d(=%d)", key, limit, breaks[key]));
		}
		sb.append(String.format(" %b, %b\n", isLegalBreaks(), isValid()));
		*/
		
		for(Endpts e : treePath) { 
			for(int j = 0; j < segments.length; j++) {
				if(e.start == j) { 
					sb.append(" -");
				} else if(e.start < j && e.end > j) { 
					sb.append("--");
				} else { 
					sb.append("  ");
				}
			}
			sb.append(String.format(" (%d)\n", e.length()));
		}
		return sb.toString();
	}

	public void print(PrintStream ps) {
		ps.print(nextString());
	}
	
	public void print() { 
		print(System.out);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	private class NextIterator implements Iterator<Endpts> {
		
		private int start, end;
		
		public NextIterator() { 
			this(null);
		}
		
		public NextIterator(int s, int e) { 
			start = s; end = e;
		}
		
		public NextIterator(Endpts last) {
			if(last == null) { 
				start = 0; 
				end = 1;
			} else {
				/*
				if(last.end >= segs) { 
					start = last.start+1;
					end = start+1;
				} else { 
					start = last.start;
					end = last.end+1;
				}
				*/
				start = last.start;
				end = last.end;
			}
		}

		public boolean hasNext() {
			return start <= segs-1 && end <= segs;
		}

		public Endpts next() {
			Endpts e = new Endpts(start, end);
			end += 1; 
			if(end > segs) { 
				start += 1;
				end = start + 1;
			}
			return e;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		} 
	}

	public static Collection<Integer> extractBreaks(Collection<Integer> dips, String str, int segs) { 
		boolean strand = str.equals("+");
		ArrayList<Integer> breaks = new ArrayList<Integer>();

		for(Integer dip : dips) { 
			if(strand) { 
				if(dip > 0) { 
					breaks.add(dip);
				}
			} else { 
				if(dip < segs-1) { 
					breaks.add(dip+1);
				}
			}
		}
		
		return breaks;
	}

	public static class BreakpointFilter implements Filter<Endpts[],Endpts[]> { 

		private Integer[] breaks; 

		public BreakpointFilter(Collection<Integer> bks) { 
			breaks = bks.toArray(new Integer[0]);
		}

		public BreakpointFilter(Collection<Integer> dips, String strand, int segs) { 
			this(extractBreaks(dips, strand, segs));	
		}

		public Endpts[] execute(Endpts[] epts) { 
			for(int i = 0; i < epts.length; i++) { 
				for(int j =0; j < breaks.length; j++) { 
					if(epts[i].start < breaks[j] && epts[i].end > breaks[j]) { 
						return null;
					}
				}
			}
			return epts;
		}
	}
}
