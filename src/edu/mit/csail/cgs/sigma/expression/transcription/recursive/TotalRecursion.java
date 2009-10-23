/*
 * Author: tdanford
 * Date: Jun 11, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import java.io.PrintStream;
import java.util.*;

import edu.mit.csail.cgs.utils.ArrayUtils;

public class TotalRecursion {
	
	public static void main(String[] args) { 
		int segs = 7;
		int calls = 4;
		
		TotalRecursion trec = new TotalRecursion(segs, calls);
		int tsize = trec.size();
		trec = null;
		System.out.println(String.format("# Blocks: %d", tsize));
		
		PrimitiveRecursion prec = new PrimitiveRecursion(segs, calls);
		int psize = prec.size();
		prec = null;
		System.out.println(String.format("# Primitive Blocks: %d", psize));
		
		double f = (double)tsize / (double)psize;
		System.out.println(String.format("%% Increase: %.2f", (f-1.0) * 100.0));
	}

	private int[] segments, breaks;
	private int calls;

	private Endpts[] epts;
	private Map<Endpts,TotalRecursion> recmap;

	public TotalRecursion(int s, int c) { 
		this(new int[s], new int[s-1], null, null, c);
	}

	private TotalRecursion(int[] s, int[] b, Endpts[] earray, Endpts e, int c) { 
		if(e != null) { 
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
		recmap = new TreeMap<Endpts,TotalRecursion>();

		/*
		 * Here, we build the recursion tree.  
		 */
		if(calls > 0) { 
			Endpts[] array = allEndpts(segments.length);
			for(int i = 0; i < array.length; i++) {
				if(array[i].isAfter(epts)) { 
					TotalRecursion rec = new TotalRecursion(segments, breaks, epts, array[i], calls-1);
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
				TotalRecursion rec = recmap.get(ekey);
				if(!rec.isCoverageComplete()) { 
					recmap.remove(ekey);
				}
			}
		} else if (calls > 1) { 
			for(Endpts ekey : keys) { 
				if(recmap.get(ekey).getNumChildren() == 0) { 
					recmap.remove(ekey);
				}
			}
		}
	}
	
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
	
	public LinkedList<TotalRecursion> leaves() { 
		LinkedList<TotalRecursion> recs = new LinkedList<TotalRecursion>();
		collectLeaves(recs);
		return recs;
	}
	
	private void collectLeaves(LinkedList<TotalRecursion> recs) { 
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

