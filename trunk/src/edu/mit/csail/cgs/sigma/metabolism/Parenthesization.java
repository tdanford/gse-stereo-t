/*
 * Author: tdanford
 * Date: Apr 25, 2008
 */
package edu.mit.csail.cgs.sigma.metabolism;

import java.util.*;
import java.util.regex.*;

import java.io.*;

/**
 * @author tdanford
 *
 */
public class Parenthesization {

	public static void main(String[] args) { 
		String line;
		try { 
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.print(">"); System.out.flush();
			while((line = br.readLine()) != null) { 
				line= line.trim();
				System.out.println(String.format("'%s'", line));
				try { 
					Parenthesization p = new Parenthesization(line);

					for(int i = 1; i <= p.getMaxDepth(); i++) { 
						Collection<String> substrings = p.getSubstrings(i);
						System.out.print(String.format("Depth %d:", i));
						for(String s : substrings) { 
							System.out.print(String.format(" %s", s));
						}
						System.out.println();
					}
					
					System.out.println("Tree:");
					printTree(p.createTree(), System.out, 0);
					
				} catch(IllegalArgumentException e) { 
					System.out.println(String.format("Poor parenthesization: %s", e.getMessage()));
				}
				System.out.print(">"); System.out.flush();
			}
		} catch(IOException e) { 
			e.printStackTrace(System.err);
		}
	}

	public static Collection<int[]> invertIndices(Collection<int[]> inds, int start, int end) { 
		LinkedList<int[]> inverted = new LinkedList<int[]>();
		int[] starts = new int[inds.size()], ends = new int[inds.size()];
		int i = 0;
		for(int[] pair : inds) { 
			starts[i] = pair[0]; 
			ends[i] = pair[1];
			//System.out.println(String.format("(%d,%d)", starts[i], ends[i]));
			i++;
		}
		
		int ps = start;
		for(i = 0; i < starts.length; i++) {
			if(starts[i] > 0) { 
				inverted.add(new int[] { ps, starts[i]-1 } );
				//System.out.println(String.format("[%d,%d]", ps, starts[i]-1));
			}
			ps = ends[i] + 1;
		}
		if(ps <= end) { 
			inverted.add(new int[] { ps, end });
			//System.out.println(String.format("[%d,%d]", ps, end));
		}
		
		return inverted;
	}
	
	public static void printTree(Object t, PrintStream ps, int depth) { 
		for(int i = 0; i < depth; i++) { 
			ps.print("  ");
		}
		if(t instanceof Collection) { 
			ps.println("+");
			Collection stree = (Collection)t;
			for(Object st : stree) { 
				printTree(st, ps, depth+1);
			}
		} else{ 
			ps.println(String.format("* '%s'", t.toString()));
		}
	}

	private String value;
	private LinkedList<Range> ranges;
	private int maxDepth;
	
	public Parenthesization(String v) { 
		value = v;
		ranges = new LinkedList<Range>();
		maxDepth = 0;
		
		Stack<Range> current = new Stack<Range>();
		
		for(int i = 0; i < v.length(); i++) { 
			char c = v.charAt(i);
			if(c == '(') { 
				Range r = new Range(i, i, current.size()+1);
				current.push(r);
				maxDepth = Math.max(maxDepth, r.depth);
				ranges.addLast(r);
			} else if (c == ')') {
				if(current.isEmpty()) { 
					throw new IllegalArgumentException(String.format(
							"Illegal end-paren at index %d of '%s' : '%s'",
							i, v, v.substring(0, i+1)));
				} else { 
					current.pop();
				}
			} else {
				// do nothing.
			}
			
			for(Range rr : current) { 
				rr.end++;
			}
		}
		
		if(!current.isEmpty()) { 
			String msg = String.format("Unfinished parenthesis in '%s'", v);
			throw new IllegalArgumentException(msg);
		}
	}
	
	public int getMaxDepth() { return maxDepth; }
	
	public Collection<int[]> findRanges(int depth) { 
		LinkedList<int[]> depthRanges = new LinkedList<int[]>();
		for(Range r : ranges) { 
			if(r.depth==depth) { 
				int[] pair = new int[] { r.start, r.end };
				depthRanges.addLast(pair);
			}
		}
		return depthRanges;
	}
	
	public Object createTree() { 
		if(maxDepth == 0) { 
			return value;
		} else { 
			Vector v = new Vector();
			LinkedList<int[]> depth1 = new LinkedList<int[]>(findRanges(1));
			LinkedList<int[]> leftover = 
				new LinkedList<int[]>(invertIndices(depth1, 0, value.length()-1));
			
			while(!depth1.isEmpty() || !leftover.isEmpty()) { 
				int dstart = depth1.isEmpty() ? value.length() : depth1.getFirst()[0];
				int lstart = leftover.isEmpty() ? value.length() : leftover.getFirst()[0];
				if(lstart < dstart) { 
					int[] r = leftover.removeFirst();
					v.add(value.substring(r[0], r[1]+1));
				} else { 
					int[] r = depth1.removeFirst();
					Parenthesization p = new Parenthesization(value.substring(r[0]+1, r[1]));
					Vector vv = new Vector();
					Object ptree = p.createTree();
					if(ptree instanceof Collection) { 
						vv.addAll((Collection)ptree);
					} else { 
						vv.add(ptree);
					}
					v.add(vv);
				}
			}
			
			return v;
		}
	}
	
	public String getValue() { 
		return value;
	}
	
	public Collection<String> getSubstrings() {
		return getSubstrings(1);
	}
	
	public Collection<String> getSubstrings(int depth) {
		return getSubstrings(depth, new int[] { 0, value.length()-1 });
	}
	
	public Collection<String> getSubstrings(int depth, int[] range) { 
		Collection<int[]> depth1 = findRanges(depth);
		LinkedList<String> substrings = new LinkedList<String>();
		
		for(int[] p : depth1) { 
			if(p[0] >= range[0] && p[1] <= range[1]) { 
				substrings.addLast(value.substring(p[0]+1, p[1]));
			}
		}
		
		return substrings;				
	}
	
	private class Range { 
		public int start, end, depth;
		
		public Range(int s, int e, int d) { 
			start = s; 
			end = e;
			depth = d;
		}
	}
}
