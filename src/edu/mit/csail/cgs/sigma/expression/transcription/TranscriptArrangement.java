/*
 * Author: tdanford
 * Date: Dec 2, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription;

import java.io.PrintStream;
import java.util.*;

import edu.mit.csail.cgs.sigma.Printable;
import edu.mit.csail.cgs.utils.models.Model;

import Jama.Matrix;

public class TranscriptArrangement extends Model {
	
	private static boolean separateSlopes = false;

	public Cluster cluster;
	public Call[] calls;
	
	/**
	 * Indicates how many transcripts are assigned to a segment
	 */
	public Integer[] segmentCoverage;
	
	public TranscriptArrangement() {}
	
	public TranscriptArrangement(Cluster c, Collection<Call> cs) { 
		cluster = c;
		segmentCoverage = new Integer[cluster.segments.length];
		for(int i = 0; i < segmentCoverage.length; i++) { 
			segmentCoverage[i] = 0;
		}
		
		calls = new Call[cs.size()];
		int i = 0;
		for(Call call : cs) { 
			calls[i] = new Call(call);
			for(int j = calls[i].start; j < calls[i].end; j++) { 
				segmentCoverage[j] += 1;
			}
			i += 1;
		}
	}
	
	public Integer callStartpoint(int i) { 
		return cluster.segmentStart(calls[i].start);
	}
	
	public Integer callEndpoint(int i) { 
		int end = calls[i].end;
		return cluster.segmentEnd(end-1);
	}
	
	public Collection<Call> getCalls() { 
		ArrayList<Call> callList = new ArrayList<Call>();
		for(Call c : calls) { 
			callList.add(c);
		}
		return callList;
	}
	
	public boolean containsOverlap() { 
		for(int i = 0; i < calls.length; i++) { 
			for(int j = i + 1; j < calls.length; j++) { 
				if(calls[i].overlaps(calls[j])) { 
					return true;
				}
			}
		}
		return false;
	}
	
	public String diagram() { 
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < cluster.segments.length; i++) { 
			sb.append("-"); 
		}
		sb.append("\n");
		
		for(int i = 0; i < calls.length; i++) { 
			for(int j = 0; j < calls[i].end; j++) { 
				if(j >= calls[i].start) { 
					sb.append("*");
				} else { 
					sb.append(" ");
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public Matrix createObservationMatrix() {
		int rows = cluster.locations.length * cluster.values.length, cols = 1;
		
		Matrix m = new Matrix(rows, cols, 0.0);
		
		for(int i = 0, k = 0; i < cluster.locations.length; i++) {
			for(int c = 0; c < cluster.channels.length; c++, k++) {
				int ch = cluster.channels[c];
				Double value = cluster.values[ch][i];
				m.set(k, 0, value);
			}
		}
		
		return m;		
	}
	
	public Matrix createDesignMatrix() {
		int rows = cluster.locations.length * cluster.channels.length;
		int numGammas = calls.length * cluster.channels.length;
		int cols = separateSlopes ? numGammas * 2 : numGammas + 1;

		Matrix m = new Matrix(rows, cols, 0.0);

		for(int i = 0, k = 0; k < cluster.segments.length; k++) { 

			Set<Integer> callIndices = overlappingCallIndices(k);

			for(int j = 0; j < cluster.segments[k].dataLocations.length; j++) { 

				double totalDist = 0.0;
				int cOffset = 0;

				for(int c = 0; c < cluster.channels.length; c++, i++) { 
					for(Integer ci : callIndices) { 
						int callEndLocation = callEndpoint(ci);
						int probeLocation = cluster.locations[i];
						int distance = callEndLocation-probeLocation;

						m.set(i, cOffset + ci, 1.0);

						if(separateSlopes) { 
							m.set(i, cOffset + calls.length + ci, (double)distance);
						}
						totalDist += (double)distance;
					}

					if(!separateSlopes) { 
						m.set(i, cols-1, totalDist);
						cOffset += calls.length;
					} else { 
						cOffset += (calls.length*2);
					}
				}
			}
		}
		
		return m;
	}
	
	/**
	 * 
	 * @param seg the index of the segment <br>
	 * <tt>0 <= seg <= cluster.segments.length-1</tt>
	 * @return the transcripts that overlap this segment
	 */
	public Set<Call> overlappingCalls(int seg) { 
		TreeSet<Call> set = new TreeSet<Call>();
		for(Call c : calls) { 
			if(c.start <= seg && c.end > seg) { 
				set.add(c);
			}
		}
		return set;
	}
	
	public Integer[] overlapCount() { 
		Integer[] counts = new Integer[cluster.segments.length];
		for(int i = 0; i < counts.length; i++) { counts[i] = 0; }
		for(Call c : calls) { 
			for(int j = c.start; j < c.end; j++) { 
				counts[j] += 1;
			}
		}
		return counts;
	}
	
	public Set<Integer> overlappingCallIndices(int seg) {
		TreeSet<Integer> set = new TreeSet<Integer>();
		for(int i = 0; i < calls.length; i++) { 
			//System.out.println(String.format("\tocI: %d,%d", calls[i].start, calls[i].end));
			if(calls[i].start <= seg && calls[i].end > seg) { 
				set.add(i);
			}
		}
		//System.out.println(String.format("overlappingCallIndices: %d -> %s", seg, set.toString()));
		return set;
	}
	
	/**
	 * checks whether every segment of the cluster each covered by a transcript
	 * @return True, if yes. False, otherwise
	 */
	public boolean coversCluster() { 
		for(int i = 0; i < segmentCoverage.length; i++) { 
			if(segmentCoverage[i] == 0) { return false; }
		}
		return true;
	}
	
	public String toString() {
		Integer[] bks = cluster.breakpoints();
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < calls.length; i++) { 
			if(i > 0) { sb.append(","); }
			int start = bks[calls[i].start], end = bks[calls[i].end];
			int length = end-start;
			sb.append(String.format("%s:%dbp", calls[i].toString(), length));
					
		}
		
		sb.append(" > "); 
		for(int i = 0; i < segmentCoverage.length; i++) { 
			if(i > 0) { sb.append(","); }
			sb.append(String.valueOf(segmentCoverage[i]));
		}
		return sb.toString();
	}
}
