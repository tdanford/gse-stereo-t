/*
 * Author: tdanford
 * Date: Jun 18, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import java.util.*;
import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;

public class DefaultClusterData implements ClusterData {
	
	private Cluster cluster;
	
	public DefaultClusterData(Cluster c) { 
		cluster = c;
	}

	public int location(int j) {
		return cluster.locations[j];
	}

	public int segmentEnd(int i) {
		return cluster.segmentEnd(i);
	}

	public int segmentStart(int i) {
		return cluster.segmentStart(i);
	}

	public int segments() {
		return cluster.segments.length;
	}

	public int size() {
		return cluster.locations.length;
	}

	public Double[] values(int j) {
		return cluster.channelValues(j);
	}

}
