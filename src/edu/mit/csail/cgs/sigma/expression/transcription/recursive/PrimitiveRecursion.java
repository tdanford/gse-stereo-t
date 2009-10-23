/*
 * Author: tdanford
 * Date: Jun 11, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import java.io.PrintStream;
import java.util.*;

import edu.mit.csail.cgs.utils.ArrayUtils;

/**
 * A "primitive" block is a set of segments, over which transcripts are arranged, 
 * which contains *no* "division" -- that is, there is no point (boundary between
 * segments) where the segments can be divided into independent left and right 
 * halves (that is, a breakpoint over which no transcript crosses).  
 * 
 * @author tdanford
 */
public class PrimitiveRecursion {
	
	public static void main(String[] args) { 
		PrimitiveRecursion rec = new PrimitiveRecursion(5, 5, 3);
		Collection<PrimitiveRecursion> lvs = rec.leaves();
		int i = 0;
		int maxPrint = 200;
		Iterator<PrimitiveRecursion> itr = lvs.iterator();
		while(itr.hasNext() && i < maxPrint) { 
			PrimitiveRecursion r = itr.next();
			r.printDiagram();
			System.out.println();
			i += 1;
		}
		
		System.out.println(String.format("# Primitive Blocks: %d", lvs.size()));
	}

	private int[] segments, breaks;
	private int calls;

	private Endpts[] epts;
	private Map<Endpts,PrimitiveRecursion> recmap;

	public PrimitiveRecursion(int s, int c) { 
		this(new int[s], new int[s-1], null, null, c, -1);
	}

	public PrimitiveRecursion(int s, int c, int max) { 
		this(new int[s], new int[s-1], null, null, c, max);
	}

	private PrimitiveRecursion(int[] s, int[] b, Endpts[] earray, Endpts e, int c, int maxOverlap) { 
		if(e != null) { 
			// Tree branch or leaf.  
			segments = s.clone();
			breaks = b.clone();
			epts = ArrayUtils.append(earray, e);

			for(int i = e.start; i < e.end; i++) { 
				segments[i] += 1;
				if(i > e.start) { 
					breaks[i-1] += 1;
				}
			} 
		} else { 
			// Top-level node in the tree.
			epts = new Endpts[] {};
			segments = s;
			breaks = b;
			
			for(int i = 0; i < segments.length; i++) { 
				segments[i] = 0;
			}
			for(int i = 0; i < breaks.length; i++) { 
				breaks[i] = 0;
			}
		}
		
		calls = c;
		recmap = new TreeMap<Endpts,PrimitiveRecursion>();

		/*
		 * Here, we build the recursion tree.  
		 */
		if(calls > 0) { 
			Endpts[] array = allEndpts(segments.length);
			for(int i = 0; i < array.length; i++) {
				if(array[i].isAfter(epts)) { 
					PrimitiveRecursion rec = new PrimitiveRecursion(segments, breaks, epts, array[i], calls-1, maxOverlap);
					recmap.put(array[i], rec);
				}
			}
		}
		
		/*
		 * Finally, we prune the "empty" parts of the tree.  
		 */
		ArrayList<Endpts> keys = new ArrayList<Endpts>(recmap.keySet());
		if(calls <= 1) { 
			for(Endpts ekey : keys) { 
				PrimitiveRecursion rec = recmap.get(ekey);
				if(!rec.isCoverageComplete() || !rec.isSpanningCoverage() || 
						(maxOverlap != -1 && !rec.isUnderMaxOverlap(maxOverlap))) { 
					recmap.remove(ekey);
				}
			}
		} else if (calls > 1) {
			// This is a higher branch, and so we prune any children that are themselves
			// empty.
			for(Endpts ekey : keys) { 
				if(recmap.get(ekey).getNumChildren() == 0) { 
					recmap.remove(ekey);
				}
			}
		}
	}
	
	/**
	 * Returns the total number of leaves in the tree.
	 * (i.e., this.leaves().size() == this.size()) 
	 * 
	 * @return
	 */
	public int size() { 
		if(calls <= 0) { 
			return 1;
		} else { 
			int s = 0;
			for(Endpts e : recmap.keySet()) { 
				s += recmap.get(e).size();
			}
			return s;
		}
	}
	
	public LinkedList<PrimitiveRecursion> leaves() { 
		LinkedList<PrimitiveRecursion> recs = new LinkedList<PrimitiveRecursion>();
		collectLeaves(recs);
		return recs;
	}
	
	private void collectLeaves(LinkedList<PrimitiveRecursion> recs) { 
		if(calls > 0) { 
			for(Endpts key : recmap.keySet()) { 
				recmap.get(key).collectLeaves(recs);
			}
		} else { 
			recs.add(this);
		}
	}
	
	public int getNumChildren() { return recmap.size(); }
	
	public void printDiagram() { 
		printDiagram(System.out);
	}
	
	public void printDiagram(PrintStream ps) { 
		for(int i = 0; i < segments.length; i++) { 
			ps.print(" |");
		}
		ps.println(" ");
		for(int i = 0; i < segments.length; i++) { 
			ps.print(String.format(" %d", segments[i]));
		}
		ps.println(" ");
		
		for(int i = 0; i < epts.length; i++) { 
			for(int j = 0; j < segments.length; j++) { 
				if(epts[i].start == j) { 
					ps.print(" -");
				} else if(epts[i].start < j && epts[i].end > j) { 
					ps.print("--");
				} else { 
					ps.print("  ");
				}
			}
			ps.println(String.format(" (%d)", epts[i].length()));
		}
	}
	
	/**
	 * In a "complete" coverage, every segment is covered by at least one transcript.
	 * @return
	 */
	public boolean isCoverageComplete() { 
		for(int i = 0; i < segments.length; i++) { 
			if(segments[i] <= 0) { 
				return false;
			}
		}
		return true;
	}
	
	/**
	 * A "spanning" coverage contains no "divisions" -- that is, every boundary 
	 * between segments is covered by at least one transcript call. 
	 * 
	 * @return
	 */
	public boolean isSpanningCoverage() {
		for(int i = 0; i < breaks.length; i++) { 
			if(breaks[i] <= 0) { 
				return false;
			}
		}
		return true;
	}
	
	/**
	 * A test for ensuring that no more than a *maximum* number of overlaps 
	 * has been generated.  
	 * 
	 * @param max
	 * @return
	 */
	public boolean isUnderMaxOverlap(int max) { 
		for(int i = 0; i < segments.length; i++) {
			if(segments[i] > max) { 
				return false;
			}
		}
		return true;
	}
	
	public static Endpts[] allEndpts(int n) {
		ArrayList<Endpts> epts = new ArrayList<Endpts>();
		for(int i = 0; i < n; i++) { 
			for(int j = i + 1; j <= n; j++) { 
				epts.add(new Endpts(i, j));
			}
		}
		Endpts[] earray = epts.toArray(new Endpts[0]);
		return earray;
	}
}

