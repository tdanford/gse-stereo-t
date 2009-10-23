/*
 * Author: tdanford
 * Date: Jun 26, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.fitters;

import java.util.*;
import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.Endpts;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.models.Model;

public class EndptsFit extends Model implements Comparable<EndptsFit> {

	public Cluster cluster;
	public Endpts[] epts;
	public Double[][] gammas;
	public Double lambda, variance;
	public Double score, likelihood;

	public EndptsFit(Cluster c, Endpts[] e, double[][] g, double l, double v) {
		this(c, e, g, l, v, 0.0);
	}
	
	public EndptsFit(Cluster c, Endpts[] e, double[][] g, double l, double v, double s) {
		this(c, e, g, l, v, s, s);
	}
	
	public EndptsFit(Cluster c, Endpts[] e, double[][] g, double l, double v, double s, double lk) { 
		cluster = c;
		epts = e;
		gammas = new Double[g.length][];
		for(int i = 0; i < gammas.length; i++) { 
			gammas[i] = new Double[g[i].length];
			for(int j = 0; j < g[i].length; j++) { 
				gammas[i][j] = g[i][j];
			}
		}
		lambda = l;
		variance = v;
		score = s;
		likelihood = lk;
	}
	
	public int hashCode() { 
		int code = 17;
		code += cluster.hashCode(); code *= 37;
		long bits = Double.doubleToLongBits(score);
		code += (int)(bits >> 32); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof EndptsFit)) { return false; }
		EndptsFit f = (EndptsFit)o;
		if(!cluster.equals(f.cluster)) { return false; }
		if(epts.length != f.epts.length) { return false; }
		if(gammas.length != f.gammas.length) { return false; }
		for(int k = 0; k < gammas.length; k++) { 
			for(int i = 0; i < epts.length; i++) {
				if(Math.abs(gammas[k][i] - f.gammas[k][i]) >= 1.0e-6) { 
					return false;
				}
			}
		}
		if(Math.abs(variance - f.variance) >= 1.0e-6) { 
			return false;
		}
		if(Math.abs(lambda - f.lambda) >= 1.0e-6) { 
			return false;
		}
		return true;
	}
	
	public int compareTo(EndptsFit f) { 
		if(score > f.score) { return -1; }
		if(score < f.score) { return 1; }
		if(epts.length < f.epts.length) { return -1; }
		if(epts.length > f.epts.length) { return 1; }
		return 0;
	}
	
	public Collection<TranscriptCall> calls() { 
		ArrayList<TranscriptCall> calllist = new ArrayList<TranscriptCall>();
		for(int i = 0; i < epts.length; i++) {
			Double[] itys = new Double[gammas.length];
			for(int k = 0; k < gammas.length; k++) { 
				itys[k] = gammas[k][i];
			}
			double fall = lambda;
			TranscriptCall tcall = new TranscriptCall(cluster.chrom(), cluster.strand(),
					cluster.segmentStart(epts[i].start), 
					cluster.segmentEnd(epts[i].end-1), 
					itys, fall);
			calllist.add(tcall);
		}
		return calllist;
	}

}
