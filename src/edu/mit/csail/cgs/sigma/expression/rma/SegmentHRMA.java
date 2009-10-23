/*
 * Author: tdanford
 * Date: May 7, 2009
 */
package edu.mit.csail.cgs.sigma.expression.rma;

import java.util.*;
import java.io.*;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.binary.ProductFunction;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.binary.SumFunction;
import edu.mit.csail.cgs.cgstools.slicer.*;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.models.data.*;

public class SegmentHRMA extends Model {
	
	public String name;
	public Integer start, end;

	private HRMA rma; 
	private HData data;
	private Integer i1, i2;
	private int[] d;
	
	public Double[] alphas;
	public Double[] lineLevels; 
	public Double lineSlope;
	public Double yvar;
	
	private FunctionModel[] lineLevelLikes;
	private FunctionModel[] alphaLikes;
	private FunctionModel lineSlopeLike;
	
	private SliceSampler[] lineLevelSamplers;
	private SliceSampler[] alphaSamplers;
	private SliceSampler lineSlopeSampler;
	
	private LinkedList<Double> differentials;
	private Double[] meanLevels, meanAlphas;
	private Double meanSlope;
	
	public SegmentHRMA() {}

	public SegmentHRMA(HRMA r, boolean strand, Integer s, Integer e, HData data, int i1, int i2) {
		rma = r;
		start = s; end = e;
		name = String.format("%d-%d", s, e);

		lineSlope = 0.001;
		yvar = 1.0;
		lineLevels = new Double[data.y.length];
		for(int j = 0; j < lineLevels.length; j++) { 
			lineLevels[j] = 2.0;
		}
		
		this.i1 = i1; this.i2 = i2;
		this.data = data;
		d = new int[i2-i1];
		alphas = new Double[d.length];

		for(int i = 0; i < d.length; i++) {
			// these need to be negative, because they're added into the predicted
			// value (* the lineSlope parameter). 
			d[i] = strand ? 
					-(end - data.x[i1 + i]) : 
					-(data.x[i1 + i] - start);
			alphas[i] = 0.0;
		}
		
		update();
	}
	
	public SegmentHRMA(HRMA rma, DataSegment seg) { 
		this(rma, seg.strand.equals("+"), 
				seg.start, seg.end, 
				new HData(seg), 
				0, seg.dataLocations.length);
		
		name = String.format("%s:%d-%d:%s", seg.chrom, seg.start, seg.end, seg.strand);
	}
	
	private void update() {
		int c = channels();
		double w = 0.01;
		
		lineLevelLikes = new FunctionModel[c];
		lineLevelSamplers = new SliceSampler[c];
		
		lineSlopeLike = new SumFunction(rma.slopeLikelihood, new LineSlopeLikelihood());
		lineSlopeSampler = new SliceSampler(lineSlopeLike, w, lineSlope);
		
		for(int i = 0; i < c; i++) { 
			lineLevelLikes[i] = new SumFunction(
					rma.betaLikelihood, new LineLevelLikelihood(i));
			lineLevelSamplers[i] = new SliceSampler(lineLevelLikes[i], w, lineLevels[i]);
		}

		alphaLikes = new FunctionModel[length()];
		alphaSamplers = new SliceSampler[length()];
		
		for(int i = 0; i < alphaLikes.length; i++) { 
			alphaLikes[i] = new SumFunction(
					rma.alphaLikelihood, new AlphaLikelihood(i));
			alphaSamplers[i] = new SliceSampler(alphaLikes[i], w, alphas[i]);
		}
	}
	
	public Double maxLevel(Integer[] chs) { 
		Double m = null;
		for(int i = 0; i < chs.length; i++) { 
			if(m == null || lineLevels[chs[i]] > m) { 
				m = lineLevels[chs[i]];
			}
		}
		return m;
	}
	
	public Double minLevel(Integer[] chs) { 
		Double m = null;
		for(int i = 0; i < chs.length; i++) { 
			if(m == null || lineLevels[chs[i]] < m) { 
				m = lineLevels[chs[i]];
			}
		}
		return m;
	}
	
	public int diffCoding(Integer[] fg, Integer[] bg) { 
		if(minLevel(fg) > maxLevel(bg)) { 
			return 1; 
		} else if (maxLevel(fg) < minLevel(bg)) { 
			return -1; 
		} else { 
			return 0;
		}
	}
	
	public double meanLevel(Integer[] chs) { 
		double sum = 0.0;
		for(int i = 0; i < chs.length; i++) { 
			sum += lineLevels[chs[i]];
		}
		return sum / (double)Math.max(chs.length, 1);
	}
	
	public double differentialLevels(Integer[] fg, Integer[] bg) { 
		return meanLevel(fg) - meanLevel(bg);
	}
	
