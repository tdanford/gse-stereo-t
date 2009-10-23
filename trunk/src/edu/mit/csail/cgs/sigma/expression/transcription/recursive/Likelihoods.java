/*
 * Author: tdanford
 * Date: Jun 15, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import java.util.*;
import java.awt.Color;
import java.io.*;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.*;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.binary.DifferenceFunction;
import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.viz.paintable.Paintable;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.layout.StackedPaintable;

public class Likelihoods extends FunctionOptimization {
	
	public static void main(String[] args) {
		
		int N = 100;
		int[] x = new int[N];
		double noise = 0.5;
		
		double w = 1000.0 / (double)(N + 1);
		for(int i = 0; i < x.length; i++) { 
			x[i] = (int)Math.round((double)(i + 1) * w);
		}
		
		TestData d = new TestData(x, noise);
		
		d.addTranscript(Math.exp(10.0), 1, 300);
		d.addTranscript(Math.exp(20.0), 200, 800);
		d.addTranscript(Math.exp(15.0), 700, 1000);
		
		d.generate();
		double variance = d.noise() * d.noise();
		double[] y = d.y()[0];
		
		double[] gs = new double[d.getNumTranscripts()];
		for(int i = 0; i < gs.length; i++) { gs[i] = 1.0; }
		
		double lmbda = 1.0e-6;
		
		for(int i = 0; i < x.length; i++) { 
			System.out.println(String.format("%d (%d) : %.2f (%.2f)", i, x[i], y[i], Math.exp(y[i])));
		}
		
		Likelihoods f0 = new Likelihoods(y, gs, x, d.threePrime(), variance, lmbda);
		FunctionModel ff = f0.getDLGammaFunction(2);
		evalf(ff, 0.0);
		evalf(ff, 1.0);
		evalf(ff, 2.0);
		
		for(int t = 0; t < d.getNumTranscripts(); t++) { 
			int[] b = d.indexBounds(t);
			f0.setTranscriptBounds(t, b[0], b[1]+1);
			//f1.setTranscriptBounds(t, b[0], b[1]+1);
			System.out.println(String.format("T %d : [%d, %d)", t, b[0], b[1]));
		}

		int paintT = 1;
		FunctionModel like = f0.getLGammaFunction(paintT);
		FunctionModel dlike = f0.getDLGammaFunction(paintT);
		FunctionModelPaintable lPaint = new FunctionModelPaintable(like);
		FunctionModelPaintable dlPaint = new FunctionModelPaintable(dlike);

		lPaint.synchronizeProperty(FunctionModelPaintable.xScaleKey, dlPaint);

		/*
		double fzero = f.eval(zero);
		double logzero = Math.log(zero);
		*/

		double lz = -50.0, uz = 2.0 * Math.exp(5.0);
		double ly = -1.0;
		double uy = 1.0;
		double h = Math.abs(uy - ly);
		
		debugPrintZeroFinding = true;
		f0.setDebugPrint(true);
		
		//f0.gradientOptimize();
		f0.coordinateOptimize();
		
		double max = //like.eval(f0.gammas[paintT]);
			10.0;
		double min = //Math.min(like.eval(lz), like.eval(uz));
			-150.0;
		System.out.println(String.format("min,max: %f, %f", min, max));
		for(int i = 0; i < 10; i++) { 
			double xx = lz + ((uz-lz) / 11.0) * (double)(i + 1);
			evalf(like, xx);
		}
		
		ly = Math.min(ly, uy);
		uy = Math.max(ly, uy);
		
		lPaint.setProperty(FunctionModelPaintable.xScaleKey, new PaintableScale(lz, uz));
		dlPaint.setProperty(FunctionModelPaintable.xScaleKey, new PaintableScale(lz, uz));
		
		lPaint.setProperty(FunctionModelPaintable.yScaleKey, new PaintableScale(min, max));
		dlPaint.setProperty(FunctionModelPaintable.yScaleKey, new PaintableScale(ly, uy));
		
		dlPaint.setProperty(FunctionModelPaintable.colorKey, Color.blue);

		//Paintable p = new StackedPaintable(lPaint);
		Paintable p = new StackedPaintable(lPaint, dlPaint);
		
		//PaintableFrame pf = new PaintableFrame("gamma-derivative", p);

		double[] gammas = f0.gammas();
		for(int i = 0; i < gammas.length; i++) { 
			System.out.println(String.format("Gamma %d: %.2f", i, Math.log(gammas[i])));
		}
		System.out.println(String.format("Lambda: %f", f0.lambda));
	}
	
	private static void evalf(FunctionModel f, double x) { 
		System.out.println(String.format("f(%.3f) = %f", x, f.eval(x)));
	}

	private double[] y, gammas;
	private int[] x, threeprime;
	private double variance, lambda;
	private Map<Integer,Set<Integer>> transcripts;
	private boolean debugPrint;
	
	public Likelihoods(Cluster c, Endpts[] epts, Integer ch) {
		y = c.channeldValues(ch);
		x = c.channeliLocations();
		
		variance = 1.0;
		lambda = 0.001;
		debugPrint = false;
		
		gammas = new double[epts.length];
		threeprime = new int[epts.length];
		
		transcripts = new TreeMap<Integer,Set<Integer>>();
		for(int i = 0; i < y.length; i++) {
			transcripts.put(i, new TreeSet<Integer>());
		}

		for(int i = 0; i < gammas.length; i++) { 
			gammas[i] = 1.0;
			int startIdx = epts[i].start;
			int endIdx = epts[i].end;
			threeprime[i] = c.segments[endIdx-1].end;
		
			int j1 = c.channelSegmentOffset(startIdx);
			int j2 = endIdx < c.segments.length ? 
					c.channelSegmentOffset(endIdx) :  
					y.length;
					
			if(j2 > y.length || j1 < 0) { 
				throw new IllegalArgumentException(String.format("illegal j1,j2: %d,%d (y.length=%d)", j1, j2, y.length));
			}
					
			for(int j = j1; j < j2; j++) { 
				transcripts.get(j).add(i);
			}
		}		
	}
	
	public Likelihoods(TranscriptArrangement arr, Integer ch) { 
		y = convertDoubles(arr.cluster.channelValues(ch));
		x = convertInts(arr.cluster.channelLocations());
		variance = 1.0;
		lambda = 0.001;
		gammas = new double[arr.calls.length];
		debugPrint = false;
		threeprime = new int[arr.calls.length];
		
		transcripts = new TreeMap<Integer,Set<Integer>>();
		for(int i = 0; i < y.length; i++) {
			transcripts.put(i, new TreeSet<Integer>());
		}

		for(int i = 0; i < gammas.length; i++) { 
			gammas[i] = 1.0;
			int startIdx = arr.calls[i].start;
			int endIdx = arr.calls[i].end;
			threeprime[i] = arr.cluster.segments[endIdx-1].end;
		
			int j1 = arr.cluster.segmentOffsets[startIdx];
			int j2 = endIdx < arr.cluster.segments.length ? 
					arr.cluster.segmentOffsets[endIdx] : 
					y.length;
					
			for(int j = j1; j < j2; j++) { 
				transcripts.get(j).add(i);
			}
		}
	}
	
	private static int[] convertInts(Integer[] d) { 
		int[] a = new int[d.length];
		for(int i = 0; i < d.length; i++) { a[i] = d[i]; }
		return a;
	}
	
	private static double[] convertDoubles(Double[] d) { 
		double[] a = new double[d.length];
		for(int i = 0; i < d.length; i++) { a[i] = d[i]; }
		return a;
	}
	
	public Likelihoods(double[] ys, double[] gs, int[] xs, int[] three, double v, double l) {
		y = ys;
		gammas = gs;
		x = xs;
		threeprime = three;
		debugPrint = false;
		
		if(x.length != y.length) { throw new IllegalArgumentException(); }
		if(threeprime.length != gammas.length) { throw new IllegalArgumentException(); }
		
		variance = v;
		lambda = l;
		transcripts = new TreeMap<Integer,Set<Integer>>();
		
		for(int i =0; i < y.length; i++) {  
			transcripts.put(i, new TreeSet<Integer>());
		}
	}
	
	public void setDebugPrint(boolean dp) { 
		debugPrint = dp;
	}
	
	public FunctionModel getLGammaFunction(int t) { 
		return new LGamma(t);
	}
	
	public FunctionModel getLLambdaFunction() { 
		return new LLambda();
	}
	
	public FunctionModel getDLGammaFunction(int t) { 
		return new DLGamma(t);
	}
	
	public FunctionModel getDLLambdaFunction() { 
		return new DLLambda();
	}
	
	public double lambda() { return lambda; }
	public double variance() { return variance; }
	
	public double delta(int i, int t) {
		int diff = threeprime[t] - x[i];
		return diff >= 0 ? (double)diff : 0.0;
	}
	
	public void setTranscriptBounds(int t, int i1, int i2) { 
		for(int i = i1; i < i2; i++) { 
			transcripts.get(i).add(t);
		}
	}
	
	public double[] gammas() { return gammas; }

	/**
	 * This is the backtracking step assessment necessary for gradient descent.
	 * 
	 * (To be more specific: the gradient calculation gives us the *direction* 
	 * of the step we want to take, to increase the absolute level of the 
	 * log-likelihood function. However, we need to walk some distance in that 
	 * direction, and it's not clear how far that distance should be -- if we 
	 * walk too far, then we overshoot the maximum, and this could lead to 
	 * oscillations.  In this method, we simply check that different values of the 
	 * step size will lead to an increasing absolute value of the function.  
	 * If it doesn't, then we decrease the step size until it does.)  
	 * 
	 * @param likelihood
	 * @param dgammas
	 * @param dlambda
	 * @param eps
	 * @return
	 */
	public Double findStep(double likelihood, double[] dgammas, double dlambda, double eps) {
		if(debugPrintZeroFinding) { 
			System.out.println(
					String.format(
							"\nfindStep(like=%f,#dgammas=%d,dlambda=%f,eps=%f)",
							likelihood, dgammas.length, dlambda, eps));
		}
		
		Double step = 1.0;
		
		double newLikelihood = steppedLikelihood(dgammas, dlambda, step);
		boolean iterating = true;
		int decreased = 0;

		while(iterating && newLikelihood < likelihood && (step > 0.0 && !Double.isNaN(step))) { 
			step /= 2.0;
			decreased += 1;
			newLikelihood = steppedLikelihood(dgammas, dlambda, step);
			if(debugPrintZeroFinding) { 
				System.out.println(String.format("\tstep -> %f, like=%f", step, newLikelihood));
			}
			//iterating = false;
			for(int i = 0; i < dgammas.length && !iterating; i++) { 
				//iterating = dgammas[i] * step > eps;
			}
		}
		
		if(debugPrintZeroFinding) { 
			System.out.println(String.format("\tstep-decreases: %d", decreased));
		}

		if(!iterating || step == 0.0 || Double.isNaN(step)) {
			if(debugPrintZeroFinding) { 
				System.out.println("\tstep=NULL");
			}
			return null;
		} else { 
			if(debugPrintZeroFinding) { 
				System.out.println(String.format("\tstep=%f", step));
			}
		}

		return step;
	}

	public boolean gradientOptimize() {
		boolean iterating = true;
		double eps = 0.01;
		double zeroEps = 1.0e-6;
		int iters = 0;
		
		FunctionModel[] dlgammas = new FunctionModel[gammas.length];
		for(int i = 0; i < dlgammas.length; i++) { 
			dlgammas[i] = new DLGamma(i);
		}
		FunctionModel dllambda = new DLLambda();

		double[] dgammas = new double[gammas.length];
		double dlambda = 0.0;

		while(iterating || iters < 5) {
			
			double dsum = 0.0;
			iters += 1;
			if(debugPrint) { System.out.print(String.format("%d:", iters)); }

			for(int t = 0; t < gammas.length; t++) {
				double z = 0.0;

				/*
				 *  First, we calculate the derivative at this point.
				 */

				try {
					dgammas[t] = dlgammas[t].eval(gammas[t]);
					dsum += dgammas[t];

					//z = findZero(dlgammas[t], 1.0, eps);
				} catch(IllegalArgumentException e) { 
					StringBuilder sb = new StringBuilder();
					for(int k = 0; k < gammas.length; k++) { 
						sb.append(k == t ? "***** " : String.format("%.5f ", gammas[t]));
					}
					sb.append(String.format("lmbda=%.5f", lambda));
					sb.append(String.format(" - Couldn't optimize gammas[%d]", t));
					//System.out.println(sb.toString());
					return false;
				}
			}
			
			//double lz = findZero(dllambda, 0.0, 1.0, zeroEps);
			dlambda = dllambda.eval(lambda);
			dsum += dlambda;
			
			for(int t = 0; t < dgammas.length; t++) { 
				dgammas[t] /= dsum;
				if(debugPrint) { 
					System.out.print(String.format(" dg%d=%f (%f)", t, dgammas[t], gammas[t]));
				}
			}
			dlambda /= dsum;
			if(debugPrint) { 
				System.out.print(String.format(" dl=%f (%f)\n", dlambda, lambda));
			}

			/*
			 * Now, we do the updating.
			 */

			Double step = findStep(likelihood(), dgammas, dlambda, eps);
			iterating = false;

			if(step != null) { 
				System.out.print(String.format(" step=%f", step));
				for(int t = 0; t < gammas.length; t++) {
					double dg = step * dgammas[t];
					gammas[t] += dg;
					iterating = iterating || dg >= eps;
					if(debugPrint) { 
						System.out.print(String.format(
								" g%d=%.5f (%.5f)", t, gammas[t], Math.log(gammas[t]))); 
					}
				}

				double dl = step * dlambda;
				lambda += dl;
				if(debugPrint) { System.out.println(String.format(", lmbda=%f", lambda)); }

				variance = estimateVariance();
			}
		}
		
		return true;
	}
	
	public boolean coordinateOptimize() {
		boolean iterating = true;
		double eps = 0.01;
		double zeroEps = 1.0e-6;
		int iters = 0;
		
		FunctionModel[] dlgammas = new FunctionModel[gammas.length];
		for(int i = 0; i < dlgammas.length; i++) { 
			dlgammas[i] = new DLGamma(i);
		}
		FunctionModel dllambda = new DLLambda();

		double[] dgammas = new double[gammas.length];
		double dlambda = 0.0;
		
		while(iterating) {
			
			iters += 1;
			if(debugPrint) { System.out.print(String.format("%d:", iters)); }
			iterating = false;

			for(int t = 0; t < gammas.length; t++) {
				double z = 0.0;

				try { 
					z = findZero(dlgammas[t], 1.0, eps);
				} catch(IllegalArgumentException e) { 
					StringBuilder sb = new StringBuilder();
					for(int k = 0; k < gammas.length; k++) { 
						sb.append(k == t ? "***** " : String.format("%.5f ", gammas[t]));
					}
					sb.append(String.format("lmbda=%.5f", lambda));
					sb.append(String.format(" - Couldn't optimize gammas[%d]", t));
					//System.out.println(sb.toString());
					return false;
				}
		
				z = Math.max(z, 0.001);
				double d = z - gammas[t];
				iterating = iterating || Math.abs(d) >= eps;
				//dgammas[t] = d;
				gammas[t] = z;
			}
			
			double lz = findZero(dllambda, 1.0, 1.0e-6);
			//double lz = findZero(dllambda, 0.0, 1.0, zeroEps);
			double dz = lz - lambda;
			//dlambda = dz;
			lambda = lz;

			for(int t = 0; t < gammas.length; t++) { 
				if(debugPrint) { 
					System.out.print(String.format(
							" g%d=%.5f (%.5f)", t, gammas[t], Math.log(gammas[t]))); 
				}
			}
			if(debugPrint) { System.out.println(String.format(", lmbda=%f", lambda)); }

			variance = estimateVariance();
		}
		
		return true;
	}
	
	public double error() { 
		double sum = 0.0;
		
		for(int i = 0; i < y.length; i++) {
			double pi = 0.0;

			for(int t : transcripts.get(i)) {
				double gammai = gammas[t];
				double dit = delta(i, t);

				double pit = gammai * Math.exp(-lambda * dit);
				pi += pit;
			}

			double zi = Math.log(pi);
			double err = Math.abs(y[i] - zi);

			sum += err;
		}

		return sum;				
	}

	public double steppedLikelihood(double[] dgammas, double dlambda, double step) { 
		double c1 = 0.0;
		double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
		double sum = 0.0;
		
		double cpart = -0.5 * Math.log(Math.sqrt(2.0 * Math.PI * variance));
		
		for(int i = 0; i < y.length; i++) {
			c1 += cpart;
			double pi = 0.0;

			for(int t : transcripts.get(i)) {
				double gammai = gammas[t] + (step * dgammas[t]);
				double lmbda = lambda + (step * dlambda);
				double dit = delta(i, t);

				double pit = gammai * Math.exp(-lmbda * dit);
				pi += pit;
			}

			double zi = Math.log(pi);
			double err = y[i] - zi;

			sum += (err*err);
		}

		return c1 + c3 * sum;		
	}

	public double likelihood() { 
		double c1 = 0.0;
		double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
		double sum = 0.0;
		
		double cpart = -0.5 * Math.log(Math.sqrt(2.0 * Math.PI * variance));
		
		for(int i = 0; i < y.length; i++) {
			c1 += cpart;
			double pi = 0.0;

			for(int t : transcripts.get(i)) {
				double gammai = gammas[t];
				double dit = delta(i, t);

				double pit = gammai * Math.exp(-lambda * dit);
				pi += pit;
			}

			double zi = Math.log(pi);
			double err = y[i] - zi;

			sum += (err*err);
		}

		return c1 + c3 * sum;		
	}
	
	public double estimateVariance() { 
		double sum = 0.0;
		int N = 0;
		
		for(int i = 0; i < y.length; i++) { 
			double pi = 0.0;

			for(int t : transcripts.get(i)) {
				double gammai = gammas[t];
				double dit = delta(i, t);

				double pit = gammai * Math.exp(-lambda * dit);
				pi += pit;
			}

			double zi = Math.log(pi);
			double err = y[i] - zi;

			sum += (err*err);
		}
		
		return sum / (double)(Math.max(1, N-1));
	}

	public class LGamma extends FunctionModel {
		
		private int gammat;
		
		public LGamma(int gt) { 
			gammat = gt;
		}

		public Double eval(Double gamma) {
			double c1 = 0.0;
			double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
			double sum = 0.0;
			
			double cpart = -0.5 * Math.log(Math.sqrt(2.0 * Math.PI * variance));
			
			for(int i = 0; i < y.length; i++) {
				if(transcripts.get(i).contains(gammat)) {
					c1 += cpart;
					double pi = 0.0;

					for(int t : transcripts.get(i)) {
						double gammai = gammat == t ? gamma : gammas[t];
						double dit = delta(i, t);

						double pit = gammai * Math.exp(-lambda * dit);
						pi += pit;
					}

					double zi = Math.log(pi);
					double err = y[i] - zi;
					
					sum += (err*err);
				}
			}
			
			return c1 + c3 * sum;
		} 
	}

	public class DLGamma extends FunctionModel {
		
		private int gammat;
		
		public DLGamma(int gt) { 
			gammat = gt;
		}

		public Double eval(Double gamma) {
			double c = 1.0 / (double)Math.sqrt(variance);
			double sum = 0.0;
			for(int i = 0; i < y.length; i++) {
				if(transcripts.get(i).contains(gammat)) { 
					double pi = 0.0;

					for(int t : transcripts.get(i)) {
						double gammai = gammat == t ? gamma : gammas[t];
						double dit = delta(i, t);

						double pit = gammai * Math.exp(-lambda * dit);
						pi += pit;
					}

					double zi = Math.log(pi);
					double err = y[i] - zi;
					double ratio = (Math.exp(-lambda * delta(i, gammat))) / pi;
					double termi = (err * ratio);

					sum += termi;
				}
			}
			
			return c * sum;
		} 
	}

	public class LLambda extends FunctionModel {
		
		public LLambda() { 
		}

		public Double eval(Double lmbda) {
			if(lmbda < 0.0) { 
				return -Double.MAX_VALUE;
			}
			
			double c1 = 0.0;
			double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
			double sum = 0.0;
			
			double cpart = -0.5 * Math.log(Math.sqrt(2.0 * Math.PI * variance));
			
			for(int i = 0; i < y.length; i++) {
				c1 += cpart;
				double pi = 0.0;

				for(int t : transcripts.get(i)) {
					double gammai = gammas[t];
					double dit = delta(i, t);

					double pit = gammai * Math.exp(-lmbda * dit);
					pi += pit;
				}

				double zi = Math.log(pi);
				double err = y[i] - zi;

				sum += (err*err);
			}
			
			return c1 + c3 * sum;
		} 
	}

	public class DLLambda extends FunctionModel {
		
		public DLLambda() {}
		
		public Double eval(Double lmbda) {
			if(lmbda < 0.0) { 
				return Double.NaN;
			}
			
			double c = 1.0 / (double)Math.sqrt(variance);
			double sum = 0.0;

			for(int i = 0; i < y.length; i++) {
				double pi = 1.0e-5;
				double pdi = 0.0;

				for(int t : transcripts.get(i)) {
					double gammai = gammas[t];
					double dit = delta(i, t);

					double falloff = Math.exp(-lmbda * dit);
					double pit = gammai * falloff;
					double pdit = pit * dit;
					
					pi += pit;
					pdi += pdit;
				}

				double zi = Math.log(pi);
				if(Double.isInfinite(zi)) { 
					for(int t : transcripts.get(i)) {
						double gammai = gammas[t];
						double dit = delta(i, t);

						double falloff = Math.exp(-lmbda * dit);
						double pit = gammai * falloff;
						double pdit = pit * dit;
						
						System.out.println(String.format("\tgamma[%d]=%.3f, delta[i][%d]=%.1f, falloff=%.3f", t, gammai, t, dit, falloff));
					}
					System.out.println(String.format("dll: y[i]=%.3f, z[i]=%.3f", y[i], zi));
				}
				
				double err = y[i] - zi;
				double ratio = pdi / pi;
				double termi = (err * ratio);

				sum += termi;
			}
			
			return c * sum;
		} 
	}
}
