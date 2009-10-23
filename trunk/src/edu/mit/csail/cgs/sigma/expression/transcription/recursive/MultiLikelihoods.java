/*
 * Author: tdanford
 * Date: Jun 15, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import java.util.*;
import java.util.regex.*;
import java.awt.Color;
import java.io.*;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.*;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.binary.DifferenceFunction;
import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowDataLoader;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.viz.paintable.Paintable;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.layout.StackedPaintable;

public class MultiLikelihoods extends FunctionOptimization {
	
	public static void main(String[] args) {
		WorkflowProperties props = new WorkflowProperties();
		String key = args[0];
		String chrom = args[1];
		String strand = args[2];
		String channelString = args[3];
		
		File probeInput = new File(props.getDirectory(), 
				String.format("%s_%s.data", key, strand.equals("+") ? "plus" : "negative"));
		try {
			WorkflowDataLoader probes = new WorkflowDataLoader(probeInput);
			Pattern regionPattern = Pattern.compile("(\\d+)-(\\d+)");
			ArrayList<RegionKey> regions = new ArrayList<RegionKey>();
			
			String[] carray = channelString.split(",");
			Integer[] channels = new Integer[carray.length];
			for(int i = 0; i < channels.length; i++) { channels[i] = Integer.parseInt(carray[i]); }
			
			for(int i = 4; i < args.length; i++) { 
				Matcher m = regionPattern.matcher(args[i]);
				if(m.matches()) { 
					Integer start = Integer.parseInt(m.group(1));
					Integer end = Integer.parseInt(m.group(2));
					RegionKey r = new RegionKey(chrom, start, end, strand);
					System.out.println(String.format("Transcript Input: %s", r.toString()));
					regions.add(r);
				}
			}
			
			RegionKey[] transcripts = regions.toArray(new RegionKey[0]);
			
			MultiLikelihoods mlikes = new MultiLikelihoods(transcripts, channels, probes);
			mlikes.coordinateOptimize();
			
			double[][] gammas = mlikes.gammas();
			
			for(int t = 0; t < transcripts.length; t++) {
				System.out.print(String.format("T: (%s:%d-%d:%s)", 
						chrom, transcripts[t].start, transcripts[t].end, strand));
				for(int i = 0; i < gammas.length; i++) {
					System.out.print(String.format("\t%d: %.2f", channels[i], Math.log(gammas[i][t])));
				}
				System.out.println();
			}
			System.out.println(String.format("Lambda: %.5f, Var: %.2f", mlikes.lambda, mlikes.variance));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private double[][] y;           // ys[j][i] : (j = channel, i = probe)
	private double[][] gammas;      // gammas[j][t] : (j = gamma, t = trscrpt)

	// There can be fewer gammas than channels, if some channels share a gamma!
	// The chGamma array maps channels to gamma indices -- indicating which gamma 
	// chGamma[j] has responsibility for channel j.
	private int[] chGammas;    // chGamma[j] ( j = channel )
	
	// temporary variable, so we don't have to allocate and re-allocate.
	private double[][] dgammas;
	private FunctionModel[][] dlgammas;
	
	private boolean strand;
	private int[] x, threeprime;
	private double variance, lambda;
	private Map<Integer,Set<Integer>> transcripts;
	private boolean debugPrint;
	
	public MultiLikelihoods(RegionKey[] trans, Integer[] chs, Iterator<ProbeLine> ps) { 
		ArrayList<double[]> ys = new ArrayList<double[]>();
		ArrayList<Integer> xs = new ArrayList<Integer>();
		
		chGammas = new int[chs.length];
		dgammas = new double[chs.length][trans.length];
		gammas = new double[chs.length][trans.length];
		dlgammas = new FunctionModel[chs.length][trans.length];
		
		strand = trans[0].strand.equals("+");
		threeprime = new int[trans.length];
		
		int min = trans[0].start, max = trans[0].end;
		
		for(int t = 0; t < trans.length; t++) { 
			threeprime[t] = strand ? trans[t].end : trans[t].start;
			min = Math.min(min, trans[t].end);
			max = Math.max(max, trans[t].end);
		}
		
		variance = 1.0;
		lambda = 0.001;
		debugPrint = false;
		
		transcripts = new TreeMap<Integer,Set<Integer>>();
		String chrom = trans[0].chrom;
		y = new double[chs.length][];
		
		while(ps.hasNext()) { 
			ProbeLine p = ps.next();
			if(p.chrom.equals(chrom) && p.strand.equals("+") == strand && p.offset >= min && p.offset <= max) { 
				double[] yline = new double[chs.length];
				for(int i = 0; i < chs.length; i++) { 
					yline[i] = p.values[chs[i]];
				}
				ys.add(yline);
				xs.add(p.offset);
			}
		}
		
		x = new int[xs.size()];
		
		for(int i = 0; i < x.length; i++) { 
			x[i] = xs.get(i);
			transcripts.put(i, new TreeSet<Integer>());
			
			for(int t = 0; t < trans.length; t++) { 
				if(trans[t].contains(x[i])) { 
					transcripts.get(i).add(t);
				}
			}
		}
		
		for(int i = 0; i < chs.length; i++) { 
			y[i] = new double[x.length];
			for(int j = 0; j < x.length; j++) { 
				y[i][j] = ys.get(j)[i]; 
			}
		}

		for(int i = 0; i < chs.length; i++) {
			chGammas[i] = i;
			
			for(int j = 0; j < trans.length; j++) { 
				dlgammas[i][j] = new DLGamma(i, j);
				gammas[i][j] = 1.0;
			}
		}
		
	}
	
	public MultiLikelihoods(Cluster c, Endpts[] epts, Integer[] chs, Integer[][] gms) {
		x = c.channeliLocations();
		y = new double[chs.length][];
		chGammas = new int[chs.length];
		
		gammas = new double[gms.length][];
		dgammas = new double[gms.length][epts.length];

		dlgammas = new FunctionModel[gammas.length][epts.length];
		for(int j = 0; j < dlgammas.length; j++) {
			for(int k = 0; k < gms[j].length; k++) { 
				chGammas[gms[j][k]] = j;
			}
			dlgammas[j] = new FunctionModel[epts.length]; 
			gammas[j] = new double[epts.length];

			for(int t = 0; t < dlgammas[j].length; t++) { 
				dlgammas[j][t] = new DLGamma(j, t);
				gammas[j][t] = 1.0;
			}
		}

		for(int j = 0; j < chs.length; j++) { 
			y[j] = c.channeldValues(chs[j]);
		}
		
		variance = 1.0;
		lambda = 0.001;
		debugPrint = false;
		
		threeprime = new int[epts.length];
		
		transcripts = new TreeMap<Integer,Set<Integer>>();
		for(int i = 0; i < y[0].length; i++) {
			transcripts.put(i, new TreeSet<Integer>());
		}
		
		strand = c.strand().equals("+");

		for(int t = 0; t < epts.length; t++) { 
			int startIdx = epts[t].start;
			int endIdx = epts[t].end;
		
			threeprime[t] = strand ? 
					c.segments[endIdx-1].end :
					c.segments[startIdx].start;
		
			int j1 = c.channelSegmentOffset(startIdx);
			int j2 = endIdx < c.segments.length ? 
					c.channelSegmentOffset(endIdx) :  
					y[0].length;
			
			/*
			for(int k = 0; k < c.segments.length; k++) { 
				System.err.println(String.format("%d = %d", k, c.segments[k].dataLocations.length));
			}
			System.err.println(String.format("T %d -> (%d,%d) %d,%d", t, startIdx, endIdx, j1, j2));
			*/
					
			if(j2 > y[0].length || j1 < 0) { 
				throw new IllegalArgumentException(String.format("illegal j1,j2: %d,%d (y.length=%d)", j1, j2, y.length));
			}
					
			for(int j = j1; j < j2; j++) { 
				transcripts.get(j).add(t);
			}
		}		
	}
	
	public MultiLikelihoods(double[][] ys, int[] xs, int[] three, double v, double l) {
		y = ys;
		gammas = new double[ys.length][three.length];
		dgammas = new double[ys.length][three.length];
		dlgammas = new FunctionModel[ys.length][three.length];
		chGammas = new int[ys.length];

		strand = true;
		x = xs;
		threeprime = three;
		debugPrint = true;
		debugPrintZeroFinding = true;

		for(int j = 0; j < dlgammas.length; j++) {
			chGammas[j] = j;
			
			for(int t = 0; t < dlgammas[j].length; t++) { 
				dlgammas[j][t] = new DLGamma(j, t);
				gammas[j][t] = 1.0;
				dgammas[j][t] = 0.0;
			}
		}

		if(x.length != ys[0].length) { throw new IllegalArgumentException(); }
		
		variance = v;
		lambda = l;
		transcripts = new TreeMap<Integer,Set<Integer>>();
		
		for(int i =0; i < ys[0].length; i++) {  
			transcripts.put(i, new TreeSet<Integer>());
		}
	}
	
	public void setDebugPrint(boolean dp) { 
		debugPrint = dp;
		System.out.println(String.format("debugPrint: %b", debugPrint));
	}
	
	public FunctionModel getLGammaFunction(int j, int t) { 
		return new LGamma(j, t);
	}
	
	public FunctionModel getLLambdaFunction() { 
		return new LLambda();
	}
	
	public FunctionModel getDLGammaFunction(int j, int t) { 
		return new DLGamma(j, t);
	}
	
	public FunctionModel getDLLambdaFunction() { 
		return new DLLambda();
	}
	
	public int channelGamma(int ch) { return chGammas[ch]; }
	
	public Set<Integer> gammaChannels(int gi) { 
		Set<Integer> chs = new TreeSet<Integer>();
		for(int j = 0; j < y.length; j++) { 
			if(chGammas[j] == gi) { 
				chs.add(j);
			}
		}
		return chs;
	}
	
	public double lambda() { return lambda; }
	public double variance() { return variance; }
	
	public double delta(int i, int t) {
		int diff = strand ? threeprime[t] - x[i] : x[i] - threeprime[t];
		return diff >= 0 ? (double)diff : 0.0;
	}
	
	public void setTranscriptBounds(int t, int i1, int i2) { 
		System.out.println(String.format("%d -> [%d, %d)", t, i1, i2));
		for(int i = i1; i < i2; i++) { 
			transcripts.get(i).add(t);
		}
	}
	
	public double[] gammas(int j) { return gammas[chGammas[j]]; }
	
	public double[][] gammas() {
		double[][] gs = new double[y.length][];
		for(int j = 0; j < gs.length; j++) { 
			gs[j] = gammas(j).clone();
		}
		return gs;
	}

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
	public Double findStep(double likelihood, double[][] dgammas, double dlambda, double eps) { 
		Double step = 1.0;
		double newLikelihood = steppedLikelihood(dgammas, dlambda, step);
		boolean iterating = true;
		int decreased = 0;

		while(iterating && newLikelihood < likelihood && 
			 (step > 0.0 && !Double.isNaN(step))) {
			
			step /= 2.0;
			decreased += 1;
			newLikelihood = steppedLikelihood(dgammas, dlambda, step);
			
			for(int j = 0; j < dgammas.length && iterating; j++) { 
				for(int t = 0; t < dgammas.length && iterating; t++) { 
					iterating = dgammas[j][t] * step > eps;
				}
			}
		}

		if(!iterating || step == 0.0 || Double.isNaN(step)) { 
			return null;
		}

		return step;
	}

	/**
	 * optimize() sets values for all the parameters: 
	 *    gammas[][]
	 *    lambda
	 *    variance
	 * such that they approximate the maximum of the log-likelihood function.
	 * 
	 * This is done through an iterative approach: gradient descent.
	 * 
	 * @return
	 */
	public boolean gradientOptimize() {
		
		System.out.println("\noptimize()\n");
		
		// (iterating=false) when our step size drops below
		// a certain, threshold level (defined by the eps parameter)
		boolean iterating = true;  
		double eps = 0.01;
		int iters = 0;

		// dllambda computes the derivative of the log-likelihood function
		// with respect to the lambda parameter -- computed *at* the current
		// point (defined by the gammas and variance parameters)
		// 
		// The corresponding helper classes and data structures 
		// (dlgammas and dgammas) are stored as fields to the class itself, 
		// so that we don't have to rebuild them every time if we call 
		// optimize() multiple times.) 
		// The 'variance' parameter needs no numerical log-likelihood-derivative,
		// since its maximum-likelihood value can be computed in closed form.
		FunctionModel dllambda = new DLLambda();
		double dlambda = 0.0;
		
		while(iterating) {
			
			iters += 1;

			if(debugPrint) { 
				System.out.println(String.format("#%d:", iters));
			}

			// We set it to false, and then update -- if any of our gammas
			// change by more than eps, it flips back to true and we keep 
			// iterating.
			iterating = false;

			for(int j = 0; j < gammas.length; j++) {
				if(debugPrint) { 
					System.out.print(String.format("j=%d ", j));
				}
				for(int t = 0; t < gammas[j].length; t++) {
					System.out.print(String.format("t=%d ", t));
					double z = 0.0;

					/*
					 *  First, we calculate the derivative at this point.
					 */

					try { 
						z = findZero(dlgammas[j][t], 1.0, eps);
						System.out.print(String.format("z=%.2f ", z));
			
					} catch(IllegalArgumentException e) {
						
						// This really shouldn't happen, but if we can't 
						// bracket the zero of the gamma parameter, then we 
						// signal an error and abort the optimization.
						
						StringBuilder sb = new StringBuilder();
						for(int k = 0; k < gammas[j].length; k++) { 
							sb.append(k == t ? "***** " : String.format("%.5f ", gammas[j][t]));
						}
						sb.append(String.format("lmbda=%.5f", lambda));
						sb.append(String.format(" - Couldn't optimize gammas[%d][%d]", j, t));
						System.err.println(sb.toString());
			
						return false;
					}

					// This should be a dead-letter, too.
					z = Math.max(z, 0.001);
					
					double d = z - gammas[j][t];
					iterating = iterating || Math.abs(d) >= eps;
					dgammas[j][t] = d;
					System.out.println(String.format("dg=%.2f ", dgammas[j][t]));
					
					if(debugPrint) { 
						System.out.print(String.format("dg[%d]=%.2f ", t, d));
					}
				}
				if(debugPrint) { 
					System.out.println();
				}
			}
			
			try {
				double lz = findZero(dllambda, 1.0e-5, 1.0e-6);
				double dz = lz - lambda;
				dlambda = dz;
				
				if(debugPrint) { 
					System.out.print(String.format("dlbda=%.2f ", dlambda));
				}
			} catch(IllegalArgumentException e) {
				System.err.println(String.format("Couldn't optimize lambda: %f", lambda));
				return false;
			}


			/*
			 * Now, we do the updating.
			 */

			Double step = findStep(likelihood(), dgammas, dlambda, eps);

			if(step != null) { 
				for(int j = 0; j < gammas.length; j++) { 
					for(int t = 0; t < gammas[j].length; t++) { 
						
						gammas[j][t] += (step * dgammas[j][t]);
						
						if(debugPrint) { 
							System.out.print(String.format(
									" g%d,%d=%.5f (%.5f)", j, t, 
									gammas[j][t], 
									Math.log(gammas[j][t])));
							System.out.flush();
						}
					}
				}

				lambda += (step * dlambda);
				if(debugPrint) { 
					System.out.println(String.format(", lmbda=%f", lambda)); 
				}

				// estimateVariance() computes the closed-form maximum likelihood
				// estimate of the variance.
				
				variance = estimateVariance();
				
			} else { 
				iterating = false;
			}
		}
		
		return true;
	}
	
	public boolean coordinateOptimize() {
		
		// (iterating=false) when our step size drops below
		// a certain, threshold level (defined by the eps parameter)
		boolean iterating = true;  
		double eps = 0.01;
		int iters = 0;

		// dllambda computes the derivative of the log-likelihood function
		// with respect to the lambda parameter -- computed *at* the current
		// point (defined by the gammas and variance parameters)
		// 
		// The corresponding helper classes and data structures 
		// (dlgammas and dgammas) are stored as fields to the class itself, 
		// so that we don't have to rebuild them every time if we call 
		// optimize() multiple times.) 
		// The 'variance' parameter needs no numerical log-likelihood-derivative,
		// since its maximum-likelihood value can be computed in closed form.
		FunctionModel dllambda = new DLLambda();
		
		while(iterating) {
			
			iters += 1;

			if(debugPrint) { 
				System.out.println(String.format("#%d:", iters));
			}

			// We set it to false, and then update -- if any of our gammas
			// change by more than eps, it flips back to true and we keep 
			// iterating.
			iterating = false;

			for(int j = 0; j < gammas.length; j++) {
				
				if(debugPrint) { 
					System.out.print(String.format("j=%d", j));
				}
				
				for(int t = 0; t < gammas[j].length; t++) {
					double z = 0.0;

					/*
					 *  First, we calculate the derivative at this point.
					 */

					try { 
						z = findZero(dlgammas[j][t], 1.0, eps);
			
					} catch(IllegalArgumentException e) {
						
						// This really shouldn't happen, but if we can't 
						// bracket the zero of the gamma parameter, then we 
						// signal an error and abort the optimization.
						
						StringBuilder sb = new StringBuilder();
						for(int k = 0; k < gammas[j].length; k++) { 
							sb.append(k == t ? "***** " : String.format("%.5f ", gammas[j][t]));
						}
						sb.append(String.format("lmbda=%.5f", lambda));
						sb.append(String.format(" - Couldn't optimize gammas[%d][%d]", j, t));
						System.err.println(sb.toString());
			
						return false;
					}

					// This should be a dead-letter, too.
					z = Math.max(z, 0.001);
					double d = gammas[j][t] - z;
					iterating = iterating || Math.abs(d) >= eps;
					gammas[j][t] = z;

					if(debugPrint) { 
						System.out.print(String.format(" g[%d][%d]=%f ", t, j, z));
					}
				}

				if(debugPrint) { 
					System.out.println();
				}
			}

			// Set the lambda parameter...
			try { 
				double lz = findZero(dllambda, 1.0e-5, 1.0e-6);
				lambda = Math.max(0.0, lz);
				
			} catch(IllegalArgumentException e) {
				System.err.println(String.format("Couldn't optimize lambda: %f", lambda));
				return false;
			}
			
			if(debugPrint) { 
				System.out.println(String.format("lambda=%f", lambda));
				System.out.println(String.format("log-likelihood=%f", likelihood()));
			}

			// estimateVariance() computes the closed-form maximum likelihood
			// estimate of the variance.
			
			variance = estimateVariance();
		}
		
		return true;
	}
	
	private double steppedLikelihood(double[][] dgammas, double dlambda, double step) { 
		double c1 = 0.0;
		double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
		double sum = 0.0;
		
		double cpart = -0.5 * Math.log(2.0 * Math.PI * variance);

		for(int j = 0; j < y.length; j++) {
			int gi = chGammas[j];
			
			for(int i = 0; i < y[j].length; i++) { 
				c1 += cpart;
		
				double pi = 0.0;
				Set<Integer> trans = transcripts.get(i);

				for(int t : trans) {
					double gammai = gammas[gi][t] + (step * dgammas[gi][t]);
					double lmbda = lambda + (step * dlambda);
					double dit = delta(i, t);

					double pit = gammai * Math.exp(-lmbda * dit);
					pi += pit;
				}

				double zi = Math.log(pi);
				double err = y[j][i] - zi;

				sum += (err*err);
			}
		}

		return c1 + c3 * sum;		
	}

	public double likelihood() { 
		double sum = 0.0;
		for(int j = 0; j < y.length; j++) { 
			sum += likelihood(j);
		}
		return sum;
	}
	
	public double likelihood(int j) { 
		double c1 = 0.0;
		double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
		double sum = 0.0;
		int gi = chGammas[j];
		
		double cpart = -0.5 * Math.log(2.0 * Math.PI * variance);

		for(int i = 0; i < y[j].length; i++) {
			c1 += cpart;
			double pi = 0.0;

			for(int t : transcripts.get(i)) {
				double gammai = gammas[gi][t];
				double dit = delta(i, t);

				double pit = gammai * Math.exp(-lambda * dit);
				pi += pit;
			}

			double zi = Math.log(pi);
			double err = y[j][i] - zi;

			sum += (err*err);
		}

		return c1 + c3 * sum;		
	}
	
	public double estimateVariance() { 
		double sum = 0.0;
		int N = 0;
		
		for(int j = 0; j < y.length; j++) {
			int gi = chGammas[j];
			for(int i = 0; i < y[j].length; i++) {
				double pi = 0.0;

				for(int t : transcripts.get(i)) {
					double gammai = gammas[gi][t];
					double dit = delta(i, t);

					double pit = gammai * Math.exp(-lambda * dit);
					pi += pit;
				}

				double zi = Math.log(pi);
				double err = y[j][i] - zi;

				sum += (err*err);
			}
		}
		
		return sum / (double)(Math.max(1, N));
	}

	public class LGamma extends FunctionModel {
		
		private int gammat, gammaj;
		private Set<Integer> gammaChs;
		
		public LGamma(int gj, int gt) {
			gammat = gt;
			gammaj = gj;
			gammaChs = gammaChannels(gj);
		}

		public Double eval(Double gamma) {
			double c1 = 0.0;
			double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
			double sum = 0.0;
			
			double cpart = -0.5 * Math.log(2.0 * Math.PI * variance);

			for(Integer j : gammaChs) { 
				for(int i = 0; i < y[j].length; i++) {
					if(transcripts.get(i).contains(gammat)) {
						c1 += cpart;
						double pi = 0.0;

						for(int t : transcripts.get(i)) {
							double gammai = gammat == t ? gamma : gammas[gammaj][t];
							double dit = delta(i, t);

							double pit = gammai * Math.exp(-lambda * dit);
							pi += pit;
						}

						double zi = Math.log(pi);
						double err = y[j][i] - zi;

						sum += (err*err);
					}
				}
			}
			
			return c1 + c3 * sum;
		} 
	}

	public class DLGamma extends FunctionModel {
		
		private int gammaj, gammat;
		private Set<Integer> gammaChs;
		
		public DLGamma(int gj, int gt) {
			gammaj = gj;
			gammat = gt;
			gammaChs = gammaChannels(gj);
		}

		public Double eval(Double gamma) {
			double c = 1.0 / (double)Math.sqrt(variance);
			double sum = 0.0;
			
			for(Integer j : gammaChs) { 
				for(int i = 0; i < y[j].length; i++) {

					if(transcripts.get(i).contains(gammat)) {

						// pi : predicted (real scale) value for probe i
						double pi = 0.0;  

						for(int t : transcripts.get(i)) {
							double gammai = gammat == t ? gamma : gammas[gammaj][t];
							double dit = delta(i, t);

							double pit = gammai * Math.exp(-lambda * dit);
							pi += pit;
						}

						double zi = Math.log(pi);
						double err = y[j][i] - zi;
						double ratio = (Math.exp(-lambda * delta(i, gammat))) / pi;
						double termi = (err * ratio);

						sum += termi;
					}
				}
			}

			return c * sum;
		} 
	}

	public class LLambda extends FunctionModel {
		
		public LLambda() { 
		}

		public Double eval(Double lmbda) {
			double c1 = 0.0;
			double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
			double sum = 0.0;
			
			double cpart = -0.5 * Math.log(Math.sqrt(2.0 * Math.PI * variance));
			
			for(int j =0; j < y.length; j++) {
				int gi = chGammas[j];
				for(int i = 0; i < y[j].length; i++) {
					c1 += cpart;
					double pi = 0.0;

					for(int t : transcripts.get(i)) {
						double gammai = gammas[gi][t];
						double dit = delta(i, t);

						double pit = gammai * Math.exp(-lmbda * dit);
						pi += pit;
					}

					double zi = Math.log(pi);
					double err = y[j][i] - zi;

					sum += (err*err);
				}
			}
			
			return c1 + c3 * sum;
		} 
	}

	public class DLLambda extends FunctionModel {
		
		public DLLambda() {}
		
		public Double eval(Double lmbda) {
			double c = 1.0 / (double)Math.sqrt(variance);
			double sum = 0.0;

			for(int j = 0; j < y.length; j++) {
				int gi = chGammas[j];
				for(int i = 0; i < y[j].length; i++) {
					double pi = 0.0;
					double pdi = 0.0;

					for(int t : transcripts.get(i)) {
						double gammai = gammas[gi][t];
						double dit = delta(i, t);

						double falloff = Math.exp(-lmbda * dit);
						double pit = gammai * falloff;
						double pdit = pit * dit;
					
						/*
						if(isBad(pit)) { 
							System.err.println(String.format("%d,%d,%d pit=%f, -lambda=%f, dit=%f, gammai=%f", 
									j, i, t, pit, 
									-lambda, dit, gammai));
						}
						if(isBad(pdit)) { 
							System.err.println(String.format("%d,%d,%d pdit=%f",
									j, i, t, pdit));
						}
						*/

						pi += pit;
						pdi += pdit;
					}

					double zi = Math.log(pi);

					/*
					if(isBad(zi)) { 
						System.err.println(transcripts.get(i).toString());
						System.err.println(String.format("%d,%d zi=%f",
								j, i, zi));
					}
					*/

					double err = y[j][i] - zi;
					double ratio = pdi / pi;
					double termi = (err * ratio);

					sum += termi;
				}
			}
			
			return c * sum;
		} 
	}
}