	public void sample(int n, Integer[] fg, Integer[] bg) { 
		differentials = new LinkedList<Double>();
		meanLevels = new Double[lineLevels.length];
		meanAlphas = new Double[alphas.length];
		meanSlope = 0.0;
		for(int i = 0; i < meanLevels.length; i++) { meanLevels[i] = 0.0; }
		for(int i = 0; i < meanAlphas.length; i++) { meanAlphas[i] = 0.0; }
		
		int k = 1;
		for(int i = 0; i < n; i++) { 
			sample(k);
			for(int j = 0; j < meanLevels.length; j++) { meanLevels[j] += lineLevels[j]; }
			for(int j = 0; j < meanAlphas.length; j++) { meanAlphas[j] += alphas[j]; }
			meanSlope += lineSlope;
			differentials.add(differentialLevels(fg, bg));
		}
		for(int j = 0; j < meanLevels.length; j++) { meanLevels[j] /= (double)n; }
		for(int j = 0; j < meanAlphas.length; j++) { meanAlphas[j] /= (double)n; }
		meanSlope /= (double)n;
	}
	
	public double[] diffProbs(int n, Integer[] fg, Integer[] bg) {
		int k = 2;
		double[] p = new double[] { 0.0, 0.0, 0.0 }; 
		for(int i = 0; i < n; i++) { 
			sample(k);
			int code = diffCoding(fg, bg);
			p[code+1] += 1.0;
		}
		for(int i = 0; i < p.length; i++) { 
			p[i] /= (double)Math.max(1, n);
		}
		return p;
	}
	
	public void sample(int n) { 
		for(int i = 0; i < n; i++) { 
			sample();
		}
	}
	
	public void sample() { 
		for(int i = 0; i < alphas.length; i++) { 
			alphas[i] = alphaSamplers[i].nextLogX();
		}
		
		for(int j = 0; j < lineLevels.length; j++) { 
			lineLevels[j] = lineLevelSamplers[j].nextLogX();
		}
		
		lineSlope = lineSlopeSampler.nextLogX();
	}
	
	public Integer channels() { 
		return data.y.length;
	}
	
	public Integer length() { 
		return i2-i1;
	}

	public class LineLevelLikelihood extends FunctionModel {
		public Integer i;
		public LineLevelLikelihood() { i = -1; }
		public LineLevelLikelihood(Integer ii) { i = ii; }

		public Double eval(Double v) {
			return logLineLevelLikelihood(i, v);
		} 
	}
	
	public class AlphaLikelihood extends FunctionModel {
		public Integer i;
		public AlphaLikelihood() { i = -1; }
		public AlphaLikelihood(Integer ii) { i = ii; }

		public Double eval(Double v) {
			return logAlphaLikelihood(i, v);
		} 
	}
	
	public class LineSlopeLikelihood extends FunctionModel {
		public LineSlopeLikelihood() {}

		public Double eval(Double v) {
			return logLineSlopeLikelihood(v);
		} 
	}
	
	public double predicted(int i, int j) { 
		double alpha = alphas[i];
		double level = lineLevels[j];
		double dig = lineSlope * d[i];
		return alpha + level + dig;
	}
	
	public double residual(int i, int j) { 
		return data.y[j][i] - predicted(i, j);
	}
	
	public double logAlphaLikelihood(int ri, double ralf) {
		//System.out.print(String.format("logAlphaLikelihood(%d, %.5f)", ri, ralf)); System.out.flush();
		double ll = 0.0;
		double var2 = 2.0 * yvar;
		double c = -Math.log(Math.sqrt(var2 * Math.PI));

		for(int j = 0; j < data.y.length; j++) {
			double p_ij = ralf + lineLevels[j] + lineSlope * d[ri];
			double e_ij = data.y[j][ri] - p_ij;
			ll += (-(e_ij*e_ij) / var2) + c;
		}
		//System.out.println(String.format("=%f", ll));
		return ll;
	}
	
	public double logLineLevelLikelihood(int rj, double lvl) {
		//System.out.print(String.format("logLineLevelLikelihood(%d, %.5f)", rj, lvl)); System.out.flush();
		double ll = 0.0;
		double var2 = 2.0 * yvar;
		double c = -Math.log(Math.sqrt(var2 * Math.PI));

		for(int i = i1; i < i2; i++) { 
			int k = i - i1;
			double p_ij = alphas[k] + lvl + lineSlope * d[k];
			double e_ij = data.y[rj][i] - p_ij;
			ll += (-(e_ij*e_ij) / var2) + c;
		}
		//System.out.println(String.format("=%f", ll));
		return ll;
	}

	public double logLineSlopeLikelihood(double slp) {
		//System.out.print(String.format("logLineSlopeLikelihood(%.5f)", slp)); System.out.flush();
		double ll = 0.0;
		double var2 = 2.0 * yvar;
		double c = -Math.log(Math.sqrt(var2 * Math.PI));

		for(int j = 0; j < channels(); j++) { 
			for(int i = i1; i < i2; i++) { 
				int k = i - i1;
				double p_ij = alphas[k] + lineLevels[j] + slp * d[k];
				double e_ij = data.y[j][i] - p_ij;
				ll += (-(e_ij*e_ij) / var2) + c;
			}
		}
		//System.out.println(String.format("=%f", ll));
		return ll;
	}

	public void saveDifferentials(Integer[] fg, Integer[] bg) {
		sample(100, fg, bg);
		data.saveDifferentials(meanLevels, meanSlope, meanAlphas, differentials.toArray(new Double[0]));
	}
}
