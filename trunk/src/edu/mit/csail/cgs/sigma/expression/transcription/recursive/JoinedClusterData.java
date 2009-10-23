/*
 * Author: tdanford
 * Date: Jun 18, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

public class JoinedClusterData implements ClusterData {
	
	private ClusterData d1, d2;
	
	public JoinedClusterData(ClusterData cd1, ClusterData cd2) {
		d1 = cd1; d2 = cd2;
	}

	public int location(int j) {
		if(j < d1.size()) { 
			return d1.location(j);
		} else { 
			return d2.location(j-d1.size());
		}
	}
	
	public String toString() { 
		return String.format("%s%s", d1.toString(), d2.toString());
	}

	public int segmentEnd(int i) {
		if(i < d1.segments()) { 
			return d1.segmentEnd(i); 
		} else { 
			return d2.segmentEnd(i-d1.segments());
		}
	}

	public int segmentStart(int i) {
		if(i < d1.segments()) { 
			return d1.segmentStart(i); 
		} else { 
			return d2.segmentStart(i-d1.segments());
		}
	}

	public int segments() {
		return d1.segments() + d2.segments();
	}

	public int size() {
		return d1.size() + d2.size();
	}

	public Double[] values(int j) {
		if(j < d1.size()) { 
			return d1.values(j);
		} else { 
			return d2.values(j-d1.size());
		}
	}

}
