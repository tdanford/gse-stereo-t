/*
 * Author: tdanford
 * Date: May 8, 2009
 */
package edu.mit.csail.cgs.sigma.expression.rma;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.cgstools.slicer.*;
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.WholeGenome;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.models.data.*;

public class HRMA extends Model {
	
	public static void main(String[] args) { 
		WorkflowProperties props = new WorkflowProperties();
		try {
			//String key = "s288c";
			String key = "txns288c";
			
			if(args.length > 0) { key = args[0]; }
			WholeGenome genome = WholeGenome.loadWholeGenome(props, key);
			
			genome.loadIterators();
			Iterator<DataSegment> segs = new SerialIterator<DataSegment>(
					genome.getWatsonSegments(), genome.getCrickSegments());
			
			segs = new FilterIterator<DataSegment,DataSegment>(new Filter<DataSegment,DataSegment>() { 
				public DataSegment execute(DataSegment s) { 
					return s.hasConsistentType(Segment.LINE, new Integer[] { 0, 2 }) ? s : null;
				}
			}, segs);
			
			HRMA hrma = new HRMA(segs, 500);
			
			int n = 50;
			
			for(int i = 0; i < n; i++) { 
				hrma.sample();
			}
			
			Integer[] fg = new Integer[] { 0, 2 };
			Integer[] bg = new Integer[] { 1, 3 };
			hrma.printDiffProbs(100, fg, bg);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public SegmentHRMA[] segments;
	
	public Double gamma_beta;  // parameters for exponential dist. on segment lvls.
	public FunctionModel betaLikelihood;
	
	public Hyperparameters<HRMA> slopeHypers;
	public FunctionModel slopeLikelihood;
	
	public Double var_alpha;  // probe-specific levels.
	public FunctionModel alphaLikelihood;
	
	public HRMA() {}
	
	public HRMA(Collection<DataSegment> segs) { 
		update();
		segments = new SegmentHRMA[segs.size()];
		int i = 0;
		for(DataSegment s : segs) { 
			segments[i++] = new SegmentHRMA(this, s);
		}
		System.out.println(String.format("# Segments: %d", segments.length));
	}
	
	public HRMA(Iterator<DataSegment> segs) { 
		this(collectSegments(segs, -1));
	}
	
	public HRMA(Iterator<DataSegment> segs, int c) { 
		this(collectSegments(segs, c));
	}
	
	public void saveDifferentials(Integer[] fg, Integer[] bg) {
		System.out.println(String.format("Saving differentials (%d)", segments.length));
		for(int i = 0; i < segments.length; i++) { 
			segments[i].saveDifferentials(fg, bg);
			if(i > 0 && i % 100 == 0) { System.out.print("."); System.out.flush(); }
			if(i > 0 && i % 1000 == 0) { System.out.println("(" + (i/1000) + "k)"); }
		}
		System.out.println();
	}
	
	public void printDiffProbs(int n, Integer[] fg, Integer[] bg) { 
		for(int i = 0; i < segments.length; i++) { 
			double[] p = segments[i].diffProbs(n, fg, bg);
			System.out.println(String.format("%.3f\t%.3f\t%.3f\t%s",  
					p[0], p[1], p[2], segments[i].name));
		}
	}
	
	public static Collection<DataSegment> collectSegments(Iterator<DataSegment> segs, int c) { 
		ArrayList<DataSegment> slist = new ArrayList<DataSegment>();
		int si = 0;
		while((c == -1 || si < c) && segs.hasNext()) {
			slist.add(segs.next());
			si += 1;
		}
		return slist;
	}
	
	public void update() { 
		gamma_beta = 1.0;
		slopeHypers = new ExponentialHyperparameters(1.0); 
		var_alpha = 1.0;
		betaLikelihood = new FunctionModel() {
			public Double eval(Double beta) {
				return logLikeBeta(beta);
			} 
		};
		slopeLikelihood = slopeHypers.likelihood();
		alphaLikelihood = new FunctionModel() {
			public Double eval(Double alpha) {
				return logLikeAlpha(alpha);
			} 
		};
	}

	public void sample(int n) {
		for(int i = 0; i < n; i++) { 
			sample();
		}
	}

	public void sample() {
		System.out.println(String.format("Sampling: (%d)", segments.length));
		for(int i = 0; i < segments.length; i++) { 
			segments[i].sample();
			if(i > 0 && i % 100 == 0) { System.out.print("."); System.out.flush(); }
			if(i > 0 && i % 1000 == 0) { System.out.println("(" + (i/1000) + "k)"); }
		}
		System.out.println("\nEstimating hyper-parameters...");
		gamma_beta = estimateBeta();
		var_alpha = estimateAlphaVar();
		slopeHypers.estimate(this);
		System.out.println(String.format("\tBeta: %f", gamma_beta));
		System.out.println(String.format("\tVar-Alpha: %f", var_alpha));
		
		String[] names = slopeHypers.names();
		Double[] values = slopeHypers.values();
		for(int i = 0; i < names.length; i++) { 
			System.out.println(String.format("\tSlope %s: %f", names[i], values[i]));
		}
	}
	
	public Double estimateBeta() { 
		double betasum = 0.0;
		int c = 0;
		for(int i = 0; i < segments.length; i++) { 
			for(int j = 0; j < segments[i].lineLevels.length; j++) { 
				betasum += segments[i].lineLevels[j];
				c += 1;
			}
		}
		
		betasum /= (double)Math.max(c, 1);
		return c != 0 ? 1.0 / betasum : 1.0;
	}
	
	public Double estimateAlphaVar() { 
		double alphasum = 0.0;
		int c = 0;
		for(int i = 0; i < segments.length; i++) {
			for(int j = 0; j < segments[i].alphas.length; j++) { 
				double alpha = segments[i].alphas[j];
				alphasum += (alpha*alpha);
			}
			c += segments[i].alphas.length;
		}
		alphasum /= (double)Math.max(1, c);
		return alphasum;				
	}
	
	public Double logLikeBeta(Double beta) { 
		return Math.log(gamma_beta) - gamma_beta * beta;
	}
	
	public Double logLikeAlpha(Double alpha) { 
		double diff = alpha;
		double expt = -(diff*diff) / (2.0*var_alpha);
		//double logc = -0.5 * Math.log(Math.sqrt(2.0 * var_alpha * Math.PI));
		//return logc + expt;
		return expt;
	}

	public static class ExponentialHyperparameters extends Model implements Hyperparameters<HRMA> {
		
		public Double lambda;
		private Double logLambda;
		
		public ExponentialHyperparameters() {
			lambda = 1.0;
			update();
		}
		
		public ExponentialHyperparameters(Double lmbda) { 
			lambda = lmbda;
			update();
		}
		
		public void update() { 
			logLambda = Math.log(lambda);
		}

		public void estimate(HRMA hrma) {
			double sum = 0.0;
			for(int i = 0; i < hrma.segments.length; i++) { 
				sum += hrma.segments[i].lineSlope;
			}
			sum /= (double)Math.max(1, hrma.segments.length);
			lambda = 1.0 / sum;
			update();
		}

		public FunctionModel likelihood() {
			return new FunctionModel() { 
				public Double eval(Double x) { 
					if(x < 0.0) { return -Double.MAX_VALUE; }
					return logLambda - lambda * x; 
				}
			};
		}

		public int size() {
			return 1;
		}
		
		public String[] names() { 
			return new String[] { "lambda" };
		}

		public Double[] values() {
			return new Double[] { lambda };
		} 
	}
}
