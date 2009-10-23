/*
 * Author: tdanford
 * Date: Dec 2, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription;

import java.util.*;

/**
 * Arrangements is an iterator -- given a Cluster, and a number (k) which indicates
 * the number of transcript calls to generate over that cluster, Arrangements will 
 * return a stream of TranscriptArrangement objects.  Each TranscriptArrangement is 
 * a set of TranscriptCalls over the Cluster; Arrangements will eventually return 
 * *every* legal TranscriptArrangement over the given Cluster.
 * 
 * @author tdanford
 *
 */
public class Arrangements implements Iterator<TranscriptArrangement> {
	
	/**
	 * Cluster of (neighboring) segments
	 */
	private Cluster cluster;
	
	/**
	 * breakpoints within the cluster
	 */
	private Integer[] breakpoints;
	private Set<Integer> continuousBreakpointIndices;
	
	/**
	 * Represents the <tt>K-TranscriptCalls</tt> combinations out of the 
	 * cluster of segments
	 */
	private Integer K;
	
	/**
	 * The transcripts (<tt>calls.length = K</tt>)
	 */
	private Call[] calls;
	private TranscriptArrangement nextArr;
	
	public Arrangements(Cluster c, int k) {
		cluster = c;
		breakpoints = cluster.breakpoints();
		continuousBreakpointIndices = new TreeSet<Integer>();
		
		for(int i = 1; i < breakpoints.length-1; i++) { 
			if(cluster.segments[i-1].isContinuous(cluster.segments[i], c.channels)) { 
				continuousBreakpointIndices.add(i);
			}
		}
		
		K = k;
		nextArr = null;
		calls = null;
		findNextArrangement();
	}

	private Set<Call> callSet() { 
		TreeSet<Call> set = new TreeSet<Call>();
		for(int i = 0; i < calls.length; i++) { 
			Call c = calls[i];
			
			if(set.contains(c)) { 
				return null; 
			} else { 
				set.add(c); 
			}
		}
		return set;
	}
	
	private boolean lastCall(Call c) { 
		return c.end.equals(cluster.segments.length) && c.start.equals(c.end-1);
	}
	
	private boolean nextCall(Call c) { 
		if(c.end < breakpoints.length-1) { 
			c.end += 1;
		} else { 
			if(c.start < breakpoints.length-1) { 
				c.start += 1;
				c.end = c.start+1;
			} else { 
				return false;
			}
		}
		return true;
	}
	
	private void printCallArray() { 
		for(int i = 0; i < calls.length; i++) { 
			if(i > 0) { System.out.print(","); }
			System.out.print(calls[i].toString());
		}
		System.out.println();
	}
	
	private void findNextArrangement() { 
		nextArr = null;
		boolean foundNext = false;

		do { 
			foundNext = false;
			
			if(calls == null) { 
				calls = new Call[K];
				for(int i = 0; i < calls.length; i++) { 
					calls[i] = new Call(0, 1);
				}			
				foundNext = true;
			} else { 
				int i;
				for(i = calls.length-1; i >= 0 && lastCall(calls[i]); i--) 
					{}
				if(i >= 0) { 
					nextCall(calls[i]);
					for(int j = i + 1; j < calls.length; j++) { 
						calls[j] = new Call(calls[i]);
					}
					foundNext = true;
				}
			}
			
			if(foundNext) { 
				Set<Call> callset = callSet();
				if(callset != null) { 
					nextArr = new TranscriptArrangement(cluster, callset);
					if(!nextArr.coversCluster()) { 
						nextArr = null;
					}
				}
			}
		} while(foundNext && nextArr == null);
	}

	public boolean hasNext() {
		return nextArr != null;
	}

	public TranscriptArrangement next() {
		TranscriptArrangement arr = nextArr;
		findNextArrangement();
		return arr;
	}

	public void remove() {
		throw new UnsupportedOperationException("remove() isn't supported.");
	}
}
