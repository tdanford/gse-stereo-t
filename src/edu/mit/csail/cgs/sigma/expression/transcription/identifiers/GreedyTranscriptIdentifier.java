/*
 * Author: tdanford
 * Date: Mar 25, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.identifiers;

import java.util.*;
import java.io.*;

import Jama.Matrix;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.expression.models.Transcript;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.utils.models.data.DataRegression;
import edu.mit.csail.cgs.utils.models.data.XYPoint;

public class GreedyTranscriptIdentifier {
	
	private Genome genome;
	private String chrom;
	private char strand;
	
	private DataSegment[] segs;
	private Double[][] residuals;
	private Double secondMoment;
	
	public GreedyTranscriptIdentifier(Genome g, Collection<DataSegment> s) { 
		this(g, s.toArray(new DataSegment[0]));
	}

	public GreedyTranscriptIdentifier(Genome g, DataSegment[] s) {
		genome = g;
		segs = s.clone();
		Arrays.sort(segs);
	
		chrom = segs[0].chrom;
		strand = segs[0].strand.charAt(0);
	}
	
	public Collection<Transcript> fitTranscripts(int channel) {
		buildResiduals(channel);
		ArrayList<Transcript> ts = new ArrayList<Transcript>();
		Set<TranscriptFit> fits = new HashSet<TranscriptFit>();
		
		double coef = 0.5;
		int maxTranscripts = 5;
		
		for(int i = 0; i < maxTranscripts; i++) { 
			TranscriptFit fit = findNextTranscript(fits);
	
			if(fit.p[0] <= secondMoment * coef) {
				fits.add(fit);
			
				int start = segs[fit.is[0]].firstLocation();
				int end = segs[fit.is[1]].lastLocation();
				Transcript t = 
					new Transcript(genome, chrom, start, end, strand, 
							new double[] { fit.p[1], fit.p[2] });
				ts.add(t);
				
				System.out.println(String.format("Transcript: %d-%d:%s", start, end, strand));
			}
		}
		
		return ts;
	}
	
	private class TranscriptFit { 
		public Double[] p;
		public Integer[] is;
		
		public TranscriptFit(Double[] _p, Integer[] _is) { 
			is = _is;
			p = _p;
		}
		
		public int hashCode() { 
			int code = 17;
			code += is[0]; code *= 37;
			code += is[1]; code *= 37;
			return code;
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof TranscriptFit)) { return false; }
			TranscriptFit f = (TranscriptFit)o;
			return f.is[0].equals(is[0]) && f.is[1].equals(is[1]);
		}
	}
	
	private TranscriptFit findNextTranscript(Collection<TranscriptFit> fits) {
		TranscriptFit nextFit = null;
		Integer fi1 = null, fi2 = null; 
		Double[] params = null;
		
		for(int i1 = 0; i1 < segs.length; i1++) { 
			for(int i2 = i1; i2 < segs.length; i2++) { 
				Double[] p = conditionalResidualSecondMoment(i1, i2);
				if(params == null || p[0] < params[0]) {
					fi1 = i1; fi2 = i2;
					params = p;
					nextFit = new TranscriptFit(params, new Integer[] { fi1, fi2 });
				}
			}
		}
		
		return new TranscriptFit(params, new Integer[] { fi1, fi2 });
	}
	
	private void buildResiduals(int ch) { 
		residuals = new Double[segs.length][];
		for(int i = 0; i < segs.length; i++) { 
			residuals[i] = segs[i].dataValues[ch].clone();
		}
		secondMoment = residualSecondMoment();
	}
	
	private Double residualSecondMoment() {
		double sum = 0.0;
		int length = 0;
		for(int i = 0; i < residuals.length; i++) { 
			for(int j = 0; j < residuals[i].length; j++) { 
				double v = residuals[i][j];
				sum += (v*v);
			}
			length += residuals[i].length;
		}
		return sum / (double)length; 
	}	
	
	private void updateResiduals(int i1, int i2, Double[] p) { 
		double mean = p[1], slope = p[2];
		int baseLocation = segs[i2].end;
		
		int length = 0;
		double sum = 0.0;
		for(int i = 0; i < residuals.length; i++) { 
			for(int j = 0; j < residuals[i].length; j++) { 
				if(i >= i1 && i <= i2) { 
					double x = (double)(baseLocation - segs[i].dataLocations[j]);
					double pred = x * slope + mean;
					residuals[i][j] -= pred;
				}
				
				double v = residuals[i][j];
				sum += (v*v);
			}
			length += residuals[i].length;
		}
		secondMoment = sum / (double)length;
	}
	
	private Double[] conditionalResidualSecondMoment(int i1, int i2) {
		double sum = 0.0;
		
		DataFrame<XYPoint> f = new DataFrame<XYPoint>(XYPoint.class);
		int baseLocation = segs[i2].end;
		for(int i = i1; i <= i2; i++) {
			for(int j = 0; j < residuals[i].length; j++) { 
				int loc = segs[i].dataLocations[j];
				int diff = baseLocation - loc;
				f.addObject(new XYPoint((double)diff, residuals[i][j]));
			}
		}
		DataRegression<XYPoint> regression = 
			new DataRegression<XYPoint>(f, "y ~ x + 1");
		regression.calculate();
		Matrix m = regression.getBetaHat();
		
		double mean = m.get(0, 0);
		double slope = m.get(1, 0);
		
		int length = 0;
		for(int i = 0; i < residuals.length; i++) { 
			for(int j = 0; j < residuals[i].length; j++) { 
				double v = residuals[i][j];

				if(i >= i1 && i <= i2) { 
					double x = (double)(baseLocation - segs[i].dataLocations[j]);
					double pred = x * slope + mean;
					v -= pred;
				}
				
				sum += (v*v);
			}
			length += residuals[i].length;
		}
		double var = sum / (double)length;
		
		return new Double[] { var, mean, slope };
	}
}
