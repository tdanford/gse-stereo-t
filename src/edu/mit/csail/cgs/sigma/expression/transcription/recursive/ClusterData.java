/*
 * Author: tdanford
 * Date: Jun 10, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

public interface ClusterData {

	public int size();               // number of probes 
	public int segments();           // number of segments
	public int segmentStart(int i);  // returns index of start. (inclusive)
	public int segmentEnd(int i);    // returns index+1 of end. (i.e. exclusive)
	
	public int location(int j);      // location (genomic) of probe j
	public Double[] values(int j);   // array of values of probe j
}
