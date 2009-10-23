/*
 * Author: tdanford
 * Date: Jun 18, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

public class SubsetClusterData implements ClusterData {
	
	private ClusterData data;
	private int start, end;
	private int probeOffset, probeCount;
	
	public SubsetClusterData(ClusterData d, int s, int e) { 
		data = d;
		start = s;
		end = e;
		probeOffset = data.segmentStart(start);
		probeCount = data.segmentEnd(end-1) - probeOffset + 1;
	}

	public int location(int j) {
		return data.location(j + probeOffset);
	}

	public int segmentEnd(int i) {
		return data.segmentEnd(i + start);
	}

	public int segmentStart(int i) {
		return data.segmentStart(i + start);
	}

	public int segments() {
		return end-start;
	}

	public int size() {
		return probeCount;
	}

	public Double[] values(int j) {
		return data.values(j+probeOffset);
	}

}
